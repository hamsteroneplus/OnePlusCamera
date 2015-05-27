package com.oneplus.camera.ui;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Range;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.camera.Camera.MeteringRect;
import com.oneplus.camera.widget.RotateRelativeLayout;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.ExposureController;
import com.oneplus.camera.FocusController;
import com.oneplus.camera.FocusState;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.PhotoCaptureState;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.VideoCaptureState;
import com.oneplus.widget.ViewUtils;

final class FocusExposureIndicator extends UIComponent
{
	// Constants.
	private static final long DURATION_FOCUS_INDICATOR_VISIBLE = 1000;
	private static final long DURATION_FOCUS_INDICATOR_SHOW = 300;
	private static final long DURATION_EXPOSURE_COMP_ICON_ROTATION = 100;
	private static final int MSG_HIDE_FOCUS_INDICATOR = 10000;
	
	
	// Private fields.
	private MeteringRect m_AfRegion;
	private FocusController m_FocusController;
	private ImageView m_FocusIndicator;
	private FocusExposureRegionDrawable m_FocusingDrawable;
	private FocusExposureRegionDrawable m_FocusLockedDrawable;
	private RotateRelativeLayout m_FocusLockedIndicatorContainer;
	private TextView m_FocusLockedIndicator;
	private ImageView m_ExposureCompDirectionIcon;
	private View m_ExposureCompDragIcon;
	private View m_ExposureCompDragIconContainer;
	private ExposureController m_ExposureController;
	private Animation m_IndicatorAnimation;
	private final Point m_IndicatorCenterPointInWindow = new Point();
	private View m_IndicatorContainer;
	private int m_IndicatorContainerHeight;
	private int m_IndicatorContainerWidth;
	private boolean m_IsChangingExposureComp;
	private float m_RelativeExposureComp;
	private float m_RelativeExposureCompStep = 0.1f;
	private final int[] m_TempLocationBuffer = new int[2];
	private Viewfinder m_Viewfinder;
	
	
	// Drawable for AE/AF position.
	private static final class FocusExposureRegionDrawable extends Drawable
	{
		// Private fields.
		private final Drawable m_BaseDrawable;
		private final Paint m_ExposureCompPaint;
		private final int m_ExposureCompBorderWidth;
		private float m_RelativeExposureComp;
		
		// Constructor.
		public FocusExposureRegionDrawable(Context context, int resId)
		{
			m_BaseDrawable = context.getDrawable(resId);
			m_BaseDrawable.setCallback(new Drawable.Callback()
			{
				@Override
				public void unscheduleDrawable(Drawable who, Runnable what)
				{
					FocusExposureRegionDrawable.this.unscheduleSelf(what);
				}
				
				@Override
				public void scheduleDrawable(Drawable who, Runnable what, long when)
				{
					FocusExposureRegionDrawable.this.scheduleSelf(what, when);
				}
				
				@Override
				public void invalidateDrawable(Drawable who)
				{
					FocusExposureRegionDrawable.this.invalidateSelf();
				}
			});
			m_ExposureCompBorderWidth = context.getResources().getDimensionPixelSize(R.dimen.exposure_comp_border_thickness);
			m_ExposureCompPaint = new Paint();
			m_ExposureCompPaint.setColor(context.getResources().getColor(R.color.exposure_comp_border));
			m_ExposureCompPaint.setStyle(Paint.Style.STROKE);
			m_ExposureCompPaint.setStrokeWidth(m_ExposureCompBorderWidth);
			m_ExposureCompPaint.setAntiAlias(true);
		}
		
		// Draw content.
		@Override
		public void draw(Canvas canvas)
		{
			// draw base indicator
			Rect bounds = this.getBounds();
			m_BaseDrawable.setBounds(bounds);
			m_BaseDrawable.draw(canvas);
			
			// draw exposure compensation
			if(Math.abs(m_RelativeExposureComp) > 0.01)
			{
				int offset = (m_ExposureCompBorderWidth / 2);
				bounds.inset(offset, offset);
				canvas.drawArc(bounds.left, bounds.top, bounds.right, bounds.bottom, 270, 360 * m_RelativeExposureComp, false, m_ExposureCompPaint);
				bounds.inset(-offset, -offset);
			}
		}
		
