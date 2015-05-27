package com.oneplus.camera;

import java.util.LinkedList;
import java.util.List;

import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.BaseActivity.State;
import com.oneplus.camera.Camera.MeteringRect;
import com.oneplus.camera.capturemode.CaptureModeManager;

final class UIExposureControllerImpl extends ProxyComponent<ExposureController> implements ExposureController
{
	// Private fields.
	private final LinkedList<AELockHandle> m_AELockHandles = new LinkedList<>();
	
	
	// Class for focus lock handle.
	private final class AELockHandle extends Handle
	{
		public final Handle internalHandle;
		
		public AELockHandle(Handle internalHandle)
		{
			super("AELockWrapper");
			this.internalHandle = internalHandle;
		}

		@Override
		protected void onClose(int flags)
		{
			unlockAutoExposure(this);
		}
	}
	
	
	// Constructor.
	UIExposureControllerImpl(CameraActivity cameraActivity)
	{
		super("UI Exposure Controller", cameraActivity, cameraActivity.getCameraThread(), ExposureController.class);
	}
	
	
	// Lock AE
	@Override
	public Handle lockAutoExposure(int flags)
	{
		this.verifyAccess();
		Handle handle = this.callTargetMethod("lockAutoExposure", new Class[]{ int.class }, flags);
		if(Handle.isValid(handle))
		{
			AELockHandle wrappedHandle = new AELockHandle(handle);
			m_AELockHandles.add(wrappedHandle);
			if(m_AELockHandles.size() == 1)
				this.setReadOnly(PROP_IS_AE_LOCKED, true);
			return wrappedHandle;
		}
		return null;
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
	
	
	// Initialize.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		CaptureModeManager captureModeManager = this.findComponent(CaptureModeManager.class);
		
		// add call-backs
		CameraActivity activity = this.getCameraActivity();
		PropertyChangedCallback unlockFocusCallback = new PropertyChangedCallback()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
			{
				unlockAutoExposure();
			}
		};
		activity.addCallback(CameraActivity.PROP_CAMERA, unlockFocusCallback);
		activity.addCallback(CameraActivity.PROP_MEDIA_TYPE, unlockFocusCallback);
		activity.addCallback(CameraActivity.PROP_STATE, new PropertyChangedCallback<State>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<State> key, PropertyChangeEventArgs<State> e)
			{
				if(e.getNewValue() == State.PAUSING)
					unlockAutoExposure();
			}
		});
		if(captureModeManager != null)
			captureModeManager.addCallback(CaptureModeManager.PROP_CAPTURE_MODE, unlockFocusCallback);
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
	
	
	// Unlock AE.
	private void unlockAutoExposure()
	{
		if(m_AELockHandles.isEmpty())
			return;
		
		Log.w(TAG, "unlockAutoExposure()");
		
		AELockHandle[] handles = new AELockHandle[m_AELockHandles.size()];
		m_AELockHandles.toArray(handles);
		for(int i = handles.length - 1 ; i >= 0 ; --i)
			Handle.close(handles[i]);
	}
	private void unlockAutoExposure(AELockHandle handle)
	{
		this.verifyAccess();
		if(!m_AELockHandles.remove(handle))
			return;
		Handle.close(handle.internalHandle);
	}
}
