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
	private OptionsPanel m_OptionsPanel;
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
		// check state
		if(m_FlashController == null)
		{
			Log.e(TAG, "onFlashButtonClicked() - No flash controller");
			return;
		}
		if(!this.isCaptureUIEnabled())
			return;
		
		// switch flash mode
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
		m_OptionsPanel = this.findComponent(OptionsPanel.class);
		
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
		m_MoreOptionsButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onMoreOptionsButtonClicked();
			}
		});
		m_SelfTimerButton = (ImageButton)m_CaptureBar.findViewById(R.id.self_timer_button);
		m_SelfTimerButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onSelfTimerButtonClicked();
			}
		});
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
		cameraActivity.addCallback(CameraActivity.PROP_IS_SELF_TIMER_STARTED, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				updateButtonBackgrounds();
				updateFlashButton();
				updateMoreOptionsButton();
				updateSelfTimerButton();
				updateSwitchCameraButton();
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, new PropertyChangedCallback<MediaType>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<MediaType> key, PropertyChangeEventArgs<MediaType> e)
			{
				updateButtonFunctions(true);
				updateSelfTimerButton();
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
		cameraActivity.addCallback(CameraActivity.PROP_SELF_TIMER_INTERVAL, new PropertyChangedCallback<Long>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Long> key, PropertyChangeEventArgs<Long> e)
			{
				updateSelfTimerButton(e.getNewValue());
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
		if(m_OptionsPanel != null)
		{
			m_OptionsPanel.addCallback(OptionsPanel.PROP_HAS_ITEMS, new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					updateMoreOptionsButton();
				}
			});
			m_OptionsPanel.addCallback(OptionsPanel.PROP_IS_VISIBLE, new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					updateMoreOptionsButton(e.getNewValue());
				}
			});
		}
		
		// setup initial button states
		this.updateButtonFunctions(true);
		
		// setup initial UI rotation
		this.addAutoRotateView(m_PrimaryButton);
		this.addAutoRotateView(m_FlashButton);
		this.addAutoRotateView(m_MoreOptionsButton);
		this.addAutoRotateView(m_SelfTimerButton);
		this.addAutoRotateView(m_SwitchCameraButton);
		
		// setup button initial states
		this.updateFlashButton();
		this.updateMoreOptionsButton();
		this.updateSelfTimerButton();
		this.updateSwitchCameraButton();
	}
	
	
	// Called when more options button clicked.
	private void onMoreOptionsButtonClicked()
	{
		// check state
		if(m_OptionsPanel == null)
			return;
		if(!this.isCaptureUIEnabled())
			return;
		
		// open or close panel
		if(m_OptionsPanel.get(OptionsPanel.PROP_IS_VISIBLE))
			m_OptionsPanel.closeOptionsPanel(0);
		else
			m_OptionsPanel.openOptionsPanel(0);
	}
	
	
	// Called when primary button pressed.
	@SuppressWarnings("incomplete-switch")
	private void onPrimaryButtonPressed()
	{
		if(!this.isCaptureUIEnabled())
			return;
		switch(m_PrimaryButtonFunction)
		{
			case CAPTURE_PHOTO:
				if(this.getCameraActivity().get(CameraActivity.PROP_IS_SELF_TIMER_STARTED))
					Log.v(TAG, "onPrimaryButtonPressed() - Self timer is started");
				else
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
					if(!this.isCaptureUIEnabled())
						return;
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
				else if(this.getCameraActivity().get(CameraActivity.PROP_IS_SELF_TIMER_STARTED))
				{
					Log.v(TAG, "onPrimaryButtonReleased() - Stop self timer");
					m_PhotoCaptureHandle = Handle.close(m_PhotoCaptureHandle);
				}
				break;
			}
			case CAPTURE_VIDEO:
				if(!this.isCaptureUIEnabled())
					return;
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
	
	
	// Called when self timer button clicked.
	private void onSelfTimerButtonClicked()
	{
		// check state
		if(!this.isCaptureUIEnabled())
			return;
		
		// switch self timer
		long seconds = this.getCameraActivity().get(CameraActivity.PROP_SELF_TIMER_INTERVAL);
		if(seconds == 0L)
			seconds = 3L;
		else if(seconds == 3L)
			seconds = 5L;
		else if(seconds == 5L)
			seconds = 10L;
		else
			seconds = 0L;
		this.getCameraActivity().set(CameraActivity.PROP_SELF_TIMER_INTERVAL, seconds);
	}
	
	
	// Called when camera switch button clicked.
	private void onSwitchCameraButtonClicked()
	{
		// check state
		if(!this.isCaptureUIEnabled())
			return;
		
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
						if(this.getCameraActivity().get(CameraActivity.PROP_IS_SELF_TIMER_STARTED))
							m_PrimaryButton.setBackgroundResource(R.drawable.capture_button_stop_self_timer);
						else
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
		
		// update visibility
		CameraActivity cameraActivity = this.getCameraActivity();
		if(!m_FlashController.get(FlashController.PROP_HAS_FLASH) 
				|| m_FlashController.get(FlashController.PROP_IS_FLASH_DISABLED)
				|| cameraActivity.get(CameraActivity.PROP_IS_SELF_TIMER_STARTED))
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
	
	
	// Update more options button.
	private void updateMoreOptionsButton()
	{
		this.updateMoreOptionsButton(m_OptionsPanel != null && m_OptionsPanel.get(OptionsPanel.PROP_IS_VISIBLE));
	}
	private void updateMoreOptionsButton(boolean isPanelVisible)
	{
		// check state
		if(m_MoreOptionsButton == null)
			return;
		
		// update visibility
		CameraActivity cameraActivity = this.getCameraActivity();
		if(cameraActivity.get(CameraActivity.PROP_IS_SELF_TIMER_STARTED)
				|| m_OptionsPanel == null
				|| !m_OptionsPanel.get(OptionsPanel.PROP_HAS_ITEMS))
		{
			this.setViewVisibility(m_MoreOptionsButton, false);
			return;
		}
		else
			this.setViewVisibility(m_MoreOptionsButton, true);
		
		// update icon
		if(isPanelVisible)
			m_MoreOptionsButton.setImageResource(R.drawable.more_options_on);
		else
			m_MoreOptionsButton.setImageResource(R.drawable.more_options);
	}
	
	
	// Update self timer button.
	private void updateSelfTimerButton()
	{
		this.updateSelfTimerButton(this.getCameraActivity().get(CameraActivity.PROP_SELF_TIMER_INTERVAL));
	}
	private void updateSelfTimerButton(long seconds)
	{
		// check state
		if(m_SelfTimerButton == null)
			return;
		
		// update visibility
		CameraActivity cameraActivity = this.getCameraActivity();
		if(cameraActivity.get(CameraActivity.PROP_IS_SELF_TIMER_STARTED)
				|| this.getMediaType() != MediaType.PHOTO)
		{
			this.setViewVisibility(m_SelfTimerButton, false);
			return;
		}
		else
			this.setViewVisibility(m_SelfTimerButton, true);
		
		// update icon
		int resId;
		if(seconds == 3L)
			resId = R.drawable.self_timer_3s_on;
		else if(seconds == 5L)
			resId = R.drawable.self_timer_5s_on;
		else if(seconds == 10L)
			resId = R.drawable.self_timer_10s_on;
		else if(seconds > 0)
			resId = R.drawable.self_timer_on;
		else
			resId = R.drawable.self_timer_off;
		m_SelfTimerButton.setImageResource(resId);
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
		CameraActivity cameraActivity = this.getCameraActivity();
		if(cameraActivity.get(CameraActivity.PROP_IS_SELF_TIMER_STARTED)
				|| cameraActivity.get(CameraActivity.PROP_IS_CAMERA_LOCKED))
		{
			this.setViewVisibility(m_SwitchCameraButton, false);
		}
		else
			this.setViewVisibility(m_SwitchCameraButton, true);
		if(camera == null || camera.get(Camera.PROP_LENS_FACING) == LensFacing.BACK)
			m_SwitchCameraButton.setImageResource(R.drawable.switch_camera);
		else
			m_SwitchCameraButton.setImageResource(R.drawable.switch_camera_on);
	}
}
