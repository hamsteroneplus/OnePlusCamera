package com.oneplus.camera.media;

import com.oneplus.base.Handle;
import com.oneplus.base.component.Component;

/**
 * Sound and audio manager interface.
 */
public interface AudioManager extends Component
{
	/**
	 * The audio stream for the phone ring.
	 */
	int STREAM_RING = android.media.AudioManager.STREAM_RING;
	/**
	 * Flag to indicate playing sound with loop.
	 */
	int FLAG_LOOP = 0x1;
	
	
	/**
	 * Load sound into memory.
	 * @param resId Sound resource ID.
	 * @param streamType Audio stream type:
	 * <ul>
	 *   <li>{@link #STREAM_RING}</li>
	 * </ul>
	 * @param flags Flags, reserved.
	 * @return Handle for the sound.
	 */
	Handle loadSound(int resId, int streamType, int flags);
	
	
	/**
	 * Play loaded sound.
	 * @param sound Sound handle returned from {@link #loadSound(int, int)}.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_LOOP}</li>
	 * </ul>
	 * @return Handle for the playing.
	 */
	Handle playSound(Handle sound, int flags);
}
