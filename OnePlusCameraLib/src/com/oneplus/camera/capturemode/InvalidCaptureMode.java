package com.oneplus.camera.capturemode;

import android.graphics.drawable.Drawable;

import com.oneplus.camera.InvalidMode;
import com.oneplus.camera.Settings;

class InvalidCaptureMode extends InvalidMode<CaptureMode> implements CaptureMode
{
	// Get custom settings.
	@Override
	public Settings getCustomSettings()
	{
		return null;
	}

	@Override
	public String getDisplayName()
	{
		return null;
	}

	@Override
	public Drawable getImage(ImageUsage usage)
	{
		return null;
	}
}
