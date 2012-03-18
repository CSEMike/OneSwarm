package edu.washington.cs.oneswarm.f2f;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;

public class ExperimentalHarnessManager {
    private static ExperimentalHarnessManager inst;
    public static final String EXPERIMENTAL_CONFIG_PROPERTY = "oneswarm.experimental.config.file";
    private static List<ExperimentInterface> experiments;
    private static Logger logger = Logger.getLogger(ExperimentalHarnessManager.class.getName());

    private CoreInterface core;

    /**
     * Get the ExperimentalHarnessManger. The manager will only be provided if
     * this instance of oneswarm is running with experimental code enabled.
     * 
     * @return The ExperimentalHarnessManager.
     */
    public static ExperimentalHarnessManager get() {

        if (inst == null) {
            experiments = new ArrayList<ExperimentInterface>();
            inst = new ExperimentalHarnessManager();
        }
        return inst;
    }

    /**
     * Determine if Experimental code should be run. Triggers on whether
     * configuration
     * contains a "oneswarm.experimental.config.file" property.
     * 
     * @return True if experimental code should be loaded.
     */
    public static boolean isEnabled() {
        return System.getProperty(EXPERIMENTAL_CONFIG_PROPERTY) != null;
    }

    /**
     * Create a new experimental harness manager. This class is a singleton,
     * instances should be accessed through ExperimentalHarnessManager.get().
     */
    private ExperimentalHarnessManager() {
        // Private constructor. Access via get().
    }

    /**
     * Set the core oneswarm interface, so that experiments are able to
     * access oneswarm internals.
     * 
     * @param core
     *            The Oneswarm Core.
     */
    public void setCore(CoreInterface core) {
        this.core = core;
    }

    /**
     * Get the core oneswarm interface - meant for use by experiments.
     * 
     * @return The Oneswarm Core.
     */
    public CoreInterface getCoreInterface() {
        return core;
    }

    /**
     * Start the ExperimentalHarness - Load all experiments, and
     * process commands listed in the experimental configuration file.
     */
    public void start() {
        // Distribute initial commands.
        try {
            String path = System.getProperty(EXPERIMENTAL_CONFIG_PROPERTY);
            if (path != null) {
                logger.info("Loading experimental configuration from " + path);

                BufferedReader in = new BufferedReader(new FileReader(path));
                while (in.ready()) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    distribute(line);
                }
                ConfigurationManager.getInstance().setDirty();

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }
    }

    /**
     * Attempt to execute a command string by distributing it to the first
     * experiment which lists the first word of the command in it's list of
     * keys.
     * 
     * @param command
     *            The command to execute.
     */
    protected void distribute(String command) {
        String[] toks = command.split(" ");
        String key = toks[0];
        if (key.equals("inject")) {
            try {
                // Huzzah for reflection.
                ExperimentInterface experiment = (ExperimentInterface) Thread.currentThread()
                        .getContextClassLoader().loadClass(toks[1].trim()).getConstructor()
                        .newInstance();
                logger.info("Added experiments in " + toks[1]);
                experiment.load();
                experiments.add(experiment);
            } catch (Exception e) {
                logger.warning("Could not load experiment provider " + toks[1]);
                logger.warning("Classpath is " + System.getProperty("java.class.path", null));
                e.printStackTrace();
            }
            return;
        }

        for (ExperimentInterface ei : experiments) {
            for (String offer : ei.getKeys()) {
                if (offer.equals(key)) {
                    logger.info(key + " is executed by " + ei.getClass().getName());
                    ei.execute(command);
                    return;
                }
            }
        }
    }

    public void enqueue(String[] commands) {
        for (String command : commands) {
            distribute(command);
        }
        ConfigurationManager.getInstance().setDirty();
    }
}
