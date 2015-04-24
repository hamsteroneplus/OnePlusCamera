package com.oneplus.gl;

/**
 * Object which can be drawn by {@link DrawingContext}.
 */
public abstract class DrawableObject extends EglObject
{
	// Package fields
	DrawingContext drawingContext;
	DrawableObject prevDrawableObj;
	DrawableObject nextDrawableObj;
	
	
	/**
	 * Check whether object drawing contains alpha blending or not.
	 * @return Alpha blending state.
	 */
	public abstract boolean hasAlphaBlending();
	
	
	/**
	 * Called when drawing this object.
	 * @param dc {@link DrawingContext} instance.
	 */
	protected abstract void onDraw(DrawingContext dc);
}
