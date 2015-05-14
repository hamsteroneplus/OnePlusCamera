package com.oneplus.camera.ui;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.component.Component;

/**
 * Touch AF UI interface.
 */
public interface TouchAutoFocusUI extends Component
{
	/**
	 * Event raised when triggering touch AF.
	 */
	EventKey<EventArgs> EVENT_TOUCH_AF = new EventKey<EventArgs>("TouchAF", EventArgs.class, TouchAutoFocusUI.class);
}
