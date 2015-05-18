package com.oneplus.camera.capturemode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.Mode.State;
import com.oneplus.camera.scene.Scene;
import com.oneplus.camera.Settings;

final class CaptureModeManagerImpl extends CameraComponent implements CaptureModeManager
{
	// Constants.
	private static final String SETTINGS_KEY_CAPTURE_MODE = "CaptureMode.Current";
	
	
	// Private fields.
	private final List<CaptureMode> m_ActiveCaptureModes = new ArrayList<>();
	private final List<CaptureMode> m_AllCaptureModes = new ArrayList<>();
	private CaptureMode m_CaptureMode = CaptureMode.INVALID;
	private final List<CaptureModeBuilder> m_CaptureModeBuilders = new ArrayList<>();
	private boolean m_IsInitCaptureModeSet;
	private CaptureMode m_PreviousCaptureMode;
	private final List<CaptureMode> m_ReadOnlyActiveCaptureModes;
	private Handle m_SettingsHandle;
	
	
	// Call-backs.
	private final PropertyChangedCallback<CaptureMode.State> m_CaptureModeStateChangedCallback = new PropertyChangedCallback<CaptureMode.State>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<CaptureMode.State> key, PropertyChangeEventArgs<CaptureMode.State> e)
		{
			switch(e.getNewValue())
			{
				case DISABLED:
					onCaptureModeDisabled((CaptureMode)source);
					break;
				case RELEASED:
					onCaptureModeReleased((CaptureMode)source);
					break;
				default:
					if(e.getOldValue() == State.DISABLED)
						onCaptureModeEnabled((CaptureMode)source);
					break;
			}
		}
	};
	
	
	// Constructor
	CaptureModeManagerImpl(CameraActivity cameraActivity)
	{
		super("Capture Mode Manager", cameraActivity, true);
		this.enablePropertyLogs(PROP_CAPTURE_MODE, LOG_PROPERTY_CHANGE);
		m_ReadOnlyActiveCaptureModes = Collections.unmodifiableList(m_ActiveCaptureModes);
		this.setReadOnly(PROP_CAPTURE_MODES, m_ReadOnlyActiveCaptureModes);
	}
	
	
	// Add capture mode builder.
	@Override
	public boolean addBuilder(CaptureModeBuilder builder, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "addBuilder() - Component is not running");
			return false;
		}
		
		// check parameter
		if(builder == null)
		{
			Log.e(TAG, "addBuilder() - No builder to add");
			return false;
		}
		
		// add builder
		m_CaptureModeBuilders.add(builder);
		this.createCaptureMode(builder);
		return true;
	}
	
	
	// Change to initial capture mode immediately.
	@Override
	public boolean changeToInitialCaptureMode(int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "changeToInitialCaptureMode() - Component is not running");
			return false;
		}
		if(m_IsInitCaptureModeSet)
			return true;
		
		// enter capture mode
		String captureModeId = this.getSettings().getString(SETTINGS_KEY_CAPTURE_MODE);
		CaptureMode initCaptureMode = null;
		if(captureModeId != null)
		{
			for(int i = m_ActiveCaptureModes.size() - 1 ; i >= 0 ; --i)
			{
				CaptureMode captureMode = m_ActiveCaptureModes.get(i);
				if(captureModeId.equals(captureMode.get(CaptureMode.PROP_ID)))
				{
					initCaptureMode = captureMode;
					break;
				}
			}
		}
		if(initCaptureMode == null)
		{
			if(!m_ActiveCaptureModes.isEmpty())
				initCaptureMode = m_ActiveCaptureModes.get(0);
		}
		if(initCaptureMode != null)
		{
			Log.v(TAG, "changeToInitialCaptureMode() - Initial capture mode : ", initCaptureMode);
			this.setCaptureMode(initCaptureMode, 0);
		}
		else
		{
			Log.e(TAG, "changeToInitialCaptureMode() - No initial capture mode");
			return false;
		}
		
		// complete
		m_IsInitCaptureModeSet = true;
		return true;
	}
	
	
	// Create capture mode.
	private boolean createCaptureMode(CaptureModeBuilder builder)
	{
		try
		{
			CaptureMode captureMode = builder.createCaptureMode(this.getCameraActivity());
			if(captureMode == null)
			{
				Log.e(TAG, "createCaptureMode() - No capture mode created by " + builder);
				return false;
			}
			Log.v(TAG, "createCaptureMode() - Create '", captureMode, "'");
			m_AllCaptureModes.add(captureMode);
			if(captureMode.get(CaptureMode.PROP_STATE) != State.DISABLED)
			{
				captureMode.addCallback(Scene.PROP_STATE, m_CaptureModeStateChangedCallback);
				m_ActiveCaptureModes.add(captureMode);
				this.raise(EVENT_CAPTURE_MODE_ADDED, new CaptureModeEventArgs(captureMode));
			}
			return true;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "createCaptureMode() - Fail to create capture mode by " + builder, ex);
			return false;
		}
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_CAPTURE_MODE)
			return (TValue)m_CaptureMode;
		if(key == PROP_CAPTURE_MODES)
			return (TValue)m_ReadOnlyActiveCaptureModes;
		return super.get(key);
	}
	
	
	// Called when capture mode state changed to DISABLED.
	private void onCaptureModeDisabled(CaptureMode captureMode)
	{
		if(m_ActiveCaptureModes.remove(captureMode))
		{
			// exit this capture mode
			if(m_CaptureMode == captureMode)
			{
				Log.w(TAG, "onCaptureModeDisabled() - Capture mode '" + captureMode + "' has been disabled when using, exit from this capture mode");
				this.switchToPreviousCaptureMode();
			}
			
			// raise event
			this.raise(EVENT_CAPTURE_MODE_REMOVED, new CaptureModeEventArgs(captureMode));
		}
	}
	
	
	// Called when capture mode state changed from DISABLED.
	private void onCaptureModeEnabled(CaptureMode captureMode)
	{
		int index = m_AllCaptureModes.indexOf(captureMode);
		if(index < 0)
			return;
		for(int i = 0, count = m_ActiveCaptureModes.size() ; i <= count ; ++i)
		{
			if(i < count)
			{
				CaptureMode activeCaptureMode = m_ActiveCaptureModes.get(i);
				if(activeCaptureMode == captureMode)
					return;
				if(m_AllCaptureModes.indexOf(activeCaptureMode) > index)
				{
					m_ActiveCaptureModes.add(i, captureMode);
					break;
				}
			}
			else
				m_ActiveCaptureModes.add(captureMode);
		}
		this.raise(EVENT_CAPTURE_MODE_ADDED, new CaptureModeEventArgs(captureMode));
	}
	
	
	// Called when capture mode state changed to RELEASED.
	private void onCaptureModeReleased(CaptureMode captureMode)
	{
		if(m_ActiveCaptureModes.remove(captureMode))
		{
			// exit this scene
			if(m_CaptureMode == captureMode)
			{
				Log.w(TAG, "onCaptureModeReleased() - Capture mode '" + captureMode + "' has been released when using, exit from this capture mode");
				this.switchToPreviousCaptureMode();
			}
			
			// raise event
			this.raise(EVENT_CAPTURE_MODE_REMOVED, new CaptureModeEventArgs(captureMode));
		}
		if(m_AllCaptureModes.remove(captureMode))
			captureMode.removeCallback(Scene.PROP_STATE, m_CaptureModeStateChangedCallback);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// create capture modes
		for(int i = 0, count = m_CaptureModeBuilders.size() ; i < count ; ++i)
			this.createCaptureMode(m_CaptureModeBuilders.get(i));
	}
	
	
	// Called when releasing.
	@Override
	protected void onRelease()
	{
		// release capture modes
		for(int i = m_AllCaptureModes.size() - 1 ; i >= 0 ; --i)
		{
			CaptureMode captureMode = m_AllCaptureModes.get(i);
			captureMode.removeCallback(CaptureMode.PROP_STATE, m_CaptureModeStateChangedCallback);
			captureMode.release();
		}
		m_AllCaptureModes.clear();
		
		// restore settings
		m_SettingsHandle = Handle.close(m_SettingsHandle);
		
		// call super
		super.onRelease();
	}

	
	// Change capture mode.
	@Override
	public boolean setCaptureMode(CaptureMode captureMode, int flags)
	{
		Log.v(TAG, "setCaptureMode() - Capture mode : ", captureMode);
		
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setCaptureMode() - Component is not running");
			return false;
		}
		CameraActivity cameraActivity = this.getCameraActivity();
		switch(cameraActivity.get(CameraActivity.PROP_PHOTO_CAPTURE_STATE))
		{
			case PREPARING:
			case READY:
				break;
			default:
				Log.e(TAG, "setCaptureMode() - Photo capture state is " + cameraActivity.get(CameraActivity.PROP_PHOTO_CAPTURE_STATE));
				return false;
		}
		switch(cameraActivity.get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
		{
			case PREPARING:
			case READY:
				break;
			default:
				Log.e(TAG, "setCaptureMode() - Video capture state is " + cameraActivity.get(CameraActivity.PROP_VIDEO_CAPTURE_STATE));
				return false;
		}
		
		// check parameter
		if(captureMode == null)
		{
			Log.e(TAG, "setCaptureMode() - No capture mode");
			return false;
		}
		if(!this.get(PROP_CAPTURE_MODES).contains(captureMode))
		{
			Log.e(TAG, "setCaptureMode() - Capture mode '" + captureMode + "' is not contained in list");
			return false;
		}
		CaptureMode prevMode = m_CaptureMode;
		if(prevMode == captureMode)
		{
			Log.v(TAG, "setCaptureMode() - Change to same capture mode");
			return true;
		}
		
		// update state
		m_IsInitCaptureModeSet = true;
		
		// stop preview
		boolean restartPreview;
		switch(cameraActivity.get(CameraActivity.PROP_CAMERA_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				Log.v(TAG, "setCaptureMode() - Stop preview to change capture mode");
				restartPreview = true;
				cameraActivity.stopCameraPreview();
				break;
			default:
				restartPreview = false;
				break;
		}
		
		// exit current capture mode
		prevMode.exit(captureMode, CaptureMode.FLAG_PRESERVE_CAMERA_PREVIEW_STATE);
		
		// change settings
		Settings settings = captureMode.getCustomSettings();
		Handle settingsHandle = (settings != null ? cameraActivity.setSettings(settings) : null);
		
		// enter capture mode
		try
		{
			// enter capture mode
			if(!captureMode.enter(prevMode, CaptureMode.FLAG_PRESERVE_CAMERA_PREVIEW_STATE))
			{
				Log.e(TAG, "setCaptureMode() - Fail to enter '" + captureMode + "', back to '" + prevMode + "'");
				Handle.close(settingsHandle);
				if(!prevMode.enter(CaptureMode.INVALID, CaptureMode.FLAG_PRESERVE_CAMERA_PREVIEW_STATE))
				{
					Log.e(TAG, "setCaptureMode() - Fail to enter '" + prevMode + "'");
					throw new RuntimeException("Fail to Change capture mode");
				}
				return false;
			}
			
			// update state
			Handle.close(m_SettingsHandle);
			m_SettingsHandle = settingsHandle;
			m_PreviousCaptureMode = (prevMode != CaptureMode.INVALID && m_ActiveCaptureModes.contains(prevMode) ? prevMode : null);
			m_CaptureMode = captureMode;
			this.getSettings().set(SETTINGS_KEY_CAPTURE_MODE, captureMode.get(CaptureMode.PROP_ID));
			this.notifyPropertyChanged(PROP_CAPTURE_MODE, prevMode, captureMode);
		}
		finally
		{
			if(restartPreview)
			{
				Log.v(TAG, "setCaptureMode() - Restart preview");
				cameraActivity.startCameraPreview();
			}
		}
		
		// complete
		return true;
	}
	
	
	// Change to previous capture mode.
	private boolean switchToPreviousCaptureMode()
	{
		// switch to previous mode
		if(m_PreviousCaptureMode != null && this.setCaptureMode(m_PreviousCaptureMode, 0))
			return true;
		
		// select first capture mode
		if(!m_ActiveCaptureModes.isEmpty() && this.setCaptureMode(m_ActiveCaptureModes.get(0), 0))
			return true;
		
		// complete
		Log.e(TAG, "switchToPreviousCaptureMode() - No capture mode to switch");
		return false;
	}
}
