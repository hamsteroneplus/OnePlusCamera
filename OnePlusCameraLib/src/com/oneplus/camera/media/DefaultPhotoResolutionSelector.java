package com.oneplus.camera.media;

import java.util.List;

import com.oneplus.base.Log;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.Settings;
import com.oneplus.util.AspectRatio;

/**
 * Default implementation of {@link PhotoResolutionSelector}.
 */
public class DefaultPhotoResolutionSelector extends DefaultResolutionSelector implements PhotoResolutionSelector
{
	/**
	 * Settings key of current main (back) camera photo resolution.
	 */
	public static final String SETTINGS_KEY_RESOLUTION_BACK = "Resolution.Photo.Back";
	/**
	 * Settings key of current front camera photo resolution.
	 */
	public static final String SETTINGS_KEY_RESOLUTION_FRONT = "Resolution.Photo.Front";
	
	
	// Constants
	private static final AspectRatio[] ASPECT_RATIOS = new AspectRatio[]{
		AspectRatio.RATIO_4x3,
		AspectRatio.RATIO_16x9,
		AspectRatio.RATIO_1x1,
	};
	
	
	// Static initializer
	static
	{
		Settings.addPrivateKey(SETTINGS_KEY_RESOLUTION_BACK);
		Settings.addPrivateKey(SETTINGS_KEY_RESOLUTION_FRONT);
	}
	
	
	/**
	 * Initialize new DefaultPhotoResolutionSelector instance.
	 * @param cameraActivity Camera activity.
	 */
	protected DefaultPhotoResolutionSelector(CameraActivity cameraActivity)
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
		
		// use largest resolution
		if(resolutionList.size() > 0)
			return resolutionList.get(0);
		
		// no available resolution
		Log.e(TAG, "selectResolution() - Empty resolution list");
		return null;
	}
	
	
	// Select available resolutions.
	@Override
	public List<Resolution> selectResolutions(Camera camera, Settings settings, Restriction restriction)
	{
		return this.selectResolutions(camera, settings, ASPECT_RATIOS, 1, restriction);
	}
}
