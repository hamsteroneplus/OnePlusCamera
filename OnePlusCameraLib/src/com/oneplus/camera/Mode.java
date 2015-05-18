package com.oneplus.camera;

import com.oneplus.base.BaseObject;
import com.oneplus.base.PropertyKey;

/**
 * Base interface for capture mode/scene/effect, etc.
 */
public interface Mode<T extends Mode<?>> extends BaseObject
{
	/**
	 * Flag to keep camera preview state.
	 */
	int FLAG_PRESERVE_CAMERA_PREVIEW_STATE = 0x1;
	
	
	/**
	 * Read-only property for ID represents this mode.
	 */
	PropertyKey<String> PROP_ID = new PropertyKey<>("ID", String.class, Mode.class, "");
	/**
	 * Read-only property for current mode state.
	 */
	PropertyKey<State> PROP_STATE = new PropertyKey<>("State", State.class, Mode.class, State.EXITED);
	
	
	/**
	 * Mode state.
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
	 * Get display name.
	 * @return Display name of this mode.
	 */
	String getDisplayName();
	
	
	/**
	 * Enter to this mode.
	 * @param prevMode Previous mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 * @return Whether mode enters successfully or not.
	 */
	boolean enter(T prevMode, int flags);
	
	
	/**
	 * Exit from this mode.
	 * @param nextMode Next mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 */
	void exit(T nextMode, int flags);
}
