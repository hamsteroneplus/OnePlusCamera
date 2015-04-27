package com.oneplus.util;

import java.util.Comparator;

import android.util.Size;

/**
 * Comparator for {@link Size} according to its width*height.
 */
public class SizeComparator implements Comparator<Size>
{
	/**
	 * Default instance.
	 */
	public static final SizeComparator DEFAULT = new SizeComparator();
	
	
	// Compare
	@Override
	public int compare(Size x, Size y)
	{
		int sizeX = (x != null ? x.getWidth() * x.getHeight() : 0);
		int sizeY = (y != null ? y.getWidth() * y.getHeight() : 0);
		return (sizeX - sizeY);
	}
}
