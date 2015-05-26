package com.oneplus.camera.ui;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.ZoomController;

final class PinchZoomingUI extends CameraComponent
{
	// Constants.
	private static final float RATIO_SCALING = 1.2f;
	private static final long DURATION_START_SCALING = 100;
	
	
	// Private fields.
	private ScaleGestureDetector m_GestureDetector;
	private float m_InitialDigitalZoom = 1;
	private float m_InitialSpan;
	private boolean m_IsScaling;
	private long m_ScaleBeginTime;
	private ZoomController m_ZoomController;
	
	
	// Listeners.
	private final ScaleGestureDetector.OnScaleGestureListener m_ScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener()
	{
		@Override
		public void onScaleEnd(ScaleGestureDetector detector)
		{
			m_IsScaling = false;
		}
		
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector)
		{
			m_ScaleBeginTime = SystemClock.elapsedRealtime();
			m_InitialSpan = detector.getCurrentSpan();
			m_InitialDigitalZoom = m_ZoomController.get(ZoomController.PROP_DIGITAL_ZOOM);
			m_IsScaling = true;
			return true;
		}
		
		@Override
		public boolean onScale(ScaleGestureDetector detector)
		{
			onScaleByGesture(detector.getCurrentSpan());
			return true;
		}
	};
	
	
	// Constructor.
	PinchZoomingUI(CameraActivity cameraActivity)
	{
		super("Pinch Zooming UI", cameraActivity, true);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_ZoomController = this.findComponent(ZoomController.class);
		
		// create gesture detector
		CameraActivity cameraActivity = this.getCameraActivity();
		m_GestureDetector = new ScaleGestureDetector(cameraActivity, m_ScaleGestureListener, this.getHandler());
		
		// add event handlers
		cameraActivity.addHandler(CameraActivity.EVENT_TOUCH, new EventHandler<MotionEventArgs>()
		{
			@Override
			public void onEventReceived(EventSource source, EventKey<MotionEventArgs> key, MotionEventArgs e)
			{
				onTouch(e);
			}
		});
	}
	
	
	// Called when scaling.
	private void onScaleByGesture(float span)
	{
		// check time
		if((SystemClock.elapsedRealtime() - m_ScaleBeginTime) < DURATION_START_SCALING)
			return;
		
		// check ratio of span
		float spanRatio = (span / m_InitialSpan * RATIO_SCALING);
		if(Math.abs(spanRatio) <= 0.001f)
			return;
		
		// calculate zoom
		float digitalZoom = (m_InitialDigitalZoom * spanRatio);
		float maxDigitalZoom = m_ZoomController.get(ZoomController.PROP_MAX_DIGITAL_ZOOM);
		if(digitalZoom < 1)
			digitalZoom = 1;
		else
			digitalZoom = Math.min(digitalZoom, maxDigitalZoom);
		
		// change zoom
		m_ZoomController.set(ZoomController.PROP_DIGITAL_ZOOM, digitalZoom);
	}
	
	
	// Handle touch event.
	private void onTouch(MotionEventArgs e)
	{
		// check event
		if(e.isHandled())
			return;
		
		// check zoom state
		if(m_ZoomController == null 
				|| !m_ZoomController.get(ZoomController.PROP_IS_DIGITAL_ZOOM_SUPPORTED)
				|| m_ZoomController.get(ZoomController.PROP_IS_ZOOM_LOCKED))
		{
			return;
		}
		
		// get motion event
		MotionEvent event = e.getMotionEvent();
		if(event == null)
			return;
		
		// detect gesture
		m_GestureDetector.onTouchEvent(event);
		if(m_IsScaling)
			e.setHandled();
	}
}
