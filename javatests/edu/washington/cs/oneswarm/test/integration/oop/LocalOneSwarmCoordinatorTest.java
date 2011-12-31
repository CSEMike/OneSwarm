package edu.washington.cs.oneswarm.test.integration.oop;

import static org.junit.Assert.fail;
import junit.framework.Assert;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.ExperimentalHarnessManager;

/**
 * Test communication between between oneswarm instances in multiple-process
 * integration tests.
 * 
 * @author willscott
 * 
 */
public class LocalOneSwarmCoordinatorTest {
    LocalOneSwarmCoordinator coordinator;
    LocalOneSwarmCoordinatee coordinatee;

    @Before
    public void setUp() throws Exception {
        // Make sure our experimental commands are available.
        System.setProperty("oneswarm.experimental.config.file", "dummyconfig");
        ExperimentalHarnessManager
                .get()
                .enqueue(
                        new String[] { "inject edu.washington.cs.oneswarm.test.integration.oop.LocalOneSwarmExperiment" });

        System.setProperty("oneswarm.test.local.classpath", "dummy");
        System.setProperty("oneswarm.test.coordinator.poll", "1");
        LocalOneSwarm los = new LocalOneSwarm(true);
        coordinator = new LocalOneSwarmCoordinator(los);
        String rendevous = "http://127.0.0.1:" + coordinator.getServerPort() + "/s";
        coordinatee = new LocalOneSwarmCoordinatee(rendevous);
        coordinator.start();
        coordinatee.start();
    }

    @After
    public void tearDown() throws Exception {
        coordinator.setDone();
        coordinatee.interrupt();
    }

    @Test
    public void testSend() {
        COConfigurationManager.setParameter("Computer Name", "InitialTestName");
        coordinator.addCommand("name ExampleTestName");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Thread interrupted");
        }
        Assert.assertEquals("ExampleTestName".toLowerCase(),
                COConfigurationManager.getStringParameter("Computer Name"));
    }

    @Test
    public void testReply() {
        coordinatee.addResponse("onlinefriends", "1023");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Thread interrupted");
        }
        System.out.println(coordinator.getOnlineFriendCount());
        Assert.assertEquals(1023, coordinator.getOnlineFriendCount());
    }
}
