package edu.uw.cse.netlab.utils;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ConciseLogFormatter extends Formatter {

	public String format(LogRecord record) {
		return record.getLoggerName() + ": " + record.getMessage() + "\n";
	}

}
