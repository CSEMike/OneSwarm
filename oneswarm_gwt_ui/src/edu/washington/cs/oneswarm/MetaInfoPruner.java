package edu.washington.cs.oneswarm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.watchdir.UpdatingFileTree;
import edu.washington.cs.oneswarm.watchdir.UpdatingFileTreeListener;

public class MetaInfoPruner {
	
	public static final String ACCESS_TIMES_FNAME = "accesses.properties";
	
	private static Logger logger = Logger.getLogger(MetaInfoPruner.class.getName());
	
	Properties activeHashes = new Properties();
	int lastSaveCount = 0;

	private boolean unsaved = false;
	
	private static final MetaInfoPruner inst = new MetaInfoPruner();
	
	public static MetaInfoPruner get() {
		return inst;
	}
	
	private MetaInfoPruner() {		
		activeHashes.setProperty("created", Long.toString(System.currentTimeMillis()));
		
		load_active_hashes();
		
		lastSaveCount = activeHashes.size();
	}
	
	private void load_active_hashes() {
		synchronized(activeHashes)
		{
			try {
				activeHashes.load(new FileInputStream(SystemProperties.getMetaInfoPath() + SystemProperties.SEP + ACCESS_TIMES_FNAME));
				logger.info("loaded: " + activeHashes.size() + " hash accesses. history created: " + new Date(Long.parseLong(activeHashes.getProperty("created"))));
			}
			catch( IOException e )
			{
				logger.warning("couldn't load metainfo access times!");
				e.printStackTrace();
			}
		}
	}

	protected void save_active_hashes() {
		if( unsaved == false ) {
			return;
		}
		
		synchronized(activeHashes)
		{
			try {
				logger.info("save active hashes");
				FileOutputStream out = new FileOutputStream(SystemProperties.getMetaInfoPath() + SystemProperties.SEP + ACCESS_TIMES_FNAME);
				activeHashes.setProperty("lastsave", Long.toString((new Date()).getTime()));
				activeHashes.store(out, "infohash -> last observation");
				out.flush();
				
				lastSaveCount = activeHashes.size();
				unsaved = false;
			}
			catch( IOException e )
			{
				logger.warning("couldn't load metainfo access times!");
				e.printStackTrace();
			}
		}
	}

	public void recordActiveHash(String inHexHash) {
		synchronized(activeHashes)
		{
			activeHashes.setProperty(inHexHash, Long.toString(System.currentTimeMillis()));
			
			int upper_thresh = (int)Math.min((double)lastSaveCount + 0.2*((double)lastSaveCount), (double)(lastSaveCount + 20));
			
			logger.finest("record: " + inHexHash + " thresh: " + upper_thresh + " curr: " + lastSaveCount); 
			
			/**
			 * If we've added a bunch, or if it's been an hour since our last save 
			 */
			long lastSave = 0;
			Object o = activeHashes.getProperty("lastsave");
			if( o != null ) {
				lastSave = (new Date(Long.parseLong((String)o))).getTime();
			}
			if( upper_thresh < activeHashes.size() || (lastSave + 900000) < System.currentTimeMillis() ) // 15 minutes
			{
				save_active_hashes();
			}
		}
	}

