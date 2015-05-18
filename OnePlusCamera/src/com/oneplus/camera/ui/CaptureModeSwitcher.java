package com.oneplus.camera.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.PointF;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.base.ScreenSize;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.capturemode.CaptureMode;
import com.oneplus.camera.capturemode.CaptureMode.ImageUsage;
import com.oneplus.camera.capturemode.CaptureModeManager;

class CaptureModeSwitcher extends UIComponent
{
	// Constants
	private static final long DURATION_SWIPE_TO_SWITCH_MODE = 800;
	private static final float RATIO_DIRECTION_THRESHOLD = 0.25f;
	
	
	// Private fields.
	private final List<CaptureModeItem> m_CaptureModeItems = new ArrayList<>();
	private ViewGroup m_CaptureModeItemsContainer;
	private CaptureModeManager m_CaptureModeManager;
	private View m_CaptureModesPanel;
	private Handle m_CaptureUIDisableHandle;
	private GestureState m_GestureState = GestureState.IDLE;
	private float m_SwipeDirectionThreshold;
	private PointF m_TouchDownPosition;
	private long m_TouchDownTime;
	
	
	// Class for capture mode item.
	private final class CaptureModeItem
	{
		public final CaptureMode captureMode;
		public ImageView iconImageView;
		public View itemView;
		public TextView titleTextView;
		
