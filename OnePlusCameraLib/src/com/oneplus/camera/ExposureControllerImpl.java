package com.oneplus.camera;

import java.util.List;

import android.util.Range;

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
	@SuppressWarnings("rawtypes")
	private final PropertyChangedCallback m_CameraPropertyChangedCallback = new PropertyChangedCallback()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
		{
			onCameraPropertyChanged(key, e.getNewValue());
		}
	};
	
	
	// Constructor.
	ExposureControllerImpl(CameraThread cameraThread)
	{
		super("Exposure Controller", cameraThread, true);
	}
	
	
	// Attach to given camera
	@SuppressWarnings("unchecked")
	private void attachToCamera(Camera camera)
	{
		// check camera
		if(camera == null)
			return;
		
		// add call-backs
		camera.addCallback(Camera.PROP_AE_REGIONS, m_CameraPropertyChangedCallback);
		camera.addCallback(Camera.PROP_EXPOSURE_COMPENSATION, m_CameraPropertyChangedCallback);
		camera.addCallback(Camera.PROP_EXPOSURE_COMPENSATION_RANGE, m_CameraPropertyChangedCallback);
		camera.addCallback(Camera.PROP_EXPOSURE_COMPENSATION_STEP, m_CameraPropertyChangedCallback);
		camera.addCallback(Camera.PROP_IS_AE_LOCKED, m_CameraPropertyChangedCallback);
		
		// get current state
		this.onCameraPropertyChanged(Camera.PROP_AE_REGIONS, camera.get(Camera.PROP_AE_REGIONS));
		this.onCameraPropertyChanged(Camera.PROP_EXPOSURE_COMPENSATION, camera.get(Camera.PROP_EXPOSURE_COMPENSATION));
		this.onCameraPropertyChanged(Camera.PROP_EXPOSURE_COMPENSATION_RANGE, camera.get(Camera.PROP_EXPOSURE_COMPENSATION_RANGE));
		this.onCameraPropertyChanged(Camera.PROP_EXPOSURE_COMPENSATION_STEP, camera.get(Camera.PROP_EXPOSURE_COMPENSATION_STEP));
		this.onCameraPropertyChanged(Camera.PROP_IS_AE_LOCKED, camera.get(Camera.PROP_IS_AE_LOCKED));
	}
	
	
	// Detach from given camera
	@SuppressWarnings("unchecked")
	private void detachFromCamera(Camera camera)
	{
		// check camera
		if(camera == null)
			return;
		
		// remove call-backs
		camera.removeCallback(Camera.PROP_AE_REGIONS, m_CameraPropertyChangedCallback);
		camera.removeCallback(Camera.PROP_EXPOSURE_COMPENSATION, m_CameraPropertyChangedCallback);
		camera.removeCallback(Camera.PROP_EXPOSURE_COMPENSATION_RANGE, m_CameraPropertyChangedCallback);
		camera.removeCallback(Camera.PROP_EXPOSURE_COMPENSATION_STEP, m_CameraPropertyChangedCallback);
		camera.removeCallback(Camera.PROP_IS_AE_LOCKED, m_CameraPropertyChangedCallback);
	}
	
	
	// Called when AE lock state changes.
	private void onAELockedChanged(boolean isLocked)
	{
		super.setReadOnly(PROP_IS_AE_LOCKED, isLocked);
	}
	
	
	// Called when camera property changes.
	@SuppressWarnings("unchecked")
	private void onCameraPropertyChanged(PropertyKey<?> key, Object newValue)
	{
		if(key == Camera.PROP_AE_REGIONS)
			super.set(PROP_AE_REGIONS, (List<MeteringRect>)newValue);
		else if(key == Camera.PROP_EXPOSURE_COMPENSATION)
			super.set(PROP_EXPOSURE_COMPENSATION, (Float)newValue);
		else if(key == Camera.PROP_EXPOSURE_COMPENSATION_RANGE)
			super.setReadOnly(PROP_EXPOSURE_COMPENSATION_RANGE, (Range<Float>)newValue);
		else if(key == Camera.PROP_EXPOSURE_COMPENSATION_STEP)
			super.setReadOnly(PROP_EXPOSURE_COMPENSATION_STEP, (Float)newValue);
		else if(key == Camera.PROP_IS_AE_LOCKED)
			this.onAELockedChanged((Boolean)newValue);
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
				detachFromCamera(e.getOldValue());
				attachToCamera(e.getNewValue());
			}
		});
		
		// attach to camera
		this.attachToCamera(this.getCamera());
	}
	
	
	// Release component.
	@Override
	protected void onRelease()
	{
		// detach from camera
		this.detachFromCamera(this.getCamera());
		
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
		if(key == PROP_EXPOSURE_COMPENSATION)
			return this.setExposureCompensationProp((Float)value);
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
	
	
	// Set PROP_EXPOSURE_COMPENSATION property
	private boolean setExposureCompensationProp(float ev)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setExposureCompensationProp() - Component is not running");
			return false;
		}
		
		// check camera
		Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "setExposureCompensationProp() - No primary camera");
			return false;
		}
		
		// set AE regions
		return camera.set(Camera.PROP_EXPOSURE_COMPENSATION, ev);
	}
}
