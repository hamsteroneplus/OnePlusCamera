package com.oneplus.camera.capturemode;

import com.oneplus.base.Log;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.ModeUI;

/**
 * Base class for capture mode based-on component.
 */
public abstract class ComponentBasedCaptureMode<TComponent extends ModeUI<?>> extends BasicCaptureMode
{
	// Private fields.
	private TComponent m_Component;
	private final Class<? extends TComponent> m_ComponentClass;
	
	
	/**
	 * Initialize new ComponentBasedCaptureMode instance.
	 * @param cameraActivity Camera activity.
	 * @param id ID represents this capture mode.
	 * @param customSettingsName Name for custom settings.
	 * @param componentType Type of related component.
	 */
	protected ComponentBasedCaptureMode(CameraActivity cameraActivity, String id, String customSettingsName, Class<? extends TComponent> componentType)
	{
		super(cameraActivity, id, customSettingsName);
		if(componentType == null)
			throw new IllegalArgumentException("No component type.");
		m_ComponentClass = componentType;
	}
	
	
	/**
	 * Call {@link ModeUI#enter(int)}.
	 * @param prevMode Previous mode.
	 * @param flags Flags from {@link #onEnter(CaptureMode, int)}.
	 * @param customFlags Custom flags passed to {@link ModeUI#enter(int)}.
	 * @return Whether {@link ModeUI#enter(int)} is successfully called or not.
	 */
	protected boolean onCallComponentEnter(CaptureMode prevMode, int flags, int customFlags)
	{
		if(m_Component == null)
		{
			Log.e(TAG, "onCallComponentEnter() - No component to call");
			return false;
		}
		return m_Component.enter(customFlags);
	}
	
	
	/**
	 * Call {@link ModeUI#exit(int)}.
	 * @param nextMode Next mode.
	 * @param flags Flags from {@link #onExit(CaptureMode, int)}.
	 * @param customFlags Custom flags passed to {@link ModeUI#exit(int)}.
	 */
	protected void onCallComponentExit(CaptureMode nextMode, int flags, int customFlags)
	{
		if(m_Component != null)
			m_Component.exit(customFlags);
	}
	
	
	// Enter capture mode.
	@Override
	protected boolean onEnter(CaptureMode prevMode, int flags)
	{
		if(m_Component == null)
			m_Component = this.getCameraActivity().findComponent(m_ComponentClass);
		return this.onCallComponentEnter(prevMode, flags, 0);
	}
	
	
	// Exit capture mode.
	@Override
	protected void onExit(CaptureMode nextMode, int flags)
	{
		this.onCallComponentExit(nextMode, flags, 0);
	}
	
	
	// Release capture mode.
	@Override
	protected void onRelease()
	{
		m_Component = null;
		super.onRelease();
	}
}
