package com.oneplus.camera.capturemode;

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
}
