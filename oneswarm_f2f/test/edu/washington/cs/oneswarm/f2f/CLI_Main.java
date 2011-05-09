package edu.washington.cs.oneswarm.f2f;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

class CLI_Main {
    AzureusCore mCore;

    static CLI_Main mInst = null;

    public static CLI_Main get() {
        return mInst;
    }

    File mErr = null, mOut = null;

    public File getOut() {
        return mOut;
    }

    public File getErr() {
        return mErr;
    }

    public CLI_Main(String[] args) {
        try {
            mErr = File.createTempFile("ost", "err");
            mOut = File.createTempFile("ost", "out");

            // skip this for now.
            // System.out.println("tail -f " + mOut.getAbsolutePath());
            // System.out.println("tail -f " + mErr.getAbsolutePath());
            //
            // System.setOut(new PrintStream( new FileOutputStream(mOut), true
            // ));
            // System.setErr(new PrintStream( new FileOutputStream(mErr), true
            // ));
        } catch (IOException e) {
            System.err.println("Couldn't redirect stdout, err. " + e);
            e.printStackTrace();
            System.exit(-1);
        }

        // this makes sure we're the only instance running on this machine.
        // DEBUG -- removed for local testing
        // (new Thread(new ReceiveOrders(4312))).start();
        // final SendStatus status = new SendStatus("recycle.cs.washington.edu",
        // 4312);

        final String[] args_shadow = args;

        byte[] torrent_bytes = null;

        boolean keep_last = false, keep_curr = false;
        int rate = 1024; // 1 MBps
        int wait_after_finished_secs = 0; // default is immediately quit
        double wait_after_ratio = Double.MAX_VALUE;

        String outfile = null;

        // COConfigurationManager.setParameter("dht.logging", false);

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-file")) {
                    FileInputStream fis = new FileInputStream(args[++i]);
                    torrent_bytes = new byte[fis.available()];
                    fis.read(torrent_bytes);
                } else if (args[i].equals("-bytes")) {
                    torrent_bytes = args[++i].getBytes();
                } else if (args[i].equals("-keep_last")) {
                    keep_last = true;
                } else if (args[i].equals("-keep_curr")) {
                    keep_curr = true;
                } else if (args[i].equals("-outfile")) {
                    outfile = args[++i];
                } else if (args[i].equals("-rate")) {
                    rate = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-stay")) {
                    wait_after_finished_secs = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-ratio")) {
                    wait_after_ratio = Double.parseDouble(args[++i]);
                }
            }
        } catch (IOException e) {
            System.err.println("Couldn't load torrent: " + e);
            System.exit(-1);
        }

        COConfigurationManager.preInitialise();

        impose_settings(rate);

        long start = System.currentTimeMillis();
        mCore = AzureusCoreFactory.create();
        System.out.println("starting core took: " + (System.currentTimeMillis() - start) + " ms");

        final Object ready = new Object();

        mCore.addLifecycleListener(new AzureusCoreLifecycleListener() {
            public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
                System.out.println("component created: " + component);
            }

            public boolean requiresPluginInitCompleteBeforeStartedEvent() {
                return false;
            }

            public boolean restartRequested(AzureusCore core) throws AzureusCoreException {
                return false;
            }

            public void started(AzureusCore core) {
                System.out.println("Core started");
                // DEBUG -- removed for local testing
                // (new Thread(status)).start();

                synchronized (ready) {
                    ready.notifyAll();
                }
            }

            public boolean stopRequested(AzureusCore core) throws AzureusCoreException {
                return false;
            }

            public void stopped(AzureusCore core) {
            }

            public void stopping(AzureusCore core) {
            }

            public boolean syncInvokeRequired() {
                return false;
            }

        });

        mCore.start();
        try {
            synchronized (ready) {
                while (mCore.isStarted() == false)
                    ready.wait();
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(-1);
        }

        // Remove all existing download managers if they exist.
        for (DownloadManager dm : (DownloadManager[]) (mCore.getGlobalManager()
                .getDownloadManagers().toArray(new DownloadManager[0]))) {
            if (keep_last == false) {
                System.out.println("Removing existing download manager: " + dm);
                try {
                    mCore.getGlobalManager().removeDownloadManager(dm);
                } catch (AzureusCoreException e) {
                    e.printStackTrace();
                } catch (GlobalManagerDownloadRemovalVetoException e) {
                    e.printStackTrace();
                }
            }

            /**
             * If there is some weirdness during torrent add (or out of program
             * movement of torrent / data files), Azureus will simply fail
             * silently during DownloadManager recreation. Here we try to detect
             * when things are weird.
             */
            dm.addListener(new DownloadManagerListener() {
                public void completionChanged(DownloadManager manager, boolean completed) {
                }

                public void downloadComplete(DownloadManager manager) {
                }

                public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
                }

                public void positionChanged(DownloadManager download, int oldPosition,
                        int newPosition) {
                }

                public void stateChanged(DownloadManager manager, int state) {
                    System.out.println(manager + " state: " + state);
                    if (state == DownloadManager.STATE_ERROR) {
                        try {
                            mCore.getGlobalManager().removeDownloadManager(manager);
                        } catch (AzureusCoreException e) {
                            e.printStackTrace();
                        } catch (GlobalManagerDownloadRemovalVetoException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        // make sure we have this in the CP..
        // LocalIdentity.get();

        if (torrent_bytes != null)
            start_distribution(torrent_bytes, keep_curr, outfile, wait_after_finished_secs,
                    wait_after_ratio);
        else {
            System.out.println("Nothing to do explicitly...");
            try {
                // DEBUG -- removed for local testing
                // while( status.getTorrent() == null )
                // Thread.sleep(1000);
                // DEBUG -- removed for local testing
                // System.out.println(
                // "got torrent bytes from coordinator, starting...");
                // start_distribution(torrent_bytes);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        mInst = new CLI_Main(args);
    }

    private static void start_distribution(byte[] torrent_bytes, boolean keep_curr, String outfile,
            final int wait_after_finished_secs, final double wait_after_ratio) {
        try {
            File torrent = File.createTempFile("ost", "torrent");
            FileOutputStream fos = new FileOutputStream(torrent);
            fos.write(torrent_bytes);
            fos.close();
            if (keep_curr)
                torrent.deleteOnExit();

            File saved = null;
            if (outfile == null) {
                saved = File.createTempFile("ost", "file");
                if (keep_curr)
                    saved.deleteOnExit();
            } else {
                saved = new File(outfile);
            }

            final DownloadManager dm = AzureusCoreFactory.getSingleton().getGlobalManager()
                    .addDownloadManager(torrent.getAbsolutePath(), saved.getAbsolutePath());
            dm.addListener(new DownloadManagerListener() {
                long dist_start_time = System.currentTimeMillis();

                public void completionChanged(DownloadManager manager, boolean completed) {
                }

                public void downloadComplete(DownloadManager manager) {
                    System.out.println("dl_completion "
                            + (System.currentTimeMillis() - dist_start_time) / 1000);
                    System.out.println("download completed. waiting: " + wait_after_finished_secs
                            + " / ratio: " + wait_after_ratio);

                    final long completion_time = System.currentTimeMillis();

                    (new Timer()).schedule(new TimerTask() {
                        public void run() {
                            DownloadManagerStats stats = dm.getStats();
                            double ratio = (double) stats.getTotalDataBytesSent()
                                    / (double) stats.getTotalDataBytesReceived();
                            if (ratio > wait_after_ratio) {
                                System.out.println("ratio reached: " + ratio);
                                exit();
                            }
                            if ((System.currentTimeMillis() - completion_time) > wait_after_finished_secs * 1000) {
                                System.out.println("wait after time reached: "
                                        + wait_after_finished_secs);
                                exit();
                            }
                        }
                    }, 1000, 1000);
                }

                public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
                }

                public void positionChanged(DownloadManager download, int oldPosition,
                        int newPosition) {
                }

                public void stateChanged(final DownloadManager manager, int state) {
                    System.out.println("State changed for: " + manager.getTorrentFileName()
                            + " -- " + state);
                    if (state == DownloadManager.STATE_DOWNLOADING) {
                        System.out.println("download manager initialized, adding listeners...");
                        manager.getPeerManager().addListener(new PEPeerManagerListener() {
                            public void destroyed() {
                            }

                            public void peerAdded(PEPeerManager manager, PEPeer peer) {
                                System.out.println("got peer: " + peer);
                            }

                            public void peerRemoved(PEPeerManager manager, PEPeer peer) {
                            }
                        });

                        manager.getTrackerClient().addListener(new TRTrackerAnnouncerListener() {

                            public void receivedTrackerResponse(TRTrackerAnnouncerResponse response) {
                                System.out.println("Received tracker response: "
                                        + response.getStatus() + " peers: "
                                        + response.getPeers().length);
                                dist_start_time = System.currentTimeMillis();
                            }

                            public void urlChanged(TRTrackerAnnouncer announcer, URL old_url,
                                    URL new_url, boolean explicit) {
                            }

                            public void urlRefresh() {
                            }
                        });

                        (new Timer("Download status reporter", true)).schedule(new TimerTask() {
                            public void run() {
                                DownloadManagerStats stats = manager.getStats();

                                System.out.println(" DL: "
                                        + stats.getDataReceiveRate()
                                        / 1024
                                        + " ["
                                        + stats.getTotalDataBytesReceived()
                                        / (1024)
                                        + "]"
                                        + " UL: "
                                        + stats.getDataSendRate()
                                        / 1024
                                        + " ["
                                        + stats.getTotalDataBytesSent()
                                        / (1024)
                                        + "]"
                                        + " Peers: "
                                        + manager.getPeerManager().getPeers().size()
                                        + " %: "
                                        + (double) ((double) stats.getTotalGoodDataBytesReceived() / (double) manager
                                                .getSize())
                                        * 100.0
                                        + " MaxUL: "
                                        + COConfigurationManager
                                                .getIntParameter("Max Upload Speed KBs"));
                            }
                        }, 1000, 5 * 1000);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error downloading specified swarm: " + e);
            e.printStackTrace();
        }
    }

    private static void impose_settings(int rate) {
        COConfigurationManager.setParameter("Save Torrent Files", false);
        COConfigurationManager.setParameter("Max Upload Speed KBs", rate);
        System.out.println("rate is: " + rate);
        COConfigurationManager.setParameter("LAN Speed Enabled", false);

        System.out.println("default torrent dir is: "
                + COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory"));
        COConfigurationManager.addParameterListener("General_sDefaultTorrent_Directory",
                new ParameterListener() {
                    public void parameterChanged(String parameterName) {
                        System.out.println("changed********: " + parameterName);
                        System.out.println("default torrent dir is: "
                                + COConfigurationManager
                                        .getStringParameter("General_sDefaultTorrent_Directory"));
                    }
                });
    }

    public static void exit() {
        try {
            if (AzureusCoreImpl.isCoreAvailable()) {
                System.out.println("Calling core stop");
                AzureusCoreImpl.getSingleton().stop();
            }
        } catch (Exception e) {
            System.err.println("stop exception: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
