package edu.washington.cs.oneswarm.f2f.multisource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.commons.io.FileSystemUtils;
import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerException;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.TextSearchResult.TextSearchResponseItem;
import edu.washington.cs.oneswarm.f2f.network.F2FDownloadManager;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.network.SearchManager.TextSearchListener;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.share.ShareManagerTools;
import edu.washington.cs.oneswarm.plugins.PluginCallback;

public class Sha1SourceFinder {
    public static final String ONESWARM_AUTO_ADDED = Sha1DownloadManager.ONESWARM_AUTO_ADDED;
    private final static long MAX_METAINFO_REQUEST_RATE = 60 * 1000;
    private final static long MAX_AUTO_ADDED_SWARMS = 10;
    private final static long REMOVE_AUTO_ADDED_IF_IDLE_SEC = 5 * 60;
    private final static long MIN_TIME_BEFORE_REMOVE = 2 * 60 * 1000;
    private final SearchManager searchManager;
    private final OverlayManager overlayManager;
    private static Logger logger = Logger.getLogger(Sha1SourceFinder.class.getName());

    private final HashMap<DownloadManager, AdditionalSourceFinder> finders = new HashMap<DownloadManager, AdditionalSourceFinder>();

    private final HashMap<HashWrapper, Long> alreadyDownloading = new HashMap<HashWrapper, Long>();

    private final HashSet<HashWrapper> alreadyAdded = new HashSet<HashWrapper>();
    private final static long startTime = System.currentTimeMillis();
    private static final boolean DEBUG = false;

