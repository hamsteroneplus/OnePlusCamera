package com.oneplus.camera.capturemode;

import android.graphics.drawable.Drawable;

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
	 * Capture mode image usage.
	 */
	public enum ImageUsage
	{
		/**
		 * Icon on capture modes panel.
		 */
		CAPTURE_MODES_PANEL_ICON,
	}
	
	
	/**
	 * Get custom settings for this capture mode.
	 * @return Custom settings, or Null to use global settings.
	 */
	Settings getCustomSettings();
	
	
	/**
	 * Get related image.
	 * @param usage Image usage.
	 * @return Image related to this capture mode.
	 */
	Drawable getImage(ImageUsage usage);
}
