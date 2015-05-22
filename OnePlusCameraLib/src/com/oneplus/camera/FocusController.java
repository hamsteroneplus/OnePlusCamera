package com.oneplus.camera;

import java.util.Collections;
import java.util.List;

import com.oneplus.base.Handle;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Camera focus controller interface.
 */
public interface FocusController extends Component
{
	/**
	 * Flag to indicate AF should be triggered only one time.
	 */
	int FLAG_SINGLE_AF = 0x1;
	/**
	 * Flag to indicate AF can be triggered continuously.
	 */
	int FLAG_CONTINOUS_AF = 0x2;
	
	
	/**
	 * Read-only property to get current AF regions.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Camera.MeteringRect>> PROP_AF_REGIONS = new PropertyKey<List<Camera.MeteringRect>>("AFRegions", (Class)List.class, FocusController.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property to check whether focus can be changed on primary camera or not.
	 */
	PropertyKey<Boolean> PROP_CAN_CHANGE_FOCUS = new PropertyKey<>("CanChangeFocus", Boolean.class, FocusController.class, false);
	/**
	 * Read-only property to check current focus mode.
	 */
	PropertyKey<FocusMode> PROP_FOCUS_MODE = new PropertyKey<>("FocusMode", FocusMode.class, FocusController.class, FocusMode.DISABLED);
	/**
	 * Read-only property to check current focus state.
	 */
	PropertyKey<FocusState> PROP_FOCUS_STATE = new PropertyKey<>("FocusState", FocusState.class, FocusController.class, FocusState.INACTIVE);
	/**
	 * Read-only property to check whether focus is locked or not.
	 */
	PropertyKey<Boolean> PROP_IS_FOCUS_LOCKED = new PropertyKey<>("IsFocusLocked", Boolean.class, FocusController.class, false);
	
	
	/**
	 * Start auto focus.
	 * @param regions AF regions.
	 * @param flags Flags :
	 * <ul>
	 *   <li>{@link #FLAG_CONTINOUS_AF}</li>
	 *   <li>{@link #FLAG_SINGLE_AF}</li>
	 * </ul>
	 * @return Handle to this AF.
	 */
	Handle startAutoFocus(List<Camera.MeteringRect> regions, int flags);
}
