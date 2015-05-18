package com.oneplus.camera;

import com.oneplus.camera.scene.HdrSceneBuilder;
import com.oneplus.camera.scene.PhotoFaceBeautySceneBuilder;
import com.oneplus.camera.scene.SceneBuilder;

class SceneBuilders
{
	static final SceneBuilder[] BUILDERS = new SceneBuilder[]{
		new PhotoFaceBeautySceneBuilder(),
		new HdrSceneBuilder(),
	};
}
