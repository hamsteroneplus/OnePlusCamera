package com.oneplus.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oneplus.base.BaseActivity;
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
import com.oneplus.base.ScreenSize;
import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.base.component.ComponentEventArgs;
import com.oneplus.base.component.ComponentManager;
import com.oneplus.base.component.ComponentOwner;
import com.oneplus.camera.media.MediaType;
import com.oneplus.camera.media.ResolutionManager;
import com.oneplus.camera.media.ResolutionManagerBuilder;
import com.oneplus.camera.ui.Viewfinder;
import com.oneplus.camera.ui.ViewfinderBuilder;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * Base class for camera-related activity.
 */
public abstract class CameraActivity extends BaseActivity implements ComponentOwner
{
	// Default component builders
	private static final ComponentBuilder[] DEFAULT_COMPONENT_BUILDERS = new ComponentBuilder[]{
		new ResolutionManagerBuilder(),
		new ViewfinderBuilder(),
	};
	
	
	/**
	 * Read-only property for available camera list.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final PropertyKey<List<Camera>> PROP_AVAILABLE_CAMERAS = new PropertyKey<List<Camera>>("AvailableCameras", (Class)List.class, CameraActivity.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property for current primary camera.
	 */
	public static final PropertyKey<Camera> PROP_CAMERA = new PropertyKey<>("Camera", Camera.class, CameraActivity.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property for current camera preview size.
	 */
	public static final PropertyKey<Size> PROP_CAMERA_PREVIEW_SIZE = new PropertyKey<>("CameraPreviewSize", Size.class, CameraActivity.class, new Size(0, 0));
	/**
	 * Read-only property to get root content view.
	 */
	public static final PropertyKey<View> PROP_CONTENT_VIEW = new PropertyKey<>("ContentView", View.class, CameraActivity.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property to check whether camera thread is started or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_CAMERA_THREAD_STARTED = new PropertyKey<>("IsCameraThreadStarted", Boolean.class, CameraActivity.class, false);
	/**
	 * Read-only property to check whether activity is launching (before first starting preview) or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_LAUNCHING = new PropertyKey<>("IsLaunching", Boolean.class, CameraActivity.class, true);
	/**
	 * Read-only property for current captured media type.
	 */
	public static final PropertyKey<MediaType> PROP_MEDIA_TYPE = new PropertyKey<>("MediaType", MediaType.class, CameraActivity.class, MediaType.PHOTO);
	/**
	 * Read-only property for photo capture state.
	 */
	public static final PropertyKey<PhotoCaptureState> PROP_PHOTO_CAPTURE_STATE = new PropertyKey<>("PhotoCaptureState", PhotoCaptureState.class, CameraActivity.class, PhotoCaptureState.PREPARING);
	/**
	 * Read-only property for screen size.
	 */
	public static final PropertyKey<ScreenSize> PROP_SCREEN_SIZE = new PropertyKey<>("ScreenSize", ScreenSize.class, CameraActivity.class, ScreenSize.EMPTY);
	/**
	 * Read-only property for current settings.
	 */
	public static final PropertyKey<Settings> PROP_SETTINGS = new PropertyKey<>("Settings", Settings.class, CameraActivity.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property for video capture state.
	 */
	public static final PropertyKey<VideoCaptureState> PROP_VIDEO_CAPTURE_STATE = new PropertyKey<>("VideoCaptureState", VideoCaptureState.class, CameraActivity.class, VideoCaptureState.PREPARING);
	
	
	/**
	 * Event raised when media capture is cancelled.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_CAPTURE_CANCELLED = new EventKey<>("CaptureCancelled", CaptureEventArgs.class, CameraActivity.class);
	/**
	 * Event raised when media capture completed.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_CAPTURE_COMPLETED = new EventKey<>("CaptureCompleted", CaptureEventArgs.class, CameraActivity.class);
	/**
	 * Event raised when media capture starts.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_CAPTURE_STARTED = new EventKey<>("CaptureStarted", CaptureEventArgs.class, CameraActivity.class);
	
	
	// Constants
	private static final int MSG_CAMERA_THREAD_EVENT_RAISED = -1;
	private static final int MSG_CAMERA_THREAD_PROP_CHANGED = -2;
	
	
	// Private fields
	private CameraThread m_CameraThread;
	private ComponentManager m_ComponentManager;
	private final List<ComponentBuilder> m_InitialComponentBuilders = new ArrayList<>();
	private boolean m_IsCameraPreviewReceiverReady;
	private CaptureHandleImpl m_PendingPhotoCaptureHandle;
	private CaptureHandleImpl m_PhotoCaptureHandle;
	private ResolutionManager m_ResolutionManager;
	private final List<SettingsHandle> m_SettingsHandles = new ArrayList<>();
	private CaptureHandleImpl m_VideoCaptureHandle;
	private Viewfinder m_Viewfinder;
	
	
	// Class capture handle.
	private final class CaptureHandleImpl extends CaptureHandle
	{
		public final long creationTime;
		public final int frameCount;
		public CaptureHandle internalCaptureHandle;
		
		public CaptureHandleImpl(int frameCount)
		{
			super(MediaType.PHOTO);
			this.frameCount = frameCount;
			this.creationTime = SystemClock.elapsedRealtime();
		}

		@Override
		protected void onClose(int flags)
		{
			//
		}
	}
	
	
	// Class for settings handle.
	private final class SettingsHandle extends Handle
	{
		public final Settings settings;
		
		public SettingsHandle(Settings settings)
		{
			super("Settings");
			this.settings = settings;
		}

		@Override
		protected void onClose(int flags)
		{
			restoreSettings(this);
		}
	}
	
	
	/**
	 * Initialize new CameraActivity instance.
	 */
	protected CameraActivity()
	{}
	
	
	/**
	 * Add component builders.
	 * @param builders Component builders to add.
	 */
	public final void addComponentBuilders(final ComponentBuilder[] builders)
	{
		this.verifyAccess();
		if(m_ComponentManager != null)
			m_ComponentManager.addComponentBuilders(builders, this);
		else
			m_InitialComponentBuilders.addAll(Arrays.asList(builders));
	}
	
	
	// Bind to camera thread (called in camera thread).
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void bindToCameraThread(MediaType initialMediaType, List<EventKey<?>> eventKeys, List<PropertyKey<?>> propKeys)
	{
		Log.v(TAG, "bindToCameraThread()");
		
		// add event handlers
		if(!eventKeys.isEmpty())
		{
			EventHandler handler = new EventHandler()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey key, EventArgs e)
				{
					HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_THREAD_EVENT_RAISED, 0, 0, new Object[]{ key, e.clone() });
				}
			};
			for(int i = eventKeys.size() - 1 ; i >= 0 ; --i)
				m_CameraThread.addHandler(eventKeys.get(i), handler);
		}
		
		// add property changed call-backs
		if(!propKeys.isEmpty())
		{
			PropertyChangedCallback callback = new PropertyChangedCallback()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
				{
					HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_THREAD_PROP_CHANGED, 0, 0, new Object[]{ key, e.clone() });
				}
			};
			for(int i = propKeys.size() - 1 ; i >= 0 ; --i)
				m_CameraThread.addCallback(propKeys.get(i), callback);
		}
		
		// set initial media type
		if(!m_CameraThread.setMediaType(initialMediaType))
			Log.e(TAG, "bindToCameraThread() - Fail to set initial media type to " + initialMediaType);
		
		// notify
		this.onBindToCameraThread();
	}
	
	
	// Bind to related components
	private boolean bindToComponents()
	{
		// ResolutionManager
		if(this.getResolutionManager() == null)
		{
			Log.e(TAG, "bindToComponents() - No ResolutionManager");
			return false;
		}
		
		// Viewfinder
		if(this.getViewfinder() == null)
		{
			Log.e(TAG, "bindToComponents() - No Viewfinder");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	/**
	 * Check whether camera preview can be started or not according to current states.
	 * @return Whether camera preview can be started or not.
	 */
	protected boolean canStartCameraPreview()
	{
		// check activity state
		switch(this.get(PROP_STATE))
		{
			case RESUMING:
			case RUNNING:
				break;
			default:
				return false;
		}
		
		// check preview receiver
		if(!m_IsCameraPreviewReceiverReady)
			return false;
		
		// check orientation
		if(this.get(PROP_CONFIG_ORIENTATION) != Configuration.ORIENTATION_LANDSCAPE)
			return false;
		
		// OK
		return true;
	}
	
	
	/**
	 * Check whether video snapshot can be triggered in current state or not.
	 * @return Video snapshot support state.
	 */
	public boolean canVideoSnapshot()
	{
		switch(this.get(PROP_MEDIA_TYPE))
		{
			case PHOTO:
				return false;
			case VIDEO:
				switch(this.get(PROP_VIDEO_CAPTURE_STATE))
				{
					case CAPTURING:
					case PAUSED:
						return true;
					default:
						return false;
				}
			default:
				Log.e(TAG, "canVideoSnapshot() - Unknown media type : " + this.get(PROP_MEDIA_TYPE));
				return false;
		}
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
	public CaptureHandle capturePhoto(int frameCount, int flags)
	{
		// check parameter
		if(frameCount == 0)
		{
			Log.e(TAG, "capturePhoto() - Invalid frame count");
			return null;
		}
		
		// check state
		this.verifyAccess();
		if(m_CameraThread == null)
		{
			Log.e(TAG, "capturePhoto() - No camera thread");
			return null;
		}
		
		// check photo capture state
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case READY:
				break;
			case STARTING:
			case CAPTURING:
			case STOPPING:
				m_PendingPhotoCaptureHandle = new CaptureHandleImpl(frameCount);
				Log.w(TAG, "capturePhoto() - Start capture after current capture completes, create handle : " + m_PendingPhotoCaptureHandle);
				return m_PendingPhotoCaptureHandle;
			default:
				Log.e(TAG, "capturePhoto() - Capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return null;
		}
		if(this.get(PROP_STATE) != State.RUNNING)
		{
			Log.e(TAG, "capturePhoto() - Activity state is " + this.get(PROP_STATE));
			return null;
		}
		
		// create handle
		CaptureHandleImpl handle = new CaptureHandleImpl(frameCount);
		
		// capture
		if(!this.capturePhoto(handle))
		{
			Log.e(TAG, "capturePhoto() - Fail to capture");
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
			return null;
		}
		
		// complete
		return m_PhotoCaptureHandle;
	}
	
	
	// Start photo capture.
	private boolean capturePhoto(CaptureHandleImpl handle)
	{
		Log.v(TAG, "capturePhoto() - Handle : ", handle);
		
		// check video snapshot
		if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO && !this.canVideoSnapshot())
		{
			Log.e(TAG, "capturePhoto() - Cannot take video snapshot");
			return false;
		}
		
		// change state
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.STARTING);
		
		// start count-down
		switch(this.get(PROP_MEDIA_TYPE))
		{
			case PHOTO:
				//
				break;
			case VIDEO:
				Log.w(TAG, "capturePhoto() - Video snapshot");
				break;
		}
		
		// capture
		Log.w(TAG, "capturePhoto() - Capture");
		handle.internalCaptureHandle = m_CameraThread.capturePhoto(handle.frameCount, 0);
		if(!Handle.isValid(handle.internalCaptureHandle))
			return false;
		
		// save handle
		m_PhotoCaptureHandle = handle;
		
		// update states
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.CAPTURING);
		this.raise(EVENT_CAPTURE_STARTED, new CaptureEventArgs(handle));
		
		// complete
		return true;
	}
	
	
	/**
	 * Complete media capture process.
	 * @param handle Capture handle returned from {@link #capturePhoto(int, int) capturePhoto}.
	 */
	public void completeCapture(CaptureHandle handle)
	{
		// check parameter
		if(handle == null)
		{
			Log.e(TAG, "completeCapture() - No handle");
			return;
		}
		if(!(handle instanceof CaptureHandleImpl))
		{
			Log.e(TAG, "completeCapture() - Invalid handle");
			return;
		}
		
		// check state
		this.verifyAccess();
		
		// complete capture
		CaptureHandleImpl handleImpl = (CaptureHandleImpl)handle;
		switch(handleImpl.getMediaType())
		{
			case PHOTO:
				this.completeCapturePhoto(handleImpl);
				break;
			case VIDEO:
				//
				break;
		}
	}
	
	
	// complete capture photo.
	private void completeCapturePhoto(CaptureHandleImpl handle)
	{
		// check handle
		if(m_PhotoCaptureHandle != handle)
		{
			Log.e(TAG, "completeCapturePhoto() - Invalid handle");
			return;
		}
		
		// check state
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case CAPTURING:
			case STOPPING:
				break;
			default:
				Log.e(TAG, "completeCapturePhoto() - Current capture state : " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return;
		}
		
		// change state
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.STOPPING);
		
		// show review screen or complete process
		//
		this.onCaptureCompleted(handle);
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
	
	
	/**
	 * Get related camera thread.
	 * @return Related {@link CameraThread}.
	 */
	public final CameraThread getCameraThread()
	{
		return m_CameraThread;
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
	 * Get resolution manager.
	 * @return Resolution manager.
	 */
	public final ResolutionManager getResolutionManager()
	{
		if(m_ResolutionManager == null)
		{
			m_ResolutionManager = m_ComponentManager.findComponent(ResolutionManager.class, this);
			if(m_ResolutionManager != null)
			{
				PropertyChangedCallback<Size> callback = new PropertyChangedCallback<Size>()
				{
					@Override
					public void onPropertyChanged(PropertySource source, PropertyKey<Size> key, PropertyChangeEventArgs<Size> e)
					{
						selectCameraPreviewSize();
					}
				};
				m_ResolutionManager.addCallback(ResolutionManager.PROP_PHOTO_PREVIEW_SIZE, callback);
				m_ResolutionManager.addCallback(ResolutionManager.PROP_VIDEO_PREVIEW_SIZE, callback);
			}
			else
				Log.e(TAG, "getResolutionManager() - No ResolutionManager");
		}
		return m_ResolutionManager;
	}
	
	
	/**
	 * Get viewfinder.
	 * @return Viewfinder.
	 */
	public final Viewfinder getViewfinder()
	{
		if(m_Viewfinder == null)
		{
			m_Viewfinder = m_ComponentManager.findComponent(Viewfinder.class, this);
			if(m_Viewfinder != null)
			{
				m_Viewfinder.addCallback(Viewfinder.PROP_PREVIEW_RECEIVER, new PropertyChangedCallback<Object>()
				{
					@Override
					public void onPropertyChanged(PropertySource source, PropertyKey<Object> key, PropertyChangeEventArgs<Object> e)
					{
						Object receiver = e.getNewValue();
						if(receiver != null)
							onCameraPreviewReceiverReady(receiver);
						else
							onCameraPreviewReceiverDestroyed();
					}
				});
			}
			else
				Log.e(TAG, "bindToComponents() - No Viewfinder");
		}
		return m_Viewfinder;
	}
	
	
	/**
	 * Handle message.
	 * @param msg Message.
	 */
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_CAMERA_THREAD_EVENT_RAISED:
			{
				Object[] array = (Object[])msg.obj;
				this.onCameraThreadEventReceived((EventKey<?>)array[0], (EventArgs)array[1]);
				break;
			}
			
			case MSG_CAMERA_THREAD_PROP_CHANGED:
			{
				Object[] array = (Object[])msg.obj;
				this.onCameraThreadPropertyChanged((PropertyKey<?>)array[0], (PropertyChangeEventArgs<?>)array[1]);
				break;
			}
		}
	}
	
	
	/**
	 * Called when available cameras list changes.
	 * @param cameras Available cameras.
	 */
	protected void onAvailableCamerasChanged(List<Camera> cameras)
	{
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera != null)
		{
			if(cameras.contains(camera))
				return;
			Log.w(TAG, "onAvailableCamerasChanged() - Camera " + camera + " is not contained in new list");
		}
		
		// update property
		this.setReadOnly(PROP_AVAILABLE_CAMERAS, cameras);
	}
	
	
	/**
	 * Called when binding to camera thread.
	 */
	protected void onBindToCameraThread()
	{
		// get initial camera list
		final List<Camera> cameras = m_CameraThread.get(CameraThread.PROP_AVAILABLE_CAMERAS);
		HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				onAvailableCamerasChanged(cameras);
			}
		});
	}
	
	
	/**
	 * Called before adding event handlers to camera thread.
	 * @param keys Key of events.
	 */
	protected void onBindingToCameraThreadEvents(List<EventKey<?>> keys)
	{
		keys.add(CameraThread.EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED);
	}
	
	
	/**
	 * Called before adding call-backs to camera thread properties.
	 * @param keys Key of properties.
	 */
	protected void onBindingToCameraThreadProperties(List<PropertyKey<?>> keys)
	{
		keys.add(CameraThread.PROP_AVAILABLE_CAMERAS);
	}
	
	
	/**
	 * Called when camera preview receiver destroyed.
	 */
	protected void onCameraPreviewReceiverDestroyed()
	{
		// update state
		m_IsCameraPreviewReceiverReady = false;
		
		// stop preview
		this.stopCameraPreview(true);
	}
	
	
	/**
	 * Called when camera preview receiver ready.
	 * @param receiver Camera preview receiver.
	 */
	protected void onCameraPreviewReceiverReady(Object receiver)
	{
		// stop preview first
		if(m_IsCameraPreviewReceiverReady)
			this.onCameraPreviewReceiverDestroyed();
		
		// update state
		m_IsCameraPreviewReceiverReady = true;
		
		// start preview
		this.startCameraPreview();
	}
	
	
	/**
	 * Called when receiving camera thread event in UI thread.
	 * @param key Event key.
	 * @param e Event data.
	 */
	protected void onCameraThreadEventReceived(EventKey<?> key, EventArgs e)
	{
		if(key == CameraThread.EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED)
			this.onDefaultPhotoCaptureCompleted((CaptureEventArgs)e);
	}
	
	
	/**
	 * Called when receiving camera thread property change in UI thread.
	 * @param key Key of changed property.
	 * @param e Event data.
	 */
	@SuppressWarnings("unchecked")
	protected void onCameraThreadPropertyChanged(PropertyKey<?> key, PropertyChangeEventArgs<?> e)
	{
		if(key == CameraThread.PROP_AVAILABLE_CAMERAS)
			this.onAvailableCamerasChanged((List<Camera>)e.getNewValue());
	}
	
	
	/**
	 * Called when camera thread starts.
	 */
	protected void onCameraThreadStarted()
	{
		// start binding to camera thread
		final List<EventKey<?>> eventKeys = new ArrayList<>();
		final List<PropertyKey<?>> propKeys = new ArrayList<>();
		final MediaType initialMediaType = this.get(PROP_MEDIA_TYPE);
		this.onBindingToCameraThreadEvents(eventKeys);
		this.onBindingToCameraThreadProperties(propKeys);
		Handler handler = m_CameraThread.getHandler();
		if(handler == null)
		{
			Log.e(TAG, "onCameraThreadStarted() - No camera thread handler");
			this.finish();
			return;
		}
		if(!handler.postAtFrontOfQueue(new Runnable()
		{
			@Override
			public void run()
			{
				bindToCameraThread(initialMediaType, eventKeys, propKeys);
			}
		}))
		{
			Log.e(TAG, "onCameraThreadStarted() - Fail to start binding to camera thread");
			this.finish();
			return;
		}
	}
	
	
	// Called when media capture process completed, including review screen.
	private void onCaptureCompleted(CaptureHandleImpl handle)
	{
		Log.w(TAG, "onCaptureCompleted() - Handle : " + handle);
		
		// clear pending photo capture
		CaptureHandleImpl pendingHandle = m_PendingPhotoCaptureHandle;
		m_PendingPhotoCaptureHandle = null;
		
		// complete capture
		if(this.get(PROP_STATE) == State.RUNNING)
		{
			switch(handle.getMediaType())
			{
				case PHOTO:
				{
					if(this.startCameraPreview())
					{
						// cancel pending capture if completing by review screen
						if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.REVIEWING)
							pendingHandle = null;
						
						// reset capture state
						this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
						
						// start pending photo capture
						if(pendingHandle != null && (SystemClock.elapsedRealtime() - pendingHandle.creationTime) <= 800)
						{
							Log.w(TAG, "onCaptureCompleted() - Capture next photo immediately");
							if(this.capturePhoto(pendingHandle))
								return;
						}
					}
					else
					{
						Log.e(TAG, "onCaptureCompleted() - Fail to start camera preview");
						this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
					}
					break;
				}
				
				case VIDEO:
				{
					//
					break;
				}
			}
		}
		else
		{
			Log.w(TAG, "onCaptureCompleted() -Activity state is " + this.get(PROP_STATE));
			
			// reset capture state
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
		}
	}
	
	
	// Called when configuration changes.
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		// call super
		super.onConfigurationChanged(newConfig);
		
		// update screen size
		this.updateScreenSize();
		
		// start preview
		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
			this.startCameraPreview();
	}
	
	
	// Called after setting content view.
	private void onContentViewSet(View view)
	{
		// update property
		this.setReadOnly(PROP_CONTENT_VIEW, view);
		
		// create component with HIGH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.HIGH, this);
		
		// bind to components
		if(!this.bindToComponents())
			this.finish();
	}
	
	
	// Called when creating activity.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{	
		// call super
		super.onCreate(savedInstanceState);
		
		// enable logs
		this.enablePropertyLogs(PROP_CAMERA_PREVIEW_SIZE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PHOTO_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_SETTINGS, LOG_PROPERTY_CHANGE);
		
		// create global settings
		Settings settings = new Settings(this, null, false);
		m_SettingsHandles.add(new SettingsHandle(settings));
		this.setReadOnly(PROP_SETTINGS, settings);
		
		// create camera thread
		m_CameraThread = new CameraThread(this, new BaseThread.ThreadStartCallback()
		{
			@Override
			public void onCameraThreadStarted(BaseThread cameraThread)
			{
				CameraActivity.this.setReadOnly(PROP_IS_CAMERA_THREAD_STARTED, true);
				CameraActivity.this.onCameraThreadStarted();
			}
		}, this.getHandler());
		
		// update screen size
		this.updateScreenSize();
		
		// create component manager
		m_ComponentManager = new ComponentManager();
		m_ComponentManager.addComponentBuilders(DEFAULT_COMPONENT_BUILDERS, this);
		m_ComponentManager.addHandler(ComponentManager.EVENT_COMPONENT_ADDED, new EventHandler<ComponentEventArgs<Component>>()
		{
			@Override
			public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
			{
				CameraActivity.this.raise(EVENT_COMPONENT_ADDED, e);
			}
		});
		m_ComponentManager.addHandler(ComponentManager.EVENT_COMPONENT_REMOVED, new EventHandler<ComponentEventArgs<Component>>()
		{
			@Override
			public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
			{
				CameraActivity.this.raise(EVENT_COMPONENT_REMOVED, e);
			}
		});
		if(!m_InitialComponentBuilders.isEmpty())
		{
			ComponentBuilder[] builders = new ComponentBuilder[m_InitialComponentBuilders.size()];
			m_InitialComponentBuilders.toArray(builders);
			m_InitialComponentBuilders.clear();
			m_ComponentManager.addComponentBuilders(builders, this);
		}
		
		// create component with LAUNCH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.LAUNCH, this);
	}
	
	
	// Called when default photo capture process completed by camera thread.
	private void onDefaultPhotoCaptureCompleted(CaptureEventArgs e)
	{
		// check handle
		if(m_PhotoCaptureHandle == null || m_PhotoCaptureHandle.internalCaptureHandle != e.getCaptureHandle())
		{
			Log.w(TAG, "onDefaultPhotoCaptureCompleted() - Unknown capture handle : " + e.getCaptureHandle());
			Log.w(TAG, "onDefaultPhotoCaptureCompleted() - Expected capture handle : " + (m_PhotoCaptureHandle != null ? m_PhotoCaptureHandle.internalCaptureHandle : null));
			return;
		}
		
		// complete capture
		this.completeCapture(m_PhotoCaptureHandle);
	}
	
	
	// Called when destroying activity
	@Override
	protected void onDestroy()
	{
		// change state
		this.setReadOnly(PROP_STATE, State.DESTROYING);
		
		// release camera thread
		if(m_CameraThread != null)
		{
			m_CameraThread.release();
			try
			{
				m_CameraThread.join();
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "onDestroy() - Fail to join camera thread", ex);
			}
		}
		
		// call super
		super.onDestroy();
	}
	
	
	// Called when pausing.
	@Override
	protected void onPause()
	{
		// stop preview
		this.stopCameraPreview(true);
		
		// call super
		super.onPause();
		
		// close all cameras
		if(m_CameraThread != null)
			m_CameraThread.closeCameras();
	}
	
	
	// Called when resuming.
	@Override
	protected void onResume()
	{
		// update screen size
		this.updateScreenSize();
		
		// call super
		super.onResume();
		
		// start preview
		if(this.canStartCameraPreview())
			this.startCameraPreview();
	}
	
	
	// Release and remove given component.
	@Override
	public void removeComponent(Component component)
	{
		m_ComponentManager.removeComponent(component);
	}
	
	
	// Restore to previous settings.
	private void restoreSettings(SettingsHandle handle)
	{
		// check state
		this.verifyAccess();
		
		// check handle
		int index = m_SettingsHandles.indexOf(handle);
		if(index < 0)
		{
			Log.w(TAG, "restoreSettings() - Invalid handle");
			return;
		}
		
		Log.w(TAG, "restoreSettings() - Handle : " + handle);
		
		// restore settings
		m_SettingsHandles.remove(index);
		if(index == m_SettingsHandles.size())
		{
			if(!m_SettingsHandles.isEmpty())
			{
				SettingsHandle prevHandle = m_SettingsHandles.get(index - 1);
				this.setReadOnly(PROP_SETTINGS, prevHandle.settings);
			}
			else
			{
				Log.e(TAG, "restoreSettings() - All settings are removed");
				this.setReadOnly(PROP_SETTINGS, null);
			}
		}
	}
	
	
	/**
	 * Select camera preview size according to current states.
	 */
	protected void selectCameraPreviewSize()
	{
		ResolutionManager resolutionManager = this.getResolutionManager();
		if(resolutionManager == null)
		{
			Log.e(TAG, "selectCameraPreviewSize() - No ResolutionManager.");
			return;
		}
		Size previewSize;
		switch(this.get(PROP_MEDIA_TYPE))
		{
			case PHOTO:
				previewSize = resolutionManager.get(ResolutionManager.PROP_PHOTO_PREVIEW_SIZE);
				break;
			case VIDEO:
				previewSize = resolutionManager.get(ResolutionManager.PROP_VIDEO_PREVIEW_SIZE);
				break;
			default:
				Log.e(TAG, "selectCameraPreviewSize() - Unknown media type : " + this.get(PROP_MEDIA_TYPE));
				return;
		}
		this.setReadOnly(PROP_CAMERA_PREVIEW_SIZE, previewSize);
	}
	
	
	// Set content view.
	@Override
	public void setContentView(int layoutResID)
	{
		Log.v(TAG, "setContentView() - Load content view [start]");
		View view = this.getLayoutInflater().inflate(layoutResID, null);
		Log.v(TAG, "setContentView() - Load content view [end]");
		this.setContentView(view);
	}
	
	
	// Set content view.
	@Override
	public void setContentView(View view)
	{
		Log.v(TAG, "setContentView() - Set content view [start]");
		super.setContentView(view);
		Log.v(TAG, "setContentView() - Set content view [end]");
		this.onContentViewSet(view);
	}
	
	
	// Set content view.
	@Override
	public void setContentView(View view, LayoutParams params)
	{
		Log.v(TAG, "setContentView() - Set content view [start]");
		super.setContentView(view, params);
		Log.v(TAG, "setContentView() - Set content view [end]");
		this.onContentViewSet(view);
	}
	
	
	/**
	 * Start current primary camera preview.
	 * @return Whether preview starts successfully or not.
	 */
	public final boolean startCameraPreview()
	{
		// check state
		this.verifyAccess();
		if(!this.canStartCameraPreview())
		{
			Log.w(TAG, "startCameraPreview() - Cannot start preview in current state");
			return false;
		}
		
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.w(TAG, "startCameraPreview() - No camera to start preview");
			return false;
		}
		
		// start preview
		if(!m_CameraThread.startCameraPreview(camera, m_Viewfinder.get(Viewfinder.PROP_PREVIEW_RECEIVER)))
		{
			Log.e(TAG, "startCameraPreview() - Fail to start camera preview");
			return false;
		}
		
		// change state and create components with NORMAL priority
		if(this.setReadOnly(PROP_IS_LAUNCHING, false))
			m_ComponentManager.createComponents(ComponentCreationPriority.NORMAL, this);
		
		// change capture state
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.PREPARING)
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
		if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO && this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.PREPARING)
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
		
		// complete
		return true;
	}
	
	
	/**
	 * Stop current primary camera preview.
	 */
	public final void stopCameraPreview()
	{
		this.stopCameraPreview(false);
	}
	
	
	// Stop camera preview.
	private void stopCameraPreview(boolean sync)
	{
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
			return;
		
		// change capture state
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.READY)
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
		if(this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.READY)
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
		
		// stop preview
		int flags = (sync ? CameraThread.FLAG_SYNCHRONOUS : 0);
		if(m_CameraThread != null)
		{
			if(!m_CameraThread.stopCameraPreview(camera, flags))
			{
				if(sync)
					Log.e(TAG, "stopCameraPreview() - Fail to stop camera preview synchronously");
				else
					Log.e(TAG, "stopCameraPreview() - Fail to stop camera preview");
			}
		}
	}
	
	
	/**
	 * Change current media type.
	 * @param mediaType New media type.
	 * @return Whether media type changes successfully or not.
	 */
	public boolean setMediaType(MediaType mediaType)
	{
		// check state
		this.verifyAccess();
		if(this.get(PROP_MEDIA_TYPE) == mediaType)
			return true;
		Log.w(TAG, "setMediaType() - Media type : " + mediaType);
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
						Log.e(TAG, "setMediaType() - Current video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
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
						Log.e(TAG, "setMediaType() - Current photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
						return false;
				}
				break;
			}
			
			default:
				Log.e(TAG, "setMediaType() - Unknown media type : " + mediaType);
				return false;
		}
		
		// change media type
		if(this.get(PROP_IS_CAMERA_THREAD_STARTED))
		{
			if(!m_CameraThread.setMediaType(mediaType))
			{
				Log.e(TAG, "setMediaType() - Fail to change media type");
				return false;
			}
		}
		else
			Log.w(TAG, "setMediaType() - Change media type before camera thread start");
		this.setReadOnly(PROP_MEDIA_TYPE, mediaType);
		
		// select preview size
		this.selectCameraPreviewSize();
		
		// complete
		return true;
	}
	
	
	/**
	 * Change current settings.
	 * @param settings New settings to apply.
	 * @return Handle to this settings.
	 */
	public final Handle setSettings(Settings settings)
	{
		// check state
		this.verifyAccess();
		
		// check parameter
		if(settings == null)
		{
			Log.e(TAG, "setSettings() - No settings.");
			return null;
		}
		
		// create handle
		SettingsHandle handle = new SettingsHandle(settings);
		m_SettingsHandles.add(handle);
		Log.w(TAG, "setSettings() - Create handle : " + handle);
		
		// apply
		this.setReadOnly(PROP_SETTINGS, settings);
		return handle;
	}
	
	
	// Update screen size.
	private void updateScreenSize()
	{
		ScreenSize size = new ScreenSize(this);
		if(this.setReadOnly(PROP_SCREEN_SIZE, size))
			Log.w(TAG, "updateScreenSize() - Screen size : " + size);
		if(m_CameraThread != null)
			m_CameraThread.setScreenSize(size);
	}
}
