package com.oneplus.base.component;

import com.oneplus.base.BaseObject;
import com.oneplus.base.HandlerObject;
import com.oneplus.base.PropertyKey;

/**
 * Component interface.
 */
public interface Component extends BaseObject, HandlerObject
{
	/**
	 * Property for component owner.
	 */
	PropertyKey<ComponentOwner> PROP_OWNER = new PropertyKey<>("Owner", ComponentOwner.class, Component.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Property for component state.
	 */
	PropertyKey<ComponentState> PROP_STATE = new PropertyKey<>("State", ComponentState.class, Component.class, ComponentState.NEW);
	
	
	/**
	 * Initialize component.
	 * @return Initialization result.
	 */
	boolean initialize();
}
