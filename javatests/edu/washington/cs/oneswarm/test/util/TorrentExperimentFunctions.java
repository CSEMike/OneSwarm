package edu.washington.cs.oneswarm.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.TextSearchResult;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.plugins.PluginCallback;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;

public class TorrentExperimentFunctions {
    private static Logger logger = Logger.getLogger(TorrentExperimentFunctions.class.getName());
    private final AzureusCore azCore;
    private final CoreInterface coreInterface;
    private final DownloadMonitor pht;

    public TorrentExperimentFunctions(CoreInterface coreInterface, DownloadMonitor pht) {
        azCore = AzureusCoreImpl.getSingleton();
        this.coreInterface = coreInterface;
        this.pht = pht;
    }

    public void downloadTorrentAndStart(final String url, final long at,
            final ArrayList<GroupBean> perms, final int maxul_bytes) {

        // we only use single file torrents, and are sure to remove things
        // before adding to prevent unintended seeding.
        TorrentDownloader downloader = TorrentDownloaderFactory.create(
                new TorrentDownloaderCallBackInterface() {
                    @Override
                    public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
                        switch (state) {
                        case TorrentDownloader.STATE_CANCELLED:
                        case TorrentDownloader.STATE_DUPLICATE:
                        case TorrentDownloader.STATE_ERROR:
                            logger.warning("Error during download: " + url + " / " + state + " "
                                    + inf.getError());
                            break;

                        case TorrentDownloader.STATE_FINISHED:
                            final File file = inf.getFile();
                            (new File("/tmp/expfile")).delete();
                            logger.info("Waiting until: " + (new Date(at)) + " to start");
                            (new Thread("startWaiter for download: " + url) {
                                @Override
                                public void run() {
                                    try {
                                        while (System.currentTimeMillis() < at) {
                                            Thread.sleep(100);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    logger.info("starting " + url + " @ " + (new Date()));

                                    try {
                                        TOTorrent tor = TOTorrentFactory
                                                .deserialiseFromBEncodedFile(file);
                                        byte[] hashBytes = tor.getHash();
                                        PermissionsDAO.get().setGroupsForHash(
                                                ByteFormatter.encodeString(hashBytes), perms, true);
                                        DownloadManager dm = azCore.getGlobalManager()
                                                .addDownloadManager(file.getAbsolutePath(),
                                                        "/tmp/expfile");

                                        dm.getStats().setUploadRateLimitBytesPerSecond(maxul_bytes);

                                        watch_locally(dm);
                                    } catch (TOTorrentException e) {
                                        logger.warning(e.toString());
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                            break;
                        }
                    }
                }, url);

        downloader.setDeleteFileOnCancel(true);
        downloader.start();

    }

    public synchronized void watch_locally(final DownloadManager dm) {
        if (pht == null) {
            return;
        }
        dm.addListener(new DownloadManagerListener() {
            @Override
            public void completionChanged(DownloadManager manager, boolean completed) {
            }

            @Override
            public void downloadComplete(DownloadManager manager) {
                pht.downloadFinished(dm, (System.currentTimeMillis() - start_times.get(dm)));
                start_times.remove(dm);
            }

            @Override
            public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
            }

            @Override
            public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
            }

            @Override
            public void stateChanged(DownloadManager manager, int state) {
            }
        });
        dm.addPeerListener(new DownloadManagerPeerListener() {
            final long added = System.currentTimeMillis();

            @Override
            public void peerAdded(PEPeer peer) {
                if (start_times.containsKey(dm) == false) {
                    start_times.put(dm, System.currentTimeMillis());
                }
                pht.downloadBootstrapped(dm, (System.currentTimeMillis() - added));
            }

            @Override
            public void peerManagerAdded(PEPeerManager manager) {
            }

            @Override
            public void peerManagerRemoved(PEPeerManager manager) {
            }

            @Override
            public void peerManagerWillBeAdded(PEPeerManager manager) {
            }

            @Override
            public void peerRemoved(PEPeer peer) {
            }
        }, true);
    }

    public void createRandomAndShare(String name, long sizeBytes) throws IOException,
            TOTorrentException {

        logger.info("createRandomAndShare()");

        Random r = new Random();
        File randomBytes = File.createTempFile("ost", "randombytes");
        randomBytes.deleteOnExit();
        FileOutputStream out = new FileOutputStream(randomBytes);

        byte[] chunk = new byte[16 * 1024];
        long tot = 0;
        while (tot < sizeBytes) {
            r.nextBytes(chunk);
            long howmany = Math.min(chunk.length, sizeBytes - tot);
            out.write(chunk, 0, (int) howmany);
            tot += howmany;
        }
        out.flush();
        out.close();

        logger.info("wrote " + tot + " random bytes, creating...");

        creatTorrentAndShare(randomBytes);
    }

    public void downloadAndShare(String swarmName, String url) throws MalformedURLException,
            IOException, TOTorrentException {

        logger.info("downloadAndShare()");

        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        byte[] data = new byte[16 * 1024];

        File outFile = File.createTempFile("ost", "sharescratch");
        outFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(outFile);

        InputStream in = conn.getInputStream();
        int read = 0, tot = 0;
        int last_perc = 0;
        while ((read = in.read(data)) > 0) {
            out.write(data, 0, read);
            tot += read;

            if (conn.getContentLength() > 0) {
                int perc = (tot * 100 / conn.getContentLength());
                if (perc > last_perc) {
                    last_perc = perc;
                    logger.info("HTTP download completion: " + last_perc);
                }
            }
        }
        out.flush();

        creatTorrentAndShare(outFile);
    }

    public void creatTorrentAndShare(File outFile) throws TOTorrentException, IOException {
        TOTorrentCreator creator = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength(
                outFile, new URL("http://tracker.invalid/announce"), true);
        creator.addListener(new TOTorrentProgressListener() {
            @Override
            public void reportCurrentTask(String task_description) {
                logger.info("torrent creation: " + task_description);
            }

            @Override
            public void reportProgress(int percent_complete) {
                logger.info("torrent creation completion: " + percent_complete);
            }
        });
        TOTorrent outTorrent = creator.create();
        logger.info("created torrent: " + (new String(outTorrent.getName())));
        addAndStartTorrent(outTorrent, outFile);
    }

    ConcurrentHashMap<DownloadManager, Long> start_times = new ConcurrentHashMap<DownloadManager, Long>();

    public void downloadAndStart(final String base64hash, final long delay) throws IOException,
            TOTorrentException {
        if (delay > 0) {
            Thread t = new Thread(new Runnable() {                
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                        _downloadAndStart(base64hash);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (TOTorrentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.setName("delayed download");
            t.start();
        } else {
            _downloadAndStart(base64hash);            
        }
    }

    private void _downloadAndStart(final String base64hash) throws TOTorrentException {
        int searchID = coreInterface.getF2FInterface().sendSearch("id:" + base64hash);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean requested = false;
        long timeout = 60 * 1000;

        long started = System.currentTimeMillis();
        boolean done = false;
        while (started + timeout > System.currentTimeMillis() && !done) {
            List<TextSearchResult> results = coreInterface.getF2FInterface().getSearchResult(
                    searchID);
            if (results.size() > 0 && !requested) {
                requested = true;
                final int channel_id = results.get(0).getFirstSeenChannelId();
                final int connection_id = results.get(0).getFirstSeenConnectionId();

                coreInterface.getF2FInterface().getMetaInfo(connection_id, channel_id, base64hash,
                        0, new PluginCallback<byte[]>() {
                            @Override
                            public void dataRecieved(long count) {
                                logger.finer("Read " + count);
                            }

                            @Override
                            public void errorOccured(String str) {
                                logger.warning("Error occurred during metainfo download: "
                                        + connection_id + " / " + channel_id + " / " + str);
                            }

                            @Override
                            public void progressUpdate(int percentage) {
                                logger.finer("Progress updated: " + percentage);
                            }

                            @Override
                            public void requestCompleted(byte[] bytes) {
                                try {
                                    out.write(bytes);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                logger.fine("Metainfo DL completed: " + bytes.length + " / "
                                        + base64hash);
                            }
                        });
            }
            if (out.size() > 0) {
                logger.info("Read " + out.size() + " bytes of metainfo");

                // will crash if this doesn't parse, which is what we want
                final TOTorrent torrent = TorrentUtils
                        .readFromBEncodedInputStream(new ByteArrayInputStream(out.toByteArray()));

                (new Thread("f2f dl exp start wait thread for: " + base64hash) {
                    @Override
                    public void run() {
                        try {
                            final DownloadManager dm = addAndStartTorrent(torrent);
                            logger.info("successfully added: " + base64hash);
                            watch_locally(dm);
                        } catch (IOException e) {
                            logger.warning(e.toString());
                            e.printStackTrace();
                        } catch (TOTorrentException e) {
                            logger.warning(e.toString());
                            e.printStackTrace();
                        }
                    }
                }).start();

                done = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
    }

    public DownloadManager addAndStartTorrent(TOTorrent torrent) throws TOTorrentException,
            IOException {
        return addAndStartTorrent(torrent, null);
    }

    public DownloadManager addAndStartTorrent(TOTorrent torrent, File outFile)
            throws TOTorrentException, IOException {
        File scratch = File.createTempFile("ost", "expdl");
        scratch.deleteOnExit();
        // (new FileOutputStream(scratch)).write(out.toByteArray());
        torrent.serialiseToBEncodedFile(scratch);
        if (outFile == null) {
            outFile = File.createTempFile("ost", "savedl");
        }
        outFile.deleteOnExit();

        ArrayList<GroupBean> converted_groups = new ArrayList<GroupBean>();
        converted_groups.add(GroupBean.ALL_FRIENDS);
        try {
            PermissionsDAO.get().setGroupsForHash(ByteFormatter.encodeString(torrent.getHash()),
                    converted_groups, true);
            logger.finest("add dl, groups: ");
            for (GroupBean g : converted_groups) {
                logger.finest(g.toString());
            }
            logger.finest("end groups");
        } catch (Exception e) {
            e.printStackTrace();
            Debug.out("couldn't set perms for swarm! " + torrent.getName());
        }

        logger.info("set permissions");

        return azCore.getGlobalManager().addDownloadManager(scratch.getAbsolutePath(),
                outFile.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    public void cleardls() {
        for (DownloadManager dm : (List<DownloadManager>) azCore.getGlobalManager()
                .getDownloadManagers()) {
            try {
                azCore.getGlobalManager().removeDownloadManager(dm, true, true);
            } catch (Exception e) {
                logger.warning("Failed to clear dls: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
