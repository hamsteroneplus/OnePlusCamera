package com.oneplus.camera.slowmotion;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.CameraThreadComponentBuilder;

/**
 * Builder for slow-motion controller component.
 */
public final class SlowMotionControllerBuilder extends CameraThreadComponentBuilder
{
	/**
	 * Initialize new SlowMotionControllerBuilder instance.
	 */
	public SlowMotionControllerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, SlowMotionController.class);
	}
	
	
	// Create components.
	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new SlowMotionController(cameraThread);
	}
}
