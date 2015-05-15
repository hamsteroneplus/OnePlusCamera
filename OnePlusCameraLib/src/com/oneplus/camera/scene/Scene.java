package com.oneplus.camera.scene;

import com.oneplus.camera.Mode;

/**
 * Scene interface.
 */
public interface Scene extends Mode<Scene>
{
	/**
	 * Invalid scene.
	 */
	Scene INVALID = new InvalidScene();
}
