package edu.washington.cs.oneswarm.f2f.friends;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.dht.DHTConnector;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanReader;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanWriter;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanReader.OSF2FXMLBeanReaderCallback;
import edu.washington.cs.publickey.PublicKeyFriend;

public class FriendManager {

    static Logger logger = Logger.getLogger(FriendManager.class.getName());

    public static File OSF2F_DIR;
    public static String OSF2F_DIR_NAME = "osf2f";
    private final static String OSF2F_FRIEND_FILE = "osf2f.friends";
    static {
        OSF2F_DIR = new File(SystemProperties.getUserPath() + File.separator + OSF2F_DIR_NAME
                + File.separator);
    }
    private final ClassLoader cl;

    private final Semaphore diskSemaphore = new Semaphore(1);

    private final FriendImportManager friendImportManager;

    private ConcurrentHashMap<FriendKey, Friend> friends;
    private Set<String> ignoreFutureFriendRequestsFrom = Collections
            .synchronizedSet(new HashSet<String>());

    private final Map<FriendKey, Long> nonFriendConnectionAttempts = Collections
            .synchronizedMap(new LinkedHashMap<FriendKey, Long>() {
                private static final int MAX_ENTRIES = 20;
                private static final long serialVersionUID = 1L;

                protected boolean removeEldestEntry(Map.Entry<FriendKey, Long> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    private final byte[] ownPublicKey;

    private volatile boolean readCompleted = false;

    public FriendManager(ClassLoader classLoader, byte[] ownPublicKey) {
        this.ownPublicKey = ownPublicKey;
        cl = classLoader;
        friends = new ConcurrentHashMap<FriendKey, Friend>();
        readFromDisk();
        registerShutdownHook();
        friendImportManager = new FriendImportManager(this);

        Timer timer = new Timer("friend write timer", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                flushToDisk(false, true, false);
            }
        }, 5 * 60 * 1000, 2 * 60 * 1000);
    }

    public int addFriend(Friend newFriend) {
        if (newFriend == null) {
            logger.warning("tried to add friend (but friend null)");
            (new Exception()).printStackTrace();
            return 0;
        }
        Friend[] newFriends = { newFriend };
        return this.addFriend(newFriends);
    }

    public int addFriend(Friend[] newFriends) {

        if (newFriends == null) {
            Debug.out("tried to add friend (but friend null)");
            return 0;
        }
        logger.finer("adding  " + newFriends.length + " friends");
        int numAdded = 0;

        for (Friend osF2FFriend : newFriends) {
            if (!Arrays.equals(ownPublicKey, osF2FFriend.getPublicKey())) {
                FriendKey fk = new FriendKey(osF2FFriend.getPublicKey());
                Friend existingFriend = friends.get(fk);
                if (existingFriend == null || existingFriend.isBlocked()) {
                    numAdded++;
                    if (osF2FFriend.getDateAdded() == null) {
                        osF2FFriend.setDateAdded(new Date());
                    } else if (osF2FFriend.getDateAdded().equals(new Date(0))) {
                        osF2FFriend.setDateAdded(new Date());
                    }
                    friends.put(fk, osF2FFriend);

                    /**
                     * PIAMOD -- syncing with the groups code here
                     */
                    PermissionsDAO.get().refresh_friend_groups();

                    if (!osF2FFriend.isBlocked()) {
                        // trigger a dht lookup of the friend
                        DHTConnector friendConnector = OSF2FMain.getSingelton().getDHTConnector();
                        if (friendConnector != null) {
                            try {
                                friendConnector.publishLocationInfoForFriend(osF2FFriend);
                            } catch (Exception e) {
                                Debug.out("problem when trying to publish friend location in dht",
                                        e);
                            }
                            friendConnector.connectToFriend(osF2FFriend);
                        } else {
                            Debug.out("friend connector=null");
                        }
                    }
                } else {
                    /*
                     * maybe we have new ip:port info for the friend, try to
                     * connect
                     */
                    if (osF2FFriend.getLastConnectIP() != null
                            && osF2FFriend.getLastConnectPort() > 0) {
                        existingFriend.setLastConnectIP(osF2FFriend.getLastConnectIP());
                        existingFriend.setLastConnectPort(osF2FFriend.getLastConnectPort());
                        // trigger a dht lookup of the friend
                        DHTConnector friendConnector = OSF2FMain.getSingelton().getDHTConnector();
                        if (friendConnector != null) {
                            try {
                                friendConnector.publishLocationInfoForFriend(osF2FFriend);
                            } catch (Exception e) {
                                Debug.out("problem when trying to publish friend location in dht",
                                        e);
                            }
                            friendConnector.connectToFriend(osF2FFriend);
                        }
                    }
                }
            } else {
                Debug.out("tried to add myself as friend");
            }

        }

        if (numAdded > 0) {
            this.flushToDisk(true, false, false);
        }
        logger.fine("added friends:" + numAdded);
        return numAdded;
    }

    public void addToIgnoreRequestList(String publicKey) {
        ignoreFutureFriendRequestsFrom.add((publicKey));
    }

    /**
     * Converts a list of friends from PublicKeyFriends objects to Friend
     * objects, if the friend name is null they are skipped
     */
    public List<Friend> convertToFriendArrayAndFilter(List<PublicKeyFriend> importedFriends) {
        LinkedList<Friend> newFriends = new LinkedList<Friend>();
        for (PublicKeyFriend publicKeyFriend : importedFriends) {
            /*
             * skip if we can't map the uid to a real name
             */
            if (publicKeyFriend.getRealName() == null) {
                Debug.out("could not get real name for keynick: " + publicKeyFriend.getKeyNick()
                        + " , this should not happen unless you are using multiple xmpp accounts");
                continue;
            }

            /*
             * skip if the public key is null
             */
            byte[] publicKey = publicKeyFriend.getPublicKey();
            if (publicKey == null) {
                continue;
            }

            /*
             * skip if the user is the local machine
             */
            if (Arrays.equals(publicKey, ownPublicKey)) {
                continue;
            }

            FriendKey key = new FriendKey(publicKey);
            /*
             * skip if we are already friends with the user
             */
            if (friends.get(key) != null) {
                continue;
            }

            /*
             * ok, all is good, add to list
             */
            String nick = publicKeyFriend.getRealName() + " (" + publicKeyFriend.getKeyNick() + ")";
            boolean canSeeFilelist = false;
            Friend newFriend = new Friend(publicKeyFriend.getSourceNetwork().getNetworkName(),
                    nick, publicKey, canSeeFilelist);
            newFriend.setRequestFileList(false);
            newFriend.setBlocked(true);
            newFriend.setNewFriend(true);
            newFriends.add(newFriend);
        }
        return newFriends;
    }

    public List<Friend> filterKnownFriends(List<Friend> allFriends) {
        LinkedList<Friend> newFriends = new LinkedList<Friend>();
        for (Friend f : allFriends) {
            byte[] publicKey = f.getPublicKey();
            if (publicKey != null && !Arrays.equals(publicKey, ownPublicKey)) {
                FriendKey key = new FriendKey(publicKey);
                if (friends.get(key) == null) {
                    newFriends.add(f);
                }
            }
        }
        return newFriends;
    }

    public void flushToDisk(final boolean makeBackup, final boolean block,
            final boolean allowDecreasedSize) {
        // check if we are running already
        if (!diskSemaphore.tryAcquire()) {
            // ok, don't sweat it, lets skip this one since the friend flush
            // thread will write this to disk every 5 min anyway
            return;
        } else {
            // ok, permits are available, lets release it
            diskSemaphore.release();
        }

        FriendBean[] b = new FriendBean[friends.size()];
        Friend[] f = friends.values().toArray(new Friend[friends.size()]);

        for (int i = 0; i < b.length; i++) {
            b[i] = new FriendBean(f[i]);
        }

        Thread t = new Thread(new OSF2FXMLBeanWriter<FriendBean>(b, diskSemaphore,
                OSF2F_FRIEND_FILE, makeBackup, allowDecreasedSize));
        t.setName("OSF2FXMLBeanWriter flushToDisk()");
        t.setContextClassLoader(cl);

        t.start();
        if (block) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        logger.finest("wrote " + b.length + " friends to disk");
    }

    public Map<byte[], String> getDeniedIncomingConnections() {
        Map<byte[], String> m = new HashMap<byte[], String>();

        synchronized (nonFriendConnectionAttempts) {
            for (FriendKey key : nonFriendConnectionAttempts.keySet()) {
                m.put(key.getPublicKey(), key.ip);
            }
        }
        return m;
    }

    public Friend getFriend(byte[] publicKey) {
        if (publicKey == null) {
            (new Exception()).printStackTrace();
            logger.warning("getFriend() called with null key");
            return null;
        }
        this.waitForRead();
        return friends.get(new FriendKey(publicKey));
    }

    public Friend getFriend(FriendBean bean) {
        try {
            Date lastConnectDate = null;
            if (bean.getLastConnectDate() != 0) {
                lastConnectDate = new Date(bean.getLastConnectDate());
            }
            Friend created = new Friend(bean.isCanSeeFileList(), bean.isAllowChat(), new Date(
                    bean.getDateAdded()), lastConnectDate, InetAddress.getByName(bean
                    .getLastConnectIP()), bean.getLastConnectPort(), bean.getNick(),
                    Base64.decode(bean.getPublicKey()), bean.getSourceNetwork(),
                    bean.getTotalDownloaded(), bean.getTotalUploaded(), bean.isBlocked(),
                    bean.isNewFriend());
            // fix this bug for old friends that didn't have it properly set.
            if (created.getDateAdded() == null) {
                created.setDateAdded(new Date());
            } else if (created.getDateAdded().equals(new Date(0))) {
                created.setDateAdded(new Date());
            }
            created.setRequestFileList(bean.isRequestFileList());
            // don't want to change the constructor...
            created.setGroup(bean.getGroup());

            if (bean.getDhtReadLocation() != null) {
                created.setDhtReadLocation(Base64.decode(bean.getDhtReadLocation()));
            }
            if (bean.getDhtWriteLocation() != null) {
                created.setDhtWriteLocation(Base64.decode(bean.getDhtWriteLocation()));
            }
            created.setDhtLocationConfirmed(bean.isDhtLocationConfirmed());
            return created;
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // /**
    // * New friends are added in the blocked state
    // *
    // * @param sourceNet
    // * @param importedFriends
    // * @return
    // */
    //

    public Friend[] getFriends() {
        this.waitForRead();
        Friend[] f = new Friend[friends.size()];
        return friends.values().toArray(f);
    }

    public List<byte[]> getKnownKeysForFriendImport() {
        List<byte[]> knownKeys = new LinkedList<byte[]>();
        for (Friend f : friends.values()) {
            /*
             * filter friends added manually from getting sent, we don't want
             * the server to know about those
             */
            if (!f.getSourceNetwork().equals("Manual") && !f.getSourceNetwork().equals("Lan")) {
                knownKeys.add(f.getPublicKey());
            }
        }
        knownKeys.add(ownPublicKey);
        return knownKeys;
    }

    // public List<Friend> getNewFriends() {
    // LinkedList<Friend> newFriends = new LinkedList<Friend>();
    // for (Friend f : friends.values()) {
    // if (f.isNewFriend()) {
    // newFriends.add(f);
    // }
    // }
    // return newFriends;
    // }

    public Map<String, Integer> getNewFriendsCountsFromAutoCheck() {
        List<PublicKeyFriend> newFriends = friendImportManager.getNewFriends();
        List<Friend> filtered = convertToFriendArrayAndFilter(newFriends);
        Map<String, Integer> countUsersPerNetwork = new HashMap<String, Integer>();

        for (Friend friend : filtered) {
            if (!isOnIgnoreRequestList(new String(Base64.encode(friend.getPublicKey())))) {
                String network = friend.getSourceNetwork();
                if (!countUsersPerNetwork.containsKey(network)) {
                    countUsersPerNetwork.put(network, 0);
                }
                // Log.log("New friend from autocheck: " + )
                countUsersPerNetwork.put(network, countUsersPerNetwork.get(network) + 1);
            }
        }

        logger.finest("returning new friends from autocheck: " + countUsersPerNetwork);
        return countUsersPerNetwork;
    }

    public void gotConnectionFromNonFriend(String ip, byte[] remotePub) {
        FriendKey key = new FriendKey(remotePub);
        key.ip = ip;
        nonFriendConnectionAttempts.put(key, System.currentTimeMillis());
    }

    public boolean hasTriedToConnect(Friend f) {
        return nonFriendConnectionAttempts.containsKey(new FriendKey(f.getPublicKey()));
    }

    public boolean isOnIgnoreRequestList(String publickey) {
        return ignoreFutureFriendRequestsFrom.contains((publickey));
    }

    private void readFromDisk() {
        try {
            logger.finer("reading friend file from disk, waiting for disk semaphore");
            diskSemaphore.acquire();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        OSF2FXMLBeanReader<FriendBean> reader = new OSF2FXMLBeanReader<FriendBean>(cl,
                FriendBean.class, OSF2F_FRIEND_FILE, diskSemaphore,
                new OSF2FXMLBeanReaderCallback<FriendBean>() {
                    public void readObject(FriendBean object) {
                        Friend friend = getFriend(object);
                        FriendKey fk = new FriendKey(friend.getPublicKey());
                        friends.put(fk, friend);
                    }

                    public void completed() {
                        // update community server friends to not request file
                        // list, can
                        // be removed in 2010...
                        logger.finest("checking for initial file list requests settings");
                        handleFileListRequestInitial();
                        logger.finer("friend read completed");
                    }

                    private void handleFileListRequestInitial() {
                        String param = "osf2f_friend_file_list_request_initial";
                        if (!COConfigurationManager.hasParameter(param, true)) {
                            for (Friend f : friends.values()) {
                                // default to limited=don't request the file
                                // list
                                f.setRequestFileList(f.isCanSeeFileList());
                            }
                            logger.finest("updated friends, saving...");
                            flushToDisk(true, true, false);
                            COConfigurationManager.setParameter(param, new Boolean(true));
                        }

                    }
                });
        // Thread t = new Thread(reader);
        // t.setDaemon(true);
        // t.start();
        logger.finer("reading friend file from disk, reading");
        reader.run();
        logger.finer("reading friend file from disk, completed");
    }

    private void registerShutdownHook() {
        AzureusCoreImpl.getSingleton().addLifecycleListener(new AzureusCoreLifecycleListener() {

            public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
                // TODO Auto-generated method stub

            }

            public boolean restartRequested(AzureusCore core) throws AzureusCoreException {
                // TODO Auto-generated method stub
                return false;
            }

            public void started(AzureusCore core) {
                // TODO Auto-generated method stub

            }

            public void stopped(AzureusCore core) {
                // TODO Auto-generated method stub

            }

            public void stopping(AzureusCore core) {
                logger.fine("stopping, ");
                flushToDisk(false, true, false);

            }

            public boolean stopRequested(AzureusCore core) throws AzureusCoreException {
                // System.out
                // .println("stop requested, flushing friends to disk");
                // flushToDisk(false);
                return true;
            }

            public boolean syncInvokeRequired() {
                // TODO Auto-generated method stub
                return false;
            }
        });
    }

    public void removeFriend(byte[] publicKey) {
        friends.remove(new FriendKey(publicKey));
        boolean makeBackup = true;
        boolean block = true;
        this.flushToDisk(makeBackup, block, true);

        /**
         * PIAMOD -- syncing with the groups code here
         */
        PermissionsDAO.get().refresh_friend_groups();
    }

    private void waitForRead() {
        if (!readCompleted) {
            try {
                diskSemaphore.acquire();
                diskSemaphore.release();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            readCompleted = true;
        }
    }

    private class FriendKey {
        private int hash = 0;
        private String ip;
        private byte[] publicKey;

        public FriendKey(byte[] publicKey) {
            this.publicKey = publicKey;
        }

        public boolean equals(Object obj) {
            if (obj instanceof FriendKey) {
                FriendKey comp = (FriendKey) obj;
                if (Arrays.equals(comp.getPublicKey(), this.getPublicKey())) {
                    return true;
                }
            }
            return false;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public int hashCode() {
            if (hash == 0) {
                hash = Arrays.hashCode(publicKey);
            }
            return hash;
        }
    }

}
