/**
 *
 */
package edu.washington.cs.oneswarm.f2f.network;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.BigFatLock;
import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelReset;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FTextSearch;
import edu.washington.cs.oneswarm.f2f.network.OverlayTransport.WriteQueueWaiter;
import edu.washington.cs.oneswarm.f2f.network.QueueManager.QueueBuckets;

class FriendConnectionQueue implements Comparable<FriendConnectionQueue> {

	private static BigFatLock lock = OverlayManager.lock;
	private final static Logger logger = Logger.getLogger(FriendConnectionQueue.class.getName());

	public final static int MAX_FRIEND_QUEUE_LENGTH = (OSF2FMessage.MAX_MESSAGE_SIZE + 9);
	//
	// private final static int MAX_TRANSPORT_QUEUE_MS = 400;

	private static final int MAX_INTERNAL_TRANSPORT_QUEUE_LENGTH = MAX_FRIEND_QUEUE_LENGTH;

	/*
	 * if a search is more than 6s old it is expired
	 */
	private final static int MAX_SEARCH_AGE = 6 * 1000;
	private final static int MAX_SEARCH_QUEUE = 50;
	public static final boolean QUEUE_LENGTH_DEBUG = false;

	private long dataBytesUploaded = 0;

	private final LinkedList<OSF2FMessage> forwardQueue = new LinkedList<OSF2FMessage>();
	private int forwardQueueBytes = 0;
	private long forwardQueueDelay = 0;

	/**
	 * This is synchronized by locking on the QueueManager
	 */
	// private HashMap<Integer, Long> forwardQueueTimes = new HashMap<Integer,
	// Long>();
	private final Friend friend;

	private double friendScore = 1;

	private long lastMessageSentTime;
	private final NetworkConnection nc;
	private long protocolBytesUploaded = 0;

	private final InternalQueueListener queueListener = new InternalQueueListener();
	private final QueueManager queueManager;
	private volatile boolean registeredForForwardSelects = false;
	private boolean registeredForSearchSelects = false;

	private volatile boolean registeredForTransportSelects = false;

	private final LinkedList<QueuedSearch> searchQueue = new LinkedList<QueuedSearch>();
	// private final SpeedManager speedManager;
	private final GlobalManagerStats stats;

	private final LinkedList<OSF2FMessage> transportQueue = new LinkedList<OSF2FMessage>();

	private volatile int transportQueueBytes = 0;
	private long transportQueueDelay = 0;

	private final ConcurrentLinkedQueue<WriteQueueWaiter> transportWaiters = new ConcurrentLinkedQueue<WriteQueueWaiter>();
	private final Average uploadAverage = Average.getInstance(1000, 10);

	public FriendConnectionQueue(QueueManager queueManager, FriendConnection fc) {
		// this.friendConnection = fc;
		this.nc = fc.getNetworkConnection();
		this.friend = fc.getRemoteFriend();
		this.queueManager = queueManager;
		// this.speedManager = new SpeedManager(queueManager, false);
		this.stats = fc.getStats();
		getFriendScore(true);
		nc.getOutgoingMessageQueue().registerQueueListener(queueListener);
		logger.fine(getDescription() + "connection queue created");
	}

	/**
	 * closes the queue, also returns any messages that are still in the queue
	 * for the queuemanager to free in it's next checking run
	 *
	 * @return
	 */
	List<OSF2FMessage> close() {
		nc.getOutgoingMessageQueue().cancelQueueListener(queueListener);

		lock.lock();
		try {
			List<OSF2FMessage> queuedMessages = new LinkedList<OSF2FMessage>();

			OSF2FMessage m;
			while ((m = transportQueue.poll()) != null) {
				queuedMessages.add(m);
			}

			while ((m = forwardQueue.poll()) != null) {
				queuedMessages.add(m);
			}
			QueuedSearch qs;
			while ((qs = searchQueue.poll()) != null) {
				m = qs.getSearchMessage();
				if (m != null) {
					queuedMessages.add(m);
				}
			}
			return queuedMessages;
		} finally {
			lock.unlock();
		}
	}

