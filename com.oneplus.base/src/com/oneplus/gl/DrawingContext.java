package com.oneplus.gl;

import android.opengl.GLES20;

/**
 * Schedules OpenGL object drawing, and holds related global states.
 */
public final class DrawingContext extends EglObject
{
	// Private fields
	private DrawableObject m_AlphaBlendingObjects;
	private final float[] m_MvpMatrix = new float[16];
	
	
	/**
	 * Abandon all pending object drawing.
	 */
	public void abandon()
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		
		// abandon pending objects
		this.abandonInternal();
	}
	
	
	// Abandon all pending object drawing.
	private void abandonInternal()
	{
		DrawableObject obj = m_AlphaBlendingObjects;
		while(obj != null)
		{
			DrawableObject nextObj = obj.nextDrawableObj;
			obj.drawingContext = null;
			obj.nextDrawableObj = null;
			if(nextObj != null)
				nextObj.prevDrawableObj = null;
			obj = nextObj;
		}
		m_AlphaBlendingObjects = null;
	}
	
	
	/**
	 * Change current MVP matrix.
	 * @param mvpMatrix New MVP matrix.
	 */
	public void changeMvpMatrix(float[] mvpMatrix)
	{
		this.commit();
		System.arraycopy(mvpMatrix, 0, m_MvpMatrix, 0, 16);
	}
	
	
	/**
	 * Commit all object drawing.
	 */
	public void commit()
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		
		// enable alpha blending
		if(m_AlphaBlendingObjects == null)
			return;
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		
		// commit
		while(m_AlphaBlendingObjects != null)
		{
			DrawableObject obj = m_AlphaBlendingObjects;
			m_AlphaBlendingObjects = obj.nextDrawableObj;
			if(m_AlphaBlendingObjects != null)
				m_AlphaBlendingObjects.prevDrawableObj = null;
			obj.drawingContext = null;
			obj.nextDrawableObj = null;
			obj.onDraw(this);
			if(obj.drawingContext == this && obj.nextDrawableObj != null)
				throw new RuntimeException("Recursive object drawing on " + obj);
		}
	}
	
	
	/**
	 * Draw given object.
	 * @param obj Object to draw.
	 */
	public void draw(DrawableObject obj)
	{
		// commit previous drawing
		if(obj.drawingContext != null)
		{
			obj.drawingContext.commit();
			if(obj.drawingContext != null)
				throw new RuntimeException("Recursive object drawing on " + obj);
		}
		
		// draw directly
		if(!obj.hasAlphaBlending())
		{
			obj.onDraw(this);
			return;
		}
		
		// draw later
		obj.nextDrawableObj = m_AlphaBlendingObjects;
		if(m_AlphaBlendingObjects != null)
			m_AlphaBlendingObjects.prevDrawableObj = obj;
		m_AlphaBlendingObjects = obj;
	}
	
	
	// Called when EGL context destroyed.
	@Override
	protected void onEglContextDestroyed()
	{
		this.abandonInternal();
		super.onEglContextDestroyed();
	}
	
	
	// Called when releasing object.
	@Override
	protected void onRelease()
	{
		this.abandonInternal();
		super.onRelease();
	}
}
