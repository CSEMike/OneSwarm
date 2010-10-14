/**
 * 
 */
package edu.washington.cs.oneswarm.f2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TextSearchResult
{
	public int getFirstSeenConnectionId() {
		return firstSeenConnectionId;
	}

	public static class TextSearchResponse
	{
		final ArrayList<TextSearchResponseItem> items;

		private final long											time;

		private final String										searchString;

		public TextSearchResponse(String searchString) {
			time = System.currentTimeMillis();
			items = new ArrayList<TextSearchResponseItem>();
			this.searchString = searchString;
		}

		public void add(TextSearchResponseItem resp) {
			items.add(resp);
		}

		public ArrayList<TextSearchResponseItem> getItems() {
			return items;
		}

		public long getTime() {
			return time;
		}

		public String getSearchString() {
			return searchString;
		}
	}

	public static class TextSearchResponseItem
	{
		private final int			channelId;

		private final int			connectionId;

		private final FileList fileList;

		private final Friend	 throughFriend;

		private final long		 time;

		public TextSearchResponseItem(Friend throughFriend, FileList fileList,
				long time, int channelId, int connectionId) {
			this.channelId = channelId;
			this.fileList = fileList;
			this.throughFriend = throughFriend;
			this.time = time;
			this.connectionId = connectionId;
		}

		public int getChannelId() {
			return channelId;
		}

		public FileList getFileList() {
			return fileList;
		}

		public Friend getThroughFriend() {
			return throughFriend;
		}

		public long getTime() {
			return time;
		}

		public int getConnectionId() {
			return connectionId;
		}

	}

	private final FileCollection collection;

	private long								 firstSeenTime;

	private int									firstSeenChannelId;

	private int									firstSeenConnectionId;

	private final List<Friend>	 throughFriends = new ArrayList<Friend>();

	private final List<Long>		 delay					= new ArrayList<Long>();

	private final List<Integer>	channels			 = new LinkedList<Integer>();

	private final boolean				inLibrary;

	public boolean isInLibrary() {
		return inLibrary;
	}

	public TextSearchResult(TextSearchResult.TextSearchResponseItem item,
			FileCollection collection, boolean inLibrary) {
		this.collection = collection;
		this.firstSeenTime = item.getTime();
		this.firstSeenChannelId = item.getChannelId();
		this.firstSeenConnectionId = item.getConnectionId();
		this.throughFriends.add(item.getThroughFriend());
		this.delay.add(item.getTime());
		this.inLibrary = inLibrary;
		this.channels.add(item.channelId);
	}

	public int getFirstSeenChannelId() {
		return firstSeenChannelId;
	}

	public FileCollection getCollection() {
		return collection;
	}

	public long getFirstSeenTime() {
		return firstSeenTime;
	}

	public List<Friend> getThroughFriends() {
		return throughFriends;
	}

	public List<Long> getDelays() {
		return delay;
	}

	public void merge(TextSearchResult.TextSearchResponseItem item,
			FileCollection c) {
		if (!c.getUniqueID().equals(collection.getUniqueID())) {
			throw new RuntimeException("trying to merge incompatible collections");
		}

		if (item.getTime() < firstSeenTime) {
			firstSeenTime = item.getTime();
			firstSeenChannelId = item.getChannelId();
			firstSeenConnectionId = item.getConnectionId();
		}
		if (!throughFriends.contains(item.getThroughFriend())) {
			throughFriends.add(item.getThroughFriend());
			delay.add(item.getTime());
		}
		if (!channels.contains(item.channelId)) {
			this.channels.add(item.channelId);
		}
		/*
		 * add any files that wasn't in here before
		 */
		Set<FileListFile> existing = new HashSet<FileListFile>();
		existing.addAll(collection.getChildren());
		for (FileListFile newFile : c.getChildren()) {
			if (!existing.contains(newFile)) {
				collection.getChildren().add(newFile);
			}
		}
	}

	public String toString() {

		StringBuilder friends = new StringBuilder();
		for (int i = 0; i < throughFriends.size(); i++) {
			friends.append(throughFriends.get(i).getNick() + ", ");
		}
		return collection.getName() + " (" + firstSeenTime + "ms) '"
				+ collection.getUniqueID() + "'channel: " + firstSeenChannelId + " || "
				+ friends.toString();
	}
}