    public Sha1SourceFinder(OverlayManager overlayManager) {
        this.overlayManager = overlayManager;
        this.searchManager = overlayManager.getSearchManager();
        Thread t = new Thread(new AdditionalSourceFinderWorker());
        t.setName("Sha1SourceFinder");
        t.setDaemon(true);
        t.start();

        // run at app start 5 minutes in
        Timer emptyFolderTimer = new Timer();
        emptyFolderTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeOldAndEmptyFolders();
            }
        }, 5 * 60 * 1000);
    }

    @SuppressWarnings("unchecked")
    private int getAutoAddedSwarmCount() {
        List<DownloadManager> dms = AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManagers();
        // for each auto added download, that it has at least on interesting
        // sha1, if not, delete it!!!
        int count = 0;
        for (DownloadManager dm : dms) {
            if (dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
                count++;
            }
        }
        return count;
    }

    private volatile boolean checkQueued = false;

    public void queueSha1Tasks() {
        logger.finest("requesting sha1 task");
        checkQueued = true;
    }

    private boolean isHashActive(byte[] hash) {
        return AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManager(new HashWrapper(hash)) != null;
    }

    private void removeOldAndEmptyFolders() {
        logger.finer("scanning for old unused auto added folders");
        try {
            File autoAddedDir = Sha1DownloadManager.getMultiTorrentDownloadDir();
            if (autoAddedDir.isDirectory()) {
                // check for .torrent files
                File[] dotTorrents = autoAddedDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        boolean torrentFile = name.toLowerCase().endsWith(".torrent");
                        if (!torrentFile) {
                            return false;
                        }
                        return torrentFile;
                    }
                });

                logger.finest("looking for .torrent files, found: " + dotTorrents.length);
                // remove any unused ones
                for (File dotTorrent : dotTorrents) {
                    logger.finest("considering: " + dotTorrent.getCanonicalPath());
                    if (!dotTorrent.isFile()) {
                        logger.finest("not file: " + dotTorrent.getCanonicalPath());
                        continue;
                    }
                    try {
                        // all auto added torrent files have files names that is
                        // the base32 encode if the info hash
                        String name = dotTorrent.getName();
                        String id = name.substring(0, name.length() - ".torrent".length());
                        if (id.length() != 32) {
                            logger.finest("not base32: " + id + " len=" + id.length());
                            continue;
                        }
                        byte[] infoHash = Base32.decode(id);
                        if (infoHash.length == 20 && !isHashActive(infoHash)) {
                            logger.fine("removing no longer used .torrent file: " + name);
                            dotTorrent.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                File[] directories = autoAddedDir.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        if (!pathname.isDirectory()) {
                            return false;
                        }
                        if (pathname.getName().length() != 32) {
                            return false;
                        }
                        return true;
                    }
                });

                logger.finest("looking for empty folders, candidates: " + directories.length);
                for (File dir : directories) {
                    try {
                        byte[] infoHash = Base32.decode(dir.getName());
                        if (infoHash.length == 20 && !isHashActive(infoHash)) {
                            logger.fine("removing empty auto added download dir: " + dir.getName());
                            recursiveRemoveEmptyDirectory(dir.getAbsolutePath(), dir);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void recursiveRemoveEmptyDirectory(String canonicalpreefix, File rootDir)
            throws IOException {
        // check that we didn't take a symlink outside our base folder
        if (!rootDir.getCanonicalPath().startsWith(canonicalpreefix)) {
            return;
        }

        if (!rootDir.isDirectory()) {
            return;
        }
        // do a depth first search and remove empty folders
        File[] children = rootDir.listFiles();
        for (File child : children) {
            recursiveRemoveEmptyDirectory(canonicalpreefix, child);
        }
        if (rootDir.listFiles().length == 0) {
            // if the dir is empty, delete
            logger.finer("deleting: " + rootDir.getCanonicalPath());
            rootDir.delete();
        }
    }

    @SuppressWarnings("unchecked")
    private void checkForUnusedAutoAddedAndDeleteThem(boolean pauseEncouraged) {
        logger.finest("checking for unused autoadded torrents");
        HashSet<HashWrapper> interestingSha1s = getInterestingSha1s();

        boolean dmRemoved = false;
        List<DownloadManager> dms = AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManagers();
        // for each auto added download, check that it has at least one
        // interesting sha1, if not, delete it!!!
        dm_loop: for (DownloadManager dm : dms) {

            if (dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
                HashWrapper[] hashesFromDownload = Sha1DownloadManager.getHashesFromDownload(dm,
                        FileListFile.KEY_SHA1_HASH, false);

                // check if this dm still has something to give, that is
                // if we got any data from it in the last 5 minutes
                boolean active = dm.getStats().getTimeSinceLastDataReceivedInSeconds() < REMOVE_AUTO_ADDED_IF_IDLE_SEC;
                boolean justStarted = System.currentTimeMillis() - startTime < MIN_TIME_BEFORE_REMOVE;
                boolean justAdded = System.currentTimeMillis() - dm.getStats().getTimeStarted() < MIN_TIME_BEFORE_REMOVE;
                boolean properState = dm.getState() == DownloadManager.STATE_DOWNLOADING
                        || dm.getState() == DownloadManager.STATE_SEEDING;

                // for the auto added download to be in good standing
                // it must have a sha1 peer pointing at a non-auto-added
                // download
                // and that download must have a sha1 peer pointing at it
                boolean hasProperVirtualPeers = false;
                if (!justStarted && properState) {
                    PEPeerManager peerManager = dm.getPeerManager();
                    if (peerManager != null) {
                        List peers = peerManager.getPeers();
                        good_standing_loop: for (Object o : peers) {
                            if (o instanceof Sha1Peer) {
                                Sha1Peer localPeer = (Sha1Peer) o;

                                DownloadManager srcDownloadManager = localPeer
                                        .getSourceDownloadManager();
                                // only non auto added downloads count
                                if (srcDownloadManager.getDownloadState().getBooleanAttribute(
                                        ONESWARM_AUTO_ADDED)) {
                                    continue;
                                }
                                PEPeerManager srcPeerManager = srcDownloadManager.getPeerManager();
                                if (srcPeerManager == null) {
                                    continue;
                                }
                                for (Object o2 : srcPeerManager.getPeers()) {
                                    if (o2 instanceof Sha1Peer) {
                                        Sha1Peer s2 = (Sha1Peer) o2;
                                        if (s2.getSourceDownloadManager().equals(dm)) {
                                            hasProperVirtualPeers = true;
                                            break good_standing_loop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                boolean interestingSha1Files = false;
                for (HashWrapper h : hashesFromDownload) {
                    if (interestingSha1s.contains(h)) {
                        interestingSha1Files = true;
                        break;
                    }
                }

                if (interestingSha1Files && dm.getState() != DownloadManager.STATE_ERROR) {
                    // continue means that we keep the download
                    if (justAdded) {
                        continue dm_loop;
                    }
                    if (justStarted) {
                        continue dm_loop;
                    }
                    // if it is not just started or we just started oneswarm,
                    // make sure that it is in proper state, currently
                    // downloading, that the virtual peers are set up
                    if (properState && hasProperVirtualPeers && active) {// &&
                        // !completed)
                        // {
                        continue dm_loop;
                    }
                }

                // no interesting sha1s found, delete!!!
                try {
                    logger.finer(dm.getDisplayName()
                            + " is no longer needed, removing: properState=" + properState
                            + " virtualPeers=" + hasProperVirtualPeers + " active=" + active
                            + " insterestingSha1=" + interestingSha1Files);

                    TOTorrent torrent = dm.getTorrent();
                    boolean simple = torrent.isSimpleTorrent();
                    String id = new String(Base32.encode(torrent.getHash()));

                    DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();
                    File[] filesToDelete = new File[files.length];
                    for (int i = 0; i < files.length; i++) {
                        filesToDelete[i] = files[i].getFile(true);
                    }
                    AzureusCoreImpl.getSingleton().getGlobalManager()
                            .removeDownloadManager(dm, true, false);
                    dmRemoved = true;

                    File torrentFile = new File(Sha1DownloadManager.getMultiTorrentDownloadDir(),
                            id + ".torrent");
                    if (torrentFile.isFile()) {
                        torrentFile.delete();
                    }

                    // now, delete each file, slighly different checks depending
                    // on simple torrent or not
                    if (simple) {
                        // one file, parent should be the infohash
                        if (filesToDelete.length != 1) {
                            logger.warning("tried to delete files from simple torrent but there are more that 1 file!");
                            continue dm_loop;
                        }
                        File toDelete = filesToDelete[0];
                        File parent = toDelete.getCanonicalFile().getParentFile();
                        if (parent == null || !parent.getName().equals(id)) {
                            logger.warning("tried to delete files from simple torrent but the directory has changed! '"
                                    + parent + "'");
                            continue dm_loop;
                        }
                        if (toDelete.isFile()) {
                            logger.finer("deleting file: " + toDelete);
                            toDelete.delete();
                        }
                        if (parent.list().length == 0) {
                            logger.finer("directory empty, deleting it as well " + parent);
                            parent.delete();
                        }
                    } else {
                        // not simple
                        if (filesToDelete.length == 0) {
                            logger.warning("no files to delete");
                            continue dm_loop;
                        }

                        // the parents parent should be the id
                        File parent = filesToDelete[0].getCanonicalFile().getParentFile();
                        if (parent == null) {
                            logger.warning("parent file is null!");
                            continue dm_loop;
                        }
                        File grandParent = parent.getParentFile();
                        if (grandParent == null || !grandParent.getName().equals(id)) {
                            logger.warning("tried to delete files from torrent but the directory has changed! grandparent='"
                                    + parent + "'");
                            continue dm_loop;
                        }

                        for (int i = 0; i < filesToDelete.length; i++) {
                            File toDelete = filesToDelete[i];
                            logger.finer("deleting file: " + toDelete);
                            toDelete.delete();
                        }

                        if (parent != null && parent.isDirectory() && parent.list().length == 0) {
                            logger.finer("deleting parent dir: " + parent);
                            parent.delete();
                        }

                        if (grandParent != null && grandParent.isDirectory()
                                && grandParent.list().length == 0) {
                            logger.finer("deleting grand parent dir: " + grandParent);
                            grandParent.delete();
                        }
                    }

                } catch (AzureusCoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (GlobalManagerDownloadRemovalVetoException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (TOTorrentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        /*
         * if we removed a download manager we are done for this round, we can
         * do the fine tuning next round
         */
        if (dmRemoved) {
            return;
        }

        // next step, check for individual files that no longer are interesting
        // (maybe we completed it)
        long totalSpaceToSave = 0;
        for (DownloadManager dm : dms) {
            try {
                if (dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
                    HashWrapper[] hashesFromDownload = Sha1DownloadManager.getHashesFromDownload(
                            dm, FileListFile.KEY_SHA1_HASH, false);
                    DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();
                    TOTorrent torrent = dm.getTorrent();
                    String id = new String(Base32.encode(torrent.getHash()));
                    boolean hasPaused = false;
                    boolean resumeNeeded = false;
                    for (int i = 0; i < hashesFromDownload.length; i++) {
                        DiskManagerFileInfo file = files[i];
                        HashWrapper hash = hashesFromDownload[i];
                        if (!file.isSkipped() && !interestingSha1s.contains(hash)) {
                            totalSpaceToSave += file.getLength();
                            // file is no longer interesting, consider to delete
                            File f = file.getFile(true);
                            logger.finest("found individual file that no longer is interesting, considering for deletion: "
                                    + f);
                            // check if we need to pause or if we are allowed to
                            // pause
                            if (pauseEncouraged || dm.isPaused()) {
                                // need to pause the dm if we didn't already
                                if (!hasPaused) {
                                    resumeNeeded = dm.pause();
                                }

                                // the parent should be the id
                                File parent = f.getCanonicalFile().getParentFile();
                                if (parent == null) {
                                    logger.warning("parent file is null!");
                                    continue;
                                }
                                if (parent == null || !parent.getName().equals(id)) {
                                    logger.warning("tried to compact files from torrent but the directory has changed! parent='"
                                            + parent + "'");
                                    continue;
                                }
                                logger.finer("compacting file: " + f);
                                /*
                                 * setting the file to compact will remove
                                 * everything except the first and last piece as
                                 * they might be needed for the adjacent files
                                 */
                                file.setSkipped(true);
                                file.setStorageType(DiskManagerFileInfo.ST_COMPACT);
                            }
                        }
                    }
                    if (resumeNeeded) {
                        dm.resume();
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TOTorrentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /*
         * call the function again if we can save enough space
         */
        if (totalSpaceToSave > 1024 * 1024 * 1024) {
            checkForUnusedAutoAddedAndDeleteThem(true);
        }
    }

    public void searchForAlternativeSources(DownloadManager dm) {
        if (COConfigurationManager.getBooleanParameter("oneswarm.multi.torrent.enabled") == false) {
            logger.fine("skipping sha1 additional sources download");
            return;
        }

        if (dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
            Debug.out("requested search for additional sources for autoadded download!!!");
            return;
        }
        logger.fine("searching for additional sources for: " + dm.getDisplayName());
        AdditionalSourceFinder finder = null;
        synchronized (Sha1SourceFinder.this) {
            finder = finders.get(dm);
            if (finder == null) {
                logger.finer("creating multi torrent source finder");
                /*
                 * need to create a new source finder for this download
                 */
                DownloadManagerState downloadState = dm.getDownloadState();
                if (downloadState == null) {
                    logger.finer("no download state, returning");
                    return;
                }
                String[] sha1s = downloadState.getListAttribute(FileListFile.KEY_SHA1_HASH);
                if (sha1s == null || sha1s.length == 0) {
                    logger.finer("no sha1 hashes, returning");
                    return;
                }
                finder = new AdditionalSourceFinder(dm);
                finders.put(dm, finder);
            }
        }
        if (getAutoAddedSwarmCount() < MAX_AUTO_ADDED_SWARMS) {
            finder.searchForAdditionalSources();
        }
    }

    /**
     * adds a non autoadded download without any files selected
     * 
     * @param metainfo
     */

    private void addDownload(String originalTorrentName, byte[] metainfo) {
        if (COConfigurationManager.getBooleanParameter("oneswarm.multi.torrent.enabled") == false) {
            logger.fine("skipping sha1 additional sources download");
            return;
        }
        if (getAutoAddedSwarmCount() >= MAX_AUTO_ADDED_SWARMS) {
            logger.finer("not adding download (max auto added num reached)");
            return;
        }
        try {

            TOTorrent torrent = TorrentUtils.readFromBEncodedInputStream(new ByteArrayInputStream(
                    metainfo));
            logger.fine("auto adding download, torrent=" + originalTorrentName + " added="
                    + new String(torrent.getName()));

            String torrentHashString = Base32.encode(torrent.getHash());
            String id = new String(torrentHashString);
            String shortId = id.substring(0, 6);
            String newName = originalTorrentName + " (additional source " + shortId + ")";

            File f = new File(Sha1DownloadManager.getMultiTorrentDownloadDir(), torrentHashString
                    + ".torrent");
            TorrentUtils.writeToFile(torrent, f);
            ArrayList<GroupBean> permissions = new ArrayList<GroupBean>();
            permissions.add(GroupBean.ALL_FRIENDS);
            HashSet<String> selectedFiles = new HashSet<String>();
            File downloadDir = new File(Sha1DownloadManager.getMultiTorrentDownloadDir(), id);
            if (!downloadDir.isDirectory()) {
                downloadDir.mkdir();
            }
            logger.finer("saving auto added torrent to: " + downloadDir.getCanonicalPath());
            DownloadManager dm = ShareManagerTools.addDownload(selectedFiles, permissions,
                    downloadDir.getCanonicalPath(), true, f, torrent);
            synchronized (Sha1SourceFinder.this) {
                alreadyAdded.add(new HashWrapper(torrent.getHash()));
            }
            DownloadManagerState dms = dm.getDownloadState();
            dms.setBooleanAttribute(ONESWARM_AUTO_ADDED, true);

            // listeners are notified in a separate thread, we need the sha1
            // hashes straight away so set them here if available
            Sha1HashManager.getInstance().downloadManagerAdded(dm, false);

            // and check that they seem ok
            HashWrapper[] hashes = Sha1DownloadManager.getHashesFromDownload(dm,
                    FileListFile.KEY_SHA1_HASH, false);
            if (hashes == null || hashes.length != torrent.getFiles().length) {
                logger.warning("downloaded torrent because of sha1 match but there are no (or wrong amount of) sha1 hashes in there!");
                AzureusCoreImpl.getSingleton().getGlobalManager()
                        .removeDownloadManager(dm, true, true);
                return;
            }
            dms.setDisplayName(newName);
            logger.finest("setting name to: " + newName);
            //
            dm.renameDownload(id);
            logger.finest("setting save path to: " + id);
            /*
             * now, figure out which files that are useful
             */
            searchExistingAutoAddedDownloads();

            // do a check again after we clean up any unused files
            queueSha1Tasks();
        } catch (TOTorrentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (AzureusCoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GlobalManagerDownloadRemovalVetoException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DownloadManagerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    private void searchExistingAutoAddedDownloads() {
        if (COConfigurationManager.getBooleanParameter("oneswarm.multi.torrent.enabled") == false) {
            logger.fine("skipping sha1 additional sources download");
            return;
        }
        logger.finer("searching existing auto-added downloads for interesting files that are skipped");
        /*
         * for each normal download that is currently downloading, get the sha1
         * hashes we are interested in
         */
        HashSet<HashWrapper> interestingSha1s = getInterestingSha1s();

        List<DownloadManager> dms = AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManagers();
        long totalDiskSpaceUsed = 0;
        for (DownloadManager dm : dms) {
            if (dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
                DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isSkipped()) {
                        totalDiskSpaceUsed += files[i].getLength();
                    }
                }
            }
        }
        logger.finer("using " + (totalDiskSpaceUsed / 1024 / 1024)
                + " MB for autoadded downloads already");

        // this value is a bit tricky to explain, a value of 1 means that we
        // will use as much for temp torrents as we have free space (after we
        // allocated it all...)
        double maxRatioOfFreeSpaceToUse = COConfigurationManager
                .getFloatParameter("oneswarm.max.multi.torrent.auto.disk.space");
        for (DownloadManager dm : dms) {
            if (dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
                try {
                    File saveLocation = dm.getAbsoluteSaveLocation().getCanonicalFile()
                            .getParentFile();
                    if (!dm.getTorrent().isSimpleTorrent()) {
                        saveLocation = saveLocation.getParentFile();
                    }

                    long freeSpace = 0;
                    try {
                        saveLocation.getUsableSpace();
                    } catch (Throwable e) {
                        // for java 5
                    }
                    if (freeSpace == 0) {
                        if (FileUtil.getUsableSpaceSupported()) {
                            logger.finest("using FileUtil for free space");
                            freeSpace = FileUtil.getUsableSpace(saveLocation);
                        }
                        if (freeSpace == 0) {
                            logger.finest("using FileSystemUtil for free space");
                            freeSpace = 1024 * FileSystemUtils.freeSpaceKb(saveLocation
                                    .getCanonicalPath());
                        }
                    }
                    logger.finest("free space at " + saveLocation.getAbsolutePath() + " "
                            + (freeSpace / 1024 / 1024) + "MB");

                    HashWrapper[] hashesFromDownload = Sha1DownloadManager.getHashesFromDownload(
                            dm, FileListFile.KEY_SHA1_HASH, false);
                    DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();
                    // check all pieces and set the ones we are interested in to
                    // non
                    // skip
                    for (int i = 0; i < hashesFromDownload.length; i++) {
                        DiskManagerFileInfo file = files[i];
                        HashWrapper hash = hashesFromDownload[i];
                        if (file.isSkipped() && interestingSha1s.contains(hash)) {

                            logger.finer("considering file: "
                                    + file.getTorrentFile().getRelativePath());
                            long fileSize = file.getTorrentFile().getLength();
                            logger.finest("file size: " + (fileSize / 1024 / 1024) + "MB");

                            double limit = freeSpace * maxRatioOfFreeSpaceToUse;
                            long used = totalDiskSpaceUsed + fileSize;
                            if (used > limit) {
                                logger.finer("not enabling download of file, free="
                                        + (freeSpace / 1024 / 1024) + " used="
                                        + (used / 1024 / 1024) + " limit=" + (limit / 1024 / 1024));
                                continue;
                            }
                            logger.finer("enabling download of file: "
                                    + file.getTorrentFile().getRelativePath());
                            file.setSkipped(false);
                            totalDiskSpaceUsed += fileSize;
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private HashSet<HashWrapper> getInterestingSha1s() {
        HashSet<HashWrapper> interestingSha1s = new HashSet<HashWrapper>();
        List<DownloadManager> dms_ = AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManagers();

        for (DownloadManager dm : dms_) {
            if (dm.getState() == DownloadManager.STATE_DOWNLOADING && !dm.isDownloadComplete(false)
                    && !dm.getDownloadState().getBooleanAttribute(ONESWARM_AUTO_ADDED)) {
                HashWrapper[] hashesFromDownload = Sha1DownloadManager.getHashesFromDownload(dm,
                        FileListFile.KEY_SHA1_HASH, false);
                DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();
                for (int i = 0; i < hashesFromDownload.length; i++) {
                    DiskManagerFileInfo file = files[i];
                    if (file.isSkipped()) {
                        continue;
                    }
                    if (file.getDownloaded() == file.getLength()) {
                        continue;
                    }
                    interestingSha1s.add(hashesFromDownload[i]);
                }
            }
        }
        return interestingSha1s;
    }

    private class AdditionalSourceFinderWorker implements Runnable {
        private long lastRun = System.currentTimeMillis();
        private final static long PERIODIC_CHECK_RATE = 60 * 1000;

        public void run() {
            try {
                while (true) {
                    Thread.sleep(5000);
                    if (checkQueued || System.currentTimeMillis() - lastRun > PERIODIC_CHECK_RATE) {
                        checkQueued = false;
                        lastRun = System.currentTimeMillis();
                        logger.finest("deleting uninteresting dms");
                        checkForUnusedAutoAddedAndDeleteThem(false);
                        /*
                         * this might have caused there to be more free space,
                         * check if we can add any new files
                         */
                        logger.finest("Checking for interesting files");
                        searchExistingAutoAddedDownloads();
                    }
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    private class AdditionalSourceFinder {
        private final static long MAX_CHECK_RATE = F2FDownloadManager.MAX_SEARCH_FREQ - 60 * 1000;
        private final static int MAX_SEARCHES_TO_SEND = 5;

        private final DownloadManager dm;
        private final String[] sha1s;
        private long lastCheck = 0;

        public AdditionalSourceFinder(DownloadManager dm) {
            this.dm = dm;
            sha1s = dm.getDownloadState().getListAttribute(FileListFile.KEY_SHA1_HASH);

        }

        public void searchForAdditionalSources() {
            long timeSinceLast = System.currentTimeMillis() - lastCheck;
            if (timeSinceLast < MAX_CHECK_RATE) {
                logger.finest("skipping sha1 source check, (last one was " + timeSinceLast / 1000
                        + " seconds ago");
                return;
            }
            lastCheck = System.currentTimeMillis();
            if (dm.getDiskManager() == null) {
                logger.finest("skipping sha1 source check (diskmanager not ready)");
                return;
            }
            DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();

            int searchesSent = 0;

            // make a shuffled list so we don't search for the same files all
            // the time
            ArrayList<Integer> shuffled = new ArrayList<Integer>(sha1s.length);
            for (int i = 0; i < sha1s.length; i++) {
                shuffled.add(i);
            }
            Collections.shuffle(shuffled);

            for (int i = 0; i < sha1s.length; i++) {
                if (!DEBUG && searchesSent == MAX_SEARCHES_TO_SEND) {
                    logger.finest("already sent " + searchesSent + " searches, breaking");
                    break;
                }
                int currentIndex = shuffled.get(i);
                DiskManagerFileInfo file = files[currentIndex];
                String sha1 = sha1s[currentIndex];

                if (file.isSkipped()) {
                    continue;
                }
                if (file.getDownloaded() == file.getLength()) {
                    continue;
                }

                // check for incomplete pieces, the file is only interesting if
                // there are incomplete pieces in the middle of the file,
                // first/last block does not count
                boolean incompleteInterestingPieces = false;
                DiskManagerPiece[] pieces = dm.getDiskManager().getPieces();
                for (int j = file.getFirstPieceNumber() + 1; j < file.getLastPieceNumber() - 1
                        && j < pieces.length; j++) {
                    if (pieces[j].isDone()) {
                        continue;
                    }
                    if (pieces[i].isSkipped()) {
                        continue;
                    }
                    incompleteInterestingPieces = true;
                }
                if (!incompleteInterestingPieces) {
                    continue;
                }

                final String torrentFileHash = dm.getDisplayName() + ": "
                        + file.getTorrentFile().getRelativePath() + " (" + sha1 + ")";
                logger.finer(torrentFileHash + ": searching for sources");
                searchesSent++;
                searchManager.sendTextSearch("sha1;" + sha1, new TextSearchListener() {
                    public void searchResponseReceived(TextSearchResponseItem r) {
                        List<FileCollection> swarms = r.getFileList().getElements();
                        logger.finest(torrentFileHash + ": got search results: " + swarms.size());
                        for (FileCollection swarm : swarms) {
                            final String infoHash = swarm.getUniqueID();
                            if (infoHash == null) {
                                logger.warning(torrentFileHash + ": infohash null!!!");
                                continue;
                            }
                            final HashWrapper hw = new HashWrapper(Base64.decode(infoHash));
                            if (AzureusCoreImpl.getSingleton().getGlobalManager()
                                    .getDownloadManager(hw) != null) {
                                logger.finest(torrentFileHash + ": already added");
                                continue;
                            }
                            Long lastAttempt = alreadyDownloading.get(hw);
                            if (lastAttempt != null
                                    && System.currentTimeMillis() - lastAttempt.longValue() < MAX_METAINFO_REQUEST_RATE) {
                                logger.finest(torrentFileHash + ": already downloading metainfo");
                                continue;
                            }
                            synchronized (Sha1SourceFinder.this) {
                                if (alreadyAdded.contains(hw)) {
                                    logger.finest("already added this torrent once, will not add again until next reboot");
                                    continue;
                                }

                                alreadyDownloading.put(hw, System.currentTimeMillis());
                            }
                            logger.finer(torrentFileHash + ": downloading metainfo, swarm="
                                    + infoHash);
                            overlayManager.sendMetaInfoRequest(r.getConnectionId(),
                                    r.getChannelId(), hw.getBytes(), 0,
                                    new PluginCallback<byte[]>() {
                                        public void requestCompleted(byte[] data) {
                                            logger.finer(torrentFileHash
                                                    + ": metainfo download completed, swarm="
                                                    + infoHash);
                                            addDownload(dm.getDisplayName(), data);
                                            synchronized (Sha1SourceFinder.this) {
                                                alreadyDownloading.remove(hw);
                                            }
                                        }

                                        public void progressUpdate(int progress) {
                                            logger.finest(torrentFileHash
                                                    + ": downloading metainfo, swarm=" + infoHash
                                                    + " progress=" + progress);
                                        }

                                        public void errorOccured(String string) {
                                        }

                                        public void dataRecieved(long bytes) {
                                        }
                                    });
                        }
                    }
                });
            }
        }
    }
}
