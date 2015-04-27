package com.oneplus.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.oneplus.base.EventArgs;
import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyKey;

class CameraImpl extends HandlerBaseObject implements Camera
{
	// Private fields
	private CameraCaptureSession m_CaptureSession;
	private final CameraCaptureSession.StateCallback m_CaptureSessionCallback = new CameraCaptureSession.StateCallback()
	{
		@Override
		public void onClosed(CameraCaptureSession session)
		{
			onCaptureSessionClosed(session);
		};
		
		@Override
		public void onConfigured(CameraCaptureSession session)
		{
			onCaptureSessionConfigured(session);
		}
		
		@Override
		public void onConfigureFailed(CameraCaptureSession session)
		{
			//
		}
	};
	private OperationState m_CaptureSessionState = OperationState.STOPPED;
	private final CameraCharacteristics m_Characteristics;
	private final CameraManager m_CameraManager;
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
	private final CameraCaptureSession.CaptureCallback m_PreviewCaptureCallback = new CameraCaptureSession.CaptureCallback()
	{
	};
	private Surface m_PreviewSurface;
	private volatile State m_State = State.CLOSED;
	private final List<Surface> m_TempSurfaces = new ArrayList<>();
	
	
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
		this.setReadOnly(PROP_PREVIEW_SIZES, Arrays.asList(streamConfigMap.getOutputSizes(ImageFormat.NV21)));
		
		// get picture sizes
		this.setReadOnly(PROP_PICTURE_SIZES, Arrays.asList(streamConfigMap.getOutputSizes(ImageFormat.JPEG)));
		
		// enable logs
		this.enablePropertyLogs(PROP_PREVIEW_STATE, LOG_PROPERTY_CHANGE);
		this.enablePropertyLogs(PROP_STATE, LOG_PROPERTY_CHANGE);
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
		
		// close device
		if(m_Device != null)
		{
			this.close(m_Device);
			m_Device = null;
		}
		
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
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_ID)
			return (TValue)m_Id;
		if(key == PROP_LENS_FACING)
			return (TValue)m_LensFacing;
		if(key == PROP_STATE)
			return (TValue)m_State;
		return super.get(key);
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
		
		// reset state
		m_PreviewSurface = null;
		m_CaptureSession = null;
		m_CaptureSessionState = OperationState.STOPPED;
		
		// release temporary Surfaces
		if(!m_TempSurfaces.isEmpty())
		{
			for(int i = m_TempSurfaces.size() - 1 ; i >= 0 ; --i)
				m_TempSurfaces.get(i).release();
			m_TempSurfaces.clear();
		}
	}
	
	
	// Called when capture session configured.
	private void onCaptureSessionConfigured(CameraCaptureSession session)
	{
		// check state
		if(m_CaptureSessionState != OperationState.STARTING)
		{
			Log.e(TAG, "onCaptureSessionConfigured() - Current session state is " + m_CaptureSessionState);
			if(m_CaptureSessionState == OperationState.STOPPING)
				m_CaptureSession = session;
			session.close();
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
		else
		{
			Log.e(TAG, "prepareSurface() - Unsupported receiver : " + receiver);
			return null;
		}
		return surface;
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
			default:
				Log.e(TAG, "startCaptureSession() - Current session state is " + m_CaptureSessionState);
				return false;
		}
		
		// prepare Surfaces
		List<Surface> surfaces = new ArrayList<>();
		m_PreviewSurface = this.prepareSurface(this.get(PROP_PREVIEW_RECEIVER), new Size(1280, 720));
		if(m_PreviewSurface == null)
		{
			Log.e(TAG, "startCaptureSession() - Fail to prepare Surface for preview");
			return false;
		}
		surfaces.add(m_PreviewSurface);
		
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
		if(m_State != State.OPENED)
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
		if(!this.startCaptureSession())
		{
			Log.e(TAG, "startPreview() - Fail to start capture session");
			return false;
		}
		
		// complete
		this.setReadOnly(PROP_PREVIEW_STATE, OperationState.STARTING);
		return true;
	}
	
	
	// Stop capture session.
	private void stopCaptureSession()
	{
		// stop session
		switch(m_CaptureSessionState)
		{
			case STOPPED:
			case STOPPING:
				return;
			case STARTED:
				Log.w(TAG, "stopCaptureSession() - Stop capture session for camera '" + m_Id + "'");
				m_CaptureSessionState = OperationState.STOPPING;
				m_CaptureSession.close();
				break;
			case STARTING:
				Log.w(TAG, "stopCaptureSession() - Stop while starting");
				m_CaptureSessionState = OperationState.STOPPING;
				break;
		}
	}
	
	
	// Get string represents this object.
	@Override
	public String toString()
	{
		return ("{ID=" + m_Id + ", Facing=" + m_LensFacing + "}");
	}
}
