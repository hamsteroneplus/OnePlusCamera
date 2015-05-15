package com.oneplus.camera.scene;

import com.oneplus.base.EventArgs;

/**
 * Data for {@link Scene} related events.
 */
public class SceneEventArgs extends EventArgs
{
	// Private fields.
	private final Scene m_Scene;
	
	
	/**
	 * Initialize new SceneEventArgs instance.
	 * @param scene Related scene.
	 */
	public SceneEventArgs(Scene scene)
	{
		m_Scene = scene;
	}
	
	
	/**
	 * Get related scene.
	 * @return Related scene.
	 */
	public final Scene getScene()
	{
		return m_Scene;
	}
}
