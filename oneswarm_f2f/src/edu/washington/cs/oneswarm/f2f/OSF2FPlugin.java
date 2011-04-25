package edu.washington.cs.oneswarm.f2f;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.net.magneturi.MagnetURIHandler;
import com.aelitis.net.magneturi.MagnetURIHandlerException;
import com.aelitis.net.magneturi.MagnetURIHandlerListener;
import com.aelitis.net.magneturi.MagnetURIHandlerProgressListener;

import edu.washington.cs.oneswarm.f2f.OSF2FNatChecker.NatCheckResult;
import edu.washington.cs.oneswarm.f2f.OSF2FNatChecker.NatCheckResult.Status;
import edu.washington.cs.oneswarm.f2f.OSF2FSpeedChecker.OutgoingSpeedCheck;
import edu.washington.cs.oneswarm.f2f.friends.FriendBean;
import edu.washington.cs.oneswarm.f2f.friends.FriendImportManager;
import edu.washington.cs.oneswarm.f2f.friends.LanFriendFinder;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FTextSearch;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection.OverlayForward;
import edu.washington.cs.oneswarm.f2f.network.OverlayEndpoint;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.network.OverlayTransport;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.plugins.PluginCallback;
import edu.washington.cs.publickey.PublicKeyFriend;

public class OSF2FPlugin implements Plugin {
	private final static boolean ADD_INFO_TO_AZ_SWT_UI = true;

	private static Logger logger = Logger.getLogger(OSF2FPlugin.class.getName());
	// public static boolean logToStdOut = false;
	private OSF2FMain main;

	private long mStartupTime = System.currentTimeMillis();

	class OneSwarmURIHandlerListener implements MagnetURIHandlerListener {

		private PluginInterface pluginInterface;

		public OneSwarmURIHandlerListener(PluginInterface pluginInterface) {
			this.pluginInterface = pluginInterface;
		}

		public byte[] badge() {
			return null; // we don't really need this
		}

		public boolean download(URL magnet_url) throws MagnetURIHandlerException {
			try {
				pluginInterface.getDownloadManager().addDownload(magnet_url, false);
				return (true);
			} catch (DownloadException e) {
				throw (new MagnetURIHandlerException("Operation failed", e));
			}
		}

		public byte[] download(final MagnetURIHandlerProgressListener progress, final byte[] hash, InetSocketAddress[] sources, long timeout) throws MagnetURIHandlerException {

			logger.fine("Got download request for: " + ByteFormatter.encodeString(hash) + " timeout: " + timeout + " sources has: " + sources.length);

			// no download if we've already got it
			try {
				Download dl = pluginInterface.getDownloadManager().getDownload(hash);
				if (dl != null) {
					Torrent torrent = dl.getTorrent();
					if (torrent != null) {
						return (torrent.writeToBEncodedData());
					}
				}
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}

			/**
			 * Issue a text search for the converted hash and poll for results
			 * until timeout expires.
			 */
			try {

				long started = System.currentTimeMillis();

				/**
				 * Special case -- we just started up. In which case, this will
				 * surely fail (since we have no connected friends). If we
				 * started a minute ago or less, first wait until we have one
				 * connected friend
				 */
				while (getOnlineFriends().size() == 0 && started + timeout > System.currentTimeMillis()) {
					logger.fine("Waiting for friend connection before issuing magnet search: " + (System.currentTimeMillis() - started));
					Thread.sleep(1000);
				}

				if (getOnlineFriends().size() == 0) {
					logger.warning("No online friends -- couldn't download magnet link.");
					return null;
				}

				int search_id = OSF2FPlugin.this.sendTextSearch("id:" + new String(Base64.encode(hash)));
				/**
				 * TODO: This needs to be rewritten to query multiple potential
				 * sources for metainfo. Unfortunately, all sources aren't
				 * exposed in the TextSearchResult structure right now, so we do
				 * the same thing as the rest of the code: use the first.
				 */
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				boolean requested = false;
				while (started + timeout > System.currentTimeMillis()) {
					List<TextSearchResult> results = OSF2FPlugin.this.getTextSearchResult(search_id);
					if (results.size() > 0 && !requested) {
						requested = true;
						final int channel_id = results.get(0).getFirstSeenChannelId();
						final int connection_id = results.get(0).getFirstSeenConnectionId();

						OSF2FPlugin.this.sendMetaInfoRequest(connection_id, channel_id, hash, 0, new PluginCallback<byte[]>() {
							public void dataRecieved(long count) {
								progress.reportActivity("Read: " + count);
								logger.finer("Read " + count);
							}

							public void errorOccured(String str) {
								progress.reportActivity("Error: " + str);
								logger.warning("Error occurred during metainfo download: " + connection_id + " / " + channel_id + " / " + str);
							}

							public void progressUpdate(int percentage) {
								progress.reportCompleteness(percentage);
								logger.finer("Progress updated: " + percentage);
							}

							public void requestCompleted(byte[] bytes) {
								try {
									out.write(bytes);
								} catch (IOException e) {
									e.printStackTrace();
								}
								logger.fine("Metainfo DL completed: " + bytes.length + " / " + (new String(Base64.encode(hash))));
							}
						});
					}
					if (out.size() > 0) {
						return out.toByteArray();
					}
					Thread.sleep(100);
				}
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
			return null;
		}

		public int get(String name, Map values) {
			return Integer.MIN_VALUE;
		}

		public boolean set(String name, Map values) {
			return false;
		}
	};

