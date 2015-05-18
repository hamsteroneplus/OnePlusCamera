package com.oneplus.camera.ui;

import java.util.Arrays;

import android.graphics.PointF;
import android.os.Message;
import android.view.MotionEvent;

import com.oneplus.base.BaseActivity.State;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.ScreenSize;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.FocusController;
import com.oneplus.camera.PhotoCaptureState;
import com.oneplus.camera.Camera.MeteringRect;

final class TouchFocusExposureUI extends CameraComponent implements TouchAutoFocusUI, TouchAutoExposureUI
{
	// Constants.
	private static final long DURATION_AF_LOCK_THREAHOLD = 500;
	private static final long DURATION_START_AF_THREAHOLD = 500;
	private static final float AF_REGION_WIDTH = 0.25f;
	private static final float AF_REGION_HEIGHT = 0.25f;
	private static final float TOUCH_AF_DISTANCE_THRESHOLD = 0.2f;
	private static final int MSG_START_AF = 10000;
	private static final int MSG_LOCK_AF = 10001;
	
	
	// Private fields.
	private FocusController m_FocusController;
	private float m_TouchAfDistanceThreshold;
	private Handle m_TouchAfHandle;
	private final PointF m_TouchDownPosition = new PointF(-1, -1);
	
	
	// Constructor.
	TouchFocusExposureUI(CameraActivity cameraActivity)
	{
		super("Touch AE/AF UI", cameraActivity, true);
	}
	
	
	// Bind to FocusController.
	private boolean bindToFocusController()
	{
		if(m_FocusController != null)
			return true;
		m_FocusController = this.findComponent(FocusController.class);
		if(m_FocusController == null)
			return false;
		//
		return true;
	}
	
	
	// Check current state for touch focus.
	private boolean canTouchFocus()
	{
		// check activity state
		CameraActivity cameraActivity = this.getCameraActivity();
		if(cameraActivity.get(CameraActivity.PROP_STATE) != State.RUNNING)
			return false;
		
		// check preview state
		if(!cameraActivity.get(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED))
			return false;
		
		// check capture state
		switch(cameraActivity.get(CameraActivity.PROP_MEDIA_TYPE))
		{
			case PHOTO:
				if(cameraActivity.get(CameraActivity.PROP_PHOTO_CAPTURE_STATE) != PhotoCaptureState.READY)
					return false;
				break;
				
			case VIDEO:
				switch(cameraActivity.get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
				{
					case READY:
					case CAPTURING:
					case PAUSED:
						break;
					default:
						return false;
				}
				break;
		}
		
		// OK
		return true;
	}
	
	
	// Handle messages.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_LOCK_AF:
				//
				break;
				
			case MSG_START_AF:
				this.startAutoFocus();
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add event handlers
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.addHandler(CameraActivity.EVENT_TOUCH, new EventHandler<MotionEventArgs>()
		{
			@Override
			public void onEventReceived(EventSource source, EventKey<MotionEventArgs> key, MotionEventArgs e)
			{
				onTouch(e);
			}
		});
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_SCREEN_SIZE, new PropertyChangedCallback<ScreenSize>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<ScreenSize> key, PropertyChangeEventArgs<ScreenSize> e)
			{
				updateDistanceThresholds(e.getNewValue());
			}
		});
		
		// setup touch AF threshold
		this.updateDistanceThresholds(this.getScreenSize());
	}
	
	
	// Handle touch event.
	private void onTouch(MotionEventArgs e)
	{
		// check event
		if(e.isHandled())
		{
			this.getHandler().removeMessages(MSG_START_AF);
			return;
		}
		
		// handle event
		switch(e.getAction())
		{
			case MotionEvent.ACTION_DOWN:
			{
				PointF focusCenter = new PointF();
				if(!this.getCameraActivity().getViewfinder().pointToPreview(e.getX(), e.getY(), focusCenter, 0))
					return;
				m_TouchDownPosition.x = e.getX();
				m_TouchDownPosition.y = e.getY();
				this.getHandler().sendEmptyMessageDelayed(MSG_START_AF, DURATION_START_AF_THREAHOLD);
				break;
			}
			
			case MotionEvent.ACTION_MOVE:
			{
				if(m_TouchDownPosition.x < 0 || m_TouchDownPosition.y < 0)
					break;
				float diffX = Math.abs(e.getX() - m_TouchDownPosition.x);
				float diffY = Math.abs(e.getY() - m_TouchDownPosition.y);
				if((diffX * diffX + diffY * diffY) > (m_TouchAfDistanceThreshold * m_TouchAfDistanceThreshold))
					m_TouchDownPosition.set(-1, -1);
				break;
			}
				
			case MotionEvent.ACTION_CANCEL:
				this.getHandler().removeMessages(MSG_START_AF);
				m_TouchDownPosition.set(-1, -1);
				break;
				
			case MotionEvent.ACTION_UP:
				if(this.getHandler().hasMessages(MSG_START_AF))
				{
					this.getHandler().removeMessages(MSG_START_AF);
					this.startAutoFocus();
				}
				m_TouchDownPosition.set(-1, -1);
				break;
		}
	}
	
	
	// Start auto focus.
	private void startAutoFocus()
	{
		// check state
		if(!this.bindToFocusController())
			return;
		if(!this.canTouchFocus())
			return;
		if(m_TouchDownPosition.x < 0 || m_TouchDownPosition.y < 0)
			return;
		
		// calculate focus position
		PointF focusCenter = new PointF();
		if(!this.getCameraActivity().getViewfinder().pointToPreview(m_TouchDownPosition.x, m_TouchDownPosition.y, focusCenter, 0))
			return;
		float left = (focusCenter.x - (AF_REGION_WIDTH / 2));
		float top = (focusCenter.y - (AF_REGION_HEIGHT / 2));
		float right = (left + AF_REGION_WIDTH);
		float bottom = (top + AF_REGION_HEIGHT);
		if(left < 0)
			left = 0;
		if(top < 0)
			top = 0;
		if(right > 1)
			right = 1;
		if(bottom > 1)
			bottom = 1;
		MeteringRect focusRect = new MeteringRect(left, top, right, bottom, 1);
		
		// cancel previous AF
		m_TouchAfHandle = Handle.close(m_TouchAfHandle);
		
		// start AF
		m_TouchAfHandle = m_FocusController.startAutoFocus(Arrays.asList(focusRect), FocusController.FLAG_SINGLE_AF);
		if(!Handle.isValid(m_TouchAfHandle))
		{
			Log.e(TAG, "startAutoFocus() - Fail to start touch AF");
			return;
		}
		
		// raise event
		this.raise(EVENT_TOUCH_AF, EventArgs.EMPTY);
		this.raise(EVENT_TOUCH_AE, EventArgs.EMPTY);
	}
	
	
	// Update touch AF threshold.
	private void updateDistanceThresholds(ScreenSize screenSize)
	{
		int length = Math.min(screenSize.getWidth(), screenSize.getHeight());
		m_TouchAfDistanceThreshold = (length * TOUCH_AF_DISTANCE_THRESHOLD);
	}
}