		// Get height.
		@Override
		public int getIntrinsicHeight()
		{
			return m_BaseDrawable.getIntrinsicHeight();
		}
		
		// Get width.
		@Override
		public int getIntrinsicWidth()
		{
			return m_BaseDrawable.getIntrinsicWidth();
		}
		
		// Get opacity.
		@Override
		public int getOpacity()
		{
			return m_BaseDrawable.getOpacity();
		}

		// Set alpha.
		@Override
		public void setAlpha(int alpha)
		{
			m_BaseDrawable.setAlpha(alpha);
		}

		// Set color filter.
		@Override
		public void setColorFilter(ColorFilter cf)
		{
			m_BaseDrawable.setColorFilter(cf);
		}
		
		// Set relative exposure compensation [-1, 1].
		public void setRelativeExposureComp(float exposureComp)
		{
			m_RelativeExposureComp = exposureComp;
			this.invalidateSelf();
		}
		
		// Start animation.
		public void startAnimation()
		{
			if(m_BaseDrawable instanceof AnimationDrawable)
				((AnimationDrawable)m_BaseDrawable).start();
		}
		
		// Stop animation.
		public void stopAnimation()
		{
			if(m_BaseDrawable instanceof AnimationDrawable)
			{
				((AnimationDrawable)m_BaseDrawable).stop();
				((AnimationDrawable)m_BaseDrawable).selectDrawable(0);
			}
		}
	}
	
	
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
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Hide focus indicator.
	private void hideFocusIndicator()
	{
		if(m_IndicatorContainer != null)
			m_IndicatorContainer.clearAnimation();
		this.setViewVisibility(m_IndicatorContainer, false);
	}
	
	
	// Hide focus indicator later according to current state.
	private void hideFocusIndicatorDelayed()
	{
		if(m_FocusController != null && m_FocusController.get(FocusController.PROP_IS_FOCUS_LOCKED))
			return;
		if(m_FocusController != null)
		{
			if(m_FocusController.get(FocusController.PROP_FOCUS_STATE) != FocusState.SCANNING
					&& m_IndicatorAnimation == null
					&& !m_IsChangingExposureComp)
			{
				HandlerUtils.sendMessage(FocusExposureIndicator.this, MSG_HIDE_FOCUS_INDICATOR, true, DURATION_FOCUS_INDICATOR_VISIBLE);
			}
		}
		else
			this.hideFocusIndicator();
	}
	
	
	// Called when AF regions changed.
	private void onAfRegionsChanged(List<MeteringRect> regions)
	{
		if(regions != null && !regions.isEmpty())
			m_AfRegion = regions.get(0);
		else
			m_AfRegion = null;
		//if(m_FocusController.get(FocusController.PROP_FOCUS_STATE) == FocusState.SCANNING)
			//this.showFocusIndicator(m_FocusController.get(FocusController.PROP_IS_FOCUS_LOCKED), false);
	}
	
	
	// Called when focus locked or unlocked.
	private void onFocusLockedChanged(boolean isLocked)
	{
		if(isLocked)
		{
			// show indicator
			this.showFocusIndicator(true, false);
			this.getHandler().removeMessages(MSG_HIDE_FOCUS_INDICATOR);
			
			// check recording state
			switch(this.getCameraActivity().get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
			{
				case PREPARING:
				case READY:
					break;
				default:
					return;
			}
			
			// show locked text
			this.setViewVisibility(m_FocusLockedIndicator, true);
			this.rotateLayout(m_FocusLockedIndicatorContainer, 0);
			this.rotateFocusLockedIndicator(this.getRotation());
		}
		else
		{
			this.setViewVisibility(m_FocusLockedIndicator, false);
			this.hideFocusIndicator();
		}
	}
	
	
	// Called when focus state changes.
	private void onFocusStateChanged(FocusState focusState)
	{
		if(focusState == FocusState.SCANNING)
		{
			m_IsChangingExposureComp = false;
			this.showFocusIndicator(m_FocusController.get(FocusController.PROP_IS_FOCUS_LOCKED), true);
		}
		else
			this.hideFocusIndicatorDelayed();
	}
	
	
	// Called when exposure compensation state changes.
	private void onExposureCompChanged()
	{
		// calculate relative exposure compensation
		float ev = m_ExposureController.get(ExposureController.PROP_EXPOSURE_COMPENSATION);
		Range<Float> evRange = m_ExposureController.get(ExposureController.PROP_EXPOSURE_COMPENSATION_RANGE);
		float evStep = m_ExposureController.get(ExposureController.PROP_EXPOSURE_COMPENSATION_STEP);
		float relativeExposureComp = (ev >= 0 ? (ev / evRange.getUpper()) : (-ev / evRange.getLower()));
		
		// check range
		if(Math.abs(evRange.getUpper() - evRange.getLower()) <= 0.001)
		{
			if(m_ExposureCompDragIcon != null)
				m_ExposureCompDragIcon.setVisibility(View.GONE);
			return;
		}
		else
		{
			if(m_ExposureCompDragIcon != null)
				m_ExposureCompDragIcon.setVisibility(View.VISIBLE);
		}
		
		// show exposure compensation
		m_RelativeExposureCompStep = (evStep / (evRange.getUpper() - evRange.getLower()) * 2);
		this.setRelativeExposureComp(relativeExposureComp, false, true);
	}
	
	
	// Handle touch event on exposure compensation drag icon.
	private boolean onExposureCompDragIconTouch(MotionEvent event)
	{	
		switch(event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
			{
				// cancel hiding indicator
				this.getHandler().removeMessages(MSG_HIDE_FOCUS_INDICATOR);
				
				// calculate center point
				m_IndicatorContainer.getLocationInWindow(m_TempLocationBuffer);
				m_IndicatorCenterPointInWindow.x = (m_TempLocationBuffer[0] + (m_IndicatorContainerWidth / 2));
				m_IndicatorCenterPointInWindow.y = (m_TempLocationBuffer[1] + (m_IndicatorContainerHeight / 2));
				switch((this.getRotation().getDeviceOrientation() - this.getCameraActivityRotation().getDeviceOrientation() + 360) % 360)
				{
					case 90:
						m_IndicatorCenterPointInWindow.y -= m_IndicatorContainerWidth;
						break;
					case 180:
						m_IndicatorCenterPointInWindow.x -= m_IndicatorContainerWidth;
						m_IndicatorCenterPointInWindow.y -= m_IndicatorContainerHeight;
						break;
					case 270:
						m_IndicatorCenterPointInWindow.x -= m_IndicatorContainerHeight;
						break;
				}
				
				// update state
				m_IsChangingExposureComp = true;
				break;
			}
			
			case MotionEvent.ACTION_MOVE:
			{
				if(m_IsChangingExposureComp)
				{
					// calculate relative angle
					float x = event.getRawX();
					float y = event.getRawY();
					float diffX = (x - m_IndicatorCenterPointInWindow.x);
					float diffY = (y - m_IndicatorCenterPointInWindow.y);
					float relativeAngle;
					if(diffX >= 0)
					{
						if(diffY <= 0)	// 0 - 90
							relativeAngle = (float)(Math.atan(diffX / -diffY) / Math.PI / 2);
						else			// 90 - 180
							relativeAngle = (float)((Math.PI - Math.atan(diffX / diffY)) / Math.PI / 2);
					}
					else
					{
						if(diffY <= 0)	// 270 - 360
							relativeAngle = (float)(((2 * Math.PI) - Math.atan(diffX / diffY)) / Math.PI / 2);
						else			// 180 - 270
							relativeAngle = (float)((Math.PI + Math.atan(-diffX / diffY)) / Math.PI / 2);
					}
					int orientationDiff = (this.getRotation().getDeviceOrientation() - this.getCameraActivityRotation().getDeviceOrientation());
					relativeAngle += (orientationDiff / 360f);
					if(relativeAngle > 1)
						relativeAngle -= 1;
					else if(relativeAngle < 0)
						relativeAngle += 1;
					
					// calculate relative exposure compensation
					float exposureComp = m_RelativeExposureComp;
					if(Math.abs(exposureComp - 1) <= 0.001)			// Max
					{
						if(relativeAngle > 0.5f)
							exposureComp = (Math.round(relativeAngle / m_RelativeExposureCompStep) * m_RelativeExposureCompStep);
					}
					else if(Math.abs(exposureComp + 1) <= 0.001)	// Min 
					{
						if(relativeAngle < 0.5f)
							exposureComp = -(Math.round((1 - relativeAngle) / m_RelativeExposureCompStep) * m_RelativeExposureCompStep);
					}
					else if(Math.abs(exposureComp) <= 0.001)		// 0
					{
						if(relativeAngle <= 0.5f)
							exposureComp = (Math.round(relativeAngle / m_RelativeExposureCompStep) * m_RelativeExposureCompStep);
						else
							exposureComp = -(Math.round((1 - relativeAngle) / m_RelativeExposureCompStep) * m_RelativeExposureCompStep);
					}
					else if(exposureComp > 0)
					{
						exposureComp = (Math.round(relativeAngle / m_RelativeExposureCompStep) * m_RelativeExposureCompStep);
					}
					else
					{
						exposureComp = -(Math.round((1 - relativeAngle) / m_RelativeExposureCompStep) * m_RelativeExposureCompStep);
						if(Math.abs(exposureComp) <= 0.001 && m_RelativeExposureComp < -0.5f)
							exposureComp = -1;
					}
					this.setRelativeExposureComp(exposureComp, true, true);
				}
				else
					return false;
				break;
			}
			
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			{
				// update state
				m_IsChangingExposureComp = false;
				
				// hide indicator
				this.hideFocusIndicatorDelayed();
				return false;
			}
		}
		return true;
	}
	
	
	// Initialize.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_ExposureController = this.findComponent(ExposureController.class);
		m_FocusController = this.findComponent(FocusController.class);
		m_Viewfinder = this.findComponent(Viewfinder.class);
		
		// setup focus indicator
		CameraActivity cameraActivity = this.getCameraActivity();
		Resources res = cameraActivity.getResources();
		View baseView = ((MainActivity)cameraActivity).getCaptureUIContainer();
		m_IndicatorContainer = baseView.findViewById(R.id.focus_exposure_indicator_container);
		m_IndicatorContainerWidth = res.getDimensionPixelSize(R.dimen.focus_indicator_container_width);
		m_IndicatorContainerHeight = res.getDimensionPixelSize(R.dimen.focus_indicator_container_height);
		m_FocusIndicator = (ImageView)m_IndicatorContainer.findViewById(R.id.focus_indicator);
		m_ExposureCompDragIconContainer = m_IndicatorContainer.findViewById(R.id.exposure_comp_drag_icon_container);
		m_ExposureCompDirectionIcon = (ImageView)m_IndicatorContainer.findViewById(R.id.exposure_comp_direction_icon);
		m_ExposureCompDragIcon = m_ExposureCompDragIconContainer.findViewById(R.id.exposure_comp_drag_icon);
		m_ExposureCompDragIcon.findViewById(R.id.exposure_comp_drag_icon).setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				return onExposureCompDragIconTouch(event);
			}
		});
		m_FocusLockedIndicatorContainer = (RotateRelativeLayout)baseView.findViewById(R.id.focus_exposure_lock_indicator_container);
		m_FocusLockedIndicator = (TextView)m_FocusLockedIndicatorContainer.findViewById(R.id.focus_exposure_lock_indicator);
		this.addAutoRotateView(m_IndicatorContainer);
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				if(!e.getNewValue())
				{
					switch(getCameraActivity().get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
					{
						case STARTING:
						case CAPTURING:
							break;
						default:
							hideFocusIndicator();
							break;
					}
				}
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_PHOTO_CAPTURE_STATE, new PropertyChangedCallback<PhotoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<PhotoCaptureState> key, PropertyChangeEventArgs<PhotoCaptureState> e)
			{
				if(e.getNewValue() == PhotoCaptureState.REVIEWING)
				{
					hideFocusIndicator();
					setViewVisibility(m_FocusLockedIndicator, false);
				}
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				switch(e.getNewValue())
				{
					case PREPARING:
						break;
					case READY:
						onFocusLockedChanged(m_FocusController != null && m_FocusController.get(FocusController.PROP_IS_FOCUS_LOCKED));
						break;
					case REVIEWING:
						hideFocusIndicator();
					default:
						setViewVisibility(m_FocusLockedIndicator, false);
						break;
				}
			}
		});
		if(m_ExposureController != null)
		{
			PropertyChangedCallback callback = new PropertyChangedCallback()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
				{
					onExposureCompChanged();
				}
			};
			m_ExposureController.addCallback(ExposureController.PROP_EXPOSURE_COMPENSATION, callback);
			m_ExposureController.addCallback(ExposureController.PROP_EXPOSURE_COMPENSATION_RANGE, callback);
			m_ExposureController.addCallback(ExposureController.PROP_EXPOSURE_COMPENSATION_STEP, callback);
			this.onExposureCompChanged();
		}
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
			m_FocusController.addCallback(FocusController.PROP_IS_FOCUS_LOCKED, new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					onFocusLockedChanged(e.getNewValue());
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
			this.onFocusLockedChanged(m_FocusController.get(FocusController.PROP_IS_FOCUS_LOCKED));
		}
	}
	
	
	// Called when rotation changed.
	@Override
	protected void onRotationChanged(Rotation prevRotation, Rotation newRotation)
	{
		// call super
		super.onRotationChanged(prevRotation, newRotation);
		
		// rotate focus lock indicator
		this.rotateLayout(m_FocusLockedIndicatorContainer, new ViewRotationCallback()
		{
			@Override
			public void onRotated(View view, Rotation rotation)
			{
				rotateFocusLockedIndicator(rotation);
			}
		});
	}
	
	
	// Rotate focus lock indicator to given rotation.
	private void rotateFocusLockedIndicator(Rotation rotation)
	{
		// change indicator position
		if(m_FocusLockedIndicator != null && m_FocusLockedIndicator.getVisibility() == View.VISIBLE)
		{
			CameraActivity activity = this.getCameraActivity();
			Resources res = activity.getResources();
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)m_FocusLockedIndicator.getLayoutParams();
			switch(rotation)
			{
				case LANDSCAPE:
				case INVERSE_LANDSCAPE:
					layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					layoutParams.topMargin = res.getDimensionPixelSize(R.dimen.focus_lock_text_margin_top_land);
					m_FocusLockedIndicator.setBackgroundColor(Color.TRANSPARENT);
					m_FocusLockedIndicator.setTextAppearance(activity, R.style.FocusLockText_Landscape);
					break;
				case PORTRAIT:
					layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
					layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					layoutParams.bottomMargin = res.getDimensionPixelSize(R.dimen.focus_lock_text_margin_bottom_port);
					m_FocusLockedIndicator.setBackgroundColor(res.getColor(R.color.focus_lock_text_background_port));
					m_FocusLockedIndicator.setTextAppearance(activity, R.style.FocusLockText_Portrait);
					break;
				case INVERSE_PORTRAIT:
					layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					layoutParams.topMargin = res.getDimensionPixelSize(R.dimen.focus_lock_text_margin_bottom_port);
					m_FocusLockedIndicator.setBackgroundColor(res.getColor(R.color.focus_lock_text_background_port));
					m_FocusLockedIndicator.setTextAppearance(activity, R.style.FocusLockText_Portrait);
					break;
			}
			m_FocusLockedIndicator.requestLayout();
		}
	}
	
	
	// Set relative exposure compensation [-1, 1].
	private void setRelativeExposureComp(float exposureComp, boolean apply, boolean animation)
	{
		// check state
		if(Math.abs(m_RelativeExposureComp - exposureComp) <= 0.001)
			return;
		
		Log.v(TAG, "setRelativeExposureComp() - Relative exposure compensation : ", exposureComp, ", apply : ", apply);
		
		// update state
		m_RelativeExposureComp = exposureComp;
		
		// update drawable
		if(m_FocusingDrawable != null)
			m_FocusingDrawable.setRelativeExposureComp(exposureComp);
		if(m_FocusLockedDrawable != null)
			m_FocusLockedDrawable.setRelativeExposureComp(exposureComp);
		
		// check view state
		if(m_ExposureCompDragIcon == null)
			return;
		if(m_ExposureCompDragIcon.getVisibility() != View.VISIBLE)
			animation = false;
		
		// rotate exposure compensation icon
		float viewRotation = (360 * exposureComp);
		if(m_ExposureCompDragIconContainer != null)
		{
			if(animation)
			{
				m_ExposureCompDragIconContainer.animate()
					.rotation(viewRotation)
					.setDuration(DURATION_EXPOSURE_COMP_ICON_ROTATION)
					.start();
			}
			else
				m_ExposureCompDragIconContainer.setRotation(viewRotation);
		}
		if(m_ExposureCompDragIcon != null)
		{
			if(animation)
			{
				m_ExposureCompDragIcon.animate()
					.rotation(-viewRotation)
					.setDuration(DURATION_EXPOSURE_COMP_ICON_ROTATION)
					.start();
			}
			else
				m_ExposureCompDragIcon.setRotation(-viewRotation);
		}
		
		// show/hide direction icon
		if(m_ExposureCompDirectionIcon != null)
		{
			if(Math.abs(exposureComp) > 0.001 && Math.abs(exposureComp) < 0.99)
			{
				if(exposureComp > 0)
					m_ExposureCompDirectionIcon.setImageResource(R.drawable.exposure_comp_icon_plus);
				else
					m_ExposureCompDirectionIcon.setImageResource(R.drawable.exposure_comp_icon_minus);
				this.setViewVisibility(m_ExposureCompDirectionIcon, true);
			}
			else
				m_ExposureCompDirectionIcon.setVisibility(View.GONE);
		}
		
		// apply exposure compensation
		if(m_ExposureController != null)
		{
			if(apply)
			{
				Range<Float> evRange = m_ExposureController.get(ExposureController.PROP_EXPOSURE_COMPENSATION_RANGE);
				float ev = (exposureComp >= 0 ? (exposureComp * evRange.getUpper()) : (-exposureComp * evRange.getLower()));
				m_ExposureController.set(ExposureController.PROP_EXPOSURE_COMPENSATION, ev);
			}
		}
		else
			Log.e(TAG, "setRelativeExposureComp() - No ExposureController interface");
	}
	
	
	// Show focus indicator.
	private void showFocusIndicator(boolean isFocusLocked, boolean animation)
	{
		// check state
		if(m_Viewfinder == null)
			return;
		if(m_IndicatorContainer == null)
			return;
		
		Log.v(TAG, "showFocusIndicator");
		
		// cancel hiding indicator
		HandlerUtils.removeMessages(this, MSG_HIDE_FOCUS_INDICATOR);
		 
		// cancel current animator
		if(!animation && m_IndicatorAnimation != null)
		{
			m_IndicatorContainer.clearAnimation();
			m_IndicatorAnimation = null;
		}
		
		// check preview state
		CameraActivity cameraActivity = this.getCameraActivity();
		if(!cameraActivity.get(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED))
			return;
		
		// check self-timer
		if(cameraActivity.get(CameraActivity.PROP_IS_SELF_TIMER_STARTED))
			return;
		
		// setup drawable
		if(m_FocusingDrawable == null)
		{
			float exposureComp = m_RelativeExposureComp;
			m_RelativeExposureComp = 0;
			m_FocusingDrawable = new FocusExposureRegionDrawable(cameraActivity, R.drawable.focus);
			m_FocusLockedDrawable = new FocusExposureRegionDrawable(this.getCameraActivity(), R.drawable.focus_locked_animation);
			this.setRelativeExposureComp(exposureComp, false, false);
		}
		if(isFocusLocked)
		{
			m_FocusIndicator.setImageDrawable(m_FocusLockedDrawable);
			m_FocusLockedDrawable.stopAnimation();
			m_FocusLockedDrawable.startAnimation();
		}
		else
			m_FocusIndicator.setImageDrawable(m_FocusingDrawable);
		
		// update indicator position
		float focusX, focusY;
		PointF centerPoint = new PointF();
		boolean isSensorFocus;
		if(m_AfRegion == null)
		{
			focusX = 0.5f;
			focusY = 0.5f;
			isSensorFocus = true;
		}
		else
		{
			focusX = ((m_AfRegion.getLeft() + m_AfRegion.getRight()) / 2);
			focusY = ((m_AfRegion.getTop() + m_AfRegion.getBottom()) / 2);
			isSensorFocus = false;
		}
		if(!m_Viewfinder.pointFromPreview(focusX, focusY, centerPoint, 0))
			return;
		ViewUtils.setMargins(m_IndicatorContainer, (int)(centerPoint.x - (m_IndicatorContainerWidth / 2)), (int)(centerPoint.y - (m_IndicatorContainerHeight / 2)), 0, 0);
		
		// show/hide exposure compensation drag icon
		if(m_ExposureCompDragIcon != null)
		{
			int visibility = (isSensorFocus ? View.GONE : View.VISIBLE);
			if(m_ExposureCompDragIcon.getVisibility() != visibility)
			{
				m_ExposureCompDragIcon.setVisibility(visibility);
				m_ExposureCompDragIcon.setAlpha(isSensorFocus ? 0f : 1f);
			}
		}
		
		// show indicator
		if(animation)
		{
			AnimationSet animationSet = new AnimationSet(true);
			ScaleAnimation scaleAnimation = new ScaleAnimation(1.4f, 1, 1.4f, 1, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
			AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
			scaleAnimation.setDuration(DURATION_FOCUS_INDICATOR_SHOW);
			scaleAnimation.setFillBefore(true);
			alphaAnimation.setDuration(DURATION_FOCUS_INDICATOR_SHOW);
			alphaAnimation.setFillBefore(true);
			animationSet.addAnimation(scaleAnimation);
			animationSet.addAnimation(alphaAnimation);
			animationSet.setAnimationListener(new Animation.AnimationListener()
			{
				@Override
				public void onAnimationStart(Animation animation)
				{}
				
				@Override
				public void onAnimationRepeat(Animation animation)
				{}
				
				@Override
				public void onAnimationEnd(Animation animation)
				{
					m_IndicatorAnimation = null;
					hideFocusIndicatorDelayed();
				}
			});
			m_IndicatorAnimation = animationSet;
			m_IndicatorContainer.startAnimation(animationSet);
		}
		m_IndicatorContainer.setVisibility(View.VISIBLE);
	}
}
