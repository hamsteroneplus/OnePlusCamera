package com.oneplus.camera;

import java.util.Collections;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.util.Size;
import android.view.Surface;

import com.oneplus.base.BaseObject;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerObject;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.Rotation;
import com.oneplus.util.AspectRatio;

/**
 * Camera device interface.
 */
public interface Camera extends BaseObject, HandlerObject
{
	/**
	 * Property to get or set AE regions.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<MeteringRect>> PROP_AE_REGIONS = new PropertyKey<List<MeteringRect>>("AeRegions", (Class)List.class, Camera.class, PropertyKey.FLAG_NOT_NULL, Collections.EMPTY_LIST);
	/**
	 * Property to get or set AF regions.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<MeteringRect>> PROP_AF_REGIONS = new PropertyKey<List<MeteringRect>>("AfRegions", (Class)List.class, Camera.class, PropertyKey.FLAG_NOT_NULL, Collections.EMPTY_LIST);
	/**
	 * Read-only property to check current capture state.
	 */
	PropertyKey<OperationState> PROP_CAPTURE_STATE = new PropertyKey<>("CaptureState", OperationState.class, Camera.class, OperationState.STOPPED);
	/**
	 * Property to get or set flash mode.
	 */
	PropertyKey<FlashMode> PROP_FLASH_MODE = new PropertyKey<>("FlashMode", FlashMode.class, Camera.class, PropertyKey.FLAG_NOT_NULL, FlashMode.AUTO);
	/**
	 * Property to get or set focus mode.
	 */
	PropertyKey<FocusMode> PROP_FOCUS_MODE = new PropertyKey<>("FocusMode", FocusMode.class, Camera.class, PropertyKey.FLAG_NOT_NULL, FocusMode.DISABLED);
	/**
	 * Read-only property to get available focus modes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<FocusMode>> PROP_FOCUS_MODES = new PropertyKey<List<FocusMode>>("FocusModes", (Class)List.class, Camera.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property to check current focus state.
	 */
	PropertyKey<FocusState> PROP_FOCUS_STATE = new PropertyKey<>("FocusState", FocusState.class, Camera.class, FocusState.INACTIVE);
	/**
	 * Read-only property to check whether flash is supported on this camera or not.
	 */
	PropertyKey<Boolean> PROP_HAS_FLASH = new PropertyKey<>("HasFlash", Boolean.class, Camera.class, false);
	/**
	 * Read-only property for camera ID.
	 */
	PropertyKey<String> PROP_ID = new PropertyKey<>("ID", String.class, Camera.class, "");
	/**
	 * Read-only property to check whether burst capture is supported or not.
	 */
	PropertyKey<Boolean> PROP_IS_BURST_CAPTURE_SUPPORTED = new PropertyKey<>("IsBurstCaptureSupported", Boolean.class, Camera.class, false);
	/**
	 * Read-only property to check whether manual sensor control is supported or not.
	 */
	PropertyKey<Boolean> PROP_IS_MANUAL_CONTROL_SUPPORTED = new PropertyKey<>("IsManualControlSupported", Boolean.class, Camera.class, false);
	/**
	 * Read-only property to check whether RAW capture is supported or not.
	 */
	PropertyKey<Boolean> PROP_IS_RAW_CAPTURE_SUPPORTED = new PropertyKey<>("IsRawCaptureSupported", Boolean.class, Camera.class, false);
	/**
	 * Read-only property to check whether first preview frame is received or not.
	 */
	PropertyKey<Boolean> PROP_IS_PREVIEW_RECEIVED = new PropertyKey<>("IsPreviewReceived", Boolean.class, Camera.class, false);
	/**
	 * Property to get or set whether camera is in recording mode or not.
	 */
	PropertyKey<Boolean> PROP_IS_RECORDING_MODE = new PropertyKey<>("IsRecordingMode", Boolean.class, Camera.class, PropertyKey.FLAG_NOT_NULL, false);
	/**
	 * Read-only property for camera device facing.
	 */
	PropertyKey<LensFacing> PROP_LENS_FACING = new PropertyKey<>("LensFacing", LensFacing.class, Camera.class, LensFacing.BACK);
	/**
	 * Read-only property to check maximum number of AE regions supported by camera.
	 */
	PropertyKey<Integer> PROP_MAX_AE_REGION_COUNT = new PropertyKey<>("MaxAeRegionCount", Integer.class, Camera.class, 0);
	/**
	 * Read-only property to check maximum number of AF regions supported by camera.
	 */
	PropertyKey<Integer> PROP_MAX_AF_REGION_COUNT = new PropertyKey<>("MaxAfRegionCount", Integer.class, Camera.class, 0);
	/**
	 * Property to get or set captured picture format defined in {@link ImageFormat}.
	 */
	PropertyKey<Integer> PROP_PICTURE_FORMAT = new PropertyKey<>("PictureFormat", Integer.class, Camera.class, ImageFormat.JPEG);
	/**
	 * Property to get or set captured picture rotation.
	 */
	PropertyKey<Rotation> PROP_PICTURE_ROTATION = new PropertyKey<>("PictureRotation", Rotation.class, Camera.class, PropertyKey.FLAG_NOT_NULL, Rotation.LANDSCAPE);
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
	 * Property to get or set preview size.
	 */
	PropertyKey<Size> PROP_PREVIEW_SIZE = new PropertyKey<>("PreviewSize", Size.class, Camera.class, PropertyKey.FLAG_NOT_NULL, new Size(0, 0));
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
	 * Read-only property to get sensor aspect ratio.
	 */
	PropertyKey<AspectRatio> PROP_SENSOR_RATIO = new PropertyKey<>("SensorRatio", AspectRatio.class, Camera.class, AspectRatio.UNKNOWN);
	/**
	 * Read-only property to get sensor size in pixels.
	 */
	PropertyKey<Size> PROP_SENSOR_SIZE = new PropertyKey<>("SensorSize", Size.class, Camera.class, new Size(0, 0));
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
	 * Event raised when receiving preview frame.
	 */
	EventKey<CameraCaptureEventArgs> EVENT_PREVIEW_RECEIVED = new EventKey<>("PreviewReceived", CameraCaptureEventArgs.class, Camera.class);
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
	 * Rectangle area for AE and AF.
	 */
	public static class MeteringRect
	{
		// Private fields
		private final RectF m_Rect;
		private final float m_Weight;
		
