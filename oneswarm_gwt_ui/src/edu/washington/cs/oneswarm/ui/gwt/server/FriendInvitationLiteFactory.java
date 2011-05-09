package edu.washington.cs.oneswarm.ui.gwt.server;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Base32;

import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInvitationLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public class FriendInvitationLiteFactory {

    public static FriendInvitationLite createFriendInvitationLite(FriendInvitation i) {
        FriendInvitationLite il = new FriendInvitationLite();
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
        il.setSecurityLevel(SecurityLevel.fromCode(i.getSecurityLevel()));

        il.setStatusText(i.getStatus().getDisplayString());
        il.setCreatedLocally(i.isCreatedLocally());
        return il;
    }

    public static FriendInvitation createFriendInvitation(FriendInvitationLite il) throws Exception {
        byte[] key = Base32.decode(il.getKey());
        if (key.length != FriendInvitation.INV_KEY_LENGTH) {
            throw new Exception("wrong key length (" + key.length + "!="
                    + FriendInvitation.INV_KEY_LENGTH + ")");
        }
        FriendInvitation i = new FriendInvitation(key);
        i.setName(il.getName());
        i.setMaxAge(il.getMaxAge());
        i.setCanSeeFileList(il.isCanSeeFileList());
        i.setSecurityLevel(il.getSecurityLevel().getLevel());

        i.setCreatedDate(il.getCreatedDate());
        if (il.getLastConnectIp() != null) {
            i.setLastConnectIp(il.getLastConnectIp());
        }

        if (il.getRemotePublicKey() != null) {
            i.setRemotePublicKey(Base64.decode(il.getRemotePublicKey()));
        }
        i.setCreatedLocally(il.isCreatedLocally());

        return i;
    }
}
