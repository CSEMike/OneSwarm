package edu.washington.cs.oneswarm.f2f.permissions;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerInitialisationAdapter;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter.DownloadManagerStartListener;

public class PermissionsDAO {
	private static Logger logger = Logger.getLogger(PermissionsDAO.class.getName());

	private static final String PERMISSIONS_FILE_NAME = "permissions.xml";
	private static final String GROUPS_FILE_NAME = "groups.xml";
	
	private static final String PERMISSIONS_PATH = SystemProperties.getUserPath() + File.separator + PERMISSIONS_FILE_NAME;
	private static final String GROUPS_PATH = SystemProperties.getUserPath() + File.separator + GROUPS_FILE_NAME;

	private static final String NO_PERMISSIONS_DATA = "oneswarm.no.permissions";
	
	private static PermissionsDAO inst = new PermissionsDAO();
	
	private long nextGroupID = 3;

	/**
	 * Groups v2 -- group IDs and info stored in swarm additional properties
	 */
	Map<Long, GroupBean> groupid_to_group = new HashMap<Long, GroupBean>();
	
	// soft state -- can be reconstructed from groupid_to_group, and is not saved. 
	Map<String, Long> base64Key_to_groupid = new HashMap<String, Long>();
	
	/** 
	 * we use this as a temporary storage area when setting/retrieving permissions before we've actually 
	 * added the download manager. Swarms are removed from here when they are actually initialized. 
	 */
	Map<String, ArrayList<GroupBean>> temporaryHash_to_groups = new HashMap<String, ArrayList<GroupBean>>();
	
	/**/

	private IPCInterface f2fIpc = null;

	public static final PermissionsDAO get() {
		return inst;
	}

	protected PermissionsDAO() {
	}

	public synchronized void init(IPCInterface f2f) {
		if (f2fIpc == null) {
			f2fIpc = f2f;

			add_special();
			
			try { 
				load_groups();
			} catch( Exception e ) {
				logger.warning("Error loading v2 permissions, trying load of v1. " + e.toString());
				
				try {
					load_permissions();
				} catch( IOException e2 ) { 
					logger.warning("Error loading v1 permissions: " + e.toString());
				}
			} // catch from load_groups()
			
			sanity_check_perms();
			
		} // f2fIpc != null
	}
	
	private synchronized void add_special() {
		for( GroupBean special : new GroupBean[]{GroupBean.ALL_FRIENDS, GroupBean.PUBLIC} ) {
			GroupBean candidate = groupid_to_group.get(special.getGroupID());
			if( candidate != null ) {
				if( candidate.equals(special) == true ) {
					continue;
				}
			}
			logger.fine("Adding " + special.getGroupName() + " after load");
			groupid_to_group.put(special.getGroupID(), special);
		}
	}
	
