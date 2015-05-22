package com.oneplus.base;

import java.util.ArrayDeque;

/**
 * Event data for property changing.
 */
public class PropertyChangeEventArgs<TValue> extends EventArgs implements RecyclableObject
{
	// Constants
	private static final int POOL_CAPACITY = 32;
	
	
	// Private static fields
	private static final ArrayDeque<PropertyChangeEventArgs<?>> m_Pool = new ArrayDeque<>(POOL_CAPACITY);
	
	
	// Private fields
	private volatile boolean m_IsAvailable;
	private volatile TValue m_NewValue;
	private volatile TValue m_OldValue;
	
	
	// Constructor
	private PropertyChangeEventArgs(TValue oldValue, TValue newValue)
	{
		m_OldValue = oldValue;
		m_NewValue = newValue;
	}
	
	
	/**
	 * Obtain an instance.
	 * @param oldValue Old property value.
	 * @param newValue New property value.
	 * @return {@link PropertyChangeEventArgs} instance.
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <TValue> PropertyChangeEventArgs<TValue> obtain(TValue oldValue, TValue newValue)
	{
		PropertyChangeEventArgs<TValue> e = (PropertyChangeEventArgs<TValue>)m_Pool.pollLast();
		if(e != null)
		{
			e.m_OldValue = oldValue;
			e.m_NewValue = newValue;
			e.m_IsAvailable = false;
		}
		else
			e = new PropertyChangeEventArgs<TValue>(oldValue, newValue);
		return e;
	}
	
	
	/**
	 * Get new property value.
	 * @return New property value.
	 */
	public final TValue getNewValue()
	{
		if(m_IsAvailable)
			throw new IllegalStateException();
		return m_NewValue;
	}
	
	
	/**
	 * Get old property value.
	 * @return Old property value.
	 */
	public final TValue getOldValue()
	{
		if(m_IsAvailable)
			throw new IllegalStateException();
		return m_OldValue;
	}
	
	
	/**
	 * Put instance back to pool.
	 */
	public final void recycle()
	{
		recycle(this);
	}
	
	
	// Put instance back to pool.
	private static synchronized void recycle(PropertyChangeEventArgs<?> e)
	{
		if(e.m_IsAvailable)
			return;
		if(m_Pool.size() < POOL_CAPACITY)
			m_Pool.push(e);
		e.m_OldValue = null;
		e.m_NewValue = null;
		e.m_IsAvailable = true;
		e.clearHandledState();
	}
}
