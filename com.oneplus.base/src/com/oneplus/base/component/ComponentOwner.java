package com.oneplus.base.component;

import com.oneplus.base.EventKey;
import com.oneplus.base.BaseObject;
import com.oneplus.base.HandlerObject;

/**
 * Interface for object which can owns components.
 */
public interface ComponentOwner extends BaseObject, HandlerObject
{
	/**
	 * Event raised after new component added.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	EventKey<ComponentEventArgs<Component>> EVENT_COMPONENT_ADDED = new EventKey<>("ComponentAdded", (Class)ComponentEventArgs.class, ComponentOwner.class);
	/**
	 * Event raised after removing component.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	EventKey<ComponentEventArgs<Component>> EVENT_COMPONENT_REMOVED = new EventKey<>("ComponentRemoved", (Class)ComponentEventArgs.class, ComponentOwner.class);
	
	
	/**
	 * Find component extends or implements given type.
	 * @param componentType Type extended or implemented.
	 * @return Found component, or Null if no component extends or implements given type.
	 */
	<TComponent extends Component> TComponent findComponent(Class<TComponent> componentType);
	
	/**
	 * Find all components extend or implement given type.
	 * @param componentType Type extended or implemented.
	 * @return All matched components, or Null if no component extend or implement given type.
	 */
	<TComponent extends Component> TComponent[] findComponents(Class<TComponent> componentType);
	
	/**
	 * Release and remove given component.
	 * @param component Component to remove.
	 */
	void removeComponent(Component component);
}
