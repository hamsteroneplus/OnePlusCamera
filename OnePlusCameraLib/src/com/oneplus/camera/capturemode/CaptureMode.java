package com.oneplus.camera.capturemode;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.camera.Mode;
import com.oneplus.camera.Settings;

/**
 * Capture mode interface.
 */
public interface CaptureMode extends Mode<CaptureMode>
{
	/**
	 * Invalid capture mode.
	 */
	CaptureMode INVALID = new CaptureMode()
	{
		@Override
		public <TArgs extends EventArgs> void removeHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
		{}
		
		@Override
		public <TArgs extends EventArgs> void addHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
		{}
		
		@Override
		public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
		{
			throw new IllegalAccessError();
		}
		
		@Override
		public <TValue> void removeCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
		{}
		
		@Override
		public <TValue> TValue get(PropertyKey<TValue> key)
		{
			return key.defaultValue;
		}
		
		@Override
		public <TValue> void addCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
		{}
		
		@Override
		public boolean isDependencyThread()
		{
			return false;
		}
		
		@Override
		public void release()
		{}
		
		@Override
		public void exit(CaptureMode nextMode, int flags)
		{}
		
		@Override
		public boolean enter(CaptureMode prevMode, int flags)
		{
			return false;
		}
		
		@Override
		public Settings getCustomSettings()
		{
			return null;
		}
	};
	
	
	/**
	 * Get custom settings for this capture mode.
	 * @return Custom settings, or Null to use global settings.
	 */
	Settings getCustomSettings();
}
