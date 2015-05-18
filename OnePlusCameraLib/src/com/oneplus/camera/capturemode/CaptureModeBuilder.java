package com.oneplus.camera.capturemode;

import com.oneplus.camera.CameraActivity;

/**
 * Capture mode builder interface.
 */
public interface CaptureModeBuilder
{
	/**
	 * Create capture mode.
	 * @param cameraActivity Camera activity.
	 * @return Created capture mode.
	 */
	CaptureMode createCaptureMode(CameraActivity cameraActivity);
}
