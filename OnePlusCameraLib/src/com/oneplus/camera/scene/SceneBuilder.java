package com.oneplus.camera.scene;

import com.oneplus.camera.CameraActivity;

/**
 * Scene builder interface.
 */
public interface SceneBuilder
{
	/**
	 * Create new {@link Scene} instance.
	 * @param cameraActivity Camera activity.
	 * @return Created scene.
	 */
	Scene createScene(CameraActivity cameraActivity);
}
