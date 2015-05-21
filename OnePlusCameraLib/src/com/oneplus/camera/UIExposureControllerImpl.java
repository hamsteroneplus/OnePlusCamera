package com.oneplus.camera;

import java.util.List;

import android.os.Message;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.Camera.MeteringRect;

final class UIExposureControllerImpl extends CameraComponent implements ExposureController
{
	// Constants.
	private static final int MSG_CONTROLLER_PROPERTY_CHANGED = 10000;
	
	
	// Private fields.
	private ExposureController m_Controller;
	
	
	// Constructor.
	UIExposureControllerImpl(CameraActivity cameraActivity)
	{
		super("UI Exposure Controller", cameraActivity, true);
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_CONTROLLER_PROPERTY_CHANGED:
			{
				Object[] array = (Object[])msg.obj;
				this.onControllerPropertyChanged((Camera)array[0], (PropertyKey<?>)array[1], array[2]);
				break;
			}
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when AE lock state changes.
	private void onControllerAELockedChanged(boolean isLocked)
	{
		//super.set(PROP_IS_AE_LOCKED, isLocked);
	}
	
	
	// Called when actual ExposureController found.
	private void onControllerFound(final ExposureController controller)
	{
		// check state
		if(!this.isRunningOrInitializing())
			return;
		
		// sync current values
		this.setAERegionsProp(this.get(PROP_AE_REGIONS));
		
		// add call-backs
		if(!HandlerUtils.post(controller, new Runnable()
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void run()
			{
				// add call-backs
				Log.v(TAG, "onControllerFound() - Add call-backs");
				PropertyChangedCallback callback = new PropertyChangedCallback()
				{
					@Override
					public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
					{
						Camera camera = getCameraThread().get(CameraThread.PROP_CAMERA);
						HandlerUtils.sendMessage(UIExposureControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, key, e.getNewValue() });
					}
				};
				controller.addCallback(PROP_AE_REGIONS, callback);
				controller.addCallback(PROP_IS_AE_LOCKED, callback);
				
				// sync current values
				Camera camera = getCameraThread().get(CameraThread.PROP_CAMERA);
				HandlerUtils.sendMessage(UIExposureControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_AE_REGIONS, controller.get(PROP_AE_REGIONS) });
				HandlerUtils.sendMessage(UIExposureControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_IS_AE_LOCKED, controller.get(PROP_IS_AE_LOCKED) });
			}
		}))
		{
			Log.e(TAG, "onFocusControllerFound() - Fail to perform cross-thread operation");
			return;
		}
		
		// save instance
		m_Controller = controller;
	}
	
	
	// Called when AE regions changes.
	private void onControllerAERegionsChanged(List<MeteringRect> regions)
	{
		super.set(PROP_AE_REGIONS, regions);
	}
	
	
	// Called when property in FocusController changed.
	@SuppressWarnings("unchecked")
	private void onControllerPropertyChanged(Camera camera, PropertyKey<?> key, Object newValue)
	{
		if(this.getCamera() != camera)
			return;
		if(key == PROP_AE_REGIONS)
			this.onControllerAERegionsChanged((List<MeteringRect>)newValue);
		else if(key == PROP_IS_AE_LOCKED)
			this.onControllerAELockedChanged((Boolean)newValue);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// bind to actual FocusController
		ComponentUtils.findComponent(this.getCameraThread(), ExposureController.class, this, new ComponentSearchCallback<ExposureController>()
		{
			@Override
			public void onComponentFound(ExposureController component)
			{
				onControllerFound(component);
			}
		});
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
	
	
	// Set PROP_AE_REGIONS property.
	private boolean setAERegionsProp(final List<MeteringRect> regions)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setAERegionsProp() - Component is not running");
			return false;
		}
		if(m_Controller == null)
			return super.set(PROP_AE_REGIONS, regions);
		
		// set AE regions asynchronously
		if(!HandlerUtils.post(m_Controller, new Runnable()
		{
			@Override
			public void run()
			{
				m_Controller.set(PROP_AE_REGIONS, regions);
			}
		}))
		{
			Log.e(TAG, "setAERegionsProp() - Fail to perform cross-thread operation");
			return false;
		}
		
		// complete
		return true;
	}
}
