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
	
	
	/**
	 * Find component extends or implements given type, which is also owned by same component owner.
	 * @param componentType Component type.
	 * @return Found component, or Null if no component extends or implements given type.
	 */
	protected final <T extends Component> T findComponent(Class<T> componentType)
	{
		return m_Owner.findComponent(componentType);
	}
	
	
	/**
	 * Find component which is owned by same component owner, and call-back when component found.
	 * @param componentType Type extended or implemented.
	 * @param callback Call-back.
	 * @return Whether component is found immediately (synchronously) or not.
	 */
	protected final <T extends Component> boolean findComponent(Class<T> componentType, ComponentSearchCallback<T> callback)
	{
		return ComponentUtils.findComponent(m_Owner, componentType, m_Owner, callback);
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
	 * Check whether component state is {@link ComponentState#RUNNING RUNNING} or {@link ComponentState#INITIALIZING INITIALIZING}.
	 * @return Whether component is running or initializing.
	 */
	public final boolean isRunningOrInitializing()
	{
		return (m_State == ComponentState.RUNNING || m_State == ComponentState.INITIALIZING);
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
