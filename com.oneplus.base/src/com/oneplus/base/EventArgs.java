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
	
	
	/**
	 * Initialize new EventArgs instance.
	 */
	protected EventArgs()
	{}
	
	
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
}
