package com.oneplus.widget;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewPropertyAnimator;

/**
 * Utility methods for {@link View}.
 */
public final class ViewUtils
{
	/**
	 * Call-back interface for animation completion.
	 */
	public interface AnimationCompletedCallback
	{
		/**
		 * Called when animation completed.
		 * @param view View to animation.
		 * @param isCancelled Whether animation is cancelled or not.
		 */
		void onAnimationCompleted(View view, boolean isCancelled);
	}
	
	
	// Constructor
	private ViewUtils()
	{}
	
	
	/**
	 * Rotate view.
	 * @param view View to rotate.
	 * @param toDegrees Target degrees.
	 * @param duration Animation duration in milliseconds.
	 */
	public static void rotate(View view, float toDegrees, long duration)
	{
		rotate(view, toDegrees, duration, null);
	}
	
	
	/**
	 * Rotate view.
	 * @param view View to rotate.
	 * @param toDegrees Target degrees.
	 * @param duration Animation duration in milliseconds.
	 * @param interpolator Interpolator.
	 */
	public static void rotate(View view, float toDegrees, long duration, TimeInterpolator interpolator)
	{
		if(view == null)
			return;
		ViewPropertyAnimator animator = view.animate();
		animator.rotation(toDegrees);
		animator.setDuration(duration);
		if(interpolator != null)
			animator.setInterpolator(interpolator);
		animator.start();
	}
	
	
	/**
	 * Change height of view.
	 * @param view View to change height.
	 * @param height New height.
	 */
	public static void setHeight(View view, int height)
	{
		if(view == null)
			return;
		LayoutParams params = view.getLayoutParams();
		params.height = height;
		view.requestLayout();
	}
	
	
	/**
	 * Change margins of view.
	 * @param view View to change margins.
	 * @param margins Margins around view.
	 */
	public static void setMargins(View view, int margins)
	{
		setMargins(view, margins, margins, margins, margins);
	}
	
	
	/**
	 * Change margins of view.
	 * @param view View to change margins.
	 * @param left Left margin.
	 * @param top Top margin.
	 * @param right Right margin.
	 * @param bottom Bottom margin.
	 */
	public static void setMargins(View view, int left, int top, int right, int bottom)
	{
		if(view == null)
			return;
		LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams)
		{
			MarginLayoutParams marginParams = (MarginLayoutParams)params;
			marginParams.leftMargin = left;
			marginParams.topMargin = top;
			marginParams.rightMargin = right;
			marginParams.bottomMargin = bottom;
			view.requestLayout();
		}
	}
	
	
	/**
	 * Change size of view.
	 * @param view View to change size.
	 * @param width New width.
	 * @param height New height.
	 */
	public static void setSize(View view, int width, int height)
	{
		if(view == null)
			return;
		LayoutParams params = view.getLayoutParams();
		params.width = width;
		params.height = height;
		view.requestLayout();
	}
	
	
	/**
	 * Change visibility of view.
	 * @param view View to change visibility.
	 * @param isVisible Visibility.
	 * @param duration Animation duration in milliseconds.
	 */
	public static void setVisibility(View view, boolean isVisible, long duration)
	{
		setVisibility(view, isVisible, duration, null, null);
	}
	
	
	/**
	 * Change visibility of view.
	 * @param view View to change visibility.
	 * @param isVisible Visibility.
	 * @param duration Animation duration in milliseconds.
	 * @param interpolator Interpolator.
	 */
	public static void setVisibility(View view, boolean isVisible, long duration, TimeInterpolator interpolator)
	{
		setVisibility(view, isVisible, duration, interpolator, null);
	}
	
	
	/**
	 * Change visibility of view.
	 * @param view View to change visibility.
	 * @param isVisible Visibility.
	 * @param duration Animation duration in milliseconds.
	 * @param interpolator Interpolator.
	 * @param callback Animation call-back.
	 */
	public static void setVisibility(final View view, boolean isVisible, long duration, TimeInterpolator interpolator, final AnimationCompletedCallback callback)
	{
		if(view == null)
			return;
		ViewPropertyAnimator animator = null;
		if(isVisible)
		{
			if(view.getVisibility() != View.VISIBLE)
			{
				if(duration >= 0)
				{
					animator = view.animate();
					animator.alpha(1);
				}
				view.setVisibility(View.VISIBLE);
			}
			else
				return;
		}
		else
		{
			if(view.getVisibility() == View.VISIBLE)
			{
				if(duration >= 0)
				{
					animator = view.animate();
					animator.alpha(0);
				}
				view.setVisibility(View.INVISIBLE);
			}
			else 
				return;
		}
		if(animator != null)
		{
			animator.setDuration(duration);
			if(interpolator != null)
				animator.setInterpolator(interpolator);
			if(callback != null)
			{
				animator.setListener(new Animator.AnimatorListener()
				{
					@Override
					public void onAnimationStart(Animator animation)
					{}
					
					@Override
					public void onAnimationRepeat(Animator animation)
					{}
					
					@Override
					public void onAnimationEnd(Animator animation)
					{
						callback.onAnimationCompleted(view, false);
					}
					
					@Override
					public void onAnimationCancel(Animator animation)
					{
						callback.onAnimationCompleted(view, true);
					}
				});
			}
			animator.start();
		}
	}
	
	
	/**
	 * Change width of view.
	 * @param view View to change width.
	 * @param width New width.
	 */
	public static void setWidth(View view, int width)
	{
		if(view == null)
			return;
		LayoutParams params = view.getLayoutParams();
		params.width = width;
		view.requestLayout();
	}
}
