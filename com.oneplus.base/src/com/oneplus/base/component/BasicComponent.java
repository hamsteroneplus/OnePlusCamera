package com.oneplus.base.component;

import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;

/**
 * Basic implementation of {@link Component}.
 */
public abstract class BasicComponent extends HandlerBaseObject implements Component
{
	// Private fields
	private final String m_Name;
	private final ComponentOwner m_Owner;
	private volatile ComponentState m_State = ComponentState.NEW;
	
	
	/**
	 * Initialize new BasicComponent instance.
	 * @param name Component name.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected BasicComponent(String name, ComponentOwner owner, boolean hasHandler)
	{
		super(hasHandler);
		if(name == null)
			throw new IllegalArgumentException("No component name.");
		if(owner == null)
			throw new IllegalArgumentException("No component owner.");
		m_Name = name;
		m_Owner = owner;
	}
	
	
	// Change component state.
	private ComponentState changeState(ComponentState state)
	{
		ComponentState oldState = m_State;
		if(oldState != state)
		{
			m_State = state;
			this.notifyPropertyChanged(PROP_STATE, oldState, state);
		}
		return m_State;
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_OWNER)
			return (TValue)m_Owner;
		if(key == PROP_STATE)
			return (TValue)m_State;
		return super.get(key);
	}
	
	
	// Initialize component.
	@Override
	public boolean initialize()
	{
		// check state
		this.verifyAccess();
		switch(m_State)
		{
			case NEW:
				break;
			case INITIALIZING:
			case RUNNING:
				return true;
			default:
				Log.e(TAG, "initialize() - Current state is " + m_State);
				return false;
		}
		
		// change state
		if(this.changeState(ComponentState.INITIALIZING) != ComponentState.INITIALIZING)
			return false;
		
		// initialize
		this.onInitialize();
		
		// check state
		if(m_State != ComponentState.INITIALIZING)
		{
			Log.e(TAG, "initialize() - State has been changed to " + m_State + " while initializing");
			return false;
		}
		
		// complete
		return (this.changeState(ComponentState.RUNNING) == ComponentState.RUNNING);
	}
	
	
	/**
	 * Called when deinitializing component.
	 */
	protected void onDeinitialize()
	{}
	
	
	/**
	 * Called when initializing component.
	 */
	protected void onInitialize()
	{}
	
	
	// Called when releasing component.
	@Override
	protected void onRelease()
	{
		// deinitialize
		switch(m_State)
		{
			case INITIALIZING:
			case RUNNING:
				this.changeState(ComponentState.RELEASING);
				this.onDeinitialize();
				break;
			default:
				break;
		}
		
		// change state
		this.changeState(ComponentState.RELEASED);
		
		// call super
		super.onRelease();
	}
	
	
	// Get string represents this component.
	@Override
	public String toString()
	{
		return m_Name;
	}
}
