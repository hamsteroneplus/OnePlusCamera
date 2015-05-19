package com.oneplus.camera;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;
import com.oneplus.renderscript.RenderScriptManager;
import com.oneplus.util.AspectRatio;
import com.oneplus.util.ListUtils;

class CameraImpl extends HandlerBaseObject implements Camera
{
	// Constants
	private static final long TIMEOUT_AF_COMPLETE = 5000;
	private static final long TIMEOUT_CAPTURE_SESSION_CLOSED = 5000;
	private static final int MSG_PREVIEW_FRAME_RECEIVED = 10000;
	private static final int MSG_START_AF = 10010;
	private static final int MSG_AF_TIMEOUT = 10011;
	private static final int MSG_CAPTURE_SESSION_CLOSE_TIMEOUT = 10020;
	
	
	// Private fields
	@SuppressWarnings("unchecked")
	private List<MeteringRect> m_AeRegions = Collections.EMPTY_LIST;
	@SuppressWarnings("unchecked")
	private List<MeteringRect> m_AfRegions = Collections.EMPTY_LIST;
	private Handle m_CaptureHandle;
	private CameraCaptureSession m_CaptureSession;
	private final CameraCaptureSession.StateCallback m_CaptureSessionCallback = new CameraCaptureSession.StateCallback()
	{
		@Override
		public void onConfigured(CameraCaptureSession session)
		{
			onCaptureSessionConfigured(session);
		}
		
		@Override
		public void onConfigureFailed(CameraCaptureSession session)
		{
			onCaptureSessionConfigureFailed(session);
		}
		
		public void onClosed(CameraCaptureSession session) 
		{
			onCaptureSessionClosed(session);
		}
	};
	private OperationState m_CaptureSessionState = OperationState.STOPPED;
	private final CameraCharacteristics m_Characteristics;
	private final CameraManager m_CameraManager;
	private Context m_Context;
	private CameraDevice m_Device;
	private final CameraDevice.StateCallback m_DeviceStateCallback = new CameraDevice.StateCallback()
	{
		@Override
		public void onOpened(CameraDevice camera)
		{
			onDeviceOpened(camera);
		}
		
		@Override
		public void onError(CameraDevice camera, int error)
		{
			onDeviceError(camera, error, false);
		}
		
		@Override
		public void onDisconnected(CameraDevice camera)
		{
			onDeviceError(camera, 0, true);
		}
	};
	private FlashMode m_FlashMode = FlashMode.OFF;
	private FocusMode m_FocusMode = FocusMode.DISABLED;
	private final String m_Id;
	private boolean m_IsAutoFocusTimeout;
	private boolean m_IsCaptureSequenceCompleted;
	private volatile boolean m_IsPreviewReceived;
	private boolean m_IsRecordingMode;
	private final LensFacing m_LensFacing;
	private final ImageReader.OnImageAvailableListener m_PictureAvailableListener = new ImageReader.OnImageAvailableListener()
	{
		@Override
		public void onImageAvailable(ImageReader reader)
		{
			Image image;
			try
			{
				image = reader.acquireLatestImage();
			}
			catch(Throwable ex)
			{
				image = null;
			}
			try
			{
				onPictureReceived(image);
			}
			finally
			{
				if(image != null)
					image.close();
			}
		}
	};
	private final CameraCaptureSession.CaptureCallback m_PictureCaptureCallback = new CameraCaptureSession.CaptureCallback()
	{
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
		{
			CameraImpl.this.onCaptureCompleted(session, request, result, null);
		}
		
		public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) 
		{
			CameraImpl.this.onCaptureCompleted(session, request, null, failure);
		}
		
