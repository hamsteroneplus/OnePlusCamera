package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Builder for pinch zooming UI component. 
 */
public final class PinchZoomingUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new PinchZoomingUIBuilder instance.
	 */
	public PinchZoomingUIBuilder()
	{
		super(PinchZoomingUI.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new PinchZoomingUI(cameraActivity);
	}
}
