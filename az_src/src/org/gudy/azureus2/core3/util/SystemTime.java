/*
 * Created on Apr 16, 2004 Created by Alon Rohter Copyright (C) 2004, 2005, 2006
 * Aelitis, All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros 8 Allee Lenotre, La Grille Royale,
 * 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.core3.util;

import java.util.*;

/**
 * Utility class to retrieve current system time, and catch clock backward time
 * changes.
 */
public class SystemTime {
	public static final long			TIME_GRANULARITY_MILLIS	= 25;	//internal update time ms
	private static SystemTimeProvider	instance;
	static
	{
		try
		{
			if (System.getProperty("azureus.time.use.raw.provider", "0").equals("1"))
			{
				System.out.println("Warning: Using Raw Provider, monotonous time might be inaccurate");
				instance = new RawProvider();
			} else
			{
				instance = new SteppedProvider();
			}
		} catch (Throwable e)
		{
			// might be in applet...
			instance = new SteppedProvider();
		}
	}

	public static void useRawProvider() {
		if (!(instance instanceof RawProvider))
		{
			instance = new RawProvider();
		}
	}

	private static volatile List		systemTimeConsumers		= new ArrayList();
	private static volatile List		monotoneTimeConsumers	= new ArrayList();
	private static volatile List		clock_change_list		= new ArrayList();
	private static HighPrecisionCounter	high_precision_counter;
	private static long					hpc_base_time;
	private static long					hpc_last_time;

	protected interface SystemTimeProvider {
		public long getTime();

		public long getMonoTime();
	}

	protected static class SteppedProvider implements SystemTimeProvider {
		private static final int	STEPS_PER_SECOND	= (int) (1000 / TIME_GRANULARITY_MILLIS);
		private final Thread		updater;
		private volatile long		stepped_time;
		private volatile long		currentTimeOffset = System.currentTimeMillis();
		private volatile long		last_approximate_time;
		private volatile int		access_count;
		private volatile int		slice_access_count;
		private volatile int		access_average_per_slice;
		private volatile int		drift_adjusted_granularity;

		private SteppedProvider()
		{
			stepped_time = 0;
			
			updater = new Thread("SystemTime")
			{
				public void run() {
					long adjustedTimeOffset = currentTimeOffset;
					// these averages rely on monotone time, thus won't be affected by system time changes
					final Average access_average = Average.getInstance(1000, 10);
					final Average drift_average = Average.getInstance(1000, 10);
					long lastOffset = adjustedTimeOffset;
					long lastSecond = -1000;
					int tick_count = 0;
					while (true)
					{
						long rawTime = System.currentTimeMillis();
						/*
						 * keep the monotone time in sync with the raw system
						 * time, for this we need to know the offset of the
						 * current time to the system time
						 */
						long newMonotoneTime = rawTime - adjustedTimeOffset;
						long delta = newMonotoneTime - stepped_time;
						/*
						 * unless the system time jumps, then we just guess the
						 * time that has passed and adjust the update, so that
						 * the next round can be in sync with the system time
						 * again
						 */
						if (delta < 0 || delta > 1000)
						{
							/*
							 * jump occured, update monotone time offset, but
							 * not the current time one, that only happens every
							 * second
							 */ 
							stepped_time += TIME_GRANULARITY_MILLIS;
							adjustedTimeOffset = rawTime - stepped_time;
						} else
						{ // time is good, keep it
							stepped_time = newMonotoneTime;
						}
						tick_count++;
						if (tick_count == STEPS_PER_SECOND)
						{
							if (lastOffset != adjustedTimeOffset)
							{
								final long change = adjustedTimeOffset - lastOffset;
								Iterator it = clock_change_list.iterator();
								//Debug.outNoStack("Clock change of " + change + "ms detected");
								while (it.hasNext())
								{
									((ChangeListener) it.next()).clockChanged(rawTime, change);
								}
								lastOffset = adjustedTimeOffset;
								// make the internal offset publicly visible after consumers have been notified
								currentTimeOffset = adjustedTimeOffset;
							}
							// averaging magic to estimate the amount of time that passes between each getTime invocation
							long drift = stepped_time - lastSecond - 1000;
							lastSecond = stepped_time;
							drift_average.addValue(drift);
							drift_adjusted_granularity = (int) (TIME_GRANULARITY_MILLIS + (drift_average.getAverage() / STEPS_PER_SECOND));
							access_average.addValue(access_count);
							access_average_per_slice = (int) (access_average.getAverage() / STEPS_PER_SECOND);
							//System.out.println( "access count = " + access_count + ", average = " + access_average.getAverage() + ", per slice = " + access_average_per_slice + ", drift = " + drift +", average = " + drift_average.getAverage() + ", dag =" + drift_adjusted_granularity );
							access_count = 0;
							tick_count = 0;
						}
						slice_access_count = 0;
						
						// copy reference since we use unsynced COW semantics
						List consumersRef = monotoneTimeConsumers;
						for (int i = 0; i < consumersRef.size(); i++)
						{
							TickConsumer cons = (TickConsumer) consumersRef.get(i);
							try
							{
								cons.consume(stepped_time);
							} catch (Throwable e)
							{
								Debug.printStackTrace(e);
							}
						}
						
						/*
						 * notify consumers with the external offset, internal
						 * offset is only meant for updates
						 */
						consumersRef = systemTimeConsumers;
						long adjustedTime = stepped_time + currentTimeOffset;
						for (int i = 0; i < consumersRef.size(); i++)
						{
							TickConsumer cons = (TickConsumer) consumersRef.get(i);
							try
							{
								cons.consume(adjustedTime);
							} catch (Throwable e)
							{
								Debug.printStackTrace(e);
							}
						}
						
						try
						{
							Thread.sleep(TIME_GRANULARITY_MILLIS);
						} catch (Exception e)
						{
							Debug.printStackTrace(e);
						}
					}
				}
			};
			updater.setDaemon(true);
			// we don't want this thread to lag much as it'll stuff up the upload/download rate mechanisms (for example)
			updater.setPriority(Thread.MAX_PRIORITY);
			updater.start();
		}

