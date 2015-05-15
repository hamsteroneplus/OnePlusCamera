package com.oneplus.camera.capturemode;

import java.util.ArrayList;
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
import com.oneplus.camera.Settings;

final class CaptureModeManagerImpl extends CameraComponent implements CaptureModeManager
{
	// Constants.
	private static final String SETTINGS_KEY_CAPTURE_MODE = "CaptureMode.Current";
	
	
	// Private fields.
	private final List<CaptureMode> m_AllCaptureModes = new ArrayList<>();
	private CaptureMode m_PreviousCaptureMode;
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
					
					break;
				case RELEASED:
					
					break;
				default:
					if(e.getOldValue() == State.DISABLED)
						;
					break;
			}
		}
	};
	
	
	// Constructor
	CaptureModeManagerImpl(CameraActivity cameraActivity)
	{
		super("Capture Mode Manager", cameraActivity, true);
		this.enablePropertyLogs(PROP_CAPTURE_MODE, LOG_PROPERTY_CHANGE);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// setup capture modes
		this.setupAllCaptureModes();
		
		// enter capture mode
		List<CaptureMode> captureModes = this.get(PROP_CAPTURE_MODE_LIST);
		String captureModeId = this.getSettings().getString(SETTINGS_KEY_CAPTURE_MODE);
		CaptureMode initCaptureMode = null;
		if(captureModeId != null)
		{
			for(int i = captureModes.size() - 1 ; i >= 0 ; --i)
			{
				CaptureMode captureMode = captureModes.get(i);
				if(captureModeId.equals(captureMode.get(CaptureMode.PROP_ID)))
				{
					initCaptureMode = captureMode;
					break;
				}
			}
		}
		if(initCaptureMode == null)
		{
			if(!captureModes.isEmpty())
				initCaptureMode = captureModes.get(0);
		}
		if(initCaptureMode != null)
		{
			Log.v(TAG, "onInitialize() - Initial capture mode : ", initCaptureMode);
			this.setCaptureMode(initCaptureMode, 0);
		}
		else
			Log.e(TAG, "onInitialize() - No initial capture mode");
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
	
	
	// Refresh available capture modes.
	private void refreshCaptureModes(boolean checkCaptureMode)
	{
		// refresh capture mode list
		ArrayList<CaptureMode> captureModes = new ArrayList<>();
		for(int i = 0, count = m_AllCaptureModes.size() ; i < count ; ++i)
		{
			CaptureMode captureMode = m_AllCaptureModes.get(i);
			if(captureMode.get(CaptureMode.PROP_STATE) != State.DISABLED)
			{
				Log.v(TAG, "refreshCaptureModes() - Select '", captureMode, "'");
				captureModes.add(captureMode);
			}
		}
		if(captureModes.isEmpty())
			Log.e(TAG, "refreshCaptureModes() - Empty capture mode list");
		this.setReadOnly(PROP_CAPTURE_MODE_LIST, captureModes);
		
		// change to available mode
		if(checkCaptureMode)
		{
			CaptureMode captureMode = this.get(PROP_CAPTURE_MODE);
			if(!captureModes.contains(captureMode))
			{
				Log.w(TAG, "refreshCaptureModes() - Capture mode '" + captureMode + "' is no longer available, switch back to previous capture mode");
				this.switchToPreviousCaptureMode();
			}
		}
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
		if(!this.get(PROP_CAPTURE_MODE_LIST).contains(captureMode))
		{
			Log.e(TAG, "setCaptureMode() - Capture mode '" + captureMode + "' is not contained in list");
			return false;
		}
		CaptureMode prevMode = this.get(PROP_CAPTURE_MODE);
		if(prevMode == captureMode)
		{
			Log.v(TAG, "setCaptureMode() - Change to same capture mode");
			return true;
		}
		
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
			m_PreviousCaptureMode = (prevMode != CaptureMode.INVALID ? prevMode : null);
			this.getSettings().set(SETTINGS_KEY_CAPTURE_MODE, captureMode.get(CaptureMode.PROP_ID));
			this.setReadOnly(PROP_CAPTURE_MODE, captureMode);
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
	
	
	// Setup all capture modes.
	private void setupAllCaptureModes()
	{
		// photo
		CameraActivity cameraActivity = this.getCameraActivity();
		m_AllCaptureModes.add(new PhotoCaptureMode(cameraActivity));
		
		// video
		m_AllCaptureModes.add(new VideoCaptureMode(cameraActivity));
		
		// add call-backs
		for(int i = m_AllCaptureModes.size() - 1 ; i >= 0 ; --i)
			m_AllCaptureModes.get(i).addCallback(CaptureMode.PROP_STATE, m_CaptureModeStateChangedCallback);
		
		// refresh
		this.refreshCaptureModes(false);
	}
	
	
	// Change to previous capture mode.
	private boolean switchToPreviousCaptureMode()
	{
		// complete
		return true;
	}
}
