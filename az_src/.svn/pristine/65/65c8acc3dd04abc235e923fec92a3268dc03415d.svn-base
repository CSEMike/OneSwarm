package com.aelitis.azureus.core.peermanager.control.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.control.SpeedTokenDispenser;
import com.aelitis.azureus.core.util.FeatureAvailability;

public class 
SpeedTokenDispenserPrioritised 
	implements SpeedTokenDispenser
{
	// crude TBF implementation
	private int		rateKiB;
	{
		COConfigurationManager.addAndFireParameterListeners(new String[] { "Max Download Speed KBs", "Use Request Limiting" }, new ParameterListener()
		{
			public void parameterChanged(String parameterName) {
				rateKiB = COConfigurationManager.getIntParameter("Max Download Speed KBs");
				if (!COConfigurationManager.getBooleanParameter("Use Request Limiting") || !FeatureAvailability.isRequestLimitingEnabled())
					rateKiB = 0;
				
					// sanity check 
				if ( rateKiB < 0 ){	
					rateKiB = 0;
				}
				
				threshold = Math.max(BUCKET_THRESHOLD_FACTOR*rateKiB, BUCKET_THRESHOLD_LOWER_BOUND);
				lastTime = currentTime - 1; // shortest possible delta
				refill(); // cap buffer to threshold in case something accumulated
			}
		});
	}
	private long	threshold;
	private long	bucket		= 0;
	private long	lastTime	= SystemTime.getCurrentTime();
	private long	currentTime;

	public void update(long newTime) {
		currentTime = newTime;
	}

	// allow at least 2 outstanding requests
	private static final int	BUCKET_THRESHOLD_LOWER_BOUND	= 2 * DiskManager.BLOCK_SIZE;
	// time (in seconds) at max speed until the buffer is empty: too low = latency issues; too high = overshooting for too long
	private static final int	BUCKET_RESPONSE_TIME			= 1;
	// n KiB buffer per 1KiB/s speed, that should be roughly n seconds max response time
	private static final int	BUCKET_THRESHOLD_FACTOR			= 1024 * BUCKET_RESPONSE_TIME;

	public void refill() {
		if (lastTime == currentTime || rateKiB == 0)
			return;
		
		if ( lastTime > currentTime ){
			lastTime = currentTime;
			return;
		}
		
		if ( bucket < 0 ){
			Debug.out( "Bucket is more than empty! - " + bucket );
			bucket = 0;
		}
		long delta = currentTime - lastTime;
		lastTime = currentTime;
		// upcast to long since we might exceed int-max when rate and delta are
		// large enough; then downcast again...
		long tickDelta = ( rateKiB * 1024L * delta) / 1000;
		//System.out.println("threshold:" + threshold + " update: " + bucket + " time delta:" + delta);
		bucket += tickDelta;
		if (bucket > threshold)
			bucket = threshold;
	}

	public int dispense(int numberOfChunks, int chunkSize) {
		if (rateKiB == 0)
			return numberOfChunks;
		if (chunkSize > bucket)
			return 0;
		if (chunkSize * numberOfChunks <= bucket)
		{
			bucket -= chunkSize * numberOfChunks;
			return numberOfChunks;
		}
		int availableChunks = (int)( bucket / chunkSize );
		bucket -= chunkSize * availableChunks;
		return availableChunks;
	}

	public void returnUnusedChunks(int unused, int chunkSize) {
		bucket += unused * chunkSize;
	}

	public int peek(int chunkSize) {
		if (rateKiB != 0)
			return (int)( bucket / chunkSize );
		else
			return Integer.MAX_VALUE;
	}
}
