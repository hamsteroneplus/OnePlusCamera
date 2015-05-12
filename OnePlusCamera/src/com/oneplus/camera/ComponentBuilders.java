package com.oneplus.camera;

import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.camera.capturemode.CaptureModeManagerBuilder;
import com.oneplus.camera.media.AudioManagerBuilder;
import com.oneplus.camera.ui.CaptureBarBuilder;
import com.oneplus.camera.ui.CaptureModeSwitcherBuilder;

final class ComponentBuilders
{
	static final ComponentBuilder[] BUILDERS_CAMERA_THREAD = new ComponentBuilder[]{
		new AudioManagerBuilder(),
	};
	
	
	static final ComponentBuilder[] BUILDERS_MAIN_ACTIVITY = new ComponentBuilder[]{
		//new AudioManagerBuilder(),
		new CaptureBarBuilder(),
		new CaptureModeManagerBuilder(),
		new CaptureModeSwitcherBuilder(),
		new CountDownTimerBuilder(),
		new FlashControllerBuilder(),
	};
}
