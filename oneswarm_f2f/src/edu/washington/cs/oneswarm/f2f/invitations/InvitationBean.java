package edu.washington.cs.oneswarm.f2f.invitations;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Base32;

import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.FriendInvitation.Status;

public class InvitationBean {
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

    private int securityLevel;

    private int status;

    public InvitationBean() {

    }

    public static InvitationBean createBean(FriendInvitation i) {
        InvitationBean il = new InvitationBean();
        il.setCreatedDate(i.getCreatedDate());
        il.setHasChanged(il.isHasChanged());
        il.setKey(Base32.encode(i.getKey()));
        if (i.getLastConnectIp() != null) {
            il.setLastConnectIp(i.getLastConnectIp());
        }
        il.setMaxAge(i.getMaxAge());
        il.setName(i.getName());
        if (i.getRemotePublicKey() != null) {
            il.setRemotePublicKey(new String(Base64.encode(i.getRemotePublicKey())));
        }
        il.setSecurityLevel((i.getSecurityLevel()));

        il.setStatus(i.getStatus().getCode());
        il.setCreatedLocally(i.isCreatedLocally());
        return il;
    }

    public static FriendInvitation getInvitation(InvitationBean il) {
        FriendInvitation i = new FriendInvitation(Base32.decode(il.getKey()));
        i.setName(il.getName());
        i.setMaxAge(il.getMaxAge());
        i.setCanSeeFileList(il.isCanSeeFileList());
        i.setSecurityLevel(il.getSecurityLevel());

        i.setCreatedDate(il.getCreatedDate());
        if (il.getLastConnectIp() != null) {
            i.setLastConnectIp(il.getLastConnectIp());
        }

        if (il.getRemotePublicKey() != null) {
            i.setRemotePublicKey(Base64.decode(il.getRemotePublicKey()));
        }
        i.setCreatedLocally(il.isCreatedLocally());
        i.setStatus(Status.getFromCode(il.getStatus()));

        return i;
    }

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

    public int getSecurityLevel() {
        return securityLevel;
    }

    public int getStatus() {

        return status;
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

    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}
