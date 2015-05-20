package com.oneplus.camera.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Size;

import com.oneplus.base.Log;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.Settings;
import com.oneplus.util.AspectRatio;
import com.oneplus.util.SizeComparator;

/**
 * Default implementation of {@link ResolutionSelector}.
 */
public abstract class DefaultResolutionSelector implements ResolutionSelector
{
	/**
	 * Log tag.
	 */
	protected final String TAG;
	
	
	// Private fields.
	private final CameraActivity m_CameraActivity;
	
	
	/**
	 * Initialize new DefaultResolutionSelector instance.
	 * @param cameraActivity Camera activity.
	 */
	protected DefaultResolutionSelector(CameraActivity cameraActivity)
	{
		this.TAG = this.getClass().getSimpleName();
		if(cameraActivity == null)
			throw new IllegalArgumentException("No camera activity");
		m_CameraActivity = cameraActivity;
	}
	
	
	/**
	 * Get camera activity.
	 * @return Camera activity.
	 */
	public final CameraActivity getCameraActivity()
	{
		return m_CameraActivity;
	}
	
	
	// Select proper camera preview size for given resolution.
	@Override
	public Size selectPreviewSize(Camera camera, Settings settings, Size previewContainerSize, Resolution resolution)
	{
		// check aspect ratio
		AspectRatio ratio = resolution.getAspectRatio();
		
		// check target size
		if(previewContainerSize == null)
			previewContainerSize = new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);
		if(previewContainerSize.getWidth() > resolution.getWidth() || previewContainerSize.getHeight() > resolution.getHeight())
		{
			previewContainerSize = new Size(
					Math.min(previewContainerSize.getWidth(), resolution.getWidth()), 
					Math.min(previewContainerSize.getHeight(), resolution.getHeight()));
		}
		
		// Select preview size
		Size largerSize = null;
		Size preferredSize = null;
		List<Size> allSizes = camera.get(Camera.PROP_PREVIEW_SIZES);
		if(allSizes != null)
		{
			SizeComparator comparator = SizeComparator.DEFAULT;
			for(int i = allSizes.size() - 1 ; i >= 0 ; --i)
			{
				Size size = allSizes.get(i);
				if(size != null && AspectRatio.get(size) == ratio)
				{
					int comparisonResult = comparator.compare(size, previewContainerSize);
					if(comparisonResult == 0)
						return size;
					if(comparisonResult > 0)
					{
						if(largerSize == null || comparator.compare(size, largerSize) < 0)
							largerSize = size;
					}
					else
					{
						if(preferredSize == null || comparator.compare(size, preferredSize) > 0)
							preferredSize = size;
					}
				}
			}
		}
		
		// complete
		if(preferredSize != null)
			return preferredSize;
		if(largerSize != null)
			return largerSize;
		Log.e(TAG, "selectPreviewSize() - No available preview size for " + resolution);
		return null;
	}
	
	
	/**
	 * Select available resolutions.
	 * @param camera Camera.
	 * @param settings Current settings.
	 * @param ratios Aspect ratios to select.
	 * @param numOfResolutions Number of resolutions for each aspect ratio.
	 * @param restriction Selection restriction.
	 * @return Selected resolution.
	 */
	protected final List<Resolution> selectResolutions(Camera camera, Settings settings, AspectRatio[] ratios, int numOfResolutions, Restriction restriction)
	{
		List<Size> allSizes = camera.get(Camera.PROP_PICTURE_SIZES);
		Collections.sort(allSizes, SizeComparator.DEFAULT);
		ArrayList<Resolution> resolutions = new ArrayList<>();
		for(int i = 0 ; i < ratios.length ; ++i)
		{
			AspectRatio ratio = ratios[i];
			for(int j = allSizes.size() - 1, remaining = numOfResolutions ; j >= 0 && remaining > 0 ; --j)
			{
				Size size = allSizes.get(j);
				if(size != null && AspectRatio.get(size) == ratio && Restriction.match(restriction, size))
				{
					resolutions.add(new Resolution(MediaType.PHOTO, size));
					--remaining;
				}
			}
		}
		return resolutions;
	}
}
