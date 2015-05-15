package com.oneplus.camera.capturemode;

import com.oneplus.camera.Mode;
import com.oneplus.camera.Settings;

/**
 * Capture mode interface.
 */
public interface CaptureMode extends Mode<CaptureMode>
{
	/**
	 * Invalid capture mode.
	 */
	CaptureMode INVALID = new InvalidCaptureMode();
	
	
	/**
	 * Get custom settings for this capture mode.
	 * @return Custom settings, or Null to use global settings.
	 */
	Settings getCustomSettings();
}
