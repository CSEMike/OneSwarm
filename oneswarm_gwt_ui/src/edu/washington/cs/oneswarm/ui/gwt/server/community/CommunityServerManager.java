package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.ui.gwt.F2FInterface;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCommunityServer;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.FriendInfoLiteFactory;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;

/**
 * There's a lot of code in here that duplicates functionality in the FriendImportManager in the f2f plugin, but either place we put it, there are problems 
 * (if it were there, we'd have to write a lot of plumbing to get backend tasks)
 *
 * Conclusion:
 * We should really just merge everything and stop using plugins. 
 */

public final class CommunityServerManager extends Thread {
	
	private static Logger logger = Logger.getLogger(CommunityServerManager.class.getName());
	
	private volatile static CommunityServerManager inst = null;
	
	Set<String> filtered = new HashSet<String>();
	Set<FriendInfoLite> unmunched = new HashSet<FriendInfoLite>();
	Map<String, CommunityRecord> activeServers = new HashMap<String, CommunityRecord>();
	
	F2FInterface f2f = null;
	int filteredSize = 0;
	
	Properties certificateHashes = new Properties();

	private boolean filteredSizeValid;
	PriorityBlockingQueue<RefreshCommunityServerTask> tasks = new PriorityBlockingQueue<RefreshCommunityServerTask>(5, new Comparator<RefreshCommunityServerTask>(){
		public int compare(RefreshCommunityServerTask o1, RefreshCommunityServerTask o2) {
			if( o1.getExecutionTime() > o2.getExecutionTime() ) {
				return 1;
			} else if( o1.getExecutionTime() > o2.getExecutionTime() ) {
				return -1;
			} else {
				return 0;
			}
		}});

	private File mCommunityServerHashesFile;

	public static CommunityServerManager get() {
		if( inst == null ) {
			inst = new CommunityServerManager();
		}
		return inst;
	}
	
	public CommunityServerManager() {
		f2f = new F2FInterface(AzureusCoreImpl.getSingleton().getPluginManager().getDefaultPluginInterface());
		
		/**
		 * This is the inital poll of all servers on startup -- 
		 * after this, each server will be polled according to its individual 
		 * refresh time in its own timertask
		 */
		COConfigurationManager.addAndFireParameterListener("oneswarm.community.servers", new ParameterListener(){
			boolean firstRun = true; // immediately on startup, with a delay afterwards
			public void parameterChanged(String parameterName) {
				
				Map<String, CommunityRecord> oldServers = activeServers;
				activeServers = new HashMap<String, CommunityRecord>();
				
				StringList servers = COConfigurationManager.getStringListParameter("oneswarm.community.servers");
				List<String> converted = new ArrayList<String>();
				for( int i=0; i<servers.size(); i++ ) {
					converted.add(servers.get(i));
				}
				
				Set<String> existingURLs = new HashSet<String>();
				
				for( int i=0; i<servers.size()/5; i++ ) {
					CommunityRecord server = new CommunityRecord(converted, 5*i);
					
					if( existingURLs.contains(server.getUrl()) ) {
						logger.warning("Skipping duplicate community server url: " + server.getUrl());
						continue;
					}
					
					existingURLs.add(server.getUrl());
					
					long nextRun = System.currentTimeMillis() + RefreshCommunityServerTask.DEFAULT_DELAY_MS;
					if( firstRun ) { 
						nextRun = 0;
					}
					
					if( oldServers.containsKey(server.getUrl()) == false ) {
						tasks.add(new RefreshCommunityServerTask(server, nextRun));
						logger.finer("Adding task for: " + server.getUrl());
					} else { 
						logger.fine("Skipped duplicate add task for known community server: " + server.getUrl());
					}
					activeServers.put(server.getUrl(), server);
				}
				firstRun = false;
			}});
		
		/**
		 * Needs to be after activeServers has been populated since we use that to prune old entries
		 */
		loadCertificateHashes();
		
		setDaemon(true);
		setName("CommunityServer polling");
		
		start();
	}
	
