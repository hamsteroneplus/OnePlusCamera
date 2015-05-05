package com.oneplus.camera.capturemode;

import java.util.Collections;
import java.util.List;

import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Capture mode manager interface.
 */
public interface CaptureModeManager extends Component
{
	/**
	 * Read-only property for current capture mode.
	 */
	PropertyKey<CaptureMode> PROP_CAPTURE_MODE = new PropertyKey<>("CaptureMode", CaptureMode.class, CaptureModeManager.class, CaptureMode.INVALID);
	/**
	 * Read-only property for list of available capture modes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<CaptureMode>> PROP_CAPTURE_MODE_LIST = new PropertyKey<List<CaptureMode>>("CaptureModeList", (Class)List.class, CaptureModeManager.class, Collections.EMPTY_LIST);
	
	
	/**
	 * Change capture mode.
	 * @param captureMode Capture mode.
	 * @param flags Flags, reserved.
	 * @return Whether capture mode changes successfully or not.
	 */
	boolean setCaptureMode(CaptureMode captureMode, int flags);
}
