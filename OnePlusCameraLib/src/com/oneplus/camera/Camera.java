package com.oneplus.camera;

import java.util.Collections;
import java.util.List;

import android.graphics.ImageFormat;
import android.util.Size;
import android.view.Surface;

import com.oneplus.base.BaseObject;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerObject;
import com.oneplus.base.PropertyKey;

/**
 * Camera device interface.
 */
public interface Camera extends BaseObject, HandlerObject
{
	/**
	 * Read-only property to check current capture state.
	 */
	PropertyKey<OperationState> PROP_CAPTURE_STATE = new PropertyKey<>("CaptureState", OperationState.class, Camera.class, OperationState.STOPPED);
	/**
	 * Read-only property for camera ID.
	 */
	PropertyKey<String> PROP_ID = new PropertyKey<>("ID", String.class, Camera.class, "");
	/**
	 * Read-only property for camera device facing.
	 */
	PropertyKey<LensFacing> PROP_LENS_FACING = new PropertyKey<>("LensFacing", LensFacing.class, Camera.class, LensFacing.BACK);
	/**
	 * Property to get or set captured picture format defined in {@link ImageFormat}.
	 */
	PropertyKey<Integer> PROP_PICTURE_FORMAT = new PropertyKey<>("PictureFormat", Integer.class, Camera.class, ImageFormat.JPEG);
	/**
	 * Property to get or set captured picture size.
	 */
	PropertyKey<Size> PROP_PICTURE_SIZE = new PropertyKey<>("PictureSize", Size.class, Camera.class, new Size(0, 0));
	/**
	 * Read-only property to get all supported picture sizes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Size>> PROP_PICTURE_SIZES = new PropertyKey<List<Size>>("PictureSizes", (Class)List.class, Camera.class, Collections.EMPTY_LIST);
	/**
	 * Property to get or set preview receiver.
	 */
	PropertyKey<Object> PROP_PREVIEW_RECEIVER = new PropertyKey<>("PreviewReceiver", Object.class, Camera.class, 0, null);
	/**
	 * Read-only property to get all supported preview sizes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Size>> PROP_PREVIEW_SIZES = new PropertyKey<List<Size>>("PreviewSizes", (Class)List.class, Camera.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property to check current preview state.
	 */
	PropertyKey<OperationState> PROP_PREVIEW_STATE = new PropertyKey<>("PreviewState", OperationState.class, Camera.class, OperationState.STOPPED);
	/**
	 * Read-only property to check current camera device state.
	 */
	PropertyKey<State> PROP_STATE = new PropertyKey<>("State", State.class, Camera.class, State.CLOSED);
	/**
	 * Read-only property to get all supported video sizes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Size>> PROP_VIDEO_SIZES = new PropertyKey<List<Size>>("VideoSizes", (Class)List.class, Camera.class, Collections.EMPTY_LIST);
	/**
	 * Property to get or set {@link Surface} for video recording.
	 */
	PropertyKey<Surface> PROP_VIDEO_SURFACE = new PropertyKey<>("VideoSurface", Surface.class, Camera.class, 0, null);
	
	
	/**
	 * Event raised when photo capture failed.
	 */
	EventKey<CameraCaptureEventArgs> EVENT_CAPTURE_FAILED = new EventKey<>("CaptureFailed", CameraCaptureEventArgs.class, Camera.class);
	/**
	 * Event raised when unexpected camera error occurred.
	 */
	EventKey<EventArgs> EVENT_ERROR = new EventKey<>("Error", EventArgs.class, Camera.class);
	/**
	 * Event raised when camera open cancelled.
	 */
	EventKey<EventArgs> EVENT_OPEN_CANCELLED = new EventKey<>("OpenCancelled", EventArgs.class, Camera.class);
	/**
	 * Event raised when camera open failed.
	 */
	EventKey<EventArgs> EVENT_OPEN_FAILED = new EventKey<>("OpenFailed", EventArgs.class, Camera.class);
	/**
	 * Event raised when receiving captured picture.
	 */
	EventKey<CameraCaptureEventArgs> EVENT_PICTURE_RECEIVED = new EventKey<>("PictureReceived", CameraCaptureEventArgs.class, Camera.class);
	/**
	 * Event raised when start capturing picture.
	 */
	EventKey<CameraCaptureEventArgs> EVENT_SHUTTER = new EventKey<>("Shutter", CameraCaptureEventArgs.class, Camera.class);
	
	
	/**
	 * Represents camera device facing.
	 */
	public enum LensFacing
	{
		/**
		 * The camera device faces the opposite direction as the device's screen.
		 */
		BACK,
		/**
		 * The camera device faces the same direction as the device's screen.
		 */
		FRONT,
	}
	
	
	/**
	 * Camera device state.
	 */
	public enum State
	{
		/**
		 * Opening.
		 */
		OPENING,
		/**
		 * Opened and ready to use.
		 */
		OPENED,
		/**
		 * Closing.
		 */
		CLOSING,
		/**
		 * Closed.
		 */
		CLOSED,
		/**
		 * Unavailable.
		 */
		UNAVAILABLE,
	}
	
	
	/**
	 * Start photo capture.
	 * @param frameCount Target frame count, 1 for single shot; positive integer for limited burst; negative for unlimited burst.
	 * @param flags Flags, reserved.
	 * @return Handle for this capture.
	 */
	Handle capture(int frameCount, int flags);
	
	
	/**
	 * Start closing camera device.
	 * @param flags Flags, reserved.
	 */
	void close(int flags);
	
	
	/**
	 * Start opening camera device.
	 * @param flags Flags, reserved.
	 * @return Whether opening process successfully starts or not.
	 */
	boolean open(int flags);
	
	
	/**
	 * Start preview.
	 * @param flags Flags, reserved.
	 * @return Whether preview starts successfully or not.
	 */
	boolean startPreview(int flags);
	
	
	/**
	 * Stop preview.
	 * @param flags Flags, reserved.
	 */
	void stopPreview(int flags);
}
