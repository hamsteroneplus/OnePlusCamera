package com.oneplus.camera.scene;

import android.graphics.drawable.Drawable;

import com.oneplus.camera.Mode;

/**
 * Scene interface.
 */
public interface Scene extends Mode<Scene>
{
	/**
	 * No scene.
	 */
	Scene NO_SCENE = new NoScene();
	
	
	/**
	 * Scene image usage.
	 */
	public enum ImageUsage
	{
		/**
		 * Icon on options panel.
		 */
		OPTIONS_PANEL_ICON,
	}
	
	
	/**
	 * Get string for display name.
	 * @return Display name.
	 */
	String getDisplayName();
	
	
	/**
	 * Get related image.
	 * @param usage Image usage.
	 * @return Image related to this scene.
	 */
	Drawable getImage(ImageUsage usage);
}
