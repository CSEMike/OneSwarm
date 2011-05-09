package edu.washington.cs.oneswarm.f2f.network;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;

import edu.uw.cse.netlab.reputation.GloballyAwareOneHopUnchoker;
import edu.washington.cs.oneswarm.f2f.Log;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1SourceFinder;

public class F2FDownloadManager {
    private final static String KEY_LAST_F2F_SEARCH = "KEY_LAST_F2F_SEARCH";
    // don't search more often that every 10 minutes
    public final static long MAX_SEARCH_FREQ = 10 * 60 * 1000;
    private final static long MAX_FORCE_SEARCH_FREQ = 5 * 1000;
    private final static int SEARCH_TIMER_PERIOD = 15 * 1000;
    private final static int INITIAL_WAIT_PERIOD = 10 * 1000;
    public static boolean logToStdOut = false;

    private static Logger logger = Logger.getLogger(F2FDownloadManager.class.getName());

    private final OverlayManager overlayManager;
    private final AzureusCore core;

    private final F2FDownloadSourceFinder sourceFinder;

    private final DownloadManagerListener downloadListener = new DownloadManagerListener() {
        public void completionChanged(DownloadManager manager, boolean completed) {
        }

        public void downloadComplete(DownloadManager manager) {
            // if a download manager is removed or completed, check all auto
            // added
            // download managers to see if they still are interesting
            sha1SourceFinder.queueSha1Tasks();
        }

        public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
        }

