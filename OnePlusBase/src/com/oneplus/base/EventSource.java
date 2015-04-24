package com.oneplus.base;

/**
 * Interface for object which owns one or more events.
 */
public interface EventSource extends ThreadDependentObject
{
	/**
	 * Add handler to event.
	 * @param key Event key.
	 * @param handler Event handler to add.
	 */
	<TArgs extends EventArgs> void addHandler(EventKey<TArgs> key, EventHandler<TArgs> handler);
	
	/**
	 * Remove handler from event.
	 * @param key Event key.
	 * @param handler Event handler to remove.
	 */
	<TArgs extends EventArgs> void removeHandler(EventKey<TArgs> key, EventHandler<TArgs> handler);
}
