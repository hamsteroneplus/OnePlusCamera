package com.oneplus.base.component;

import com.oneplus.base.EventArgs;

/**
 * Event data for {@link Component} related events.
 * @param <TComponent> Type of component.
 */
public class ComponentEventArgs<TComponent extends Component> extends EventArgs
{
	// Fields
	private final TComponent m_Component;
	
	
	/**
	 * Initialize new ComponentEventArgs instance.
	 * @param component Related component.
	 */
	public ComponentEventArgs(TComponent component)
	{
		m_Component = component;
	}
	
	
	/**
	 * Get related component.
	 * @return Component.
	 */
	public TComponent getComponent()
	{
		return m_Component;
	}
}
