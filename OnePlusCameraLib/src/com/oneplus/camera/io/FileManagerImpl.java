package com.oneplus.camera.io;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.media.MediaEventArgs;


final class FileManagerImpl extends CameraThreadComponent implements FileManager
{
	SaveMediaThread thread = null ;
	Handler saveHandler;

	// Constructor
	FileManagerImpl(CameraThread cameraThread)
	{
		super("File manager", cameraThread, true);
		thread = new SaveMediaThread("save media thread");
		thread.start();
		saveHandler = thread.getHandler();
		FileManagerImpl.this.addHandler(EVENT_MEDIA_FILE_SAVED, new EventHandler<MediaEventArgs>(){

			@Override
			public void onEventReceived(EventSource source, EventKey<MediaEventArgs> key, MediaEventArgs e) {
				Log.d(TAG, "test");
				
			}});
	}
	
	/**
	 * Called when deinitializing component.
	 */
	protected void onDeinitialize()
	{
		super.onDeinitialize();
		thread.getLooper().quitSafely();
		thread = null;
		saveHandler = null;
	}

	@Override
	public Handle saveMedia(final MediaSaveTask task, final int flags) {
		
				
		saveHandler.post(new Runnable() {
			public void run() {
				//save file
				if(task.saveMediaToFile()){
					FileManagerImpl.this.getCameraThread().getHandler().post(new Runnable(){

					@Override
					public void run() {
						FileManagerImpl.this.raise(EVENT_MEDIA_FILE_SAVED,  new MediaEventArgs(task));
					}});
					//insert MediaStore
					if(task.insertToMediaStore()){
						FileManagerImpl.this.getCameraThread().getHandler().post(new Runnable(){

						@Override
						public void run() {
							FileManagerImpl.this.raise(EVENT_MEDIA_SAVED,  new MediaEventArgs(task));
						}});
					}else{
						FileManagerImpl.this.getCameraThread().getHandler().post(new Runnable(){

							@Override
							public void run() {
								FileManagerImpl.this.raise(EVENT_MEDIA_SAVE_FAILED,  new MediaEventArgs(task));
							}});
					}
				}else{
					FileManagerImpl.this.getCameraThread().getHandler().post(new Runnable(){

						@Override
						public void run() {
							FileManagerImpl.this.raise(EVENT_MEDIA_SAVE_FAILED,  new MediaEventArgs(task));
						}});
				}
			}
			});
		
		return null;
	}
	
	
}

 class SaveMediaThread extends HandlerThread {
	private static final String TAG = "SaveMediaThread";
	private Handler mHandler;

	public SaveMediaThread(String name) {
		super(name);
	}

	public Handler getHandler() {
		return mHandler;
	}

	@Override
	public void start() {
		super.start();
		Looper looper = getLooper(); // will block until thread¡¦s Looper object initialized 
		mHandler = new Handler(looper) {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				// process messages here
				}
			}
		};
	}
}