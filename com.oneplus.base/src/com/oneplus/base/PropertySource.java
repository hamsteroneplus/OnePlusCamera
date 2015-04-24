package com.oneplus.base;

/**
 * Interface for object which owns one or more properties.
 */
public interface PropertySource extends ThreadDependentObject
{
	/**
	 * Add call-back for property change.
	 * @param key Property key.
	 * @param callback Call-back to add.
	 */
	<TValue> void addCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback);
	
	/**
	 * Get property value.
	 * @param key Property key.
	 * @return Property value.
	 */
	<TValue> TValue get(PropertyKey<TValue> key);
	
	/**
	 * Remove property change call-back.
	 * @param key Property key.
	 * @param callback Call-back to remove.
	 */
	<TValue> void removeCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback);
	
	/**
	 * Set property value.
	 * @param key Property key.
	 * @param value New value to set.
	 * @return Whether property value changes or not.
	 */
	<TValue> boolean set(PropertyKey<TValue> key, TValue value);
}