		public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) 
		{
			CameraImpl.this.onCaptureStarted(session, request, timestamp, frameNumber);
		}
		
		public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult)
		{
			Log.w(TAG, "onCaptureProgressed");
		}
		
		public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) 
		{
			CameraImpl.this.onCaptureSequenceCompleted();
		}
	};
	private CaptureRequest m_PictureCaptureRequest;
	private ImageReader m_PictureReader;
	private Size m_PictureSize = new Size(0, 0);
	private Surface m_PictureSurface;
	private Allocation m_PreviewCallbackAllocation;
	private final Allocation.OnBufferAvailableListener m_PreviewCallbackAllocationCallback = new Allocation.OnBufferAvailableListener()
	{
		@Override
		public void onBufferAvailable(Allocation a)
		{
			HandlerUtils.sendMessage(CameraImpl.this, MSG_PREVIEW_FRAME_RECEIVED);
		}
	};
	private byte[] m_PreviewCallbackData;
	private Surface m_PreviewCallbackSurface;
	private final CameraCaptureSession.CaptureCallback m_PreviewCaptureCallback = new CameraCaptureSession.CaptureCallback()
	{
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) 
		{
			onPreviewCaptureCompleted(result);
		}
	};
	private CaptureRequest.Builder m_PreviewRequestBuilder;
	private Size m_PreviewSize = new Size(0, 0);
	private Surface m_PreviewSurface;
	private int m_ReceivedCaptureCompletedCount;
	private final Queue<CaptureResult> m_ReceivedCaptureCompletedResults = new LinkedList<>();
	private int m_ReceivedCaptureStartedCount;
	private final Queue<CaptureResult> m_ReceivedCaptureStartedResults = new LinkedList<>();
	private int m_ReceivedPictureCount;
	private final Queue<byte[]> m_ReceivedPictures = new LinkedList<>();
	private RenderScript m_RenderScript;
	private Handle m_RenderScriptHandle;
	private int m_SceneMode = CaptureRequest.CONTROL_SCENE_MODE_DISABLED;
	private List<Integer> m_SceneModes;
	private final int m_SensorOrientation;
	private final Size m_SensorSize;
	private volatile State m_State = State.CLOSED;
	private int m_TargetCapturedFrameCount;
	@SuppressWarnings("rawtypes")
	private final List m_TempList = new ArrayList();
	private final List<Surface> m_TempSurfaces = new ArrayList<>();
	private Surface m_VideoSurface;
	
	
	// Constructor
	public CameraImpl(Context context, CameraManager cameraManager, String id, CameraCharacteristics cameraChar)
	{
		// call super
		super(true);
		
		// save info
		m_Context = context;
		m_CameraManager = cameraManager;
		m_Characteristics = cameraChar;
		m_Id = id;
		
		// get facing
		switch(cameraChar.get(CameraCharacteristics.LENS_FACING))
		{
			case CameraCharacteristics.LENS_FACING_BACK:
				m_LensFacing = LensFacing.BACK;
				break;
			case CameraCharacteristics.LENS_FACING_FRONT:
				m_LensFacing = LensFacing.FRONT;
				break;
			default:
				throw new RuntimeException("Unknown lens facing : " + cameraChar.get(CameraCharacteristics.LENS_FACING));
		}
		
		// check capabilities
		boolean isManualSupported = false;
		int[] capabilities = cameraChar.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
		for(int i = capabilities.length - 1 ; i >= 0 ; --i)
		{
			switch(capabilities[i])
			{
				case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR:
					isManualSupported = true;
					this.setReadOnly(PROP_IS_MANUAL_CONTROL_SUPPORTED, true);
					break;
				case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW:
					this.setReadOnly(PROP_IS_RAW_CAPTURE_SUPPORTED, true);
					break;
			}
		}
		this.setReadOnly(PROP_IS_BURST_CAPTURE_SUPPORTED, true);
		
		// get sensor size
		Rect sensorRect = cameraChar.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		m_SensorSize = new Size(sensorRect.width(), sensorRect.height());
		
		// get preview sizes
		StreamConfigurationMap streamConfigMap = cameraChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		this.setReadOnly(PROP_PREVIEW_SIZES, Arrays.asList(streamConfigMap.getOutputSizes(SurfaceHolder.class)));
		
		// get picture sizes
		List<Size> pictureSizes = Arrays.asList(streamConfigMap.getOutputSizes(ImageFormat.JPEG));
		this.setReadOnly(PROP_PICTURE_SIZES, pictureSizes);
		if(!pictureSizes.isEmpty())
			m_PictureSize = pictureSizes.get(0);
		
		// get video sizes
		this.setReadOnly(PROP_VIDEO_SIZES, Arrays.asList(streamConfigMap.getOutputSizes(MediaRecorder.class)));
		
		// get sensor orientation
		m_SensorOrientation = cameraChar.get(CameraCharacteristics.SENSOR_ORIENTATION);
		
		// check flash
		this.setReadOnly(PROP_HAS_FLASH, cameraChar.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
		
		// check AE region count
		this.setReadOnly(PROP_MAX_AE_REGION_COUNT, cameraChar.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));
		
		// check AF region count
		this.setReadOnly(PROP_MAX_AF_REGION_COUNT, cameraChar.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF));
		
		// check focus modes
		int[] afModes = cameraChar.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
		List<FocusMode> focusModes = new ArrayList<>();
		for(int i = afModes.length - 1 ; i >= 0 ; --i)
		{
			switch(afModes[i])
			{
				case CameraCharacteristics.CONTROL_AF_MODE_AUTO:
					focusModes.add(FocusMode.NORMAL_AF);
					break;
				case CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
				case CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
					if(!focusModes.contains(FocusMode.CONTINUOUS_AF))
						focusModes.add(FocusMode.CONTINUOUS_AF);
					m_FocusMode = FocusMode.CONTINUOUS_AF;
					break;
				case CameraCharacteristics.CONTROL_AF_MODE_OFF:
					if(isManualSupported)
						focusModes.add(FocusMode.MANUAL);
					break;
			}
		}
		focusModes.add(FocusMode.DISABLED);
		this.setReadOnly(PROP_FOCUS_MODES, Collections.unmodifiableList(focusModes));
		if(m_FocusMode == FocusMode.DISABLED && focusModes.contains(FocusMode.NORMAL_AF))
			m_FocusMode = FocusMode.NORMAL_AF;
		
		// check scene modes
		m_SceneModes = ListUtils.asList(cameraChar.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES));
		this.setReadOnly(PROP_SCENE_MODES, m_SceneModes);
		
		// enable logs
		this.enablePropertyLogs(PROP_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_FOCUS_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PREVIEW_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_STATE, LOG_PROPERTY_CHANGE);
	}
	
	
	// Add event handler.
	@SuppressWarnings("unchecked")
	@Override
	public <TArgs extends EventArgs> void addHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		if(key == EVENT_PREVIEW_RECEIVED)
			this.addPreviewReceivedHandler((EventHandler<CameraCaptureEventArgs>)handler);
		else
			super.addHandler(key, handler);
	}
	
	
	// Add handler to EVENT_PREVIEW_RECEIVED.
	private void addPreviewReceivedHandler(EventHandler<CameraCaptureEventArgs> handler)
	{
		boolean isFirstHandler = !this.hasHandlers(EVENT_PREVIEW_RECEIVED);
		super.addHandler(EVENT_PREVIEW_RECEIVED, handler);
		if(isFirstHandler && m_PreviewRequestBuilder != null && m_PreviewCallbackSurface != null)
		{
			Log.v(TAG, "addPreviewReceivedHandler() - Add preview call-back surface");
			m_PreviewRequestBuilder.addTarget(m_PreviewCallbackSurface);
			if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTED)
				this.startPreviewRequestDirectly();
		}
	}
	
	
	// Apply AF regions to preview.
	@SuppressWarnings("unchecked")
	private void applyAfRegions()
	{
		// check focus mode
		switch(this.get(PROP_FOCUS_MODE))
		{
			case CONTINUOUS_AF:
			case NORMAL_AF:
				break;
			default:
				return;
		}
		
		// check preview state
		if(m_PreviewRequestBuilder == null)
			return;
		
		// create region list
		m_TempList.clear();
		List<MeteringRectangle> regionList = (List<MeteringRectangle>)m_TempList;
		for(int i = m_AfRegions.size() - 1 ; i >= 0 ; --i)
		{
			MeteringRectangle rect = this.createMeteringRectangle(m_AfRegions.get(i));
			if(rect != null)
				regionList.add(rect);
		}
		MeteringRectangle[] regionArray;
		if(regionList.isEmpty())
			regionArray = null;
		else
		{
			regionArray = new MeteringRectangle[regionList.size()];
			regionList.toArray(regionArray);
		}
		
		// apply regions
		m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, regionArray);
		this.applyToPreview();
	}
	
	
	// Apply focus mode to preview.
	private void applyFocusMode()
	{
		// prepare values
		int afModeValue;
		switch(m_FocusMode)
		{
			case DISABLED:
				afModeValue = CaptureRequest.CONTROL_AF_MODE_OFF;
				break;
			case NORMAL_AF:
				afModeValue = CaptureRequest.CONTROL_AF_MODE_AUTO;
				break;
			case CONTINUOUS_AF:
				if(m_IsRecordingMode)
					afModeValue = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
				else
					afModeValue = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
				break;
			case MANUAL:
				afModeValue = CaptureRequest.CONTROL_AF_MODE_OFF;
				break;
			default:
				Log.e(TAG, "applyFocusMode() - Unknown focus mode : " + m_FocusMode);
				return;
		}
		
		// apply to preview
		if(m_PreviewRequestBuilder != null)
		{
			m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, afModeValue);
			this.applyToPreview();
		}
	}
	
	
	// Apply scene mode.
	private boolean applySceneMode(CaptureRequest.Builder builder, int sceneMode)
	{
		if(builder != null)
		{
			if(sceneMode == CaptureRequest.CONTROL_SCENE_MODE_DISABLED)
			{
				builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
			}
			else
			{
				builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
			}
			builder.set(CaptureRequest.CONTROL_SCENE_MODE, sceneMode);
			return true;
		}
		return false;
	}
	
	
	// Apply changed request to preview.
	private boolean applyToPreview()
	{
		if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTED)
		{
			if(!this.startPreviewRequestDirectly())
				Log.e(TAG, "applyToPreview() - Fail to apply new request to preview");
		}
		return true;
	}
	
	
	// Start photo capture.
	@Override
	public Handle capture(int frameCount, int flags)
	{
		// check parameter
		if(frameCount == 0)
		{
			Log.e(TAG, "capture() - Invalid frame count : " + frameCount);
			return null;
		}
		
		// check state
		boolean captureLater = false;
		this.verifyAccess();
		if(this.get(PROP_CAPTURE_STATE) != OperationState.STOPPED)
		{
			Log.e(TAG, "capture() - Capture state is " + this.get(PROP_CAPTURE_STATE));
			return null;
		}
		switch(m_State)
		{
			case OPENED:
				break;
			case OPENING:
				Log.w(TAG, "capture() - Opening camera, capture later");
				captureLater = true;
				break;
			default:
				Log.e(TAG, "capture() - Current state is " + m_State);
				return null;
		}
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STARTED:
				break;
			case STARTING:
				Log.w(TAG, "capture() - Starting preview, capture later");
				captureLater = true;
				break;
			default:
				Log.e(TAG, "capture() - Preview state is " + this.get(PROP_PREVIEW_STATE));
				return null;
		}
		
		// change state
		this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STARTING);
		
		// create handle
		m_CaptureHandle = new Handle("Capture")
		{
			@Override
			protected void onClose(int flags)
			{
				stopCaptureInternal();
			}
		};
		
		// start capture
		m_TargetCapturedFrameCount = frameCount;
		if(!captureLater && !this.captureInternal())
			return null;
		
		// complete
		return m_CaptureHandle;
	}
	
	
	// Start capturing photo.
	private boolean captureInternal()
	{
		// check state
		if(this.get(PROP_CAPTURE_STATE) != OperationState.STARTING)
		{
			Log.e(TAG, "captureInternal() - Capture state is " + this.get(PROP_CAPTURE_STATE));
			return false;
		}
		if(this.get(PROP_PREVIEW_STATE) != OperationState.STARTED)
		{
			Log.e(TAG, "captureInternal() - Preview state is " + this.get(PROP_PREVIEW_STATE));
			return false;
		}
		
		// check ZSL
		boolean enableZsl = true;
		
		// prepare capture request builder
		CaptureRequest.Builder builder = null;
		if(enableZsl)
		{
			try
			{
				builder = m_Device.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
				Log.v(TAG, "captureInternal() - Use ZSL template");
			}
			catch(Throwable ex)
			{}
		}
		if(builder == null)
		{
			try
			{
				builder = m_Device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
				Log.v(TAG, "captureInternal() - Use still capture template");
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "captureInternal() - Fail to create capture request builder", ex);
				return false;
			}
		}
		
		// create capture request
		try
		{
			// prepare Surfaces
			builder.addTarget(m_PreviewSurface);
			builder.addTarget(m_PictureSurface);
			if(m_VideoSurface != null)
				builder.addTarget(m_VideoSurface);
			
			// set flash mode
			this.setFlashMode(m_FlashMode, builder);
			
			// set rotation
			int deviceOrientation = this.get(PROP_PICTURE_ROTATION).getDeviceOrientation();
			if(m_LensFacing == LensFacing.FRONT)
				deviceOrientation = -deviceOrientation;
			builder.set(CaptureRequest.JPEG_ORIENTATION, (m_SensorOrientation + deviceOrientation + 360) % 360);
			
			// set scene mode
			this.applySceneMode(builder, m_SceneMode);
			
			// create request
			m_PictureCaptureRequest = builder.build();
		} 
		catch(Throwable ex)
		{
			Log.e(TAG, "captureInternal() - Fail to create capture request", ex);
			//
			this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STOPPED);
			return false;
		}
		
		// start capture
		try
		{
			if(m_TargetCapturedFrameCount == 1)
				m_CaptureSession.capture(m_PictureCaptureRequest, m_PictureCaptureCallback, this.getHandler());
			else if(m_TargetCapturedFrameCount < 0)
				m_CaptureSession.setRepeatingRequest(m_PictureCaptureRequest, m_PictureCaptureCallback, this.getHandler());
			else
			{
				List<CaptureRequest> requestList = new ArrayList<>();
				for(int i = m_TargetCapturedFrameCount ; i > 0 ; --i)
					requestList.add(m_PictureCaptureRequest);
				m_CaptureSession.captureBurst(requestList, m_PictureCaptureCallback, this.getHandler());
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "captureInternal() - Fail to start capture", ex);
			//
			this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STOPPED);
			return false;
		}
		
		// change state
		this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STARTED);
		
		// complete
		return true;
	}
	
	
	// Change camera state
	private State changeState(State state)
	{
		State oldState = m_State;
		if(oldState != state)
		{
			m_State = state;
			this.notifyPropertyChanged(PROP_STATE, oldState, state);
		}
		return m_State;
	}
	
	
	// Start closing camera.
	@Override
	public void close(int flags)
	{
		// check state
		this.verifyAccess();
		switch(m_State)
		{
			case CLOSED:
			case CLOSING:
			case UNAVAILABLE:
				return;
			case OPENED:
				break;
			case OPENING:
				Log.w(TAG, "close() - Close while opening");
				this.changeState(State.CLOSING);
				return;
		}
		
		// change state
		if(this.changeState(State.CLOSING) != State.CLOSING)
		{
			Log.w(TAG, "close() - Close process has been interrupted");
			return;
		}
		
		// stop capture
		this.stopCaptureSession(false);
		if(m_CaptureSessionState == OperationState.STOPPING)
		{
			Log.w(TAG, "close() - Wait for capture session close");
			return;
		}
		
		// close
		this.closeInternal();
	}
	
	
	// close camera.
	private void closeInternal()
	{
		// close device
		if(m_Device != null)
		{
			this.close(m_Device);
			m_Device = null;
		}
		
		// destroy RenderScript resources
		if(m_PreviewCallbackAllocation != null)
		{
			m_PreviewCallbackAllocation.destroy();
			m_PreviewCallbackAllocation = null;
		}
		m_RenderScriptHandle = Handle.close(m_RenderScriptHandle);
		m_RenderScript = null;
		
		// change state
		this.changeState(State.CLOSED);
	}
	
	
	// Close camera device.
	private void close(CameraDevice camera)
	{
		if(camera != null)
		{
			try
			{
				Log.w(TAG, "close() - Close '" + m_Id + "' [start]");
				camera.close();
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "close() - Fail to close '" + m_Id + "'", ex);
			}
			finally
			{
				Log.w(TAG, "close() - Close '" + m_Id + "' [end]");
			}
		}
	}
	
	
	// Copy image to byte array.
	private byte[] copyImage(Image image)
	{
		if(image == null)
		{
			Log.e(TAG, "copyImage() - No image");
			return new byte[0];
		}
		try
		{
			switch(image.getFormat())
			{
				case ImageFormat.JPEG:
				case ImageFormat.RAW_SENSOR:
				{
					ByteBuffer buffer = image.getPlanes()[0].getBuffer();
					byte[] array = new byte[buffer.capacity()];
					buffer.get(array);
					return array;
				}
				case ImageFormat.NV21:
				{
					int width = image.getWidth();
					int height = image.getHeight();
					int sizeY = (width * height);
					int sizeU = (sizeY / 4);
					byte[] array = new byte[sizeY + sizeU + sizeU];
					image.getPlanes()[0].getBuffer().get(array, 0, sizeY);
					image.getPlanes()[1].getBuffer().get(array, sizeY, sizeU);
					image.getPlanes()[2].getBuffer().get(array, sizeY + sizeU, sizeU);
					return array;
				}
				default:
				{
					Log.e(TAG, "copyImage() - Unknown format : " + image.getFormat());
					return new byte[0];
				}
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "copyImage() - Fail to copy image", ex);
			return new byte[0];
		}
	}
	
	
	// Create MeteringRectangle instance.
	private MeteringRectangle createMeteringRectangle(MeteringRect rect)
	{
		if(rect.isIgnorable())
			return null;
		int left = (int)(rect.getLeft() * m_SensorSize.getWidth() + 0.5f);
		int top = (int)(rect.getTop() * m_SensorSize.getHeight() + 0.5f);
		int right = (int)(rect.getRight() * m_SensorSize.getWidth() + 0.5f);
		int bottom = (int)(rect.getBottom() * m_SensorSize.getHeight() + 0.5f);
		int weight = (MeteringRectangle.METERING_WEIGHT_MIN + (int)((MeteringRectangle.METERING_WEIGHT_MAX - MeteringRectangle.METERING_WEIGHT_MIN) * rect.getWeight()));
		return new MeteringRectangle(left, top, (right - left), (bottom - top), weight);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_AE_REGIONS)
			return (TValue)m_AeRegions;
		if(key == PROP_AF_REGIONS)
			return (TValue)m_AfRegions;
		if(key == PROP_FLASH_MODE)
			return (TValue)m_FlashMode;
		if(key == PROP_FOCUS_MODE)
			return (TValue)m_FocusMode;
		if(key == PROP_ID)
			return (TValue)m_Id;
		if(key == PROP_IS_RECORDING_MODE)
			return (TValue)(Boolean)m_IsRecordingMode;
		if(key == PROP_LENS_FACING)
			return (TValue)m_LensFacing;
		if(key == PROP_PICTURE_SIZE)
			return (TValue)m_PictureSize;
		if(key == PROP_PREVIEW_SIZE)
			return (TValue)m_PreviewSize;
		if(key == PROP_SCENE_MODE)
			return (TValue)(Integer)m_SceneMode;
		if(key == PROP_SENSOR_RATIO)
			return (TValue)AspectRatio.get(m_SensorSize);
		if(key == PROP_SENSOR_SIZE)
			return (TValue)m_SensorSize;
		if(key == PROP_STATE)
			return (TValue)m_State;
		if(key == PROP_VIDEO_SURFACE)
			return (TValue)m_VideoSurface;
		return super.get(key);
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_AF_TIMEOUT:
				this.onAutoFocusTimeout();
				break;
				
			case MSG_CAPTURE_SESSION_CLOSE_TIMEOUT:
				Log.e(TAG, "handleMessage() - Capture session close timeout");
				this.onCaptureSessionClosed(m_CaptureSession);
				break;
				
			case MSG_PREVIEW_FRAME_RECEIVED:
				this.onPreviewFrameReceived();
				break;
				
			case MSG_START_AF:
				this.startAutoFocus();
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when AF timeout.
	private void onAutoFocusTimeout()
	{
		if(this.get(PROP_FOCUS_STATE) == FocusState.SCANNING)
		{
			Log.e(TAG, "onAutoFocusTimeout()");
			
			// cancel AF
			if(m_PreviewRequestBuilder != null)
			{
				m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
				this.startPreviewRequestDirectly();
			}
			
			// update state
			m_IsAutoFocusTimeout = true;
			this.setReadOnly(PROP_FOCUS_STATE, FocusState.UNFOCUSED);
		}
	}
	
	
	// Called when capture completed
	private void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result, CaptureFailure failure)
	{
		// check state
		OperationState captureState = this.get(PROP_CAPTURE_STATE);
		switch(captureState)
		{
			case STARTED:
			case STOPPING:
				break;
			default:
				Log.e(TAG, "onCaptureCompleted() - Capture state is " + captureState);
				return;
		}
		if(m_CaptureHandle == null)
		{
			Log.e(TAG, "onCaptureCompleted() - No capture handle");
			return;
		}
		
		// update index
		++m_ReceivedCaptureCompletedCount;
		
		// print logs
		Log.v(TAG, "onCaptureCompleted() - Index : ", (m_ReceivedCaptureCompletedCount - 1));
		boolean success = (failure == null);
		if(!success && this.get(PROP_CAPTURE_STATE) != OperationState.STOPPING)
			Log.e(TAG, "onCaptureCompleted() - Capture failed");
		
		// check index
		if(m_TargetCapturedFrameCount > 0 && m_ReceivedCaptureCompletedCount > m_TargetCapturedFrameCount)
		{
			Log.w(TAG, "onCaptureCompleted() - Unexpected call-back, drop");
			return;
		}
		
		// check picture
		byte[] picture;
		if(success)
		{
			picture = m_ReceivedPictures.poll();
			if(picture == null)
			{
				Log.w(TAG, "onCaptureCompleted() - Wait for picture");
				m_ReceivedCaptureCompletedResults.add(result);
				return;
			}
		}
		else
			picture = null;
		
		// handle result
		this.onPictureReceived(result, picture);
	}
	private void onCaptureCompleted(boolean continueCaptureSession)
	{
		Log.w(TAG, "onCaptureCompleted()");
		
		// clear result queues
		m_ReceivedCaptureStartedResults.clear();
		m_ReceivedCaptureCompletedResults.clear();
		m_ReceivedPictures.clear();
		
		// reset state
		m_ReceivedCaptureStartedCount = 0;
		m_ReceivedCaptureCompletedCount = 0;
		m_ReceivedPictureCount = 0;
		m_CaptureHandle = null;
		m_TargetCapturedFrameCount = 0;
		m_IsCaptureSequenceCompleted = false;
		this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STOPPED);
		
		// stop capture session or preview
		if(continueCaptureSession)
		{
			if(m_CaptureSessionState == OperationState.STOPPING)
			{
				Log.w(TAG, "onCaptureCompleted() - Stop capture session");
				m_CaptureSessionState = OperationState.STARTED;
				this.stopCaptureSession(false);
			}
			else if(this.get(PROP_PREVIEW_STATE) == OperationState.STOPPING)
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
		}
	}
	
	
	// Called when capture completes on driver side.
	private void onCaptureSequenceCompleted()
	{
		Log.v(TAG, "onCaptureSequenceCompleted()");
		
		// update state
		m_IsCaptureSequenceCompleted = true;
		
		// complete capture
		if(this.get(PROP_CAPTURE_STATE) == OperationState.STOPPING)
			this.onCaptureCompleted(true);
	}
	
	
	// Called when capture session closed.
	private void onCaptureSessionClosed(CameraCaptureSession session)
	{
		// check state
		if(m_CaptureSession != session)
		{
			Log.e(TAG, "onCaptureSessionClosed() - Unknown session : " + session);
			return;
		}
		
		Log.w(TAG, "onCaptureSessionClosed() - Session : " + session);
		
		// stop timer
		this.getHandler().removeMessages(MSG_CAPTURE_SESSION_CLOSE_TIMEOUT);
		
		// release picture reader
		if(m_PictureSurface != null)
		{
			m_PictureSurface.release();
			m_PictureSurface = null;
		}
		if(m_PictureReader != null)
		{
			m_PictureReader.close();
			m_PictureReader = null;
		}
		
		// release temporary Surfaces
		if(!m_TempSurfaces.isEmpty())
		{
			for(int i = m_TempSurfaces.size() - 1 ; i >= 0 ; --i)
				m_TempSurfaces.get(i).release();
			m_TempSurfaces.clear();
		}
		
		// cancel AF
		this.getHandler().removeMessages(MSG_START_AF);
		
		// reset state
		m_PreviewSurface = null;
		m_CaptureSession = null;
		m_CaptureSessionState = OperationState.STOPPED;
		m_IsAutoFocusTimeout = false;
		m_PreviewCallbackData = null;
		if(m_IsPreviewReceived)
		{
			m_IsPreviewReceived = false;
			this.notifyPropertyChanged(PROP_IS_PREVIEW_RECEIVED, true, false);
		}
		
		// clear request builders
		m_PreviewRequestBuilder = null;
		
		// release preview call-back buffer
		if(m_PreviewCallbackSurface != null)
		{
			m_PreviewCallbackSurface.release();
			m_PreviewCallbackSurface = null;
		}
		if(m_PreviewCallbackAllocation != null)
		{
			m_PreviewCallbackAllocation.destroy();
			m_PreviewCallbackAllocation = null;
		}
		
		// restart capture session
		if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTING)
		{
			Log.w(TAG, "onCaptureSessionClosed() - Restart capture session immediately");
			if(this.startCaptureSession())
				return;
			else
				Log.e(TAG, "onCaptureSessionClosed() - Fail to restart capture session");
		}
		
		// reset preview state
		this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
		
		// close camera
		if(m_State == State.CLOSING)
		{
			Log.w(TAG, "onCaptureSessionClosed() - Close camera");
			this.closeInternal();
		}
	}
	
	
	// Called when capture session configured.
	private void onCaptureSessionConfigured(CameraCaptureSession session)
	{
		// check state
		if(m_CaptureSessionState != OperationState.STARTING)
		{
			Log.e(TAG, "onCaptureSessionConfigured() - Current session state is " + m_CaptureSessionState);
			session.close();
			if(m_CaptureSessionState == OperationState.STOPPING)
			{
				m_CaptureSession = session;
				this.onCaptureSessionClosed(session);
			}
			return;
		}
		
		Log.w(TAG, "onCaptureSessionConfigured() - Session : " + session);
		
		// update state
		m_CaptureSessionState = OperationState.STARTED;
		m_CaptureSession = session;
		
		// start preview
		if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTING)
			this.startPreviewRequest();
	}
	
	
	// Called when capture session configure failed.
	private void onCaptureSessionConfigureFailed(CameraCaptureSession session)
	{
		// close session
		if(session != null)
			session.close();
		
		// check state
		if(m_CaptureSessionState != OperationState.STARTING)
		{
			Log.w(TAG, "onCaptureSessionConfigured() - Current session state is " + m_CaptureSessionState);
			return;
		}
		
		Log.e(TAG, "onCaptureSessionConfigureFailed()");
		
		// reset state
		m_CaptureSessionState = OperationState.STOPPED;
		
		// cancel starting preview
		if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTING)
		{
			Log.e(TAG, "onCaptureSessionConfigureFailed() - Fail to create capture session, cancel starting preview");
			//
			this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
		}
		
		// complete
		m_CaptureSession = session;
		this.onCaptureSessionClosed(session);
	}
	
	
	// Called when capture started.
	private void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) 
	{
		// check state
		OperationState captureState = this.get(PROP_CAPTURE_STATE);
		switch(captureState)
		{
			case STARTED:
			case STOPPING:
				break;
			default:
				Log.e(TAG, "onCaptureStarted() - Capture state is " + captureState);
				return;
		}
		if(m_CaptureHandle == null)
		{
			Log.e(TAG, "onCaptureStarted() - No capture handle");
			return;
		}
		
		// update index
		Log.v(TAG, "onCaptureStarted() - Index : ", m_ReceivedCaptureStartedCount);
		++m_ReceivedCaptureStartedCount;
		
		// check index
		if(m_TargetCapturedFrameCount > 0 && m_ReceivedCaptureStartedCount > m_TargetCapturedFrameCount)
		{
			Log.w(TAG, "onCaptureStarted() - Unexpected call-back, drop");
			return;
		}
		
		// raise event
		this.raise(EVENT_SHUTTER, CameraCaptureEventArgs.obtain(m_CaptureHandle, m_ReceivedCaptureStartedCount - 1, null));
	}
	
	
	// Called when camera open failed.
	@SuppressWarnings("incomplete-switch")
	private void onDeviceError(CameraDevice camera, int error, boolean disconnected)
	{
		// check state
		if(m_State != State.OPENING)
		{
			Log.w(TAG, "onDeviceError() - Current state is " + m_State);
			
			// raise event
			this.raise(EVENT_ERROR, EventArgs.EMPTY);
			
			// close camera
			this.close(camera);
			if(this.get(PROP_IS_RELEASED))
				this.changeState(State.UNAVAILABLE);
			else if(m_State == State.CLOSING)
			{
				this.raise(EVENT_OPEN_CANCELLED, EventArgs.EMPTY);
				this.changeState(State.CLOSED);
			}
			
			// stop capture
			switch(this.get(PROP_CAPTURE_STATE))
			{
				case STARTING:
				case STARTED:
					Log.e(TAG, "onDeviceError() - Stop capture directly");
					this.onCaptureCompleted(false);
					break;
			}
			
			// stop capture session
			this.stopCaptureSession(true);
			return;
		}
		
		// print logs
		if(disconnected)
			Log.e(TAG, "onDeviceError() - Camera '" + m_Id + "' disconnected");
		else
			Log.e(TAG, "onDeviceError() - Fail to open camera '" + m_Id + "', error : " + error);
		
		// cancel starting preview
		if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTING)
		{
			Log.e(TAG, "onDeviceError() - Cancel preview starting");
			//
			this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
		}
		
		// raise event
		this.raise(EVENT_OPEN_FAILED, EventArgs.EMPTY);
		
		// change state
		if(m_State == State.OPENING)
			this.changeState(State.CLOSED);
	}
	
	
	// Called when device opened.
	private void onDeviceOpened(CameraDevice camera)
	{
		// check state
		if(m_State != State.OPENING)
		{
			Log.w(TAG, "onDeviceOpened() - Current state is " + m_State);
			this.close(camera);
			if(this.get(PROP_IS_RELEASED))
				this.changeState(State.UNAVAILABLE);
			else if(m_State == State.CLOSING)
			{
				this.raise(EVENT_OPEN_CANCELLED, EventArgs.EMPTY);
				this.changeState(State.CLOSED);
			}
			return;
		}
		
		Log.w(TAG, "onDeviceOpened() - Camera ID : '" + m_Id + "', Device : " + camera);
		
		// save device instance
		m_Device = camera;
		
		// change state
		this.changeState(State.OPENED);
		
		// start preview
		if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTING)
		{
			if(!this.startCaptureSession())
			{
				Log.e(TAG, "onDeviceOpened() - Fail to start capture session");
				//
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
			}
		}
	}
	
	
	// Called when receiving picture.
	private void onPictureReceived(Image image)
	{
		// check state
		OperationState captureState = this.get(PROP_CAPTURE_STATE);
		switch(captureState)
		{
			case STARTED:
			case STOPPING:
				break;
			default:
				Log.w(TAG, "onPictureReceived() - Capture state is " + captureState);
				return;
		}
		if(m_CaptureHandle == null)
		{
			Log.e(TAG, "onPictureReceived() - No capture handle");
			return;
		}
		
		// copy image
		byte[] picture = this.copyImage(image);
		
		// update index
		++m_ReceivedPictureCount;
		Log.v(TAG, "onPictureReceived() - Index : ", (m_ReceivedPictureCount - 1), ", picture buffer size : ", picture.length);
		
		// check index
		if(m_TargetCapturedFrameCount > 0 && m_ReceivedPictureCount > m_TargetCapturedFrameCount)
		{
			Log.w(TAG, "onPictureReceived() - Unexpected picture, drop");
			return;
		}
		
		// check capture result
		CaptureResult captureResult = m_ReceivedCaptureCompletedResults.poll();
		if(captureResult == null)
		{
			m_ReceivedPictures.add(picture);
			Log.w(TAG, "onPictureReceived() - Received picture before capture completed");
			return;
		}
		
		// handle received picture
		this.onPictureReceived(captureResult, picture);
	}
	
	
	// Called when both picture and capture result are received.
	private void onPictureReceived(CaptureResult result, byte[] picture)
	{
		// prepare completing capture
		OperationState captureState = this.get(PROP_CAPTURE_STATE);
		boolean failed = (picture == null || picture.length == 0);
		boolean frameCountReached = (m_TargetCapturedFrameCount > 0 && m_ReceivedPictureCount >= m_TargetCapturedFrameCount);
		if(captureState == OperationState.STARTED)
		{
			if(frameCountReached || failed)
			{
				if(failed)
					Log.e(TAG, "onPictureReceived() - Capture failed, start completing capture");
				else
					Log.w(TAG, "onPictureReceived() - Frame count reached, start completing capture");
				captureState = OperationState.STOPPING;
				this.setReadOnly(PROP_CAPTURE_STATE, captureState);
			}
		}
		
		// raise event
		if(!failed)
		{
			int pictureFormat = this.get(PROP_PICTURE_FORMAT);
			Size pictureSize = this.get(PROP_PICTURE_SIZE);
			this.raise(EVENT_PICTURE_RECEIVED, CameraCaptureEventArgs.obtain(m_CaptureHandle, (m_ReceivedPictureCount - 1), result, picture, pictureFormat, pictureSize));
		}
		else
			this.raise(EVENT_CAPTURE_FAILED, CameraCaptureEventArgs.obtain(m_CaptureHandle, (m_ReceivedPictureCount - 1), result));
		
		// complete capture
		if((frameCountReached || failed) && captureState == OperationState.STOPPING && m_IsCaptureSequenceCompleted)
			this.onCaptureCompleted(true);
	}
	
	
	// Called when preview capture completed.
	private void onPreviewCaptureCompleted(CaptureResult result)
	{
		// check focus state
		int afState = result.get(CaptureResult.CONTROL_AF_STATE);
		switch(afState)
		{
			case CaptureResult.CONTROL_AF_STATE_INACTIVE:
				m_IsAutoFocusTimeout = false;
				this.getHandler().removeMessages(MSG_AF_TIMEOUT);
				this.setReadOnly(PROP_FOCUS_STATE, FocusState.INACTIVE);
				break;
			case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
			case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
				m_IsAutoFocusTimeout = false;
				this.getHandler().removeMessages(MSG_AF_TIMEOUT);
				this.setReadOnly(PROP_FOCUS_STATE, FocusState.FOCUSED);
				break;
			case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
			case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
				m_IsAutoFocusTimeout = false;
				this.getHandler().removeMessages(MSG_AF_TIMEOUT);
				this.setReadOnly(PROP_FOCUS_STATE, FocusState.UNFOCUSED);
				break;
			case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
			case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
				if(!m_IsAutoFocusTimeout && this.setReadOnly(PROP_FOCUS_STATE, FocusState.SCANNING))
					this.getHandler().sendEmptyMessageDelayed(MSG_AF_TIMEOUT, TIMEOUT_AF_COMPLETE);
				break;
			default:
				Log.w(TAG, "onPreviewCaptureCompleted() - Unknown AF state : " + afState);
				m_IsAutoFocusTimeout = false;
				this.getHandler().removeMessages(MSG_AF_TIMEOUT);
				this.setReadOnly(PROP_FOCUS_STATE, FocusState.INACTIVE);
				break;
		}
	}
	
	
	// Called when preview frame received.
	private void onPreviewFrameReceived()
	{
		// receive preview frame
		if(m_PreviewCallbackAllocation != null)
		{
			try
			{
				m_PreviewCallbackAllocation.ioReceive();
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "onPreviewFrameReceived() - Fail to receive preview frame", ex);
			}
		}
		
		// remove surface
		boolean isPreviewStarted = (this.get(PROP_PREVIEW_STATE) == OperationState.STARTED);
		boolean hasHandlers = this.hasHandlers(EVENT_PREVIEW_RECEIVED);
		if(!hasHandlers && m_PreviewRequestBuilder != null && m_PreviewCallbackSurface != null)
		{
			Log.v(TAG, "onPreviewFrameReceived() - Remove preview call-back surface");
			m_PreviewRequestBuilder.removeTarget(m_PreviewCallbackSurface);
			if(isPreviewStarted)
				this.startPreviewRequestDirectly();
		}
		
		// check state
		if(!isPreviewStarted)
			return;
		
		// update state
		if(!m_IsPreviewReceived)
		{
			Log.v(TAG, "onPreviewFrameReceived() - First preview frame received");
			m_IsPreviewReceived = true;
			this.notifyPropertyChanged(PROP_IS_PREVIEW_RECEIVED, false, true);
		}
		
		// raise event
		if(hasHandlers && m_PreviewCallbackAllocation != null)
		{
			int dataSize = (m_PreviewSize.getWidth() * m_PreviewSize.getHeight() * 3 / 2);
			if(m_PreviewCallbackData == null || m_PreviewCallbackData.length != dataSize)
				m_PreviewCallbackData = new byte[dataSize];
			m_PreviewCallbackAllocation.copyTo(m_PreviewCallbackData);
			this.raise(EVENT_PREVIEW_RECEIVED, CameraCaptureEventArgs.obtain(null, -1, null, m_PreviewCallbackData, ImageFormat.YUV_420_888, m_PreviewSize));
		}
	}
	
	
	// Called when releasing object.
	@Override
	protected void onRelease()
	{
		// change state
		if(m_State == State.CLOSED)
			this.changeState(State.UNAVAILABLE);
		
		// clear references
		m_Context = null;
		
		// call super
		super.onRelease();
	}


	// Start opening camera.
	@Override
	public boolean open(int flags)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		switch(m_State)
		{
			case OPENING:
			case OPENED:
				return true;
			case CLOSED:
				break;
			case CLOSING:
				Log.w(TAG, "open() - Open while closing");
				return (this.changeState(State.OPENING) == State.OPENING);
			default:
				Log.e(TAG, "open() - Invalid state : " + m_State);
				return false;
		}
		
		// open camera
		if(!this.openInternal(flags))
			return false;
		return (this.changeState(State.OPENING) == State.OPENING);
	}
	
	
	// Start opening camera.
	private boolean openInternal(int flags)
	{
		try
		{
			Log.w(TAG, "openInternal() - Start opening camera '" + m_Id + "'");
			m_CameraManager.openCamera(m_Id, m_DeviceStateCallback, this.getHandler());
			return true;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "openInternal() - Fail to start opening camera '" + m_Id + "'", ex);
			this.raise(EVENT_OPEN_FAILED, EventArgs.EMPTY);
			return false;
		}
	}
	
	
	// Prepare Surface for capture.
	private Surface prepareSurface(Object receiver)
	{
		Surface surface;
		if(receiver instanceof SurfaceHolder)
		{
			SurfaceHolder holder = (SurfaceHolder)receiver;
			//holder.setFixedSize(captureSize.getWidth(), captureSize.getHeight());
			surface = holder.getSurface();
		}
		else if(receiver instanceof SurfaceTexture)
		{
			SurfaceTexture surfaceTexture = (SurfaceTexture)receiver;
			//surfaceTexture.setDefaultBufferSize(captureSize.getWidth(), captureSize.getHeight());
			surface = new Surface(surfaceTexture);
			m_TempSurfaces.add(surface);
		}
		else if(receiver instanceof ImageReader)
		{
			ImageReader reader = (ImageReader)receiver;
			surface = reader.getSurface();
		}
		else
		{
			Log.e(TAG, "prepareSurface() - Unsupported receiver : " + receiver);
			return null;
		}
		return surface;
	}
	
	
	// Remove event handler.
	@SuppressWarnings("unchecked")
	@Override
	public <TArgs extends EventArgs> void removeHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		if(key == EVENT_PREVIEW_RECEIVED)
			this.removePreviewReceivedHandler((EventHandler<CameraCaptureEventArgs>)handler);
		else
			super.removeHandler(key, handler);
	}
	
	
	// Remove handler from EVENT_PREVIEW_RECEIVED.
	private void removePreviewReceivedHandler(EventHandler<CameraCaptureEventArgs> handler)
	{
		super.removeHandler(EVENT_PREVIEW_RECEIVED, handler);
		if(!this.hasHandlers(EVENT_PREVIEW_RECEIVED) && m_PreviewRequestBuilder != null && m_PreviewCallbackSurface != null)
		{
			Log.v(TAG, "removePreviewReceivedHandler() - Remove preview call-back surface");
			m_PreviewRequestBuilder.removeTarget(m_PreviewCallbackSurface);
			if(this.get(PROP_PREVIEW_STATE) == OperationState.STARTED)
				this.startPreviewRequestDirectly();
		}
	}
	
	
	// Set property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_AE_REGIONS)
			;
		if(key == PROP_AF_REGIONS)
			return this.setAfRegionsProp((List<MeteringRect>)value);
		if(key == PROP_FLASH_MODE)
			return this.setFlashModeProp((FlashMode)value);
		if(key == PROP_FOCUS_MODE)
			return this.setFocusModeProp((FocusMode)value);
		if(key == PROP_IS_RECORDING_MODE)
			return this.setRecordingModeProp((Boolean)value);
		if(key == PROP_PICTURE_SIZE)
			return this.setPictureSize((Size)value);
		if(key == PROP_PREVIEW_SIZE)
			return this.setPreviewSizeProp((Size)value);
		if(key == PROP_PREVIEW_RECEIVER)
			return this.setPreviewReceiver(value);
		if(key == PROP_SCENE_MODE)
			return this.setSceneModeProp((Integer)value);
		if(key == PROP_VIDEO_SURFACE)
			return this.setVideoSurface((Surface)value);
		return super.set(key, value);
	}
	
	
	// Set PROP_AF_REGIONS property.
	@SuppressWarnings("unchecked")
	private boolean setAfRegionsProp(List<MeteringRect> regions)
	{
		// check thread
		this.verifyAccess();
		
		// check parameter
		if(regions == null)
			regions = Collections.EMPTY_LIST;
		else if(regions.size() > this.get(PROP_MAX_AF_REGION_COUNT))
			throw new IllegalArgumentException("Too many AF regions");
		else
			regions = Collections.unmodifiableList(regions);
		
		// apply regions
		List<MeteringRect> oldRegions = m_AfRegions;
		m_AfRegions = regions;
		this.applyAfRegions();
		
		// start AF later
		if(!this.getHandler().hasMessages(MSG_START_AF))
			this.getHandler().sendEmptyMessage(MSG_START_AF);
		
		// update property
		return this.notifyPropertyChanged(PROP_AF_REGIONS, oldRegions, regions);
	}
	
	
	// Set flash mode.
	private void setFlashMode(FlashMode flashMode, CaptureRequest.Builder requestBuilder)
	{
		// update state
		m_FlashMode = flashMode;
		
		// apply
		if(requestBuilder != null)
		{
			int aeCtrlValue;
			int flashModeValue;
			switch(flashMode)
			{
				case AUTO:
					aeCtrlValue = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
					flashModeValue = CaptureRequest.FLASH_MODE_SINGLE;
					break;
				case OFF:
					aeCtrlValue = CaptureRequest.CONTROL_AE_MODE_ON;
					flashModeValue = CaptureRequest.FLASH_MODE_OFF;
					break;
				case ON:
					aeCtrlValue = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
					flashModeValue = CaptureRequest.FLASH_MODE_SINGLE;
					break;
				case TORCH:
					aeCtrlValue = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
					flashModeValue = CaptureRequest.FLASH_MODE_TORCH;
					break;
				default:
					throw new RuntimeException("Unsupported flash mode : " + flashMode + ".");
			}
			requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, aeCtrlValue);
			requestBuilder.set(CaptureRequest.FLASH_MODE, flashModeValue);
		}
	}
	
	
	// Set flash mode (property).
	private boolean setFlashModeProp(FlashMode flashMode)
	{
		// check state
		this.verifyAccess();
		FlashMode oldFlashMode = m_FlashMode;
		if(oldFlashMode == flashMode)
			return false;
		if(!this.get(PROP_HAS_FLASH) && flashMode != FlashMode.OFF)
		{
			Log.e(TAG, "setFlashModeProp() - No flash on camera '" + m_Id + "'");
			return false;
		}
		
		// set flash mode
		Log.v(TAG, "setFlashModeProp() - Flash mode : ", flashMode);
		this.setFlashMode(flashMode, m_PreviewRequestBuilder);
		
		// apply to preview
		this.applyToPreview();
		
		// complete
		return this.notifyPropertyChanged(PROP_FLASH_MODE, oldFlashMode, flashMode);
	}
	
	
	// Set PROP_FOCUS_MODE property.
	private boolean setFocusModeProp(FocusMode focusMode)
	{
		// check state
		this.verifyAccess();
		
		// check value
		if(focusMode == null)
			throw new IllegalArgumentException("No focus mode specified");
		List<FocusMode> focusModes = this.get(PROP_FOCUS_MODES);
		if(!focusModes.contains(focusMode))
		{
			if(focusModes.contains(FocusMode.CONTINUOUS_AF))
				focusMode = FocusMode.CONTINUOUS_AF;
			else if(focusModes.contains(FocusMode.NORMAL_AF))
				focusMode = FocusMode.NORMAL_AF;
			else
				focusMode = FocusMode.DISABLED;
			Log.e(TAG, "setFocusModeProp() - Invalid focus mode, change to " + focusMode);
		}
		if(m_FocusMode == focusMode)
			return false;
		
		// update focus mode
		FocusMode oldMode = m_FocusMode;
		m_FocusMode = focusMode;
		this.applyFocusMode();
		
		// start AF later
		if(!this.getHandler().hasMessages(MSG_START_AF))
			this.getHandler().sendEmptyMessage(MSG_START_AF);
		
		// complete
		return this.notifyPropertyChanged(PROP_FOCUS_MODE, oldMode, focusMode);
	}
	
	
	// Set picture size.
	private boolean setPictureSize(Size size)
	{
		// check size
		if(size == null)
			throw new IllegalArgumentException("No picture size specified.");
		if(!this.get(PROP_PICTURE_SIZES).contains(size))
		{
			Log.e(TAG, "setPictureSize() - Size " + size + " is not contained in size list");
			throw new IllegalArgumentException("Invalid picture size.");
		}
		if(m_PictureSize.equals(size))
			return false;
		
		// update property
		Log.w(TAG, "setPictureSize() - Size : " + size);
		Size prevSize = m_PictureSize;
		m_PictureSize = size;
		this.notifyPropertyChanged(PROP_PICTURE_SIZE, prevSize, size);
		
		// restart capture session
		if(m_CaptureSessionState != OperationState.STOPPING && m_CaptureSessionState != OperationState.STOPPED)
		{
			Log.w(TAG, "setPictureSize() - Restart capture session to apply new picture size");
			this.stopCaptureSession(false);
			this.startCaptureSession();
		}
		
		// complete
		return true;
	}
	
	
	// Set preview receiver.
	private boolean setPreviewReceiver(Object receiver)
	{
		// check state
		this.verifyAccess();
		Object prevReceiver = this.get(PROP_PREVIEW_RECEIVER);
		if(prevReceiver == receiver)
			return false;
		if(this.get(PROP_PREVIEW_STATE) != OperationState.STOPPED)
		{
			Log.e(TAG, "setPreviewReceiver() - Preview state is " + this.get(PROP_PREVIEW_STATE));
			throw new RuntimeException("Cannot change preview receiver when preview state is not STOPPED.");
		}
		
		// stop capture session
		this.stopCaptureSession(false);
		
		// change preview Surface
		if(m_PreviewRequestBuilder != null)
		{
			// remove old Surface
			if(m_PreviewSurface != null)
			{
				m_PreviewRequestBuilder.removeTarget(m_PreviewSurface);
				if(m_TempSurfaces.remove(m_PreviewSurface))
					m_PreviewSurface.release();
				m_PreviewSurface = null;
			}
			
			// add new Surface
			if(receiver != null)
			{
				Surface surface = this.prepareSurface(receiver);
				if(surface != null)
					m_PreviewRequestBuilder.addTarget(surface);
				else
				{
					Log.e(TAG, "setPreviewReceiver() - Fail to prepare Surface");
					throw new RuntimeException("Invalid preview receiver.");
				}
			}
		}
		
		// apply new receiver
		super.set(PROP_PREVIEW_RECEIVER, receiver);
		
		// complete
		return true;
	}
	
	
	// Set preview size property.
	private boolean setPreviewSizeProp(Size previewSize)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		
		// check preview size
		if(previewSize == null)
			throw new IllegalArgumentException("No preview size");
		Size oldSize = m_PreviewSize;
		if(previewSize.equals(oldSize))
			return false;
		if(!this.get(PROP_PREVIEW_SIZES).contains(previewSize))
		{
			Log.e(TAG, "setPreviewSizeProp() - Invalid preview size : " + previewSize);
			return false;
		}
		
		// stop preview first
		boolean needRestartPreview;
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				Log.w(TAG, "setPreviewSizeProp() - Stop preview to change preview size");
				this.stopPreview(0);
				needRestartPreview = true;
				break;
			default:
				needRestartPreview = false;
				break;
		}
		
		// set preview size
		m_PreviewSize = previewSize;
		
		// restart preview
		if(needRestartPreview)
		{
			Log.w(TAG, "setPreviewSizeProp() - Restart preview");
			this.startPreview(0);
		}
		
		// complete
		return this.notifyPropertyChanged(PROP_PREVIEW_SIZE, oldSize, previewSize);
	}
	
	
	// Set PROP_IS_RECORDING_MODE property.
	private boolean setRecordingModeProp(boolean isRecordingMode)
	{
		// check state
		this.verifyAccess();
		if(m_IsRecordingMode == isRecordingMode)
			return false;
		if(this.get(PROP_CAPTURE_STATE) != OperationState.STOPPED)
		{
			Log.e(TAG, "setRecordingModeProp() - Current capture state is " + this.get(PROP_CAPTURE_STATE));
			throw new IllegalStateException("Cannot change recording mode due to current capture state.");
		}
		
		Log.w(TAG, "setRecordingModeProp() - Recording mode : " + isRecordingMode);
		
		// stop preview first
		boolean needRestartPreview;
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				Log.w(TAG, "setRecordingModeProp() - Stop preview to change recording mode");
				this.stopPreview(0);
				needRestartPreview = true;
				break;
			default:
				needRestartPreview = false;
				break;
		}
		
		// change mode
		m_IsRecordingMode = isRecordingMode;
		
		// restart preview
		if(needRestartPreview)
		{
			Log.w(TAG, "setRecordingModeProp() - Restart preview");
			this.startPreview(0);
		}
		
		// complete
		return this.notifyPropertyChanged(PROP_IS_RECORDING_MODE, !isRecordingMode, isRecordingMode);
	}
	
	
	// Set PROP_SCENE_MODE property.
	private boolean setSceneModeProp(int sceneMode)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		if(m_SceneMode == sceneMode)
			return true;
		if(!m_SceneModes.contains(sceneMode))
		{
			Log.e(TAG, "setSceneModeProp() - Invalid scene mode : " + sceneMode);
			return false;
		}
		
		Log.v(TAG, "setSceneModeProp() - Scene mode : ", sceneMode);
		
		// apply scene mode
		if(m_PreviewRequestBuilder != null && this.applySceneMode(m_PreviewRequestBuilder, sceneMode))
			this.applyToPreview();
		
		// complete
		int oldSceneMode = m_SceneMode;
		m_SceneMode = sceneMode;
		return this.notifyPropertyChanged(PROP_SCENE_MODE, oldSceneMode, sceneMode);
	}
	
	
	// Set video surface.
	private boolean setVideoSurface(Surface surface)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		if(m_VideoSurface == surface)
			return false;
		if(this.get(PROP_CAPTURE_STATE) != OperationState.STOPPED)
		{
			Log.e(TAG, "setVideoSurface() - Current capture state is " + this.get(PROP_CAPTURE_STATE));
			throw new IllegalStateException("Cannot change video surface due to current capture state.");
		}
		
		Log.v(TAG, "setVideoSurface() - Surface : ", surface);
		
		// stop preview first
		boolean needRestartPreview;
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				if(m_IsRecordingMode)
				{
					Log.w(TAG, "setVideoSurface() - Stop preview to change video surface");
					this.stopPreview(0);
					needRestartPreview = true;
				}
				else
				{
					Log.w(TAG, "setVideoSurface() - Set video surface in non-recording mode");
					needRestartPreview = false;
				}
				break;
			default:
				needRestartPreview = false;
				break;
		}
		
		// change surface
		Surface oldSurface = m_VideoSurface;
		m_VideoSurface = surface;
		
		// restart preview
		if(needRestartPreview)
		{
			Log.w(TAG, "setVideoSurface() - Restart preview");
			this.startPreview(0);
		}
		
		// notify property change
		this.notifyPropertyChanged(PROP_VIDEO_SURFACE, oldSurface, surface);
		
		// complete
		return true;
	}
	
	
	// Start auto focus.
	@Override
	public boolean startAutoFocus(int flags)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		if(this.get(PROP_PREVIEW_STATE) != OperationState.STARTED)
		{
			Log.w(TAG, "startAutoFocus() - Preview state is " + this.get(PROP_PREVIEW_STATE));
			return false;
		}
		
		// start AF later
		if(!this.getHandler().hasMessages(MSG_START_AF))
			this.getHandler().sendEmptyMessage(MSG_START_AF);
		
		// complete
		return true;
	}
	private void startAutoFocus()
	{
		// check state
		if(m_PreviewRequestBuilder == null)
			return;
		
		// cancel current focus lock
		boolean isPreviewStarted = (this.get(PROP_PREVIEW_STATE) == OperationState.STARTED);
		if(isPreviewStarted)
		{
			m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
			this.startPreviewRequestDirectly();
		}
		
		// start AF
		if(m_FocusMode == FocusMode.NORMAL_AF)
		{
			if(isPreviewStarted)
			{
				Log.v(TAG, "startAutoFocus() - Trigger single AF");
				m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
				this.startPreviewRequestDirectly();
				m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
			}
			else
				m_PreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
		}
	}
	
	
	// Start capture session.
	private boolean startCaptureSession()
	{
		// check state
		switch(m_CaptureSessionState)
		{
			case STARTED:
			case STARTING:
				return true;
			case STOPPED:
				break;
				/*
			case STOPPING:
				Log.w(TAG, "startCaptureSession() - Start while stopping");
				m_CaptureSessionState = OperationState.STARTING;
				return true;
				*/
			default:
				Log.e(TAG, "startCaptureSession() - Current session state is " + m_CaptureSessionState);
				return false;
		}
		
		// check picture size
		if(m_PreviewSize.getWidth() <= 0 || m_PreviewSize.getHeight() <= 0)
		{
			Log.e(TAG, "startCaptureSession() - Empty preview size");
			return false;
		}
		
		// check picture size
		Size pictureSize = m_PictureSize;
		if(pictureSize.getWidth() <= 0 || pictureSize.getHeight() <= 0)
		{
			Log.e(TAG, "startCaptureSession() - Empty picture size");
			return false;
		}
		
		// check picture format
		int pictureFormat = this.get(PROP_PICTURE_FORMAT);
		switch(this.get(PROP_PICTURE_FORMAT))
		{
			case ImageFormat.JPEG:
			case ImageFormat.RAW_SENSOR:
			case ImageFormat.NV21:
				break;
			default:
				Log.e(TAG, "startCaptureSession() - Unknown picture format : " + pictureFormat);
				return false;
		}
		
		// prepare preview surface
		List<Surface> surfaces = new ArrayList<>();
		m_PreviewSurface = this.prepareSurface(this.get(PROP_PREVIEW_RECEIVER));
		if(m_PreviewSurface == null)
		{
			Log.e(TAG, "startCaptureSession() - Fail to prepare Surface for preview");
			return false;
		}
		surfaces.add(m_PreviewSurface);
		
		// prepare picture surface
		m_PictureReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), pictureFormat, 1);
		m_PictureReader.setOnImageAvailableListener(m_PictureAvailableListener, this.getHandler());
		m_PictureSurface = m_PictureReader.getSurface();
		surfaces.add(m_PictureSurface);
		
		// prepare video/preview call-back surface
		if(m_IsRecordingMode && m_VideoSurface != null)
		{
			Log.v(TAG, "startCaptureSession() - Video surface : ", m_VideoSurface);
			surfaces.add(m_VideoSurface);
		}
		else
		{
			// create render script
			if(m_RenderScript == null)
			{
				m_RenderScriptHandle = RenderScriptManager.createRenderScript(m_Context);
				m_RenderScript = RenderScriptManager.getRenderScript(m_RenderScriptHandle);
			}
			
			// create allocation for preview frame call-back
			if(m_RenderScript != null)
			{
				Type.Builder typeBuilder = new Type.Builder(m_RenderScript, Element.YUV(m_RenderScript));
				typeBuilder.setYuvFormat(ImageFormat.YUV_420_888);
				typeBuilder.setX(m_PreviewSize.getWidth());
				typeBuilder.setY(m_PreviewSize.getHeight());
				m_PreviewCallbackAllocation = Allocation.createTyped(m_RenderScript, typeBuilder.create(), Allocation.USAGE_IO_INPUT | Allocation.USAGE_SCRIPT);
				m_PreviewCallbackAllocation.setOnBufferAvailableListener(m_PreviewCallbackAllocationCallback);
				m_PreviewCallbackSurface = m_PreviewCallbackAllocation.getSurface();
				surfaces.add(m_PreviewCallbackSurface);
			}
		}
		
		// create request builders
		try
		{
			// create builder
			if(!m_IsRecordingMode)
				m_PreviewRequestBuilder = m_Device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			else
			{
				Log.v(TAG, "startCaptureSession() - Create request builder for video recording");
				m_PreviewRequestBuilder = m_Device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
			}
			
			// prepare output surfaces
			m_PreviewRequestBuilder.addTarget(m_PreviewSurface);
			if(m_IsRecordingMode && m_VideoSurface != null)
				m_PreviewRequestBuilder.addTarget(m_VideoSurface);
			else// if(this.hasHandlers(EVENT_PREVIEW_RECEIVED) && m_PreviewCallbackSurface != null)
			{
				Log.v(TAG, "startCaptureSession() - Add preview call-back surface");
				m_PreviewRequestBuilder.addTarget(m_PreviewCallbackSurface);
			}
			
			// setup flash mode
			this.setFlashMode(m_FlashMode, m_PreviewRequestBuilder);
			
			// setup AE states
			//
			
			// setup AF states
			this.applyFocusMode();
			this.applyAfRegions();
			
			// setup effect
			//
			
			// setup scene mode
			this.applySceneMode(m_PreviewRequestBuilder, m_SceneMode);
			
			// TEST
			/*
			try
			{
				Field field = CaptureRequest.class.getField("ONEPLUS_REC_TIME_MULTIPLE");
				CaptureRequest.Key<Boolean> key = (CaptureRequest.Key<Boolean>)field.get(null);
				m_PreviewRequestBuilder.set(key, m_IsRecordingMode);
				Log.w(TAG, "[TEST] Set ONEPLUS_REC_TIME_MULTIPLE to " + m_IsRecordingMode);
			}
			catch(Throwable ex)
			{
				Log.w(TAG, "[TEST] No ONEPLUS_REC_TIME_MULTIPLE key to use");
			}
			*/
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "startCaptureSession() - Fail to create preview request builder", ex);
		}
		
		// create session
		try
		{
			Log.w(TAG, "startCaptureSession() - Create capture session for camera '" + m_Id + "'");
			m_Device.createCaptureSession(surfaces, m_CaptureSessionCallback, this.getHandler());
		} 
		catch (Throwable ex)
		{
			Log.e(TAG, "startCaptureSession() - Fail to create capture session for camera '" + m_Id + "'", ex);
			return false;
		}
		
		// change state
		m_CaptureSessionState = OperationState.STARTING;
		
		// complete
		return true;
	}
	
	
	// Start preview.
	@Override
	public boolean startPreview(int flags)
	{
		// check state
		this.verifyAccess();
		this.verifyReleaseState();
		if(m_State != State.OPENED && m_State != State.OPENING)
		{
			Log.e(TAG, "startPreview() - Camera state is " + m_State);
			return false;
		}
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STOPPED:
				break;
			case STARTED:
			case STARTING:
				return true;
			case STOPPING:
				Log.w(TAG, "startPreview() - Start while stopping");
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STARTING);
				return true;
		}
		
		// start capture session
		if(m_State == State.OPENED)
		{
			if(m_CaptureSessionState == OperationState.STARTED)
			{
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STARTING);
				return this.startPreviewRequest();
			}
			else if(!this.startCaptureSession())
			{
				Log.e(TAG, "startPreview() - Fail to start capture session");
				return false;
			}
		}
		else
			Log.w(TAG, "startPreview() - Start preview while opening camera");
		
		// complete
		this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STARTING);
		return true;
	}
	
	
	// Start preview request.
	private boolean startPreviewRequest()
	{
		// check state
		if(m_CaptureSessionState != OperationState.STARTED)
		{
			Log.e(TAG, "startPreviewRequest() - Capture session state is " + m_CaptureSessionState);
			return false;
		}
		if(this.get(PROP_PREVIEW_STATE) != OperationState.STARTING)
		{
			Log.e(TAG, "startPreviewRequest() - Preview state is " + this.get(PROP_PREVIEW_STATE));
			return false;
		}
		
		Log.w(TAG, "startPreviewRequest() - Start preview request for camera '" + m_Id + "'");
		
		// start preview
		if(this.startPreviewRequestDirectly())
		{
			this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STARTED);
			return true;
		}
		else
		{
			this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
			return false;
		}
	}
	
	
	// Start preview request without state check.
	private boolean startPreviewRequestDirectly()
	{
		try
		{
			m_CaptureSession.setRepeatingRequest(m_PreviewRequestBuilder.build(), m_PreviewCaptureCallback, this.getHandler());
			return true;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "startPreviewRequestDirectly() - Fail to start preview for camera '" + m_Id + "'", ex);
			return false;
		}
	}
	
	
	// Stop capture.
	private void stopCaptureInternal()
	{
		// check state
		if(m_CaptureHandle == null)
		{
			Log.e(TAG, "stopCaptureInternal() - No capture handle");
			return;
		}
		OperationState captureState = this.get(PROP_CAPTURE_STATE);
		switch(captureState)
		{
			case STOPPED:
				return;
			case STARTING:
				Log.w(TAG, "stopCaptureInternal() - Stop while starting");
				break;
			case STARTED:
				break;
			case STOPPING:
				return;
		}
		
		// change state
		this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STOPPING);
		
		// stop capture
		if(captureState == OperationState.STARTED)
		{
			try
			{
				if(m_TargetCapturedFrameCount < 0)
				{
					Log.w(TAG, "stopCaptureInternal() - Stop repeating request");
					m_CaptureSession.stopRepeating();
					this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
				}
				else
				{
					//Log.w(TAG, "stopCaptureInternal() - Abort captures");
					//m_CaptureSession.abortCaptures();
				}
			}
			catch(Throwable ex)
			{
				
			}
		}
		else
		{
			//
		}
	}
	
	
	// Stop capture session.
	private void stopCaptureSession(boolean stopDirectly)
	{
		// check state
		switch(m_CaptureSessionState)
		{
			case STOPPED:
				return;
			case STOPPING:
				if(!stopDirectly)
					return;
				break;
			case STARTED:
				break;
			case STARTING:
				Log.w(TAG, "stopCaptureSession() - Stop while starting");
				m_CaptureSessionState = OperationState.STOPPING;
				return;
		}
		
		Log.w(TAG, "stopCaptureSession() - Stop capture session for camera '" + m_Id + "'");
		
		// cancel capture
		switch(this.get(PROP_CAPTURE_STATE))
		{
			case STOPPED:
				break;
			case STARTED:
				Log.w(TAG, "stopCaptureSession() - Stop capture and wait for completion");
				m_CaptureSessionState = OperationState.STOPPING;
				this.stopCaptureInternal();
				if(this.get(PROP_CAPTURE_STATE) == OperationState.STOPPED)
					break;
				return;
			case STARTING:
				Log.w(TAG, "stopCaptureSession() - Stop while starting capture, stop capture directly");
				this.onCaptureCompleted(false);
				break;
			case STOPPING:
				Log.w(TAG, "stopCaptureSession() - Wait for capture completion");
				m_CaptureSessionState = OperationState.STOPPING;
				return;
		}
		
		// stop preview
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STOPPED:
			case STOPPING:
				break;
			default:
				Log.w(TAG, "stopCaptureSession() - Stop preview directly");
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPING);
				break;
		}
		
		// reset state
		m_CaptureSessionState = OperationState.STOPPING;
		m_CaptureSession.close();
		if(stopDirectly)
			this.onCaptureSessionClosed(m_CaptureSession);
		else
			this.getHandler().sendEmptyMessageDelayed(MSG_CAPTURE_SESSION_CLOSE_TIMEOUT, TIMEOUT_CAPTURE_SESSION_CLOSED);
	}
	
	
	// Stop preview.
	@Override
	public void stopPreview(int flags)
	{
		// check state
		this.verifyAccess();
		switch(this.get(PROP_PREVIEW_STATE))
		{
			case STOPPING:
			case STOPPED:
				return;
			case STARTED:
				break;
			case STARTING:
				Log.w(TAG, "stopPreview() - Stop while starting");
				break;
		}
		
		// change state
		this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPING);
		
		// stop capture
		switch(this.get(PROP_CAPTURE_STATE))
		{
			case STOPPED:
				break;
			case STOPPING:
				Log.w(TAG, "stopPreview() - Wait for capture stop");
				return;
			case STARTING:
				Log.w(TAG, "stopPreview() - Cancel capture");
				this.stopCaptureInternal();
				break;
			case STARTED:
				Log.w(TAG, "stopPreview() - Stop capture and wait for stop");
				this.stopCaptureInternal();
				return;
		}
		
		// stop capture session
		this.stopCaptureSession(false);
		
		/*
		// stop request
		if(m_CaptureSession != null)
		{
			try
			{
				m_CaptureSession.stopRepeating();
			} 
			catch(Throwable ex)
			{
				Log.e(TAG, "stopPreview() - Fail to stop preview request", ex);
			}
			this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
		}
		else
			Log.e(TAG, "stopPreview() - No capture session to stop");
		*/
	}
	
	
	// Get string represents this object.
	@Override
	public String toString()
	{
		return ("Camera2[ID=" + m_Id + ", Facing=" + m_LensFacing + "]");
	}
}
