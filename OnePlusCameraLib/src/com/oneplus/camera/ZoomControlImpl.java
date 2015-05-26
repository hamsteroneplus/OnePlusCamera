package com.oneplus.camera;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.util.Size;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;

final class ZoomControlImpl extends CameraComponent implements ZoomController
{
	// Private fields.
	private volatile float m_DigitalZoom = 1;
	private volatile boolean m_IsDigitalZoomSupported;
	private volatile float m_MaxDigitalZoom = 1;
	private final List<Handle> m_ZoomLockHandles = new ArrayList<>();
	
	
	// Call-backs.
	private final PropertyChangedCallback<Rect> m_ScalerCropRegionChangedCallback = new PropertyChangedCallback<Rect>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Rect> key, PropertyChangeEventArgs<Rect> e)
		{
			onScalerCropRegionChanged((Camera)source, e.getNewValue());
		}
	};
	
	
	// Constructor.
	ZoomControlImpl(CameraThread cameraThread)
	{
		super("Zoom Controller", cameraThread, false);
	}
	
	
	// Attach to given camera.
	private void attachToCamera(Camera camera)
	{
		if(camera != null)
		{
			// get zoom ratios
			Size sensorSize = camera.get(Camera.PROP_SENSOR_SIZE);
			Size minCropSize = camera.get(Camera.PROP_MIN_SCALER_CROP_SIZE);
			float oldRatio = m_MaxDigitalZoom;
			boolean oldDigitalZoomSupportState = m_IsDigitalZoomSupported;
			m_MaxDigitalZoom = Math.min(sensorSize.getWidth() / (float)minCropSize.getWidth(), sensorSize.getHeight() / (float)minCropSize.getHeight());
			m_IsDigitalZoomSupported = (Math.abs(m_MaxDigitalZoom - 1) >= 0.001f);
			this.notifyPropertyChanged(PROP_IS_DIGITAL_ZOOM_SUPPORTED, oldDigitalZoomSupportState, m_IsDigitalZoomSupported);
			this.notifyPropertyChanged(PROP_MAX_DIGITAL_ZOOM, oldRatio, m_MaxDigitalZoom);
			
			// reset zoom
			camera.set(Camera.PROP_SCALER_CROP_REGION, null);
			float oldZoom = m_DigitalZoom;
			m_DigitalZoom = 1;
			this.notifyPropertyChanged(PROP_DIGITAL_ZOOM, oldZoom, 1f);
			
			// add call-backs
			camera.addCallback(Camera.PROP_SCALER_CROP_REGION, m_ScalerCropRegionChangedCallback);
		}
	}
	
	
	// Detach from given camera.
	private void detachFromCamera(Camera camera)
	{
		if(camera != null)
			camera.removeCallback(Camera.PROP_SCALER_CROP_REGION, m_ScalerCropRegionChangedCallback);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_DIGITAL_ZOOM)
			return (TValue)(Float)m_DigitalZoom;
		if(key == PROP_IS_DIGITAL_ZOOM_SUPPORTED)
			return (TValue)(Boolean)m_IsDigitalZoomSupported;
		if(key == PROP_MAX_DIGITAL_ZOOM)
			return (TValue)(Float)m_MaxDigitalZoom;
		return super.get(key);
	}
	
	
	// Lock zoom.
	@Override
	public Handle lockZoom(int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "lockZoom() - Component is not running");
			return null;
		}
		
		// lock zoom
		Handle handle = new Handle("ZoomLock")
		{
			@Override
			protected void onClose(int flags)
			{
				unlockZoom(this);
			}
		};
		m_ZoomLockHandles.add(handle);
		if(m_ZoomLockHandles.size() == 1)
		{
			this.setReadOnly(PROP_IS_ZOOM_LOCKED, true);
			if(this.getCamera() != null)
				this.setDigitalZoomProp(1f, true);
		}
		
		// complete
		return handle;
	}
	
	
	// Deinitialize.
	@Override
	protected void onDeinitialize()
	{
		this.detachFromCamera(this.getCamera());
		super.onDeinitialize();
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add call-backs.
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
		
		// attach to current camera
		this.attachToCamera(this.getCamera());
	}
	
	
	// Called when crop region changes.
	private void onScalerCropRegionChanged(Camera camera, Rect region)
	{
		float oldZoom = m_DigitalZoom;
		if(region == null)
			m_DigitalZoom = 1;
		else
		{
			Size sensorSize = camera.get(Camera.PROP_SENSOR_SIZE);
			m_DigitalZoom = Math.min(sensorSize.getWidth() / (float)region.width(), sensorSize.getHeight() / (float)region.height());
		}
		this.notifyPropertyChanged(PROP_DIGITAL_ZOOM, oldZoom, m_DigitalZoom);
	}
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_DIGITAL_ZOOM)
			return this.setDigitalZoomProp((Float)value, false);
		return super.set(key, value);
	}
	
	
	// Set PROP_DIGITAL_ZOOM property.
	private boolean setDigitalZoomProp(float zoom, boolean forceSet)
	{
		// check state
		this.verifyAccess();
		if(!m_IsDigitalZoomSupported)
		{
			Log.w(TAG, "setDigitalZoomProp() - Digital zoom is unsupported");
			return false;
		}
		
		// check camera
		Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "setDigitalZoomProp() - No primary camera");
			return false;
		}
		
		// check ratio
		if(zoom < 1 || Math.abs(zoom - 1) < 0.01f)
			zoom = 1;
		else
			zoom = Math.min(zoom, this.get(PROP_MAX_DIGITAL_ZOOM));
		if(Math.abs(m_DigitalZoom - zoom) < 0.01f)
			return false;
		
		// change zoom
		if(Math.abs(zoom - 1) < 0.01f)
		{
			Log.v(TAG, "setDigitalZoomProp() - Zoom : 1");
			camera.set(Camera.PROP_SCALER_CROP_REGION, null);
		}
		else
		{
			if(this.get(PROP_IS_ZOOM_LOCKED) && !forceSet)
			{
				Log.w(TAG, "setDigitalZoomProp() - Zoom is locked");
				return false;
			}
			Size sensorSize = camera.get(Camera.PROP_SENSOR_SIZE);
			Rect cropRegion = new Rect(0, 0, (int)(sensorSize.getWidth() / zoom), (int)(sensorSize.getHeight() / zoom));
			cropRegion.offset(((sensorSize.getWidth() - cropRegion.right) / 2), ((sensorSize.getHeight() - cropRegion.bottom) / 2));
			Log.v(TAG, "setDigitalZoomProp() - Zoom : ", zoom, ", crop region : ", cropRegion);
			camera.set(Camera.PROP_SCALER_CROP_REGION, cropRegion);
		}
		
		// complete
		return true;
	}
	
	
	// Unlock zoom.
	private void unlockZoom(Handle handle)
	{
		this.verifyAccess();
		if(m_ZoomLockHandles.remove(handle) && m_ZoomLockHandles.isEmpty())
			this.setReadOnly(PROP_IS_ZOOM_LOCKED, false);
	}
}