	public int compareTo(FriendConnectionQueue o) {
		if (friendScore > o.getFriendScore(false)) {
			return 1;
		} else if (friendScore == o.getFriendScore(false)) {
			return 0;
		} else {
			return -1;
		}
	}

	void doListenerNotifications() {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (lock.isHeldByCurrentThread()) {
				Debug.out("holding queue manager lock!!!");
			}
		}
		nc.getOutgoingMessageQueue().doListenerNotifications();
	}

	public int getCurrentUploadSpeedInBps() {
		return (int) uploadAverage.getAverage();
	}

	// private final FriendConnection friendConnection;

	public long getDataBytesUploaded() {
		return dataBytesUploaded;
	}

	public String getDebug() {
		StringBuilder b = new StringBuilder();
		lock.lock();
		try {
			b.append("   network connection=" + getTotalOutgoingQueueLengthBytes());
			b.append("   forward: reg=" + registeredForForwardSelects + " forwardQueue_num=" + forwardQueue.size() + " forwardQueue_bytes=" + forwardQueueBytes + "\n");
			b.append("   transport: reg=" + registeredForTransportSelects + " can_write=" + isReadyForTransportWrite(null) + " waiters=" + transportWaiters.size() + " num=" + transportQueue.size() + " bytes=" + transportQueueBytes + "\n");
			b.append("   search: reg=" + registeredForSearchSelects + " searchQueueLen=" + searchQueue.size() + "\n");
		} finally {
			lock.unlock();
		}

		return b.toString();
	}

	private String getDescription() {
		return friend.getNick() + ": ";
	}

	public int getForwardQueueBytes() {
		return forwardQueueBytes;
	}

	public long getForwardQueueDelay() {
		return forwardQueueDelay;
	}

	/*
	 * return the friend score of the friend associated with this queue
	 *
	 * @param update set to true if the score should be recomputed, false if the
	 * cached score should be returned. Only use cached values unless the
	 * QueueWeight array has been sorted to reflect the new values
	 */
	public double getFriendScore(boolean update) {
		if (update) {
			friendScore = friend.getFriendScore();
			// the friend score used is capped between 0.2 and 5
			// we might want to use a different function here to
			// better reward contribution
			friendScore = Math.max(friendScore, 0.2);
			friendScore = Math.min(friendScore, 5);
		}
		return friendScore;
	}

	public long getLastMessageSentTime() {
		return System.currentTimeMillis() - lastMessageSentTime;
	}

	public long getProtocolBytesUploaded() {
		return protocolBytesUploaded;
	}

	public int getTotalOutgoingQueueLengthBytes() {
		return nc.getOutgoingMessageQueue().getTotalSize();
	}

	boolean isReadyForTransportWrite(WriteQueueWaiter waiter) {
		// boolean writeable = speedManager.canQueuePacket(transportQueueBytes,
		// MAX_TRANSPORT_QUEUE_MS);
		boolean writeable = transportQueueBytes < MAX_INTERNAL_TRANSPORT_QUEUE_LENGTH;

		if (writeable == false && waiter != null) {
			transportWaiters.add(waiter);
		}
		return writeable;
	}

	public boolean isRegisteredForForwardSelects() {
		return registeredForForwardSelects;
	}

	public boolean isRegisteredForSearchSelects() {
		return registeredForSearchSelects;
	}

	public boolean isRegisteredForTransportSelects() {
		return registeredForTransportSelects;
	}

	/*
	 * this function must be called from a thread that has the lock
	 */
	private int pruneExpiredSearches() {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		int removedNum = 0;
		QueuedSearch s;
		while ((s = searchQueue.peek()) != null && s.isExpired()) {
			removedNum++;
			searchQueue.remove();
		}
		return removedNum;
	}

	private boolean queueFull() {
		return this.getTotalOutgoingQueueLengthBytes() > MAX_FRIEND_QUEUE_LENGTH;
	}

	public void queuePacketForceQueue(QueueBuckets bucket, OSF2FMessage msg) {
		/*
		 * this code needs the queue manager lock
		 */
		boolean triggerPacketSending = false;
		lock.lock();
		try {
			if (bucket == QueueBuckets.TRANSPORT) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest(getDescription() + "queueing transport: " + msg.getDescription());
				}
				transportQueue.add(msg);
				transportQueueBytes += (msg).getMessageSize();
				if (QueueManager.QUEUE_DEBUG_LOGGING) {
					// the size operation is expensive, don't run unless
					// debugging
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest(getDescription() + "transport queue size: " + transportQueue.size() + " bytes=" + transportQueueBytes + " registered=" + registeredForTransportSelects);
					}
				}
				if (!registeredForTransportSelects) {
					triggerPacketSending = true;
					registerForTransportSelects();
				}
			} else if (bucket == QueueBuckets.FORWARD || msg instanceof OSF2FSearchResp) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest(getDescription() + "queueing forward: " + msg.getDescription());
				}
				forwardQueue.add(msg);
				forwardQueueBytes += getMessageLen(msg);
				// forwardQueueTimes.put(msg.hashCode(),
				// System.currentTimeMillis());
				if (!registeredForForwardSelects) {
					triggerPacketSending = true;
					registerForForwardSelects();
				}
			} else if (msg instanceof OSF2FSearch) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest(getDescription() + "queueing search packet: " + msg.getDescription() + ", queue len=" + searchQueue.size());
				}
				pruneExpiredSearches();
				if (searchQueue.size() >= MAX_SEARCH_QUEUE) {
					final QueuedSearch drop = searchQueue.removeFirst();
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest(getDescription() + "dropping search packet from head: " + drop.getSearchMessage().getDescription() + ", queue is too long");
					}
					drop.getSearchMessage().destroy();
				}
				searchQueue.add(new QueuedSearch((OSF2FSearch) msg));

				if (!registeredForSearchSelects) {
					triggerPacketSending = true;
					registerForSearchSelects();
				}
			} else {
				Debug.out(getDescription() + "unhandled message: " + msg.getDescription());
			}
		} finally {
			lock.unlock();
		}

		/*
		 * trigger packet sending outside the lock
		 */
		if (triggerPacketSending) {
			queueManager.triggerPacketSending();
		}
	}

	public void queuePacket(QueueManager.QueueBuckets bucket, OSF2FMessage msg, boolean skipQueue) {

		if ((!requiresQueueHandling(msg)) || skipQueue) {
			// control traffic will just skip the queue, no reason to bother
			// the queue manager
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(getDescription() + "sending packet straight to network: " + msg.getDescription());
			}
			nc.getOutgoingMessageQueue().addMessage(msg, false);
		} else {
			queuePacketForceQueue(bucket, msg);
		}

	}

	private void registerForForwardSelects() {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		if (QueueManager.QUEUE_DEBUG_LOGGING) {
			if (registeredForForwardSelects) {
				Debug.out("tried to register for forward selects, but we are already registered!!!");
			}
		}
		registeredForForwardSelects = true;
		queueManager.registerForForwardSelects(FriendConnectionQueue.this);
	}

	private void registerForSearchSelects() {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		if (QueueManager.QUEUE_DEBUG_LOGGING) {
			if (registeredForSearchSelects) {
				Debug.out("tried to register for search selects, but we are already registered!!!");
			}
		}
		registeredForSearchSelects = true;
		queueManager.registerForSearchSelects(FriendConnectionQueue.this);
	}

	private void registerForTransportSelects() {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		if (QueueManager.QUEUE_DEBUG_LOGGING) {
			if (registeredForTransportSelects) {
				Debug.out("tried to register for search selects, but we are already registered!!!");
			}
		}
		registeredForTransportSelects = true;
		queueManager.registerForTransportSelects(FriendConnectionQueue.this);
	}

	/**
	 * this function should only be called from the queue manager, it will
	 * return true if more packets can be queued, false otherwise
	 */
	boolean sendQueuedForwardPacket() {
		registeredForForwardSelects = false;
		boolean packetSent = false;

		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		boolean stayRegistered = false;
		/*
		 * first, check if we can queue more
		 */
		if (queueFull()) {
			stayRegistered = false;
		}
		/*
		 * second, check if we have anything
		 *
		 * use peek instead of size, size is a O(n) operation on
		 * concurrentlinkedqueues
		 */
		else if (forwardQueue.peek() == null) {
			Debug.out(getDescription() + "sendQueuedForwardPacket() called, but noting to forward!");
			stayRegistered = false;
		}
		/*
		 * we have packets to send, send it!!!
		 */
		else {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(getDescription() + "sending packet from forward queue, len=" + forwardQueue.size() + "(bytes=" + forwardQueueBytes + ")");
			}
			OSF2FMessage forwardedMessage = forwardQueue.remove();
			nc.getOutgoingMessageQueue().addMessage(forwardedMessage, true);
			packetSent = true;
			int numBytes = getMessageLen(forwardedMessage);
			stats.protocolBytesSent(numBytes, nc.isLANLocal());
			forwardQueueBytes -= numBytes;
			if (forwardQueueBytes < 0) {
				logger.warning(getDescription() + "accounting error: forwardQueueBytes < 0 in friend connection queue: " + friend);
			}
			/*
			 * hash collitions are rare, but they can happen....
			 */
			// Long queuedTime =
			// forwardQueueTimes.remove(forwardedMessage.hashCode());
			// if (queuedTime != null) {
			// forwardQueueMs = System.currentTimeMillis() - queuedTime;
			// } else {
			// forwardQueueMs = 0;
			// }
			if (forwardQueue.peek() != null) {
				stayRegistered = true;
			} else {
				stayRegistered = false;
			}
		}

		/*
		 * update the registered flag
		 */
		if (stayRegistered) {
			/*
			 * register for more selects, but we don't need to trigger packet
			 * sending, the queue manager will just continue in the while loop
			 */
			registerForForwardSelects();
		}

		return packetSent;

	}

	/**
	 * this function should only be called from the queue manager, it will
	 * return true if more packets are available, false otherwise
	 */
	boolean sendQueuedSearchPacket() {
		logger.finest(getDescription() + "sendQueuedSearchPacket()");
		registeredForSearchSelects = false;
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		boolean packetSent = false;

		boolean stayRegistered = false;

		// remove anything that has expired from the queue
		int pruned = pruneExpiredSearches();
		/*
		 * first, check if we can queue more
		 */
		if (queueFull()) {
			stayRegistered = false;
		}
		/*
		 * then, check if we actually have anything to send
		 */
		else if (searchQueue.peek() == null) {
			if (pruned == 0) {
				Debug.out(getDescription() + "sendQueuedSearchPacket() called, but noting to forward!");
			}
			stayRegistered = false;
		} else {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(getDescription() + "sending packet from search queue");
			}
			Message search = searchQueue.remove().getSearchMessage();
			nc.getOutgoingMessageQueue().addMessage(search, true);
			packetSent = true;

			if (searchQueue.peek() != null) {
				stayRegistered = true;
			} else {
				stayRegistered = false;
			}
		}

		if (stayRegistered) {
			/*
			 * register for more selects, but we don't need to trigger packet
			 * sending, the queue manager will just continue in the while loop
			 */
			registerForSearchSelects();
		}
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(getDescription() + "sendQueuedSearchPacket(), sent=" + packetSent + " registered=" + registeredForSearchSelects + "," + stayRegistered);
		}

		return packetSent;
	}

	/**
	 * this function should only be called from the queue manager, it will
	 * return true if more packets are available, false otherwise
	 *
	 * make sure to have the queue manager lock when calling this funtion
	 */
	boolean sendQueuedTransportPacket() {
		boolean packetSent = false;
		registeredForTransportSelects = false;
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (!lock.isHeldByCurrentThread()) {
				Debug.out("not holding queue manager lock!!!");
			}
		}
		boolean stayRegistered = false;
		/*
		 * first, check if we can queue more
		 */
		if (queueFull()) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(getDescription() + "not queueing transport packet, transport queue already full");
			}
			stayRegistered = false;
		}
		/*
		 * second, we might have waiters ready to write. Let them fill up the
		 * queue
		 */
		else {
			int notified = 0;
			while (transportWaiters.peek() != null && isReadyForTransportWrite(null)) {
				transportWaiters.remove().readyForWrite();
				notified++;
			}
			if (QueueManager.QUEUE_DEBUG_LOGGING && logger.isLoggable(Level.FINEST)) {
				logger.finest(getDescription() + "notifying waiters, num=" + notified + " left=" + transportWaiters.size());
			}
			/*
			 * check if we actually have anything to send
			 */
			if (transportQueue.peek() == null) {
				stayRegistered = false;
				Debug.out(getDescription() + "sendQueuedTransportPacket() called, but noting to send!");
			} else {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest(getDescription() + "sending packet from transport queue");
				}
				Message msg = transportQueue.remove();
				nc.getOutgoingMessageQueue().addMessage(msg, true);
				packetSent = true;
				transportQueueBytes -= ((OSF2FMessage) msg).getMessageSize();
			}
			if (QueueManager.QUEUE_DEBUG_LOGGING && logger.isLoggable(Level.FINEST)) {
				logger.finest(getDescription() + "transport queue size: " + transportQueue.size());
			}
			// if the queue size
			if (transportQueue.peek() != null) {
				stayRegistered = true;
			} else {
				stayRegistered = false;
			}
		}

		if (stayRegistered) {
			/*
			 * register for more selects, but we don't need to trigger packet
			 * sending, the queue manager will just continue in the while loop
			 */
			registerForTransportSelects();
		}

		return packetSent;
	}

	@Override
	public String toString() {
		NumberFormat f = new DecimalFormat("#,###,###");
		final String nick = friend.getNick();
		return "QueueManager: " + nick.substring(0, Math.min(8, nick.length())) + "\tnetBytes=" + nc.getOutgoingMessageQueue().getTotalSize() + "\tfwDelay=" + getForwardQueueDelay() + "\tfwBytes=" + f.format(getForwardQueueBytes()) + "\ttrBytes=" + f.format(transportQueueBytes) + "\ttrDelay" + transportQueueDelay + "\tsLen=" + searchQueue.size();
	}

	public static int getMessageLen(Message message) {
		int len = 4 + 1;// OSF2FMessageFatory:166
		// final DirectByteBuffer[] data = message.getData();
		// for (DirectByteBuffer d : data) {
		// len += d.remaining(DirectByteBuffer.SS_MSG);
		// }
		if (message instanceof OSF2FMessage) {
			len += ((OSF2FMessage) message).getMessageSize();
		}
		return len;
	}

	public static boolean requiresQueueHandling(Message msg) {
		if (msg instanceof OSF2FChannelDataMsg) {
			return true;
		}

		if (msg instanceof OSF2FHashSearch) {
			return true;
		}

		/*
		 * Text searches are really two things: 1). a FILE_LIST_TYPE_COMPLETE is
		 * the intial file list request that is sent during handshake and
		 * contains the complete file list. These shouldn't be rate-limited so
		 * that connections / handshakes are fast even when the queue is full
		 *
		 * 2). Real text searches -- of which there may be many
		 */
		if (msg instanceof OSF2FTextSearch) {
			if (((OSF2FTextSearch) msg).getRequestType() == OSF2FTextSearch.FILE_LIST_TYPE_COMPLETE) {
				return false;
			} else {
				return true;
			}
		}

		if (msg instanceof OSF2FChannelReset) {
			return true;
		}

		/*
		 * search responses are queued as well to make the order they appear in
		 * reflect the congestion on the link, and to make sure that search
		 * cancels will cancel searches over slow links
		 */
		if (msg instanceof OSF2FSearchResp) {
			return true;
		}
		return false;
	}

	class InternalQueueListener implements OutgoingMessageQueue.MessageQueueListener {
		private static final long BYTES_MESSAGES_COMPARISON_INTERVAL = 5 * 1000;

		/**
		 * --------------------------------------------------------------------
		 * ----------------------------- This debugging code is only active when
		 * logging at the FINER or finer granularity and tracks the status of
		 * individual Messages
		 */
		private static final long MAX_MESSAGE_LOG_HISTORY = 10 * 1000;
		/**
		 * This should be the queue length, is it drifting? We compare this to
		 * the authoritative Azureus queue.
		 */
		private long accountingQueueLength = 0;

		private long mAddedMessagesBytes = 0;

		private long mLastByteMessageCompare = 0;

		private final ConcurrentLinkedQueue<MessageDigest> mMessages = new ConcurrentLinkedQueue<MessageDigest>();

		/**
		 * We use these to check the drift between dataBytesUploaded and the sum
		 * of the sent message sizes
		 */
		private long mSentMessagesBytes = 0;
		private long packetNum = 0;

		/**
		 * --------------------------------------------------------------------
		 * -----------------------------
		 */

		public void dataBytesSent(int byte_count) {
			dataBytesUploaded += byte_count;
			friend.updateUploaded(byte_count);
			stats.f2fBytesSent(byte_count);
			uploadAverage.addValue(byte_count);
		}

		private void examineMessageHistory() {
			while (mMessages.peek().timestamp + MAX_MESSAGE_LOG_HISTORY < System.currentTimeMillis()) {
				logger.finer(getDescription() + "Expiring: " + mMessages.remove() + " from FriendConnectionQueue of " + friend.getNick());
			}
		}

		public void flush() {
		}

		public boolean messageAdded(Message message) {
			int len = getMessageLen(message);
			queueManager.messageQueued(len);

			mAddedMessagesBytes += len;

			accountingQueueLength += len;

			if (logger.isLoggable(Level.FINER) && QUEUE_LENGTH_DEBUG) {
				mMessages.add(new MessageDigest(message));
				examineMessageHistory();

				if (mLastByteMessageCompare + BYTES_MESSAGES_COMPARISON_INTERVAL < System.currentTimeMillis()) {
					perFriendQueueLog();
					mLastByteMessageCompare = System.currentTimeMillis();
				}
			}

			return (true);
		}

		public void messageQueued(Message message) {
		}

		public void messageRemoved(Message message) {
			/*
			 * check if we need to reregister
			 */
			packetSentCheckRegistrations();

			int len = getMessageLen(message);
			queueManager.messageSent(len);

			if (logger.isLoggable(Level.FINER)) {
				mMessages.remove(new MessageDigest(message));
			}

			accountingQueueLength -= len;
		}

		public void messageSent(Message message) {
			/*
			 * check if we need to reregister
			 */
			packetSentCheckRegistrations();

			int len = getMessageLen(message);
			// speedManager.dataUploaded(len);
			queueManager.messageSent(len);
			packetNum++;
			if (logger.isLoggable(Level.FINEST) && packetNum % 100 == 0) {
				logger.finest(getDescription() + " sent: " + message.getDescription() + " forward queue: " + forwardQueueBytes + " \t::" + friend);
			}

			if (logger.isLoggable(Level.FINER)) {
				mMessages.remove(new MessageDigest(message));
			}

			mSentMessagesBytes += len;
			accountingQueueLength -= len;

			lastMessageSentTime = System.currentTimeMillis();

			if (message instanceof OSF2FChannelDataMsg) {
				OSF2FChannelDataMsg msg = (OSF2FChannelDataMsg) message;
				long delay = System.currentTimeMillis() - ((OSF2FChannelDataMsg) message).getCreatedTime();
				if (msg.isForward()) {
					forwardQueueDelay = delay;
				} else {
					transportQueueDelay = delay;
				}
			}
		}

		/*
		 * When packets are removed from the queue, check if we need to
		 * reregister with the queue manager, we might have become deregistered
		 * if we had a long queue and therefore were unable to send a packet
		 * when asked
		 */
		private void packetSentCheckRegistrations() {
			lock.lock();
			try {
				/*
				 * if the transport queue contains elements, and we are not
				 * registered, register
				 */
				if (transportQueue.peek() != null && !registeredForTransportSelects) {
					registerForTransportSelects();
				}
				/*
				 * same for forwards
				 */
				if (forwardQueue.peek() != null && !registeredForForwardSelects) {
					registerForForwardSelects();
				}
				/*
				 * and for searches
				 */
				if (searchQueue.peek() != null && !registeredForSearchSelects) {
					registerForSearchSelects();
				}
			} finally {
				lock.unlock();
			}
		}

		private void perFriendQueueLog() {
			logger.finer(getDescription() + "sent_message_bytes=" + mSentMessagesBytes + " added message bytes=" + mAddedMessagesBytes + " protocol_bytes=" + protocolBytesUploaded + " data_bytes=" + dataBytesUploaded + " accouting_queue_len=" + accountingQueueLength + " az_queue_len=" + nc.getOutgoingMessageQueue().getTotalSize() + " forwardQueueBytes=" + forwardQueueBytes + " transportQueueBytes=" + transportQueueBytes + " searchQueueLen=" + searchQueue.size());
		}

		public void protocolBytesSent(int byte_count) {
			protocolBytesUploaded += byte_count;
			friend.updateUploaded(byte_count);
			stats.f2fBytesSent(byte_count);
			uploadAverage.addValue(byte_count);
		}

		class MessageDigest {
			Message message = null;

			long timestamp = 0;

			public MessageDigest(Message m) {
				message = m;
				timestamp = System.currentTimeMillis();
			}

			@Override
			public boolean equals(Object rhs) {
				if (rhs instanceof MessageDigest) {
					return ((MessageDigest) rhs).message == message;
				}
				return false;
			}

			@Override
			public String toString() {
				return message.toString();
			}
		}

	}

	private static class QueuedSearch {
		final OSF2FSearch search;
		final long timeStamp;

		public QueuedSearch(OSF2FSearch search) {
			super();
			this.search = search;
			this.timeStamp = System.currentTimeMillis();
		}

		public OSF2FSearch getSearchMessage() {
			return search;
		}

		public boolean isExpired() {
			long age = System.currentTimeMillis() - timeStamp;
			return age > MAX_SEARCH_AGE;
		}

	}

	public void clearForwardChannel(int channelId) {
		if (QueueManager.QUEUE_LOCK_DEBUG) {
			if (lock.isHeldByCurrentThread()) {
				Debug.out("holding queue manager lock!!!");
			}
		}
		lock.lock();
		try {
			int count = 0;
			for (Iterator<OSF2FMessage> iterator = forwardQueue.iterator(); iterator.hasNext();) {
				OSF2FMessage osf2fMessage = iterator.next();
				if (osf2fMessage instanceof OSF2FChannelMsg) {
					OSF2FChannelMsg msg = (OSF2FChannelMsg) osf2fMessage;
					if (msg.getChannelId() == channelId) {
						iterator.remove();
						count++;
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest("removing channel message from friend queue: " + msg.getDescription() + ": (channel closed)");
						}
						forwardQueueBytes -= getMessageLen(msg);
						msg.destroy();
					}
				}
			}
			logger.finer("channel closed, removed " + count + " messages");
		} finally {
			lock.unlock();
		}
		doListenerNotifications();
	}

	/**
	 * Returns the contribution of this individual {@code FriendConnectionQueue}
	 * to the overall global queue (maintained by {@code QueueManager}.
	 *
	 * This is used to enforce per-friend limits on the share of the global queue.
	 */
	public long getTotalOutgoingBytesContributionToGlobalQueue() {
		return queueListener.accountingQueueLength;
	}
}