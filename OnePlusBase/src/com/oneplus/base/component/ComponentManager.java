package com.oneplus.base.component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.oneplus.base.BaseObject;
import com.oneplus.base.EventKey;
import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.Log;

/**
 * Component manager to host components and builders.
 */
public class ComponentManager extends HandlerBaseObject
{
	/**
	 * Event raised after new component added.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final EventKey<ComponentEventArgs<Component>> EVENT_COMPONENT_ADDED = new EventKey<>("ComponentAdded", (Class)ComponentEventArgs.class, ComponentManager.class);
	/**
	 * Event raised after removing component.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final EventKey<ComponentEventArgs<Component>> EVENT_COMPONENT_REMOVED = new EventKey<>("ComponentRemoved", (Class)ComponentEventArgs.class, ComponentManager.class);
	
	
	// Private fields
	private final List<Component> m_Components = new ArrayList<>();
	private final HashSet<ComponentCreationPriority> m_CreatePriorities = new HashSet<>();
	private final List<ComponentBuilder> m_Builders = new ArrayList<>();
	
	
	/**
	 * Initialize new ComponentManager instance.
	 */
	public ComponentManager()
	{
		super(true);
	}
	
	
	/**
	 * Add component builder.
	 * @param builder Component builder to add.
	 * @param args Component creation arguments.
	 */
	public synchronized void addComponentBuilder(ComponentBuilder builder, Object... args)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		
		// create component
		if(m_CreatePriorities.contains(builder.getPriority()))
			this.createComponent(builder, true, args);
		else
			m_Builders.add(builder);
	}
	
	
	/**
	 * Add component builders.
	 * @param builders Component builders to add.
	 * @param args Component creation arguments.
	 */
	public void addComponentBuilders(ComponentBuilder[] builders, Object... args)
	{
		for(int i = builders.length - 1 ; i >= 0 ; --i)
			this.addComponentBuilder(builders[i], args);
	}
	
	
	// Use given builder to create component.
	private Component createComponent(ComponentBuilder builder, boolean needInit, Object... args)
	{
		Component component = null;
		try
		{
			// create component
			component = builder.create(args);
			if(component == null)
			{
				Log.w(TAG, "createComponent() - Component is unsupported, builder : " + builder);
				return null;
			}
			
			Log.d(TAG, "createComponent() - Component : " + component);
			
			// initialize
			if(needInit && !component.initialize())
			{
				Log.w(TAG, "createComponent() - Release " + component);
				component.release();
				return null;
			}
			
			// complete
			m_Components.add(component);
			return component;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "createComponent() - Fail to create component by builder " + builder, ex);
			if(component != null)
			{
				Log.w(TAG, "createComponent() - Release " + component);
				component.release();
			}
			return null;
		}
	}
	
	
	/**
	 * Create components with specific priority.
	 * @param priority Creation priority.
	 * @param args Component creation arguments.
	 */
	public final synchronized void createComponents(ComponentCreationPriority priority, Object... args)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		
		// check category
		if(m_CreatePriorities.contains(priority))
			return;
		
		Log.w(TAG, "createComponents(" + priority + ") - Start");
		
		// create components higher with higher priority
		switch(priority)
		{
			case LAUNCH:
				break;
			case HIGH:
				this.createComponents(ComponentCreationPriority.LAUNCH, args);
				break;
			case NORMAL:
				this.createComponents(ComponentCreationPriority.HIGH, args);
				break;
			case LOW:
				this.createComponents(ComponentCreationPriority.NORMAL, args);
				break;
			case ON_DEMAND:
				throw new IllegalArgumentException("Cannot create on-demand components.");
		}
		
		// create components
		m_CreatePriorities.add(priority);
		ArrayList<Component> newComponents = new ArrayList<>();
		for(int i = m_Builders.size() - 1 ; i >= 0 ; --i)
		{
			ComponentBuilder builder = m_Builders.get(i);
			if(builder.getPriority() == priority)
			{
				Component component = this.createComponent(builder, false, args);
				if(component != null)
				{
					newComponents.add(component);
					m_Builders.remove(i);
				}
			}
		}
		
		// initialize components
		for(int i = newComponents.size() - 1 ; i >= 0 ; --i)
		{
			Component component = newComponents.get(i);
			if(!this.initializeComponent(component))
			{
				m_Components.remove(component);
				Log.w(TAG, "createComponents() - Release " + component);
				component.release();
			}
		}
		
		Log.w(TAG, "createComponents(" + priority + ") - End");
	}
	
	
	/**
	 * Find component extends or implements given type.
	 * @param componentType Type extended or implemented.
	 * @param args Component creation arguments for on-demand component.
	 * @return Found component, or Null if no component extends or implements given type.
	 */
	@SuppressWarnings("unchecked")
	public final synchronized <TComponent extends Component> TComponent findComponent(Class<TComponent> componentType, Object... args)
	{
		// search created components
		for(int i = m_Components.size() - 1 ; i >= 0 ; --i)
		{
			Component component = m_Components.get(i);
			if(componentType.isAssignableFrom(component.getClass()) && this.initializeComponent(component))
				return (TComponent)component;
		}
		
		// check thread
		if(!this.isDependencyThread())
			return null;
		
		// create on-demand component
		for(int i = m_Builders.size() - 1 ; i >= 0 ; --i)
		{
			ComponentBuilder builder = m_Builders.get(i);
			if(builder.getPriority() == ComponentCreationPriority.ON_DEMAND && builder.isComponentTypeSupported(componentType))
			{
				Component component = this.createComponent(builder, true, args);
				if(component != null)
				{
					m_Builders.remove(i);
					return (TComponent)component;
				}
			}
		}
		
		// fail
		return null;
	}
	
	
	/**
	 * Find all components extend or implement given type.
	 * @param componentType Type extended or implemented.
	 * @param args Component creation arguments for on-demand component.
	 * @return All matched components, or Null if no component extend or implement given type.
	 */
	@SuppressWarnings("unchecked")
	public final synchronized <TComponent extends Component> TComponent[] findComponents(Class<TComponent> componentType, Object... args)
	{
		// search created components
		List<Component> foundComponents = null;
		for(int i = m_Components.size() - 1 ; i >= 0 ; --i)
		{
			Component component = m_Components.get(i);
			if(componentType.isAssignableFrom(component.getClass()) && this.initializeComponent(component))
			{
				if(foundComponents == null)
					foundComponents = new ArrayList<>();
				foundComponents.add((TComponent)component);
			}
		}
		
		// create on-demand component
		if(this.isDependencyThread())
		{
			for(int i = m_Builders.size() - 1 ; i >= 0 ; --i)
			{
				ComponentBuilder builder = m_Builders.get(i);
				if(builder.getPriority() == ComponentCreationPriority.ON_DEMAND && builder.isComponentTypeSupported(componentType))
				{
					Component component = this.createComponent(builder, true, args);
					if(component != null)
					{
						m_Builders.remove(i);
						if(foundComponents == null)
							foundComponents = new ArrayList<>();
						foundComponents.add((TComponent)component);
					}
				}
			}
		}
		
		// complete
		if(foundComponents != null)
		{
			TComponent[] array = (TComponent[])new Component[foundComponents.size()];
			foundComponents.toArray(array);
			return array;
		}
		return (TComponent[])new Component[0];
	}
	
	
	// Initialize given component.
	private boolean initializeComponent(Component component)
	{
		// check state
		switch(component.get(Component.PROP_STATE))
		{
			case NEW:
				break;
			case INITIALIZING:
			case RUNNING:
				return true;
			default:
				return false;
		}
		
		Log.d(TAG, "initializeComponent() - Component : " + component);
		
		// initialize
		boolean result;
		try
		{
			result = component.initialize();
			if(!result)
				Log.e(TAG, "initializeComponent() - Fail to initialize " + component);
			this.raise(EVENT_COMPONENT_ADDED, new ComponentEventArgs<Component>(component));
		}
		catch(Throwable ex)
		{
			result = false;
			Log.e(TAG, "initializeComponent() - Fail to initialize " + component, ex);
		}
		return result;
	}
	
	
	/**
	 * Release and remove given component.
	 * @param component Component to remove.
	 */
	public synchronized final void removeComponent(Component component)
	{
		this.verifyAccess();
		this.removeComponentInternal(component);
	}
	
	
	// Called when releasing.
	@Override
	protected synchronized void onRelease()
	{
		// release all builders
		m_Builders.clear();
		
		// release all components
		Component[] components = new Component[m_Components.size()];
		m_Components.toArray(components);
		for(int i = components.length - 1 ; i >= 0 ; --i)
			this.removeComponentInternal(components[i]);
		
		// call super
		super.onRelease();
	}
	
	
	// Remove given component.
	private void removeComponentInternal(Component component)
	{
		// check state
		if(component.get(BaseObject.PROP_IS_RELEASED))
			return;
		
		// remove from table
		if(!m_Components.remove(component))
			return;
		
		Log.w(TAG, "removeComponentInternal() - Component : " + component);
		
		// raise event
		this.raise(EVENT_COMPONENT_REMOVED, new ComponentEventArgs<Component>(component));
		
		// release component
		component.release();
	}
}
