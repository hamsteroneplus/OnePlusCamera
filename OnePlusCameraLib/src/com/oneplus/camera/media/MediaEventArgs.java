package com.oneplus.camera.media;

import android.net.Uri;

import com.oneplus.base.EventArgs;
import com.oneplus.camera.CaptureHandle;
import com.oneplus.camera.io.MediaSaveTask;

/**
 * Data for media related events.
 */
public class MediaEventArgs extends EventArgs
{
	// Private fields.
	private final CaptureHandle m_CaptureHandle;
	private final Uri m_ContentUri;
	private final String m_FilePath;
	
	
	/**
	 * Initialize new MediaEventArgs instance.
	 * @param captureHandle Related capture handle.
	 * @param filePath Media file path.
	 * @param contentUri Media content URI.
	 */
	public MediaEventArgs(CaptureHandle captureHandle, String filePath, Uri contentUri)
	{
		m_CaptureHandle = captureHandle;
		m_FilePath = filePath;
		m_ContentUri = contentUri;
	}
	
	
	/**
	 * Initialize new MediaEventArgs instance.
	 * @param saveTask Completed media save task.
	 */
	public MediaEventArgs(MediaSaveTask saveTask)
	{
		m_CaptureHandle = saveTask.getCaptureHandle();
		m_FilePath = saveTask.getFilePath();
		m_ContentUri = saveTask.getContentUri();
	}
	
	
	/**
	 * Get related capture handle.
	 * @return Capture handle.
	 */
	public final CaptureHandle getCaptureHandle()
	{
		return m_CaptureHandle;
	}
	
	
	/**
	 * Get content URI of media.
	 * @return Media content URI.
	 */
	public final Uri getContentUri()
	{
		return m_ContentUri;
	}
	
	
	/**
	 * Get file path of media.
	 * @return Media file path.
	 */
	public final String getFilePath()
	{
		return m_FilePath;
	}
}
