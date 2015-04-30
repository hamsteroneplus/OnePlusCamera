package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

public final class CaptureBarBuilder extends UIComponentBuilder
{
	public CaptureBarBuilder()
	{
		super(CaptureBar.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CaptureBar(cameraActivity);
	}
}
