package com.oneplus.camera;

import com.oneplus.base.component.Component;

/**
 * Photo capture handler interface.
 */
public interface PhotoCaptureHandler extends Component
{
	/**
	 * Start photo capture.
	 * @param camera Primary camera.
	 * @param handle Handle for this capture.
	 * @return Whether capture is handled or not.
	 */
	boolean capture(Camera camera, CaptureHandle handle);
	
	
	/**
	 * Stop photo capture.
	 * @param camera Primary camera.
	 * @param handle Handle for this capture.
	 * @return Whether capture stops successfully or not.
	 */
	boolean stopCapture(Camera camera, CaptureHandle handle);
}
