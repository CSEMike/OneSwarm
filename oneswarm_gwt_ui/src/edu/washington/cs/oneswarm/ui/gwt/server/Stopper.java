package edu.washington.cs.oneswarm.ui.gwt.server;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class Stopper extends Thread implements Runnable {
    private boolean stopped = false;
    TorrentInfo[] torrents;
    CoreInterface core;

    public Stopper(TorrentInfo[] torrents, CoreInterface core) {
        this.torrents = torrents;
        this.core = core;
    }

    public void setStopped(boolean set) {
        stopped = set;
    }

    public boolean getStopped() {
        return stopped;
    }

    public void updateFields(TorrentInfo[] torrents, CoreInterface core) {
        this.torrents = torrents;
        this.core = core;
    }

    Thread stopper = new Thread(new Runnable() {
        public void run() {
            while (true) {
                try {
                    if (stopped) {
                        // Object[] torrents =
                        // AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers().toArray();
                        String[] torrentIds = new String[torrents.length];
                        for (int i = 0; i < torrents.length; i++) {
                            torrentIds[i] = torrents[i].getTorrentID();
                            core.stopDownload(torrentIds[i]);
                            System.out.println("Stopped!");
                        }
                        /*
                         * for (int i = 0;i < torrents.length;i++) {
                         * ((DownloadManager) torrents[i]).stopIt(NORM_PRIORITY,
                         * false, false); }
                         */
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public void run() {
        stopper.start();
    }
}
