package com.oneplus.camera;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Count-down controller interface.
 */
public interface CountDownTimer extends Component
{
	/**
	 * Read-only property to check whether count-down is started or not.
	 */
	PropertyKey<Boolean> PROP_IS_STARTED = new PropertyKey<>("IsStarted", Boolean.class, CountDownTimer.class, false);
	/**
	 * Read-only property to check the remaining seconds.
	 */
	PropertyKey<Long> PROP_REMAINING_SECONDS = new PropertyKey<>("RemainingSeconds", Long.class, CountDownTimer.class, 0L);
	
	
	/**
	 * Event raised when timer has been cancelled.
	 */
	EventKey<EventArgs> EVENT_CANCELLED = new EventKey<>("Cancelled", EventArgs.class, CountDownTimer.class);
	
	
	/**
	 * Start count-down.
	 * @param seconds Total seconds to count-down.
	 * @param flags Flags, reserved.
	 * @return Handle to count-down.
	 */
	Handle start(long seconds, int flags);
}
