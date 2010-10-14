/*
 * File    : PEPeerTransportProtocol.java
 * Created : 22-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.peer.impl.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransportFactory;
import org.gudy.azureus2.core3.peer.util.PeerIdentityDataID;
import org.gudy.azureus2.core3.peer.util.PeerIdentityManager;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.IPToHostNameResolver;
import org.gudy.azureus2.core3.util.IPToHostNameResolverListener;
import org.gudy.azureus2.core3.util.IPToHostNameResolverRequest;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.StringInterner;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTProvider;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.pluginsimpl.local.network.ConnectionImpl;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.NetworkConnectionImpl;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.udp.ProtocolEndpointUDP;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZBadPiece;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZHandshake;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZHave;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZPeerExchange;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZRequestHint;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZStylePeerExchange;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTBitfield;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTCancel;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTChoke;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTDHTPort;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHave;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTInterested;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTKeepAlive;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRawMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRequest;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTUnchoke;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTUninterested;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.UTPeerExchange;
import com.aelitis.azureus.core.peermanager.peerdb.PeerExchangerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.peermanager.utils.AZPeerIdentityManager;
import com.aelitis.azureus.core.peermanager.utils.ClientIdentifier;
import com.aelitis.azureus.core.peermanager.utils.OutgoingBTHaveMessageAggregator;
import com.aelitis.azureus.core.peermanager.utils.OutgoingBTPieceMessageHandler;
import com.aelitis.azureus.core.peermanager.utils.OutgoingBTPieceMessageHandlerAdapter;
import com.aelitis.azureus.core.peermanager.utils.PeerClassifier;
import com.aelitis.azureus.core.peermanager.utils.PeerMessageLimiter;

import edu.uw.cse.netlab.reputation.Computation;
import edu.uw.cse.netlab.reputation.LocalIdentity;
import edu.uw.cse.netlab.reputation.ReceiptDispatcher;
import edu.uw.cse.netlab.reputation.messages.Attestation;
import edu.uw.cse.netlab.reputation.messages.CertificateExchange;
import edu.uw.cse.netlab.reputation.messages.ReceiptBundle;
import edu.uw.cse.netlab.reputation.messages.ReceiptRequests;
import edu.uw.cse.netlab.reputation.storage.LocalTopK;
import edu.uw.cse.netlab.reputation.storage.Receipt;
import edu.uw.cse.netlab.reputation.storage.ReputationDAO;
import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.KeyManipulation;

public class PEPeerTransportProtocol extends LogRelation implements PEPeerTransport {
	protected final static LogIDs LOGID = LogIDs.PEER;
	
	private volatile int _lastPiece = -1; // last piece that was requested
											// from this peer (mostly to try to
											// request from same one)

	private HashMap<Long, Receipt> requested_receipts = new HashMap<Long, Receipt>();

	public Receipt[] getSharedIntermediaries() {
		return requested_receipts.values().toArray(new Receipt[0]);
	}

	protected final PEPeerControl manager;
	protected final DiskManager diskManager;
	protected final PiecePicker piecePicker;

	protected final int nbPieces;

	private final String peer_source;
	private byte[] peer_id;
	private final String ip;
	protected String ip_resolved;
	private IPToHostNameResolverRequest ip_resolver_request;

	private int port;

	private PeerItem peer_item_identity;
	private int tcp_listen_port = 0;
	private int udp_listen_port = 0;
	private int udp_non_data_port = 0;

	private byte crypto_level;

	protected PEPeerStats peer_stats;

	private final ArrayList requested = new ArrayList();
	private final AEMonitor requested_mon = new AEMonitor("PEPeerTransportProtocol:Req");

	private Map data;

	private long lastNeededUndonePieceChange;

	protected boolean choked_by_other_peer = true;
	/** total time the other peer has unchoked us while not snubbed */
	protected long unchokedTimeTotal;
	/** the time at which the other peer last unchoked us when not snubbed */
	protected long unchokedTime;
	protected boolean choking_other_peer = true;
	private boolean interested_in_other_peer = false;
	private boolean other_peer_interested_in_me = false;
	private long snubbed = 0;

	/** lazy allocation; null until needed */
	private volatile BitFlags peerHavePieces = null;
	private volatile boolean availabilityAdded = false;
	private volatile boolean received_bitfield;

	private boolean handshake_sent;

	private boolean seed_set_by_accessor = false;

	private final boolean incoming;

	protected volatile boolean closing = false;
	private volatile int current_peer_state;

	protected NetworkConnection connection;

	public double getWeight() {
		return ((NetworkConnectionImpl) connection).getWeight();
	}

	public void setWeight(double inWeight) {
		((NetworkConnectionImpl) connection).setWeight(inWeight);
	}
	
	double rep = 0;
	public double getReputation() {
		return rep;
	}
	
	public void setReputation( double rep ) {
		this.rep = rep;
	}

	private OutgoingBTPieceMessageHandler outgoing_piece_message_handler;
	private OutgoingBTHaveMessageAggregator outgoing_have_message_aggregator;
	private Connection plugin_connection;

	private boolean identityAdded = false; // needed so we don't remove id's in
											// closeAll() on duplicate
											// connection attempts

	protected int connection_state = PEPeerTransport.CONNECTION_PENDING;

	private String client = ""; // Client name to show to user.
	private String client_peer_id = ""; // Client name derived from the peer ID.
	private String client_handshake = ""; // Client name derived from the
											// handshake.
	private String client_handshake_version = ""; // Client version derived
													// from the handshake.

	// When superSeeding, number of unique piece announced
	private int uniquePiece = -1;

	// When downloading a piece in exclusivity mode the piece number being
	// downloaded
	private int reservedPiece = -1;

	// Spread time (0 secs , fake default)
	private int spreadTimeHint = 0 * 1000;

	protected long last_message_sent_time = 0;
	protected long last_message_received_time = 0;
	protected long last_data_message_received_time = -1;
	protected long last_good_data_time = -1; // time data written to disk was
												// recieved
	protected long last_data_message_sent_time = -1;

	private long connection_established_time = 0;

	private int consecutive_no_request_count;

	private int messaging_mode = MESSAGING_BT_ONLY;
	private Message[] supported_messages = null;
	private byte other_peer_bitfield_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_cancel_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_choke_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_handshake_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_bt_have_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_az_have_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_interested_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_keep_alive_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_pex_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_piece_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_unchoke_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_uninterested_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_request_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_bt_lt_ext_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_az_request_hint_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte other_peer_az_bad_piece_version = BTMessageFactory.MESSAGE_VERSION_INITIAL;

	// OneSwarm messages
	private byte os_certificate_exchange = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte os_receipt_requests = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte os_receipt_bundle = BTMessageFactory.MESSAGE_VERSION_INITIAL;
	private byte os_attestation = BTMessageFactory.MESSAGE_VERSION_INITIAL;

	private boolean ut_pex_enabled = false;
	private boolean ml_dht_enabled = false;

	private final AEMonitor closing_mon = new AEMonitor("PEPeerTransportProtocol:closing");
	private final AEMonitor general_mon = new AEMonitor("PEPeerTransportProtocol:data");

	private byte[] handshake_reserved_bytes = null;

	private LinkedHashMap recent_outgoing_requests;
	private AEMonitor recent_outgoing_requests_mon;

	private boolean has_received_initial_pex = false;

	private static final boolean SHOW_DISCARD_RATE_STATS;
	static {
		final String prop = System.getProperty("show.discard.rate.stats");
		SHOW_DISCARD_RATE_STATS = prop != null && prop.equals("1");
	}

	private static int requests_discarded = 0;
	private static int requests_discarded_endgame = 0;
	private static int requests_recovered = 0;
	private static int requests_completed = 0;

	private static final int REQUEST_HINT_MAX_LIFE = PiecePicker.REQUEST_HINT_MAX_LIFE + 30 * 1000;

	private int[] request_hint;

	private List peer_listeners_cow;
	private final AEMonitor peer_listeners_mon = new AEMonitor("PEPeerTransportProtocol:PL");

	private ReceiptDispatcher mReceiptDispatcher = null; // this gets created
															// if this is a
															// OneSwarm
															// connection
	private Set<PublicKey> mAttribution = null; // the reported attribution of a
												// remote peer (if one was
												// received)

	public Set<PublicKey> getAttribution() {
		return mAttribution;
	}

	// certain Optimum Online networks block peer seeding via "complete"
	// bitfield message filtering
	// lazy mode makes sure we never send a complete (seed) bitfield
	protected static boolean ENABLE_LAZY_BITFIELD;

	private static final Random rnd = new SecureRandom();

	private static final class DisconnectedTransportQueue extends LinkedHashMap {
		public DisconnectedTransportQueue() {
			super(20, 0.75F);
		}

		private static final long MAX_CACHE_AGE = 2 * 60 * 1000;

		// remove all elements older than 2 minutes until we hit the 20 again

		private void performCleaning() {
			if (size() > 20) {
				Iterator it = values().iterator();

				long now = SystemTime.getCurrentTime();

				while (it.hasNext() && size() > 20) {
					QueueEntry eldest = (QueueEntry) it.next();
					if (now < eldest.addTime || now - eldest.addTime > MAX_CACHE_AGE) {
						it.remove();
					} else {
						break;
					}
				}
			}
		}

		private static final class QueueEntry {
			public QueueEntry(PEPeerTransportProtocol trans) {
				transport = trans;
			}

			final PEPeerTransportProtocol transport;
			final long addTime = SystemTime.getCurrentTime();
		}

		// hardcap at 100
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > 100;
		}

		synchronized public Object put(HashWrapper key, PEPeerTransportProtocol value) {
			performCleaning();
			return super.put(key, new QueueEntry(value));
		}

		synchronized public PEPeerTransportProtocol remove(HashWrapper key) {
			performCleaning();
			QueueEntry entry = (QueueEntry) super.remove(key);
			if (entry != null)
				return entry.transport;
			else
				return null;
		}

	}

	private static final DisconnectedTransportQueue recentlyDisconnected = new DisconnectedTransportQueue();

	private static boolean fast_unchoke_new_peers;

	static {

		rnd.setSeed(SystemTime.getCurrentTime());

		COConfigurationManager.addAndFireParameterListeners(new String[] { "Use Lazy Bitfield", "Peer.Fast.Initial.Unchoke.Enabled" }, new ParameterListener() {
			public final void parameterChanged(String ignore) {
				final String prop = System.getProperty("azureus.lazy.bitfield");

				ENABLE_LAZY_BITFIELD = prop != null && prop.equals("1");

				ENABLE_LAZY_BITFIELD |= COConfigurationManager.getBooleanParameter("Use Lazy Bitfield");

				fast_unchoke_new_peers = COConfigurationManager.getBooleanParameter("Peer.Fast.Initial.Unchoke.Enabled");
			}
		});
	}

	// reconnect stuff
	private HashWrapper peerSessionID;
	private HashWrapper mySessionID;
	{
		byte[] newSession = new byte[20];
		rnd.nextBytes(newSession);
		mySessionID = new HashWrapper(newSession);
	}

	// allow reconnect if we've sent or recieved at least 1 piece over the
	// current connection
	private boolean allowReconnect;

	private boolean is_optimistic_unchoke = false;

	private PeerExchangerItem peer_exchange_item = null;
	private boolean peer_exchange_supported = false;

	protected PeerMessageLimiter message_limiter;

	private boolean request_hint_supported;
	private boolean bad_piece_supported;

	private boolean have_aggregation_disabled;

	private boolean mIsOneSwarm = false;

	private PublicKey[] mDirectAdvertisements = null;

	public PublicKey[] getDirectAdvertisements() {
		return mDirectAdvertisements;
	}

	// Methods call this and then use oneswarm specific information that isn't
	// available unless the handshake is complete
	public boolean isOneSwarm() {
		return mIsOneSwarm && this.getPeerState() == PEPeer.TRANSFERING;
	}

	private X509Certificate mCertificate = null;

	private AZHandshake mHandshake;

	public X509Certificate getCertificate() {
		return mCertificate;
	}

	// INCOMING
	public PEPeerTransportProtocol(PEPeerControl _manager, String _peer_source, NetworkConnection _connection, Map _initial_user_data) {
		manager = _manager;
		peer_source = _peer_source;
		connection = _connection;
		data = _initial_user_data;
		
		incoming = true;

		diskManager = manager.getDiskManager();
		piecePicker = manager.getPiecePicker();
		nbPieces = diskManager.getNbPieces();

		InetSocketAddress notional_address = _connection.getEndpoint().getNotionalAddress();

		ip = notional_address.getAddress().getHostAddress();
		port = notional_address.getPort();

		peer_item_identity = PeerItemFactory.createPeerItem(ip, port, PeerItem.convertSourceID(_peer_source), PeerItemFactory.HANDSHAKE_TYPE_PLAIN, 0, PeerItemFactory.CRYPTO_LEVEL_1, 0); // this
																																															// will
																																															// be
																																															// recreated
																																															// upon
																																															// az
																																															// handshake
																																															// decode

		plugin_connection = new ConnectionImpl(connection);

		peer_stats = manager.createPeerStats(this);

		changePeerState(PEPeer.CONNECTING);
	}

	public void start() {
		// split out connection initiation from constructor so we can get access
		// to the peer transport
		// before message processing starts

		if (incoming) {
			
			// "fake" a connect request to register our listener
			connection.connect(false, new NetworkConnection.ConnectionListener() {
				public final void connectStarted() {
					connection_state = PEPeerTransport.CONNECTION_CONNECTING;
				}

				public final void connectSuccess(ByteBuffer remaining_initial_data) { // will
																						// be
																						// called
																						// immediately
					if (Logger.isEnabled())
						Logger.log(new LogEvent(PEPeerTransportProtocol.this, LOGID, "In: Established incoming connection"));

					initializeConnection();

					/*
					 * Waiting until we've received the initiating-end's full
					 * handshake, before sending back our own, really should be
					 * the "proper" behavior. However, classic BT trackers
					 * running NAT checking will only send the first 48 bytes
					 * (up to infohash) of the peer handshake, skipping peerid,
					 * which means we'll never get their complete handshake, and
					 * thus never reply, which causes the NAT check to fail. So,
					 * we need to send our handshake earlier, after we've
					 * verified the infohash. NOTE: This code makes the
					 * assumption that the inbound infohash has already been
					 * validated, as we don't check their handshake fully before
					 * sending our own.
					 */
					sendBTHandshake();
				}

				public final void connectFailure(Throwable failure_msg) { // should
																			// never
																			// happen
					Debug.out("ERROR: incoming connect failure: ", failure_msg);
					closeConnectionInternally("ERROR: incoming connect failure [" + PEPeerTransportProtocol.this + "] : " + failure_msg.getMessage(), true, true);
				}

				public final void exceptionThrown(Throwable error) {
					if (error.getMessage() == null) {
						Debug.out(error);
					}

					closeConnectionInternally("connection exception: " + error.getMessage(), false, true);
				}

				public String getDescription() {
					return (getString());
				}
			});
		} else {
			// not pulled out startup from outbound connections yet...
		}
	}

	// OUTGOING

	public PEPeerTransportProtocol(PEPeerControl _manager, String _peer_source, String _ip, int _tcp_port, int _udp_port, boolean _use_tcp, boolean _require_crypto_handshake, byte _crypto_level, Map _initial_user_data) {
		manager = _manager;
		diskManager = manager.getDiskManager();
		piecePicker = manager.getPiecePicker();
		nbPieces = diskManager.getNbPieces();
		lastNeededUndonePieceChange = Long.MIN_VALUE;

		peer_source = _peer_source;
		ip = _ip;
		port = _tcp_port;
		tcp_listen_port = _tcp_port;
		udp_listen_port = _udp_port;
		crypto_level = _crypto_level;
		data = _initial_user_data;

		udp_non_data_port = UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber();

		peer_item_identity = PeerItemFactory.createPeerItem(ip, tcp_listen_port, PeerItem.convertSourceID(_peer_source), PeerItemFactory.HANDSHAKE_TYPE_PLAIN, _udp_port, crypto_level, 0); // this
																																															// will
																																															// be
																																															// recreated
																																															// upon
																																															// az
																																															// handshake
																																															// decode

		incoming = false;

		peer_stats = manager.createPeerStats(this);

		if (port < 0 || port > 65535) {
			closeConnectionInternally("given remote port is invalid: " + port);
			return;
		}

		// either peer specific or global pref plus optional per-download level

		boolean use_crypto = _require_crypto_handshake || NetworkManager.getCryptoRequired(manager.getAdapter().getCryptoLevel());

		if (isLANLocal())
			use_crypto = false; // dont bother with PHE for lan peers

		InetSocketAddress endpoint_address;
		ProtocolEndpoint pe;

		if (_use_tcp) {

			endpoint_address = new InetSocketAddress(ip, tcp_listen_port);

			pe = new ProtocolEndpointTCP(endpoint_address);

		} else {

			endpoint_address = new InetSocketAddress(ip, udp_listen_port);

			pe = new ProtocolEndpointUDP(endpoint_address);
		}

		ConnectionEndpoint connection_endpoint = new ConnectionEndpoint(endpoint_address);

		connection_endpoint.addProtocol(pe);

		connection = NetworkManager.getSingleton().createConnection(connection_endpoint, new BTMessageEncoder(), new BTMessageDecoder(), use_crypto, !_require_crypto_handshake, manager.getSecrets(_crypto_level));

		plugin_connection = new ConnectionImpl(connection);

		changePeerState(PEPeer.CONNECTING);

		ByteBuffer initial_outbound_data = null;

		if (use_crypto) {

			DirectByteBuffer[] ddbs = new BTHandshake(manager.getHash(), manager.getPeerId(), manager.isExtendedMessagingEnabled(), other_peer_handshake_version).getRawData();

			int handshake_len = 0;

			for (int i = 0; i < ddbs.length; i++) {

				handshake_len += ddbs[i].remaining(DirectByteBuffer.SS_PEER);
			}

			initial_outbound_data = ByteBuffer.allocate(handshake_len);

			for (int i = 0; i < ddbs.length; i++) {

				DirectByteBuffer ddb = ddbs[i];

				initial_outbound_data.put(ddb.getBuffer(DirectByteBuffer.SS_PEER));

				ddb.returnToPool();
			}

			initial_outbound_data.flip();

			handshake_sent = true;
		}

		connection.connect(initial_outbound_data, !manager.isSeeding(), new NetworkConnection.ConnectionListener() {
			private boolean connect_ok;

			public final void connectStarted() {
				connection_state = PEPeerTransport.CONNECTION_CONNECTING;
			}

			public final void connectSuccess(ByteBuffer remaining_initial_data) {
				connect_ok = true;

				if (closing) {
					// Debug.out( "PEPeerTransportProtocol::connectSuccess()
					// called when closing." );
					return;
				}

				if (Logger.isEnabled())
					Logger.log(new LogEvent(PEPeerTransportProtocol.this, LOGID, "Out: Established outgoing connection"));

				initializeConnection();

				if (remaining_initial_data != null && remaining_initial_data.remaining() > 0) {

					// queue as a *raw* message as already encoded

					connection.getOutgoingMessageQueue().addMessage(new BTRawMessage(new DirectByteBuffer(remaining_initial_data)), false);
				}

				sendBTHandshake();
			}

			public final void connectFailure(Throwable failure_msg) {
				closeConnectionInternally("failed to establish outgoing connection: " + failure_msg.getMessage(), true, true);
			}

			public final void exceptionThrown(Throwable error) {
				if (error.getMessage() == null) {
					Debug.out("error.getMessage() == null", error);
				}

				closeConnectionInternally("connection exception: " + error.getMessage(), !connect_ok, true);
			}

			public String getDescription() {
				return (getString());
			}
		});

		if (Logger.isEnabled())
			Logger.log(new LogEvent(this, LOGID, "Out: Creating outgoing connection"));
	}

	protected void initializeConnection() {
		if (closing)
			return;

		recent_outgoing_requests = new LinkedHashMap(16, .75F, true) {
			public final boolean removeEldestEntry(Map.Entry eldest) {
				return size() > 16;
			}
		};
		recent_outgoing_requests_mon = new AEMonitor("PEPeerTransportProtocol:ROR");

		message_limiter = new PeerMessageLimiter();

		/*
		 * //link in outgoing piece handler outgoing_piece_message_handler = new
		 * OutgoingBTPieceMessageHandler( this,
		 * connection.getOutgoingMessageQueue(), new
		 * OutgoingBTPieceMessageHandlerAdapter() { public void
		 * diskRequestCompleted( long bytes) { peer_stats.diskReadComplete(
		 * bytes ); } }, other_peer_piece_version);
		 */

		// link in outgoing have message aggregator
		outgoing_have_message_aggregator = new OutgoingBTHaveMessageAggregator(connection.getOutgoingMessageQueue(), other_peer_bt_have_version, other_peer_az_have_version);

		connection_established_time = SystemTime.getCurrentTime();

		connection_state = PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE;
		changePeerState(PEPeer.HANDSHAKING);

		registerForMessageHandling();
	}

	public String getPeerSource() {
		return (peer_source);
	}

	/**
	 * Close the peer connection from within the PEPeerTransport object.
	 * 
	 * @param reason
	 */
	protected void closeConnectionInternally(String reason, boolean connect_failed, boolean network_failure) {
		performClose(reason, connect_failed, false, network_failure);
	}

	protected void closeConnectionInternally(String reason) {
		performClose(reason, false, false, false);
	}

	/**
	 * Close the peer connection from the PEPeerControl manager side. NOTE: This
	 * method assumes PEPeerControl already knows about the close. This method
	 * is inteded to be only invoked by select administrative methods. You
	 * probably should not invoke this directly.
	 */
	public void closeConnection(String reason) {
		performClose(reason, false, true, false);
	}

	private void performClose(String reason, boolean connect_failed, boolean externally_closed, boolean network_failure) {
		try {
			closing_mon.enter();

			if (closing)
				return;
			closing = true;

			if (mReceiptDispatcher != null)
				mReceiptDispatcher.check(true);

			// immediatly lose interest in peer
			interested_in_other_peer = false;
			lastNeededUndonePieceChange = Long.MAX_VALUE;

			if (isSnubbed())
				manager.decNbPeersSnubbed();

			if (identityAdded) { // remove identity
				if (peer_id != null)
					PeerIdentityManager.removeIdentity(manager.getPeerIdentityDataID(), peer_id, getPort());
				else
					Debug.out("PeerIdentity added but peer_id == null !!!");
				identityAdded = false;
			}

			changePeerState(PEPeer.CLOSING);

		} finally {
			closing_mon.exit();
		}

		// cancel any pending requests (on the manager side)
		cancelRequests();

		if (outgoing_have_message_aggregator != null) {
			outgoing_have_message_aggregator.destroy();
		}

		if (peer_exchange_item != null) {
			peer_exchange_item.destroy();
		}

		if (outgoing_piece_message_handler != null) {
			outgoing_piece_message_handler.destroy();
		}

		if (connection != null) { // can be null if close is called within
									// ::<init>::, like when the given port is
									// invalid
			connection.close();
		}

		if (ip_resolver_request != null) {
			ip_resolver_request.cancel();
		}

		removeAvailability();

		changePeerState(PEPeer.DISCONNECTED);

		if (Logger.isEnabled())
			Logger.log(new LogEvent(this, LOGID, "Peer connection closed: " + reason));

		if (!externally_closed) { // if closed internally, notify manager,
									// otherwise we assume it already knows
			manager.peerConnectionClosed(this, connect_failed, network_failure);
		}

		/*
		 * all managed references should have been removed by now add to
		 * recently disconnected list and null some stuff to make the object
		 * lighter
		 */

		outgoing_have_message_aggregator = null;
		peer_exchange_item = null;
		outgoing_piece_message_handler = null;
		plugin_connection = null;

		// Edit: by isdal: don't remember f2f peers, there is no way to reconnect anyway
		if(!PEPeerSource.PS_OSF2F.equals(getPeerSource())){
			// only save stats if it's worth doing so; ignore rapid
			// connect-disconnects
			if (peer_stats.getTotalDataBytesReceived() > 0 || peer_stats.getTotalDataBytesSent() > 0 || SystemTime.getCurrentTime() - connection_established_time > 30 * 1000)
				recentlyDisconnected.put(mySessionID, this);
		}
	}

	public PEPeerTransport reconnect(boolean tryUDP) {

		boolean use_tcp = isTCP() && !(tryUDP && getUDPListenPort() > 0);

		if ((use_tcp && getTCPListenPort() > 0) || (!use_tcp && getUDPListenPort() > 0)) {
			boolean use_crypto = getPeerItemIdentity().getHandshakeType() == PeerItemFactory.HANDSHAKE_TYPE_CRYPTO;

			PEPeerTransport new_conn = PEPeerTransportFactory.createTransport(manager, getPeerSource(), getIp(), getTCPListenPort(), getUDPListenPort(), use_tcp, use_crypto, crypto_level, null);

			// log to both relations
			Logger.log(new LogEvent(new Object[] { this, new_conn }, LOGID, "attempting to reconnect, creating new connection"));
			if (new_conn instanceof PEPeerTransportProtocol) {
				PEPeerTransportProtocol pt = (PEPeerTransportProtocol) new_conn;
				pt.checkForReconnect(mySessionID);
			}

			manager.addPeer(new_conn);

			return (new_conn);
		} else {
			return (null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.core3.peer.impl.PEPeerTransport#isSafeForReconnect()
	 */
	public boolean isSafeForReconnect() {
		return allowReconnect;
	}

	private void checkForReconnect(HashWrapper oldID) {
		PEPeerTransportProtocol oldTransport = recentlyDisconnected.remove(oldID);

		if (oldTransport != null) {
			Logger.log(new LogEvent(this, LOGID, LogAlert.AT_INFORMATION, "reassociating stats from " + oldTransport + " with this connection"));
			peerSessionID = oldTransport.peerSessionID;
			peer_stats = oldTransport.peer_stats;
			peer_stats.setPeer(this);
			unchokedTimeTotal += oldTransport.unchokedTimeTotal;
			unchokedTime += oldTransport.unchokedTime;
			setSnubbed(oldTransport.isSnubbed());
			snubbed = oldTransport.snubbed;
			last_good_data_time = oldTransport.last_good_data_time;
		}
	}

	private void generateFallbackSessionId() {
		SHA1Hasher sha1 = new SHA1Hasher();
		sha1.update(peer_id);
		sha1.update(getIp().getBytes());
		mySessionID = sha1.getHash();
		checkForReconnect(mySessionID);
	}

	private void addAvailability() {
		if (!availabilityAdded && !closing && peerHavePieces != null && current_peer_state == PEPeer.TRANSFERING) {
			final List peer_listeners_ref = peer_listeners_cow;
			if (peer_listeners_ref != null) {
				for (int i = 0; i < peer_listeners_ref.size(); i++) {
					final PEPeerListener peerListener = (PEPeerListener) peer_listeners_ref.get(i);
					peerListener.addAvailability(this, peerHavePieces);
				}
				availabilityAdded = true;
			}
		}
	}

	private void removeAvailability() {
		if (availabilityAdded && peerHavePieces != null) {
			final List peer_listeners_ref = peer_listeners_cow;
			if (peer_listeners_ref != null) {
				for (int i = 0; i < peer_listeners_ref.size(); i++) {
					final PEPeerListener peerListener = (PEPeerListener) peer_listeners_ref.get(i);
					peerListener.removeAvailability(this, peerHavePieces);
				}
			}
			availabilityAdded = false;
		}
		peerHavePieces = null;
	}

	protected void sendBTHandshake() {
		if (!handshake_sent) {
			connection.getOutgoingMessageQueue().addMessage(new BTHandshake(manager.getHash(), manager.getPeerId(), manager.isExtendedMessagingEnabled(), other_peer_handshake_version), false);
		}
	}

	// We could do this in a more automated way in future, but hardcoded is
	// simple and quick,
	// so we'll do that instead. :)
	static Map lt_ext_map = UTPeerExchange.ENABLED ? Collections.singletonMap("ut_pex", new Integer(1)) : Collections.EMPTY_MAP;

	private void sendLTHandshake() {
		String client_name = Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION;
		int localTcpPort = TCPNetworkManager.getSingleton().getTCPListeningPortNumber();
		String tcpPortOverride = COConfigurationManager.getStringParameter("TCP.Listen.Port.Override");
		try {
			localTcpPort = Integer.parseInt(tcpPortOverride);
		} catch (NumberFormatException e) {
		} // ignore as invalid input
		boolean require_crypto = NetworkManager.getCryptoRequired(manager.getAdapter().getCryptoLevel());

		Map data_dict = new HashMap();
		data_dict.put("m", lt_ext_map);
		data_dict.put("v", client_name);
		data_dict.put("p", new Integer(localTcpPort));
		data_dict.put("e", new Long(require_crypto ? 1L : 0L));
		LTHandshake lt_handshake = new LTHandshake(data_dict, other_peer_bt_lt_ext_version);
		connection.getOutgoingMessageQueue().addMessage(lt_handshake, false);
	}

	private void sendAZHandshake() {
		final Message[] avail_msgs = MessageManager.getSingleton().getRegisteredMessages();
		final String[] avail_ids = new String[avail_msgs.length];
		final byte[] avail_vers = new byte[avail_msgs.length];

		for (int i = 0; i < avail_msgs.length; i++) {
			avail_ids[i] = avail_msgs[i].getID();
			avail_vers[i] = avail_msgs[i].getVersion();
		}

		int local_tcp_port = TCPNetworkManager.getSingleton().getTCPListeningPortNumber();
		int local_udp_port = UDPNetworkManager.getSingleton().getUDPListeningPortNumber();
		int local_udp2_port = UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber();
		String tcpPortOverride = COConfigurationManager.getStringParameter("TCP.Listen.Port.Override");

		try {
			local_tcp_port = Integer.parseInt(tcpPortOverride);
		} catch (NumberFormatException e) {
		} // ignore as invalid input

		boolean require_crypto = NetworkManager.getCryptoRequired(manager.getAdapter().getCryptoLevel());

		if (peerSessionID != null)
			Logger.log(new LogEvent(this, LOGID, LogEvent.LT_INFORMATION, "notifying peer of reconnect attempt"));

		AZHandshake az_handshake = new AZHandshake(AZPeerIdentityManager.getAZPeerIdentity(), mySessionID, peerSessionID, Constants.AZUREUS_NAME, Constants.AZUREUS_VERSION, local_tcp_port, local_udp_port, local_udp2_port, avail_ids, avail_vers, require_crypto ? AZHandshake.HANDSHAKE_TYPE_CRYPTO : AZHandshake.HANDSHAKE_TYPE_PLAIN, other_peer_handshake_version);

		connection.getOutgoingMessageQueue().addMessage(az_handshake, false);
	}

	public int getPeerState() {
		return current_peer_state;
	}

	public boolean isDownloadPossible() {
		if (!closing && !choked_by_other_peer) {
			if (lastNeededUndonePieceChange < piecePicker.getNeededUndonePieceChange()) {
				checkInterested();
				lastNeededUndonePieceChange = piecePicker.getNeededUndonePieceChange();
			}
			if (interested_in_other_peer && current_peer_state == PEPeer.TRANSFERING)
				return true;
		}
		return false;
	}

	public int getPercentDoneInThousandNotation() {
		long total_done = getBytesDownloaded();

		return (int) ((total_done * 1000) / diskManager.getTotalLength());
	}

	public boolean transferAvailable() {
		return (!choked_by_other_peer && interested_in_other_peer);
	}

	private void printRequestStats() {
		if (SHOW_DISCARD_RATE_STATS) {
			final float discard_perc = (requests_discarded * 100F) / ((requests_completed + requests_recovered + requests_discarded) * 1F);
			final float discard_perc_end = (requests_discarded_endgame * 100F) / ((requests_completed + requests_recovered + requests_discarded_endgame) * 1F);
			final float recover_perc = (requests_recovered * 100F) / ((requests_recovered + requests_discarded) * 1F);
			System.out.println("c=" + requests_completed + " d=" + requests_discarded + " de=" + requests_discarded_endgame + " r=" + requests_recovered + " dp=" + discard_perc + "% dpe=" + discard_perc_end + "% rp=" + recover_perc + "%");
		}
	}

	/**
	 * Checks if this peer is a seed or not by trivially checking if thier Have
	 * bitflags exisits and shows a number of bits set equal to the torrent # of
	 * pieces (and the torrent # of pieces is >0)
	 */
	private void checkSeed() {
		// seed implicitly means *something* to send (right?)
		if (peerHavePieces != null && nbPieces > 0)
			setSeed((peerHavePieces.nbSet == nbPieces));
		else
			setSeed(false);
	}

	public DiskManagerReadRequest request(final int pieceNumber, final int pieceOffset, final int pieceLength) {
		final DiskManagerReadRequest request = manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength);
		if (current_peer_state != TRANSFERING) {
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
			connection.getOutgoingMessageQueue().addMessage(new BTRequest(pieceNumber, pieceOffset, pieceLength, other_peer_request_version), false);
			_lastPiece = pieceNumber;

			try {
				recent_outgoing_requests_mon.enter();

				recent_outgoing_requests.put(request, null);
			} finally {
				recent_outgoing_requests_mon.exit();
			}
			return request;
		}
		return null;
	}

	public int getRequestIndex(DiskManagerReadRequest request) {
		try {
			requested_mon.enter();

			return (requested.indexOf(request));

		} finally {

			requested_mon.exit();
		}
	}

	public void sendCancel(DiskManagerReadRequest request) {
		if (current_peer_state != TRANSFERING)
			return;
		if (hasBeenRequested(request)) {
			removeRequest(request);
			connection.getOutgoingMessageQueue().addMessage(new BTCancel(request.getPieceNumber(), request.getOffset(), request.getLength(), other_peer_cancel_version), false);
		}
	}

	public void sendHave(int pieceNumber) {
		if (current_peer_state != TRANSFERING || pieceNumber == manager.getHiddenPiece())
			return;
		// only force if the other peer doesn't have this piece and is not yet
		// interested or we;ve disabled
		// aggregation
		final boolean force = !other_peer_interested_in_me && peerHavePieces != null && !peerHavePieces.flags[pieceNumber];

		outgoing_have_message_aggregator.queueHaveMessage(pieceNumber, force || have_aggregation_disabled);
		checkInterested();
	}

	public void sendChoke() {
		if (current_peer_state != TRANSFERING)
			return;

		if (mReceiptDispatcher != null)
			mReceiptDispatcher.check(false);

		// System.out.println( "["+(System.currentTimeMillis()/1000)+"] "
		// +connection + " choked");

		connection.getOutgoingMessageQueue().addMessage(new BTChoke(other_peer_choke_version), false);
		choking_other_peer = true;
		is_optimistic_unchoke = false;

		if (outgoing_piece_message_handler != null) {
			outgoing_piece_message_handler.removeAllPieceRequests();
			outgoing_piece_message_handler.destroy();
			outgoing_piece_message_handler = null;
		}
	}

	public void sendUnChoke() {
		if (current_peer_state != TRANSFERING)
			return;

		// System.out.println( "["+(System.currentTimeMillis()/1000)+"] "
		// +connection + " unchoked");
		if (outgoing_piece_message_handler == null) {
			outgoing_piece_message_handler = new OutgoingBTPieceMessageHandler(this, connection.getOutgoingMessageQueue(), new OutgoingBTPieceMessageHandlerAdapter() {
				public void diskRequestCompleted(long bytes) {
					peer_stats.diskReadComplete(bytes);
				}
			}, other_peer_piece_version);
		}

		choking_other_peer = false; // set this first as with pseudo peers we
									// can effectively synchronously act
		// on the unchoke advice and we don't want that borking with choked
		// still set

		connection.getOutgoingMessageQueue().addMessage(new BTUnchoke(other_peer_unchoke_version), false);
	}

	private void sendKeepAlive() {
		if (current_peer_state != TRANSFERING)
			return;

		if (outgoing_have_message_aggregator.hasPending()) {
			outgoing_have_message_aggregator.forceSendOfPending();
		} else {
			connection.getOutgoingMessageQueue().addMessage(new BTKeepAlive(other_peer_keep_alive_version), false);
		}
	}

	private void sendMainlineDHTPort() {
		if (!this.ml_dht_enabled) {
			return;
		}
		MainlineDHTProvider provider = getDHTProvider();
		if (provider == null) {
			return;
		}
		Message message = new BTDHTPort(provider.getDHTPort());
		connection.getOutgoingMessageQueue().addMessage(message, false);
	}

	/**
	 * Global checkInterested method. Early-out scan of pieces to determine if
	 * the peer is interesting or not. They're interesting if they have a piece
	 * that we Need and isn't Done
	 */
	public void checkInterested() {
		if (closing || peerHavePieces == null || peerHavePieces.nbSet == 0)
			return;

		boolean is_interesting = false;
		if (piecePicker.hasDownloadablePiece()) { // there is a piece worth
													// being interested in
			if (!isSeed()) { // check individually if don't have all
				for (int i = peerHavePieces.start; i <= peerHavePieces.end; i++) {
					if (peerHavePieces.flags[i] && diskManager.isInteresting(i)) {
						is_interesting = true;
						break;
					}
				}
			} else
				is_interesting = true;
		}

		if (is_interesting && !interested_in_other_peer)
			connection.getOutgoingMessageQueue().addMessage(new BTInterested(other_peer_interested_version), false);
		else if (!is_interesting && interested_in_other_peer)
			connection.getOutgoingMessageQueue().addMessage(new BTUninterested(other_peer_uninterested_version), false);

		interested_in_other_peer = is_interesting;
	}

	/**
	 * @deprecated no longer used by CVS code Checks if a particular piece makes
	 *             us interested in the peer
	 * @param pieceNumber
	 *            the piece number that has been received
	 */
	/*
	 * private void checkInterested(int pieceNumber) { if (closing) return;
	 *  // Do we need this piece and it's not Done? if
	 * (!interested_in_other_peer &&diskManager.isInteresting(pieceNumber)) {
	 * connection.getOutgoingMessageQueue().addMessage( new BTInterested(),
	 * false ); interested_in_other_peer =true; } }
	 */

	/**
	 * Private method to send the bitfield.
	 */
	private void sendBitField() {
		if (closing)
			return;

		// In case we're in super seed mode, we don't send our bitfield
		if (manager.isSuperSeedMode())
			return;

		// create bitfield
		final DirectByteBuffer buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, (nbPieces + 7) / 8);
		final DiskManagerPiece[] pieces = diskManager.getPieces();

		int num_pieces = pieces.length;

		HashSet lazies = null;
		int[] lazy_haves = null;

		if (ENABLE_LAZY_BITFIELD) {
			int bits_in_first_byte = Math.min(num_pieces, 8);
			int last_byte_start_bit = (num_pieces / 8) * 8;
			int bits_in_last_byte = num_pieces - last_byte_start_bit;
			if (bits_in_last_byte == 0) {
				bits_in_last_byte = 8;
				last_byte_start_bit -= 8;
			}
			lazies = new HashSet();
			// one bit from first byte
			int first_byte_entry = rnd.nextInt(bits_in_first_byte);
			if (pieces[first_byte_entry].isDone()) {
				lazies.add(new MutableInteger(first_byte_entry));
			}
			// one bit from last byte
			int last_byte_entry = last_byte_start_bit + rnd.nextInt(bits_in_last_byte);
			if (pieces[last_byte_entry].isDone()) {
				lazies.add(new MutableInteger(last_byte_entry));
			}
			// random others missing
			int other_lazies = rnd.nextInt(16) + 4;
			for (int i = 0; i < other_lazies; i++) {
				int random_entry = rnd.nextInt(num_pieces);
				if (pieces[random_entry].isDone()) {
					lazies.add(new MutableInteger(random_entry));
				}
			}
			int num_lazy = lazies.size();
			if (num_lazy == 0) {
				lazies = null;
			} else {
				lazy_haves = new int[num_lazy];
				Iterator it = lazies.iterator();
				for (int i = 0; i < num_lazy; i++) {
					int lazy_have = ((MutableInteger) it.next()).getValue();
					lazy_haves[i] = lazy_have;
				}
				if (num_lazy > 1) {
					for (int i = 0; i < num_lazy; i++) {
						int swap = rnd.nextInt(num_lazy);
						if (swap != i) {
							int temp = lazy_haves[swap];
							lazy_haves[swap] = lazy_haves[i];
							lazy_haves[i] = temp;
						}
					}
				}
			}
		}

		int bToSend = 0;
		int i = 0;

		MutableInteger mi = new MutableInteger(0);

		int hidden_piece = manager.getHiddenPiece();

		for (; i < num_pieces; i++) {

			if ((i % 8) == 0) {
				bToSend = 0;
			}

			bToSend = bToSend << 1;

			if (pieces[i].isDone() && i != hidden_piece) {

				if (lazies != null) {

					mi.setValue(i);

					if (lazies.contains(mi)) {

						// System.out.println( "LazySet: " + getIp() + " -> " +
						// i );

					} else {
						bToSend += 1;
					}
				} else {
					bToSend += 1;
				}
			}

			if ((i % 8) == 7) {
				buffer.put(DirectByteBuffer.SS_BT, (byte) bToSend);
			}
		}

		if ((i % 8) != 0) {

			bToSend = bToSend << (8 - (i % 8));
			buffer.put(DirectByteBuffer.SS_BT, (byte) bToSend);
		}

		buffer.flip(DirectByteBuffer.SS_BT);

		connection.getOutgoingMessageQueue().addMessage(new BTBitfield(buffer, other_peer_bitfield_version), false);

		if (lazy_haves != null) {

			final int[] f_lazy_haves = lazy_haves;

			SimpleTimer.addEvent("LazyHaveSender", SystemTime.getCurrentTime() + 1000 + rnd.nextInt(2000), new TimerEventPerformer() {
				int next_have = 0;

				public void perform(TimerEvent event) {
					int lazy_have = f_lazy_haves[next_have++];
					// System.out.println( "LazyDone: " + getIp() + " -> " +
					// lazy_have );

					if (current_peer_state == TRANSFERING) {

						connection.getOutgoingMessageQueue().addMessage(new BTHave(lazy_have, other_peer_bt_have_version), false);

						if (next_have < f_lazy_haves.length && current_peer_state == TRANSFERING) {

							SimpleTimer.addEvent("LazyHaveSender", SystemTime.getCurrentTime() + rnd.nextInt(2000), this);
						}
					}
				}
			});
		}

	}

	public byte[] getId() {
		return peer_id;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public int getTCPListenPort() {
		return tcp_listen_port;
	}

	public int getUDPListenPort() {
		return udp_listen_port;
	}

	public int getUDPNonDataListenPort() {
		return (udp_non_data_port);
	}

	public String getClient() {
		return client;
	}

	public boolean isIncoming() {
		return incoming;
	}

	public boolean isOptimisticUnchoke() {
		return is_optimistic_unchoke && !isChokedByMe();
	}

	public void setOptimisticUnchoke(boolean is_optimistic) {
		is_optimistic_unchoke = is_optimistic;
	}

	public PEPeerControl getControl() {
		return manager;
	}

	public PEPeerManager getManager() {
		return manager;
	}

	public PEPeerStats getStats() {
		return peer_stats;
	}

	public int[] getPriorityOffsets() {
		// normal peer has no special priority requirements

		return (null);
	}

	public boolean requestAllocationStarts(int[] base_priorities) {
		return (false);
	}

	public void requestAllocationComplete() {
	}

	/**
	 * @return null if no bitfield has been recieved yet else returns BitFlags
	 *         indicating what pieces the peer has
	 */
	public BitFlags getAvailable() {
		return peerHavePieces;
	}

	public boolean isPieceAvailable(int pieceNumber) {
		if (peerHavePieces != null)
			return peerHavePieces.flags[pieceNumber];
		return false;
	}

	public boolean isChokingMe() {
		return choked_by_other_peer;
	}

	public boolean isChokedByMe() {
		return choking_other_peer;
	}

	/**
	 * @return true if the peer is interesting to us
	 */
	public boolean isInteresting() {
		return interested_in_other_peer;
	}

	/**
	 * @return true if the peer is interested in what we're offering
	 */
	public boolean isInterested() {
		return other_peer_interested_in_me;
	}

	public boolean isSeed() {
		return seed_set_by_accessor;
	}

	private void setSeed(boolean s) {
		if (seed_set_by_accessor != s) {

			seed_set_by_accessor = s;

			if (peer_exchange_item != null && s) {

				peer_exchange_item.seedStatusChanged();
			}
		}
	}

	public boolean isSnubbed() {
		return snubbed != 0;
	}

	public long getSnubbedTime() {
		if (snubbed == 0)
			return 0;
		final long now = SystemTime.getCurrentTime();
		if (now < snubbed)
			snubbed = now - 26; // odds are ...
		return now - snubbed;
	}

	public void setSnubbed(boolean b) {
		if (!closing) {
			final long now = SystemTime.getCurrentTime();
			if (!b) {
				if (snubbed != 0) {
					snubbed = 0;
					manager.decNbPeersSnubbed();
					if (!choked_by_other_peer)
						unchokedTime = now;
				}
			} else if (snubbed == 0) {
				snubbed = now;
				manager.incNbPeersSnubbed();
				if (!choked_by_other_peer) {
					final long unchoked = now - unchokedTime;
					if (unchoked > 0)
						unchokedTimeTotal += unchoked;
				}
			}
		}
	}

	public void setUploadHint(int spreadTime) {
		spreadTimeHint = spreadTime;
	}

	public int getUploadHint() {
		return spreadTimeHint;
	}

	public void setUniqueAnnounce(int _uniquePiece) {
		uniquePiece = _uniquePiece;
	}

	public int getUniqueAnnounce() {
		return uniquePiece;
	}

	/** To retreive arbitrary objects against a peer. */
	public Object getData(String key) {
		if (data == null)
			return null;
		return data.get(key);
	}

	/** To store arbitrary objects against a peer. */
	public void setData(String key, Object value) {
		try {
			general_mon.enter();

			if (data == null) {
				data = new HashMap();
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

	public String getIPHostName() {
		if (ip_resolved == null) {

			ip_resolved = ip;

			ip_resolver_request = IPToHostNameResolver.addResolverRequest(ip_resolved, new IPToHostNameResolverListener() {
				public final void IPResolutionComplete(String res, boolean ok) {
					ip_resolved = res;
				}
			});
		}

		return (ip_resolved);
	}

	private void cancelRequests() {
		if (!closing) { // cancel any unsent requests in the queue
			final Message[] type = { new BTRequest(-1, -1, -1, other_peer_request_version) };
			connection.getOutgoingMessageQueue().removeMessagesOfType(type, false);
		}
		if (requested != null && requested.size() > 0) {
			try {
				requested_mon.enter();

				if (!closing) { // may have unchoked us, gotten a request, then
								// choked without filling it - snub them
					// if they actually have data coming in, they'll be
					// unsnubbed as soon as it writes
					final long timeSinceGoodData = getTimeSinceGoodDataReceived();
					if (timeSinceGoodData == -1 || timeSinceGoodData > 60 * 1000)
						setSnubbed(true);
				}
				for (int i = requested.size() - 1; i >= 0; i--) {
					final DiskManagerReadRequest request = (DiskManagerReadRequest) requested.remove(i);
					manager.requestCanceled(request);
				}
			} finally {

				requested_mon.exit();
			}
		}
	}

	public int getMaxNbRequests() {
		return (-1);
	}

	public int getNbRequests() {
		return requested.size();
	}

	/**
	 * 
	 * @return may be null for performance purposes
	 */

	public List getExpiredRequests() {
		List result = null;

		// this is frequently called, hence we operate without a monitor and
		// take the hit of possible exceptions due to concurrent list
		// modification (only out-of-bounds can occur)

		try {
			for (int i = requested.size() - 1; i >= 0; i--) {
				final DiskManagerReadRequest request = (DiskManagerReadRequest) requested.get(i);

				if (request.isExpired()) {

					if (result == null) {

						result = new ArrayList();
					}

					result.add(request);
				}
			}

			return (result);

		} catch (Throwable e) {

			return result;
		}
	}

	private boolean hasBeenRequested(DiskManagerReadRequest request) {
		try {
			requested_mon.enter();

			return requested.contains(request);
		} finally {
			requested_mon.exit();
		}
	}

	/**
	 * @deprecated no longer used by CVS code
	 */
	protected void addRequest(DiskManagerReadRequest request) {
		try {
			requested_mon.enter();

			requested.add(request);
		} finally {

			requested_mon.exit();
		}
		_lastPiece = request.getPieceNumber();
	}

	protected void removeRequest(DiskManagerReadRequest request) {
		try {
			requested_mon.enter();

			requested.remove(request);
		} finally {

			requested_mon.exit();
		}
		final BTRequest msg = new BTRequest(request.getPieceNumber(), request.getOffset(), request.getLength(), other_peer_request_version);
		connection.getOutgoingMessageQueue().removeMessage(msg, false);
		msg.destroy();
	}

	private void resetRequestsTime(final long now) {
		try {
			requested_mon.enter();

			final int requestedSize = requested.size();
			for (int i = 0; i < requestedSize; i++) {
				final DiskManagerReadRequest request = (DiskManagerReadRequest) requested.get(i);
				if (request != null)
					request.resetTime(now);
			}
		} finally {

			requested_mon.exit();
		}
	}

	public String toString() {
		if (connection != null && connection.isConnected()) {
			return connection + (isTCP() ? " [" : "(UDP) [") + client + "]";
		}
		return (isIncoming() ? "R: " : "L: ") + ip + ":" + port + (isTCP() ? " [" : "(UDP) [") + client + "]";
	}

	public String getString() {
		return (toString());
	}

	public void doKeepAliveCheck() {
		final long now = SystemTime.getCurrentTime();
		final long wait_time = now - last_message_sent_time;

		if (last_message_sent_time == 0 || wait_time < 0) {
			last_message_sent_time = now; // don't send if brand new
											// connection
			return;
		}

		if (wait_time > 2 * 60 * 1000) { // 2min keep-alive timer
			sendKeepAlive();
			last_message_sent_time = now; // not quite true, but we don't want
											// to queue multiple keep-alives
											// before the first is actually sent
		}
	}

	public boolean doTimeoutChecks() {
		// Timeouts for states PEPeerTransport.CONNECTION_PENDING and
		// PEPeerTransport.CONNECTION_CONNECTING are handled by the
		// ConnectDisconnectManager
		// so we don't need to deal with them here.

		final long now = SystemTime.getCurrentTime();
		// make sure we time out stalled connections
		if (connection_state == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED) {
			if (last_message_received_time > now)
				last_message_received_time = now;
			if (last_data_message_received_time > now)
				last_data_message_received_time = now;
			if (now - last_message_received_time > 5 * 60 * 1000 && now - last_data_message_received_time > 5 * 60 * 1000) { // 5min
																																// timeout
				// assume this is due to a network failure
				// e.g. something that didn't close the TCP socket properly
				// will attempt reconnect
				closeConnectionInternally("timed out while waiting for messages", false, true);
				return true;
			}
		}
		// ensure we dont get stuck in the handshaking phases
		else if (connection_state == PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE) {
			if (connection_established_time > now)
				connection_established_time = now;
			else if (now - connection_established_time > 3 * 60 * 1000) { // 3min
																			// timeout
				closeConnectionInternally("timed out while waiting for handshake");
				return true;
			}
		}

		return false;
	}

	public void doPerformanceTuningCheck() {
		Transport transport = connection.getTransport();

		if (transport != null && peer_stats != null && outgoing_piece_message_handler != null) {

			// send speed -based tuning
			final long send_rate = peer_stats.getDataSendRate() + peer_stats.getProtocolSendRate();

			if (send_rate >= 3125000) { // 25 Mbit/s
				transport.setTransportMode(Transport.TRANSPORT_MODE_TURBO);
				outgoing_piece_message_handler.setRequestReadAhead(256);
			} else if (send_rate >= 1250000) { // 10 Mbit/s
				transport.setTransportMode(Transport.TRANSPORT_MODE_TURBO);
				outgoing_piece_message_handler.setRequestReadAhead(128);
			} else if (send_rate >= 125000) { // 1 Mbit/s
				if (transport.getTransportMode() < Transport.TRANSPORT_MODE_FAST) {
					transport.setTransportMode(Transport.TRANSPORT_MODE_FAST);
				}
				outgoing_piece_message_handler.setRequestReadAhead(32);
			} else if (send_rate >= 62500) { // 500 Kbit/s
				outgoing_piece_message_handler.setRequestReadAhead(16);
			} else if (send_rate >= 31250) { // 250 Kbit/s
				outgoing_piece_message_handler.setRequestReadAhead(8);
			} else if (send_rate >= 12500) { // 100 Kbit/s
				outgoing_piece_message_handler.setRequestReadAhead(4);
			} else {
				outgoing_piece_message_handler.setRequestReadAhead(2);
			}

			// receive speed -based tuning
			final long receive_rate = peer_stats.getDataReceiveRate() + peer_stats.getProtocolReceiveRate();

			if (receive_rate >= 1250000) { // 10 Mbit/s
				transport.setTransportMode(Transport.TRANSPORT_MODE_TURBO);
			} else if (receive_rate >= 125000) { // 1 Mbit/s
				if (transport.getTransportMode() < Transport.TRANSPORT_MODE_FAST) {
					transport.setTransportMode(Transport.TRANSPORT_MODE_FAST);
				}
			}

		}
	}

	public int getConnectionState() {
		return connection_state;
	}

	public long getTimeSinceLastDataMessageReceived() {
		if (last_data_message_received_time == -1) { // never received
			return -1;
		}

		final long now = SystemTime.getCurrentTime();

		if (last_data_message_received_time > now)
			last_data_message_received_time = now; // time went backwards
		return now - last_data_message_received_time;
	}

	public long getTimeSinceGoodDataReceived() {
		if (last_good_data_time == -1)
			return -1; // never received
		final long now = SystemTime.getCurrentTime();
		if (last_good_data_time > now)
			last_good_data_time = now; // time went backwards
		return now - last_good_data_time;
	}

	public long getTimeSinceLastDataMessageSent() {
		if (last_data_message_sent_time == -1) { // never sent
			return -1;
		}
		final long now = SystemTime.getCurrentTime();
		if (last_data_message_sent_time > now)
			last_data_message_sent_time = now; // time went backwards
		return now - last_data_message_sent_time;
	}

	public long getTimeSinceConnectionEstablished() {
		if (connection_established_time == 0) { // fudge it while the transport
												// is being connected
			return 0;
		}
		final long now = SystemTime.getCurrentTime();
		if (connection_established_time > now)
			connection_established_time = now;
		return now - connection_established_time;
	}

	public int getConsecutiveNoRequestCount() {
		return (consecutive_no_request_count);
	}

	public void setConsecutiveNoRequestCount(int num) {
		consecutive_no_request_count = num;
	}

	protected void decodeReceiptRequests(ReceiptRequests message) {
		System.out.println("trying to decode receipt request: " + message);
		ReputationDAO rep = ReputationDAO.get();
		try {
			mAttribution = message.getKeys().keySet();

			Map<PublicKey, Float> keys = message.getKeys();
			int doneSoFar = 0;
			List<Receipt> receipts = new LinkedList<Receipt>();
			for (PublicKey k : keys.keySet().toArray(new PublicKey[0])) {
				try {
					long internal_id = rep.get_internal_id(k);

					// sanity check
					if (internal_id == 1)
						throw new IOException("received a request for receipts from ourself");

					Receipt att = rep.get_latest_attestation_for_id(internal_id);

					assert att.getSigningID() == internal_id : "requested a receipt from an id and got one for a different one";
					assert internal_id != rep.get_internal_id(getCertificate().getPublicKey()) : "some peer requested their own receipt";

					if (att == null) {
						System.err.println("false positive: didn't have receipt for requested intermediary: " + KeyManipulation.concise(k.getEncoded()) + " " + message + " internal id: " + internal_id);
					} else {
						receipts.add(att);
					}
				} catch (IOException e) {
					System.err.println("error minting receipt, false positive? " + e.toString());
				}

				doneSoFar++;
				if (doneSoFar > 30) // TODO: magic constant. we should figure
									// out a sensible way to negotiate this.
					break;
			}

			List<Integer> offsets = rep.compute_offsets_from_latest_receipts(receipts);

			ReceiptBundle bundle = new ReceiptBundle(receipts.toArray(new Receipt[0]), offsets, os_receipt_bundle);
			System.out.println("sending receipt bundle: " + bundle);
			connection.getOutgoingMessageQueue().addMessage(bundle, false);
		} catch (Exception e) {
			message.destroy();
			closeConnectionInternally("Error decoding receipt requests: " + e);
			return;
		}
	}

	protected void decodeReceiptBundle(ReceiptBundle message) {
		System.out.println("attempting decode of receipt bundle: " + message);

		// requested_receipts
		ReputationDAO rep = ReputationDAO.get();

		int[] offsets = message.getReceivedDueToRecoOffsets();

		for (int i = 0; i < message.getReceipts().length; i++) {
			/**
			 * Bind these together here. Because the reco_offset is not
			 * serialized, we also need to do this in the persistent storage (in
			 * case we are forced to delay verification due to unavailability)
			 */
			Receipt r = message.getReceipts()[i];
			r.set_received_due_to_reco_offset(offsets[i]);
			try {
				long local_int_id = rep.get_internal_id(r.getSigningKey());

				if (local_int_id == 1)
					throw new IOException("received a receipt bundle containing a receipt from us -- shouldn't happen");

				if (requested_receipts.containsKey(local_int_id))
					requested_receipts.put(local_int_id, r);
				else
					System.out.println("false positive dfg89df"); // TODO:
																	// more
																	// informative
																	// accounting
																	// here

				// System.out.println("successful decode (bound requested
				// receipts");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// If we're here, we haven't yet sent this but now one hop negiotiation
		// is fully complete
		this.initPostConnection(mHandshake);
	}

	protected void decodeCertificateExchange(CertificateExchange message) {
		System.out.println("got certificate exchange (bf has: " + message.getTopK().getStoredCount() + ")");

		/**
		 * 1. Add this peer to the database (or update existing entry) 2. Check
		 * the bloom filter for shared intermediaries 3. Request receipts from
		 * shared intermediaries (if this is a non-seed connection --- otherwise
		 * we don't care about ROI)
		 */

		ReputationDAO rep = ReputationDAO.get();
		long local_id = -1;

		// TODO: make this throw new IOException?
		if (mCertificate != null)
			closeConnectionInternally("got certificate exchange even though mCertificate != null!");

		mCertificate = message.getCertificate();
		mDirectAdvertisements = message.getAdvertisements();

		try {
			// 1. Add/update DB
			local_id = rep.get_internal_id(message.getCertificate().getPublicKey());
			rep.direct_observation(local_id, InetAddress.getByName(getIp()), getManager().getHash());

			rep.update_soft_state(mCertificate.getPublicKey(), InetAddress.getByName(getIp()).getAddress(), getTCPListenPort(), getUDPListenPort(), new Date());

			boolean sentReceiptReq = false;

			// 2. Check bloom filter
			System.out.println("checking bloom filter for shared ints: " + message.getTopK());
			Map<PublicKey, Float> desired_intermediaries = Computation.desired_nodes_from_topK(message.getTopK());

			// no need to ask for receipt from the signer
			desired_intermediaries.remove(getCertificate().getPublicKey());
			// or from ourselves
			desired_intermediaries.remove(LocalIdentity.get().getKeys().getPublic());

			if (desired_intermediaries.size() > 0) {
				// keep track of these so we'll know later if we're actually
				// getting receipts we asked for and not false positives
				for (PublicKey k : desired_intermediaries.keySet().toArray(new PublicKey[0])) {
					Long internal_id = rep.get_internal_id(k);

					if (internal_id == null)
						throw new IOException("internal id is null -- shouldn't happen");

					assert internal_id != 1 : "we should have removed ourselves from desired intermediaries but didn't";
					assert internal_id != rep.get_internal_id(getCertificate().getPublicKey()) : "we should have removed the remote host from desired but didn't";

					requested_receipts.put(internal_id, null);
				}

				// 3. Request receipts
				ReceiptRequests req = new ReceiptRequests(desired_intermediaries, os_receipt_requests);
				System.out.println("sending receipt request: " + req);
				connection.getOutgoingMessageQueue().addMessage(req, false);
				sentReceiptReq = true;
			}

			// didn't find shared ints, so we can immediately move to exchange
			if (sentReceiptReq == false) {
				System.out.println("didn't find any shared ints, initPostConnnection() now");
				this.initPostConnection(mHandshake);
			}

		} catch (IOException e) {
			message.destroy();
			closeConnectionInternally("Error decoding certificate exchange: " + e);
			return;
		}

	}

	protected void decodeAttestation(Attestation inAttestation) {
		System.out.println("decoding attestation " + inAttestation);

		Receipt r = inAttestation.getReceipt();
		r.setPreferredIntermediaries(requested_receipts.keySet());

		ReputationDAO rep = ReputationDAO.get();
		try {
			// HACK: we requested_receipts to record the offsets
			rep.record_attestation(inAttestation, requested_receipts);
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	protected void decodeBTHandshake(BTHandshake handshake) {
		PeerIdentityDataID my_peer_data_id = manager.getPeerIdentityDataID();

		if (!Arrays.equals(manager.getHash(), handshake.getDataHash())) {
			closeConnectionInternally("handshake has wrong infohash");
			handshake.destroy();
			return;
		}

		peer_id = handshake.getPeerId();

		// Decode a client identification string from the given peerID
		this.client_peer_id = this.client = StringInterner.intern(PeerClassifier.getClientDescription(peer_id));

		// make sure the client type is not banned
		if (!PeerClassifier.isClientTypeAllowed(client)) {
			closeConnectionInternally(client + " client type not allowed to connect, banned");
			handshake.destroy();
			return;
		}

		// make sure we are not connected to ourselves
		if (Arrays.equals(manager.getPeerId(), peer_id)) {
			manager.peerVerifiedAsSelf(this); // make sure we dont do it again
			closeConnectionInternally("given peer id matches myself");
			handshake.destroy();
			return;
		}

		// make sure we are not already connected to this peer
		boolean sameIdentity = PeerIdentityManager.containsIdentity(my_peer_data_id, peer_id, getPort());
		boolean sameIP = false;

		// allow loopback connects for co-located proxy-based connections and
		// testing
		boolean same_allowed = COConfigurationManager.getBooleanParameter("Allow Same IP Peers") || ip.equals("127.0.0.1");
		if (!same_allowed) {
			if (PeerIdentityManager.containsIPAddress(my_peer_data_id, ip)) {
				sameIP = true;
			}
		}

		if (sameIdentity) {
			boolean close = true;

			if (connection.isLANLocal()) { // this new connection is lan-local

				PEPeerTransport existing = manager.getTransportFromIdentity(peer_id);

				if (existing != null) {

					String existing_ip = existing.getIp();

					// normally we don't allow a lan-local to replace a
					// lan-local connection. There is
					// however one exception - where the existing connection
					// comes from the gateway address
					// and therefore actually denotes an effectively
					// non-lan-local connection. Unfortunately
					// we don't have a good way of finding the default gateway,
					// so just go for ending in .1

					if (!existing.isLANLocal() || (existing_ip.endsWith(".1") && !existing_ip.equals(ip))) { // so
																												// drop
																												// the
																												// existing
																												// connection
																												// if
																												// it
																												// is
																												// an
																												// external
																												// (non
																												// lan-local)
																												// one

						Debug.outNoStack("Dropping existing non-lanlocal peer connection [" + existing + "] in favour of [" + this + "]");
						manager.removePeer(existing);
						close = false;
					}
				}
			}

			if (close) {
				closeConnectionInternally("peer matches already-connected peer id");
				handshake.destroy();
				return;
			}
		}

		if (sameIP) {
			closeConnectionInternally("peer matches already-connected IP address, duplicate connections not allowed");
			handshake.destroy();
			return;
		}

		// make sure we haven't reached our connection limit
		final int maxAllowed = manager.getMaxNewConnectionsAllowed();
		if (maxAllowed == 0 && !manager.doOptimisticDisconnect(isLANLocal())) {
			final String msg = "too many existing peer connections [p" + PeerIdentityManager.getIdentityCount(my_peer_data_id) + "/g" + PeerIdentityManager.getTotalIdentityCount() + ", pmx" + PeerUtils.MAX_CONNECTIONS_PER_TORRENT + "/gmx" + PeerUtils.MAX_CONNECTIONS_TOTAL + "/dmx" + manager.getMaxConnections() + "]";
			// System.out.println( msg );
			closeConnectionInternally(msg);
			handshake.destroy();
			return;
		}

		try {
			closing_mon.enter();

			if (closing) {

				final String msg = "connection already closing";

				closeConnectionInternally(msg);

				handshake.destroy();

				return;
			}

			if (!PeerIdentityManager.addIdentity(my_peer_data_id, peer_id, getPort(), ip)) {

				closeConnectionInternally("peer matches already-connected peer id");

				handshake.destroy();

				return;
			}

			identityAdded = true;

		} finally {

			closing_mon.exit();
		}

		if (Logger.isEnabled())
			Logger.log(new LogEvent(this, LOGID, "In: has sent their handshake"));

		// Let's store the reserved bits somewhere so they can be examined later
		// (externally).
		handshake_reserved_bytes = handshake.getReserved();

		/*
		 * Waiting until we've received the initiating-end's full handshake,
		 * before sending back our own, really should be the "proper" behavior.
		 * However, classic BT trackers running NAT checking will only send the
		 * first 48 bytes (up to infohash) of the peer handshake, skipping
		 * peerid, which means we'll never get their complete handshake, and
		 * thus never reply, which causes the NAT check to fail. So, we need to
		 * send our handshake earlier, after we've verified the infohash.
		 * 
		 * if( incoming ) { //wait until we've received their handshake before
		 * sending ours sendBTHandshake(); }
		 */

		this.ml_dht_enabled = (handshake_reserved_bytes[7] & 1) == 1;
		messaging_mode = decideExtensionProtocol(handshake);

		// extended protocol processing
		if (messaging_mode == MESSAGING_AZMP) {
			/**
			 * We log when a non-Azureus client claims to support extended
			 * messaging... Obviously other Azureus clients do, so there's no
			 * point logging about them!
			 */
			if (Logger.isEnabled() && client.indexOf("Azureus") == -1) {
				Logger.log(new LogEvent(this, LOGID, "Handshake claims extended AZ " + "messaging support... enabling AZ mode."));
			}

			// Ignore the handshake setting - wait for the AZHandshake to
			// indicate
			// support instead.
			this.ml_dht_enabled = false;

			Transport transport = connection.getTransport();
			boolean enable_padding = transport.isTCP() && transport.isEncrypted();
			connection.getIncomingMessageQueue().setDecoder(new AZMessageDecoder());
			connection.getOutgoingMessageQueue().setEncoder(new AZMessageEncoder(enable_padding));

			// We will wait until we get the Az handshake before considering the
			// connection
			// initialised.
			this.sendAZHandshake();
			handshake.destroy();
		} else if (messaging_mode == MESSAGING_LTEP) {
			if (Logger.isEnabled()) {
				Logger.log(new LogEvent(this, LOGID, "Enabling LT extension protocol support..."));
			}

			connection.getIncomingMessageQueue().setDecoder(new LTMessageDecoder());
			connection.getOutgoingMessageQueue().setEncoder(new LTMessageEncoder(this));

			generateFallbackSessionId();

			/**
			 * We don't need to wait for the LT handshake, nor do we require it,
			 * nor does it matter if the LT handshake comes later, nor does it
			 * matter if it we receive it repeatedly. So there - we can
			 * initialise the connection right now. :P
			 */
			this.initPostConnection(handshake);
			this.sendLTHandshake();
		} else {
			this.client = ClientIdentifier.identifyBTOnly(this.client_peer_id, this.handshake_reserved_bytes);

			connection.getIncomingMessageQueue().getDecoder().resumeDecoding();

			generateFallbackSessionId();
			this.initPostConnection(handshake);
		}

	}

	private int decideExtensionProtocol(BTHandshake handshake) {
		boolean supports_azmp = (handshake.getReserved()[0] & 128) == 128;
		boolean supports_ltep = (handshake.getReserved()[5] & 16) == 16;

		if (!supports_azmp) {
			if (supports_ltep) {
				if (!manager.isExtendedMessagingEnabled()) {
					if (Logger.isEnabled()) {
						Logger.log(new LogEvent(this, LOGID, "Ignoring peer's LT extension protocol support," + " as disabled for this download."));
					}
					return MESSAGING_BT_ONLY; // LTEP is supported, but
												// disabled.
				}
				return MESSAGING_LTEP; // LTEP is supported.
			}
			return MESSAGING_BT_ONLY; // LTEP isn't supported.
		}

		if (!supports_ltep) {

			// Check if it is AZMP enabled.
			if (!manager.isExtendedMessagingEnabled()) {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(this, LOGID, "Ignoring peer's extended AZ messaging support," + " as disabled for this download."));
				return MESSAGING_BT_ONLY;
			}

			// Check if the client is misbehaving...
			else if (client.indexOf("Plus!") != -1) {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(this, LOGID, "Handshake mistakingly indicates" + " extended AZ messaging support...ignoring."));
				return MESSAGING_BT_ONLY;
			}

			return MESSAGING_AZMP;
		}

		boolean enp_major_bit = (handshake.getReserved()[5] & 2) == 2;
		boolean enp_minor_bit = (handshake.getReserved()[5] & 1) == 1;

		// Only enable one of the blocks below.
		String their_ext_preference = ((enp_major_bit == enp_minor_bit) ? "Force " : "Prefer ") + ((enp_major_bit) ? "AZMP" : "LTEP");

		// Force AZMP block.
		String our_ext_preference = "Force AZMP";
		boolean use_azmp = enp_major_bit || enp_minor_bit; // Anything other
															// than Force LTEP,
															// then we force
															// AZMP to be used.
		boolean we_decide = use_azmp;

		// Prefer AZMP block (untested).
		/*
		 * String our_ext_preference = "Prefer AZMP"; boolean use_azmp =
		 * enp_major_bit; // Requires other client to prefer or force AZMP.
		 * boolean we_decide = use_azmp && !enp_minor_bit; // We decide only if
		 * we are using AZMP and the other client didn't force it.
		 */

		// Prefer LTEP block (untested).
		/*
		 * String our_ext_preference = "Prefer LTEP"; boolean use_azmp =
		 * enp_major_bit && enp_minor_bit; // Only use it Force AZMP is enabled.
		 * boolean we_decide = enp_minor_bit && !use_azmp; // We decide only if
		 * we are using LTEP and the other client didn't force it.
		 */

		if (Logger.isEnabled()) {
			String msg = "Peer supports both AZMP and LTEP: ";
			msg += "\"" + our_ext_preference + "\"" + (we_decide ? ">" : "<") + ((our_ext_preference.equals(their_ext_preference)) ? "= " : " ");
			msg += "\"" + their_ext_preference + "\" - using " + (use_azmp ? "AZMP" : "LTEP");
			Logger.log(new LogEvent(this, LOGID, msg));
		}

		return (use_azmp) ? MESSAGING_AZMP : MESSAGING_LTEP;

	}

	protected void decodeLTHandshake(LTHandshake handshake) {
		String lt_handshake_name = handshake.getClientName();
		if (lt_handshake_name != null) {
			this.client_handshake = StringInterner.intern(lt_handshake_name);
			this.client = StringInterner.intern(ClientIdentifier.identifyLTEP(this.client_peer_id, this.client_handshake, this.peer_id));
		}
		if (handshake.getTCPListeningPort() > 0) {
			// Only use crypto if it was specifically requested. Not sure what
			// the default
			// should be if they haven't indicated...
			Boolean crypto_requested = handshake.isCryptoRequested();
			byte handshake_type = (crypto_requested != null && crypto_requested.booleanValue()) ? PeerItemFactory.HANDSHAKE_TYPE_CRYPTO : PeerItemFactory.HANDSHAKE_TYPE_PLAIN;
			tcp_listen_port = handshake.getTCPListeningPort();
			peer_item_identity = PeerItemFactory.createPeerItem(ip, tcp_listen_port, PeerItem.convertSourceID(peer_source), handshake_type, udp_listen_port, // probably
																																								// none
					crypto_level, 0);
		}

		LTMessageEncoder encoder = (LTMessageEncoder) connection.getOutgoingMessageQueue().getEncoder();
		encoder.updateSupportedExtensions(handshake.getExtensionMapping());
		this.ut_pex_enabled = UTPeerExchange.ENABLED && encoder.supportsUTPEX();

		/**
		 * Grr... this is one thing which I'm sure I had figured out much better
		 * than it is here... Basically, we "initialise" the connection at the
		 * BT handshake stage, because the LT handshake is mandatory or required
		 * to come first (unlike the AZ one).
		 * 
		 * But when we receive an LT handshake, we have to "initialise" it like
		 * we did previously, because we may have to set the internals up to
		 * indicate if PEX is supported.
		 * 
		 * I'm not entirely sure this method is meant to be called more than
		 * once, and I'm less convinced that it's safe to do it repeatedly over
		 * the lifetime of a properly-initialised, actually-doing-stuff
		 * connection... but I'll worry about that later.
		 */
		this.doPostHandshakeProcessing();

		handshake.destroy();
	}

	protected void decodeAZHandshake(AZHandshake handshake) {
//		System.out.println("decoding handshake");

		this.client_handshake = StringInterner.intern(handshake.getClient());
		this.client_handshake_version = StringInterner.intern(handshake.getClientVersion());
		this.client = StringInterner.intern(ClientIdentifier.identifyAZMP(this.client_peer_id, client_handshake, client_handshake_version, this.peer_id));

		if (handshake.getTCPListenPort() > 0) { // use the ports given in
												// handshake
			tcp_listen_port = handshake.getTCPListenPort();
			udp_listen_port = handshake.getUDPListenPort();
			udp_non_data_port = handshake.getUDPNonDataListenPort();
			final byte type = handshake.getHandshakeType() == AZHandshake.HANDSHAKE_TYPE_CRYPTO ? PeerItemFactory.HANDSHAKE_TYPE_CRYPTO : PeerItemFactory.HANDSHAKE_TYPE_PLAIN;

			// remake the id using the peer's remote listen port instead of
			// their random local port
			peer_item_identity = PeerItemFactory.createPeerItem(ip, tcp_listen_port, PeerItem.convertSourceID(peer_source), type, udp_listen_port, crypto_level, 0);
		}

		if (handshake.getReconnectSessionID() != null) {
			if (Logger.isEnabled()) {
				Logger.log(new LogEvent(this, LOGID, LogEvent.LT_INFORMATION, "received reconnect request ID: " + handshake.getReconnectSessionID().toBase32String()));
			}
			checkForReconnect(handshake.getReconnectSessionID());
		}

		if (handshake.getRemoteSessionID() != null)
			peerSessionID = handshake.getRemoteSessionID();
		else
			generateFallbackSessionId();

		String[] supported_message_ids = handshake.getMessageIDs();
		byte[] supported_message_versions = handshake.getMessageVersions();

		// find mutually available message types
		final ArrayList messages = new ArrayList();

		for (int i = 0; i < handshake.getMessageIDs().length; i++) {
			Message msg = MessageManager.getSingleton().lookupMessage(supported_message_ids[i]);
			if (msg != null) { // mutual support!
				messages.add(msg);

				String id = msg.getID();
				byte supported_version = supported_message_versions[i];

				// we can use == safely
				if (id == BTMessage.ID_BT_BITFIELD)
					other_peer_bitfield_version = supported_version;
				else if (id == BTMessage.ID_BT_CANCEL)
					other_peer_cancel_version = supported_version;
				else if (id == BTMessage.ID_BT_CHOKE)
					other_peer_choke_version = supported_version;
				else if (id == BTMessage.ID_BT_HANDSHAKE)
					other_peer_handshake_version = supported_version;
				else if (id == BTMessage.ID_BT_HAVE)
					other_peer_bt_have_version = supported_version;
				else if (id == BTMessage.ID_BT_INTERESTED)
					other_peer_interested_version = supported_version;
				else if (id == BTMessage.ID_BT_KEEP_ALIVE)
					other_peer_keep_alive_version = supported_version;
				else if (id == BTMessage.ID_BT_PIECE)
					other_peer_piece_version = supported_version;
				else if (id == BTMessage.ID_BT_UNCHOKE)
					other_peer_unchoke_version = supported_version;
				else if (id == BTMessage.ID_BT_UNINTERESTED)
					other_peer_uninterested_version = supported_version;
				else if (id == BTMessage.ID_BT_REQUEST)
					other_peer_request_version = supported_version;
				else if (id == AZMessage.ID_AZ_PEER_EXCHANGE)
					other_peer_pex_version = supported_version;
				else if (id == AZMessage.ID_AZ_REQUEST_HINT)
					other_peer_az_request_hint_version = supported_version;
				else if (id == AZMessage.ID_AZ_HAVE)
					other_peer_az_have_version = supported_version;
				else if (id == AZMessage.ID_AZ_BAD_PIECE)
					other_peer_az_bad_piece_version = supported_version;
				else if (id == BTMessage.ID_BT_DHT_PORT)
					this.ml_dht_enabled = true;
				/***************************************************************
				 * OneSwarm messages
				 */
				else if (id == CertificateExchange.ID_OS_CERT_EXCHANGE)
					os_certificate_exchange = supported_version;
				else if (id == ReceiptRequests.ID_OS_RECEIPT_REQUESTS)
					os_receipt_requests = supported_version;
				else if (id == ReceiptBundle.ID_OS_RECEIPT_BUNDLE)
					os_receipt_bundle = supported_version;
				else if (id == Attestation.ID_OS_ATTESTATION)
					os_attestation = supported_version;
				else {
					// we expect unmatched ones here at the moment as we're not
					// dealing with them yet or they don't make sense.
					// for example AZVER
				}
			}
		}

		supported_messages = (Message[]) messages.toArray(new Message[messages.size()]);
		if (outgoing_piece_message_handler != null)
			outgoing_piece_message_handler.setPieceVersion(other_peer_piece_version);
		outgoing_have_message_aggregator.setHaveVersion(other_peer_bt_have_version, other_peer_az_have_version);

		mIsOneSwarm = messages.contains(MessageManager.getSingleton().lookupMessage(CertificateExchange.ID_OS_CERT_EXCHANGE)) && messages.contains(MessageManager.getSingleton().lookupMessage(ReceiptRequests.ID_OS_RECEIPT_REQUESTS)) && messages.contains(MessageManager.getSingleton().lookupMessage(ReceiptBundle.ID_OS_RECEIPT_BUNDLE)) && messages.contains(MessageManager.getSingleton().lookupMessage(Attestation.ID_OS_ATTESTATION));

		/**
		 * If this is a OneSwarm connection, exchange certficiate / topK data
		 */
		if (mIsOneSwarm) {
//			System.out.println("Got OneSwarm connection " + this);
			mReceiptDispatcher = new ReceiptDispatcher(this);
			mHandshake = handshake;
			sendIDCert();
		} else {
//			System.out.println("Connection is not OneSwarm: " + this);
			this.initPostConnection(handshake);
		}
	}

	private void sendIDCert() {
		System.out.println("Sending local identifying certificate to: " + this.getClient() + " / " + this.getIp());
		try {
			LocalTopK topK = ReputationDAO.get().get_topK_by_obs();
			PublicKey[] rand = null;
			PublicKey[] keys = ReputationDAO.get().get_frequently_observed();
			Collections.shuffle(Arrays.asList(keys));
			// TODO: magic constant
			rand = new PublicKey[Math.min(keys.length, 50)];
			System.arraycopy(keys, 0, rand, 0, rand.length);

			/**
			 * We only send a bloom filter if we aren't a seed (if we are -- we
			 * really don't care about whether the remote peer values us, only
			 * whether we value him)
			 */

			BloomFilter bf = topK.getBloomFilter();
			if (getControl().isSeeding())
				bf = LocalTopK.createTopK_BF(0);

			CertificateExchange certX = new CertificateExchange(LocalIdentity.get().getCertificate(), bf, rand, os_certificate_exchange);
			System.out.println("sending ID cert. topK: " + topK);

			connection.getOutgoingMessageQueue().addMessage(certX, false);
		} catch (IOException e) {
			closeConnectionInternally("Exception during certificate sending: " + e);
		}
	}

	private void initPostConnection(Message handshake) {
		changePeerState(PEPeer.TRANSFERING);
		connection_state = PEPeerTransport.CONNECTION_FULLY_ESTABLISHED;
		sendBitField();
		handshake.destroy();
		addAvailability();
		sendMainlineDHTPort();
	}

	protected void decodeBitfield(BTBitfield bitfield) {
		received_bitfield = true;

		final DirectByteBuffer field = bitfield.getBitfield();

		final byte[] dataf = new byte[(nbPieces + 7) / 8];

		if (field.remaining(DirectByteBuffer.SS_PEER) < dataf.length) {
			final String error = toString() + " has sent invalid Bitfield: too short [" + field.remaining(DirectByteBuffer.SS_PEER) + "<" + dataf.length + "]";
			Debug.out(error);
			if (Logger.isEnabled())
				Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, error));
			bitfield.destroy();
			return;
		}

		field.get(DirectByteBuffer.SS_PEER, dataf);

		try {
			closing_mon.enter();
			if (closing)
				bitfield.destroy();
			else {
				final BitFlags tempHavePieces;
				if (peerHavePieces == null) {
					tempHavePieces = new BitFlags(nbPieces);
				} else {
					tempHavePieces = peerHavePieces;
					removeAvailability();
				}
				for (int i = 0; i < nbPieces; i++) {
					final int index = i / 8;
					final int bit = 7 - (i % 8);
					final byte bData = dataf[index];
					final byte b = (byte) (bData >> bit);
					if ((b & 0x01) == 1) {
						tempHavePieces.set(i);
						manager.updateSuperSeedPiece(this, i);
					}
				}

				bitfield.destroy();

				peerHavePieces = tempHavePieces;
				addAvailability();

				checkSeed();
				checkInterested();
			}
		} finally {
			closing_mon.exit();
		}
	}

	protected void decodeMainlineDHTPort(BTDHTPort port) {
		int i_port = port.getDHTPort();
		port.destroy();

		if (!this.ml_dht_enabled) {
			return;
		}
		MainlineDHTProvider provider = getDHTProvider();
		if (provider == null) {
			return;
		}

		try {
			provider.notifyOfIncomingPort(getIp(), i_port);
		} catch (Throwable t) {
			Debug.printStackTrace(t);
		}
	}

	protected void decodeChoke(BTChoke choke) {
		choke.destroy();
		if (!choked_by_other_peer) {
			choked_by_other_peer = true;
			cancelRequests();
			final long unchoked = SystemTime.getCurrentTime() - unchokedTime;
			if (unchoked > 0 && !isSnubbed())
				unchokedTimeTotal += unchoked;
		}
	}

	protected void decodeUnchoke(BTUnchoke unchoke) {
		unchoke.destroy();
		if (choked_by_other_peer) {
			choked_by_other_peer = false;
			if (!isSnubbed())
				unchokedTime = SystemTime.getCurrentTime();
		}
	}

	protected void decodeInterested(BTInterested interested) {
		interested.destroy();

		// Don't allow known seeds to be interested in us

		other_peer_interested_in_me = !isSeed();

		if (other_peer_interested_in_me && fast_unchoke_new_peers && isChokedByMe() && getData("fast_unchoke_done") == null) {

			setData("fast_unchoke_done", "");

			sendUnChoke();
		}
	}

	protected void decodeUninterested(BTUninterested uninterested) {
		uninterested.destroy();
		other_peer_interested_in_me = false;

		// force send any pending haves in case one of them would make the other
		// peer interested again
		if (outgoing_have_message_aggregator != null) {
			outgoing_have_message_aggregator.forceSendOfPending();
		}

	}

	protected void decodeHave(BTHave have) {
		final int pieceNumber = have.getPieceNumber();
		have.destroy();

		if ((pieceNumber >= nbPieces) || (pieceNumber < 0)) {
			closeConnectionInternally("invalid pieceNumber: " + pieceNumber);
			return;
		}

		if (closing)
			return;

		if (peerHavePieces == null)
			peerHavePieces = new BitFlags(nbPieces);

		if (!peerHavePieces.flags[pieceNumber]) {
			if (!interested_in_other_peer && diskManager.isInteresting(pieceNumber)) {
				connection.getOutgoingMessageQueue().addMessage(new BTInterested(other_peer_interested_version), false);
				interested_in_other_peer = true;
			}
			peerHavePieces.set(pieceNumber);

			final int pieceLength = manager.getPieceLength(pieceNumber);
			manager.havePiece(pieceNumber, pieceLength, this);

			checkSeed(); // maybe a seed using lazy bitfield, or suddenly
							// became a seed;
			other_peer_interested_in_me &= !isSeed(); // never consider seeds
														// interested

			peer_stats.hasNewPiece(pieceLength);
		}
	}

	protected void decodeAZHave(AZHave have) {
		final int[] pieceNumbers = have.getPieceNumbers();

		have.destroy();

		if (closing) {

			return;
		}

		if (peerHavePieces == null) {

			peerHavePieces = new BitFlags(nbPieces);
		}

		boolean send_interested = false;
		boolean new_have = false;

		for (int i = 0; i < pieceNumbers.length; i++) {

			int pieceNumber = pieceNumbers[i];

			if ((pieceNumber >= nbPieces) || (pieceNumber < 0)) {

				closeConnectionInternally("invalid pieceNumber: " + pieceNumber);

				return;
			}

			if (!peerHavePieces.flags[pieceNumber]) {

				new_have = true;

				if (!(send_interested || interested_in_other_peer) && diskManager.isInteresting(pieceNumber)) {

					send_interested = true;
				}

				peerHavePieces.set(pieceNumber);

				final int pieceLength = manager.getPieceLength(pieceNumber);

				manager.havePiece(pieceNumber, pieceLength, this);

				peer_stats.hasNewPiece(pieceLength);
			}
		}

		if (new_have) {

			checkSeed(); // maybe a seed using lazy bitfield, or suddenly
							// became a seed;

			other_peer_interested_in_me &= !isSeed(); // never consider seeds
														// interested
		}

		if (send_interested) {

			connection.getOutgoingMessageQueue().addMessage(new BTInterested(other_peer_interested_version), false);

			interested_in_other_peer = true;
		}
	}

	protected long getBytesDownloaded() {
		if (peerHavePieces == null || peerHavePieces.flags.length == 0)
			return 0;

		final long total_done;

		if (peerHavePieces.flags[nbPieces - 1]) {

			total_done = ((long) (peerHavePieces.nbSet - 1) * diskManager.getPieceLength()) + diskManager.getPieceLength(nbPieces - 1);

		} else {

			total_done = (long) peerHavePieces.nbSet * diskManager.getPieceLength();
		}

		return (Math.min(total_done, diskManager.getTotalLength()));
	}

	public long getBytesRemaining() {
		return (diskManager.getTotalLength() - getBytesDownloaded());
	}

	public void sendBadPiece(int piece_number) {
		if (bad_piece_supported) {

			AZBadPiece bp = new AZBadPiece(piece_number, other_peer_az_bad_piece_version);

			connection.getOutgoingMessageQueue().addMessage(bp, false);
		}
	}

	protected void decodeAZBadPiece(AZBadPiece bad_piece) {
		final int piece_number = bad_piece.getPieceNumber();

		bad_piece.destroy();

		manager.badPieceReported(this, piece_number);
	}

	protected void decodeRequest(BTRequest request) {
		final int number = request.getPieceNumber();
		final int offset = request.getPieceOffset();
		final int length = request.getLength();
		request.destroy();

		if (!manager.validateReadRequest(this, number, offset, length)) {
			closeConnectionInternally("request for piece #" + number + ":" + offset + "->" + (offset + length - 1) + " is an invalid request");
			return;
		}

		if (manager.getHiddenPiece() == number) {
			closeConnectionInternally("request for piece #" + number + " is invalid as piece is hidden");
			return;
		}

		if (!choking_other_peer) {
			outgoing_piece_message_handler.addPieceRequest(number, offset, length);
			allowReconnect = true;
		} else {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(this, LOGID, "decodeRequest(): peer request for piece #" + number + ":" + offset + "->" + (offset + length - 1) + " ignored as peer is currently choked."));
		}
	}

	protected void decodePiece(BTPiece piece) {
		final int pieceNumber = piece.getPieceNumber();
		final int offset = piece.getPieceOffset();
		final DirectByteBuffer payload = piece.getPieceData();
		final int length = payload.remaining(DirectByteBuffer.SS_PEER);

		if (mReceiptDispatcher != null)
			mReceiptDispatcher.check(false);

		/*
		 * if ( AEDiagnostics.CHECK_DUMMY_FILE_DATA ){ int pos =
		 * payload.position( DirectByteBuffer.SS_PEER ); long off =
		 * ((long)number) * getControl().getPieceLength(0) + offset; for (int
		 * i=0;i<length;i++){ byte v = payload.get( DirectByteBuffer.SS_PEER );
		 * if ((byte)off != v ){ System.out.println( "piece: read is bad at " +
		 * off + ": expected = " + (byte)off + ", actual = " + v ); break; }
		 * off++; } payload.position( DirectByteBuffer.SS_PEER, pos ); }
		 */

		final Object error_msg = new Object() {
			public final String toString() {
				return ("decodePiece(): Peer has sent piece #" + pieceNumber + ":" + offset + "->" + (offset + length - 1) + ", ");
			}
		};

		if (!manager.validatePieceReply(this, pieceNumber, offset, payload)) {
			peer_stats.bytesDiscarded(length);
			manager.discarded(this, length);
			requests_discarded++;
			printRequestStats();
			piece.destroy();
			if (Logger.isEnabled())
				Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, error_msg + "but piece block discarded as invalid."));
			return;
		}

		final DiskManagerReadRequest request = manager.createDiskManagerRequest(pieceNumber, offset, length);
		boolean piece_error = true;

		if (hasBeenRequested(request)) { // from active request
			removeRequest(request);
			final long now = SystemTime.getCurrentTime();
			resetRequestsTime(now);

			if (manager.isWritten(pieceNumber, offset)) { // oops, looks like
															// this block has
															// already been
															// written
				peer_stats.bytesDiscarded(length);
				manager.discarded(this, length);

				if (manager.isInEndGameMode()) { // we're probably in
													// end-game mode then
					if (last_good_data_time != -1 && now - last_good_data_time <= 60 * 1000)
						setSnubbed(false);
					last_good_data_time = now;
					requests_discarded_endgame++;
					if (Logger.isEnabled())
						Logger.log(new LogEvent(this, LogIDs.PIECES, LogEvent.LT_INFORMATION, error_msg + "but piece block ignored as already written in end-game mode."));
				} else {
					// if they're not snubbed, then most likely this peer got a
					// re-request after some other peer
					// snubbed themselves, and the slow peer finially finished
					// the piece, but before this peer did
					// so give credit to this peer anyway for having delivered a
					// block at this time
					if (!isSnubbed())
						last_good_data_time = now;
					if (Logger.isEnabled())
						Logger.log(new LogEvent(this, LogIDs.PIECES, LogEvent.LT_WARNING, error_msg + "but piece block discarded as already written."));
					requests_discarded++;
				}

				printRequestStats();
			} else { // successfully received block!
				manager.writeBlock(pieceNumber, offset, payload, this, false);
				if (last_good_data_time != -1 && now - last_good_data_time <= 60 * 1000)
					setSnubbed(false);
				last_good_data_time = now;
				requests_completed++;
				piece_error = false; // dont destroy message, as we've passed
										// the payload on to the disk manager
										// for writing
			}
		} else { // initial request may have already expired, but check if we
					// can use the data anyway
			if (!manager.isWritten(pieceNumber, offset)) {
				final boolean ever_requested;

				try {
					recent_outgoing_requests_mon.enter();
					ever_requested = recent_outgoing_requests.containsKey(request);
				} finally {
					recent_outgoing_requests_mon.exit();
				}

				if (ever_requested) { // security-measure: we dont want to be
										// accepting any ol' random block
					manager.writeBlock(pieceNumber, offset, payload, this, true);
					final long now = SystemTime.getCurrentTime();
					if (last_good_data_time != -1 && now - last_good_data_time <= 60 * 1000)
						setSnubbed(false);
					resetRequestsTime(now);
					last_good_data_time = now;
					requests_recovered++;
					printRequestStats();
					piece_error = false; // dont destroy message, as we've
											// passed the payload on to the disk
											// manager for writing
					if (Logger.isEnabled())
						Logger.log(new LogEvent(this, LogIDs.PIECES, LogEvent.LT_INFORMATION, error_msg + "expired piece block data recovered as useful."));
				} else {

					System.out.println("[" + client + "]" + error_msg + "but expired piece block discarded as never requested.");

					peer_stats.bytesDiscarded(length);
					manager.discarded(this, length);
					requests_discarded++;
					printRequestStats();
					if (Logger.isEnabled())
						Logger.log(new LogEvent(this, LogIDs.PIECES, LogEvent.LT_ERROR, error_msg + "but expired piece block discarded as never requested."));
				}
			} else {
				peer_stats.bytesDiscarded(length);
				manager.discarded(this, length);
				requests_discarded++;
				printRequestStats();
				if (Logger.isEnabled())
					Logger.log(new LogEvent(this, LogIDs.PIECES, LogEvent.LT_WARNING, error_msg + "but expired piece block discarded as already written."));
			}
		}

		if (piece_error)
			piece.destroy();
		else
			allowReconnect = true;
	}

	protected void decodeCancel(BTCancel cancel) {
		int number = cancel.getPieceNumber();
		int offset = cancel.getPieceOffset();
		int length = cancel.getLength();
		cancel.destroy();
		if (outgoing_piece_message_handler != null)
			outgoing_piece_message_handler.removePieceRequest(number, offset, length);
	}

	private void registerForMessageHandling() {

		// INCOMING MESSAGES
		connection.getIncomingMessageQueue().registerQueueListener(new IncomingMessageQueue.MessageQueueListener() {
			public final boolean messageReceived(Message message) {

				//System.out.println("got msg: " + message + " " + PEPeerTransportProtocol.this.getIp());

				if (Logger.isEnabled())
					Logger.log(new LogEvent(PEPeerTransportProtocol.this, LogIDs.NET, "Received [" + message.getDescription() + "] message"));
				final long now = SystemTime.getCurrentTime();
				last_message_received_time = now;
				if (message.getType() == Message.TYPE_DATA_PAYLOAD) {
					last_data_message_received_time = now;
				}

				String message_id = message.getID();

				if (message_id.equals(BTMessage.ID_BT_PIECE)) {
					decodePiece((BTPiece) message);
					return true;
				}

				if (closing) {
					message.destroy();
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_KEEP_ALIVE)) {
					message.destroy();

					// make sure they're not spamming us
					if (!message_limiter.countIncomingMessage(message.getID(), 6, 60 * 1000)) { // allow
																								// max
																								// 6
																								// keep-alives
																								// per
																								// 60sec
						System.out.println(manager.getDisplayName() + ": Incoming keep-alive message flood detected, dropping spamming peer connection." + PEPeerTransportProtocol.this);
						closeConnectionInternally("Incoming keep-alive message flood detected, dropping spamming peer connection.");
					}

					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_HANDSHAKE)) {
					decodeBTHandshake((BTHandshake) message);
					return true;
				}

				if (message_id.equals(AZMessage.ID_AZ_HANDSHAKE)) {
					decodeAZHandshake((AZHandshake) message);
					return true;
				}

				if (message_id.equals(CertificateExchange.ID_OS_CERT_EXCHANGE)) {
					decodeCertificateExchange((CertificateExchange) message);
					return true;
				}

				if (message_id.equals(ReceiptRequests.ID_OS_RECEIPT_REQUESTS)) {
					decodeReceiptRequests((ReceiptRequests) message);
					return true;
				}

				if (message_id.equals(ReceiptBundle.ID_OS_RECEIPT_BUNDLE)) {
					decodeReceiptBundle((ReceiptBundle) message);
					return true;
				}

				if (message_id.equals(Attestation.ID_OS_ATTESTATION)) {
					decodeAttestation((Attestation) message);
					return true;
				}

				if (message_id.equals(LTMessage.ID_LT_HANDSHAKE)) {
					decodeLTHandshake((LTHandshake) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_BITFIELD)) {
					decodeBitfield((BTBitfield) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_CHOKE)) {
					decodeChoke((BTChoke) message);
					if (choking_other_peer) {
						connection.enableEnhancedMessageProcessing(false); // downgrade
																			// back
																			// to
																			// normal
																			// handler
					}
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_UNCHOKE)) {
					decodeUnchoke((BTUnchoke) message);
					connection.enableEnhancedMessageProcessing(true); // make
																		// sure
																		// we
																		// use a
																		// fast
																		// handler
																		// for
																		// the
																		// resulting
																		// download
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_INTERESTED)) {
					decodeInterested((BTInterested) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_UNINTERESTED)) {
					decodeUninterested((BTUninterested) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_HAVE)) {
					decodeHave((BTHave) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_REQUEST)) {
					decodeRequest((BTRequest) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_CANCEL)) {
					decodeCancel((BTCancel) message);
					return true;
				}

				if (message_id.equals(BTMessage.ID_BT_DHT_PORT)) {
					decodeMainlineDHTPort((BTDHTPort) message);
					return true;
				}

				if (message_id.equals(AZMessage.ID_AZ_PEER_EXCHANGE)) {
					decodePeerExchange((AZPeerExchange) message);
					return true;
				}

				if (message_id.equals(LTMessage.ID_UT_PEX)) {
					decodePeerExchange((UTPeerExchange) message);
					return true;
				}

				if (message_id.equals(AZMessage.ID_AZ_REQUEST_HINT)) {
					decodeAZRequestHint((AZRequestHint) message);
					return true;
				}

				if (message_id.equals(AZMessage.ID_AZ_HAVE)) {
					decodeAZHave((AZHave) message);
					return true;
				}

				if (message_id.equals(AZMessage.ID_AZ_BAD_PIECE)) {
					decodeAZBadPiece((AZBadPiece) message);
					return true;
				}
				return false;
			}

			public final void protocolBytesReceived(int byte_count) {
				// update stats
				peer_stats.protocolBytesReceived(byte_count);
				manager.protocolBytesReceived(PEPeerTransportProtocol.this, byte_count);
			}

			ReputationDAO rep = ReputationDAO.get();

			public final void dataBytesReceived(int byte_count) {
				// Observe that the peer is sending data so that if theyre so
				// slow that the whole
				// data block times out, we don't think theyre not sending
				// anything at all
				last_data_message_received_time = SystemTime.getCurrentTime();

				// update stats
				peer_stats.dataBytesReceived(byte_count);

				// PIAMOD -- record bytes received/sent in the DB
				if (isOneSwarm()) {
					try {
						Set<PublicKey> attrib = getAttribution();
						if (attrib == null)
							rep.received_direct(rep.get_internal_id(mCertificate.getPublicKey()), byte_count);
						else {
							for (PublicKey k : attrib)
								rep.local_recv_due_to_remote_reco(rep.get_internal_id(k), byte_count / attrib.size());
						}
					} catch (IOException e) {
						e.printStackTrace();
						closeConnectionInternally("accounting error during receipt: " + e);
					}
				}

				manager.dataBytesReceived(PEPeerTransportProtocol.this, byte_count);
			}
		});

		// OUTGOING MESSAGES
		connection.getOutgoingMessageQueue().registerQueueListener(new OutgoingMessageQueue.MessageQueueListener() {
			public final boolean messageAdded(Message message) {
				return true;
			}

			public final void messageQueued(Message message) { /* ignore */
			}

			public final void messageRemoved(Message message) { /* ignore */
			}

			public final void messageSent(Message message) {
				// update keep-alive info
				final long now = SystemTime.getCurrentTime();
				last_message_sent_time = now;

				if (message.getType() == Message.TYPE_DATA_PAYLOAD) {
					last_data_message_sent_time = now;
				}

				if (message.getID().equals(BTMessage.ID_BT_UNCHOKE)) { // is
																		// about
																		// to
																		// send
																		// piece
																		// data
					connection.enableEnhancedMessageProcessing(true); // so
																		// make
																		// sure
																		// we
																		// use a
																		// fast
																		// handler
				} else if (message.getID().equals(BTMessage.ID_BT_CHOKE)) { // is
																			// done
																			// sending
																			// piece
																			// data
					if (choked_by_other_peer) {
						connection.enableEnhancedMessageProcessing(false); // so
																			// downgrade
																			// back
																			// to
																			// normal
																			// handler
					}
				}

				if (Logger.isEnabled())
					Logger.log(new LogEvent(PEPeerTransportProtocol.this, LogIDs.NET, "Sent [" + message.getDescription() + "] message"));
			}

			public final void protocolBytesSent(int byte_count) {
				// update stats
				peer_stats.protocolBytesSent(byte_count);
				manager.protocolBytesSent(PEPeerTransportProtocol.this, byte_count);
			}

			ReputationDAO rep = ReputationDAO.get();

			public final void dataBytesSent(int byte_count) {
				// update stats
				peer_stats.dataBytesSent(byte_count);
				manager.dataBytesSent(PEPeerTransportProtocol.this, byte_count);

				// PIAMOD -- record bytes received/sent in the DB
				if (isOneSwarm()) {
					try {
						Set<PublicKey> attrib = getAttribution();
						if (attrib == null)
							rep.sent_direct(rep.get_internal_id(mCertificate.getPublicKey()), byte_count);
						else {
							for (PublicKey k : attrib)
								rep.local_sent_due_to_remote_reco(rep.get_internal_id(k), byte_count);
						}
					} catch (IOException e) {
						e.printStackTrace();
						closeConnectionInternally("accounting error during receipt: " + e);
					}
				}
			}

			public void flush() {
			}
		});

		// start message processing

		connection.addRateLimiter(manager.getUploadLimitedRateGroup(), true);
		connection.addRateLimiter(manager.getDownloadLimitedRateGroup(), false);

		connection.startMessageProcessing();
	}

	public void addRateLimiter(LimitedRateGroup limiter, boolean upload) {
		connection.addRateLimiter(limiter, upload);
	}

	public void removeRateLimiter(LimitedRateGroup limiter, boolean upload) {
		connection.removeRateLimiter(limiter, upload);
	}

	public Connection getPluginConnection() {
		return plugin_connection;
	}

	public Message[] getSupportedMessages() {
		return supported_messages;
	}

	public boolean supportsMessaging() {
		return supported_messages != null;
	}

	public int getMessagingMode() {
		return messaging_mode;
	}

	public byte[] getHandshakeReservedBytes() {
		return this.handshake_reserved_bytes;
	}

	public void setHaveAggregationEnabled(boolean enabled) {
		have_aggregation_disabled = !enabled;
	}

	public boolean hasReceivedBitField() {
		return (received_bitfield);
	}

	public String getEncryption() {
		Transport transport = connection.getTransport();

		if (transport == null) {

			return ("");
		}

		return (transport.getEncryption());
	}

	public void addListener(PEPeerListener listener) {
		try {
			peer_listeners_mon.enter();

			if (peer_listeners_cow == null) {

				peer_listeners_cow = new ArrayList();
			}

			final List new_listeners = new ArrayList(peer_listeners_cow);

			new_listeners.add(listener);

			peer_listeners_cow = new_listeners;

		} finally {

			peer_listeners_mon.exit();
		}
	}

	public void removeListener(PEPeerListener listener) {
		try {
			peer_listeners_mon.enter();

			if (peer_listeners_cow != null) {

				List new_listeners = new ArrayList(peer_listeners_cow);

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

	private void changePeerState(int new_state) {
		current_peer_state = new_state;

		if (current_peer_state == PEPeer.TRANSFERING) { // YUCK!
			doPostHandshakeProcessing();
		}

		final List peer_listeners_ref = peer_listeners_cow;

		if (peer_listeners_ref != null) {

			for (int i = 0; i < peer_listeners_ref.size(); i++) {

				final PEPeerListener l = (PEPeerListener) peer_listeners_ref.get(i);

				l.stateChanged(this, current_peer_state);
			}
		}
	}

	private void doPostHandshakeProcessing() {
		// peer exchange registration
		if (manager.isPeerExchangeEnabled()) {
			// try and register all connections for their peer exchange info
			peer_exchange_item = manager.createPeerExchangeConnection(this);

			if (peer_exchange_item != null) {
				// check for peer exchange support
				if (ut_pex_enabled || peerSupportsMessageType(AZMessage.ID_AZ_PEER_EXCHANGE)) {
					peer_exchange_supported = true;
				} else { // no need to maintain internal states as we wont be
							// sending/receiving peer exchange messages
					peer_exchange_item.disableStateMaintenance();
				}
			}
		}

		request_hint_supported = peerSupportsMessageType(AZMessage.ID_AZ_REQUEST_HINT);
		bad_piece_supported = peerSupportsMessageType(AZMessage.ID_AZ_BAD_PIECE);
	}

	private boolean peerSupportsMessageType(String message_id) {
		if (supported_messages != null) {
			for (int i = 0; i < supported_messages.length; i++) {
				if (supported_messages[i].getID().equals(message_id))
					return true;
			}
		}
		return false;
	}

	public void updatePeerExchange() {
		if (current_peer_state != TRANSFERING)
			return;
		if (!peer_exchange_supported)
			return;

		if (peer_exchange_item != null && manager.isPeerExchangeEnabled()) {
			final PeerItem[] adds = peer_exchange_item.getNewlyAddedPeerConnections();
			final PeerItem[] drops = peer_exchange_item.getNewlyDroppedPeerConnections();

			if ((adds != null && adds.length > 0) || (drops != null && drops.length > 0)) {
				if (ut_pex_enabled) {
					connection.getOutgoingMessageQueue().addMessage(new UTPeerExchange(adds, drops, (byte) 0), false);
				} else {
					connection.getOutgoingMessageQueue().addMessage(new AZPeerExchange(manager.getHash(), adds, drops, other_peer_pex_version), false);
				}
			}
		}
	}

	protected void decodePeerExchange(AZStylePeerExchange exchange) {

		final PeerItem[] added = exchange.getAddedPeers();
		final PeerItem[] dropped = exchange.getDroppedPeers();

		// make sure they're not spamming us
		if (!message_limiter.countIncomingMessage(exchange.getID(), 7, 120 * 1000)) { // allow
																						// max
																						// 7
																						// PEX
																						// per
																						// 2min
																						// //TODO
																						// reduce
																						// max
																						// after
																						// 2308
																						// release?
			System.out.println(manager.getDisplayName() + ": Incoming PEX message flood detected, dropping spamming peer connection." + PEPeerTransportProtocol.this);
			closeConnectionInternally("Incoming PEX message flood detected, dropping spamming peer connection.");
			return;
		}

		exchange.destroy();

		if ((added != null && added.length > exchange.getMaxAllowedPeersPerVolley(!this.has_received_initial_pex, true)) || (dropped != null && dropped.length > exchange.getMaxAllowedPeersPerVolley(!this.has_received_initial_pex, false))) {

			// drop these too-large messages as they seem to be used for DOS by
			// swarm poisoners
			closeConnectionInternally("Invalid PEX message received: too large, dropping likely poisoner peer connection.");
			return;
		}

		this.has_received_initial_pex = true;

		if (peer_exchange_supported && peer_exchange_item != null && manager.isPeerExchangeEnabled()) {
			if (added != null) {
				for (int i = 0; i < added.length; i++) {
					peer_exchange_item.addConnectedPeer(added[i]);
				}
			}

			if (dropped != null) {
				for (int i = 0; i < dropped.length; i++) {
					peer_exchange_item.dropConnectedPeer(dropped[i]);
				}
			}
		} else {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(this, LOGID, "Peer Exchange disabled for this download, " + "dropping received exchange message"));
		}
	}

	public boolean sendRequestHint(int piece_number, int offset, int length, int life) {
		if (request_hint_supported) {

			AZRequestHint rh = new AZRequestHint(piece_number, offset, length, life, other_peer_az_request_hint_version);

			connection.getOutgoingMessageQueue().addMessage(rh, false);

			return (true);

		} else {

			return (false);
		}
	}

	protected void decodeAZRequestHint(AZRequestHint hint) {
		int piece_number = hint.getPieceNumber();
		int offset = hint.getOffset();
		int length = hint.getLength();
		int life = hint.getLife();

		hint.destroy();

		if (life > REQUEST_HINT_MAX_LIFE) {

			life = REQUEST_HINT_MAX_LIFE;
		}

		if (manager.validateHintRequest(this, piece_number, offset, length)) {

			if (request_hint == null) {

				// we ignore life time currently as once hinted we don't accept
				// another hint
				// until that one is satisfied. This is to prevent too many
				// pieces starting

				request_hint = new int[] { piece_number, offset, length };
			}
		}
	}

	public int[] getRequestHint() {
		return (request_hint);
	}

	public void clearRequestHint() {
		request_hint = null;
	}

	public PeerItem getPeerItemIdentity() {
		return peer_item_identity;
	}

	public int getReservedPieceNumber() {
		return reservedPiece;
	}

	public void setReservedPieceNumber(int pieceNumber) {
		reservedPiece = pieceNumber;
	}

	public int getIncomingRequestCount() {
		if (outgoing_piece_message_handler == null) {
			return (0);
		}

		return outgoing_piece_message_handler.getRequestCount();
	}

	public int getOutgoingRequestCount() {
		return (getNbRequests());
	}

	public int getOutboundDataQueueSize() {
		return (connection.getOutgoingMessageQueue().getTotalSize());
	}

	public boolean isStalledPendingLoad() {
		if (outgoing_piece_message_handler == null) {

			return (false);
		}

		return outgoing_piece_message_handler.isStalledPendingLoad();
	}

	public int[] getIncomingRequestedPieceNumbers() {
		if (outgoing_piece_message_handler == null) {
			return (new int[0]);
		}
		return outgoing_piece_message_handler.getRequestedPieceNumbers();
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
					request = (DiskManagerReadRequest) requested.get(i);
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

	public int getPercentDoneOfCurrentIncomingRequest() {
		return (connection.getIncomingMessageQueue().getPercentDoneOfCurrentMessage());
	}

	public int getPercentDoneOfCurrentOutgoingRequest() {
		return (connection.getOutgoingMessageQueue().getPercentDoneOfCurrentMessage());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.core3.logging.LogRelation#getLogRelationText()
	 */
	public String getRelationText() {
		String text = "";
		if (manager instanceof LogRelation)
			text = ((LogRelation) manager).getRelationText() + "; ";
		text += "Peer: " + toString();
		return text;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.core3.logging.LogRelation#queryForClass(java.lang.Class)
	 */
	public Object[] getQueryableInterfaces() {
		return new Object[] { manager };
	}

	public int getLastPiece() {
		return _lastPiece;
	}

	public void setLastPiece(int pieceNumber) {
		_lastPiece = pieceNumber;
	}

	public boolean isLANLocal() {
		if (connection == null)
			return (AddressUtils.isLANLocalAddress(ip) == AddressUtils.LAN_LOCAL_YES);
		return connection.isLANLocal();
	}

	public boolean isTCP() {
		return (connection.getEndpoint().getProtocols()[0].getType() == ProtocolEndpoint.PROTOCOL_TCP);
	}

	public long getUnchokedTimeTotal() {
		if (choked_by_other_peer)
			return unchokedTimeTotal;
		return unchokedTimeTotal + (SystemTime.getCurrentTime() - unchokedTime);
	}

	public void setUploadRateLimitBytesPerSecond(int bytes) {
		connection.setUploadLimit(bytes);
	}

	public void setDownloadRateLimitBytesPerSecond(int bytes) {
		connection.setDownloadLimit(bytes);
	}

	public int getUploadRateLimitBytesPerSecond() {
		return connection.getUploadLimit();
	}

	public int getDownloadRateLimitBytesPerSecond() {
		return connection.getDownloadLimit();
	}

	public String getClientNameFromPeerID() {
		return this.client_peer_id;
	}

	public String getClientNameFromExtensionHandshake() {
		if (!this.client_handshake.equals("") && !this.client_handshake_version.equals("")) {
			return this.client_handshake + " " + this.client_handshake_version;
		}
		return this.client_handshake;
	}

	private static MainlineDHTProvider getDHTProvider() {
		return AzureusCoreImpl.getSingleton().getGlobalManager().getMainlineDHTProvider();
	}

	public void generateEvidence(IndentWriter writer) {
		writer.println("ip=" + getIp() + ",in=" + isIncoming() + ",port=" + getPort() + ",cli=" + client + ",tcp=" + getTCPListenPort() + ",udp=" + getUDPListenPort() + ",oudp=" + getUDPNonDataListenPort() + ",p_state=" + getPeerState() + ",c_state=" + getConnectionState() + ",seed=" + isSeed() + ",pex=" + peer_exchange_supported + ",closing=" + closing);
		writer.println("    choked=" + choked_by_other_peer + ",choking=" + choking_other_peer + ",unchoke_time=" + unchokedTime + ", unchoke_total=" + unchokedTimeTotal + ",is_opt=" + is_optimistic_unchoke);
		writer.println("    interested=" + interested_in_other_peer + ",interesting=" + other_peer_interested_in_me + ",snubbed=" + snubbed);
		writer.println("    lp=" + _lastPiece + ",up=" + uniquePiece + ",rp=" + reservedPiece);
		writer.println("    last_sent=" + last_message_sent_time + "/" + last_data_message_sent_time + ",last_recv=" + last_message_received_time + "/" + last_data_message_received_time + "/" + last_good_data_time);
		writer.println("    conn_at=" + connection_established_time + ",cons_no_reqs=" + consecutive_no_request_count + ",discard=" + requests_discarded + "/" + requests_discarded_endgame + ",recov=" + requests_recovered + ",comp=" + requests_completed);

	}

	protected static class MutableInteger {
		private int value;

		protected MutableInteger(int v) {
			value = v;
		}

		protected void setValue(int v) {
			value = v;
		}

		protected int getValue() {
			return (value);
		}

		public int hashCode() {
			return value;
		}

		public boolean equals(Object obj) {
			if (obj instanceof MutableInteger) {
				return value == ((MutableInteger) obj).value;
			}
			return false;
		}
	}

	public Attestation sendAttestation(long inDiff) {
		if (mIsOneSwarm == false) {
			System.err.println("Trying to send attestation to non-oneswarm connection, shouldn't happen");
			(new Exception()).printStackTrace();
			return null;
		}

		try {
			System.out.println("sending attestation, diff: " + inDiff);
			Attestation attest = new Attestation(this, (int) inDiff, os_attestation);
			connection.getOutgoingMessageQueue().addMessage(attest, false);
			return attest;
		} catch (IOException e) {
			e.printStackTrace();
			closeConnectionInternally("Couldn't send attestation: " + e.toString());
		}
		return null;
	}
}
