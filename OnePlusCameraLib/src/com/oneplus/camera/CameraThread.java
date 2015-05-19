package com.oneplus.camera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Size;

import com.oneplus.base.BaseThread;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.base.ScreenSize;
import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.base.component.ComponentEventArgs;
import com.oneplus.base.component.ComponentManager;
import com.oneplus.base.component.ComponentOwner;
import com.oneplus.camera.io.FileManager;
import com.oneplus.camera.io.FileManagerBuilder;
import com.oneplus.camera.io.PhotoSaveTask;
import com.oneplus.camera.io.VideoSaveTask;
import com.oneplus.camera.media.AudioManager;
import com.oneplus.camera.media.MediaType;
import com.oneplus.camera.media.Resolution;

/**
 * Camera access and control thread.
 */
public class CameraThread extends BaseThread implements ComponentOwner
{
	// Default component builders
	private static final ComponentBuilder[] DEFAULT_COMPONENT_BUILDERS = new ComponentBuilder[]{
		new CameraDeviceManagerBuilder(), 
		new FileManagerBuilder(),
		new FocusControllerBuilder(),
	};
	
	
	// Constants
	private static final long DURATION_VIDEO_CAPTURE_DELAY = 300;
	private static final int MSG_SCREEN_SIZE_CHANGED = 10000;
	private static final int MSG_CAPTURE_VIDEO = 10010;
	
	
	/**
	 * Flag to indicate that operation should be performed synchronously.
	 */
	public static final int FLAG_SYNCHRONOUS = 0x1;
	/**
	 * Flag to indicate do not play shutter sound.
	 */
	public static final int FLAG_NO_SHUTTER_SOUND = 0x2;
	
	
	/**
	 * Read-only property for available camera list.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final PropertyKey<List<Camera>> PROP_AVAILABLE_CAMERAS = new PropertyKey<List<Camera>>("AvailableCameras", (Class)List.class, CameraThread.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property for current primary camera.
	 */
	public static final PropertyKey<Camera> PROP_CAMERA = new PropertyKey<>("Camera", Camera.class, CameraThread.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property for current primary camera preview state.
	 */
	public static final PropertyKey<OperationState> PROP_CAMERA_PREVIEW_STATE = new PropertyKey<>("CameraPreviewState", OperationState.class, CameraThread.class, OperationState.STOPPED);
	/**
	 * Property to get or set current media capture rotation.
	 */
	public static final PropertyKey<Rotation> PROP_CAPTURE_ROTATION = new PropertyKey<>("CaptureRotation", Rotation.class, CameraThread.class, PropertyKey.FLAG_NOT_NULL, Rotation.PORTRAIT);
	/**
	 * Read-only property to check whether first camera preview frame is received or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_CAMERA_PREVIEW_RECEIVED = new PropertyKey<>("IsCameraPreviewReceived", Boolean.class, CameraThread.class, false);
	/**
	 * Read-only property for current captured media type.
	 */
	public static final PropertyKey<MediaType> PROP_MEDIA_TYPE = new PropertyKey<>("MediaType", MediaType.class, CameraThread.class, MediaType.PHOTO);
	/**
	 * Read-only property for photo capture state.
	 */
	public static final PropertyKey<PhotoCaptureState> PROP_PHOTO_CAPTURE_STATE = new PropertyKey<>("PhotoCaptureState", PhotoCaptureState.class, CameraThread.class, PhotoCaptureState.PREPARING);
	/**
	 * Read-only property for screen size.
	 */
	public static final PropertyKey<ScreenSize> PROP_SCREEN_SIZE = new PropertyKey<>("ScreenSize", ScreenSize.class, CameraThread.class, ScreenSize.EMPTY);
	/**
	 * Read-only property for video capture state.
	 */
	public static final PropertyKey<VideoCaptureState> PROP_VIDEO_CAPTURE_STATE = new PropertyKey<>("VideoCaptureState", VideoCaptureState.class, CameraThread.class, VideoCaptureState.PREPARING);
	
	
	/**
	 * Event raised when unexpected camera error occurred.
	 */
	public static final EventKey<CameraEventArgs> EVENT_CAMERA_ERROR = new EventKey<>("CameraError", CameraEventArgs.class, CameraThread.class);
	/**
	 * Event raised when default photo capture process completed.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED = new EventKey<>("DefaultPhotoCaptureCompleted", CaptureEventArgs.class, CameraThread.class);
	/**
	 * Event raised when default video capture process completed.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_DEFAULT_VIDEO_CAPTURE_COMPLETED = new EventKey<>("DefaultVideoCaptureCompleted", CaptureEventArgs.class, CameraThread.class);
	
	
	// Private fields
	private AudioManager m_AudioManager;
	private Handle m_BurstCaptureSoundStreamHandle;
	private final Context m_Context;
	private Handle m_CameraCaptureHandle;
	private CameraDeviceManager m_CameraDeviceManager;
	private volatile ComponentManager m_ComponentManager;
	private final PhotoCaptureHandlerHandle m_DefaultPhotoCaptureHandlerHandle = new PhotoCaptureHandlerHandle(null);
	private Handle m_DefaultShutterSoundHandle;
	private final VideoCaptureHandlerHandle m_DefaultVideoCaptureHandlerHandle = new VideoCaptureHandlerHandle(null);
	private FocusController m_FocusController;
	private boolean m_IsCapturingBurstPhotos;
	private boolean m_IsNormalComponentsCreated;
	private final List<ComponentBuilder> m_InitialComponentBuilders = new ArrayList<>();
	private volatile MediaType m_InitialMediaType;
	private volatile ScreenSize m_InitialScreenSize;
	private MediaRecorder m_MediaRecorder;
	private final List<CameraPreviewStopRequest> m_PendingCameraPreviewStopRequests = new ArrayList<>();
	private PhotoCaptureHandle m_PhotoCaptureHandle;
	private PhotoCaptureHandlerHandle m_PhotoCaptureHandlerHandle;
	private List<PhotoCaptureHandlerHandle> m_PhotoCaptureHandlerHandles;
	private volatile ResourceIdTable m_ResourceIdTable;
	private VideoCaptureHandle m_VideoCaptureHandle;
	private VideoCaptureHandlerHandle m_VideoCaptureHandlerHandle;
	private List<VideoCaptureHandlerHandle> m_VideoCaptureHandlerHandles;
	private String m_VideoFilePath;
	private Handle m_VideoStartSoundHandle;
	private Handle m_VideoStopSoundHandle;
	
	
	// Runnables.
	private final Runnable m_CloseCamerasRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			closeCamerasInternal();
		}
	};
	
	
	// Property call-backs.
	private final PropertyChangedCallback<Boolean> m_CameraPreviewReceivedChangedCallback = new PropertyChangedCallback<Boolean>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
		{
			onCameraPreviewReceivedStateChanged((Camera)source, e.getNewValue());
		}
	};
	private final PropertyChangedCallback<OperationState> m_CameraPreviewStateChangedCallback = new PropertyChangedCallback<OperationState>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
		{
			onCameraPreviewStateChanged((Camera)source, e.getOldValue(), e.getNewValue());
		}
	};
	private final PropertyChangedCallback<OperationState> m_CaptureStateChangedCallback = new PropertyChangedCallback<OperationState>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
		{
			if(e.getNewValue() == OperationState.STOPPED)
				onCaptureCompleted((Camera)source);
		}
	};
	
	
	// Event handlers.
	private final EventHandler<EventArgs> m_CameraErrorHandler = new EventHandler<EventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e)
		{
			onCameraError((Camera)source);
		}
	};
	private final EventHandler<CameraCaptureEventArgs> m_CaptureFailedHandler = new EventHandler<CameraCaptureEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<CameraCaptureEventArgs> key, CameraCaptureEventArgs e)
		{
			onCaptureFailed(e);
		}
	};
	private final EventHandler<CameraCaptureEventArgs> m_PictureReceivedHandler = new EventHandler<CameraCaptureEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<CameraCaptureEventArgs> key, CameraCaptureEventArgs e)
		{
			onPictureReceived(e);
		}
	};
	private final EventHandler<CameraCaptureEventArgs> m_ShutterHandler = new EventHandler<CameraCaptureEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<CameraCaptureEventArgs> key, CameraCaptureEventArgs e)
		{
			onShutter(e);
		}
	};
	
	
	/**
	 * Resource ID table.
	 */
	public static class ResourceIdTable implements Cloneable
	{
		/**
		 * Sound resource ID for photo capture.
		 */
		public int photoShutterSound;
		/**
		 * Sound resource ID for video recording start.
		 */
		public int videoStartSound;
		/**
		 * Sound resource ID for video recording stop.
		 */
		public int videoStopSound;
		
		/**
		 * Create clone.
		 * @return Clone.
		 */
		@Override
		public ResourceIdTable clone()
		{
			try
			{
				return (ResourceIdTable)super.clone();
			} 
			catch(CloneNotSupportedException ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}
	
	
	// Class for capture handler.
	private final class PhotoCaptureHandlerHandle extends Handle
	{
		public final PhotoCaptureHandler captureHandler;
		
		public PhotoCaptureHandlerHandle(PhotoCaptureHandler handler)
		{
			super("PhotoCaptureHandler");
			this.captureHandler = handler;
		}

		@Override
		protected void onClose(int flags)
		{
			//
		}
	}
	private final class VideoCaptureHandlerHandle extends Handle
	{
		public final VideoCaptureHandler captureHandler;
		
		public VideoCaptureHandlerHandle(VideoCaptureHandler handler)
		{
			super("VideoCaptureHandler");
			this.captureHandler = handler;
		}

		@Override
		protected void onClose(int flags)
		{
			//
		}
	}
	
	
	// Class for capture handle.
	private final class PhotoCaptureHandle extends CaptureHandle
	{
		public PhotoCaptureHandler captureHandler;
		public final int frameCount;
		
		public PhotoCaptureHandle(int frameCount)
		{
			super(MediaType.PHOTO);
			this.frameCount = frameCount;
		}
		
		public void complete()
		{
			this.closeDirectly();
		}

		@Override
		protected void onClose(int flags)
		{
			stopPhotoCapture(this);
		}
	}
	private final class VideoCaptureHandle extends CaptureHandle
	{
		public CamcorderProfile camcorderProfile;
		public VideoCaptureHandler captureHandler;
		
		public VideoCaptureHandle()
		{
			super(MediaType.VIDEO);
		}
		
		public void complete()
		{
			this.closeDirectly();
		}

		@Override
		protected void onClose(int flags)
		{
			stopVideoCapture(this, flags);
		}
	}
	
	
	// Class for camera preview stop request.
	private static final class CameraPreviewStopRequest
	{
		public final Camera camera;
		public final int flags;
		public final boolean[] result;
		
		public CameraPreviewStopRequest(Camera camera, boolean[] result, int flags)
		{
			this.camera = camera;
			this.flags = flags;
			this.result = result;
		}
	}
	
	
	/**
	 * Initialize new CameraThread instance.
	 * @param context Related {@link android.content.Context Context}.
	 * @param callback Call-back when camera thread starts.
	 * @param callbackHandler Handler for call-back.
	 */
	public CameraThread(Context context, ThreadStartCallback callback, Handler callbackHandler)
	{
		super("Camera Thread", callback, callbackHandler);
		if(context == null)
			throw new IllegalArgumentException("No context.");
		m_Context = context;
	}
	
	
	/**
	 * Add component builders to camera thread.
	 * @param builders Component builders to add.
	 */
	public final void addComponentBuilders(final ComponentBuilder[] builders)
	{
		if(this.isDependencyThread())
			m_ComponentManager.addComponentBuilders(builders, this);
		else
		{
			synchronized(this)
			{
				if(m_ComponentManager != null)
				{
					HandlerUtils.post(this, new Runnable()
					{
						@Override
						public void run()
						{
							m_ComponentManager.addComponentBuilders(builders, CameraThread.this);
						}
					});
				}
				else
					m_InitialComponentBuilders.addAll(Arrays.asList(builders));
			}
		}
	}
	
	
	// Bind to initial components.
	@SuppressWarnings("unchecked")
	private boolean bindToInitialComponents()
	{
		// bind to AudioManager
		m_AudioManager = m_ComponentManager.findComponent(AudioManager.class, this);
		if(m_AudioManager != null)
		{
			if(m_ResourceIdTable != null)
			{
				if(m_ResourceIdTable.photoShutterSound != 0)
					m_DefaultShutterSoundHandle = m_AudioManager.loadSound(m_ResourceIdTable.photoShutterSound, AudioManager.STREAM_RING, 0);
				if(m_ResourceIdTable.videoStartSound != 0)
					m_VideoStartSoundHandle = m_AudioManager.loadSound(m_ResourceIdTable.videoStartSound, AudioManager.STREAM_RING, 0);
				if(m_ResourceIdTable.videoStopSound != 0)
					m_VideoStopSoundHandle = m_AudioManager.loadSound(m_ResourceIdTable.videoStopSound, AudioManager.STREAM_RING, 0);
			}
		}
		else
			Log.w(TAG, "bindToInitialComponents() - No AudioManager");
		
		// bind to CameraDeviceManager
		m_CameraDeviceManager = m_ComponentManager.findComponent(CameraDeviceManager.class);
		if(m_CameraDeviceManager != null)
		{
			m_CameraDeviceManager.addCallback(CameraDeviceManager.PROP_AVAILABLE_CAMERAS, new PropertyChangedCallback<List<Camera>>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<List<Camera>> key, PropertyChangeEventArgs<List<Camera>> e)
				{
					onAvailableCamerasChanged(e.getOldValue(), e.getNewValue());
				}
			});
			this.onAvailableCamerasChanged(Collections.EMPTY_LIST, m_CameraDeviceManager.get(CameraDeviceManager.PROP_AVAILABLE_CAMERAS));
		}
		else
		{
			Log.e(TAG, "bindToInitialComponents() - No CameraDeviceManager");
			return false;
		}
		
		// bind to FileManager
		m_ComponentManager.findComponent(FileManager.class , this);
		
		// complete
		return true;
	}
	
	
	// Bind to normal components.
	private void bindToNormalComponents()
	{
		// bind to FocusController
		m_FocusController = m_ComponentManager.findComponent(FocusController.class, this);
		if(m_FocusController != null)
		{
			m_FocusController.addCallback(FocusController.PROP_FOCUS_STATE, new PropertyChangedCallback<FocusState>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<FocusState> key, PropertyChangeEventArgs<FocusState> e)
				{
					onFocusStateChanged(e.getNewValue());
				}
			});
		}
		else
			Log.w(TAG, "bindToNormalComponents() - No FocusController");
	}
	
	
	/**
	 * Get internal component manager.
	 * @return Component manager.
	 */
	protected final ComponentManager getComponentManager()
	{
		return m_ComponentManager;
	}
	
	
	/**
	 * Get related context.
	 * @return {@link android.content.Context Context}.
	 */
	public final Context getContext()
	{
		return m_Context;
	}
	
	
	/**
	 * Start photo capture.
	 * @return Capture handle.
	 */
	public final CaptureHandle capturePhoto()
	{
		return this.capturePhoto(1, 0);
	}
	
	
	/**
	 * Start photo capture.
	 * @param frameCount Target frame count, 1 for single shot; positive integer for limited burst; negative for unlimited burst.
	 * @return Capture handle.
	 */
	public final CaptureHandle capturePhoto(int frameCount)
	{
		return this.capturePhoto(frameCount, 0);
	}
	
	
	/**
	 * Start photo capture.
	 * @param frameCount Target frame count, 1 for single shot; positive integer for limited burst; negative for unlimited burst.
	 * @param flags Flags, reserved.
	 * @return Capture handle.
	 */
	public final CaptureHandle capturePhoto(final int frameCount, final int flags)
	{
		// check parameter
		if(frameCount == 0)
		{
			Log.e(TAG, "capturePhoto() - Invalid frame count");
			return null;
		}
		
		// create handle
		final PhotoCaptureHandle handle = new PhotoCaptureHandle(frameCount);
		
		// capture
		if(this.isDependencyThread())
		{
			if(this.capturePhotoInternal(handle, false))
				return handle;
			return null;
		}
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				capturePhotoInternal(handle, false);
			}
		}))
		{
			Log.v(TAG, "capturePhoto() - Create handle ", handle);
			return handle;
		}
		Log.e(TAG, "capturePhoto() - Fail to perform cross-thread operation");
		return null;
	}
	
	
	// Capture photo.
	private boolean capturePhotoInternal(PhotoCaptureHandle handle, boolean isFocusLocked)
	{
		// clear state
		m_PhotoCaptureHandle = null;
		
		// check state
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case READY:
				break;
			case STARTING:
				if(isFocusLocked)
					break;
			default:
				Log.e(TAG, "capturePhotoInternal() - Capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return false;
		}
		
		Log.w(TAG, "capturePhotoInternal() - Handle : " + handle + ", focus locked : " + isFocusLocked);
		
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "capturePhotoInternal() - No primary camera");
			return false;
		}
		
		// change state
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.STARTING);
		
		// lock focus
		if(!isFocusLocked)
		{
			if(m_FocusController != null)
			{
				// check state
				boolean lockFocus = false;
				FocusState focusState = m_FocusController.get(FocusController.PROP_FOCUS_STATE);
				if(focusState == FocusState.FOCUSED)
				{
					if(m_FocusController.get(FocusController.PROP_FOCUS_MODE) == FocusMode.CONTINUOUS_AF)
						Log.v(TAG, "capturePhotoInternal() - No need to lock focus, because current focus mode is continuous AF");
					else
						lockFocus = true;
				}
				else if(focusState == FocusState.SCANNING)
				{
					Log.w(TAG, "capturePhotoInternal() - Waiting for focus complete");
					m_PhotoCaptureHandle = handle;
					return true;
				}
				else
					lockFocus = true;
				
				// start locking focus
				if(lockFocus)
				{
					Log.w(TAG, "capturePhotoInternal() - Start locking focus");
					if(Handle.isValid(m_FocusController.startAutoFocus(m_FocusController.get(FocusController.PROP_AF_REGIONS), FocusController.FLAG_SINGLE_AF)))
					{
						m_PhotoCaptureHandle = handle;
						return true;
					}
					Log.e(TAG, "capturePhotoInternal() - Fail to start locking focus");
				}
			}
			else
				Log.w(TAG, "capturePhotoInternal() - No FocusController to lock focus");
		}
		
		// capture
		PhotoCaptureHandlerHandle handlerHandle = null;
		try
		{
			// prepare parameters
			camera.set(Camera.PROP_PICTURE_ROTATION, this.get(PROP_CAPTURE_ROTATION));
			
			// capture
			for(int i = m_PhotoCaptureHandlerHandles.size() - 1 ; i >= 0 ; --i)
			{
				handlerHandle = m_PhotoCaptureHandlerHandles.get(i);
				if(handlerHandle.captureHandler.capture(camera, handle))
				{
					Log.w(TAG, "capturePhotoInternal() - Capture process is handled by " + handlerHandle.captureHandler);
					break;
				}
				handlerHandle = null;
			}
			if(handlerHandle == null)
			{
				Log.v(TAG, "capturePhotoInternal() - Use default capture process");
				if(!this.capturePhotoInternal(handle.frameCount))
					throw new RuntimeException("Fail to use default photo capture process.");
				handlerHandle = m_DefaultPhotoCaptureHandlerHandle;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "capturePhotoInternal() - Fail to capture", ex);
			if(this.get(PROP_CAMERA_PREVIEW_STATE) == OperationState.STARTED)
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
			else
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
			return false;
		}
		
		// complete
		m_PhotoCaptureHandlerHandle = handlerHandle;
		m_PhotoCaptureHandle = handle;
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.CAPTURING);
		return true;
	}
	
	
	// Default photo capture process.
	private boolean capturePhotoInternal(int frameCount)
	{
		// prepare event handlers
		Camera camera = this.get(PROP_CAMERA);
		camera.addHandler(Camera.EVENT_CAPTURE_FAILED, m_CaptureFailedHandler);
		camera.addHandler(Camera.EVENT_PICTURE_RECEIVED, m_PictureReceivedHandler);
		camera.addHandler(Camera.EVENT_SHUTTER, m_ShutterHandler);
		
		// prepare property changed call-backs.
		camera.addCallback(Camera.PROP_CAPTURE_STATE, m_CaptureStateChangedCallback);
		
		// capture
		m_CameraCaptureHandle = camera.capture(frameCount, 0);
		if(!Handle.isValid(m_CameraCaptureHandle))
		{
			Log.e(TAG, "capturePhotoInternal() - Fail to capture");
			camera.removeHandler(Camera.EVENT_CAPTURE_FAILED, m_CaptureFailedHandler);
			camera.removeHandler(Camera.EVENT_PICTURE_RECEIVED, m_PictureReceivedHandler);
			camera.removeHandler(Camera.EVENT_SHUTTER, m_ShutterHandler);
			camera.removeCallback(Camera.PROP_CAPTURE_STATE, m_CaptureStateChangedCallback);
			return false;
		}
		
		// complete
		m_IsCapturingBurstPhotos = (frameCount != 1);
		return true;
	}
	
	
	/**
	 * Start video capture.
	 * @param resolution Video resolution.
	 * @return Capture handle.
	 */
	public final CaptureHandle captureVideo(final Resolution resolution)
	{
		if(resolution == null)
		{
			Log.e(TAG, "captureVideo() - No video resolution");
			return null;
		}
		if(resolution.getTargetType() != MediaType.VIDEO)
		{
			Log.e(TAG, "captureVideo() - Invalid resolution : " + resolution);
			return null;
		}
		final VideoCaptureHandle handle = new VideoCaptureHandle();
		if(this.isDependencyThread())
		{
			if(this.captureVideoInternal(handle, resolution, false))
				return handle;
			return null;
		}
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				captureVideoInternal(handle, resolution, false);
			}
		}))
		{
			return handle;
		}
		Log.e(TAG, "captureVideo() - Fail to perform cross-thread operation");
		return null;
	}
	
	
	// Start video capture.
	private boolean captureVideoInternal(VideoCaptureHandle handle, Resolution resolution, boolean isShutterSoundPlayed)
	{
		// check state
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case READY:
				break;
			case STARTING:
				if(isShutterSoundPlayed)
					break;
			default:
				Log.e(TAG, "captureVideoInternal() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
				return false;
		}
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "captureVideoInternal() - No primary camera");
			return false;
		}
		
		Log.v(TAG, "captureVideoInternal() - Handle : ", handle, ", resolution : ", resolution, ", shutter sound played : ", isShutterSoundPlayed);
		
		// check storage
		if(!isShutterSoundPlayed)
		{
			// check directory
			File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA");
			if(!directory.exists() && !directory.mkdir())
			{
				Log.e(TAG, "captureVideoInternal() - Fail to create " + directory.getAbsolutePath());
				return false;
			}
			
			// check file path
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(directory, "VID_" + dateFormat.format(new Date()) + ".mp4");
			m_VideoFilePath = file.getAbsolutePath();
			Log.w(TAG, "captureVideoInternal() - Video file path : " + m_VideoFilePath);
		}
		
		// change state
		this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.STARTING);
		
		// play shutter sound
		long shutterSoundTime = 0;
		if(!isShutterSoundPlayed && Handle.isValid(m_VideoStartSoundHandle))
		{
			m_AudioManager.playSound(m_VideoStartSoundHandle, 0);
			shutterSoundTime = SystemClock.elapsedRealtime();
		}
		
		// prepare media recorder and start later
		if(!isShutterSoundPlayed)
		{
			// prepare media recorder
			MediaRecorder mediaRecorder = new MediaRecorder();
			if(!this.prepareMediaRecorder(camera, mediaRecorder, resolution))
			{
				Log.e(TAG, "captureVideoInternal() - Fail to prepare media recorder");
				mediaRecorder.release();
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
				return false;
			}
			m_MediaRecorder = mediaRecorder;
			
			// start recording later
			long delay = (DURATION_VIDEO_CAPTURE_DELAY - (SystemClock.elapsedRealtime() - shutterSoundTime));
			if(delay > 0)
			{
				Log.w(TAG, "captureVideoInternal() - Start video recording " + delay + " ms later");
				HandlerUtils.sendMessage(this, MSG_CAPTURE_VIDEO, 0, 0, resolution, delay);
				m_VideoCaptureHandle = handle;
				return true;
			}
		}
		
		// start
		try
		{
			m_MediaRecorder.start();
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "captureVideoInternal() - Fail to start", ex);
			this.get(PROP_CAMERA).set(Camera.PROP_VIDEO_SURFACE, null);
			m_MediaRecorder.release();
			m_VideoCaptureHandle = null;
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
			return false;
		}
		
		// update state
		m_VideoCaptureHandle = handle;
		this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.CAPTURING);
		
		// complete
		return true;
	}
	
	
	/**
	 * Close given camera.
	 * @param camera Camera to close.
	 */
	public final void closeCamera(final Camera camera)
	{
		// check parameter
		if(camera == null)
		{
			Log.e(TAG, "closeCamera() - No camera to close");
			return;
		}
		
		// close camera
		if(this.isDependencyThread())
			this.closeCameraInternal(camera);
		else if(!HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				closeCameraInternal(camera);
			}
		}))
		{
			Log.e(TAG, "closeCamera() - Fail to perform cross-thread operation");
		}
	}
	
	
	// Close camera.
	private void closeCameraInternal(Camera camera)
	{
		Log.w(TAG, "closeCameraInternal() - Start");
		Log.v(TAG, "closeCameraInternal() - Camera : ", camera);
		camera.close(0);
		Log.w(TAG, "closeCameraInternal() - End");
	}
	
	
	/**
	 * Close all cameras.
	 */
	public final void closeCameras()
	{
		if(this.isDependencyThread())
			this.closeCamerasInternal();
		else if(!HandlerUtils.post(this, m_CloseCamerasRunnable))
			Log.e(TAG, "closeCameras() - Fail to perform cross-thread operation");
	}
	
	
	// Close all cameras
	private void closeCamerasInternal()
	{
		Log.w(TAG, "closeCamerasInternal() - Start");
		List<Camera> cameras = this.get(PROP_AVAILABLE_CAMERAS);
		for(int i = cameras.size() - 1 ; i >= 0 ; --i)
			cameras.get(i).close(0);
		Log.w(TAG, "closeCamerasInternal() - End");
	}
	
	
	/**
	 * Complete media capture process.
	 * @param captureHandler Handle returned from {@link #setPhotoCaptureHandler(PhotoCaptureHandler, int)} or {@link #setVideoCaptureHandler(VideoCaptureHandler, int)}.
	 * @param handle Capture handle.
	 * @return Whether capture completes successfully or not.
	 */
	public final boolean completeCapture(Handle captureHandler, CaptureHandle handle)
	{
		// check state
		this.verifyAccess();
		if(captureHandler == null)
		{
			Log.e(TAG, "completeCapture() - No capture handler");
			return false;
		}
		if(handle == null)
		{
			Log.e(TAG, "completeCapture() - No capture handle");
			return false;
		}
		
		// complete capture
		return this.completeCaptureInternal(captureHandler, handle, true);
	}
	
	
	// Complete media capture process.
	private boolean completeCaptureInternal(Handle captureHandler, CaptureHandle handle, boolean checkHandles)
	{
		Log.w(TAG, "completeCaptureInternal() - Handle : " + handle);
		
		// complete capture
		switch(handle.getMediaType())
		{
			case PHOTO:
			{
				// check handles
				if(checkHandles)
				{
					if(m_PhotoCaptureHandlerHandle != captureHandler)
					{
						Log.e(TAG, "completeCaptureInternal() - Invalid capture handler : " + captureHandler);
						return false;
					}
					if(handle != m_PhotoCaptureHandle)
					{
						Log.e(TAG, "completeCaptureInternal() - Invalid capture handle : " + handle);
						return false;
					}
				}
				
				// clear states
				m_PhotoCaptureHandle = null;
				m_PhotoCaptureHandlerHandle = null;
				m_IsCapturingBurstPhotos = false;
				
				// update property
				if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO)
					Log.w(TAG, "completeCaptureInternal() - Complete video snapshot");
				if(this.get(PROP_CAMERA_PREVIEW_STATE) == OperationState.STARTED)
					this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
				else
					this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
				break;
			}
			case VIDEO:
			{
				// check handles
				//
				break;
			}
		}
		
		// complete
		return true;
	}
	
	
	// Find component extends or implements given type.
	@Override
	public <TComponent extends Component> TComponent findComponent(Class<TComponent> componentType)
	{
		if(m_ComponentManager != null)
			return m_ComponentManager.findComponent(componentType, this);
		return null;
	}
	
	
	// Find all components extend or implement given type.
	@SuppressWarnings("unchecked")
	@Override
	public <TComponent extends Component> TComponent[] findComponents(Class<TComponent> componentType)
	{
		if(m_ComponentManager != null)
			return m_ComponentManager.findComponents(componentType, this);
		return (TComponent[])new Component[0];
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_CAPTURE_VIDEO:
				this.captureVideoInternal(m_VideoCaptureHandle, (Resolution)msg.obj, true);
				break;
				
			case MSG_SCREEN_SIZE_CHANGED:
				this.setReadOnly(PROP_SCREEN_SIZE, (ScreenSize)msg.obj);
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when available camera list changes.
	private void onAvailableCamerasChanged(List<Camera> oldCameras, List<Camera> cameras)
	{
		// attach/detach call-backs
		for(int i = cameras.size() - 1 ; i >= 0 ; --i)
		{
			Camera camera = cameras.get(i);
			if(!oldCameras.contains(camera))
			{
				camera.addCallback(Camera.PROP_IS_PREVIEW_RECEIVED, m_CameraPreviewReceivedChangedCallback);
				camera.addCallback(Camera.PROP_PREVIEW_STATE, m_CameraPreviewStateChangedCallback);
				camera.addHandler(Camera.EVENT_ERROR, m_CameraErrorHandler);
			}
		}
		for(int i = oldCameras.size() - 1 ; i >= 0 ; --i)
		{
			Camera camera = oldCameras.get(i);
			if(!cameras.contains(camera))
			{
				camera.removeCallback(Camera.PROP_IS_PREVIEW_RECEIVED, m_CameraPreviewReceivedChangedCallback);
				camera.removeCallback(Camera.PROP_PREVIEW_STATE, m_CameraPreviewStateChangedCallback);
				camera.removeHandler(Camera.EVENT_ERROR, m_CameraErrorHandler);
			}
		}
		
		// update property
		this.setReadOnly(PROP_AVAILABLE_CAMERAS, cameras);
	}
	
	
	// Called when unexpected camera error occurred.
	private void onCameraError(Camera camera)
	{
		if(this.get(PROP_CAMERA) == camera)
		{
			Log.e(TAG, "onCameraError() - Camera : " + camera);
			this.raise(EVENT_CAMERA_ERROR, new CameraEventArgs(camera));
		}
	}
	
	
	// Called when primary camera preview receiving state changes.
	private void onCameraPreviewReceivedStateChanged(Camera camera, boolean isReceived)
	{
		// check camera
		if(this.get(PROP_CAMERA) != camera)
			return;
		
		// update property
		this.setReadOnly(PROP_IS_CAMERA_PREVIEW_RECEIVED, isReceived);
	}
	
	
	// Called when primary camera preview state changes.
	private void onCameraPreviewStateChanged(Camera camera, OperationState prevState, OperationState state)
	{
		// continue stopping preview
		if(state == OperationState.STARTED)
		{
			for(int i = m_PendingCameraPreviewStopRequests.size() - 1 ; i >= 0 ; --i)
			{
				CameraPreviewStopRequest request = m_PendingCameraPreviewStopRequests.get(i);
				if(request.camera == camera)
				{
					Log.w(TAG, "onCameraPreviewStateChanged() - Continue stopping preview for " + camera);
					m_PendingCameraPreviewStopRequests.remove(i);
					this.stopCameraPreviewInternal(camera, request.result, request.flags);
				}
			}
			if(camera.get(Camera.PROP_PREVIEW_STATE) != state)
				return;
		}
		
		// check camera
		if(this.get(PROP_CAMERA) != camera)
			return;
		
		// update preview state property
		this.setReadOnly(PROP_CAMERA_PREVIEW_STATE, state);
		
		// release media recorder
		if(m_VideoCaptureHandle == null && m_MediaRecorder != null)
		{
			if(state == OperationState.STARTED || state == OperationState.STOPPED)
			{
				Log.v(TAG, "onCameraPreviewStateChanged() - Release media recorder");
				m_MediaRecorder.release();
				m_MediaRecorder = null;
			}
		}
		
		// update capture state properties
		if(state == OperationState.STARTED)
		{
			// change capture state
			if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.PREPARING)
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
			if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO && this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.PREPARING)
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
		}
		else
		{
			// change capture state
			if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.READY)
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
			if(this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.READY)
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
		}
	}
	
	
	// Called when capture completed.
	private void onCaptureCompleted(Camera camera)
	{
		// remove handlers and call-backs
		camera.removeHandler(Camera.EVENT_CAPTURE_FAILED, m_CaptureFailedHandler);
		camera.removeHandler(Camera.EVENT_PICTURE_RECEIVED, m_PictureReceivedHandler);
		camera.removeHandler(Camera.EVENT_SHUTTER, m_ShutterHandler);
		camera.removeCallback(Camera.PROP_CAPTURE_STATE, m_CaptureStateChangedCallback);
		
		// reset state
		m_CameraCaptureHandle = null;
		
		// raise event
		this.raise(EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED, new CaptureEventArgs(m_PhotoCaptureHandle));
		
		// complete capture
		this.completeCapture(m_DefaultPhotoCaptureHandlerHandle, m_PhotoCaptureHandle);
	}
	
	
	// Called when capture failed.
	private void onCaptureFailed(CameraCaptureEventArgs e)
	{
		//
	}
	
	
	// Called when focus state changes.
	private void onFocusStateChanged(FocusState focusState)
	{
		// continue capture photo
		if(focusState != FocusState.SCANNING 
				&& this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.STARTING
				&& Handle.isValid(m_PhotoCaptureHandle))
		{
			Log.w(TAG, "onFocusStateChanged() - Continue capturing photo");
			this.capturePhotoInternal(m_PhotoCaptureHandle, true);
		}
	}
	
	
	// Called when receiving captured picture.
	private void onPictureReceived(CameraCaptureEventArgs e)
	{
		Log.v(TAG, "onPictureReceived() - Index : ", e.getFrameIndex());
		
		m_ComponentManager.findComponent(FileManager.class, this).saveMedia(new PhotoSaveTask(this.getContext(), e), 0);
	}
	
	
	// Called when starting capturing picture.
	private void onShutter(CameraCaptureEventArgs e)
	{
		Log.v(TAG, "onShutter() - Index : ", e.getFrameIndex());
		
		// play shutter sound
		if(e.getFrameIndex() == 0)
		{
			if(m_IsCapturingBurstPhotos)
			{
				//if(Handle.isValid(m_DefaultShutterSoundHandle))
					//m_BurstCaptureSoundStreamHandle = m_AudioManager.playSound(m_DefaultShutterSoundHandle, AudioManager.FLAG_LOOP);
				//else
					//Log.w(TAG, "onShutter() - No sound for burst capture");
			}
			else
				this.playDefaultShutterSound();
		}
	}
	
	
	// Called when thread starts.
	@Override
	protected void onStarted()
	{
		// call super
		super.onStarted();
		
		// create component with HIGH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.HIGH, this);
		
		// bind to components
		if(!this.bindToInitialComponents())
			throw new RuntimeException("Fail to bind components.");
		
	}
	
	
	// Called when starting thread.
	@Override
	protected void onStarting()
	{
		// call super
		super.onStarting();
		
		// enable logs
		this.enablePropertyLogs(PROP_CAMERA_PREVIEW_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_CAPTURE_ROTATION, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PHOTO_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_VIDEO_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		
		// create handle lists
		m_PhotoCaptureHandlerHandles = new ArrayList<>();
		m_VideoCaptureHandlerHandles = new ArrayList<>();
		
		// setup initial states
		synchronized(this)
		{
			// setup screen size
			if(m_InitialScreenSize != null)
			{
				Log.v(TAG, "onStarting() - Initial screen size : ", m_InitialScreenSize);
				this.setReadOnly(PROP_SCREEN_SIZE, m_InitialScreenSize);
				m_InitialScreenSize = null;
			}
			
			// setup media type
			if(m_InitialMediaType != null)
			{
				Log.v(TAG, "onStarting() - Initial media type : ", m_InitialMediaType);
				this.setReadOnly(PROP_MEDIA_TYPE, m_InitialMediaType);
			}
			
			// create component manager
			m_ComponentManager = new ComponentManager();
			m_ComponentManager.addComponentBuilders(DEFAULT_COMPONENT_BUILDERS, this);
			m_ComponentManager.addHandler(ComponentManager.EVENT_COMPONENT_ADDED, new EventHandler<ComponentEventArgs<Component>>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
				{
					CameraThread.this.raise(EVENT_COMPONENT_ADDED, e);
				}
			});
			m_ComponentManager.addHandler(ComponentManager.EVENT_COMPONENT_REMOVED, new EventHandler<ComponentEventArgs<Component>>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
				{
					CameraThread.this.raise(EVENT_COMPONENT_REMOVED, e);
				}
			});
			if(!m_InitialComponentBuilders.isEmpty())
			{
				ComponentBuilder[] builders = new ComponentBuilder[m_InitialComponentBuilders.size()];
				m_InitialComponentBuilders.toArray(builders);
				m_InitialComponentBuilders.clear();
				m_ComponentManager.addComponentBuilders(builders, this);
			}
		}
		
		// create component with LAUNCH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.LAUNCH, this);
	}
	
	
	// Called before stopping thread.
	@Override
	protected void onStopping()
	{
		// close all cameras
		this.closeCamerasInternal();
		
		// call super
		super.onStopping();
	}
	
	
	/**
	 * Open given camera.
	 * @param camera Camera to open.
	 * @return Whether camera opening starts successfully or not.
	 */
	public final boolean openCamera(Camera camera)
	{
		return this.openCamera(camera, 0);
	}
	
	
	/**
	 * Open given camera.
	 * @param camera Camera to open.
	 * @param flags Flags, reserved.
	 * @return Whether camera opening starts successfully or not.
	 */
	public final boolean openCamera(final Camera camera, final int flags)
	{
		if(camera == null)
		{
			Log.e(TAG, "openCamera() - No camera");
			return false;
		}
		if(this.isDependencyThread())
			return this.openCameraInternal(camera, flags);
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				openCameraInternal(camera, flags);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "openCamera() - Fail to perform cross-thread operation");
		return false;
	}
	
	
	// Open camera.
	private boolean openCameraInternal(Camera camera, int flags)
	{
		// check camera list
		if(!this.get(PROP_AVAILABLE_CAMERAS).contains(camera))
		{
			Log.e(TAG, "openCameraInternal() - Camera " + camera + " is not contained in available camera list");
			return false;
		}
		
		// check state
		switch(camera.get(Camera.PROP_STATE))
		{
			case OPENING:
			case OPENED:
				return true;
			default:
				break;
		}
		
		// open camera
		Log.v(TAG, "openCameraInternal() - Open ", camera);
		try
		{
			if(!camera.open(0))
			{
				Log.e(TAG, "openCameraInternal() - Fail to open " + camera);
				return false;
			}
		}
		catch(Throwable ex)
		{
			return false;
		}
		
		// set recording mode
		camera.set(Camera.PROP_IS_RECORDING_MODE, this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO);
		
		// update property
		this.setReadOnly(PROP_IS_CAMERA_PREVIEW_RECEIVED, camera.get(Camera.PROP_IS_PREVIEW_RECEIVED));
		this.setReadOnly(PROP_CAMERA, camera);
		
		// complete
		return true;
	}
	
	
	/**
	 * Play default shutter sound.
	 */
	public void playDefaultShutterSound()
	{
		// check state
		this.verifyAccess();
		if(!Handle.isValid(m_DefaultShutterSoundHandle))
		{
			Log.w(TAG, "playDefaultShutterSound() - No shutter sound to play");
			return;
		}
		
		// play sound
		m_AudioManager.playSound(m_DefaultShutterSoundHandle, 0);
	}
	
	
	// Prepare media recorder.
	private boolean prepareMediaRecorder(Camera camera, MediaRecorder mediaRecorder, Resolution resolution)
	{
		// use capture handler to prepare
		boolean isProfilePrepared = false;
		if(!m_VideoCaptureHandlerHandles.isEmpty())
		{
			for(int i = m_VideoCaptureHandlerHandles.size() - 1 ; i >= 0 ; --i)
			{
				VideoCaptureHandler captureHandler = m_VideoCaptureHandlerHandles.get(i).captureHandler;
				try
				{
					if(captureHandler.prepareCamcorderProfile(camera, mediaRecorder, resolution))
					{
						Log.w(TAG, "prepareMediaRecorder() - Profile is prepared by " + captureHandler);
						isProfilePrepared = true;
						break;
					}
				}
				catch(Throwable ex)
				{
					Log.e(TAG, "prepareMediaRecorder() - Fail to prepare media recorder by " + captureHandler, ex);
					return false;
				}
			}
		}
		
		// prepare media recorder
		try
		{
			// setup parameters
			if(!isProfilePrepared)
			{
				// select profile
				CamcorderProfile profile;
				if(resolution.is4kVideo())
					profile = CamcorderProfile.get(CamcorderProfile.QUALITY_2160P);
				else if(resolution.is1080pVideo())
					profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
				else if(resolution.is720pVideo())
					profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
				else if(resolution.isMmsVideo())
					profile = CamcorderProfile.get(CamcorderProfile.QUALITY_QCIF);
				else
				{
					Log.e(TAG, "prepareMediaRecorder() - Unknown resolution : " + resolution);
					return false;
				}
				
				// set AV sources
				mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
				mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
				
				// set profile
				mediaRecorder.setProfile(profile);
				//mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);    
				//mediaRecorder.setVideoFrameRate(profile.videoFrameRate);                
				//mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);              
			    //mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);                
			    //mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);                
			    //mediaRecorder.setAudioChannels(profile.audioChannels);              
			    //mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);                
			    //mediaRecorder.setVideoEncoder(profile.videoCodec);              
			   	//mediaRecorder.setAudioEncoder(profile.audioCodec);
				
				// set orientation
				int orientation = (this.get(PROP_CAPTURE_ROTATION).getDeviceOrientation() - Rotation.LANDSCAPE.getDeviceOrientation());
				if(orientation < 0)
					orientation += 360;
				Log.v(TAG, "prepareMediaRecorder() - Orientation : ", orientation);
				mediaRecorder.setOrientationHint(orientation);
			}
			
			// set output file
			mediaRecorder.setOutputFile(m_VideoFilePath);
			
			// prepare
			Log.w(TAG, "prepareMediaRecorder() - MediaRecorder.prepare [start]");
			mediaRecorder.prepare();
			Log.w(TAG, "prepareMediaRecorder() - MediaRecorder.prepare [end]");
			
			// prepare video surface
			camera.set(Camera.PROP_VIDEO_SURFACE, mediaRecorder.getSurface());
			
			// complete
			return true;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "prepareMediaRecorder() - Fail to prepare media recorder ", ex);
			return false;
		}
	}
	
	
	// Release and remove given component.
	@Override
	public void removeComponent(Component component)
	{
		this.verifyAccess();
		m_ComponentManager.removeComponent(component);
	}
	
	
	/**
	 * Change current media type.
	 * @param mediaType New media type.
	 * @return Whether media type changes successfully or not.
	 */
	public boolean setMediaType(final MediaType mediaType)
	{
		if(this.isDependencyThread())
			return this.setMediaTypeInternal(mediaType);
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				setMediaTypeInternal(mediaType);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "setMediaType() - Fail to perform cross-thread operation");
		return false;
	}
	
	
	// Change current media type.
	private boolean setMediaTypeInternal(MediaType mediaType)
	{
		// check state
		if(this.get(PROP_MEDIA_TYPE) == mediaType)
			return true;
		Log.v(TAG, "setMediaTypeInternal() - Media type : ", mediaType);
		switch(mediaType)
		{
			case PHOTO:
			{
				switch(this.get(PROP_VIDEO_CAPTURE_STATE))
				{
					case PREPARING:
					case READY:
						break;
					default:
						Log.e(TAG, "setMediaTypeInternal() - Current video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
						return false;
				}
				break;
			}
			
			case VIDEO:
			{
				switch(this.get(PROP_PHOTO_CAPTURE_STATE))
				{
					case PREPARING:
					case READY:
						break;
					default:
						Log.e(TAG, "setMediaTypeInternal() - Current photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
						return false;
				}
				break;
			}
			
			default:
				Log.e(TAG, "setMediaTypeInternal() - Unknown media type : " + mediaType);
				return false;
		}
		
		// stop preview first
		Camera camera = this.get(PROP_CAMERA);
		boolean needRestartPreview;
		switch(this.get(PROP_CAMERA_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				Log.w(TAG, "setMediaTypeInternal() - Stop preview to change media type");
				needRestartPreview = true;
				if(!this.stopCameraPreview(camera))
				{
					Log.e(TAG, "setMediaTypeInternal() - Fail to stop preview");
					return false;
				}
				break;
			default:
				needRestartPreview = false;
				break;
		}
		
		// change media type
		this.setReadOnly(PROP_MEDIA_TYPE, mediaType);
		
		// set recording mode
		if(camera != null)
			camera.set(Camera.PROP_IS_RECORDING_MODE, mediaType == MediaType.VIDEO);
		
		// start preview
		if(needRestartPreview)
		{
			Log.w(TAG, "setMediaTypeInternal() - Restart preview");
			if(!this.startCameraPreview(camera, null, null))
				Log.e(TAG, "setMediaTypeInternal() - Fail to restart preview");
		}
		
		// complete
		return true;
	}
	
	
	//
	public final Handle setPhotoCaptureHandler(PhotoCaptureHandler handler, int flags)
	{
		return null;
	}
	
	
	/**
	 * Set resource ID table before starting thread.
	 * @param table Resource ID table.
	 */
	public final synchronized void setResourceIdTable(ResourceIdTable table)
	{
		// check state
		if(this.get(PROP_IS_STARTED))
			throw new RuntimeException("Cannot change resource ID table after starting");
		
		// save state
		m_ResourceIdTable = (table != null ? table.clone() : null);
	}
	
	
	/**
	 * Set screen size.
	 * @param size Screen size.
	 */
	final void setScreenSize(ScreenSize size)
	{
		if(size == null)
			throw new IllegalArgumentException("No screen size.");
		if(this.isDependencyThread())
			this.setReadOnly(PROP_SCREEN_SIZE, size);
		else
		{
			synchronized(this)
			{
				if(!HandlerUtils.sendMessage(this, MSG_SCREEN_SIZE_CHANGED, 0, 0, size))
					m_InitialScreenSize = size;
			}
		}
	}
	
	
	//
	public final Handle setVideoCaptureHandler(VideoCaptureHandler handler, int flags)
	{
		return null;
	}
	
	
	// Start camera thread with given media type.
	public synchronized void start(MediaType mediaType)
	{
		this.start();
		m_InitialMediaType = mediaType;
	}
	
	
	/**
	 * Start camera preview.
	 * @param camera Camera to start preview.
	 * @param previewSize Preview size.
	 * @param receiver Camera preview receiver, Null to use current receiver.
	 * @return Whether camera preview starts successfully or not.
	 */
	public final boolean startCameraPreview(Camera camera, Size previewSize, Object receiver)
	{
		return this.startCameraPreview(camera, previewSize, receiver, 0);
	}
	
	
	/**
	 * Start camera preview.
	 * @param camera Camera to start preview.
	 * @param previewSize Preview size.
	 * @param receiver Camera preview receiver, Null to use current receiver.
	 * @param flags Flags, reserved.
	 * @return Whether camera preview starts successfully or not.
	 */
	public final boolean startCameraPreview(final Camera camera, final Size previewSize, final Object receiver, final int flags)
	{
		// check parameter
		if(camera == null)
		{
			Log.e(TAG, "startCameraPreview() - No camera");
			return false;
		}
		if(this.isDependencyThread())
			return this.startCameraPreviewInternal(camera, previewSize, receiver, flags);
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				startCameraPreviewInternal(camera, previewSize, receiver, flags);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "startCameraPreview() - Fail to perform cross-thread operation");
		return false;
	}
	
	
	// Start camera preview.
	@SuppressWarnings("incomplete-switch")
	private boolean startCameraPreviewInternal(Camera camera, Size previewSize, Object receiver, int flags)
	{
		// open camera first
		if(!this.openCameraInternal(camera, 0))
		{
			Log.e(TAG, "startCameraPreviewInternal() - Fail to open camera");
			return false;
		}
		
		// check state
		switch(camera.get(Camera.PROP_PREVIEW_STATE))
		{
			case STOPPED:
				break;
			case STARTING:
			case STARTED:
				if(receiver != null && camera.get(Camera.PROP_PREVIEW_RECEIVER) != receiver)
				{
					Log.w(TAG, "startCameraPreviewInternal() - Preview receiver changed, stop preview first");
					camera.stopPreview(0);
				}
				break;
		}
		
		// set receiver
		if(receiver != null)
		{
			Log.w(TAG, "startCameraPreviewInternal() - Change preview receiver to " + receiver);
			camera.set(Camera.PROP_PREVIEW_RECEIVER, receiver);
		}
		else
			Log.v(TAG, "startCameraPreviewInternal() - Use current preview receiver");
		
		// set preview size
		if(previewSize != null)
		{
			Log.w(TAG, "startCameraPreviewInternal() - Preview size : " + previewSize);
			camera.set(Camera.PROP_PREVIEW_SIZE, previewSize);
		}
		else
			Log.v(TAG, "startCameraPreviewInternal() - Use current preview size");
		
		// start preview
		Log.w(TAG, "startCameraPreviewInternal() - Start preview for camera " + camera);
		if(!camera.startPreview(0))
		{
			Log.e(TAG, "startCameraPreviewInternal() - Fail to start preview for camera " + camera);
			return false;
		}
		
		// create component with NORMAL priority
		if(!m_IsNormalComponentsCreated)
		{
			m_IsNormalComponentsCreated = true;
			m_ComponentManager.createComponents(ComponentCreationPriority.NORMAL, this);
			this.bindToNormalComponents();
		}
		
		// complete
		return true;
	}
	
	
	/**
	 * Stop camera preview.
	 * @param camera Camera to stop preview.
	 * @return Camera preview stops successfully or not.
	 */
	public final boolean stopCameraPreview(Camera camera)
	{
		return this.stopCameraPreview(camera, 0);
	}
	
	
	/**
	 * Stop camera preview.
	 * @param camera Camera to stop preview.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_SYNCHRONOUS}</li>
	 * </ul>
	 * @return Camera preview stops successfully or not.
	 */
	public final boolean stopCameraPreview(final Camera camera, final int flags)
	{
		if(camera == null)
		{
			Log.e(TAG, "stopCameraPreview() - No camera");
			return false;
		}
		if(this.isDependencyThread())
			return this.stopCameraPreviewInternal(camera, null, flags);
		else
		{
			final boolean isSync = ((flags & FLAG_SYNCHRONOUS) != 0);
			final boolean[] result = new boolean[]{ false };
			synchronized(result)
			{
				if(!HandlerUtils.post(this, new Runnable()
				{
					@Override
					public void run()
					{
						stopCameraPreviewInternal(camera, (isSync ? result : null), flags);
					}
				}))
				{
					Log.e(TAG, "stopCameraPreview() - Fail to perform cross-thread operation");
					return false;
				}
				if(isSync)
				{
					try
					{
						Log.w(TAG, "stopCameraPreview() - Wait for camera thread [start]");
						result.wait(5000);
						Log.w(TAG, "stopCameraPreview() - Wait for camera thread [end]");
						if(result[0])
							return true;
						Log.e(TAG, "stopCameraPreview() - Timeout");
						return false;
					}
					catch(InterruptedException ex)
					{
						Log.e(TAG, "stopCameraPreview() - Interrupted", ex);
						return false;
					}
				}
				return true;
			}
		}
	}
	
	
	// Stop camera preview
	private boolean stopCameraPreviewInternal(final Camera camera, final boolean[] result, int flags)
	{
		try
		{
			// waiting for starting preview
			if(camera.get(Camera.PROP_PREVIEW_STATE) == OperationState.STARTING)
			{
				if(result != null)
				{
					Log.w(TAG, "stopCameraPreviewInternal() - Wait for preview start");
					m_PendingCameraPreviewStopRequests.add(new CameraPreviewStopRequest(camera, result, flags));
					return true;
				}
			}
			
			// stop preview
			Log.v(TAG, "stopCameraPreviewInternal() - Stop preview [start]");
			camera.stopPreview(0);
			Log.v(TAG, "stopCameraPreviewInternal() - Stop preview [end]");
			
			// stop video recording
			if(Handle.isValid(m_VideoCaptureHandle))
			{
				Log.w(TAG, "stopCameraPreviewInternal() - Stop video recording");
				stopVideoCaptureInternal(m_VideoCaptureHandle, FLAG_NO_SHUTTER_SOUND);
			}
			
			// notify waiting thread
			if(result != null)
			{
				if(camera.get(Camera.PROP_PREVIEW_STATE) != OperationState.STOPPING)
				{
					synchronized(result)
					{
						Log.w(TAG, "stopCameraPreviewInternal() - Notify waiting thread");
						result[0] = true;
						result.notifyAll();
					}
				}
				else
				{
					Log.w(TAG, "stopCameraPreviewInternal() - Wait for camera preview stop");
					camera.addCallback(Camera.PROP_PREVIEW_STATE, new PropertyChangedCallback<OperationState>()
					{
						@Override
						public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
						{
							if(e.getOldValue() == OperationState.STOPPING)
							{
								synchronized(result)
								{
									Log.w(TAG, "stopCameraPreviewInternal() - Notify waiting thread");
									result[0] = true;
									result.notifyAll();
								}
								camera.removeCallback(Camera.PROP_PREVIEW_STATE, this);
							}
						}
					});
				}
			}
			return true;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "stopCameraPreviewInternal() - Error stopping camera preview", ex);
			if(result != null)
			{
				synchronized(result)
				{
					Log.w(TAG, "stopCameraPreviewInternal() - Notify waiting thread");
					result[0] = true;
					result.notifyAll();
				}
			}
			return false;
		}
	}
	
	
	// Stop photo capture.
	private void stopPhotoCapture(final PhotoCaptureHandle handle)
	{
		if(this.isDependencyThread())
			this.stopPhotoCaptureInternal(handle);
		else if(!HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				stopPhotoCaptureInternal(handle);
			}
		}))
		{
			Log.e(TAG, "stopPhotoCapture() - Fail to perform cross-thread operation");
		}
	}
	
	
	// Stop photo capture.
	private void stopPhotoCaptureInternal(PhotoCaptureHandle handle)
	{
		// check handle
		if(m_PhotoCaptureHandle != handle)
		{
			Log.e(TAG, "stopPhotoCaptureInternal() - Invalid handle");
			return;
		}
		
		Log.v(TAG, "stopPhotoCaptureInternal() - Handle : ", handle);
		
		// cancel directly when locking focus
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.STARTING)
		{
			Log.w(TAG, "stopPhotoCaptureInternal() - Stop when locking focus");
			this.completeCaptureInternal(null, null, false);
			return;
		}
		
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "stopPhotoCaptureInternal() - No camera");
			return;
		}
		
		// stop capture
		try
		{
			if(handle.captureHandler == null)
			{
				Log.w(TAG, "stopPhotoCaptureInternal() - Use default photo capture stop process");
				m_CameraCaptureHandle = Handle.close(m_CameraCaptureHandle);
				m_BurstCaptureSoundStreamHandle = Handle.close(m_BurstCaptureSoundStreamHandle);
			}
			else
			{
				Log.w(TAG, "stopPhotoCaptureInternal() - Use " + handle.captureHandler + " to stop capture");
				if(!handle.captureHandler.stopCapture(camera, handle))
					Log.e(TAG, "stopPhotoCaptureInternal() - Fail to stop capture");
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "stopPhotoCaptureInternal() - Fail to stop capture", ex);
		}
	}
	
	
	// Stop video recording.
	private void stopVideoCapture(final VideoCaptureHandle handle, final int flags)
	{
		if(this.isDependencyThread())
			this.stopVideoCaptureInternal(handle, flags);
		else if(!HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				stopVideoCaptureInternal(handle, flags);
			}
		}))
		{
			Log.e(TAG, "stopVideoCapture() - Fail to perform cross-thread operation");
		}
	}
	
	
	// Stop video recording.
	private void stopVideoCaptureInternal(VideoCaptureHandle handle, int flags)
	{
		// check handle
		if(m_VideoCaptureHandle != handle)
		{
			Log.w(TAG, "stopVideoCaptureInternal() - Invalid handle");
			return;
		}
		
		// check state
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case CAPTURING:
			case PAUSING:
			case PAUSED:
			case RESUMING:
				break;
			default:
				Log.w(TAG, "stopVideoCaptureInternal() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
				break;
		}
		
		// check state
		boolean isStarting = (this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.STARTING && this.getHandler().hasMessages(MSG_CAPTURE_VIDEO));
		
		// change state
		this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.STOPPING);
		
		// stop recording
		if(m_MediaRecorder != null && !isStarting)
		{
			try
			{
				this.get(PROP_CAMERA).stopPreview(0);
				Log.w(TAG, "stopVideoCaptureInternal() - MediaRecorder.stop [start]");
				m_MediaRecorder.stop();
				Log.w(TAG, "stopVideoCaptureInternal() - MediaRecorder.stop [end]");
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "stopVideoCaptureInternal() - Fail to stop recorder", ex);
			}
		}
		
		// play sound
		if((flags & FLAG_NO_SHUTTER_SOUND) == 0 && Handle.isValid(m_VideoStopSoundHandle))
			m_AudioManager.playSound(m_VideoStopSoundHandle, 0);
		
		// close handle
		handle.complete();
		m_VideoCaptureHandle = null;
		
		// clear video surface
		Camera camera = this.get(PROP_CAMERA);
		camera.set(Camera.PROP_VIDEO_SURFACE, null);
		
		// stop directly before starting recording
		if(isStarting)
		{
			Log.w(TAG, "stopVideoCaptureInternal() - Stop while starting");
			this.getHandler().removeMessages(MSG_CAPTURE_VIDEO);
		}
		
		// save video
		if(!isStarting)
		{
			VideoSaveTask saveTask = new VideoSaveTask(this.getContext(), m_VideoFilePath);
			m_ComponentManager.findComponent(FileManager.class, this).saveMedia(saveTask, 0);
		}
		
		// raise event
		this.raise(EVENT_DEFAULT_VIDEO_CAPTURE_COMPLETED, new CaptureEventArgs(handle));
		
		// release media recorder
		if(m_MediaRecorder != null)
		{
			switch(camera.get(Camera.PROP_PREVIEW_STATE))
			{
				case STARTED:
				case STOPPED:
					Log.v(TAG, "stopVideoCaptureInternal() - Release media recorder");
					m_MediaRecorder.release();
					m_MediaRecorder = null;
					break;
				default:
					Log.w(TAG, "stopVideoCaptureInternal() - Release media recorder after preview start or stop");
					break;
			}
		}
		
		// complete
		if(this.get(PROP_CAMERA_PREVIEW_STATE) == OperationState.STARTED && this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO)
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
		else
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
	}
}
