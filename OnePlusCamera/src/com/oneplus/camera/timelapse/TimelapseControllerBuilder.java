package com.oneplus.camera.timelapse;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.CameraThreadComponentBuilder;

/**
 * Builder for time-lapse controller component.
 */
public final class TimelapseControllerBuilder extends CameraThreadComponentBuilder
{
	/**
	 * Initialize new TimelapseControllerBuilder instance.
	 */
	public TimelapseControllerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, TimelapseController.class);
	}
	
	
	// Create components.
	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new TimelapseController(cameraThread);
	}
}
