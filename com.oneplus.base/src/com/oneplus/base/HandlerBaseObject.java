package com.oneplus.base;

import android.os.Handler;
import android.os.Message;

/**
 * Implementation of {@link BaseObject} and {@link HandlerObject}.
 */
public abstract class HandlerBaseObject extends BasicBaseObject implements HandlerObject
{
	// Private fields
	private final InternalHandler m_Handler;
	
	
	// Class for Handler
	private static final class InternalHandler extends Handler
	{
		private final String m_Tag;
		private volatile HandlerBaseObject m_Owner;
		
		public InternalHandler(HandlerBaseObject owner)
		{
			m_Owner = owner;
			m_Tag = owner.TAG;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			HandlerBaseObject owner = m_Owner;
			if(owner != null)
				owner.handleMessage(msg);
			else
				Log.e(m_Tag, "Owner released, drop message " + msg.what);
		}
		
		public void release()
		{
			m_Owner = null;
		}
	}
	
	
	/**
	 * Initialize new HandlerBaseObject instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected HandlerBaseObject(boolean hasHandler)
	{
		m_Handler = (hasHandler ? new InternalHandler(this) : null);
	}
	
	
	/**
	 * Initialize new HandlerBaseObject instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 * @param tag Log tag.
	 */
	protected HandlerBaseObject(boolean hasHandler, String tag)
	{
		super(tag);
		m_Handler = (hasHandler ? new InternalHandler(this) : null);
	}
	
	
	// Get handler.
	@Override
	public final Handler getHandler()
	{
		return m_Handler;
	}
	
	
	/**
	 * Handle message.
	 * @param msg Message.
	 */
	protected void handleMessage(Message msg)
	{}
	
	
	// Called when releasing object.
	@Override
	protected void onRelease()
	{
		if(m_Handler != null)
			m_Handler.release();
		super.onRelease();
	}
}
