package com.oneplus.camera.ui;

import java.util.ArrayDeque;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.oneplus.base.Handle;
import com.oneplus.base.RecyclableObject;
import com.oneplus.base.component.Component;

/**
 * 2D camera preview overlay instance.
 */
public interface CameraPreviewOverlay extends Component
{
	/**
	 * Overlay renderer interface.
	 */
	public interface Renderer
	{
		void onRender(Canvas canvas, RenderingParams params);
	}
	
	
	/**
	 * Overlay rendering parameters.
	 */
	public class RenderingParams implements RecyclableObject
	{
		private static final int POOL_SIZE = 2;
		private static final ArrayDeque<RenderingParams> POOL = new ArrayDeque<>(POOL_SIZE);
		private volatile boolean m_IsFreeInstance;
		private volatile RectF m_PreviewBounds;
		
		// Constructor.
		private RenderingParams()
		{}
		
		/**
		 * Get camera preview pixel bounds on canvas.
		 * @return
		 */
		public RectF getPreviewBounds()
		{
			return m_PreviewBounds;
		}
		
		/**
		 * Obtain an available {@link RenderingParams} instance to use.
		 * @param previewBounds Camera preview bounds on screen.
		 * @return {@link RenderingParams} instance
		 */
		public static synchronized RenderingParams obtain(RectF previewBounds)
		{
			RenderingParams params = POOL.pollLast();
			if(params != null)
				params.m_IsFreeInstance = false;
			else
				params = new RenderingParams();
			params.m_PreviewBounds = previewBounds;
			return params;
		}
		
		/**
		 * Put instance back to pool for future usage.
		 */
		@Override
		public void recycle()
		{
			synchronized(RenderingParams.class)
			{
				if(m_IsFreeInstance)
					return;
				m_PreviewBounds = null;
				m_IsFreeInstance = true;
				if(POOL.size() < POOL_SIZE)
					POOL.addLast(this);
			}
		}
	}
	
	
	/**
	 * Add renderer to render preview overlay.
	 * @param renderer Renderer to add.
	 * @param flags Flags, reserved.
	 * @return Handle to overlay renderer.
	 */
	Handle addRenderer(Renderer renderer, int flags);
	
	
	/**
	 * Invalidate camera preview overlay to trigger redrawing.
	 */
	void invalidateCameraPreviewOverlay();
}
