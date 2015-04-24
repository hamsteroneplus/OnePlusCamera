package com.oneplus.camera;

import android.content.Context;

import com.oneplus.base.component.BasicComponent;

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
		super(name, cameraThread, hasHandler);
		m_CameraThread = cameraThread;
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
}
