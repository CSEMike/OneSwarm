package edu.washington.cs.oneswarm.f2f.friends;

import java.io.File;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;

import edu.washington.cs.oneswarm.f2f.Log;
import edu.washington.cs.publickey.CryptoHandler;
import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.ssl.client.PublicKeySSLClient;
import edu.washington.cs.publickey.xmpp.XMPPNetwork;
import edu.washington.cs.publickey.xmpp.client.PublicKeyXmppClient;

public class FriendImportManager {
    public final static String OSF2F_ENABLE_FRIEND_NOTIFICATIONS = "OSF2F.FriendNotifications";
    private final static String SSL_PUBLIC_KEY_publickey_cs_washington_edu = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCv9or8xCzErICJ5PI3IzQveXIlgNb0zsgeP4UhpuOQgwFjPuvuCi2LK6CmZ2LR0feGdacX9uNF8UDMsGAcqQL8sToGyKnnINzO9uDjfeVMETQSjZpDtkWqpSEKTGoSqK92RJPQaNGz1l785sVYfheQ4/yh6tOCE2A8u9Ah66FcJwIDAQAB";
    private final static int SSL_PORT = 5221;
    private final static int CHECK_PERIOD = 15 * 60 * 1000;
    private final static String SSL_HOSTNAME = "publickey.oneswarm.org";

    public static File importedFriendsFile;
    public final static String FRIENDS_CACHE_FILE_NAME = "importedFriends.xml";
    public static final File FRIENDS_CACHE_DIR;
    public static boolean logToStdOut = false;
    static {
        FRIENDS_CACHE_DIR = new File(SystemProperties.getUserPath() + File.separator
                + FriendManager.OSF2F_DIR_NAME);
        if (!FRIENDS_CACHE_DIR.isDirectory()) {
            FRIENDS_CACHE_DIR.mkdirs();
        }
        importedFriendsFile = new File(FRIENDS_CACHE_DIR, FRIENDS_CACHE_FILE_NAME);
    }

    private List<PublicKeyFriend> newFriends = new LinkedList<PublicKeyFriend>();
    final FriendManager friendManager;

    public FriendImportManager(final FriendManager friendManager) {
        this.friendManager = friendManager;
        COConfigurationManager.addParameterListener(OSF2F_ENABLE_FRIEND_NOTIFICATIONS,
                new ParameterListener() {
                    @Override
                    public void parameterChanged(String parameterName) {
                        boolean enabled = isNotificationsEnabled();
                        if (enabled) {
                            checkForNewFriends();
                        } else {
                            newFriends = new LinkedList<PublicKeyFriend>();
                        }
                    }
                });

        // TimerTask timerTask = new TimerTask() {
        // @Override
        // public void run() {
        // checkForNewFriends();
        // }
        // };
        // Timer t = new Timer("FriendImportManager", true);
        //
        // t.schedule(timerTask, 20 * 1000, CHECK_PERIOD);
    }

    private synchronized void checkForNewFriends() {
        try {
            if (isNotificationsEnabled()) {
                Log.log("autochecking for new friends", logToStdOut);
                newFriends = checkForNewFriends(friendManager.getKnownKeysForFriendImport());
                Log.log("autocheck says: " + newFriends.size(), true);
            } else {
                Log.log("Skipping new friend auto check due to privacy settings", logToStdOut);
            }
            // Log.log("Counts: " + countUsersPerNetwork(newFriends),
            // logToStdOut);
        } catch (Exception e) {
            Debug.out("Friend request check failed: " + e.getMessage());
        }
    }

    private boolean isNotificationsEnabled() {
        return COConfigurationManager.getBooleanParameter(OSF2F_ENABLE_FRIEND_NOTIFICATIONS);
    }

    public List<PublicKeyFriend> getNewFriends() {
        return newFriends;
    }

    public static String[] getXmppNetworks() {
        List<String> networks = new LinkedList<String>();
        for (XMPPNetwork net : XMPPNetwork.networks) {
            networks.add(net.getDisplayName());
        }
        return networks.toArray(new String[networks.size()]);
    }

