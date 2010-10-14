package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public interface OneSwarmUIService extends RemoteService {
	public Boolean startBackend() throws OneSwarmException;

	public String getVersion(String session);

	public String selectFileOrDirectory(String session, boolean directory);

	public Boolean createSwarmFromLocalFileSystemPath(String session, String basePath, ArrayList<String> paths, boolean startSeeding, String announce, ArrayList<PermissionsGroup> inPermittedGroups);

	public ReportableException reportError(ReportableException inError);

	public String ping(String session, String latestVersion) throws Exception;

	public TorrentList getTorrentsInfo(String session, int page);

	public TorrentList getTransferringInfo(String session);

	public HashMap<String, String> getSidebarStats(String session);

	public HashMap<String, String> getDataStats(String session);

	public HashMap<String, String> getLimits(String session);

	public HashMap<String, String> getCounts(String session);

	public boolean getStopped(String session);

	public boolean recentFriendChanges(String session);

	public void setRecentChanges(String session, boolean value);

	public void setLimits(String session, String day, String week, String month, String year);

	public String[] checkIfWarning(String session);

	public void resetLimit(String session, String limittype);

	public boolean startTorrent(String session, String[] torrentIDs);

	public boolean stopTorrent(String session, String[] torrenIDs);

	public int downloadTorrent(String session, String path);

	public void addDownloadFromLocalTorrentDefaultSaveLocation(String session, String inPathToTorrent, ArrayList<PermissionsGroup> inPermissions) throws OneSwarmException;

	// public void addDownloadFromLocalTorrent(String session, String path,
	// String savePath, boolean skipCheck, ArrayList<PermissionsGroup> inPermissions)
	// throws OneSwarmException;

	public int downloadTorrent(String session, int friendConnection, int channelId, String torrentId, int lengthHint);

	public Integer getTorrentDownloadProgress(String session, int torrentDownloadID);

	public FileListLite[] getTorrentFiles(String session, int torrentDownloadID);

	public Boolean addTorrent(String session, int torrentDownloadID, FileListLite[] selectedFiles, ArrayList<PermissionsGroup> inPerms, String path, boolean noStream);

	public Boolean torrentExists(String session, String torrentID);

	public boolean deleteData(String session, String[] torrentID);

	public ReportableException deleteFromShareKeepData(String session, String[] torrentID);

	public ReportableException deleteCompletely(String session, String[] torrentID);

	public void addFriend(String session, FriendInfoLite friendInfoLite, boolean testOnly) throws OneSwarmException;

	public FriendInfoLite[] scanXMLForFriends(String session, String text) throws OneSwarmException;

	public void applySwarmPermissionChanges(String session, ArrayList<TorrentInfo> inSwarms);

	public FriendList getFriends(String session, int prevListId, boolean includeDisconnected, boolean includeBlocked);

	public int getNumberFriendsCount(String session);

	public String getMyPublicKey(String session);

	public FileListLite[] getFileList(String session, int connectionId, String filter, int startNum, int num, long maxCacheAge);

	public Integer sendSearch(String session, String searchString);

	public TextSearchResultLite[] getSearchResult(String session, int searchId);

	public ReportableException revealSwarmInFinder(String session, TorrentInfo[] inSwarm);

	// public void revealPathInFinder(String session, String path);

	public ReportableException openFileDefaultApp(String session, TorrentInfo[] inSwarm);

	public FileTree getFiles(String session, String path);

	public ArrayList<HashMap<String, String>> getFriendTransferStats(String session);

	public void setFriendsSettings(String session, FriendInfoLite[] updated);

	public FriendList getPendingCommunityFriendImports(String session) throws OneSwarmException;

	public FriendInfoLite[] getNewUsersFromXMPP(String session, String xmppNetworkName, String username, char[] password, String machineName) throws OneSwarmException;

	public int pollCommunityServer(String session, CommunityRecord record) throws OneSwarmException;

	public HashMap<String, Integer> getTorrentsState(String session);

	public String getComputerName(String session);

	public void setComputerName(String session, String computerName);

	public TorrentInfo[] pagedTorrentStateRefresh(String session, ArrayList<String> whichOnes);

	public Integer getIntegerParameterValue(String session, String inParamName);

	public void setIntegerParameterValue(String session, String inParamName, Integer inValue);

	public Boolean getBooleanParameterValue(String session, String inParamName);

	public void setBooleanParameterValue(String session, String inParamName, Boolean inValue);

	public String getStringParameterValue(String session, String inParamName);

	public void setStringParameterValue(String session, String inParamName, String inValue);

	public ArrayList<String> getStringListParameterValue(String session, String inParamName);

	public void setStringListParameterValue(String session, String inParamName, ArrayList<String> value);

	public int getDownloadManagersCount(String session);

	public PagedTorrentInfo getPagedAndFilteredSwarms(int inPage, int swarmsPerPage, String filter, int sort, String type, boolean includeF2F, int selectedFriendID, String inTagPath);

	public FileListLite[] getFilesForDownloadingTorrentHash(String session, String inOneSwarmHash);

	public String getTorrentName(String session, int inID);

	public ReportableException updateSkippedFiles(String session, FileListLite[] lites);

	public ArrayList<PermissionsGroup> getAllGroups(String session);

	public ArrayList<FriendInfoLite> getFriendsForGroup(String session, PermissionsGroup inGroup);

	public ArrayList<PermissionsGroup> getGroupsForSwarm(String session, TorrentInfo inSwarm);

	public ReportableException setGroupsForSwarm(String session, TorrentInfo inSwarm, ArrayList<PermissionsGroup> inGroups);

	public PermissionsGroup updateGroupMembership(String session, PermissionsGroup inGroup, ArrayList<FriendInfoLite> inMembers) throws OneSwarmException;

	public ReportableException removeGroup(String session, Long inGroupID);

	public void connectToFriends(String session, FriendInfoLite[] friendLite);

	public FriendInfoLite getUpdatedFriendInfo(String session, FriendInfoLite friendLite);

	public BackendTask[] getBackendTasks(String session);

	public BackendTask getBackendTask(String session, int inID);

	public void cancelBackendTask(String session, int inID);

	/**
	 * 
	 */
	public String debug(String session, String which);

	public FriendInfoLite[] getLanOneSwarmUsers(String session);

	public HashMap<String, String> getDeniedIncomingConnections(String session);

	public String getRemoteAccessUserName(String session);

	public String saveRemoteAccessCredentials(String session, String username, String password);

	public String[] getListenAddresses(String session);

	public HashMap<String, Integer> getNewFriendsCountsFromAutoCheck(String session);

	public String getPlatform(String session);

	public void deleteFriends(String session, FriendInfoLite[] friend);

	public void addToIgnoreRequestList(String session, FriendInfoLite friend);

	public String getGtalkStatus(String session);

	public FileTree getAllTags(String session);

	public FileTree getTags(String session, String inOneSwarmHash) throws OneSwarmException;

	public void setTags(String session, String inOneSwarmHash, String[] path);

	public FriendInfoLite getSelf(String session);

	public HashMap<String, String[]> getUsersWithMessages(String session);

	public HashMap<String, Integer> getUnreadMessageCounts(String session);

	public SerialChatMessage[] getMessagesForUser(String session, String base64PublicKey, boolean include_read, int limit);

	public boolean sendChatMessage(String session, String base64PublicKey, SerialChatMessage message) throws OneSwarmException;

	public int clearChatLog(String session, String base64PublicKey);

	public void updateRemoteAccessIpFilter(String session, String selectedFilterType, String filterString) throws OneSwarmException;

	public ArrayList<BackendErrorReport> getBackendErrors(String session);

	public String getDebugMessageLog(String session, String friendPublicKey);

	public String[] getBase64HashesForOneSwarmHashes(String session, String[] inOneSwarmHashes);

	public String[] getBase64HashesForBase32s(String session, String[] inBase32s) throws OneSwarmException;

	public FriendInvitationLite createInvitation(String session, String name, boolean canSeeFileList, long maxAge, SecurityLevel securityLevel);

	public void redeemInvitation(String session, FriendInvitationLite invitation, boolean testOnly) throws OneSwarmException;

	public ArrayList<FriendInvitationLite> getSentFriendInvitations(String session);

	public ArrayList<FriendInvitationLite> getRedeemedFriendInvitations(String session);

	public void updateFriendInvitations(String sessionID, FriendInvitationLite invitation);

	public void deleteFriendInvitations(String sessionID, ArrayList<FriendInvitationLite> invitations);

	public String copyTorrentInfoToMagnetLink(String sessionID, String[] torrentIDs) throws OneSwarmException;

	public void refreshFileAssociations(String session) throws OneSwarmException;

	public LocaleLite[] getLocales(String session);

	public HashMap<String, String> getFileInfo(String session, FileListLite file, boolean getFFmpegData) throws OneSwarmException;

	public void applyDefaultSettings(String session);

	int getNumberOnlineFriends(String session);

	BackendTask performSpeedCheck(String session, double setWithFraction);

	BackendTask publishSwarms(String session, TorrentInfo[] infos, String[] previewPaths, String[] comments, String[] categories, CommunityRecord toServer);

	ArrayList<String> getCategoriesForCommunityServer(String sessionID, CommunityRecord selected);

	void triggerNatCheck(String sessionID);

	HashMap<String, String> getNatCheckResult(String sessionID);

	void fixPermissions(String session, TorrentInfo torrent, boolean inFixAll) throws OneSwarmException;

	Boolean isStreamingDownload(String session, String infohash);

	void setStreamingDownload(String session, String infohash, boolean streaming);
	
	String getMultiTorrentSourceTemp(String session);
}
