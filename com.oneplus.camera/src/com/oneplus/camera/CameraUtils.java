package com.oneplus.camera;

import java.util.List;

/**
 * Utility methods for {@link Camera}.
 */
public final class CameraUtils
{
	// Constructor
	private CameraUtils()
	{}
	
	
	/**
	 * Find camera with specific facing.
	 * @param list Camera list to search.
	 * @param facing Camera lens facing.
	 * @param isRemovable Select removable camera or not.
	 * @return Found camera, or Null if no matched camera.
	 */
	public static Camera findCamera(List<Camera> list, Camera.LensFacing facing, boolean isRemovable)
	{
		if(list != null)
		{
			for(int i = list.size() - 1 ; i >= 0 ; --i)
			{
				Camera camera = list.get(i);
				if(camera != null 
						&& camera.get(Camera.PROP_LENS_FACING) == facing
						&& isNonRemovableCamera(camera.get(Camera.PROP_ID)) != isRemovable)
				{
					return camera;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Find camera with specific ID.
	 * @param list Camera list to search.
	 * @param id Camera ID.
	 * @return Found camera, or Null if no matched camera.
	 */
	public static Camera findCamera(List<Camera> list, String id)
	{
		if(list != null)
		{
			for(int i = list.size() - 1 ; i >= 0 ; --i)
			{
				Camera camera = list.get(i);
				if(camera != null && camera.get(Camera.PROP_ID).equals(id))
					return camera;
			}
		}
		return null;
	}
	
	
	/**
	 * Check whether given ID represents non-removable camera or not.
	 * @param id Camera ID to check.
	 * @return Whether it is non-removable camera ID or not.
	 */
	public static boolean isNonRemovableCamera(String id)
	{
		for(int i = id.length() - 1 ; i >= 0 ; --i)
		{
			char c = id.charAt(i);
			if(c < '0' || c > '9')
				return false;
		}
		return true;
	}
}
