package com.oneplus.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.SeekBar;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.OperationState;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.ZoomController;
import com.oneplus.camera.media.MediaType;

final class ZoomBarImpl extends UIComponent
{
	// Constants.
	private static final long DURATION_ZOOM_BAR_VISIBLE = 1000;
	private static final int MSG_HIDE_ZOOM_BAR = 10000;
	
	
	// Private fields.
	private View m_Container;
	private boolean m_UpdatingZoom;
	private SeekBar m_ZoomBar;
	private ZoomController m_ZoomController;
	
	
	// Classes for zoom bar progress drawables.
	private static final class BaseProgressDrawable extends Drawable
	{
		private final Paint m_Paint;
		private final float m_Thickness;
		
		public BaseProgressDrawable(Context context, int color)
		{
			m_Paint = new Paint();
			m_Paint.setStyle(Paint.Style.FILL);
			m_Paint.setColor(color);
			m_Thickness = context.getResources().getDimensionPixelSize(R.dimen.zoom_bar_track_thickness);
		}
		
		@Override
		public void draw(Canvas canvas)
		{
			Rect bounds = this.getBounds();
			float top = (bounds.top + ((bounds.height() - m_Thickness) / 2));
			canvas.drawRect(bounds.left, top, bounds.right, top + m_Thickness, m_Paint);
		}

		@Override
		public void setAlpha(int alpha)
		{}

		@Override
		public void setColorFilter(ColorFilter cf)
		{}

		@Override
		public int getOpacity()
		{
			return 255;
		}
	}
	private static final class ProgressDrawable extends LayerDrawable
	{
		public ProgressDrawable(Context context)
		{
			super(new Drawable[]{
				new BaseProgressDrawable(context, context.getResources().getColor(R.color.zoom_bar_track)),
				new ClipDrawable(new BaseProgressDrawable(context, context.getResources().getColor(R.color.zoom_bar_track_progress)), Gravity.START, ClipDrawable.HORIZONTAL),
			});
			this.setId(0, android.R.id.background);
			this.setId(1, android.R.id.progress);
		}
	}
	
	
	// Constructor.
	ZoomBarImpl(CameraActivity cameraActivity)
	{
		super("Zoom Bar", cameraActivity, true);
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_HIDE_ZOOM_BAR:
				this.setViewVisibility(m_Container, false, DURATION_FADE_IN, INTERPOLATOR_FADE_IN);
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
		
		// find components
		m_ZoomController = this.findComponent(ZoomController.class);
		
		// setup views
		MainActivity cameraActivity = (MainActivity)this.getCameraActivity();
		m_Container = cameraActivity.getCaptureUIContainer().findViewById(R.id.zoom_bar_container);
		m_ZoomBar = (SeekBar)m_Container.findViewById(R.id.zoom_bar);
		m_ZoomBar.setProgressDrawable(new ProgressDrawable(cameraActivity));
		m_ZoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if(!m_UpdatingZoom && m_ZoomController != null)
				{
					m_UpdatingZoom = true;
					float zoom = (1 + ((float)progress / seekBar.getMax() * (m_ZoomController.get(ZoomController.PROP_MAX_DIGITAL_ZOOM) - 1)));
					m_ZoomController.set(ZoomController.PROP_DIGITAL_ZOOM, zoom);
					m_UpdatingZoom = false;
				}
			}
		});
		this.addAutoRotateView(m_Container.findViewById(R.id.zoom_bar_icon_plus));
		this.addAutoRotateView(m_Container.findViewById(R.id.zoom_bar_icon_minus));
		
		// add call-backs.
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, new PropertyChangedCallback<Camera>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Camera> key, PropertyChangeEventArgs<Camera> e)
			{
				setViewVisibility(m_Container, false);
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_IS_READY_TO_CAPTURE, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				if(!e.getNewValue())
					setViewVisibility(m_Container, false);
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, new PropertyChangedCallback<MediaType>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<MediaType> key, PropertyChangeEventArgs<MediaType> e)
			{
				setViewVisibility(m_Container, false);
			}
		});
		if(m_ZoomController != null)
		{
			m_ZoomController.addCallback(ZoomController.PROP_DIGITAL_ZOOM, new PropertyChangedCallback<Float>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Float> key, PropertyChangeEventArgs<Float> e)
				{
					showCurrentZoom();
					if(getCameraActivity().get(CameraActivity.PROP_CAMERA_PREVIEW_STATE) == OperationState.STARTED
							&& !m_ZoomController.get(ZoomController.PROP_IS_ZOOM_LOCKED))
					{
						HandlerUtils.sendMessage(ZoomBarImpl.this, MSG_HIDE_ZOOM_BAR, true, DURATION_ZOOM_BAR_VISIBLE);
						setViewVisibility(m_Container, true);
					}
				}
			});
			m_ZoomController.addCallback(ZoomController.PROP_IS_DIGITAL_ZOOM_SUPPORTED, new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					if(!e.getNewValue())
						setViewVisibility(m_Container, false);
				}
			});
			m_ZoomController.addCallback(ZoomController.PROP_IS_ZOOM_LOCKED, new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					if(!e.getNewValue())
						setViewVisibility(m_Container, false);
				}
			});
			m_ZoomController.addCallback(ZoomController.PROP_MAX_DIGITAL_ZOOM, new PropertyChangedCallback<Float>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Float> key, PropertyChangeEventArgs<Float> e)
				{
					showCurrentZoom();
				}
			});
		}
	}
	
	
	// Show current zoom.
	private void showCurrentZoom()
	{
		if(m_UpdatingZoom || m_ZoomController == null || m_ZoomBar == null)
			return;
		float maxDigitalZoom = m_ZoomController.get(ZoomController.PROP_MAX_DIGITAL_ZOOM);
		float digitalZoom = m_ZoomController.get(ZoomController.PROP_DIGITAL_ZOOM);
		m_UpdatingZoom = true;
		m_ZoomBar.setProgress((int)(m_ZoomBar.getMax() * ((digitalZoom - 1) / (maxDigitalZoom - 1))));
		m_UpdatingZoom = false;
	}
}
