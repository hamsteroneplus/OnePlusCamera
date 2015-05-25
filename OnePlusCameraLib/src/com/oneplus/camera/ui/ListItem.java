package com.oneplus.camera.ui;

import com.oneplus.base.BasicBaseObject;
import com.oneplus.base.PropertyKey;

/**
 * Base class for single settings item.
 */
public abstract class ListItem extends BasicBaseObject
{
	/**
	 * Property for checked state.
	 */
	public static final PropertyKey<Boolean> PROP_IS_CHECKED = new PropertyKey<>("IsChecked", Boolean.class, ListItem.class, 0, null);
	/**
	 * Property to indicate whether item is enabled or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_ENABLED = new PropertyKey<>("IsEnabled", Boolean.class, ListItem.class, PropertyKey.FLAG_NOT_NULL, true);
	/**
	 * Property for summary text.
	 */
	public static final PropertyKey<CharSequence> PROP_SUMMARY = new PropertyKey<>("Summary", CharSequence.class, ListItem.class, 0, null);
	/**
	 * Property for title text.
	 */
	public static final PropertyKey<CharSequence> PROP_TITLE = new PropertyKey<>("Title", CharSequence.class, ListItem.class, 0, null);
}
