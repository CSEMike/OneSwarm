package edu.washington.cs.oneswarm.test.util;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public class SingleProcessTestBase {

    /** The locally running selenium test server. */
    static Process seleniumServer;

    /** The selenium control interface. */
    protected static Selenium selenium;

    @BeforeClass
    public static void setUpClass() throws Exception {
        seleniumServer = TestUtils.startSeleniumServer((new File(".").getAbsolutePath()));
        TestUtils.awaitJVMOneSwarmStart();

        selenium = new DefaultSelenium("127.0.0.1", 4444, "*firefox", TestUtils.JVM_INSTANCE_WEB_UI) {
            // Fix for bug:
            // http://code.google.com/p/selenium/issues/detail?id=408
            @Override
            public void open(String url) {
                commandProcessor.doCommand("open", new String[] { url, "true" });
            }
        };
        selenium.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Quit OneSwarm
        if (UIFunctionsManager.getUIFunctions() != null) {
            UIFunctionsManager.getUIFunctions().requestShutdown();
        }
        // Quit browser
        if (selenium != null) {
            selenium.stop();
        }
        // Quit RC Server
        if (seleniumServer != null) {
            seleniumServer.destroy();
        }
    }
}
