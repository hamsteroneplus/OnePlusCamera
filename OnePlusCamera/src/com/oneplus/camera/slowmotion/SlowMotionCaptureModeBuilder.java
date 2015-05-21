package com.oneplus.camera.slowmotion;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.capturemode.CaptureMode;
import com.oneplus.camera.capturemode.CaptureModeBuilder;

/**
 * Builder for slow-motion video capture mode.
 */
public class SlowMotionCaptureModeBuilder implements CaptureModeBuilder
{
	// Create capture mode.
	@Override
	public CaptureMode createCaptureMode(CameraActivity cameraActivity)
	{
		return new SlowMotionCaptureMode(cameraActivity);
	}
}
