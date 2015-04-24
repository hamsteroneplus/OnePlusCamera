package com.oneplus.base.component;

/**
 * Component builder interface.
 */
public interface ComponentBuilder
{
	/**
	 * Create component.
	 * @param args Creation arguments.
	 * @return Created component, or Null if component is unsupported in current state.
	 */
	Component create(Object... args);
	
	/**
	 * Get creation priority.
	 * @return Creation priority.
	 */
	ComponentCreationPriority getPriority();
	
	/**
	 * Check whether given component type is supported or not.
	 * @param componentType Component type to check.
	 * @return Component type support state.
	 */
	boolean isComponentTypeSupported(Class<?> componentType);
}
