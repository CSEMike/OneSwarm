package edu.washington.cs.oneswarm.f2f;

public interface ExperimentInterface {
    /**
     * Get the keys which this experiment can interpret.
     * 
     * @return A list of keys the experiment is able to handle.
     */
    public String[] getKeys();

    /**
     * Execute a command received from a test or loaded from the
     * startup list of experiment commands. The first token in
     * the command will be a key listed in getKeys().
     * 
     * @param command
     *            The Command to execute.
     */
    public void execute(String command);

    /**
     * Called at startup to allow the experiment to perform desired startup
     * tasks.
     */
    public void load();
}
