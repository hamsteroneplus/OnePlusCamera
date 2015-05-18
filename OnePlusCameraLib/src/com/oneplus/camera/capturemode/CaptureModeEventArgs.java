package com.oneplus.camera.capturemode;

import com.oneplus.base.EventArgs;

/**
 * Data for capture mode related events.
 */
public class CaptureModeEventArgs extends EventArgs
{
	// Private fields.
	private final CaptureMode m_CaptureMode;
	
	
	/**
	 * Initialize new CaptureModeEventArgs instance.
	 * @param captureMode Related capture mode.
	 */
	public CaptureModeEventArgs(CaptureMode captureMode)
	{
		m_CaptureMode = captureMode;
	}
	
	
	/**
	 * Get related capture mode.
	 * @return Capture mode.
	 */
	public CaptureMode getCaptureMode()
	{
		return m_CaptureMode;
	}
}
