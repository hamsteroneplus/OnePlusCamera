package com.oneplus.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Vertical {@link SeekBar}.
 */
public class VerticalSeekBar extends SeekBar
{
	/**
	 * Initialize new VerticalSeekBar instance.
	 * @param context Context.
	 */
	public VerticalSeekBar(Context context)
	{
		super(context);
	}
	
	
	/**
	 * Initialize new VerticalSeekBar instance.
	 * @param context Context.
	 * @param attrs Attributes.
	 */
	public VerticalSeekBar(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.setPadding(this.getPaddingLeft(), this.getPaddingTop(), this.getPaddingRight(), this.getPaddingBottom());
	}
	
	
	// Called when drawing view.
	@Override
	protected synchronized void onDraw(Canvas canvas)
	{
		canvas.rotate(-90);
		canvas.translate(-this.getHeight(), 0);
		super.onDraw(canvas);
	}
	
	
	// Measure size.
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		this.setMeasuredDimension(this.getMeasuredHeight(), this.getMeasuredWidth());
	}
	
	
	// Called when size changes.
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(h, w, oldh, oldw);
	}
	
	
	// Handle touch event.
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (!isEnabled())
            return false;
        switch (event.getAction()) 
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            {
            	int progress = (this.getMax() - (int)(this.getMax() * event.getY() / this.getHeight()));
                this.setProgress(progress);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
	}
	
	
	// Set paddings.
	@Override
	public void setPadding(int left, int top, int right, int bottom)
	{
		super.setPadding(bottom, left, top, right);
	}
	
	
	// Set progress.
	@Override
	public synchronized void setProgress(int progress)
	{
		super.setProgress(progress);
		this.onSizeChanged(this.getWidth(), this.getHeight(), 0, 0);
	}
}
