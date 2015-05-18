package com.oneplus.camera.scene;

import android.graphics.drawable.Drawable;

import com.oneplus.camera.InvalidMode;

final class NoScene extends InvalidMode<Scene> implements Scene 
{
	@Override
	public String getDisplayName()
	{
		return null;
	}

	
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		return null;
	}
}
