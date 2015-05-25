package com.oneplus.camera.location;

import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraThread;

/**
 * Component builder for {@link LocationManager}.
 */
public final class LocationManagerBuilder implements ComponentBuilder
{
	// Create component.
	@Override
	public Component create(Object... args)
	{
		if(args != null && args.length > 0)
		{
			if(args[0] instanceof CameraActivity)
				return new UILocationManagerImpl((CameraActivity)args[0]);
			if(args[0] instanceof CameraThread)
				return new LocationManagerImpl((CameraThread)args[0]);
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
		return LocationManager.class.isAssignableFrom(componentType);
	}
}
