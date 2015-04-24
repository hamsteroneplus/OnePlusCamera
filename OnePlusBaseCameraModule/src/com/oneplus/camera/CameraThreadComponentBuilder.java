package com.oneplus.camera;

import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;

/**
 * Base class for component builder to build {@link CameraThreadComponent}.
 */
public abstract class CameraThreadComponentBuilder implements ComponentBuilder
{
	// Private fields
	private final Class<? extends CameraThreadComponent> m_ComponentType;
	private final ComponentCreationPriority m_Priority;
	
	
	/**
	 * Initialize new CameraThreadComponentBuilder instance with {@link ComponentCreationPriority#NORMAL NORMAL} priority.
	 * @param componentType Type of component.
	 */
	protected CameraThreadComponentBuilder(Class<? extends CameraThreadComponent> componentType)
	{
		this(ComponentCreationPriority.NORMAL, componentType);
	}
	
	
	/**
	 * Initialize new CameraThreadComponentBuilder instance.
	 * @param priority Component category.
	 * @param componentType Type of component.
	 */
	protected CameraThreadComponentBuilder(ComponentCreationPriority priority, Class<? extends CameraThreadComponent> componentType)
	{
		if(priority == null)
			throw new IllegalArgumentException("No creation priority.");
		if(componentType == null)
			throw new IllegalArgumentException("No component type.");
		m_Priority = priority;
		m_ComponentType = componentType;
	}
	
	
	// Create component.
	@Override
	public Component create(Object... args)
	{
		if(args == null || args.length == 0)
			return null;
		if(!(args[0] instanceof CameraThread))
			return null;
		return this.create((CameraThread)args[0]);
	}
	
	
	/**
	 * Create component.
	 * @param cameraThread Camera thread.
	 * @return Created component, or Null if component is unsupported in current state.
	 */
	protected abstract CameraThreadComponent create(CameraThread cameraThread);
	
	
	// Get priority.
	public final ComponentCreationPriority getPriority()
	{
		return m_Priority;
	}

	
	// Check whether given component type is supported or not.
	@Override
	public boolean isComponentTypeSupported(Class<?> componentType)
	{
		return componentType.isAssignableFrom(m_ComponentType);
	}
}
