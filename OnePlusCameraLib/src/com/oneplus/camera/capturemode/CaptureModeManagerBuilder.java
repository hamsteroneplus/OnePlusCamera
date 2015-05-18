package com.oneplus.camera.capturemode;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for {@link CaptureModeManager}.
 */
public final class CaptureModeManagerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new CaptureModeManagerBuilder instance.
	 */
	public CaptureModeManagerBuilder()
	{
		super(ComponentCreationPriority.LAUNCH, CaptureModeManagerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CaptureModeManagerImpl(cameraActivity);
	}
}
