package com.oneplus.camera.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.camera.CameraThread;
import com.oneplus.camera.CameraThreadComponent;
import com.oneplus.camera.media.MediaEventArgs;

final class FileManagerImpl extends CameraThreadComponent implements FileManager {
	private FileManageerThread m_FileThread = null;
	private DecodeBitmapThread m_DecodeBitmapThread = null;
	private Handler m_FileHandler, m_DecodeBitmapHandler;
	private final int MESSAGE_SAVE_MEDIA = 1000;
	private final int MESSAGE_LOAD_IMAGES = 1001;
	private final int MESSAGE_GET_BITMAP = 1002;
	private final List<File> m_FileList = new ArrayList<>();
	private FileObserver m_FileObserver;
	// A managed pool of background decoder threads
	private ThreadPoolExecutor m_DecodeThreadPool;
	private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
	// Sets the amount of time an idle thread will wait for a task before
	// terminating
	private static final int KEEP_ALIVE_TIME = 1;
	// Sets the Time Unit to seconds
	private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;;
	// A queue of Runnables for the image decoding pool
	private BlockingQueue<Runnable> m_DecodeWorkQueue;

	private final File m_DefaultFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
			"100MEDIA");
	static final String[] IMAGE_FILTER = { ".jpg", };
	static final String[] VIDEO_FILTER = { ".mp4", };

	// Constructor
	FileManagerImpl(CameraThread cameraThread) {
		super("File manager", cameraThread, true);
	}

	/**
	 * Called when initializing component.
	 */
	protected void onInitialize() {
		// start file thread
		m_FileThread = new FileManageerThread("save media thread");
		m_FileThread.start();
		m_FileHandler = m_FileThread.getHandler();
		// start image thread
		m_DecodeBitmapThread = new DecodeBitmapThread("decode bitmap thread");
		m_DecodeBitmapThread.start();
		m_DecodeBitmapHandler = m_DecodeBitmapThread.getHandler();
		// observe file change
		m_FileHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_LOAD_IMAGES));
		m_FileObserver = new FileObserver(m_DefaultFolder.getAbsolutePath()) {

			@Override
			public void onEvent(int event, String file) {
				if (event == FileObserver.DELETE) {
					m_FileHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_LOAD_IMAGES, 1, 0));
				}
			}
		};
		m_FileObserver.startWatching(); // START OBSERVING

		/*
		 * Creates a work queue for the pool of Thread objects used for
		 * decoding, using a linked list queue that blocks when the queue is
		 * empty.
		 */
		m_DecodeWorkQueue = new LinkedBlockingQueue<Runnable>();
		m_DecodeThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
				m_DecodeWorkQueue);
	}

	/**
	 * Called when deinitializing component.
	 */
	protected void onDeinitialize() {
		super.onDeinitialize();
		m_FileThread.quitSafely();
		m_FileThread = null;
		m_FileHandler = null;
		m_FileList.clear();
		m_FileObserver.stopWatching();
		m_FileObserver = null;
	}

	@Override
	public Handle saveMedia(final MediaSaveTask task, final int flags) {
		verifyAccess();
		if (task != null && isRunningOrInitializing()) {
			m_FileHandler.sendMessageAtFrontOfQueue(Message.obtain(m_FileHandler, MESSAGE_SAVE_MEDIA, task));
		}

		return null;
	}

	@Override
	public List<File> getMediaFiles() {
		return new ArrayList<File>(m_FileList);
	}

	@Override
	public void setCurrent(int position){
		m_DecodeBitmapThread.m_Current = position;
	}
	
	@Override
	public void getBitmap(final String path, final int width, final int height, final PhotoCallback callback, final boolean isVertical, int position) {

		if(position == m_DecodeBitmapThread.m_Current){
			Log.d(TAG, "getBitmap: now");
			m_DecodeBitmapHandler.sendMessageAtFrontOfQueue(Message.obtain(m_FileHandler, MESSAGE_GET_BITMAP, width, height, 
					new BitmapArgs(position, path, isVertical, callback)));
		}else{
			Log.d(TAG, "getBitmap: later");
			m_DecodeBitmapHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_GET_BITMAP, width, height, 
					new BitmapArgs(position, path, isVertical, callback)));
		}
	}

	private class BitmapArgs {
		private int m_Position;
		private String m_Path;
		private PhotoCallback m_callback;
		private boolean m_IsVertical;

		BitmapArgs(int position, String path, boolean isVertical, PhotoCallback callback) {
			m_Position = position;
			m_Path = path;
			m_IsVertical = isVertical;
			m_callback = callback;
		}
		
		int getPosition() {
			return m_Position;
		}

		String getPath() {
			return m_Path;
		}

		PhotoCallback getCallback() {
			return m_callback;
		}
	}

	public Bitmap decodeBitmap(String path, int width, int height) {
		Bitmap bitmap = null;
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, width, height);
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		bitmap = BitmapFactory.decodeFile(path, options);

		return bitmap;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2
			// and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
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
			Looper looper = getLooper(); // will block until thread¡¦s Looper
											// object initialized
			m_Handler = new Handler(looper) {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					// process messages here
					case MESSAGE_SAVE_MEDIA: {
						MediaSaveTask task = (MediaSaveTask) msg.obj;
						// save file
						if (task.saveMediaToFile()) {
							m_FileList.add(0, new File(task.getFilePath()));
							notifyCameraThread(EVENT_MEDIA_FILE_SAVED, task);
							notifyCameraThread(EVENT_MEDIA_FILES_ADDED, task);
							// insert MediaStore
							if (task.insertToMediaStore()) {
								notifyCameraThread(EVENT_MEDIA_SAVED, task);
							} else {
								notifyCameraThread(EVENT_MEDIA_SAVE_FAILED, task);
							}
						} else {
							notifyCameraThread(EVENT_MEDIA_SAVE_FAILED, task);
						}
						break;
					}
					case MESSAGE_LOAD_IMAGES: {
						m_FileList.clear();
						if (m_DefaultFolder.exists()) {
							File[] files = m_DefaultFolder.listFiles(new FilenameFilter() {
								public boolean accept(File dir, String name) {
									boolean ret = false;
									for (String filter : IMAGE_FILTER) {
										if (name.toLowerCase().endsWith(filter)) {
											ret = true;
											break;
										}
									}
									for (String filter : VIDEO_FILTER) {
										if (name.toLowerCase().endsWith(filter)) {
											ret = true;
											break;
										}
									}
									return ret;
								}
							});
							if (files != null && files.length > 0) {
								Arrays.sort(files, new Comparator<File>() {
									public int compare(File f1, File f2) {
										return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
									}
								});
								
								m_FileList.addAll(Arrays.asList(files));
								Iterator<File> it = m_FileList.iterator();
								File fileItem;
								while (it.hasNext()) {
									fileItem = it.next();
									if (fileItem.length()==0) {
										it.remove();
									}
								}
							}
							if (msg.arg1 == 1) {
								notifyCameraThread(EVENT_MEDIA_FILES_RESET, EventArgs.EMPTY);
							}
						}
						break;
					}
					case MESSAGE_GET_BITMAP: {
						BitmapArgs args = (BitmapArgs) msg.obj;
						boolean isImage = false;
						for (String filter : IMAGE_FILTER) {
							if (args.getPath().toLowerCase().endsWith(filter)) {
								isImage = true;
								break;
							}
						}
						Bitmap bitmap;
						Boolean isVideo;
						if (isImage) {
							bitmap = decodeBitmap(args.getPath(), msg.arg1, msg.arg2);
							isVideo = false;
						} else {
							bitmap = ThumbnailUtils.createVideoThumbnail(args.getPath(),
									MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
							isVideo = true;
						}
						if (args.m_IsVertical) {
							Matrix matrix = new Matrix();

							matrix.postRotate(90);

							bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
						}

						args.getCallback().onBitmapLoad(ThumbnailUtils.extractThumbnail(bitmap, msg.arg1, msg.arg2), isVideo, false);
						break;
					}
					}
				}
			};
		}
	}

	class DecodeBitmapThread extends HandlerThread {
		private static final String TAG = "DecodeBitmapThread";
		private Handler m_Handler;
		private int m_Current;
		static final private int OFFSET = 2;

		public DecodeBitmapThread(String name) {
			super(name);
		}

		public Handler getHandler() {
			return m_Handler;
		}
		
		private boolean checkInterrupt(int position){
			boolean result = position > m_Current+OFFSET || position < Math.max(1, m_Current-OFFSET);
			if(result){
				Log.d(TAG, "checkInterrupt: position: " + position +" m_Current: " + m_Current);
			}
			return result;
		}

		@Override
		public void start() {
			super.start();
			Looper looper = getLooper(); // will block until thread¡¦s Looper
											// object initialized
			m_Handler = new Handler(looper) {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case MESSAGE_GET_BITMAP: {
						BitmapArgs args = (BitmapArgs) msg.obj;
						String path = args.getPath();
						PhotoCallback callback = args.getCallback();
						int position = args.getPosition();
						int width = msg.arg1;
						int height = msg.arg2;
						//
						boolean isImage = false;
						for (String filter : IMAGE_FILTER) {
							if (path.toLowerCase().endsWith(filter)) {
								isImage = true;
								break;
							}
						}
						Bitmap bitmap;
						Boolean isVideo;
						//
						if(checkInterrupt(position)){
							callback.onBitmapLoad(null, !isImage, true);
							return;
						}
						//
						if (isImage) {
							bitmap = decodeBitmap(path, width, height);
							isVideo = false;
						} else {
							bitmap = ThumbnailUtils.createVideoThumbnail(path,
									MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
							isVideo = true;
						}
						//
						if(checkInterrupt(position)){
							callback.onBitmapLoad(null, isVideo, true);
							return;
						}
						
						Log.d(TAG, ": " + position + " bitmap: " + bitmap + " path: "+path);
						//
						if (args.m_IsVertical && bitmap!=null) {
							Matrix matrix = new Matrix();
							matrix.postRotate(90);
							bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
						}
						callback.onBitmapLoad(ThumbnailUtils.extractThumbnail(bitmap, width, height), isVideo, false);
						break;
					}
					}
				}
			};
		}
	}
}
