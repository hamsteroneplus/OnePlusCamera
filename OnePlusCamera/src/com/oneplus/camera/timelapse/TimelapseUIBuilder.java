package com.oneplus.camera.timelapse;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Builder for time-lapse UI component.
 */
public final class TimelapseUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new TimelapseUIBuilder instance.
	 */
	public TimelapseUIBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, TimelapseUI.class);
	}

	
	// Create components.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new TimelapseUI(cameraActivity);
	}
}
