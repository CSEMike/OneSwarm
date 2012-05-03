package edu.washington.cs.oneswarm.ui.gwt.client.fileDialog;


import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.washington.cs.oneswarm.ui.gwt.shared.fileDialog.FileItem;

public interface ServerFileSystemAsync {
	void listFiles(String path, AsyncCallback<FileItem[]> callback);
}