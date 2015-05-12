package com.oneplus.camera;

import android.os.SystemClock;

import com.oneplus.base.EventArgs;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;

public class CountDownTimerImpl extends CameraComponent implements CountDownTimer {
	
	private final long m_Interval = 1000;
	// compute data
	private long m_CountdownSecs = 0;
	private long m_ElapsedTime = 0;
	private Handle m_CurrentHandle = null;
	// operations
	private Handle m_CloseHandle = new Handle("CountDownTimer"){
		// handle user cancel
		@Override
		protected void onClose(int flags) {
			if(m_CurrentHandle != null && this == m_CurrentHandle){
				raise(EVENT_CANCELLED,  EventArgs.EMPTY);
				// stop timer
				HandlerUtils.removeCallbacks(CountDownTimerImpl.this, m_Timer);
				// reset compute data
				resetComputeData();
			}
		}};;
	private Runnable m_Timer = new Runnable() {
		   @Override
		   public void run() {
			  // notify count down
			  setReadOnly(PROP_REMAINING_SECONDS, --m_CountdownSecs);
			  // repeat or finish
			  if(m_CountdownSecs != 0){
				  // compensate timer deviation
				  long currentTime = SystemClock.elapsedRealtime();
				  HandlerUtils.post(CountDownTimerImpl.this, this, m_Interval - (currentTime - m_ElapsedTime - 1000));
			      m_ElapsedTime = currentTime;
			  }else{
				  // reset compute data
				  resetComputeData();
			  }
		   }};

	// Constructor
	CountDownTimerImpl(CameraActivity cameraActivity)
	{
		super("CountDown Timer manager", cameraActivity, true);
	}	

	@Override
	public Handle start(long seconds, int flags) {
		if(m_CurrentHandle != null){
			return null;
		}
		verifyAccess();
		//init count down
		m_CurrentHandle = m_CloseHandle;
		m_CountdownSecs = seconds;
		setReadOnly(PROP_IS_STARTED, true);
		setReadOnly(PROP_REMAINING_SECONDS, m_CountdownSecs);
		m_ElapsedTime = SystemClock.elapsedRealtime();	
		//start timer
		HandlerUtils.post(CountDownTimerImpl.this, m_Timer, m_Interval);
		
		return m_CurrentHandle;
	}
	
	// reset compute data
	void resetComputeData(){
		setReadOnly(PROP_IS_STARTED, false);
		m_CurrentHandle = null;
		m_ElapsedTime = 0;
		m_CountdownSecs = 0;
	}
}
