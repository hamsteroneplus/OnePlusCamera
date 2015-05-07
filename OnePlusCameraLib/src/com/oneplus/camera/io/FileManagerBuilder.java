package com.oneplus.camera.io;

import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.CameraThreadComponentBuilder;

public class FileManagerBuilder extends CameraThreadComponentBuilder
{
	public FileManagerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, FileManagerImpl.class);
	}

	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new FileManagerImpl(cameraThread);
	}
}
