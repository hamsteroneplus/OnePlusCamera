package com.oneplus.camera.media;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for {@link ResolutionManager}.
 */
public final class ResolutionManagerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new ResolutionManagerBuilder instance.
	 */
	public ResolutionManagerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, ResolutionManagerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new ResolutionManagerImpl(cameraActivity);
	}
}
