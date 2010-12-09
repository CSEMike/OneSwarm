package edu.washington.cs.oneswarm.f2f.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Debug;

import edu.washington.cs.oneswarm.f2f.BigFatLock;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.ui.gwt.BackendErrorLog;

public class QueueManager {

	/*
	 * all calls that are modifying the queue should synchronize on this object
	 */

	private static BigFatLock lock = OverlayManager.lock;
	private static Logger logger = Logger.getLogger(QueueManager.class.getName());

	public static final int MAX_GLOBAL_QUEUE_LEN_MS = 200;

	// no matter how slow the connection is, always allow at least 3.5 friend
	// connection queues
	public static final int MIN_GLOBAL_QUEUE_LEN_BYTES = (int) (3.5 * FriendConnectionQueue.MAX_FRIEND_QUEUE_LENGTH);

	// 40*4K = 160K
	public static final int MAX_GLOBAL_QUEUE_LEN_BYTES = 40 * FriendConnectionQueue.MAX_FRIEND_QUEUE_LENGTH;

	public static final boolean QUEUE_DEBUG_LOGGING = System.getProperty("oneswarm.queue.debug.logging") != null;

	final static boolean QUEUE_LOCK_DEBUG = System.getProperty("oneswarm.queue.lock.debug") != null;

	private final LinkedList<FriendConnectionQueue> forwards = new LinkedList<FriendConnectionQueue>();

	private int globalQueueLengthBytes = 0;
	private final SpeedManager globalSpeedManager;
	private long lastPacketSending = System.currentTimeMillis();

	private long lastPacketSent = System.currentTimeMillis();

	private int lastQueueDiff = 0;

	private int memFreed = 0;

	private final ConcurrentLinkedQueue<OSF2FMessage> messagesToBeFreed = new ConcurrentLinkedQueue<OSF2FMessage>();

	private final Map<FriendConnection, FriendConnectionQueue> queueManagers = Collections.synchronizedMap(new HashMap<FriendConnection, FriendConnectionQueue>());

	private final LinkedList<FriendConnectionQueue> searches = new LinkedList<FriendConnectionQueue>();

	private int totalQueueDiffFixed = 0;

	private final LinkedList<FriendConnectionQueue> transports = new LinkedList<FriendConnectionQueue>();

	private final Random random = new Random();

	private final static double PROB_SEARCH_TRAFFIC = 0.1;
	private final static double PROB_FORWARD_TRAFFIC = 0.2;

	private final static double LIMIT_TRANSPORT_TRAFFIC = 1.0 - PROB_FORWARD_TRAFFIC - PROB_SEARCH_TRAFFIC;
	private final static double LIMIT_FORWARD_TRAFFIC = 1.0 - PROB_SEARCH_TRAFFIC;

	/**
	 * The maximum fraction of the global queue length
	 * (MAX_GLOBAL_QUEUE_LEN_BYTES) allowed for each friend.
	 */
	private static final double MAX_QUEUE_FRACTION_PER_FRIEND = 0.4;

	public QueueManager() {
		this.globalSpeedManager = new SpeedManager(this, true);

		if (QUEUE_DEBUG_LOGGING) {
			logger.info("using queue.debug.logging");
		}
		if (QUEUE_LOCK_DEBUG) {
			logger.info("using queue.lock.debug");
		}

		/*
		 * schedule a QueueLengthChecker to update the approximate queue length
		 * to the exact queue length, just using the listeners is leaking a
		 * couple bytes each time a friend connects/disconnects which causes the
		 * global queue length to drift over time
		 */
		Timer t = new Timer("QueueLengthChecker", true);
		t.schedule(new QueueChecker(), 0, 60 * 1000);
	}

