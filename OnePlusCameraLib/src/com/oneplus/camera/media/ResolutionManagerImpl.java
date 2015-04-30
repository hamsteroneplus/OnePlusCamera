package com.oneplus.camera.media;

import java.util.Collections;
import java.util.List;

import android.util.Size;

import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.Camera;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.CameraDeviceManager;
import com.oneplus.camera.Settings;
import com.oneplus.camera.media.ResolutionSelector.Restriction;
import com.oneplus.camera.ui.Viewfinder;

@SuppressWarnings("unused")
final class ResolutionManagerImpl extends CameraComponent implements ResolutionManager
{
	// Private fields
	private PhotoResolutionSelector m_DefaultPhotoResSelector;
	private VideoResolutionSelector m_DefaultVideoResSelector;
	private Viewfinder m_Viewfinder;
	
	
	// Constructor
	ResolutionManagerImpl(CameraActivity cameraActivity)
	{
		super("Resolution Manager", cameraActivity, false);
		this.enablePropertyLogs(PROP_PHOTO_PREVIEW_SIZE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PHOTO_RESOLUTION, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_VIDEO_PREVIEW_SIZE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_VIDEO_RESOLUTION, LOG_PROPERTY_CHANGE);
	}
	
	
	// Initialize component.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// create default selectors
		CameraActivity cameraActivity = this.getCameraActivity();
		m_DefaultPhotoResSelector = new DefaultPhotoResolutionSelector(cameraActivity);
		m_DefaultVideoResSelector = new DefaultVideoResolutionSelector(cameraActivity);
		
		// find components
		ComponentUtils.findComponent(this.getCameraActivity(), Viewfinder.class, this, new ComponentSearchCallback<Viewfinder>()
		{
			@Override
			public void onComponentFound(Viewfinder component)
			{
				m_Viewfinder = component;
				if(getCameraActivity().get(CameraActivity.PROP_CAMERA) != null)
					selectResolutions(true, true);
			}
		});
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, new PropertyChangedCallback<Camera>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Camera> key, PropertyChangeEventArgs<Camera> e)
			{
				selectResolutions(true, true);
			}
		});
		
		// setup initial resolutions
		if(cameraActivity.get(CameraActivity.PROP_CAMERA) != null && m_Viewfinder != null)
			this.selectResolutions(false, false);
	}
	
	
	// Refresh all resolutions.
	private boolean selectResolutions(boolean preservePhotoRes, boolean preserveVideoRes)
	{
		// check state
		if(m_Viewfinder == null)
		{
			Log.w(TAG, "selectResolutions() - No Viewfinder");
			return false;
		}
		
		Log.v(TAG, "selectResolutions(", preservePhotoRes, ", ", preserveVideoRes, ")");
		
		// get current camera
		Camera camera = this.getCameraActivity().get(CameraActivity.PROP_CAMERA);
		if(camera == null)
			Log.e(TAG, "selectResolutions() - No camera");
		
		// get preview container size
		Size previewContainerSize;
		if(m_Viewfinder != null)
			previewContainerSize = m_Viewfinder.get(Viewfinder.PROP_PREVIEW_CONTAINER_SIZE);
		else
			previewContainerSize = this.getCameraActivity().get(CameraActivity.PROP_SCREEN_SIZE).toSize();
		
		// check selector
		PhotoResolutionSelector photoResSelector = m_DefaultPhotoResSelector;
		VideoResolutionSelector videoResSelector = m_DefaultVideoResSelector;
		
		// select photo resolution
		Settings settings = this.getSettings();
		List<Resolution> photoResolutions = this.selectResolutions(photoResSelector, camera, settings, null);
		Resolution photoResolution;
		Size photoPreviewSize;
		if(!photoResolutions.isEmpty())
		{
			photoResolution = (preservePhotoRes ? this.get(PROP_PHOTO_RESOLUTION) : null);
			photoResolution = photoResSelector.selectResolution(camera, settings, photoResolutions, photoResolution, null);
			if(photoResolution != null)
				photoPreviewSize = photoResSelector.selectPreviewSize(camera, settings, previewContainerSize, photoResolution);
			else
				photoPreviewSize = new Size(0, 0);
		}
		else
		{
			Log.e(TAG, "selectResolutions() - Empty photo resolution list");
			photoResolution = null;
			photoPreviewSize = new Size(0, 0);
		}
		this.setReadOnly(PROP_PHOTO_RESOLUTION_LIST, photoResolutions);
		this.setReadOnly(PROP_PHOTO_RESOLUTION, photoResolution);
		this.setReadOnly(PROP_PHOTO_PREVIEW_SIZE, photoPreviewSize);
		
		// select photo resolution
		List<Resolution> videoResolutions = this.selectResolutions(videoResSelector, camera, settings, null);
		Resolution videoResolution;
		Size videoPreviewSize;
		if(!videoResolutions.isEmpty())
		{
			videoResolution = (preserveVideoRes ? this.get(PROP_VIDEO_RESOLUTION) : null);
			videoResolution = videoResSelector.selectResolution(camera, settings, videoResolutions, videoResolution, null);
			if(videoResolution != null)
				videoPreviewSize = videoResSelector.selectPreviewSize(camera, settings, previewContainerSize, videoResolution);
			else
				videoPreviewSize = new Size(0, 0);
		}
		else
		{
			Log.e(TAG, "selectResolutions() - Empty video resolution list");
			videoResolution = null;
			videoPreviewSize = new Size(0, 0);
		}
		this.setReadOnly(PROP_VIDEO_RESOLUTION_LIST, videoResolutions);
		this.setReadOnly(PROP_VIDEO_RESOLUTION, videoResolution);
		this.setReadOnly(PROP_VIDEO_PREVIEW_SIZE, videoPreviewSize);
		
		// complete
		return true;
	}
	
	
	// Select available resolutions by given selector.
	@SuppressWarnings("unchecked")
	private List<Resolution> selectResolutions(ResolutionSelector selector, Camera camera, Settings settings, Restriction restriction)
	{
		if(camera == null)
			return Collections.EMPTY_LIST;
		List<Resolution> resolutions;
		try
		{
			resolutions = selector.selectResolutions(camera, settings, restriction);
			if(resolutions == null)
			{
				Log.e(TAG, "selectResolutions() - Got Null resolution list from " + selector);
				resolutions = Collections.EMPTY_LIST;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "selectResolutions() - Fail to select resolutions", ex);
			resolutions = Collections.EMPTY_LIST;
		}
		if(resolutions.isEmpty())
			Log.e(TAG, "selectResolutions() - Empty resolution list");
		return resolutions;
	}
}