        public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
        }

        public void stateChanged(DownloadManager dm, int state) {
            if (state == DownloadManager.STATE_DOWNLOADING) {
                sourceFinder.sendSearch(dm, true);

                // if this is a normal download, check for additional
                // sources
                try {
                    boolean f2fAllowed = isSharedWithFriends(dm.getTorrent().getHash());
                    boolean autoadded = dm.getDownloadState().getBooleanAttribute(
                            Sha1SourceFinder.ONESWARM_AUTO_ADDED);
                    boolean running = !dm.isDownloadComplete(false);
                    boolean f2fConnected = overlayManager.getConnectCount() > 0;
                    if (f2fAllowed && !autoadded && running && f2fConnected) {
                        sha1SourceFinder.searchForAlternativeSources(dm);
                    }
                } catch (TOTorrentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };

    private final String KEY_PEER_LISTENER = "KEY_PEER_LISTENER";

    private class F2FPeerListener implements DownloadManagerPeerListener {

        private final DownloadManager dm;

        public F2FPeerListener(DownloadManager dm) {
            this.dm = dm;
        }

        public void peerAdded(PEPeer peer) {
        }

        public void peerManagerAdded(PEPeerManager manager) {
        }

        public void peerManagerRemoved(PEPeerManager manager) {
        }

        public void peerManagerWillBeAdded(PEPeerManager manager) {
        }

        @SuppressWarnings("unchecked")
        public void peerRemoved(PEPeer peer) {
            if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
                int numPeers = 0;
                int numSeeds = 0;
                List<PEPeerTransport> peers = peer.getManager().getPeers();
                for (PEPeerTransport p : peers) {
                    if (p.getPeerState() == PEPeer.TRANSFERING) {
                        if (p.isSeed()) {
                            numSeeds++;
                        } else {
                            numPeers++;
                        }
                    }
                }
                logger.fine("peer removed: peers/seeds: " + numPeers + "/" + numSeeds);
                // one or less (the removed peer is not yet removed from the
                // accounting in the DownloadManager
                if (numPeers + numSeeds <= 1) {
                    logger.fine("F2FDownloadManager: last peer removed, allowing search again");
                    // allow next search in 5 s to give all the connections and
                    // overlay transports time to disconnect and deregister
                    dm.setData(KEY_LAST_F2F_SEARCH, System.currentTimeMillis() - MAX_SEARCH_FREQ
                            + 5000);
                }
            }
        }
    };

    private Sha1SourceFinder sha1SourceFinder;

    @SuppressWarnings("unchecked")
    public F2FDownloadManager(OverlayManager _overlayManager) {
        this.overlayManager = _overlayManager;
        this.sha1SourceFinder = new Sha1SourceFinder(overlayManager);
        core = AzureusCoreFactory.getSingleton();
        sourceFinder = new F2FDownloadSourceFinder();

        // add a listener to all existing downloads
        List<DownloadManager> downloads = core.getGlobalManager().getDownloadManagers();
        for (DownloadManager d : downloads) {
            d.addListener(downloadListener);
        }

        // plus to all potential new downloads
        core.getGlobalManager().addListener(new GlobalManagerListener() {
            public void destroyInitiated() {
            }

            public void destroyed() {
            }

            public void downloadManagerAdded(DownloadManager dm) {
                dm.addListener(downloadListener);

                F2FPeerListener peerListener = new F2FPeerListener(dm);
                dm.addPeerListener(peerListener);
                dm.setData(KEY_PEER_LISTENER, peerListener);
            }

            public void downloadManagerRemoved(DownloadManager dm) {
                dm.removeListener(downloadListener);
                F2FPeerListener listener = (F2FPeerListener) dm.getData(KEY_PEER_LISTENER);
                if (listener != null && listener instanceof F2FPeerListener) {
                    dm.removePeerListener(listener);
                }
                // if a download manager is removed or completed, check all auto
                // added
                // download managers to see if they still are interesting
                sha1SourceFinder.queueSha1Tasks();
            }

            public void seedingStatusChanged(boolean seeding_only_mode) {
            }
        });

        // finally, schedule our timertask
        Timer t = new Timer("F2F peer finder", true);

        t.schedule(sourceFinder, INITIAL_WAIT_PERIOD, SEARCH_TIMER_PERIOD);

        (new Timer("torrent pruning and F2F startstop", true)).schedule(new TimerTask() {
            public void run() {

                logger.fine("F2F startstop round");

                Set<String> active_torrent_files = new HashSet<String>();
                for (DownloadManager dm : (List<DownloadManager>) core.getGlobalManager()
                        .getDownloadManagers()) {
                    active_torrent_files.add(dm.getTorrentFileName());
                    // System.out.println("adding: " + dm.getTorrentFileName() +
                    // " to active");

                    // System.out.println("considering: " +
                    // dm.getDisplayName());
                    /**
                     * things downloading (dm.getState() ==
                     * DownloadManager.STATE_DOWNLOADING ||)
                     */
                    if (!(dm.getState() == DownloadManager.STATE_SEEDING)) {
                        continue;
                    }

                    // nothing < 1 hr old
                    if (dm.getDownloadState().getLongParameter(
                            DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME) > System
                            .currentTimeMillis() - 3600000) {
                        continue;
                    }
                    // if( dm.isForceStart() )
                    // {
                    // continue;
                    // }

                    if (dm.isDownloadComplete(false) == false) {
                        continue;
                    }

                    String[] sources = dm.getDownloadState().getPeerSources();

                    /**
                     * if F2F only...
                     */
                    if (sources.length == 1 && sources[0].equals(PEPeerSource.PS_OSF2F)) {
                        logger.fine("stopping f2f only: " + dm.getDisplayName());
                        /**
                         * if no peers (after at least 5 minutes), stop
                         */
                        if (dm.getPeerManager().getPeers().size() == 0) {
                            dm.stopIt(DownloadManager.STATE_STOPPED, false, false);
                        }
                    }
                }

                logger.fine("active has: " + active_torrent_files.size());
                if (logger.isLoggable(Level.FINEST)) {
                    for (String s : active_torrent_files) {
                        logger.finest("active: " + s);
                    }
                }

                logger.fine("pruning torrent dir");
                String configSavePath = COConfigurationManager
                        .getStringParameter("General_sDefaultTorrent_Directory");
                if (configSavePath != null) {
                    File[] fileList = (new File(configSavePath)).listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".torrent");
                        }
                    });
                    if (fileList != null) {
                        for (File torrent : fileList) {
                            try {
                                TOTorrent tf = TorrentUtils.readFromFile(torrent, false);

                                if (core.getGlobalManager().getDownloadManager(tf.getHashWrapper()) != null) {
                                    continue;
                                }

                                if (torrent.lastModified() + (5 * 60 * 1000) > System
                                        .currentTimeMillis()) {
                                    logger.finer("skipping removal of too-new torrent: "
                                            + torrent.getName());
                                    continue;
                                }

                                logger.finer("removing: " + torrent.getName());
                                FileUtil.deleteWithRecycle(torrent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } // fileList != null
                    else {
                        logger.info("fileList for save path: " + configSavePath
                                + " is null, nothing to prune");
                    }
                } else {
                    logger.info("default save path is null (nothing to prune)");
                }

            }
        }, 5 * 60 * 1000, 120 * 60 * 1000);
    }

    public static boolean isSharedWithPublic(byte[] infohash) {
        DownloadManagerState state = getState(infohash);

        boolean hasPeerSource = false, hasNetwork = false;

        for (String source : state.getPeerSources()) {
            if (source.equals(PEPeerSource.PS_BT_TRACKER) || source.equals(PEPeerSource.PS_DHT)) {
                hasPeerSource = true;
            }
        }

        for (String network : state.getNetworks()) {
            if (network.equals(AENetworkClassifier.AT_PUBLIC)) {
                hasNetwork = true;
                break;
            }
        }

        return hasPeerSource && hasNetwork;
    }

    public static boolean isSharedWithFriends(byte[] infohash) {
        DownloadManagerState state = getState(infohash);

        boolean hasPeerSource = false, hasNetwork = false;

        for (String source : state.getPeerSources()) {
            if (source.equals(PEPeerSource.PS_OSF2F)) {
                hasPeerSource = true;
                break;
            }
        }

        for (String network : state.getNetworks()) {
            if (network.equals(AENetworkClassifier.AT_OSF2F)) {
                hasNetwork = true;
                break;
            }
        }

        return hasPeerSource && hasNetwork;
    }

    public static DownloadManagerState getState(byte[] infohash) {
        if (infohash == null) {
            Debug.out("infohash=null");
        }
        DownloadManager dm = getDownloadManager(infohash);

        if (dm == null) {
            Debug.out("downloadmanager=null");
        }

        DownloadManagerState state = dm.getDownloadState();
        if (state == null) {
            logger.fine("  state=null");
        }

        return state;
    }

    private static DownloadManager getDownloadManager(byte[] infohash) {
        DownloadManager dm = TorrentUtils.getDownloadManager(new HashWrapper(infohash));
        if (dm == null) {
            logger.fine("  unknown torrent, dm=null");
        }
        return dm;
    }

    public void setTorrentPrivacy(byte[] infohash, boolean publicNet, boolean f2fNet) {
        logger.fine("setting torrent privacy: pub=" + publicNet + " f2f=" + f2fNet);
        if (publicNet && f2fNet) {
            logger.fine("setTorrentPublic");
            setTorrentPublic(infohash);
        } else if (publicNet && !f2fNet) {
            logger.fine("setTorrentInternetOnly");
            setTorrentInternetOnly(infohash);
        } else if (!publicNet && f2fNet) {
            logger.fine("setTorrentFriendsOnly");
            setTorrentFriendsOnly(infohash);
        } else if (!publicNet && !f2fNet) {
            logger.fine("setTorrentPrivate");
            setTorrentPrivate(infohash);
        }
        overlayManager.getFilelistManager().scheduleFileListRefresh();
    }

    private static void setTorrentPublic(byte[] infohash) {
        logger.fine("Setting torrent Public");
        DownloadManagerState state = getState(infohash);

        // enable all peer sources
        for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
            String source = PEPeerSource.PS_SOURCES[i];
            state.setPeerSourceEnabled(source, true);
        }

        // enable all networks
        for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
            String network = AENetworkClassifier.AT_NETWORKS[i];
            state.setNetworkEnabled(network, true);
        }
    }

    private static void setTorrentInternetOnly(byte[] infohash) {
        logger.fine("Setting torrent Internet Only");

        DownloadManagerState state = getState(infohash);

        // enable all but f2f
        for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
            String source = PEPeerSource.PS_SOURCES[i];
            if (!source.equals(PEPeerSource.PS_OSF2F)) {
                state.setPeerSourceEnabled(source, true);
            } else {
                state.setPeerSourceEnabled(source, false);
            }
        }

        for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
            String network = AENetworkClassifier.AT_NETWORKS[i];
            if (!network.equals(AENetworkClassifier.AT_OSF2F)) {
                state.setNetworkEnabled(network, true);
            } else {
                state.setNetworkEnabled(network, false);
            }
        }
    }

    private static void setTorrentPrivate(byte[] infohash) {
        logger.fine("Setting torrent Private");

        DownloadManagerState state = getState(infohash);

        // disable all
        for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
            String source = PEPeerSource.PS_SOURCES[i];
            state.setPeerSourceEnabled(source, false);
        }

        for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
            String network = AENetworkClassifier.AT_NETWORKS[i];

            state.setNetworkEnabled(network, false);

        }
    }

    private static void setTorrentFriendsOnly(byte[] infohash) {
        logger.fine("Setting torrent Friends Only");

        DownloadManagerState state = getState(infohash);

        logger.fine("got state, #: " + getDownloadManager(infohash).getState());

        // set the peer sources to only be f2f
        for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {
            String source = PEPeerSource.PS_SOURCES[i];
            if (source.equals(PEPeerSource.PS_OSF2F)) {
                state.setPeerSourceEnabled(source, true);
            } else {
                state.setPeerSourceEnabled(source, false);
            }
        }

        for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
            String network = AENetworkClassifier.AT_NETWORKS[i];
            if (network.equals(AENetworkClassifier.AT_OSF2F)) {
                state.setNetworkEnabled(network, true);
            } else {
                state.setNetworkEnabled(network, false);
            }
        }

        logger.fine("done setting friends only");

    }

    // public void sendSearch(byte[] infohash) {
    // sourceFinder.sendSearch(getDownloadManager(infohash), true);
    // }

    class F2FDownloadSourceFinder extends TimerTask {

        private volatile boolean running = false;

        public F2FDownloadSourceFinder() {

        }

        @SuppressWarnings("unchecked")
        public void run() {
            if (!running) {

                try {

                    running = true;

                    List<DownloadManager> downloads = core.getGlobalManager().getDownloadManagers();
                    ArrayList<DownloadManager> toShuffle = new ArrayList<DownloadManager>(downloads);
                    Collections.shuffle(toShuffle);
                    logger.fine("running f2f source finder");
                    for (DownloadManager d : toShuffle) {
                        sendSearch(d, false);
                    }

                } finally {
                    running = false;
                }
            }
        }

        public void sendSearch(DownloadManager d, boolean forceSearch) {
            try {

                if (d == null) {
                    logger.warning("Not sending search (d==null");
                    return;
                }
                // logger.fine("processing: " + d.getDisplayName());

                if (d.getState() != DownloadManager.STATE_DOWNLOADING) {
                    // logger.fine("not sending search, state != Downloading",
                    // logToStdOut);
                    return;
                }

                if (overlayManager.getConnectCount() == 0) {
                    logger.fine("no friends connected, skipping f2f search");
                    return;
                }

                // check if we already sent a search to this destination
                // within the max time limit

                Object l = d.getData(KEY_LAST_F2F_SEARCH);
                long lastSearch;
                if (l == null) {
                    lastSearch = 0;
                } else {
                    lastSearch = (Long) l;
                }

                boolean searchAllowed = false;
                if (System.currentTimeMillis() - lastSearch > MAX_SEARCH_FREQ) {
                    searchAllowed = true;
                } else if (forceSearch
                        && System.currentTimeMillis() - lastSearch > MAX_FORCE_SEARCH_FREQ) {
                    searchAllowed = true;
                }
                if (!searchAllowed) {
                    logger.fine("not sending search, lastTime: "
                            + (System.currentTimeMillis() - lastSearch) + "<" + MAX_SEARCH_FREQ
                            + " force=" + forceSearch);
                    return;
                }
                byte[] infohash = d.getTorrent().getHash();
                DownloadManagerState state = getState(infohash);

                // if the friend network is enabled, auto send
                // searches
                if (state.isNetworkEnabled(AENetworkClassifier.AT_OSF2F)) {
                    overlayManager.getSearchManager().sendHashSearch(infohash);
                    d.setData(KEY_LAST_F2F_SEARCH, System.currentTimeMillis());
                    logger.fine("sending F2F search: " + d.getDisplayName());

                    boolean autoadded = state
                            .getBooleanAttribute(Sha1SourceFinder.ONESWARM_AUTO_ADDED);
                    if (!autoadded) {
                        logger.fine("sending sha1 search: " + d.getDisplayName());
                        sha1SourceFinder.searchForAlternativeSources(d);
                    }
                }

            } catch (TOTorrentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
