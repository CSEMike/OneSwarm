package edu.washington.cs.oneswarm.f2f.chat;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Chat {
	
	String nick;
	long timestamp;
	String message;
	boolean unread;
	long uid;
	boolean outgoing;
	boolean sent;
	
	public Chat( long inUID, String inNick, long inTimestamp, String message, boolean unread, boolean outgoing, boolean sent ) {
		nick = inNick;
		uid = inUID;
		timestamp = inTimestamp;
		this.message = message; 
		this.unread = unread;
		this.outgoing = outgoing;
		this.sent = sent;
	}
	
	public static Chat fromResultSet( ResultSet rs ) throws SQLException {
		Chat neu = new Chat(rs.getLong("uid"), rs.getString("nick_at_receive"), 
				rs.getTimestamp("mtimestamp").getTime(), rs.getString("message"), rs.getShort("unread") == 1, 
				rs.getShort("outgoing") == 1, rs.getShort("sent") == 1);
		return neu;
	}
	
	public boolean isOutgoing() {
		return outgoing;
	}
	
	public long getUID() {
		return uid;
	}
	
	public boolean isUnread() {
		return unread;
	}
	
	public boolean isSent() {
		return sent;
	}
	
	public String getNick() {
		return nick;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getMessage() {
		return message;
	}
}
