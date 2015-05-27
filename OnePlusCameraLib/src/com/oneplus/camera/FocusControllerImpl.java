package com.oneplus.camera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera.MeteringRect;

final class FocusControllerImpl extends CameraComponent implements FocusController
{
	// Private fields
	private AfHandle m_CurrentAfHandle;
	private final List<Handle> m_FocusLockHandles = new ArrayList<>();
	private AfHandle m_PendingAfHandle;
	
	
	// Call-backs.
	private final PropertyChangedCallback<List<MeteringRect>> m_AfRegionsChangedCallback = new PropertyChangedCallback<List<MeteringRect>>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<List<MeteringRect>> key, PropertyChangeEventArgs<List<MeteringRect>> e)
		{
			onAfRegionsChanged((Camera)source, e.getNewValue());
		}
	};
	private final PropertyChangedCallback<FocusMode> m_FocusModeChangedCallback = new PropertyChangedCallback<FocusMode>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<FocusMode> key, PropertyChangeEventArgs<FocusMode> e)
		{
			onFocusModeChanged((Camera)source, e.getNewValue());
		}
	};
	private final PropertyChangedCallback<FocusState> m_FocusStateChangedCallback = new PropertyChangedCallback<FocusState>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<FocusState> key, PropertyChangeEventArgs<FocusState> e)
		{
			onFocusStateChanged((Camera)source, e.getNewValue());
		}
	};
	
	
	// Class for AF.
	private final class AfHandle extends Handle
	{
		public final int flags;
		public final List<MeteringRect> regions;
		
		public AfHandle(List<MeteringRect> regions, int flags)
		{
			super("AutoFocus");
			this.regions = regions;
			this.flags = flags;
		}
		
		public void complete()
		{
			this.closeDirectly();
		}

		@Override
		protected void onClose(int flags)
		{
			//
		}
	}
	
	
	// Constructor
	FocusControllerImpl(CameraThread cameraThread)
	{
		super("Focus Controller", cameraThread, true);
	}
	
	
	// Cancel all AF.
	private void cancelAutoFocus()
	{
		//
	}
	
	
	// Lock focus.
	@Override
	public Handle lockFocus(int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "lockFocus() - Component is not running");
			return null;
		}
		
		// create handle
		Handle handle = new Handle("FocusLock")
		{
			@Override
			protected void onClose(int flags)
			{
				unlockFocus(this);
			}
		};
		m_FocusLockHandles.add(handle);
		Log.v(TAG, "lockFocus() - Handle : ", handle, ", handle count : ", m_FocusLockHandles.size());
		
		// clear pending AF
		if(m_PendingAfHandle != null)
		{
			m_PendingAfHandle.complete();
			m_PendingAfHandle = null;
		}
		
		// start locking focus
		if(m_FocusLockHandles.size() == 1)
		{
			this.setReadOnly(PROP_IS_FOCUS_LOCKED, true);
			if(this.get(PROP_FOCUS_STATE) != FocusState.SCANNING && this.get(PROP_FOCUS_MODE) == FocusMode.CONTINUOUS_AF)
			{
				Camera camera = this.getCamera();
				if(camera != null)
				{
					Log.w(TAG, "lockFocus() - Disable continuous AF to lock focus");
					camera.set(Camera.PROP_FOCUS_MODE, FocusMode.NORMAL_AF);
				}
			}
		}
		
		// complete
		return handle;
	}
	
	
	// Called when AF regions changed.
	private void onAfRegionsChanged(Camera camera, List<MeteringRect> regions)
	{
		this.setReadOnly(PROP_AF_REGIONS, regions);
	}
	
	
	// Called when primary camera changed.
	private void onCameraChanged(Camera oldCamera, Camera newCamera)
	{
		// cancel all AF
		this.cancelAutoFocus();
		
		// unbind from old camera
		if(oldCamera != null)
		{
			oldCamera.removeCallback(Camera.PROP_AF_REGIONS, m_AfRegionsChangedCallback);
			oldCamera.removeCallback(Camera.PROP_FOCUS_MODE, m_FocusModeChangedCallback);
			oldCamera.removeCallback(Camera.PROP_FOCUS_STATE, m_FocusStateChangedCallback);
		}
		
		// bind to new camera
		if(newCamera != null)
		{
			newCamera.addCallback(Camera.PROP_AF_REGIONS, m_AfRegionsChangedCallback);
			newCamera.addCallback(Camera.PROP_FOCUS_MODE, m_FocusModeChangedCallback);
			newCamera.addCallback(Camera.PROP_FOCUS_STATE, m_FocusStateChangedCallback);
			this.onFocusStateChanged(newCamera, newCamera.get(Camera.PROP_FOCUS_STATE));
			this.onFocusModeChanged(newCamera, newCamera.get(Camera.PROP_FOCUS_MODE));
		}
		else
		{
			this.onFocusStateChanged(newCamera, FocusState.INACTIVE);
			this.onFocusModeChanged(newCamera, FocusMode.DISABLED);
		}
	}
	
	
	// Called when focus mode changed.
	private void onFocusModeChanged(Camera camera, FocusMode focusMode)
	{
		this.setReadOnly(PROP_FOCUS_MODE, focusMode);
	}
	
	
	// Called when focus state changed.
	private void onFocusStateChanged(Camera camera, FocusState focusState)
	{
		this.setReadOnly(PROP_FOCUS_STATE, focusState);
		if(focusState != FocusState.SCANNING)
		{
			// complete current AF
			if(Handle.isValid(m_CurrentAfHandle))
			{
				m_CurrentAfHandle.complete();
				m_CurrentAfHandle = null;
			}
			
			// lock focus
			if(this.get(PROP_IS_FOCUS_LOCKED) && this.get(PROP_FOCUS_MODE) == FocusMode.CONTINUOUS_AF)
			{
				Log.w(TAG, "onFocusStateChanged() - Disable continuous AF to lock focus");
				camera.set(Camera.PROP_FOCUS_MODE, FocusMode.NORMAL_AF);
			}
			
			// start next AF
			if(m_PendingAfHandle != null)
			{
				AfHandle handle = m_PendingAfHandle;
				m_PendingAfHandle = null;
				Log.v(TAG, "onFocusStateChanged() - Start pending AF, handle : ", handle);
				this.startAutoFocus(camera, handle);
			}
		}
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
				onCameraChanged(e.getOldValue(), e.getNewValue());
			}
		});
		
		// bind to current camera
		this.onCameraChanged(null, this.getCamera());
	}
	
	
	// Start auto focus.
	@SuppressWarnings("unchecked")
	@Override
	public Handle startAutoFocus(List<Camera.MeteringRect> regions, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "startAutoFocus() - Component is not running");
			return null;
		}
		if(this.get(PROP_IS_FOCUS_LOCKED))
		{
			Log.w(TAG, "startAutoFocus() - Focus is locked");
			return null;
		}
		
		// check parameter
		if((flags & (FLAG_CONTINOUS_AF | FLAG_SINGLE_AF)) == (FLAG_CONTINOUS_AF | FLAG_SINGLE_AF))
		{
			Log.e(TAG, "startAutoFocus() - Invalid flags : " + flags);
			return null;
		}
		if(regions == null)
			regions = Collections.EMPTY_LIST;
		
		// check camera
		Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "startAutoFocus() - No primary camera");
			return null;
		}
		
		// check focus state
		AfHandle handle = new AfHandle(regions, flags);
		Log.v(TAG, "startAutoFocus() - Create handle : ", handle);
		/*
		if(camera.get(Camera.PROP_FOCUS_STATE) == FocusState.SCANNING)
		{
			Log.v(TAG, "startAutoFocus() - Focus state is SCANNING, start AF later");
			if(m_PendingAfHandle != null)
				Log.v(TAG, "startAutoFocus() - Cancel previous pending AF");
			m_PendingAfHandle = handle;
			return handle;
		}
		*/
		
		// start AF
		if(!this.startAutoFocus(camera, handle))
			return null;
		return handle;
	}
	private boolean startAutoFocus(Camera camera, AfHandle handle)
	{
		// check focus mode
		boolean continuousAF;
		List<FocusMode> focusModes = camera.get(Camera.PROP_FOCUS_MODES);
		if((handle.flags & FLAG_CONTINOUS_AF) != 0)
		{
			continuousAF = true;
			if(!focusModes.contains(FocusMode.CONTINUOUS_AF))
			{
				Log.e(TAG, "startAutoFocus() - Continuous AF is unsupported");
				return false;
			}
		}
		else if((handle.flags & FLAG_SINGLE_AF) != 0)
		{
			continuousAF = false;
			if(!focusModes.contains(FocusMode.NORMAL_AF))
			{
				Log.e(TAG, "startAutoFocus() - Single AF is unsupported");
				return false;
			}
		}
		else
		{
			continuousAF = focusModes.contains(FocusMode.CONTINUOUS_AF);
			if(!continuousAF && !focusModes.contains(FocusMode.NORMAL_AF))
			{
				Log.e(TAG, "startAutoFocus() - Both single and continuous AF is unsupported");
				return false;
			}
		}
		
		Log.v(TAG, "startAutoFocus() - Handle : ", handle);
		
		// set AF regions
		try
		{
			camera.set(Camera.PROP_AF_REGIONS, handle.regions);
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "startAutoFocus() - Fail to set AF regions", ex);
			return false;
		}
		
		// start AF
		try
		{
			if(continuousAF)
				camera.set(Camera.PROP_FOCUS_MODE, FocusMode.CONTINUOUS_AF);
			else
			{
				camera.set(Camera.PROP_FOCUS_MODE, FocusMode.NORMAL_AF);
				if(!camera.startAutoFocus(0))
				{
					Log.e(TAG, "startAutoFocus() - Fail to start single AF");
					return false;
				}
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "startAutoFocus() - Fail to start AF", ex);
			return false;
		}
		
		// complete
		m_CurrentAfHandle = handle;
		return true;
	}
	
	
	// Unlock focus.
	private void unlockFocus(Handle handle)
	{
		// remove handle
		this.verifyAccess();
		if(!m_FocusLockHandles.remove(handle))
			return;
		
		Log.v(TAG, "unlockFocus() - Handle : ", handle, ", handle count : ", m_FocusLockHandles.size());
		
		// check state
		if(!m_FocusLockHandles.isEmpty())
			return;
		
		// unlock focus
		this.setReadOnly(PROP_IS_FOCUS_LOCKED, false);
	}
}
