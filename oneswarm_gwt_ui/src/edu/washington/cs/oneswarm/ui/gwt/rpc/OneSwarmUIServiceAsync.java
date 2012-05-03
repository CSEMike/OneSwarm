package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;
import edu.washington.cs.oneswarm.ui.gwt.shared.fileDialog.FileItem;

public interface OneSwarmUIServiceAsync {
    public void startBackend(AsyncCallback callback);

    public void getVersion(String session, AsyncCallback<String> callback);

    /*void selectFileOrDirectory(String session, boolean directory,
			AsyncCallback<String> callback);*/

    public void createSwarmFromLocalFileSystemPath(String session, String basePath,
            ArrayList<String> path, boolean startSeeding, String announce,
            ArrayList<PermissionsGroup> inPermittedGroups, AsyncCallback<Boolean> callback);

    public void reportError(ReportableException inError, AsyncCallback callback);

    public void getTorrentsInfo(String session, int page, AsyncCallback callback);

    public void getTransferringInfo(String session, AsyncCallback<TorrentList> inCallback);

    public void getStopped(String session, AsyncCallback<Boolean> inCallback);

    public void recentFriendChanges(String session, AsyncCallback<Boolean> inCallback);

    public void setRecentChanges(String session, boolean value, AsyncCallback callback);

    public void getSidebarStats(String session, AsyncCallback<HashMap<String, String>> inCallback);

    public void getLimits(String session, AsyncCallback<HashMap<String, String>> inCallback);

    public void getDataStats(String session, AsyncCallback<HashMap<String, String>> inCallback);

    public void getCounts(String session, AsyncCallback<HashMap<String, String>> inCallback);

    public void resetLimit(String session, String limittype, AsyncCallback callback);

    public void checkIfWarning(String session, AsyncCallback<String[]> inCallback);

    public void setLimits(String session, String day, String week, String month, String year,
            AsyncCallback callback);

    public void ping(String session, String version, AsyncCallback<String> callback);

    public void startTorrent(String session, String[] torrentID, AsyncCallback<Boolean> callback);

    public void stopTorrent(String session, String[] torrenID, AsyncCallback<Boolean> callback);

    public void downloadTorrent(String session, String path, AsyncCallback<Integer> callback);

    public void downloadTorrent(String session, int friendConnection, int channelId,
            String torrentId, int lengthHint, AsyncCallback<Integer> callback);

    public void addDownloadFromLocalTorrentDefaultSaveLocation(String session,
            String inPathToTorrent, ArrayList<PermissionsGroup> inPermissions,
            AsyncCallback<Void> callback);

    // public void addDownloadFromLocalTorrent(String session, String path,
    // String savePath, boolean skipCheck, ArrayList<PermissionsGroup>
    // inPermissions,
    // AsyncCallback<Void> callback);

    public void getTorrentDownloadProgress(String session, int torrentDownloadID,
            AsyncCallback<Integer> callback);

    public void getTorrentFiles(String session, int torrentDownloadID,
            AsyncCallback<FileListLite[]> callback);

    public void getTorrentName(String session, int inID, AsyncCallback<String> callback);

    public void addTorrent(String session, int torrentDownloadID, FileListLite[] selectedFiles,
            ArrayList<PermissionsGroup> inPerms, String path, boolean noStream,
            AsyncCallback<Boolean> callback);

    public void torrentExists(String session, String torrentID, AsyncCallback<Boolean> callback);

    public void deleteData(String session, String[] torrentID, AsyncCallback<Boolean> callback);

    public void deleteFromShareKeepData(String session, String[] torrentID,
            AsyncCallback<ReportableException> callback);

    public void deleteCompletely(String session, String[] torrentID,
            AsyncCallback<ReportableException> callback);

    public void addFriend(String session, FriendInfoLite friendInfoLite, boolean testOnly,
            AsyncCallback<Void> callback);

    public void scanXMLForFriends(String session, String text,
            AsyncCallback<FriendInfoLite[]> callback);

    public void applySwarmPermissionChanges(String session, ArrayList<TorrentInfo> inSwarms,
            AsyncCallback<Void> callback);

    public void getFriends(String session, int prevListId, boolean includeDisconnected,
            boolean includeBlocked, AsyncCallback<FriendList> callback);

    public void getMyPublicKey(String session, AsyncCallback<String> callback);

    public void getFileList(String session, int connectionId, String filter, int startNum, int num,
            long maxCacheAge, AsyncCallback<FileListLite[]> callback);

    public void sendSearch(String session, String searchString, AsyncCallback<Integer> callback);

    public void revealSwarmInFinder(String session, TorrentInfo[] inSwarm,
            AsyncCallback<ReportableException> callback);

    // public void revealPathInFinder(String session, String path, AsyncCallback
    // callback);

    public void openFileDefaultApp(String session, TorrentInfo[] inSwarm,
            AsyncCallback<ReportableException> callback);

