package org.gudy.azureus2.platform.unix;

import java.io.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;

public class ScriptAfterShutdown
{
	private static PrintStream sysout;

	public static void main(String[] args) {
		// Set transitory so not everything gets loaded up. (such as the AEDiagnostic's tidy flag)
		System.setProperty("transitory.startup", "1");

		// Since stdout will be in a shell script, redirect any stdout not coming
		// from us to stderr 
		sysout = System.out;
		try {
			System.setOut(new PrintStream(new FileOutputStream("/dev/stderr")));
		} catch (FileNotFoundException e) {
		}

		String extraCmds = COConfigurationManager.getStringParameter(
				"scriptaftershutdown", null);
		if (extraCmds != null) {
			boolean exit = COConfigurationManager.getBooleanParameter(
					"scriptaftershutdown.exit", false);
			if (exit) {
				COConfigurationManager.removeParameter("scriptaftershutdown.exit");
			}
			COConfigurationManager.removeParameter("scriptaftershutdown");
			COConfigurationManager.save();
			sysout.println(extraCmds);
			if (exit) {
				sysout.println("exit");
			}
		} else {
			log("No shutdown tasks to do");
		}
	}

	public static void addExtraCommand(String s) {
		String extraCmds = COConfigurationManager.getStringParameter(
				"scriptaftershutdown", null);
		if (extraCmds == null) {
			extraCmds = s + "\n";
		} else {
			extraCmds += s + "\n";
		}
		COConfigurationManager.setParameter("scriptaftershutdown", extraCmds);
	}

	public static void setRequiresExit(boolean requiresExit) {
		if (requiresExit) {
			COConfigurationManager.setParameter("scriptaftershutdown.exit", true);
		}
	}
	
	private static void log(String string) {
		sysout.println("echo \"" + string.replaceAll("\"", "\\\"") + "\"");
	}
}
