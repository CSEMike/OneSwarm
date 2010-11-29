package edu.washington.cs.oneswarm.test.integration.oop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Constants;
import org.junit.Assert;

import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
import edu.washington.cs.oneswarm.test.util.ProcessLogConsumer;
import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * Encapsulates a locally running testing instance of OneSwarm. Each instance of this class
 * corresponds to a separate process running on the local machine.
 *
 * This class should only be used by integration tests.
 */
public class LocalOneSwarm {

	private static Logger logger = Logger.getLogger(LocalOneSwarm.class.getName());

	// Used to automatically choose instance labels.
	private static int instanceCount = 0;

	/** The set of listeners. */
	List<LocalOneSwarmListener> listeners = Collections
			.synchronizedList(new ArrayList<LocalOneSwarmListener>());

	/** The configuration of this instance. */
	LocalOneSwarm.Config config = new LocalOneSwarm.Config();

	/** The running OneSwarm process. */
	Process process = null;

	/** The root path to the OneSwarm. Paths are constructed relative to this. */
	String rootPath = null;

	/** The experimental coordinator for this instance. */
	LocalOneSwarmCoordinator coordinator = null;

	/** The current state of this instance. */
	State state = State.CONFIGURING;

	/** Possible states of a LocalOneSwarm instance. */
	public enum State {
		CONFIGURING,
		STARTING,
		RUNNING,
	};

	/** Configuration parameters for the local instance. */
	public class Config {

		/** The label for this instance. */
		String label = "LocalOneSwarm";

		/** The path to system java. */
		String javaPath = "java";

		/** The path to the GWT war output directory. */
		String warRootPath;

		/** The port on which the local webserver listens for GUI connections. */
		int webUiPort = 29615;

		/**
		 * The port to use for the StartServer (used to pass params on Windows and detect
		 * multiple concurrent invocations on other platforms.
		 */
		int startServerPort = 6885;

		/** Classpath elements for the invocation. */
		List<String> classPathElements = new ArrayList<String>();

		public String getLabel() { return label; }
		public void setLabel(String label) { this.label = label; }

		public String getWarRootPath() { return warRootPath; }
		public void setWarRootPath(String path) { warRootPath = path; }

		public List<String> getClassPathElements() { return classPathElements; }
		public void addClassPathElement(String path) { classPathElements.add(path); }

		public int getWebUiPort() { return webUiPort; }
		public void setWebUiPort(int port) { webUiPort = port; }

		public int getStartServerPort() { return startServerPort; }
		public void setStartServerPort(int port) { startServerPort = port; }
	}

	/** Forcibly shuts down the OneSwarm instance associated with this object. */
	Thread cancelThread = new Thread() {
		@Override
		public void run() {
			if (process == null) {
				return;
			}
			logger.info("Attemting to kill: " + process);
			process.destroy();
		}
	};

