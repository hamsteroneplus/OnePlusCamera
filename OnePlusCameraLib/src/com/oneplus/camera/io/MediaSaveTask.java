package com.oneplus.camera.io;

import android.content.ContentValues;
import android.net.Uri;

import com.oneplus.base.Log;
import com.oneplus.camera.CaptureHandle;

/**
 * Base class for task to save single media.
 */
public abstract class MediaSaveTask
{
	/**
	 * Log tag.
	 */
	protected final String TAG;
	
	
	// Private fields.
	private volatile CaptureHandle m_CaptureHandle;
	private volatile Uri m_ContentUri;
	private volatile String m_FilePath;
	
	
	/**
	 * Initialize new MediaSaveTask instance.
	 */
	protected MediaSaveTask()
	{
		this.TAG = this.getClass().getSimpleName();
	}
	
	
	/**
	 * Initialize new MediaSaveTask instance.
	 * @param captureHandle Related media capture handle.
	 */
	protected MediaSaveTask(CaptureHandle captureHandle)
	{
		this();
		m_CaptureHandle = captureHandle;
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
	 * Get content URI of saved media.
	 * @return Media content URI.
	 */
	public final Uri getContentUri()
	{
		return m_ContentUri;
	}
	
	
	/**
	 * Get file path of saved media.
	 * @return Media file path.
	 */
	public final String getFilePath()
	{
		return m_FilePath;
	}
	
	
	/**
	 * Insert saved media to media store.
	 * @return Whether media is inserted to media store successfully or not.
	 */
	public final boolean insertToMediaStore()
	{
		// check file path
		if(m_FilePath == null)
		{
			Log.e(TAG, "insertToMediaStore() - No media file path");
			return false;
		}
		
		// prepare values
		ContentValues values = new ContentValues();
		try
		{
			if(!this.onPrepareMediaStoreValues(m_FilePath, values))
			{
				Log.e(TAG, "insertToMediaStore() - Fail to prepare values");
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "insertToMediaStore() - Fail to prepare values", ex);
			return false;
		}
		
		// insert
		try
		{
			m_ContentUri = this.onInsertToMediaStore(m_FilePath, values);
			if(m_ContentUri != null)
				Log.v(TAG, "insertToMediaStore() - Content URI : ", m_ContentUri);
			else
			{
				Log.e(TAG, "insertToMediaStore() - Fail to insert");
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "insertToMediaStore() - Fail to insert", ex);
			return false;
		}
		
		// complete
		return true;
	}
	
	
	/**
	 * Called when generating file path for media.
	 * @return Media file path.
	 */
	protected abstract String onGenerateFilePath();
	
	
	/**
	 * Called when inserting media to media store.
	 * @param filePath Media file path.
	 * @param preparedValues Prepared media store values.
	 * @return Media content URI.
	 */
	protected abstract Uri onInsertToMediaStore(String filePath, ContentValues preparedValues);
	
	
	/**
	 * Called before saving media to file.
	 * @param filePath Media file path.
	 * @return Whether operation performed successfully or not.
	 */
	protected boolean onPrepareFileSave(String filePath)
	{
		return true;
	}
	
	
	/**
	 * Called when preparing values for media store.
	 * @param filePath Media file path.
	 * @param values Prepared media store values.
	 * @return Whether operation performed successfully or not.
	 */
	protected abstract boolean onPrepareMediaStoreValues(String filePath, ContentValues values);
	
	
	/**
	 * Called when saving media to given file.
	 * @param filePath Media file path.
	 * @return Whether operation performed successfully or not.
	 */
	protected abstract boolean onSaveToFile(String filePath);
	
	
	/**
	 * Save media to file.
	 * @return Whether media is saved to file successfully or not.
	 */
	public final boolean saveMediaToFile()
	{
		// get file path
		try
		{
			m_FilePath = this.onGenerateFilePath();
			if(m_FilePath != null)
				Log.v(TAG, "saveMediaToFile() - File path : ", m_FilePath);
			else
			{
				Log.e(TAG, "saveMediaToFile() - No available file path");
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "saveMediaToFile() - No available file path", ex);
			return false;
		}
		
		// prepare
		try
		{
			if(!this.onPrepareFileSave(m_FilePath))
			{
				Log.e(TAG, "saveMediaToFile() - Fail to prepare media save");
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "saveMediaToFile() - Fail to prepare media save", ex);
			return false;
		}
		
		// save to file
		try
		{
			Log.v(TAG, "saveMediaToFile() - Save to file [start]");
			if(!this.onSaveToFile(m_FilePath))
			{
				Log.e(TAG, "saveMediaToFile() - Fail to save media to file");
				return false;
			}else{
				Log.v(TAG, "saveMediaToFile() - Save to file [end]");
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "saveMediaToFile() - Fail to save media to file", ex);
			return false;
		}
		// insert file to mediastore
		try
		{
			m_ContentUri = this.onInsertToMediaStore(m_FilePath, null);
			if (m_ContentUri == null || m_ContentUri.equals(Uri.EMPTY)) {
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "onInsertToMediaStore() - Fail to insert media to mediastore", ex);
			return false;
		}
		
		// complete
		return true;
	}

}
