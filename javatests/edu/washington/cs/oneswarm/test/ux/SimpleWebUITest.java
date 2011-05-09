package edu.washington.cs.oneswarm.test.ux;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.oneswarm.test.util.SingleProcessTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * A suite of tests that make sure the UI responds as expected to basic
 * commands, e.g., without crashing.
 */
public class SimpleWebUITest extends SingleProcessTestBase {

    /** Opens the web UI in Firefox. */
    @Before
    public void setUp() throws Exception {
        selenium.open("/");
    }

    @Test
    public void testStartup() throws Exception {
        /*
         * Test plan: Load the Web UI and perform a (very) basic check that it
         * succeeded.
         * 
         * TODO(piatek): Improve this test to check for the presence of expected
         * defaults.
         */
        selenium.waitForPageToLoad("5000");
        assertTrue(selenium.getTitle().contains("OneSwarm"));
    }

    /** Closes the web UI */
    @After
    public void tearDownTest() throws Exception {
        selenium.close();
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws IOException {
        TestUtils.swtCompatibleTestRunner(SimpleWebUITest.class);
    }
}
