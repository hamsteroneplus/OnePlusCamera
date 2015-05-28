package com.oneplus.camera;

import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.capturemode.CaptureModeManager;
import com.oneplus.camera.scene.SceneManager;

import android.content.ActivityNotFoundException;
import android.content.Intent;
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
	private SceneManager m_SceneManager;
	
	
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
	
	
	/**
	 * Get {@link SceneManager} component.
	 * @return {@link SceneManager}.
	 */
	public final SceneManager getSceneManager()
	{
		return m_SceneManager;
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
		
		// setup capture modes
		CaptureModeManager captureModeManager = this.findComponent(CaptureModeManager.class);
		if(captureModeManager != null)
		{
			for(int i = 0, count = CaptureModeBuilders.BUILDERS.length ; i < count ; ++i)
				captureModeManager.addBuilder(CaptureModeBuilders.BUILDERS[i], 0);
		}
		else
			Log.e(TAG, "onCreate() - No CaptureModeManager");
		
		// change to initial capture mode
		if(captureModeManager != null)
			captureModeManager.changeToInitialCaptureMode(0);
		
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
		m_SceneManager = this.findComponent(SceneManager.class);
		if(m_SceneManager != null)
		{
			for(int i = 0, count = SceneBuilders.BUILDERS.length ; i < count ; ++i)
				m_SceneManager.addBuilder(SceneBuilders.BUILDERS[i], 0);
		}
		else
			Log.e(TAG, "onCreate() - No SceneManager interface");
		
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
	
	
	/**
	 * Open advanced settings UI.
	 * @return True if advanced settings UI shows successfully, False otherwise.
	 */
	public final boolean showAdvancedSettings()
	{
		// start activity
		try
		{
			Settings settings = this.get(PROP_SETTINGS);
			Intent intent = new Intent(this.getApplicationContext(), AdvancedSettingsActivity.class);
			intent.putExtra(AdvancedSettingsActivity.EXTRA_SETTINGS_NAME, settings.getName());
			intent.putExtra(AdvancedSettingsActivity.EXTRA_SETTINGS_IS_VOLATILE, settings.isVolatile());
			this.startActivityForResult(intent, 0);
		}
		catch(ActivityNotFoundException ex)
		{
			Log.e(TAG, "showAdvancedSettings() - Fail to start activity", ex);
			return false;
		}
		
		// complete
		return true;
	}
}
