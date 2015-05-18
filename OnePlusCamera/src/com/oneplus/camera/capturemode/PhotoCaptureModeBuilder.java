package com.oneplus.camera.capturemode;

import com.oneplus.camera.CameraActivity;

/**
 * Builder for photo capture mode.
 */
public class PhotoCaptureModeBuilder implements CaptureModeBuilder
{
	// Create capture mode.
	@Override
	public CaptureMode createCaptureMode(CameraActivity cameraActivity)
	{
		return new PhotoCaptureMode(cameraActivity);
	}
}
