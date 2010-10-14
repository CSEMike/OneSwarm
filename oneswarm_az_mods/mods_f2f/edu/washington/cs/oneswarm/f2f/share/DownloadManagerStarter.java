package edu.washington.cs.oneswarm.f2f.share;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;

public class DownloadManagerStarter
{
	private static Logger logger = Logger.getLogger(DownloadManagerStarter.class.getName());

	public static void startDownload(final DownloadManager downloadManager,
			final DownloadManagerStartListener listener) {
		final AtomicBoolean started = new AtomicBoolean(false);
		int state = downloadManager.getState();
		if (state == DownloadManager.STATE_DOWNLOADING
				|| state == DownloadManager.STATE_SEEDING) {
			// download is running, set state to connected
			if (downloadManager.getPeerManager() != null) {
				listener.downloadStarted();
			} else {
				waitForPeerManager(downloadManager, listener);
			}
		} else {
			logger.finer("download manager not in seed or download state, starting. current state="
					+ state);

			// add a listener that will start the download as soon as the state
			// is downloading or seeding
			downloadManager.addListener(new DownloadManagerListener() {

				public void completionChanged(DownloadManager manager, boolean completed) {
				}

				public void downloadComplete(DownloadManager manager) {
				}

				public void filePriorityChanged(DownloadManager download,
						DiskManagerFileInfo file) {
				}

				public void positionChanged(DownloadManager download, int oldPosition,
						int newPosition) {
				}

				public void stateChanged(DownloadManager manager, int s) {
					if (s == DownloadManager.STATE_DOWNLOADING
							|| s == DownloadManager.STATE_SEEDING) {
						if (!started.get()) {
							started.set(true);

							downloadManager.removeListener(this);
							if (downloadManager.getPeerManager() != null) {
								listener.downloadStarted();
							} else {
								waitForPeerManager(downloadManager, listener);
							}
						}
					} 
//					else if(s == DownloadManager.STATE_ERROR){
//						manager.forceRecheck();
//					} else if (s == DownloadManager.STATE_STOPPED){
//						manager.startDownload();
//					}
				}

			});
			downloadManager.setForceStart(true);
			// if there is some error, force recheck
			if (state == Download.ST_ERROR) {
				downloadManager.forceRecheck(new ForceRecheckListener(){
					public void forceRecheckComplete(DownloadManager dm) {
						int newState = dm.getState();
						
						if (newState == Download.ST_STOPPED || newState == Download.ST_QUEUED) {
							logger.finer("Restarting download");
							downloadManager.setStateWaiting();

						} else if (newState == Download.ST_READY) {
							logger.finer("starting download");
							downloadManager.startDownload();
						}
					}});
				logger.finer("rechecking data");
			}
			else if (state == Download.ST_STOPPED || state == Download.ST_QUEUED) {
				logger.finer("Restarting download");
				downloadManager.setStateWaiting();

			} else if (state == Download.ST_READY) {
				logger.finer("starting download");
				downloadManager.startDownload();
			}
		}
	}

	private static void waitForPeerManager(final DownloadManager downloadManager,
			final DownloadManagerStartListener listener) {
		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					int count = 0;
					while (downloadManager.getPeerManager() == null) {
						if (count > 100) {
							Debug.out("unable to start download, peer manager is null even after 10s");
							return;
						}
						count++;
						Debug.out("waiting for peer manager... sleeping 100ms");
						Thread.sleep(100);
					}
					if (count > 0) {
						Debug.out("peer manager arrived :-)");
					}
					listener.downloadStarted();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.setName("PeerManagerWaiter");
		t.start();
	}

	public interface DownloadManagerStartListener
	{
		public void downloadStarted();
	}
}
