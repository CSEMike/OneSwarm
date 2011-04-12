package edu.washington.cs.oneswarm.test.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.xerces.impl.dv.util.Base64;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.aelitis.azureus.ui.UIFunctionsManager;

import edu.washington.cs.oneswarm.f2f.dht.CHTCallback;
import edu.washington.cs.oneswarm.f2f.dht.CHTClientHTTP;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CommunityServerCHTTest {

	private static Logger logger = Logger.getLogger(CommunityServerTest.class.getName());

	@BeforeClass
	public static void setupClass() {
		TestUtils.awaitJVMOneSwarmStart();
	}

	@Before
	public void setupTest() throws IOException {
		TestUtils.flushCommunityServerState();
	}

	@Test
	public void testCommunityServerCHT() throws Exception {
		// Test plan: Start OneSwarm, perform some CHT puts and verify contents unmodified upon
		// retrieval.
		logger.info("Start testCommunityServerCHT()");

		if (TestUtils.isLocalCommunityServerRunning() == false) {
			logger.warning("No local community server running at "
					+ TestUtils.TEST_COMMUNITY_SERVER + " -- skipping community server test.");
			return;
		}

		final int testBatchSize = 10;

		try {
			CommunityRecord rec = CommunityServerTest.getTestCommunityRecord();
			rec.setAllowAddressResolution(true);
			rec.setCht_path("cht");
			CHTClientHTTP cht = new CHTClientHTTP(rec);

			Random r = new Random();
			List<byte[]> offeredKeys = new ArrayList<byte[]>();
			List<byte[]> offeredValues = new ArrayList<byte[]>();
			List<CHTCallback> callbacks = new ArrayList<CHTCallback>();

			final Map<String, byte[]> receivedValues = new HashMap<String, byte[]>();
			final CountDownLatch valuesToReceive = new CountDownLatch(testBatchSize);

			CHTCallback defaultCallback = new CHTCallback() {
				@Override
				public void valueReceived(byte[] key, byte[] value) {
					receivedValues.put(Base64.encode(key), value);
					valuesToReceive.countDown();
				}

				@Override
				public void errorReceived(Throwable cause) {
					logger.severe("CHT error: " + cause.toString());
					Assert.fail();
				}
			};

			for (int i = 0; i < testBatchSize; i++) {
				byte[] key = new byte[20];
				byte[] val = new byte[5];
				r.nextBytes(key);
				r.nextBytes(val);
				offeredKeys.add(key);
				offeredValues.add(val);
				callbacks.add(defaultCallback);

				cht.put(key, val);
			}

			Thread.sleep(3000);

			// Issue requests for every key we put
			for (byte[] key : offeredKeys) {
				cht.get(key, defaultCallback);
			}

			// Wait for all gets to return
			valuesToReceive.await(10, TimeUnit.SECONDS);

			// Verify contents
			for (int i = 0; i < offeredKeys.size(); i++) {
				if (Arrays.equals(offeredValues.get(i),
						receivedValues.get(Base64.encode(offeredKeys.get(i)))) == false) {
					Assert.fail("cht get() values failed to match.");
				}
			}

		} catch (Exception e) {
			logger.severe(e.toString());
			e.printStackTrace();
			Assert.fail();
		} finally {
			logger.info("End testCommunityServerCHT()");
		}
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		// Quit OneSwarm
		if (UIFunctionsManager.getUIFunctions() != null) {
			UIFunctionsManager.getUIFunctions().requestShutdown();
		}
	}

	/** Boilerplate code for running as executable. */
	public static void main(String[] args) throws Exception {
		TestUtils.swtCompatibleTestRunner(CommunityServerCHTTest.class);
	}
}
