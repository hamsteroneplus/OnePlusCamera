package com.oneplus.camera.ui;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.component.Component;

/**
 * Touch auto exposure UI interface.
 */
public interface TouchAutoExposureUI extends Component
{
	/**
	 * Event raised when triggering touch AE.
	 */
	EventKey<EventArgs> EVENT_TOUCH_AE = new EventKey<EventArgs>("TouchAE", EventArgs.class, TouchAutoFocusUI.class);
}
