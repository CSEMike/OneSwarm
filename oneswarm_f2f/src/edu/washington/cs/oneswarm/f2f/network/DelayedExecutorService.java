/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.network;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DelayedExecutorService {
	/*
	 * the time accuracy isn't that great anyway and we use slack that makes it
	 * even worse, the buckets used are delay mod BUCKET_SIZE
	 */
	private static final int BUCKET_SIZE = 10;
	private static final int CHECK_PERIOD = 60 * 1000;
	protected final static Logger logger;
	private final static DelayedExecutorService instance;
	static {
		logger = Logger.getLogger(DelayedExecutorService.class.getName());
		instance = new DelayedExecutorService();
	}
	private final HashMap<Long, DelayedExecutor> fixedDelayExecutors = new HashMap<Long, DelayedExecutor>();

	private final VariableDelayExecutor variableDelayExecutor = new VariableDelayExecutor();

	private DelayedExecutorService() {
		Timer executorStopTimer = new Timer("DelayedExecutorCheckTimer", true);
		executorStopTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (variableDelayExecutor.running && variableDelayExecutor.isIdle()) {
					variableDelayExecutor.stop();
				}

				synchronized (fixedDelayExecutors) {
					int running = 0;
					for (Iterator<DelayedExecutor> iterator = fixedDelayExecutors.values().iterator(); iterator.hasNext();) {
						DelayedExecutor e = iterator.next();
						if (e.running) {
							if (e.isIdle()) {
								e.stop();
								/*
								 * don't remove it, killing the thread is enough
								 * for now, we could potentially remove it from
								 * the list when we are sure that no overlay
								 * transports will use it anymore
								 */
								// iterator.remove();
							} else {
								running++;
							}
						}
					}
					logger.finer("cleaning up threads, running=" + running + " total=" + fixedDelayExecutors.size());
				}
			}
		}, CHECK_PERIOD, CHECK_PERIOD);
	}

	public DelayedExecutor getFixedDelayExecutor(long delay) {
		synchronized (fixedDelayExecutors) {
			long bucket = Math.round(delay / (double) BUCKET_SIZE);
			DelayedExecutor e = fixedDelayExecutors.get(bucket);
			if (e == null) {
				e = new FixedDelayExecutor(bucket * BUCKET_SIZE);
				fixedDelayExecutors.put(bucket, e);
				logger.finer("creating fixed delay executor: asked_delay=" + delay + " bucket=" + bucket + " size=" + fixedDelayExecutors.size());
			}
			return e;
		}
	}

	public DelayedExecutor getVariableDelayExecutor() {
		return variableDelayExecutor;
	}

	public static DelayedExecutorService getInstance() {
		return instance;
	}

	public static class DelayedExecutionEntry {
		final long createdAt;
		final long executeAt;
		final long slack;

		final TimerTask task;

		public DelayedExecutionEntry(long executeAt, long slack, TimerTask task) {
			this.task = task;
			this.executeAt = executeAt;
			this.slack = slack;
			this.createdAt = System.nanoTime();
		}
	}

	public abstract static class DelayedExecutor {
		protected Thread executorThread;

		protected volatile long lastExecutionTime = 0;
		protected final BlockingQueue<DelayedExecutionEntry> queue = createQueue();
		protected volatile boolean running = false;

		protected volatile long sleepingUntil = 0;

		protected abstract BlockingQueue<DelayedExecutionEntry> createQueue();

		public abstract String getDescription();

		public boolean isEmpty() {
			return queue.isEmpty();
		}

		public boolean isIdle() {
			return System.currentTimeMillis() > lastExecutionTime + CHECK_PERIOD && queue.isEmpty();
		}

		public abstract void queue(List<DelayedExecutionEntry> batch);

		/**
		 * Queue a task for later execution, if the delay is 0 it will run it
		 * instantly in the urrent thread
		 * 
		 * @param delay
		 * @param slack
		 *            Allow task to be executed up to slack ms earlier than the
		 *            deadline if that avoids a call to Thread.sleep
		 * 
		 * @param task
		 */
		public void queue(long delay, long slack, TimerTask task) {
			if (delay <= 0) {
				task.run();
				return;
			}
			DelayedExecutionEntry entry = new DelayedExecutionEntry(System.currentTimeMillis() + delay, slack, task);
			List<DelayedExecutionEntry> batch = new LinkedList<DelayedExecutionEntry>();
			batch.add(entry);
			queue(batch);
		}

		public void queue(long delay, TimerTask task) {
			queue(delay, 0, task);
		}

		void start() {
			synchronized (this) {
				if (!running) {
					logger.fine("starting thread: " + getDescription());
					running = true;
					lastExecutionTime = System.currentTimeMillis();
					DelayedExecutionRunner r = new DelayedExecutionRunner();
					executorThread = new Thread(r);
					executorThread.setDaemon(true);
					executorThread.setName(getDescription());
					executorThread.start();
				} else {
					logger.warning("start called but thread already running: " + getDescription());
				}
			}
		}

		void stop() {
			synchronized (this) {
				if (running && queue.peek() == null) {
					logger.fine("stopping delay executor thread: " + getDescription());
					running = false;
					if (executorThread != null) {
						executorThread.interrupt();
					}
				} else {
					logger.warning("stop called but thread is either no running or non-empty: " + getDescription());
				}
			}
		}

		private class DelayedExecutionRunner implements Runnable {

			public void run() {
				logger.fine("executor thread started");
				while (running) {
					DelayedExecutionEntry entry = null;
					try {
						entry = queue.take();
						long currentTime = System.currentTimeMillis();

						long sleepMs = entry.executeAt - currentTime;
						/*
						 * sleep at least "slack" ms, if we are to close to the
						 * time run the task anyway even though it can be a bit
						 * early
						 */
						if (sleepMs > entry.slack) {
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest(getDescription() + ": sleeping " + sleepMs + "ms");
							}
							sleepingUntil = entry.executeAt;
							Thread.sleep(sleepMs);
						} else {
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest(getDescription() + ": skipping sleep this time, sleepMs=" + sleepMs + ", slack=" + entry.slack);
							}
						}
					} catch (InterruptedException e) {
						logger.finest(getDescription() + ": executor thread interupted");
						if (!running) {
							logger.finer(getDescription() + ": stopping executor thread");
							continue;
						}
						// this is expected if we add anything to the head of
						// the
						// queue
						if (entry != null) {
							logger.finer(getDescription() + ": interrupted, returing current entry to queue");
							queue.add(entry);
						}
						continue;
					}
					long startTime = System.currentTimeMillis();
					entry.task.run();
					long elapsed = System.currentTimeMillis() - startTime;
					if (elapsed > 20) {
						logger.warning(getDescription() + ": took " + elapsed + "ms to run task! (parent=" + getDescription() + ")");
					}
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest(getDescription() + ": executed task in: " + elapsed + " ms");
					}
					lastExecutionTime = System.currentTimeMillis();

				}
				logger.fine(getDescription() + ":executor thread stopped");
			}
		}
	}

	private static class FixedDelayExecutor extends DelayedExecutor {
		private final long delay;
		private final String desc;

		private FixedDelayExecutor(long delay) {
			this.delay = delay;
			this.desc = "FixedDelayExecutor:" + delay;
		}

		@Override
		protected BlockingQueue<DelayedExecutionEntry> createQueue() {
			return new LinkedBlockingQueue<DelayedExecutionEntry>();
		}

		@Override
		public String getDescription() {
			return desc;
		}

		@Override
		public void queue(List<DelayedExecutionEntry> batch) {
			throw new RuntimeException("batch adding not supported");
		}

		@Override
		public void queue(long delay, long slack, TimerTask task) {
			if (delay + BUCKET_SIZE < this.delay || delay - BUCKET_SIZE > this.delay) {
				throw new RuntimeException("delay must be " + this.delay + "+/- " + BUCKET_SIZE);
			}
			delay = this.delay;
			if (delay <= 0) {
				task.run();
				return;
			}

			DelayedExecutionEntry entry = new DelayedExecutionEntry(System.currentTimeMillis() + delay, slack, task);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("queuing task: delay=" + delay + " slack=" + slack);
			}
			queue.add(entry);
			synchronized (this) {
				if (!running) {
					start();
				}
			}
		}

	}

	private static class VariableDelayExecutor extends DelayedExecutor {
		private VariableDelayExecutor() {
			logger.fine("DelayedExecutor created");

		}

		@Override
		protected BlockingQueue<DelayedExecutionEntry> createQueue() {
			return new PriorityBlockingQueue<DelayedExecutionEntry>(10000, new Comparator<DelayedExecutionEntry>() {
				public int compare(DelayedExecutionEntry o1, DelayedExecutionEntry o2) {
					if (o1.executeAt < o2.executeAt) {
						return -1;
					} else if (o1.executeAt == o2.executeAt) {
						if (o1.createdAt < o2.createdAt) {
							return -1;
						} else if (o1.createdAt == o2.createdAt) {
							return 0;
						} else {
							return 1;
						}
					} else {
						return 1;
					}
				}
			});
		}

		@Override
		public String getDescription() {
			return "VariableDelayExecutor";
		}

		public void queue(List<DelayedExecutionEntry> batch) {
			List<DelayedExecutionEntry> clone = new LinkedList<DelayedExecutionEntry>(batch);
			long earliestRun = Long.MAX_VALUE;
			long currentTime = System.currentTimeMillis();

			/*
			 * first, check if any of these are expired already, in that case
			 * run then straight up
			 */
			for (Iterator<DelayedExecutionEntry> iterator = clone.iterator(); iterator.hasNext();) {
				DelayedExecutionEntry e = iterator.next();
				if (e.executeAt <= currentTime) {
					e.task.run();
					iterator.remove();
				} else {
					if (e.executeAt < earliestRun) {
						earliestRun = e.executeAt;
					}
				}
			}
			/*
			 * if we have anything left, add to queue
			 */
			if (clone.size() > 0) {
				synchronized (this) {
					if (!running) {
						start();
					}
				}
				queue.addAll(clone);

				if (sleepingUntil > earliestRun) {
					logger.finer("interrupting queue executor thread");
					executorThread.interrupt();
				}
			}
		}

	}
}
