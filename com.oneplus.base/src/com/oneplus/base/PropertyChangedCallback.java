package com.oneplus.base;

/**
 * Call-back after changing property value.
 * @param <TValue> Type of property value.
 */
public interface PropertyChangedCallback<TValue>
{
	/**
	 * Called after changing property value.
	 * @param source Property source.
	 * @param key Property key.
	 * @param e Event data.
	 */
	void onPropertyChanged(PropertySource source, PropertyKey<TValue> key, PropertyChangeEventArgs<TValue> e);
}
