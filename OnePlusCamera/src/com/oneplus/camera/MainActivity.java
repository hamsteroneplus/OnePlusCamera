package com.oneplus.camera;

import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.scene.SceneManager;

import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewStub;

/**
 * Main camera activity.
 */
public class MainActivity extends CameraActivity
{
	// Private fields
	private ViewGroup m_CaptureUIContainer;
	
	
	/**
	 * Initialize new MainActivity instance.
	 */
	public MainActivity()
	{
		// Select components
		this.addComponentBuilders(ComponentBuilders.BUILDERS_MAIN_ACTIVITY);
	}
	
	
	/**
	 * Get capture UI container.
	 * @return Capture UI container.
	 */
	public final ViewGroup getCaptureUIContainer()
	{
		return m_CaptureUIContainer;
	}
	
	
	// Called when creating.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// call super
		super.onCreate(savedInstanceState);
		
		// add call-backs
		this.addCallback(PROP_IS_LAUNCHING, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				if(!e.getNewValue())
					onLaunchCompleted();
			}
		});
		
		// start camera thread
		CameraThread.ResourceIdTable resIdTable = new CameraThread.ResourceIdTable();
		CameraThread cameraThread = this.getCameraThread();
		cameraThread.addComponentBuilders(ComponentBuilders.BUILDERS_CAMERA_THREAD);
		resIdTable.photoShutterSound = R.raw.shutter_photo;
		resIdTable.videoStartSound = R.raw.record_start;
		resIdTable.videoStopSound = R.raw.record_end;
		cameraThread.setResourceIdTable(resIdTable);
		cameraThread.start(this.get(PROP_MEDIA_TYPE));
		
		// setup scene builders
		final SceneManager sceneManager = this.findComponent(SceneManager.class);
		if(sceneManager != null)
		{
			for(int i = 0, count = SceneBuilders.BUILDERS.length ; i < count ; ++i)
				sceneManager.addBuilder(SceneBuilders.BUILDERS[i], 0);
		}
		else
			Log.e(TAG, "onCreate() - No SceneManager");
		
		// setup effect builders
		//
		
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
	
	
	// Called when launch completed.
	private void onLaunchCompleted()
	{
		// inflate capture UI
		Log.v(TAG, "onLaunchCompleted() - Inflate capture UI [start]");
		m_CaptureUIContainer = (ViewGroup)((ViewStub)this.findViewById(R.id.capture_ui_container)).inflate();
		Log.v(TAG, "onLaunchCompleted() - Inflate capture UI [end]");
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