	public LocalOneSwarm() throws IOException {
		rootPath = new File(".").getAbsolutePath();

		config.setLabel("LocalOneSwarm-" + instanceCount);

		/*
		 * 2 * instanceCount since the client uses the local port + 1 for SSL/remote access, and we
		 * specify 3*instanceCount+2 as the start server port for the instance.
		 */
		config.setWebUiPort(3000 + (3 * instanceCount));
		config.setStartServerPort(3000 + (3 * instanceCount + 2));

		// The coordinator listens for connections from running clients and sends commands
		coordinator = new LocalOneSwarmCoordinator(this);
		coordinator.start();

		instanceCount++;

		config.setWarRootPath("gwt-bin/war");

		if (System.getProperty("oneswarm.test.local.classpath") == null) {

			System.err.println(
				"********************************************************\n" +
				"*     Need to specify oneswarm.test.local.classpath    *\n" +
				"*                                                      *\n" +
				"* To support both IDE auto builds and ant builds,      *\n" +
				"* LocalOneSwarm requires you to manually set the       *\n" +
				"* OneSwarm-specific classpath entries. See the ant     *\n" +
				"* build.xml run-tests target for an example of this    *\n" +
				"* value.                                               *\n" +
				"* (If you're building in eclipse, perhaps add:         *\n" +
				"*     -Doneswarm.test.local.classpath=eclipse-bin      *\n" +
				"* to your run configuration JVM parameters.            *\n" +
				"********************************************************");
			Assert.fail();
		}

		String [] entries = System.getProperty("oneswarm.test.local.classpath").split(":");
		for (String entry : entries) {
			config.addClassPathElement(entry);
			System.out.println("Added " + entry + " to cp");
		}

		/* SWT */
		String swt = "build/swt/";
		if (Constants.isOSX) {
			swt += "swt-osx-cocoa-x86_64.jar";
		} else if (Constants.isLinux) {
			if (System.getProperty("sun.arch.data.model").equals("32")) {
				swt += "swt-gtk-linux-x86.jar";
			} else {
				swt += "swt-gtk-linux-x86_64.jar";
			}
		} else if (Constants.isWindows) {
			swt += "swt-win32-x86.jar";
		}
		config.addClassPathElement(swt);

		/* Other dependencies */
		final String COMMONS = "build/gwt-libs/commons-http/";
		final String GWT = "build/gwt-libs/";
		final String F2F = "build/f2f-libs/";
		final String CORE = "build/core-libs/";
		String [] deps = {

			// Apache commons
			COMMONS + "jcip-annotations.jar",
			COMMONS + "httpmime-4.0.jar",
			COMMONS + "httpcore-4.0.1.jar",
			COMMONS + "httpclient-4.0.jar",
			COMMONS + "commons-logging-1.1.1.jar",
			COMMONS + "commons-io-1.3.2.jar",
			COMMONS + "commons-fileupload-1.2.1.jar",
			COMMONS + "commons-codec-1.4.jar",
			COMMONS + "apache-mime4j-0.6.jar",

			// GWT Core
			GWT + "gwt/gwt-user.jar",
			GWT + "gwt/gwt-servlet.jar",
			GWT + "gwt/gwt-dev.jar",
			// GWT Libs
			GWT + "gwt-incubator/gwt-incubator.jar",
			GWT + "gwt-dnd/gwt-dnd-2.6.5.jar",
			// TODO(piatek): Move this?
			GWT + "jaudiotagger.jar",
			// Jetty
			GWT + "jetty/jetty.jar",
			GWT + "jetty/jetty-util.jar",
			GWT + "jetty/jetty-servlet-api.jar",
			GWT + "jetty/jetty-management.jar",

			// F2F Libs
			F2F + "smack.jar",
			F2F + "publickey-client.jar",
			F2F + "ecs-1.4.2.jar",

			// Core libs
			CORE + "derby.jar",
			CORE + "log4j.jar",
			CORE + "junit.jar",
			CORE + "commons-cli.jar",
		};

		for (String dep : deps) {
			config.addClassPathElement(dep);
		}
	}

	/** Adds {@code listener} to the set of listeners. */
	public void addListener(LocalOneSwarmListener listener) {
		listeners.add(listener);
	}

	/** Removes {@code listener} from the set of listeners. */
	public void removeListener(LocalOneSwarmListener listener) {
		listeners.remove(listener);
	}

	/** Returns the current state of the instance. */
	public State getState() {
		return state;
	}

	/** Called by our OneSwarmCoordinator when receiving heartbeats from clients. */
	void coordinatorReceivedHeartbeat() {
		if (state == State.STARTING) {
			state = State.RUNNING;

			// Broadcast the start event to listeners
			for (LocalOneSwarmListener l : listeners.toArray(new LocalOneSwarmListener[0])) {
				l.instanceStarted(this);
			}
		}
	}