    public void getSearchResult(String session, int searchId,
            AsyncCallback<TextSearchResultLite[]> callback);

    public void getFiles(String session, String path, AsyncCallback callback);

    public void getFriendTransferStats(String session,
            AsyncCallback<ArrayList<HashMap<String, String>>> callback);

    public void setFriendsSettings(String session, FriendInfoLite[] updated,
            AsyncCallback<Void> callback);

    public void getPendingCommunityFriendImports(String session, AsyncCallback<FriendList> callback);

    public void getNewUsersFromXMPP(String session, String xmppNetworkName, String username,
            char[] password, String machineName, AsyncCallback<FriendInfoLite[]> callback);

    public void pollCommunityServer(String session, CommunityRecord record,
            AsyncCallback<Integer> callback);

    public void getTorrentsState(String session, AsyncCallback<HashMap<String, Integer>> callback);

    public void getComputerName(String session, AsyncCallback<String> callback);

    public void setComputerName(String session, String computerName, AsyncCallback<Void> callback);

    public void pagedTorrentStateRefresh(String session, ArrayList<String> whichOnes,
            AsyncCallback<TorrentInfo[]> callback);

    public void getIntegerParameterValue(String session, String inParamName,
            AsyncCallback<Integer> callback);

    public void setIntegerParameterValue(String session, String inParamName, Integer inValue,
            AsyncCallback<Void> callback);

    public void getBooleanParameterValue(String session, String inParamName,
            AsyncCallback<Boolean> callback);

    public void setBooleanParameterValue(String session, String inParamName, Boolean inValue,
            AsyncCallback<Void> callback);

    public void getStringParameterValue(String session, String inParamName,
            AsyncCallback<String> callback);

    public void setStringParameterValue(String session, String inParamName, String inValue,
            AsyncCallback<Void> callback);

    public void getStringListParameterValue(String session, String inParamName,
            AsyncCallback<ArrayList<String>> callback);

    public void setStringListParameterValue(String session, String inParamName,
            ArrayList<String> value, AsyncCallback<Void> callback);

    public void getDownloadManagersCount(String session, AsyncCallback<Integer> callback);

    public void getPagedAndFilteredSwarms(int inPage, int swarmsPerPage, String filter, int sort,
            String type, boolean includeF2F, int inSelectedFriendID, String inTagPath,
            AsyncCallback<PagedTorrentInfo> callback);

    public void getFilesForDownloadingTorrentHash(String session, String inOneSwarmHash,
            AsyncCallback<FileListLite[]> callback);

    public void updateSkippedFiles(String session, FileListLite[] lites,
            AsyncCallback<ReportableException> callback);

    public void getAllGroups(String session, AsyncCallback<ArrayList<PermissionsGroup>> callback);

    public void getFriendsForGroup(String session, PermissionsGroup inGroup,
            AsyncCallback<ArrayList<FriendInfoLite>> callback);

    public void getGroupsForSwarm(String session, TorrentInfo inSwarm,
            AsyncCallback<ArrayList<PermissionsGroup>> callback);

    public void setGroupsForSwarm(String session, TorrentInfo inSwarm,
            ArrayList<PermissionsGroup> inGroups, AsyncCallback<ReportableException> callback);

    public void updateGroupMembership(String session, PermissionsGroup inGroup,
            ArrayList<FriendInfoLite> inMembers, AsyncCallback<PermissionsGroup> callback);

    public void removeGroup(String session, Long inGroupID,
            AsyncCallback<ReportableException> callback);

    public void connectToFriends(String session, FriendInfoLite[] friendLite,
            AsyncCallback<Void> callback);

    public void getUpdatedFriendInfo(String session, FriendInfoLite friendLite,
            AsyncCallback<FriendInfoLite> callback);

    public void getBackendTasks(String session, AsyncCallback<BackendTask[]> callback);

    public void getBackendTask(String session, int inID, AsyncCallback<BackendTask> callback);

    public void cancelBackendTask(String session, int inID, AsyncCallback<Void> callback);

    public void debug(String session, String which, AsyncCallback<String> callback);

    public void getLanOneSwarmUsers(String session, AsyncCallback<FriendInfoLite[]> callback);

    public void getRemoteAccessUserName(String session, AsyncCallback<String> callback);

    public void saveRemoteAccessCredentials(String session, String username, String password,
            AsyncCallback<String> callback);

    public void getListenAddresses(String session, AsyncCallback<String[]> asyncCallback);

    public void getNewFriendsCountsFromAutoCheck(String session,
            AsyncCallback<HashMap<String, Integer>> callback);

    public void getDeniedIncomingConnections(String session,
            AsyncCallback<HashMap<String, String>> callback);

    public void getPlatform(String session, AsyncCallback<String> callback);

    public void deleteFriends(String session, FriendInfoLite[] friend, AsyncCallback<Void> callback);

