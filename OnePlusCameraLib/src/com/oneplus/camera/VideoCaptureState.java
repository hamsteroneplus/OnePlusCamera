package com.oneplus.camera;

/**
 * Video capture state.
 */
public enum VideoCaptureState
{
	/**
	 * Preparing.
	 */
	PREPARING,
	/**
	 * Ready to capture.
	 */
	READY,
	/**
	 * Starting.
	 */
	STARTING,
	/**
	 * Recording.
	 */
	CAPTURING,
	/**
	 * Pausing.
	 */
	PAUSING,
	/**
	 * Paused.
	 */
	PAUSED,
	/**
	 * Resuming.
	 */
	RESUMING,
	/**
	 * Stopping or processing.
	 */
	STOPPING,
	/**
	 * Reviewing.
	 */
	REVIEWING,
}
