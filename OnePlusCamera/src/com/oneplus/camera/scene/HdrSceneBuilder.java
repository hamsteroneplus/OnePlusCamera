package com.oneplus.camera.scene;

import com.oneplus.camera.CameraActivity;

/**
 * Builder to create HDR scene.
 */
public final class HdrSceneBuilder implements SceneBuilder
{
	// Create scene.
	@Override
	public Scene createScene(CameraActivity cameraActivity)
	{
		return new HdrScene(cameraActivity);
	}
}
