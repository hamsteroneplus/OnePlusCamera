package com.oneplus.base;

/**
 * Base object interface.
 */
public interface BaseObject extends ThreadDependentObject, PropertySource, EventSource
{
	/**
	 * Property to indicate whether object has been released or not.
	 */
	PropertyKey<Boolean> PROP_IS_RELEASED = new PropertyKey<>("IsReleased", Boolean.class, BaseObject.class, false);
	
	
	/**
	 * Release this object.
	 */
	void release();
}
