/**
 * 
 */
package edu.washington.cs.oneswarm.f2f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTransportHelperFilterStream;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

import edu.washington.cs.oneswarm.f2f.OSF2FMain.OsProtocolMatcher;
import edu.washington.cs.oneswarm.f2f.dht.DHTConnector;
import edu.washington.cs.oneswarm.f2f.friends.FriendManager;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageDecoder;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageEncoder;
import edu.washington.cs.oneswarm.f2f.network.F2FDownloadManager;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;

/**
 * this class is an alternative main for oneswarm F2F, it authenticates friends
 * using a static list of hostnames (or IPs) instead of the default public key
 * The public key of remote friends is ignored
 * 
 * @author isdal
 * 
 */
public class ExperimentMain {

    public static boolean logToStdOut = true;

    private NetworkManager.ByteMatcher matcher;

    private OverlayManager overlayManager;
    private FriendManager friendManager;

    private FileListManager fileListManager;
    private F2FDownloadManager f2DownloadManager;
    private static final HashMap<String, Boolean> friends = new HashMap<String, Boolean>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out
                    .println("usage: ExperimentMain permissions_file host_address torrent_file destination_file upload_rate");
            System.exit(0);
        }
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
            String line;
            while ((line = in.readLine()) != null) {
                String[] split = line.split(" ");
                if (split[0].equals(args[1])) {
                    friends.put(split[1], true);
                    System.out.println("adding friend: " + split[1]);
                } else if (split[1].equals(args[1])) {
                    System.out.println("adding friend: " + split[0]);
                    friends.put(split[0], true);
                }
            }
            if (friends.size() < 1) {
                System.out.println("no friends :-(, exiting");
            }
            new CLI_Main(new String[] { "-file", args[2], "-outfile", args[3], "-rate", args[4] });

            new ExperimentMain(args[0], args[1]);

        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private final Friend testUser;
    private final PublicKey localPublicKey;

    public ExperimentMain(String permissionsFile, String hostAddress) throws InterruptedException,
            UnknownHostException {

        localPublicKey = OneSwarmSslKeyManager.getInstance().getOwnPublicKey();

        testUser = new Friend("manual", "tester", localPublicKey.getEncoded(), false);

        this.friendManager = new FriendManager(Thread.currentThread().getContextClassLoader(),
                localPublicKey.getEncoded());
        // this.fileListManager = new FileListManager();
        this.overlayManager = new OverlayManager(friendManager, localPublicKey, fileListManager,
                AzureusCoreImpl.getSingleton().getGlobalManager().getStats());
        matcher = new OsProtocolMatcher(true);

        NetworkManager.getSingleton().requestIncomingConnectionRouting(matcher,
                new ExperimentRouterListener(), new MessageStreamFactory() {
                    public MessageStreamDecoder createDecoder() {
                        return new OSF2FMessageDecoder();
                    }

                    public MessageStreamEncoder createEncoder() {
                        return new OSF2FMessageEncoder();
                    }
                });

        AzureusCoreImpl.getSingleton().getInstanceManager().setIncludeWellKnownLANs(false);
        this.f2DownloadManager = new F2FDownloadManager(overlayManager);
        System.out.println("sleeping before initiating connections");
        Thread.sleep(60 * 1000);

        for (String friendIp : friends.keySet()) {
            overlayManager.createOutgoingConnection(new ConnectionEndpoint(new InetSocketAddress(
                    InetAddress.getByName(friendIp), 4919)), testUser);
        }
    }

    private class ExperimentRouterListener implements NetworkManager.RoutingListener {
        public boolean autoCryptoFallback() {
            return (false);
        }

        public void connectionRouted(NetworkConnection connection, Object routing_data) {

            if (!connection.getTransport().getEncryption()
                    .startsWith(OneSwarmSslTransportHelperFilterStream.SSL_NAME)) {
                Log.log(LogEvent.LT_WARNING, "OSF2F connection without SSL!: " + connection + " ("
                        + connection.getTransport().getEncryption() + ")", logToStdOut);
                return;
            }
            byte[][] remoteKeys = matcher.getSharedSecrets();
            byte[] remotePub = new byte[0];
            if (remoteKeys != null) {
                remotePub = remoteKeys[0];
            }
            Log.log("Incoming connection from [" + connection
                    + "] successfully routed to OneSwarm F2F: encr: "
                    + connection.getTransport().getEncryption() + "\n" + "remote public key: "
                    + Base64.encode(remotePub), logToStdOut);

            String remoteIp = connection.getEndpoint().getNotionalAddress().getAddress()
                    .getHostAddress();
            if (friends.containsKey(remoteIp)) {
                Log.log("got connection from friend, creating friendconnection");
                overlayManager.createIncomingConnection(remotePub, connection);
            } else {
                Debug.out("got connection from non friend: " + remoteIp);
            }

        }
    }

}
