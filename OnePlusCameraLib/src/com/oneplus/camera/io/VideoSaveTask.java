package com.oneplus.camera.io;

import com.oneplus.base.Log;
import com.oneplus.io.Path;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.Media;

public class VideoSaveTask extends MediaSaveTask {

	protected final String TAG;
	private final String filePath;
	private final Context context;
	
	public VideoSaveTask(Context context, String filePath)
	{
		this.TAG = this.getClass().getSimpleName();
		this.context = context;
		this.filePath = filePath;
	}
	
	@Override
	protected String onGenerateFilePath() {
		return filePath;
	}

	@Override
	protected Uri onInsertToMediaStore(String filePath, ContentValues preparedValues) {
		Log.d(TAG, "onInsertToMediaStore: " + " filePath: " + filePath + " preparedValues: " + preparedValues);
		return context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, preparedValues);
	}

	@Override
	protected boolean onPrepareMediaStoreValues(String filePath, ContentValues values) {
		values.put(Media.TITLE, Path.getFileNameWithoutExtension(filePath));
		values.put(Media.DESCRIPTION, Path.getFileName(filePath));
		values.put(Video.Media.MIME_TYPE, "video/mp4");
		values.put(MediaStore.MediaColumns.DATA, filePath);
		return true;
	}

	// no need to save file for video
	@Override
	protected boolean onSaveToFile(String filePath) {
		return true;
	}
}
