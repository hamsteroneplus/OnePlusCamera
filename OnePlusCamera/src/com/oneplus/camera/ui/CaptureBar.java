package com.oneplus.camera.ui;

import java.util.LinkedList;

import android.graphics.drawable.Drawable;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

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
import com.oneplus.base.Rotation;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CaptureEventArgs;
import com.oneplus.camera.CaptureHandle;
import com.oneplus.camera.FlashController;
import com.oneplus.camera.FlashMode;
import com.oneplus.camera.PhotoCaptureState;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.VideoCaptureState;
import com.oneplus.camera.Camera.LensFacing;
import com.oneplus.camera.media.MediaType;
import com.oneplus.util.ListUtils;

final class CaptureBar extends UIComponent implements CaptureButtons
{
	// Constants
	private static final int MSG_START_BURST_CAPTURE = 10000;
	private static final long BURST_TRIGGER_THRESHOLD = 500;
	
	
	// Private fields
	private View m_CaptureBar;
	private ImageButton m_FlashButton;
	private FlashController m_FlashController;
	private boolean m_IsCapturingBurstPhotos;
	private ImageButton m_MoreOptionsButton;
	private CaptureHandle m_PhotoCaptureHandle;
	private Button m_PrimaryButton;
	private final LinkedList<ButtonDrawableHandle> m_PrimaryButtonBackgroundHandles = new LinkedList<>();
	private CaptureButtonFunction m_PrimaryButtonFunction = CaptureButtonFunction.CAPTURE_PHOTO;
	private ImageButton m_SelfTimerButton;
	private ImageButton m_SwitchCameraButton;
	private CaptureHandle m_VideoCaptureHandle;
	
	
	// Constants for capture button function.
	private enum CaptureButtonFunction
	{
		CAPTURE_PHOTO,
		CAPTURE_VIDEO,
		PAUSE_RESUME_VIDEO,
	}
	
	
	// Class for button drawable.
	private final class ButtonDrawableHandle extends Handle
	{
		public final Drawable drawable;
		public final int flags;
		
		public ButtonDrawableHandle(Drawable drawable, int flags)
		{
			super("CaptureButtonDrawable");
			this.drawable = drawable;
			this.flags = flags;
		}

