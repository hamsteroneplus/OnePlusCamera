package com.oneplus.camera.scene;

import android.hardware.camera2.CaptureRequest;

import com.oneplus.camera.CameraActivity;

final class PortraitScene extends PhotoScene
{
	PortraitScene(CameraActivity cameraActivity)
	{
		super(cameraActivity, "Portrait", CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT);
	}
}
