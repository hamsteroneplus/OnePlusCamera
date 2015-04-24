package com.oneplus.base;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Base class for thread which implements {@link BaseObject} and {@link HandlerObject} interfaces.
 */
public abstract class BaseThread extends Thread implements BaseObject, HandlerObject
{
	/**
	 * Log tag.
	 */
	protected final String TAG;
	
	/**
	 * Property to check whether camera thread is started or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_STARTED = new PropertyKey<>("IsStarted", Boolean.class, BaseThread.class, false);
	
	
	/**
	 * Event raised when thread starts.
	 */
	public static final EventKey<EventArgs> EVENT_STARTED = new EventKey<>("Started", EventArgs.class, BaseThread.class);
	/**
	 * Event raised before stopping thread.
	 */
	public static final EventKey<EventArgs> EVENT_STOPPING = new EventKey<>("Stopping", EventArgs.class, BaseThread.class);
	
	
	// Constants
	private static final int MSG_QUIT = -1;
	
	
	// Private fields
	private volatile BaseObjectAdapter m_BaseObjectAdapter;
	private volatile InternalHandler m_Handler;
	private volatile boolean m_IsReleased;
	private volatile boolean m_IsStartCalled;
	private final ThreadStartCallback m_ThreadStartCallback;
	private final Handler m_ThreadStartCallbackHandler;
	
	
	// Class for Handler
	private static final class InternalHandler extends Handler
	{
		private volatile BaseThread m_Owner;
		
		public InternalHandler(BaseThread owner)
		{
			m_Owner = owner;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			BaseThread owner = m_Owner;
			if(owner != null)
				owner.handleMessage(msg);
			else
				Log.e("BaseThread", "Owner released, drop message " + msg.what);
		}
		
		public void release()
		{
			m_Owner = null;
		}
	}
	
	
	/**
	 * Call-back for thread starts.
	 */
	public interface ThreadStartCallback
	{
		/**
		 * Called when thread starts.
		 * @param thread Started {@link BaseThread}.
		 */
		void onCameraThreadStarted(BaseThread thread);
	}
	
	
	/**
	 * Initialize new BaseThread instance.
	 * @param name Thread name.
	 * @param callback Call-back when thread starts.
	 * @param callbackHandler Handler for call-back.
	 */
	protected BaseThread(String name, ThreadStartCallback callback, Handler callbackHandler)
	{
		super(name);
		this.TAG = this.getClass().getSimpleName();
		m_ThreadStartCallback = callback;
		m_ThreadStartCallbackHandler = callbackHandler;
	}
	
	
	// Add call-back for property change.
	@Override
	public <TValue> void addCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
	{
		this.verifyAccess();
		m_BaseObjectAdapter.addCallback(key, callback);
	}
	
	
	// Add handler to event.
	@Override
	public <TArgs extends EventArgs> void addHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		this.verifyAccess();
		m_BaseObjectAdapter.addHandler(key, handler);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_IS_RELEASED)
			return (TValue)(Boolean)m_IsReleased;
		if(this.isDependencyThread())
			return m_BaseObjectAdapter.get(key);
		return key.defaultValue;
	}
	
	
	// Get handler.
	@Override
	public final Handler getHandler()
	{
		return m_Handler;
	}
	
	
	/**
	 * Handle thread message.
	 * @param msg Message.
	 */
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_QUIT:
				Log.w(TAG, "handleMessage() - Quit looper");
				this.onStopping();
				Looper.myLooper().quit();
				break;
		}
	}
	
	
	// Check whether current thread is the thread which object depends on or not.
	@Override
	public boolean isDependencyThread()
	{
		return (Thread.currentThread() == this);
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
	 * Called when thread starts.
	 */
	protected void onStarted()
	{
		// call-back
		if(m_ThreadStartCallback != null && m_ThreadStartCallbackHandler != null)
		{
			if(!m_ThreadStartCallbackHandler.postAtFrontOfQueue(new Runnable()
			{
				@Override
				public void run()
				{
					m_ThreadStartCallback.onCameraThreadStarted(BaseThread.this);
				}
			}))
			{
				Log.e(TAG, "onStarted() - Fail to call-back");
			}
		}
	}
	
	
	/**
	 * Called when thread starting.
	 */
	protected void onStarting()
	{}
	
	
	/**
	 * Called when stopping thread.
	 */
	protected void onStopping()
	{
		this.raise(EVENT_STOPPING, EventArgs.EMPTY);
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
	
	
	// Release thread.
	@Override
	public synchronized void release()
	{
		// check state
		if(m_IsReleased)
			return;
		
		Log.w(TAG, "release()");
		
		// update state
		m_IsReleased = true;
		if(!m_IsStartCalled)
		{
			Log.w(TAG, "release() - Start thread to prevent thread leak");
			super.start();
			m_IsStartCalled = true;
		}
		
		// quit looper
		HandlerUtils.sendMessage(this, MSG_QUIT);
	}
	
	
	// Remove property change call-back.
	@Override
	public <TValue> void removeCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
	{
		this.verifyAccess();
		m_BaseObjectAdapter.removeCallback(key, callback);
	}
	
	
	// Remove handler from event.
	@Override
	public <TArgs extends EventArgs> void removeHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		this.verifyAccess();
		m_BaseObjectAdapter.removeHandler(key, handler);
	}
	
	
	// Thread entry.
	@Override
	public void run()
	{
		Log.w(TAG, "+++++ START +++++");
		try
		{
			// check state
			synchronized(this)
			{
				if(m_IsReleased)
				{
					Log.w(TAG, "Released before starting thread");
					return;
				}
			}
			
			// prepare base object
			m_BaseObjectAdapter = new BaseObjectAdapter(this, this.TAG);
			
			// prepare handler
			Looper.prepare();
			m_Handler = new InternalHandler(this);
			
			// start thread monitor
			ThreadMonitor.startMonitorCurrentThread();
			
			// notify
			this.onStarting();
			
			// notify
			this.onStarted();
			
			// enter message loop
			Log.v(TAG, "Enter loop");
			Looper.loop();
		}
		finally
		{
			// release handler
			if(m_Handler != null)
				m_Handler.release();
			
			// release base object
			if(m_BaseObjectAdapter != null)
				m_BaseObjectAdapter.release();
			
			// stop thread monitor
			ThreadMonitor.stopMonitorCurrentThread();
			
			Log.w(TAG, "+++++ STOP +++++");
		}
	}
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		this.verifyAccess();
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
	
	
	// Start thread.
	@Override
	public synchronized void start()
	{
		// check state
		if(m_IsReleased)
			throw new RuntimeException("Thread has been released.");
		if(m_IsStartCalled)
			throw new RuntimeException("Thread is already started.");
		
		Log.w(TAG, "start()");
		
		// change state
		m_IsStartCalled = true;
		
		// start
		super.start();
	}
	
	
	/**
	 * Throw {@link RuntimeException} if current thread is not base thread.
	 */
	public final void verifyAccess()
	{
		if(Thread.currentThread() != this)
			throw new RuntimeException("Cross-thread access.");
	}
}
