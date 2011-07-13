package edu.uw.cse.netlab.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ConciseLogFormatterWithTime extends Formatter {
    static long startTime = System.currentTimeMillis();
    static long lastLogTime = System.currentTimeMillis();

    static NumberFormat formatter = new DecimalFormat("0000.000");
    static NumberFormat shortFormatter = new DecimalFormat("###0.000");

    public String format(LogRecord record) {
        long curr = System.currentTimeMillis();
        long relative = curr - startTime;
        long diff = curr - lastLogTime;
        lastLogTime = curr;
        String relSeconds = formatter.format(relative / 1000.0);
        String diffSeconds = shortFormatter.format(diff / 1000.0);
        return relSeconds + "+" + diffSeconds + ": " + record.getLoggerName() + "."
                + record.getSourceMethodName() + "(): " + record.getMessage() + "\n";
    }
}
