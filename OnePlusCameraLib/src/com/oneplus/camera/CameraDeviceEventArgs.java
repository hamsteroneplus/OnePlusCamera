package com.oneplus.camera;

import android.hardware.camera2.CameraDevice;

/**
 * Event data for {@link CameraDevice} related events.
 */
public class CameraDeviceEventArgs extends CameraIdEventArgs
{
	// Fields
	private final CameraDevice m_CameraDevice;
	
	
	/**
	 * Initialize new CameraDeviceEventArgs instance.
	 * @param cameraDevice Related {@link CameraDevice}.
	 */
	public CameraDeviceEventArgs(CameraDevice cameraDevice)
	{
		super(cameraDevice.getId());
		m_CameraDevice = cameraDevice;
	}
	
	
	/**
	 * Get related camera device.
	 * @return {@link CameraDevice}.
	 */
	public final CameraDevice getCameraDevice()
	{
		return m_CameraDevice;
	}
}
