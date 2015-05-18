package com.oneplus.camera.capturemode;

import android.graphics.drawable.Drawable;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
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
	
	
	// Get display name.
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.capture_mode_photo);
	}
	
	
	// Get image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case CAPTURE_MODES_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.capture_mode_panel_icon_photo);
			default:
				return null;
		}
	}
}
