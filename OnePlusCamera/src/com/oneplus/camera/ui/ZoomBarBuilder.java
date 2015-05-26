package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for zoom bar.
 */
public final class ZoomBarBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new ZoomBarBuilder instance.
	 */
	public ZoomBarBuilder()
	{
		super(ZoomBarImpl.class);
	}
	
	
	// Create components.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new ZoomBarImpl(cameraActivity);
	}
}
