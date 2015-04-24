package com.oneplus.gl;

import java.util.List;

import com.oneplus.base.BaseThread;
import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.view.Surface;

/**
 * OpenGL thread.
 */
public class GLThread extends BaseThread
{
	/**
	 * Abandon all content on surface.
	 */
	public static final int FLAG_ABANDON_CONTENT = 0x1;
	
	
	/**
	 * Read-only property to check whether EGL context is ready to use or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_EGL_CONTEXT_READY = new PropertyKey<>("IsEGLContextReady", Boolean.class, GLThread.class, false);
	
	
	// Constants
	private static final int[] EGL_CONFIG_ATTRS_ARGB = new int[]{
		EGL14.EGL_ALPHA_SIZE, 8,
		EGL14.EGL_RED_SIZE, 8,
		EGL14.EGL_GREEN_SIZE, 8,
		EGL14.EGL_BLUE_SIZE, 8,
		EGL14.EGL_DEPTH_SIZE, 16,
		EGL14.EGL_NONE
	};
	private static final int[] EGL_CONTEXT_ATTRS = new int[]{
		EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
		EGL14.EGL_NONE
	};
	private static final int[] EGL_EMPTY_ATTRS = new int[]{
		EGL14.EGL_NONE
	};
	
	
	// Private fields 
	private List<EglSurfaceHandle> m_ActiveEglSurfaceHandles;
	private EGLConfig m_EglConfig;
	private EGLContext m_EglContext;
	private EGLDisplay m_EglDisplay;
	private List<EglSurfaceHandle> m_EglSurfaceHandles;
	private final Object m_EglStateLock = new Object();
	
	
	// Class for EGL surface handle
	private final class EglSurfaceHandle extends Handle
	{
		public EGLSurface eglSurface;
		public int height;
		public final Surface surface;
		public int width;
		
		public EglSurfaceHandle(Surface surface, int width, int height)
		{
			super("EGLSurface");
			this.surface = surface;
			this.width = width;
			this.height = height;
		}

		@Override
		protected void onClose(int flags)
		{
			destroyEglSurface(this);
		}
	}
	
	
	/**
	 * Initialize new GLThread instance.
	 * @param name Thread name.
	 * @param callback Call-back when thread starts.
	 * @param callbackHandler Handler for call-back.
	 */
	protected GLThread(String name, ThreadStartCallback callback, Handler callbackHandler)
	{
		super(name, callback, callbackHandler);
	}
	
	
	// Create EGL context.
	private boolean createEglContext()
	{
		// check state
		if(m_EglContext != null)
			return true;
		
		// get EGL display
		if(m_EglDisplay == null)
		{
			m_EglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
			Log.w(TAG, "createEglContext() - EGL display : " + m_EglDisplay);
		}
		
		// initialize EGL
		int[] versions = new int[2];
		if(!EGL14.eglInitialize(m_EglDisplay, versions, 0, versions, 1))
		{
			Log.e(TAG, "createEglContext() - Fail to initialize EGL");
			return false;
		}
		Log.w(TAG, "createEglContext() - EGL version : " + versions[0] + "." + versions[1]);
		
		// choose configuration
		EGLConfig[] configs = new EGLConfig[1];
		int[] configCount = new int[1];
		if(!EGL14.eglChooseConfig(m_EglDisplay, EGL_CONFIG_ATTRS_ARGB, 0, configs, 0, 1, configCount, 0))
		{
			Log.e(TAG, "createEglContext() - Fail to choose configuration");
			EGL14.eglTerminate(m_EglDisplay);
			return false;
		}
		m_EglConfig = configs[0];
		Log.w(TAG, "createEglContext() - EGL config : " + m_EglConfig);
		
		// create context
		m_EglContext = EGL14.eglCreateContext(m_EglDisplay, m_EglConfig, null, EGL_CONTEXT_ATTRS, 0);
		Log.w(TAG, "createEglContext() - EGL context : " + m_EglContext);
		
		// complete
		return true;
	}
	
	
	// Create EGL surface.
	private boolean createEglSurface(EglSurfaceHandle handle)
	{
		// check state
		if(handle.eglSurface != null)
			return true;
		
		// create EGL surface
		EGLSurface eglSurface;
		if(handle.surface != null)
		{
			Log.v(TAG, "createEglSurface() - Create window surface for ", handle.surface);
			eglSurface = EGL14.eglCreateWindowSurface(m_EglDisplay, m_EglConfig, handle.surface, EGL_EMPTY_ATTRS, 0);
		}
		else
		{
			Log.v(TAG, "createEglSurface() - Create pbuffer surface");
			int[] attrs = new int[]{
					EGL14.EGL_WIDTH, handle.width,
					EGL14.EGL_HEIGHT, handle.height,
					EGL14.EGL_NONE
			};
			eglSurface = EGL14.eglCreatePbufferSurface(m_EglDisplay, m_EglConfig, attrs, 0);
		}
		if(eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE)
		{
			Log.e(TAG, "createEglSurface() - Fail to create EGL surface");
			return false;
		}
		Log.w(TAG, "createEglSurface() -  EGL surface : " + eglSurface + ", size : " + handle.width + "x" + handle.height);
		handle.eglSurface = eglSurface;
		
		// complete
		return true;
	}
	
	
	// Destroy EGL context.
	private void destroyEglContext()
	{
		// check state
		if(m_EglContext == null)
			return;
		
		// destroy EGL surfaces
		if(!m_EglSurfaceHandles.isEmpty())
		{
			Log.w(TAG, "destroyEglContext() - Destroy all EGL surfaces");
			for(int i = m_EglSurfaceHandles.size() - 1 ; i >= 0 ; --i)
				this.destroyEglSurface(m_EglSurfaceHandles.get(i));
			m_EglSurfaceHandles.clear();
		}
		
		// destroy EGL context
		Log.w(TAG, "destroyEglContext() - Destroy EGL context");
		EGL14.eglDestroyContext(m_EglDisplay, m_EglContext);
		m_EglContext = null;
		m_EglConfig = null;
		
		// terminate EGL
		EGL14.eglTerminate(m_EglDisplay);
	}
	
	
	// Destroy EGL surface.
	private void destroyEglSurface(EglSurfaceHandle handle)
	{
		// check state
		if(handle.eglSurface == null)
			return;
		
		// stop using
		this.stopUsingEglSurface(handle, FLAG_ABANDON_CONTENT);
		
		// destroy EGL surface
		Log.w(TAG, "destroyEglSurface() - Destroy " + handle.eglSurface);
		EGL14.eglDestroySurface(m_EglDisplay, handle.eglSurface);
		handle.eglSurface = null;
	}
	
	
	// Stop using EGL surface
	private void stopUsingEglSurface(EglSurfaceHandle handle, int flags)
	{
		// check handle
		int listSize = m_ActiveEglSurfaceHandles.size();
		int handleIndex = m_ActiveEglSurfaceHandles.indexOf(handle);
		if(handleIndex < 0)
			return;
		
		// remove from list
		m_ActiveEglSurfaceHandles.remove(handleIndex);
		
		// flush content
		boolean isLastHandle = (handleIndex == (listSize - 1));
		if(handleIndex == (listSize - 1) && (flags & FLAG_ABANDON_CONTENT) == 0 && handle.eglSurface != null)
		{
			if(!EGL14.eglSwapBuffers(m_EglDisplay, handle.eglSurface))
				Log.e(TAG, "stopUsingEglSurface() - Fail to swap buffers for " + handle.eglSurface);
		}
		
		// switch to previous EGL surface
		if(isLastHandle)
		{
			EGLSurface prevEglSurface = null;
			for(int i = handleIndex - 1 ; i >= 0 ; --i)
			{
				handle = m_ActiveEglSurfaceHandles.get(i);
				prevEglSurface = handle.eglSurface;
				if(prevEglSurface != null)
					break;
				m_ActiveEglSurfaceHandles.remove(i);
			}
			if(prevEglSurface != null)
			{
				if(!EGL14.eglMakeCurrent(m_EglDisplay, prevEglSurface, prevEglSurface, m_EglContext))
				{
					Log.w(TAG, "stopUsingEglSurface() - Fail to switch EGL surface back to " + prevEglSurface);
					EGL14.eglMakeCurrent(m_EglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, m_EglContext);
					EglContextManager.notifyEglContextDestroyed();
					this.notifyPropertyChanged(PROP_IS_EGL_CONTEXT_READY, true, false);
				}
			}
			else
			{
				Log.w(TAG, "stopUsingEglSurface() - No active EGL surfaces");
				EGL14.eglMakeCurrent(m_EglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, m_EglContext);
				EglContextManager.notifyEglContextDestroyed();
				this.notifyPropertyChanged(PROP_IS_EGL_CONTEXT_READY, true, false);
			}
		}
	}
}
