package com.oneplus.camera;

import android.os.Message;
import android.os.SystemClock;

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
import com.oneplus.base.BaseActivity.State;
import com.oneplus.camera.ui.TouchAutoFocusUI;

final class SensorFocusControllerImpl extends CameraComponent
{
	// Constants.
	private static final long DURATION_SENSOR_AF_AFTER_TOUCH_AF = 3000;
	private static final long DURATION_START_SENSOR_AF = 500;
	private static final float STABLE_THRESHOLD = 1.5f;
	private static final int MSG_START_AF = 10000;
	
	
	// Private fields.
	private SensorAfState m_AfState = SensorAfState.UNSTABLE;
	private ExposureController m_ExposureController;
	private FocusController m_FocusController;
	private boolean m_IsAEResetNeeded;
	private final float[] m_LastAccelerometerValues = new float[3];
	private long m_LastTouchAFTime;
	private TouchAutoFocusUI m_TouchAutoFocusUI;
	
	
	// Sensor focus state.
	private enum SensorAfState
	{
		UNSTABLE,
		STABLE_BEFORE_AF,
		STABLE_WITH_AF,
	}
	
	
	// Constructor.
	SensorFocusControllerImpl(CameraActivity cameraActivity)
	{
		super("Sensor AF Controller", cameraActivity, true);
	}
	
	
	// Check current state for sensor focus.
	private boolean canSensorFocus()
	{
		// check activity state
		CameraActivity cameraActivity = this.getCameraActivity();
		if(cameraActivity.get(CameraActivity.PROP_STATE) != State.RUNNING)
			return false;
		
		// check preview state
		if(!cameraActivity.get(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED))
			return false;
		
		// check touch state
		if(cameraActivity.get(CameraActivity.PROP_IS_TOUCHING_ON_SCREEN))
			return false;
		
		// check touch AF state
		if((SystemClock.elapsedRealtime() - m_LastTouchAFTime) < DURATION_SENSOR_AF_AFTER_TOUCH_AF)
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
			case MSG_START_AF:
			{
				if(m_AfState == SensorAfState.STABLE_BEFORE_AF)
				{
					if(this.startAutoFocus())
						m_AfState = SensorAfState.STABLE_WITH_AF;
				}
				break;
			}
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when accelerometer values changed.
	private void onAccelerometerValuesChanged(float[] values)
	{
		// check value differences
		boolean isMoved = (Math.abs(m_LastAccelerometerValues[0] - values[0]) >= STABLE_THRESHOLD
				|| Math.abs(m_LastAccelerometerValues[1] - values[1]) >= STABLE_THRESHOLD
				|| Math.abs(m_LastAccelerometerValues[2] - values[2]) >= STABLE_THRESHOLD);
		
		// save values
		System.arraycopy(values, 0, m_LastAccelerometerValues, 0, 3);
		
		// change state
		if(isMoved)
			this.resetAfState();
		else if(m_AfState == SensorAfState.UNSTABLE)
		{
			m_AfState = SensorAfState.STABLE_BEFORE_AF;
			this.getHandler().sendEmptyMessageDelayed(MSG_START_AF, DURATION_START_SENSOR_AF);
		}
		else if(m_AfState == SensorAfState.STABLE_BEFORE_AF && !this.getHandler().hasMessages(MSG_START_AF))
		{
			if(this.startAutoFocus())
				m_AfState = SensorAfState.STABLE_WITH_AF;
		}
	}
	
	
	// Reset sensor AF state.
	private void resetAfState()
	{
		m_AfState = SensorAfState.UNSTABLE;
		m_IsAEResetNeeded = false;
		this.getHandler().removeMessages(MSG_START_AF);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_ExposureController = this.findComponent(ExposureController.class);
		m_FocusController = this.findComponent(FocusController.class);
		m_TouchAutoFocusUI = this.findComponent(TouchAutoFocusUI.class);
		
		// add event handlers
		CameraActivity cameraActivity = this.getCameraActivity();
		if(m_TouchAutoFocusUI != null)
		{
			m_TouchAutoFocusUI.addHandler(TouchAutoFocusUI.EVENT_TOUCH_AF, new EventHandler<EventArgs>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e)
				{
					m_LastTouchAFTime = SystemClock.elapsedRealtime();
					resetAfState();
				}
			});
		}
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_ACCELEROMETER_VALUES, new PropertyChangedCallback<float[]>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<float[]> key, PropertyChangeEventArgs<float[]> e)
			{
				onAccelerometerValuesChanged(e.getNewValue());
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_IS_TOUCHING_ON_SCREEN, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				if(!e.getNewValue())
				{
					resetAfState();
					m_LastTouchAFTime = SystemClock.elapsedRealtime();
				}
			}
		});
		if(m_FocusController != null)
		{
			m_FocusController.addCallback(FocusController.PROP_FOCUS_STATE, new PropertyChangedCallback<FocusState>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<FocusState> key, PropertyChangeEventArgs<FocusState> e)
				{
					// reset AE
					if(m_AfState == SensorAfState.STABLE_WITH_AF 
							&& m_IsAEResetNeeded
							&& e.getNewValue() == FocusState.SCANNING 
							&& m_ExposureController != null)
					{
						m_ExposureController.set(ExposureController.PROP_AE_REGIONS, null);
						m_ExposureController.set(ExposureController.PROP_EXPOSURE_COMPENSATION, 0f);
						m_IsAEResetNeeded = false;
					}
				}
			});
		}
	}
	
	// Start auto focus.
	private boolean startAutoFocus()
	{
		// check touch AF state
		if((SystemClock.elapsedRealtime() - m_LastTouchAFTime) < DURATION_SENSOR_AF_AFTER_TOUCH_AF)
			return true;
		
		// check focus lock
		if(m_FocusController.get(FocusController.PROP_IS_FOCUS_LOCKED))
			return true;
		
		// check state
		if(!this.canSensorFocus())
			return false;
		
		// update state
		m_IsAEResetNeeded = true;
		
		// check focus mode
		if(m_FocusController.get(FocusController.PROP_FOCUS_MODE) == FocusMode.CONTINUOUS_AF 
				&& m_FocusController.get(FocusController.PROP_AF_REGIONS).isEmpty())
		{
			Log.v(TAG, "startAutoFocus() - Already performing continuous AF, skip sensor AF");
			return true;
		}
		
		Log.v(TAG, "startAutoFocus()");
		
		// start AF
		Handle handle = m_FocusController.startAutoFocus(null, FocusController.FLAG_CONTINOUS_AF);
		if(!Handle.isValid(handle))
		{
			Log.e(TAG, "startAutoFocus() - Fail to start sensor AF");
			return false;
		}
		
		// reset AE
		if(m_ExposureController != null)
		{
			m_ExposureController.set(ExposureController.PROP_AE_REGIONS, null);
			m_ExposureController.set(ExposureController.PROP_EXPOSURE_COMPENSATION, 0f);
			m_IsAEResetNeeded = false;
		}
		
		// complete
		return true;
	}
}
