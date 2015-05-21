package com.oneplus.camera.media;

import java.util.Collections;
import java.util.List;

import android.util.Size;

import com.oneplus.base.Handle;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Media resolution manager interface.
 */
public interface ResolutionManager extends Component
{
	/**
	 * Flag to synchronize photo/video resolution between two resolution selectors.
	 */
	int FLAG_SYNC_RESOLUTION = 0x1;
	
	
	/**
	 * Read-only property to get current camera preview size for photo.
	 */
	PropertyKey<Size> PROP_PHOTO_PREVIEW_SIZE = new PropertyKey<>("PhotoPreviewSize", Size.class, ResolutionManager.class, new Size(0, 0));
	/**
	 * Property for current photo resolution.
	 */
	PropertyKey<Resolution> PROP_PHOTO_RESOLUTION = new PropertyKey<>("PhotoResolution", Resolution.class, ResolutionManager.class, 0, null);
	/**
	 * Read-only property for available photo resolutions.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Resolution>> PROP_PHOTO_RESOLUTION_LIST = new PropertyKey<List<Resolution>>("PhotoResolutionList", (Class)List.class, ResolutionManager.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property to get current camera preview size for video.
	 */
	PropertyKey<Size> PROP_VIDEO_PREVIEW_SIZE = new PropertyKey<>("VideoPreviewSize", Size.class, ResolutionManager.class, new Size(0, 0));
	/**
	 * Property for current video resolution.
	 */
	PropertyKey<Resolution> PROP_VIDEO_RESOLUTION = new PropertyKey<>("VideoResolution", Resolution.class, ResolutionManager.class, 0, null);
	/**
	 * Read-only property for available video resolutions.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Resolution>> PROP_VIDEO_RESOLUTION_LIST = new PropertyKey<List<Resolution>>("VideoResolutionList", (Class)List.class, ResolutionManager.class, Collections.EMPTY_LIST);
	
	
	/**
	 * Change photo or video resolution selector.
	 * @param selector Photo or video resolution selector.
	 * @param flags Flags :
	 * <ul>
	 *   <li>{@link #FLAG_SYNC_RESOLUTION}</li>
	 * </ul>
	 * When closing handle, you can use flags :
	 * <ul>
	 *   <li>{@link #FLAG_SYNC_RESOLUTION}</li>
	 * </ul>
	 * @return Handle to resolution selector.
	 */
	Handle setResolutionSelector(ResolutionSelector selector, int flags);
}
