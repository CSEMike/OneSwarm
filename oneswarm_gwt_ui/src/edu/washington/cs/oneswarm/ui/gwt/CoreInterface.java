package edu.washington.cs.oneswarm.ui.gwt;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.torrent.TorrentFile;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.share.ShareManagerTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.server.StatelessSwarmFilter;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager.DataNotAvailableException;

public class CoreInterface {

    private boolean quit = false;

    private final PluginInterface pluginInterface;
    private final F2FInterface f2fInterface;

    private ConcurrentHashMap<RequiresShutdown, Boolean> shutdownObjects;

    private String sessionID;

    private StatelessSwarmFilter mSwarmFilter = null;

    public CoreInterface(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
        this.sessionID = generateSessionID();
        this.shutdownObjects = new ConcurrentHashMap<RequiresShutdown, Boolean>();

        this.f2fInterface = new F2FInterface(pluginInterface);
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownThread(this)));

        mSwarmFilter = new StatelessSwarmFilter(this);
    }

    public DownloadManager getDownloadManager() {
        return pluginInterface.getDownloadManager();
    }

    public String getSessionID() {
        return sessionID;
    }

    public void startDownload(String torrentID) throws DownloadException {
        startDownload(getDownload(torrentID));
    }

    public void startDownload(Download download) throws DownloadException {

        download.setForceStart(true);
        // if there is some error, force recheck
        if (download.getState() == Download.ST_ERROR) {
            download.recheckData();
            log("rechecking data");
        }

        if (download.getState() == Download.ST_STOPPED || download.getState() == Download.ST_QUEUED) {
            log("Restarting download");
            download.restart();

        } else if (download.getState() == Download.ST_READY) {
            log("starting download");
            download.start();
        }

        // PIAMOD -- we'll undo this elsewhere. we use force starts to override
        // azureus's default (incomprehensible) behavior
        // download.setForceStart(false);
    }

    public void stopDownload(String torrentID) throws DownloadException {
        stopDownload(getDownload(torrentID));
    }

    public void stopDownload(Download download) throws DownloadException {
        if (download.getState() != Download.ST_STOPPED) {
            download.stop();
        }

    }

    public Download getDownload(String torrentID) {
        if (torrentID.startsWith(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX)) {
            torrentID = torrentID.substring(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX.length());
            byte[] torrentHash = Base32.decode(torrentID);
            try {
                return pluginInterface.getDownloadManager().getDownload(torrentHash);
            } catch (DownloadException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public void shutdown() {
        this.quit = true;
        // stop everything
        for (RequiresShutdown shutdown : shutdownObjects.keySet()) {
            shutdown.shutdown();
        }
    }

    public boolean removeTorrent(String torrentID, final boolean delete_torrent,
            final boolean delete_data) {
        try {
            Download d = this.getDownload(torrentID);

            if (d == null) {
                return false;
            }

            int dl_state = d.getState();

            // check if it is stopped already
            if (dl_state != Download.ST_STOPPED) {

                d.stop();
                // if not, stop it and remove when state has changed
                d.addListener(new DownloadListener() {

                    public void positionChanged(Download download, int oldPosition, int newPosition) {
                    }

                    public void stateChanged(Download download, int old_state, int new_state) {
                        if ((new_state == Download.ST_STOPPED || new_state == Download.ST_ERROR)) {
                            download.removeListener(this);
                            try {
                                deleteStoppedDownload(download, delete_torrent, delete_data);
                            } catch (DownloadException e) {
                                e.printStackTrace();
                            } catch (DownloadRemovalVetoException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
            }

            deleteStoppedDownload(d, delete_torrent, delete_data);
            return true;
        } catch (DownloadException e) {
            e.printStackTrace();
        } catch (DownloadRemovalVetoException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void deleteStoppedDownload(Download download, boolean delete_torrent,
            boolean delete_data) throws DownloadException, DownloadRemovalVetoException {

        if (delete_torrent && delete_data) {
            download.remove(true, true);
        } else if (delete_torrent && !delete_data) {
            download.remove(true, false);
        } else if (!delete_torrent && delete_data) {
            download.getStats().deleteDataFiles();
        }

        if (delete_data && !delete_torrent) {
            download.recheckData();
        }

    }

    private String generateSessionID() {
        MessageDigest md;
        String runID = "";
        try {
            md = MessageDigest.getInstance("SHA");
            for (int i = 0; i < 1000; i++) {

                md.update((byte) Math.random());
            }

            byte[] md5Bytes = md.digest();

            String hexString = new String(Base32.encode(md5Bytes));
            runID = hexString;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return runID;
    }

    public void addShutdownObject(RequiresShutdown obj) {
        System.out.println("Adding shutdown " + obj.toString());
        shutdownObjects.put(obj, true);
    }

    public void removeShutdownObject(RequiresShutdown obj) {
        if (shutdownObjects.contains(obj)) {
            System.out.println("Removing shutdown " + obj.toString());
        }
        shutdownObjects.remove(obj);
    }

    private class ShutDownThread implements Runnable {
        private CoreInterface coreInterface;

        public ShutDownThread(CoreInterface coreInterface) {
            this.coreInterface = coreInterface;
        }

        public void run() {
            coreInterface.shutdown();
        }

    }

    public F2FInterface getF2FInterface() {
        return f2fInterface;
    }

    public PluginInterface getPluginInterface() {

        return pluginInterface;
    }

    public static File getMetaInfoDir(Torrent torrent) throws TorrentException {
        return getMetaInfoDir(torrent.getHash());
    }

    public static File getMetaInfoDir(byte[] torrentHash) throws TorrentException {
        String oneSwarmMetaInfoDir = SystemProperties.getMetaInfoPath();

        String torrentHex = new String(Base32.encode(torrentHash));
        char firstChar = torrentHex.charAt(0);

        String torrentMetaInfoDirString = oneSwarmMetaInfoDir + SystemProperties.SEP + firstChar
                + SystemProperties.SEP + torrentHex;
        File torrentMetaInfoDir = new File(torrentMetaInfoDirString);
        torrentMetaInfoDir.mkdirs();
        if (torrentMetaInfoDir.isDirectory()) {
            return torrentMetaInfoDir;
        }
        return null;
    }

    public File getImageFile(Download download) throws TorrentException {
        TorrentFile activeFile = CoreTools.getBiggestPreviewableFile(download);
        if (activeFile == null) {
            return null;
        }
        DiskManagerFileInfo fileInfo = CoreTools.getDiskManagerFileInfo(activeFile, download);
        if (fileInfo == null) {
            return null;
        }
        try {
            return FFMpegAsyncOperationManager.getInstance().getPreviewImage(
                    download.getTorrent().getHash(), fileInfo.getFile(), 0, TimeUnit.MILLISECONDS);
        } catch (DataNotAvailableException e) {
            return null;
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    /**
     * 
     * @param hash
     *            base32
     * @return true if this is one of our friend's files (i.e., we haven't
     *         started a download, but we want to show in the UI)
     */
    public boolean isF2FHash(String hash) {
        if (hash.startsWith(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX)) {
            hash = hash.substring(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX.length());
        }
        org.gudy.azureus2.core3.download.DownloadManager dm = AzureusCoreImpl.getSingleton()
                .getGlobalManager().getDownloadManager(new HashWrapper(Base32.decode(hash)));
        return dm == null; // == null -> we don't have it -> f2f.
    }

    public StatelessSwarmFilter getSwarmFilter() {
        return mSwarmFilter;
    }

    public String getRemoteAccessRate() {
        if (remoteAccessForward == null) {
            return "0";
        } else {
            long total = 0;
            List<Map<String, String>> stats = remoteAccessForward.getRemoteAccessStats();
            for (Map<String, String> s : stats) {
                long rate = Long.parseLong(s.get("upload_rate"));
                total += rate;
            }
            return "" + total;
        }
    }

    private RemoteAccessForward remoteAccessForward;

    public void setRemoteAccess(RemoteAccessForward remoteAccessForward) {
        this.remoteAccessForward = remoteAccessForward;
    }

    public String getRemoteAccessIps() {
        if (remoteAccessForward == null) {
            return "";
        } else {
            HashSet<String> ips = new HashSet<String>();
            List<Map<String, String>> stats = remoteAccessForward.getRemoteAccessStats();
            for (Map<String, String> s : stats) {
                String ip = s.get("remote_ip");
                String host = s.get("remote_dns");
                if (host == null) {
                    ips.add(ip);
                } else {
                    ips.add(ip + " (" + host + ")");
                }
            }

            StringBuilder b = new StringBuilder();
            for (String ip : ips) {
                b.append(ip + ", ");
            }
            if (b.length() > 2) {
                return b.toString().substring(0, b.length() - 2);
            } else {
                return "";
            }
        }
    }

    public RemoteAccessForward getRemoteAccessForward() {
        return remoteAccessForward;
    }
}
