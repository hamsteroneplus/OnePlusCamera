package com.oneplus.camera.ui;

import android.content.res.Configuration;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Message;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RelativeLayout;

import com.oneplus.base.BaseActivity;
import com.oneplus.base.BaseActivity.State;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.base.ScreenSize;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.UIComponent;

final class ViewfinderImpl extends UIComponent implements Viewfinder
{
	// Constants
	private static final int MSG_RECREATE_DIRECT_OUTPUT_SURFACE = 10000;
	
	
	// Private fields
	private int m_DirectOutputSurfaceFormat;
	private SurfaceHolder m_DirectOutputSurfaceHolder;
	private Size m_DirectOutputSurfaceSize;
	private SurfaceView m_DirectOutputSurfaceView;
	private boolean m_IsDirectOutputSurfaceCreated;
	private PreviewRenderingMode m_PreviewRenderingMode = PreviewRenderingMode.DIRECT;
	private Size m_ScreenSize = new Size(0, 0);
	
	
	// Constructor
	ViewfinderImpl(CameraActivity cameraActivity)
	{
		super("Viewfinder", cameraActivity, true);
		this.enablePropertyLogs(PROP_PREVIEW_RECEIVER, LOG_PROPERTY_CHANGE);
	}
	
	
	// Calculate preview bounds. (Called in any thread)
	private void calculatePreviewBounds(Size containerSize, Rotation rotation, Size previewSize, boolean isScreen, Rect result)
	{
		// rotate preview size
		if(rotation.isPortrait())
			previewSize = new Size(previewSize.getHeight(), previewSize.getWidth());
		
		// calculate resized preview size
		float ratioX = ((float)containerSize.getWidth() / previewSize.getWidth());
		float ratioY = ((float)containerSize.getHeight() / previewSize.getHeight());
		float ratio = Math.min(ratioX, ratioY);
		int newPreviewWidth = (int)(previewSize.getWidth() * ratio + 0.5f);
		int newPreviewHeight = (int)(previewSize.getHeight() * ratio + 0.5f);
		
		// check position
		if(isScreen)
		{
			if(rotation.isLandscape())
			{
				int centerX = (containerSize.getHeight() * 2 / 3);
				result.left = Math.max(0, (centerX - (newPreviewWidth / 2)));
				result.top = ((containerSize.getHeight() - newPreviewHeight) / 2);
			}
			else
			{
				int centerY = (containerSize.getWidth() * 2 / 3);
				result.left = ((containerSize.getWidth() - newPreviewWidth) / 2);
				result.top = Math.max(0, (centerY - (newPreviewHeight / 2)));
			}
		}
		else
		{
			result.left = ((containerSize.getWidth() - newPreviewWidth) / 2);
			result.top = ((containerSize.getHeight() - newPreviewHeight) / 2);
		}
		result.right = (result.left + newPreviewWidth);
		result.bottom = (result.top + newPreviewHeight);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_PREVIEW_CONTAINER_SIZE)
			return (TValue)m_ScreenSize;
		if(key == PROP_PREVIEW_RENDERING_MODE)
			return (TValue)m_PreviewRenderingMode;
		return super.get(key);
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_RECREATE_DIRECT_OUTPUT_SURFACE:
				this.recreateDirectOutputSurface();
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Initialize UI.
	private void initializeUI(View rootView)
	{
		// check root view
		if(!(rootView instanceof RelativeLayout))
			throw new RuntimeException("Activity root layout must be RelativeLayout.");
		
		// create SurfaceView
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		if(this.getCameraActivityRotation().isLandscape())
		{
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		}
		else
		{
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		}
		m_DirectOutputSurfaceView = new SurfaceView(this.getCameraActivity());
		m_DirectOutputSurfaceHolder = m_DirectOutputSurfaceView.getHolder();
		m_DirectOutputSurfaceHolder.addCallback(new SurfaceHolder.Callback()
		{
			@Override
			public void surfaceDestroyed(SurfaceHolder holder)
			{
				onDirectOutputSurfaceDestroyed();
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder)
			{
				onDirectOutputSurfaceCreated();
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
			{
				onDirectOutputSurfaceChanged(format, width, height);
			}
		});
		((ViewGroup)rootView).addView(m_DirectOutputSurfaceView, 0, layoutParams);
		
		// setup preview bounds
		this.updatePreviewBounds();
	}
	
	
	// Called when Surface destroyed.
	private void onDirectOutputSurfaceDestroyed()
	{
		Log.w(TAG, "onDirectOutputSurfaceDestroyed()");
		
		// update state
		m_IsDirectOutputSurfaceCreated = false;
		
		// invalidate preview receiver
		this.setReadOnly(PROP_PREVIEW_RECEIVER, null);
	}
	
	
	// Called when Surface created.
	private void onDirectOutputSurfaceCreated()
	{
		Log.w(TAG, "onDirectOutputSurfaceCreated()");
		
		// update state
		m_IsDirectOutputSurfaceCreated = true;
		
		// setup Surface size
		this.updateDirectOutputSurfaceSize(this.getCameraActivity().get(CameraActivity.PROP_CAMERA_PREVIEW_SIZE));
	}
	
	
	// Called when Surface size changed.
	private void onDirectOutputSurfaceChanged(int format, int width, int height)
	{
		Log.w(TAG, "onDirectOutputSurfaceChanged() - Format : " + format + ", size : " + width + "x" + height);
		
		// save state
		m_DirectOutputSurfaceFormat = format;
		m_DirectOutputSurfaceSize = new Size(width, height);
		
		// update preview receiver state
		switch(m_PreviewRenderingMode)
		{
			case DIRECT:
			{
				Size previewSize = this.getCameraActivity().get(CameraActivity.PROP_CAMERA_PREVIEW_SIZE);
				if(m_DirectOutputSurfaceSize.equals(previewSize))
					this.updatePreviewReceiverState();
				else
					this.updateDirectOutputSurfaceSize(previewSize);
				break;
			}
			case OPENGL:
				//
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
		//
		
		// setup SurfaceView
		CameraActivity cameraActivity = this.getCameraActivity();
		View rootView = cameraActivity.get(CameraActivity.PROP_CONTENT_VIEW);
		if(rootView != null)
			this.initializeUI(rootView);
		else
		{
			cameraActivity.addCallback(CameraActivity.PROP_CONTENT_VIEW, new PropertyChangedCallback<View>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<View> key, PropertyChangeEventArgs<View> e)
				{
					initializeUI(e.getNewValue());
					source.removeCallback(CameraActivity.PROP_CONTENT_VIEW, this);
				}
			});
		}
		
		// check activity state
		cameraActivity.addCallback(CameraActivity.PROP_STATE, new PropertyChangedCallback<State>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<State> key, PropertyChangeEventArgs<State> e)
			{
				if(e.getNewValue() == State.RESUMING)
				{
					switch(m_PreviewRenderingMode)
					{
						case DIRECT:
							if(m_IsDirectOutputSurfaceCreated && get(PROP_PREVIEW_RECEIVER) == null)
								onDirectOutputSurfaceChanged(m_DirectOutputSurfaceFormat, m_DirectOutputSurfaceSize.getWidth(), m_DirectOutputSurfaceSize.getHeight());
							break;
							
						case OPENGL:
							//
							break;
					}
				}
			}
		});
		
		// check screen size
		cameraActivity.addCallback(CameraActivity.PROP_SCREEN_SIZE, new PropertyChangedCallback<ScreenSize>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<ScreenSize> key, PropertyChangeEventArgs<ScreenSize> e)
			{
				onScreenSizeChanged(e.getNewValue(), true);
			}
		});
		this.onScreenSizeChanged(cameraActivity.get(CameraActivity.PROP_SCREEN_SIZE), true);
		
		// check preview size
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA_PREVIEW_SIZE, new PropertyChangedCallback<Size>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Size> key, PropertyChangeEventArgs<Size> e)
			{
				onPreviewSizeChanged(e.getNewValue());
			}
		});
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_CONFIG_ORIENTATION, new PropertyChangedCallback<Integer>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Integer> key, PropertyChangeEventArgs<Integer> e)
			{
				updatePreviewReceiverState();
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_STATE, new PropertyChangedCallback<BaseActivity.State>()
		{
			@SuppressWarnings("incomplete-switch")
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<BaseActivity.State> key, PropertyChangeEventArgs<BaseActivity.State> e)
			{
				switch(e.getNewValue())
				{
					case PAUSING:
						if(m_DirectOutputSurfaceView != null && m_DirectOutputSurfaceView.getVisibility() == View.VISIBLE)
							HandlerUtils.sendMessage(ViewfinderImpl.this, MSG_RECREATE_DIRECT_OUTPUT_SURFACE);
						setReadOnly(PROP_PREVIEW_RECEIVER, null);
						break;
				}
			}
		});
	}
	
	
	// Called when preview size changed.
	private void onPreviewSizeChanged(Size previewSize)
	{
		// invalidate preview receiver
		this.setReadOnly(PROP_PREVIEW_RECEIVER, null);
		
		// set Surface size
		this.updateDirectOutputSurfaceSize(previewSize);
		
		// update preview bounds
		this.updatePreviewBounds(previewSize);
	}
	
	
	// Called when screen size changed.
	private void onScreenSizeChanged(ScreenSize screenSize, boolean isInit)
	{
		// print log
		if(!isInit)
			Log.w(TAG, "onScreenSizeChanged() - Changed to " + screenSize);
		
		// save size
		m_ScreenSize = screenSize.toSize();
		
		// update preview bounds
		this.updatePreviewBounds();
	}
	
	
	// Calculate position on screen from relative position in preview.
	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean pointFromPreview(float previewX, float previewY, PointF result, int flags)
	{
		// check parameter
		if(result == null)
			return false;
		if((flags & FLAG_NO_BOUNDS_CHECKING) == 0)
		{
			if(previewX < 0 || previewX > 1 || previewY < 0 || previewY > 1)
				return false;
		}
		
		// get preview bounds
		RectF previewBounds = this.get(PROP_PREVIEW_BOUNDS);
		float containerWidth, containerHeight;
		if(this.getCameraActivityRotation().isLandscape())
		{
			containerWidth = previewBounds.width();
			containerHeight = previewBounds.height();
		}
		else
		{
			containerWidth = previewBounds.height();
			containerHeight = previewBounds.width();
		}
		
		// convert to position in preview container
		float x = (containerWidth * previewX);
		float y = (containerHeight * previewY);
		
		// rotate position
		switch(this.getCameraActivityRotation())
		{
			case PORTRAIT:
			{
				float newX = (containerHeight - y);
				y = x;
				x = newX;
				break;
			}
			case INVERSE_PORTRAIT:
			{
				float newY = (containerWidth - x);
				x = y;
				y = newY;
				break;
			}
			case INVERSE_LANDSCAPE:
			{
				x = (containerWidth - x);
				y = (containerHeight - y);
				break;
			}
		}
		
		// complete
		result.x = (previewBounds.left + x);
		result.y = (previewBounds.top + y);
		return true;
	}
	
	
	// Calculate relative position in preview from screen position.
	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean pointToPreview(float screenX, float screenY, PointF result, int flags)
	{
		// check parameter
		if(result == null)
			return false;
		
		// check bounds
		RectF previewBounds = this.get(PROP_PREVIEW_BOUNDS);
		if((flags & FLAG_NO_BOUNDS_CHECKING) == 0 && !previewBounds.contains(screenX, screenY))
			return false;
		
		// rotate position
		float containerWidth = previewBounds.width();
		float containerHeight = previewBounds.height();
		screenX -= previewBounds.left;
		screenY -= previewBounds.top;
		switch(this.getCameraActivityRotation())
		{
			case PORTRAIT:
			{
				float newY = (containerWidth - screenX);
				screenX = screenY;
				screenY = newY;
				break;
			}
			case INVERSE_PORTRAIT:
			{
				float newX = (containerHeight - screenY);
				screenY = screenX;
				screenX = newX;
				break;
			}
			case INVERSE_LANDSCAPE:
			{
				screenX = (containerWidth - screenX);
				screenY = (containerHeight - screenY);
				break;
			}
		}
		
		// complete
		if(this.getCameraActivityRotation().isLandscape())
		{
			result.x = ((float)screenX / containerWidth);
			result.y = ((float)screenY / containerHeight);
		}
		else
		{
			result.x = ((float)screenX / containerHeight);
			result.y = ((float)screenY / containerWidth);
		}
		return true;
	}
	
	
	// Re-create direct output Surface.
	private void recreateDirectOutputSurface()
	{
		if(m_DirectOutputSurfaceView != null)
		{
			Log.v(TAG, "recreateDirectOutputSurface()");
			m_DirectOutputSurfaceView.setVisibility(View.INVISIBLE);
			m_DirectOutputSurfaceView.setVisibility(View.VISIBLE);
		}
	}
	
	
	// Set size of direct output Surface.
	private void updateDirectOutputSurfaceSize(Size size)
	{
		if(m_DirectOutputSurfaceHolder != null && size.getWidth() > 0 && size.getHeight() > 0)
			m_DirectOutputSurfaceHolder.setFixedSize(size.getWidth(), size.getHeight());
	}
	
	
	// Update preview bounds.
	private void updatePreviewBounds()
	{
		this.updatePreviewBounds(this.getCameraActivity().get(CameraActivity.PROP_CAMERA_PREVIEW_SIZE));
	}
	private void updatePreviewBounds(Size previewSize)
	{
		// calculate preview bounds
		Rect previewBounds = new Rect();
		this.calculatePreviewBounds(m_ScreenSize, this.getCameraActivityRotation(), previewSize, true, previewBounds);
		
		// update SurfaceView bounds
		if(m_DirectOutputSurfaceView != null)
		{
			MarginLayoutParams layoutParams = (MarginLayoutParams)m_DirectOutputSurfaceView.getLayoutParams();
			layoutParams.width = previewBounds.width();
			layoutParams.height = previewBounds.height();
			layoutParams.topMargin = previewBounds.top;
			layoutParams.leftMargin = previewBounds.left;
			m_DirectOutputSurfaceView.requestLayout();
		}
		
		// update property
		this.setReadOnly(PROP_PREVIEW_BOUNDS, new RectF(previewBounds));
	}
	
	
	// Update preview receiver state.
	private void updatePreviewReceiverState()
	{
		// check activity state
		boolean isReceiverReady = true;
		CameraActivity cameraActivity = this.getCameraActivity();
		switch(cameraActivity.get(CameraActivity.PROP_STATE))
		{
			case RESUMING:
			case RUNNING:
				break;
			default:
				isReceiverReady = false;
				break;
		}
		
		// check receiver state
		Object receiver = null;
		Size previewSize = this.getCameraActivity().get(CameraActivity.PROP_CAMERA_PREVIEW_SIZE);
		switch(m_PreviewRenderingMode)
		{
			case DIRECT:
				if(!m_IsDirectOutputSurfaceCreated || !m_DirectOutputSurfaceSize.equals(previewSize))
					isReceiverReady = false;
				receiver = m_DirectOutputSurfaceHolder;
				break;
			case OPENGL:
				//
				receiver = null;
				break;
		}
		
		// check orientation
		if(isReceiverReady)
		{
			if(this.getCameraActivityRotation().isLandscape())
			{
				if(cameraActivity.get(CameraActivity.PROP_CONFIG_ORIENTATION) != Configuration.ORIENTATION_LANDSCAPE)
					isReceiverReady = false;
			}
			else
			{
				if(cameraActivity.get(CameraActivity.PROP_CONFIG_ORIENTATION) != Configuration.ORIENTATION_PORTRAIT)
					isReceiverReady = false;
			}
		}
		
		// update receiver state
		if(isReceiverReady)
			this.setReadOnly(PROP_PREVIEW_RECEIVER, receiver);
		else
			this.setReadOnly(PROP_PREVIEW_RECEIVER, null);
	}
}
