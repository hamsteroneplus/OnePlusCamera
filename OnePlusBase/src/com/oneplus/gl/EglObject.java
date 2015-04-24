package com.oneplus.gl;

import com.oneplus.base.BasicThreadDependentObject;

/**
 * Object which is bound to specific EGL context.
 */
public abstract class EglObject extends BasicThreadDependentObject
{
	// Package fields
	EglObjectHolder holder;
	
	
	// Private fields
	private boolean m_IsReleased;
	
	
	/**
	 * Initialize new EglObject instance.
	 */
	protected EglObject()
	{
		EglContextManager.registerEglObject(this);
	}
	
	
	/**
	 * Get related OpenGL object ID.
	 * @return OpenGL object ID.
	 */
	public int getObjectId()
	{
		return 0;
	}
	
	
	/**
	 * Check whether EGL context is ready or not.
	 * @return Whether EGL context is ready or not.
	 */
	public final boolean isEglContextReady()
	{
		return EglContextManager.isEglContextReady();
	}
	
	
	/**
	 * Check whether object has been released or not.
	 * @return Whether object has been released or not.
	 */
	protected final boolean isReleased()
	{
		return m_IsReleased;
	}
	
	
	/**
	 * Called when EGL context destroyed.
	 */
	protected void onEglContextDestroyed()
	{}
	
	
	/**
	 * Called when EGL context is ready to use.
	 */
	protected void onEglContextReady()
	{}
	
	
	/**
	 * Called when releasing object.
	 */
	protected void onRelease()
	{}
	
	
	/**
	 * Release given object and related resources.
	 * @param obj EGL object to release.
	 * @return Null reference.
	 */
	public static EglObject release(EglObject obj)
	{
		// check state
		if(obj == null)
			return null;
		if(obj.m_IsReleased)
			return null;
		obj.verifyAccess();
		
		// unregister
		EglContextManager.unregisterEglObject(obj);
		
		// release
		obj.onRelease();
		
		// update state
		obj.m_IsReleased = true;
		
		// complete
		return null;
	}
	
	
	/**
	 * Throws {@link RuntimeException} is object has been released.
	 */
	protected final void verifyReleaseState()
	{
		if(m_IsReleased)
			throw new RuntimeException("Object has been released.");
	}
}
