package edu.washington.cs.oneswarm.test.util;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;

public class LocalProcessesTestBase extends OneSwarmTestBase {

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
