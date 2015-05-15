package com.oneplus.camera.scene;

import com.oneplus.camera.CameraActivity;

/**
 * Builder for portrait scene.
 */
public final class PortraitSceneBuilder implements SceneBuilder
{
	// Create scene.
	@Override
	public Scene createScene(CameraActivity cameraActivity)
	{
		return new PortraitScene(cameraActivity);
	}
}
