package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendErrorReport;

public class BackendErrorLog {
	static BackendErrorLog inst = new BackendErrorLog();

	private String prefix;
	private ArrayList<BackendErrorReport> reports = new ArrayList<BackendErrorReport>();

	String f2fVersion = "";
	String gwtVersion = "";

	private BackendErrorLog() {
		try {
			f2fVersion = AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByID("osf2f").getPluginVersion();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			final PluginInterface plugin = AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByID("osgwtui");
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

	public synchronized void logException(Exception e) {
		StringWriter out = new StringWriter();
		e.printStackTrace(new PrintWriter(out));
		reports.add(new BackendErrorReport(prefix + out.getBuffer().toString()));
	}

	@SuppressWarnings("unchecked")
	public synchronized ArrayList<BackendErrorReport> getReports() {
		ArrayList<BackendErrorReport> out = reports;
		reports = new ArrayList<BackendErrorReport>();
		return out;
	}

	public synchronized void logString(String string) {
		logString(string, true);
	}
	
	public synchronized void logString(String string, boolean show_report) { 
		reports.add(new BackendErrorReport(string, show_report));
	}
}
