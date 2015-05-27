package com.oneplus.camera;

import java.util.List;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;
import com.oneplus.camera.Camera.MeteringRect;

final class UIExposureControllerImpl extends ProxyComponent<ExposureController> implements ExposureController
{
	// Constructor.
	UIExposureControllerImpl(CameraActivity cameraActivity)
	{
		super("UI Exposure Controller", cameraActivity, cameraActivity.getCameraThread(), ExposureController.class);
	}
	
	
	// Called before binding to controller.
	@Override
	protected void onBindingToTargetProperties(List<PropertyKey<?>> keys)
	{
		super.onBindingToTargetProperties(keys);
		keys.add(PROP_AE_REGIONS);
		keys.add(PROP_EXPOSURE_COMPENSATION);
		keys.add(PROP_EXPOSURE_COMPENSATION_RANGE);
		keys.add(PROP_EXPOSURE_COMPENSATION_STEP);
		keys.add(PROP_IS_AE_LOCKED);
	}
	
	
	// Called when AE lock state changes.
	private void onControllerAELockedChanged(boolean isLocked)
	{
		//super.set(PROP_IS_AE_LOCKED, isLocked);
	}
	
	
	// Called when controller bounds.
	@Override
	protected void onTargetBound(ExposureController target)
	{
		// call super
		super.onTargetBound(target);
		
		// sync states to controller
		//
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
		if(!this.isTargetBound())
			return super.set(PROP_AE_REGIONS, regions);
		
		// set AE regions asynchronously
		if(!HandlerUtils.post(this.getTargetOwner(), new Runnable()
		{
			@Override
			public void run()
			{
				getTarget().set(PROP_AE_REGIONS, regions);
			}
		}))
		{
			Log.e(TAG, "setAERegionsProp() - Fail to perform cross-thread operation");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Set PROP_EXPOSURE_COMPENSATION property
	private boolean setExposureCompensationProp(final float ev)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setExposureCompensationProp() - Component is not running");
			return false;
		}
		if(!this.isTargetBound())
			return super.set(PROP_EXPOSURE_COMPENSATION, ev);
		
		// set EV asynchronously
		if(!HandlerUtils.post(this.getTargetOwner(), new Runnable()
		{
			@Override
			public void run()
			{
				getTarget().set(PROP_EXPOSURE_COMPENSATION, ev);
			}
		}))
		{
			Log.e(TAG, "setExposureCompensationProp() - Fail to perform cross-thread operation");
			return false;
		}
		
		// complete
		return true;
	}
}
