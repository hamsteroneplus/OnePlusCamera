package com.oneplus.camera.io;

import java.io.File;
import java.util.List;

import android.graphics.Bitmap;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.component.Component;
import com.oneplus.camera.media.MediaEventArgs;

/**
 * File manager interface.
 */
public interface FileManager extends Component
{
	/**
	 * Event raised when media file deleted or initial.
	 */
	EventKey<EventArgs> EVENT_MEDIA_FILES_RESET = new EventKey<>("MediaFileUpdated", EventArgs.class, FileManager.class);
	/**
	 * Event raised when media file added.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_FILE_ADDED = new EventKey<>("MediaFileUpdated", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised when media file saving completed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_FILE_SAVED = new EventKey<>("MediaFileSaved", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised when media file saving completed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_FILE_DELETED = new EventKey<>("MediaFileSaved", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised when media saving cancelled.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_SAVE_CANCELLED = new EventKey<>("MediaSaveCancelled", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised when media saving process failed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_SAVE_FAILED = new EventKey<>("MediaSaveFailed", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised after media saving process completed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_SAVED = new EventKey<>("MediaSaved", MediaEventArgs.class, FileManager.class);
	
	public interface PhotoCallback
	{
		/**
		 * Called when bitmap load.
		 */
		void onBitmapLoad(Bitmap bitmap, boolean isVideo, boolean isIntrrupt);
	}
	
	void setCurrent(int position);
	
	void getBitmap(final String path, final int width, final int height, final PhotoCallback callback, int position);
	/**
	 * Start media saving asynchronously.
	 * @param task Media save task.
	 * @param flags Flags, reserved.
	 * @return Handle to media saving.
	 */
	Handle saveMedia(MediaSaveTask task, int flags);
	
	boolean deleteFile(String path, boolean notify);
	/**
	 * get all medias asynchronously.
	 * @return all medias.
	 */
	List<File> getMediaFiles();
}
