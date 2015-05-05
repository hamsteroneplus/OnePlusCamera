package com.oneplus.camera.capturemode;

import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.Settings;

/**
 * Basic implementation of {@link CaptureMode}.
 */
public abstract class BasicCaptureMode extends HandlerBaseObject implements CaptureMode
{
	// Private static fields
	static final BasicCaptureMode INVALID = new BasicCaptureMode()
	{
		@Override
		protected boolean onEnter(CaptureMode prevMode, int flags)
		{
			return false;
		}

		@Override
		protected void onExit(CaptureMode nextMode, int flags)
		{}
	};
	
	
	// Private fields
	private final CameraActivity m_CameraActivity;
	private Settings m_CustomSettings;
	private final String m_CustomSettingsName;
	private boolean m_IsCustomSettingsReady;
	private State m_State = State.EXITED;
	
	
	// Constructor
	private BasicCaptureMode()
	{
		super(false);
		m_CameraActivity = null;
		m_CustomSettingsName = null;
	}
	
	
	/**
	 * Initialize new BasicCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param id ID represents this capture mode.
	 * @param customSettingsName Name for custom settings.
	 */
	protected BasicCaptureMode(CameraActivity cameraActivity, String id, String customSettingsName)
	{
		super(true);
		if(cameraActivity == null)
			throw new IllegalArgumentException("No camera activity");
		this.setReadOnly(PROP_ID, id);
		m_CameraActivity = cameraActivity;
		m_CustomSettingsName = customSettingsName;
	}
	
	
	// Change state.
	private State changeState(State state)
	{
		State oldState = m_State;
		if(oldState != state)
		{
			m_State = state;
			this.notifyPropertyChanged(PROP_STATE, oldState, state);
		}
		return m_State;
	}
	
	
	/**
	 * Disable this capture mode until calling {@link #enable()}.
	 */
	protected final void disable()
	{
		this.verifyAccess();
		switch(m_State)
		{
			case DISABLED:
			case RELEASED:
				return;
			case EXITED:
				Log.w(TAG, "disable()");
				break;
			default:
				Log.w(TAG, "disable() - Current state is " + m_State);
				break;
		}
		this.changeState(State.DISABLED);
	}
	
	
	/**
	 * Enable this capture mode.
	 */
	protected final void enable()
	{
		this.verifyAccess();
		if(m_State != State.DISABLED)
			return;
		Log.w(TAG, "enable()");
		this.changeState(State.EXITED);
	}
	
	
	// Enter capture mode.
	@Override
	public final boolean enter(CaptureMode prevMode, int flags)
	{
		// check state
		this.verifyAccess();
		if(m_State != State.EXITED)
		{
			Log.e(TAG, "enter() - Current state is " + m_State);
			return false;
		}
		
		// change state
		if(this.changeState(State.ENTERING) != State.ENTERING)
		{
			Log.e(TAG, "enter() - Entering process was interrupted");
			return false;
		}
		
		// enter
		try
		{
			if(!this.onEnter(prevMode, flags))
			{
				Log.e(TAG, "enter() - Fail to enter");
				if(m_State == State.ENTERING)
					this.changeState(State.EXITED);
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "enter() - Fail to enter", ex);
			if(m_State == State.ENTERING)
				this.changeState(State.EXITED);
			return false;
		}
		
		// change state
		if(m_State != State.ENTERING || this.changeState(State.ENTERED) != State.ENTERED)
		{
			Log.e(TAG, "enter() - Entering process was interrupted");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Exit capture mode.
	@Override
	public final void exit(CaptureMode nextMode, int flags)
	{
		// check state
		this.verifyAccess();
		if(m_State != State.ENTERED)
			return;
		
		// change state
		if(this.changeState(State.EXITING) != State.EXITING)
		{
			Log.w(TAG, "exit() - Exiting process was interrupted");
			return;
		}
		
		// exit
		try
		{
			this.onExit(nextMode, flags);
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "exit() - Unhandled exception occurred while exiting", ex);
		}
		
		// change state
		if(m_State == State.EXITING)
			this.changeState(State.EXITED);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_STATE)
			return (TValue)m_State;
		return super.get(key);
	}
	
	
	/**
	 * Get related camera activity.
	 * @return Camera activity.
	 */
	public final CameraActivity getCameraActivity()
	{
		return m_CameraActivity;
	}
	
	
	// Get custom settings for this capture mode.
	@Override
	public Settings getCustomSettings()
	{
		this.verifyAccess();
		if(!m_IsCustomSettingsReady && m_State != State.RELEASED)
		{
			m_CustomSettings = this.onCreateCustomSettings(m_CustomSettingsName);
			m_IsCustomSettingsReady = true;
		}
		return m_CustomSettings;
	}
	
	
	/**
	 * Check whether capture mode is entered or not.
	 * @return Whether capture mode is entered or not.
	 */
	public final boolean isEntered()
	{
		return (m_State == State.ENTERED);
	}
	
	
	/**
	 * Called when creating custom settings.
	 * @param name Settings name.
	 * @return Custom settings, no Null to use global settings.
	 */
	protected Settings onCreateCustomSettings(String name)
	{
		if(name != null)
			return new Settings(m_CameraActivity, name, m_CameraActivity.isServiceMode());
		return null;
	}
	
	
	/**
	 * Called when entering capture mode.
	 * @param prevMode Previous capture mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 * @return Whether capture mode enters successfully or not.
	 */
	protected abstract boolean onEnter(CaptureMode prevMode, int flags);
	
	
	/**
	 * Called when exiting capture mode.
	 * @param nextMode Next capture mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 */
	protected abstract void onExit(CaptureMode nextMode, int flags);
	
	
	// Called when releasing.
	@Override
	protected void onRelease()
	{
		// change state
		this.changeState(State.RELEASED);
		
		// release custom settings
		if(m_CustomSettings != null)
		{
			m_CustomSettings.release();
			m_CustomSettings = null;
		}
		
		// call super
		super.onRelease();
	}
	
	
	// Set property value.
	@Override
	protected <TValue> boolean setReadOnly(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_STATE)
			throw new IllegalAccessError("Cannot change capture mode state.");
		return super.setReadOnly(key, value);
	}
	
	
	// Get string represents this capture mode.
	@Override
	public String toString()
	{
		return this.get(PROP_ID);
	}
}
