package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

/**
 * Builder for recording timer UI.
 */
public final class RecordingTimerUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new RecordingTimerUIBuilder instance.
	 */
	public RecordingTimerUIBuilder()
	{
		super(RecordingTimerUI.class);
	}
	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new RecordingTimerUI(cameraActivity);
	}
}