		public long getTime() {
			return getMonoTime() + currentTimeOffset;
		}

		public long getMonoTime() {
			long adjusted_time = stepped_time;
			long averageSliceStep = access_average_per_slice;
			if (averageSliceStep > 0)
			{
				long sliceStep = (drift_adjusted_granularity * slice_access_count) / averageSliceStep;
				if (sliceStep >= drift_adjusted_granularity)
				{
					sliceStep = drift_adjusted_granularity - 1;
				}
				adjusted_time += sliceStep;
			}
			access_count++;
			slice_access_count++;
			// make sure we don't go backwards
			if (adjusted_time < last_approximate_time)
				adjusted_time = last_approximate_time;
			else
				last_approximate_time = adjusted_time;
			return adjusted_time;
		}
	}

	protected static class RawProvider implements SystemTimeProvider {
		private static final int	STEPS_PER_SECOND	= (int) (1000 / TIME_GRANULARITY_MILLIS);
		private final Thread		updater;
		private volatile long		adjustedTimeOffset;

		private RawProvider()
		{
			System.out.println("SystemTime: using raw time provider");
			updater = new Thread("SystemTime")
			{
				long	last_time;

				public void run() {
					while (true)
					{
						long current_time = getTime();
						if (last_time != 0)
						{
							long offset = current_time - last_time;
							if (offset < 0 || offset > 5000)
							{
								// clock's changed
								adjustedTimeOffset += offset;
								Iterator it = clock_change_list.iterator();
								while (it.hasNext())
								{
									((ChangeListener) it.next()).clockChanged(current_time, offset);
								}
							}
						}
						last_time = current_time;
						List consumer_list_ref = systemTimeConsumers;
						for (int i = 0; i < consumer_list_ref.size(); i++)
						{
							TickConsumer cons = (TickConsumer) consumer_list_ref.get(i);
							try
							{
								cons.consume(current_time);
							} catch (Throwable e)
							{
								Debug.printStackTrace(e);
							}
						}
						consumer_list_ref = monotoneTimeConsumers;
						long adjustedTime = current_time - adjustedTimeOffset;
						for (int i = 0; i < consumer_list_ref.size(); i++)
						{
							TickConsumer cons = (TickConsumer) consumer_list_ref.get(i);
							try
							{
								cons.consume(adjustedTime);
							} catch (Throwable e)
							{
								Debug.printStackTrace(e);
							}
						}
						try
						{
							Thread.sleep(TIME_GRANULARITY_MILLIS);
						} catch (Exception e)
						{
							Debug.printStackTrace(e);
						}
					}
				}
			};
			updater.setDaemon(true);
			// we don't want this thread to lag much as it'll stuff up the upload/download rate mechanisms (for example)
			updater.setPriority(Thread.MAX_PRIORITY);
			updater.start();
		}

		public long getTime() {
			return System.currentTimeMillis();
		}

		/**
		 * This implementation does not guarantee monotonous time increases with
		 * 100% accuracy as the adjustedTimeOffset is only adjusted every
		 * TIME_GRANULARITY_MILLIS
		 */
		public long getMonoTime() {
			return getTime() - adjustedTimeOffset;
		}
	}

	/**
	 * Note that this can this time can jump into the future or past due to
	 * clock adjustments use getMonotonousTime() if you need steady increases
	 * 
	 * @return current system time in millisecond since epoch
	 */
	public static long getCurrentTime() {
		return (instance.getTime());
	}