	public void initialize(final PluginInterface pluginInterface) throws PluginException {
		System.err.println("Loading friend-to-friend plugin");
		Log.setLogger(pluginInterface.getLogger());
		main = OSF2FMain.getSingelton();

		main.init(pluginInterface);

		if (ADD_INFO_TO_AZ_SWT_UI) {
			new OSF2FAzSwtUi(main).initialize(pluginInterface);
		}

		MagnetURIHandler.getSingleton().addListener(new OneSwarmURIHandlerListener(pluginInterface));
	}

	public Integer addFriend(Friend f) {
		return main.getFriendManager().addFriend(f);
	}

	public void stopTransfers() {
		main.getOverlayManager().closeAllConnections();
	}

	public void restartTransfers() {
		main.getOverlayManager().restartAllConnections();
	}

	public void connectToFriend(String publicKey) {
		Friend f = getFriend(publicKey);
		if (f != null) {
			main.getDHTConnector().connectToFriend(f);
		} else {
			Log.log(Log.LT_WARNING, "Friend not found: " + publicKey, true);
		}
	}

	public void sendChatToFriend(Friend friend, String inPlaintextMessage) {
		main.getOverlayManager().sendChatMessage(friend.getConnectionId(), inPlaintextMessage);
	}

	public Friend getFriend(String publicKey) {
		publicKey = publicKey.replaceAll("\\s+", "");
		Friend f = main.getFriendManager().getFriend(Base64.decode(publicKey));
		return f;
	}

	public Map<Integer, Friend> getOnlineFriends() {
		Map<Integer, Friend> friends = main.getOverlayManager().getConnectedFriends();

		// System.out.println("connected friends: " + friends.size());
		return friends;
	}

	public List<Friend> getFriends() {
		return Arrays.asList(main.getFriendManager().getFriends());
	}

	public void sendFileListRequest(int connectionId, long maxCacheAge, PluginCallback<FileList> callback) {
		// TODO might want to check if we have a previous list
		main.getOverlayManager().sendFileListRequest(connectionId, maxCacheAge, callback);
		// callback.requestCompleted(new byte[10]);
	}

	public void sendMetaInfoRequest(int connectionId, int channelId, byte[] infohash, int lengthHint, PluginCallback<byte[]> callback) {
		main.getOverlayManager().sendMetaInfoRequest(connectionId, channelId, infohash, lengthHint, callback);
		// callback.requestCompleted(new byte[10]);
	}

