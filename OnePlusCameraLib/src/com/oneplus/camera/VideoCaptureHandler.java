package com.oneplus.camera;

import com.oneplus.camera.media.Resolution;

import android.media.MediaRecorder;

/**
 * Video capture handler interface.
 */
public interface VideoCaptureHandler
{
	/**
	 * Prepare profile for recording.
	 * @param camera Primary camera.
	 * @param mediaRecorder Media recorder to prepare.
	 * @param resolution Video resolution.
	 * @return True if media recorder is prepared by handler, False otherwise.
	 */
	boolean prepareCamcorderProfile(Camera camera, MediaRecorder mediaRecorder, Resolution resolution);
}
