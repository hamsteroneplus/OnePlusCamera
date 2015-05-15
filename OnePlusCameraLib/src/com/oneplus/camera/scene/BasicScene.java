package com.oneplus.camera.scene;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.BasicMode;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.media.MediaType;

/**
 * Basic implementation of {@link Scene} interface.
 */
public abstract class BasicScene extends BasicMode<Scene> implements Scene
{
	// Call-backs.
	private final PropertyChangedCallback<Camera> m_CameraChangedCallback = new PropertyChangedCallback<Camera>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Camera> key, PropertyChangeEventArgs<Camera> e)
		{
			onCameraChanged(e.getNewValue());
		}
	};
	private final PropertyChangedCallback<MediaType> m_MediaTypeChangedCallback = new PropertyChangedCallback<MediaType>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<MediaType> key, PropertyChangeEventArgs<MediaType> e)
		{
			onMediaTypeChanged(e.getNewValue());
		}
	};
	
	
	/**
	 * Initialize new BasicScene instance.
	 * @param cameraActivity Camera activity.
	 * @param id Mode ID.
	 */
	protected BasicScene(CameraActivity cameraActivity, String id)
	{
		super(cameraActivity, id);
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, m_CameraChangedCallback);
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, m_MediaTypeChangedCallback);
		this.onCameraChanged(cameraActivity.get(CameraActivity.PROP_CAMERA));
		this.onMediaTypeChanged(cameraActivity.get(CameraActivity.PROP_MEDIA_TYPE));
	}
	
	
	/**
	 * Called when primary camera changes.
	 * @param camera New primary camera.
	 */
	protected void onCameraChanged(Camera camera)
	{}
	
	
	/**
	 * Called when captured media type changes.
	 * @param mediaType New media type.
	 */
	protected void onMediaTypeChanged(MediaType mediaType)
	{}
	
	
	// Release scene.
	@Override
	protected void onRelease()
	{
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.removeCallback(CameraActivity.PROP_CAMERA, m_CameraChangedCallback);
		cameraActivity.removeCallback(CameraActivity.PROP_MEDIA_TYPE, m_MediaTypeChangedCallback);
		super.onRelease();
	}
}