	/** Asynchronously starts the process associated with this instance. */
	public void start() throws IOException {

		state = State.STARTING;

		// Construct a ProcessBuilder with common options
		ProcessBuilder pb = new ProcessBuilder(config.javaPath,
			"-Xmx256m",
			"-Ddebug.war=" + new File(rootPath, config.warRootPath),
			"-Dazureus.security.manager.install=0",
			"-DMULTI_INSTANCE=true");

		List<String> command = pb.command();

		// Add platform-specific options
		if (Constants.isOSX) {
			command.add("-XstartOnFirstThread");
		}

		// Add classpath
		StringBuilder cpString = new StringBuilder();
		for (String path : config.classPathElements) {
			File entry = new File(rootPath, path);
			if (entry.exists() == false) {
				logger.warning("Classpath entry not found: " + entry.getAbsolutePath());
			}
			cpString.append(entry.getAbsolutePath());
			cpString.append(File.pathSeparator);
		}
		command.add("-cp");
		// -1 because of the spurious ':' at the end
		command.add(cpString.substring(0, cpString.length()-1));

		// Configure system properties for test instances
		Map<String, String> scratchPaths = TestUtils.createScratchLocationsForTest(config.label);
		logger.info(config.getLabel() + " paths: " + scratchPaths);

		/*
		 * Create the experimental config file that will register this client with our locally
		 * running coordination server.
		 */
		PrintStream experimentalConfig = new PrintStream(new FileOutputStream(
				scratchPaths.get("experimentalConfig")));
		experimentalConfig.println("name " + config.getLabel());
		experimentalConfig.println(
				"register http://127.0.0.1:" + coordinator.getServerPort() + "/s");
		experimentalConfig.close();

		// Add the appropriate config properties
		command.add("-Doneswarm.integration.test=1");
		command.add("-Doneswarm.integration.user.data=" + scratchPaths.get("userData"));
		command.add("-Dazureus.config.path=" + scratchPaths.get("userData"));
		command.add("-Doneswarm.integration.web.ui.port=" + config.getWebUiPort());
		command.add("-Doneswarm.integration.start.server.port=" + config.getStartServerPort());
		command.add("-Doneswarm.experimental.config.file=" +
				scratchPaths.get("experimentalConfig"));
		command.add("-Dnolaunch_startup=1");
		command.add("-Doneswarm.test.coordinator.poll=2");
		
		if (Constants.isWindows) {
			command.add("-Djava.library.path=" + (new File(rootPath, "build/core-libs/dll").getAbsolutePath()));
		}

		// Main class
		command.add("com.aelitis.azureus.ui.Main");

		// Kick-off: merge stderr and stdout, set the working directory, and start.
		pb.redirectErrorStream(true);
		pb.directory(new File(scratchPaths.get("workingDir")));
		process = pb.start();
		
		logger.info("Forked OneSwarm instance: " + config.label);
		
		// Consume the unified log.
		new ProcessLogConsumer(config.label, process).start();

		try {
			System.out.println("**** proc resp: " + process.waitFor());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Make sure this process gets torn down when if the test is killed
		Runtime.getRuntime().addShutdownHook(cancelThread);
	}

	/** Asynchronously stops the process associated with this instance. */
	public void stop() {
		cancelThread.start();
		coordinator.setDone();
		Runtime.getRuntime().removeShutdownHook(cancelThread);
	}

	/** Blocks until {@count} friends are online. */
	public void waitForOnlineFriends(final int count) {
		new ConditionWaiter(new ConditionWaiter.Predicate(){
			public boolean satisfied() {
				return getCoordinator().onlineFriendCount >= count;
			}}, 20*1000).await();
	}

	/** Blocks until the instance's public key is available and returns it. */
	public String getPublicKey() {
		new ConditionWaiter(new ConditionWaiter.Predicate() {
			public boolean satisfied() {
				return getCoordinator().encodedPublicKey != null;
			}
		}, 20*1000).await();
		return getCoordinator().encodedPublicKey;
	}

	/** Returns the root path of the OneSwarm build folder. */
	public String getRootPath() {
		return rootPath;
	}

	public LocalOneSwarmCoordinator getCoordinator() {
		return coordinator;
	}

	@Override
	public String toString() {
		return config.label;
	}

	public static void main (String [] args) throws Exception {
		new LocalOneSwarm().start();
		while (true) {
			Thread.sleep(100);
		}
	}

	/** Returns the label of this instance. */
	public String getLabel() {
		return config.label;
	}

}
