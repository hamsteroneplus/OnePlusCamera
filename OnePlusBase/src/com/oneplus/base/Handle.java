package com.oneplus.base;

/**
 * Object represents an operation.
 */
public abstract class Handle
{
	/**
	 * Handle name.
	 */
	public final String name;
	
	
	// Private fields
	private volatile boolean m_IsClosed;
	
	
	/**
	 * Initialize new Handle instance.
	 * @param name Handle name.
	 */
	protected Handle(String name)
	{
		this.name = name;
	}
	
	
	/**
	 * Close handle with default flags.
	 * @param handle Handle to close.
	 * @return Null handle.
	 */
	public static <T extends Handle> T close(T handle)
	{
		return close(handle, 0);
	}
	
	
	/**
	 * Close handle with flags.
	 * @param handle Handle to close.
	 * @param flags Flags.
	 * @return Null handle.
	 */
	public static <T extends Handle> T close(T handle, int flags)
	{
		if(handle != null)
		{
			synchronized(handle)
			{
				Handle actualHandle = (Handle)handle;
				if(actualHandle.m_IsClosed)
					return null;
				actualHandle.m_IsClosed = true;
			}
			handle.onClose(flags);
		}
		return null;
	}
	
	
	/**
	 * Close handle directly without any additional operations.
	 */
	protected final void closeDirectly()
	{
		synchronized(this)
		{
			m_IsClosed = true;
		}
	}
	
	
	/**
	 * Check whether given handle is valid (Non-Null and not closed) or not.
	 * @param handle Handle to check.
	 * @return Whether given handle is valid or not.
	 */
	public static boolean isValid(Handle handle)
	{
		return (handle != null && !handle.m_IsClosed);
	}
	
	
	/**
	 * Called when closing handle.
	 * @param flags Flags.
	 */
	protected abstract void onClose(int flags);
	
	
	// Get string represents this handle.
	@Override
	public String toString()
	{
		return this.name + " [" + this.hashCode() + "]";
	}
}
