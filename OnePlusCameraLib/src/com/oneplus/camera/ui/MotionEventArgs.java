package com.oneplus.camera.ui;

import java.util.ArrayDeque;

import android.view.MotionEvent;

import com.oneplus.base.EventArgs;
import com.oneplus.base.RecyclableObject;

/**
 * Data for motion related events.
 */
public class MotionEventArgs extends EventArgs implements RecyclableObject
{
	// Constants.
	private static final int POOL_SIZE = 8;
	
	
	// Private static fields.
	private static final ArrayDeque<MotionEventArgs> POOL = new ArrayDeque<>(POOL_SIZE);
	
	
	// Private fields.
	private volatile int m_Action;
	private volatile boolean m_IsFreeInstance;
	private volatile int m_PointerCount;
	private volatile float m_X;
	private volatile float m_Y;
	
	
	// Constructor.
	private MotionEventArgs()
	{}
	
	
	/**
	 * Get the kind of action being performed.
	 * @return The action.
	 */
	public final int getAction()
	{
		return m_Action;
	}
	
	
	/**
	 * Get number of pointers contained in this data.
	 * @return Number of pointers.
	 */
	public final int getPointerCount()
	{
		return m_PointerCount;
	}
	
	
	/**
	 * Get horizontal position.
	 * @return Horizontal position.
	 */
	public final float getX()
	{
		return m_X;
	}
	
	
	/**
	 * Get vertical position.
	 * @return Vertical position.
	 */
	public final float getY()
	{
		return m_Y;
	}
	
	
	/**
	 * Obtain an available instance.
	 * @param event {@link MotionEvent}.
	 * @return {@link MotionEventArgs} instance.
	 */
	public static synchronized MotionEventArgs obtain(MotionEvent event)
	{
		MotionEventArgs e = POOL.pollLast();
		if(e == null)
			e = new MotionEventArgs();
		e.m_Action = event.getAction();
		e.m_PointerCount = event.getPointerCount();
		e.m_X = event.getX();
		e.m_Y = event.getY();
		e.m_IsFreeInstance = false;
		return e;
	}
	
	
	/**
	 * Put instance back to pool for future usage.
	 */
	public void recycle()
	{
		synchronized(MotionEventArgs.class)
		{
			if(m_IsFreeInstance)
				return;
			m_IsFreeInstance = true;
			this.clearHandledState();
			if(POOL.size() < POOL_SIZE)
				POOL.addLast(this);
		}
	}
}
