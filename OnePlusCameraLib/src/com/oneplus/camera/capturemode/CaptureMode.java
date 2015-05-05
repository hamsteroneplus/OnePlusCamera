package com.oneplus.camera.capturemode;

import com.oneplus.base.BaseObject;
import com.oneplus.base.PropertyKey;
import com.oneplus.camera.Settings;

/**
 * Capture mode interface.
 */
public interface CaptureMode extends BaseObject
{
	/**
	 * Invalid capture mode.
	 */
	CaptureMode INVALID = BasicCaptureMode.INVALID;
	
	
	/**
	 * Flag to keep camera preview state.
	 */
	int FLAG_PRESERVE_CAMERA_PREVIEW_STATE = 0x1;
	
	
	/**
	 * Read-only property for ID represents this capture mode.
	 */
	PropertyKey<String> PROP_ID = new PropertyKey<>("ID", String.class, CaptureMode.class, "");
	/**
	 * Read-only property for current capture mode state.
	 */
	PropertyKey<State> PROP_STATE = new PropertyKey<>("State", State.class, CaptureMode.class, State.EXITED);
	
	
	/**
	 * Capture mode state.
	 */
	enum State
	{
		/**
		 * Entering.
		 */
		ENTERING,
		/**
		 * Entered.
		 */
		ENTERED,
		/**
		 * Exited.
		 */
		EXITING,
		/**
		 * Exited.
		 */
		EXITED,
		/**
		 * Disabled.
		 */
		DISABLED,
		/**
		 * Released.
		 */
		RELEASED,
	}
	
	
	/**
	 * Enter capture mode.
	 * @param prevMode Previous capture mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 * @return Whether capture mode enters successfully or not.
	 */
	boolean enter(CaptureMode prevMode, int flags);
	
	
	/**
	 * Exit capture mode.
	 * @param nextMode Next capture mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 */
	void exit(CaptureMode nextMode, int flags);
	
	
	/**
	 * Get custom settings for this capture mode.
	 * @return Custom settings, or Null to use global settings.
	 */
	Settings getCustomSettings();
}
