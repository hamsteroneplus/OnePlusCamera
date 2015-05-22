package com.oneplus.camera;

import java.util.List;

import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera.MeteringRect;

final class ExposureControllerImpl extends CameraComponent implements ExposureController
{
	// Private fields.
	//
	
	
	// Call-backs.
	private final PropertyChangedCallback<List<MeteringRect>> m_AERegionsChangedCallback = new PropertyChangedCallback<List<MeteringRect>>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<List<MeteringRect>> key, PropertyChangeEventArgs<List<MeteringRect>> e)
		{
			onAERegionsChanged(e.getNewValue());
		}
	};
	private final PropertyChangedCallback<Boolean> m_IsAELockedChangedCallback = new PropertyChangedCallback<Boolean>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
		{
			onAELockedChanged(e.getNewValue());
		}
	};
	
	
	// Constructor.
	ExposureControllerImpl(CameraThread cameraThread)
	{
		super("Exposure Controller", cameraThread, true);
	}
	
	
	// Called when AE lock state changes.
	private void onAELockedChanged(boolean isLocked)
	{
		super.setReadOnly(PROP_IS_AE_LOCKED, isLocked);
	}
	
	
	// Called when AE regions changes.
	private void onAERegionsChanged(List<MeteringRect> regions)
	{
		super.set(PROP_AE_REGIONS, regions);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add property changed call-backs
		CameraThread cameraThread = this.getCameraThread();
		cameraThread.addCallback(CameraThread.PROP_CAMERA, new PropertyChangedCallback<Camera>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Camera> key, PropertyChangeEventArgs<Camera> e)
			{
				Camera camera = e.getOldValue();
				if(camera != null)
				{
					camera.removeCallback(Camera.PROP_AE_REGIONS, m_AERegionsChangedCallback);
					camera.removeCallback(Camera.PROP_IS_AE_LOCKED, m_IsAELockedChangedCallback);
				}
				camera = e.getNewValue();
				if(camera != null && isRunningOrInitializing())
				{
					camera.addCallback(Camera.PROP_AE_REGIONS, m_AERegionsChangedCallback);
					camera.addCallback(Camera.PROP_IS_AE_LOCKED, m_IsAELockedChangedCallback);
				}
			}
		});
		
		// attach to camera
		Camera camera = this.getCamera();
		if(camera != null)
		{
			camera.addCallback(Camera.PROP_AE_REGIONS, m_AERegionsChangedCallback);
			camera.addCallback(Camera.PROP_IS_AE_LOCKED, m_IsAELockedChangedCallback);
		}
	}
	
	
	// Release component.
	@Override
	protected void onRelease()
	{
		// detach from camera
		Camera camera = this.getCamera();
		if(camera != null)
		{
			camera.removeCallback(Camera.PROP_AE_REGIONS, m_AERegionsChangedCallback);
			camera.removeCallback(Camera.PROP_IS_AE_LOCKED, m_IsAELockedChangedCallback);
		}
		
		// call super
		super.onRelease();
	}
	
	
	// Set property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_AE_REGIONS)
			return this.setAERegionsProp((List<MeteringRect>)value);
		return super.set(key, value);
	}
	
	
	// Set PROP_IS_AE_LOCKED property.
	private boolean setAELockedProp(boolean isLocked)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setAELockedProp() - Component is not running");
			return false;
		}
		
		// check camera
		Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "setAELockedProp() - No primary camera");
			return false;
		}
		
		// set AE lock
		return camera.set(Camera.PROP_IS_AE_LOCKED, isLocked);
	}
	
	
	// Set PROP_AE_REGIONS property.
	private boolean setAERegionsProp(List<MeteringRect> regions)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setAERegionsProp() - Component is not running");
			return false;
		}
		
		// check camera
		Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "setAERegionsProp() - No primary camera");
			return false;
		}
		
		// set AE regions
		return camera.set(Camera.PROP_AE_REGIONS, regions);
	}
}