	private boolean canQueuePacket() {
		if (globalQueueLengthBytes > MAX_GLOBAL_QUEUE_LEN_BYTES) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Queue manager: can queue=false: " + globalQueueLengthBytes + ">" + MAX_GLOBAL_QUEUE_LEN_BYTES);
			}
			return false;
		} else if (globalQueueLengthBytes < MIN_GLOBAL_QUEUE_LEN_BYTES) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Queue manager: can queue=true: " + globalQueueLengthBytes + "<" + MIN_GLOBAL_QUEUE_LEN_BYTES);
			}
			return true;
		} else {
			boolean canQueuePacket = globalSpeedManager.canQueuePacket(globalQueueLengthBytes, MAX_GLOBAL_QUEUE_LEN_MS);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Queue manager: can queue=" + canQueuePacket);
			}
			return canQueuePacket;
		}
	}

	void deregisterForQueueHandling(FriendConnection fc) {
		FriendConnectionQueue queue;
		lock.lock();
		try {
			queue = queueManagers.remove(fc);
		} finally {
			lock.unlock();
		}
		if (queue != null) {
			final List<OSF2FMessage> messagesStillInQueue = queue.close();
			for (OSF2FMessage message : messagesStillInQueue) {
				globalQueueLengthBytes -= FriendConnectionQueue.getMessageLen(message);
			}
			messagesToBeFreed.addAll(messagesStillInQueue);
		}
	}

	public String getDebug() {
		StringBuilder sb = new StringBuilder();

		boolean canQueue = canQueuePacket();

		sb.append("can_queue=" + canQueue + " queued_bytes=" + globalQueueLengthBytes + " last_queued=" + (System.currentTimeMillis() - lastPacketSent) + " speed=" + globalSpeedManager.getCurrentUploadSpeed() + " total_drift=" + totalQueueDiffFixed + " last_min_drift=" + lastQueueDiff + " mem_freed=" + memFreed + "\nready to send: forwards=" + forwards.size() + " transports=" + transports.size() + " searches=" + searches.size());

		// If we can't queue, include extra information which may be helpful when debugging.
		if (!canQueue) {
			sb.append("\nglobalQueueLengthBytes > MAX_GLOBAL_QUEUE_LEN_BYTES: " + (globalQueueLengthBytes > MAX_GLOBAL_QUEUE_LEN_BYTES));
			sb.append("\nglobalSpeedManager.canQueuePacket(globalQueueLengthBytes, MAX_GLOBAL_QUEUE_LEN_MS): " + globalSpeedManager.canQueuePacket(globalQueueLengthBytes, MAX_GLOBAL_QUEUE_LEN_MS));
			sb.append("\ncurrentUploadSpeeD: " + globalSpeedManager.getCurrentUploadSpeed());
		}

		return sb.toString();
	}

	public String getForwardQueueLengthDebug() {
		StringBuilder b = new StringBuilder();
		b.append("global queue: " + globalQueueLengthBytes + "\n");
		ArrayList<FriendConnectionQueue> queues = new ArrayList<FriendConnectionQueue>();
		queues.addAll(queueManagers.values());

		Collections.sort(queues, new Comparator<FriendConnectionQueue>() {
			public int compare(FriendConnectionQueue o1, FriendConnectionQueue o2) {
				// if (o1.getForwardQueueBytes() > o2.getForwardQueueBytes()) {
				// return -1;
				// } else if (o1.getForwardQueueBytes() ==
				// o2.getForwardQueueBytes()) {
				// return 0;
				// } else {
				// return 1;
				// }
				if (o1.getForwardQueueDelay() > o2.getForwardQueueDelay()) {
					return -1;
				} else if (o1.getForwardQueueDelay() == o2.getForwardQueueDelay()) {
					return 0;
				} else {
					return 1;
				}
			}
		});

		for (FriendConnectionQueue q : queues) {
			b.append(q.toString() + "\n");
		}

		return b.toString();
	}

	void messageQueued(int bytes) {
		globalQueueLengthBytes += bytes;
	}

	void messageSent(int bytes) {
		globalQueueLengthBytes -= bytes;

		if (globalQueueLengthBytes < 0) {
			logger.fine("Irregular accounting: global message queue length (" + globalQueueLengthBytes + " < 0). Resetting...");
			globalQueueLengthBytes = 0;
		}

		globalSpeedManager.dataUploaded(bytes);
		logger.finest("Message sent: " + bytes + " bytes globalQueue=" + globalQueueLengthBytes + " current speed=" + ((int) globalSpeedManager.getCurrentUploadSpeed()));
		triggerPacketSending();
	}

	FriendConnectionQueue registerConnectionForQueueHandling(FriendConnection fc) {
		FriendConnectionQueue m = new FriendConnectionQueue(this, fc);
		lock.lock();
		try {
			queueManagers.put(fc, m);
		} finally {
			lock.unlock();
		}
		return m;
	}

	void registerForForwardSelects(FriendConnectionQueue friendConnectionManager) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(friendConnectionManager + " registering for forward selects");
		}
		if (QUEUE_DEBUG_LOGGING) {
			if (forwards.contains(friendConnectionManager)) {
				Debug.out("tried to register forward, but forward is already there!!!!");
			}
		}
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		forwards.offer(friendConnectionManager);
	}

	void registerForSearchSelects(FriendConnectionQueue friendConnectionManager) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(friendConnectionManager + " registering for search selects");
		}
		if (QUEUE_DEBUG_LOGGING) {
			if (searches.contains(friendConnectionManager)) {
				Debug.out("tried to register for search selects, but already registered!!!!");
			}
		}
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		searches.offer(friendConnectionManager);
	}

	void registerForTransportSelects(FriendConnectionQueue friendConnectionManager) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(friendConnectionManager + " registering for transport selects");
		}
		if (QUEUE_DEBUG_LOGGING) {
			if (transports.contains(friendConnectionManager)) {
				Debug.out("tried to register transport, but transport is already there!!!!");
			}
		}
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		transports.offer(friendConnectionManager);

	}

	/*
	 * make sure to not hold the lock when calling this method, it must be
	 * "lock free" to be able to do the azurues message notifications
	 */
	int triggerPacketSending() {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (lock.isHeldByCurrentThread()) {
				Debug.out("holding queue manager lock!!!");
			}
		}

		int packetsSent = 0;

		LinkedList<FriendConnectionQueue> toNotify = new LinkedList<FriendConnectionQueue>();
		lock.lock();
		try {
			while (canQueuePacket() && (transports.peek() != null || forwards.peek() != null || searches.peek() != null)) {
				double rand = random.nextDouble();
				lastPacketSending = System.currentTimeMillis();
				logger.finest("packet sending triggered");

				// ok, there is room to queue a packet
				if (isFriendQueueAdmissible(transports.peek()) && rand < LIMIT_TRANSPORT_TRAFFIC) {
					// lets use round robin for now, remove the first and put it
					// last
					FriendConnectionQueue luckyFriendQueue = transports.remove();
					// tell it to send a packet
					boolean packetSent = luckyFriendQueue.sendQueuedTransportPacket();
					// if it has more, it will register itself for additional
					// selects
					if (packetSent) {
						packetsSent++;
						toNotify.add(luckyFriendQueue);
					}
				} else if (isFriendQueueAdmissible(forwards.peek()) && rand < LIMIT_FORWARD_TRAFFIC) {
					FriendConnectionQueue luckyFriendQueue = forwards.remove();
					// tell it to send a packet
					boolean packetSent = luckyFriendQueue.sendQueuedForwardPacket();
					// if it has more, it will register itself for additional
					// selects
					if (packetSent) {
						packetsSent++;
						toNotify.add(luckyFriendQueue);
					}
				} else if (isFriendQueueAdmissible(searches.peek())) {
					FriendConnectionQueue luckyFriendQueue = searches.remove();
					// tell it to send a packet
					boolean packetSent = luckyFriendQueue.sendQueuedSearchPacket();
					// if it has more, it will register itself for additional
					// selects
					if (packetSent) {
						packetsSent++;
						toNotify.add(luckyFriendQueue);
					}
				}
				if (packetsSent > 0) {
					lastPacketSent = System.currentTimeMillis();
				}

			}
		} finally {
			lock.unlock();
		}
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("sent " + packetsSent + " packets");
		}
		/*
		 * then perform the listener notifications on the connection that queued
		 * packets
		 */
		for (FriendConnectionQueue fc : toNotify) {
			fc.doListenerNotifications();
		}

		return packetsSent;

	}

	/**
	 * Returns true iff our queueing policy permits sending from a given queue.
	 *
	 * Overall, our goal is to prevent one friend connection from dominating
	 * 'too much' of our overall queue. If this happens, the global queue can
	 * become temporarily 'stuck' waiting for queued bytes to time out.
	 */
	private boolean isFriendQueueAdmissible(FriendConnectionQueue friendQueue) {
		if (friendQueue == null) {
			return false;
		}

		// If this friendQueue currently has queued more than
		// MAX_QUEUE_FRACTION_PER_FRIEND of the global queue size, refuse.
		long totalQueuedBytes = friendQueue.getTotalOutgoingBytesContributionToGlobalQueue();

		if (totalQueuedBytes > MAX_GLOBAL_QUEUE_LEN_BYTES) {
			logger.warning("*** Total queued bytes for friendQueue exceeds max queue. total: "
					+ totalQueuedBytes + " max: " + MAX_GLOBAL_QUEUE_LEN_BYTES + " friend: "
					+ friendQueue.toString());
		}

		if (totalQueuedBytes > MAX_QUEUE_FRACTION_PER_FRIEND * MAX_GLOBAL_QUEUE_LEN_BYTES) {
			return false;
		}

		return true;
	}

	public static enum QueueBuckets {
		CONTROL, FORWARD, TRANSPORT;
	}

	/**
	 *
	 * The per-friend connection queues are authoritative, and the global queue
	 * length is just a heuristic that's designed to roughly approximate it.
	 * Unexpected events (e.g., client disconnects, etc.) can cause the byte
	 * accounting that we rely on to measure the sum of the individual friend
	 * queues to drift. So, periodically, we synchronize it.
	 *
	 */
	private class QueueChecker extends TimerTask {

		@Override
		public void run() {

			lock.lock();

			try {
				try {
					OSF2FMessage m;
					while ((m = messagesToBeFreed.poll()) != null) {
						memFreed += m.getMessageSize();
						m.destroy();
					}

					int totalQueueLen = 0;
					for (FriendConnectionQueue q : queueManagers.values()) {
						totalQueueLen += q.getTotalOutgoingQueueLengthBytes();
					}
					lastQueueDiff = globalQueueLengthBytes - totalQueueLen;
					totalQueueDiffFixed += Math.abs(lastQueueDiff);
					globalQueueLengthBytes = totalQueueLen;

					logger.finer("pre-queuelengthcorrect ul=" + globalSpeedManager.getCurrentUploadSpeed());
					logger.finer((new Date()) + " queuelengthcorrect=" + lastQueueDiff + " tot=" + totalQueueDiffFixed + " ");
				} finally {
					lock.unlock();
				}

				try {
					Thread.sleep(5 * 1000);
				} catch (Exception e) {}

				lock.lock();
				try {
					logger.finer("post-queuelengthcorrect ul=" + globalSpeedManager.getCurrentUploadSpeed());
				} finally {
					lock.unlock();
				}

				/*
				 * check for registration inconsistency as well (but only if we
				 * havn't sent anything in 10s
				 */
				if (System.currentTimeMillis() - lastPacketSent > 10 * 1000) {
					lock.lock();
					try {
						/*
						 * transports
						 */

						for (FriendConnectionQueue q : queueManagers.values()) {
							boolean reg = q.isRegisteredForForwardSelects();
							boolean contains = transports.contains(q);
							if (reg != contains) {
								Debug.out("transport registration inconsistency (fixed): reg=" + reg + " contains=" + contains);
								if (reg && !contains) {
									transports.add(q);
								}
							}
						}
						/*
						 * forwards
						 */

						for (FriendConnectionQueue q : queueManagers.values()) {
							boolean reg = q.isRegisteredForForwardSelects();
							boolean contains = forwards.contains(q);
							if (reg != contains) {
								Debug.out("forward registration inconsistency (fixed): reg=" + reg + " contains=" + contains);
								if (reg && !contains) {
									forwards.add(q);
								}
							}
						}
						/*
						 * searches
						 */

						for (FriendConnectionQueue q : queueManagers.values()) {
							boolean reg = q.isRegisteredForForwardSelects();
							boolean contains = searches.contains(q);
							if (reg != contains) {
								Debug.out("search registration inconsistency (fixed): reg=" + reg + " contains=" + contains);
								if (reg && !contains) {
									searches.add(q);
								}
							}
						}
					} finally {
						lock.unlock();
					}

					/*
					 * check if it was a long time since we sent a packet if so,
					 * trigger
					 */
					long timeSinceLastPacket = System.currentTimeMillis() - lastPacketSending;
					if (timeSinceLastPacket > 30 * 1000) {
						int packetsSent = triggerPacketSending();
						if (packetsSent > 0) {
							Debug.out("recovered from packet sending error, no packets sent last 30 senconds but after trigger " + packetsSent + " were sent");
						}
					}
				}
			} catch (Throwable t) {
				logger.warning("*** Unhandled error in the QueueChecker timer task: " + t.toString());
				t.printStackTrace();
				BackendErrorLog.get().logException(t);
			}
		} // run()
	}
}
