package com.oneplus.camera;

import java.util.ArrayDeque;
import java.util.Queue;

import com.oneplus.base.EventArgs;

/**
 * Event data for {@link Settings#EVENT_VALUE_CHANGED}.
 */
public class SettingsValueChangedEventArgs extends EventArgs
{
	// Constants
	private static final int POOL_SIZE = 8;
	
	
	// Private static fields
	private static final Queue<SettingsValueChangedEventArgs> POOL = new ArrayDeque<>(POOL_SIZE);
	
	
	// Private fields
	private volatile String m_Key;
	
	
	// Constructor
	private SettingsValueChangedEventArgs(String key)
	{
		m_Key = key;
	}
	
	
	/**
	 * Get key of changed value.
	 * @return Key.
	 */
	public final String getKey()
	{
		return m_Key;
	}
	
	
	/**
	 * Obtain available instance.
	 * @param key Key of changed value.
	 * @return {@link SettingsValueChangedEventArgs} instance.
	 */
	static synchronized SettingsValueChangedEventArgs obtain(String key)
	{
		SettingsValueChangedEventArgs e = POOL.poll();
		if(e != null)
			e.m_Key = key;
		else 
			e = new SettingsValueChangedEventArgs(key);
		return e;
	}
	
	
	/**
	 * Put instance back to pool.
	 */
	final void recycle()
	{
		synchronized(SettingsValueChangedEventArgs.class)
		{
			if(POOL.size() < POOL_SIZE)
				POOL.add(this);
		}
	}
}
