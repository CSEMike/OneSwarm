package edu.washington.cs.oneswarm.f2f.servicesharing;


import static org.junit.Assert.*;

import java.util.Date;
import java.util.logging.Logger;

import org.junit.*;

import edu.washington.cs.oneswarm.test.integration.ServiceSharingClientTest;
import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * Tests ServerPublicInfo, verifying that it correctly represents the policy of
 * the server it represents.
 * 
 * @author Nick
 * 
 */
public class ProxyPolicyTest extends OneSwarmTestBase {

	private static Logger logger = Logger
			.getLogger(ServiceSharingClientTest.class.getName());

	@Before
	public void setupLogging() {
		logFinest(logger);
		logFinest(ServiceSharingLoopback.logger);
		logFinest(ServiceSharingManager.logger);
		logFinest(ServiceChannelEndpoint.logger);
	}

	@Test
	public void testServerPublicInfo() throws Exception {
		/*
		 * Verify that the exit policy passed to the ServerPublicInfo is
		 * correctly represented in subsequent calls to 'allowsConnection(String
		 * url, int port)'
		 * 
		 * Test plan: -Create a ServerPublicInfo with a complex exit policy
		 * -Verify that the results match the set of manually predetermined
		 * results for each case
		 */

		try {
			String[] policy = new String[]{
					"reject yahoo.com:*",
					"accept *:80",
					"reject *.google.com:40",
					"accept google.com:40",
					"reject *.2.*.*:40",
					"accept 4.*.2.2:40",
					"reject *:*"
					};
			
			ServerPublicInfo server = new ServerPublicInfo(
					"Servo The Magnificent", 
					"123.45.67.89", 
					"123456789DEADBEEF987654321CAFEBABE", 
					275, 
					policy, 
					new Date(), 
					"Version string 2.0"
					);
			
			//Sample Url's to test
			String[] urls = new String[]{
					"google.com",
					"yahoo.com",
					"maps.google.com",
					"4.2.2.2",
					"4.5.2.2"
					};
			
			//Sample ports to test
			int[] ports = new int[]{
					80,
					40
			};
			
			//First index is url, second index is port
			boolean[][] expected = new boolean[][]{
					{true, true},
					{false, false},
					{true, false},
					{true, false},
					{true, true}
			};
			
			if(expected.length != urls.length || expected[0].length != ports.length)
				throw new Exception("Invalid test: Dimmensions of 'expected' does not match the number of urls or ports.");
					
			for(int x = 0; x < urls.length; x++){
				for(int y = 0; y < ports.length; y++){
					if(expected[x][y] != server.allowsConnectionTo(urls[x], ports[y])){
						throw new Exception("Reported result ("+ expected[x][y] +") differs from expeceted result ("+ !expected[x][y] +") for "+ urls[x] + ":" + ports[y]);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.severe(e.toString());
			fail();
		} finally {
			logger.info("End testServerPublicInfo()");
		}
	}

	/** Boilerplate code for running as executable. */
	public static void main(String[] args) throws Exception {
		TestUtils.swtCompatibleTestRunner(ProxyPolicyTest.class);
	}

}
