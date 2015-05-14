package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for {@link TouchAutoExposureUI} and {@link TouchAutoFocusUI}.
 */
public final class TouchFocusExposureUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new TouchFocusExposureUIBuilder instance.
	 */
	public TouchFocusExposureUIBuilder()
	{
		super(TouchFocusExposureUI.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new TouchFocusExposureUI(cameraActivity);
	}
}
