package com.oneplus.camera;

import java.util.LinkedList;
import java.util.List;

import com.oneplus.base.BaseActivity.State;
import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera.MeteringRect;
import com.oneplus.camera.capturemode.CaptureModeManager;

final class UIFocusControllerImpl extends ProxyComponent<FocusController> implements FocusController
{
	// Private fields.
	private final LinkedList<FocusLockHandle> m_FocusLockHandles = new LinkedList<>();
	
	
	// Class for focus lock handle.
	private final class FocusLockHandle extends Handle
	{
		public final Handle internalHandle;
		
		public FocusLockHandle(Handle internalHandle)
		{
			super("FocusLockWrapper");
			this.internalHandle = internalHandle;
		}

		@Override
		protected void onClose(int flags)
		{
			unlockFocus(this);
		}
	}
	
	
	// Constructor
	UIFocusControllerImpl(CameraActivity cameraActivity)
	{
		super("Focus Controller (UI)", cameraActivity, cameraActivity.getCameraThread(), FocusController.class);
	}
	
	
	// Called before binding to target properties.
	@Override
	protected void onBindingToTargetProperties(List<PropertyKey<?>> keys)
	{
		super.onBindingToTargetProperties(keys);
		keys.add(PROP_AF_REGIONS);
		keys.add(PROP_CAN_CHANGE_FOCUS);
		keys.add(PROP_FOCUS_MODE);
		keys.add(PROP_FOCUS_STATE);
		keys.add(PROP_IS_FOCUS_LOCKED);
	}
	
	
	// Lock focus.
	@Override
	public Handle lockFocus(int flags)
	{
		this.verifyAccess();
		Handle handle = this.callTargetMethod("lockFocus", new Class[]{ int.class }, flags);
		if(Handle.isValid(handle))
		{
			FocusLockHandle wrappedHandle = new FocusLockHandle(handle);
			m_FocusLockHandles.add(wrappedHandle);
			if(m_FocusLockHandles.size() == 1)
				this.setReadOnly(PROP_IS_FOCUS_LOCKED, true);
			return wrappedHandle;
		}
		return null;
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
				unlockFocus();
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
					unlockFocus();
			}
		});
		if(captureModeManager != null)
			captureModeManager.addCallback(CaptureModeManager.PROP_CAPTURE_MODE, unlockFocusCallback);
	}
	
	
	// Start auto focus.
	@Override
	public Handle startAutoFocus(List<MeteringRect> regions, int flags)
	{
		this.verifyAccess();
		return this.callTargetMethod("startAutoFocus", new Class[]{ List.class, int.class }, regions, flags);
	}
	
	
	// Unlock focus.
	private void unlockFocus()
	{
		if(m_FocusLockHandles.isEmpty())
			return;
		
		Log.w(TAG, "unlockFocus()");
		
		FocusLockHandle[] handles = new FocusLockHandle[m_FocusLockHandles.size()];
		m_FocusLockHandles.toArray(handles);
		for(int i = handles.length - 1 ; i >= 0 ; --i)
			Handle.close(handles[i]);
	}
	private void unlockFocus(FocusLockHandle handle)
	{
		if(!m_FocusLockHandles.remove(handle))
			return;
		Handle.close(handle.internalHandle);
	}
}
