package org.gudy.azureus2.core3.disk;



import java.io.File;
import java.io.IOException;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * @author Olivier
 * 
 */
public interface 
DiskManagerFileInfo 
{
	
	// *****************************************************************
	  /* EDIT:
	   * made by isdal@cs.washington.edu
	   * 
	   * added support for in order download of video files
	   * always specify the file suffix in lower case
	   */
	
	public final static String[] PLAYABLE_TYPES = { "asf","avi", "mpeg", "mpg",
		"xvid", "divx", "flv", "mp3", "mov", "wmv","mkv","mp4","wma" ,"aac","m4v","flac","ogv","oga","ogg","m4b"};
	public final static String[] PLAYABLE_TYPES_WITH_INDEX = { "asf", "avi", "mpeg", "mpg",
		"xvid", "divx", "mov", "wmv","mkv","mp4","wma","m4v","flac","ogv","oga","ogg","m4b"};

	public boolean isInOrderDownload();

	
	// *****************************************************************
	
	public static final int READ = 1;
	public static final int WRITE = 2;

	public static final int	ST_LINEAR	= 1;
	public static final int	ST_COMPACT	= 2;
	
		// set methods
		
	public void setPriority(boolean b);
	
	public void setSkipped(boolean b);
	 
	/**
	 * Relink the file to the destination given - this method deals with if the file
	 * is part of a simple torrent or not (so it may set the download name to keep it
	 * in sync). If you just want a simple relink, use setLinkAtomic.
	 * 
	 * @param link_destination
	 * @return
	 */
	public boolean
	setLink(
		File	link_destination );
	
	public boolean setLinkAtomic(File link_destination);
	
		// gets the current link, null if none
	
	public File
	getLink();
	
		/**
		 * Download must be stopped before calling this!
		 * @param type	one of ST_LINEAR or ST_COMPACT
		 */
	
	public boolean
	setStorageType(
		int		type );
	
	public int
	getStorageType();
	
	 	// get methods
	 	
	public int getAccessMode();
	
	public long getDownloaded();
	
	public String getExtension();
		
	public int getFirstPieceNumber();
  
	public int getLastPieceNumber();
	
	public long getLength();
		
	public int getNbPieces();
			
	public boolean isPriority();
	
	public boolean isSkipped();
	
	public int	getIndex();
	
	public DownloadManager	getDownloadManager();
	
	public DiskManager getDiskManager();
	
	public File getFile( boolean follow_link );
	
	public TOTorrentFile
	getTorrentFile();
	
	public DirectByteBuffer
	read(
		long	offset,
		int		length )
	
		throws IOException;
	
	public void
	flushCache()
	
		throws	Exception;
	
	public void
	close();
	
	public void
	addListener(
		DiskManagerFileInfoListener	listener );
	
	public void
	removeListener(
		DiskManagerFileInfoListener	listener );
	
}
