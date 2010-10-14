package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilEncodingException;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.f2f.share.ShareManagerTools;
import edu.washington.cs.oneswarm.plugins.PluginCallback;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.RequiresShutdown;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;

public class AddTorrentManager implements RequiresShutdown {
	private static Logger logger = Logger.getLogger(AddTorrentManager.class.getName());

	private Random mRandom = new Random();

	private class TorrentDownloaderLite {
		private int downloadState;
		private int totalRead;
		private int percentDone;
		private String status;
		private File file;

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		public int getDownloadState() {
			return downloadState;
		}

		public void setDownloadState(int downloadState) {
			this.downloadState = downloadState;
		}

		public int getTotalRead() {
			return totalRead;
		}

		public void setTotalRead(int totalRead) {
			this.totalRead = totalRead;
		}

		public int getPercentDone() {
			return percentDone;
		}

		public void setPercentDone(int percentDone) {
			this.percentDone = percentDone;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	private final ConcurrentHashMap<Integer, TorrentDownloaderLite> downloaderMap;

	// private final ConcurrentHashMap<Integer, Torrent>
	private final CoreInterface coreInterface;
	private final AzureusCore core;

	private boolean quit = false;

	public AddTorrentManager(CoreInterface coreInterface) {
		this.downloaderMap = new ConcurrentHashMap<Integer, TorrentDownloaderLite>();
		this.coreInterface = coreInterface;
		this.core = AzureusCoreImpl.getSingleton();
		coreInterface.addShutdownObject(this);
	}

	public int downloadTorrent(int friendConnection, int channelId, String torrentId, int lengthHint) {
		int rand = mRandom.nextInt();
		final TorrentDownloaderLite downloader = new TorrentDownloaderLite();
		downloaderMap.put(rand, downloader);
		coreInterface.getF2FInterface().getMetaInfo(friendConnection, channelId, torrentId, lengthHint, new PluginCallback<byte[]>() {

			public void dataRecieved(long bytes) {
				downloader.setTotalRead(((int) (downloader.getTotalRead() + bytes)));
				downloader.setDownloadState(TorrentDownloader.STATE_DOWNLOADING);

			}

			public void errorOccured(String string) {
				downloader.setStatus("Error: " + string);
				downloader.setDownloadState(TorrentDownloader.STATE_ERROR);
			}

			public void progressUpdate(int progress) {
				downloader.setStatus("Downloading: " + progress + "%");
				downloader.setPercentDone(progress);
			}

			public void requestCompleted(byte[] data) {
				try {
					File f = File.createTempFile("oneswarm_", ".torrent");
					f.deleteOnExit();
					logger.fine("writing torrent to file: " + f.getCanonicalPath());

					/**
					 * We've modified the TOTorrentImpl to preserve the required
					 * OneSwarm attributes while dumping the rest (this include
					 * no text search, tags, album, etc.)
					 */
					TOTorrent torrent = TorrentUtils.readFromBEncodedInputStream(new ByteArrayInputStream(data));
					torrent.setAdditionalStringProperty("encoding", "UTF-8");
					TorrentUtils.writeToFile(torrent, f);
					downloader.setFile(f);

					downloader.setDownloadState(TorrentDownloader.STATE_FINISHED);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					errorOccured(e.getMessage());
				} catch (TOTorrentException e) {
					System.err.println(e);
					e.printStackTrace();
					errorOccured(e.getMessage());
				}

			}
		});

		return rand;
	}

	public int downloadTorrent(String url) {

		// url decode the string
		try {
			logger.finer("decoding url");
			url = URLDecoder.decode(url, "ISO-8859-1");
			logger.finer("new url is: " + url);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return -1;
		}

		if (url == null) {
			return -1;
		}

		if (url.startsWith(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX)) {
			logger.finer("modifying torrent url from: " + url);
			url = url.substring(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX.length());
			url = OneSwarmConstants.BITTORRENT_MAGNET_PREFIX_REAL + url;
			logger.finer("new torrent url: " + url);
		}

		int torrentId = -1;
		try {

			File tempFile = File.createTempFile("OneSwam_tmp_torrent_", ".torrent");
			tempFile.deleteOnExit();
			logger.finest("creating temp file: " + tempFile.getCanonicalPath());
			final TorrentDownloaderLite downloader = new TorrentDownloaderLite();

			TorrentDownloader torrentDownloader = TorrentDownloaderFactory.create(new TorrentDownloaderCallBackInterface() {

				public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
					try {
						logger.finest("got torrent download event: state=" + state + " done=" + inf.getPercentDone() + " URL='" + inf.getURL() + "'");
						try {
							downloader.setTotalRead(inf.getTotalRead());
							downloader.setDownloadState(state);
							downloader.setPercentDone(inf.getPercentDone());
							downloader.setStatus(inf.getStatus());
							if (state != TorrentDownloader.STATE_ERROR) {
								downloader.setFile(inf.getFile());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						switch (state) {
						case TorrentDownloader.STATE_CANCELLED:
							downloaderMap.remove(inf.getURL().hashCode());
							break;
						case TorrentDownloader.STATE_ERROR:
							downloaderMap.remove(inf.getURL().hashCode());
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						if (inf.getURL() != null) {
							downloaderMap.remove(inf.getURL().hashCode());
						}
					}
				}
			}, url, null, tempFile.getCanonicalPath());
			torrentDownloader.start();
			torrentDownloader.setDeleteFileOnCancel(true);
			torrentId = url.hashCode();
			logger.fine("started torrent download, id=" + torrentId);
			downloaderMap.put(torrentId, downloader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

		return torrentId;
	}

	public int getPercentageDone(int torrentDownloaderHash) {
		TorrentDownloaderLite inf = downloaderMap.get(torrentDownloaderHash);
		if (inf != null) {
			if (inf.getDownloadState() == TorrentDownloader.STATE_FINISHED) {
				return 100;
			}
			return inf.getPercentDone();
		}
		return -2;
	}

	public int getTotalRead(int torrentDownloaderHash) {
		TorrentDownloaderLite inf = downloaderMap.get(torrentDownloaderHash);
		if (inf != null) {
			return inf.getTotalRead();
		}
		return -1;
	}

	public String getStatus(int torrentDownloaderHash) {
		TorrentDownloaderLite inf = downloaderMap.get(torrentDownloaderHash);

		if (inf != null) {
			return inf.getStatus();
		}

		return null;
	}

	public FileListLite[] getFiles(int torrentDownloaderHash) {
		TorrentDownloaderLite inf = downloaderMap.get(torrentDownloaderHash);
		if (inf != null && inf.getDownloadState() == TorrentDownloader.STATE_FINISHED) {

			File f = inf.getFile();

			try {
				logger.finest("about to readFromFile()");
				TOTorrent torrent = TorrentUtils.readFromFile(f, false);
				logger.finest("processing torrent: " + new String(torrent.getName()));
				logger.finest("after readFromFile()");

				boolean limitedVisibility = torrent.getAdditionalProperty("OneSwarmNoShare") != null;
				if (limitedVisibility) {
					logger.fine("torrent has OneSwarmNoShare set");
				} else {
					logger.fine("torrent is shared with all friends");
				}

				/*
				 * check the locale
				 */
				String enc = LocaleTorrentUtil.getCurrentTorrentEncoding(torrent);
				if (enc == null) {
					logger.finer("torrent lacks encoding field, trying to detect...");
					// we might need to query the user for this...
					try {
						// this will set the encoding field in the torrent
						LocaleTorrentUtil.getTorrentEncoding(torrent);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// if that didn't work, set the default
					enc = LocaleTorrentUtil.getCurrentTorrentEncoding(torrent);
					logger.finer("got encoding: " + enc);
					if (enc == null) {
						try {
							LocaleTorrentUtil.setDefaultTorrentEncoding(torrent);
							enc = LocaleTorrentUtil.getCurrentTorrentEncoding(torrent);
							logger.finer("setting default encoding instead: " + enc);
						} catch (LocaleUtilEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (enc != null) {
						TorrentUtils.writeToFile(torrent);
					}
				}
				TOTorrentFile[] files = torrent.getFiles();
				logger.finest("after getFiles(), file num=" + files.length);
				FileListLite[] fList = new FileListLite[files.length];
				for (int i = 0; i < files.length; i++) {
					TOTorrentFile torrentFile = files[i];
					logger.finest("adding: " + torrentFile.getRelativePath());
					byte[][] pathComponents = torrentFile.getPathComponents();
					for (byte[] bs : pathComponents) {
						logger.finest("path component: " + new String(bs));
					}

					try {
						fList[i] = new FileListLite(new String(Base64.encode(torrent.getHash())), new String(torrent.getName(), "UTF-8"), torrentFile.getRelativePath(), torrentFile.getLength(), files.length, new Date().getTime(), 1, false, false);
					} catch (UnsupportedEncodingException e) {
						System.err.println(e);
						logger.severe(e.toString());
						fList[i] = new FileListLite(new String(Base64.encode(torrent.getHash())), new String(torrent.getName()), torrentFile.getRelativePath(), torrentFile.getLength(), files.length, new Date().getTime(), 1, false, false);
					}
					if (limitedVisibility) {
						fList[i].setOneSwarmNoShare(true);
					}
					FileListFile flf = new FileListFile();
					ShareManagerTools.setSha1AndEd2k(torrent, torrentFile, flf);
					if (flf.getSha1Hash() != null) {
						fList[i].setSha1Hash(new String(Base64.encode(flf.getSha1Hash())));
					}
					if (flf.getEd2kHash() != null) {
						fList[i].setEd2kHash(new String(ShareManagerTools.base16Encode(flf.getEd2kHash())));
					}
				}
				return fList;
			} catch (TOTorrentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new FileListLite[0];

	}

	public boolean addTorrent(int torrentDownloaderHash, final FileListLite[] selectedFiles, final List<PermissionsGroup> inPerms, String path, final boolean noStream) {
		TorrentDownloaderLite inf = null;
		logger.fine("Adding torrent: " + torrentDownloaderHash);

		try {
			inf = downloaderMap.get(torrentDownloaderHash);
			if (inf == null) {
				return false;
			}
			while (!quit && inf.getDownloadState() != TorrentDownloader.STATE_FINISHED) {
				logger.finer("waiting for torrent file to download: downloaded=" + inf.getTotalRead() + " (" + inf.getPercentDone() + "%)");
				Thread.sleep(100);
			}
			if (!quit) {
				File f = inf.getFile();
				TOTorrent torrent = TorrentUtils.readFromFile(f, false);
				Set<String> selectedFilesSet = new HashSet<String>();
				if (selectedFiles != null) {
					for (FileListLite fileListLite : selectedFiles) {
						selectedFilesSet.add(fileListLite.getFileName());
					}
				}

				ArrayList<GroupBean> converted_groups = new ArrayList<GroupBean>(inPerms.size());

				for (PermissionsGroup g : inPerms) {
					try {
						converted_groups.add(PermissionsDAO.get().getGroup(g.getGroupID()));
					} catch (Exception e) {
						Debug.out("problem converting PermissionsGroup to GroupBean: " + g.getName());
						e.printStackTrace();
					}
				}

				return ShareManagerTools.addDownload(selectedFilesSet, converted_groups, path, noStream, f, torrent) != null;
			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AzureusCoreException e) {
			System.err.println(e);
			e.printStackTrace();
		} catch (TOTorrentException e) {
			System.err.println(e);
			e.printStackTrace();
		} finally {
			File file = inf.getFile();
			if (file != null) {
				file.delete();
			}
		}
		return false;
	}

	// private Download addDownload(Torrent torrent) throws DownloadException,
	// TorrentException, IOException, InterruptedException {
	// String defaultDir =
	// COConfigurationManager.getDirectoryParameter("Default save path");
	// if( defaultDir == null ) {
	// String docPath = SystemProperties.getDocPath();
	// File f = new File(docPath, "OneSwarm Downloads");
	// ConfigurationManager.getInstance().setParameter("Default save path",
	// f.getAbsolutePath());
	// defaultDir = f.getAbsolutePath();
	// }
	// return addDownload(torrent, defaultDir);
	// }
	//	
	// private Download addDownload(Torrent torrent, String path) throws
	// DownloadException, TorrentException, IOException, InterruptedException {
	// // check if the torrent already exists
	// Download d;
	// if ((d =
	// coreInterface.getDownloadManager().getDownload(torrent.getHash())) !=
	// null) {
	// logger.fine("torrent already exists!");
	// return d;
	// } else {
	//
	// File torrentFile = writeTorrentFile(torrent);
	// Torrent newTorrent =
	// coreInterface.getPluginInterface().getTorrentManager().createFromBEncodedFile(torrentFile);
	//			
	// d = coreInterface.getDownloadManager().addDownloadStopped(newTorrent,
	// torrentFile, new File(path));
	// return d;
	// }
	// }
	//
	// private File writeTorrentFile(Torrent torrent) throws TorrentException {
	//
	// File torrentMetaInfoDir = CoreInterface.getMetaInfoDir(torrent);
	//
	// File torrentFile = new File(torrentMetaInfoDir, "metainfo.torrent");
	// torrent.writeToFile(torrentFile);
	// return torrentFile;
	// }

	public void shutdown() {
		quit = true;
	}

	public String getTorrentName(int torrentDownloadID) {
		TorrentDownloaderLite inf = downloaderMap.get(torrentDownloadID);
		TOTorrent torrent;
		try {
			torrent = TorrentUtils.readFromFile(inf.getFile(), false);
		} catch (TOTorrentException e1) {
			return null;
		}
		String name;
		try {
			name = new String(torrent.getName(), "UTF-8");
			return name;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new String(torrent.getName());
		}
	}
}
