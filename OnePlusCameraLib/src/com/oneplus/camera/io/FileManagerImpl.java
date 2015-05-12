package com.oneplus.camera.io;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.media.MediaEventArgs;


final class FileManagerImpl extends CameraThreadComponent implements FileManager
{
	SaveMediaThread m_Thread = null ;
	Handler m_SaveHandler;

	// Constructor
	FileManagerImpl(CameraThread cameraThread)
	{
		super("File manager", cameraThread, true);
	}
	
	/**
	 * Called when initializing component.
	 */
	protected void onInitialize()
	{
		m_Thread = new SaveMediaThread("save media thread");
		m_Thread.start();
		m_SaveHandler = m_Thread.getHandler();
	}
	
	/**
	 * Called when deinitializing component.
	 */
	protected void onDeinitialize()
	{
		super.onDeinitialize();
		m_Thread.quitSafely();
		m_Thread = null;
		m_SaveHandler = null;
	}

	@Override
	public Handle saveMedia(final MediaSaveTask task, final int flags) {
		verifyAccess();
		if(task != null && isRunningOrInitializing()){
			m_SaveHandler.post(new Runnable() {
				public void run() {
					//save file
					if(task.saveMediaToFile()){
						notifyCameraThread(EVENT_MEDIA_FILE_SAVED, task);
						//insert MediaStore
						if(task.insertToMediaStore()){
							notifyCameraThread(EVENT_MEDIA_SAVED, task);
						}else{
							notifyCameraThread(EVENT_MEDIA_SAVE_FAILED, task);
						}
					}else{
						notifyCameraThread(EVENT_MEDIA_SAVE_FAILED, task);
					}
				}
			});
		}
		
		return null;
	}
	
	private boolean notifyCameraThread(final EventKey<MediaEventArgs> event, final MediaSaveTask task){
		return HandlerUtils.post(this, new Runnable(){

			@Override
			public void run() {
				raise(event,  new MediaEventArgs(task));
			}});
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
}

