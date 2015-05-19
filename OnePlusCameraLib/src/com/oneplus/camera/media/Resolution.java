package com.oneplus.camera.media;

import java.util.Locale;

import android.content.Context;
import android.util.Size;

import com.oneplus.util.AspectRatio;

/**
 * Media resolution.
 */
public final class Resolution implements Comparable<Resolution>
{
	// Private fields
	private AspectRatio m_AspectRatio;
	private final int m_Height;
	private final MediaType m_TargetType;
	private final int m_Width;
	
	
	/**
	 * Initialize new Resolution instance.
	 * @param type Target media type.
	 * @param width Pixel width.
	 * @param height Pixel height.
	 */
	public Resolution(MediaType type, int width, int height)
	{
		m_TargetType = type;
		m_Width = width;
		m_Height = height;
	}
	
	
	/**
	 * Initialize new Resolution instance.
	 * @param type Target media type.
	 * @param size Pixel size.
	 */
	public Resolution(MediaType type, Size size)
	{
		m_TargetType = type;
		m_Width = size.getWidth();
		m_Height = size.getHeight();
	}
	
	
	// Compare to another resolution.
	@Override
	public int compareTo(Resolution resolution)
	{
		if(resolution != null)
		{
			int size = (m_Width * m_Height);
			int anotherSize = (resolution.m_Width * resolution.m_Height);
			return (size - anotherSize);
		}
		return 1;
	}
	
	
	// Check equality.
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Resolution)
		{
			Resolution resolution = (Resolution)obj;
			return (m_TargetType == resolution.m_TargetType
					&& m_Width == resolution.m_Width
					&& m_Height == resolution.m_Height);
		}
		return false;
	}
	
	
	/**
	 * Get {@link Resolution} instance from key.
	 * @param key Key.
	 * @return {@link Resolution} instance, or Null if key is invalid.
	 */
	public static Resolution fromKey(String key)
	{
		// check parameter
		if(key == null)
			return null;
		
		// check media type
		MediaType targetType;
		int index = key.indexOf('_');
		if(index < 0)
			return null;
		try
		{
			targetType = Enum.valueOf(MediaType.class, key.substring(0, index));
		}
		catch(Throwable ex)
		{
			return null;
		}
		
		// check size
		int width, height;
		int sizeSeparatorIndex = key.indexOf('x', index + 1);
		if(sizeSeparatorIndex < 0)
			return null;
		try
		{
			width = Integer.parseInt(key.substring(index + 1, sizeSeparatorIndex));
			height = Integer.parseInt(key.substring(sizeSeparatorIndex + 1));
		}
		catch(Throwable ex)
		{
			return null;
		}
		
		// create
		return new Resolution(targetType, width, height);
	}
	
	
	/**
	 * Get aspect ratio.
	 * @return Aspect ratio.
	 */
	public AspectRatio getAspectRatio()
	{
		if(m_AspectRatio == null)
			m_AspectRatio = AspectRatio.get(m_Width, m_Height);
		return m_AspectRatio;
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
	 * Get key represents this resolution.
	 * @return Key.
	 */
	public String getKey()
	{
		return (m_TargetType + "_" + m_Width + "x" + m_Height);
	}
	
	
	/**
	 * Get mega-pixel count.
	 * @return Mega-pixel count.
	 */
	public float getMegaPixels()
	{
		return (m_Width * m_Height / 1024f / 1024f);
	}
	
	
	/**
	 * Get description for mega-pixels.
	 * @return Description for mega-pixels.
	 */
	public String getMegaPixelsDescription()
	{
		float mp = (m_Width * m_Height / 1024f / 1024f);
		return String.format(Locale.US, "%.1fMP", mp);
	}
	
	
	/**
	 * Get description for menu.
	 * @param context Context.
	 * @return Description for menu.
	 */
	public String getMenuDescription(Context context)
	{
		return (this.getMegaPixelsDescription() + " (" + m_Width + "x" + m_Height + ")");
	}
	
	
	/**
	 * Get target media type.
	 * @return Target media type.
	 */
	public MediaType getTargetType()
	{
		return m_TargetType;
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
	 * Check whether this is a 1080p (Full HD) video resolution or not.
	 * @return 1080p video resolution or not.
	 */
	public boolean is1080pVideo()
	{
		return (m_TargetType == MediaType.VIDEO
				&& m_Width == 1920
				&& (m_Height == 1080 || m_Height == 1088));
	}
	
	
	/**
	 * Check whether this is a 4K video resolution or not.
	 * @return 4K video resolution or not.
	 */
	public boolean is4kVideo()
	{
		return (m_TargetType == MediaType.VIDEO
				&& (m_Width == 4096 || m_Width == 3840)
				&& m_Height == 2160);
	}
	
	
	/**
	 * Check whether this is a 720p (HD) video resolution or not.
	 * @return 720p video resolution or not.
	 */
	public boolean is720pVideo()
	{
		return (m_TargetType == MediaType.VIDEO
				&& m_Width == 1270
				&& m_Height == 720);
	}
	
	
	/**
	 * Check whether this is a MMS (QCIF) video resolution or not.
	 * @return MMS video resolution or not.
	 */
	public boolean isMmsVideo()
	{
		return (m_TargetType == MediaType.VIDEO
				&& m_Width == 176
				&& m_Height == 144);
	}
	
	
	/**
	 * Check whether this resolution is wider than given one or not.
	 * @param resolution Another resolution to check.
	 * @return Whether resolution is wider than given one or not.
	 */
	public boolean isWiderThan(Resolution resolution)
	{
		if(resolution != null)
			return (((float)m_Width / m_Height) > ((float)resolution.m_Width / resolution.m_Height));
		return false;
	}
	
	
	/**
	 * Convert to {@link android.util.Size}.
	 * @return {@link android.util.Size Size}.
	 */
	public Size toSize()
	{
		return new Size(m_Width, m_Height);
	}
	
	
	// Get string represents this resolution.
	@Override
	public String toString()
	{
		AspectRatio ratio = this.getAspectRatio();
		if(ratio != AspectRatio.UNKNOWN)
			return (m_Width + "x" + m_Height + "(" + ratio + ", " + this.getMegaPixelsDescription() + ")");
		return (m_Width + "x" + m_Height + "(" + this.getMegaPixelsDescription() + ")");
	}
}
