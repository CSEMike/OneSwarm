package edu.washington.cs.oneswarm.test.integration.oop;

import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;

import com.aelitis.azureus.ui.UIFunctionsManager;

import edu.washington.cs.oneswarm.f2f.ExperimentInterface;
import edu.washington.cs.oneswarm.f2f.ExperimentalHarnessManager;
import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.ui.gwt.F2FInterface;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

/**
 * Handle Commands used for communication in multi-process integration tests.
 * 
 * @author willscott
 * 
 */
public class LocalOneSwarmExperiment implements ExperimentInterface {
    private static List<LocalOneSwarmCoordinatee> coordinatees = Collections
            .synchronizedList(new ArrayList<LocalOneSwarmCoordinatee>());
    private static Logger logger = Logger.getLogger(LocalOneSwarmExperiment.class.getName());

    @Override
    public String[] getKeys() {
        return new String[] { "port", "name", "register", "community_server", "setprop",
                "booleanSetting", "intSetting", "stringSetting", "floatSetting", "addkey",
                "forceall", "shutdown", "clean_community_servers" };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(String command) {
        String[] toks = command.split("\\s+");
        toks[0] = toks[0].toLowerCase();
        if (toks[0].equals("port")) {
            int newport = Integer.parseInt(toks[1]);
            COConfigurationManager.setParameter("TCP.Listen.Port", newport);
            logger.info("Set port: " + newport);
        } else if (toks[0].equals("name")) {
            COConfigurationManager.setParameter("Computer Name", toks[1]);
            logger.info("Set comp name: " + toks[1]);
        } else if (toks[0].equals("register")) {
            final int port = COConfigurationManager.getIntParameter("TCP.Listen.Port");
            final String url = toks[1] + "?port=" + port;
            logger.info("Will register with: " + url);
            LocalOneSwarmCoordinatee losc = new LocalOneSwarmCoordinatee(url);
            coordinatees.add(losc);
            losc.start();
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
            List<String> currentServers = (List<String>) COConfigurationManager
                    .getParameter("oneswarm.community.servers");
            if (currentServers == null) {
                currentServers = new ArrayList<String>();
            }
            
            CommunityRecord rec = new CommunityRecord(Arrays.asList(new String[] { url, "", "",
                    "Exp. contacts", "true;false;false;false;" + howmany }), 0);
            
            currentServers.addAll(Arrays.asList(rec.toTokens()));
            COConfigurationManager.setParameter("oneswarm.community.servers", currentServers);
        } else if (toks[0].equals("clean_community_servers")) {
            List<String> servers = new ArrayList<String>();
            COConfigurationManager.setParameter("oneswarm.community.servers", servers);
        } else if (toks[0].equals("setprop")) {
            System.setProperty(toks[1], toks[2]);
        } else if (toks[0].equals("booleansetting")) {
            logger.info("boolean setting: " + toks[1]);
            ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "),
                    Boolean.parseBoolean(toks[2]));
            ConfigurationManager.getInstance().setDirty();
        } else if (toks[0].equals("intsetting")) {
            logger.info("integer setting: " + toks[1] + "=" + toks[2]);
            ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "),
                    Integer.parseInt(toks[2]));
            ConfigurationManager.getInstance().setDirty();
        } else if (toks[0].equals("stringsetting")) {
            ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "), toks[2]);
            ConfigurationManager.getInstance().setDirty();
        } else if (toks[0].equals("floatsetting")) {
            ConfigurationManager.getInstance().setParameter(toks[1].replaceAll("@", " "),
                    Float.parseFloat(toks[2]));
            ConfigurationManager.getInstance().setDirty();
        } else if (toks[0].equals("addkey")) {
            try {
                boolean allowChat = (toks.length > 3) ? Boolean.parseBoolean(toks[3]) : false;
                boolean shareFileList = (toks.length > 4) ? Boolean.parseBoolean(toks[4]) : false;
                InetAddress ip = (toks.length > 5) ? InetAddress.getByName(toks[5]) : null;
                Integer port = (toks.length > 6) ? Integer.parseInt(toks[6]) : null;

                addFriend(toks[1], toks[2], allowChat, shareFileList, ip, port);
            } catch (Exception e) {
                logger.warning("for key " + toks[2]);
                logger.warning("Failed to add key: " + e.getMessage());
            }
        } else if (toks[0].equals("forceall")) {
            F2FInterface f2f = ExperimentalHarnessManager.get().getCoreInterface()
                    .getF2FInterface();
            for (FriendInfoLite f : f2f.getFriends(true, false)) {
                f2f.connectToFriend(f.getPublicKey());
            }
            // wait some time for connections to come up
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                logger.warning("Thread interrupted while initiating friend connections.");
                e.printStackTrace();
            }
        } else if (toks[0].equals("shutdown")) {
            while (UIFunctionsManager.getUIFunctions() == null) {
                logger.warning("Waiting for non-null UI functions...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (UIFunctionsManager.getUIFunctions().requestShutdown() == false) {
                System.err.print("Could not gracefully shutdown.  Halting Forcefully.");
                Runtime.getRuntime().halt(-1);
            }
        }
    }

    @Override
    public void load() {
        /**
         * Some settings we just always want
         */
        COConfigurationManager.setParameter("oneswarm.beta.updates", true);
        COConfigurationManager.setParameter("Allow.Incoming.Speed.Check", true);
        COConfigurationManager.setParameter("f2f_forward_search_probability", 1.0f);

    }

    public static List<LocalOneSwarmCoordinatee> getCoordinatees() {
        return coordinatees;
    }

    private void addFriend(String group, String keybase64, boolean allowChat,
            boolean shareFileList, InetAddress ip, Integer port) throws InvalidKeyException {
        F2FInterface f2f = ExperimentalHarnessManager.get().getCoreInterface().getF2FInterface();

        String deDupedNick = "Experimental" + keybase64.hashCode();
        FriendInfoLite[] friends = f2f.getFriends(true, true);
        boolean dupe = false;
        do {
            dupe = false;
            for (int i = 0; i < friends.length; i++) {
                if (friends[i].getName().equals(deDupedNick)) {
                    deDupedNick += ".";
                    dupe = true;
                    break;
                }
            }
        } while (dupe);

        Friend f = new Friend("EXPERIMENTAL", deDupedNick, keybase64);
        f.setBlocked(false);
        f.setCanSeeFileList(shareFileList);
        f.setAllowChat(allowChat);
        f.setNewFriend(true);
        f.setGroup(group);
        f.setDateAdded(new Date());

        if (ip != null) {
            f.setLastConnectIP(ip);
        }
        if (port != null) {
            f.setLastConnectPort(port);
        }
        f2f.addFriend(f);

        logger.info("addfriend: " + keybase64);
    }
}
