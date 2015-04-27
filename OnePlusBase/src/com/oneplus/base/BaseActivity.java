package com.oneplus.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Base class for activity which implements {@link BaseObject} and {@link HandlerObject} interfaces.
 */
public abstract class BaseActivity extends Activity implements BaseObject, HandlerObject
{
	/**
	 * Log tag.
	 */
	protected final String TAG;
	
	
	/**
	 * Read-only property to check whether activity is running or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_RUNNING = new PropertyKey<>("IsRunning", Boolean.class, BaseActivity.class, false);
	/**
	 * Read-only property to check current activity state.
	 */
	public static final PropertyKey<State> PROP_STATE = new PropertyKey<>("State", State.class, BaseActivity.class, State.NEW);
	
	
	/**
	 * Activity state.
	 */
	public enum State
	{
		/**
		 * Instance is just created.
		 */
		NEW,
		/**
		 * Creating.
		 */
		CREATING,
		/**
		 * Receiving new intent.
		 */
		NEW_INTENT,
		/**
		 * Resuming.
		 */
		RESUMING,
		/**
		 * Running.
		 */
		RUNNING,
		/**
		 * Pausing.
		 */
		PAUSING,
		/**
		 * Paused.
		 */
		PAUSED,
		/**
		 * Stopped.
		 */
		STOPPED,
		/**
		 * Destroying.
		 */
		DESTROYING,
		/**
		 * Destroyed.
		 */
		DESTROYED,
	}
	
	
	// Private fields
	private final BaseObjectAdapter m_BaseObjectAdapter;
	private InternalHandler m_Handler;
	private final Thread m_MainThread;
	
	
	// Class for handler
	private static final class InternalHandler extends Handler
	{
		private volatile BaseActivity m_Owner;
		
		public InternalHandler(BaseActivity owner)
		{
			m_Owner = owner;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			BaseActivity owner = m_Owner;
			if(owner != null)
				owner.handleMessage(msg);
			else
				Log.e("BaseActivity", "Owner released, drop message " + msg.what);
		}
		
		public void release()
		{
			m_Owner = null;
		}
	}
	
	
	/**
	 * Initialize new BaseActivity instance.
	 */
	protected BaseActivity()
	{
		this.TAG = this.getClass().getSimpleName();
		m_MainThread = Thread.currentThread();
		m_BaseObjectAdapter = new BaseObjectAdapter(this, this.TAG);
		m_BaseObjectAdapter.enablePropertyLogs(PROP_IS_RUNNING, BaseObjectAdapter.LOG_PROPERTY_CHANGE);
		m_BaseObjectAdapter.enablePropertyLogs(PROP_STATE, BaseObjectAdapter.LOG_PROPERTY_CHANGE);
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
	
	
	// Called when creating activity.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// call super
		super.onCreate(savedInstanceState);
		
		// create handler
		m_Handler = new InternalHandler(this);
		
		// change state
		this.setReadOnly(PROP_STATE, State.CREATING);
	}
	
	
	// Called when destroying activity
	@Override
	protected void onDestroy()
	{
		// change state
		this.setReadOnly(PROP_STATE, State.DESTROYING);
		this.setReadOnly(PROP_STATE, State.DESTROYED);
		
		// release handler
		if(m_Handler != null)
			m_Handler.release();
		
		// release base object
		m_BaseObjectAdapter.release();
		
		// call super
		super.onDestroy();
	}
	
	
	// Called when receiving new intent.
	@Override
	protected void onNewIntent(Intent intent)
	{
		// call super
		super.onNewIntent(intent);
		
		// change state
		this.setReadOnly(PROP_STATE, State.NEW_INTENT);
	}
	
	
	// Called when pausing.
	@Override
	protected void onPause()
	{
		// change state
		this.setReadOnly(PROP_IS_RUNNING, false);
		this.setReadOnly(PROP_STATE, State.PAUSING);
		
		// call super
		super.onPause();
		
		// change state
		this.setReadOnly(PROP_STATE, State.STOPPED);
	}
	
	
	// Called when resuming.
	@Override
	protected void onResume()
	{
		// change state
		this.setReadOnly(PROP_STATE, State.RESUMING);
		
		// call super
		super.onResume();
		
		// change state
		this.setReadOnly(PROP_STATE, State.RUNNING);
		this.setReadOnly(PROP_IS_RUNNING, true);
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
