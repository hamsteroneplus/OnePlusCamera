package com.oneplus.camera;

import com.oneplus.base.EventArgs;

/**
 * Data for {@link Camera} related events.
 */
public class CameraEventArgs extends EventArgs
{
	// Private fields.
	private final Camera m_Camera;
	
	
	/**
	 * Initialize new CameraEventArgs instance.
	 * @param camera Related camera.
	 */
	public CameraEventArgs(Camera camera)
	{
		m_Camera = camera;
	}
	
	
	/**
	 * Get related camera.
	 * @return Related camera.
	 */
	public final Camera getCamera()
	{
		return m_Camera;
	}
}
