package com.oneplus.base;

/**
 * Logger.
 */
public final class Log
{
	// Private static fields
	private static volatile boolean m_PrintDebugLogs = true;
	private static volatile boolean m_PrintVerboseLogs = true;
	
	
	// Constructor
	private Log()
	{}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 */
	public static void d(String tag, String message)
	{
		if(m_PrintDebugLogs)
			android.util.Log.d(tag, message);
	}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 */
	public static void d(String tag, String message, Object arg1)
	{
		if(m_PrintDebugLogs)
			android.util.Log.d(tag, message + arg1);
	}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 */
	public static void d(String tag, String message, Object arg1, Object arg2)
	{
		if(m_PrintDebugLogs)
			android.util.Log.d(tag, message + arg1 + arg2 );
	}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 * @param arg3 3rd object append to message.
	 */
	public static void d(String tag, String message, Object arg1, Object arg2, Object arg3)
	{
		if(m_PrintDebugLogs)
			android.util.Log.d(tag, message + arg1 + arg2 + arg3);
	}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 * @param arg3 3rd object append to message.
	 * @param arg4 4th object append to message.
	 */
	public static void d(String tag, String message, Object arg1, Object arg2, Object arg3, Object arg4)
	{
		if(m_PrintDebugLogs)
			android.util.Log.d(tag, message + arg1 + arg2 + arg3 + arg4);
	}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 * @param arg3 3rd object append to message.
	 * @param arg4 4th object append to message.
	 * @param arg5 5th object append to message.
	 */
	public static void d(String tag, String message, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
	{
		if(m_PrintDebugLogs)
			android.util.Log.d(tag, message + arg1 + arg2 + arg3 + arg4 + arg5);
	}
	
	
	/**
	 * Print debug log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param args Objects append to message.
	 */
	public static void d(String tag, String message, Object... args)
	{
		if(m_PrintDebugLogs)
		{
			StringBuilder buffer = new StringBuilder(message);
			for(int i = 0 ; i < args.length ; ++i)
				buffer.append(args[i]);
			android.util.Log.d(tag, buffer.toString());
		}
	}
	
	
	/**
	 * Disable debug logs.
	 */
	public static void disableDebugLogs()
	{
		m_PrintDebugLogs = false;
	}
	
	
	/**
	 * Disable verbose logs.
	 */
	public static void disableVerboseLogs()
	{
		m_PrintVerboseLogs = false;
	}
	
	
	/**
	 * Print error log.
	 * @param tag Tag.
	 * @param message Message.
	 */
	public static void e(String tag, String message)
	{
		android.util.Log.e(tag, message);
	}
	
	
	/**
	 * Print error log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param tr Related {@link Throwable}.
	 */
	public static void e(String tag, String message, Throwable tr)
	{
		android.util.Log.e(tag, message, tr);
	}
	
	
	/**
	 * Enable debug logs.
	 */
	public static void enableDebugLogs()
	{
		m_PrintDebugLogs = true;
	}
	
	
	/**
	 * Enable verbose logs.
	 */
	public static void enableVerboseLogs()
	{
		m_PrintVerboseLogs = true;
	}
	
	
	/**
	 * Get formatted string to print {@link StackTraceElement}.
	 * @param element {@link StackTraceElement}.
	 * @return Formatted string.
	 */
	public static String formatStackTraceElement(StackTraceElement element)
	{
		// check class and method name
		StringBuilder buffer = new StringBuilder(element.getClassName());
		buffer.append('.');
		buffer.append(element.getMethodName());
		
		// check method type
		if(element.isNativeMethod())
			buffer.append(" (Native method)");
		
		// check file name
		String fileName = element.getFileName();
		if(fileName != null)
		{
			buffer.append(" (");
			buffer.append(fileName);
			int lineNumber = element.getLineNumber();
			if(lineNumber > 0)
			{
				buffer.append(':');
				buffer.append(lineNumber);
			}
			buffer.append(')');
		}
		
		// complete
		return buffer.toString();
	}
	
	
	/**
	 * Print stack trace of current thread.
	 * @param tag Log tag.
	 */
	public static void printStackTrace(String tag)
	{
		printStackTrace(tag, Thread.currentThread().getStackTrace(), 3);
	}
	
	
	/**
	 * Print given stack trace.
	 * @param tag Log tag.
	 * @param stackTrace Stack trace.
	 */
	public static void printStackTrace(String tag, StackTraceElement[] stackTrace)
	{
		printStackTrace(tag, stackTrace, 0);
	}
	
	
	// Print given stack trace.
	private static void printStackTrace(String tag, StackTraceElement[] stackTrace, int startIndex)
	{
		if(stackTrace != null)
		{
			for(int i = startIndex ; i < stackTrace.length ; ++i)
				Log.w(tag, "  -> " + formatStackTraceElement(stackTrace[i]));
		}
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 */
	public static void v(String tag, String message)
	{
		if(m_PrintVerboseLogs)
			android.util.Log.v(tag, message);
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 */
	public static void v(String tag, String message, Object arg1)
	{
		if(m_PrintVerboseLogs)
			android.util.Log.v(tag, message + arg1);
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 */
	public static void v(String tag, String message, Object arg1, Object arg2)
	{
		if(m_PrintVerboseLogs)
			android.util.Log.v(tag, message + arg1 + arg2 );
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 * @param arg3 3rd object append to message.
	 */
	public static void v(String tag, String message, Object arg1, Object arg2, Object arg3)
	{
		if(m_PrintVerboseLogs)
			android.util.Log.v(tag, message + arg1 + arg2 + arg3);
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 * @param arg3 3rd object append to message.
	 * @param arg4 4th object append to message.
	 */
	public static void v(String tag, String message, Object arg1, Object arg2, Object arg3, Object arg4)
	{
		if(m_PrintVerboseLogs)
			android.util.Log.v(tag, message + arg1 + arg2 + arg3 + arg4);
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param arg1 1st object append to message.
	 * @param arg2 2nd object append to message.
	 * @param arg3 3rd object append to message.
	 * @param arg4 4th object append to message.
	 * @param arg5 5th object append to message.
	 */
	public static void v(String tag, String message, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
	{
		if(m_PrintVerboseLogs)
			android.util.Log.v(tag, message + arg1 + arg2 + arg3 + arg4 + arg5);
	}
	
	
	/**
	 * Print verbose log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param args Objects append to message.
	 */
	public static void v(String tag, String message, Object... args)
	{
		if(m_PrintVerboseLogs)
		{
			StringBuilder buffer = new StringBuilder(message);
			for(int i = 0 ; i < args.length ; ++i)
				buffer.append(args[i]);
			android.util.Log.v(tag, buffer.toString());
		}
	}
	
	
	/**
	 * Print warn log.
	 * @param tag Tag.
	 * @param message Message.
	 */
	public static void w(String tag, String message)
	{
		android.util.Log.w(tag, message);
	}
	
	
	/**
	 * Print warn log.
	 * @param tag Tag.
	 * @param message Message.
	 * @param tr Related {@link Throwable}.
	 */
	public static void w(String tag, String message, Throwable tr)
	{
		android.util.Log.w(tag, message, tr);
	}
}
