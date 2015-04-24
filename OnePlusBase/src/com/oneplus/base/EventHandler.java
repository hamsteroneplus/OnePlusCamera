package com.oneplus.base;

/**
 * Event handler.
 * @param <TArgs> Type of event data.
 */
public interface EventHandler<TArgs extends EventArgs>
{
	/**
	 * Called when receiving event.
	 * @param source Event source.
	 * @param key Event key.
	 * @param e Event data.
	 */
	void onEventReceived(EventSource source, EventKey<TArgs> key, TArgs e);
}
