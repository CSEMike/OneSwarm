package edu.washington.cs.oneswarm.planetlab;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.ui.UIFunctionsManager;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.TextSearchResult;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.plugins.PluginCallback;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.server.FriendInfoLiteFactory;

public class CoordinatorExecutor extends Thread {

	private static Logger logger = Logger.getLogger(CoordinatorHeartbeatThread.class.getName());

	List<String> commands = new ArrayList<String>();

	private CoreInterface coreInterface;

	private AzureusCore azCore;

	private final CoordinatorHeartbeatThread parent;

	public CoordinatorExecutor( CoordinatorHeartbeatThread parent, byte [] commands ) throws IOException {
		this.parent = parent;
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(commands)));
		while( in.ready() ) {
			String line = in.readLine();
			if( line != null ) {
				this.commands.add(line);
			}
		}
	}

	public CoordinatorExecutor( CoordinatorHeartbeatThread parent, String [] inCommands ) {
		this.parent = parent;
		for( String s : inCommands ) {
			commands.add(s);
		}
	}

	@Override
	public void run() {

		coreInterface = ExperimentConfigManager.get().getCoreInterface();
		azCore = AzureusCoreImpl.getSingleton();

		try {
			for( String s : commands ) {

				if( s.length() == 0 ) {
					continue;
				}

				String [] toks = s.split("\\s+");

				logger.info("Processing command: " + s + " / " + toks[0]);

				if( toks[0].equals("ok") ) {
					;
				}
				else if (toks[0].equals("setprop")) {
					System.setProperty(toks[1], toks[2]);
				}
				else if( toks[0].equals("shutdown") ) {
					while( UIFunctionsManager.getUIFunctions() == null ) {
						logger.warning("Waiting for non-null UI functions...");
						Thread.sleep(1000);
					}

					if( UIFunctionsManager.getUIFunctions().requestShutdown() == false ) {
						System.err.println("Requested shutdown failed, so we are halting forcefully.");
						Runtime.getRuntime().halt(-1);
					}
				} else if( toks[0].equals("addkey") ) {

					boolean allowChat = false;
					boolean shareFileList = false;

					if (toks.length > 3) {
						allowChat = Boolean.parseBoolean(toks[3]);
					}
					if (toks.length > 4) {
						shareFileList = Boolean.parseBoolean(toks[4]);
					}

					addFriend(toks[1], toks[2], allowChat, shareFileList);

				} else if( toks[0].equals("removekey") ) {
					Friend toDel = coreInterface.getF2FInterface().getFriend(toks[1]);
					if( toDel != null ) {
						coreInterface.getF2FInterface().deleteFriend(FriendInfoLiteFactory.createFriendInfo(toDel));
					} else {
						logger.warning("couldn't find friend to delete for key: " + toks[1]);
					}
				} else if( toks[0].equals("blockgroup") ) {
					for( FriendInfoLite flite : coreInterface.getF2FInterface().getFriends(true, true) ) {
						Friend f = coreInterface.getF2FInterface().getFriend(flite.getPublicKey());
						if( f.getGroup().equals(toks[1]) ) {
							f.setBlocked(true);
						}
					}
				} else if( toks[0].equals("removegroup") ) {
					for( FriendInfoLite flite : coreInterface.getF2FInterface().getFriends(true, true) ) {
						Friend f = coreInterface.getF2FInterface().getFriend(flite.getPublicKey());
						if( f.getGroup().equals(toks[1]) ) {
							coreInterface.getF2FInterface().deleteFriend(FriendInfoLiteFactory.createFriendInfo(f));
						}
					}
				} else if( toks[0].equals("forceall") ) {
					for( FriendInfoLite f : coreInterface.getF2FInterface().getFriends(true, false) ) {
						coreInterface.getF2FInterface().connectToFriend(f.getPublicKey());
					}
					// wait some time for connections to come up
					Thread.sleep(10*1000);
				} else if( toks[0].equals("blockkey") ) {
					Friend blockFriend = getFriendForKey(toks[1]);
					if( blockFriend != null ) {
						blockFriend.setBlocked(true);
						if( blockFriend.isConnected() ) {
							coreInterface.getF2FInterface().disconnectFriend(blockFriend);
						}
					} else {
						logger.warning("tried to block key not in friend list: " + toks[1]);
					}
				} else if( toks[0].equals("unblockgroup") ) {
					for( FriendInfoLite flite : coreInterface.getF2FInterface().getFriends(true, true) ) {
						Friend f = coreInterface.getF2FInterface().getFriend(flite.getPublicKey());
						if( f.getGroup().equals(toks[1]) ) {
							f.setBlocked(false);
						}
					}
				} else if( toks[0].equals("unblockkey" ) ) {
					getFriendForKey(toks[1]).setBlocked(false);
				} else if( toks[0].equals("waitconnected" ) ) {
					long start = System.currentTimeMillis();
					FriendInfoLite [] online;
					boolean done = false;
					do {
						online = coreInterface.getF2FInterface().getFriends(false, false);
						if( toks.length > 1 ) {
							for( FriendInfoLite f : online ) {
								if( f.getPublicKey().equals(toks[1]) ) {
									done = true;
								}
							}
						} else {
							done = online.length > 0;
						}
						Thread.sleep(1000);
					} while( !done );

					logger.info("Waited " + (System.currentTimeMillis()-start) + " for connected");
				} else if( toks[0].equals("blockall") ) {
					for( FriendInfoLite flite : coreInterface.getF2FInterface().getFriends(true, true) ) {
						Friend f = coreInterface.getF2FInterface().getFriend(flite.getPublicKey());
						if( f.isBlocked() == false ) {
							f.setBlocked(true);
						}
					}
					coreInterface.getF2FInterface().stopTransfers();
					coreInterface.getF2FInterface().restartTransfers();
				} else if( toks[0].equals("blockrandom") ) {
					int target = 0;
					try {
						target = Integer.parseInt(toks[1]);
					} catch( Exception e ) {
						e.printStackTrace();
						continue;
					}
					if( target <= 0 ) {
						logger.warning("Can't specify blockrandom with target remaining <=0");
						continue;
					}
					FriendInfoLite[] allFriends = coreInterface.getF2FInterface().getFriends(true, false);
					FriendInfoLite[] onlineFriends = coreInterface.getF2FInterface().getFriends(false, false);

					Collections.shuffle(Arrays.asList(allFriends));
					int onlineToBlock = onlineFriends.length - target;
					int onlineBlocked = 0, stillOnline = 0;
					logger.info("Total friends: " + allFriends.length + " onlineToBlock: " + onlineToBlock + " online: " + onlineFriends.length );
					for( FriendInfoLite flite : allFriends ) {

						Friend f = coreInterface.getF2FInterface().getFriend(flite.getPublicKey());
						if( f.isConnected() == false ) {
							logger.info("blocking disconnected user: " + f.getNick());
							f.setBlocked(true);
							coreInterface.getF2FInterface().disconnectFriend(f);
						} else {
							if( onlineBlocked < onlineToBlock ) {
								logger.info("blocking online user: " + f.getNick());
								f.setBlocked(true);
								coreInterface.getF2FInterface().disconnectFriend(f);
								onlineBlocked++;
							} else {
								logger.info("Skipping block of online user: " + f.getNick() + " (over required amount)");
								stillOnline++;
							}
						}
					}
					logger.info("finished, stillOnline="+stillOnline);
//					coreInterface.getF2FInterface().stopTransfers();
//					coreInterface.getF2FInterface().restartTransfers();
				} else if( toks[0].equals("unblockall" ) ) {
					for( FriendInfoLite flite : coreInterface.getF2FInterface().getFriends(true, true) ) {
						Friend f = coreInterface.getF2FInterface().getFriend(flite.getPublicKey());
						if( f.isBlocked() == true ) {
							f.setBlocked(false);
						}
					}
//					coreInterface.getF2FInterface().stopTransfers();
//					coreInterface.getF2FInterface().restartTransfers();
				} else if( toks[0].equals("download") ) {
					long when = 0;
					if( toks.length >= 3 ) {
						when = Long.parseLong(toks[2]);
					}
					downloadAndStart(toks[1], when);
				} else if( toks[0].equals("cleardls") ) {
					for( DownloadManager dm : (List<DownloadManager>)azCore.getGlobalManager().getDownloadManagers() ) {
						azCore.getGlobalManager().removeDownloadManager(dm, true, true);
					}
				} else if( toks[0].equals("share") ) {
					if( toks[2].startsWith("http") ) {
						downloadAndShare(toks[1], toks[2]);
					} else {
						createRandomAndShare(toks[1], Long.parseLong(toks[2]));
					}
				} else if( toks[0].equals("btshare") ) {
					long at = 0;
					int maxul_bytes = 0;
					if( toks.length >= 3 ) {
						at = Long.parseLong(toks[2]);
					}
					if( toks.length >= 4 ) {
						maxul_bytes = Integer.parseInt(toks[3]);
					}
					ArrayList<GroupBean> converted = new ArrayList<GroupBean>();
					converted.add(GroupBean.PUBLIC);
					downloadTorrentAndStart(toks[1], at, converted, maxul_bytes);
				} else if( toks[0].equals("f2fshare") ) {
					long at = 0;
					int maxul_bytes =0;
					if( toks.length >= 3 ) {
						at = Long.parseLong(toks[2]);
					}
					if( toks.length >= 4 ) {
						maxul_bytes = Integer.parseInt(toks[3]);
					}
					ArrayList<GroupBean> converted = new ArrayList<GroupBean>();
					converted.add(GroupBean.ALL_FRIENDS);
					downloadTorrentAndStart(toks[1], at, converted, maxul_bytes);
				} else if( toks[0].equals("booleanSetting") ) {
					ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "), Boolean.parseBoolean(toks[2]));
					ConfigurationManager.getInstance().setDirty();
				} else if( toks[0].equals("intSetting" ) ) {
					ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "), Integer.parseInt(toks[2]));
					ConfigurationManager.getInstance().setDirty();
				} else if( toks[0].equals("stringSetting" ) ) {
					ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "), toks[2]);
					ConfigurationManager.getInstance().setDirty();
				} else if( toks[0].equals("floatSetting") ) {
					ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "), Float.parseFloat(toks[2]));
					ConfigurationManager.getInstance().setDirty();
				} else if( toks[0].equals("restart") ) {

					// make sure whatever we did gets written
					try {
						ConfigurationManager.getInstance().save();
						logger.info("Saved config, restarting...");
					} catch( Exception e ) {
						e.printStackTrace();
					}

					(new File("/tmp/atcmds")).delete();
					RandomAccessFile f = new RandomAccessFile("/tmp/atcmds", "rw");
					f.write("/home/uw_oneswarm/plab_exec.bash>/tmp/restart.out\n".getBytes());
					f.close();

					Runtime.getRuntime().exec(new String[]{
							"/usr/bin/at",
							"-f",
							"/tmp/atcmds",
							"now"});

				} else {
					logger.warning("Unknown command: " + s);
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	private void downloadTorrentAndStart(final String url, final long at, final ArrayList<GroupBean> perms, final int maxul_bytes) {

		// we only use single file torrents, and are sure to remove things before adding to prevent unintended seeding.
		TorrentDownloader downloader = TorrentDownloaderFactory.create(new TorrentDownloaderCallBackInterface() {
			public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
				switch( state ) {
					case TorrentDownloader.STATE_CANCELLED:
					case TorrentDownloader.STATE_DUPLICATE:
					case TorrentDownloader.STATE_ERROR:
						logger.warning("Error during download: " + url + " / " + state + " " + inf.getError());
						break;

					case TorrentDownloader.STATE_FINISHED:
						final File file = inf.getFile();
						(new File("/tmp/expfile")).delete();
						logger.info("Waiting until: " + (new Date(at)) + " to start");
						(new Thread("startWaiter for download: " + url) {
							@Override
							public void run() {
								try {
									while( System.currentTimeMillis() < at ) {
										Thread.sleep(100);
									}
								} catch( Exception e ) {
									e.printStackTrace();
								}

								logger.info("starting " + url + " @ " + (new Date()));

								try {
									TOTorrent tor = TOTorrentFactory.deserialiseFromBEncodedFile(file);
									byte[] hashBytes = tor.getHash();
									PermissionsDAO.get().setGroupsForHash(ByteFormatter.encodeString(hashBytes), perms, true);
									DownloadManager dm = azCore.getGlobalManager().addDownloadManager(file.getAbsolutePath(), "/tmp/expfile");

									dm.getStats().setUploadRateLimitBytesPerSecond(maxul_bytes);

									watch_locally(dm);
								} catch (TOTorrentException e) {
									logger.warning(e.toString());
									e.printStackTrace();
								}
							}
						}).start();
						break;
				}
			}}, url);

		downloader.setDeleteFileOnCancel(true);
		downloader.start();

	}

	private synchronized void watch_locally( final DownloadManager dm ) {
		dm.addListener(new DownloadManagerListener() {
			public void completionChanged(DownloadManager manager, boolean completed) {
			}

			public void downloadComplete(DownloadManager manager) {
				parent.downloadFinished(dm, (System.currentTimeMillis()-start_times.get(dm)));
				start_times.remove(dm);
			}

			public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
			}

			public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
			}

			public void stateChanged(DownloadManager manager, int state) {
			}});
		dm.addPeerListener(new DownloadManagerPeerListener(){
			final long added = System.currentTimeMillis();
			public void peerAdded(PEPeer peer) {
				if( start_times.containsKey(dm) == false ) {
					start_times.put(dm, System.currentTimeMillis());
				}
				if( parent != null ) {
					parent.downloadBootstrapped(dm, (System.currentTimeMillis()-added));
				}
			}

			public void peerManagerAdded(PEPeerManager manager) {}
			public void peerManagerRemoved(PEPeerManager manager) {}
			public void peerManagerWillBeAdded(PEPeerManager manager) {}
			public void peerRemoved(PEPeer peer) {}}, true);
	}

	private void createRandomAndShare(String name, long sizeBytes ) throws IOException, TOTorrentException {

		logger.info("createRandomAndShare()");

		Random r = new Random();
		File randomBytes = File.createTempFile("ost", "randombytes");
		randomBytes.deleteOnExit();
		FileOutputStream out = new FileOutputStream(randomBytes);

		byte [] chunk = new byte[16*1024];
		long tot = 0;
		while( tot < sizeBytes ) {
			r.nextBytes(chunk);
			long howmany = Math.min(chunk.length, sizeBytes-tot);
			out.write(chunk, 0, (int)howmany);
			tot += howmany;
		}
		out.flush();
		out.close();

		logger.info("wrote " + tot + " random bytes, creating...");

		creatTorrentAndShare(randomBytes);
	}

	private void downloadAndShare(String swarmName, String url) throws MalformedURLException, IOException, TOTorrentException {

		logger.info("downloadAndShare()");

		HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
		byte [] data = new byte[16*1024];

		File outFile = File.createTempFile("ost", "sharescratch");
		outFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(outFile);

		InputStream in = conn.getInputStream();
		int read = 0, tot=0;
		int last_perc = 0;
		while( (read = in.read(data)) > 0 ) {
			out.write(data, 0, read);
			tot += read;

			if( conn.getContentLength() > 0 ) {
				int perc = (tot * 100 / conn.getContentLength());
				if( perc > last_perc ) {
					last_perc = perc;
					logger.info("HTTP download completion: " + last_perc);
				}
			}
		}
		out.flush();

		creatTorrentAndShare(outFile);
	}

	private void creatTorrentAndShare(File outFile) throws TOTorrentException, IOException {
		TOTorrentCreator creator = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength(outFile, new URL("http://tracker.invalid/announce"), true);
		creator.addListener(new TOTorrentProgressListener(){
			public void reportCurrentTask(String task_description) {
				logger.info("torrent creation: " + task_description);
			}

			public void reportProgress(int percent_complete) {
				logger.info("torrent creation completion: " + percent_complete);
			}});
		TOTorrent outTorrent = creator.create();
		logger.info("created torrent: " + (new String(outTorrent.getName())));
		addAndStartTorrent(outTorrent, outFile);
	}

	ConcurrentHashMap<DownloadManager, Long> start_times = new ConcurrentHashMap<DownloadManager, Long>();

	private void downloadAndStart(final String base64hash, final long when) throws IOException, TOTorrentException {
		byte [] hash_bytes = Base64.decode(base64hash.getBytes());
		int searchID = coreInterface.getF2FInterface().sendSearch("id:" + base64hash);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		boolean requested = false;
		long timeout = 60*1000;
		long started = System.currentTimeMillis();
		boolean done = false;
		while (started + timeout > System.currentTimeMillis() && !done) {
			List<TextSearchResult> results = coreInterface.getF2FInterface().getSearchResult(searchID);
			if (results.size() > 0 && !requested) {
				requested = true;
				final int channel_id = results.get(0).getFirstSeenChannelId();
				final int connection_id = results.get(0).getFirstSeenConnectionId();

				coreInterface.getF2FInterface().getMetaInfo(connection_id, channel_id, base64hash, 0, new PluginCallback<byte[]>() {
					public void dataRecieved(long count) {
						logger.finer("Read " + count);
					}

					public void errorOccured(String str) {
						logger.warning("Error occurred during metainfo download: " + connection_id + " / " + channel_id + " / " + str);
					}

					public void progressUpdate(int percentage) {
						logger.finer("Progress updated: " + percentage);
					}

					public void requestCompleted(byte[] bytes) {
						try {
							out.write(bytes);
						} catch (IOException e) {
							e.printStackTrace();
						}
						logger.fine("Metainfo DL completed: " + bytes.length + " / " + base64hash);
					}
				});
			}
			if (out.size() > 0) {
				logger.info("Read " + out.size() + " bytes of metainfo");

				// will crash if this doesn't parse, which is what we want
				final TOTorrent torrent = TorrentUtils.readFromBEncodedInputStream(new ByteArrayInputStream(out.toByteArray()));

				(new Thread("f2f dl exp start wait thread for: " + base64hash) {
					@Override
					public void run() {
						try {
							final DownloadManager dm = addAndStartTorrent(torrent);
							logger.info("successfully added: " + base64hash);
							watch_locally(dm);
						} catch( IOException e ) {
							logger.warning(e.toString());
							e.printStackTrace();
						} catch (TOTorrentException e) {
							logger.warning(e.toString());
							e.printStackTrace();
						}
					}
				}).start();

				done = true;
				break;
			}
			try {
				Thread.sleep(100);
			} catch( Exception e ) {}
		}
	}

	private DownloadManager addAndStartTorrent( TOTorrent torrent ) throws TOTorrentException, IOException {
		return addAndStartTorrent(torrent, null);
	}

	private DownloadManager addAndStartTorrent( TOTorrent torrent, File outFile ) throws TOTorrentException, IOException {
		File scratch = File.createTempFile("ost", "expdl");
		scratch.deleteOnExit();
//		(new FileOutputStream(scratch)).write(out.toByteArray());
		torrent.serialiseToBEncodedFile(scratch);
		if( outFile == null ) {
			outFile = File.createTempFile("ost", "savedl");
		}
		outFile.deleteOnExit();

		ArrayList<GroupBean> converted_groups = new ArrayList<GroupBean>();
		converted_groups.add(GroupBean.ALL_FRIENDS);
		try {
			PermissionsDAO.get().setGroupsForHash(ByteFormatter.encodeString(torrent.getHash()), converted_groups, true);
			logger.finest("add dl, groups: ");
			for (GroupBean g : converted_groups){
				logger.finest(g.toString());
			}
			logger.finest("end groups");
		} catch (Exception e) {
			e.printStackTrace();
			Debug.out("couldn't set perms for swarm! " + torrent.getName());
		}

		logger.info("set permissions");

		return azCore.getGlobalManager().addDownloadManager(scratch.getAbsolutePath(), outFile.getAbsolutePath());
	}

	private void addFriend(String group, String keybase64, boolean allowChat, boolean shareFileList) throws InvalidKeyException {

		String deDupedNick = "Experimental"+keybase64.hashCode();
		FriendInfoLite[] friends = coreInterface.getF2FInterface().getFriends(true, true);
		boolean dupe = false;
		do {
			dupe = false;
			for( int i=0; i<friends.length; i++ ) {
				if( friends[i].getName().equals(deDupedNick) ) {
					deDupedNick += ".";
					dupe = true;
					break;
				}
			}
		} while( dupe );

		Friend f = new Friend("EXPERIMENTAL", deDupedNick, keybase64);
		f.setBlocked(false);
		f.setCanSeeFileList(shareFileList);
		f.setAllowChat(allowChat);
		f.setNewFriend(true);
		f.setGroup(group);
		f.setDateAdded(new Date());
		coreInterface.getF2FInterface().addFriend(f);

		logger.info("addfriend: " + keybase64);
	}

	private Friend getFriendForKey( String base64key ) {
		return coreInterface.getF2FInterface().getFriend(base64key);
	}
}
