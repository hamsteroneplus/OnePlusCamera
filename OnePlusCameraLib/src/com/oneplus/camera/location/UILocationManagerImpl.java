package com.oneplus.camera.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.BaseActivity.State;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.OperationState;
import com.oneplus.camera.VideoCaptureState;

final class UILocationManagerImpl extends CameraComponent implements LocationManager
{
	// Constants.
	private static final long LAST_KNOWN_LOCATION_EXPIRATION_INTERVAL = (30 * 60 * 1000);	// 30 mins
	private static final int PROVIDER_INDEX_GPS = 0;
	private static final int PROVIDER_INDEX_NETWORK = 1;
	
	
	// Private fields.
	private final Location[] m_Locations = new Location[2];
	private final LocationListener[] m_LocationListeners = new LocationListener[2];
	private android.location.LocationManager m_SysLocationManager;
	
	
	// Constructor.
	UILocationManagerImpl(CameraActivity cameraActivity)
	{
		super("Location Manager", cameraActivity, false);
	}
	
	
	// Deinitialize.
	@Override
	protected void onDeinitialize()
	{
		// stop location listeners
		this.stopLocationListeners();
		
		// clear references
		m_SysLocationManager = null;
		
		// call super
		super.onDeinitialize();
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add property changed call-backs
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA_PREVIEW_STATE, new PropertyChangedCallback<OperationState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
			{
				if(e.getNewValue() == OperationState.STARTED)
					startLocationListeners();
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_STATE, new PropertyChangedCallback<CameraActivity.State>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<CameraActivity.State> key, PropertyChangeEventArgs<CameraActivity.State> e)
			{
				if(e.getOldValue() == State.RUNNING)
					stopLocationListeners();
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				switch(e.getNewValue())
				{
					case PREPARING:
					case READY:
						break;
					default:
						stopLocationListeners();
						break;
				}
			}
		});
	}
	
	
	// Called when location changes.
	private void onLocationChanged(int providerIndex, Location location)
	{
		Log.v(TAG, "onLocationChanged() - Provider index : ", providerIndex, ", location : ", location);
		
		// save location
		m_Locations[providerIndex] = location;
		
		// update location
		for(int i = providerIndex - 1 ; i >= 0 ; --i)
		{
			if(m_Locations[i] != null)
				return;
		}
		if(location == null)
		{
			for(int i = providerIndex + 1 ; i < m_Locations.length ; ++i)
			{
				location = m_Locations[i];
				if(location != null)
					break;
			}
		}
		Log.v(TAG, "onLocationChanged() - Use location : ", location);
		this.setReadOnly(PROP_LOCATION, location);
	}
	
	
	// Start location listener.
	private void startLocationListeners()
	{
		// check state
		if(this.get(PROP_IS_LOCATION_LISTENER_STARTED))
			return;
		if(!this.getSettings().getBoolean(SETTINGS_KEY_SAVE_LOCATION))
		{
			this.setReadOnly(PROP_LOCATION, null);
			return;
		}
		CameraActivity cameraActivity = this.getCameraActivity();
		switch(cameraActivity.get(CameraActivity.PROP_STATE))
		{
			case RESUMING:
			case RUNNING:
				break;
			default:
				return;
		}
		switch(cameraActivity.get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
		{
			case PREPARING:
			case READY:
				break;
			default:
				return;
		}
		
		// get location manager
		if(m_SysLocationManager == null)
			m_SysLocationManager = (android.location.LocationManager)cameraActivity.getSystemService(Context.LOCATION_SERVICE);
		
		// create location listeners
		for(int i = m_LocationListeners.length - 1 ; i >= 0 ; --i)
		{
			if(m_LocationListeners[i] == null)
			{
				final int index = i;
				m_LocationListeners[i] = new LocationListener()
				{
					@Override
					public void onStatusChanged(String provider, int status, Bundle extras)
					{}
					
					@Override
					public void onProviderEnabled(String provider)
					{}
					
					@Override
					public void onProviderDisabled(String provider)
					{}
					
					@Override
					public void onLocationChanged(Location location)
					{
						UILocationManagerImpl.this.onLocationChanged(index, location);
					}
				};
			}
		}
		
		// start network location listener
		boolean hasProviders = false;
		if(m_SysLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER))
		{
			Log.v(TAG, "startLocationListeners() - Start network location listener");
			hasProviders = true;
			m_SysLocationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 10000, 0, m_LocationListeners[PROVIDER_INDEX_NETWORK]);
		}
		
		// start GPS location listener
		if(m_SysLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
		{
			// start listener
			Log.v(TAG, "startLocationListeners() - Start GPS location listener");
			hasProviders = true;
			m_SysLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 30000, 0, m_LocationListeners[PROVIDER_INDEX_GPS]);
			
			// get last known location
			Location location = m_SysLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
			if(location != null && (System.currentTimeMillis() - location.getTime()) <= LAST_KNOWN_LOCATION_EXPIRATION_INTERVAL)
			{
				Log.v(TAG, "startLocationListeners() - Use last known GPS location : ", location);
				this.onLocationChanged(PROVIDER_INDEX_GPS, location);
			}
		}
		
		// check result
		if(!hasProviders)
			Log.w(TAG, "startLocationListeners() - No available location providers");
		
		// complete
		this.setReadOnly(PROP_IS_LOCATION_LISTENER_STARTED, true);
	}
	
	
	// stop location listeners.
	private void stopLocationListeners()
	{
		// check state
		if(m_SysLocationManager == null)
			return;
		if(!this.get(PROP_IS_LOCATION_LISTENER_STARTED))
			return;
		
		// stop location listeners
		Log.v(TAG, "stopLocationListeners()");
		for(int i = m_LocationListeners.length - 1 ; i >= 0 ; --i)
		{
			if(m_LocationListeners[i] != null)
				m_SysLocationManager.removeUpdates(m_LocationListeners[i]);
		}
		this.setReadOnly(PROP_IS_LOCATION_LISTENER_STARTED, false);
	}
}