		public CaptureModeItem(CaptureMode captureMode, LayoutInflater inflater, Resources res)
		{
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			layoutParams.leftMargin = res.getDimensionPixelSize(R.dimen.capture_modes_panel_item_margin_left);
			layoutParams.topMargin = res.getDimensionPixelSize(R.dimen.capture_modes_panel_item_margin_top);
			layoutParams.rightMargin = res.getDimensionPixelSize(R.dimen.capture_modes_panel_item_margin_right);
			layoutParams.bottomMargin = res.getDimensionPixelSize(R.dimen.capture_modes_panel_item_margin_bottom);
			this.captureMode = captureMode;
			this.itemView = inflater.inflate(R.layout.layout_capture_modes_panel_item, null);
			this.itemView.setLayoutParams(layoutParams);
			this.itemView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onCaptureModeItemClicked(CaptureModeItem.this);
				}
			});
			this.iconImageView = (ImageView)this.itemView.findViewById(R.id.capture_modes_panel_item_icon);
			this.iconImageView.setImageDrawable(captureMode.getImage(ImageUsage.CAPTURE_MODES_PANEL_ICON));
			this.titleTextView = (TextView)this.itemView.findViewById(R.id.capture_modes_panel_item_title);
			this.titleTextView.setText(captureMode.getDisplayName());
		}
	}
	
	
	// Gesture state.
	private enum GestureState
	{
		IDLE,
		INVALID,
		CHECKING_DIRECTION,
		SLIDING_UP,
		SLIDING_DOWN,
		SLIDING_RIGHT,
	}
	
	
	// Constructor
	CaptureModeSwitcher(CameraActivity cameraActivity)
	{
		super("Capture Mode Switcher", cameraActivity, true);
	}
	
	
	// Close capture modes panel.
	private void closeCaptureModesPanel()
	{
		// enable capture UI
		m_CaptureUIDisableHandle = Handle.close(m_CaptureUIDisableHandle);
		
		// close panel
		if(!(m_CaptureModesPanel instanceof ViewStub))
			this.setViewVisibility(m_CaptureModesPanel, false, DURATION_FADE_IN, null);
	}
	
	
	// Called when advanced settings button clicked.
	private void onAdvancedSettingsButtonClicked()
	{
		//
	}
	
	
	// Called when capture mode added.
	private void onCaptureModeAdded(CaptureMode captureMode)
	{
		// find index
		int index = m_CaptureModeManager.get(CaptureModeManager.PROP_CAPTURE_MODES).indexOf(captureMode);
		if(index < 0)
			return;
		
		// create item
		CameraActivity cameraActivity = this.getCameraActivity();
		CaptureModeItem item = new CaptureModeItem(captureMode, cameraActivity.getLayoutInflater(), cameraActivity.getResources());
		
		// add item
		m_CaptureModeItems.add(index, item);
		m_CaptureModeItemsContainer.addView(item.itemView, index);
	}
	
	
	// Called when capture mode item clicked.
	private void onCaptureModeItemClicked(CaptureModeItem item)
	{
		//
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_CaptureModeManager = this.findComponent(CaptureModeManager.class);
		
		// setup base view
		CameraActivity cameraActivity = this.getCameraActivity();
		m_CaptureModesPanel = cameraActivity.findViewById(R.id.capture_modes_panel);
		
		// add event handlers
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
				updateDirectionThreshold(e.getNewValue());
			}
		});
		
		// setup swipe thresholds
		this.updateDirectionThreshold(cameraActivity.get(CameraActivity.PROP_SCREEN_SIZE));
	}
	
	
	// Called when rotation changes.
	@Override
	protected void onRotationChanged(Rotation prevRotation, Rotation newRotation)
	{
		super.onRotationChanged(prevRotation, newRotation);
		m_GestureState = GestureState.INVALID;
	}
	
	
	// Handle touch event on preview.
	@SuppressWarnings("incomplete-switch")
	private boolean onTouch(MotionEventArgs e)
	{
		if(e.isHandled())
		{
			m_TouchDownPosition = null;
			m_GestureState = GestureState.IDLE;
			return false;
		}
		switch(e.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				m_TouchDownPosition = new PointF(e.getX(), e.getY());
				m_TouchDownTime = SystemClock.elapsedRealtime();
				m_GestureState = GestureState.CHECKING_DIRECTION;
				return true;
				
			case MotionEvent.ACTION_MOVE:
				if(m_TouchDownPosition != null)
				{
					// check duration
					if((SystemClock.elapsedRealtime() - m_TouchDownTime) >= DURATION_SWIPE_TO_SWITCH_MODE)
					{
						Log.v(TAG, "onBaseViewTouch() - Timeout");
						m_TouchDownPosition = null;
						m_GestureState = GestureState.INVALID;
						break;
					}
					
					// check state
					if(m_GestureState == GestureState.INVALID)
						break;
					
					// check direction
					float diffX = (e.getX() - m_TouchDownPosition.x);
					float diffY = (e.getY() - m_TouchDownPosition.y);
					Rotation activityRotation = this.getCameraActivityRotation();
					Rotation rotation = this.getRotation();
					int orientationDiff = (rotation.getDeviceOrientation() - activityRotation.getDeviceOrientation());
					if(orientationDiff < 0)
						orientationDiff += 360;
					if(m_GestureState == GestureState.CHECKING_DIRECTION)
					{
						if(diffX >= m_SwipeDirectionThreshold)
						{
							switch(orientationDiff)
							{
								case 0:
									m_GestureState = GestureState.SLIDING_RIGHT;
									break;
								case 90:
									m_GestureState = GestureState.SLIDING_DOWN;
									break;
								case 180:
									m_GestureState = GestureState.INVALID;
									break;
								case 270:
									m_GestureState = GestureState.SLIDING_UP;
									break;
							}
						}
						else if(-diffX >= m_SwipeDirectionThreshold)
						{
							switch(orientationDiff)
							{
								case 0:
									m_GestureState = GestureState.INVALID;
									break;
								case 90:
									m_GestureState = GestureState.SLIDING_UP;
									break;
								case 180:
									m_GestureState = GestureState.SLIDING_RIGHT;
									break;
								case 270:
									m_GestureState = GestureState.SLIDING_DOWN;
									break;
							}
						}
						else if(diffY >= m_SwipeDirectionThreshold)
						{
							switch(orientationDiff)
							{
								case 0:
									m_GestureState = GestureState.SLIDING_DOWN;
									break;
								case 90:
									m_GestureState = GestureState.INVALID;
									break;
								case 180:
									m_GestureState = GestureState.SLIDING_UP;
									break;
								case 270:
									m_GestureState = GestureState.SLIDING_RIGHT;
									break;
							}
						}
						else if(-diffY >= m_SwipeDirectionThreshold)
						{
							switch(orientationDiff)
							{
								case 0:
									m_GestureState = GestureState.SLIDING_UP;
									break;
								case 90:
									m_GestureState = GestureState.SLIDING_RIGHT;
									break;
								case 180:
									m_GestureState = GestureState.SLIDING_DOWN;
									break;
								case 270:
									m_GestureState = GestureState.INVALID;
									break;
							}
						}
					}
					else
						break;
					
					// handle gesture
					switch(m_GestureState)
					{
						case SLIDING_DOWN:
							m_GestureState = GestureState.IDLE;
							e.setHandled();
							this.switchCaptureMode(1);
							break;
						case SLIDING_UP:
							m_GestureState = GestureState.IDLE;
							e.setHandled();
							this.switchCaptureMode(-1);
							break;
						case SLIDING_RIGHT:
							m_GestureState = GestureState.IDLE;
							e.setHandled();
							this.openCaptureModesPanel();
							break;
					}
					
					// complete
					return true;
				}
				break;
				
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				m_TouchDownPosition = null;
				m_GestureState = GestureState.IDLE;
				break;
		}
		return false;
	}
	
	
	// Open capture modes panel.
	private void openCaptureModesPanel()
	{
		// setup panel
		if(m_CaptureModesPanel instanceof ViewStub)
		{
			// setup views
			m_CaptureModesPanel = ((ViewStub)m_CaptureModesPanel).inflate();
			m_CaptureModesPanel.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					closeCaptureModesPanel();
					return true;
				}
			});
			m_CaptureModeItemsContainer = (ViewGroup)m_CaptureModesPanel.findViewById(R.id.capture_modes_panel_items_container);
			View advSettingsButton = m_CaptureModesPanel.findViewById(R.id.advanced_settings_button);
			advSettingsButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onAdvancedSettingsButtonClicked();
				}
			});
			this.addAutoRotateView(advSettingsButton);
			
			// show capture modes
			if(m_CaptureModeManager != null)
			{
				List<CaptureMode> captureModes = m_CaptureModeManager.get(CaptureModeManager.PROP_CAPTURE_MODES);
				for(int i = 0, count = captureModes.size() ; i < count ; ++i)
					this.onCaptureModeAdded(captureModes.get(i));
			}
		}
		
		// disable capture UI
		m_CaptureUIDisableHandle = this.getCameraActivity().disableCaptureUI();
		
		// open panel
		this.setViewVisibility(m_CaptureModesPanel, true);
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
			Log.e(TAG, "switchCaptureMode() - No capture mode manager");
			return false;
		}
		
		// check capture mode
		CaptureMode captureMode = m_CaptureModeManager.get(CaptureModeManager.PROP_CAPTURE_MODE);
		List<CaptureMode> captureModeList = m_CaptureModeManager.get(CaptureModeManager.PROP_CAPTURE_MODES);
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
		m_SwipeDirectionThreshold = (length * RATIO_DIRECTION_THRESHOLD);
		m_TouchDownPosition = null;
	}
}
