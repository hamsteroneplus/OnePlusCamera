package com.oneplus.camera.scene;

import android.hardware.camera2.CaptureRequest;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.media.MediaType;

/**
 * Base class for photo scene.
 */
public abstract class PhotoScene extends BasicScene
{
	// Private fields.
	private Camera m_Camera;
	private final Integer m_SceneMode;
	
	
	/**
	 * Initialize new PhotoScene instance.
	 * @param cameraActivity Camera activity.
	 * @param id Mode ID.
	 */
	protected PhotoScene(CameraActivity cameraActivity, String id)
	{
		super(cameraActivity, id);
		m_SceneMode = null;
	}
	
	
	/**
	 * Initialize new PhotoScene instance.
	 * @param cameraActivity Camera activity.
	 * @param id Mode ID.
	 * @param sceneMode Camera scene mode.
	 */
	protected PhotoScene(CameraActivity cameraActivity, String id, int sceneMode)
	{
		super(cameraActivity, id);
		m_SceneMode = sceneMode;
	}
	
	
	// Called when camera changes.
	@Override
	protected void onCameraChanged(Camera camera)
	{
		// call super
		super.onCameraChanged(camera);
		
		// check scene mode
		if(m_SceneMode != null)
		{
			if(camera != null 
					&& camera.get(Camera.PROP_SCENE_MODES).contains(m_SceneMode) 
					&& this.getMediaType() == MediaType.PHOTO)
			{
				this.enable();
			}
			else
				this.disable();
		}
	}
	
	
	// Enter scene.
	@Override
	protected boolean onEnter(Scene prevScene, int flags)
	{
		// no nothing if there is no scene mode
		if(m_SceneMode == null)
			return true;
		
		// check camera
		m_Camera = this.getCamera();
		if(m_Camera == null)
		{
			Log.e(TAG, "onEnter() - No camera to enter scene");
			return false;
		}
		
		// enter
		final Camera camera = m_Camera;
		if(!HandlerUtils.post(camera, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					camera.set(Camera.PROP_SCENE_MODE, m_SceneMode);
				}
				catch(Throwable ex)
				{
					Log.e(TAG, "onEnter() - Fail to set scene mode", ex);
				}
			}
		}))
		{
			Log.e(TAG, "onEnter() - Fail to perform cross-thread operation");
			return false;
		}
		return true;
	}
	
	
	// Exit scene.
	@Override
	protected void onExit(Scene nextScene, int flags)
	{
		// no nothing if there is no scene mode
		if(m_SceneMode == null)
			return;
		
		// check camera
		if(m_Camera == null)
		{
			Log.w(TAG, "onExit() - No camera to exit scene");
			return;
		}
		
		// exit scene
		final Camera camera = m_Camera;
		m_Camera = null;
		if(!HandlerUtils.post(camera, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					camera.set(Camera.PROP_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED);
				}
				catch(Throwable ex)
				{
					Log.e(TAG, "onExit() - Fail to set scene mode", ex);
				}
			}
		}))
		{
			Log.e(TAG, "onExit() - Fail to perform cross-thread operation");
		}
	}
	
	
	// Called when media type changes.
	@Override
	protected void onMediaTypeChanged(MediaType mediaType)
	{
		// call super
		super.onMediaTypeChanged(mediaType);
		
		// check media type
		if(mediaType == MediaType.PHOTO)
		{
			if(m_SceneMode != null)
			{
				Camera camera = this.getCamera();
				if(camera != null && camera.get(Camera.PROP_SCENE_MODES).contains(m_SceneMode))
					this.enable();
				else
					this.disable();
			}
			else
				this.enable();
		}
		else
			this.disable();
	}
}
