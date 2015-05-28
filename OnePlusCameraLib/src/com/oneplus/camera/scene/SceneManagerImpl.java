package com.oneplus.camera.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.Mode;
import com.oneplus.camera.Mode.State;
import com.oneplus.util.ListUtils;

final class SceneManagerImpl extends CameraComponent implements SceneManager
{
	// Private fields.
	private final List<Scene> m_ActiveScenes = new ArrayList<>();
	private Scene m_DefaultScene = Scene.NO_SCENE;
	private final List<DefaultSceneHandle> m_DefaultSceneHandles = new ArrayList<>();
	private Scene m_Scene = Scene.NO_SCENE;
	private final List<SceneBuilder> m_SceneBuilders = new ArrayList<>();
	private final List<Scene> m_Scenes = new ArrayList<>();
	
	
	// Class for default scene handle.
	private final class DefaultSceneHandle extends Handle
	{
		public final Scene scene;
		
		public DefaultSceneHandle(Scene scene)
		{
			super("DefaultScene");
			this.scene = scene;
		}

		@Override
		protected void onClose(int flags)
		{
			restoreDefaultScene(this, flags);
		}
	}
	
	
	// Call-backs.
	private final PropertyChangedCallback<Mode.State> m_SceneStateChangedCallback = new PropertyChangedCallback<Mode.State>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Mode.State> key, PropertyChangeEventArgs<Mode.State> e)
		{
			switch(e.getNewValue())
			{
				case DISABLED:
					onSceneDisabled((Scene)source);
					break;
				case RELEASED:
					onSceneReleased((Scene)source);
					break;
				default:
					if(e.getOldValue() == State.DISABLED)
						onSceneEnabled((Scene)source);
					break;
			}
		}
	};
	
	
	// Constructor.
	SceneManagerImpl(CameraActivity cameraActivity)
	{
		super("Scene Manager", cameraActivity, false);
		this.setReadOnly(PROP_SCENES, Collections.unmodifiableList(m_ActiveScenes));
	}
	
	
	// Add scene builder.
	@Override
	public boolean addBuilder(SceneBuilder builder, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "addBuilder() - Component is not running");
			return false;
		}
		
		// check parameter
		if(builder == null)
		{
			Log.e(TAG, "addBuilder() - No builder to add");
			return false;
		}
		
		// add builder and create scene
		m_SceneBuilders.add(builder);
		this.createScene(builder);
		
		// complete
		return true;
	}
	
	
	// Change default scene.
	@Override
	public Handle setDefaultScene(Scene scene, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing(true))
			return null;
		
		// check parameter
		if(scene == null)
		{
			Log.e(TAG, "setDefaultScene() - No scene specified");
			return null;
		}
		
		Log.v(TAG, "setDefaultScene() - Scene : ", scene);
		
		// create handle
		DefaultSceneHandle handle = new DefaultSceneHandle(scene);
		m_DefaultSceneHandles.add(handle);
		
		// change default scene
		this.updateDefaultScene();
		if((flags & FLAG_PRESERVE_CURRENT_SCENE) == 0)
			this.setScene(m_DefaultScene, 0);
		
		// complete
		return handle;
	}
	
	
	// Create new scene.
	private boolean createScene(SceneBuilder builder)
	{
		try
		{
			Scene scene = builder.createScene(this.getCameraActivity());
			if(scene != null)
			{
				Log.v(TAG, "createScene() - Scene : ", scene);
				scene.addCallback(Scene.PROP_STATE, m_SceneStateChangedCallback);
				m_Scenes.add(scene);
				if(scene.get(Scene.PROP_STATE) != State.DISABLED)
				{
					m_ActiveScenes.add(scene);
					this.raise(EVENT_SCENE_ADDED, new SceneEventArgs(scene));
				}
				return true;
			}
			Log.e(TAG, "createScene() - No scene created by " + builder);
			return false;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "createScene() - Fail to create scene by " + builder, ex);
			return false;
		}
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_SCENE)
			return (TValue)m_Scene;
		return super.get(key);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
	}
	
	
	// Called when scene state changed to DISABLED.
	private void onSceneDisabled(Scene scene)
	{
		if(m_ActiveScenes.remove(scene))
		{
			// update default scene
			if(!m_DefaultSceneHandles.isEmpty() && m_DefaultSceneHandles.get(m_DefaultSceneHandles.size() - 1).scene == scene)
			{
				Log.w(TAG, "onSceneDisabled() - Default scene '" + scene + "' disabled");
				this.updateDefaultScene();
			}
			
			// exit this scene
			if(m_Scene == scene)
			{
				Log.w(TAG, "onSceneDisabled() - Scene '" + scene + "' has been disabled when using, exit from this scene");
				this.setScene(Scene.NO_SCENE, 0);
			}
			
			// raise event
			this.raise(EVENT_SCENE_REMOVED, new SceneEventArgs(scene));
		}
	}
	
	
	// Called when scene state changed from DISABLED.
	private void onSceneEnabled(Scene scene)
	{
		int index = m_Scenes.indexOf(scene);
		if(index < 0)
			return;
		for(int i = 0, count = m_ActiveScenes.size() ; i <= count ; ++i)
		{
			if(i < count)
			{
				Scene activeScene = m_ActiveScenes.get(i);
				if(activeScene == scene)
					return;
				if(m_Scenes.indexOf(activeScene) > index)
				{
					m_ActiveScenes.add(i, scene);
					break;
				}
			}
			else
				m_ActiveScenes.add(scene);
		}
		if(!m_DefaultSceneHandles.isEmpty() && m_DefaultSceneHandles.get(m_DefaultSceneHandles.size() - 1).scene == scene)
		{
			Log.w(TAG, "onSceneEnabled() - Default scene '" + scene + "' enabled");
			this.updateDefaultScene();
		}
		this.raise(EVENT_SCENE_ADDED, new SceneEventArgs(scene));
	}
	
	
	// Called when scene state changed to RELEASED.
	private void onSceneReleased(Scene scene)
	{
		if(m_ActiveScenes.remove(scene))
		{
			// update default scene
			if(!m_DefaultSceneHandles.isEmpty() && m_DefaultSceneHandles.get(m_DefaultSceneHandles.size() - 1).scene == scene)
			{
				Log.w(TAG, "onSceneReleased() - Default scene '" + scene + "' released");
				this.updateDefaultScene();
			}
			
			// exit this scene
			if(m_Scene == scene)
			{
				Log.w(TAG, "onSceneReleased() - Scene '" + scene + "' has been released when using, exit from this scene");
				this.setScene(Scene.NO_SCENE, 0);
			}
			
			// raise event
			this.raise(EVENT_SCENE_REMOVED, new SceneEventArgs(scene));
		}
		if(m_Scenes.remove(scene))
			scene.removeCallback(Scene.PROP_STATE, m_SceneStateChangedCallback);
	}
	
	
	// Restore default scene.
	private void restoreDefaultScene(DefaultSceneHandle handle, int flags)
	{
		// remove handle
		this.verifyAccess();
		boolean isLast = ListUtils.isLastObject(m_DefaultSceneHandles, handle);
		if(!m_DefaultSceneHandles.remove(handle))
			return;
		
		// restore
		if(isLast)
		{
			// update default scene
			this.updateDefaultScene();
			
			// change scene
			if((flags & FLAG_PRESERVE_CURRENT_SCENE) == 0)
				this.setScene(m_DefaultScene, 0);
		}
	}

	
	// Change current scene.
	@Override
	public boolean setScene(Scene scene, int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "setScene() - Component is not running");
			return false;
		}
		
		// check scene
		if(scene == null)
		{
			Log.e(TAG, "setScene() - No scene to change");
			return false;
		}
		if(scene != Scene.NO_SCENE && !m_ActiveScenes.contains(scene))
		{
			Log.e(TAG, "setScene() - Scene '" + scene + "' is not contained in list");
			return false;
		}
		if(m_Scene == scene)
			return true;
		
		// stop preview first
		CameraActivity cameraActivity = this.getCameraActivity();
		boolean restartPreview;
		switch(cameraActivity.get(CameraActivity.PROP_CAMERA_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				Log.w(TAG, "setScene() - Stop preview to change scene");
				cameraActivity.stopCameraPreview();
				restartPreview = true;
				break;
			default:
				restartPreview = false;
				break;
		}
		
		// exit current scene
		Log.v(TAG, "setScene() - Exit from '", m_Scene, "'");
		m_Scene.exit(scene, Scene.FLAG_PRESERVE_CAMERA_PREVIEW_STATE);
		
		// enter next scene
		try
		{
			// enter next scene
			Log.v(TAG, "setScene() - Enter to '", scene, "'");
			if(scene != Scene.NO_SCENE && !scene.enter(m_Scene, Scene.FLAG_PRESERVE_CAMERA_PREVIEW_STATE))
			{
				Log.e(TAG, "setScene() - Fail to enter '" + scene + "', go back to previous scene");
				if(!this.setScene(m_Scene, 0))
					throw new RuntimeException("Fail to change scene.");
				return false;
			}
			
			// update property
			Scene oldScene = m_Scene;
			m_Scene = scene;
			this.notifyPropertyChanged(PROP_SCENE, oldScene, scene);
			
			// complete
			return true;
		}
		finally
		{
			if(restartPreview)
			{
				Log.w(TAG, "setScene() - Restart preview");
				cameraActivity.startCameraPreview();
			}
		}
	}
	
	
	// Update default scene according to current state.
	private void updateDefaultScene()
	{
		if(!m_DefaultSceneHandles.isEmpty())
		{
			m_DefaultScene = m_DefaultSceneHandles.get(m_DefaultSceneHandles.size() - 1).scene;
			if(!m_ActiveScenes.contains(m_DefaultScene))
			{
				Log.e(TAG, "updateDefaultScene() - Scene : " + m_DefaultScene + " is not contained in active list");
				m_DefaultScene = Scene.NO_SCENE;
			}
		}
		else
			m_DefaultScene = Scene.NO_SCENE;
		Log.v(TAG, "updateDefaultScene() - Default scene : ", m_DefaultScene);
	}
}
