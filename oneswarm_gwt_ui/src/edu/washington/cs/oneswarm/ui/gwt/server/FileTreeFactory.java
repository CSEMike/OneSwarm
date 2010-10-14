package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.oneswarm.ui.gwt.rpc.FileTree;

public class FileTreeFactory {
	public static void main(String[] args) {
		FileTree tree;
		try {
			tree = createFileTree(new File("/Volumes/im"));
			tree.print(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static FileTree createFileTree(File root) throws IOException 
	{
		FileTree tree = new FileTree();

		if (root.isDirectory()) {
			File[] files = root.listFiles(new FilenameFilter(){
				public boolean accept( File dir, String name ) {
					return name.startsWith(".") == false && !ignored(name); // no need to view hidden files 
				}});
			if( files == null )
				return null;
			List<FileTree> children = new ArrayList<FileTree>(files.length);
			for (int i = 0; i < files.length; i++) {
				FileTree c = createFileTree(files[i]);
				if( c != null ) // we might not have perms everywhere...
					children.add(c);
			}
			tree.setChildren(children.toArray(new FileTree[0]));
		}
		tree.setName(root.getName());
		tree.setFullpath(root.getCanonicalPath());
		return tree;
	}

	public static boolean ignored(String name) {
		return name.equals("Desktop DF") ||
				name.equals("Desktop DB") || 
				name.equals("thumbs.db") || 
				name.equals("desktop.ini");
	}

}
