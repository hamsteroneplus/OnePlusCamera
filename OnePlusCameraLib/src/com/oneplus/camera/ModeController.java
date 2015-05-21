package com.oneplus.camera;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.component.Component;

import android.os.Message;

/**
 * Base class of controller component for specific mode.
 * @param <TUI> Type of UI component.
 */
public abstract class ModeController<TUI extends Component> extends CameraComponent
{
	// Constants.
	private static final int MSG_ENTER = -10000;
	private static final int MSG_EXIT = -10001;
	private static final int MSG_LINK = -10002;
	
	
	// Private fields.
	private boolean m_IsEntered;
	private TUI m_UI;
	
	
	/**
	 * Initialize new ModeController instance.
	 * @param name Component name.
	 * @param cameraThread Camera thread.
	 */
	protected ModeController(String name, CameraThread cameraThread)
	{
		super(name, cameraThread, true);
	}
	
	
	/**
	 * Enter mode.
	 * @param flags Custom flags.
	 * @return Whether mode is successfully entered or not.
	 */
	final boolean enter(int flags)
	{
		// enter asynchronously
		if(!this.isDependencyThread())
		{
			HandlerUtils.removeMessages(this, MSG_EXIT);
			return HandlerUtils.sendMessage(this, MSG_ENTER, flags, 0, null, true);
		}
		
		// check state
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "enter() - Component is not running");
			return false;
		}
		if(m_IsEntered)
			return true;
		
		Log.v(TAG, "enter()");
		
		// enter
		try
		{
			if(!this.onEnter(flags))
			{
				Log.e(TAG, "enter() - Fail to enter mode");
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "enter() - Fail to enter mode", ex);
			return false;
		}
		
		// complete
		m_IsEntered = true;
		return true;
	}
	
	
	/**
	 * Exit mode.
	 * @param flags Custom flags.
	 */
	final void exit(int flags)
	{
		// exit asynchronously
		if(!this.isDependencyThread())
		{
			HandlerUtils.removeMessages(this, MSG_ENTER);
			HandlerUtils.sendMessage(this, MSG_EXIT, flags, 0, null, true);
			return;
		}
		
		// check state
		if(!m_IsEntered)
			return;
		
		Log.v(TAG, "exit()");
		
		// exit
		try
		{
			this.onExit(flags);
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "exit() - Error occurred while exiting mode", ex);
		}
		
		// complete
		m_IsEntered = false;
	}
	
	
	// Handle message.
	@SuppressWarnings("unchecked")
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_ENTER:
				if(!this.enter(msg.arg1))
					Log.e(TAG, "handleMessage() - Fail to enter mode asynchronously");
				break;
				
			case MSG_EXIT:
				this.exit(msg.arg1);
				break;
				
			case MSG_LINK:
				this.link((TUI)msg.obj);
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	/**
	 * Get linked UI component.
	 * @return UI component.
	 */
	protected final TUI getUI()
	{
		return m_UI;
	}
	
	
	/**
	 * Check whether mode is entered or not.
	 * @return Whether mode is entered or not.
	 */
	protected final boolean isEntered()
	{
		return m_IsEntered;
	}
	
	
	/**
	 * Check whether UI component is linked or not.
	 * @return UI component state.
	 */
	protected final boolean isUILinked()
	{
		return (m_UI != null);
	}
	
	
	/**
	 * Link with given UI component.
	 * @param ui UI component.
	 */
	final void link(TUI ui)
	{
		if(this.isDependencyThread())
		{
			if(!this.isRunningOrInitializing())
			{
				Log.e(TAG, "link() - Component is not running");
				return;
			}
			m_UI = ui;
			this.onUILinked(ui);
		}
		else if(!HandlerUtils.sendMessage(this, MSG_LINK, 0, 0, ui))
			Log.e(TAG, "link() - Fail to perform cross-thread operation");
	}
	
	
	/**
	 * Called when entering mode.
	 * @param flags Custom flags.
	 * @return Whether mode is successfully entered or not.
	 */
	protected boolean onEnter(int flags)
	{
		return true;
	}
	
	
	/**
	 * Called when exiting mode.
	 * @param flags Custom flags.
	 */
	protected void onExit(int flags)
	{}
	
	
	// Releasing component.
	@Override
	protected void onRelease()
	{
		m_UI = null;
		super.onRelease();
	}
	
	
	/**
	 * Called when linked with UI component.
	 * @param ui UI component.
	 */
	protected void onUILinked(TUI ui)
	{}
}