	public void prune() {
		
		logger.info("Pruning metainfo...");
		
		File metainfo_dir_file = new File(SystemProperties.getMetaInfoPath());
		if( metainfo_dir_file.exists() == false )
		{
			logger.warning("couldn't get/create metainfo directory. skipping pruning.");
			return;
		}
		
		/*
		 * First, update the 'observed' time of all the swarms we have locally
		 */
		for (DownloadManager dm : (List<DownloadManager>)AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers()) {
			try {
				if( dm.getTorrent() == null ) {
					logger.warning("Skipped null torrent for: " + dm.getDisplayName());
					continue;
				}
				recordActiveHash(ByteFormatter.encodeString(dm.getTorrent().getHash()));
			} catch (TOTorrentException e1) {
				logger.severe("TorrentException while recording active hash: " + e1.toString());
				e1.printStackTrace();
			}
		}
		
		UpdatingFileTree metainfo_dir = new UpdatingFileTree(metainfo_dir_file, new UpdatingFileTreeListener(){
			public void broadcastChange(UpdatingFileTree arg0, boolean arg1) {
				// ignored since we never update this obejct
			}});
		List<UpdatingFileTree> stack = new ArrayList<UpdatingFileTree>();
		stack.add(metainfo_dir);
		
		int removed = 0;
		
		if( activeHashes.getProperty("created") == null ) {
			activeHashes.setProperty("created", Long.toString(System.currentTimeMillis()));
		}
		
		/**
		 * only remove 300 at a time to avoid lagging for a long time since we just started doing this...
		 */
		while( stack.size() > 0 && removed < 300 )
		{
			UpdatingFileTree curr = stack.remove(0);
			if( curr.isDirectory() && curr.getThisFile().getName().length() == 32 )
			{
				byte [] hash = Base32.decode(curr.getThisFile().getName());
				String hexHash = ByteFormatter.encodeString(hash);
				if( activeHashes.getProperty(hexHash, null) == null )
				{
					if( oldEnough(Long.parseLong(activeHashes.getProperty("created"))) )
					{
						logger.fine("don't know anything about hash, but history old enough for removal: " + hexHash);
						remove(curr, hexHash);
						removed++;
					}
					else
					{
						logger.fine("don't know anything about hash and history not old enough for removal: " + hexHash);		
					}
				}
				else
				{
					if( oldEnough(Long.parseLong(activeHashes.getProperty(hexHash))) )
					{
						logger.fine("removing metainfo for old swarm: " + hexHash);
						remove(curr, hexHash);
						removed++;
					}
				}
			}
			else
			{
				for( UpdatingFileTree kid : curr.getChildren() )
				{
					stack.add(kid);
				}
			}
		}
		
		logger.info("save active hashes...");
		save_active_hashes();
	} 
	
	private void remove(UpdatingFileTree curr, String hexHash) {
		
		if( curr.getThisFile().getAbsolutePath().startsWith(SystemProperties.getMetaInfoPath()) ) {
			logger.fine("would be Removing: " + curr.getThisFile().getAbsolutePath());
			FileUtil.recursiveDelete(curr.getThisFile());
			activeHashes.remove(hexHash);
			unsaved = true;
		} else {
			logger.warning("Meta info remove doesn't start with metainfo directory! " + curr.getThisFile().getAbsolutePath());
		}
	}

	private static boolean oldEnough( long time )
	{
		long monthBack = System.currentTimeMillis() - (2629743L*1000L); // 1 month
		if( time < monthBack )
		{
			logger.finest("Time " + (new Date(time)) + " is old enough (relative to: " + (new Date(monthBack)) + ") " + time + " / " + monthBack); 
			return true;
		}
		return false;
	}
	
	/**
	 * testing only
	 */
	
	public static void main( String [] args ) throws Exception {
		
		File metainfo_dir_file = new File(SystemProperties.getMetaInfoPath());
		
		UpdatingFileTree metainfo_dir = new UpdatingFileTree(metainfo_dir_file, new UpdatingFileTreeListener(){
			public void broadcastChange(UpdatingFileTree arg0, boolean arg1) {}});
		List<UpdatingFileTree> stack = new ArrayList<UpdatingFileTree>();
		stack.add(metainfo_dir);
		
		Properties activeHashes = new Properties();
		activeHashes.load(new FileInputStream(SystemProperties.getMetaInfoPath() + SystemProperties.SEP + ACCESS_TIMES_FNAME));
		
		while( stack.size() > 0 )
		{
			UpdatingFileTree curr = stack.remove(0);
			if( curr.isDirectory() && curr.getThisFile().getName().length() == 32 )
			{
				byte [] hash = Base32.decode(curr.getThisFile().getName());
				String hexHash = ByteFormatter.encodeString(hash);
				
				System.out.println("Considering hash dir: " + curr.getThisFile().getName() + " / " + hexHash);
				
				if( activeHashes.getProperty(hexHash, null) == null )
				{
					if( oldEnough(Long.parseLong(activeHashes.getProperty("created"))) )
					{
						System.out.println("\tdon't know anything about hash, but history old enough for removal");
					}
					else
					{
						System.out.println("\tdon't know anything about hash and history not old enough for removal");		
					}
				}
				else
				{
					if( oldEnough(Long.parseLong(activeHashes.getProperty(hexHash))) )
					{
						System.out.println("\twould remove metainfo for old swarm: " + hexHash);
					} else { 
						System.out.println("\ttoo young for metainfo removal");
					}
				}
			}
			else
			{
				for( UpdatingFileTree kid : curr.getChildren() )
				{
					stack.add(kid);
				}
			}
		}
	}
}
