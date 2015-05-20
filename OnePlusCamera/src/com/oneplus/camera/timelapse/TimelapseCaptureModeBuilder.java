package com.oneplus.camera.timelapse;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.capturemode.CaptureMode;
import com.oneplus.camera.capturemode.CaptureModeBuilder;

/**
 * Builder for timelapse video capture mode.
 */
public class TimelapseCaptureModeBuilder implements CaptureModeBuilder
{
	// Create capture mode.
	@Override
	public CaptureMode createCaptureMode(CameraActivity cameraActivity)
	{
		return new TimelapseCaptureMode(cameraActivity);
	}
}