	/**
	 * Time that is guaranteed to grow monotonously and also ignores larger
	 * jumps into the future which might be caused by adjusting the system clock<br>
	 * <br>
	 * 
	 * <b>Do not mix times retrieved by this method with normal time!</b>
	 * 
	 * TODO once we move to java 1.5 use atomic stuff to harden the guarantee
	 * (multithreaded access can weaken it at the moment)
	 * 
	 * @return the amount of real time passed since the program start in
	 *         milliseconds
	 */
	public static long getMonotonousTime() {
		return instance.getMonoTime();
	}

	public static long getOffsetTime(long offsetMS) {
		return instance.getTime() + offsetMS;
	}

	public static void registerConsumer(TickConsumer c) {
		synchronized (instance)
		{
			List new_list = new ArrayList(systemTimeConsumers);
			new_list.add(c);
			systemTimeConsumers = new_list;
		}
	}

	public static void unregisterConsumer(TickConsumer c) {
		synchronized (instance)
		{
			List new_list = new ArrayList(systemTimeConsumers);
			new_list.remove(c);
			systemTimeConsumers = new_list;
		}
	}

	public static void registerMonotonousConsumer(TickConsumer c) {
		synchronized (instance)
		{
			List new_list = new ArrayList(monotoneTimeConsumers);
			new_list.add(c);
			monotoneTimeConsumers = new_list;
		}
	}

	public static void unregisterMonotonousConsumer(TickConsumer c) {
		synchronized (instance)
		{
			List new_list = new ArrayList(monotoneTimeConsumers);
			new_list.remove(c);
			monotoneTimeConsumers = new_list;
		}
	}

	public static void registerClockChangeListener(ChangeListener c) {
		synchronized (instance)
		{
			List new_list = new ArrayList(clock_change_list);
			new_list.add(c);
			clock_change_list = new_list;
		}
	}

	public static void unregisterClockChangeListener(ChangeListener c) {
		synchronized (instance)
		{
			List new_list = new ArrayList(clock_change_list);
			new_list.remove(c);
			clock_change_list = new_list;
		}
	}

	public interface TickConsumer {
		public void consume(long current_time);
	}

	public interface ChangeListener {
		public void clockChanged(long current_time, long change_millis);
	}

	public static long getHighPrecisionCounter() {
		if (high_precision_counter == null)
		{
			AEDiagnostics.load15Stuff();
			synchronized (SystemTime.class)
			{
				long now = getCurrentTime();
				if (now < hpc_last_time)
				{
					// clock's gone back, by at least
					long gone_back_by_at_least = hpc_last_time - now;
					// all we can do is move the logical start time back too to ensure that our
					// counter doesn't got backwards
					hpc_base_time -= gone_back_by_at_least;
				}
				hpc_last_time = now;
				return ((now - hpc_base_time) * 1000000);
			}
		} else
		{
			return (high_precision_counter.nanoTime());
		}
	}

	public static void registerHighPrecisionCounter(HighPrecisionCounter counter) {
		high_precision_counter = counter;
	}

	public interface HighPrecisionCounter {
		public long nanoTime();
	}

	public static void main(String[] args) {
		for (int i = 0; i < 1; i++)
		{
			//final int f_i = i;
			new Thread()
			{
				public void run() {
					/*
					 * Average access_average = Average.getInstance( 1000, 10 );
					 * 
					 * long last = SystemTime.getCurrentTime();
					 * 
					 * int count = 0;
					 * 
					 * while( true ){
					 * 
					 * long now = SystemTime.getCurrentTime();
					 * 
					 * long diff = now - last;
					 * 
					 * System.out.println( "diff=" + diff );
					 * 
					 * last = now;
					 * 
					 * access_average.addValue( diff );
					 * 
					 * count++;
					 * 
					 * if ( count == 33 ){
					 * 
					 * System.out.println( "AVERAGE " + f_i + " = " +
					 * access_average.getAverage());
					 * 
					 * count = 0; }
					 * 
					 * try{ Thread.sleep( 3 );
					 * 
					 * }catch( Throwable e ){ } }
					 */
					long cstart = SystemTime.getCurrentTime();
					long mstart = SystemTime.getMonotonousTime();
					System.out.println("alter system clock to see differences between monotonous and current time");
					long cLastRound = cstart;
					long mLastRound = mstart;
					while (true)
					{
						long mnow = SystemTime.getMonotonousTime();
						long cnow = SystemTime.getCurrentTime();
						//if(mLastRound > mnow)
						System.out.println("current: " + (cnow - cstart) + " monotonous:" + (mnow - mstart) + " delta current:" + (cnow - cLastRound) + " delta monotonous:" + (mnow - mLastRound));
						cLastRound = cnow;
						mLastRound = mnow;
						try
						{
							Thread.sleep(15);
						} catch (Throwable e)
						{}
					}
				}
			}.start();
		}
	}
}
