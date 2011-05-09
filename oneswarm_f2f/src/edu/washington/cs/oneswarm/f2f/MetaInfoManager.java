package edu.washington.cs.oneswarm.f2f;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;

public class MetaInfoManager {

    private static java.util.logging.Logger logger = Logger.getLogger(MetaInfoManager.class
            .getName());

    private final HashMap<Integer, Boolean> existingInfoHashes;

    public MetaInfoManager() {
        existingInfoHashes = new HashMap<Integer, Boolean>();
    }

    public byte[] getMetaInfo(Friend remoteFriend, byte type, byte[] infohash) {
        DownloadManager dm = AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManager(new HashWrapper(infohash));
        if (dm != null) {
            // it is "allowed", check permissions
            boolean allowed = PermissionsDAO.get().hasPermissions(remoteFriend.getPublicKey(),
                    infohash);
            if (allowed) {
                TOTorrent torrent = dm.getTorrent();
                switch (type) {
                case OSF2FMessage.METAINFO_TYPE_BITTORRENT:
                    try {

                        TOTorrent clone = TOTorrentFactory.deserialiseFromMap(torrent
                                .serialiseToMap());
                        if (clone.getPrivate()) {
                            String replaceWithUrl = "http://tracker.removed.was.private.torrent.invalid/announce";
                            clone.setAnnounceURL(new URL(replaceWithUrl));
                            clone.getAnnounceURLGroup().setAnnounceURLSets(
                                    new TOTorrentAnnounceURLSet[0]);
                        }
                        /*
                         * if the swarm has custom permissions (not shared with
                         * all friends) set the OneSwarmNoShare flag to 1 to
                         * indicate that the file should have special
                         * permissions
                         */
                        if (!PermissionsDAO.get().hasAllFriendsPermission(infohash)) {
                            clone.setAdditionalLongProperty(TOTorrentImpl.OS_NO_SHARE, new Long(1));
                            clone.setAdditionalLongProperty(TOTorrentImpl.OS_NO_TEXT_SEARCH,
                                    new Long(1));
                        }
                        /**
                         * Strip out anything that's specific to us locally...
                         */
                        clone.removeAdditionalProperties();

                        if (logger.isLoggable(Level.FINEST)) {
                            System.out.println("*************original torrent*******************");
                            torrent.print();
                            System.out.println("*************filtered torrent*******************");
                            clone.print();
                            System.out.println("************************************************");
                        }
                        Map root = clone.serialiseToMap();
                        return BEncoder.encode(root);
                    } catch (IOException e) {
                        Debug.out("failed to serialize torrent", e);
                    } catch (TOTorrentException e) {
                        Debug.out("failed to serialize torrent", e);
                    }

                    break;
                case OSF2FMessage.METAINFO_TYPE_THUMBNAIL:
                    try {

                        File imageFile = getImageFile(torrent.getHash(), false);
                        if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                            logger.fine("got image request, looking at file "
                                    + imageFile.getAbsolutePath());
                            BufferedInputStream source = new BufferedInputStream(
                                    new FileInputStream(imageFile));
                            byte[] allData = readStream(source);
                            logger.fine("read " + allData.length + " bytes");
                            return allData;
                        }
                        return new byte[0];
                    } catch (TOTorrentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                default:
                    Debug.out("metainfo type '" + type + "' not supported");
                    break;
                }
            } else {
                logger.fine("got metainfo request for " + new String(dm.getTorrent().getName())
                        + " but friend '" + remoteFriend.getNick() + "' has no access to that file");
            }

        }
        return null;
    }

