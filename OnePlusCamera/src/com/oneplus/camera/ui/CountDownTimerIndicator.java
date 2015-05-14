package com.oneplus.camera.ui;

import android.graphics.RectF;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CountDownTimer;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;

final class CountDownTimerIndicator extends UIComponent
{
	// Private fields.
	private View m_Container;
	private CountDownTimer m_CountDownTimer;
	private TextView m_TimerTextView;
	private Viewfinder m_Viewfinder;
	
	
	// Constructor.
	CountDownTimerIndicator(CameraActivity cameraActivity)
	{
		super("Count-down Timer Indicator", cameraActivity, false);
	}
	
	
	// Called when count-down remaining seconds changes.
	private void onCountDownTimerChanged(long remainingSeconds)
	{
		// prepare UI
		if(!this.setupUI())
			return;
		
		// update timer text
		if(remainingSeconds > 0)
		{
			m_TimerTextView.setText(Long.toString(remainingSeconds));
			m_TimerTextView.setVisibility(View.INVISIBLE);
			m_TimerTextView.setAlpha(0f);
			this.setViewVisibility(m_TimerTextView, true, 400, null);
		}
		else
			m_TimerTextView.setText(null);
	}
	
	
	// Called when count-down timer starts.
	private void onCountDownTimerStarted()
	{
		// prepare UI
		if(!this.setupUI())
			return;
		
		// show UI
		this.setViewVisibility(m_Container, true, 0, null);
	}
	
	
	// Called when count-down timer stops.
	private void onCountDownTimerStopped()
	{
		if(m_TimerTextView != null)
			m_TimerTextView.setText(null);
		if(m_Container != null)
			m_Container.setVisibility(View.GONE);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_CountDownTimer = this.findComponent(CountDownTimer.class);
		m_Viewfinder = this.findComponent(Viewfinder.class);
		
		// add property changed call-back
		if(m_CountDownTimer != null)
		{
			m_CountDownTimer.addCallback(CountDownTimer.PROP_IS_STARTED, new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					if(e.getNewValue())
						onCountDownTimerStarted();
					else
						onCountDownTimerStopped();
				}
			});
			m_CountDownTimer.addCallback(CountDownTimer.PROP_REMAINING_SECONDS, new PropertyChangedCallback<Long>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Long> key, PropertyChangeEventArgs<Long> e)
				{
					onCountDownTimerChanged(e.getNewValue());
				}
			});
		}
		if(m_Viewfinder != null)
		{
			m_Viewfinder.addCallback(Viewfinder.PROP_PREVIEW_BOUNDS, new PropertyChangedCallback<RectF>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<RectF> key, PropertyChangeEventArgs<RectF> e)
				{
					updateContainerBounds();
				}
			});
		}
	}
	
	
	// Call when UI rotation changes.
	@Override
	protected void onRotationChanged(Rotation prevRotation, Rotation newRotation)
	{
		// call super
		super.onRotationChanged(prevRotation, newRotation);
		
		// rotate views
		this.rotateView(m_TimerTextView, newRotation, 0);
	}
	
	
	// Setup layouts.
	private boolean setupUI()
	{
		// check state
		if(m_TimerTextView != null)
			return true;
		
		// inflate UI
		m_Container = ((ViewStub)((MainActivity)this.getCameraActivity()).findViewById(R.id.count_down_timer_container)).inflate();
		m_TimerTextView = (TextView)m_Container.findViewById(R.id.count_down_timer_text);
		
		// initialize UI
		this.updateContainerBounds();
		this.rotateView(m_TimerTextView, this.getRotation(), 0);
		
		// complete
		return true;
	}


	// Update container bounds.
	private void updateContainerBounds()
	{
		// check state
		if(m_Container == null || m_Viewfinder == null)
			return;
		
		// update bounds
		RectF previewBounds = m_Viewfinder.get(Viewfinder.PROP_PREVIEW_BOUNDS);
		MarginLayoutParams layoutParams = (MarginLayoutParams)m_Container.getLayoutParams();
		layoutParams.width = (int)(previewBounds.width() + 0.5f);
		layoutParams.height = (int)(previewBounds.height() + 0.5f);
		layoutParams.leftMargin = (int)(previewBounds.left + 0.5f);
		layoutParams.topMargin = (int)(previewBounds.top + 0.5f);
		m_Container.requestLayout();
	}
}