	private void loadCertificateHashes() {
		File keysDir = new File(SystemProperties.getUserPath() + File.separator + "keys" + File.separator);
		if( keysDir.isDirectory() == false ) {
			keysDir.mkdirs();
		}
		mCommunityServerHashesFile = new File(keysDir, "community_server_hashes");
		
		if (mCommunityServerHashesFile.exists() == false) {
			logger.warning("community_server_hashes file does not exist.");
			return;
		}
		
		try {
			certificateHashes.load(new FileInputStream(mCommunityServerHashesFile));
			
			/** 
			 * Prune old entries
			 */
			boolean pruned = false;
			for( Object serverURL : certificateHashes.keySet().toArray() ) {
				if( activeServers.containsKey((String)serverURL) == false ) {
					certificateHashes.remove(serverURL);
					pruned = true;
					logger.finer("Pruning old hash: " + serverURL);
				}
			}
			
			if( pruned ) {
				saveCertificateHashes();
			}
			
		} catch( IOException e ) {
			e.printStackTrace();
			logger.warning(e.toString());
		}
	}
	
	public String getBase64CommunityServerCertificateHash( String inURL ) {
		return certificateHashes.getProperty(inURL, null);
	}
	
	public void trustCommunityServerCertificateHash(String inURL, String inBase64Hash) {
		/**
		 * skip if we already have an entry for this server
		 */
		if( certificateHashes.getProperty(inURL, null) != null ) {
			logger.warning("Ignoring duplicate trust call for community server: " + inURL);
			return;
		}
		
		certificateHashes.setProperty(inURL, inBase64Hash);
		saveCertificateHashes();
	}
	
