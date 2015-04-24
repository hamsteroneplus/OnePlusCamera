package com.oneplus.camera;

import java.util.Collections;
import java.util.List;

import android.util.Size;

import com.oneplus.base.BaseObject;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.PropertyKey;

/**
 * Camera device interface.
 */
public interface Camera extends BaseObject
{
	/**
	 * Read-only property for camera ID.
	 */
	PropertyKey<String> PROP_ID = new PropertyKey<>("ID", String.class, Camera.class, "");
	/**
	 * Read-only property for camera device facing.
	 */
	PropertyKey<LensFacing> PROP_LENS_FACING = new PropertyKey<>("LensFacing", LensFacing.class, Camera.class, LensFacing.BACK);
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
	 * Event raised when camera open cancelled.
	 */
	EventKey<EventArgs> EVENT_OPEN_CANCELLED = new EventKey<>("OpenCancelled", EventArgs.class, Camera.class);
	/**
	 * Event raised when camera open failed.
	 */
	EventKey<EventArgs> EVENT_OPEN_FAILED = new EventKey<>("OpenFailed", EventArgs.class, Camera.class);
	
	
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
	 * Represents camera related operation state.
	 */
	public enum OperationState
	{
		/**
		 * Starting.
		 */
		STARTING,
		/**
		 * Started.
		 */
		STARTED,
		/**
		 * Stopping.
		 */
		STOPPING,
		/**
		 * Stopped.
		 */
		STOPPED,
	}
	
	
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
}
