package com.oneplus.camera.capturemode;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.media.MediaType;

/**
 * Capture mode to capture video.
 */
public class VideoCaptureMode extends SimpleCaptureMode
{
	/**
	 * Initialize new VideoCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 */
	public VideoCaptureMode(CameraActivity cameraActivity)
	{
		this(cameraActivity, "Video");
	}
	
	
	/**
	 * Initialize new VideoCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param customSettingsName Name for custom settings.
	 */
	public VideoCaptureMode(CameraActivity cameraActivity, String customSettingsName)
	{
		super(cameraActivity, "Video", MediaType.VIDEO, customSettingsName);
	}
}