		@Override
		protected void onClose(int flags)
		{
			restorePrimaryButtonBackground(this);
		}
	}
	
	
	// Constructor
	CaptureBar(CameraActivity cameraActivity)
	{
		super("Capture Bar", cameraActivity, true);
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_START_BURST_CAPTURE:
				this.startBurstCapture();
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when flash button clicked.
	private void onFlashButtonClicked()
	{
		if(m_FlashController == null)
		{
			Log.e(TAG, "onFlashButtonClicked() - No flash controller");
			return;
		}
		FlashMode flashMode;
		switch(m_FlashController.get(FlashController.PROP_FLASH_MODE))
		{
			case AUTO:
				if(this.getMediaType() == MediaType.PHOTO)
					flashMode = FlashMode.ON;
				else
					flashMode = FlashMode.TORCH;
				break;
			case OFF:
				if(this.getMediaType() == MediaType.PHOTO)
					flashMode = FlashMode.AUTO;
				else
					flashMode = FlashMode.TORCH;
				break;
			default:
				flashMode = FlashMode.OFF;
				break;
		}
		m_FlashController.set(FlashController.PROP_FLASH_MODE, flashMode);
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
					HandlerUtils.removeMessages(this, MSG_START_BURST_CAPTURE);
				}
				break;
			case VIDEO:
				//
				break;
		}
	}
	
	
	// Initialize.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_FlashController = this.findComponent(FlashController.class);
		
		// setup UI
		CameraActivity cameraActivity = this.getCameraActivity();
		m_CaptureBar = cameraActivity.findViewById(R.id.capture_bar);
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
		m_FlashButton = (ImageButton)m_CaptureBar.findViewById(R.id.flash_button);
		m_FlashButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onFlashButtonClicked();
			}
		});
		m_MoreOptionsButton = (ImageButton)m_CaptureBar.findViewById(R.id.more_options_button);
		m_SelfTimerButton = (ImageButton)m_CaptureBar.findViewById(R.id.self_timer_button);
		m_SwitchCameraButton = (ImageButton)m_CaptureBar.findViewById(R.id.switch_camera_button);
		m_SwitchCameraButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onSwitchCameraButtonClicked();
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
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, new PropertyChangedCallback<Camera>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Camera> key, PropertyChangeEventArgs<Camera> e)
			{
				updateSwitchCameraButton(e.getNewValue());
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_IS_CAMERA_LOCKED, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				updateSwitchCameraButton();
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, new PropertyChangedCallback<MediaType>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<MediaType> key, PropertyChangeEventArgs<MediaType> e)
			{
				updateButtonFunctions(true);
			}
		});
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
		cameraActivity.addCallback(CameraActivity.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				updateButtonFunctions(true);
			}
		});
		if(m_FlashController != null)
		{
			PropertyChangedCallback callback = new PropertyChangedCallback()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
				{
					updateFlashButton();
				}
			};
			m_FlashController.addCallback(FlashController.PROP_FLASH_MODE, callback);
			m_FlashController.addCallback(FlashController.PROP_HAS_FLASH, callback);
			m_FlashController.addCallback(FlashController.PROP_IS_FLASH_DISABLED, callback);
		}
		
		// setup initial button states
		this.updateButtonFunctions(true);
		
		// setup initial UI rotation
		Rotation rotation = this.getRotation();
		this.rotateView(m_PrimaryButton, rotation, 0);
		this.rotateView(m_FlashButton, rotation, 0);
		this.rotateView(m_MoreOptionsButton, rotation, 0);
		this.rotateView(m_SelfTimerButton, rotation, 0);
		this.rotateView(m_SwitchCameraButton, rotation, 0);
		
		// setup button initial states
		this.updateFlashButton();
		this.updateSwitchCameraButton();
	}
	
	
	// Called when primary button pressed.
	@SuppressWarnings("incomplete-switch")
	private void onPrimaryButtonPressed()
	{
		switch(m_PrimaryButtonFunction)
		{
			case CAPTURE_PHOTO:
				HandlerUtils.sendMessage(this, MSG_START_BURST_CAPTURE, BURST_TRIGGER_THRESHOLD);
				break;
		}
	}
	
	
	// Called when primary button released.
	private void onPrimaryButtonReleased()
	{
		// cancel triggering burst shots
		HandlerUtils.removeMessages(this, MSG_START_BURST_CAPTURE);
		
		// trigger capture
		switch(m_PrimaryButtonFunction)
		{
			case CAPTURE_PHOTO:
			{
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
				break;
			}
			case CAPTURE_VIDEO:
				switch(this.getCameraActivity().get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
				{
					case READY:
						m_VideoCaptureHandle = this.getCameraActivity().captureVideo();
						if(!Handle.isValid(m_VideoCaptureHandle))
							Log.e(TAG, "onPrimaryButtonReleased() - Fail to capture video");
						break;
					case PAUSED:
					case CAPTURING:
						m_VideoCaptureHandle = Handle.close(m_VideoCaptureHandle);
						break;
					default:
						break;
				}
				break;
			case PAUSE_RESUME_VIDEO:
				//
				break;
		}
	}
	
	
	// Called when rotation changed.
	@Override
	protected void onRotationChanged(Rotation prevRotation, Rotation newRotation)
	{
		// call super
		super.onRotationChanged(prevRotation, newRotation);
		
		// rotate buttons
		this.rotateView(m_PrimaryButton, newRotation);
		this.rotateView(m_FlashButton, newRotation);
		this.rotateView(m_MoreOptionsButton, newRotation);
		this.rotateView(m_SelfTimerButton, newRotation);
		this.rotateView(m_SwitchCameraButton, newRotation);
	}
	
	
	// Called when camera switch button clicked.
	private void onSwitchCameraButtonClicked()
	{
		// check state
		//
		
		// switch camera
		if(!this.getCameraActivity().switchCamera())
			Log.e(TAG, "onSwitchCameraButtonClicked() - Fail to switch camera");
	}
	
	
	// Restore background of primary capture button.
	private void restorePrimaryButtonBackground(ButtonDrawableHandle handle)
	{
		// check thread
		this.verifyAccess();
		
		// remove handle
		boolean isLastHandle = ListUtils.isLastObject(m_PrimaryButtonBackgroundHandles, handle);
		if(!m_PrimaryButtonBackgroundHandles.remove(handle))
			return;
		
		// update buttons
		if(isLastHandle)
			this.updateButtonBackgrounds();
	}
	
	
	// Change background of primary capture button.
	@Override
	public Handle setPrimaryButtonBackground(Drawable drawable, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setPrimaryButtonBackground() - Component is not running");
			return null;
		}
		
		// create handle
		ButtonDrawableHandle handle = new ButtonDrawableHandle(drawable, flags);
		
		// update button
		this.updateButtonBackgrounds();
		return handle;
	}
	
	
	// Start burst capture.
	private void startBurstCapture()
	{
		// check state
		CameraActivity cameraActivity = this.getCameraActivity();
		PhotoCaptureState photoCaptureState = cameraActivity.get(CameraActivity.PROP_PHOTO_CAPTURE_STATE);
		VideoCaptureState videoCaptureState = cameraActivity.get(CameraActivity.PROP_VIDEO_CAPTURE_STATE);
		if(photoCaptureState != PhotoCaptureState.READY)
		{
			Log.e(TAG, "startBurstCapture() - Photo capture state is " + photoCaptureState);
			return;
		}
		if(videoCaptureState != VideoCaptureState.READY && videoCaptureState != VideoCaptureState.PREPARING)
		{
			Log.e(TAG, "startBurstCapture() - Video capture state is " + videoCaptureState);
			return;
		}
		
		Log.v(TAG, "startBurstCapture()");
		
		// start burst capture
		m_PhotoCaptureHandle = this.getCameraActivity().capturePhoto(-1);
		if(!Handle.isValid(m_PhotoCaptureHandle))
		{
			Log.e(TAG, "startBurstCapture() - Fail to capture photo");
			return;
		}
		m_IsCapturingBurstPhotos = true;
	}
	
	
	// Update capture button background.
	private void updateButtonBackgrounds()
	{
		// update primary button
		if(m_PrimaryButton != null)
		{
			if(m_PrimaryButtonBackgroundHandles.isEmpty())
			{
				switch(m_PrimaryButtonFunction)
				{
					case CAPTURE_PHOTO:
						m_PrimaryButton.setBackgroundResource(R.drawable.capture_button_background);
						break;
					case CAPTURE_VIDEO:
						switch(this.getCameraActivity().get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
						{
							case CAPTURING:
							case STOPPING:
								m_PrimaryButton.setBackgroundResource(R.drawable.capture_button_video_recording);
								break;
							default:
								m_PrimaryButton.setBackgroundResource(R.drawable.capture_button_video);
								break;
						}
						break;
					case PAUSE_RESUME_VIDEO:
						//
						break;
				}
			}
			else
				m_PrimaryButton.setBackground(m_PrimaryButtonBackgroundHandles.getLast().drawable);
		}
	}
	
	
	// Update capture button functions.
	private void updateButtonFunctions(boolean updateBackground)
	{
		//CameraActivity cameraActivity = this.getCameraActivity();
		switch(this.getMediaType())
		{
			case PHOTO:
				m_PrimaryButtonFunction = CaptureButtonFunction.CAPTURE_PHOTO;
				break;
			case VIDEO:
				m_PrimaryButtonFunction = CaptureButtonFunction.CAPTURE_VIDEO;
				break;
		}
		if(updateBackground)
			this.updateButtonBackgrounds();
	}
	
	
	// Update flash button.
	private void updateFlashButton()
	{
		// check state
		if(m_FlashButton == null)
			return;
		if(m_FlashController == null)
		{
			Log.e(TAG, "updateFlashButton() - No flash controller");
			this.setViewVisibility(m_FlashButton, false);
			return;
		}
		
		// check flash function
		if(!m_FlashController.get(FlashController.PROP_HAS_FLASH) || m_FlashController.get(FlashController.PROP_IS_FLASH_DISABLED))
		{
			this.setViewVisibility(m_FlashButton, false);
			return;
		}
		else
			this.setViewVisibility(m_FlashButton, true);
		
		// update icon
		switch(m_FlashController.get(FlashController.PROP_FLASH_MODE))
		{
			case AUTO:
				m_FlashButton.setImageResource(R.drawable.flash_auto);
				break;
			case ON:
			case TORCH:
				m_FlashButton.setImageResource(R.drawable.flash_on);
				break;
			default:
				m_FlashButton.setImageResource(R.drawable.flash_off);
				break;
		}
	}
	
	
	// Update switch camera button.
	private void updateSwitchCameraButton()
	{
		this.updateSwitchCameraButton(this.getCamera());
	}
	private void updateSwitchCameraButton(Camera camera)
	{
		if(m_SwitchCameraButton == null)
			return;
		this.setViewVisibility(m_SwitchCameraButton, !this.getCameraActivity().get(CameraActivity.PROP_IS_CAMERA_LOCKED));
		if(camera == null || camera.get(Camera.PROP_LENS_FACING) == LensFacing.BACK)
			m_SwitchCameraButton.setImageResource(R.drawable.switch_camera);
		else
			m_SwitchCameraButton.setImageResource(R.drawable.switch_camera_on);
	}
}
