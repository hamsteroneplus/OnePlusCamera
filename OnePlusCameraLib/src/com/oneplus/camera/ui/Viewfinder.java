package com.oneplus.camera.ui;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Size;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Viewfinder interface.
 */
public interface Viewfinder extends Component
{
	/**
	 * Flag to ignore bounds checking.
	 */
	int FLAG_NO_BOUNDS_CHECKING = 0x1;
	
	
	/**
	 * Read-only property for maximum preview container size on screen.
	 */
	PropertyKey<Size> PROP_PREVIEW_CONTAINER_SIZE = new PropertyKey<>("PreviewContainerSize", Size.class, Viewfinder.class, new Size(0, 0));
	/**
	 * Read-only property for current preview pixel bounds on screen.
	 */
	PropertyKey<RectF> PROP_PREVIEW_BOUNDS = new PropertyKey<>("PreviewBounds", RectF.class, Viewfinder.class, new RectF());
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
	
	
	/**
	 * Calculate position on screen from relative position in preview.
	 * @param previewX Horizontal relative position in preview.
	 * @param previewY Vertical relative position in preview.
	 * @param result Result position on screen.
	 * @param flags Flags :
	 * <ul>
	 *   <li>{@link #FLAG_NO_BOUNDS_CHECKING}</li>
	 * </ul>
	 * @return Whether position conversion succeeded or not.
	 */
	boolean pointFromPreview(float previewX, float previewY, PointF result, int flags);
	
	
	/**
	 * Calculate relative position in preview from screen position.
	 * @param screenX Horizontal screen position.
	 * @param screenY Vertical screen position.
	 * @param result Result relative position in preview.
	 * @param flags Flags :
	 * <ul>
	 *   <li>{@link #FLAG_NO_BOUNDS_CHECKING}</li>
	 * </ul>
	 * @return Whether position conversion succeeded or not.
	 */
	boolean pointToPreview(float screenX, float screenY, PointF result, int flags);
}
