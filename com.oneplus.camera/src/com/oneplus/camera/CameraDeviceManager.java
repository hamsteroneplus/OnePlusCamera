package com.oneplus.camera;

import java.util.Collections;
import java.util.List;

import com.oneplus.base.EventKey;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Camera device manager interface.
 */
public interface CameraDeviceManager extends Component
{
	/**
	 * Property for available camera list.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Camera>> PROP_AVAILABLE_CAMERAS = new PropertyKey<List<Camera>>("AvailableCameras", (Class)List.class, CameraDeviceManager.class, Collections.EMPTY_LIST);
	
	
	/**
	 * Event raised when camera closed.
	 */
	EventKey<CameraIdEventArgs> EVENT_CAMERA_CLOSED = new EventKey<>("CameraClosed", CameraIdEventArgs.class, CameraDeviceManager.class);
	/**
	 * Event raised when camera open cancelled.
	 */
	EventKey<CameraIdEventArgs> EVENT_CAMERA_OPEN_CANCELLED = new EventKey<>("CameraOpenCancelled", CameraIdEventArgs.class, CameraDeviceManager.class);
	/**
	 * Event raised when camera open failed.
	 */
	EventKey<CameraIdEventArgs> EVENT_CAMERA_OPEN_FAILED = new EventKey<>("CameraOpenFailed", CameraIdEventArgs.class, CameraDeviceManager.class);
	/**
	 * Event raised when camera opened.
	 */
	EventKey<CameraDeviceEventArgs> EVENT_CAMERA_OPENED = new EventKey<>("CameraOpened", CameraDeviceEventArgs.class, CameraDeviceManager.class);
	/**
	 * Event raised when opening camera.
	 */
	EventKey<CameraIdEventArgs> EVENT_CAMERA_OPENING = new EventKey<>("CameraOpening", CameraIdEventArgs.class, CameraDeviceManager.class);
}
