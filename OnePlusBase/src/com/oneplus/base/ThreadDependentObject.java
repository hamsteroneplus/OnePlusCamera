package com.oneplus.base;

/**
 * Interface for object which depends on specific thread.
 */
public interface ThreadDependentObject
{
	/**
	 * Check whether current thread is the thread which object depends on or not.
	 * @return Whether current thread is dependency thread or not.
	 */
	boolean isDependencyThread();
}
