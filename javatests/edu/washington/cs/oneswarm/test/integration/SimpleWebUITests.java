package edu.washington.cs.oneswarm.test.integration;

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * A suite of tests that make sure the UI responds as expected to basic commands,
 * e.g., without crashing.
 */
public class SimpleWebUITests {

	/** A locally running instance of OneSwarm. */
	static LocalOneSwarm instance;

	/** The locally running selenium test server. */
	static Process seleniumServer;

	@BeforeClass
	public static void setUpClass() throws Exception {
		instance = new LocalOneSwarm();
		seleniumServer = TestUtils.startSeleniumServer(instance.getRootPath());
		instance.start();
		TestUtils.awaitInstanceStart(instance);
	}

	private Selenium selenium;

	/** Opens the web UI in Firefox. */
	@Before
	public void setUp() throws Exception {
		selenium = new DefaultSelenium("127.0.0.1", 4444, "*firefox", "http://127.0.0.1:3000/") {
			// Fix for bug: http://code.google.com/p/selenium/issues/detail?id=408
        	@Override
			public void open(String url) {
        		commandProcessor.doCommand("open", new String[] {url,"true"});
        	}
        };
        selenium.start();
	}

	@Test
	public void testStartup() throws Exception {
		/*
		 * Test plan: Load the Web UI and perform a (very) basic check that it succeeded.
		 *
		 * TODO(piatek): Improve this test to check for the presence of expected defaults.
		 */
		try {
			selenium.open("/");
		} catch(Exception e) {
			e.printStackTrace();
		}

		selenium.waitForPageToLoad("5000");
		assertTrue(selenium.getTitle().contains("OneSwarm"));
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		instance.stop();
		seleniumServer.destroy();
	}

	/** Boilerplate code for running as executable. */
	public static void main (String [] args) {
        junit.textui.TestRunner.run (suite());
	}

	public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(SimpleWebUITests.class);
	}
}
