package com.oneplus.camera.io;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oneplus.base.Log;
import com.oneplus.camera.CameraCaptureEventArgs;
import com.oneplus.io.Path;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;

public class PhotoSaveTask extends MediaSaveTask {

	protected final String TAG;
	private final CameraCaptureEventArgs args;
	private final Context context;
	static private	int	suffix = 0;
	
	public PhotoSaveTask(Context context, CameraCaptureEventArgs e)
	{
		this.TAG = this.getClass().getSimpleName();
		this.context = context;
		this.args = e;
	}
	
	@Override
	protected String onGenerateFilePath() {
		File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA");
		if(!directory.exists() && !directory.mkdir())
		{
			Log.e(TAG, "onPictureReceived() - Fail to create " + directory.getAbsolutePath());
			return null;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		File file = new File(directory, "IMG_" + dateFormat.format(new Date()) + ".jpg");
		if(file.exists()){
			file = new File(directory, "IMG_" + dateFormat.format(new Date()) + "_" + suffix + ".jpg");
			suffix += 1;
		}else{
			suffix = 0;
		}
		Log.w(TAG, "onPictureReceived() - Write picture to " + file);
		return file.getAbsolutePath();
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
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.MediaColumns.DATA, filePath);
		return true;
	}

	@Override
	protected boolean onSaveToFile(String filePath) {
		Log.w(TAG, "onSaveToFile()");

		File file = new File(filePath);
		//
		try(FileOutputStream stream = new FileOutputStream(file))
		{
			stream.write(args.getPicture());
			Log.w(TAG, "onPictureReceived() - Picture saved");
		} 
		catch (Throwable ex)
		{
			Log.e(TAG, "onPictureReceived() - Fail to write " + file, ex);
			return false;
		}
		
		return true;
	}

}
