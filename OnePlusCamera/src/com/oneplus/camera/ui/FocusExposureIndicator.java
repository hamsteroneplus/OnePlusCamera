package com.oneplus.camera.ui;

import java.util.List;

import android.animation.Animator;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera.MeteringRect;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.FocusController;
import com.oneplus.camera.FocusState;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.widget.ViewUtils;

final class FocusExposureIndicator extends UIComponent
{
	// Constants.
	private static final long DURATION_FOCUS_INDICATOR_VISIBLE = 300;
	private static final long DURATION_FOCUS_INDICATOR_SHOW = 300;
	private static final int MSG_HIDE_FOCUS_INDICATOR = 10000;
	private static final int MSG_SHOW_FOCUS_INDICATOR = 10001;
	
	
	// Private fields.
	private MeteringRect m_AfRegion;
	private FocusController m_FocusController;
	private ImageView m_FocusIndicator;
	private Drawable m_FocusingDrawable;
	private Drawable m_FocusLockedDrawable;
	private ViewPropertyAnimator m_FocusIndicatorAnimator;
	private Viewfinder m_Viewfinder;
	
	
	// Constructor.
	FocusExposureIndicator(CameraActivity cameraActivity)
	{
		super("Focus/Exposure indicator", cameraActivity, true);
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_HIDE_FOCUS_INDICATOR:
				this.hideFocusIndicator();
				break;
				
			case MSG_SHOW_FOCUS_INDICATOR:
				this.showFocusIndicator(true);
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Hide focus indicator.
	private void hideFocusIndicator()
	{
		if(m_FocusIndicatorAnimator != null)
		{
			m_FocusIndicatorAnimator.cancel();
			m_FocusIndicatorAnimator = null;
		}
		this.setViewVisibility(m_FocusIndicator, false);
		HandlerUtils.removeMessages(this, MSG_SHOW_FOCUS_INDICATOR);
	}
	
	
	// Called when AF regions changed.
	private void onAfRegionsChanged(List<MeteringRect> regions)
	{
		if(regions != null && !regions.isEmpty())
			m_AfRegion = regions.get(0);
		else
			m_AfRegion = null;
		HandlerUtils.sendMessage(this, MSG_SHOW_FOCUS_INDICATOR, true);
	}
	
	
	// Called when focus state changes.
	private void onFocusStateChanged(FocusState focusState)
	{
		if(focusState == FocusState.SCANNING)
			HandlerUtils.sendMessage(this, MSG_SHOW_FOCUS_INDICATOR, true);
		else if(m_FocusIndicatorAnimator == null)
			HandlerUtils.sendMessage(this, MSG_HIDE_FOCUS_INDICATOR, true, DURATION_FOCUS_INDICATOR_VISIBLE);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_FocusController = this.findComponent(FocusController.class);
		m_Viewfinder = this.findComponent(Viewfinder.class);
		
		// setup focus indicator
		CameraActivity cameraActivity = this.getCameraActivity();
		View baseView = ((MainActivity)cameraActivity).getCaptureUIContainer();
		m_FocusIndicator = (ImageView)baseView.findViewById(R.id.focus_indicator);
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				if(!e.getNewValue())
					hideFocusIndicator();
			}
		});
		if(m_FocusController != null)
		{
			m_FocusController.addCallback(FocusController.PROP_AF_REGIONS, new PropertyChangedCallback<List<MeteringRect>>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<List<MeteringRect>> key, PropertyChangeEventArgs<List<MeteringRect>> e)
				{
					onAfRegionsChanged(e.getNewValue());
				}
			});
			m_FocusController.addCallback(FocusController.PROP_FOCUS_STATE, new PropertyChangedCallback<FocusState>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<FocusState> key, PropertyChangeEventArgs<FocusState> e)
				{
					onFocusStateChanged(e.getNewValue());
				}
			});
		}
		else
			Log.w(TAG, "onInitialize() - No FocusController");
		
		// setup initial focus indicator state
		if(m_FocusController != null)
		{
			this.onAfRegionsChanged(m_FocusController.get(FocusController.PROP_AF_REGIONS));
			this.onFocusStateChanged(m_FocusController.get(FocusController.PROP_FOCUS_STATE));
		}
	}
	
	
	// Show focus indicator.
	private void showFocusIndicator(boolean animation)
	{
		// check state
		if(m_Viewfinder == null)
			return;
		if(m_FocusIndicator == null)
			return;
		
		// cancel hiding indicator
		HandlerUtils.removeMessages(this, MSG_HIDE_FOCUS_INDICATOR);
		
		// cancel current animator
		if(m_FocusIndicatorAnimator != null)
		{
			m_FocusIndicatorAnimator.cancel();
			m_FocusIndicatorAnimator = null;
		}
		
		// check preview state
		if(!this.getCameraActivity().get(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED))
			return;
		
		// setup drawable
		if(m_FocusingDrawable == null)
			m_FocusingDrawable = this.getCameraActivity().getResources().getDrawable(R.drawable.focus);
		m_FocusIndicator.setImageDrawable(m_FocusingDrawable);
		
		// update indicator position
		float focusX, focusY;
		PointF centerPoint = new PointF();
		if(m_AfRegion == null)
		{
			focusX = 0.5f;
			focusY = 0.5f;
		}
		else
		{
			focusX = ((m_AfRegion.getLeft() + m_AfRegion.getRight()) / 2);
			focusY = ((m_AfRegion.getTop() + m_AfRegion.getBottom()) / 2);
		}
		if(!m_Viewfinder.pointFromPreview(focusX, focusY, centerPoint, 0))
			return;
		ViewUtils.setMargins(m_FocusIndicator, (int)(centerPoint.x - (m_FocusingDrawable.getIntrinsicWidth() / 2)), (int)(centerPoint.y - (m_FocusingDrawable.getIntrinsicHeight() / 2)), 0, 0);
		
		// show indicator
		if(animation)
		{
			m_FocusIndicatorAnimator = m_FocusIndicator.animate();
			m_FocusIndicatorAnimator.setListener(new Animator.AnimatorListener()
			{
				@Override
				public void onAnimationStart(Animator animation)
				{}
				
				@Override
				public void onAnimationRepeat(Animator animation)
				{}
				
				@Override
				public void onAnimationEnd(Animator animation)
				{
					m_FocusIndicatorAnimator = null;
					if(m_FocusController != null && m_FocusController.get(FocusController.PROP_FOCUS_STATE) != FocusState.SCANNING)
						HandlerUtils.sendMessage(FocusExposureIndicator.this, MSG_HIDE_FOCUS_INDICATOR, true, DURATION_FOCUS_INDICATOR_VISIBLE);
				}
				
				@Override
				public void onAnimationCancel(Animator animation)
				{
					m_FocusIndicatorAnimator = null;
					if(m_FocusController != null && m_FocusController.get(FocusController.PROP_FOCUS_STATE) != FocusState.SCANNING)
						HandlerUtils.sendMessage(FocusExposureIndicator.this, MSG_HIDE_FOCUS_INDICATOR, true, DURATION_FOCUS_INDICATOR_VISIBLE);
				}
			});
			m_FocusIndicator.setAlpha(0f);
			m_FocusIndicator.setScaleX(1.4f);
			m_FocusIndicator.setScaleY(1.4f);
			m_FocusIndicatorAnimator.setDuration(DURATION_FOCUS_INDICATOR_SHOW);
			m_FocusIndicatorAnimator.alpha(1);
			m_FocusIndicatorAnimator.scaleX(1);
			m_FocusIndicatorAnimator.scaleY(1);
			m_FocusIndicatorAnimator.start();
		}
		m_FocusIndicator.setVisibility(View.VISIBLE);
	}
}
