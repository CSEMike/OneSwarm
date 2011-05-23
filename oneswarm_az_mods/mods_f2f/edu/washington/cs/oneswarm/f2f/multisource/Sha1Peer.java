package edu.washington.cs.oneswarm.f2f.multisource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPieceListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.network.Connection;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;
import com.aelitis.azureus.core.peermanager.piecepicker.impl.PiecePickerImpl;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

import edu.washington.cs.oneswarm.f2f.multisource.Sha1PieceRequestTranslator.PieceTranslationExcetion;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter.DownloadManagerStartListener;

public class Sha1Peer
	implements PEPeerTransport
{
	private final static Logger										 logger													= Logger.getLogger(Sha1Peer.class.getName());

	private volatile int														_lastPiece											= -1;																				// last piece that was requested from

	private boolean																 availabilityAdded							 = false;

	private int																		 consecutive_no_request_count;

	// this peer (mostly to try to
	// request from same one)
	private long																		create_time										 = SystemTime.getCurrentTime();

	// private Map data;
	private int																		 current_peer_state							= PEPeer.CONNECTING;

	private int																		 current_connection_state				= PEPeerTransport.CONNECTION_CONNECTING;

	private Map<String, Object>										 data;

	private final DownloadManager									 destinationDownloadManager;

	private final AEMonitor												 general_mon										 = new AEMonitor(
																																											"SHA1peer:data");

	private final PEPeerControl										 manager;

	private HashSet<PEPeerListener>								 peer_listeners									= new HashSet<PEPeerListener>();

	private List<PEPeerListener>										peer_listeners_cow;

	private final AEMonitor												 peer_listeners_mon							= new AEMonitor(
																																											"SHA1peer:PL");

	private PEPeerStats														 peer_stats;

	private int[]																	 priorityOffsets								 = null;

	private final ArrayList<DiskManagerReadRequest> requested											 = new ArrayList<DiskManagerReadRequest>();

	private final AEMonitor												 requested_mon									 = new AEMonitor(
																																											"SHA1peer:Req");

	private Sha1PieceRequestTranslator							requestTranslator;

	private int																		 reservedPiece									 = -1;

	private final Sha1DownloadManager							 sha1DownloadManager;

	private final DownloadManager									 sourceDownloadManager;

	private boolean																 started												 = false;

	private SourcePieceListener										 sourcePieceListener						 = new SourcePieceListener();

	private boolean																 closed													= false;

	private BitFlags																available											 = null;

	private boolean																 interesting										 = false;

	private long																		lastAvailableUpdate						 = 0;

	private long																		MAX_MS_BETWEEN_AVAILABLE_UPDATE = 5 * 1000;

	private Average																 sha1DownloadSpeedAverage;

	private void updateAvailable() throws PieceTranslationExcetion {
		lastAvailableUpdate = System.currentTimeMillis();
		if (requestTranslator == null || current_peer_state != TRANSFERING) {
			available = new BitFlags(
					new boolean[destinationDownloadManager.getTorrent().getNumberOfPieces()]);
			interesting = false;
			return;
		}
		available = requestTranslator.getAvailable();
		DiskManagerPiece[] pieces = destinationDownloadManager.getDiskManager().getPieces();
		boolean intr = false;
		for (int i = 0; i < pieces.length; i++) {
			if (available.flags[i]
					&& destinationDownloadManager.getDiskManager().isInteresting(i)) {
				intr = true;
				break;
			}
		}
		interesting = intr;
	}

	public Sha1Peer(Sha1DownloadManager sha1DownloadManager,
			DownloadManager sourceDownloadManager,
			DownloadManager destinationDownloadManager)
			throws PieceTranslationExcetion {
		this.sha1DownloadManager = sha1DownloadManager;
		this.sourceDownloadManager = sourceDownloadManager;
		this.destinationDownloadManager = destinationDownloadManager;
		this.manager = (PEPeerControl) destinationDownloadManager.getPeerManager();
		this.updateAvailable();

		sourceDownloadManager.addListener(new DownloadManagerListener() {

			public void stateChanged(DownloadManager manager, int state) {
				if (!closed) {
					if (manager.getState() != DownloadManager.STATE_DOWNLOADING
							&& manager.getState() != DownloadManager.STATE_SEEDING) {
						closeConnection("source download manager not in downloading or seeding state");
						Sha1Peer.this.sourceDownloadManager.removeListener(this);
					}
				}
			}

			public void positionChanged(DownloadManager download, int oldPosition,
					int newPosition) {
			}

			public void filePriorityChanged(DownloadManager download,
					DiskManagerFileInfo file) {
				// TODO Auto-generated method stub

			}

			public void downloadComplete(DownloadManager manager) {
			}

			public void completionChanged(DownloadManager manager, boolean bCompleted) {
			}
		});

		if (this.destinationDownloadManager.getData("sha1_rate") == null) {
			this.sha1DownloadSpeedAverage = Average.getInstance(1000, 60);
			this.destinationDownloadManager.setData("sha1_rate",
					sha1DownloadSpeedAverage);
		} else {
			this.sha1DownloadSpeedAverage = (Average) this.destinationDownloadManager.getData("sha1_rate");
		}
	}

	public DownloadManager getSourceDownloadManager() {
		return sourceDownloadManager;
	}

	public DownloadManager getDestinationDownloadManager() {
		return destinationDownloadManager;
	}

	private void addAvailability() {
		if (!availabilityAdded && current_peer_state == PEPeerTransport.TRANSFERING) {
			final List<PEPeerListener> peer_listeners_ref = peer_listeners_cow;
			if (peer_listeners_ref != null) {
				for (int i = 0; i < peer_listeners_ref.size(); i++) {
					final PEPeerListener peerListener = peer_listeners_ref.get(i);
					peerListener.addAvailability(this, getAvailable());
				}
				availabilityAdded = true;
			}
		}
	}

	public void addListener(final PEPeerListener listener) {
		try {
			peer_listeners_mon.enter();

			if (peer_listeners_cow == null) {

				peer_listeners_cow = new ArrayList<PEPeerListener>();
			}

			final List<PEPeerListener> new_listeners = new ArrayList<PEPeerListener>(
					peer_listeners_cow);

			new_listeners.add(listener);

			peer_listeners_cow = new_listeners;

		} finally {

			peer_listeners_mon.exit();
		}
	}

	public void addRateLimiter(LimitedRateGroup limiter, boolean upload) {
	}

	/**
	 * Nothing to do if called
	 */
	public void checkInterested() {
	}

	public void clearRequestHint() {
	}

	public void closeConnection(String reason) {
		logger.fine("peer closed: " + reason);
		if (!closed) {
			closed = true;
			removeAvailability();
			stateChanged(DISCONNECTED, PEPeerTransport.DISCONNECTED);
			sha1DownloadManager.peerClosed(this);
			manager.peerConnectionClosed(this, false, false);
			sourceDownloadManager.removePieceListener(sourcePieceListener);
		}
	}

	public void doKeepAliveCheck() {
	}

	public void doPerformanceTuningCheck() {
	}

	// take this opportunity to refresh the disk pieces
	long lastPieceRefresh = System.currentTimeMillis();

	public boolean doTimeoutChecks() {
		return false;
	}

	private void downloadManagerStarted() {
		stateChanged(PEPeer.TRANSFERING,
				PEPeerTransport.CONNECTION_FULLY_ESTABLISHED);
		try {
			peer_stats = manager.createPeerStats(this);
			requestTranslator = new Sha1PieceRequestTranslator(this,
					sourceDownloadManager, destinationDownloadManager);
			updateAvailable();
			sourceDownloadManager.addPieceListener(sourcePieceListener);
			addAvailability();
		} catch (Exception e) {
			e.printStackTrace();
			closeConnection("got exception when creating piece translator");
		}
	}

	public boolean equals(Object o) {
		if (!(o instanceof Sha1Peer)) {
			return false;
		}
		Sha1Peer s = (Sha1Peer) o;
		return s.sourceDownloadManager.equals(sourceDownloadManager)
				&& s.destinationDownloadManager.equals(destinationDownloadManager);
	}

	public void generateEvidence(IndentWriter writer) {
		writer.println("sha1peer: state=" + current_peer_state);
	}

	public BitFlags getAvailable() {
		if (!closed
				&& System.currentTimeMillis() - lastAvailableUpdate > MAX_MS_BETWEEN_AVAILABLE_UPDATE) {
			try {
				updateAvailable();
			} catch (PieceTranslationExcetion e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return available;
	}

	public long getBytesRemaining() {
		long torrentSize = destinationDownloadManager.getSize();
		if (current_peer_state != TRANSFERING || closed) {
			return torrentSize;
		}

		long downloaded = 0;
		DiskManagerPiece[] pieces = destinationDownloadManager.getDiskManager().getPieces();
		for (int i = 0; i < available.flags.length; i++) {
			if (available.flags[i]) {
				downloaded += pieces[i].getLength();
			}
		}
		return torrentSize - downloaded;
	}

	public String getClient() {
		return ("OneSwarm SHA1 virtual peer");
	}

	public String getClientNameFromExtensionHandshake() {
		return null;
	}

	public String getClientNameFromPeerID() {
		return null;
	}

	public int getConnectionState() {
		return current_connection_state;
	}

	public int getConsecutiveNoRequestCount() {
		return (consecutive_no_request_count);
	}

	public PEPeerControl getControl() {
		return manager;
	}

	/** To retreive arbitrary objects against a peer. */
	public Object getData(String key) {
		if (data == null)
			return null;
		return data.get(key);
	}

	public int getDownloadRateLimitBytesPerSecond() {
		// 1GB/s should be enough...
		return 1024 * 1024 * 1024;
	}

	public String getEncryption() {
		return ("Machine-local-only");
	}

	public List<DiskManagerReadRequest> getExpiredRequests() {
		List<DiskManagerReadRequest> result = null;

		// this is frequently called, hence we operate without a monitor and
		// take the hit of possible exceptions due to concurrent list
		// modification (only out-of-bounds can occur)

		try {
			for (int i = requested.size() - 1; i >= 0; i--) {
				final DiskManagerReadRequest request = requested.get(i);

				if (request.isExpired()) {

					if (result == null) {

						result = new ArrayList<DiskManagerReadRequest>();
					}

					result.add(request);
				}
			}

			return (result);

		} catch (Throwable e) {

			return result;
		}
	}

	public byte[] getHandshakeReservedBytes() {
		return new byte[8];
	}

	public byte[] getId() {
		byte[] id = new byte[20];
		id[0] = (byte) 83;
		id[1] = (byte) 72;
		id[2] = (byte) 65;
		id[3] = (byte) 49;
		return id;
	}

	public int getIncomingRequestCount() {
		return (0);
	}

	public int[] getIncomingRequestedPieceNumbers() {
		return (new int[0]);
	}

	// PEPeer stuff

	public String getIp() {
		return "local: " + sourceDownloadManager.getDisplayName() + "->"
				+ destinationDownloadManager.getDisplayName();
	}

	public String getIPHostName() {
		return "local: " + sourceDownloadManager.getDisplayName() + "->"
				+ destinationDownloadManager.getDisplayName();
	}

	public int getLastPiece() {
		return _lastPiece;
	}

	public PEPeerManager getManager() {
		return manager;
	}

	public int getMaxNbRequests() {
		// each request is 16K
		// requests are allocated 10x/sec
		// allow up to 160MB of outstanding requests
		// that is 10,000 block
		return 10 * 1000;
	}

	public int getMessagingMode() {
		return PEPeer.MESSAGING_BT_ONLY;
	}

	public int getNbRequests() {
		return requested.size();
	}

	public int getOutboundDataQueueSize() {
		return 0;
	}

	public int getOutgoingRequestCount() {
		return getNbRequests();
	}

	public int[] getOutgoingRequestedPieceNumbers() {
		try {
			requested_mon.enter();

			/**
			 * Cheap hack to reduce (but not remove all) the # of duplicate
			 * entries
			 */
			int iLastNumber = -1;

			// allocate max size needed (we'll shrink it later)
			final int[] pieceNumbers = new int[requested.size()];
			int pos = 0;

			for (int i = 0; i < requested.size(); i++) {
				DiskManagerReadRequest request = null;
				try {
					request = requested.get(i);
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}

				if (request != null && iLastNumber != request.getPieceNumber()) {
					iLastNumber = request.getPieceNumber();
					pieceNumbers[pos++] = iLastNumber;
				}
			}

			final int[] trimmed = new int[pos];
			System.arraycopy(pieceNumbers, 0, trimmed, 0, pos);

			return trimmed;

		} finally {
			requested_mon.exit();
		}
	}

	public PeerItem getPeerItemIdentity() {
		return PeerItemFactory.createPeerItem(getIp(), getPort(),
				PeerItemFactory.PEER_SOURCE_INCOMING,
				PeerItemFactory.HANDSHAKE_TYPE_PLAIN, getUDPListenPort(),
				PeerItemFactory.CRYPTO_LEVEL_1, 0);
	}

	public String getPeerSource() {
		return "SHA1: " + sourceDownloadManager.getDisplayName();
	}

	public int getPeerState() {
		return current_peer_state;
	}

	public int getPercentDoneInThousandNotation() {
		long total = destinationDownloadManager.getSize();
		return (int) ((1000 * (total - getBytesRemaining())) / total);
	}

	public int getPercentDoneOfCurrentIncomingRequest() {
		return 0;
	}

	public int getPercentDoneOfCurrentOutgoingRequest() {
		return 0;
	}

	public Connection getPluginConnection() {
		return null;
	}

	public int getPort() {
		return 0;
	}

	public int[] getPriorityOffsets() {
		if (priorityOffsets == null) {
			priorityOffsets = getPriorityOffsetsArray();
		}
		return priorityOffsets;
	}

	private int[] getPriorityOffsetsArray() {
		// we want it to read in order to make the disk seeks less rough
		int totalPieceNum = destinationDownloadManager.getNbPieces();
		int[] piecePrio = new int[totalPieceNum];
		for (int i = 0; i < piecePrio.length; i++) {
			piecePrio[i] = (int) Math.round(PiecePickerImpl.PRIORITY_IN_ORDER_FILES
					- (i * (PiecePickerImpl.PRIORITY_IN_ORDER_FILES / totalPieceNum)));
		}
		return piecePrio;
	}

	public int[] getRequestHint() {
		return null;
	}

	public int getRequestIndex(DiskManagerReadRequest request) {
		return (requested.indexOf(request));
	}

	public int getReservedPieceNumber() {
		return (reservedPiece);
	}

	public long getSnubbedTime() {
		return 0;
	}

	public PEPeerStats getStats() {
		return peer_stats;
	}

	public Message[] getSupportedMessages() {
		return null;
	}

	public int getTCPListenPort() {
		return 0;
	}

	public long getTimeSinceConnectionEstablished() {
		long now = SystemTime.getCurrentTime();

		if (now > create_time) {

			return (now - create_time);
		}

		return (0);
	}

	public long getTimeSinceGoodDataReceived() {
		return (0);
	}

	public long getTimeSinceLastDataMessageReceived() {
		return 0;
	}

	public long getTimeSinceLastDataMessageSent() {
		return 0;
	}

	public int getUDPListenPort() {
		return 0;
	}

	public int getUDPNonDataListenPort() {
		return 0;
	}

	public int getUniqueAnnounce() {
		return -1;
	}

	public int getUploadHint() {
		return 0;
	}

	public int getUploadRateLimitBytesPerSecond() {
		return 0;
	}

	public int hashCode() {
		return (2 * sourceDownloadManager.hashCode())
				^ destinationDownloadManager.hashCode();
	}

	public boolean hasReceivedBitField() {
		return requestTranslator != null;
	}

	/**
	 * Apaprently nothing significant to do if called
	 */
	public boolean isAvailabilityAdded() {
		return availabilityAdded;
	}

	public boolean isChokedByMe() {
		return true;
	}

	public boolean isChokingMe() {
		return false;
	}

	public boolean isDownloadPossible() {
		return this.isInteresting();
	}

	public boolean isIncoming() {
		return true;
	}

	public boolean isInterested() {
		return false;
	}

	public boolean isInteresting() {
		if (closed) {
			return false;
		}
		return interesting;
	}

	public boolean isLANLocal() {
		return true;
	}

	public boolean isOptimisticUnchoke() {
		return false;
	}

	public boolean isPieceAvailable(int pieceNumber) {
		if (closed) {
			return false;
		}

		return available.flags[pieceNumber];
	}

	public boolean isSafeForReconnect() {
		return false;
	}

	public boolean isSeed() {
		if (closed) {
			return false;
		}
		return available.nbSet == destinationDownloadManager.getNbPieces();
	}

	public boolean isSnubbed() {
		return false;
	}

	public boolean isStalledPendingLoad() {
		return (false);
	}

	public boolean isTCP() {
		return (true);
	}

	public PEPeerTransport reconnect(boolean tryUDP) {
		return null;
	}

	private void removeAvailability() {
		if (availabilityAdded && requestTranslator != null) {
			final List<PEPeerListener> peer_listeners_ref = peer_listeners_cow;
			if (peer_listeners_ref != null) {
				for (int i = 0; i < peer_listeners_ref.size(); i++) {
					final PEPeerListener peerListener = peer_listeners_ref.get(i);
					peerListener.removeAvailability(this, getAvailable());
				}
			}
			availabilityAdded = false;
		}
		requestTranslator = null;
	}

	public void removeListener(PEPeerListener listener) {
		try {
			peer_listeners_mon.enter();

			if (peer_listeners_cow != null) {

				List<PEPeerListener> new_listeners = new ArrayList<PEPeerListener>(
						peer_listeners_cow);

				new_listeners.remove(listener);

				if (new_listeners.isEmpty()) {

					new_listeners = null;
				}

				peer_listeners_cow = new_listeners;
			}
		} finally {

			peer_listeners_mon.exit();
		}
	}

	public void removeRateLimiter(LimitedRateGroup limiter, boolean upload) {
	}

	private void removeRequest(DiskManagerReadRequest request) {
		try {
			requested_mon.enter();

			requested.remove(request);
		} finally {

			requested_mon.exit();
		}
	}

	/**
	 * 
	 * @param pieceNumber
	 * @param pieceOffset
	 * @param pieceLength
	 * @return true is the piece is really requested
	 */
	long lastRequest = System.currentTimeMillis();

	public DiskManagerReadRequest request(final int pieceNumber,
			final int pieceOffset, final int pieceLength) {
		DiskManagerReadRequest request = manager.createDiskManagerRequest(
				pieceNumber, pieceOffset, pieceLength);
		if (current_peer_state != TRANSFERING || requestTranslator == null) {
			manager.requestCanceled(request);
			return null;
		}
		boolean added = false;
		try {
			requested_mon.enter();

			if (!requested.contains(request)) {
				requested.add(request);
				added = true;
			}
		} finally {
			requested_mon.exit();
		}
		if (added) {
			_lastPiece = pieceNumber;
			final long requestStartTime = System.currentTimeMillis();
			requestTranslator.request(request, new DiskManagerReadRequestListener() {

				public int getPriority() {
					return 0;
				}

				public void readCompleted(DiskManagerReadRequest _request,
						DirectByteBuffer data) {
					removeRequest(_request);
					long time = System.currentTimeMillis() - requestStartTime;
					logger.finest("completed request: piece=" + pieceNumber + " time="
							+ time + " offset=" + pieceOffset + " len=" + pieceLength);
					manager.writeBlock(pieceNumber, pieceOffset, data, Sha1Peer.this,
							false);
					int len = _request.getLength();
					peer_stats.dataBytesReceived(len);
					sha1DownloadSpeedAverage.addValue(len);
					// manager.dataBytesReceived(Sha1Peer.this, len);
				}

				public void readFailed(DiskManagerReadRequest request, Throwable cause) {
					closeConnection("read failed: " + cause.getMessage());
				}

				public void requestExecuted(long bytes) {
				}
			});
			return request;
		} else {
			return null;
		}
	}

	public void requestAllocationComplete() {
	}

	public boolean requestAllocationStarts(int[] base_priorities) {
		return false;
	}

	public void sendBadPiece(int piece_number) {
	}

	public void sendCancel(DiskManagerReadRequest request) {
	}

	/**
	 * Should never be called
	 */
	public void sendChoke() {
	}

	/**
	 * Nothing to do if called
	 */
	public void sendHave(int piece) {
	}

	public boolean sendRequestHint(int piece_number, int offset, int length,
			int life) {
		return (false);
	}

	/**
	 * Should never be called
	 */
	public void sendUnChoke() {
	}

	public void setConsecutiveNoRequestCount(int num) {
		consecutive_no_request_count = num;
	}

	/** To store arbitrary objects against a peer. */
	public void setData(String key, Object value) {
		try {
			general_mon.enter();

			if (data == null) {
				data = new HashMap<String, Object>();
			}
			if (value == null) {
				if (data.containsKey(key))
					data.remove(key);
			} else {
				data.put(key, value);
			}
		} finally {
			general_mon.exit();
		}
	}

	public void setDownloadRateLimitBytesPerSecond(int bytes) {
	};

	public void setHaveAggregationEnabled(boolean enabled) {
	}

	public void setLastPiece(int pieceNumber) {
		_lastPiece = pieceNumber;
	}

	public void setOptimisticUnchoke(boolean is_optimistic) {
	}

	public void setReservedPieceNumber(int pieceNumber) {
		reservedPiece = pieceNumber;
	}

	public void setSnubbed(boolean b) {
	}

	public void setUniqueAnnounce(int uniquePieceNumber) {
	}

	public void setUploadHint(int timeToSpread) {
	}

	public void setUploadRateLimitBytesPerSecond(int bytes) {
	}

	public void start() {
		logger.fine("peer started");
		if (!started) {
			started = true;
			DownloadManagerStarter.startDownload(destinationDownloadManager,
					new DownloadManagerStartListener() {
						public void downloadStarted() {
							DownloadManagerStarter.startDownload(sourceDownloadManager,
									new DownloadManagerStartListener() {
										public void downloadStarted() {
											downloadManagerStarted();
										}
									});
						}
					});
		}
	}

	private void stateChanged(int peer_state, int connection_state) {
		this.current_peer_state = peer_state;
		this.current_connection_state = connection_state;
		for (PEPeerListener l : peer_listeners) {
			l.stateChanged(this, current_peer_state);
		}
	}

	protected void stop() {
		// do nothing
	}

	public boolean supportsMessaging() {
		return false;
	}

	public boolean transferAvailable() {
		return (this.current_peer_state == TRANSFERING);
	}

	public void updatePeerExchange() {
	}

	private class SourcePieceListener
		implements DownloadManagerPieceListener
	{

		public void pieceAdded(PEPiece piece) {
			try {
				if (getPeerState() == TRANSFERING) {
					logger.finest("source piece added: " + piece.getPieceNumber());
					BitFlags newAvailable = requestTranslator.getAvailable();
					for (int i = 0; i < newAvailable.flags.length; i++) {
						if (newAvailable.flags[i] == true && available.flags[i] == false) {
							updateAvailable();
							logger.finest("notifying manager of new piece: " + i);
							int pieceSize = destinationDownloadManager.getDiskManager().getPiece(
									i).getLength();
							manager.havePiece(i, pieceSize, Sha1Peer.this);
							peer_stats.hasNewPiece(pieceSize);
						}
					}
				}
			} catch (PieceTranslationExcetion e) {
				closeConnection("problem when updating have fields: " + e.getMessage());
			}

		}

		public void pieceRemoved(PEPiece piece) {
		}

	}
}
