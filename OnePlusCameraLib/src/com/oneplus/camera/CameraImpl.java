package com.oneplus.camera;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.oneplus.base.EventArgs;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;

class CameraImpl extends HandlerBaseObject implements Camera
{
	// Private fields
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
	private int m_CompletedFrameIndex = -1;
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
	private final String m_Id;
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
			Log.w(TAG, "onCaptureSequenceCompleted");
		}
	};
	private CaptureRequest m_PictureCaptureRequest;
	private ImageReader m_PictureReader;
	private Size m_PictureSize = new Size(0, 0);
	private Surface m_PictureSurface;
	private final CameraCaptureSession.CaptureCallback m_PreviewCaptureCallback = new CameraCaptureSession.CaptureCallback()
	{
	};
	private Surface m_PreviewSurface;
	private final Queue<CaptureResult> m_ReceivedCaptureCompletedResults = new LinkedList<>();
	private final Queue<CaptureResult> m_ReceivedCaptureStartedResults = new LinkedList<>();
	private final Queue<byte[]> m_ReceivedPictures = new LinkedList<>();
	private volatile State m_State = State.CLOSED;
	private int m_TargetCapturedFrameCount;
	private final List<Surface> m_TempSurfaces = new ArrayList<>();
	private Surface m_VideoSurface;
	
	
	// Constructor
	public CameraImpl(CameraManager cameraManager, String id, CameraCharacteristics cameraChar)
	{
		// call super
		super(true);
		
		// save characteristics
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
		
		// enable logs
		this.enablePropertyLogs(PROP_CAPTURE_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_PREVIEW_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_STATE, LOG_PROPERTY_CHANGE);
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
				//
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
			}
			catch(Throwable ex)
			{}
		}
		if(builder == null)
		{
			try
			{
				builder = m_Device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
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
			builder.addTarget(m_PictureSurface);
			//
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
				m_CaptureSession.setRepeatingBurst(Arrays.asList(m_PictureCaptureRequest), m_PictureCaptureCallback, this.getHandler());
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
		this.stopCaptureSession();
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
		
		// change state
		this.changeState(State.CLOSED);
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
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_ID)
			return (TValue)m_Id;
		if(key == PROP_LENS_FACING)
			return (TValue)m_LensFacing;
		if(key == PROP_PICTURE_SIZE)
			return (TValue)m_PictureSize;
		if(key == PROP_STATE)
			return (TValue)m_State;
		if(key == PROP_VIDEO_SURFACE)
			return (TValue)m_VideoSurface;
		return super.get(key);
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
		
		// print logs
		Log.v(TAG, "onCaptureCompleted() - Index : ", (m_CompletedFrameIndex + 1));
		boolean success = (failure == null);
		if(!success)
			Log.e(TAG, "onCaptureCompleted() - Capture failed");
		
		// check index
		if(m_TargetCapturedFrameCount > 0 && m_CompletedFrameIndex >= (m_TargetCapturedFrameCount - 1))
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
		
		// complete capture
		this.onCaptureCompleted(result, failure, picture);
	}
	private void onCaptureCompleted(CaptureResult result, CaptureFailure failure, byte[] picture)
	{
		// update index
		++m_CompletedFrameIndex;
		
		// prepare completing capture
		OperationState captureState = this.get(PROP_CAPTURE_STATE);
		boolean failed = (picture == null || picture.length == 0);
		boolean frameCountReached = (m_TargetCapturedFrameCount > 0 && m_CompletedFrameIndex >= (m_TargetCapturedFrameCount - 1));
		if(captureState == OperationState.STARTED)
		{
			if(frameCountReached || failed)
			{
				if(failed)
					Log.e(TAG, "onCaptureCompleted() - Capture failed, start completing capture");
				else
					Log.w(TAG, "onCaptureCompleted() - Frame count reached, start completing capture");
				captureState = OperationState.STOPPING;
				this.setReadOnly(PROP_CAPTURE_STATE, captureState);
			}
		}
		
		// raise event
		if(!failed)
		{
			int pictureFormat = this.get(PROP_PICTURE_FORMAT);
			Size pictureSize = this.get(PROP_PICTURE_SIZE);
			this.raise(EVENT_PICTURE_RECEIVED, CameraCaptureEventArgs.obtain(m_CaptureHandle, m_CompletedFrameIndex, result, picture, pictureFormat, pictureSize));
		}
		else
			this.raise(EVENT_CAPTURE_FAILED, CameraCaptureEventArgs.obtain(m_CaptureHandle, m_CompletedFrameIndex, result));
		
		// complete capture
		if((frameCountReached || failed) && captureState == OperationState.STOPPING)
			this.onCaptureCompleted();
	}
	private void onCaptureCompleted()
	{
		Log.w(TAG, "onCaptureCompleted()");
		
		// clear result queues
		m_ReceivedCaptureStartedResults.clear();
		m_ReceivedCaptureCompletedResults.clear();
		m_ReceivedPictures.clear();
		
		// reset state
		m_CaptureHandle = null;
		m_CompletedFrameIndex = -1;
		m_TargetCapturedFrameCount = 0;
		this.setReadOnly(PROP_CAPTURE_STATE, OperationState.STOPPED);
		
		// stop capture session
		if(m_CaptureSessionState == OperationState.STOPPING)
		{
			Log.w(TAG, "onCaptureCompleted() - Stop capture session");
			m_CaptureSessionState = OperationState.STARTED;
			this.stopCaptureSession();
		}
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
		
		// reset state
		m_PreviewSurface = null;
		m_CaptureSession = null;
		m_CaptureSessionState = OperationState.STOPPED;
		
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
		{
			Log.w(TAG, "onCaptureSessionConfigured() - Start preview for camera '" + m_Id + "'");
			try
			{
				CaptureRequest.Builder builder = m_Device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
				builder.addTarget(m_PreviewSurface);
				if(m_VideoSurface != null)
					builder.addTarget(m_VideoSurface);
				session.setRepeatingRequest(builder.build(), m_PreviewCaptureCallback, this.getHandler());
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STARTED);
			}
			catch(Throwable ex)
			{
				Log.e(TAG, "onCaptureSessionConfigured() - Fail to start preview for camera '" + m_Id + "'", ex);
				//
				this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STOPPED);
			}
		}
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
		
		// print logs
		Log.v(TAG, "onCaptureStarted() - Index : ", (m_CompletedFrameIndex + 1));
		
		// check index
		if(m_TargetCapturedFrameCount > 0 && m_CompletedFrameIndex >= (m_TargetCapturedFrameCount - 1))
		{
			Log.w(TAG, "onCaptureStarted() - Unexpected call-back, drop");
			return;
		}
		
		// raise event
		this.raise(EVENT_SHUTTER, CameraCaptureEventArgs.obtain(m_CaptureHandle, m_CompletedFrameIndex + 1, null));
	}
	
	
	// Called when camera open failed.
	private void onDeviceError(CameraDevice camera, int error, boolean disconnected)
	{
		// check state
		if(m_State != State.OPENING)
		{
			Log.w(TAG, "onDeviceError() - Current state is " + m_State);
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
		
		// complete
		m_Device = camera;
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
				Log.e(TAG, "onPictureReceived() - Capture state is " + captureState);
				return;
		}
		if(m_CaptureHandle == null)
		{
			Log.e(TAG, "onPictureReceived() - No capture handle");
			return;
		}
		
		// copy image
		byte[] picture = this.copyImage(image);
		
		Log.v(TAG, "onPictureReceived() - Index : ", (m_CompletedFrameIndex + 1), ", picture buffer size : ", picture.length);
		
		// check index
		if(m_TargetCapturedFrameCount > 0 && m_CompletedFrameIndex >= (m_TargetCapturedFrameCount - 1))
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
		
		// complete capture
		this.onCaptureCompleted(captureResult, null, picture);
	}
	
	
	// Called when releasing object.
	@Override
	protected void onRelease()
	{
		// change state
		if(m_State == State.CLOSED)
			this.changeState(State.UNAVAILABLE);
		
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
	private Surface prepareSurface(Object receiver, Size captureSize)
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
	
	
	// Set property value.
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_PICTURE_SIZE)
			return this.setPictureSize((Size)value);
		if(key == PROP_VIDEO_SURFACE)
			return this.setVideoSurface((Surface)value);
		return super.set(key, value);
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
			this.stopCaptureSession();
			this.startCaptureSession();
		}
		
		// complete
		return true;
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
				Log.w(TAG, "setVideoSurface() - Stop preview to change video surface");
				this.stopPreview(0);
				needRestartPreview = true;
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
		m_PreviewSurface = this.prepareSurface(this.get(PROP_PREVIEW_RECEIVER), new Size(1280, 720));
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
		
		// prepare video surface
		if(m_VideoSurface != null)
		{
			Log.v(TAG, "startCaptureSession() - Video surface : ", m_VideoSurface);
			surfaces.add(m_VideoSurface);
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
			if(!this.startCaptureSession())
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
	
	
	//
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
				m_CaptureSession.abortCaptures();
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
	private void stopCaptureSession()
	{
		// check state
		switch(m_CaptureSessionState)
		{
			case STOPPED:
			case STOPPING:
				return;
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
				this.onCaptureCompleted();
				break;
			case STOPPING:
				Log.w(TAG, "stopCaptureSession() - Wait for capture completion");
				m_CaptureSessionState = OperationState.STOPPING;
				return;
		}
		
		// reset state
		m_CaptureSessionState = OperationState.STOPPING;
		m_CaptureSession.close();
		//this.onCaptureSessionClosed(m_CaptureSession);
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
		
		// stop session
		this.stopCaptureSession();
	}
	
	
	// Get string represents this object.
	@Override
	public String toString()
	{
		return ("Camera2[ID=" + m_Id + ", Facing=" + m_LensFacing + "]");
	}
}