	public int sendTextSearch(String searchString) {
		return main.getOverlayManager().getSearchManager().sendTextSearch(searchString,null);
	}

	public List<TextSearchResult> getTextSearchResult(int searchId) {
		return main.getOverlayManager().getSearchManager().getSearchResult(searchId);
	}

	public String getMyPublicKey() {
		return new String(Base64.encode(main.getOverlayManager().getOwnPublicKey().getEncoded()));
	}

	public void setTorrentPrivacy(byte[] infohash, boolean publicNet, boolean f2fNet) {

		(new RuntimeException("This method is deprecated. Stop using it.")).printStackTrace();

		main.getF2DownloadManager().setTorrentPrivacy(infohash, publicNet, f2fNet);
	}

	public boolean isSharedWithFriends(byte[] infohash) {
		return main.getF2DownloadManager().isSharedWithFriends(infohash);
	}

	public boolean isSharedWithPublic(byte[] infohash) {
		return main.getF2DownloadManager().isSharedWithPublic(infohash);
	}

	@SuppressWarnings("unchecked")
	public Map<String, String>[] getTransferStats() {
		List<Map<String, String>> map = new LinkedList<Map<String, String>>();

		List<FriendConnection> friendConnections = main.getOverlayManager().getFriendConnections();

		// start by adding the transports, transports are ignored from now on
		// only forwards are returned in this query

		// for (FriendConnection fc : friendConnections) {
		// ConcurrentHashMap<Integer, OverlayTransport> trans =
		// fc.getOverlayTransports();
		// for (OverlayTransport t : trans.values()) {
		// HashMap<String, String> stats = new HashMap<String, String>();
		// stats.put("type", "transport");
		// stats.put("to", fc.getRemoteFriend().getNick());
		// stats.put("from", "Me");
		// stats.put("out", "" + t.getBytesOut());
		// stats.put("in", "" + t.getBytesIn());
		// stats.put("uid", "transport" +
		// ByteFormatter.encodeString(fc.getRemoteFriend().getPublicKey()));
		// map.add(stats);
		// }
		// }

		// add the forwards
		HashMap<Integer, OverlayForward> forwards = new HashMap<Integer, OverlayForward>();
		for (FriendConnection fc : friendConnections) {
			for (Integer channelId : fc.getOverlayForwards().keySet()) {
				OverlayForward f = fc.getOverlayForwards().get(channelId);
				// add the first forward
				if (!forwards.containsKey(channelId)) {
					forwards.put(channelId, f);
				} else {
					// this is the second, figure out in which direction the
					// forward is
					OverlayForward forward1 = forwards.get(channelId);
					OverlayForward forward2 = f;
					/*
					 * the forwarding rate is the sum of the rates, one in each
					 * direction
					 */
					long rate = forward1.getForwardingRate() + forward2.getForwardingRate();
					long total = forward1.getBytesForwarded() + forward2.getBytesForwarded();

					// this is for the ui, if the rate is close to 0, skip it
					// except when total forwarded is significant, then show it
					if (rate > 100 || total > 1024 * 1024) {
						HashMap<String, String> stats = new HashMap<String, String>();
						stats.put("id", "" + Integer.toHexString(channelId));
						stats.put("type", "forward");

						stats.put("rate", "" + rate);
						stats.put("total", "" + total);

						/*
						 * try to decode the content if possible, default to
						 * unknown
						 */
						stats.put("content", "unknown");
						OSF2FSearch sourceMessage = forward1.getSourceMessage();
						if (sourceMessage instanceof OSF2FTextSearch) {
							stats.put("content", ((OSF2FTextSearch) sourceMessage).getSearchString());
						} else if (sourceMessage instanceof OSF2FHashSearch) {
							FileListManager filelistManager = main.getOverlayManager().getFilelistManager();
							String torrentNameIfKnown = filelistManager.getTorrentNameFromInfoHashHash(((OSF2FHashSearch) sourceMessage).getInfohashhash());
							if (torrentNameIfKnown != null) {
								stats.put("content", torrentNameIfKnown);
							}
						}

						if (forward1.isSearcherSide()) {
							// this means that forward one sent the search,
							// forward1.remoteFriend() is the person from which
							// the data is coming
							stats.put("from", forward1.getRemoteFriend().getNick());
							stats.put("to", forward2.getRemoteFriend().getNick());

							stats.put("uid", "fwd" + forward1.getRemoteFriend().getPublicKey() + "->" + forward2.getRemoteFriend().getPublicKey());
						} else {
							stats.put("to", forward1.getRemoteFriend().getNick());
							stats.put("from", forward2.getRemoteFriend().getNick());

							stats.put("uid", "fwd" + forward2.getRemoteFriend().getPublicKey() + "->" + forward1.getRemoteFriend().getPublicKey());
						}
						map.add(stats);
					}
				}
			}
		}

		return map.toArray(new HashMap[map.size()]);
	}

