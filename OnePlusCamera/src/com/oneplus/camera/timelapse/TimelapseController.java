package com.oneplus.camera.timelapse;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Message;

import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.Rotation;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.VideoCaptureHandler;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.media.Resolution;

final class TimelapseController extends CameraComponent implements VideoCaptureHandler
{
	// Constants.
	static final float SPEED_RATIO = 6.0f;
	private static final int MSG_ENTER = 10000;
	private static final int MSG_EXIT = 10001;
	
	
	// Private fields.
	private Handle m_CaptureHandlerHandle;
	private boolean m_IsEntered;
	
	
	// Constructor.
	TimelapseController(CameraThread cameraThread)
	{
		super("Time-lapse Controller", cameraThread, true);
	}
	
	
	// Enter.
	boolean enter()
	{
		// enter asynchronously
		if(!this.isDependencyThread())
		{
			HandlerUtils.removeMessages(this, MSG_EXIT);
			return HandlerUtils.sendMessage(this, MSG_ENTER, true);
		}
		
		// check state
		if(m_IsEntered)
			return true;
		
		Log.v(TAG, "enter()");
		
		// register capture handler
		m_CaptureHandlerHandle = this.getCameraThread().setVideoCaptureHandler(this, 0);
		if(!Handle.isValid(m_CaptureHandlerHandle))
		{
			Log.e(TAG, "enter() - Fail to set capture handler");
			return false;
		}
		
		// complete
		m_IsEntered = true;
		return true;
	}
	
	
	// Exit.
	void exit()
	{
		// exit asynchronously
		if(!this.isDependencyThread())
		{
			HandlerUtils.removeMessages(this, MSG_ENTER);
			HandlerUtils.sendMessage(this, MSG_EXIT, true);
			return;
		}
		
		// check state
		if(!m_IsEntered)
			return;
		
		Log.v(TAG, "exit()");
		
		// remove capture handler
		m_CaptureHandlerHandle = Handle.close(m_CaptureHandlerHandle);
		
		// complete
		m_IsEntered = false;
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_ENTER:
				this.enter();
				break;
				
			case MSG_EXIT:
				this.exit();
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}

	
	// Prepare camcorder profile.
	@Override
	public boolean prepareCamcorderProfile(Camera camera, MediaRecorder mediaRecorder, Resolution resolution)
	{
		// check state
		if(!m_IsEntered)
		{
			Log.w(TAG, "prepareCamcorderProfile() - Not entered");
			return false;
		}
		
		// select profile
		CamcorderProfile profile;
		if(resolution.is4kVideo())
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_2160P);
		else if(resolution.is1080pVideo())
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_1080P);
		else if(resolution.is720pVideo())
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_720P);
		else if(resolution.isMmsVideo())
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_QCIF);
		else
		{
			Log.e(TAG, "prepareCamcorderProfile() - Unknown resolution : " + resolution);
			throw new RuntimeException("Unknown resolution : " + resolution);
		}
		
		// set AV sources
		//mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		
		// set profile
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);    
		mediaRecorder.setVideoFrameRate(profile.videoFrameRate);              
		mediaRecorder.setCaptureRate(profile.videoFrameRate / SPEED_RATIO);
		mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);              
	    mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);                
	    //mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);                
	    //mediaRecorder.setAudioChannels(profile.audioChannels);              
	    //mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);                
	    mediaRecorder.setVideoEncoder(profile.videoCodec);              
	   	//mediaRecorder.setAudioEncoder(profile.audioCodec);
		
		// set orientation
		int orientation = (this.getCameraThread().get(CameraThread.PROP_CAPTURE_ROTATION).getDeviceOrientation() - Rotation.LANDSCAPE.getDeviceOrientation());
		if(orientation < 0)
			orientation += 360;
		Log.v(TAG, "prepareCamcorderProfile() - Orientation : ", orientation);
		mediaRecorder.setOrientationHint(orientation);
		
		// complete
		return true;
	}
}
