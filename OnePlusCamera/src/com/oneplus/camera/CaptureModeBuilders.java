package com.oneplus.camera;

import com.oneplus.camera.capturemode.CaptureModeBuilder;
import com.oneplus.camera.capturemode.PhotoCaptureModeBuilder;
import com.oneplus.camera.capturemode.VideoCaptureModeBuilder;
import com.oneplus.camera.slowmotion.SlowMotionCaptureModeBuilder;
import com.oneplus.camera.timelapse.TimelapseCaptureModeBuilder;

final class CaptureModeBuilders
{
	public static final CaptureModeBuilder[] BUILDERS = new CaptureModeBuilder[]{
		new PhotoCaptureModeBuilder(),
		new VideoCaptureModeBuilder(),
		new SlowMotionCaptureModeBuilder(),
		new TimelapseCaptureModeBuilder(),
	};
}
