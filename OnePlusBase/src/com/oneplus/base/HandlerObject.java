package com.oneplus.base;

import android.os.Handler;

/**
 * Interface for object which owns an {@link android.os.Handler}.
 */
public interface HandlerObject extends ThreadDependentObject
{
	/**
	 * Get owned {@link android.os.Handler Handler}.
	 * @return {@link android.os.Handler}.
	 */
	Handler getHandler();
}
