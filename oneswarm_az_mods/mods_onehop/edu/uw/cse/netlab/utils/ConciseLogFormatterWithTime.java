package edu.uw.cse.netlab.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ConciseLogFormatterWithTime
	extends Formatter
{
	static long				 startTime = System.currentTimeMillis();

	static NumberFormat formatter = new DecimalFormat("0000.000");

	public String format(LogRecord record) {
		long time = System.currentTimeMillis() - startTime;
		String seconds = formatter.format(time / 1000.0);
		return seconds + ": " + record.getLoggerName() + "."
				+ record.getSourceMethodName() + "(): " + record.getMessage() + "\n";
	}

}