		/**
		 * Initialize new MeteringRect instance.
		 * @param rect Normalized rectangle area.
		 * @param weight Weight in [0, 1], or {@link Float#NaN} to ignore this area.
		 */
		public MeteringRect(RectF rect, float weight)
		{
			m_Rect = new RectF(rect);
			if(Float.isNaN(weight))
				m_Weight = Float.NaN;
			else if(weight >= 0 && weight <= 1)
				m_Weight = weight;
			else
				throw new IllegalArgumentException("Weight must be a value in [0, 1]");
		}
		
		
		/**
		 * Initialize new MeteringRect instance.
		 * @param left Normalized left bound of rectangle area.
		 * @param top Normalized top bound of rectangle area.
		 * @param right Normalized right bound of rectangle area.
		 * @param bottom Normalized bottom bound of rectangle area.
		 * @param weight Weight in [0, 1], or {@link Float#NaN} to ignore this area.
		 */
		public MeteringRect(float left, float top, float right, float bottom, float weight)
		{
			m_Rect = new RectF(left, top, right, bottom);
			if(Float.isNaN(weight))
				m_Weight = Float.NaN;
			else if(weight >= 0 && weight <= 1)
				m_Weight = weight;
			else
				throw new IllegalArgumentException("Weight must be a value in [0, 1]");
		}
		
		
		/**
		 * Get bottom bound of rectangle area.
		 * @return Normalized bottom bound of rectangle area.
		 */
		public final float getBottom()
		{
			return m_Rect.bottom;
		}
		
		
		/**
		 * Get left bound of rectangle area.
		 * @return Normalized left bound of rectangle area.
		 */
		public final float getLeft()
		{
			return m_Rect.left;
		}
		
		
		/**
		 * Get rectangle area.
		 * @return Normalized rectangle area.
		 */
		public final RectF getRect()
		{
			return new RectF(m_Rect);
		}
		
		
		/**
		 * Get right bound of rectangle area.
		 * @return Normalized right bound of rectangle area.
		 */
		public final float getRight()
		{
			return m_Rect.right;
		}
		
		
		/**
		 * Get top bound of rectangle area.
		 * @return Normalized top bound of rectangle area.
		 */
		public final float getTop()
		{
			return m_Rect.top;
		}
		
		
		/**
		 * Get weight.
		 * @return Weight in [0, 1], or {@link Float#NaN} to ignore this area.
		 */
		public final float getWeight()
		{
			return m_Weight;
		}
		
		
		/**
		 * Check whether rectangle is ignorable or not.
		 * @return Whether rectangle is ignorable or not.
		 */
		public final boolean isIgnorable()
		{
			return (m_Rect.isEmpty()
					|| m_Rect.left > 1
					|| m_Rect.top > 1
					|| m_Rect.right < 0
					|| m_Rect.bottom < 0
					|| Float.isNaN(m_Weight));
		}
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
	 * Start auto focus.
	 * @param flags Flags, reserved.
	 * @return Whether auto focus starts successfully or not.
	 */
	boolean startAutoFocus(int flags);
	
	
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
