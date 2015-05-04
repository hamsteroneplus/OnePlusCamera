package com.oneplus.camera;

import java.util.List;

import com.oneplus.base.Log;

import android.os.Bundle;

/**
 * Main camera activity.
 */
public class MainActivity extends CameraActivity
{
	/**
	 * Initialize new MainActivity instance.
	 */
	public MainActivity()
	{
		// Select components
		this.addComponentBuilders(ComponentBuilders.BUILDERS_MAIN_ACTIVITY);
	}
	
	
	// Called when available cameras list changes.
	@Override
	protected void onAvailableCamerasChanged(List<Camera> cameras)
	{
		// call super
		super.onAvailableCamerasChanged(cameras);
		
		// check state
		if(this.get(PROP_CAMERA) != null)
			return;
		
		// select camera
		Camera camera = CameraUtils.findCamera(cameras, Camera.LensFacing.BACK, false);
		Log.w(TAG, "onAvailableCamerasChanged() - Select " + camera);
		this.setReadOnly(PROP_CAMERA, camera);
		
		// check activity state
		switch(this.get(PROP_STATE))
		{
			case CREATING:
			case RESUMING:
			case RUNNING:
				break;
			default:
				return;
		}
		
		// open camera
		if(!this.getCameraThread().openCamera(camera))
			Log.e(TAG, "onAvailableCamerasChanged() - Fail to open camera " + camera);
	}
	
	
	// Called when creating.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// call super
		super.onCreate(savedInstanceState);
		
		// start camera thread
		CameraThread cameraThread = this.getCameraThread();
		cameraThread.addComponentBuilders(ComponentBuilders.BUILDERS_CAMERA_THREAD);
		cameraThread.setDefaultShutterSound(R.raw.shutter_photo);
		cameraThread.start();
		
		// set content view
		setContentView(R.layout.activity_main);
	}
	
	
	// Called when destroying.
	@Override
	protected void onDestroy()
	{
		// call super
		super.onDestroy();
	}
	
	
	// Called when resuming.
	@Override
	protected void onResume()
	{
		// call super
		super.onResume();
	}
	
	
	// Called when pausing.
	@Override
	protected void onPause()
	{
		// call super
		super.onPause();
	}
}
