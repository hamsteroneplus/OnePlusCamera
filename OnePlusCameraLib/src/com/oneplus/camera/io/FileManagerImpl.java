package com.oneplus.camera.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.media.MediaEventArgs;


final class FileManagerImpl extends CameraThreadComponent implements FileManager
{
	private FileManageerThread m_Thread = null ;
	private Handler m_FileHandler;
	private final int MESSAGE_SAVE_MEDIA = 1000;
	private final int MESSAGE_LOAD_IMAGES = 1001;
	private final List<File> m_FileList = new ArrayList<>();
	private FileObserver m_FileObserver;
	private final File m_DefaultFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA");

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
		m_Thread = new FileManageerThread("save media thread");
		m_Thread.start();
		m_FileHandler = m_Thread.getHandler();
		m_FileHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_LOAD_IMAGES));
		m_FileObserver = new FileObserver(m_DefaultFolder.getAbsolutePath()) { // set up a file observer to watch this directory on sd card

			@Override
			public void onEvent(int event, String file) {
				Log.d(TAG, "charles event: " + event + " file: " + file);
				if (event == FileObserver.DELETE) {
					m_FileHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_LOAD_IMAGES, 1, 0));
				}
			}
		};
		m_FileObserver.startWatching(); // START OBSERVING 
	}
	
	/**
	 * Called when deinitializing component.
	 */
	protected void onDeinitialize()
	{
		super.onDeinitialize();
		m_Thread.quitSafely();
		m_Thread = null;
		m_FileHandler = null;
		m_FileList.clear();
		m_FileObserver.stopWatching();
		m_FileObserver = null;
	}

	@Override
	public Handle saveMedia(final MediaSaveTask task, final int flags) {
		verifyAccess();
		if(task != null && isRunningOrInitializing()){
			m_FileHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_SAVE_MEDIA, task));
		}
		
		return null;
	}
	
	@Override
	public List<File> getMediaFiles() {
		return m_FileList;
	}
	
	private boolean notifyCameraThread(final EventKey<MediaEventArgs> event, final MediaSaveTask task) {
		return HandlerUtils.post(this, new Runnable() {

			@Override
			public void run() {
				raise(event, new MediaEventArgs(task));
			}
		});
	}

	private boolean notifyCameraThread(final EventKey<EventArgs> event, final EventArgs args) {
		return HandlerUtils.post(this, new Runnable() {

			@Override
			public void run() {
				raise(event, args);
			}
		});
	}
	
    class FileManageerThread extends HandlerThread {
    	private static final String TAG = "SaveMediaThread";
		private Handler m_Handler;

		public FileManageerThread(String name) {
			super(name);
		}

		public Handler getHandler() {
			return m_Handler;
		}

		@Override
		public void start() {
			super.start();
			Looper looper = getLooper(); // will block until thread¡¦s Looper object initialized 
			m_Handler = new Handler(looper) {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					// process messages here
						case MESSAGE_SAVE_MEDIA:
						{
							MediaSaveTask task = (MediaSaveTask) msg.obj;
							//save file
							if(task.saveMediaToFile()){
								m_FileList.add(0, new File(task.getFilePath()));
								notifyCameraThread(EVENT_MEDIA_FILE_SAVED, task);
								notifyCameraThread(EVENT_MEDIA_FILES_UPDATED,  EventArgs.EMPTY);
								//insert MediaStore
								if(task.insertToMediaStore()){
									notifyCameraThread(EVENT_MEDIA_SAVED, task);
								}else{
									notifyCameraThread(EVENT_MEDIA_SAVE_FAILED, task);
								}
							}else{
								notifyCameraThread(EVENT_MEDIA_SAVE_FAILED, task);
							}
							break;
						}
						case MESSAGE_LOAD_IMAGES:
						{
							m_FileList.clear();
							if(m_DefaultFolder.exists()){
							File[] files = m_DefaultFolder.listFiles();
								if(files != null && files.length > 0){
									m_FileList.addAll(Arrays.asList(m_DefaultFolder.listFiles()));	
								}
								if(msg.arg1 == 1){
									notifyCameraThread(EVENT_MEDIA_FILES_UPDATED,  EventArgs.EMPTY);
								}
							}
						}
					}
				}
			};
		}
	}
}

