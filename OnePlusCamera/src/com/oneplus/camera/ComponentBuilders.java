package com.oneplus.camera;

import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.camera.capturemode.CaptureModeManagerBuilder;
import com.oneplus.camera.location.LocationManagerBuilder;
import com.oneplus.camera.media.AudioManagerBuilder;
import com.oneplus.camera.scene.SceneManagerBuilder;
import com.oneplus.camera.slowmotion.SlowMotionControllerBuilder;
import com.oneplus.camera.slowmotion.SlowMotionUIBuilder;
import com.oneplus.camera.timelapse.TimelapseControllerBuilder;
import com.oneplus.camera.timelapse.TimelapseUIBuilder;
import com.oneplus.camera.ui.CaptureBarBuilder;
import com.oneplus.camera.ui.CaptureModeSwitcherBuilder;
import com.oneplus.camera.ui.CountDownTimerIndicatorBuilder;
import com.oneplus.camera.ui.OptionsPanelBuilder;
import com.oneplus.camera.ui.PreviewGalleryBuilder;
import com.oneplus.camera.ui.FocusExposureIndicatorBuilder;
import com.oneplus.camera.ui.RecordingTimerUIBuilder;
import com.oneplus.camera.ui.TouchFocusExposureUIBuilder;

final class ComponentBuilders
{
	static final ComponentBuilder[] BUILDERS_CAMERA_THREAD = new ComponentBuilder[]{
		new AudioManagerBuilder(),
		new LocationManagerBuilder(),
		new SlowMotionControllerBuilder(),
		new TimelapseControllerBuilder(),
	};
	
	
	static final ComponentBuilder[] BUILDERS_MAIN_ACTIVITY = new ComponentBuilder[]{
		//new AudioManagerBuilder(),
		new CaptureBarBuilder(),
		new CaptureModeManagerBuilder(),
		new CaptureModeSwitcherBuilder(),
		new CountDownTimerBuilder(),
		new CountDownTimerIndicatorBuilder(),
		new FlashControllerBuilder(),
		new FocusExposureIndicatorBuilder(),
		new LocationManagerBuilder(),
		new OptionsPanelBuilder(),
		new PreviewGalleryBuilder(),
		new RecordingTimerUIBuilder(),
		new SceneManagerBuilder(),
		new SensorFocusControllerBuilder(),
		new SlowMotionUIBuilder(),
		new TimelapseUIBuilder(),
		new TouchFocusExposureUIBuilder(),
	};
}