	public String[] getSupportedXMPPNetworks() {
		return FriendImportManager.getXmppNetworks();
	}

	public List<Friend> getNewUsersFromXMPP(String xmppNetworkName, String username, char[] password, String machineName) throws Exception {

		List<byte[]> knownKeys = main.getFriendManager().getKnownKeysForFriendImport();
		List<PublicKeyFriend> importedFriends = FriendImportManager.importXMPPFriends(knownKeys, xmppNetworkName, username, password, machineName);
		if (importedFriends == null) {
			return new LinkedList<Friend>();
		}

		List<Friend> newFriends = main.getFriendManager().convertToFriendArrayAndFilter(importedFriends);
		return newFriends;

	}
	
	public void disconnectFriend(Friend inFriend) {
		main.getOverlayManager().disconnectFriend(inFriend);
	}

	public void setFriendSettings(String publicKey, String nickname, boolean blocked, boolean canSeeFileList, boolean allowChat, boolean requestFileList, String group) {
		Friend f = getFriend(publicKey);
		if (f != null) {
			boolean triggerLookup = false;
			if (f.isBlocked() && !blocked) {
				triggerLookup = true;
			}

			boolean sendFileList = false;
			if (f.isCanSeeFileList() != canSeeFileList) {
				sendFileList = true;
			}

			String oldNick = f.getNick();

			Log.log("updating: " + nickname, true);
			f.setNick(nickname);
			f.setBlocked(blocked);
			f.setCanSeeFileList(canSeeFileList);
			f.setRequestFileList(requestFileList);
			f.setAllowChat(allowChat);
			f.setNewFriend(false);
			f.setGroup(group);
			System.out.println("setting group: " + group);
			if (f.getDateAdded() == null) {
				f.setDateAdded(new Date());
			} else if (f.getDateAdded().equals(new Date(0))) {
				f.setDateAdded(new Date());
			}
			if (blocked) {
				// disconnect all connections to the friend
				disconnectFriend(f);
			}
			main.getFriendManager().flushToDisk(false, true, false);
			if (triggerLookup) {
				main.getDHTConnector().connectToFriend(f);
				try {
					main.getDHTConnector().publishLocationInfoForFriend(f);
				} catch (Exception e) {
				}
			}

			/**
			 * PIAMOD -- syncing with the groups code here (we may have changed
			 * the name)
			 */
			// PermissionsDAO.get().refresh_friend_groups();
			try {
				PermissionsDAO.get().renameGroup(PermissionsDAO.get().getUserGroup(new String(Base64.encode(f.getPublicKey()))).getGroupID(), f.getNick());
			} catch (IOException e) {
				logger.warning("Group rename failed: " + e.toString());
				e.printStackTrace();
			}

			if (f.getStatus() == Friend.STATUS_ONLINE && sendFileList) {
				main.getOverlayManager().triggerFileListUpdates();
			}
		} else {
			Log.log(Log.LT_WARNING, "Friend not found: " + publicKey, true);
		}
	}

