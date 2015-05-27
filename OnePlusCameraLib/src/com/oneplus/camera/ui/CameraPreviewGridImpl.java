package com.oneplus.camera.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.oneplus.base.BaseActivity.State;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.ui.CameraPreviewOverlay.RenderingParams;

final class CameraPreviewGridImpl extends CameraComponent implements CameraPreviewGrid, CameraPreviewOverlay.Renderer
{
	// Constants.
	private static final float STROKE_WIDTH = 2;
	
	
	// Private fields.
	private Paint m_StrokePaint;
	
	
	// Constructor.
	CameraPreviewGridImpl(CameraActivity cameraActivity)
	{
		super("Grid", cameraActivity, false);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		CameraPreviewOverlay previewOverlay = this.findComponent(CameraPreviewOverlay.class);
		
		// prepare drawing resources
		m_StrokePaint = new Paint();
		m_StrokePaint.setStyle(Paint.Style.STROKE);
		m_StrokePaint.setStrokeWidth(STROKE_WIDTH);
		m_StrokePaint.setColor(Color.WHITE);
		
		// add preview renderer
		if(previewOverlay != null)
			previewOverlay.addRenderer(this, 0);
		
		// add call-backs
		CameraActivity activity = this.getCameraActivity();
		activity.addCallback(CameraActivity.PROP_STATE, new PropertyChangedCallback<State>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<State> key, PropertyChangeEventArgs<State> e)
			{
				setReadOnly(PROP_IS_VISIBLE, getSettings().getBoolean(SETTINGS_KEY_IS_GRID_VISIBLE));
			}
		});
		
		// setup initial grid states
		this.setReadOnly(PROP_IS_VISIBLE, this.getSettings().getBoolean(SETTINGS_KEY_IS_GRID_VISIBLE));
	}

	
	// Called when rendering preview overlay.
	@Override
	public void onRender(Canvas canvas, RenderingParams params)
	{
		if(this.get(PROP_IS_VISIBLE))
		{
			// draw horizontal lines
			RectF bounds = params.getPreviewBounds();
			float y = (bounds.top + (bounds.height() / 3) - (STROKE_WIDTH / 2));
			canvas.drawLine(bounds.left, y, bounds.right, y, m_StrokePaint);
			y = (bounds.top + (bounds.height() * 2 / 3) - (STROKE_WIDTH / 2));
			canvas.drawLine(bounds.left, y, bounds.right, y, m_StrokePaint);
			
			// draw vertical lines
			float x = (bounds.left + (bounds.width() / 3) + (STROKE_WIDTH / 2));
			canvas.drawLine(x, bounds.top, x, bounds.bottom, m_StrokePaint);
			x = (bounds.left + (bounds.width() * 2 / 3) + (STROKE_WIDTH / 2));
			canvas.drawLine(x, bounds.top, x, bounds.bottom, m_StrokePaint);
		}
	}
}
