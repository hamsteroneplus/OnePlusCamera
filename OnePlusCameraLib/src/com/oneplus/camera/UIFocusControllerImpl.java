package com.oneplus.camera;

import java.util.List;

import android.os.Message;

import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.Camera.MeteringRect;

final class UIFocusControllerImpl extends CameraComponent implements FocusController
{
	// Constants.
	private static final int MSG_CONTROLLER_PROPERTY_CHANGED = 10000;
	
	
	// Private fields.
	private FocusController m_FocusController;
	private WrappedAfHandle m_PendingAfHandle;
	
	
	// Class for AF handle.
	private final class WrappedAfHandle extends Handle
	{
		public Handle actualHandle;
		public final int flags;
		public final List<MeteringRect> regions;
		
		public WrappedAfHandle(List<MeteringRect> regions, int flags)
		{
			super("WrappedAutoFocus");
			this.regions = regions;
			this.flags = flags;
		}

		@Override
		protected void onClose(final int flags)
		{
			if(!HandlerUtils.post(m_FocusController, new Runnable()
			{
				@Override
				public void run()
				{
					actualHandle = Handle.close(actualHandle, flags);
				}
			}))
			{
				Log.e(TAG, "onClose() - Fail to perform cross-thread operation");
			}
		}
	}
	
	
	// Constructor
	UIFocusControllerImpl(CameraActivity cameraActivity)
	{
		super("Focus Controller (UI)", cameraActivity, true);
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
				this.onFocusControllerPropertyChanged((Camera)array[0], (PropertyKey<?>)array[1], array[2]);
				break;
			}
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when actual FocusController found.
	private void onFocusControllerFound(final FocusController focusController)
	{
		// check state
		if(!this.isRunningOrInitializing())
			return;
		
		// add call-backs
		if(!HandlerUtils.post(focusController, new Runnable()
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void run()
			{
				// add call-backs
				Log.v(TAG, "onFocusControllerFound() - Add call-backs");
				PropertyChangedCallback callback = new PropertyChangedCallback()
				{
					@Override
					public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
					{
						Camera camera = getCameraThread().get(CameraThread.PROP_CAMERA);
						HandlerUtils.sendMessage(UIFocusControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, key, e.getNewValue() });
					}
				};
				focusController.addCallback(PROP_AF_REGIONS, callback);
				focusController.addCallback(PROP_CAN_CHANGE_FOCUS, callback);
				focusController.addCallback(PROP_FOCUS_MODE, callback);
				focusController.addCallback(PROP_FOCUS_STATE, callback);
				focusController.addCallback(PROP_IS_FOCUS_LOCKED, callback);
				
				// sync current values
				Camera camera = getCameraThread().get(CameraThread.PROP_CAMERA);
				HandlerUtils.sendMessage(UIFocusControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_AF_REGIONS, focusController.get(PROP_AF_REGIONS) });
				HandlerUtils.sendMessage(UIFocusControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_CAN_CHANGE_FOCUS, focusController.get(PROP_CAN_CHANGE_FOCUS) });
				HandlerUtils.sendMessage(UIFocusControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_FOCUS_MODE, focusController.get(PROP_FOCUS_MODE) });
				HandlerUtils.sendMessage(UIFocusControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_FOCUS_STATE, focusController.get(PROP_FOCUS_STATE) });
				HandlerUtils.sendMessage(UIFocusControllerImpl.this, MSG_CONTROLLER_PROPERTY_CHANGED, 0, 0, new Object[]{ camera, PROP_IS_FOCUS_LOCKED, focusController.get(PROP_IS_FOCUS_LOCKED) });
			}
		}))
		{
			Log.e(TAG, "onFocusControllerFound() - Fail to perform cross-thread operation");
			return;
		}
		
		// save instance
		m_FocusController = focusController;
		
		// start AF
		if(m_PendingAfHandle != null)
		{
			WrappedAfHandle handle = m_PendingAfHandle;
			m_PendingAfHandle = null;
			this.startAutoFocus(handle);
		}
	}
	
	
	// Called when property in FocusController changed.
	private void onFocusControllerPropertyChanged(Camera camera, PropertyKey<?> key, Object newValue)
	{
		if(this.getCamera() != camera)
			return;
		if(key == PROP_FOCUS_STATE)
			this.onFocusStateChanged((FocusState)newValue);
	}
	
	
	// Called when focus state changed.
	private void onFocusStateChanged(FocusState focusState)
	{
		Log.v(TAG, "onFocusStateChanged() - ", focusState);
		this.setReadOnly(PROP_FOCUS_STATE, focusState);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// bind to actual FocusController
		ComponentUtils.findComponent(this.getCameraThread(), FocusController.class, this, new ComponentSearchCallback<FocusController>()
		{
			@Override
			public void onComponentFound(FocusController component)
			{
				onFocusControllerFound(component);
			}
		});
	}

	
	// Start auto focus.
	@Override
	public Handle startAutoFocus(List<MeteringRect> regions, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "startAutoFocus() - Component is not running");
			return null;
		}
		
		// create handle
		WrappedAfHandle handle = new WrappedAfHandle(regions, flags);
		
		// check focus controller
		if(m_FocusController == null)
		{
			Log.w(TAG, "startAutoFocus() - FocusController is not ready, start AF later");
			m_PendingAfHandle = handle;
			return handle;
		}
		
		// start AF
		if(!this.startAutoFocus(handle))
			return null;
		return handle;
	}
	private boolean startAutoFocus(final WrappedAfHandle handle)
	{
		if(HandlerUtils.post(m_FocusController, new Runnable()
		{
			@Override
			public void run()
			{
				if(Handle.isValid(handle))
					handle.actualHandle = m_FocusController.startAutoFocus(handle.regions, handle.flags);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "startAutoFocus() - Fail to perform cross-thread operation");
		return false;
	}
}