	public List<Integer> getAndClearTextSearchStats() {
		return main.getOverlayManager().getSearchManager().getAndClearTextSearchStats();
	}

	public List<Integer> getAndClearHashSearchStats() {
		return main.getOverlayManager().getSearchManager().getAndClearHashSearchStats();
	}

	public int getAndClearForwardedSearchNum() {
		return main.getOverlayManager().getSearchManager().getAndClearForwardedSearchNum();
	}

	public Map<Friend, FileList> getOnlineFileLists() {
		Map<Friend, FileList> onlineFileLists = new HashMap<Friend, FileList>();
		List<Friend> friends = new LinkedList<Friend>(getOnlineFriends().values());

		for (Friend friend : friends) {
			FileList fl = main.getOverlayManager().getFilelistManager().getFriendsList(friend);
			onlineFileLists.put(friend, fl);
		}
		return onlineFileLists;
	}

	public void registerForFriendConnectNotifications(FriendConnectListener callback) {
		main.getOverlayManager().registerForConnectNotifications(callback);
	}

	public List<Friend> getLanOneSwarmUsers() {
		LanFriendFinder lanFriendFinder = main.getLanFriendFinder();
		if (lanFriendFinder != null) {
			return lanFriendFinder.getNearbyUsers();
		}
		return new LinkedList<Friend>();
	}

	public void refreshFileLists() {
		main.getOverlayManager().getFilelistManager().scheduleFileListRefresh();
	}

	public Map<String, Integer> getNewFriendsCountsFromAutoCheck() {
		Map<String, Integer> newFriendsCountsFromAutoCheck = main.getFriendManager().getNewFriendsCountsFromAutoCheck();
		List<Friend> lanOneSwarmUsers = getLanOneSwarmUsers();
		List<Friend> lanUsers = main.getFriendManager().filterKnownFriends(lanOneSwarmUsers);

		/*
		 * only show lan users as requests if they have attempted to connect
		 */
		int num = 0;
		for (Friend lanUser : lanUsers) {
			if (main.getFriendManager().hasTriedToConnect(lanUser)) {
				if (!main.getFriendManager().isOnIgnoreRequestList(new String(Base64.encode(lanUser.getPublicKey())))) {
					num++;
				} else {
					logger.fine("not adding friend, is on ignore request list");
				}
			} else {
				// logger.finest("not adding friends, has not tried to connect");
			}
		}
		newFriendsCountsFromAutoCheck.put("Lan", num);
		return newFriendsCountsFromAutoCheck;
	}

	public Map<byte[], String> getDeniedIncomingConnections() {
		return main.getFriendManager().getDeniedIncomingConnections();
	}

	public void deleteFriend(String publicKey) {
		Friend f = getFriend(publicKey);
		if (f != null) {
			/*
			 * remove the friend and disconnect any existing connections
			 */
			main.getFriendManager().removeFriend(f.getPublicKey());
			main.getOverlayManager().disconnectFriend(f);
		} else {
			Log.log(Log.LT_WARNING, "Friend not found: " + publicKey, true);
		}
	}

	public void addToIgnoreRequestList(String publicKey) {
		main.getFriendManager().addToIgnoreRequestList(publicKey);
	}

	public String getGtalkStatus() {
		return FriendImportManager.getGtalkStatus();
	}

	public FriendInvitation createInvitation(String name, boolean canSeeFileList, long maxAge, byte securityLevel) {
		return main.getAuthManager().createInvitation(name, canSeeFileList, maxAge, securityLevel);
	}

	public void redeemInvitation(FriendInvitation invitation, boolean testOnly) throws Exception {
		main.redeemInvitation(invitation, testOnly);
	}

	public void updateInvitation(FriendInvitation invitation) {
		main.getAuthManager().updateInvitation(invitation);
	}

