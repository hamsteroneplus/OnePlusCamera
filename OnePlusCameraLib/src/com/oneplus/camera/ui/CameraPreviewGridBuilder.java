package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for {@link CameraPreviewGrid}.
 */
public final class CameraPreviewGridBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new CameraPreviewGridBuilder instance.
	 */
	public CameraPreviewGridBuilder()
	{
		super(CameraPreviewGridImpl.class);
	}
	
	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CameraPreviewGridImpl(cameraActivity);
	}
}
