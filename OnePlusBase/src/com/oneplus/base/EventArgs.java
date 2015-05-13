package com.oneplus.base;

/**
 * Base class for event data.
 */
public class EventArgs implements Cloneable
{
	/**
	 * Empty event data.
	 */
	public static final EventArgs EMPTY = new EventArgs();
	
	
	// Private fields.
	private volatile boolean m_IsHandled;
	
	
	/**
	 * Initialize new EventArgs instance.
	 */
	protected EventArgs()
	{}
	
	
	/**
	 * Set handled state back to False.
	 */
	protected final void clearHandledState()
	{
		m_IsHandled = false;
	}
	
	
	/**
	 * Clone this event data.
	 */
	public EventArgs clone()
	{
		try
		{
			return (EventArgs)super.clone();
		}
		catch(CloneNotSupportedException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	
	/**
	 * Check whether event is handled or not.
	 * @return Whether event is handled or not.
	 */
	public final boolean isHandled()
	{
		return m_IsHandled;
	}
	
	
	/**
	 * Mark that event has been handled.
	 */
	public final void setHandled()
	{
		m_IsHandled = true;
	}
}
