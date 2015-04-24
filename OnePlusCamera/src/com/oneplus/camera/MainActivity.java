package com.oneplus.camera;

import java.util.List;

import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Camera.State;
import com.oneplus.util.AspectRatio;
import com.oneplus.util.SizeComparator;

import android.os.Bundle;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends CameraActivity
{
	private Camera m_Camera;
	private CameraDeviceManager m_CameraDeviceManager;
	private boolean m_IsPreviewSurfaceReady;
	private Size m_SurfaceSize = new Size(0, 0);
	private SurfaceView m_SurfaceView;
	
	private final PropertyChangedCallback<Camera.State> m_CameraStateChangedCallback = new PropertyChangedCallback<Camera.State>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<State> key, PropertyChangeEventArgs<State> e)
		{
			final Camera.State state = e.getNewValue();
			HandlerUtils.post(MainActivity.this, new Runnable()
			{
				@Override
				public void run()
				{
					onCameraStateChanged(state);
				}
			});
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getCameraThread().start();
		
		setContentView(R.layout.activity_main);
		
		m_SurfaceView = (SurfaceView)this.findViewById(R.id.surface_view);
		m_SurfaceView.getHolder().addCallback(new SurfaceHolder.Callback()
		{
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder)
			{
				Log.w(TAG, "surfaceDestroyed");
				m_IsPreviewSurfaceReady = false;
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder)
			{
				Log.w(TAG, "surfaceCreated");
				//holder.setFixedSize(1280, 720);
				//m_IsPreviewSurfaceReady = true;
				//startPreview();
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
			{
				Log.w(TAG, "surfaceChanged : " + width + " x " + height);
				m_SurfaceSize = new Size(width, height);
				m_IsPreviewSurfaceReady = true;
				startPreview();
			}
		});
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		if(m_CameraDeviceManager == null)
			m_CameraDeviceManager = this.getCameraThread().findComponent(CameraDeviceManager.class);
		if(m_CameraDeviceManager != null)
		{
			List<Camera> list = m_CameraDeviceManager.get(CameraDeviceManager.PROP_AVAILABLE_CAMERAS);
			m_Camera = CameraUtils.findCamera(list, Camera.LensFacing.BACK, false);
			if(m_Camera != null)
			{
				HandlerUtils.post(m_CameraDeviceManager, new Runnable()
				{
					@Override
					public void run()
					{
						m_Camera.addCallback(Camera.PROP_STATE, m_CameraStateChangedCallback);
						m_Camera.open(0);
					}
				});
			}
		}
	}
	
	@Override
	protected void onPause()
	{
		if(m_Camera != null)
		{
			final Camera camera = m_Camera;
			HandlerUtils.post(m_CameraDeviceManager, new Runnable()
			{
				@Override
				public void run()
				{
					camera.close(0);
					camera.removeCallback(Camera.PROP_STATE, m_CameraStateChangedCallback);
				}
			});
			m_Camera = null;
		}
		
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	private void onCameraStateChanged(Camera.State state)
	{
		if(state == State.OPENED)
			this.startPreview();
	}
	
	private void startPreview()
	{
		if(!this.get(PROP_IS_CAMERA_THREAD_STARTED) 
				|| !m_IsPreviewSurfaceReady 
				|| m_Camera == null 
				|| m_Camera.get(Camera.PROP_STATE) != State.OPENED)
		{
			return;
		}
		
		final SurfaceHolder holder = m_SurfaceView.getHolder();
		List<Size> previewSizes = m_Camera.get(Camera.PROP_PREVIEW_SIZES);
		SizeComparator comparator = new SizeComparator();
		Size previewSize = null;
		for(Size size : previewSizes)
		{
			AspectRatio ratio = AspectRatio.get(size.getWidth(), size.getHeight());
			Log.w("Hamster", "preview size : " + size + ", ratio : " + ratio);
			if(ratio == AspectRatio.RATIO_16x9 && (previewSize == null || comparator.compare(size, previewSize) > 0))
				previewSize = size;
		}
		//previewSize = new Size(800, 480);
		if(previewSize != null)
		{
			Log.w("Hamster", "use preview size : " + previewSize);
			if(!previewSize.equals(m_SurfaceSize))
			{
				holder.setFixedSize(previewSize.getWidth(), previewSize.getHeight());
				return;
			}
		}
		
		HandlerUtils.post(this.getCameraThread(), new Runnable()
		{
			@Override
			public void run()
			{
				m_Camera.set(Camera.PROP_PREVIEW_RECEIVER, holder);
				m_Camera.startPreview(0);
			}
		});
	}
}
