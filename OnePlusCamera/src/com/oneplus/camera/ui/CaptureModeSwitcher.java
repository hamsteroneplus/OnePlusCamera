package com.oneplus.camera.ui;

import java.util.List;

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.base.ScreenSize;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.capturemode.CaptureMode;
import com.oneplus.camera.capturemode.CaptureModeManager;

class CaptureModeSwitcher extends UIComponent
{
	// Constants
	private static final long DURATION_SWIPE_TO_SWITCH_MODE = 800;
	private static final float RATIO_CORRECT_DIRECTION_THRESHOLD = 0.25f;
	private static final float RATIO_INCORRECT_DIRECTION_THRESHOLD = 0.1f;
	
	
	// Private fields.
	private View m_BaseView;
	private PointF m_BaseViewTouchDownPosition;
	private long m_BaseViewTouchDownTime;
	private CaptureModeManager m_CaptureModeManager;
	private float m_CorrectSwipeDirectionThreshold;
	private float m_IncorrectSwipeDirectionThreshold;
	
	
	// Constructor
	CaptureModeSwitcher(CameraActivity cameraActivity)
	{
		super("Capture Mode Switcher", cameraActivity, true);
	}
	
	
	// Handle touch event on base view.
	private boolean onBaseViewTouch(MotionEvent event)
	{
		switch(event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				m_BaseViewTouchDownPosition = new PointF(event.getX(), event.getY());
				m_BaseViewTouchDownTime = SystemClock.elapsedRealtime();
				return true;
				
			case MotionEvent.ACTION_MOVE:
				if(m_BaseViewTouchDownPosition != null)
				{
					// check duration
					if((SystemClock.elapsedRealtime() - m_BaseViewTouchDownTime) >= DURATION_SWIPE_TO_SWITCH_MODE)
					{
						Log.v(TAG, "onBaseViewTouch() - Timeout");
						m_BaseViewTouchDownPosition = null;
						break;
					}
					
					// check direction
					float diffX = (event.getX() - m_BaseViewTouchDownPosition.x);
					float diffY = (event.getY() - m_BaseViewTouchDownPosition.y);
					Rotation activityRotation = this.getCameraActivityRotation();
					Rotation rotation = this.getRotation();
					if(rotation.isLandscape() == activityRotation.isLandscape())
					{
						if(Math.abs(diffX) >= m_IncorrectSwipeDirectionThreshold)
						{
							Log.v(TAG, "onBaseViewTouch() - Incorrect direction");
							m_BaseViewTouchDownPosition = null;
							break;
						}
						int direction;
						if(diffY >= m_CorrectSwipeDirectionThreshold)
							direction = 1;
						else if(diffY <= -m_CorrectSwipeDirectionThreshold)
							direction = -1;
						else
							direction = 0;
						if(rotation != activityRotation)
							direction = -direction;
						if(direction != 0)
						{
							m_BaseViewTouchDownPosition = null;
							this.switchCaptureMode(direction);
							break;
						}
					}
					else
					{
						if(Math.abs(diffY) >= m_IncorrectSwipeDirectionThreshold)
						{
							Log.v(TAG, "onBaseViewTouch() - Incorrect direction");
							m_BaseViewTouchDownPosition = null;
							break;
						}
						int direction;
						if(diffX >= m_CorrectSwipeDirectionThreshold)
							direction = 1;
						else if(diffX <= -m_CorrectSwipeDirectionThreshold)
							direction = -1;
						else
							direction = 0;
						switch(activityRotation)
						{
							case LANDSCAPE:
								if(rotation == Rotation.INVERSE_PORTRAIT)
									direction = -direction;
							case PORTRAIT:
								if(rotation == Rotation.LANDSCAPE)
									direction = -direction;
								break;
							case INVERSE_LANDSCAPE:
								if(rotation == Rotation.PORTRAIT)
									direction = -direction;
								break;
							case INVERSE_PORTRAIT:
								if(rotation == Rotation.INVERSE_LANDSCAPE)
									direction = -direction;
								break;
						}
						if(direction != 0)
						{
							m_BaseViewTouchDownPosition = null;
							this.switchCaptureMode(direction);
							break;
						}
					}
					
					// complete
					return true;
				}
				break;
				
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				m_BaseViewTouchDownPosition = null;
				break;
		}
		return false;
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// setup base view
		CameraActivity cameraActivity = this.getCameraActivity();
		m_BaseView = ((MainActivity)cameraActivity).getCaptureUIContainer().findViewById(R.id.capture_mode_switcher_container);
		m_BaseView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				return onBaseViewTouch(event);
			}
		});
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_SCREEN_SIZE, new PropertyChangedCallback<ScreenSize>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<ScreenSize> key, PropertyChangeEventArgs<ScreenSize> e)
			{
				updateDirectionThreshold(e.getNewValue());
			}
		});
		
		// setup swipe thresholds
		this.updateDirectionThreshold(cameraActivity.get(CameraActivity.PROP_SCREEN_SIZE));
	}
	
	
	// Switch capture mode.
	private boolean switchCaptureMode(int direction)
	{
		// check parameter
		if(direction == 0)
			return true;
		
		Log.v(TAG, "switchCaptureMode() - Direction : ", direction);
		
		// check capture mode manager
		if(m_CaptureModeManager == null)
		{
			m_CaptureModeManager = this.findComponent(CaptureModeManager.class);
			if(m_CaptureModeManager == null)
			{
				Log.e(TAG, "switchCaptureMode() - No capture mode manager");
				return false;
			}
		}
		
		// check capture mode
		CaptureMode captureMode = m_CaptureModeManager.get(CaptureModeManager.PROP_CAPTURE_MODE);
		List<CaptureMode> captureModeList = m_CaptureModeManager.get(CaptureModeManager.PROP_CAPTURE_MODE_LIST);
		int index = captureModeList.indexOf(captureMode);
		if(index < 0)
			Log.w(TAG, "switchCaptureMode() - Unknown current capture mode : " + captureMode);
		
		// select next capture mode
		index += direction;
		if(index < 0)
		{
			Log.v(TAG, "switchCaptureMode() - Current capture mode is the first one");
			return false;
		}
		if(index >= captureModeList.size())
		{
			Log.v(TAG, "switchCaptureMode() - Current capture mode is the last one");
			return false;
		}
		if(!m_CaptureModeManager.setCaptureMode(captureModeList.get(index), 0))
		{
			Log.e(TAG, "switchCaptureMode() - Fail to change capture mode");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Refresh thresholds for swiping.
	private void updateDirectionThreshold(ScreenSize screenSize)
	{
		int length = Math.min(screenSize.getWidth(), screenSize.getHeight());
		m_CorrectSwipeDirectionThreshold = (length * RATIO_CORRECT_DIRECTION_THRESHOLD);
		m_IncorrectSwipeDirectionThreshold = (length * RATIO_INCORRECT_DIRECTION_THRESHOLD);
		m_BaseViewTouchDownPosition = null;
	}
}
