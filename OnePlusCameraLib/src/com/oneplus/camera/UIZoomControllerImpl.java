package com.oneplus.camera;

import java.util.List;

import android.os.SystemClock;

import com.oneplus.base.BaseActivity.State;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;

final class UIZoomControllerImpl extends ProxyComponent<ZoomController> implements ZoomController
{
	// Private fields.
	private long m_LastZoomChangedTime;
	
	
	// Constructor.
	UIZoomControllerImpl(CameraActivity cameraActivity)
	{
		super("UI Zoom Controller", cameraActivity, cameraActivity.getCameraThread(), ZoomController.class);
	}
	
	
	// Apply digital zoom.
	private boolean applyDigitalZoom(final float zoom)
	{
		if(this.isTargetBound())
		{
			if(!HandlerUtils.post(this.getTargetOwner(), new Runnable()
			{
				@Override
				public void run()
				{
					getTarget().set(PROP_DIGITAL_ZOOM, zoom);
				}
			}))
			{
				Log.e(TAG, "applyDigitalZoom() - Fail to set zoom asynchronously");
				return false;
			}
			return true;
		}
		else
		{
			Log.w(TAG, "applyDigitalZoom() - Target is not ready, set zoom later");
			return true;
		}
	}
	
	
	// Lock zoom.
	@Override
	public Handle lockZoom(int flags)
	{
		// check state
		this.verifyAccess();
		
		// lock zoom
		Handle handle = this.callTargetMethod("lockZoom", new Class[]{ int.class }, flags);
		if(Handle.isValid(handle))
			this.setReadOnly(PROP_IS_ZOOM_LOCKED, true);
		return handle;
	}
	
	
	// Called before binding to target.
	@Override
	protected void onBindingToTargetProperties(List<PropertyKey<?>> keys)
	{
		super.onBindingToTargetProperties(keys);
		keys.add(PROP_DIGITAL_ZOOM);
		keys.add(PROP_IS_DIGITAL_ZOOM_SUPPORTED);
		keys.add(PROP_IS_ZOOM_LOCKED);
		keys.add(PROP_MAX_DIGITAL_ZOOM);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add call-backs
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.addCallback(CameraActivity.PROP_STATE, new PropertyChangedCallback<State>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<State> key, PropertyChangeEventArgs<State> e)
			{
				if(e.getNewValue() == State.NEW_INTENT)
					setDigitalZoomProp(1f);
			}
		});
	}
	
	
	// Called when target bound.
	@Override
	protected void onTargetBound(ZoomController target)
	{
		// call super
		super.onTargetBound(target);
		
		// apply zoom
		if(m_LastZoomChangedTime > 0)
			this.applyDigitalZoom(this.get(PROP_DIGITAL_ZOOM));
	}
	
	
	// Called after changing target property.
	@Override
	protected void onTargetPropertyChanged(long time, PropertyKey<?> key, PropertyChangeEventArgs<?> e)
	{
		if(key == PROP_DIGITAL_ZOOM)
		{
			if(time >= m_LastZoomChangedTime)
				super.set(PROP_DIGITAL_ZOOM, (Float)e.getNewValue());
		}
		else
			super.onTargetPropertyChanged(time, key, e);
	}
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_DIGITAL_ZOOM)
			return this.setDigitalZoomProp((Float)value);
		return super.set(key, value);
	}
	
	
	// Set PROP_DIGITAL_ZOOM property.
	private boolean setDigitalZoomProp(float zoom)
	{
		// check state
		this.verifyAccess();
		if(!this.get(PROP_IS_DIGITAL_ZOOM_SUPPORTED))
		{
			Log.w(TAG, "setDigitalZoomProp() - Digital zoom is unsupported");
			return false;
		}
		
		// check ratio
		if(zoom < 1 || Math.abs(zoom - 1) < 0.01f)
			zoom = 1;
		else
			zoom = Math.min(zoom, this.get(PROP_MAX_DIGITAL_ZOOM));
		
		// save zoom
		m_LastZoomChangedTime = SystemClock.elapsedRealtimeNanos();
		if(!super.set(PROP_DIGITAL_ZOOM, zoom))
			return false;
		zoom = this.get(PROP_DIGITAL_ZOOM);
		
		// apply zoom
		this.applyDigitalZoom(zoom);
		
		// complete
		return true;
	}
}
