package com.oneplus.base.component;

/**
 * Call-back interface for component search.
 * @param <TComponent> Type of component.
 */
public interface ComponentSearchCallback<TComponent extends Component>
{
	/**
	 * Called when component found.
	 * @param component Found component.
	 */
	void onComponentFound(TComponent component);
}
