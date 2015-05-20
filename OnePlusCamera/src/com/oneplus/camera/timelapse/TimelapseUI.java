package com.oneplus.camera.timelapse;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.ui.CaptureButtons;

final class TimelapseUI extends UIComponent
{
	// Private fields.
	private Handle m_CaptureButtonBgHandle;
	private Handle m_CaptureButtonIconHandle;
	private CaptureButtons m_CaptureButtons;
	private TimelapseController m_Controller;
	private boolean m_IsEntered;
	private Handle m_RecordingTimeRatioHandle;
	
	
	// Constructor.
	TimelapseUI(CameraActivity cameraActivity)
	{
		super("Time-lapse UI", cameraActivity, true);
	}
	
	
	// Enter.
	boolean enter()
	{
		// check state
		if(m_IsEntered)
			return true;
		
		// notify controller
		if(m_Controller != null)
		{
			if(!m_Controller.enter())
			{
				Log.e(TAG, "enter() - Fail to enter controller");
				return false;
			}
		}
		else
			Log.w(TAG, "enter() - Enter controller later");
		
		// update state
		m_IsEntered = true;
		
		// change recording time ratio
		m_RecordingTimeRatioHandle = this.getCameraActivity().setRecordingTimeRatio(1 / TimelapseController.SPEED_RATIO);
		
		// change capture button images
		this.setCaptureButtonImages();
		
		// complete
		return true;
	}
	
	
	// Exit.
	void exit()
	{
		// check state
		if(!m_IsEntered)
			return;
		
		// notify controller
		if(m_Controller != null)
			m_Controller.exit();
		
		// restore recording time ratio
		m_RecordingTimeRatioHandle = Handle.close(m_RecordingTimeRatioHandle);
		
		// restore capture button images
		m_CaptureButtonBgHandle = Handle.close(m_CaptureButtonBgHandle);
		m_CaptureButtonIconHandle = Handle.close(m_CaptureButtonIconHandle);
		
		// complete
		m_IsEntered = false;
	}
	
	
	// Called when camera thread started.
	@Override
	protected void onCameraThreadStarted()
	{
		// call super
		super.onCameraThreadStarted();
		
		// find controller
		if(m_IsEntered)
		{
			ComponentUtils.findComponent(this.getCameraThread(), TimelapseController.class, this, new ComponentSearchCallback<TimelapseController>()
			{
				@Override
				public void onComponentFound(TimelapseController component)
				{
					onControllerFound(component);
				}
			});
		}
	}
	
	
	// Called when CaptureButtons interface found.
	private void onCaptureButtonsFound(CaptureButtons component)
	{
		// save instance
		m_CaptureButtons = component;
		
		// set capture button images
		if(m_IsEntered)
			this.setCaptureButtonImages();
	}
	
	
	// Called when controller found.
	private void onControllerFound(TimelapseController controller)
	{
		m_Controller = controller;
		if(m_IsEntered)
		{
			Log.w(TAG, "onControllerFound() - Enter controller again");
			if(!controller.enter())
				Log.e(TAG, "onControllerFound() - Fail to enter controller");
		}
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		ComponentUtils.findComponent(this.getCameraActivity(), CaptureButtons.class, this, new ComponentSearchCallback<CaptureButtons>()
		{
			@Override
			public void onComponentFound(CaptureButtons component)
			{
				onCaptureButtonsFound(component);
			}
		});
		if(this.isCameraThreadStarted())
		{
			ComponentUtils.findComponent(this.getCameraThread(), TimelapseController.class, this, new ComponentSearchCallback<TimelapseController>()
			{
				@Override
				public void onComponentFound(TimelapseController component)
				{
					onControllerFound(component);
				}
			});
		}
	}
	
	
	// Set capture button images.
	private void setCaptureButtonImages()
	{
		if(m_CaptureButtons == null || !m_IsEntered)
			return;
		CameraActivity camaraActivity = this.getCameraActivity();
		if(!Handle.isValid(m_CaptureButtonBgHandle))
			m_CaptureButtonBgHandle = m_CaptureButtons.setPrimaryButtonBackground(camaraActivity.getDrawable(R.drawable.capture_button_timelapse_border), 0);
		if(!Handle.isValid(m_CaptureButtonIconHandle))
			m_CaptureButtonIconHandle = m_CaptureButtons.setPrimaryButtonIcon(camaraActivity.getDrawable(R.drawable.capture_button_timelapse_icon), 0);
	}
}
