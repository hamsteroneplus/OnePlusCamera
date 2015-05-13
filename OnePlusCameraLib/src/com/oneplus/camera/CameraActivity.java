package com.oneplus.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
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
import com.oneplus.base.Rotation;
import com.oneplus.base.ScreenSize;
import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.base.component.ComponentEventArgs;
import com.oneplus.base.component.ComponentManager;
import com.oneplus.base.component.ComponentOwner;
import com.oneplus.camera.Camera.LensFacing;
import com.oneplus.camera.media.MediaType;
import com.oneplus.camera.media.ResolutionManager;
import com.oneplus.camera.media.ResolutionManagerBuilder;
import com.oneplus.camera.ui.MotionEventArgs;
import com.oneplus.camera.ui.Viewfinder;
import com.oneplus.camera.ui.ViewfinderBuilder;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * Base class for camera-related activity.
 */
public abstract class CameraActivity extends BaseActivity implements ComponentOwner
{
	// Default component builders
	private static final ComponentBuilder[] DEFAULT_COMPONENT_BUILDERS = new ComponentBuilder[]{
		new FocusControllerBuilder(),
		new ResolutionManagerBuilder(),
		new ViewfinderBuilder(),
	};
	
	
	/**
	 * Read-only property for current activity rotation.
	 */
	public static final PropertyKey<Rotation> PROP_ACTIVITY_ROTATION = new PropertyKey<>("ActivityRotation", Rotation.class, CameraActivity.class, Rotation.LANDSCAPE);
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
	 * Read-only property for current primary camera preview state.
	 */
	public static final PropertyKey<OperationState> PROP_CAMERA_PREVIEW_STATE = new PropertyKey<>("CameraPreviewState", OperationState.class, CameraActivity.class, OperationState.STOPPED);
	/**
	 * Read-only property to get root content view.
	 */
	public static final PropertyKey<View> PROP_CONTENT_VIEW = new PropertyKey<>("ContentView", View.class, CameraActivity.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property to check whether camera is locked (cannot to be switched) or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_CAMERA_LOCKED = new PropertyKey<>("IsCameraLocked", Boolean.class, CameraActivity.class, false);
	/**
	 * Read-only property to check whether camera thread is started or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_CAMERA_THREAD_STARTED = new PropertyKey<>("IsCameraThreadStarted", Boolean.class, CameraActivity.class, false);
	/**
	 * Read-only property to check whether self timer is started or not.
	 */
	public static final PropertyKey<Boolean> PROP_IS_SELF_TIMER_STARTED = new PropertyKey<>("IsSelfTimerStarted", Boolean.class, CameraActivity.class, false);
	/**
	 * Read-only property for current device orientation.
	 */
	public static final PropertyKey<Integer> PROP_DEVICE_ORIENTATION = new PropertyKey<>("DeviceOrientation", Integer.class, CameraActivity.class, 0);
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
	 * Read-only property for current UI rotation.
	 */
	public static final PropertyKey<Rotation> PROP_ROTATION = new PropertyKey<>("Rotation", Rotation.class, CameraActivity.class, Rotation.PORTRAIT);
	/**
	 * Read-only property for screen size.
	 */
	public static final PropertyKey<ScreenSize> PROP_SCREEN_SIZE = new PropertyKey<>("ScreenSize", ScreenSize.class, CameraActivity.class, ScreenSize.EMPTY);
	/**
	 * Property to get or set self-timer interval in seconds.
	 */
	public static final PropertyKey<Long> PROP_SELF_TIMER_INTERVAL = new PropertyKey<>("SelfTimerInterval", Long.class, CameraActivity.class, PropertyKey.FLAG_NOT_NULL, 0L);
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
	 * Event raised when media capture failed.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_CAPTURE_FAILED = new EventKey<>("CaptureFailed", CaptureEventArgs.class, CameraActivity.class);
	/**
	 * Event raised when media capture starts.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_CAPTURE_STARTED = new EventKey<>("CaptureStarted", CaptureEventArgs.class, CameraActivity.class);
	/**
	 * Event raised when handling touch input.
	 */
	public static final EventKey<MotionEventArgs> EVENT_TOUCH = new EventKey<>("Touch", MotionEventArgs.class, CameraActivity.class);
	
	
	// Constants
	private static final String SETTINGS_KEY_CAMERA_LENS_FACING = "CameraLensFacing";
	private static final String SETTINGS_KEY_SELF_TIMER_INTERVAL_BACK = "SelfTimer.Back";
	private static final String SETTINGS_KEY_SELF_TIMER_INTERVAL_FRONT = "SelfTimer.Front";
	private static final int MSG_CAMERA_THREAD_EVENT_RAISED = -1;
	private static final int MSG_CAMERA_THREAD_PROP_CHANGED = -2;
	private static final int MSG_CAMERA_PREVIEW_START_FAILED = -10;
	private static final int MSG_CAMERA_PREVIEW_STARTED = -11;
	private static final int MSG_PHOTO_CAPTURE_FAILED = -20;
	private static final int MSG_PHOTO_CAPTURE_STARTED = -21;
	private static final int MSG_VIDEO_CAPTURE_FAILED = -30;
	private static final int MSG_VIDEO_CAPTURE_STARTED = -31;
	
	
	// Private fields
	private Rotation m_ActivityRotation = Rotation.LANDSCAPE;
	private final LinkedList<CameraLockHandle> m_CameraLockHandles = new LinkedList<>();
	private CameraThread m_CameraThread;
	private OperationState m_CameraPreviewState = OperationState.STOPPED;
	private ComponentManager m_ComponentManager;
	private CountDownTimer m_CountDownTimer;
	private final List<ComponentBuilder> m_InitialComponentBuilders = new ArrayList<>();
	private boolean m_IsCameraPreviewReceiverReady;
	private boolean m_IsOrientationListenerStarted;
	private OrientationEventListener m_OrientationListener;
	private CaptureHandleImpl m_PendingPhotoCaptureHandle;
	private CaptureHandleImpl m_PhotoCaptureHandle;
	private ResolutionManager m_ResolutionManager;
	private Rotation m_Rotation = Rotation.PORTRAIT;
	private Handle m_SelfTimerHandle;
	private final List<SettingsHandle> m_SettingsHandles = new ArrayList<>();
	private CaptureHandleImpl m_VideoCaptureHandle;
	private Viewfinder m_Viewfinder;
	
	
	// Class for capture handle.
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
		public CaptureHandleImpl()
		{
			super(MediaType.VIDEO);
			this.frameCount = 0;
			this.creationTime = SystemClock.elapsedRealtime();
		}
		
		public final void close()
		{
			super.closeDirectly();
		}

		@Override
		protected void onClose(int flags)
		{
			switch(this.getMediaType())
			{
				case PHOTO:
					stopPhotoCapture(this);
					break;
				case VIDEO:
					stopVideoCapture(this);
					break;
			}
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
	
	
	// Class for camera lock handle.
	private final class CameraLockHandle extends Handle
	{
		public final LensFacing lensFacing;
		
		public CameraLockHandle(LensFacing lensFacing)
		{
			super("CameraLock");
			this.lensFacing = lensFacing;
		}

		@Override
		protected void onClose(int flags)
		{
			unlockCamera(this);
		}
	}
	
	
	// Static initializer.
	static
	{
		Settings.setGlobalDefaultValue(SETTINGS_KEY_CAMERA_LENS_FACING, LensFacing.BACK);
		Settings.setGlobalDefaultValue(SETTINGS_KEY_SELF_TIMER_INTERVAL_BACK, 0L);
		Settings.setGlobalDefaultValue(SETTINGS_KEY_SELF_TIMER_INTERVAL_FRONT, 0L);
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
	private boolean bindToInitialComponents()
	{
		// ResolutionManager
		if(this.getResolutionManager() == null)
		{
			Log.e(TAG, "bindToInitialComponents() - No ResolutionManager");
			return false;
		}
		
		// Viewfinder
		if(this.getViewfinder() == null)
		{
			Log.e(TAG, "bindToInitialComponents() - No Viewfinder");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Bind to related normal component.
	private boolean bindToNormalComponents()
	{
		// CountDownTimer
		if(m_CountDownTimer == null)
		{
			m_CountDownTimer = m_ComponentManager.findComponent(CountDownTimer.class, this);
			if(m_CountDownTimer != null)
			{
				m_CountDownTimer.addCallback(CountDownTimer.PROP_REMAINING_SECONDS, new PropertyChangedCallback<Long>()
				{
					@Override
					public void onPropertyChanged(PropertySource source, PropertyKey<Long> key, PropertyChangeEventArgs<Long> e)
					{
						onCountDownTimerChanged(e.getNewValue());
					}
				});
				m_CountDownTimer.addHandler(CountDownTimer.EVENT_CANCELLED, new EventHandler<EventArgs>()
				{
					@Override
					public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e)
					{
						onCountDownTimerCancelled();
					}
				});
				this.updateSelfTimerInternal();
			}
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
		if(this.get(PROP_ACTIVITY_ROTATION).isLandscape())
		{
			if(this.get(PROP_CONFIG_ORIENTATION) != Configuration.ORIENTATION_LANDSCAPE)
				return false;
		}
		else
		{
			if(this.get(PROP_CONFIG_ORIENTATION) != Configuration.ORIENTATION_PORTRAIT)
				return false;
		}
		
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
				if(Handle.isValid(m_SelfTimerHandle))
				{
					Log.w(TAG, "capturePhoto() - Counting-down self timer");
					return null;
				}
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
		if(!this.capturePhoto(handle, false))
		{
			Log.e(TAG, "capturePhoto() - Fail to capture");
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
			return null;
		}
		
		// complete
		return m_PhotoCaptureHandle;
	}
	
	
	// Start photo capture.
	private boolean capturePhoto(final CaptureHandleImpl handle, boolean fromSelfTimer)
	{
		Log.v(TAG, "capturePhoto() - Handle : ", handle, ", from self timer : ", fromSelfTimer);
		
		// check video snapshot
		if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO && !this.canVideoSnapshot())
		{
			Log.e(TAG, "capturePhoto() - Cannot take video snapshot");
			if(fromSelfTimer)
				this.resetPhotoCaptureState();
			return false;
		}
		
		// change state
		if(!fromSelfTimer)
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.STARTING);
		
		// start count-down
		if(!fromSelfTimer)
		{
			switch(this.get(PROP_MEDIA_TYPE))
			{
				case PHOTO:
				{
					long seconds = this.get(PROP_SELF_TIMER_INTERVAL);
					if(seconds > 0)
					{
						if(m_CountDownTimer != null)
						{
							Log.w(TAG, "capturePhoto() - Start self timer");
							m_SelfTimerHandle = m_CountDownTimer.start(seconds, 0);
							if(Handle.isValid(m_SelfTimerHandle))
							{
								this.setReadOnly(PROP_IS_SELF_TIMER_STARTED, true);
								return true;
							}
							Log.e(TAG, "capturePhoto() - Fail to start self timer");
						}
						else
							Log.w(TAG, "capturePhoto() - No CountDownTimer interface");
					}
					break;
				}
				case VIDEO:
					Log.w(TAG, "capturePhoto() - Video snapshot");
					break;
			}
		}
		
		// capture
		Log.w(TAG, "capturePhoto() - Capture");
		if(!HandlerUtils.post(m_CameraThread, new Runnable()
		{
			@Override
			public void run()
			{
				Log.w(TAG, "capturePhoto() - Capture in camera thread");
				CaptureHandle internalHandle = m_CameraThread.capturePhoto(handle.frameCount, 0);
				if(Handle.isValid(internalHandle))
					HandlerUtils.sendMessage(CameraActivity.this, MSG_PHOTO_CAPTURE_STARTED, 0, 0, new Object[]{ handle, internalHandle });
				else
					HandlerUtils.sendMessage(CameraActivity.this, MSG_PHOTO_CAPTURE_FAILED, 0, 0, handle);
			}
		}))
		{
			Log.e(TAG, "capturePhoto() - Fail to perform cross-thread operation");
			this.resetPhotoCaptureState();
			return false;
		}
		
		// save handle
		m_PhotoCaptureHandle = handle;
		
		// update states
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.CAPTURING);
		this.raise(EVENT_CAPTURE_STARTED, new CaptureEventArgs(handle));
		
		// complete
		return true;
	}
	
	
	/**
	 * Start video capture.
	 * @return Capture handle.
	 */
	public final CaptureHandle captureVideo()
	{
		return this.captureVideo(0);
	}
	
	
	/**
	 * Start video capture.
	 * @param flags Flags, reserved.
	 * @return Capture handle.
	 */
	public CaptureHandle captureVideo(int flags)
	{
		// check state
		this.verifyAccess();
		if(this.get(PROP_VIDEO_CAPTURE_STATE) != VideoCaptureState.READY)
		{
			Log.e(TAG, "captureVideo() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
			return null;
		}
		
		// create handle
		CaptureHandleImpl handle = new CaptureHandleImpl();
		
		// capture
		if(!this.captureVideo(handle))
			return null;
		return handle;
	}
	
	
	// Start capturing video.
	private boolean captureVideo(final CaptureHandleImpl handle)
	{
		Log.v(TAG, "captureVideo() - Handle : ", handle);
		
		// change state
		this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.STARTING);
		
		// capture
		Log.w(TAG, "captureVideo() - Capture");
		if(!HandlerUtils.post(m_CameraThread, new Runnable()
		{
			@Override
			public void run()
			{
				Log.w(TAG, "captureVideo() - Capture in camera thread");
				CaptureHandle internalHandle = m_CameraThread.captureVideo();
				if(Handle.isValid(internalHandle))
					HandlerUtils.sendMessage(CameraActivity.this, MSG_VIDEO_CAPTURE_STARTED, 0, 0, new Object[]{ handle, internalHandle });
				else
					HandlerUtils.sendMessage(CameraActivity.this, MSG_VIDEO_CAPTURE_FAILED, 0, 0, handle);
			}
		}))
		{
			Log.e(TAG, "captureVideo() - Fail to perform cross-thread operation");
			this.resetVideoCaptureState();
			return false;
		}
		
		// save handle
		m_VideoCaptureHandle = handle;
		
		// complete
		return true;
	}
	
	
	// Change preview state.
	private OperationState changeCameraPreviewState(OperationState state)
	{
		OperationState oldState = m_CameraPreviewState;
		if(oldState != state)
		{
			m_CameraPreviewState = state;
			this.notifyPropertyChanged(PROP_CAMERA_PREVIEW_STATE, oldState, state);
			return m_CameraPreviewState;
		}
		return oldState;
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
				this.completePhotoCapture(handleImpl);
				break;
			case VIDEO:
				this.completeVideoCapture(handleImpl);
				break;
		}
	}
	
	
	// complete capturing photo.
	private void completePhotoCapture(CaptureHandleImpl handle)
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
	
	
	// complete capturing video.
	private void completeVideoCapture(CaptureHandleImpl handle)
	{
		// check handle
		if(m_VideoCaptureHandle != handle)
		{
			Log.e(TAG, "completeVideoCapture() - Invalid handle");
			return;
		}
		
		// check state
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case CAPTURING:
			case PAUSED:
			case PAUSING:
			case RESUMING:
			case STOPPING:
				break;
			default:
				Log.e(TAG, "completeVideoCapture() - Current capture state : " + this.get(PROP_VIDEO_CAPTURE_STATE));
				return;
		}
		
		// change state
		this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.STOPPING);
		
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
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_ACTIVITY_ROTATION)
			return (TValue)m_ActivityRotation;
		if(key == PROP_CAMERA_PREVIEW_STATE)
			return (TValue)m_CameraPreviewState;
		if(key == PROP_ROTATION)
			return (TValue)m_Rotation;
		return super.get(key);
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
			case MSG_CAMERA_PREVIEW_START_FAILED:
				this.onCameraPreviewStartFailed((Camera)msg.obj);
				break;
				
			case MSG_CAMERA_PREVIEW_STARTED:
				this.onCameraPreviewStarted((Camera)msg.obj);
				break;
			
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
			
			case MSG_PHOTO_CAPTURE_FAILED:
				this.onPhotoCaptureFailed((CaptureHandleImpl)msg.obj);
				break;
				
			case MSG_PHOTO_CAPTURE_STARTED:
			{
				Object[] array = (Object[])msg.obj;
				this.onPhotoCaptureStarted((CaptureHandleImpl)array[0], (CaptureHandle)array[1]);
				break;
			}
			
			case MSG_VIDEO_CAPTURE_FAILED:
				this.onVideoCaptureFailed((CaptureHandleImpl)msg.obj);
				break;
				
			case MSG_VIDEO_CAPTURE_STARTED:
			{
				Object[] array = (Object[])msg.obj;
				this.onVideoCaptureStarted((CaptureHandleImpl)array[0], (CaptureHandle)array[1]);
				break;
			}
		}
	}
	
	
	/**
	 * Check whether current instance is launched in service mode or not.
	 * @return Launched in service mode or not.
	 */
	public boolean isServiceMode()
	{
		return false;
	}
	
	
	/**
	 * Lock camera with specific lens facing.
	 * @param lensFacing Camera lens facing.
	 * @return Handle to camera lock.
	 */
	public Handle lockCamera(LensFacing lensFacing)
	{
		// check state
		this.verifyAccess();
		if(this.get(PROP_STATE) == State.DESTROYED)
		{
			Log.e(TAG, "lockCamera() - Activity state is DESTROYED");
			return null;
		}
		
		// check parameter
		if(lensFacing == null)
		{
			Log.e(TAG, "lockCamera() - No lens facing specified");
			return null;
		}
		
		// check with current lock
		if(!m_CameraLockHandles.isEmpty() && m_CameraLockHandles.getLast().lensFacing != lensFacing)
		{
			Log.e(TAG, "lockCamera() - Camera is locked to " + m_CameraLockHandles.getLast().lensFacing);
			return null;
		}
		
		// lock camera
		CameraLockHandle handle = new CameraLockHandle(lensFacing);
		m_CameraLockHandles.addLast(handle);
		Log.w(TAG, "lockCamera() - Lens facing : " + lensFacing + ", handle : " + handle);
		if(m_CameraLockHandles.size() == 1)
		{
			if(!this.switchCamera(lensFacing))
			{
				Log.e(TAG, "lockCamera() - Fail to switch camera");
				m_CameraLockHandles.clear();
				return null;
			}
			this.setReadOnly(PROP_IS_CAMERA_LOCKED, true);
		}
		return handle;
	}
	
	
	/**
	 * Called when available cameras list changes.
	 * @param cameras Available cameras.
	 */
	protected void onAvailableCamerasChanged(List<Camera> cameras)
	{
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		boolean selectCamera = (camera == null);
		if(camera != null)
		{
			if(!cameras.contains(camera))
			{
				Log.w(TAG, "onAvailableCamerasChanged() - Camera " + camera + " is not contained in new list");
				selectCamera = true;
			}
		}
		
		// update property
		this.setReadOnly(PROP_AVAILABLE_CAMERAS, cameras);
		
		// check state
		if(!selectCamera)
			return;
		
		// select camera
		LensFacing lensFacing;
		if(m_CameraLockHandles.isEmpty())
			lensFacing = this.get(PROP_SETTINGS).getEnum(SETTINGS_KEY_CAMERA_LENS_FACING, LensFacing.class);
		else
			lensFacing = m_CameraLockHandles.getLast().lensFacing;
		camera = CameraUtils.findCamera(cameras, lensFacing, false);
		if(camera == null && m_CameraLockHandles.isEmpty())
		{
			Log.e(TAG, "onAvailableCamerasChanged() - No camera with lens facing " + lensFacing + ", select another camera");
			lensFacing = (lensFacing == LensFacing.BACK ? LensFacing.FRONT : LensFacing.BACK);
			camera = CameraUtils.findCamera(cameras, lensFacing, false);
		}
		if(camera != null)
		{
			Log.w(TAG, "onAvailableCamerasChanged() - Select " + camera);
			this.get(PROP_SETTINGS).set(SETTINGS_KEY_CAMERA_LENS_FACING, camera.get(Camera.PROP_LENS_FACING));
		}
		else
			Log.e(TAG, "onAvailableCamerasChanged() - No camera to use");
		this.setReadOnly(PROP_CAMERA, camera);
		
		// check activity state
		switch(this.get(PROP_STATE))
		{
			case CREATING:
			case RESUMING:
			case RUNNING:
				break;
			default:
				return;
		}
		
		// open camera
		if(camera != null && !this.getCameraThread().openCamera(camera))
			Log.e(TAG, "onAvailableCamerasChanged() - Fail to open camera " + camera);
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
		keys.add(CameraThread.EVENT_CAMERA_ERROR);
		keys.add(CameraThread.EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED);
		keys.add(CameraThread.EVENT_DEFAULT_VIDEO_CAPTURE_COMPLETED);
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
	 * Called when unexpected camera error occurred.
	 * @param camera Camera.
	 */
	protected void onCameraError(Camera camera)
	{
		Log.e(TAG, "onCameraError() - Camera : " + camera);
		
		// finish directly
		if(this.get(PROP_CAMERA) == camera)
			this.finish();
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
	 * Called when camera preview started.
	 */
	protected void onCameraPreviewStarted()
	{
		// change state
		if(this.changeCameraPreviewState(OperationState.STARTED) != OperationState.STARTED)
		{
			Log.e(TAG, "onCameraPreviewStarted() - Process interrupted");
			return;
		}
		
		Log.w(TAG, "onCameraPreviewStarted()");
		
		// change capture state
		if(this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.PREPARING)
			this.resetVideoCaptureState();
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.PREPARING)
			this.resetPhotoCaptureState();
	}
	
	
	// Called when camera preview started.
	private void onCameraPreviewStarted(Camera camera)
	{
		// check camera
		if(this.get(PROP_CAMERA) != camera)
			return;
		
		// check state
		if(m_CameraPreviewState != OperationState.STARTING)
		{
			Log.w(TAG, "onCameraPreviewStarted() - Preview state is " + m_CameraPreviewState);
			return;
		}
		
		// complete preview start process
		this.onCameraPreviewStarted();
	}
	
	
	/**
	 * Called when camera preview start failed.
	 */
	protected void onCameraPreviewStartFailed()
	{
		Log.e(TAG, "onCameraPreviewStartFailed()");
		
		this.changeCameraPreviewState(OperationState.STOPPED);
	}
	
	
	// Called when camera preview start failed.
	private void onCameraPreviewStartFailed(Camera camera)
	{
		// check camera
		if(this.get(PROP_CAMERA) != camera)
			return;
		
		// check state
		if(m_CameraPreviewState != OperationState.STARTING)
		{
			Log.w(TAG, "onCameraPreviewStartFailed() - Preview state is " + m_CameraPreviewState);
			return;
		}
		
		// complete preview start process
		this.onCameraPreviewStartFailed();
	}
	
	
	/**
	 * Called when receiving camera thread event in UI thread.
	 * @param key Event key.
	 * @param e Event data.
	 */
	protected void onCameraThreadEventReceived(EventKey<?> key, EventArgs e)
	{
		if(key == CameraThread.EVENT_CAMERA_ERROR)
			this.onCameraError(((CameraEventArgs)e).getCamera());
		else if(key == CameraThread.EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED)
			this.onDefaultPhotoCaptureCompleted((CaptureEventArgs)e);
		else if(key == CameraThread.EVENT_DEFAULT_VIDEO_CAPTURE_COMPLETED)
			this.onDefaultVideoCaptureCompleted((CaptureEventArgs)e);
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
						this.resetPhotoCaptureState();
						
						// start pending photo capture
						if(pendingHandle != null && (SystemClock.elapsedRealtime() - pendingHandle.creationTime) <= 800)
						{
							Log.w(TAG, "onCaptureCompleted() - Capture next photo immediately");
							if(this.capturePhoto(pendingHandle, false))
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
					if(this.startCameraPreview())
					{
						// reset capture state
						this.resetVideoCaptureState();
					}
					else
					{
						Log.e(TAG, "onCaptureCompleted() - Fail to start camera preview");
						this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
					}
					break;
				}
			}
		}
		else
		{
			Log.w(TAG, "onCaptureCompleted() -Activity state is " + this.get(PROP_STATE));
			
			// reset capture state
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
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
		if(!this.bindToInitialComponents())
			this.finish();
	}
	
	
	// Called when count-down timer cancelled.
	private void onCountDownTimerCancelled()
	{
		// check state
		if(!Handle.isValid(m_SelfTimerHandle))
			return;
		
		// clear handle
		m_SelfTimerHandle = null;
		this.setReadOnly(PROP_IS_SELF_TIMER_STARTED, false);
		
		// reset capture state
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.STARTING)
			this.resetPhotoCaptureState();
		else
			Log.w(TAG, "onCountDownTimerCancelled() - Photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
	}
	
	
	// Called when count-down timer changed.
	private void onCountDownTimerChanged(long seconds)
	{
		// check state
		if(!Handle.isValid(m_SelfTimerHandle))
			return;
		
		Log.v(TAG, "onCountDownTimerChanged() - Remaining seconds : ", seconds);
		
		// capture photo
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.STARTING)
		{
			if(seconds == 0)
			{
				m_SelfTimerHandle = null;
				this.setReadOnly(PROP_IS_SELF_TIMER_STARTED, false);
				if(Handle.isValid(m_PhotoCaptureHandle))
				{
					Log.v(TAG, "onCountDownTimerChanged() - Capture photo");
					this.capturePhoto(m_PhotoCaptureHandle, true);
				}
				else
				{
					Log.e(TAG, "onCountDownTimerChanged() - No capture handle");
					this.resetPhotoCaptureState();
				}
			}
		}
		else
		{
			Log.e(TAG, "onCountDownTimerChanged() - Photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
			m_SelfTimerHandle = Handle.close(m_SelfTimerHandle);
			this.setReadOnly(PROP_IS_SELF_TIMER_STARTED, false);
		}
	}
	
	
	// Called when creating activity.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{	
		// call super
		super.onCreate(savedInstanceState);
		
		// enable logs
		this.enablePropertyLogs(PROP_CAMERA_PREVIEW_SIZE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_CAMERA_PREVIEW_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PHOTO_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_ROTATION, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_SETTINGS, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_VIDEO_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		
		// check activity rotation
		this.onRequestedOrientationChanged(this.getRequestedOrientation());
		
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
	
	
	// Called when default video capture process completed by camera thread.
	private void onDefaultVideoCaptureCompleted(CaptureEventArgs e)
	{
		// check handle
		if(m_VideoCaptureHandle == null || m_VideoCaptureHandle.internalCaptureHandle != e.getCaptureHandle())
		{
			Log.w(TAG, "onDefaultVideoCaptureCompleted() - Unknown capture handle : " + e.getCaptureHandle());
			Log.w(TAG, "onDefaultVideoCaptureCompleted() - Expected capture handle : " + (m_VideoCaptureHandle != null ? m_VideoCaptureHandle.internalCaptureHandle : null));
			return;
		}
		
		// complete capture
		this.completeCapture(m_VideoCaptureHandle);
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
	
	
	/**
	 * Called when device orientation changes.
	 * @param orientation Device orientation [0-359], or {@link OrientationEventListener#ORIENTATION_UNKNOWN}.
	 */
	protected void onDeviceOrientationChanged(int orientation)
	{
		// check orientation
		if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
		{
			Log.w(TAG, "onDeviceOrientationChanged() - Unknown orientation");
			return;
		}
		this.setReadOnly(PROP_DEVICE_ORIENTATION, orientation);
		
		// check difference with current rotation
		int diff = (orientation - m_Rotation.getDeviceOrientation());
		if(diff > 180)
			diff = (360 - diff);
		else if(diff < -180)
			diff = (360 + diff);
		if(Math.abs(diff) <= 60)
			return;
		
		// convert to rotation
		Rotation prevRotation = m_Rotation;
		m_Rotation = Rotation.fromDeviceOrientation(orientation);
		if(prevRotation == m_Rotation)
			return;
		
		// handle rotation change
		this.onRotationChanged(prevRotation, m_Rotation);
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
		
		// stop orientation listener
		this.stopOrientationListener();
	}
	
	
	// Called when photo capture failed.
	private void onPhotoCaptureFailed(CaptureHandleImpl handle)
	{
		// check handle
		if(m_PhotoCaptureHandle != handle)
		{
			Log.w(TAG, "onPhotoCaptureFailed() - Unknown handle : " + handle + ", expected handle : " + m_PhotoCaptureHandle);
			return;
		}
		
		Log.e(TAG, "onPhotoCaptureFailed() - Handle : " + handle);
		
		// handle fail case
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case CAPTURING:
				this.raise(EVENT_CAPTURE_FAILED, new CaptureEventArgs(handle));
				this.stopPhotoCapture(handle);
				this.completeCapture(handle);
				break;
			case STOPPING:
				this.raise(EVENT_CAPTURE_FAILED, new CaptureEventArgs(handle));
				this.completeCapture(handle);
				break;
			default:
				Log.w(TAG, "onPhotoCaptureFailed() - Photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return;
		}
	}
	
	
	// Called when photo capture started.
	private void onPhotoCaptureStarted(CaptureHandleImpl handle, CaptureHandle internalHandle)
	{
		// check handle
		if(m_PhotoCaptureHandle != handle)
		{
			Log.e(TAG, "onPhotoCaptureStarted() - Unknown handle : " + handle + ", expected handle : " + m_PhotoCaptureHandle);
			return;
		}
		
		Log.v(TAG, "onPhotoCaptureStarted() - Handle : ", handle);
		
		// check state
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case CAPTURING:
				handle.internalCaptureHandle = internalHandle;
				break;
			case STOPPING:
				Log.w(TAG, "onPhotoCaptureStarted() - Stop capture immediately");
				Handle.close(internalHandle);
				break;
			default:
				Log.e(TAG, "onPhotoCaptureStarted() - Photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return;
		}
	}
	
	
	// Called when activity orientation changed.
	private void onRequestedOrientationChanged(int orientation)
	{
		Rotation oldRotation = m_ActivityRotation;
		Rotation newRotation;
		switch(this.getRequestedOrientation())
		{
			case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
				newRotation = Rotation.LANDSCAPE;
				break;
			case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
				newRotation = Rotation.PORTRAIT;
				break;
			case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
				newRotation = Rotation.INVERSE_LANDSCAPE;
				break;
			case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
				newRotation = Rotation.INVERSE_PORTRAIT;
				break;
			default:
			{
				Log.e(TAG, "onRequestedOrientationChanged() - Unsupported orientation : " + orientation);
				newRotation = Rotation.LANDSCAPE;
				break;
			}
		}
		if(oldRotation != newRotation)
		{
			m_ActivityRotation = newRotation;
			this.notifyPropertyChanged(PROP_ACTIVITY_ROTATION, oldRotation, newRotation);
		}
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
		
		// start orientation listener
		this.startOrientationListener();
	}
	
	
	/**
	 * Called when UI rotation changes.
	 * @param prevRotation Previous rotation.
	 * @param newRotation New rotation.
	 */
	protected void onRotationChanged(final Rotation prevRotation, final Rotation newRotation)
	{
		// notify camera thread
		HandlerUtils.post(m_CameraThread, new Runnable()
		{
			@Override
			public void run()
			{
				m_CameraThread.set(CameraThread.PROP_CAPTURE_ROTATION, newRotation);
			}
		});
		
		// notify property change
		this.notifyPropertyChanged(PROP_ROTATION, prevRotation, newRotation);
	}
	
	
	// Handle touch event
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// raise event
		MotionEventArgs e = MotionEventArgs.obtain(event);
		this.raise(EVENT_TOUCH, e);
		if(e.isHandled())
		{
			e.recycle();
			return true;
		}
		e.recycle();
		
		// call super
		return super.onTouchEvent(event);
	}
	
	
	// Called when video capture failed.
	private void onVideoCaptureFailed(CaptureHandleImpl handle)
	{
		// check handle
		if(m_VideoCaptureHandle != handle)
		{
			Log.w(TAG, "onVideoCaptureFailed() - Unknown handle : " + handle + ", expected handle : " + m_PhotoCaptureHandle);
			return;
		}
		
		Log.e(TAG, "onVideoCaptureFailed() - Handle : " + handle);
		
		// handle fail case
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case STARTING:
			case CAPTURING:
				this.raise(EVENT_CAPTURE_FAILED, new CaptureEventArgs(handle));
				this.stopVideoCapture(handle);
				this.completeCapture(handle);
				break;
			case STOPPING:
				this.raise(EVENT_CAPTURE_FAILED, new CaptureEventArgs(handle));
				this.completeCapture(handle);
				break;
			default:
				Log.w(TAG, "onVideoCaptureFailed() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
				return;
		}
	}
	
	
	// Called when video capture started.
	private void onVideoCaptureStarted(CaptureHandleImpl handle, CaptureHandle internalHandle)
	{
		// check handle
		if(m_VideoCaptureHandle != handle)
		{
			Log.e(TAG, "onVideoCaptureStarted() - Unknown handle : " + handle + ", expected handle : " + m_PhotoCaptureHandle);
			return;
		}
		
		Log.v(TAG, "onVideoCaptureStarted() - Handle : ", handle);
		
		// check state
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case STARTING:
				handle.internalCaptureHandle = internalHandle;
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.CAPTURING);
				this.raise(EVENT_CAPTURE_STARTED, new CaptureEventArgs(handle));
				break;
			case STOPPING:
				Log.w(TAG, "onVideoCaptureStarted() - Stop capture immediately");
				Handle.close(internalHandle);
				break;
			default:
				Log.e(TAG, "onVideoCaptureStarted() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
				return;
		}
	}
	
	
	// Release and remove given component.
	@Override
	public void removeComponent(Component component)
	{
		m_ComponentManager.removeComponent(component);
	}
	
	
	// Reset photo capture state according to current states.
	private void resetPhotoCaptureState()
	{
		switch(this.get(PROP_MEDIA_TYPE))
		{
			case PHOTO:
				if(m_CameraPreviewState == OperationState.STARTED)
					this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
				else
					this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
				break;
			case VIDEO:
				switch(this.get(PROP_VIDEO_CAPTURE_STATE))
				{
					case CAPTURING:
					case PAUSED:
						this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
						break;
					default:
						this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
						break;
				}
				break;
		}
	}
	
	
	// Reset video capture state according to current states.
	private void resetVideoCaptureState()
	{
		if(m_CameraPreviewState == OperationState.STARTED && this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO)
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
		else
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
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
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_SELF_TIMER_INTERVAL)
			return this.setSelfTimerIntervalProp((Long)value);
		return super.set(key, value);
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
		
		// update self timer
		this.updateSelfTimerInternal();
		
		// complete
		return true;
	}
	
	
	// Set read-only property value.
	@Override
	protected <TValue> boolean setReadOnly(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_ACTIVITY_ROTATION)
			throw new IllegalAccessError("Cannot change activity rotation.");
		if(key == PROP_CAMERA_PREVIEW_STATE)
			throw new IllegalAccessError("Cannot change camera preview state.");
		if(key == PROP_ROTATION)
			throw new IllegalAccessError("Cannot change UI rotation.");
		return super.setReadOnly(key, value);
	}
	
	
	// Set activity orientation.
	@Override
	public void setRequestedOrientation(int requestedOrientation)
	{
		super.setRequestedOrientation(requestedOrientation);
		this.onRequestedOrientationChanged(requestedOrientation);
	}
	
	
	// Set self timer interval property.
	private boolean setSelfTimerIntervalProp(long seconds)
	{
		// check value
		if(seconds < 0)
			throw new IllegalArgumentException("Self timer interval cannot be negative.");
		
		// check camera
		/*
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "setSelfTimerIntervalProp() - No primary camera");
			super.set(PROP_SELF_TIMER_INTERVAL, 0L);
			return false;
		}
		*/
		
		// save interval to settings
		this.get(PROP_SETTINGS).set(SETTINGS_KEY_SELF_TIMER_INTERVAL_BACK, seconds);
		this.get(PROP_SETTINGS).set(SETTINGS_KEY_SELF_TIMER_INTERVAL_FRONT, seconds);
		
		// update property
		return super.set(PROP_SELF_TIMER_INTERVAL, seconds);
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
	
	
	/**
	 * Start current primary camera preview.
	 * @return Whether preview starts successfully or not.
	 */
	public final boolean startCameraPreview()
	{
		// check state
		this.verifyAccess();
		switch(m_CameraPreviewState)
		{
			case STOPPED:
				break;
			case STOPPING:
				Log.w(TAG, "startCameraPreview() - Start while stopping");
				break;
			case STARTING:
			case STARTED:
				return true;
		}
		if(!this.canStartCameraPreview())
		{
			Log.w(TAG, "startCameraPreview() - Cannot start preview in current state");
			return false;
		}
		
		// check camera
		final Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.w(TAG, "startCameraPreview() - No camera to start preview");
			return false;
		}
		
		// change state
		if(this.changeCameraPreviewState(OperationState.STARTING) != OperationState.STARTING)
		{
			Log.e(TAG, "startCameraPreview() - Process interrupted");
			return false;
		}
		
		// start preview
		final Size previewSize = this.get(PROP_CAMERA_PREVIEW_SIZE);
		final Object previewReceiver = m_Viewfinder.get(Viewfinder.PROP_PREVIEW_RECEIVER);
		if(!HandlerUtils.post(m_CameraThread, new Runnable()
		{
			@Override
			public void run()
			{
				switch(camera.get(Camera.PROP_PREVIEW_STATE))
				{
					case STARTING:
						break;
					case STARTED:
						HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_PREVIEW_STARTED, 0, 0, camera);
						break;
					default:
						if(m_CameraThread.startCameraPreview(camera, previewSize, previewReceiver))
						{
							switch(camera.get(Camera.PROP_PREVIEW_STATE))
							{
								case STARTED:
									HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_PREVIEW_STARTED, 0, 0, camera);
									break;
								case STARTING:
									Log.v(TAG, "startCameraPreview() - Wait for camera preview start");
									camera.addCallback(Camera.PROP_PREVIEW_STATE, new PropertyChangedCallback<OperationState>()
									{
										@Override
										public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
										{
											if(e.getNewValue() == OperationState.STARTED)
												HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_PREVIEW_STARTED, 0, 0, camera);
											else
											{
												Log.e(TAG, "startCameraPreview() - Fail to start camera preview");
												HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_PREVIEW_START_FAILED, 0, 0, camera);
											}
											camera.removeCallback(Camera.PROP_PREVIEW_STATE, this);
										}
									});
									break;
								default:
									Log.e(TAG, "startCameraPreview() - Fail to start camera preview");
									HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_PREVIEW_START_FAILED, 0, 0, camera);
									break;
							}
						}
						else
						{
							Log.e(TAG, "startCameraPreview() - Fail to start camera preview");
							HandlerUtils.sendMessage(CameraActivity.this, MSG_CAMERA_PREVIEW_START_FAILED, 0, 0, camera);
						}
						break;
				}
			}
		}))
		{
			Log.e(TAG, "startCameraPreview() - Fail to perform cross-thread operation");
			if(m_CameraPreviewState == OperationState.STARTING)
				this.changeCameraPreviewState(OperationState.STOPPED);
			return false;
		}
		
		// change state and create components with NORMAL priority
		if(this.setReadOnly(PROP_IS_LAUNCHING, false))
		{
			m_ComponentManager.createComponents(ComponentCreationPriority.NORMAL, this);
			this.bindToNormalComponents();
		}
		
		// complete
		return true;
	}
	
	
	// Start orientation listener.
	private void startOrientationListener()
	{
		// check state
		if(m_IsOrientationListenerStarted)
			return;
		switch(this.get(PROP_STATE))
		{
			case RESUMING:
			case RUNNING:
				break;
			default:
				return;
		}
		
		// create listener
		if(m_OrientationListener == null)
		{
			m_OrientationListener = new OrientationEventListener(this)
			{
				@Override
				public void onOrientationChanged(int orientation)
				{
					onDeviceOrientationChanged(orientation);
				}
			};
		}
		
		// start listener
		Log.v(TAG, "startOrientationListener()");
		m_OrientationListener.enable();
		m_IsOrientationListenerStarted = true;
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
		// check state
		this.verifyAccess();
		switch(m_CameraPreviewState)
		{
			case STARTED:
				break;
			case STARTING:
				Log.w(TAG, "stopCameraPreview() - Stop while starting");
				break;
			case STOPPING:
				if(!sync)
					return;
				break;
			case STOPPED:
				return;
		}
		
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			this.changeCameraPreviewState(OperationState.STOPPED);
			return;
		}
		
		// change capture state
		if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.READY)
			this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
		if(this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.READY)
			this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
		
		// change state
		if(this.changeCameraPreviewState(OperationState.STOPPING) != OperationState.STOPPING)
		{
			Log.w(TAG, "stopCameraPreview() - Process interrupted");
			return;
		}
		
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
		
		// change state
		if(m_CameraPreviewState == OperationState.STOPPING)
			this.changeCameraPreviewState(OperationState.STOPPED);
	}
	
	
	// Stop device orientation listener.
	private void stopOrientationListener()
	{
		if(!m_IsOrientationListenerStarted || m_OrientationListener == null)
			return;
		Log.v(TAG, "stopOrientationListener()");
		m_OrientationListener.disable();
		m_IsOrientationListenerStarted = false;
	}
	
	
	// Stop capturing photo.
	private void stopPhotoCapture(CaptureHandleImpl handle)
	{
		// check state
		this.verifyAccess();
		if(m_PhotoCaptureHandle != handle)
		{
			Log.w(TAG, "stopPhotoCapture() - Invalid handle");
			return;
		}
		
		Log.w(TAG, "stopPhotoCapture() - Handle : " + handle);
		
		// close handle
		handle.close();
		
		// cancel pending capture
		if(m_PendingPhotoCaptureHandle == handle)
		{
			Log.w(TAG, "stopPhotoCapture() - Cancel pending capture");
			m_PendingPhotoCaptureHandle = null;
			return;
		}
		
		// stop self-timer
		if(Handle.isValid(m_SelfTimerHandle))
		{
			Log.w(TAG, "stopPhotoCapture() - Stop self timer");
			this.setReadOnly(PROP_IS_SELF_TIMER_STARTED, false);
			m_SelfTimerHandle = Handle.close(m_SelfTimerHandle);
			this.onCaptureCompleted(handle);
			return;
		}
		
		// change capture state
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case CAPTURING:
			case STOPPING:
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.STOPPING);
				break;
			default:
				Log.e(TAG, "stopPhotoCapture() - Photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return;
		}
		
		// stop capture
		if(Handle.isValid(handle.internalCaptureHandle))
			Handle.close(handle.internalCaptureHandle);
		else
			Log.w(TAG, "stopPhotoCapture() - Stop when starting");
	}
	
	
	// Stop capturing video.
	private void stopVideoCapture(CaptureHandleImpl handle)
	{
		// check state
		this.verifyAccess();
		if(m_VideoCaptureHandle != handle)
		{
			Log.w(TAG, "stopVideoCapture() - Invalid handle");
			return;
		}
		
		Log.w(TAG, "stopVideoCapture() - Handle : " + handle);
		
		// close handle
		handle.close();
		
		// change capture state
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case STARTING:
			case CAPTURING:
			case PAUSING:
			case PAUSED:
			case RESUMING:
			case STOPPING:
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.STOPPING);
				break;
			default:
				Log.e(TAG, "stopVideoCapture() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
				return;
		}
		
		// stop capture
		if(Handle.isValid(handle.internalCaptureHandle))
			Handle.close(handle.internalCaptureHandle);
		else
			Log.w(TAG, "stopPhotoCapture() - Stop when starting");
	}
	
	
	/**
	 * Switch to another camera.
	 * @return Whether camera switched successfully or not.
	 */
	public boolean switchCamera()
	{
		// check state
		this.verifyAccess();
		
		// get current camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "switchCamera() - No primary camera");
			return false;
		}
		
		// check camera lock
		if(!m_CameraLockHandles.isEmpty() && m_CameraLockHandles.getLast().lensFacing == camera.get(Camera.PROP_LENS_FACING))
		{
			Log.e(TAG, "switchCamera() - Camera is locked to " + m_CameraLockHandles.getLast().lensFacing);
			return false;
		}
		
		// select another camera
		LensFacing lensFacing = (camera.get(Camera.PROP_LENS_FACING) == LensFacing.BACK ? LensFacing.FRONT : LensFacing.BACK);
		Camera newCamera = CameraUtils.findCamera(this.get(PROP_AVAILABLE_CAMERAS), lensFacing, false);
		if(newCamera == null)
		{
			Log.e(TAG, "switchCamera() - No camera to switch");
			return false;
		}
		Log.w(TAG, "switchCamera() - Select " + newCamera);
		
		// switch
		return this.switchCamera(newCamera);
	}
	
	
	/**
	 * Switch to camera with specific lens facing.
	 * @param lensFacing Camera lens facing.
	 * @return Whether camera switched successfully or not.
	 */
	public boolean switchCamera(LensFacing lensFacing)
	{
		// check state
		this.verifyAccess();
		
		// get current camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
			Log.w(TAG, "switchCamera() - No primary camera");
		
		// check camera lock
		if(!m_CameraLockHandles.isEmpty() && m_CameraLockHandles.getLast().lensFacing != lensFacing)
		{
			Log.e(TAG, "switchCamera() - Camera is locked to " + m_CameraLockHandles.getLast().lensFacing);
			return false;
		}
		
		// select another camera
		List<Camera> cameras = this.get(PROP_AVAILABLE_CAMERAS);
		if(cameras.isEmpty())
		{
			Log.w(TAG, "switchCamera() - Camera list is not ready yet, switch camera later");
			this.get(PROP_SETTINGS).set(SETTINGS_KEY_CAMERA_LENS_FACING, lensFacing);
			return true;
		}
		Camera newCamera = CameraUtils.findCamera(cameras, lensFacing, false);
		if(newCamera == null)
		{
			Log.e(TAG, "switchCamera() - No camera with lens facing " + lensFacing);
			return false;
		}
		if(camera == newCamera)
			return true;
		
		// switch
		return this.switchCamera(newCamera);
	}
	
	
	// Switch to given camera
	private boolean switchCamera(Camera newCamera)
	{
		// check state
		switch(this.get(PROP_PHOTO_CAPTURE_STATE))
		{
			case PREPARING:
			case READY:
				break;
			default:
				Log.e(TAG, "switchCamera() - Photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
				return false;
		}
		switch(this.get(PROP_VIDEO_CAPTURE_STATE))
		{
			case PREPARING:
			case READY:
				break;
			default:
				Log.e(TAG, "switchCamera() - Video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
				return false;
		}
		
		// stop preview
		boolean restartPreview;
		switch(m_CameraPreviewState)
		{
			case STARTING:
			case STARTED:
				restartPreview = true;
				break;
			default:
				restartPreview = false;
				break;
		}
		this.stopCameraPreview(true);
		if(m_CameraPreviewState != OperationState.STOPPED)
		{
			Log.e(TAG, "switchCamera() - Preview state is " + m_CameraPreviewState);
			return false;
		}
		
		// close camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera != null)
			m_CameraThread.closeCamera(camera);
		
		// open camera
		boolean success = m_CameraThread.openCamera(newCamera);
		if(success)
		{
			this.setReadOnly(PROP_CAMERA, newCamera);
			this.get(PROP_SETTINGS).set(SETTINGS_KEY_CAMERA_LENS_FACING, newCamera.get(Camera.PROP_LENS_FACING));
		}
		else
			Log.e(TAG, "switchCamera() - Fail to open camera by camera thread");
		
		// open camera or start preview
		if(restartPreview && !this.startCameraPreview())
			Log.e(TAG, "switchCamera() - Fail to restart preview");
		
		// complete
		return success;
	}
	
	
	// Unlock camera.
	private void unlockCamera(CameraLockHandle handle)
	{
		// check thread
		this.verifyAccess();
		
		// unlock
		if(m_CameraLockHandles.remove(handle) && m_CameraLockHandles.isEmpty())
		{
			Log.w(TAG, "unlockCamera()");
			this.setReadOnly(PROP_IS_CAMERA_LOCKED, false);
		}
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
	
	
	// Update self timer interval according to current state
	private void updateSelfTimerInternal()
	{
		// check count-down timer
		if(m_CountDownTimer == null)
			return;
		
		// check media type
		if(this.get(PROP_MEDIA_TYPE) != MediaType.PHOTO)
		{
			super.set(PROP_SELF_TIMER_INTERVAL, 0L);
			return;
		}
		
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "updateSelfTimerInternal() - No primary camera");
			super.set(PROP_SELF_TIMER_INTERVAL, 0L);
			return;
		}
		
		// get interval from settings
		String key = (camera.get(Camera.PROP_LENS_FACING) == LensFacing.BACK ? SETTINGS_KEY_SELF_TIMER_INTERVAL_BACK : SETTINGS_KEY_SELF_TIMER_INTERVAL_FRONT);
		long seconds = Math.max(0, this.get(PROP_SETTINGS).getLong(key));
		super.set(PROP_SELF_TIMER_INTERVAL, seconds);
	}
}
