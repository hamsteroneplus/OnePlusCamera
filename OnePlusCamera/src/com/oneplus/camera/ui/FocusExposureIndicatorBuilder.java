package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for focus/exposure indicator.
 */
public final class FocusExposureIndicatorBuilder extends UIComponentBuilder
{
	// Constructor.
	public FocusExposureIndicatorBuilder()
	{
		super(FocusExposureIndicator.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new FocusExposureIndicator(cameraActivity);
	}
}
