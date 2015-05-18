package com.oneplus.camera.capturemode;

import android.graphics.drawable.Drawable;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
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
	
	
	// Get display name.
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.capture_mode_video);
	}
	
	
	// Get image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case CAPTURE_MODES_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.capture_mode_panel_icon_video);
			default:
				return null;
		}
	}
}
