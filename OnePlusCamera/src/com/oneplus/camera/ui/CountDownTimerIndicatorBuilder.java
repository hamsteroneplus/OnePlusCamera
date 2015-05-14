package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for count-down timer indicator.
 */
public final class CountDownTimerIndicatorBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new CountDownTimerIndicatorBuilder instance.
	 */
	public CountDownTimerIndicatorBuilder()
	{
		super(CountDownTimerIndicator.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CountDownTimerIndicator(cameraActivity);
	}
}
