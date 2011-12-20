package edu.washington.cs.oneswarm.test.util;

import java.util.LinkedList;
import java.util.List;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;

public class LocalProcessesTestBase extends OneSwarmTestBase {

    /** Commands to send before friends are connected */
    protected static List<String> preConnectCommands = new LinkedList<String>();
    static {
        preConnectCommands.add("booleanSetting OSF2F.Use@DHT@Proxy false");
        preConnectCommands.add("booleanSetting OSF2F.LanFriendFinder false");
        preConnectCommands.add("booleanSetting dht.enabled false");
    }

    protected static void startLocalInstance() {
        // Start a local client in this JVM
        TestUtils.awaitJVMOneSwarmStart();

        // Disable lan, dht, cht friend connections.
        ConfigurationManager.getInstance().setParameter("OSF2F.Use DHT Proxy", false);
        ConfigurationManager.getInstance().setParameter("OSF2F.LanFriendFinder", false);
        ConfigurationManager.getInstance().setParameter("dht.enabled", false);
        ConfigurationManager.getInstance().setDirty();
    }

}
