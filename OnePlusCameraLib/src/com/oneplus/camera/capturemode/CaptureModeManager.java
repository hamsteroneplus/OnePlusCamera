package com.oneplus.camera.capturemode;

import java.util.Collections;
import java.util.List;

import com.oneplus.base.EventKey;
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
	PropertyKey<List<CaptureMode>> PROP_CAPTURE_MODES = new PropertyKey<List<CaptureMode>>("CaptureModes", (Class)List.class, CaptureModeManager.class, Collections.EMPTY_LIST);
	
	
	/**
	 * Event raised when adding capture mode to list.
	 */
	EventKey<CaptureModeEventArgs> EVENT_CAPTURE_MODE_ADDED = new EventKey<>("CaptureModeAdded", CaptureModeEventArgs.class, CaptureModeManager.class);
	/**
	 * Event raised after removing capture mode from list.
	 */
	EventKey<CaptureModeEventArgs> EVENT_CAPTURE_MODE_REMOVED = new EventKey<>("CaptureModeRemoved", CaptureModeEventArgs.class, CaptureModeManager.class);
	
	
	/**
	 * Add capture mode builder.
	 * @param builder Builder to add.
	 * @param flags Flags, reserved.
	 * @return Whether builder added successfully or not.
	 */
	boolean addBuilder(CaptureModeBuilder builder, int flags);
	
	
	/**
	 * Change to initial capture mode immediately.
	 * @param flags Flags, reserved.
	 * @return Whether capture mode changes successfully or not.
	 */
	boolean changeToInitialCaptureMode(int flags);
	
	
	/**
	 * Change capture mode.
	 * @param captureMode Capture mode.
	 * @param flags Flags, reserved.
	 * @return Whether capture mode changes successfully or not.
	 */
	boolean setCaptureMode(CaptureMode captureMode, int flags);
}
