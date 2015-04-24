package com.oneplus.base;

import java.util.LinkedList;

import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;

/**
 * Thread monitor.
 */
public final class ThreadMonitor
{
	// Constants
	private static final String TAG = "ThreadMonitor";
	private static final long THREAD_CHECK_INTERVAL = 3000;
	
	
	// Private static fields
	private static final LinkedList<ThreadInfo> m_AllThreadInfos = new LinkedList<>();
	private static final ThreadLocal<ThreadInfo> m_CurrentThreadInfo = new ThreadLocal<>();
	private static volatile boolean m_IsPrepared;
	private static volatile Thread m_MonitorThread;
	
	
	// Class for monitored thread info
	private static final class ThreadInfo
	{
		public final Handler handler;
		public volatile long lastResponseTime;
		public volatile boolean notResponding;
		public volatile int pendingResponseCount;
		public final Thread thread;
		public final int threadId;
		
		public ThreadInfo()
		{
			this.thread = Thread.currentThread();
			this.threadId = Process.myTid();
			this.handler = new Handler();
			this.lastResponseTime = SystemClock.elapsedRealtime();
			m_CurrentThreadInfo.set(this);
		}
	}
	
	
	// Response call-back
	private static final Runnable m_ResponseCallback = new Runnable()
	{
		@Override
		public void run()
		{
			ThreadInfo threadInfo = m_CurrentThreadInfo.get();
			synchronized(threadInfo)
			{
				--threadInfo.pendingResponseCount;
				threadInfo.lastResponseTime = SystemClock.elapsedRealtime();
				if(threadInfo.notResponding && threadInfo.pendingResponseCount <= 0)
				{
					threadInfo.notResponding = false;
					Log.w(TAG, "Get response from thread '" + threadInfo.thread.getName() + "' (" + threadInfo.threadId + ")");
				}
			}
		}
	};
	
	
	// Constructor
	private ThreadMonitor()
	{}
	
	
	/**
	 * Prepare thread monitor in order to monitor threads.
	 */
	public static synchronized void prepare()
	{
		// check state
		if(m_IsPrepared)
			return;
		
		Log.w(TAG, "prepare()");
		
		// start monitor thread
		m_MonitorThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				threadMonitorProc();
			}
		});
		m_MonitorThread.setName("Thread monitor");
		m_MonitorThread.start();
		m_IsPrepared = true;
	}
	
	
	// Print logs for thread blocking.
	private static void printThreadBlockedLogs(ThreadInfo threadInfo)
	{
		// check response time
		long duration = (SystemClock.elapsedRealtime() - threadInfo.lastResponseTime);
		
		// print header
		Log.w(TAG, String.format("Thread '%s' (%d) is not responding, last response time is %.2f seconds ago. Stack trace :", threadInfo.thread.getName(), threadInfo.threadId, (duration / 1000.0)));
		
		// print stack trace
		StackTraceElement[] stackTrace = threadInfo.thread.getStackTrace();
		for(int i = 0 ; i < stackTrace.length ; ++i)
			Log.w(TAG, "  -> " + Log.formatStackTraceElement(stackTrace[i]));
	}
	
	
	/**
	 * Release all resources used by thread monitor.
	 */
	public static synchronized void release()
	{
		// check state
		if(!m_IsPrepared)
			return;
		
		Log.w(TAG, "release()");
		
		// stop monitor threads
		if(m_MonitorThread != null)
		{
			m_MonitorThread.interrupt();
			m_MonitorThread = null;
		}
		
		// clear thread info
		m_AllThreadInfos.clear();
		m_IsPrepared = false;
	}
	
	
	/**
	 * Start monitoring current thread.
	 */
	public static synchronized void startMonitorCurrentThread()
	{
		// check state
		if(!m_IsPrepared)
			return;
		ThreadInfo threadInfo = m_CurrentThreadInfo.get();
		if(threadInfo != null)
			return;
		
		// create thread info
		threadInfo = new ThreadInfo();
		m_AllThreadInfos.add(threadInfo);
		
		Log.w(TAG, "Start monitor '" + threadInfo.thread.getName() + "' (" + threadInfo.threadId + ")");
	}
	
	
	/**
	 * Stop monitoring current thread.
	 */
	public static synchronized void stopMonitorCurrentThread()
	{
		// check state
		if(!m_IsPrepared)
			return;
		
		// get thread info
		ThreadInfo threadInfo = m_CurrentThreadInfo.get();
		if(threadInfo == null)
			return;
		m_AllThreadInfos.remove(threadInfo);
		
		Log.w(TAG, "Stop monitor '" + threadInfo.thread.getName() + "' (" + threadInfo.threadId + ")");
	}
	
	
	// Monitor thread entry.
	private static void threadMonitorProc()
	{
		Log.w(TAG, "***** Monitor thread start *****");
		try
		{
			while(true)
			{
				synchronized(ThreadMonitor.class)
				{
					for(int i = m_AllThreadInfos.size() - 1 ; i >= 0 ; --i)
					{
						// check response
						ThreadInfo threadInfo = m_AllThreadInfos.get(i);
						if(threadInfo.pendingResponseCount > 0)
						{
							threadInfo.notResponding = true;
							printThreadBlockedLogs(threadInfo);
							continue;
						}
						
						// request response
						threadInfo.handler.post(m_ResponseCallback);
						++threadInfo.pendingResponseCount;
					}
				}
				Thread.sleep(THREAD_CHECK_INTERVAL);
			}
		}
		catch(InterruptedException ex)
		{}
		finally
		{
			Log.w(TAG, "***** Monitor thread stop *****");
		}
	}
}
