package com.oneplus.camera;

import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;

final class FocusControllerBuilder implements ComponentBuilder
{
	// Create component.
	@Override
	public Component create(Object... args)
	{
		if(args != null && args.length > 0)
		{
			if(args[0] instanceof CameraActivity)
				return new UIFocusControllerImpl((CameraActivity)args[0]);
			if(args[0] instanceof CameraThread)
				return new FocusControllerImpl((CameraThread)args[0]);
		}
		return null;
	}

	
	// Get priority.
	@Override
	public ComponentCreationPriority getPriority()
	{
		return ComponentCreationPriority.NORMAL;
	}

	
	// Check whether given type is supported or not.
	@Override
	public boolean isComponentTypeSupported(Class<?> componentType)
	{
		return FocusController.class.isAssignableFrom(componentType);
	}
}
