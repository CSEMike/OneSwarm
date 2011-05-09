package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FriendInfoLite implements IsSerializable, Comparable<FriendInfoLite> {

    public final static int STATUS_OFFLINE = 0;

    public final static int STATUS_CONNECTING = 1;

    public final static int STATUS_HANDSHAKING = 2;

    public final static int STATUS_ONLINE = 3;

    public final static int STATUS_TO_BE_DELETED = -1;

    private boolean blocked;
    private boolean canSeeFileList;
    private boolean requestFileList;

    public boolean isRequestFileList() {
        return requestFileList;
    }

    public void setRequestFileList(boolean requestFileList) {
        this.requestFileList = requestFileList;
    }

    private boolean allowChat;
    private int connectionId;
    private int id;
    private String name;
    private String publicKey;
    private String source;
    private int status;
    private long downloadedTotal;
    private long downloadedSession;
    private long uploadedTotal;
    private long uploadedSession;
    private String connectLog;
    private Date lastConnectedDate;
    private String lastConnectIp;
    private int lastConnectPort;
    private boolean supportsChat;
    private boolean supportsExtendedFileLists;
    private Date dateAdded;

    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date value) {
        dateAdded = value;
    }

    public Date getLastConnectedDate() {
        return lastConnectedDate;
    }

    // public boolean getselectedintable() {
    // return selectedinfriendtable;
    // }
    //
    // public void setselectedintable(boolean value) {
    // selectedinfriendtable = value;
    // }

    public void setLastConnectedDate(Date lastConnectedDate) {
        this.lastConnectedDate = lastConnectedDate;
    }

    public String getLastConnectIp() {
        return lastConnectIp;
    }

    public void setLastConnectIp(String lastConnectIp) {
        this.lastConnectIp = lastConnectIp;
    }

    public int getLastConnectPort() {
        return lastConnectPort;
    }

    public void setLastConnectPort(int lastConnectPort) {
        this.lastConnectPort = lastConnectPort;
    }

    public FriendInfoLite() {

    }

    private int hashOfStateFields = 0;

    public int getHashOfStateFields() {
        if (hashOfStateFields == 0) {
            String stringToHash = "" + blocked + connectionId + id + name + publicKey + status;
            hashOfStateFields = stringToHash.hashCode();
        }
        return hashOfStateFields;
    }

    public FriendInfoLite(String publicKey, int connected, int id, String name, String source,
            int status, boolean blocked, boolean canSeeFileList, boolean allowChat,
            boolean requestFileList) {
        super();
        this.connectionId = connected;
        this.id = id;
        this.name = name;
        this.source = source;
        this.status = status;
        this.blocked = blocked;
        this.canSeeFileList = canSeeFileList;
        this.allowChat = allowChat;
        this.publicKey = publicKey;
        this.requestFileList = requestFileList;
    }

    public String getConnectionLog() {
        return connectLog;
    }

    public void setConnectionLog(String connectLog) {
        this.connectLog = connectLog;
    }

    public long getDownloadedTotal() {
        return downloadedTotal;
    }

    public void setDownloadedTotal(long downloadedTotal) {
        this.downloadedTotal = downloadedTotal;
    }

    public long getDownloadedSession() {
        return downloadedSession;
    }

    public void setDownloadedSession(long downloadedSession) {
        this.downloadedSession = downloadedSession;
    }

    public long getUploadedTotal() {
        return uploadedTotal;
    }

    public void setUploadedTotal(long uploadedTotal) {
        this.uploadedTotal = uploadedTotal;
    }

    public long getUploadedSession() {
        return uploadedSession;
    }

    public void setUploadedSession(long uploadedSession) {
        this.uploadedSession = uploadedSession;
    }

    public int compareTo(FriendInfoLite o) {
        if (o != null) {
            if (o.getStatus() == STATUS_ONLINE && getStatus() == STATUS_ONLINE) {
                String n = o.getName();

                if (n != null && name != null) {
                    return name.toLowerCase().compareTo(n.toLowerCase());
                }
            } else if (getStatus() == STATUS_ONLINE) {
                return -1;
            } else if (o.getStatus() == STATUS_ONLINE) {
                return 1;
            } else {
                String n = o.getName();
                if (n != null && name != null) {
                    return name.toLowerCase().compareTo(n.toLowerCase());
                }
            }

        }
        return 1;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSource() {
        return source;
    }

    public int getStatus() {
        return status;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isCanSeeFileList() {
        return canSeeFileList;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setCanSeeFileList(boolean canSeeFileList) {
        this.canSeeFileList = canSeeFileList;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean equals(Object rhs) {
        if (rhs instanceof FriendInfoLite) {
            return ((FriendInfoLite) rhs).getPublicKey().equals(this.getPublicKey());
        }
        return false;
    }

    public int hashCode() {
        return this.getPublicKey().hashCode();
    }

    public String toString() {
        return getName();
    }

    public boolean isSupportsChat() {
        return supportsChat;
    }

    public void setSupportsChat(boolean supportsChat) {
        this.supportsChat = supportsChat;
    }

    public boolean isSupportsExtendedFileLists() {
        return supportsExtendedFileLists;
    }

    public void setSupportsExtendedFileLists(boolean supportsExtendedFileLists) {
        this.supportsExtendedFileLists = supportsExtendedFileLists;
    }

    public boolean isAllowChat() {
        return allowChat;
    }

    public void setAllowChat(boolean allowChat) {
        this.allowChat = allowChat;
    }

    public boolean isConnected() {
        return getStatus() == STATUS_ONLINE;
    }
}
