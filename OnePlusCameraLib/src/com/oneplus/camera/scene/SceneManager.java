package com.oneplus.camera.scene;

import java.util.Collections;
import java.util.List;

import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.component.Component;

/**
 * Scene manager interface.
 */
public interface SceneManager extends Component
{
	/**
	 * Flag to indicate that current scene should not be changed after operation completes.
	 */
	int FLAG_PRESERVE_CURRENT_SCENE = 0x1;
	
	
	/**
	 * Read-only property to get current scene.
	 */
	PropertyKey<Scene> PROP_SCENE = new PropertyKey<>("Scene", Scene.class, SceneManager.class, Scene.NO_SCENE);
	/**
	 * Read-only property to get all available scenes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Scene>> PROP_SCENES = new PropertyKey<List<Scene>>("Scenes", (Class)List.class, SceneManager.class, Collections.EMPTY_LIST);
	
	
	/**
	 * Event raised when new scene added.
	 */
	EventKey<SceneEventArgs> EVENT_SCENE_ADDED = new EventKey<>("SceneAdded", SceneEventArgs.class, SceneManager.class);
	/**
	 * Event raised when scene has been removed.
	 */
	EventKey<SceneEventArgs> EVENT_SCENE_REMOVED = new EventKey<>("SceneRemoved", SceneEventArgs.class, SceneManager.class);
	
	
	/**
	 * Add scene builder.
	 * @param builder Builder to add.
	 * @param flags Flags, reserved.
	 * @return Whether builder added successfully or not.
	 */
	boolean addBuilder(SceneBuilder builder, int flags);
	
	
	/**
	 * Change default scene.
	 * @param scene Default scene.
	 * @param flags Flags :
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CURRENT_SCENE}</li>
	 * </ul>
	 * When closing handle, you can use :
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CURRENT_SCENE}</li>
	 * </ul>
	 * @return Handle to default scene.
	 */
	Handle setDefaultScene(Scene scene, int flags);
	
	
	/**
	 * Change current scene.
	 * @param scene Scene to change, or {@link Scene#NO_SCENE} to clear.
	 * @param flags Flags, reserved.
	 * @return Whether scene changes successfully or not.
	 */
	boolean setScene(Scene scene, int flags);
}
