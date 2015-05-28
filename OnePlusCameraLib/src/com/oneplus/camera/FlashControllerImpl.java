package com.oneplus.camera;

import java.util.LinkedList;

import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera.LensFacing;
import com.oneplus.camera.media.MediaType;

final class FlashControllerImpl extends CameraComponent implements FlashController
{
	// Constants.
	private static final String SETTINGS_KEY_FLASH_MODE_BACK = "FlashMode.Back";
	private static final String SETTINGS_KEY_FLASH_MODE_FRONT = "FlashMode.Front";
	
	
	// Private fields.
	private final LinkedList<Handle> m_FlashDisableHandle = new LinkedList<>();
	
	
	// Static initializer
	static
	{
		//Settings.addPrivateKey(SETTINGS_KEY_FLASH_MODE_BACK);
		//Settings.addPrivateKey(SETTINGS_KEY_FLASH_MODE_FRONT);
		Settings.setGlobalDefaultValue(SETTINGS_KEY_FLASH_MODE_BACK, FlashMode.AUTO);
		Settings.setGlobalDefaultValue(SETTINGS_KEY_FLASH_MODE_FRONT, FlashMode.AUTO);
	}
	
	
	// Constructor
	FlashControllerImpl(CameraActivity cameraActivity)
	{
		super("Flash Controller", cameraActivity, true);
	}
	
	
	// Disable flash temporarily.
	@Override
	public Handle disableFlash(int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "disableFlash() - Component is not running");
			return null;
		}
		
		// create handle
		Handle handle = new Handle("FlashDisable")
		{
			@Override
			protected void onClose(int flags)
			{
				enableFlash(this);
			}
		};
		m_FlashDisableHandle.add(handle);
		
		// disable flash
		if(m_FlashDisableHandle.size() == 1)
			this.updateFlashState();
		
		// complete
		return handle;
	}
	
	
	// Enable flash.
	private void enableFlash(Handle handle)
	{
		this.verifyAccess();
		if(!m_FlashDisableHandle.remove(handle) || !m_FlashDisableHandle.isEmpty())
			return;
		this.updateFlashState();
	}
	
	
	// Initialize.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add property changed call-backs
		CameraActivity cameraActivity = this.getCameraActivity();
		PropertyChangedCallback callback = new PropertyChangedCallback()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
			{
				updateFlashState();
			}
		};
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, callback);
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, callback);
		cameraActivity.addCallback(CameraActivity.PROP_SETTINGS, callback);
		
		// setup initial flash state
		this.updateFlashState();
	}
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_FLASH_MODE)
			return this.setFlashModeProp((FlashMode)value, false);
		return super.set(key, value);
	}
	
	
	// Set flash mode to camera.
	private boolean setFlashMode(final FlashMode flashMode)
	{
		final Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "setFlashMode() - No primary camera");
			return false;
		}
		if(!HandlerUtils.post(camera, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					camera.set(Camera.PROP_FLASH_MODE, flashMode);
				}
				catch(Throwable ex)
				{
					Log.e(TAG, "setFlashMode() - Fail to set flash mode", ex);
				}
			}
		}))
		{
			Log.e(TAG, "setFlashMode() - Fail to perform cross-thread operation");
			return false;
		}
		return true;
	}
	
	
	// Set flash mode.
	private boolean setFlashModeProp(FlashMode flashMode, boolean forceSet)
	{
		if(forceSet || this.get(PROP_FLASH_MODE) != flashMode)
		{
			// check state
			this.verifyAccess();
			Log.v(TAG, "setFlashModeProp() - Flash mode : ", flashMode);
			if(flashMode == null)
				throw new IllegalArgumentException("No flash mode.");
			if(flashMode != FlashMode.OFF && (!this.get(PROP_HAS_FLASH) || this.get(PROP_IS_FLASH_DISABLED)))
			{
				Log.e(TAG, "setFlashModeProp() - No flash support");
				return false;
			}
			
			// apply flash mode
			Camera camera = this.getCamera();
			String settingsKey = (camera.get(Camera.PROP_LENS_FACING) == LensFacing.BACK ? SETTINGS_KEY_FLASH_MODE_BACK : SETTINGS_KEY_FLASH_MODE_FRONT);
			this.setFlashMode(flashMode);
			
			// save to settings
			this.getSettings().set(settingsKey, flashMode);
			
			// set property
			return super.set(PROP_FLASH_MODE, flashMode);
		}
		return false;
	}
	
	
	// Update flash state.
	@SuppressWarnings("incomplete-switch")
	private void updateFlashState()
	{
		// check camera
		Camera camera = this.getCamera();
		if(camera == null)
			Log.e(TAG, "updateFlashState() - No primary camera");
		
		// get settings key
		String settingsKey;
		if(camera != null)
			settingsKey = (camera.get(Camera.PROP_LENS_FACING) == LensFacing.BACK ? SETTINGS_KEY_FLASH_MODE_BACK : SETTINGS_KEY_FLASH_MODE_FRONT);
		else
			settingsKey = null;
		
		// check flash function
		if(camera == null || !camera.get(Camera.PROP_HAS_FLASH))
		{
			if(settingsKey != null)
				this.getSettings().set(settingsKey, FlashMode.OFF);
			this.setReadOnly(PROP_HAS_FLASH, false);
			super.set(PROP_FLASH_MODE, FlashMode.OFF);
			this.setReadOnly(PROP_IS_FLASH_DISABLED, true);
			return;
		}
		else
			this.setReadOnly(PROP_HAS_FLASH, true);
		
		// check disable handles
		if(!m_FlashDisableHandle.isEmpty())
		{
			this.setReadOnly(PROP_IS_FLASH_DISABLED, true);
			this.setFlashMode(FlashMode.OFF);
			return;
		}
		
		// enable flash
		this.setReadOnly(PROP_IS_FLASH_DISABLED, false);
		
		// setup flash mode
		FlashMode flashMode = this.getSettings().getEnum(settingsKey, FlashMode.class);
		switch(flashMode)
		{
			case AUTO:
				if(this.getMediaType() == MediaType.VIDEO)
					flashMode = FlashMode.OFF;
				break;
			case ON:
				if(this.getMediaType() == MediaType.VIDEO)
					flashMode = FlashMode.TORCH;
				break;
			case TORCH:
				if(this.getMediaType() == MediaType.PHOTO)
					flashMode = FlashMode.ON;
				break;
		}
		this.setFlashModeProp(flashMode, true);
	}
}
