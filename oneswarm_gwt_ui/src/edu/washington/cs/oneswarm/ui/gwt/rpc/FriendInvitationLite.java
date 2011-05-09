package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public class FriendInvitationLite implements IsSerializable {

    private boolean canSeeFileList;
    private long createdDate;

    private boolean createdLocally;

    private boolean hasChanged = false;

    private String key;

    private long lastConnectDate;

    private String lastConnectIp;

    private int lastConnectPort = 0;

    private long maxAge;

    private String name;

    private String remotePublicKey;

    private SecurityLevel securityLevel;

    private String statusText;

    public long getCreatedDate() {
        return createdDate;
    }

    public String getKey() {
        return key;
    }

    public long getLastConnectDate() {
        return lastConnectDate;
    }

    public String getLastConnectIp() {
        return lastConnectIp;
    }

    public int getLastConnectPort() {
        return lastConnectPort;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public String getName() {
        return name;
    }

    public String getRemotePublicKey() {
        return remotePublicKey;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public String getStatusText() {

        return statusText;
    }

    public boolean isCanSeeFileList() {
        return canSeeFileList;
    }

    public boolean isCreatedLocally() {
        return createdLocally;
    }

    public boolean isHasChanged() {
        return hasChanged;
    }

    public void setCanSeeFileList(boolean canSeeFileList) {
        this.canSeeFileList = canSeeFileList;
    }

    public void setCreatedDate(long date) {
        this.createdDate = date;
    }

    public void setCreatedLocally(boolean createdLocally) {
        this.createdLocally = createdLocally;
    }

    public void setHasChanged(boolean hasChanged) {
        this.hasChanged = hasChanged;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setLastConnectDate(long lastConnectDate) {
        this.lastConnectDate = lastConnectDate;
    }

    public void setLastConnectIp(String lastConnectIp) {
        this.lastConnectIp = lastConnectIp;
    }

    public void setLastConnectPort(int lastConnectPort) {
        this.lastConnectPort = lastConnectPort;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRemotePublicKey(String remotePublicKey) {
        this.remotePublicKey = remotePublicKey;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public void setStatusText(String text) {
        this.statusText = text;
    }

}
