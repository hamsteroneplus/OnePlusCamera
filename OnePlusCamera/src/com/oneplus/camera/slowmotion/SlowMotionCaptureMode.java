package com.oneplus.camera.slowmotion;

import android.graphics.drawable.Drawable;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.capturemode.ComponentBasedCaptureMode;

final class SlowMotionCaptureMode extends ComponentBasedCaptureMode<SlowMotionUI>
{
	// Constructor.
	SlowMotionCaptureMode(CameraActivity cameraActivity)
	{
		super(cameraActivity, "Slow-motion", "slowmotion", SlowMotionUI.class);
	}
	
	
	// Get display name
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.capture_mode_slow_motion);
	}

	
	// Get related image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case CAPTURE_MODES_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.capture_mode_panel_icon_slow_motion);
			default:
				return null;
		}
	}
}
