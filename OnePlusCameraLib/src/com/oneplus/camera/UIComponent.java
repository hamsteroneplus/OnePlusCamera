package com.oneplus.camera;

import android.animation.TimeInterpolator;
import android.view.View;
import android.view.animation.PathInterpolator;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.Rotation;
import com.oneplus.widget.ViewUtils;

/**
 * Base class for UI related component.
 */
public abstract class UIComponent extends CameraComponent
{
	/**
	 * Duration of UI fade-in in milliseconds.
	 */
	public static final long DURATION_FADE_IN = 600;
	/**
	 * Duration of UI fade-out in milliseconds.
	 */
	public static final long DURATION_FADE_OUT = 600;
	/**
	 * Duration of UI rotation in milliseconds.
	 */
	public static final long DURATION_ROTATION = 600;
	/**
	 * Interpolator for UI fade-in animation.
	 */
	public static final TimeInterpolator INTERPOLATOR_FADE_IN;
	/**
	 * Interpolator for UI fade-out animation.
	 */
	public static final TimeInterpolator INTERPOLATOR_FADE_OUT;
	/**
	 * Interpolator for UI rotation animation.
	 */
	public static final TimeInterpolator INTERPOLATOR_ROTATION;
	
	
	// Call-backs.
	private final PropertyChangedCallback<Rotation> m_RotationChangedCallback = new PropertyChangedCallback<Rotation>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Rotation> key, PropertyChangeEventArgs<Rotation> e)
		{
			onRotationChanged(e.getOldValue(), e.getNewValue());
		}
	};
	
	
	// Static initializer.
	static
	{
		INTERPOLATOR_FADE_IN = new PathInterpolator(0.8f, 0, 0.2f, 1);
		INTERPOLATOR_FADE_OUT = INTERPOLATOR_FADE_IN;
		INTERPOLATOR_ROTATION = INTERPOLATOR_FADE_IN;
	}
	
	
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
	 * Get current camera activity rotation.
	 * @return Current activity rotation.
	 */
	protected final Rotation getCameraActivityRotation()
	{
		return this.getCameraActivity().get(CameraActivity.PROP_ACTIVITY_ROTATION);
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
	
	
	/**
	 * Change rotation of view.
	 * @param view View to change rotation.
	 * @param toRotation New rotation.
	 */
	protected void rotateView(View view, Rotation toRotation)
	{
		this.rotateView(view, toRotation, DURATION_ROTATION, INTERPOLATOR_ROTATION);
	}
	
	
	/**
	 * Change rotation of view.
	 * @param view View to change rotation.
	 * @param toRotation New rotation.
	 * @param duration Animation duration.
	 */
	protected void rotateView(View view, Rotation toRotation, long duration)
	{
		this.rotateView(view, toRotation, duration, INTERPOLATOR_ROTATION);
	}
	
	
	/**
	 * Change rotation of view.
	 * @param view View to change rotation.
	 * @param toRotation New rotation.
	 * @param duration Animation duration.
	 * @param interpolator Animation interpolator.
	 */
	protected void rotateView(View view, Rotation toRotation, long duration, TimeInterpolator interpolator)
	{
		if(view == null)
			return;
		Rotation baseRotation = this.getCameraActivity().get(CameraActivity.PROP_ACTIVITY_ROTATION);
		float fromDegrees = view.getRotation();
		float toDegrees = (baseRotation.getDeviceOrientation() - toRotation.getDeviceOrientation());
		if(Math.abs(fromDegrees - toDegrees) < 0.1f)
			return;
		if(duration > 0)
		{
			if(Math.abs(toDegrees - fromDegrees) > 180)
			{
				if(fromDegrees > toDegrees)
					view.setRotation(fromDegrees - 360);
				else
					view.setRotation(fromDegrees + 360);
			}
			ViewUtils.rotate(view, toDegrees, duration, interpolator);
		}
		else
			view.setRotation(toDegrees);
	}
}
