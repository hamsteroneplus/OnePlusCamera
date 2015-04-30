package com.oneplus.camera;

/**
 * Base class for camera application component.
 */
public abstract class CameraComponent extends CameraThreadComponent
{
	// Private fields
	private final CameraActivity m_CameraActivity;
	
	
	/**
	 * Initialize new CameraComponent instance.
	 * @param name Component name.
	 * @param activity {@link CameraActivity} instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected CameraComponent(String name, CameraActivity activity, boolean hasHandler)
	{
		super(name, activity.getCameraThread(), hasHandler);
		m_CameraActivity = activity;
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
	}
	
	
	/**
	 * Get current primary camera.
	 * @return Primary camera.
	 */
	protected final Camera getCamera()
	{
		if(m_CameraActivity.isDependencyThread())
			return m_CameraActivity.get(CameraActivity.PROP_CAMERA);
		else
			return this.getCameraThread().get(CameraThread.PROP_CAMERA);
	}
	
	
	/**
	 * Get related {@link CameraActivity} instance.
	 * @return {@link CameraActivity} instance.
	 */
	public final CameraActivity getCameraActivity()
	{
		return m_CameraActivity;
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
