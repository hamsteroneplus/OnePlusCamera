package com.oneplus.camera;

import java.util.ArrayDeque;
import java.util.Queue;

import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureResult;
import android.util.Size;

import com.oneplus.base.EventArgs;
import com.oneplus.base.Handle;

/**
 * Data for capture related events.
 */
public class CameraCaptureEventArgs extends EventArgs
{
	// Constants
	private static final int POOL_SIZE = 8;
	
	
	// Static fields
	private static final Queue<CameraCaptureEventArgs> POOL = new ArrayDeque<>(POOL_SIZE);
	
	
	// Private fields
	private volatile CaptureResult m_CaptureResult;
	private volatile int m_FrameIndex;
	private volatile Handle m_Handle;
	private volatile boolean m_IsFreeInstance;
	private volatile byte[] m_Picture;
	private volatile int m_PictureFormat;
	private volatile Size m_PictureSize;
	
	
	// Constructor
	private CameraCaptureEventArgs()
	{}
	
	
	/**
	 * Get capture result.
	 * @return Capture result.
	 */
	public final CaptureResult getCaptureResult()
	{
		return m_CaptureResult;
	}
	
	
	/**
	 * Get zero-based frame index.
	 * @return Frame index.
	 */
	public final int getFrameIndex()
	{
		return m_FrameIndex;
	}
	
	
	/**
	 * Get capture handle returned from {@link Camera#capture(int, int)}.
	 * @return Capture handle.
	 */
	public final Handle getHandle()
	{
		return m_Handle;
	}
	
	
	/**
	 * Get related picture buffer.
	 * @return Picture buffer, or Null if there is no related picture.
	 */
	public final byte[] getPicture()
	{
		return m_Picture;
	}
	
	
	/**
	 * Get related picture format defined in {@link ImageFormat}.
	 * @return Picture format, or 0 if there is no related picture.
	 */
	public final int getPictureFormat()
	{
		return m_PictureFormat;
	}
	
	
	/**
	 * Get related picture size.
	 * @return Picture size, or Null if there is no related picture.
	 */
	public final Size getPictureSize()
	{
		return m_PictureSize;
	}
	
	
	/**
	 * Get an available CameraCaptureEventArgs instance.
	 * @param handle Handle returned from {@link Camera#capture(int, int)}.
	 * @param frameIndex Zero-based frame index.
	 * @param result Capture result.
	 * @return CameraCaptureEventArgs instance.
	 */
	public static synchronized CameraCaptureEventArgs obtain(Handle handle, int frameIndex, CaptureResult result)
	{
		return obtain(handle, frameIndex, result, null, 0, null);
	}
	
	
	/**
	 * Get an available CameraCaptureEventArgs instance.
	 * @param handle Handle returned from {@link Camera#capture(int, int)}.
	 * @param frameIndex Zero-based frame index.
	 * @param result Capture result.
	 * @param picture Picture buffer, or Null if there is no related picture.
	 * @param pictureFormat Picture format, or 0 if there is no related picture.
	 * @param pictureSize Picture size, or Null if there is no related picture.
	 * @return CameraCaptureEventArgs instance.
	 */
	public static synchronized CameraCaptureEventArgs obtain(Handle handle, int frameIndex, CaptureResult result, byte[] picture, int pictureFormat, Size pictureSize)
	{
		CameraCaptureEventArgs e = POOL.poll();
		if(e != null)
			e.m_IsFreeInstance = false;
		else
			e = new CameraCaptureEventArgs();
		e.m_Handle = handle;
		e.m_FrameIndex = frameIndex;
		e.m_CaptureResult = result;
		e.m_Picture = picture;
		e.m_PictureFormat = pictureFormat;
		e.m_PictureSize = pictureSize;
		return e;
	}
	
	
	/**
	 * Put instance back to pool.
	 */
	public void recycle()
	{
		synchronized(CameraCaptureEventArgs.class)
		{
			if(!m_IsFreeInstance && POOL.size() < POOL_SIZE)
			{
				m_Handle = null;
				m_FrameIndex = -1;
				m_CaptureResult = null;
				m_Picture = null;
				m_PictureFormat = 0;
				m_PictureSize = null;
				m_IsFreeInstance = true;
				POOL.add(this);
			}
		}
	}
}
