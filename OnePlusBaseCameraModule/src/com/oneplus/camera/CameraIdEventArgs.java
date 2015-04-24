package com.oneplus.camera;

import com.oneplus.base.EventArgs;

/**
 * Event data for camera ID related events.
 */
public class CameraIdEventArgs extends EventArgs
{
	// Fields
	private final String m_CameraId;
	
	
	/**
	 * Initialize new CameraIdEventArgs instance.
	 * @param cameraId Related camera ID.
	 */
	public CameraIdEventArgs(String cameraId)
	{
		m_CameraId = cameraId;
	}
	
	
	/**
	 * Get related camera ID.
	 * @return Camera ID.
	 */
	public final String getCameraId()
	{
		return m_CameraId;
	}
}
