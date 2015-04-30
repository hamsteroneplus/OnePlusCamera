package com.oneplus.camera.media;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.media.AudioAttributes;
import android.media.SoundPool;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.CameraThread;

final class AudioManagerImpl extends CameraComponent implements AudioManager
{
	// Private fields.
	private final SoundPool.OnLoadCompleteListener m_LoadCompleteListener = new SoundPool.OnLoadCompleteListener()
	{
		@Override
		public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
		{
			onSoundLoaded(soundPool, sampleId, status);
		}
	};
	private final Hashtable<Integer, List<SoundHandle>> m_SoundHandles = new Hashtable<>();
	private final Hashtable<Integer, SoundPool> m_SoundPools = new Hashtable<>();
	
	
	// Class for sound handle.
	private final class SoundHandle extends Handle
	{
		public boolean isLoaded;
		public List<StreamHandle> pendingStreams;
		public final int resourceId;
		public int soundId;
		public final int streamType;
		
		public SoundHandle(int resId, int streamType, int soundId)
		{
			super("Sound");
			this.resourceId = resId;
			this.streamType = streamType;
			this.soundId = soundId;
		}

		@Override
		protected void onClose(int flags)
		{
			unloadSound(this);
		}
	}
	
	
	// Class for stream handle.
	private final class StreamHandle extends Handle
	{
		public final int flags;
		public final SoundHandle sound;
		public int streamId;
		
		public StreamHandle(SoundHandle sound, int flags)
		{
			super("SoundStream");
			this.sound = sound;
			this.flags = flags;
		}

