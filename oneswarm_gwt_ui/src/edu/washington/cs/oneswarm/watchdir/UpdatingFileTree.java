package edu.washington.cs.oneswarm.watchdir;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Constants;

import edu.uw.cse.netlab.reputation.GloballyAwareOneHopUnchoker;

public class UpdatingFileTree {
	
	public static final long IDLE_THRESHOLD = 60 * 1000; // 1 minute without mods
	
	private static Logger logger = Logger.getLogger(UpdatingFileTree.class.getName());
	
	List<UpdatingFileTree> children = Collections.synchronizedList(new ArrayList<UpdatingFileTree>());
	File thisFile = null;
	long lastRefreshed = 0;
	boolean gone = false;
	private UpdatingFileTreeListener mSpawn;
	boolean mHasDirectoryChildren = false;
	String mRelativePath = null;
	
	public UpdatingFileTree( File root ) {
		this(root, new UpdatingFileTreeListener(){
			public void broadcastChange(UpdatingFileTree path, boolean isDelete) {}}, Integer.MAX_VALUE );
	}
	
	public UpdatingFileTree( File root, UpdatingFileTreeListener spawn ) {
		this(root, spawn, Integer.MAX_VALUE);
	}
	
	public UpdatingFileTree( File root, UpdatingFileTreeListener spawn, int maxDepth )
	{
		thisFile = root;
		
		mSpawn = spawn;
		
		lastRefreshed = thisFile.lastModified();
		
		if( thisFile.isDirectory() ) {
			File[] files = thisFile.listFiles(new FilenameFilter(){
				public boolean accept( File dir, String name ) {
					/**
					 * We don't want to hash trash on windows
					 */
					if( name.toLowerCase().equals("recycler") && Constants.isWindows )
						return false;
					
					return name.startsWith(".") == false; // skip hidden files
				}});
			if( files != null && maxDepth > 0 )
			{
				List<UpdatingFileTree> children = new ArrayList<UpdatingFileTree>(files.length);
				for (int i = 0; i < files.length; i++) {
					
					UpdatingFileTree c = new UpdatingFileTree(files[i], mSpawn);
					if( c != null ) // we might not have perms everywhere...
					{
						if( c.thisFile.lastModified() + IDLE_THRESHOLD < System.currentTimeMillis() )
						{
							children.add(c);
							if( c.isDirectory() )
							{
								mHasDirectoryChildren = true;
							}
						}
						else
						{
							logger.fine("skipping fresh child: " + c.thisFile.getAbsolutePath()); 
						}
					}
				}
				
				this.children = children;
			}
			else
			{
				gone = true;
			}
		}
	}
	
	public long getLastModified() { 
		return lastRefreshed;
	}
	
	/**
	 * so we get directories before their files...
	 */
	public void broadcast()
	{
		mSpawn.broadcastChange(this, false);
		for( UpdatingFileTree c : children )
			c.broadcast();
	}
	
	public boolean isDirectory() { return thisFile.isDirectory(); }
	
	public void update()
	{
		logger.finest("update of: " + this.getThisFile().getAbsolutePath());
		if( thisFile.lastModified() != lastRefreshed || mHasDirectoryChildren )
		{
			if( thisFile.exists() == false ) {
				this.gone = true;
			}
			
			/**
			 * 1. check if children still there
			 */
			mHasDirectoryChildren = false;
			if( children != null )
			{
				for( UpdatingFileTree c : children.toArray(new UpdatingFileTree[0]) )
				{
					if( c.gone )
					{
						children.remove(c);
						logger.finest("child gone this pass: " + c.getThisFile().getAbsolutePath());
					}
					else
					{
						c.update();
						if( c.isDirectory() )
						{
							mHasDirectoryChildren = true;
						}
					}
				}
			}
			
			/**
			 * 2. Check if there are new children
			 */
			File[] files = thisFile.listFiles(new FilenameFilter(){
				public boolean accept( File dir, String name ) {
					return name.startsWith(".") == false; // skip hidden files
				}});
			if( files != null )
			{
				for (int i = 0; i < files.length; i++) {
					boolean newFile = true; 
					for( UpdatingFileTree c : children )
					{
						if( c.thisFile.equals(files[i]) == true )
						{
							newFile = false;
							break;
						}
					}
					if( newFile )
					{
						UpdatingFileTree neu = new UpdatingFileTree(files[i], mSpawn);
						long thresh = neu.thisFile.lastModified() + IDLE_THRESHOLD;
						if( thresh < System.currentTimeMillis() )
						{
							children.add(neu);
							if( neu.isDirectory() )
							{
								mHasDirectoryChildren = true;
							}
							neu.broadcast();
						}
						else
						{
							logger.fine("skipping too-fresh file: " + neu.thisFile.getAbsolutePath() + " " + (thresh-System.currentTimeMillis()) + "s left");
						}
					}
				}
			}
			else if( thisFile.exists() == false )
			{
				logger.finest(getThisFile().getAbsolutePath() + " will be gone next pass");
				gone = true; // will get removed during the next pass
				mSpawn.broadcastChange(this, true);
			}
		} else if( thisFile.exists() == false ) {
			logger.finest(getThisFile().getAbsolutePath() + " will be gone next pass");
			gone = true; // will get removed during the next pass
			mSpawn.broadcastChange(this, true);
		}
	}

	public List<UpdatingFileTree> getDirectoryChildren()
	{
		if( mHasDirectoryChildren == false )
		{
			return new ArrayList<UpdatingFileTree>(0);
		}
		else
		{
			List<UpdatingFileTree> out = new ArrayList<UpdatingFileTree>();
			for( UpdatingFileTree u : children )
			{
				if( u.isDirectory() && !u.gone )
					out.add(u);
			}
			return out;
		}
	}
	
	public List<UpdatingFileTree> getFileChildren()
	{
		List<UpdatingFileTree> out = new ArrayList<UpdatingFileTree>();
		for( UpdatingFileTree u : children )
		{
			if( u.isDirectory() == false && !u.gone )
				out.add(u);
		}
		return out;
	}
	
	public List<UpdatingFileTree> getChildren()
	{
		return children;
	}
	
	public String toString() 
	{
		return thisFile.getName() + (isDirectory() && children != null ? " (" + children.size() + " children)" : ""); 
	}

	public File getThisFile() {
		return thisFile;
	}
	
	public long modifiedChecksum() { 
		long sum = getLastModified();
		for( UpdatingFileTree k : children ) { 
			sum += k.modifiedChecksum();
		}
		return sum;
	}
}
