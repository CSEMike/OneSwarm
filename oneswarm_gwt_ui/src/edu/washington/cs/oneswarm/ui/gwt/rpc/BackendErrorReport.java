package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BackendErrorReport implements IsSerializable {
	
	String message;
	boolean show_report_text;
	
	public BackendErrorReport() {
		show_report_text = true;
	}
	
	public BackendErrorReport( String message ) { 
		this(message, true);
	}
	
	public BackendErrorReport( String message, boolean showReport ) { 
		this.message = message;
		this.show_report_text = showReport;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean isShowReportText() { 
		return show_report_text;
	}
}
