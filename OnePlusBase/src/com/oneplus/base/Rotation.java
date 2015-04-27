package com.oneplus.base;

/**
 * Rotation.
 */
public enum Rotation
{
	/**
	 * Landscape.
	 */
	LANDSCAPE,
	/**
	 * Inverse landscape.
	 */
	INVERSE_LANDSCAPE,
	/**
	 * Portrait.
	 */
	PORTRAIT,
	/**
	 * Inverse portrait.
	 */
	INVERSE_PORTRAIT,
	
	;
	
	/**
	 * Check whether this rotation is landscape (include inverse case) or not.
	 * @return Is landscape or not.
	 */
	public boolean isLandscape()
	{
		switch(this)
		{
			case LANDSCAPE:
			case INVERSE_LANDSCAPE:
				return true;
			default:
				return false;
		}
	}
	
	
	/**
	 * Check whether this rotation is portrait (include inverse case) or not.
	 * @return Is portrait or not.
	 */
	public boolean isPortrait()
	{
		switch(this)
		{
			case PORTRAIT:
			case INVERSE_PORTRAIT:
				return true;
			default:
				return false;
		}
	}
}
