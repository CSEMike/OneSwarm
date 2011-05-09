package edu.washington.cs.oneswarm.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/** Utility class which forks a thread to print the logs of a {@code Process}. */
public class ProcessLogConsumer extends Thread {

    private static Logger logger = Logger.getLogger(ProcessLogConsumer.class.getName());

    /** The process from which to consume logs. */
    Process process;

    /** A label for the log of this process. */
    String label;

    public ProcessLogConsumer(String label, Process process) {
        this.label = label;
        this.process = process;
        setName("ProcessLogConsumer: " + label);
        setDaemon(true);
    }

    /** The consumer thread task. */
    @Override
    public void run() {

        logger.info("Started ProcessLogConsumer: " + label);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                logger.info("[" + label + "]: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("[" + label + "]: Ended.");
    }
}
