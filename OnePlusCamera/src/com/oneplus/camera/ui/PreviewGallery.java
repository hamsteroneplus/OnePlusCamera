package com.oneplus.camera.ui;

import java.util.LinkedList;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

final class PreviewGallery extends UIComponent implements CaptureButtons
{
	// Constants
	private static final int MSG_START_BURST_CAPTURE = 10000;
	private static final long BURST_TRIGGER_THRESHOLD = 500;
	
	
	// Private fields
	private View m_PreviewGallery;
	private ViewPager m_ViewPager;
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
	PreviewGallery(CameraActivity cameraActivity)
	{
		super("Preview Gallery", cameraActivity, true);
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
	
	

	

	
	// Initialize.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		
		// setup UI
		final CameraActivity cameraActivity = this.getCameraActivity();
		m_PreviewGallery = cameraActivity.findViewById(R.id.preview_gallery);
		m_ViewPager = (ViewPager) m_PreviewGallery.findViewById(R.id.preview_gallery_pager);
		m_ViewPager.setAdapter(new PagerAdapter(cameraActivity.getFragmentManager()));
		m_ViewPager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int state) {
				
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				
			}

			@Override
			public void onPageSelected(int position) {
				if(position == 0){
					m_PreviewGallery.setBackgroundDrawable(null);
				}else{
					m_PreviewGallery.setBackgroundColor(cameraActivity.getResources().getColor(R.color.Previerw_gallery_background));
				}
			}});
		m_ViewPager.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				cameraActivity.onTouchEvent(event);
				return false;
			}
		});
		
//		m_PrimaryButton = (Button)m_PreviewGallery.findViewById(R.id.primary_capture_button);
//		m_PrimaryButton.setOnTouchListener(new View.OnTouchListener()
//		{
//			@Override
//			public boolean onTouch(View v, MotionEvent event)
//			{
//				switch(event.getAction())
//				{
//					case MotionEvent.ACTION_DOWN:
//						onPrimaryButtonPressed();
//						break;
//					case MotionEvent.ACTION_CANCEL:
//					case MotionEvent.ACTION_UP:
//						onPrimaryButtonReleased();
//						break;
//				}
//				return false;
//			}
//		});

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
//				if(!Handle.isValid(m_PhotoCaptureHandle))
//				{
//					m_PhotoCaptureHandle = this.getCameraActivity().capturePhoto();
//					if(!Handle.isValid(m_PhotoCaptureHandle))
//						Log.e(TAG, "onPrimaryButtonReleased() - Fail to capture photo");
//				}
//				else if(m_IsCapturingBurstPhotos)
//				{
//					Log.w(TAG, "onPrimaryButtonReleased() - Stop burst shots");
//					m_IsCapturingBurstPhotos = false;
//					m_PhotoCaptureHandle = Handle.close(m_PhotoCaptureHandle);
//				}
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
	
	private static class ImageFragment extends Fragment {
		@Override
		public void onCreate(Bundle savedInstanceState)  
		{
		    super.onCreate(savedInstanceState);

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(android.R.layout.simple_gallery_item, container, false);

			return view;
		}
	}
	
    private static class PagerAdapter extends FragmentPagerAdapter {
    private static int NUM_ITEMS = 30;

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
//            case 0: // Fragment # 0 - This will show FirstFragment
//                return new Fragment();
            default:
                return new ImageFragment();
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }


}
