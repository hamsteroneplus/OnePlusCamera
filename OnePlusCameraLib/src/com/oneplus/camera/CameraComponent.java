package com.oneplus.camera;

import com.oneplus.camera.media.MediaType;

/**
 * Base class for camera application component.
 */
public abstract class CameraComponent extends CameraThreadComponent
{
	// Private fields
	private final CameraActivity m_CameraActivity;
	private final boolean m_IsCameraThreadComponent;
	
	
	/**
	 * Initialize new CameraComponent instance.
	 * @param name Component name.
	 * @param activity {@link CameraActivity} instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected CameraComponent(String name, CameraActivity activity, boolean hasHandler)
	{
		super(name, activity, activity.getCameraThread(), hasHandler);
		m_CameraActivity = activity;
		m_IsCameraThreadComponent = false;
	}
	
	
	/**
	 * Initialize new CameraComponent instance.
	 * @param name Component name.
	 * @param cameraThread {@link CameraThread} instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected CameraComponent(String name, CameraThread cameraThread, boolean hasHandler)
	{
		super(name, cameraThread, hasHandler);
		m_CameraActivity = (CameraActivity)cameraThread.getContext();
		m_IsCameraThreadComponent = true;
	}
	
	
	// Get current primary camera.
	@Override
	protected Camera getCamera()
	{
		if(m_IsCameraThreadComponent)
			return super.getCamera();
		return m_CameraActivity.get(CameraActivity.PROP_CAMERA);
	}
	
	
	/**
	 * Get related {@link CameraActivity} instance.
	 * @return {@link CameraActivity} instance.
	 */
	public final CameraActivity getCameraActivity()
	{
		return m_CameraActivity;
	}
	
	
	// Get current capture media type.
	@Override
	protected MediaType getMediaType()
	{
		if(m_IsCameraThreadComponent)
			return super.getMediaType();
		return m_CameraActivity.get(CameraActivity.PROP_MEDIA_TYPE);
	}
	
	
	/**
	 * Get current settings.
	 * @return Settings.
	 */
	protected final Settings getSettings()
	{
		return m_CameraActivity.get(CameraActivity.PROP_SETTINGS);
	}
}
