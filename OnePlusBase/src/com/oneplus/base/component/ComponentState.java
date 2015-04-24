package com.oneplus.base.component;

/**
 * Component state.
 */
public enum ComponentState
{
	/**
	 * Component is just created.
	 */
	NEW,
	/**
	 * Initializing component.
	 */
	INITIALIZING,
	/**
	 * Running.
	 */
	RUNNING,
	/**
	 * Releasing component.
	 */
	RELEASING,
	/**
	 * Released and cannot be used anymore.
	 */
	RELEASED,
}
