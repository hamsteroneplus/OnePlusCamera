package com.oneplus.gl;

import java.lang.ref.WeakReference;

final class EglObjectHolder
{
	public WeakReference<EglObject> eglObject;
	public EglObjectHolder prevHolder;
	public EglObjectHolder nextHolder;
}
