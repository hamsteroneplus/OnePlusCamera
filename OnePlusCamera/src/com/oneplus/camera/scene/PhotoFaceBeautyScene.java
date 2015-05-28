package com.oneplus.camera.scene;

import android.graphics.drawable.Drawable;
import android.hardware.camera2.CaptureRequest;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;

/**
 * Face beauty scene for photo.
 */
public final class PhotoFaceBeautyScene extends PhotoScene
{
	// Constructor.
	PhotoFaceBeautyScene(CameraActivity cameraActivity)
	{
		super(cameraActivity, "Face Beauty (Photo)", CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT, FLAG_DISABLE_FLASH);
	}
	
	
	// Get string for display name.
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.scene_face_beauty_photo);
	}
	
	
	// Get related image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case OPTIONS_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.options_panel_icon_face_beauty_photo);
			default:
				return null;
		}
	}
}
