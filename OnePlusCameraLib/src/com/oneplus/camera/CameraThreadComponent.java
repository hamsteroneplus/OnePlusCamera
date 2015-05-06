package com.oneplus.camera;

import android.content.Context;

import com.oneplus.base.component.BasicComponent;
import com.oneplus.base.component.ComponentOwner;
import com.oneplus.camera.media.MediaType;

/**
 * Base class for component in camera thread.
 */
public abstract class CameraThreadComponent extends BasicComponent
{
	// Fields
	private final CameraThread m_CameraThread;
	
	
	/**
	 * Initialize new CameraThreadComponent instance.
	 * @param name Component name.
	 * @param cameraThread {@link CameraThread} instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected CameraThreadComponent(String name, CameraThread cameraThread, boolean hasHandler)
	{
		this(name, cameraThread, cameraThread, hasHandler);
	}
	
	
	/**
	 * Initialize new CameraThreadComponent instance.
	 * @param name Component name.
	 * @param owner Component owner.
	 * @param cameraThread {@link CameraThread} instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected CameraThreadComponent(String name, ComponentOwner owner, CameraThread cameraThread, boolean hasHandler)
	{
		super(name, owner, hasHandler);
		m_CameraThread = cameraThread;
	}
	
	
	/**
	 * Get current primary camera.
	 * @return Primary camera.
	 */
	protected Camera getCamera()
	{
		return m_CameraThread.get(CameraThread.PROP_CAMERA);
	}
	
	
	/**
	 * Get camera thread owns this component.
	 * @return {@link CameraThread}.
	 */
	public final CameraThread getCameraThread()
	{
		return m_CameraThread;
	}
	
	
	/**
	 * Get related context.
	 * @return {@link Context}.
	 */
	public final Context getContext()
	{
		return m_CameraThread.getContext();
	}
	
	
	/**
	 * Get current capture media type.
	 * @return
	 */
	protected MediaType getMediaType()
	{
		return m_CameraThread.get(CameraThread.PROP_MEDIA_TYPE);
	}
}
