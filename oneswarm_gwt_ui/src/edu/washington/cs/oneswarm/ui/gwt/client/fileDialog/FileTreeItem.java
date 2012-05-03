package edu.washington.cs.oneswarm.ui.gwt.client.fileDialog;


import com.google.gwt.user.client.ui.IsTreeItem;
import com.google.gwt.user.client.ui.TreeItem;

import edu.washington.cs.oneswarm.ui.gwt.shared.fileDialog.FileItem;

public class FileTreeItem extends TreeItem implements IsTreeItem {
	private FileItem file;

	public FileTreeItem(FileItem file) {
		super(file.name);
		this.file = file;
	}

	public String fileName() {
		return file.name;
	}

	public String filePath() {
		return file.filePath;
	}

	public boolean isDirectory() {
		return file.isDirectory;
	}
}
