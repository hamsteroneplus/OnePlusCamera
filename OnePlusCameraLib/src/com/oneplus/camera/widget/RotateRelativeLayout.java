package com.oneplus.camera.widget;

import com.oneplus.base.Rotation;
import com.oneplus.camera.CameraActivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.RelativeLayout;

public class RotateRelativeLayout extends RelativeLayout{

	private static final String TAG = "RotateRelativeLayout";
	private int mCurrentOrientation = 0;
	private int mTempOrientation = 0;
	private Rotation mRotation;
	private RectF newRectF;
	private Matrix invMatrix;
	private Matrix rotMatrix;
	
	public RotateRelativeLayout(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		rotMatrix = new Matrix();
		invMatrix = new Matrix();
		newRectF = new RectF();		
	}


	public RotateRelativeLayout(Context context, AttributeSet attrs) {
		// TODO Auto-generated constructor stub
		super(context, attrs);
		rotMatrix = new Matrix();
		invMatrix = new Matrix();
		newRectF = new RectF();		
	}
	
	
	private Rotation getActivityRotation()
	{
		Context context = this.getContext();
		if(context instanceof CameraActivity)
			return ((CameraActivity)context).get(CameraActivity.PROP_ACTIVITY_ROTATION);
		return mRotation;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		
		Rotation activityRotation = this.getActivityRotation();
		
	    //Log.e(TAG, "onLayout left = " + arg1 + ", top = " + arg2 + ", right = " + arg3 + ", bottom = " + arg4);
	    if (activityRotation != null
	    		&& mRotation != null
	    		&& activityRotation.isLandscape() != mRotation.isLandscape()) {
            super.onLayout(changed, top, left, bottom, right);
            return;
        }
	    /*if(mCurrentOrientation != HTCCamera.SCREEN_MODE_PORTRAIT || mCurrentOrientation == HTCCamera.SCREEN_MODE_IPORTRAIT)
        {
            super.onLayout(changed, top, left, bottom, right);
            return;
        }*/
                    
        super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		Rotation activityRotation = this.getActivityRotation();
		
	    if (activityRotation != null
	    		&& mRotation != null
	    		&& activityRotation.isLandscape() != mRotation.isLandscape()) {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        
        rotateMeasureMent();

        return;
	}
	
	protected void rotateMeasureMent() {
	    //Log.e(TAG, "rotateMeasureMent height = " + this.getMeasuredHeight() + ", width = " + this.getMeasuredWidth());
		Rotation activityRotation = this.getActivityRotation();
		
	    if (activityRotation != null
	    		&& mRotation != null
	    		&& activityRotation.isLandscape() != mRotation.isLandscape()) {
            this.setMeasuredDimension(this.getMeasuredHeight(), this.getMeasuredWidth());
        } 
	    
	    int diff;
	    if (activityRotation != null && mRotation != null)
	    	diff = Math.abs(activityRotation.getDeviceOrientation() - mRotation.getDeviceOrientation());
	    else
	    	diff = 0;
        
        rotMatrix.reset();
        
        switch(diff)
        {
            case 0:  // 0
                rotMatrix.setRotate(0);
                rotMatrix.postTranslate(0, 0);
                break;
            case 90:  // 90
                rotMatrix.setRotate(90);
                rotMatrix.postTranslate(this.getMeasuredHeight(), 0);
                break;
            case 180:  // 180
                rotMatrix.setRotate(180);
                rotMatrix.postTranslate(this.getMeasuredWidth(), this.getMeasuredHeight());
                break;
            case 270:  // 270
                rotMatrix.setRotate(270);
                rotMatrix.postTranslate(0, this.getMeasuredWidth());
                break;
            default:
                break;
        }
        
        invMatrix = new Matrix(rotMatrix);        
        rotMatrix.invert(invMatrix);
        
        return;
	}

	@Override
    protected void dispatchDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        canvas.save();
        canvas.concat(invMatrix);      
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        float[] pos = new float[2];
        float x = event.getX();
        float y = event.getY();
        pos[0] = x;
        pos[1] = y;
        float[] posDst = new float[2];
        rotMatrix.mapPoints(posDst,pos);
        event.setLocation(posDst[0], posDst[1]);
				
		boolean result = super.dispatchTouchEvent(event);
        event.setLocation(x, y);
        return result;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
    	
    	Rotation activityRotation = this.getActivityRotation();
    	int diff;
 	    if (activityRotation != null && mRotation != null)
 	    	diff = Math.abs(activityRotation.getDeviceOrientation() - mRotation.getDeviceOrientation());
 	    else
 	    	diff = 0;
    	
        switch(diff)
        {           
            case 0:  // 0                
                event.setLocation(event.getX(), event.getY());
                break;
            case 90:  // 90
                event.setLocation(-event.getY(), event.getX());               
                break;
            case 180:  // 180                
                event.setLocation(-event.getX(), -event.getY());
                break;
            case 270:  // 270               
                event.setLocation(event.getY(), -event.getX());
                break;
            default:
                break;
        }
        
        return super.dispatchTrackballEvent(event);
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect rect) {
        // TODO Auto-generated method stub
        rect.offset(location[0], location[1]);
        newRectF.set(rect);
        invMatrix.mapRect(newRectF);
        newRectF.roundOut(rect);
        invalidate(rect);
        //return null;
        return super.invalidateChildInParent(location, rect);
    }	
	
	public void setOrientationDelay(int orientation){
					
		mTempOrientation = orientation;
		
		//Log.e(TAG, "setRotation temp_orientation_mode = " + temp_orientation_mode + ", this.current_orientation_mode = " + this.current_orientation_mode);
	}
	
	public void setOrientation(int orientation){
        
        if(mCurrentOrientation == orientation || orientation == -1)
            return;
        
        //Log.e(TAG, "setRotation newRotation = " + newRotation + ", this.current_orientation_mode = " + this.current_orientation_mode);
        
        mCurrentOrientation = orientation;
        requestLayout();
        invalidate();
        
    }
	
	public final void setRotation(Rotation rotation)
	{
		if(mRotation == rotation)
			return;
		mRotation = rotation;
		requestLayout();
	    invalidate();
	}
    
    public int getOrientation(){
        //Log.e(TAG, "getRotation current_orientation_mode = " + current_orientation_mode);
        return mCurrentOrientation;     
    }

	@Override
	protected void onAnimationEnd() {
		// TODO Auto-generated method stub
		super.onAnimationEnd();
		//mCurrentOrientation = mTempOrientation;
		requestLayout();
		invalidate();
	}    
    
}
