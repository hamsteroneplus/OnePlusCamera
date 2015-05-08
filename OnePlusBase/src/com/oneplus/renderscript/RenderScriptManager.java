package com.oneplus.renderscript;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;

import android.content.Context;
import android.renderscript.RenderScript;

/**
 * Provide {@link RenderScript} management functions.
 */
public final class RenderScriptManager
{
	// Constants
	private static final String TAG = "RenderScriptManager";
	
	
	// Private fields
	private static final ThreadLocal<RenderScriptState> m_RenderScriptState = new ThreadLocal<>();
	
	
	// Class for RenderScript state for each thread.
	private static final class RenderScriptState
	{
		public int referenceCounter = 1;
		public final RenderScript renderScript;
		
		public RenderScriptState(RenderScript rs)
		{
			this.renderScript = rs;
		}
	}
	
	
	// Class for RenderScript handle.
	private static final class RenderScriptHandle extends Handle
	{
		public final Thread thread;
		
		public RenderScriptHandle()
		{
			super("RenderScript");
			this.thread = Thread.currentThread();
		}

		@Override
		protected void onClose(int flags)
		{
			destroyRenderScript(this);
		}
	}
	
	
	// Constructor
	private RenderScriptManager()
	{}
	
	
	/**
	 * Create RenderScript context.
	 * @param context Context to create RenderScript.
	 * @return Handle to RenderScript context.
	 */
	public static Handle createRenderScript(Context context)
	{
		RenderScriptState state = m_RenderScriptState.get();
		if(state == null)
		{
			RenderScript rs = RenderScript.create(context);
			state = new RenderScriptState(rs);
			m_RenderScriptState.set(state);
		}
		else
			++state.referenceCounter;
		return new RenderScriptHandle();
	}
	
	
	// Destroy RenderScript context.
	private static void destroyRenderScript(RenderScriptHandle handle)
	{
		if(handle.thread != Thread.currentThread())
			throw new IllegalAccessError("Cannot destroy RenderScript context from another thread.");
		RenderScriptState state = m_RenderScriptState.get();
		if(state != null)
		{
			--state.referenceCounter;
			if(state.referenceCounter <= 0)
			{
				m_RenderScriptState.set(null);
				state.renderScript.finish();
				state.renderScript.destroy();
			}
		}
	}
	
	
	/**
	 * Get created RenderScript context.
	 * @param handle Handle returned from {@link #createRenderScript(Context)}.
	 * @return RenderScript context, or Null if handle is invalid.
	 */
	public static RenderScript getRenderScript(Handle handle)
	{
		if(handle == null)
		{
			Log.e(TAG, "getRenderScript() - Null handle");
			return null;
		}
		if(!(handle instanceof RenderScriptHandle) || !Handle.isValid(handle))
		{
			Log.e(TAG, "getRenderScript() - Invalid handle");
			return null;
		}
		RenderScriptHandle rsHandle = (RenderScriptHandle)handle;
		if(rsHandle.thread != Thread.currentThread())
			throw new IllegalAccessError("Cannot get RenderScript context from another thread.");
		RenderScriptState state = m_RenderScriptState.get();
		if(state != null)
			return state.renderScript;
		Log.e(TAG, "getRenderScript() - No RenderScript context");
		return null;
	}
}
