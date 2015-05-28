package com.oneplus.camera.capturemode;

import java.util.List;

import android.graphics.drawable.Drawable;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera;
import com.oneplus.camera.Camera.LensFacing;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.media.MediaType;
import com.oneplus.camera.scene.PhotoFaceBeautyScene;
import com.oneplus.camera.scene.Scene;
import com.oneplus.camera.scene.SceneEventArgs;
import com.oneplus.camera.scene.SceneManager;

/**
 * Capture mode to capture photo.
 */
public class PhotoCaptureMode extends SimpleCaptureMode
{
	// Private fields.
	private Scene m_FrontCamDefaultScene;
	private Handle m_FrontCamDefaultSceneHandle;
	private SceneManager m_SceneManager;
	
	
	// Call-backs.
	private final PropertyChangedCallback<Camera> m_CameraChangedCallback = new PropertyChangedCallback<Camera>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Camera> key, PropertyChangeEventArgs<Camera> e)
		{
			onCameraChanged(e.getNewValue());
		}
	};
	
	
	// Event handlers.
	private final EventHandler<SceneEventArgs> m_SceneAddedHandler = new EventHandler<SceneEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<SceneEventArgs> key, SceneEventArgs e)
		{
			onSceneAdded(e.getScene());
			if(m_FrontCamDefaultScene != null)
				source.removeHandler(key, this);
		}
	};
	
	
	/**
	 * Initialize new PhotoCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 */
	public PhotoCaptureMode(CameraActivity cameraActivity)
	{
		this(cameraActivity, "photo");
	}
	
	
	/**
	 * Initialize new PhotoCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param customSettingsName Name for custom settings.
	 */
	public PhotoCaptureMode(CameraActivity cameraActivity, String customSettingsName)
	{
		// call super
		super(cameraActivity, "Photo", MediaType.PHOTO, customSettingsName);
		
		// add call-back
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, m_CameraChangedCallback);
	}
	
	
	// Get display name.
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.capture_mode_photo);
	}
	
	
	// Get image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case CAPTURE_MODES_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.capture_mode_panel_icon_photo);
			default:
				return null;
		}
	}
	
	
	// Called when primary camera changes.
	private void onCameraChanged(Camera camera)
	{
		// find scene manager
		if(m_SceneManager == null)
		{
			m_SceneManager = ((MainActivity)this.getCameraActivity()).getSceneManager();
			if(m_SceneManager == null)
			{
				Log.w(TAG, "onCameraChanged() - No SceneManager interface");
				return;
			}
			List<Scene> scenes = m_SceneManager.get(SceneManager.PROP_SCENES);
			for(int i = scenes.size() - 1 ; i >= 0 ; --i)
				this.onSceneAdded(scenes.get(i));
			if(m_FrontCamDefaultScene == null)
			{
				m_SceneManager.addHandler(SceneManager.EVENT_SCENE_ADDED, m_SceneAddedHandler);
				return;
			}
		}
		
		// change default scene
		if(camera != null && camera.get(Camera.PROP_LENS_FACING) == LensFacing.FRONT)
		{
			if(!Handle.isValid(m_FrontCamDefaultSceneHandle) && m_FrontCamDefaultScene != null)
			{
				Log.v(TAG, "onCameraChanged() - Set default scene to ", m_FrontCamDefaultScene);
				m_FrontCamDefaultSceneHandle = m_SceneManager.setDefaultScene(m_FrontCamDefaultScene, 0);
			}
		}
		else
			m_FrontCamDefaultSceneHandle = Handle.close(m_FrontCamDefaultSceneHandle);
	}
	
	
	// Release scene.
	@Override
	protected void onRelease()
	{
		// remove call-backs
		this.getCameraActivity().removeCallback(CameraActivity.PROP_CAMERA, m_CameraChangedCallback);
		
		// remove event handlers
		if(m_SceneManager != null)
			m_SceneManager.removeHandler(SceneManager.EVENT_SCENE_ADDED, m_SceneAddedHandler);
		
		// call super
		super.onRelease();
	}
	
	
	// Called when scene added.
	private void onSceneAdded(Scene scene)
	{
		if(m_FrontCamDefaultScene == null && scene instanceof PhotoFaceBeautyScene)
		{
			// save scene
			m_FrontCamDefaultScene = scene;
			
			// set default scene
			this.onCameraChanged(this.getCamera());
		}
	}
}
