package com.oneplus.camera;

import com.oneplus.base.BasicBaseObject;

/**
 * Base class for invalid mode.
 */
public abstract class InvalidMode<T extends Mode<?>> extends BasicBaseObject implements Mode<T>
{
	/**
	 * Initialize new InvalidMode instance.
	 */
	protected InvalidMode()
	{}
	
	
	// Enter this mode.
	@Override
	public boolean enter(T prevMode, int flags)
	{
		return false;
	}
	
	
	// Exit from this mode.
	@Override
	public void exit(T nextMode, int flags)
	{}
	
	
	// Get string represents this mode.
	@Override
	public String toString()
	{
		return "(Invalid)";
	}
}