		@Override
		protected void onClose(int flags)
		{
			stopSound(this);
		}
	}
	
	
	// Constructor
	AudioManagerImpl(CameraActivity cameraActivity)
	{
		super("Camera Audio Manager", cameraActivity, false);
	}
	AudioManagerImpl(CameraThread cameraThread)
	{
		super("Camera Audio Manager", cameraThread, false);
	}
	
	
	// Get sound pool for given stream type.
	private SoundPool getSoundPool(int streamType, boolean createNew)
	{
		SoundPool soundPool = m_SoundPools.get(streamType);
		if(soundPool == null && createNew)
		{
			Log.v(TAG, "getSoundPool() - Create sound pool for stream type ", streamType);
			AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
			SoundPool.Builder soundPoolBuilder = new SoundPool.Builder();
			attrBuilder.setLegacyStreamType(streamType);
			attrBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
			attrBuilder.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT);
			soundPoolBuilder.setAudioAttributes(attrBuilder.build());
			soundPoolBuilder.setMaxStreams(4);
			soundPool = soundPoolBuilder.build();
			soundPool.setOnLoadCompleteListener(m_LoadCompleteListener);
			m_SoundPools.put(streamType, soundPool);
		}
		return soundPool;
	}
	
	
	// Load sound into memory.
	@Override
	public Handle loadSound(int resId, int streamType, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "loadSound() - Component is not running");
			return null;
		}
		
		// check loaded sound
		List<SoundHandle> handleList = m_SoundHandles.get(streamType);
		if(handleList != null)
		{
			for(int i = handleList.size() - 1 ; i >= 0 ; --i)
			{
				SoundHandle handle = handleList.get(i);
				if(handle.resourceId == resId)
					return handle;
			}
		}
		
		// get sound pool
		SoundPool soundPool = this.getSoundPool(streamType, true);
		
		// load sound
		int soundId = soundPool.load(getContext(), resId, 1);
		
		// create handle
		SoundHandle handle = new SoundHandle(resId, streamType, soundId);
		Log.v(TAG, "loadSound() - Resource : ", resId, ", handle : ", handle);
		if(handleList == null)
		{
			handleList = new ArrayList<>();
			m_SoundHandles.put(streamType, handleList);
		}
		handleList.add(handle);
		return handle;
	}
	
	
	// Deinitialize.
	@Override
	protected void onDeinitialize()
	{
		// release sound pools
		for(SoundPool soundPool : m_SoundPools.values())
			soundPool.release();
		m_SoundPools.clear();
		
		// clear handles
		m_SoundHandles.clear();
		
		// call super
		super.onDeinitialize();
	}
	
	
	// Called when sound loaded.
	private void onSoundLoaded(SoundPool soundPool, int soundId, int status)
	{
		// check stream type
		Integer streamType = null;
		for(Map.Entry<Integer, SoundPool> entry : m_SoundPools.entrySet())
		{
			if(entry.getValue() == soundPool)
			{
				streamType = entry.getKey();
				break;
			}
		}
		if(streamType == null)
		{
			Log.e(TAG, "onSoundLoaded() - Unknown sound pool");
			if(soundId != 0)
				soundPool.unload(soundId);
			return;
		}
		
		// find sound handle
		SoundHandle soundHandle = null;
		List<SoundHandle> handleList = m_SoundHandles.get(streamType);
		if(handleList != null)
		{
			for(int i = handleList.size() - 1 ; i >= 0 ; --i)
			{
				SoundHandle handle = handleList.get(i);
				if(handle.soundId == soundId)
				{
					soundHandle = handle;
					break;
				}
			}
		}
		if(soundHandle == null)
		{
			Log.e(TAG, "onSoundLoaded() - Unknown sound ID : " + soundId);
			if(soundId != 0)
				soundPool.unload(soundId);
			return;
		}
		
		Log.v(TAG, "onSoundLoaded() - Handle : ", soundHandle);
		
		// update state
		soundHandle.isLoaded = true;
		
		// play sound
		if(soundHandle.pendingStreams != null)
		{
			for(int i = soundHandle.pendingStreams.size() - 1 ; i >= 0 ; --i)
				this.playSound(soundHandle.pendingStreams.get(i));
			soundHandle.pendingStreams.clear();
		}
	}
	
	
	// Play sound.
	@Override
	public Handle playSound(Handle sound, int flags)
	{
		// check handle
		if(sound == null)
		{
			Log.e(TAG, "playSound() - Null handle");
			return null;
		}
		if(!(sound instanceof SoundHandle))
		{
			Log.e(TAG, "playSound() - Invalid handle");
			return null;
		}
		
		// check thread
		this.verifyAccess();
		
		// check sound
		SoundHandle soundHandle = (SoundHandle)sound;
		List<SoundHandle> handleList = m_SoundHandles.get(soundHandle.streamType);
		if(handleList == null || !handleList.contains(soundHandle))
		{
			Log.e(TAG, "playSound() - Invalid handle");
			return null;
		}
		
		// create handle
		StreamHandle handle = new StreamHandle(soundHandle, flags);
		
		// play sound later if sound is not loaded yet
		if(!soundHandle.isLoaded)
		{
			Log.w(TAG, "playSound() - Sound " + soundHandle + " is not loaded yet, play later");
			if(soundHandle.pendingStreams == null)
				soundHandle.pendingStreams = new ArrayList<>();
			soundHandle.pendingStreams.add(handle);
			return handle;
		}
		
		// play sound
		if(!this.playSound(handle))
			return null;
		return handle;
	}
	
	
	// Play sound.
	private boolean playSound(StreamHandle handle)
	{
		Log.v(TAG, "playSound() - Sound handle : ", handle.sound, ", stream handle : ", handle);
		
		// get sound pool
		SoundPool soundPool = this.getSoundPool(handle.sound.streamType, true);
		
		// play sound
		int streamId = soundPool.play(handle.sound.soundId, 1, 1, 1, ((handle.flags & FLAG_LOOP) == 0 ? 0 : -1), 1);
		if(streamId == 0)
		{
			Log.e(TAG, "playSound() - Fail to play sound " + handle.sound);
			return false;
		}
		handle.streamId = streamId;
		
		// complete
		return true;
	}
	
	
	// Stop playing sound.
	private void stopSound(StreamHandle handle)
	{
		// check thread
		this.verifyAccess();
		
		Log.v(TAG, "stopSound() - Handle : ", handle);
		
		// remove from pending streams
		SoundHandle soundHandle = handle.sound;
		if(soundHandle.pendingStreams != null && soundHandle.pendingStreams.remove(handle))
			return;
		
		// check sound state
		if(!soundHandle.isLoaded)
			return;
		
		// get sound pool
		SoundPool soundPool = this.getSoundPool(soundHandle.streamType, false);
		if(soundPool == null)
		{
			Log.w(TAG, "stopSound() - No sound pool to stop");
			return;
		}
		
		// stop stream
		if(handle.streamId != 0)
		{
			soundPool.stop(handle.streamId);
			handle.streamId = 0;
		}
	}
	
	
	// Unload sound.
	private void unloadSound(SoundHandle handle)
	{
		// check thread
		this.verifyAccess();
		
		Log.v(TAG, "unloadSound() - Handle : ", handle);
		
		// clear pending streams
		handle.pendingStreams = null;
		
		// get sound pool
		SoundPool soundPool = this.getSoundPool(handle.streamType, false);
		
		// unload
		if(handle.soundId != 0)
		{
			if(soundPool != null)
				soundPool.unload(handle.soundId);
			else
				Log.w(TAG, "unloadSound() - No sound pool to unload");
			handle.soundId = 0;
		}
		handle.isLoaded = false;
		
		// remove from handle list
		List<SoundHandle> handleList = m_SoundHandles.get(handle.streamType);
		if(handleList != null)
			handleList.remove(handle);
	}
}
