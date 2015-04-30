package com.oneplus.camera.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Size;

import com.oneplus.base.Log;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.Settings;

/**
 * Default implementation of {@link VideoResolutionSelector}.
 */
public class DefaultVideoResolutionSelector extends DefaultResolutionSelector implements VideoResolutionSelector
{
	/**
	 * Settings key of current main (back) camera video resolution.
	 */
	public static final String SETTINGS_KEY_RESOLUTION_BACK = "Resolution.Video.Back";
	/**
	 * Settings key of current front camera video resolution.
	 */
	public static final String SETTINGS_KEY_RESOLUTION_FRONT = "Resolution.Video.Front";
	
	
	// Private static fields.
	//private static final 
	
	
	// Static initializer
	static
	{
		Settings.addPrivateKey(SETTINGS_KEY_RESOLUTION_BACK);
		Settings.addPrivateKey(SETTINGS_KEY_RESOLUTION_FRONT);
	}
	
	
	/**
	 * Initialize new DefaultVideoResolutionSelector instance.
	 * @param cameraActivity Camera activity.
	 */
	protected DefaultVideoResolutionSelector(CameraActivity cameraActivity)
	{
		super(cameraActivity);
	}
	
	
	// Save selected resolution.
	@Override
	public void saveResolution(Camera camera, Settings settings, Resolution resolution)
	{
		switch(camera.get(Camera.PROP_LENS_FACING))
		{
			case BACK:
				settings.set(SETTINGS_KEY_RESOLUTION_BACK, resolution.getKey());
				break;
			case FRONT:
				settings.set(SETTINGS_KEY_RESOLUTION_FRONT, resolution.getKey());
				break;
			default:
				Log.w(TAG, "saveResolution() - Unknown camera lens facing : " + camera.get(Camera.PROP_LENS_FACING));
				break;
		}
	}

	
	// Select resolution from available resolutions.
	@Override
	public Resolution selectResolution(Camera camera, Settings settings, List<Resolution> resolutionList, Resolution currentResolution, Restriction restriction)
	{
		// get saved resolution
		if(currentResolution == null)
		{
			switch(camera.get(Camera.PROP_LENS_FACING))
			{
				case BACK:
					currentResolution = Resolution.fromKey(settings.getString(SETTINGS_KEY_RESOLUTION_BACK));
					break;
				case FRONT:
					currentResolution = Resolution.fromKey(settings.getString(SETTINGS_KEY_RESOLUTION_FRONT));
					break;
				default:
					Log.w(TAG, "selectResolution() - Unknown camera lens facing : " + camera.get(Camera.PROP_LENS_FACING));
					break;
			}
		}
		
		// use current resolution
		if(currentResolution != null)
		{
			for(int i = resolutionList.size() - 1 ; i >= 0 ; --i)
			{
				Resolution resolution = resolutionList.get(i);
				if(resolution != null && resolution.equals(currentResolution))
					return resolution;
			}
		}
		
		// use resolution closest to screen size
		Size screenSize = this.getCameraActivity().get(CameraActivity.PROP_SCREEN_SIZE).toSize();
		int screenPixelCount = (screenSize.getWidth() * screenSize.getHeight());
		int minPixelCountDiff = 0;
		currentResolution = null;
		for(int i = resolutionList.size() - 1 ; i >= 0 ; --i)
		{
			Resolution resolution = resolutionList.get(i);
			int pixelDiff = Math.abs(screenPixelCount - (resolution.getWidth() * resolution.getHeight()));
			if(currentResolution == null || pixelDiff < minPixelCountDiff)
			{
				currentResolution = resolution;
				minPixelCountDiff = pixelDiff;
			}
		}
		if(currentResolution != null)
			return currentResolution;
		
		// no available resolution
		Log.e(TAG, "selectResolution() - Empty resolution list");
		return null;
	}

	
	// Select available resolutions.
	@Override
	public List<Resolution> selectResolutions(Camera camera, Settings settings, Restriction restriction)
	{
		List<Size> allSizes = camera.get(Camera.PROP_VIDEO_SIZES);
		ArrayList<Resolution> resolutions = new ArrayList<>();
		boolean has4K = false;
		boolean has2K = false;
		for(int i = allSizes.size() - 1 ; i >= 0 ; --i)
		{
			Size size = allSizes.get(i);
			if(Restriction.match(restriction, size))
			{
				// check 4K video
				int width = size.getWidth();
				int height = size.getHeight();
				if(!has4K && (width == 4096 || width == 3840) && height == 2160)
				{
					resolutions.add(new Resolution(MediaType.VIDEO, width, height));
					has4K = true;
					continue;
				}
				
				// check 2K video
				if(!has2K && width == 1920 && (height == 1080 || height == 1088))
				{
					resolutions.add(new Resolution(MediaType.VIDEO, width, height));
					has2K = true;
					continue;
				}
				
				// check other videos
				if((width == 1280 && height == 720)
					|| (width == 176 && height == 144))
				{
					resolutions.add(new Resolution(MediaType.VIDEO, width, height));
				}
			}
		}
		Collections.sort(resolutions);
		Collections.reverse(resolutions);
		return resolutions;
	}
	
}
