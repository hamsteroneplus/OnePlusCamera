package com.oneplus.camera.scene;

import android.hardware.camera2.CaptureRequest;

import com.oneplus.camera.CameraActivity;

final class HdrScene extends PhotoScene
{
	// Constructor.
	HdrScene(CameraActivity cameraActivity)
	{
		super(cameraActivity, "HDR", CaptureRequest.CONTROL_SCENE_MODE_HDR);
	}
}
