package edu.washington.cs.oneswarm.ui.gwt.client.fileDialog;

import com.google.gwt.user.client.ui.TreeItem;

import edu.washington.cs.oneswarm.ui.gwt.rpc.FileInfo;

public class FileTreeItem extends TreeItem {
	private FileInfo file;

	public FileTreeItem(FileInfo file) {
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
	
	public FileInfo.FileStatusFlag fileStatus(){
		return file.statusFlag;
	}
}
