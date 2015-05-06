package com.oneplus.camera.ui;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

public final class CaptureModeSwitcherBuilder extends UIComponentBuilder
{
	public CaptureModeSwitcherBuilder()
	{
		super(ComponentCreationPriority.NORMAL, CaptureModeSwitcher.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CaptureModeSwitcher(cameraActivity);
	}
}
