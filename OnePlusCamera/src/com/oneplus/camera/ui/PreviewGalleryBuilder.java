package com.oneplus.camera.ui;

import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.UIComponentBuilder;

public final class PreviewGalleryBuilder extends UIComponentBuilder
{
	public PreviewGalleryBuilder()
	{
		super(PreviewGallery.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new PreviewGallery(cameraActivity);
	}
}
