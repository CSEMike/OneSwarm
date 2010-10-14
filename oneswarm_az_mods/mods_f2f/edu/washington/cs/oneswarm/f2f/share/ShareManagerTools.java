package edu.washington.cs.oneswarm.f2f.share;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerInitialisationAdapter;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentFileImpl;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;

public class ShareManagerTools
{
	private static Logger logger = Logger.getLogger(ShareManagerTools.class.getName());

	public static DownloadManager addDownload(final Set<String> selectedFiles, final ArrayList<GroupBean> inPerms, String path, final boolean noStream, File torrentFile, TOTorrent torrent) throws IOException, TOTorrentException {
		AzureusCore core = AzureusCoreImpl.getSingleton();
		if (core.getGlobalManager().getDownloadManager(torrent) != null) {
			String msg = "Trying to add download manager with hash that already exists: " + new String(torrent.getName());
			logger.warning(msg);
			throw new IOException(msg);
		}

		try {
			PermissionsDAO.get().setGroupsForHash(ByteFormatter.encodeString(torrent.getHash()), inPerms, true);
			logger.finest("add dl, groups: ");
			for (GroupBean g : inPerms) {
				logger.finest(g.toString());
			}
			logger.finest("end groups");
		} catch (Exception e) {
			e.printStackTrace();
			Debug.out("couldn't set perms for swarm! " + torrent.getName());
		}

		/**
		 * Use specified path if we have it, otherwise look for saved path. If
		 * we don't have a save path (corrupt settings?) -- we instead use
		 * docPath/OneSwarm Downloads
		 */
		if (path == null) {
			path = COConfigurationManager.getDirectoryParameter("Default save path");
			if (path == null) {
				String docPath = SystemProperties.getDocPath();
				File dlTmp = new File(docPath, "OneSwarm Downloads");
				ConfigurationManager.getInstance().setParameter("Default save path", dlTmp.getAbsolutePath());
				path = dlTmp.getAbsolutePath();
			}
		}

		final TOTorrentFile[] files = torrent.getFiles();

		final DownloadManager dm = core.getGlobalManager().addDownloadManager(torrentFile.getAbsolutePath(), torrent.getHash(), path, null, DownloadManager.STATE_WAITING, true, false, new DownloadManagerInitialisationAdapter() {
			public void initialised(DownloadManager dm) {
				DiskManagerFileInfo[] fileInfos = dm.getDiskManagerFileInfo();
				try {

					dm.getDownloadState().supressStateSave(true);

					for (int fileIndex = 0; fileIndex < fileInfos.length; fileIndex++) {
						DiskManagerFileInfo diskManagerFileInfo = fileInfos[fileIndex];
						String fileName = diskManagerFileInfo.getTorrentFile().getRelativePath();

						if (files[fileIndex].getLength() == diskManagerFileInfo.getLength()) {
							if (!selectedFiles.contains(fileName)) {
								diskManagerFileInfo.setSkipped(true);
								if (!diskManagerFileInfo.getFile(false).exists()) {
									logger.finest("setting compact since file doesn't exist");
									diskManagerFileInfo.setStorageType(DiskManagerFileInfo.ST_COMPACT);
								} else {
									logger.finest("Couldn't set compact since file exists.");
								}
							}
						} else {
							logger.finest("Skipping file: " + files[fileIndex].getRelativePath() + " since lengths disagree: " + files[fileIndex].getLength() + " / " + diskManagerFileInfo.getLength());
						}
					}
				} finally {
					dm.getDownloadState().supressStateSave(false);
					dm.getDownloadState().setBooleanAttribute(FileCollection.ONESWARM_STREAM_ATTRIBUTE, !noStream);
				}
			}
		});
		
		if (dm == null) {
			String msg = "Couldn't create download manager for torrent: " + new String(torrent.getName());
			logger.severe(msg);
			throw new IOException(msg);
		}
		logger.fine("Starting torrent download");
		return dm;
	}

	
	public static void setSha1AndEd2k(TOTorrent torrent,
			TOTorrentFile torrentFile, FileListFile f) {
		Map torrentMap = null;
		if (torrent.isSimpleTorrent()) {
			logger.finest("simple torrent: " + new String(torrent.getName()));
			try {
				if ((torrent instanceof TOTorrentImpl)) {
					torrentMap = ((TOTorrentImpl) torrent).getAdditionalInfoProperties();
				} else {
					torrentMap = (Map) torrent.serialiseToMap().get("info");
				}

			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} else if (torrentFile instanceof TOTorrentFileImpl) {
			torrentMap = ((TOTorrentFileImpl) torrentFile).getAdditionalProperties();
		} else {
			return;
		}
		if (torrentMap.containsKey(FileListFile.KEY_SHA1_HASH)) {
			byte[] hash = (byte[]) torrentMap.get(FileListFile.KEY_SHA1_HASH);
			f.setSha1Hash(hash);
			logger.finest("adding sha1 hash to '" + f.getFileName() + "': "
					+ new String(Base64.encode(hash)));
		} else {
			logger.finest("not adding sha1 hash to '" + f.getFileName()
					+ "', no key in map");
		}
		if (torrentMap.containsKey(FileListFile.KEY_ED2K_HASH)) {
			byte[] hash = (byte[]) torrentMap.get(FileListFile.KEY_ED2K_HASH);
			f.setEd2kHash(hash);
			logger.finest("adding ed2k hash to '" + f.getFileName() + "': "
					+ new String(Base64.encode(hash)));
		} else {
			logger.finest("not adding ed2k hash to '" + f.getFileName()
					+ "', no key in map");
		}
	}

	public static String baseXtoBase64(String encoded, int expectedBytes)
			throws UnsupportedEncodingException {
		return new String(Base64.encode(baseXdecode(encoded, expectedBytes)));
	}

	public static byte[] baseXdecode(String encoded, int expectedBytes)
			throws UnsupportedEncodingException {
		// check what encoding that is likely
		encoded = encoded.trim();
		int stringLength = encoded.length();
		logger.finest("trying to find base of '" + encoded + "' input bytes="
				+ stringLength + ", expected after decode=" + expectedBytes);
		/*
		 * check for base 64, base64 converts 3 input bytes to 4 output bytes
		 */
		// base64 has special rules for padding, if the string ends with "=" the expected length will be 1 more
		int base64expectedBytes = expectedBytes;
		if (encoded.endsWith("=")) {
			base64expectedBytes = expectedBytes + 1;
		}
		if (stringLength * 3 == base64expectedBytes * 4) {
			logger.finest("detected base64 encoding");
			try {
				byte[] result = Base64.decode(encoded);
				return result;
			} catch (Exception e) {
				logger.finest("tried base 64 encoding but got exception: "
						+ e.getMessage());
			}
		}
		/*
		 * base32 encodes in chunks of 40 bits, each 40 bit chunk becomes 8 characters
		 */
		if (stringLength * 5 == expectedBytes * 8) {
			logger.finest("detected base32 encoding");
			try {
				byte[] result = Base32.decode(encoded);
				return result;
			} catch (Exception e) {
				logger.finest("tried base 32 encoding but got exception: "
						+ e.getMessage());
			}
		}
		/*
		 * base16 encoding (hex)
		 */
		if (stringLength == 2 * expectedBytes) {
			logger.finest("detected base16 encoding");
			try {
				byte[] result = base16Decode(encoded);
				return result;
			} catch (Exception e) {
				logger.finest("tried base 16 encoding but got exception: "
						+ e.getMessage());
			}
		}

		throw new UnsupportedEncodingException("unable to detect base for input '"
				+ encoded + "'");
	}

	private static byte[] base16Decode(String enc) {
		enc = enc.trim().toUpperCase();
		int l = enc.length();
		int i = 0, j = 0;
		byte[] dec = new byte[l / 2];

		while (i < l) {
			byte n, b;

			b = (byte) (enc.charAt(i));
			if ((b >= (byte) ('0')) && (b <= (byte) ('9')))
				n = (byte) (b - (byte) ('0'));
			else
				n = (byte) (b - (byte) ('A') + 10);

			b = (byte) (enc.charAt(i + 1));
			if ((b >= (byte) ('0')) && (b <= (byte) ('9')))
				n = (byte) ((n << 4) + b - (byte) ('0'));
			else
				n = (byte) ((n << 4) + b - (byte) ('A') + 10);

			dec[j] = n;

			i += 2;
			j++;
		}
		return dec;
	}

	private static final byte[] HEX_CHAR_TABLE = {
		(byte) '0',
		(byte) '1',
		(byte) '2',
		(byte) '3',
		(byte) '4',
		(byte) '5',
		(byte) '6',
		(byte) '7',
		(byte) '8',
		(byte) '9',
		(byte) 'A',
		(byte) 'B',
		(byte) 'C',
		(byte) 'D',
		(byte) 'E',
		(byte) 'F'
																						 };

	public static String base16Encode(byte[] raw) {
		byte[] hex = new byte[2 * raw.length];
		int index = 0;

		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		try {
			return new String(hex, "ASCII");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public static void main(String[] args) {

		try {
			String path = new File("test").getCanonicalFile().getParentFile().getParentFile().getCanonicalPath();
			System.out.println(path);
			File logConfig = new File(path + "/oneswarm_gwt_ui/logging.properties");
			final LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(new FileInputStream(logConfig));
			logger.setLevel(Level.ALL);

			String test1 = "62D28B6452CE095302957EC862B1273A";
			String test2 = test1.toLowerCase();
			System.out.println(base16Encode(baseXdecode(test1, 16)));
			System.out.println(base16Encode(baseXdecode(test2, 16)));

			System.out.println(baseXtoBase64("73518D92C27476BAD7DEBFC7F51B9F10", 16));
			
			System.out.println(baseXtoBase64("yPZS9rqauuJSO7oDVuOiEeVstGY=", 20));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
