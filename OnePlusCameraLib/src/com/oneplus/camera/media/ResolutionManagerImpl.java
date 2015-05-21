package com.oneplus.camera.media;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.util.Size;

import com.oneplus.base.Handle;
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
	private Resolution m_PhotoResolution;
	private final LinkedList<ResolutionSelectorHandle> m_PhotoResSelectorHandles = new LinkedList<>();
	private Resolution m_VideoResolution;
	private final LinkedList<ResolutionSelectorHandle> m_VideoResSelectorHandles = new LinkedList<>();
	private Viewfinder m_Viewfinder;
	
	
	// Class for resolution selector handle.
	private final class ResolutionSelectorHandle extends Handle
	{
		public final ResolutionSelector selector;
		
		public ResolutionSelectorHandle(ResolutionSelector selector)
		{
			super("ResolutionSelector");
			this.selector = selector;
		}

		@Override
		protected void onClose(int flags)
		{
			restoreResolutionSelector(this, flags);
		}
	}
	
	
	// Constructor
	ResolutionManagerImpl(CameraActivity cameraActivity)
	{
		super("Resolution Manager", cameraActivity, false);
		this.enablePropertyLogs(PROP_PHOTO_PREVIEW_SIZE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PHOTO_RESOLUTION, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_VIDEO_PREVIEW_SIZE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_VIDEO_RESOLUTION, LOG_PROPERTY_CHANGE);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_PHOTO_RESOLUTION)
			return (TValue)m_PhotoResolution;
		if(key == PROP_VIDEO_RESOLUTION)
			return (TValue)m_VideoResolution;
		return super.get(key);
	}
	
	
	// Get active photo resolution selector.
	private PhotoResolutionSelector getPhotoResolutionSelector()
	{
		if(m_PhotoResSelectorHandles.isEmpty())
			return m_DefaultPhotoResSelector;
		return (PhotoResolutionSelector)m_PhotoResSelectorHandles.getLast().selector;
	}
	
	
	// Get preview container size.
	private Size getPreviewContainerSize()
	{
		if(m_Viewfinder != null)
			return m_Viewfinder.get(Viewfinder.PROP_PREVIEW_CONTAINER_SIZE);
		return this.getCameraActivity().get(CameraActivity.PROP_SCREEN_SIZE).toSize();
	}
	
	
	// Get active video resolution selector.
	private VideoResolutionSelector getVideoResolutionSelector()
	{
		if(m_VideoResSelectorHandles.isEmpty())
			return m_DefaultVideoResSelector;
		return (VideoResolutionSelector)m_VideoResSelectorHandles.getLast().selector;
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
	
	
	// Restore resolution selector.
	private void restoreResolutionSelector(ResolutionSelectorHandle handle, int flags)
	{
		// check state
		this.verifyAccess();
		
		// remove handle
		boolean syncPhotoRes = false;
		boolean syncVideoRes = false;
		Log.v(TAG, "restoreResolutionSelector() - Selector : ", handle.selector, ", handle : ", handle);
		if(handle.selector instanceof PhotoResolutionSelector)
		{
			syncPhotoRes = ((flags & FLAG_SYNC_RESOLUTION) != 0);
			m_PhotoResSelectorHandles.remove(handle);
		}
		if(handle.selector instanceof VideoResolutionSelector)
		{
			syncVideoRes = ((flags & FLAG_SYNC_RESOLUTION) != 0);
			m_VideoResSelectorHandles.remove(handle);
		}
		
		// update resolutions
		this.selectResolutions(syncPhotoRes, syncVideoRes);
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
		Size previewContainerSize = this.getPreviewContainerSize();
		
		// check selector
		PhotoResolutionSelector photoResSelector = this.getPhotoResolutionSelector();
		VideoResolutionSelector videoResSelector = this.getVideoResolutionSelector();
		
		// select photo resolution
		Settings settings = this.getSettings();
		List<Resolution> photoResolutions = this.selectResolutions(photoResSelector, camera, settings, null);
		Resolution currentPhotoResolution = m_PhotoResolution;
		Resolution photoResolution;
		Size photoPreviewSize;
		if(!photoResolutions.isEmpty())
		{
			photoResolution = (preservePhotoRes ? currentPhotoResolution : null);
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
		m_PhotoResolution = photoResolution;
		this.setReadOnly(PROP_PHOTO_RESOLUTION_LIST, photoResolutions);
		this.notifyPropertyChanged(PROP_PHOTO_RESOLUTION, currentPhotoResolution, photoResolution);
		this.setReadOnly(PROP_PHOTO_PREVIEW_SIZE, photoPreviewSize);
		
		// select photo resolution
		List<Resolution> videoResolutions = this.selectResolutions(videoResSelector, camera, settings, null);
		Resolution currentVideoResolution = m_VideoResolution;
		Resolution videoResolution;
		Size videoPreviewSize;
		if(!videoResolutions.isEmpty())
		{
			videoResolution = (preserveVideoRes ? currentVideoResolution : null);
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
		m_VideoResolution = videoResolution;
		this.setReadOnly(PROP_VIDEO_RESOLUTION_LIST, videoResolutions);
		this.notifyPropertyChanged(PROP_VIDEO_RESOLUTION, currentVideoResolution, videoResolution);
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
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_PHOTO_RESOLUTION)
			return this.setPhotoResolutionProp((Resolution)value);
		if(key == PROP_VIDEO_RESOLUTION)
			return this.setVideoResolutionProp((Resolution)value);
		return super.set(key, value);
	}
	
	
	// Set PROP_PHOTO_RESOLUTION property.
	private boolean setPhotoResolutionProp(Resolution resolution)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setPhotoResolutionProp() - Component is not running");
			return false;
		}
		
		// check resolution
		if((m_PhotoResolution != null && m_PhotoResolution.equals(resolution)) || m_PhotoResolution == resolution)
			return false;
		if(resolution != null && !this.get(PROP_PHOTO_RESOLUTION_LIST).contains(resolution))
		{
			Log.e(TAG, "setPhotoResolutionProp() - Resolution " + resolution + " is not contained in list");
			return false;
		}
		
		// get current camera
		Camera camera = this.getCameraActivity().get(CameraActivity.PROP_CAMERA);
		if(camera == null)
			Log.e(TAG, "selectResolutions() - No camera");
		
		// select preview size
		Settings settings = this.getSettings();
		PhotoResolutionSelector resolutionSelector = this.getPhotoResolutionSelector();
		Size previewSize = resolutionSelector.selectPreviewSize(camera, settings, this.getPreviewContainerSize(), resolution);
		
		// save resolution
		resolutionSelector.saveResolution(camera, settings, resolution);
		
		// update property
		Resolution oldResolution = m_PhotoResolution;
		m_PhotoResolution = resolution;
		this.notifyPropertyChanged(PROP_PHOTO_RESOLUTION, oldResolution, resolution);
		this.setReadOnly(PROP_PHOTO_PREVIEW_SIZE, previewSize);
		return true;
	}
	
	
	// Change photo or video resolution selector.
	@Override
	public Handle setResolutionSelector(ResolutionSelector selector, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setResolutionSelector() - Component is not running");
			return null;
		}
		
		// check parameter
		if(selector == null)
		{
			Log.e(TAG, "setResolutionSelector() - No resolution selector");
			return null;
		}
		
		// create handle
		boolean syncPhotoRes = false;
		boolean syncVideoRes = false;
		boolean isKnownType = false;
		ResolutionSelectorHandle handle = new ResolutionSelectorHandle(selector);
		Log.v(TAG, "setResolutionSelector() - Selector : ", selector, ", handle : ", handle);
		if(selector instanceof PhotoResolutionSelector)
		{
			isKnownType = true;
			syncPhotoRes = ((flags & FLAG_SYNC_RESOLUTION) != 0);
			m_PhotoResSelectorHandles.add(handle);
		}
		if(selector instanceof VideoResolutionSelector)
		{
			isKnownType = true;
			syncVideoRes = ((flags & FLAG_SYNC_RESOLUTION) != 0);
			m_VideoResSelectorHandles.add(handle);
		}
		if(!isKnownType)
		{
			Log.e(TAG, "setResolutionSelector() - Unknown selector type");
			return null;
		}
		
		// update resolutions
		this.selectResolutions(syncPhotoRes, syncVideoRes);
		
		// complete
		return handle;
	}
	
	
	// Set PROP_VIDEO_RESOLUTION property.
	private boolean setVideoResolutionProp(Resolution resolution)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setVideoResolutionProp() - Component is not running");
			return false;
		}
		
		// check resolution
		if((m_VideoResolution != null && m_VideoResolution.equals(resolution)) || m_VideoResolution == resolution)
			return false;
		if(resolution != null && !this.get(PROP_VIDEO_RESOLUTION_LIST).contains(resolution))
		{
			Log.e(TAG, "setVideoResolutionProp() - Resolution " + resolution + " is not contained in list");
			return false;
		}
		
		// get current camera
		Camera camera = this.getCameraActivity().get(CameraActivity.PROP_CAMERA);
		if(camera == null)
			Log.e(TAG, "selectResolutions() - No camera");
		
		// select preview size
		Settings settings = this.getSettings();
		VideoResolutionSelector resolutionSelector = this.getVideoResolutionSelector();
		Size previewSize = resolutionSelector.selectPreviewSize(camera, settings, this.getPreviewContainerSize(), resolution);
		
		// save resolution
		resolutionSelector.saveResolution(camera, settings, resolution);
		
		// update property
		Resolution oldResolution = m_VideoResolution;
		m_VideoResolution = resolution;
		this.notifyPropertyChanged(PROP_VIDEO_RESOLUTION, oldResolution, resolution);
		this.setReadOnly(PROP_VIDEO_PREVIEW_SIZE, previewSize);
		return true;
	}
}
