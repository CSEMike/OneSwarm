package edu.washington.cs.oneswarm.test.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.xerces.impl.dv.util.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TwoProcessTestBase;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CommunityServerManager;

public class CommunityServerTest extends TwoProcessTestBase {

    private static Logger logger = Logger.getLogger(CommunityServerTest.class.getName());

    // XPath for relevant items.
    private static final String ADD_COMMUNITY_SERVER_LINK = "addFriendItemLink";
    private static final String COMMUNITY_URL_TEXTBOX = "communityUrlTextBox";
    private static final String SUBSCRIBE_BUTTON = "communityServerSaveButton";
    private static final String DISMISS_BUTTON = "communitySaveAfterReceiveButton";

    public static final String TEST_COMMUNITY_URL = "http://" + TestUtils.TEST_COMMUNITY_SERVER
            + "/";

    // Probably shouldn't configure like this, but setupClass inheritance isn't
    // workable.
    // and we need this configuration to occur before setUpClass() in
    // TwoProcessTestBase.
    static {
        connectPeers = false;
    }

    @Before
    public void setupTest() throws IOException {
        TestUtils.flushCommunityServerState();
    }

    @Test
    public void testCommunityServerRegistration() throws InterruptedException {
        try {
            // Test plan: Register for community server using the web UI in both
            // clients. We
            // register in the OOP instance first and the local instance second.
            // After the local
            // registration completes, verify that we've added a friend with a
            // key and nick that
            // matches the OOP instance.

            logger.info("Start testCommunityServerRegistration()");

            if (TestUtils.isLocalCommunityServerRunning() == false) {
                logger.warning("No local community server running at "
                        + TestUtils.TEST_COMMUNITY_SERVER + " -- skipping community server test.");
                return;
            }

            // Subscribe to the community server using the local JVM instance.
            // In this case, we
            // subscribe programatically for simplicity.
            CommunityRecord rec = getTestCommunityRecord();
            addServerAndRefresh(rec);
            Thread.sleep(2000);

            // In the OOP instance, using selenium to actually test adding a
            // community server using
            // the web UI.
            selenium.openWindow("http://127.0.0.1:3000/", "localinstance");
            selenium.selectWindow("localinstance");

            Thread.sleep(5000);
            TestUtils.awaitAndClick(selenium, ADD_COMMUNITY_SERVER_LINK);
            Thread.sleep(5000);
            TestUtils.awaitAndClick(selenium, COMMUNITY_URL_TEXTBOX);
            selenium.focus(COMMUNITY_URL_TEXTBOX);

            selenium.type(COMMUNITY_URL_TEXTBOX, "");
            selenium.typeKeys(COMMUNITY_URL_TEXTBOX, TEST_COMMUNITY_URL);

            TestUtils.awaitAndClick(selenium, SUBSCRIBE_BUTTON);
            Thread.sleep(500);
            TestUtils.awaitAndClick(selenium, DISMISS_BUTTON);

            // After subscribing on the OOP instance, refresh the server and
            // verify that we received
            // the remote key.
            CommunityServerManager.get().refreshAll();
            Thread.sleep(2000);
            localOneSwarm.waitForOnlineFriends(1);

            final String remoteKey = localOneSwarm.getPublicKey();
            final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            final String localKey = Base64.encode(f2fMain.getFriendManager().getFriends()[0]
                    .getPublicKey());
            Assert.assertEquals(remoteKey, localKey);

        } finally {
            logger.info("End testCommunityServerRegistration()");
        }
    }

    @Test
    public void testCommunityServerCHTEndToEnd() throws Exception {
        // Test plan: Finish the registration test above, then disable the DHT
        // and LAN peer
        // discovery. Disconnect all friends, and then reconnect to all friends,
        // and verify that a
        // CHT lookup success status message appears in the friend logs.
        try {
            logger.info("Start testCommunityServerRegistration()");

            testCommunityServerRegistration();

            // Disable other methods of performing address resolution
            COConfigurationManager.setParameter("dht.enabled", false);
            COConfigurationManager.setParameter("OSF2F.LanFriendFinder", false);

            // Remove the last connected IP cache for the test friend
            final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            final Friend friend = f2fMain.getFriendManager().getFriends()[0];
            friend.setLastConnectIP(null);
            friend.setLastConnectPort(0);

            // Disconnect
            f2fMain.getOverlayManager().closeAllConnections();

            // Force the remote host to republish location information -- we'll
            // then force
            // a reconnect locally. This is necessary to avoid a once per hour
            // rate limit on
            // CHT publishing.
            localOneSwarm.getCoordinator().addCommand("forceRepublish");
            Thread.sleep(3 * 1000);

            // Connect to the friend and await the expected resolve message to
            // appear in the friend
            // log.
            f2fMain.getDHTConnector().connectToFriend(friend);

            logger.info("Awaiting resolve message in friend connect log...");
            new ConditionWaiter(new ConditionWaiter.Predicate() {
                @Override
                public boolean satisfied() {
                    String connectionLog = friend.getConnectionLog();
                    return connectionLog.contains("Resolved friend location from: HTTP:CHT");
                }
            }, 5000).await();

        } finally {
            logger.info("End testCommunityServerCHTEndToEnd()");
        }
    }

    private void addServerAndRefresh(CommunityRecord rec) {
        List<String> appended = new ArrayList<String>();
        appended.addAll(Arrays.asList(rec.toTokens()));
        COConfigurationManager.setParameter("oneswarm.community.servers", appended);
        ConfigurationManager.getInstance().setDirty();
        CommunityServerManager.get().refreshAll();
    }

    public static CommunityRecord getTestCommunityRecord() {
        CommunityRecord rec = new CommunityRecord(Arrays.asList(new String[] { TEST_COMMUNITY_URL,
                "", "", "Exp. contacts", "true;false;false;false;" + 26 }), 0);
        rec.setAllowAddressResolution(true);
        return rec;
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(CommunityServerTest.class);
    }
}
