package com.oneplus.camera;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oneplus.base.Log;
import com.oneplus.base.component.ComponentCreationPriority;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.SystemClock;

final class CameraDeviceManagerImpl extends CameraThreadComponent implements CameraDeviceManager
{
	// Private fields
	private CameraManager m_CameraManager;
	
	
	// Constructor
	CameraDeviceManagerImpl(CameraThread cameraThread)
	{
		super("Camera device manager", cameraThread, true);
	}
	
	
	// Deinitialize.
	@Override
	protected void onDeinitialize()
	{
		// clear reference
		m_CameraManager = null;
		
		// call super
		super.onDeinitialize();
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// get camera manager
		m_CameraManager = (CameraManager)this.getContext().getSystemService(Context.CAMERA_SERVICE);
		
		// refresh list
		this.refreshCameraList();
	}
	
	
	// Refresh camera list.
	@SuppressWarnings("unchecked")
	private void refreshCameraList()
	{
		// check state
		if(m_CameraManager == null)
		{
			Log.e(TAG, "refreshCameraList() - No CameraManager");
			return;
		}
		
		// get current list
		List<Camera> currentList = this.get(PROP_AVAILABLE_CAMERAS);
		
		// create new list
		long time = SystemClock.elapsedRealtime();
		List<Camera> newList;
		try
		{
			String[] cameraIdList = m_CameraManager.getCameraIdList();
			Camera[] cameras = new Camera[cameraIdList.length];
			for(int i = cameraIdList.length - 1 ; i >= 0 ; --i)
			{
				String id = cameraIdList[i];
				if(currentList != null)
				{
					for(int j = currentList.size() - 1 ; j >= 0 ; --j)
					{
						Camera camera = currentList.get(j);
						if(camera.get(Camera.PROP_ID).equals(id))
						{
							cameras[i] = camera;
							break;
						}
					}
				}
				if(cameras[i] == null)
					cameras[i] = new CameraImpl(m_CameraManager, id, m_CameraManager.getCameraCharacteristics(id));
			}
			newList = Arrays.asList(cameras);
		} 
		catch (Throwable ex)
		{
			Log.e(TAG, "refreshCameraList() - Fail to create camera list", ex);
			newList = (List<Camera>)Collections.EMPTY_LIST;
		}
		
		// check time
		time = (SystemClock.elapsedRealtime() - time);
		Log.v(TAG, "refreshCameraList() - Takes ", time, "ms to refresh list, cameras : ", newList);
		
		// update property
		this.setReadOnly(PROP_AVAILABLE_CAMERAS, newList);
	}
}

class CameraDeviceManagerBuilder extends CameraThreadComponentBuilder
{
	public CameraDeviceManagerBuilder()
	{
		super(ComponentCreationPriority.LAUNCH, CameraDeviceManagerImpl.class);
	}

	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new CameraDeviceManagerImpl(cameraThread);
	}
}