    public void addToIgnoreRequestList(String session, FriendInfoLite friend,
            AsyncCallback<Void> callback);

    public void getGtalkStatus(String session, AsyncCallback<String> callback);

    public void getAllTags(String session, AsyncCallback<FileTree> callback);

    public void getTags(String session, String inOneSwarmHash, AsyncCallback<FileTree> callback);

    public void setTags(String session, String inOneSwarmHash, String[] path,
            AsyncCallback<Void> callback);

    public void getSelf(String session, AsyncCallback<FriendInfoLite> callback);

    public void getUsersWithMessages(String session,
            AsyncCallback<HashMap<String, String[]>> callback);

    public void getUnreadMessageCounts(String session,
            AsyncCallback<HashMap<String, Integer>> callback);

    public void getMessagesForUser(String session, String base64PublicKey, boolean include_read,
            int limit, AsyncCallback<SerialChatMessage[]> callback);

    public void sendChatMessage(String session, String base64PublicKey, SerialChatMessage message,
            AsyncCallback<Boolean> callback);

    public void clearChatLog(String session, String base64PublicKey, AsyncCallback<Integer> callback);

    public void updateRemoteAccessIpFilter(String session, String selectedFilterType,
            String filterString, AsyncCallback<Void> callback);

    public void getBackendErrors(String session,
            AsyncCallback<ArrayList<BackendErrorReport>> callback);

    public void getDebugMessageLog(String session, String friendPublicKey,
            AsyncCallback<String> callback);

    public void getBase64HashesForOneSwarmHashes(String session, String[] inOneSwarmHashes,
            AsyncCallback<String[]> callback);

    public void getBase64HashesForBase32s(String session, String[] inBase32s,
            AsyncCallback<String[]> callback);

    public void createInvitation(String session, String name, boolean canSeeFileList, long maxAge,
            SecurityLevel securityLevel, AsyncCallback<FriendInvitationLite> callback);

    public void redeemInvitation(String session, FriendInvitationLite invitation, boolean testOnly,
            AsyncCallback<Void> callback);

    public void getSentFriendInvitations(String session,
            AsyncCallback<ArrayList<FriendInvitationLite>> callback);

    public void getRedeemedFriendInvitations(String session,
            AsyncCallback<ArrayList<FriendInvitationLite>> callback);

    public void updateFriendInvitations(String sessionID, FriendInvitationLite invitation,
            AsyncCallback<Void> asyncCallback);

    public void deleteFriendInvitations(String sessionID,
            ArrayList<FriendInvitationLite> invitations, AsyncCallback<Void> asyncCallback);

    public void copyTorrentInfoToMagnetLink(String sessionID, String[] torrentIDs,
            AsyncCallback<String> asyncCallback);

    public void refreshFileAssociations(String session, AsyncCallback<Void> callback);

    public void getLocales(String session, AsyncCallback<LocaleLite[]> callback);

    public void getFileInfo(String session, FileListLite file, boolean getMediaInfo,
            AsyncCallback<HashMap<String, String>> callback);

    public void performSpeedCheck(String session, double setWithFraction,
            AsyncCallback<BackendTask> callback);

    void applyDefaultSettings(String session, AsyncCallback<Void> callback);

    public void getNumberFriendsCount(String session, AsyncCallback<Integer> callback);

    void getNumberOnlineFriends(String session, AsyncCallback<Integer> callback);

    void publishSwarms(String session, TorrentInfo[] infos, String[] previewPaths,
            String[] comments, String[] categories, CommunityRecord toServer,
            AsyncCallback<BackendTask> callback);

    public void getCategoriesForCommunityServer(String sessionID, CommunityRecord selected,
            AsyncCallback<ArrayList<String>> asyncCallback);

    void triggerNatCheck(String sessionID, AsyncCallback<Void> callback);

    void getNatCheckResult(String sessionID, AsyncCallback<HashMap<String, String>> callback);

    public void fixPermissions(String sessionID, TorrentInfo torrent, boolean inFixAll,
            AsyncCallback<Void> asyncCallback);

    void isStreamingDownload(String session, String infohash, AsyncCallback<Boolean> callback);

    void setStreamingDownload(String session, String infohash, boolean streaming,
            AsyncCallback<Void> callback);

    void getMultiTorrentSourceTemp(String session, AsyncCallback<String> callback);

    void getClientServices(AsyncCallback<ArrayList<ClientServiceDTO>> callback);

    void getSharedServices(AsyncCallback<ArrayList<SharedServiceDTO>> callback);

    void saveClientServices(ArrayList<ClientServiceDTO> services, AsyncCallback<Void> callback);

    void saveSharedServices(ArrayList<SharedServiceDTO> services, AsyncCallback<Void> callback);

	void listFiles(String session, String string, AsyncCallback<FileItem[]> callback);


}