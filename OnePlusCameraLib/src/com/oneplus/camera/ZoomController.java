package com.oneplus.camera;

import com.oneplus.base.Handle;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Camera zoom controller interface.
 */
public interface ZoomController extends Component
{
	/**
	 * Property to get or set digital zoom applied on primary camera.
	 */
	PropertyKey<Float> PROP_DIGITAL_ZOOM = new PropertyKey<>("DigitalZoom", Float.class, ZoomController.class, PropertyKey.FLAG_NOT_NULL, 1f);
	/**
	 * Read-only property to check whether digital zoom is supported by primary camera or not.
	 */
	PropertyKey<Boolean> PROP_IS_DIGITAL_ZOOM_SUPPORTED = new PropertyKey<>("IsDigitalZoomSupported", Boolean.class, ZoomController.class, false);
	/**
	 * Read-only property to check whether zoom (digital and optical) is locked or not.
	 */
	PropertyKey<Boolean> PROP_IS_ZOOM_LOCKED = new PropertyKey<>("IsZoomLocked", Boolean.class, ZoomController.class, false);
	/**
	 * Read-only property to get maximum digital zoom supported by primary camera.
	 */
	PropertyKey<Float> PROP_MAX_DIGITAL_ZOOM = new PropertyKey<>("MaxDigitalZoom", Float.class, ZoomController.class, 1f);
	
	
	/**
	 * Lock digital and optical zoom.
	 * @param flags Flags, reserved.
	 * @return Handle to zoom lock.
	 */
	Handle lockZoom(int flags);
}
