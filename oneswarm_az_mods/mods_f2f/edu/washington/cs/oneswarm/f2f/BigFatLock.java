package edu.washington.cs.oneswarm.f2f;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Debug;

public class BigFatLock
	extends ReentrantLock
{

	private static final long			 serialVersionUID	 = 1L;

	private static Logger					 logger						 = Logger.getLogger(BigFatLock.class.getName());

	private long										mMostRecentAcquire;

	/**
	 * These are for debugging.
	 */
	public LinkedList<Long>				 recentRuntimes		 = new LinkedList<Long>();

	public LinkedList<Long>				 recentAcquireTimes = new LinkedList<Long>();

	public long										 longestRunTime		 = 0;

	public long										 longestWaitTime		= 0;

	public long										 waits[][]					= new long[][] {
		{
		0,
		10
		},
		{
		0,
		20
		},
		{
		0,
		50
		},
		{
		0,
		100
		}
																										 };

	private StackTraceElement[] lockedBy;
	
	public static final long				mCreated					 = System.currentTimeMillis();

	private final static BigFatLock instance					 = new BigFatLock();

	public static BigFatLock getInstance(boolean isAEMonitor) {
		/*
		 * if checkloglevel and the log level is <FINE, return null
		 */
		if (isAEMonitor) {
			if (logger.isLoggable(Level.FINE)) {
				return instance;
			} else {
				return null;
			}
		}
		return instance;
	}

	protected BigFatLock() {

	}

	public String getLockedTrace(){
		if(this.isLocked() && lockedBy != null){
			return getStackTraceAsString(lockedBy);
		}
		return "";
	}
	
	public void lock() {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("trying to lock: " + Thread.currentThread().getName()
					+ " count=" + super.getHoldCount());
		}

		if (logger.isLoggable(Level.FINE)) {
			lockedBy = new Exception().getStackTrace();
			if (this.isLocked() && this.isHeldByCurrentThread()) {
				new Exception("Lock called byt thread already owner").printStackTrace();
			}
		}
		
		

		long start = System.currentTimeMillis();
		super.lock();
		long time = (System.currentTimeMillis() - start);
		if (mCreated + 10000 < System.currentTimeMillis()) {
			if (time > longestWaitTime) {
				longestWaitTime = time;
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			synchronized (recentAcquireTimes) {
				recentAcquireTimes.add(time);
				if (recentAcquireTimes.size() > 1000) {
					recentAcquireTimes.removeFirst();
				}

				for (long[] w : waits) {
					if (time > w[1]) {
						w[0]++;
					}
				}
			}
		}
		mMostRecentAcquire = System.currentTimeMillis();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finer("lock: " + Thread.currentThread().getName() + " count="
					+ super.getHoldCount());
		}
	}

	public void unlock() {

		super.unlock();

		long time = System.currentTimeMillis() - mMostRecentAcquire;
		if (mCreated + 10000 < System.currentTimeMillis()) {
			if (time > longestRunTime) {
				longestRunTime = time;
			}
		}
		if (logger.isLoggable(Level.FINE)) {
			synchronized (recentRuntimes) {
				recentRuntimes.add(time);
				if (recentRuntimes.size() > 1000) {
					recentRuntimes.removeFirst();
				}
			}
		}

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("unlock: " + Thread.currentThread().getName() + " count="
					+ super.getHoldCount());
		}
	}

	public String getLockDebug() {
		StringBuilder b = new StringBuilder();

		final Thread owner = getOwner();
		boolean skip = false;
		if (owner == null) {
			b.append("owner: null\n");
			b.append(" count=" + super.getHoldCount() + "\n");
			skip = true;
		}
		if (!skip) {
			b.append("owner: " + owner.getName() + "\n");
			b.append(" count=" + super.getHoldCount() + "\n");
			b.append(getStackTraceAsString(owner.getStackTrace()) + "\n\n");

			b.append("waiting threads:\n");
			final Collection<Thread> queuedThreads = getQueuedThreads();
			for (Thread thread : queuedThreads) {
				b.append("name=" + thread.getName() + "\n");
				b.append("status=" + thread.getState().name() + "\n");
				b.append(getStackTraceAsString(thread.getStackTrace()) + "\n\n");
			}

			b.append("\n\nall other threads:\n");
			for (Thread thread : Thread.getAllStackTraces().keySet()) {
				b.append("name=" + thread.getName() + "\n");
				b.append("status=" + thread.getState().name() + "\n");
				b.append(getStackTraceAsString(thread.getStackTrace()) + "\n\n");
			}

		}

		b.append("\nlongest runtime: " + longestRunTime + " longest wait: "
				+ longestWaitTime + "\n");

		for (long[] w : waits) {
			b.append("waits > " + w[1] + " " + w[0] + "\n");
		}

		synchronized (recentAcquireTimes) {
			b.append("\n\nwaits (" + recentAcquireTimes.size() + "):");
			for (Long l : recentAcquireTimes) {
				b.append(l + "\n");
			}
		}
		synchronized (recentRuntimes) {
			b.append("\n\nrun:");
			for (Long l : recentRuntimes) {
				b.append(l + "\n");
			}
		}

		return b.toString();
	}

	private String getStackTraceAsString(final StackTraceElement[] trace) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < trace.length; i++) {
			b.append(("\tat " + trace[i] + "\n"));
		}
		return b.toString();
	}
}
