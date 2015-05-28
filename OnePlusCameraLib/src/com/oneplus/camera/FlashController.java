package com.oneplus.camera;

import com.oneplus.base.Handle;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Camera flash controller interface.
 */
public interface FlashController extends Component
{
	/**
	 * Property to get or set flash mode for primary camera.
	 */
	PropertyKey<FlashMode> PROP_FLASH_MODE = new PropertyKey<>("FlashMode", FlashMode.class, FlashController.class, PropertyKey.FLAG_NOT_NULL, FlashMode.OFF);
	/**
	 * Read-only property to check whether flash is supported on primary camera or not.
	 */
	PropertyKey<Boolean> PROP_HAS_FLASH = new PropertyKey<>("HasFlash", Boolean.class, FlashController.class, false);
	/**
	 * Read-only property to check whether flash is disabled or not.
	 */
	PropertyKey<Boolean> PROP_IS_FLASH_DISABLED = new PropertyKey<>("IsFlashDisabled", Boolean.class, FlashController.class, false);
	
	
	/**
	 * Disable flash temporarily.
	 * @param flags Flags, reserved.
	 * @return Handle to this operation.
	 */
	Handle disableFlash(int flags);
}
