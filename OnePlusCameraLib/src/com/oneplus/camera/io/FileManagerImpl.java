package com.oneplus.camera.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Environment;
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
	private FileManageerThread m_Thread = null ;
	private Handler m_FileHandler;
	private final int MESSAGE_SAVE_MEDIA = 1000;
	private final int MESSAGE_LOAD_IMAGES = 1001;
	private final List<File> m_FileList = new ArrayList<>();

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
	
	private boolean notifyCameraThread(final EventKey<MediaEventArgs> event, final MediaSaveTask task){
		return HandlerUtils.post(this, new Runnable(){

			@Override
			public void run() {
				raise(event,  new MediaEventArgs(task));
			}});
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
								notifyCameraThread(EVENT_MEDIA_FILE_SAVED, task);
								m_FileList.add(new File(task.getFilePath()));
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
							File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA");      
							m_FileList.addAll(Arrays.asList(directory.listFiles()));
						}
					}
				}
			};
		}
	}
}

