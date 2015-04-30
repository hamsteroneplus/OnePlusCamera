package com.oneplus.camera.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.CaptureEventArgs;
import com.oneplus.camera.CaptureHandle;
import com.oneplus.camera.PhotoCaptureState;
import com.oneplus.camera.R;
import com.oneplus.camera.media.MediaType;

final class CaptureBar extends CameraComponent implements CaptureButtons
{
	// Constants
	private static final int MSG_START_BURST_SHOTS = 10000;
	private static final long BURST_TRIGGER_THRESHOLD = 500;
	
	
	// Private fields
	private View m_CaptureBar;
	private boolean m_IsCapturingBurstPhotos;
	private CaptureHandle m_PhotoCaptureHandle;
	private Button m_PrimaryButton;
	private CaptureButtonFunction m_PrimaryButtonFunction = CaptureButtonFunction.CAPTURE_PHOTO;
	private CaptureHandle m_VideoCaptureHandle;
	
	
	// Constants for capture button function.
	private enum CaptureButtonFunction
	{
		CAPTURE_PHOTO,
		CAPTURE_VIDEO,
		PAUSE_RESUME_VIDEO,
	}
	
	
	// Constructor
	CaptureBar(CameraActivity cameraActivity)
	{
		super("Capture Bar", cameraActivity, true);
	}
	
	
	// Called when media capture starts.
	private void onCaptureStarted(CaptureEventArgs e)
	{
		switch(e.getMediaType())
		{
			case PHOTO:
				if(m_PhotoCaptureHandle != e.getCaptureHandle())
				{
					// keep handle
					Log.v(TAG, "onCaptureStarted() - Unknown capture");
					m_PhotoCaptureHandle = e.getCaptureHandle();
					
					// cancel triggering burst shots
					HandlerUtils.removeMessages(this, MSG_START_BURST_SHOTS);
				}
				break;
			case VIDEO:
				//
				break;
		}
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// setup UI
		CameraActivity cameraActivity = this.getCameraActivity();
		m_CaptureBar = ((ViewStub)cameraActivity.findViewById(R.id.capture_bar)).inflate();
		m_PrimaryButton = (Button)m_CaptureBar.findViewById(R.id.primary_capture_button);
		m_PrimaryButton.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				switch(event.getAction())
				{
					case MotionEvent.ACTION_DOWN:
						onPrimaryButtonPressed();
						break;
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						onPrimaryButtonReleased();
						break;
				}
				return false;
			}
		});
		
		// add event handlers
		cameraActivity.addHandler(CameraActivity.EVENT_CAPTURE_STARTED, new EventHandler<CaptureEventArgs>()
		{
			@Override
			public void onEventReceived(EventSource source, EventKey<CaptureEventArgs> key, CaptureEventArgs e)
			{
				onCaptureStarted(e);
			}
		});
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_PHOTO_CAPTURE_STATE, new PropertyChangedCallback<PhotoCaptureState>()
		{
			@SuppressWarnings("incomplete-switch")
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<PhotoCaptureState> key, PropertyChangeEventArgs<PhotoCaptureState> e)
			{
				switch(e.getNewValue())
				{
					case PREPARING:
					case READY:
						m_PhotoCaptureHandle = null;
						break;
				}
			}
		});
		
		HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				//getCameraActivity().setMediaType(MediaType.VIDEO);
			}
		}, 3000);
		
		HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				//getCameraActivity().setMediaType(MediaType.PHOTO);
			}
		}, 6000);
	}
	
	
	// Called when primary button pressed.
	private void onPrimaryButtonPressed()
	{
		//
		HandlerUtils.sendMessage(this, MSG_START_BURST_SHOTS, BURST_TRIGGER_THRESHOLD);
	}
	
	
	// Called when primary button released.
	private void onPrimaryButtonReleased()
	{
		/*
		if(Handle.isValid(m_VideoCaptureHandle))
			m_VideoCaptureHandle = Handle.close(m_VideoCaptureHandle);
		else
			m_VideoCaptureHandle = this.getCameraThread().captureVideo();
		*/
		// cancel triggering burst shots
		HandlerUtils.removeMessages(this, MSG_START_BURST_SHOTS);
		
		// take single shot or stop burst shots
		if(!Handle.isValid(m_PhotoCaptureHandle))
		{
			m_PhotoCaptureHandle = this.getCameraActivity().capturePhoto();
			if(!Handle.isValid(m_PhotoCaptureHandle))
				Log.e(TAG, "onPrimaryButtonReleased() - Fail to capture photo");
		}
		else if(m_IsCapturingBurstPhotos)
		{
			Log.w(TAG, "onPrimaryButtonReleased() - Stop burst shots");
			m_IsCapturingBurstPhotos = false;
			m_PhotoCaptureHandle = Handle.close(m_PhotoCaptureHandle);
		}
	}
	
	
	// Start burst shots.
	private void startBurstShots()
	{
		//
	}
}
