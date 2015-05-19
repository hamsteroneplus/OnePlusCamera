package com.oneplus.camera.ui;

import java.util.Locale;

import android.view.ViewStub;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.VideoCaptureState;
import com.oneplus.camera.widget.RotateRelativeLayout;

final class RecordingTimerUI extends UIComponent
{
	// Private fields.
	private RotateRelativeLayout m_Container;
	private TextView m_TimerTextView;
	
	
	// Constructor.
	RecordingTimerUI(CameraActivity cameraActivity)
	{
		super("Recording Timer", cameraActivity, false);
	}
	
	
	// Hide recording timer.
	private void hideRecordingTimer()
	{
		this.setViewVisibility(m_Container, false);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add property changed call-backs
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.addCallback(CameraActivity.PROP_ELAPSED_RECORDING_SECONDS, new PropertyChangedCallback<Long>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Long> key, PropertyChangeEventArgs<Long> e)
			{
				updateRecordingTimer(e.getNewValue());
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@SuppressWarnings("incomplete-switch")
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				switch(e.getNewValue())
				{
					case CAPTURING:
						showRecordingTimer();
						break;
					case STOPPING:
						hideRecordingTimer();
						break;
				}
			}
		});
	}
	
	
	// Show recording timer.
	private void showRecordingTimer()
	{
		// setup layout
		CameraActivity cameraActivity = this.getCameraActivity();
		if(m_TimerTextView == null)
		{
			m_Container = (RotateRelativeLayout)((ViewStub)((MainActivity)cameraActivity).getCaptureUIContainer().findViewById(R.id.recording_timer_container)).inflate();
			m_TimerTextView = (TextView)m_Container.findViewById(R.id.recording_timer);
			this.updateRecordingTimer(cameraActivity.get(CameraActivity.PROP_ELAPSED_RECORDING_SECONDS));
		}
		
		// update rotation
		Rotation rotation = this.getRotation();
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)m_TimerTextView.getLayoutParams();
		m_Container.setRotation(rotation);
		switch(rotation)
		{
			case LANDSCAPE:
			case INVERSE_LANDSCAPE:
				layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				layoutParams.topMargin = cameraActivity.getResources().getDimensionPixelSize(R.dimen.recording_timer_text_margin_top_land);
				layoutParams.bottomMargin = 0;
				break;
			case PORTRAIT:
				layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				layoutParams.topMargin = 0;
				layoutParams.bottomMargin = cameraActivity.getResources().getDimensionPixelSize(R.dimen.recording_timer_text_margin_bottom_port);
				break;
			case INVERSE_PORTRAIT:
				layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				layoutParams.topMargin = cameraActivity.getResources().getDimensionPixelSize(R.dimen.recording_timer_text_margin_bottom_port);
				layoutParams.bottomMargin = 0;
				break;
		}
		m_TimerTextView.requestLayout();
		
		// show timer
		this.setViewVisibility(m_Container, true);
	}
	
	
	// Update recording timer.
	private void updateRecordingTimer(long seconds)
	{
		if(m_TimerTextView == null)
			return;
		long hours = (seconds / 3600);
		seconds -= (hours * 3600);
		long minutes = (seconds / 60);
		seconds -= (minutes * 60);
		m_TimerTextView.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
	}
}
