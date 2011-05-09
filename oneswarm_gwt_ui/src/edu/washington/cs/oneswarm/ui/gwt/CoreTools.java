package edu.washington.cs.oneswarm.ui.gwt;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.TorrentFile;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType;

public class CoreTools {

    private static LinkedHashMap<DownloadManager, TOTorrentFile> biggestFilesCache = new LinkedHashMap<DownloadManager, TOTorrentFile>() {
        private static final long serialVersionUID = 1L;
        private static final int MAX_ENTRIES = 100;

        protected boolean removeEldestEntry(Map.Entry<DownloadManager, TOTorrentFile> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public static DiskManagerFileInfo getDiskManagerFileInfo(TorrentFile torrentFile,
            Download download) {
        DiskManagerFileInfo[] fileInfo = download.getDiskManagerFileInfo();

        for (DiskManagerFileInfo info : fileInfo) {
            File currentFile = info.getFile();

            try {
                // Log.log("looking at: " + currentFile.getCanonicalPath());
                if (currentFile.getCanonicalPath().endsWith(torrentFile.getName())) {

                    return info;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public static TOTorrentFile getBiggestFile(DownloadManager d, boolean videoOnly) {
        /*
         * check the cache
         */
        if (biggestFilesCache.containsKey(d)) {
            return biggestFilesCache.get(d);
        }

        if (d == null) {
            Debug.out("tried to get biggest file from null download manager");
            biggestFilesCache.put(d, null);
            return null;
        }

        final TOTorrentFile[] files = d.getTorrent().getFiles();
        if (files == null || files.length == 0) {
            Debug.out("tried to get biggest file but download managerhas no files");
            biggestFilesCache.put(d, null);
            return null;
        }
        TOTorrentFile biggestAndBest = null;

        for (TOTorrentFile i : files) {
            // if (i.isSkipped()) {
            // continue;
            // }
            if (videoOnly && InOrderType.getType(i.getRelativePath()) == null) {
                continue;
            }
            if (biggestAndBest == null || i.getLength() > biggestAndBest.getLength()) {
                biggestAndBest = i;
            }
        }
        if (biggestAndBest == null) {
            biggestFilesCache.put(d, null);
            return null;
        }

        biggestFilesCache.put(d, biggestAndBest);
        return biggestAndBest;
    }

    public static TorrentFile getBiggestVideoFile(Download download) {
        TorrentFile biggestAndBest = null;
        if (download == null) {
            Debug.out("getBiggestVideoFileFor null download!!!");
            return null;
        }
        TorrentFile[] files = download.getTorrent().getFiles();
        // Log.log("file asked for does not exist, searching");
        if (files.length > 0) {

            DiskManagerFileInfo[] infos = download.getDiskManagerFileInfo();

            for (int i = 0; i < files.length; i++) {
                TorrentFile file = files[i];
                // Log.log(file.getName() + " " + file.getSize());

                if (infos[i].isSkipped())
                    continue;

                if (InOrderType.getType(file.getName()) != null) {
                    if (biggestAndBest == null) {
                        biggestAndBest = file;
                    } else if (biggestAndBest.getSize() < file.getSize()) {
                        biggestAndBest = file;
                    }
                }
            }
        }
        if (biggestAndBest != null) {
            // Log.log("biggest=" + biggestAndBest.getName() + " "
            // + biggestAndBest.getSize());
        } else {
            // Log.log("no media file found");
        }
        return biggestAndBest;
    }

    public static TorrentFile getBiggestPreviewableFile(Download download) {
        TorrentFile biggestAndBest = null;
        TorrentFile[] files = download.getTorrent().getFiles();
        // Log.log("file asked for does not exist, searching");
        if (files.length > 0) {

            DiskManagerFileInfo[] infos = download.getDiskManagerFileInfo();

            for (int i = 0; i < files.length; i++) {
                TorrentFile file = files[i];
                // Log.log(file.getName() + " " + file.getSize());

                if (infos[i].isSkipped()) {
                    continue;
                }
                InOrderType type = InOrderType.getType(file.getName());
                if (type != null
                        && (type.type == FileTypeFilter.Videos || type.jwPlayerType.equals("image") || type.type
                                .equals(FileTypeFilter.Audio))) {
                    if (biggestAndBest == null) {
                        biggestAndBest = file;
                    } else if (biggestAndBest.getSize() < file.getSize()) {
                        biggestAndBest = file;
                    }
                }
            }
        }
        if (biggestAndBest != null) {
            // Log.log("biggest=" + biggestAndBest.getName() + " "
            // + biggestAndBest.getSize());
        } else {
            // Log.log("no media file found");
        }
        return biggestAndBest;
    }

    public static TorrentFile getTorrentFile(Download download, String filePathInTorrent) {
        if (filePathInTorrent == null) {
            return null;
        }
        TorrentFile[] torrentFiles = download.getTorrent().getFiles();

        for (TorrentFile file : torrentFiles) {
            if (file.getName().equals(filePathInTorrent)) {
                return file;
            }
            // else if ((file.getName() + ".flv").equals(filePathInTorrent)) {
            // Log.log("found file: " + file.getName() + " " + file.getSize());
            // return file;
            // }

        }
        return null;
    }
}
