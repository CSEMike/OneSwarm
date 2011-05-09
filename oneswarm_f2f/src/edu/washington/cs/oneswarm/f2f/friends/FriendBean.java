/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.friends;

import java.net.InetAddress;

import org.bouncycastle.util.encoders.Base64;

import edu.washington.cs.oneswarm.f2f.Friend;

public class FriendBean {

    private boolean blocked;
    private boolean canSeeFileList;
    private boolean requestFileList = true;

    public boolean isRequestFileList() {
        return requestFileList;
    }

    public void setRequestFileList(boolean requestFileList) {
        this.requestFileList = requestFileList;
    }

    private boolean allowChat = true;
    private long dateAdded = 0;
    private long lastConnectDate = 0;
    private String lastConnectIP = "0.0.0.0";
    private int lastConnectPort;
    private boolean newFriend = false;
    private String nick;
    private String publicKey;

    private boolean dhtLocationConfirmed = false;

    public boolean isDhtLocationConfirmed() {
        return dhtLocationConfirmed;
    }

    public void setDhtLocationConfirmed(boolean dhtLocationConfirmed) {
        this.dhtLocationConfirmed = dhtLocationConfirmed;
    }

    private String dhtWriteLocation;
    private String dhtReadLocation;

    public String getDhtWriteLocation() {
        return dhtWriteLocation;
    }

    public void setDhtWriteLocation(String dhtWriteLocation) {
        this.dhtWriteLocation = dhtWriteLocation;
    }

    public String getDhtReadLocation() {
        return dhtReadLocation;
    }

    public void setDhtReadLocation(String dhtReadLocation) {
        this.dhtReadLocation = dhtReadLocation;
    }

    /**
     * Added in 0.62 - mjp
     */
    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    private String sourceNetwork;

    private long totalDownloaded;
    private long totalUploaded;

    public FriendBean() {
    }

    public FriendBean(Friend f) {

        this.nick = f.getNick();
        this.sourceNetwork = f.getSourceNetwork();

        this.publicKey = new String(Base64.encode(f.getPublicKey()));

        if (f.getDateAdded() != null) {
            this.dateAdded = f.getDateAdded().getTime();
        } else {
            this.dateAdded = System.currentTimeMillis();
        }
        if (f.getLastConnectDate() != null) {
            this.lastConnectDate = f.getLastConnectDate().getTime();
        }
        InetAddress lastConnIP = f.getLastConnectIP();
        if (lastConnIP != null) {
            this.lastConnectIP = f.getLastConnectIP().getHostAddress();
        }
        this.totalDownloaded = f.getTotalDownloaded();
        this.totalUploaded = f.getTotalUploaded();
        this.lastConnectPort = f.getLastConnectPort();
        this.canSeeFileList = f.isCanSeeFileList();
        this.blocked = f.isBlocked();
        this.group = f.getGroup();

        this.allowChat = f.isAllowChat();

        if (f.getDhtReadLocation() != null) {
            this.dhtReadLocation = new String(Base64.encode(f.getDhtReadLocation()));
        }
        if (f.getDhtWriteLocation() != null) {
            this.dhtWriteLocation = new String(Base64.encode(f.getDhtWriteLocation()));
        }
        this.dhtLocationConfirmed = f.isDhtLocationConfirmed();
    }

    public long getDateAdded() {
        return this.dateAdded;
    }

    public long getLastConnectDate() {
        return this.lastConnectDate;
    }

    public String getLastConnectIP() {
        return lastConnectIP;
    }

    public int getLastConnectPort() {
        return lastConnectPort;
    }

    public String getNick() {
        return this.nick;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public String getSourceNetwork() {
        return sourceNetwork;
    }

    public long getTotalDownloaded() {
        return totalDownloaded;
    }

    public long getTotalUploaded() {
        return totalUploaded;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isCanSeeFileList() {
        return canSeeFileList;
    }

    public boolean isNewFriend() {
        return newFriend;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setCanSeeFileList(boolean canSeeFileList) {
        this.canSeeFileList = canSeeFileList;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public void setLastConnectDate(long lastConnectDate) {
        this.lastConnectDate = lastConnectDate;
    }

    public void setLastConnectIP(String lastConnectIP) {
        this.lastConnectIP = lastConnectIP;
    }

    public void setLastConnectPort(int lastConnectPort) {
        this.lastConnectPort = lastConnectPort;
    }

    public void setNewFriend(boolean newFriend) {
        this.newFriend = newFriend;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setSourceNetwork(String sourceNetwork) {
        this.sourceNetwork = sourceNetwork;
    }

    public void setTotalDownloaded(long totalDownloaded) {
        this.totalDownloaded = totalDownloaded;
    }

    public void setTotalUploaded(long totalUploaded) {
        this.totalUploaded = totalUploaded;
    }

    public boolean isAllowChat() {
        return allowChat;
    }

    public void setAllowChat(boolean allowChat) {
        this.allowChat = allowChat;
    }

}