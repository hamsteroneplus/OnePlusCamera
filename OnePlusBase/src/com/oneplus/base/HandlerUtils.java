package com.oneplus.base;

import android.os.Handler;
import android.os.Message;

/**
 * Utility methods for {@link android.os.Handler} and {@link HandlerObject}.
 */
public final class HandlerUtils
{
	// Constants
	private static final String TAG = "HandlerUtils";
	
	
	// Constructor
	private HandlerUtils()
	{}
	
	
	/**
	 * Check whether given message is contained in given handler object or not.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @return Whether message is contained in given handler object or not.
	 */
	public static boolean hasMessages(HandlerObject target, int what)
	{
		if(target == null)
			return false;
		Handler handler = target.getHandler();
		if(handler == null)
			return false;
		return handler.hasMessages(what);
	}
	
	
	/**
	 * Post call-back to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param r Call-back to post.
	 * @return Whether call-back be post successfully or not.
	 */
	public static boolean post(HandlerObject target, Runnable r)
	{
		return post(target, r, 0);
	}
	
	
	/**
	 * Post call-back to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param r Call-back to post.
	 * @param delayMillis Delay time in milliseconds.
	 * @return Whether call-back be post successfully or not.
	 */
	public static boolean post(HandlerObject target, Runnable r, long delayMillis)
	{
		// check handler
		if(target == null)
		{
			android.util.Log.e(TAG, "post() - No target to post");
			return false;
		}
		Handler handler = target.getHandler();
		if(handler == null)
		{
			android.util.Log.e(TAG, "post() - No Handler to post");
			return false;
		}
		
		// post
		if(delayMillis <= 0)
			return handler.post(r);
		return handler.postDelayed(r, delayMillis);
	}
	
	
	/**
	 * Remove call-back from {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param r Call-back to remove.
	 */
	public static void removeCallbacks(HandlerObject target, Runnable r)
	{
		if(target == null)
			return;
		Handler handler = target.getHandler();
		if(handler == null)
			return;
		handler.removeCallbacks(r);
	}
	
	
	/**
	 * Remove message from {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 */
	public static void removeMessages(HandlerObject target, int what)
	{
		if(target == null)
			return;
		Handler handler = target.getHandler();
		if(handler == null)
			return;
		handler.removeMessages(what);
	}
	
	
	/**
	 * Remove message from {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param obj Object attached in message.
	 */
	public static void removeMessages(HandlerObject target, int what, Object obj)
	{
		if(target == null)
			return;
		Handler handler = target.getHandler();
		if(handler == null)
			return;
		handler.removeMessages(what, obj);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what)
	{
		return sendMessage(target, what, 0, 0, null, false, 0);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param delayMillis Delay time in milliseconds.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, long delayMillis)
	{
		return sendMessage(target, what, 0, 0, null, false, delayMillis);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param uniqueMessage True to remove message before sending.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, boolean uniqueMessage)
	{
		return sendMessage(target, what, 0, 0, null, uniqueMessage, 0);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param uniqueMessage True to remove message before sending.
	 * @param delayMillis Delay time in milliseconds.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, boolean uniqueMessage, long delayMillis)
	{
		return sendMessage(target, what, 0, 0, null, uniqueMessage, delayMillis);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param arg1 Argument 1.
	 * @param arg2 Argument 2.
	 * @param obj Object.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, int arg1, int arg2, Object obj)
	{
		return sendMessage(target, what, arg1, arg2, obj, false, 0);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param arg1 Argument 1.
	 * @param arg2 Argument 2.
	 * @param obj Object.
	 * @param delayMillis Delay time in milliseconds.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, int arg1, int arg2, Object obj, long delayMillis)
	{
		return sendMessage(target, what, arg1, arg2, obj, false, delayMillis);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param arg1 Argument 1.
	 * @param arg2 Argument 2.
	 * @param obj Object.
	 * @param uniqueMessage True to remove message before sending.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, int arg1, int arg2, Object obj, boolean uniqueMessage)
	{
		return sendMessage(target, what, arg1, arg2, obj, uniqueMessage, 0);
	}
	
	
	/**
	 * Send message to given {@link HandlerObject}.
	 * @param target Target {@link HandlerObject}.
	 * @param what Message.
	 * @param arg1 Argument 1.
	 * @param arg2 Argument 2.
	 * @param obj Object.
	 * @param uniqueMessage True to remove message before sending.
	 * @param delayMillis Delay time in milliseconds.
	 * @return Whether message be sent successfully or not.
	 */
	public static boolean sendMessage(HandlerObject target, int what, int arg1, int arg2, Object obj, boolean uniqueMessage, long delayMillis)
	{
		// check handler
		if(target == null)
		{
			android.util.Log.e(TAG, "sendMessage() - No target to send " + what);
			return false;
		}
		Handler handler = target.getHandler();
		if(handler == null)
		{
			android.util.Log.e(TAG, "sendMessage() - No Handler to send " + what);
			return false;
		}
		
		// send message
		if(uniqueMessage)
			handler.removeMessages(what);
		Message msg = Message.obtain(handler, what, arg1, arg2, obj);
		if(delayMillis <= 0)
			return handler.sendMessage(msg);
		return handler.sendMessageDelayed(msg, delayMillis);
	}
}
