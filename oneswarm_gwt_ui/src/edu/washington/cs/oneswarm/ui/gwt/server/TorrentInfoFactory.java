package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.ui.gwt.F2FInterface;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class TorrentInfoFactory {
	/*
	 * for multi source speeds to be recorded properly, keep track of the number
	 * of bytes and update the download rate displayed to be the max of the
	 * azureus internal version and the calculated value
	 */
	private final static String KEY_BYTES = "multisource_downloaded_bytes";
	private final static String KEY_AVERAGE_OBJ = "multisource_average_obj";

	public static TorrentInfo create(F2FInterface f2fIface, Download download, String defaultVideoFile) {
		TorrentInfo torrentInfo = new TorrentInfo();

		Torrent torrent = download.getTorrent();
		DownloadStats downloadStat = download.getStats();

		int status = download.getState();
		int torrent_seeds = 0;
		int torrentLeechers = 0;
		if (status == Download.ST_DOWNLOADING) {

			try {
				torrent_seeds = download.getPeerManager().getStats().getConnectedSeeds();
			} catch (Throwable e) {
				torrent_seeds = 0;
			}

			try {
				torrentLeechers = download.getPeerManager().getStats().getConnectedLeechers();
			} catch (Throwable e) {
				torrentLeechers = 0;
			}

		} else if (status == Download.ST_SEEDING) {

			try {
				torrentLeechers = download.getPeerManager().getStats().getConnectedLeechers();
			} catch (Throwable e) {
				torrentLeechers = 0;
			}
		}

		torrentInfo.setSharePublic(f2fIface.isSharedWithPublic(download.getTorrent().getHash()));
		torrentInfo.setShareWithFriends(f2fIface.isSharedWithFriends(download.getTorrent().getHash()));

		torrentInfo.setStatus(download.getState());

		torrentInfo.setTorrentID(OneSwarmHashUtils.createOneSwarmHash(torrent.getHash()));

		torrentInfo.setName(download.getName());

		torrentInfo.setComment(torrent.getComment());

		torrentInfo.setDownloaded(downloadStat.getDownloaded());

		DownloadManager downloadManager = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(download.getTorrent().getHash()));
		if (downloadManager == null)
			return null;

		torrentInfo.setProgress(computeSkippedAwareProgress(downloadManager));
		DownloadManagerState downloadState = downloadManager.getDownloadState();
		Long dateVal = downloadState.getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
		torrentInfo.setAddedDate(dateVal);

		setErrorStateFromDLManager(torrentInfo, downloadManager);

		long sizeExcludeSkipped = 0;
		DiskManagerFileInfo[] files = download.getDiskManagerFileInfo();
		for (DiskManagerFileInfo df : files) {
			if (!df.isSkipped()) {
				sizeExcludeSkipped += df.getLength();
			}
		}
		torrentInfo.setTotalSize(sizeExcludeSkipped);

		torrentInfo.setDownloadRate(downloadStat.getDownloadAverage() / 1024);

		torrentInfo.setUploadRate(downloadStat.getUploadAverage() / 1024);

		torrentInfo.setRemaining(downloadStat.getETA());

		try {
			torrentInfo.setDefaultMovieName(URLEncoder.encode(defaultVideoFile, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		torrentInfo.setStatusText(downloadStat.getStatus(true));

		torrentInfo.setTotalUploaded(downloadStat.getUploaded());

		torrentInfo.setTotalDownloaded(downloadStat.getDownloaded());

		torrentInfo.setSeeders(torrent_seeds);

		int torrent_total_seeds = download.getLastScrapeResult().getSeedCount();
		torrentInfo.setTotalSeeders(torrent_total_seeds);

		torrentInfo.setLeechers(torrentLeechers);

		int torrent_total_leechers = download.getLastScrapeResult().getNonSeedCount();
		torrentInfo.setTotalLeechers(torrent_total_leechers);

		/*
		 * ************** count the number of f2f peers in the swarm
		 */
		int f2fSeeds = 0;
		int f2fLeechers = 0;
		PEPeerManager peerManager = downloadManager.getPeerManager();
		if (peerManager != null) {
			final List peers = peerManager.getPeers();
			final int peersSize = peers.size();
			for (int i = 0; i < peersSize; i++) {
				final PEPeerTransport peer = (PEPeerTransport) peers.get(i);
				if ("FriendToFriend over SSL".equals(peer.getEncryption())) {
					if (peer.isSeed()) {
						f2fSeeds++;
					} else {
						f2fLeechers++;
					}
				}
			}
		}
		torrentInfo.setSeedersF2F(f2fSeeds);
		torrentInfo.setLeechersF2f(f2fLeechers);
		// ***************

		torrentInfo.setAvailability(downloadStat.getAvailability());

		torrentInfo.setNumFiles(download.getDiskManagerFileInfo().length);

		torrentInfo.calcHashCode();
		if(downloadManager.getData("sha1_rate") != null){
			Average a = (Average) downloadManager.getData("sha1_rate");
			long rate = a.getAverage();
			if(rate > 0){
				torrentInfo.setExtraSourceSpeed(rate);
			}
		}
		return torrentInfo;
	}

	private static void setErrorStateFromDLManager(TorrentInfo torrentInfo, DownloadManager downloadManager) {
		short errorState = 0;
		if (downloadManager.getData("oneswarm.no.permissions") != null) {
			errorState |= TorrentInfo.NO_PERMISSIONS;
		}
		torrentInfo.setErrorState(errorState);
	}

	public static TorrentInfo create(F2FInterface f2fIface, DownloadManager download, String defaultVideoFile) throws AzureusCoreException, TOTorrentException {
		TorrentInfo torrentInfo = new TorrentInfo();

		TOTorrent torrent = download.getTorrent();
		DownloadManagerStats downloadStat = download.getStats();

		int status = download.getState();
		int torrent_seeds = 0;
		int torrentLeechers = 0;
		if (status == Download.ST_DOWNLOADING) {

			try {
				torrent_seeds = download.getPeerManager().getNbSeeds();
			} catch (Throwable e) {
				torrent_seeds = 0;
			}

			try {
				torrentLeechers = download.getPeerManager().getNbPeers();
			} catch (Throwable e) {
				torrentLeechers = 0;
			}

		} else if (status == Download.ST_SEEDING) {

			try {
				torrentLeechers = download.getPeerManager().getNbPeers();
			} catch (Throwable e) {
				torrentLeechers = 0;
			}
		}

		torrentInfo.setSharePublic(f2fIface.isSharedWithPublic(download.getTorrent().getHash()));

		torrentInfo.setShareWithFriends(f2fIface.isSharedWithFriends(download.getTorrent().getHash()));

		torrentInfo.setStatus(convertState(download.getState()));

		torrentInfo.setTorrentID(OneSwarmHashUtils.createOneSwarmHash(torrent.getHash()));

		torrentInfo.setName(new String(torrent.getName()));
		String comment = "";
		if (torrent.getComment() != null) {
			comment = new String(torrent.getComment());
		}
		torrentInfo.setComment(comment);

		torrentInfo.setDownloaded(downloadStat.getTotalDataBytesReceived());

		// torrentInfo.setProgress(download.getStats().getCompleted());
		/*
		 * Replaced this to be aware of skipped files
		 */
		// torrentInfo.setProgress(downloadStat.getCompleted());

		torrentInfo.setProgress(computeSkippedAwareProgress(download));

		DownloadManager downloadManager = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(download.getTorrent().getHash()));
		if (downloadManager == null)
			return null;
		DownloadManagerState downloadState = downloadManager.getDownloadState();
		Long dateVal = downloadState.getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
		torrentInfo.setAddedDate(dateVal);

		setErrorStateFromDLManager(torrentInfo, downloadManager);

		torrentInfo.setTotalSize(torrent.getSize());

		torrentInfo.setDownloadRate(downloadStat.getDataReceiveRate() / 1024.0);

		torrentInfo.setUploadRate(downloadStat.getDataSendRate() / 1024.0);

		torrentInfo.setRemaining(downloadStat.getElapsedTime());

		try {
			if (defaultVideoFile == null) {
				torrentInfo.setDefaultMovieName("");
			} else {
				torrentInfo.setDefaultMovieName(URLEncoder.encode(defaultVideoFile, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		torrentInfo.setStatusText(DisplayFormatters.formatDownloadStatusDefaultLocale(download));

		torrentInfo.setTotalUploaded(downloadStat.getTotalDataBytesSent() + downloadStat.getTotalProtocolBytesSent());

		torrentInfo.setTotalDownloaded(downloadStat.getTotalGoodDataBytesReceived());

		torrentInfo.setSeeders(torrent_seeds);

		TRTrackerScraperResponse trackerScrapeResponse = download.getTrackerScrapeResponse();
		if (trackerScrapeResponse != null) {
			int torrent_total_seeds = trackerScrapeResponse.getSeeds();
			int torrent_total_leechers = trackerScrapeResponse.getPeers();
			torrentInfo.setTotalLeechers(torrent_total_leechers);
			torrentInfo.setTotalSeeders(torrent_total_seeds);
		}

		torrentInfo.setLeechers(torrentLeechers);

		/*
		 * ************** count the number of f2f peers in the swarm
		 */
		int f2fSeeds = 0;
		int f2fLeechers = 0;
		PEPeerManager peerManager = downloadManager.getPeerManager();
		if (peerManager != null) {
			final List peers = peerManager.getPeers();
			final int peersSize = peers.size();
			for (int i = 0; i < peersSize; i++) {
				final PEPeerTransport peer = (PEPeerTransport) peers.get(i);
				if ("FriendToFriend over SSL".equals(peer.getEncryption())) {
					if (peer.isSeed()) {
						f2fSeeds++;
					} else {
						f2fLeechers++;
					}
				}
			}
		}
		torrentInfo.setSeedersF2F(f2fSeeds);
		torrentInfo.setLeechersF2f(f2fLeechers);
		// ***************

		torrentInfo.setAvailability(downloadStat.getAvailability());

		torrentInfo.setNumFiles(download.getDiskManagerFileInfo().length);

		torrentInfo.calcHashCode();

		if(downloadManager.getData("sha1_rate") != null){
			Average a = (Average) downloadManager.getData("sha1_rate");
			long rate = a.getAverage();
			if(rate > 0){
				torrentInfo.setExtraSourceSpeed(rate);
			}
		}
		
		return torrentInfo;
	}

	private static int computeSkippedAwareProgress(DownloadManager download) {

		if (download.getState() == DownloadManager.STATE_CHECKING) {
			return 0;
		}

		if (download.isDownloadComplete(false)) {
			return 1000;
		}

		org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] infos = download.getDiskManagerFileInfo();
		TOTorrentFile[] files = download.getTorrent().getFiles();

		long downloaded = 0, toDownload = 0;

		for (int i = 0; i < infos.length; i++) {
			org.gudy.azureus2.core3.disk.DiskManagerFileInfo info = infos[i];

			// System.out.println("size: " + downloaded + " / " + toDownload +
			// " (after: " + info.getFile().getName() + " skipped? " +
			// (info.isSkipped() ? 'y' : 'n'));

			if (info.isSkipped())
				continue;

			toDownload += files[i].getLength();
			downloaded += info.getDownloaded();
		}

		// System.out.println("downloaded: " + downloaded + " toDownload: " +
		// toDownload);

		int completion = (int) (downloaded * 100 / toDownload * 100) / 10;

		/**
		 * corner case. disk manager seems to report 100% DLs for a short time
		 * during startup before 0ing.
		 */
		if (completion == 1000 && download.getStats().getCompleted() == 0)
			return 0;

		return completion;
	}

	public static TorrentInfo create(F2FInterface f2fIface, DownloadManagerAdapter download, String largestFileName) {
		TorrentInfo torrentInfo = new TorrentInfo();
		// Torrent torrent = download.getTorrent();
		// DownloadManagerStats downloadStat = download.getStats();

		int torrent_seeds = 0;
		int torrentLeechers = 0;

		torrentInfo.setSharePublic(false);
		torrentInfo.setShareWithFriends(true);

		torrentInfo.setStatus(download.getState());

		try {
			torrentInfo.setTorrentID(OneSwarmHashUtils.createOneSwarmHash(download.getTorrent().getHash()));
		} catch (TOTorrentException e) {
			e.printStackTrace();
		}

		torrentInfo.setName(download.getDisplayName());

		torrentInfo.setComment("");

		torrentInfo.setDownloaded(0);

		torrentInfo.setProgress(1000);

		DownloadManagerState downloadState = download.getDownloadState();
		Long dateVal = downloadState.getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
		torrentInfo.setAddedDate(dateVal);

		torrentInfo.setTotalSize(download.getTorrent().getSize());

		torrentInfo.setDownloadRate(0);

		torrentInfo.setUploadRate(0);

		torrentInfo.setRemaining("");

		torrentInfo.setDefaultMovieName(largestFileName);

		torrentInfo.setStatusText("f2f");

		torrentInfo.setTotalUploaded(0);

		torrentInfo.setTotalDownloaded(0);

		torrentInfo.setSeeders(torrent_seeds);

		int torrent_total_seeds = 0;
		torrentInfo.setTotalSeeders(torrent_total_seeds);

		torrentInfo.setLeechers(torrentLeechers);

		int torrent_total_leechers = 0;
		torrentInfo.setTotalLeechers(torrent_total_leechers);

		torrentInfo.setAvailability(1.0);

		torrentInfo.setNumFiles(1);

		torrentInfo.calcHashCode();

		torrentInfo.setF2F_ID(download.getFriendID(), download.getFriendNick());

		FileCollection coll = download.getCollection();
		torrentInfo.setFileListLiteRep(new FileListLite(coll.getUniqueID(), coll.getName(), coll.getChildren().get(0).getFileName(), coll.getChildren().get(0).getLength(), coll.getFileNum(), coll.getAddedTimeUTC(), 1, false, false));

		return torrentInfo;
	}

	private static int convertState(int dm_state) {
		// dm states: waiting -> initialising -> initialized ->
		// disk states: allocating -> checking -> ready ->
		// dm states: downloading -> finishing -> seeding -> stopping -> stopped

		// "initialize" call takes from waiting -> initialising -> waiting (no
		// port) or initialized (ok)
		// if initialized then disk manager runs through to ready
		// "startdownload" takes ready -> dl etc.
		// "stopIt" takes to stopped which is equiv to ready

		int our_state;

		switch (dm_state) {
		case DownloadManager.STATE_WAITING: {
			our_state = Download.ST_WAITING;

			break;
		}
		case DownloadManager.STATE_INITIALIZING:
		case DownloadManager.STATE_INITIALIZED:
		case DownloadManager.STATE_ALLOCATING:
		case DownloadManager.STATE_CHECKING: {
			our_state = Download.ST_PREPARING;

			break;
		}
		case DownloadManager.STATE_READY: {
			our_state = Download.ST_READY;

			break;
		}
		case DownloadManager.STATE_DOWNLOADING:
		case DownloadManager.STATE_FINISHING: // finishing download - transit to
			// seeding
		{
			our_state = Download.ST_DOWNLOADING;

			break;
		}
		case DownloadManager.STATE_SEEDING: {
			our_state = Download.ST_SEEDING;

			break;
		}
		case DownloadManager.STATE_STOPPING: {
			our_state = Download.ST_STOPPING;

			break;
		}
		case DownloadManager.STATE_STOPPED: {
			our_state = Download.ST_STOPPED;

			break;
		}
		case DownloadManager.STATE_QUEUED: {
			our_state = Download.ST_QUEUED;

			break;
		}
		case DownloadManager.STATE_ERROR: {
			our_state = Download.ST_ERROR;

			break;
		}
		default: {
			our_state = Download.ST_ERROR;
		}
		}

		return (our_state);
	}

}
