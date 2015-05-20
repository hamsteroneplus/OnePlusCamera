package com.oneplus.camera.timelapse;

import android.graphics.drawable.Drawable;

import com.oneplus.base.Log;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.capturemode.CaptureMode;
import com.oneplus.camera.capturemode.SimpleCaptureMode;
import com.oneplus.camera.media.MediaType;

final class TimelapseCaptureMode extends SimpleCaptureMode
{
	// Private fields.
	private TimelapseUI m_UI;
	
	
	// Constructor.
	TimelapseCaptureMode(CameraActivity cameraActivity)
	{
		super(cameraActivity, "Time-lapse", MediaType.VIDEO, "timelapse");
	}
	
	
	// Get display name
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.capture_mode_timelapse);
	}

	
	// Get related image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case CAPTURE_MODES_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.capture_mode_panel_icon_timelapse);
			default:
				return null;
		}
	}
	
	
	// Enter.
	@Override
	protected boolean onEnter(CaptureMode prevMode, int flags)
	{
		// call super
		if(!super.onEnter(prevMode, flags))
			return false;
		
		// find UI component
		if(m_UI == null)
		{
			m_UI = this.getCameraActivity().findComponent(TimelapseUI.class);
			if(m_UI == null)
			{
				Log.e(TAG, "onEnter() - No TimelapseUI");
				return false;
			}
		}
		
		// enter
		if(!m_UI.enter())
		{
			Log.e(TAG, "onEnter() - Fail to enter UI");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Exit.
	@Override
	protected void onExit(CaptureMode nextMode, int flags)
	{
		// exit
		if(m_UI != null)
			m_UI.exit();
		
		// call super
		super.onExit(nextMode, flags);
	}
}
