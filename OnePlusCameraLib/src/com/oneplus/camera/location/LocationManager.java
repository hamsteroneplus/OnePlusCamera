package com.oneplus.camera.location;

import android.location.Location;

import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Location manager.
 */
public interface LocationManager extends Component
{
	/**
	 * Settings key to indicate whether location should be saved in media or not.
	 */
	String SETTINGS_KEY_SAVE_LOCATION = "Location.Save";
	
	
	/**
	 * Read-only property to check whether location listener is started or not.
	 */
	PropertyKey<Boolean> PROP_IS_LOCATION_LISTENER_STARTED = new PropertyKey<Boolean>("IsLocationListenerStarted", Boolean.class, LocationManager.class, false);
	/**
	 * Read-only property to get current location.
	 */
	PropertyKey<Location> PROP_LOCATION = new PropertyKey<>("Location", Location.class, LocationManager.class, PropertyKey.FLAG_READONLY, null);
}
