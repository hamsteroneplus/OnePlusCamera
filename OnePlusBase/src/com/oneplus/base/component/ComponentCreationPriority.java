package com.oneplus.base.component;

/**
 * Priority of component creation.
 */
public enum ComponentCreationPriority
{
	/**
	 * Created when launching application, this is the highest priority.
	 */
	LAUNCH,
	/**
	 * High priority.
	 */
	HIGH,
	/**
	 * Normal priority.
	 */
	NORMAL,
	/**
	 * Low priority.
	 */
	LOW,
	/**
	 * Created on-demand.
	 */
	ON_DEMAND,
}