	public synchronized void saveCertificateHashes() {
		try {
			certificateHashes.store(new FileOutputStream(mCommunityServerHashesFile), "url => SHA1(certificate)");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.warning(e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			logger.warning(e.toString());
		}
	}

	class RefreshCommunityServerTask implements Runnable {
		
		public static final long DEFAULT_DELAY_MS = 15 * 60 * 1000; // 15 minutes
		
		private CommunityRecord server;

		private long executionTime;
		
		public RefreshCommunityServerTask( CommunityRecord rec, long executionTime ) {
			this.server = rec;
			this.executionTime = executionTime;
		}

		public long getExecutionTime() {
			return executionTime;
		}

		public void run() {
			
			if( activeServers.containsKey(server.getUrl()) == false ) { 
				logger.fine("Skipping check of removed community server: " + server.getUrl());
				return;
			}
			
			logger.info("Refreshing community server: " + server.getUrl());
			/**
			 * don't actually run this as a thread when in timertask
			 */
			CommunityServerRequest req = null;
			req = new CommunityServerRequest(server, false);
			int tid = BackendTaskManager.get().createTask("Refreshing community server", new CancellationListener(){
				public void cancelled(int inID) {}});
			BackendTaskManager.get().getTask(tid).setSummary(server.getUrl());
			req.setTaskID(tid);
			req.run();
			
			long delay = DEFAULT_DELAY_MS;
			if( req.getRefreshInterval() != null ) {
				try {
					delay = Long.parseLong(req.getRefreshInterval()) * 1000;
					if( delay <= 0 ) { 
						logger.warning("Non-positive delay! " + delay + " from " + server.getUrl());
						delay = DEFAULT_DELAY_MS;
					}
				} catch( Exception e ) {};
			}
			
			delay = Math.max(delay, server.getMinimum_refresh_interval() * 60 * 1000);
			
			logger.fine("Next refresh " + server.getUrl() + " in " + delay + " (" + (new java.util.Date(System.currentTimeMillis()+delay)).toString() + ")");
			
			tasks.add(new RefreshCommunityServerTask(server, System.currentTimeMillis() + delay));
			logger.finer("After insert, tasks has: " + tasks.size());
		}
	}
	
	public void run() { 
		try {
			while( true ) {
				RefreshCommunityServerTask t = tasks.peek();
				if( t != null ) {
					if( t.getExecutionTime() < System.currentTimeMillis() ) {
						tasks.poll();
						long start = System.currentTimeMillis();
						t.run();
						logger.finest("Community processing took: " + (System.currentTimeMillis()-start) + " tasks queue: " + tasks.size());
					}
				}
				
				Thread.sleep(1000);
			}
		} catch( Exception e ) {
			e.printStackTrace();
			logger.warning(e.toString());
		}
	}
	
	/**
	 * 
	 * Called to update the latest view of community peers for a particular server
	 * 
	 * @param parsed -- the {key, nick} pairs
	 * @param group -- which group to add these to
	 * @param manual_confirmation -- prompt user for addition?
	 * @param sync_server_deletes -- are we using the server's response to delete entries (i.e., keeping ourselves completely up-to-date)
	 * @param from_url -- which community server did these come from
	 * @param pruning_threshold -- if not using server deletes, how many friends should we have before removing the oldest ones?
	 */
	public synchronized void feed( List<String[]> parsed, CommunityRecord fromServer ) {
		
		final String group = fromServer.getGroup();
		final boolean manual_confirmation = fromServer.isConfirm_updates();
		final boolean sync_server_deletes = fromServer.isSync_deletes();
		final String from_url = fromServer.getUrl();
		final int pruning_threshold = fromServer.getPruning_threshold();
		
		
		FriendInfoLite [] existing = f2f.getFriends(true, true);
		
		Set<String> uncovered_existing_keys = new HashSet<String>();
		List<FriendInfoLite> existing_from_this_server = new ArrayList<FriendInfoLite>();
		
		// make sure filtered always has the latest set of local public keys
		for( FriendInfoLite f : existing ) {
			filtered.add(f.getPublicKey());
		}
		
		if( sync_server_deletes ) {
			/**
			 * Add all the existing friends from this community server to a set and check them off as we enumerate the response
			 */
			for( FriendInfoLite f : existing ) {
				if( f.getSource().endsWith(from_url) ) {
					uncovered_existing_keys.add(f.getPublicKey());
				}
			}
			logger.finer("Sync-deletes checking for " + uncovered_existing_keys.size() + " from " + from_url);
		} else { 
			for( FriendInfoLite f : existing ) {
				if( f.getSource().endsWith(from_url) ) {
					existing_from_this_server.add(f);
				}
			}
		}
		
		for( String [] pair : parsed ) {
			String key = pair[0];
			String nick = pair[1];
			
			if( sync_server_deletes ) {
				uncovered_existing_keys.remove(key);
			}
			
			if( filtered.contains(key) ) {
				logger.finer("skipping duplicate fed friend: " + nick + " / " + key);
				continue;
			}
			
			if( manual_confirmation ) {
				FriendInfoLite converted = FriendInfoLiteFactory.createFromKeyAndNick(key, nick, FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME + " " + from_url );
				converted.setDateAdded(new Date());
				converted.setGroup(group);
				unmunched.add(converted);
				logger.finer("fed: " + key + " / " + nick);
			} else {
				logger.finest("Auto-add consideration of " + key + " / " + nick);
//				FriendInfoLite [] existing = f2f.getFriends(true, true);
				boolean duplicateNickname = false;
				String deDupedNick = nick;
				do {
					duplicateNickname = false;
					for( FriendInfoLite e : existing ) {
						if( e.getPublicKey().equals(key) ) {
							logger.warning("bailing out early due to duplicate key that SHOULD HAVE been eliminated: " + nick + " / " + key);
							return;
						}
						if( e.getName().equals(deDupedNick) ) {
							duplicateNickname = true;
							deDupedNick += ".";
						}
					}
				} while( duplicateNickname );
				if( key.equals(f2f.getMyPublicKey()) ) {
					logger.warning("Skipping adding own key to friend list");
					continue;
				}
				try {
					Friend f = new Friend(FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME + " " + from_url, deDupedNick, key);
					f.setBlocked(false);
//					f.setCanSeeFileList(!fromServer.isLimited_default());
					f.setCanSeeFileList(false);
					// limited=do not request file list
					f.setRequestFileList(f.isCanSeeFileList());
					f.setAllowChat(fromServer.isChat_default());
					f.setNewFriend(true);
					f.setGroup(group);
					f.setDateAdded(new Date());
					f.setRequestFileList(false);
					f2f.addFriend(f);
					filtered.add(key);
					logger.finer("auto added: " + deDupedNick);
				} catch (InvalidKeyException e) {
					e.printStackTrace();
					logger.warning(e.toString());
				}
			}
		}
		
		if( sync_server_deletes ) {
			logger.finer(uncovered_existing_keys.size() + " remain in uncovered_existing_keys");
			for( String s : uncovered_existing_keys ) {
				if( manual_confirmation ) {
					FriendInfoLite converted = FriendInfoLiteFactory.createFromKeyAndNick(s, "", FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME + " " + from_url );
					converted.setStatus(FriendInfoLite.STATUS_TO_BE_DELETED);
					unmunched.add(converted);
					logger.finest("Added: " + s + " to unmunched"); 
				} else { 
					logger.finer("Auto-deleting (server-sync) friend: " + s);
					FriendInfoLite converted = new FriendInfoLite();
					converted.setPublicKey(s);
					f2f.deleteFriend(converted);
				}
			}
		} else if( existing_from_this_server.size() > pruning_threshold ) {
			/**
			 * Overflow -- time to prune. Sort the existing peers we have from this server 
			 * by the time of our most recent connection and then snip the most distant. 
			 */
			final long now = System.currentTimeMillis();
			Collections.sort(existing_from_this_server, new Comparator<FriendInfoLite>(){
				public int compare(FriendInfoLite o1, FriendInfoLite o2) {
					Date lastConn1 = o1.getLastConnectedDate() != null ? o1.getLastConnectedDate() : new Date(0); 
					Date lastConn2 = o2.getLastConnectedDate() != null ? o2.getLastConnectedDate() : new Date(0);
					
					return - lastConn1.compareTo(lastConn2);
				}});
			
//			for( FriendInfoLite f : existing_from_this_server ) {
//				System.out.println("debug: " + f.getLastConnectedDate() + " / " + f.getName());
//			}
			
			for( int i=0; i<existing_from_this_server.size(); i++ ) {
				FriendInfoLite f = existing_from_this_server.get(i);
				if( f.getDateAdded() == null ) {
					continue;
				}
				/**
				 * To give new peers a chance -- we only apply this to peers that have been added 
				 * at least 8 hours ago  
				 */
				if( f.getDateAdded().getTime() + (8 * 60 * 60 * 1000) > System.currentTimeMillis() ) {
					logger.finest("Not considering threshold delete for new friend " + f.getName() + " added " + (now - f.getDateAdded().getTime())/3600000 + " hours ago" );
					continue;
				}
				
				/**
				 * If we're connected to this peer (or have connected in the last 2 hours), don't prune 
				 */
				if( f.getStatus() == FriendInfoLite.STATUS_ONLINE ) {
					logger.finest("Not considering threshold delete for connected friend: " + f.getName()); 
					continue;
				}
				if( f.getLastConnectedDate() != null ) {
					if( f.getLastConnectedDate().getTime() + (2 * 60 * 60 * 1000) > System.currentTimeMillis() ) {
						logger.finest("Not considering threshold delete for recently connected friend: " + f.getName());
						continue;
					}
				}
				
				/**
				 * At this point, prune if we're above the threshold for old friends 
				 */
				if( i > pruning_threshold ) {
					if( manual_confirmation ) {
						FriendInfoLite converted = FriendInfoLiteFactory.createFromKeyAndNick(f.getPublicKey(), "", FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME + " " + from_url );
						converted.setStatus(FriendInfoLite.STATUS_TO_BE_DELETED);
						unmunched.add(converted);
						logger.finest("Added: " + f.getPublicKey() + " to unmunched " + f.getName() + " lastConn: " + f.getLastConnectedDate() + " i: "+ i + " / " + pruning_threshold ); 
					} else { 
						logger.finer("Auto-deleting (due to threshold excess) old friend from " + from_url + " / " + f.getName() + " i: " + i + " / (" + pruning_threshold + ")"); 
						FriendInfoLite converted = new FriendInfoLite();
						converted.setPublicKey(f.getPublicKey());
						f2f.deleteFriend(converted);
					}
				} else { 
					logger.finest(f.getName() + " under consideration, but under the disconnection threshold " + i + " / " + (pruning_threshold));
				}
			}
		}

		filteredSizeValid = false;
	}
	
	public synchronized FriendInfoLite [] filter( FriendInfoLite [] inList ) {
		List<FriendInfoLite> outList = new ArrayList<FriendInfoLite>();
		
		/**
		 * Skip existing friends
		 */
		FriendInfoLite [] existing = f2f.getFriends(true, true);
		Set<String> existing_keys = new HashSet<String>();
		for( FriendInfoLite f : existing ) {
			existing_keys.add(f.getPublicKey());
		}
		/**
		 * Just in case we happen to encounter a buggy community server...
		 */
		String myid = f2f.getMyPublicKey();
		existing_keys.add(myid);
		
		int removed = 0;
		for( FriendInfoLite f : inList ) {
			if( (existing_keys.contains(f.getPublicKey()) == false &&
				filtered.contains(f.getPublicKey()) == false) || 
				f.getStatus() == FriendInfoLite.STATUS_TO_BE_DELETED ) { // these _will_ be in the friend list, but that's intentional
				outList.add(f);
			} else {
				removed++;
			}
		}
		
		logger.fine("skipped " + removed + " duplicates");
		
		return outList.toArray(new FriendInfoLite[0]);
	}
	
	public synchronized FriendList munch() {
		/**
		 * Filtered never gets cleared out -- this is so you can ignore people within a given execution without being 
		 * prompted again and again
		 */
		
		FriendList out = new FriendList();
		Set<FriendInfoLite> outSet = unmunched;
		unmunched = new HashSet<FriendInfoLite>();
		filteredSize = 0;
		filteredSizeValid = true;
		
		out.setFriendList(filter(outSet.toArray(new FriendInfoLite[0])));
		
		logger.finer(out.getFriendList().length + " munched");
		
		// now these are all filtered
		for( FriendInfoLite f : out.getFriendList() ) { 	
			filtered.add(f.getPublicKey());
		}
		
		return out;
	}
	
	public synchronized int getUnmunchedCount() {
		// do this lazily to avoid unnecessary processing
		if( filteredSizeValid == false ) {
			recomputeFilteredSize();
		}
//		logger.finest("getUnmunchedCount: " + filteredSize);
		return filteredSize;
	}

	private void recomputeFilteredSize() {
		filteredSize = filter(unmunched.toArray(new FriendInfoLite[0])).length;
		filteredSizeValid = true;
	}

	public void refreshAll() {
		synchronized(tasks) { 
			for( RefreshCommunityServerTask t : tasks ) {
				t.executionTime = 0;
			}
			logger.info("Refreshing " + tasks.size());
		}
	}
	
	public String debug() {
		String out = "tasks:\n";
		synchronized(tasks) { 
			for( RefreshCommunityServerTask t : tasks ) {
				out += t.server.getUrl() + " " + (System.currentTimeMillis() - t.getExecutionTime()) + "\n";
			}
		}
		out += "done\n";
		return out;
	}
}

