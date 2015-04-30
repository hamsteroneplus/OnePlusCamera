package com.oneplus.camera;

import com.oneplus.base.EventArgs;
import com.oneplus.camera.media.MediaType;

/**
 * Data for media capture related events.
 */
public class CaptureEventArgs extends EventArgs
{
	// Private fields.
	private final CaptureHandle m_CaptureHandle;
	
	
	/**
	 * Initialize new CaptureEventArgs instance.
	 * @param handle Capture handle.
	 */
	public CaptureEventArgs(CaptureHandle handle)
	{
		m_CaptureHandle = handle;
	}
	
	
	/**
	 * Get capture handle.
	 * @return Capture handle.
	 */
	public final CaptureHandle getCaptureHandle()
	{
		return m_CaptureHandle;
	}
	
	
	/**
	 * Get captured media type.
	 * @return Media type.
	 */
	public final MediaType getMediaType()
	{
		return (m_CaptureHandle != null ? m_CaptureHandle.getMediaType() : null);
	}
}
