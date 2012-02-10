package edu.washington.cs.oneswarm.test.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uw.cse.netlab.utils.ConciseLogFormatterWithTime;

public class OneSwarmTestBase {

    private void enableLogging(Logger logger) {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new ConciseLogFormatterWithTime());
        handler.setLevel(Level.ALL);
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.addHandler(handler);
    }

    protected void logFinest(Logger logger) {
        enableLogging(logger);
        logger.setLevel(Level.ALL);
    }

    protected void logFiner(Logger logger) {
        enableLogging(logger);
        logger.setLevel(Level.FINER);
    }

    protected void logFine(Logger logger) {
        enableLogging(logger);
        logger.setLevel(Level.FINE);
    }

    protected void logInfo(Logger logger) {
        enableLogging(logger);
        logger.setLevel(Level.INFO);
    }

    protected void saveResult(String filename, String[] header, Object[] data) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        writeAsCsv(header, out);
        writeAsCsv(data, out);
        out.close();
    }

    private void writeAsCsv(Object[] data, BufferedWriter out) throws IOException {
        for (int i = 0; i < data.length; i++) {
            out.append("" + data[i]);
            if (i + 1 < data.length) {
                out.append(",");
            }
        }
        out.append("\n");
    }
}