    public List<byte[]> getTorrentThumbnailNeeded(List<byte[]> infohashes) {
        List<byte[]> newHashes = new LinkedList<byte[]>();
        logger.finest("getTorrentThumbnailNeeded considering: " + infohashes.size() + " hashes");
        for (byte[] infohash : infohashes) {
            if (!existingInfoHashes.containsKey(Arrays.hashCode(infohash))) {
                DownloadManager dm = AzureusCoreImpl.getSingleton().getGlobalManager()
                        .getDownloadManager(new HashWrapper(infohash));
                // don't send requests for stuff we already have data for
                if (dm != null) {
                    existingInfoHashes.put(Arrays.hashCode(infohash), true);
                    logger.finest("we already have: " + ByteFormatter.encodeString(infohash));
                } else {
                    // new hash, lets check if we have it on disk
                    try {
                        if (!hasImage(infohash)) {
                            // check if we tried this file before
                            logger.finest("checking refresh to get image for: "
                                    + ByteFormatter.encodeString(infohash));
                            if (getNextPreviewRequestTime(infohash) <= System.currentTimeMillis()) {
                                newHashes.add(infohash);
                                logger.finest("added: " + ByteFormatter.encodeString(infohash));
                            } else {
                                logger.finest("too old: " + ByteFormatter.encodeString(infohash));
                            }
                        } else {
                            existingInfoHashes.put(Arrays.hashCode(infohash), true);
                            logger.finest("we already have image for: "
                                    + ByteFormatter.encodeString(infohash));
                        }
                    } catch (TOTorrentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            } else {
                logger.finest("we already have: " + ByteFormatter.encodeString(infohash));
            }
        }
        Collections.reverse(newHashes); // newest things first
        return newHashes;
    }

    private static byte[] readStream(BufferedInputStream source) throws IOException {
        ArrayList<byte[]> data = new ArrayList<byte[]>();
        int read = 0;
        int total = 0;
        byte[] buffer = new byte[32 * 1024];
        while ((read = source.read(buffer, 0, buffer.length)) != -1) {
            byte[] toSave = new byte[read];
            System.arraycopy(buffer, 0, toSave, 0, toSave.length);
            data.add(toSave);
            total += read;
        }
        source.close();

        byte[] allData = new byte[total];
        int pos = 0;
        for (int i = 0; i < data.size(); i++) {
            byte[] b = data.get(i);
            System.arraycopy(b, 0, allData, pos, b.length);
            pos += b.length;
        }
        return allData;
    }

    private static File getMetaInfoDir(byte[] hash) throws TOTorrentException {
        String oneSwarmMetaInfoDir = SystemProperties.getMetaInfoPath();
        // String torrentHex = new String(Hex.encode(torrent.getHash()));
        String torrentHex = new String(Base32.encode(hash));
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

    private static File getImageFile(byte[] hash, boolean allowFriend) throws TOTorrentException {

        File metaInfoDir = getMetaInfoDir(hash);
        List<File> metainfoFiles = Arrays.asList(metaInfoDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return name.startsWith("preview_") && name.endsWith(".png");
            }
        }));
        Collections.sort(metainfoFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return (int) (o1.lastModified() - o2.lastModified());
            }
        });
        File friendFile = null;
        for (int i = 0; i < metainfoFiles.size(); i++) {
            File currFile = metainfoFiles.get(i);
            if (currFile.getName().equals("preview_friend.png")) {
                friendFile = currFile;
            } else {
                return currFile;
            }
        }
        if (allowFriend && friendFile != null) {
            return friendFile;
        } else {
            return null;
        }
    }

    private static boolean hasImage(byte[] hash) throws TOTorrentException {
        File imgFile = getImageFile(hash, false);

        if (imgFile != null && imgFile.exists() && imgFile.length() > 0) {
            return true;
        }
        imgFile = getImageFile(hash, true);
        if (imgFile != null && imgFile.exists() && imgFile.length() > 0) {
            return true;
        }
        return false;
    }

    private static long getNextPreviewRequestTime(byte[] hash) throws TOTorrentException,
            IOException {
        File errorfile = new File(getMetaInfoDir(hash), "preview_friend_error.log");
        long timeVal = System.currentTimeMillis() - 10;
        if (errorfile.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(errorfile));
            String line = reader.readLine();
            reader.close();
            try {
                timeVal = Long.parseLong(line);
            } catch (Exception e) {
            }
        }
        return timeVal;
    }

    private static long getLastPreviewRequestInterval(byte[] hash) throws TOTorrentException,
            IOException {
        File errorfile = new File(getMetaInfoDir(hash), "preview_friend_error.log");
        long timeVal = 900000; // 15 minutes
        if (errorfile.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(errorfile));
            String line = reader.readLine();
            line = reader.readLine();
            reader.close();
            try {
                timeVal = Long.parseLong(line);
            } catch (Exception e) {
            }
        }
        return timeVal;
    }

    public void gotImageResponse(byte[] infohash, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                // System.err.println(
                // "got image response of len 0, increment counter?");
                // wait with this for a couple of versions
                previewImageRequestFailed(infohash);
            } else {
                File imageFile = new File(getMetaInfoDir(infohash), "preview_friend.png");
                logger.fine("got image response, writing to: " + imageFile.getPath());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(imageFile));
                out.write(data);
                out.close();
            }
        } catch (TOTorrentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void previewImageRequestFailed(byte[] hash) throws TOTorrentException,
            IOException {
        long previousInterval = 900000; // 15 minutes
        try {
            previousInterval = getLastPreviewRequestInterval(hash);
        } catch (Exception e) {
        }
        long newInterval = Math.min(previousInterval * 2, 604800000); // 1 week
        long nextTime = System.currentTimeMillis() + newInterval;
        logger.fine("request failed, previousInterval: " + previousInterval + " nextInterval: "
                + newInterval + " next time: " + (new Date(nextTime)));
        File errorFile = new File(getMetaInfoDir(hash), "preview_friend_error.log");
        FileWriter f = new FileWriter(errorFile);
        f.write("" + (nextTime) + "\n");
        f.write("" + newInterval + "\n");
        f.close();
    }

}
