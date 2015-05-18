package com.oneplus.camera.ui;

import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Options panel interface.
 */
public interface OptionsPanel extends Component
{
	/**
	 * Read-only property to check whether there is at least one item on panel or not. 
	 */
	PropertyKey<Boolean> PROP_HAS_ITEMS = new PropertyKey<>("HasItems", Boolean.class, OptionsPanel.class, false);
	/**
	 * Read-only property to check visibility. 
	 */
	PropertyKey<Boolean> PROP_IS_VISIBLE = new PropertyKey<>("IsVisible", Boolean.class, OptionsPanel.class, false);
	
	
	/**
	 * Close options panel.
	 * @param flags Flags, reserved.
	 */
	void closeOptionsPanel(int flags);
	
	
	/**
	 * Open options panel.
	 * @param flags Flags, reserved.
	 * @return Whether options opens shows successfully or not.
	 */
	boolean openOptionsPanel(int flags);
}
