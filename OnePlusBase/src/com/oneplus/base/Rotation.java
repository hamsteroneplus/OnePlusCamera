package com.oneplus.base;

/**
 * Rotation.
 */
public enum Rotation
{
	/**
	 * Landscape.
	 */
	LANDSCAPE(270),
	/**
	 * Inverse landscape.
	 */
	INVERSE_LANDSCAPE(90),
	/**
	 * Portrait.
	 */
	PORTRAIT(0),
	/**
	 * Inverse portrait.
	 */
	INVERSE_PORTRAIT(180),
	
	;
	
	// Private fields.
	private final int m_DeviceOrientation;
	
	
	// Constructor.
	private Rotation(int deviceOrientation)
	{
		m_DeviceOrientation = deviceOrientation;
	}
	
	
	/**
	 * Get rotation from device orientation.
	 * @param orientation Device orientation.
	 * @return Rotation.
	 */
	public static Rotation fromDeviceOrientation(int orientation)
	{
		while(orientation < 0)
			orientation += 360;
		orientation %= 360;
		if(orientation >= 45 && orientation < 135)
			return INVERSE_LANDSCAPE;
		if(orientation >= 135 && orientation < 225)
			return INVERSE_PORTRAIT;
		if(orientation >= 225 && orientation < 315)
			return LANDSCAPE;
		return PORTRAIT;
	}
	
	
	/**
	 * Get device orientation in degrees [0-359].
	 * @return Device orientation.
	 */
	public int getDeviceOrientation()
	{
		return m_DeviceOrientation;
	}
	
	
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