	private synchronized void sanity_check_perms() { 
		AzureusCore core = AzureusCoreImpl.getSingleton();
		
		for( DownloadManager dm : (List<DownloadManager>)core.getGlobalManager().getDownloadManagers() ) {
			try {
				String [] ids = dm.getDownloadState().getListAttribute(TOTorrentImpl.OS_PERMISSIONS);
				
				if( ids == null ) {
					dm.setData(NO_PERMISSIONS_DATA, Boolean.TRUE);					
				} else if( ids.length == 0 ) { 
					dm.setData(NO_PERMISSIONS_DATA, Boolean.TRUE);
				}
				
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This is a hack, but is needed to deal with load order issues.
	 */
	public synchronized void f2fInitialized() {
		AzureusCoreImpl.getSingleton().getGlobalManager().addDownloadManagerInitialisationAdapter(new DownloadManagerInitialisationAdapter(){
			@Override
            public void initialised(DownloadManager manager) {
				try {
					String hexHash = ByteFormatter.encodeString(manager.getTorrent().getHash());
					ArrayList<GroupBean> groups = PermissionsDAO.this.getGroupsForHash(hexHash);
					if( temporaryHash_to_groups.containsKey(hexHash) ) {
						PermissionsDAO.this.setGroupsForHash(hexHash, groups, false, manager);
						logger.finer("Removing from temporaryHash_to_groups, size: " + (temporaryHash_to_groups.size()-1));
						temporaryHash_to_groups.remove(hexHash);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
	}
	
	private void load_groups() throws IOException {
		logger.info("loading groups (v2)");
		XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(GROUPS_PATH)));
		Map<Long, GroupBean> scratch_groupid_to_group = (Map<Long, GroupBean>)decoder.readObject();
		
		/**
		 * Sanity check this file -- does it include all friends and public at the correct IDs? If not, throw it out. 
		 */
		for( GroupBean special : new GroupBean[]{GroupBean.ALL_FRIENDS, GroupBean.PUBLIC} ) {
			if( scratch_groupid_to_group.get(special.getGroupID()).equals(special) == false ) {
				throw new IOException("Seemingly corrupt groups file -- special group IDs don't match requirements (" + special + ")");
			}
		}
		
		groupid_to_group = scratch_groupid_to_group;
	}

	private void load_permissions() throws IOException {
		logger.info("loading permissions... (v1)");
		
		/**
		 * v1 state -- we try to load this and then convert it. 
		 */
		Map<String, List<GroupBean>> infohash_to_groups = new HashMap<String, List<GroupBean>>();
		Map<String, GroupBean> groupname_to_group = new HashMap<String, GroupBean>();
		
		XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(PERMISSIONS_PATH)));

		infohash_to_groups = (Map<String, List<GroupBean>>) decoder.readObject();
		groupname_to_group = (Map<String, GroupBean>) decoder.readObject();

		/**
		 * Sanity check stored values, eliminate null groups, etc.
		 */
		for (String groupName : groupname_to_group.keySet().toArray(new String[0])) {
			if (groupname_to_group.get(groupName) == null) {
				groupname_to_group.remove(groupName);
				logger.warning("removed groupname with null group: " + groupName);
			}
		}
		for (String infohash : infohash_to_groups.keySet().toArray(new String[0])) {
			if (infohash_to_groups.get(infohash) == null) {
				infohash_to_groups.remove(infohash);
				logger.warning("removed infohash to group mapping with null groups: " + infohash);
			}
		}
		
		/**
		 * Build up v2 of group structures
		 */
		for( GroupBean g : groupname_to_group.values() ) { 
			
			// these were added separately
			if( g.equals(GroupBean.PUBLIC) ) {
				g.setGroupID(GroupBean.PUBLIC.getGroupID());
				continue;
			}
			
			if( g.equals(GroupBean.ALL_FRIENDS) ) {
				g.setGroupID(GroupBean.ALL_FRIENDS.getGroupID());
				continue;
			}
			
			if( g.getGroupID() <= 0 ) { 
				logger.warning("Group has invalid group ID: " + g.getGroupID() + ", setting to: " + (nextGroupID+1));
				g.setGroupID(nextGroupID++);
			}
			groupid_to_group.put(g.getGroupID(), g);
			
			if( g.isUserGroup() ) { 
				base64Key_to_groupid.put(g.getMemberKeys().get(0), g.getGroupID());
			}
		}

		if (logger.isLoggable(Level.FINEST)) {
			for (String hash : infohash_to_groups.keySet()) {
				logger.finest(hash + " has: ");
				for (GroupBean g : infohash_to_groups.get(hash)) {
					if( g == null ) { 
						logger.finest("\tnull");
					} else {
						logger.finest("\t" + g.getGroupName());
					}
				}
			}
		}

		refresh_friend_groups();

		logger.info("loaded existing permissions for " + infohash_to_groups.size());

		checkAndConvertOldPermissions(groupname_to_group, infohash_to_groups);

		save_groups();
	}
	
	public void checkAndConvertOldPermissions(Map<String, GroupBean> groupnameToGroup, Map<String, List<GroupBean>> infohash_to_groups) { 
		/**
		 * Check for any existing DL managers that don't have a permissions attribute (and give them one)
		 */
		AzureusCore core = AzureusCoreImpl.getSingleton();
		for( DownloadManager dm : (List<DownloadManager>)core.getGlobalManager().getDownloadManagers() ) {
			try {
				String [] ids = dm.getDownloadState().getListAttribute(TOTorrentImpl.OS_PERMISSIONS);
				
				// null or length zero.
				boolean convert = ids == null;
				if( ids != null ) {
					convert = ids.length == 0;
				}
				
				if( convert ) { 
					String hexHash = ByteFormatter.encodeString(dm.getTorrent().getHash());
					
					List<GroupBean> existing = infohash_to_groups.get(hexHash);
					String [] converted; 
					if( existing == null ) {
						logger.warning("Swarm has no permissions (either attrib or xml): " + dm.getDisplayName());
						continue;
					} else {
					
						logger.info(dm.getDisplayName() + " needs attrib permissions conversion");
						
						converted = new String[existing.size()];
						for( int i=0; i<existing.size(); i++ ) { 
							
							/**
							 * The group ID is retrieved from the groupnameToGroup structure because that's the one 
							 * that's been converted -- the beans in the v1 hash->groups structure will all have 0 group IDs, but
							 * we generated IDs for groupname -> group
							 */
							try {
								logger.finest("Converting: " + existing.get(i).toString());
								converted[i] = groupnameToGroup.get(existing.get(i).getGroupName()).getGroupID()+"";
							} catch( Exception e ) {
								logger.warning("Error converting group. (" + e.toString() + ")");
								converted[i] = "0";
							}
						}
						
						dm.getDownloadState().setListAttribute(TOTorrentImpl.OS_PERMISSIONS, converted);
					}
					
				}
			} catch( Exception e ) { 
				e.printStackTrace();
				logger.warning("Error during permissions conversion check: " + e.toString()); 
			}
		}
	}

	public synchronized void refresh_friend_groups() {
		logger.fine("refresh friend groups");

		List<Friend> permittable_friends = new ArrayList<Friend>();

		try {
			List<Friend> raw_friends = (List<Friend>) f2fIpc.invoke("getFriends", new Object[0]);
			for (Friend f : raw_friends) {
				if (f.isBlocked() == false && f.isCanSeeFileList()) {
					permittable_friends.add(f);
				}
			}
		} catch (IPCException e) {
			logger.warning("IPC error: " + e.toString() + " (check stderr)");
			e.printStackTrace();
		}
		
		Set<String> covered_keys = new HashSet<String>();
		
		/**
		 * Add any new users and build up a list of current keys
		 */
		for( Friend f : permittable_friends ) { 
			
			String base64Key = new String(Base64.encode(f.getPublicKey()));
			covered_keys.add(base64Key);
			
			if( base64Key_to_groupid.containsKey(base64Key) == false ) {
				logger.fine("Adding new user group for: " + f.getNick());
				List<String> scratchKeys = new ArrayList<String>();
				scratchKeys.add(base64Key);
				this.addGroup(f.getNick(), scratchKeys, true);
			}
		}
		
		/**
		 * Now, for any of the keys in our current list which are not in this set, remove. 
		 */
		boolean save = false;
		for( String key : base64Key_to_groupid.keySet().toArray(new String[0]) ) { 
			if( covered_keys.contains(key) == false ) {
				logger.fine("Sync with friend list removes user group: " + key);
				try {
					removeGroupID(base64Key_to_groupid.get(key), true);
					save = true;
				} catch (IOException e) {
                    // e.printStackTrace();
					logger.warning(e.toString());
				}
			}
		}
		if( save ) {
			save_groups();
		}
	}

	public synchronized void renameGroup(long inGroupID, String neu) throws IOException {
		
		GroupBean g = groupid_to_group.get(inGroupID);
		if( g == null ) { 
			throw new IOException("Invalid group id: " + inGroupID);
		}
		
		if( g.getGroupID() <= 2 ) {
			throw new IOException("Cannot rename special groups (public, all friends)");
		}
		
		g.setGroupName(neu);
		save_groups();
	}

	public void refreshFileLists() {
		try {
			logger.fine("refresh file lists in f2f...");
			f2fIpc.invoke("refreshFileLists", new Object[0]);
			logger.fine("done refreshFileLists()");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized void save_groups() {
		logger.fine("save groups");
		try {
			XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(GROUPS_PATH)));
			encoder.writeObject(groupid_to_group);
			encoder.close();
			logger.fine("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized long addGroup(String inName, List<String> userKeys, boolean isUserGroup) {
		
		if( isUserGroup && userKeys.size() != 1 ) { 
			throw new RuntimeException("logic problem -- add user group with != 1 key! (" + userKeys.size() + ")");
		}
		
		GroupBean g = GroupBean.createGroup(inName, userKeys, isUserGroup, nextGroupID++);
		groupid_to_group.put(g.getGroupID(), g);
		
		if( isUserGroup ) {
			base64Key_to_groupid.put(userKeys.get(0), g.getGroupID());
		}
		
		save_groups();
		logger.fine("added new group: " + inName);
		
		return g.getGroupID();
	}
	
	public synchronized void updateGroupKeys( Long inID, List<String> keys ) throws IOException { 
		GroupBean g = getGroup(inID);
		
		if( g == null ) {
			throw new IOException("Invalid group ID: " + inID + " (group not found)");
		}
		
		if( g.isUserGroup() == true ) {
			throw new IOException("Can't update keys for a user group.");
		}
		g.setMemberKeys(keys);
	}
	
	public synchronized void removeGroupID( Long inID ) throws IOException { 
		removeGroupID(inID, false);
	}
	
	private synchronized void removeGroupID( Long inID, boolean userRemoval ) throws IOException { 
		
		if( inID <= 0 ) { 
			throw new IOException("Invalid group ID: " + inID);
		}
		if( inID <= 2 ) { 
			throw new IOException("Cannot remove reserved group " + inID);
		}
		
		GroupBean g = getGroup(inID);
		if( g == null ) {
			throw new IOException("Invalid group id (group not found): " + inID);	
		}
		
		if( userRemoval == false && g.isUserGroup() ) {
			throw new IOException("Tried to remove user group.");
		}
		
		groupid_to_group.remove(inID);
	}

	public synchronized boolean hasPublicPermission(byte[] infoHash) {
		String inHexHash = ByteFormatter.encodeString(infoHash);
		List<GroupBean> allowedGroups = getGroupsForHash(inHexHash);
		if (allowedGroups == null) {
			return false;
		}
		return allowedGroups.contains(GroupBean.PUBLIC);
	}

	public synchronized boolean hasAllFriendsPermission(byte[] infoHash) {
		String inHexHash = ByteFormatter.encodeString(infoHash);
		List<GroupBean> allowedGroups = getGroupsForHash(inHexHash);
		if (allowedGroups == null) {
			return false;
		}
		return allowedGroups.contains(GroupBean.ALL_FRIENDS);
	}
	
	public synchronized ArrayList<GroupBean> getGroupsForHash( String inHexHash ) {
		
		final DownloadManager dm = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(ByteFormatter.decodeString(inHexHash)));
		
		if( dm == null ) {
			
			ArrayList<GroupBean> scratch = temporaryHash_to_groups.get(inHexHash);
			if( scratch != null ) {
				return scratch;
			}
			
			logger.warning("getGroupsForHash_v2 with null download manager! " + inHexHash);
			return new ArrayList<GroupBean>();
		}
		
		if( temporaryHash_to_groups.containsKey(inHexHash) ) {
			logger.warning("DownloadManager is not null, but temporary hash still has entry for swarm: " + dm.getDisplayName());
		}
		
		String[] groups = dm.getDownloadState().getListAttribute(TOTorrentImpl.OS_PERMISSIONS);
		ArrayList<GroupBean> out = new ArrayList<GroupBean>();
		try { 
			
			if( groups == null ) {
				logger.warning("No permissions for swarm: " + dm.getDisplayName());
				dm.setData(NO_PERMISSIONS_DATA, Boolean.TRUE);
				return out;
			}
			
			if( groups.length == 0 ) {
				logger.fine("Swarm not shared with anyone. " + dm.getDisplayName());
				dm.setData(NO_PERMISSIONS_DATA, Boolean.TRUE);
				return out;
			}
			
			for( String idStr : groups ) {
				try {
					long id = Long.parseLong(idStr);
					GroupBean g = groupid_to_group.get(id);
					if( g != null ) { 
						out.add(g);
					} else { 
						logger.warning("Swarm has nonexistent group: " + id);
					}
				} catch( NumberFormatException e ) { 
					logger.warning("Malformed group id during parse of " + dm.getDisplayName() + " / " + inHexHash);
					e.printStackTrace();
				}
			}
		} catch( Exception e ) { 
			logger.warning("Error reading permissions for: " + dm.getDisplayName() + " / " + e.toString());
			e.printStackTrace();
		}
		
		return out;
	}
	
	public synchronized void setGroupsForHash( String inHexHash, final ArrayList<GroupBean> inGroups, boolean addingDL ) {
		setGroupsForHash( inHexHash, inGroups, addingDL, null );
	}
	
	private synchronized void setGroupsForHash( String inHexHash, final ArrayList<GroupBean> inGroups, boolean addingDL, DownloadManager inDM ) {
		
		logger.fine("setGroupsForHash v2 -- " + inHexHash + " w/ " + inGroups.size() + " groups");
		
		DownloadManager dm = null;
		if( inDM != null ) {
			dm = inDM;
		} else {
			dm = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(ByteFormatter.decodeString(inHexHash)));
		}
		
		if( dm == null ) {
			if( !addingDL ) { 
				logger.warning("Tried to set groups for hash with null download manager: " + inHexHash);
				return;
			}
			
			temporaryHash_to_groups.put(inHexHash, inGroups);
			logger.finest("setGroupsForHash -- used temporary storage while adding.");
			return;
		}
		
		List<GroupBean> existing = getGroupsForHash(inHexHash);
		boolean _started_with_public = false;
		if (existing != null) {
			if (existing.contains(GroupBean.PUBLIC)) {
				logger.fine(inHexHash + " started with public");
				_started_with_public = true;
			}
		}
		final boolean started_with_public = _started_with_public;
		
		List<String> idsList = new ArrayList<String>();
		for( GroupBean g : inGroups ) { 
			if( g.getGroupID() <= 0 ) { 
				logger.warning("setGroupsForHash_v2 called with group having invalid id! " + g.getGroupName() + "  -- skipping.");
				continue;
			}
			
			idsList.add(g.getGroupID()+"");
		}
		
		dm.getDownloadState().setListAttribute(TOTorrentImpl.OS_PERMISSIONS, idsList.toArray(new String[0]));
		dm.getDownloadState().save();
		
		if( idsList.size() == 0 ) {
			dm.setData(NO_PERMISSIONS_DATA, Boolean.TRUE);
		} else {
			dm.setData(NO_PERMISSIONS_DATA, null);
		}
		
		logger.finer("Set perms property, now checking start/stop and network enabling.");
		
		/**
		 * If this includes either of the special groups, we need to update
		 * these ourselves immediately (these are not on demand)
		 */
		boolean any_friend = false;
		for (GroupBean g : inGroups) {
			if (g.equals(GroupBean.PUBLIC) == false) {
				any_friend = true;
			}
		}

		/*
		 * sync up the enabled networks
		 */
		if (dm != null) {

			logger.fine("setTorrentPrivacy: " + inHexHash + " pub?: " + Boolean.toString(inGroups.contains(GroupBean.PUBLIC)) + " f2f?: " + Boolean.toString(any_friend));

			setTorrentPrivacy( dm, inGroups.contains(GroupBean.PUBLIC), any_friend );
			
		}
		/**
		 * If we add/remove the public network we need to restart the torrent
		 */
		if (started_with_public != inGroups.contains(GroupBean.PUBLIC)) {
			logger.fine("changing public visibility requires stop/start");
			if (dm != null) {
				if (dm.getState() != DownloadManager.STATE_STOPPED || dm.getState() != DownloadManager.STATE_STOPPING) {
					dm.addListener(new DownloadManagerListener() {
						@Override
                        public void completionChanged(DownloadManager manager, boolean completed) {
						}

						@Override
                        public void downloadComplete(DownloadManager manager) {
						}

						@Override
                        public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
						}

						@Override
                        public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
						}

						@Override
                        public void stateChanged(final DownloadManager manager, int state) {
							if (state == DownloadManager.STATE_QUEUED) {
								DownloadManagerStarter.startDownload(manager, new DownloadManagerStartListener() {
									@Override
                                    public void downloadStarted() {
										if (started_with_public == false && inGroups.contains(GroupBean.PUBLIC) == true) {
											logger.finer("public network added, forcing tracker update");
											manager.getTrackerClient().update(true);
										}
									}
								});
							}
						}
					});
					logger.fine("stopping download: " + dm.getDisplayName());
					dm.stopIt(DownloadManager.STATE_QUEUED, false, false);
				}
			}
		}

		refreshFileLists();
	}

	
	public synchronized GroupBean getGroup( long inID ) { 
		return groupid_to_group.get(inID);
	}
	
	public synchronized GroupBean getUserGroup( String inBase64Key ) { 
		Long groupid = base64Key_to_groupid.get(inBase64Key);
		if( groupid == null ) {
			return null;
		}
		return groupid_to_group.get(groupid);
	}

	public synchronized Collection<GroupBean> getAllGroups() {
		return Arrays.asList(groupid_to_group.values().toArray(new GroupBean[0]));
	}

	public synchronized boolean hasPermissions(String inBase64Key, byte[] inSwarmHash) {
		String hashStr = ByteFormatter.encodeString(inSwarmHash);
		List<GroupBean> groups = getGroupsForHash(hashStr);
		
		for( GroupBean g : groups ) {
			if( g.equals(GroupBean.ALL_FRIENDS) ) {
				return true;
			}
			
			if( g.getMemberKeys().contains(inBase64Key) ) {
				return true;
			}
		}
		
		return false;
	}

	public synchronized boolean hasPermissions(byte[] inKey, byte[] inSwarmHash) {
		return hasPermissions(new String(Base64.encode(inKey)), inSwarmHash);
	}
	
	public void setTorrentPrivacy(DownloadManager dm, boolean publicNet, boolean f2fNet) {
		logger.fine("setting torrent privacy: pub=" + publicNet + " f2f=" + f2fNet);
		if (publicNet && f2fNet) {
			logger.fine("setTorrentPublic");
			setTorrentPublic(dm);
		} else if (publicNet && !f2fNet) {
			logger.fine("setTorrentInternetOnly");
			setTorrentInternetOnly(dm);
		} else if (!publicNet && f2fNet) {
			logger.fine("setTorrentFriendsOnly");
			setTorrentFriendsOnly(dm);
		} else if (!publicNet && !f2fNet) {
			logger.fine("setTorrentPrivate");
			setTorrentPrivate(dm);
		}
		refreshFileLists();
	}

	private static void setTorrentPublic(DownloadManager dm) {
		logger.fine("Setting torrent Public");
		DownloadManagerState state = dm.getDownloadState();

		// enable all peer sources
		for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
			String source = PEPeerSource.PS_SOURCES[i];
			state.setPeerSourceEnabled(source, true);
		}

		// enable all networks
		for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
			String network = AENetworkClassifier.AT_NETWORKS[i];
			state.setNetworkEnabled(network, true);
		}
	}
	
