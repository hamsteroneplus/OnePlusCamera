package com.oneplus.camera.capturemode;

import com.oneplus.camera.BasicMode;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.Settings;

/**
 * Basic implementation of {@link CaptureMode}.
 */
public abstract class BasicCaptureMode extends BasicMode<CaptureMode> implements CaptureMode
{
	// Private fields
	private Settings m_CustomSettings;
	private final String m_CustomSettingsName;
	private boolean m_IsCustomSettingsReady;
	
	
	/**
	 * Initialize new BasicCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param id ID represents this capture mode.
	 * @param customSettingsName Name for custom settings.
	 */
	protected BasicCaptureMode(CameraActivity cameraActivity, String id, String customSettingsName)
	{
		super(cameraActivity, id);
		m_CustomSettingsName = customSettingsName;
	}
	
	
	// Get custom settings for this capture mode.
	@Override
	public Settings getCustomSettings()
	{
		this.verifyAccess();
		if(!m_IsCustomSettingsReady && this.get(PROP_STATE) != State.RELEASED)
		{
			m_CustomSettings = this.onCreateCustomSettings(m_CustomSettingsName);
			m_IsCustomSettingsReady = true;
		}
		return m_CustomSettings;
	}
	
	
	/**
	 * Called when creating custom settings.
	 * @param name Settings name.
	 * @return Custom settings, no Null to use global settings.
	 */
	protected Settings onCreateCustomSettings(String name)
	{
		if(name != null)
		{
			CameraActivity cameraActivity = this.getCameraActivity();
			return new Settings(cameraActivity, name, cameraActivity.isServiceMode());
		}
		return null;
	}
	
	
	// Called when releasing.
	@Override
	protected void onRelease()
	{
		// release custom settings
		if(m_CustomSettings != null)
		{
			m_CustomSettings.release();
			m_CustomSettings = null;
		}
		
		// call super
		super.onRelease();
	}
}
