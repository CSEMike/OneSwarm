package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FileInfo implements Serializable, IsSerializable, Comparable<FileInfo>{
	private static final long serialVersionUID = 3L;
	public String filePath;
	public String name;
	public boolean isDirectory;
	public FileStatusFlag statusFlag;
	
	public FileInfo(){}

	public enum FileStatusFlag {OK, NO_READ_PERMISSION}
	
	public FileInfo(String filePath, String name, boolean isDirectory, boolean isReadable){
		this.filePath = filePath;
		this.name = name;
		this.isDirectory = isDirectory;
		if(isReadable)
			statusFlag = FileStatusFlag.OK;
		else
			statusFlag = FileStatusFlag.NO_READ_PERMISSION;
	}

	public int compareTo(FileInfo o) {
		if(isDirectory & !o.isDirectory)
			return 1;
		if(o.isDirectory & isDirectory)
			return -1;
		return filePath.compareTo(o.filePath);
	}
}
