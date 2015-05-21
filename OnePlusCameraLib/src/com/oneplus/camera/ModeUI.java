package com.oneplus.camera;

import com.oneplus.base.Log;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;

/**
 * Base class of UI component for specific mode.
 * @param <TController> Type of controller component.
 */
@SuppressWarnings("rawtypes")
public abstract class ModeUI<TController extends ModeController<?>> extends UIComponent
{
	// Private fields.
	private TController m_Controller;
	private final Class<? extends TController> m_ControllerClass;
	private int m_EnterFlags;
	private boolean m_IsEntered;
	
	
	// Call-back.
	private final ComponentSearchCallback<TController> m_ControllerSearchCallback = new ComponentSearchCallback<TController>()
	{
		@Override
		public void onComponentFound(TController component)
		{
			onControllerFound(component);
		}
	};
	
	
	/**
	 * Initialize new ModeUI instance.
	 * @param name Component name.
	 * @param cameraActivity Camera activity.
	 * @param controllerType Type of controller component.
	 */
	protected ModeUI(String name, CameraActivity cameraActivity, Class<? extends TController> controllerType)
	{
		super(name, cameraActivity, true);
		if(controllerType == null)
			throw new IllegalArgumentException("No controller type.");
		m_ControllerClass = controllerType;
	}
	
	
	/**
	 * Enter mode.
	 * @param flags Custom flags.
	 * @return Whether mode is successfully entered or not.
	 */
	public final boolean enter(int flags)
	{
		// check state
		this.verifyAccess();
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
	public final void exit(int flags)
	{
		// check state
		this.verifyAccess();
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
	
	
	/**
	 * Get linked controller component.
	 * @return Controller component.
	 */
	protected final TController getController()
	{
		return m_Controller;
	}
	
	
	/**
	 * Check whether controller component is linked or not.
	 * @return Controller component state.
	 */
	protected final boolean isControllerLinked()
	{
		return (m_Controller != null);
	}
	
	
	/**
	 * Check whether mode is entered or not.
	 * @return Whether mode is entered or not.
	 */
	protected final boolean isEntered()
	{
		return m_IsEntered;
	}
	
	
	// Called when camera thread started.
	@SuppressWarnings("unchecked")
	@Override
	protected void onCameraThreadStarted()
	{
		super.onCameraThreadStarted();
		ComponentUtils.findComponent(this.getCameraThread(), m_ControllerClass, this, (ComponentSearchCallback)m_ControllerSearchCallback);
	}
	
	
	// Called when controller found.
	@SuppressWarnings({ "unchecked" })
	private void onControllerFound(TController controller)
	{
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "onControllerFound() - Component is not running");
			return;
		}
		((ModeController)controller).link(this);
		m_Controller = controller;
		this.onControllerLinked(controller);
	}
	
	
	/**
	 * Called when linked with related controller component.
	 * @param controller Controller component.
	 */
	protected void onControllerLinked(TController controller)
	{
		if(m_IsEntered)
		{
			Log.w(TAG, "onControllerLinked() - Enter mode again");
			if(!controller.enter(m_EnterFlags))
			{
				Log.e(TAG, "onControllerLinked() - Fail to enter mode");
				this.exit(0);
			}
		}
	}
	
	
	/**
	 * Called when entering mode.
	 * @param flags Custom flags.
	 * @return Whether mode is successfully entered or not.
	 */
	protected boolean onEnter(int flags)
	{
		if(m_Controller == null)
			Log.w(TAG, "onEnter() - Enter mode later when controller linked");
		else if(!m_Controller.enter(flags))
		{
			Log.e(TAG, "onEnter() - Fail to enter mode");
			return false;
		}
		return true;
	}
	
	
	/**
	 * Called when exiting mode.
	 * @param flags Custom flags.
	 */
	protected void onExit(int flags)
	{
		if(m_Controller != null)
			m_Controller.exit(flags);
	}
	
	
	// Initialize.
	@SuppressWarnings("unchecked")
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find controller
		if(this.isCameraThreadStarted())
			ComponentUtils.findComponent(this.getCameraThread(), m_ControllerClass, this, (ComponentSearchCallback)m_ControllerSearchCallback);
	}
	
	
	// Releasing component.
	@Override
	protected void onRelease()
	{
		m_Controller = null;
		super.onRelease();
	}
}
