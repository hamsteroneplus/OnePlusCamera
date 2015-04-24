package com.oneplus.util;

/**
 * Aspect ratio.
 */
public enum AspectRatio
{
	/**
	 * Unknown ratio.
	 */
	UNKNOWN(0, 0),
	/**
	 * 16 : 9.
	 */
	RATIO_16x9(16, 9),
	/**
	 * 16 : 10.
	 */
	RATIO_16x10(16, 10),
	/**
	 * 4 : 3.
	 */
	RATIO_4x3(4, 3),
	/**
	 * 3 : 2.
	 */
	RATIO_3x2(3, 2),
	/**
	 * 1 : 1.
	 */
	RATIO_1x1(1, 1),
	
	;
	
	// Fields
	private final float landscapeRatio;
	private final float portraitRatio;
	
	
	// Constructor
	AspectRatio(int width, int height)
	{
		if(height > 0)
		{
			this.landscapeRatio = ((float)width / height);
			this.portraitRatio = ((float)height / width);
		}
		else
		{
			this.landscapeRatio = 0;
			this.portraitRatio = 0;
		}
	}
	
	
	/**
	 * Get aspect ratio from given size.
	 * @param width Width.
	 * @param height Height.
	 * @return Aspect ratio, {@link #UNKNOWN} if the ratio is unknown.
	 */
	public static AspectRatio get(float width, float height)
	{
		if(width >= height)
		{
			if(height > 0)
			{
				double ratio = (width / height);
				AspectRatio[] ratios = values();
				for(int i = ratios.length - 1 ; i > 0 ; --i)
				{
					if(Math.abs(ratio - ratios[i].landscapeRatio) <= 0.03f)
						return ratios[i];
				}
			}
		}
		else
		{
			if(width > 0)
			{
				double ratio = (height / width);
				AspectRatio[] ratios = values();
				for(int i = ratios.length - 1 ; i > 0 ; --i)
				{
					if(Math.abs(ratio - ratios[i].portraitRatio) <= 0.03f)
						return ratios[i];
				}
			}
		}
		return UNKNOWN;
	}
}
