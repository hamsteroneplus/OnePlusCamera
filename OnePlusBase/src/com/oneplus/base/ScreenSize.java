package com.oneplus.base;

import android.content.Context;
import android.graphics.Point;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;

/**
 * Represent screen size.
 */
public final class ScreenSize implements Comparable<ScreenSize>
{
	/**
	 * Empty screen size.
	 */
	public static final ScreenSize EMPTY = new ScreenSize();
	
	
	// Private fields
	private final int m_Height;
	private final int m_Width;
	
	
	// Constructor
	private ScreenSize()
	{
		m_Width = 0;
		m_Height = 0;
	}
	
	
	/**
	 * Initialize new ScreenSize instance.
	 * @param context Context.
	 */
	public ScreenSize(Context context)
	{
		WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		Point size = new Point();
		display.getRealSize(size);
		m_Width = size.x;
		m_Height = size.y;
	}
	
	
	// Compare to another screen size.
	@Override
	public int compareTo(ScreenSize size)
	{
		if(size != null)
		{
			int pixelCount = (m_Width * m_Height);
			int anotherPixelCount = (size.m_Width * size.m_Height);
			return (pixelCount - anotherPixelCount);
		}
		return 1;
	}
	
	
	// Check equality.
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ScreenSize)
		{
			ScreenSize size = (ScreenSize)obj;
			return (m_Width == size.m_Width && m_Height == size.m_Height);
		}
		return false;
	}
	
	
	/**
	 * Get pixel height.
	 * @return Pixel height.
	 */
	public int getHeight()
	{
		return m_Height;
	}
	
	
	/**
	 * Get pixel width.
	 * @return Pixel width.
	 */
	public int getWidth()
	{
		return m_Width;
	}
	
	
	// Calculate hash code.
	@Override
	public int hashCode()
	{
		return ((m_Width << 16) | (m_Height & 0xFFFF));
	}
	
	
	/**
	 * Check whether screen size is Full HD or not.
	 * @return Whether screen size is Full HD or not.
	 */
	public boolean isFullHD()
	{
		return (m_Height == 1080);
	}
	
	
	/**
	 * Check whether screen size is HD or not.
	 * @return Whether screen size is HD or not.
	 */
	public boolean isHD()
	{
		return (m_Height == 720);
	}
	
	
	/**
	 * Convert to {@link android.util.Size}.
	 * @return {@link android.util.Size Size}.
	 */
	public Size toSize()
	{
		return new Size(m_Width, m_Height);
	}
	
	
	// Get string represents this size.
	@Override
	public String toString()
	{
		String name = null;
		switch(m_Height)
		{
			case 1080:
				name = "Full HD";
				break;
			case 720:
				name = "HD";
				break;
		}
		if(name != null)
			return (m_Width + "x" + m_Height + " (" + name + ")");
		return (m_Width + "x" + m_Height);
	}
}
