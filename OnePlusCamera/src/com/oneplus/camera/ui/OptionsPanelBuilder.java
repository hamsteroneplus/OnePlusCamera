package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

public final class OptionsPanelBuilder extends UIComponentBuilder
{
	public OptionsPanelBuilder()
	{
		super(OptionsPanelImpl.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new OptionsPanelImpl(cameraActivity);
	}
}
