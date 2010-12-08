package edu.washington.cs.oneswarm.ui.gwt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

public class BackendErrorLog {
	static BackendErrorLog inst = new BackendErrorLog();

	public class ErrorReport {
		public String message;
		public boolean show_report_text;

		public ErrorReport(String message, boolean show) {
			this.message = message;
			this.show_report_text = show;
		}

		public ErrorReport(String message) {
			this(message, true);
		}
	}

	private final String prefix;
	private ArrayList<ErrorReport> reports = new ArrayList<ErrorReport>();

	String f2fVersion = "";
	String gwtVersion = "";

	private BackendErrorLog() {
		try {
			f2fVersion = AzureusCoreImpl.getSingleton().getPluginManager()
					.getPluginInterfaceByID("osf2f").getPluginVersion();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			final PluginInterface plugin = AzureusCoreImpl.getSingleton().getPluginManager()
					.getPluginInterfaceByID("osgwtui");
			if (plugin != null) {
				gwtVersion = plugin.getPluginVersion();
			} else {
				Debug.out("osgwt plugin not loaded");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		prefix = f2fVersion + "/" + gwtVersion + " ";
	}

	public static BackendErrorLog get() {
		return inst;
	}

	public synchronized void logException(Throwable e) {
		StringWriter out = new StringWriter();
		e.printStackTrace(new PrintWriter(out));
		reports.add(new ErrorReport(prefix + out.getBuffer().toString()));
	}

	@SuppressWarnings("unchecked")
	public synchronized ArrayList<ErrorReport> getReports() {
		ArrayList<ErrorReport> out = reports;
		reports = new ArrayList<ErrorReport>();
		return out;
	}

	public synchronized void logString(String string) {
		logString(string, true);
	}

	public synchronized void logString(String string, boolean show_report) {
		reports.add(new ErrorReport(string, show_report));
	}
}
