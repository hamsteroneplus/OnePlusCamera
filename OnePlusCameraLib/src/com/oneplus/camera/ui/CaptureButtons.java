package com.oneplus.camera.ui;

import android.graphics.drawable.Drawable;

import com.oneplus.base.Handle;
import com.oneplus.base.component.Component;

/**
 * Media capture button(s) interface.
 */
public interface CaptureButtons extends Component
{
	/**
	 * Capture button style.
	 */
	enum ButtonStyle
	{
		/**
		 * Normal.
		 */
		NORMAL,
		/**
		 * Small button.
		 */
		SMALL,
	}
	
	
	/**
	 * Change background of primary capture button.
	 * @param drawable New background drawable.
	 * @param flags Flags, reserved.
	 * @return Handle to button background.
	 */
	Handle setPrimaryButtonBackground(Drawable drawable, int flags);
}
