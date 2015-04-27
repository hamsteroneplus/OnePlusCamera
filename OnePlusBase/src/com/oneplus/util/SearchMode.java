package com.oneplus.util;

/**
 * Search mode.
 */
public enum SearchMode
{
	/**
	 * Find nearest.
	 */
	NEAREST,
	/**
	 * Find nearest, and object must not below target.
	 */
	NEAREST_ABOVE_OR_EQUALS,
	/**
	 * Find nearest, and object must not above target.
	 */
	NEAREST_BELOW_OR_EQUALS,
}
