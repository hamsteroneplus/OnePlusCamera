package com.oneplus.camera;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oneplus.base.EventArgs;
import com.oneplus.base.Handle;
import com.oneplus.camera.Camera.MeteringRect;

/**
 * Data for camera metering related events.
 */
public class MeteringEventArgs extends EventArgs
{
	// Constants.
	private static final int POOL_SIZE = 8;
	
	
	// Private static fields.
	private static final ArrayDeque<MeteringEventArgs> POOL = new ArrayDeque<>(POOL_SIZE);
	
	
	// Private fields.
	private volatile Handle m_Handle;
	private volatile boolean m_IsFreeInstance;
	private volatile boolean m_IsSuccessful;
	private final List<MeteringRect> m_ReadOnlyRegions;
	private final List<MeteringRect> m_Regions = new ArrayList<>();
	
	
	// Constructor.
	private MeteringEventArgs()
	{
		m_ReadOnlyRegions = Collections.unmodifiableList(m_Regions);
	}
	
	
	// Create clone.
	@Override
	public MeteringEventArgs clone()
	{
		return obtain(m_Handle, m_Regions, m_IsSuccessful);
	}
	
	
	/**
	 * Get related handle.
	 * @return Handle.
	 */
	public final Handle getHandle()
	{
		return m_Handle;
	}
	
	
	/**
	 * Get metering regions.
	 */
	public final List<MeteringRect> getRegions()
	{
		return m_ReadOnlyRegions;
	}
	
	
	/**
	 * Check whether metering is successful or not.
	 * @return Whether metering is successful or not.
	 */
	public final boolean isSuccessful()
	{
		return m_IsSuccessful;
	}
	
	
	/**
	 * Obtain an available instance.
	 * @param handle Related handle.
	 * @param regions Metering regions.
	 * @param isSuccessful Whether metering is successful or not.
	 * @return {@link MeteringEventArgs} instance.
	 */
	public static synchronized MeteringEventArgs obtain(Handle handle, List<MeteringRect> regions, boolean isSuccessful)
	{
		MeteringEventArgs e = POOL.pollLast();
		if(e != null)
			e.m_IsFreeInstance = false;
		else
			e = new MeteringEventArgs();
		if(regions != null)
		{
			for(int i = regions.size() - 1 ; i >= 0 ; --i)
				e.m_Regions.add(regions.get(i));
		}
		e.m_Handle = handle;
		e.m_IsSuccessful = isSuccessful;
		return e;
	}
	
	
	/**
	 * Put instance back to pool for future usage.
	 */
	public final void recycle()
	{
		synchronized(MeteringEventArgs.class)
		{
			if(!m_IsFreeInstance)
			{
				m_IsFreeInstance = true;
				m_Regions.clear();
				m_Handle = null;
			}
			if(POOL.size() < POOL_SIZE)
				POOL.addLast(this);
		}
	}
}
