package com.oneplus.camera.capturemode;

import com.oneplus.base.Log;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.media.MediaType;

/**
 * Capture mode for specific media type.
 */
public abstract class SimpleCaptureMode extends BasicCaptureMode
{
	// Private fields.
	private final MediaType m_MediaType;
	
	
	/**
	 * Initialize new SimpleCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param id ID represents this capture mode.
	 * @param mediaType Target media type.
	 * @param customSettingsName Name for custom settings.
	 */
	protected SimpleCaptureMode(CameraActivity cameraActivity, String id, MediaType mediaType, String customSettingsName)
	{
		// call super
		super(cameraActivity, id, customSettingsName);
		
		// setup fields
		if(mediaType == null)
			throw new IllegalArgumentException("No target media type.");
		m_MediaType = mediaType;
	}
	
	
	// Enter capture mode.
	@Override
	protected boolean onEnter(CaptureMode prevMode, int flags)
	{
		// stop preview
		CameraActivity cameraActivity = this.getCameraActivity();
		boolean restartPreview;
		if((flags & FLAG_PRESERVE_CAMERA_PREVIEW_STATE) == 0)
		{
			switch(cameraActivity.get(CameraActivity.PROP_CAMERA_PREVIEW_STATE))
			{
				case STARTING:
				case STARTED:
					Log.v(TAG, "onEnter() - Stop preview");
					restartPreview = true;
					cameraActivity.stopCameraPreview();
					break;
				default:
					restartPreview = false;
					break;
			}
		}
		else
			restartPreview = false;
		
		// enter
		try
		{
			// change media type
			if(!cameraActivity.setMediaType(m_MediaType))
			{
				Log.e(TAG, "onEnter() - Fail to change nedia type to " + m_MediaType);
				return false;
			}
			
			// complete
			return true;
		}
		finally
		{
			if(restartPreview)
			{
				Log.v(TAG, "onEnter() - Restart preview");
				cameraActivity.startCameraPreview();
			}
		}
	}

	
	// Exit capture mode.
	@Override
	protected void onExit(CaptureMode nextMode, int flags)
	{}
}
