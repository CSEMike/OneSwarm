package edu.washington.cs.oneswarm.f2ftest;

import java.net.InetSocketAddress;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.Log;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

public class SearchTester {

    private final static byte[] infohash = Base64
            .decode(new String("8KiDKijBtD0Wvtm8dbwmslnagoA="));
    private static AzureusCore core;

    public static void main(String[] args) {
        COConfigurationManager.setParameter("TCP.Listen.Port", 12345);
        COConfigurationManager.setParameter("Max Upload Speed KBs", 0);
        COConfigurationManager.setParameter("Auto Upload Speed Enabled", 0);
        COConfigurationManager.setParameter("Auto Upload Speed Debug Enabled", 0);
        COConfigurationManager.setParameter("AutoSpeed Available", 0);

        core = AzureusCoreImpl.create();
        core.start();
        COConfigurationManager.setParameter("TCP.Listen.Port", 12345);
        COConfigurationManager.setParameter("Max Upload Speed KBs", 0);
        COConfigurationManager.addParameterListener("Max Upload Speed KBs",
                new ParameterListener() {

                    public void parameterChanged(String parameterName) {
                        System.out.println("upload speed changed: new speed "
                                + COConfigurationManager.getIntParameter("Max Upload Speed KBs"));

                    }
                });
        COConfigurationManager.setParameter("Auto Upload Speed Enabled", 0);
        COConfigurationManager.setParameter("Auto Upload Speed Debug Enabled", 0);
        COConfigurationManager.setParameter("AutoSpeed Available", 0);

        new SearchTester();

        System.out.println("CLIENT: done");
        core.requestStop();
    }

    /*
     * public SearchTester() { try { IPCInterface ipc =
     * core.getPluginManager().getPluginInterfaceByID( "osf2f").getIPC();
     * Integer res = (Integer) ipc.invoke("add", new Object[] { 5, 7 });
     * System.out.println("res=" + res); ipc.invoke("sendFileListRequest", new
     * Object[] {});
     * 
     * if (main.getOverlayManager().getFilelistManager().getMetainfoHash(
     * INFOHASHHASH) == null) { for (FriendConnection friendConnection : conn) {
     * friendConnection.sendMetaInfoRequest(
     * OSF2FMessage.METAINFO_TYPE_BITTORRENT, infohash); } Thread.sleep(10000);
     * 
     * }
     * 
     * Thread.sleep(600 * 1000); } catch (InterruptedException e) { // TODO
     * Auto-generated catch block e.printStackTrace(); } }
     * 
     * private void startDownload() { DownloadManager downloadManager =
     * TorrentUtils .getDownloadManager(new HashWrapper(infohash));
     * conn.get(0).setFriendToFriendOnly(downloadManager); PEPeerManager manager
     * = downloadManager.getPeerManager(); if (manager == null) {
     * downloadManager.initialize(); downloadManager.addListener(new
     * DownloadManagerListener() {
     * 
     * public void completionChanged(DownloadManager manager, boolean completed)
     * { // TODO Auto-generated method stub
     * 
     * }
     * 
     * public void downloadComplete(DownloadManager manager) { // TODO
     * Auto-generated method stub
     * 
     * }
     * 
     * public void filePriorityChanged(DownloadManager download,
     * DiskManagerFileInfo file) { // TODO Auto-generated method stub
     * 
     * }
     * 
     * public void positionChanged(DownloadManager download, int oldPosition,
     * int newPosition) { // TODO Auto-generated method stub
     * 
     * }
     * 
     * public void stateChanged(DownloadManager manager, int state) { if (state
     * == DownloadManager.STATE_READY) { manager.startDownload();
     * 
     * } else if (state == DownloadManager.STATE_SEEDING || state ==
     * DownloadManager.STATE_DOWNLOADING) { sendSearch(infohash); } else if
     * (state == DownloadManager.STATE_ERROR) {
     * manager.stopIt(DownloadManager.STATE_STOPPED, true, true);
     * System.err.println("torrent in error state, " +
     * "removing, try again later"); } Log.log("state=" + state);
     * 
     * } }); } else { sendSearch(infohash); } }
     * 
     * private void sendSearch(byte[] infoHash) {
     * 
     * }
     */
}
