package com.oneplus.camera.scene;

import com.oneplus.base.Log;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.ModeUI;

/**
 * Base class for capture mode based-on component.
 */
public abstract class ComponentBasedScene<TComponent extends ModeUI<?>> extends BasicScene
{
	// Private fields.
	private TComponent m_Component;
	private final Class<? extends TComponent> m_ComponentClass;
	
	
	/**
	 * Initialize new ComponentBasedScene instance.
	 * @param cameraActivity Camera activity.
	 * @param id ID represents this scene.
	 * @param componentType Type of related component.
	 */
	protected ComponentBasedScene(CameraActivity cameraActivity, String id, Class<? extends TComponent> componentType)
	{
		super(cameraActivity, id);
		if(componentType == null)
			throw new IllegalArgumentException("No component type.");
		m_ComponentClass = componentType;
	}
	
	
	/**
	 * Call {@link ModeUI#enter(int)}.
	 * @param prevScene Previous scene.
	 * @param flags Flags from {@link #onEnter(CaptureMode, int)}.
	 * @param customFlags Custom flags passed to {@link ModeUI#enter(int)}.
	 * @return Whether {@link ModeUI#enter(int)} is successfully called or not.
	 */
	protected boolean onCallComponentEnter(Scene prevScene, int flags, int customFlags)
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
	 * @param nextScene Next scene.
	 * @param flags Flags from {@link #onExit(CaptureMode, int)}.
	 * @param customFlags Custom flags passed to {@link ModeUI#exit(int)}.
	 */
	protected void onCallComponentExit(Scene nextScene, int flags, int customFlags)
	{
		if(m_Component != null)
			m_Component.exit(customFlags);
	}
	
	
	// Enter scene.
	@Override
	protected boolean onEnter(Scene prevScene, int flags)
	{
		if(m_Component == null)
			m_Component = this.getCameraActivity().findComponent(m_ComponentClass);
		return this.onCallComponentEnter(prevScene, flags, 0);
	}
	
	
	// Exit scene.
	@Override
	protected void onExit(Scene nextScene, int flags)
	{
		this.onCallComponentExit(nextScene, flags, 0);
	}
	
	
	// Release scene.
	@Override
	protected void onRelease()
	{
		m_Component = null;
		super.onRelease();
	}
}
