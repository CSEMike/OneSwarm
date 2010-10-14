package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Arrays;
import java.util.Comparator;

import com.google.gwt.user.client.rpc.IsSerializable;


public class TorrentList implements IsSerializable {
	private int totalTorrentNum;
	private TorrentInfo[] torrentInfos;

	public TorrentList() {
		totalTorrentNum = 0;
		torrentInfos = new TorrentInfo[0];

	}

	public int getTotalTorrentNum() {
		return totalTorrentNum;
	}

	public TorrentInfo[] getTorrentInfos() {
		return torrentInfos;
	}

	public void setTotalTorrentNum(int totalTorrentNum) {
		this.totalTorrentNum = totalTorrentNum;
	}

	public void setTorrentInfos(TorrentInfo[] torrentInfos) {
		this.torrentInfos = torrentInfos;
	}

	public void sortDate() {
		Arrays.sort(torrentInfos, new Comparator<TorrentInfo>() {
			public int compare(TorrentInfo o1, TorrentInfo o2) {
				if (o1 != null && o2 != null) {
					if (o1.getDownloaded() < o2.getDownloaded()) {
						return 1;
					}
				}
				return -1;
			}
		});
	}

}
