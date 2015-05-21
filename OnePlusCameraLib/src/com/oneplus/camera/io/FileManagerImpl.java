package com.oneplus.camera.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
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
	private FileManageerThread m_Thread = null;
	private Handler m_FileHandler;
	private final int MESSAGE_SAVE_MEDIA = 1000;
	private final int MESSAGE_LOAD_IMAGES = 1001;
	private final int MESSAGE_GET_BITMAP = 1002;
	private final List<File> m_FileList = new ArrayList<>();
	private FileObserver m_FileObserver;
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
		m_Thread = new FileManageerThread("save media thread");
		m_Thread.start();
		m_FileHandler = m_Thread.getHandler();
		m_FileHandler.sendMessage(Message.obtain(m_FileHandler, MESSAGE_LOAD_IMAGES));
		m_FileObserver = new FileObserver(m_DefaultFolder.getAbsolutePath()) { // set
																				// up
																				// a
																				// file
																				// observer
																				// to
																				// watch
																				// this
																				// directory
																				// on
																				// sd
																				// card

			@Override
			public void onEvent(int event, String file) {
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
	protected void onDeinitialize() {
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
	public void getBitmap(final String path, final int width, final int height, final PhotoCallback callback) {

		m_FileHandler.sendMessage(Message
				.obtain(m_FileHandler, MESSAGE_GET_BITMAP, width, height, new BitmapArgs(path, callback)));
	}

	private class BitmapArgs {
		private String m_Path;
		private PhotoCallback m_callback;

		BitmapArgs(String path, PhotoCallback callback) {
			m_Path = path;
			m_callback = callback;
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

	public Bitmap scaleCenterCrop(Bitmap source, int newWidth, int newHeight) {
		if (source == null) {
			Log.e(TAG, "error:: Bitmap is null");
			return null;
		}
		if (newWidth <= 0) {
			Log.e(TAG, "error:: newWidth less or equal 0");
			return null;
		}
		if (newHeight <= 0) {
			Log.e(TAG, "error:: newHeight less or equal 0");
			return null;
		}
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		// Compute the scaling factors to fit the new height and width,
		// respectively.
		// To cover the final image, the final scaling will be the bigger
		// of these two.
		float xScale = (float) newWidth / sourceWidth;
		float yScale = (float) newHeight / sourceHeight;
		float scale = Math.max(xScale, yScale);

		// Now get the size of the source bitmap when scaled
		float scaledWidth = scale * sourceWidth;
		float scaledHeight = scale * sourceHeight;

		// Let's find out the upper left coordinates if the scaled bitmap
		// should be centered in the new size give by the parameters
		float left = (newWidth - scaledWidth) / 2;
		float top = (newHeight - scaledHeight) / 2;

		// The target rectangle for the new, scaled version of the source
		// bitmap will now
		// be
		RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

		// Finally, we create a new bitmap of the specified size and draw
		// our new,
		// scaled bitmap onto it.
		Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
		Canvas canvas = new Canvas(dest);
		canvas.drawBitmap(source, null, targetRect, null);

		return dest;
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
							notifyCameraThread(EVENT_MEDIA_FILES_ADDED, EventArgs.EMPTY);
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
											Log.d(TAG, "charles " + "name: " + name + "   filter: " + filter);
											ret = true;
											break;
										}
									}
									for (String filter : VIDEO_FILTER) {
										if (name.toLowerCase().endsWith(filter)) {
											Log.d(TAG, "charles " + "name: " + name + "   filter: " + filter);
											ret = true;
											break;
										}
									}
									return ret;
								}
							});
							Log.d(TAG, "charles " + files.length);
							if (files != null && files.length > 0) {
								Arrays.sort(files, new Comparator<File>() {
									public int compare(File f1, File f2) {
										return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
									}
								});
								m_FileList.addAll(Arrays.asList(files));
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
						if (isImage) {
							args.getCallback().onBitmapLoad(
									scaleCenterCrop(decodeBitmap(args.getPath(), msg.arg1, msg.arg2), msg.arg1, msg.arg2), false);
							break;
						} else {
							args.getCallback().onBitmapLoad(
									scaleCenterCrop(ThumbnailUtils.createVideoThumbnail(args.getPath(),
											MediaStore.Video.Thumbnails.FULL_SCREEN_KIND), msg.arg1, msg.arg2), true);
						}
					}
					}
				}
			};
		}
	}
}
