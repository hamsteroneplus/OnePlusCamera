package com.oneplus.camera.slowmotion;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Range;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.ModeController;
import com.oneplus.camera.VideoCaptureHandler;
import com.oneplus.camera.VideoCaptureState;
import com.oneplus.camera.media.Resolution;

final class SlowMotionController extends ModeController<SlowMotionUI> implements VideoCaptureHandler
{
	// Constants.
	static final float SPEED_RATIO = 0.25f;
	
	
	// Private fields.
	private Handle m_CaptureHandlerHandle;
	
	
	// Constructor.
	SlowMotionController(CameraThread cameraThread)
	{
		super("Slow-motion Controller", cameraThread);
	}
	
	
	// Enter mode.
	@Override
	protected boolean onEnter(int flags)
	{
		// call super
		if(!super.onEnter(flags))
			return false;
		
		// register capture handler
		m_CaptureHandlerHandle = this.getCameraThread().setVideoCaptureHandler(this, 0);
		if(!Handle.isValid(m_CaptureHandlerHandle))
		{
			Log.e(TAG, "onEnter() - Fail to set capture handler");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Exit mode.
	@Override
	protected void onExit(int flags)
	{
		// remove capture handler
		m_CaptureHandlerHandle = Handle.close(m_CaptureHandlerHandle);
		
		// call super
		super.onExit(flags);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add property changed call-backs.
		CameraThread cameraThread = this.getCameraThread();
		cameraThread.addCallback(CameraThread.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@SuppressWarnings("incomplete-switch")
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				switch(e.getOldValue())
				{
					case STARTING:
						if(e.getNewValue() == VideoCaptureState.CAPTURING)
							break;
					case STOPPING:
						onVideoCaptureStopped();
						break;
				}
			}
		});
	}
	
	
	// Called after stopping video capture.
	private void onVideoCaptureStopped()
	{
		// check state
		if(!this.isEntered())
			return;
		
		// get camera
		Camera camera = this.getCamera();
		if(camera == null)
		{
			Log.e(TAG, "onVideoCaptureStopped() - No camera");
			return;
		}
		
		// restore FPS range
		camera.set(Camera.PROP_PREVIEW_FPS_RANGE, null);
	}


	// Prepare camcorder profile.
	@Override
	public boolean prepareCamcorderProfile(Camera camera, MediaRecorder mediaRecorder, Resolution resolution)
	{
		// check state
		if(!this.isEntered())
		{
			Log.w(TAG, "prepareCamcorderProfile() - Not entered");
			return false;
		}
		
		// select profile
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
		
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
		
		// select FPS range
		camera.set(Camera.PROP_PREVIEW_FPS_RANGE, new Range<Integer>(15, 120));
		
		// complete
		return true;
	}
}
