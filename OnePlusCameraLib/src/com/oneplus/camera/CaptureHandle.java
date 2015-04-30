package com.oneplus.camera;

import com.oneplus.base.Handle;
import com.oneplus.camera.media.MediaType;

/**
 * Handle represents a media capture.
 */
public abstract class CaptureHandle extends Handle
{
	// Private fields
	private final MediaType m_MediaType;
	
	
	/**
	 * Initialize new CaptureHandle instance.
	 * @param mediaType Captured media type.
	 */
	protected CaptureHandle(MediaType mediaType)
	{
		super("Capture");
		if(mediaType == null)
			throw new IllegalArgumentException("No media type specified.");
		m_MediaType = mediaType;
	}
	
	
	/**
	 * Get captured media type.
	 * @return Media type.
	 */
	public final MediaType getMediaType()
	{
		return m_MediaType;
	}
}