	private static void setTorrentInternetOnly(DownloadManager dm) {
		logger.fine("Setting torrent Internet Only");

		DownloadManagerState state = dm.getDownloadState();

		// enable all but f2f
		for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
			String source = PEPeerSource.PS_SOURCES[i];
			if (!source.equals(PEPeerSource.PS_OSF2F)) {
				state.setPeerSourceEnabled(source, true);
			} else {
				state.setPeerSourceEnabled(source, false);
			}
		}

		for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
			String network = AENetworkClassifier.AT_NETWORKS[i];
			if (!network.equals(AENetworkClassifier.AT_OSF2F)) {
				state.setNetworkEnabled(network, true);
			} else {
				state.setNetworkEnabled(network, false);
			}
		}
	}

	private static void setTorrentPrivate(DownloadManager dm) {
		logger.fine("Setting torrent Private");

		DownloadManagerState state = dm.getDownloadState();

		// disable all
		for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
			String source = PEPeerSource.PS_SOURCES[i];
			state.setPeerSourceEnabled(source, false);
		}

		for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
			String network = AENetworkClassifier.AT_NETWORKS[i];

			state.setNetworkEnabled(network, false);

		}
	}

	private static void setTorrentFriendsOnly(DownloadManager dm) {
		logger.fine("Setting torrent Friends Only");

		DownloadManagerState state = dm.getDownloadState();
		
		// set the peer sources to only be f2f
		for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
			String source = PEPeerSource.PS_SOURCES[i];
			if (source.equals(PEPeerSource.PS_OSF2F)) {
				state.setPeerSourceEnabled(source, true);
			} else {
				state.setPeerSourceEnabled(source, false);
			}
		}

		for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
			String network = AENetworkClassifier.AT_NETWORKS[i];
			if (network.equals(AENetworkClassifier.AT_OSF2F)) {
				state.setNetworkEnabled(network, true);
			} else {
				state.setNetworkEnabled(network, false);
			}
		}

		logger.fine("done setting friends only");
		
	}

	public static final void main(String[] args) {

	}
}