	public void deleteInvitation(FriendInvitation invitation) {
		main.getAuthManager().deleteInvitation(invitation);
	}

	public FriendInvitation getInvitation(HashWrapper key) {
		return main.getAuthManager().getInvitation(key);
	}

	public List<FriendInvitation> getLocallyCreatedInvitations() {
		return main.getAuthManager().getLocallyCreatedInvitations();
	}

	public List<FriendInvitation> getRedeemedInvitations() {
		return main.getAuthManager().getRedeemedInvitations();
	}

	public String getDebugInfo() {

		StringBuilder b = new StringBuilder();
		long msSinceCheck = System.currentTimeMillis() - main.getOverlayManager().getLastConnectionCheckRun();
		b.append("Last connection check run: " + (msSinceCheck / 1000) + "s ago\n");
		b.append("Queue:\n");
		b.append(main.getOverlayManager().getQueueManager().getDebug() + "\n");

		List<FriendConnection> friendConnections = main.getOverlayManager().getFriendConnections();

		int totalForwards = 0;
		int totalTransports = 0;
		for (FriendConnection f : friendConnections) {
			totalForwards += f.getOverlayForwards().size();
			totalTransports += f.getOverlayTransports().size();
		}
		b.append("Total forwards: " + totalForwards + "\n");
		b.append("Total transports: " + totalTransports + "\n\n");
		b.append("Friend Connections Summary:\n");
		for (FriendConnection f : friendConnections) {

			b.append(f.getRemoteFriend().getNick() + " (" + f.getRemoteIp() + ") sendQueueBytes=" + f.getTotalOutgoingQueueLengthBytes() + " lastSent=" + f.getLastMessageSentTime() + " lastReci=" + f.getLastMessageRecvTime() + " " + "imageMetaQueue=" + f.getImageMetaInfoQueueSize() + " " + "torrentMetaQeue=" + f.getTorrentMetaInfoQueueSize() + "\n");
			b.append(f.getQueueDebug());
		}

		b.append("\nFriend Connections Details:\n");
		for (FriendConnection f : friendConnections) {
			b.append(f.getRemoteFriend().getNick() + " (" + f.getRemoteIp() + ") sendQueueBytes=" + f.getTotalOutgoingQueueLengthBytes() + " lastSent=" + f.getLastMessageSentTime() + " lastReci=" + f.getLastMessageRecvTime() + "\n");
			b.append(f.getQueueDebug());
			Map<Integer, OverlayForward> overlayForwards = f.getOverlayForwards();
			if (overlayForwards.size() > 0) {
				b.append("   Forwards:\n");
			}
			for (OverlayForward of : overlayForwards.values()) {
				b.append("      channel=" + Integer.toHexString(of.getChannelId()) + " " + of.getRemoteFriend().getNick() + " lastSent=" + of.getLastMsgTime() + " src=" + of.getSourceMessage().getDescription() + "\n");
			}
			Collection<OverlayEndpoint> transports = f.getOverlayTransports().values();
			if (transports.size() > 0) {
				b.append("   Transports: \n");
			}
			for (OverlayEndpoint ot : transports) {
				b.append("      channel=" + Integer.toHexString(ot.getChannelId()) + " path=" + Integer.toHexString(ot.getPathID()) + " lastSent=" + ot.getLastMsgTime() + "\n");
			}
		}

		b.append("\n\n");

		List<String> sentSearches = main.getOverlayManager().getSearchManager().debugSentSearches();
		if (sentSearches.size() > 0) {
			b.append("Sent Searches:\n");
			for (String string : sentSearches) {
				b.append("   " + string + "\n");
			}
			b.append("\n");
		}

		List<String> forwardedSearches = main.getOverlayManager().getSearchManager().debugForwardedSearches();
		if (forwardedSearches.size() > 0) {
			b.append("Forwarded Searches:\n");
			for (String string : forwardedSearches) {
				b.append("   " + string + "\n");
			}
			b.append("\n");
		}

		List<String> canceledSearches = main.getOverlayManager().getSearchManager().debugCanceledSearches();
		if (canceledSearches.size() > 0) {
			b.append("Canceled searches:\n");
			for (String c : canceledSearches) {
				b.append("   " + c + "\n");
			}
			b.append("\n");
		}
		return b.toString();
	}

