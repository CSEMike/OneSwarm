package edu.washington.cs.oneswarm.test.ux;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * A suite of tests that make sure the UI responds as expected to basic commands,
 * e.g., without crashing.
 */
public class SimpleWebUITest {

	/** The locally running selenium test server. */
	static Process seleniumServer;

	@BeforeClass
	public static void setUpClass() throws Exception {
		seleniumServer = TestUtils.startSeleniumServer((new File(".").getAbsolutePath()));
		TestUtils.awaitJVMOneSwarmStart();

		selenium = new DefaultSelenium("127.0.0.1", 4444, "*firefox",
				TestUtils.JVM_INSTANCE_WEB_UI) {
			// Fix for bug: http://code.google.com/p/selenium/issues/detail?id=408
        	@Override
			public void open(String url) {
        		commandProcessor.doCommand("open", new String[] {url,"true"});
        	}
        };
        selenium.start();
	}

	private static Selenium selenium;

	/** Opens the web UI in Firefox. */
	@Before
	public void setUp() throws Exception {
		selenium.open("/");
	}

	@Test
	public void testStartup() throws Exception {
		/*
		 * Test plan: Load the Web UI and perform a (very) basic check that it succeeded.
		 *
		 * TODO(piatek): Improve this test to check for the presence of expected defaults.
		 */
		selenium.waitForPageToLoad("5000");
		assertTrue(selenium.getTitle().contains("OneSwarm"));
	}

	/** Closes the web UI */
	@After
	public void tearDownTest() throws Exception {
		selenium.close();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		// Quit OneSwarm
		AzureusCoreImpl.getSingleton().stop();
		// Quit browser
		selenium.stop();
		// Quit RC Server
		seleniumServer.destroy();
	}

	/** Boilerplate code for running as executable. */
	public static void main (String [] args) throws IOException {
		TestUtils.swtCompatibleTestRunner(SimpleWebUITest.class);
	}
}
