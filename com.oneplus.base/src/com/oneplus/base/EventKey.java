package com.oneplus.base;

/**
 * Key to represent an event.
 * @param <TArgs> Type of event data.
 */
public final class EventKey<TArgs extends EventArgs>
{
	/**
	 * Flag indicates that event can be interrupted by handler.
	 */
	public static final int FLAG_INTERRUPTIBLE = 0x1;
	
	
	/**
	 * Type of event data.
	 */
	public final Class<TArgs> argumentType;
	/**
	 * Flags.
	 */
	public final int flags;
	/**
	 * Unique event ID.
	 */
	public final int id;
	/**
	 * Event name.
	 */
	public final String name;
	/**
	 * Type of object owns this event.
	 */
	public final Class<? extends EventSource> ownerType;
	
	
	// Private static fields
	private static volatile int m_NextId = 1;
	
	
	/**
	 * Initialize new EventKey instance.
	 * @param name Event name.
	 * @param argType Type of event data.
	 * @param ownerType Type of object owns this event.
	 */
	public EventKey(String name, Class<TArgs> argType, Class<? extends EventSource> ownerType)
	{
		this(name, argType, ownerType, 0);
	}
	
	
	/**
	 * Initialize new EventKey instance.
	 * @param name Event name.
	 * @param argType Type of event data.
	 * @param ownerType Type of object owns this event.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_INTERRUPTIBLE FLAG_INTERRUPTIBLE}</li>
	 * </ul>
	 */
	public EventKey(String name, Class<TArgs> argType, Class<? extends EventSource> ownerType, int flags)
	{
		// check parameter
		if(name == null)
			throw new IllegalArgumentException("No property name.");
		if(argType == null)
			throw new IllegalArgumentException("No argument type.");
		if(ownerType == null)
			throw new IllegalArgumentException("No owner type.");
		
		// initialize
		this.argumentType = argType;
		this.flags = flags;
		this.id = generateId();
		this.name = name;
		this.ownerType = ownerType;
	}
	
	
	// Generate ID
	private static synchronized int generateId()
	{
		return (m_NextId++);
	}
	
	
	/**
	 * Check whether event can be interrupted by handler or not.
	 * @return Whether event can be interrupted by handler or not.
	 */
	public boolean isInterruptible()
	{
		return ((this.flags & FLAG_INTERRUPTIBLE) != 0);
	}
	
	
	// Get string represents this event.
	@Override
	public String toString()
	{
		return this.name + "(id=" + this.id + ")";
	}
}
