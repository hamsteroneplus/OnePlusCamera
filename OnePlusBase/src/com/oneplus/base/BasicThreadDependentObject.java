package com.oneplus.base;

/**
 * Base implementation of {@link ThreadDependentObject}.
 */
public abstract class BasicThreadDependentObject implements ThreadDependentObject
{
	/**
	 * Log tag.
	 */
	protected String TAG;
	
	
	// Private fields
	private final Thread m_DependencyThread;
	
	
	/**
	 * Initialize new BasicThreadDependentObject instance.
	 * @param tag Log tag.
	 */
	protected BasicThreadDependentObject(String tag)
	{
		this.TAG = tag;
		m_DependencyThread = Thread.currentThread();
	}
	
	
	/**
	 * Initialize new BasicThreadDependentObject instance.
	 */
	protected BasicThreadDependentObject()
	{
		this.TAG = this.getClass().getSimpleName();
		m_DependencyThread = Thread.currentThread();
	}
	
	
	// Check whether current thread is the thread which object depends on or not.
	@Override
	public final boolean isDependencyThread()
	{
		return (m_DependencyThread == Thread.currentThread());
	}
	
	
	/**
	 * Throws {@link RuntimeException} if current thread is not dependency thread.
	 */
	protected final void verifyAccess()
	{
		if(m_DependencyThread != Thread.currentThread())
			throw new RuntimeException("Cross-thread access.");
	}
}
