package com.oneplus.camera;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;

/**
 * Base class for UI related component.
 */
public abstract class UIComponent extends CameraComponent
{
	/**
	 * Duration of UI fade-in in milliseconds.
	 */
	public static final long DURATION_FADE_IN = 300;
	/**
	 * Duration of UI fade-out in milliseconds.
	 */
	public static final long DURATION_FADE_OUT = 300;
	
	
	// Call-backs.
	private final PropertyChangedCallback<Rotation> m_RotationChangedCallback = new PropertyChangedCallback<Rotation>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Rotation> key, PropertyChangeEventArgs<Rotation> e)
		{
			onRotationChanged(e.getOldValue(), e.getNewValue());
		}
	};
	
	
	/**
	 * Initialize new CameraComponent instance.
	 * @param name Component name.
	 * @param activity {@link CameraActivity} instance.
	 * @param hasHandler Whether internal {@link android.os.Handler Handler} should be created or not.
	 */
	protected UIComponent(String name, CameraActivity cameraActivity, boolean hasHandler)
	{
		super(name, cameraActivity, hasHandler);
	}
	
	
	/**
	 * Get current UI rotation.
	 * @return Current rotation.
	 */
	protected final Rotation getRotation()
	{
		return this.getCameraActivity().get(CameraActivity.PROP_ROTATION);
	}
	
	
	// Deinitialize.
	@Override
	protected void onDeinitialize()
	{
		this.getCameraActivity().removeCallback(CameraActivity.PROP_ROTATION, m_RotationChangedCallback);
		super.onDeinitialize();
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		this.getCameraActivity().addCallback(CameraActivity.PROP_ROTATION, m_RotationChangedCallback);
	}
	
	
	/**
	 * Called when UI rotation changes.
	 * @param prevRotation Previous rotation.
	 * @param newRotation New rotation.
	 */
	protected void onRotationChanged(Rotation prevRotation, Rotation newRotation)
	{}
}
