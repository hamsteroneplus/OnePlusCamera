package com.oneplus.camera;

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
