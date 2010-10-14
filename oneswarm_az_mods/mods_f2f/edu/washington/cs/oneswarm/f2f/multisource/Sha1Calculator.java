package edu.washington.cs.oneswarm.f2f.multisource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.ED2KHasher;
import org.gudy.azureus2.core3.util.SHA1Hasher;

import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1CalcListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1Result;

class Sha1Calculator
{
	private final static Logger	 logger = Logger.getLogger(Sha1Calculator.class.getName());

	private final ExecutorService executors;

	private volatile boolean			quit	 = false;

	public Sha1Calculator() {
		executors = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("Sha1+ed2k calculator worker thread");
						t.setDaemon(true);
						return t;
					}
				});

	}

	public void getHashesFromDownload(DownloadManager dm,
			List<Sha1CalcListener> listeners) {
		logger.fine("job submitted: " + dm.getDisplayName() + " listeners="
				+ listeners.size());
		if (quit == true) {
			Exception e = new Exception("calculator shut down");
			for (Sha1CalcListener listener : listeners) {
				listener.errorOccured(e);
			}
			return;
		}
		if (dm == null) {
			Exception e = new Exception("dm is null");
			for (Sha1CalcListener listener : listeners) {
				listener.errorOccured(e);
			}
			return;
		}

		executors.submit(new Sha1Ed2kWorkerWorker(dm, listeners));
	}

	public void stop() {
		quit = true;
	}

	private class Sha1Ed2kWorkerWorker
		implements Runnable
	{
		final DownloadManager				dm;

		final List<Sha1CalcListener> listeners;

		private Sha1Ed2kWorkerWorker(DownloadManager dm,
				List<Sha1CalcListener> listeners) {
			this.listeners = listeners;
			this.dm = dm;
		}

		private void notifyListenersCompleted(Sha1Result result) {
			for (Sha1CalcListener listener : listeners) {
				listener.completed(result);
			}
		}

		private void notifyListenersErrorOccured(Exception e) {
			for (Sha1CalcListener listener : listeners) {
				listener.errorOccured(e);
			}
		}

		private void notifyListenersProgress(double d) {
			logger.finest("hash progress: " + d);
			for (Sha1CalcListener listener : listeners) {
				listener.progress(d);
			}
		}

		public void run() {
			try {
				logger.fine("started: " + dm.getDisplayName());
				if (!dm.isDownloadComplete(false)) {
					notifyListenersErrorOccured(new Exception("download not completed"));
					return;
				}

				if (quit == true) {
					notifyListenersErrorOccured(new Exception("calc service stopped"));
					return;
				}
				TOTorrentFile[] tFiles = dm.getTorrent().getFiles();
				DiskManagerFileInfo[] dFiles = dm.getDiskManagerFileInfo();
				ArrayList<byte[]> ed2kHashes = new ArrayList<byte[]>(tFiles.length);
				ArrayList<byte[]> sha1Hashes = new ArrayList<byte[]>(tFiles.length);

				double totalToHash = 0;
				for (DiskManagerFileInfo d : dFiles) {
					if (!d.isSkipped()) {
						totalToHash += d.getLength();
						logger.finest("adding: " + d.getLength() + " ("
								+ d.getTorrentFile().getRelativePath() + ")");
					}
				}
				logger.finer("total to hash: " + totalToHash);
				double totalRead = 0;
				for (int i = 0; i < dFiles.length && !quit; i++) {

					DiskManagerFileInfo dFile = dFiles[i];
					TOTorrentFile tFile = tFiles[i];
					if (dFile.isSkipped()) {
						ed2kHashes.add(null);
						sha1Hashes.add(null);
						continue;
					}

					File fileOnDisk = dFile.getFile(true);
					if (!fileOnDisk.exists()) {
						notifyListenersErrorOccured(new Exception("file: " + fileOnDisk
								+ " not found"));
						return;
					}

					if (fileOnDisk.length() != tFile.getLength()) {
						notifyListenersErrorOccured(new Exception("file: " + fileOnDisk
								+ " not completed (" + fileOnDisk.length() + " != "
								+ tFile.getLength()));
						return;
					}

					double rateLimitMBps = COConfigurationManager.getIntParameter("oneswarm.max.sha1.hash.rate.kbps") / 1024.0;

					SHA1Hasher sha1_hash = new SHA1Hasher();
					ED2KHasher ed2k_hash = new ED2KHasher();

					byte[] buffer = new byte[1024 * 1024];
					File f = dFile.getFile(true);
					InputStream in = new FileInputStream(f);
					logger.finer("hashing file: " + f.getCanonicalPath());
					int len;
					long lastRead = System.currentTimeMillis();
					while ((len = in.read(buffer)) != -1 && !quit) {
						totalRead += len;
						sha1_hash.update(buffer, 0, len);
						ed2k_hash.update(buffer, 0, len);

						notifyListenersProgress(totalRead / totalToHash);

						/*
						 * limit hash rate to rateMBps
						 */
						long msTaken = System.currentTimeMillis() - lastRead;
						double mbRead = len / 1024.0 / 1024.0;
						logger.finest("read " + mbRead + " MB in " + msTaken + "ms");
						if (rateLimitMBps > 0) {
							long targetMs = Math.round(1000 * (mbRead / rateLimitMBps));
							if (msTaken < targetMs) {
								long s = targetMs - msTaken;
								logger.finest("sleeping " + s + " ms to limit hash speed");
								Thread.sleep(s);
							}
						}
						lastRead = System.currentTimeMillis();
					}
					sha1Hashes.add(sha1_hash.getDigest());
					ed2kHashes.add(ed2k_hash.getDigest());
				}
				if (!quit) {
					notifyListenersCompleted(new Sha1Result(sha1Hashes, ed2kHashes));
				} else {
					notifyListenersErrorOccured(new Exception("calc service stopped"));
					return;
				}
			} catch (Exception e) {
				notifyListenersErrorOccured(e);
			}
		}
	}

}
