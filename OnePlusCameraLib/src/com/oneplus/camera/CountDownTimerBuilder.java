package com.oneplus.camera;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

public class CountDownTimerBuilder extends UIComponentBuilder
{
	public CountDownTimerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, CountDownTimerImpl.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CountDownTimerImpl(cameraActivity);
	}
}
