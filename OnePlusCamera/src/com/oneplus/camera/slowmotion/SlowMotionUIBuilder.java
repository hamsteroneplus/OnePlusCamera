package com.oneplus.camera.slowmotion;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Builder for slow-motion UI component.
 */
public final class SlowMotionUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new SlowMotionUIBuilder instance.
	 */
	public SlowMotionUIBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, SlowMotionUI.class);
	}

	
	// Create components.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new SlowMotionUI(cameraActivity);
	}
}
