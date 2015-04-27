package com.oneplus.camera;

/**
 * State of {@link CameraThread}.
 */
public enum CameraThreadState
{
	/**
	 * Thread is just created.
	 */
	NEW,
	/**
	 * Starting.
	 */
	STARTING,
	/**
	 * Thread is running.
	 */
	RUNNING,
	/**
	 * Releasing.
	 */
	RELEASING,
	/**
	 * Released and cannot use anymore.
	 */
	RELEASED,
}
