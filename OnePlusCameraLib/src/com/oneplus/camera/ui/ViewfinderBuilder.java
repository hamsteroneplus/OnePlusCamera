package com.oneplus.camera.ui;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Builder for {@link Viewfinder}.
 */
public final class ViewfinderBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new ViewfinderBuilder instance.
	 */
	public ViewfinderBuilder()
	{
		super(ComponentCreationPriority.HIGH, ViewfinderImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new ViewfinderImpl(cameraActivity);
	}
}
