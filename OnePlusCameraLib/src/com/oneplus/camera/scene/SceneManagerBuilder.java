package com.oneplus.camera.scene;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Component builder for {@link SceneManager}.
 */
public final class SceneManagerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new SceneManagerBuilder instance.
	 */
	public SceneManagerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, SceneManagerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new SceneManagerImpl(cameraActivity);
	}
}
