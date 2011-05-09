package edu.washington.cs.oneswarm.planetlab;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;

import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.RemoteAccessConfig;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;

public class ExperimentConfigManager {
    private static ExperimentConfigManager inst = null;

    private static Logger logger = Logger.getLogger(ExperimentConfigManager.class.getName());

    public static final String EXPERIMENTAL_CONFIG_PROPERTY = "oneswarm.experimental.config.file";

    List<CoordinatorHeartbeatThread> heartbeatThreads = new ArrayList<CoordinatorHeartbeatThread>();

    AnalyticsConfigData analyticsConfig;

    public static synchronized ExperimentConfigManager get() {
        /**
         * Only load this code if we're configured to be running in experimental
         * mode.
         */
        if (!isEnabled()) {
            return null;
        }

        if (inst == null) {
            inst = new ExperimentConfigManager();
        }
        return inst;
    }

    public static boolean isEnabled() {
        return System.getProperty(EXPERIMENTAL_CONFIG_PROPERTY) != null;
    }

    private CoreInterface coreInterface;

    private JGoogleAnalyticsTracker analyticsTracker;

    public void setCore(CoreInterface core) {
        this.coreInterface = core;
    }

    public CoreInterface getCoreInterface() {
        return coreInterface;
    }

    public ExperimentConfigManager() {
        if (isEnabled()) {
            load();
        }
    }

    public void startHeartbeats() {
        // double-check that we're only doing this if in experiment mode
        if (!isEnabled()) {
            return;
        }

        for (CoordinatorHeartbeatThread t : heartbeatThreads) {
            if (t.isAlive() == false) {
                t.start();
            } else {
                logger.warning("Thread is already alive. Trying to startHeartbeats() twice?");
            }
        }
    }

    private void load() {
        try {
            String path = System.getProperty(EXPERIMENTAL_CONFIG_PROPERTY);
            if (path != null) {
                logger.info("Picked up experimental config: " + path);

                new AnalyticsReporter(OSF2FMain.getSingelton()).start();

                /**
                 * Some settings we just always want
                 */
                COConfigurationManager.setParameter("oneswarm.beta.updates", true);
                COConfigurationManager.setParameter("Allow.Incoming.Speed.Check", true);
                List<CommunityRecord> comm_servers = new ArrayList<CommunityRecord>();

                /**
                 * Off by default
                 */
                COConfigurationManager.setParameter(OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY,
                        false);

                BufferedReader in = new BufferedReader(new FileReader(path));
                while (in.ready()) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] toks = line.toLowerCase().split("\\s+");
                    if (toks[0].equals("port")) {
                        int newport = Integer.parseInt(toks[1]);
                        COConfigurationManager.setParameter("TCP.Listen.Port", newport);
                        logger.info("Exp config, set port: " + newport);
                    } else if (toks[0].equals("community_server")) {
                        String url = toks[1];
                        int howmany = 50;
                        if (toks.length > 2) {
                            try {
                                howmany = Integer.parseInt(toks[3]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        CommunityRecord rec = new CommunityRecord(Arrays.asList(new String[] { url,
                                "", "", "Exp. contacts", "true;false;false;false;" + howmany }), 0);
                        comm_servers.add(rec);
                        logger.info("exp config, set community server: " + url);
                    } else if (toks[0].equals("remote_access")) {
                        String user = toks[1];
                        String pw = toks[2];
                        RemoteAccessConfig.saveRemoteAccessCredentials(user, pw);
                        boolean oldValue = COConfigurationManager
                                .getBooleanParameter(OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY);
                        if (oldValue == true) {
                            COConfigurationManager.setParameter(
                                    OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY, false);
                            COConfigurationManager.setParameter(
                                    OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY, oldValue);
                        }
                        COConfigurationManager.setParameter(
                                OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY, true);
                        logger.info("Exp config, remote access: " + user + " / enabled");
                    } else if (toks[0].equals("name")) {
                        COConfigurationManager.setParameter("Computer Name", toks[1]);
                        logger.info("Exp config, set comp name: " + toks[1]);
                    } else if (toks[0].equals("register")) {
                        final int port = COConfigurationManager.getIntParameter("TCP.Listen.Port");
                        final String url = toks[1] + "?port=" + port;
                        heartbeatThreads.add(new CoordinatorHeartbeatThread(this, url));
                    } else if (toks[0].equals("resetproxy")) {
                        Random r = new Random();
                        String[] commands = new String[] { "cleardls", "unblockall",
                                "booleanSetting Enable.Proxy false",
                                "booleanSetting Proxy.Data.Enable false",
                                "booleanSetting Enable.SOCKS false",
                                "intSetting TCP.Listen.Port " + (r.nextInt(1000) + 1234), "restart" };
                        (new CoordinatorExecutor(null, commands)).start();
                    }
                }

                List<String> appended = new ArrayList<String>();
                for (CommunityRecord rec : comm_servers) {
                    appended.addAll(Arrays.asList(rec.toTokens()));
                }
                System.out.print("final comm server config string: ");
                for (String s : appended) {
                    System.out.print(s + " ");
                }
                System.out.println("");
                COConfigurationManager.setParameter("oneswarm.community.servers", appended);

                ConfigurationManager.getInstance().setDirty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }
    }
}
