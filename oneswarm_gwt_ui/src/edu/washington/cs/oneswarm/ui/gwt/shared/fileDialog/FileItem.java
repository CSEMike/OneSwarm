package edu.washington.cs.oneswarm.ui.gwt.shared.fileDialog;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FileItem implements Serializable, IsSerializable, Comparable<FileItem>{
	private static final long serialVersionUID = 3L;
	public String filePath;
	public String name;
	public boolean isDirectory;
	
	public FileItem(){}
	
	public FileItem(String filePath, String name, boolean isDirectory){
		this.filePath = filePath;
		this.name = name;
		this.isDirectory = isDirectory;
	}

	public int compareTo(FileItem o) {
		if(isDirectory & !o.isDirectory)
			return 1;
		if(o.isDirectory & isDirectory)
			return -1;
		return filePath.compareTo(o.filePath);
	}
}
