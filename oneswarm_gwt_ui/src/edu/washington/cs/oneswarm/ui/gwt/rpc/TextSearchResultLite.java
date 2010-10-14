/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author isdal
 * 
 */
public class TextSearchResultLite implements IsSerializable {

	private String collectionName;
	private String collectionId;
	private int channelId;
	private int connectionId;
	private long age;
	private String[] throughFriends;
	private Long[] friendDelay;
	private long fileSize;
	private String fileName;
	private long addedTimeUTC;
	private boolean inLibrary = false;
	private TorrentInfo torrentInfo;

	public TorrentInfo getTorrentInfo() {
		return torrentInfo;
	}

	public void setTorrentInfo(TorrentInfo torrentInfo) {
		this.torrentInfo = torrentInfo;
	}

	public long getAddedTimeUTC() {
		return addedTimeUTC;
	}

	public boolean isInLibrary() {
		return inLibrary;
	}

	public void setInLibrary(TorrentInfo torrentInfo) {
		this.inLibrary = true;
		this.torrentInfo = torrentInfo;
	}

	public TextSearchResultLite(long age, int channelId, String collectionId, String collectionName, int connectionId, String fileName, long fileSize, Long[] friendDelay, String[] throughFriends, long addedTimeUTC) {

		this.age = age;
		this.channelId = channelId;
		this.collectionId = collectionId;
		this.collectionName = collectionName;
		this.connectionId = connectionId;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.friendDelay = friendDelay;
		this.throughFriends = throughFriends;
		this.addedTimeUTC = addedTimeUTC;
	}

	public long getFileSize() {
		return fileSize;
	}

	public String getFileName() {
		return fileName;
	}

	/**
	 * for IsSerializable
	 */
	public TextSearchResultLite() {

	}

	public String getCollectionName() {
		return collectionName;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public int getChannelId() {
		return channelId;
	}

	public long getAge() {
		return age;
	}

	public String[] getThroughFriends() {
		return throughFriends;
	}

	public int getConnectionId() {
		return connectionId;
	}

	public Long[] getFriendDelay() {
		return friendDelay;
	}

}
