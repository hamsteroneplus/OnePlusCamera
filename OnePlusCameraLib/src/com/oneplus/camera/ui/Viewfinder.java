package com.oneplus.camera.ui;

import android.graphics.Rect;
import android.util.Size;

import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Viewfinder interface.
 */
public interface Viewfinder extends Component
{
	/**
	 * Read-only property for maximum preview container size on screen.
	 */
	PropertyKey<Size> PROP_PREVIEW_CONTAINER_SIZE = new PropertyKey<>("PreviewContainerSize", Size.class, Viewfinder.class, new Size(0, 0));
	/**
	 * Read-only property for current preview pixel bounds on screen.
	 */
	PropertyKey<Rect> PROP_PREVIEW_BOUNDS = new PropertyKey<>("PreviewBounds", Rect.class, Viewfinder.class, new Rect());
	/**
	 * Read-only property for prepared camera preview receiver.
	 */
	PropertyKey<Object> PROP_PREVIEW_RECEIVER = new PropertyKey<>("PreviewReceiver", Object.class, Viewfinder.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property for current preview rendering mode.
	 */
	PropertyKey<PreviewRenderingMode> PROP_PREVIEW_RENDERING_MODE = new PropertyKey<>("PreviewRenderingMode", PreviewRenderingMode.class, Viewfinder.class, PreviewRenderingMode.DIRECT);
	
	
	/**
	 * Camera preview rendering mode.
	 */
	public enum PreviewRenderingMode
	{
		/**
		 * Direct output by camera device.
		 */
		DIRECT,
		/**
		 * Rendered by OpenGL.
		 */
		OPENGL,
	}
}
