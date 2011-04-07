package edu.washington.cs.oneswarm.test.util;

import java.io.File;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import edu.washington.cs.oneswarm.test.integration.oop.LocalOneSwarm;

public class TwoProcessTestBase {

	private static Logger logger = Logger.getLogger(TwoProcessTestBase.class.getName());

	/** The locally running selenium test server. */
	protected static Process seleniumServer;

	/** The selenium control interface. */
	protected static Selenium selenium;

	/** The OneSwarm instance with which we will chat. */
	protected static LocalOneSwarm localOneSwarm;

	/** Should the two created peers be connected? */
	protected static boolean connectPeers = true;

	@BeforeClass
	public static void setUpClass() throws Exception {
		seleniumServer = TestUtils.startSeleniumServer((new File(".").getAbsolutePath()));

		// Start a local client in this JVM
		TestUtils.awaitJVMOneSwarmStart();

		// One additional remote client with which we'll chat
		localOneSwarm = TestUtils.spawnOneSwarmInstance(connectPeers);
		logger.info("OOP LocalOneSwarm started.");

		selenium = new DefaultSelenium("127.0.0.1", 4444, "*firefox", TestUtils.JVM_INSTANCE_WEB_UI) {
			// Fix for bug: http://code.google.com/p/selenium/issues/detail?id=408
			@Override
			public void open(String url) {
				commandProcessor.doCommand("open", new String[] { url, "true" });
			}
		};
		selenium.start();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		logger.info("Tearing down test. Quitting JVM instance");
		// Quit OneSwarm
		if (UIFunctionsManager.getUIFunctions() != null) {
			UIFunctionsManager.getUIFunctions().requestShutdown();
		}
		logger.info("Sending shutdown to oop instance");
		localOneSwarm.getCoordinator().addCommand("shutdown");
		new ConditionWaiter(new ConditionWaiter.Predicate() {
			@Override
			public boolean satisfied() {
				return localOneSwarm.getCoordinator().getPendingCommands().size() == 0;
			}
		}, 10000).await();
		localOneSwarm.stop();
		logger.info("selenium.stop()");
		// Quit browser
		if (selenium != null) {
			selenium.stop();
		}
		logger.info("selenium server stop");
		// Quit RC Server
		if (seleniumServer != null) {
			seleniumServer.destroy();
		}
	}
}