    public static List<PublicKeyFriend> checkForNewFriends(List<byte[]> knownKeys) throws Exception {

        PublicKeySSLClient client = new PublicKeySSLClient(importedFriendsFile, knownKeys,
                SSL_HOSTNAME, SSL_PORT, Base64.decode(SSL_PUBLIC_KEY_publickey_cs_washington_edu),
                new CryptoHandler() {

                    @Override
                    public PublicKey getPublicKey() {
                        return OneSwarmSslKeyManager.getInstance().getOwnPublicKey();
                    }

                    @Override
                    public SSLContext getSSLContext() throws Exception {
                        return OneSwarmSslKeyManager.getInstance().getSSLContext();
                    }

                    @Override
                    public byte[] sign(byte[] data) throws Exception {
                        return null;
                    }
                });
        Log.log("autochecking: connecting", logToStdOut);
        client.connect();
        Log.log("autochecking: updateFriends", logToStdOut);
        client.updateFriends();
        Log.log("autochecking: disconnect", logToStdOut);
        client.disconnect();
        List<PublicKeyFriend> friends = client.getFriends();
        Log.log("autochecking: got: " + friends.size(), logToStdOut);
        return friends;

    }

    /*
     * use many of them so that we don't cause any load balancing issues for our
     * friends at gtalk...
     */
    public final static String[] GTALK_KEY_SERVER_BOTS = new String[] {
            "publickey.cs.washington.edu@gmail.com", "publickey1.cs.washington.edu@gmail.com",
            "publickey2.cs.washington.edu@gmail.com", "publickey3.cs.washington.edu@gmail.com",
            "publickey4.cs.washington.edu@gmail.com", "publickey5.cs.washington.edu@gmail.com",
            "publickey6.cs.washington.edu@gmail.com", "publickey7.cs.washington.edu@gmail.com",
            "publickey8.cs.washington.edu@gmail.com", "publickey9.cs.washington.edu@gmail.com" };

    private static String gtalkStatus = null;

    public static List<PublicKeyFriend> importXMPPFriends(List<byte[]> knownKeys,
            String networkName, String username, char[] password, String machineName)
            throws Exception {
        File tempImportFile = new File(FRIENDS_CACHE_FILE_NAME, FRIENDS_CACHE_FILE_NAME + "_"
                + Long.toHexString(System.currentTimeMillis()) + ".xml");
        tempImportFile.deleteOnExit();
        try {

            XMPPNetwork net = XMPPNetwork.getFromName(networkName);
            final PublicKeyXmppClient client = new PublicKeyXmppClient(tempImportFile, knownKeys,
                    net, username, password, machineName, new CryptoHandler() {

                        @Override
                        public PublicKey getPublicKey() {
                            return OneSwarmSslKeyManager.getInstance().getOwnPublicKey();
                        }

                        @Override
                        public SSLContext getSSLContext() throws Exception {
                            return OneSwarmSslKeyManager.getInstance().getSSLContext();
                        }

                        @Override
                        public byte[] sign(byte[] data) throws Exception {
                            return OneSwarmSslKeyManager.getInstance().sign(data);
                        }
                    });
            client.setServerBotName("OneSwarm Friend Finder");
            client.setServerBotUserId(GTALK_KEY_SERVER_BOTS[new Random()
                    .nextInt(GTALK_KEY_SERVER_BOTS.length)]);

            gtalkStatus = "connecting...";
            /*
             * start a thread to update the status, we will want to do this
             * using an object and a manager later
             */
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 30; i++) {
                            if (gtalkStatus == null) {
                                return;
                            }
                            gtalkStatus = client.getStatus();
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.setName("gtalk status checker");
            t.start();

            /*
             * the do the actual work
             */
            client.connect();
            client.updateFriends();
            client.disconnect();
            gtalkStatus = null;
            List<PublicKeyFriend> importedFriends = client.getFriends();
            FileUtil.copyFile(tempImportFile, importedFriendsFile);
            return importedFriends;
        } finally {
            tempImportFile.delete();
        }
    }

    public static String getGtalkStatus() {
        return gtalkStatus;
    }

}
