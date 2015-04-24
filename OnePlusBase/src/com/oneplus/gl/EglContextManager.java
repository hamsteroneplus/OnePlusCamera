package com.oneplus.gl;

import java.lang.ref.WeakReference;

import com.oneplus.base.Log;

/**
 * EGL context manager.
 */
public final class EglContextManager
{
	// Private static fields
	private static final String TAG = "EglContextManager";
	private static final ThreadLocal<EglContextState> m_CurrentEglContextState = new ThreadLocal<>();
	
	
	// Class for EGL context state.
	private static final class EglContextState
	{
		public EglObjectHolder activeEglObjectHolders;
		public EglObjectHolder freeEglObjectHolders;
		public boolean isEglContextReady;
	}
	
	
	// Constructor
	private EglContextManager()
	{}
	
	
	// Get EGL context state for current thread.
	private static EglContextState getEglContextState(boolean createNew)
	{
		EglContextState state = m_CurrentEglContextState.get();
		if(state == null && createNew)
		{
			state = new EglContextState();
			m_CurrentEglContextState.set(state);
		}
		return state;
	}
	
	
	/**
	 * Check whether EGL context for current thread is ready to use or not.
	 * @return Whether EGL context is ready or not.
	 */
	public static boolean isEglContextReady()
	{
		EglContextState contextState = getEglContextState(false);
		return (contextState != null && contextState.isEglContextReady);
	}
	
	
	/**
	 * Notify that EGL context for current thread is destroyed.
	 */
	public static void notifyEglContextDestroyed()
	{
		// check state
		EglContextState contextState = getEglContextState(false);
		if(contextState == null || !contextState.isEglContextReady)
			return;
		
		// update state
		contextState.isEglContextReady = true;
		
		// notify objects
		int notifiedCount = 0;
		int recycledCount = 0;
		EglObjectHolder holder = contextState.activeEglObjectHolders;
		while(holder != null)
		{
			EglObjectHolder nextHolder = holder.nextHolder;
			EglObject obj = holder.eglObject.get();
			if(obj != null)
			{
				++notifiedCount;
				obj.onEglContextDestroyed();
			}
			else
			{
				++recycledCount;
				recycleEglObjectHolder(contextState, holder);
			}
			holder = nextHolder;
		}
		Log.v(TAG, "notifyEglContextDestroyed() - ", notifiedCount, " notified, ", recycledCount, " recycled");
	}
	
	
	/**
	 * Notify that EGL context for current thread is ready to use.
	 */
	public static void notifyEglContextReady()
	{
		// check state
		EglContextState contextState = getEglContextState(true);
		if(contextState.isEglContextReady)
			return;
		
		// update state
		contextState.isEglContextReady = true;
		
		// notify objects
		int notifiedCount = 0;
		int recycledCount = 0;
		EglObjectHolder holder = contextState.activeEglObjectHolders;
		while(holder != null)
		{
			EglObjectHolder nextHolder = holder.nextHolder;
			EglObject obj = holder.eglObject.get();
			if(obj != null)
			{
				++notifiedCount;
				obj.onEglContextReady();
			}
			else
			{
				++recycledCount;
				recycleEglObjectHolder(contextState, holder);
			}
			holder = nextHolder;
		}
		Log.v(TAG, "notifyEglContextReady() - ", notifiedCount, " notified, ", recycledCount, " recycled");
	}
	
	
	// Recycle EGL object holder.
	private static void recycleEglObjectHolder(EglContextState contextState, EglObjectHolder holder)
	{
		if(contextState != null && holder != null)
		{
			if(holder.prevHolder != null)
				holder.prevHolder.nextHolder = holder.nextHolder;
			if(holder.nextHolder != null)
				holder.nextHolder.prevHolder = holder.prevHolder;
			holder.prevHolder = null;
			holder.nextHolder = contextState.freeEglObjectHolders;
			if(contextState.freeEglObjectHolders != null)
				contextState.freeEglObjectHolders.prevHolder = holder;
			contextState.freeEglObjectHolders = holder;
			holder.eglObject = null;
		}
	}
	
	
	// Register new EGL object.
	static void registerEglObject(EglObject obj)
	{
		EglContextState contextState = getEglContextState(true);
		EglObjectHolder holder = contextState.freeEglObjectHolders;
		if(holder != null)
		{
			holder.prevHolder = null;
			if(holder.nextHolder != null)
				holder.nextHolder.prevHolder = null;
		}
		else
			holder = new EglObjectHolder();
		holder.eglObject = new WeakReference<>(obj);
		holder.nextHolder = contextState.activeEglObjectHolders;
		if(contextState.activeEglObjectHolders != null)
			contextState.activeEglObjectHolders.prevHolder = holder;
		obj.holder = holder;
	}
	
	
	// Unregister EGL object.
	static void unregisterEglObject(EglObject obj)
	{
		EglContextState contextState = getEglContextState(false);
		recycleEglObjectHolder(contextState, obj.holder);
		obj.holder = null;
	}
}
