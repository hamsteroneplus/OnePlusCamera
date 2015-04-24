package com.oneplus.camera;

import com.oneplus.base.BaseObject;
import com.oneplus.base.BaseObjectAdapter;
import com.oneplus.base.BaseThread;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.HandlerObject;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Base class for camera-related activity.
 */
public abstract class CameraActivity extends Activity implements BaseObject, HandlerObject
{
	// Constants
	protected final String TAG;
	
	
	/**
	 * Read-only property to check whether camera thread is started or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_CAMERA_THREAD_STARTED = new PropertyKey<>("IsCameraThreadStarted", Boolean.class, CameraActivity.class, false);
	
	
	// Private fields
	private final BaseObjectAdapter m_BaseObjectAdapter;
	private CameraThread m_CameraThread;
	private InternalHandler m_Handler;
	private final Thread m_MainThread;
	
	
	// Class for handler
	private static final class InternalHandler extends Handler
	{
		private volatile CameraActivity m_Owner;
		
		public InternalHandler(CameraActivity owner)
		{
			m_Owner = owner;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			CameraActivity owner = m_Owner;
			if(owner != null)
				owner.handleMessage(msg);
			else
				Log.e("CameraActivity", "Owner released, drop message " + msg.what);
		}
		
		public void release()
		{
			m_Owner = null;
		}
	}
	
	
	/**
	 * Initialize new CameraActivity instance.
	 */
	protected CameraActivity()
	{
		this.TAG = this.getClass().getSimpleName();
		m_MainThread = Thread.currentThread();
		m_BaseObjectAdapter = new BaseObjectAdapter(this, this.TAG);
	}
	
	
	// Add call-back for property change.
	@Override
	public <TValue> void addCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
	{
		m_BaseObjectAdapter.addCallback(key, callback);
	}
	
	
	// Add handler to event.
	@Override
	public <TArgs extends EventArgs> void addHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		m_BaseObjectAdapter.addHandler(key, handler);
	}
	
	
	// Get property value.
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		return m_BaseObjectAdapter.get(key);
	}
	
	
	/**
	 * Get related camera thread.
	 * @return Related {@link CameraThread}.
	 */
	public final CameraThread getCameraThread()
	{
		return m_CameraThread;
	}
	
	
	// Get handler.
	@Override
	public Handler getHandler()
	{
		return m_Handler;
	}
	
	
	/**
	 * Handle message.
	 * @param msg Message.
	 */
	protected void handleMessage(Message msg)
	{}
	
	
	// Check whether current thread is the thread which object depends on or not.
	@Override
	public boolean isDependencyThread()
	{
		return (Thread.currentThread() == m_MainThread);
	}
	
	
	/**
	 * Notify that value of given property has been changed.
	 * @param key Property key.
	 * @param oldValue Old property value.
	 * @param newValue New property value.
	 * @return Whether property value changes or not.
	 */
	protected <TValue> boolean notifyPropertyChanged(PropertyKey<TValue> key, TValue oldValue, TValue newValue)
	{
		return m_BaseObjectAdapter.notifyPropertyChanged(key, oldValue, newValue);
	}
	
	
	/**
	 * Called when camera thread starts.
	 */
	protected void onCameraThreadStarted()
	{}
	
	
	// Called when creating activity.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// call super
		super.onCreate(savedInstanceState);
		
		// create handler
		m_Handler = new InternalHandler(this);
		
		// create camera thread
		m_CameraThread = new CameraThread(this, new BaseThread.ThreadStartCallback()
		{
			@Override
			public void onCameraThreadStarted(BaseThread cameraThread)
			{
				CameraActivity.this.setReadOnly(PROP_IS_CAMERA_THREAD_STARTED, true);
				CameraActivity.this.onCameraThreadStarted();
			}
		}, m_Handler);
	}
	
	
	// Called when destroying activity
	@Override
	protected void onDestroy()
	{
		// release camera thread
		if(m_CameraThread != null)
		{
			m_CameraThread.release();
			try
			{
				m_CameraThread.join();
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "onDestroy() - Fail to join camera thread", ex);
			}
		}
		
		// release handler
		if(m_Handler != null)
			m_Handler.release();
		
		// release base object
		m_BaseObjectAdapter.release();
		
		// call super
		super.onDestroy();
	}
	
	
	/**
	 * Raise event.
	 * @param key Key of event to raise.
	 * @param e Event data.
	 */
	protected <TArgs extends EventArgs> void raise(EventKey<TArgs> key, TArgs e)
	{
		m_BaseObjectAdapter.raise(key, e);
	}
	
	
	// Release activity.
	@Override
	public final void release()
	{
		this.finish();
	}
	
	
	// Remove property change call-back.
	@Override
	public <TValue> void removeCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
	{
		m_BaseObjectAdapter.removeCallback(key, callback);
	}
	
	
	// Remove handler from event.
	@Override
	public <TArgs extends EventArgs> void removeHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		m_BaseObjectAdapter.removeHandler(key, handler);
	}
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		return m_BaseObjectAdapter.set(key, value);
	}
	
	
	/**
	 * Set read-only property.
	 * @param key Property key.
	 * @param value New value.
	 * @return Whether property value changes or not.
	 */
	protected <TValue> boolean setReadOnly(PropertyKey<TValue> key, TValue value)
	{
		return m_BaseObjectAdapter.setReadOnly(key, value);
	}
	
	
	/**
	 * Throw {@link RuntimeException} if current thread is not main thread.
	 */
	public final void verifyAccess()
	{
		if(Thread.currentThread() != m_MainThread)
			throw new RuntimeException("Cross-thread access.");
	}
}
