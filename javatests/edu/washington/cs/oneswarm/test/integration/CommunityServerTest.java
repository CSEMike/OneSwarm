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

import edu.washington.cs.oneswarm.f2f.OSF2FMain;
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

	// Probably shouldn't configure like this, but setupClass inheritance isn't workable.
	// and we need this configuration to occur before setUpClass() in TwoProcessTestBase.
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
			// Test plan: Register for community server using the web UI in both clients. We
			// register in the OOP instance first and the local instance second. After the local
			// registration completes, verify that we've added a friend with a key and nick that
			// matches the OOP instance.

			logger.info("Start testCommunityServerRegistration()");

			if (TestUtils.isLocalCommunityServerRunning() == false) {
				logger.warning("No local community server running at "
						+ TestUtils.TEST_COMMUNITY_SERVER + " -- skipping community server test.");
				return;
			}

			// Subscribe to the community server using the local JVM instance. In this case, we
			// subscribe programatically for simplicity.
			CommunityRecord rec = getTestCommunityRecord();
			addServerAndRefresh(rec);
			Thread.sleep(2000);

			// In the OOP instance, using selenium to actually test adding a community server using
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

			// After subscribing on the OOP instance, refresh the server and verify that we received
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

	private void addServerAndRefresh(CommunityRecord rec) {
		List<String> appended = new ArrayList<String>();
		appended.addAll(Arrays.asList(rec.toTokens()));
		COConfigurationManager.setParameter("oneswarm.community.servers", appended);
		ConfigurationManager.getInstance().setDirty();
		CommunityServerManager.get().refreshAll();
	}

	public static CommunityRecord getTestCommunityRecord() {
		return new CommunityRecord(Arrays.asList(new String[] { TEST_COMMUNITY_URL, "", "",
				"Exp. contacts", "true;false;false;false;" + 26 }), 0);
	}

	/** Boilerplate code for running as executable. */
	public static void main(String[] args) throws Exception {
		TestUtils.swtCompatibleTestRunner(CommunityServerTest.class);
	}
}