	public String getDebugMessageLog(String friendPublicKey) {
		List<FriendConnection> friendConnections = main.getOverlayManager().getFriendConnections();
		for (FriendConnection f : friendConnections) {
			if (Arrays.equals(f.getRemotePublicKey(), Base64.decode(friendPublicKey))) {
				return f.getDebugMessageLog();
			}
		}

		return "friend not online";
	}

	public String getSearchDebugLog() {
		StringBuilder b = new StringBuilder();
		FileListManager fm = main.getOverlayManager().getFilelistManager();
		long searchesTotal = fm.getSearchesTotal();
		long searchCacheHits = fm.getSearchCacheHits();
		long percent = 0;
		if (searchesTotal > 0) {
			percent = (100 * searchCacheHits) / searchesTotal;
		}
		b.append("search cache: \n   total_searches=" + searchesTotal + "   cache_hits=" + searchCacheHits + "(" + percent + "%)\n\n");
		b.append(main.getOverlayManager().getSearchManager().getSearchDebug());
		return b.toString();
	}

	public Friend[] scanXMLForFriends(String xml) {
		try {
			XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new ByteArrayInputStream(xml.getBytes())), this, null, main.getClassLoader());
			FriendBean[] o = (FriendBean[]) decoder.readObject();
			Friend[] conv = new Friend[o.length];

			int i = 0;
			for (FriendBean extracted : o) {
				conv[i++] = main.getFriendManager().getFriend(extracted);
			}
			decoder.close();
			return conv;
		} catch (Exception e) {
			System.err.println("error scanning XML for friends: " + e.toString());
			return null;
		}
	}

	public String getLockDebug() {
		return OverlayManager.lock.getLockDebug();
	}

	public String getForwardQueueLengthDebug() {
		return main.getOverlayManager().getQueueManager().getForwardQueueLengthDebug();
	}

	public int performSpeedCheck() {
		return main.getSpeedChecker().performSpeedCheck();
	}

	public void cancelSpeedCheck(int testId) {
		OutgoingSpeedCheck check = main.getSpeedChecker().getSpeedCheck(testId);
		if (check != null) {
			check.close();
		}
	}

	public HashMap<String, Double> getSpeedCheckResult(int checkId) {
		OutgoingSpeedCheck speedCheck = main.getSpeedChecker().getSpeedCheck(checkId);
		if (speedCheck == null) {
			return null;
		}
		HashMap<String, Double> result = new HashMap<String, Double>();
		result.put("progress", speedCheck.getProgress());
		result.put("local", 1.0 * speedCheck.getLocalEstimate());
		result.put("remote", 1.0 * speedCheck.getRemoteEstimate());
		result.put("completed", speedCheck.isCompleted() ? 1.0 : 0.0);
		result.put("closed", speedCheck.isClosed() ? 1.0 : 0.0);
		result.put("good_servers", speedCheck.getGoodServers() * 1.0);
		result.put("total_servers", speedCheck.getServerCount() * 1.0);
		return result;
	}

	public void triggerNATCheck() {
		main.getNatChecker().triggerNatCheck();
	}

	public HashMap<String, String> getNatCheckResult() {
		NatCheckResult res = main.getNatChecker().getResult();

		HashMap<String, String> map = new HashMap<String, String>();
		if (res == null) {
			return map;
		}
		map.put("status", "" + res.getStatus().getCode());
		if (res.getStatus() == Status.SUCCESS) {
			map.put("ip", res.ip);
			map.put("port", "" + res.port);
		}
		return map;
	}
}
