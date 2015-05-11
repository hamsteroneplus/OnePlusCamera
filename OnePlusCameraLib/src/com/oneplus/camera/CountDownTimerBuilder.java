package com.oneplus.camera;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.CameraThreadComponentBuilder;

public class CountDownTimerBuilder extends CameraThreadComponentBuilder
{
	public CountDownTimerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, CountDownTimerImpl.class);
	}

	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new CountDownTimerImpl(cameraThread);
	}
}
