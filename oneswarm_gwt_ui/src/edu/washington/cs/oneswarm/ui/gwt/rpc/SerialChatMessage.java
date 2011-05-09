package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SerialChatMessage implements IsSerializable {

    long uid;
    String message;
    boolean unread;
    long timestamp;
    String nickname;
    boolean outgoing;
    boolean sent;

    public SerialChatMessage() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public void setOutgoing(boolean outgoing) {
        this.outgoing = outgoing;
    }

    public String toString() {
        return "[" + getNickname() + " @ " + (new Date(timestamp)) + "]: " + message + " (unread: "
                + unread + " / outgoing: " + outgoing + ")";
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

}
