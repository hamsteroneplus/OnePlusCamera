package com.oneplus.camera.media;

import java.util.List;

import com.oneplus.camera.Camera;
import com.oneplus.camera.Settings;

import android.util.Size;

/**
 * Media resolution selector interface.
 */
public interface ResolutionSelector
{
	/**
	 * Resolution selection restriction.
	 */
	public static final class Restriction
	{
		/**
		 * Maximum mega-pixels count, {@link Float#NaN} if no limitation.
		 */
		public final float maxMegaPixels;
		/**
		 * Maximum pixel size, Null if no limitation.
		 */
		public final Size maxSize;
		
		
		/**
		 * Initialize new Restriction instance.
		 * @param maxSize Maximum pixel size, Null if no limitation.
		 */
		public Restriction(Size maxSize)
		{
			this(maxSize, Float.NaN);
		}
		
		
		/**
		 * Initialize new Restriction instance.
		 * @param maxSize Maximum pixel size, Null if no limitation.
		 */
		public Restriction(float maxMegaPixels)
		{
			this(null, maxMegaPixels);
		}
		
		
		/**
		 * Initialize new Restriction instance.
		 * @param maxSize Maximum pixel size, Null if no limitation.
		 * @param maxMegaPixels Maximum mega-pixels count, {@link Float#NaN} if no limitation.
		 */
		public Restriction(Size maxSize, float maxMegaPixels)
		{
			this.maxSize = maxSize;
			this.maxMegaPixels = maxMegaPixels;
		}
		
		
		/**
		 * Check whether at least one limitation defined in restriction is valid or not.
		 * @param restriction {@link Restriction} to check.
		 * @return Whether restriction is valid or not.
		 */
		public static boolean hasRestriction(Restriction restriction)
		{
			if(restriction == null)
				return false;
			return (restriction.maxSize != null
					|| !Float.isNaN(restriction.maxMegaPixels));
		}
		
		
		/**
		 * Check whether given resolution can match the restriction or not.
		 * @param restriction Restriction, Null if no restriction.
		 * @param resolution Resolution to check.
		 * @return Whether given resolution can match the restriction or not.
		 */
		public static boolean match(Restriction restriction, Resolution resolution)
		{
			if(resolution == null)
				return false;
			if(restriction == null)
				return true;
			if(restriction.maxSize != null 
					&& (resolution.getWidth() > restriction.maxSize.getWidth() || resolution.getHeight() > restriction.maxSize.getHeight()))
			{
				return false;
			}
			if(!Float.isNaN(restriction.maxMegaPixels) && resolution.getMegaPixels() > restriction.maxMegaPixels)
				return false;
			return true;
		}
		
		
		/**
		 * Check whether given size can match the restriction or not.
		 * @param restriction Restriction, Null if no restriction.
		 * @param size Size to check.
		 * @return Whether given size can match the restriction or not.
		 */
		public static boolean match(Restriction restriction, Size size)
		{
			if(size == null)
				return false;
			if(restriction == null)
				return true;
			if(restriction.maxSize != null 
					&& (size.getWidth() > restriction.maxSize.getWidth() || size.getHeight() > restriction.maxSize.getHeight()))
			{
				return false;
			}
			if(!Float.isNaN(restriction.maxMegaPixels) && (size.getWidth() * size.getHeight() / 1024f / 1024) > restriction.maxMegaPixels)
				return false;
			return true;
		}
	}
	
	
	/**
	 * Save selected resolution.
	 * @param camera Camera.
	 * @param settings Current settings.
	 * @param resolution Resolution to save.
	 */
	void saveResolution(Camera camera, Settings settings, Resolution resolution);
	
	
	/**
	 * Select proper camera preview size for given resolution.
	 * @param camera Camera.
	 * @param settings Current settings.
	 * @param previewContainerSize Camera preview container (UI) size.
	 * @param resolution Current resolution.
	 * @return Selected camera preview size.
	 */
	Size selectPreviewSize(Camera camera, Settings settings, Size previewContainerSize, Resolution resolution);
	
	
	/**
	 * Select resolution from available resolutions.
	 * @param camera Camera.
	 * @param settings Current settings.
	 * @param resolutionList Available resolutions.
	 * @param currentResolution Current resolution.
	 * @param restriction Selection restriction.
	 * @return Selected resolution.
	 */
	Resolution selectResolution(Camera camera, Settings settings, List<Resolution> resolutionList, Resolution currentResolution, Restriction restriction);
	
	
	/**
	 * Select available resolutions.
	 * @param camera Camera.
	 * @param settings Current settings.
	 * @param restriction Selection restriction.
	 * @return Available resolutions.
	 */
	List<Resolution> selectResolutions(Camera camera, Settings settings, Restriction restriction);
}
