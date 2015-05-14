package com.oneplus.camera;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for sensor AF controller.
 */
public final class SensorFocusControllerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new SensorFocusControllerBuilder instance.
	 */
	public SensorFocusControllerBuilder()
	{
		super(SensorFocusControllerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new SensorFocusControllerImpl(cameraActivity);
	}
}
