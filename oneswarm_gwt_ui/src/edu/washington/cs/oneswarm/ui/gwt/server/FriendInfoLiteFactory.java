package edu.washington.cs.oneswarm.ui.gwt.server;

import java.util.Date;

import org.bouncycastle.util.encoders.Base64;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class FriendInfoLiteFactory {

    public static FriendInfoLite createFriendInfo(Friend f) {
        String publicKey = new String(Base64.encode(f.getPublicKey()));
        int connectionId = f.getConnectionId();
        int friendId = f.hashCode();
        FriendInfoLite friendInfoLite = new FriendInfoLite(publicKey, connectionId, friendId,
                f.getNick(), f.getSourceNetwork(), f.getStatus(), f.isBlocked(),
                f.isCanSeeFileList(), f.isAllowChat(), f.isRequestFileList());
        friendInfoLite.setDownloadedSession(f.getTotalDownloadSinceAppStart());
        friendInfoLite.setDownloadedTotal(f.getTotalDownloaded());
        friendInfoLite.setUploadedSession(f.getTotalUploadSinceAppStart());
        friendInfoLite.setUploadedTotal(f.getTotalUploaded());
        friendInfoLite.setConnectionLog(f.getConnectionLog());
        if (f.getLastConnectIP() != null) {
            friendInfoLite.setLastConnectIp(f.getLastConnectIP().getHostAddress());
        }
        friendInfoLite.setLastConnectPort(f.getLastConnectPort());
        friendInfoLite.setLastConnectedDate(f.getLastConnectDate());
        friendInfoLite.setSupportsChat(f.isSupportsChat());
        friendInfoLite.setSupportsExtendedFileLists(f.isSupportsExtendedFileLists());
        friendInfoLite.setDateAdded(f.getDateAdded());
        friendInfoLite.setGroup(f.getGroup());
        return friendInfoLite;
    }

    public static FriendInfoLite createFromKeyAndNick(String key, String nick, String sourceNetwork) {
        FriendInfoLite converted = new FriendInfoLite();
        converted.setPublicKey(key);
        converted.setName(nick);
        converted.setAllowChat(false);
        converted.setCanSeeFileList(false);
        converted.setSource(sourceNetwork);
        converted.setDateAdded(new Date());
        return converted;
    }
}
