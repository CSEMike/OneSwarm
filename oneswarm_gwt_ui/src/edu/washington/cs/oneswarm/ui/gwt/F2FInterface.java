package edu.washington.cs.oneswarm.ui.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.FileList;
import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.FriendConnectListener;
import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.TextSearchResult;
import edu.washington.cs.oneswarm.f2f.share.ShareManagerTools;
import edu.washington.cs.oneswarm.plugins.PluginCallback;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.server.FriendInfoLiteFactory;

public class F2FInterface {

    private IPCInterface f2fIpc;

    public F2FInterface(final PluginInterface pi) {
        PluginInterface f2fIf = pi.getPluginManager().getPluginInterfaceByID("osf2f");
        if (f2fIf != null && f2fIf.isOperational()) {
            f2fIpc = f2fIf.getIPC();
        } else {
            Debug.out("Friend to friend plugin not available yet, will try again later");
            Thread t = new Thread(new Runnable() {

                public void run() {
                    try {
                        int tryNum = 1;
                        while (f2fIpc == null) {
                            Thread.sleep(2000);
                            Debug.out("trying to load f2f stuff again, try num=" + tryNum);
                            PluginInterface f2fIf = pi.getPluginManager().getPluginInterfaceByID(
                                    "osf2f");
                            if (f2fIf != null && f2fIf.isOperational()) {
                                f2fIpc = f2fIf.getIPC();
                            } else {
                                Debug.out("Friend to friend plugin not available, trynum=" + tryNum
                                        + " failed! " + f2fIf);
                            }
                            tryNum++;
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    public boolean f2fPluginLoaded() {
        return f2fIpc != null;
    }

    public FriendInfoLite[] getFriends() {
        return getFriends(true, true);
    }

    // public FriendInfoLite[] getOnlineFriends() {
    // return getFriends(true, false);
    // }

    @SuppressWarnings("unchecked")
    public FriendInfoLite[] getFriends(boolean includeDisconnected, boolean includeBlocked) {
        if (f2fIpc != null) {
            try {
                List<Friend> friends = (List<Friend>) f2fIpc.invoke("getFriends", new Object[0]);
                List<FriendInfoLite> selectedFriends = new LinkedList<FriendInfoLite>();
                for (Friend f : friends) {
                    if (!f.isBlocked() || includeBlocked)
                        switch (f.getStatus()) {
                        case Friend.STATUS_OFFLINE:
                            if (includeDisconnected) {
                                selectedFriends.add(FriendInfoLiteFactory.createFriendInfo(f));
                            }
                            break;
                        case Friend.STATUS_CONNECTING:
                            if (includeDisconnected) {
                                selectedFriends.add(FriendInfoLiteFactory.createFriendInfo(f));
                            }
                            break;

                        case Friend.STATUS_HANDSHAKING:
                            selectedFriends.add(FriendInfoLiteFactory.createFriendInfo(f));
                            break;

                        case Friend.STATUS_ONLINE:
                            selectedFriends.add(FriendInfoLiteFactory.createFriendInfo(f));
                            break;
                        }
                }
                return selectedFriends.toArray(new FriendInfoLite[selectedFriends.size()]);
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new FriendInfoLite[0];
    }

    public int addFriend(Friend f) {
        if (f2fIpc != null) {
            try {
                return (Integer) f2fIpc.invoke("addFriend", new Object[] { f });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }

    public void connectToFriend(String friendsPublicKey) {
        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("connectToFriend", new Object[] { friendsPublicKey });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void disconnectFriend(Friend inFriend) {
        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("disconnectFriend", new Object[] { inFriend });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void sendChatMessage(Friend inFriend, String inPlaintext) {
        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("sendChatToFriend", new Object[] { inFriend, inPlaintext });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public Friend getFriend(String friendsPublicKey) {
        if (f2fIpc != null) {
            try {
                return (Friend) f2fIpc.invoke("getFriend", new Object[] { friendsPublicKey });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        return null;

    }

    public String getMyPublicKey() {
        if (f2fIpc != null) {
            try {
                return (String) f2fIpc.invoke("getMyPublicKey", new Object[0]);
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public String getGtalkStatus() {
        if (f2fIpc != null) {
            try {
                return (String) f2fIpc.invoke("getGtalkStatus", new Object[0]);
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Blocking method
     * 
     * @param connectionId
     * @param filter
     * @param startNum
     * @param num
     * @return
     */
    public List<FileListLite> getFileList(int connectionId, String filter, int startNum, int num,
            long maxCacheAge) {

        // array of pointers to FileList
        final FileList[] result = new FileList[1];
        final Semaphore resultWaiter = new Semaphore(0);

        PluginCallback<FileList> callback = new PluginCallback<FileList>() {
            public void dataRecieved(long bytes) {
            }

            public void errorOccured(String e) {
                throw new RuntimeException("An error occured: " + e);
            }

            public void progressUpdate(int progress) {
            }

            public void requestCompleted(FileList data) {
                result[0] = data;
                resultWaiter.release();
            }
        };

        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("sendFileListRequest", new Object[] { connectionId, maxCacheAge,
                        callback });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }

        try {
            System.out.println("F2F interface waiting for FileList");
            resultWaiter.acquire();

            if (result == null) {
                throw new RuntimeException("F2F interface got FileList list = null");
            }

            System.out.println("F2F interface got FileList list len=" + result[0].getFileNum());

            FileList filtered;
            if (filter.equals("")) {
                filtered = result[0];
            } else {
                filtered = result[0].searchMatches(filter);
                System.out.println("filtering for '" + filter + "' got " + filtered.getFileNum()
                        + " matches");
            }
            List<FileListLite> converted = convertFileList(filtered);
            // return converted.subList(startNum, Math.min(startNum + num,
            // converted.size()));
            return groupAwareSubList(converted, startNum, num);

        } catch (InterruptedException e) {
            throw new RuntimeException("interupted during filelist request");
        }

    }

    private List<FileListLite> groupAwareSubList(List<FileListLite> inRawFilesList,
            int inStartGroup, int inMaxGroups) {
        int skippedGroups = 0;
        String lastSkippedID = "";
        int addedGroups = 0;
        String lastAddedID = "";
        boolean adding = false;
        List<FileListLite> outList = new ArrayList<FileListLite>();

        for (int fileItr = 0; fileItr < inRawFilesList.size(); fileItr++) {
            FileListLite f = inRawFilesList.get(fileItr);
            if (skippedGroups < inStartGroup) {
                /**
                 * hopefully the files list is grouped... (this should work
                 * otherwise, but will be weird)
                 */
                if (lastSkippedID.equals(f.getCollectionId()) == false) {
                    lastSkippedID = f.getCollectionId();
                    skippedGroups++;
                }
            } else {
                if (f.getCollectionId().equals(lastSkippedID) && !adding) // adding
                // is
                // in
                // case
                // list
                // is
                // not
                // sorted
                // by
                // group
                {
                    continue; // need to finish skipping everything here...
                } else {
                    adding = true;
                }

                if (lastAddedID.equals(f.getCollectionId()) == false) {
                    lastAddedID = f.getCollectionId();
                    addedGroups++;
                }

                if (addedGroups > inMaxGroups)
                    break;

                outList.add(f);
            }
        }

        return outList;
    }

    private static List<FileListLite> convertFileList(FileList fileList) {
        List<FileListLite> converted = new ArrayList<FileListLite>();

        // int totalFiles = (int)fileList.getFileNum();

        int totalGroups = fileList.getElements().size();
        for (FileCollection collection : fileList.getElements()) {

            String collectionName = collection.getName();
            String collectionId = collection.getUniqueID();

            List<FileListFile> files = collection.getChildren();

            for (FileListFile file : files) {
                FileListLite fll = new FileListLite(collectionId, collectionName,
                        file.getFileName(), file.getLength(), collection.getFileNum(),
                        collection.getAddedTimeUTC(), totalGroups, false, false);
                if (file.getSha1Hash() != null) {
                    fll.setSha1Hash(new String(Base64.encode(file.getSha1Hash())));
                }
                if (file.getEd2kHash() != null) {
                    fll.setEd2kHash(new String(ShareManagerTools.base16Encode(file.getEd2kHash())));
                }
                converted.add(fll);
            }
        }

        return converted;
    }

    public void getMetaInfo(int connectionId, int channelId, String torrentId, int lengthHint,
            PluginCallback<byte[]> callback) {
        if (f2fIpc != null) {
            try {
                // if the length of the infohash is known we can speed up things
                // one rtt, -1 means not known
                f2fIpc.invoke("sendMetaInfoRequest",
                        new Object[] { connectionId, channelId, Base64.decode(torrentId),
                                lengthHint, callback });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                callback.errorOccured(e.getMessage());
            }
        }
    }

    // /**
    // * Blocking
    // *
    // * @param connectionId
    // * @param channelId
    // * @param lengthHint
    // * @param torrentId
    // * @return
    // */
    // public byte[] getMetaInfo(int connectionId, int channelId, String
    // torrentId, int lengthHint) {
    //
    // // array of pointers to FileList
    // final byte[][] result = new byte[1][0];
    // final Semaphore resultWaiter = new Semaphore(0);
    //
    // PluginCallback<byte[]> callback = new PluginCallback<byte[]>() {
    // public void dataRecieved(long bytes) {
    // }
    //
    // public void errorOccured(String error) {
    // resultWaiter.release();
    // }
    //
    // public void progressUpdate(int progress) {
    // }
    //
    // public void requestCompleted(byte[] data) {
    // result[0] = data;
    // resultWaiter.release();
    // }
    // };
    //
    // this.getMetaInfo(connectionId, channelId, torrentId, lengthHint,
    // callback);
    //
    // try {
    // System.out.println("F2F interface waiting metainfo");
    // resultWaiter.acquire();
    // if (result != null) {
    // System.out.println("F2F interface got metainfo len=" + result[0].length);
    // return result[0];
    // } else {
    // System.out.println("F2F interface got null metainfo!");
    // }
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    //
    // return null;
    //
    // }

    public void setTorrentPrivacy(byte[] infohash, boolean publicNet, boolean f2fNet) {

        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("setTorrentPrivacy", new Object[] { infohash, publicNet, f2fNet });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public boolean isSharedWithPublic(byte[] infohash) {
        if (f2fIpc != null) {
            try {
                return (Boolean) f2fIpc.invoke("isSharedWithPublic", new Object[] { infohash });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean isSharedWithFriends(byte[] infohash) {
        if (f2fIpc != null) {
            try {
                return (Boolean) f2fIpc.invoke("isSharedWithFriends", new Object[] { infohash });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return true;
    }

    public int sendSearch(String search) {

        if (f2fIpc != null) {
            try {
                return (Integer) f2fIpc.invoke("sendTextSearch", new Object[] { search });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public List<TextSearchResult> getSearchResult(int searchId) {
        if (f2fIpc != null) {
            try {
                return (List<TextSearchResult>) f2fIpc.invoke("getTextSearchResult",
                        new Object[] { searchId });
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new ArrayList<TextSearchResult>();
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, String>[] getTransferStats() {
        if (f2fIpc != null) {
            try {
                return (HashMap<String, String>[]) f2fIpc.invoke("getTransferStats", new Object[0]);
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new HashMap[0];
    }

    public String[] getSupportedXMPPNetworks() {
        if (f2fIpc != null) {
            try {
                return (String[]) f2fIpc.invoke("getSupportedXMPPNetworks", new Object[0]);
            } catch (IPCException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new String[0];
    }

    @SuppressWarnings("unchecked")
    public FriendInfoLite[] getNewUsersFromXMPP(String xmppNetworkName, String username,
            char[] password, String machineName) throws Exception {

        List<Friend> friends = (List<Friend>) f2fIpc.invoke("getNewUsersFromXMPP", new Object[] {
                xmppNetworkName, username, password, machineName });
        if (friends == null) {
            return null;
        }
        FriendInfoLite[] fLite = new FriendInfoLite[friends.size()];
        for (int i = 0; i < fLite.length; i++) {
            Friend f = friends.get(i);
            fLite[i] = FriendInfoLiteFactory.createFriendInfo(f);
        }
        return fLite;

    }

    public void setFriendSettings(String publicKey, String nickname, boolean blocked,
            boolean canSeeFileList, boolean allowChat, boolean requestFileList, String group) {
        if (group == null) {
            group = "";
        }
        System.out.println(publicKey + " " + nickname + " " + blocked + " " + canSeeFileList + " "
                + allowChat);
        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("setFriendSettings", new Object[] { publicKey, nickname, blocked,
                        canSeeFileList, allowChat, requestFileList, group });
            } catch (IPCException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressWarnings("unchecked")
    public Map<Friend, FileList> getOnlineFileLists() {
        if (f2fIpc != null) {
            try {
                Map<Friend, FileList> f = (Map<Friend, FileList>) f2fIpc.invoke(
                        "getOnlineFileLists", new Object[0]);
                return f;
            } catch (IPCException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<Friend, FileList>();
    }

    public void registerForFriendConnectNotifications(FriendConnectListener callback) {

        if (f2fIpc != null) {
            try {
                f2fIpc.invoke("registerForFriendConnectNotifications", new Object[] { callback });
            } catch (IPCException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressWarnings("unchecked")
    public FriendInfoLite[] getLanOneSwarmUsers() {
        try {
            List<Friend> friends = (List<Friend>) f2fIpc.invoke("getLanOneSwarmUsers",
                    new Object[] {});
            if (friends == null) {
                return null;
            }
            FriendInfoLite[] fLite = new FriendInfoLite[friends.size()];
            for (int i = 0; i < fLite.length; i++) {
                Friend f = friends.get(i);
                fLite[i] = FriendInfoLiteFactory.createFriendInfo(f);
            }
            return fLite;
        } catch (IPCException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, String> getDeniedIncomingConnections() {
        try {
            HashMap<byte[], String> connections = (HashMap<byte[], String>) f2fIpc.invoke(
                    "getDeniedIncomingConnections", new Object[] {});
            HashMap<String, String> withBase64 = new HashMap<String, String>();
            for (byte[] pubkey : connections.keySet()) {
                withBase64.put(new String(Base64.encode(pubkey)), (connections.get(pubkey)));
            }
            return withBase64;
        } catch (IPCException e) {
            e.printStackTrace();
            return new HashMap<String, String>();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getNewFriendsCountsFromAutoCheck() {
        try {
            Map<String, Integer> counts = (Map<String, Integer>) f2fIpc.invoke(
                    "getNewFriendsCountsFromAutoCheck", new Object[] {});
            return counts;
        } catch (IPCException e) {
            e.printStackTrace();
            return new HashMap<String, Integer>();
        }
    }

    public void deleteFriend(FriendInfoLite friend) {
        try {
            f2fIpc.invoke("deleteFriend", new Object[] { friend.getPublicKey() });
        } catch (IPCException e) {
            e.printStackTrace();

        }
    }

    public void stopTransfers() {
        try {
            f2fIpc.invoke("stopTransfers", new Object[0]);
        } catch (IPCException e) {
            e.printStackTrace();
        }
    }

    public void restartTransfers() {
        try {
            f2fIpc.invoke("restartTransfers", new Object[0]);
        } catch (IPCException e) {
            e.printStackTrace();
        }
    }

    public void addToIgnoreRequestList(FriendInfoLite friend) {
        try {
            f2fIpc.invoke("addToIgnoreRequestList", new Object[] { friend.getPublicKey() });
        } catch (IPCException e) {
            e.printStackTrace();

        }
    }

    public String getDebugInfo() {
        try {
            return (String) f2fIpc.invoke("getDebugInfo", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }

    public String getDebugMessageLog(String friendPublicKey) {
        try {
            return (String) f2fIpc.invoke("getDebugMessageLog", new Object[] { friendPublicKey });
        } catch (IPCException e) {
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }

    public Friend[] scanXMLForFriends(String xml) {
        try {
            return (Friend[]) f2fIpc.invoke("scanXMLForFriends", new Object[] { xml });
        } catch (IPCException e) {
            e.printStackTrace();
            return null;
        }
    }

    public FriendInvitation createInvitation(String name, boolean canSeeFileList, long maxAge,
            byte securityLevel) {
        try {
            return (FriendInvitation) f2fIpc.invoke("createInvitation", new Object[] { name,
                    canSeeFileList, maxAge, securityLevel });
        } catch (IPCException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void redeemInvitation(FriendInvitation invitation, boolean testOnly) throws Exception {

        f2fIpc.invoke("redeemInvitation", new Object[] { invitation, testOnly });
    }

    public void updateInvitation(FriendInvitation invitation) {
        try {
            f2fIpc.invoke("updateInvitation", new Object[] { invitation });
        } catch (IPCException e) {
            e.printStackTrace();

        }
    }

    public FriendInvitation getInvitation(HashWrapper key) {
        try {
            return (FriendInvitation) f2fIpc.invoke("redeemInvitation", new Object[] { key });
        } catch (IPCException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<FriendInvitation> getLocallyCreatedInvitations() {
        try {
            return (List<FriendInvitation>) f2fIpc.invoke("getLocallyCreatedInvitations",
                    new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();
            return new LinkedList<FriendInvitation>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<FriendInvitation> getRedeemedInvitations() {
        try {
            return (List<FriendInvitation>) f2fIpc
                    .invoke("getRedeemedInvitations", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();
            return new LinkedList<FriendInvitation>();
        }
    }

    public void deleteInvitation(FriendInvitation invitation) {
        try {
            f2fIpc.invoke("deleteInvitation", new Object[] { invitation });
        } catch (IPCException e) {
            e.printStackTrace();

        }
    }

    public String getSearchDebugLog() {
        try {
            return (String) f2fIpc.invoke("getSearchDebugLog", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();

        }
        return "";
    }

    public String getLockDebug() {
        try {
            return (String) f2fIpc.invoke("getLockDebug", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();

        }
        return "";
    }

    public String getForwardQueueLengthDebug() {
        try {
            return (String) f2fIpc.invoke("getForwardQueueLengthDebug", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();

        }
        return "";
    }

    public int performSpeedCheck() {
        try {
            return (Integer) f2fIpc.invoke("performSpeedCheck", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();

        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Double> getSpeedCheckResult(int checkId) {
        try {
            return (HashMap<String, Double>) f2fIpc.invoke("getSpeedCheckResult",
                    new Object[] { checkId });
        } catch (IPCException e) {
            e.printStackTrace();

        }
        return null;
    }

    public void cancelSpeedCheck(int checkId) {
        try {
            f2fIpc.invoke("cancelSpeedCheck", new Object[] { checkId });
        } catch (IPCException e) {
            e.printStackTrace();

        }
    }

    public void triggerNATCheck() {
        try {
            f2fIpc.invoke("triggerNATCheck", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();

        }
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, String> getNatCheckResult() {
        try {
            return (HashMap<String, String>) f2fIpc.invoke("getNatCheckResult", new Object[] {});
        } catch (IPCException e) {
            e.printStackTrace();
        }
        return null;
    }
}
