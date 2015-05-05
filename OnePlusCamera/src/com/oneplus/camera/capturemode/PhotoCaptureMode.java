package com.oneplus.camera.capturemode;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.media.MediaType;

/**
 * Capture mode to capture photo.
 */
public class PhotoCaptureMode extends SimpleCaptureMode
{
	/**
	 * Initialize new PhotoCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 */
	public PhotoCaptureMode(CameraActivity cameraActivity)
	{
		this(cameraActivity, "Photo");
	}
	
	
	/**
	 * Initialize new PhotoCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param customSettingsName Name for custom settings.
	 */
	public PhotoCaptureMode(CameraActivity cameraActivity, String customSettingsName)
	{
		super(cameraActivity, "Photo", MediaType.PHOTO, customSettingsName);
	}
}
