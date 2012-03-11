/*
 * File    : Download.java
 * Created : 06-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.plugins.download;

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.plugins.download.savelocation.DefaultSaveLocationManager;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.peers.PeerManager;

/**
 * Management of a Torrent's activity.
 * 
 * <b>Note:</b> All listener based methods are now located in {@link DownloadEventNotifier}.
 * 
 * <PRE>
 * A download's lifecycle:
 * torrent gets added
 *    state -> QUEUED
 * slot becomes available, queued torrent is picked, "restart" executed
 *    state -> WAITING
 * state moves through PREPARING to READY
 *    state -> PREPARING
 *    state -> READY
 * execute "start" method
 *    state -> SEEDING -or- DOWNLOADING
 * if torrent is DOWNLOADING, and completes, state changes to SEEDING
 *
 * Path 1                   | Path 2
 * -------------------------+------------------------------------------------
 * execute "stop" method    | startstop rules are met, execute "stopandQueue"
 *    state -> STOPPING     |     state -> STOPPING
 *    state -> STOPPED      |     state -> STOPPED
 *                          |     state -> QUEUED
 * execute "remove" method -> deletes the download
 * a "stop" method call can be made when the download is in all states except STOPPED
 * </PRE>
 *
 * @author parg
 */

public interface 
Download extends DownloadEventNotifier
{
  /** waiting to be told to start preparing */  
	public static final int ST_WAITING     = 1;
  /** getting files ready (allocating/checking) */  
	public static final int ST_PREPARING   = 2;
  /** ready to be started if required */  
	public static final int ST_READY       = 3;
  /** downloading */  
	public static final int ST_DOWNLOADING = 4;
  /** seeding */  
	public static final int ST_SEEDING     = 5;
  /** stopping */  
	public static final int ST_STOPPING    = 6;
  /** stopped, do not auto-start! */  
	public static final int ST_STOPPED     = 7;
  /** failed */  
	public static final int ST_ERROR       = 8;
  /** stopped, but ready for auto-starting */  
	public static final int ST_QUEUED      = 9;

	public static final String[] ST_NAMES = 
		{
			"",
			"Waiting",
			"Preparing",
			"Ready",
			"Downloading",
			"Seeding",
			"Stopping",
			"Stopped",
			"Error",
			"Queued",
		};
	
  /** Use more of the upload bandwidth than low priority downloads 
   *  don't change these as they are used by remote clients */
	
	public static final int	PR_HIGH_PRIORITY	= 1;
  /** Use less of the upload bandwidth than high priority downloads */  
	public static final int	PR_LOW_PRIORITY		= 2;
	
	
		/**
		 * Flags values
		 * @since 2.3.0.5
		 */
	
	public static final long FLAG_ONLY_EVER_SEEDED			= 0x00000001;
	public static final long FLAG_SCAN_INCOMPLETE_PIECES	= 0x00000002;
	
	/**
	 * Flag value - if set, it prevents any of the "move on completion" or
	 * "move on removal" rules taking place.
	 * 
	 * @since 2.5.0.1
	 */
	public static final long FLAG_DISABLE_AUTO_FILE_MOVE 	= 0x00000004;

    /**
     * Flag value - if set, then it means this download has been considered
     * for "move on completion", and it should not be considered again for
     * it. This value is more for internal use rather than plugin use.
     * 
     * @since 2.5.0.1
     */
    public static final long FLAG_MOVE_ON_COMPLETION_DONE	= 0x00000008;
	
    /**
     * Flag value - if set the user won't be bothered with popups/completion events during
     * the download's life. This is used, for example, for downloads used to run speed-tests
     * @since 3.0.1.3
     */
    
    public static final long FLAG_LOW_NOISE					= 0x00000010;

    	/**
    	 * Flag value - normally the permitted peer sources for a download are fixed and can't be changed
    	 * this flag allows the permitted peer source set to be increased/decreased (but not beyond the enforced
    	 * values required to honour a torrent's 'private' flag
    	 */
    public static final long FLAG_ALLOW_PERMITTED_PEER_SOURCE_CHANGES = 0x00000020;
    
    
    /**
     * Flag value - if set the data will not be delete when the download is "deleted" from
     * the v3 interface.
     * @since 3.1.0.0
     */
    public static final long FLAG_DO_NOT_DELETE_DATA_ON_REMOVE = 0x00000040;
    
    /**
     * Force direct delete of download data when delete requested, rather than recoverable delete,
     * and no user prompt
     * @since 4.3.1.5
     */
    
    public static final long FLAG_FORCE_DIRECT_DELETE = 0x00000080;
    
    /**
     * Used to disable IP filter rules for a download when ip-filtering is enabled
     * @since 4.7.0.3
     */
    
    public static final long FLAG_DISABLE_IP_FILTER = 0x00000100;

    
	/** get state from above ST_ set
   * @return ST_ constant
   *
   * @since 2.0.7.0
   */
	public int
	getState();

	/**
	 * For the STOPPING state this method gives the state that is being transited too (STOPPED, QUEUED or ERROR)
	 * @return
	 * @since 2.3.0.5
	 */
	
	public int
	getSubState();
	
	/** When the download state is ERROR this method returns the error details
   * @return
   *
   * @since 2.0.7.0
   */
	public String
	getErrorStateDetails();
	
		/**
		 * Get the flag value
		 * @since 2.3.0.5
		 * @param flag	FLAG value from above
		 * @return
		 */
	
	public boolean getFlag(long	flag);
	
	/**
	 * Set the flag value.
	 * 
	 * @since 2.5.0.1
	 * @param flag FLAG value from above
	 * @param set <code>true</code> to enable the flag, <code>false</code> to disable it.
	 */
	public void setFlag(long flag, boolean set);

	/**
	 * get all the flags as a bitmap
	 * @since 4209
	 * @return
	 */
	
	public long
	getFlags();
	
	/**
	 * Index of download. {@link #getPosition()}
	 * @return	index - 0 based
   *
   * @since 2.0.7.0
	 */
	public int
	getIndex();
	
	/**
	 * Each download has a corresponding torrent
	 * @return	the download's torrent
   *
   * @since 2.0.7.0
	 */
	public Torrent
	getTorrent();
	
	/**
	 * See lifecycle description above 
	 * @throws DownloadException
   *
   * @since 2.0.7.0
	 */
	public void
	initialize()
	
		throws DownloadException;

	/**
	 * See lifecycle description above 
	 * @throws DownloadException
   *
   * @since 2.0.7.0
	 */
	public void
	start()
	
		throws DownloadException;
	
	/**
	 * See lifecycle description above 
	 * @throws DownloadException
   *
   * @since 2.0.7.0
	 */
	public void
	stop()
	
		throws DownloadException;
	
	/**
	 * See lifecycle description above 
	 * @throws DownloadException
   *
   * @since 2.0.8.0
	 */
	public void
	stopAndQueue()
	
		throws DownloadException;

	/**
	 * See lifecycle description above 
	 * @throws DownloadException
   *
   * @since 2.0.7.0
	 */
	public void
	restart()
	
		throws DownloadException;
	
	
		/**
		 * Performs a complete recheck of the downloaded data 
		 * Download must be in stopped, queued or error state
		 * Action is performed asynchronously and will progress the download through
		 * states PREPARING back to the relevant state
		 * @throws DownloadException
		 * @since 2.1.0.3
		 */
	
	public void
	recheckData()
	
		throws DownloadException;
	
	/**
	 * When a download is "start-stop locked" it means that seeding rules shouldn't start or
	 * stop the download as it is under manual control
	 * @return True if download is locked and should not be started or stopped
   *
   * @since 2.0.7.0
	 */
	public boolean
	isStartStopLocked();
	
  /** Retrieves whether the download is force started
   * @return True if download is force started.  False if not.
   *
   * @since 2.0.8.0
   */  
	public boolean
	isForceStart();
	
  /** Set the forcestart state of the download
   * @param forceStart True - Download will start, despite any Start/Stop rules/limits<BR>
   * False - Turn forcestart state off.  Download may or may not stop, depending on
   * Start/Stop rules/limits
   *
   * @since 2.0.8.0
   */  
	public void
	setForceStart(boolean forceStart);
	
	/**
	 * Downloads can either be low or high priority (see PR_ constants above)
	 * @return the download's priority
	 *
	 * @deprecated >= 2.1.0.6 does nothing
	 * @since 2.0.7.0
	 */
	public int
	getPriority();
	
	/**
	 * This method sets a download's priority
	 * @param priority the required priority, see PR_ constants above
	 * @deprecated >= 2.1.0.6 does nothing
	 *
	 * @since 2.0.7.0
	 */
	public void
	setPriority(
		int		priority );
	
	/** When a download's priority is locked this means that seeding rules should not change
   * a downloads priority, it is under manual control
   * @return whether it is locked or not
   * @deprecated >= 2.0.8.0 does nothing
   *
   * @since 2.0.7.0
   */
	public boolean
	isPriorityLocked();
	
	/**
	 * @since 2403
	 * @return
	 */
	
	public boolean
	isPaused();
	
	/**
	 * Pause the download
	 * @since 2501
	 */
	
	public void
	pause();
	
	/**
	 * Resume the download if paused
	 * @since 2501
	 */
	
	public void
	resume();
	
	/** Returns the name of the torrent.  Similar to Torrent.getName() and is usefull
   * if getTorrent() returns null and you still need the name.
   * @return name of the torrent
   *
   * @since 2.0.8.0
   */
	public String 
	getName();
	
	/** Returns the full file path and name of the .torrent file
	 *
	 * @return File name of the torrent.
   *
   * @since 2.1.0.0
	 */
  public String getTorrentFileName();
  
  
  	/**
  	 * Gets an attribute of this download. For category use the Category torrent attribute
  	 * @param attribute
  	 * @return
  	 */
  
  public String
  getAttribute(
  	TorrentAttribute		attribute );
  
  /**
	 * Sets an attribute of this download. For category use the Category torrent attribute
   * 
   * @param attribute Previously created attribute
   * @param value Value to store.  null to remove attribute
   */
  public void
  setAttribute(
  	TorrentAttribute		attribute,
	String					value );
  
  public String[]
  getListAttribute(
	TorrentAttribute		attribute );
  
  /**
   * 
   * @param attribute
   * @param value
   * @since 2.5.0.1
   */
  public void setListAttribute(TorrentAttribute attribute, String[] value);
  
  /**
   * 
   * @param attribute
   * @param value		must be bencodable - key is string, value is Map, List, Long or byte[]
   */
  
  public void
  setMapAttribute(
	TorrentAttribute		attribute,
	Map						value );
  
  public Map
  getMapAttribute(
	TorrentAttribute		attribute );
  
  /**
   * Gets the value of the given attribute from the download. If no value is
   * set, then <code>0</code> will be returned.  
   */
  public int getIntAttribute(TorrentAttribute attribute);
  
  /**
   * Sets an integer attribute on this download.
   */
  public void setIntAttribute(TorrentAttribute attribute, int value);

  /**
   * Gets the value of the given attribute from the download. If no value is
   * set, then <code>0</code> will be returned.  
   */
  public long getLongAttribute(TorrentAttribute attribute);
  
  /**
   * Sets a long attribute on this download.
   */
  public void setLongAttribute(TorrentAttribute attribute, long value);

  /**
   * Gets the value of the given attribute from the download. If no value is
   * set, then <code>false</code> will be returned.  
   */
  public boolean getBooleanAttribute(TorrentAttribute attribute);
  
  /**
   * Sets a boolean attribute on this download.
   */
  public void setBooleanAttribute(TorrentAttribute attribute, boolean value);
  
  /**
   * Returns <code>true</code> if the download has an explicit value stored for
   * the given attribute.
   */
  public boolean hasAttribute(TorrentAttribute attribute);
  
  /** Returns the name of the Category
   *
   * @return name of the category
   *
   * @since 2.1.0.0
   * @deprecated Use TorrentAttribute.TA_CATEGORY (2.2.0.3)
   */
  public String getCategoryName();
  
  /** Sets the category for the download 
   *
   * @param sName Category name
   *
   * @since 2.1.0.0
   * @deprecated Use TorrentAttribute.TA_CATEGORY (2.2.0.3)
   */
  public void setCategory(String sName);

	/**
	 * Removes a download. The download must be stopped or in error. Removal may fail if another 
	 * component does not want the removal to occur - in this case a "veto" exception is thrown
	 * @throws DownloadException
	 * @throws DownloadRemovalVetoException
   *
   * @since 2.0.7.0
	 */
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException;
	
		/**
		 * Same as "remove" but, if successful, deletes the torrent and/or data
		 * @param delete_torrent
		 * @param delete_data
		 * @throws DownloadException
		 * @throws DownloadRemovalVetoException
		 * @since 2.2.0.3
		 */
	
	public void
	remove(
		boolean	delete_torrent,
		boolean	delete_data )
	
		throws DownloadException, DownloadRemovalVetoException;

	/**
	 * Returns the current position in the queue
	 * Completed and Incompleted downloads have seperate position sets.  This means
	 * we can have a position x for Completed, and position x for Incompleted.
   *
   * @since 2.0.8.0
	 */
	public int
	getPosition();
	
		/**
		 * returns the time this download was created in milliseconds
		 * @return
		 */
	
	public long
	getCreationTime();
	
	/**
	 * Sets the position in the queue
	 * Completed and Incompleted downloads have seperate position sets
   *
   * @since 2.0.8.0
	 */
	public void
	setPosition(
		int newPosition);

	/**
	 * Moves the download position up one
   *
   * @since 2.1.0.0
	 */
	public void
	moveUp();
	
	/**
	 * Moves the download down one position
   *
   * @since 2.1.0.0
	 */
	public void
	moveDown();
	
		/**
		 * Moves a download and re-orders the others appropriately. Note that setPosition does not do this, it
		 * merely sets the position thus making it possible, for example, for two downloads to have the same
		 * position 
		 * @param position
		 * @since 2.3.0.7
		 */
	
	public void
	moveTo(
		int		position );
	
	/**
	 * Tests whether or not a download can be removed. Due to synchronization issues it is possible
	 * for a download to report OK here but still fail removal.
	 * @return
	 * @throws DownloadRemovalVetoException
   *
   * @since 2.0.7.0
	 */
	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException;
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result );
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result );
	
	/**
	 * Gives access to the last announce result received from the tracker for the download
	 * @return
   *
   * @since 2.0.7.0
	 */
	
	public DownloadAnnounceResult
	getLastAnnounceResult();
	
	/**
	 * Gives access to the last scrape result received from the tracker for the download
	 * @return a non-null DownloadScrapeResult
   *
   * @since 2.0.7.0
	 */
	public DownloadScrapeResult
	getLastScrapeResult();
	
	/**
	 * Gives access to the current activation state. Note that we currently only fire the activation listener
	 * on an increase in activation requirements. This method however gives the current view of the state
	 * and takes into account decreases too
	 * @return
	 * @since 2.4.0.3
	 */
	
	public DownloadActivationEvent
	getActivationState();
	
	/**
	 * Gives access to the download's statistics
	 * @return
   *
   * @since 2.0.7.0
	 */
	public DownloadStats
	getStats();
	
	/** Downloads can be persistent (be remembered across Azureus sessions), or
	 * non-persistent.
	 *
	 * @return true - persistent<br>
	 *         false - non-persistent
     *
     * @since 2.1.0.0
	 */
	
    public boolean
    isPersistent();
  
    /**
     * Sets the maximum download speed in bytes per second. 0 -> unlimited
     * @since 2.1.0.2
     * @param kb
     */
    
  	public void
	setMaximumDownloadKBPerSecond(
		int		kb );
  	
  	public int
	getMaximumDownloadKBPerSecond();
    
    
	    /**
	     * Get the max upload rate allowed for this download.
	     * @return upload rate in bytes per second, 0 for unlimited, -1 for upload disabled
	     */
  	
    public int getUploadRateLimitBytesPerSecond();
    
	    /**
	     * Set the max upload rate allowed for this download.
	     * @param max_rate_bps limit in bytes per second, 0 for unlimited, -1 for upload disabled
	     */
    
    public void setUploadRateLimitBytesPerSecond( int max_rate_bps );
    
	    /**
	     * Get the max download rate allowed for this download.
	     * @return upload rate in bytes per second, 0 for unlimited, -1 for download disabled
	     * @since 3013
	     */
    
    public int getDownloadRateLimitBytesPerSecond();
    
	    /**
	     * Set the max download rate allowed for this download.
	     * @param max_rate_bps limit in bytes per second, 0 for unlimited, -1 for dowmload disabled
	     * @since 3013
	     */
    
    public void setDownloadRateLimitBytesPerSecond( int max_rate_bps );
  

	/**
	 * Indicates if the download has completed or not, exluding any files marked
	 * as Do No Download
	 *  
	 * @return Download Complete status
	 * @since 2.1.0.4
	 */
	public boolean isComplete();

	/**
	 * Indicates if the download has completed or not
	 *  
	 * @param bIncludeDND Whether to include DND files when determining 
	 *                     completion state
	 * @return Download Complete status
	 * 
	 * @since 2.4.0.3
	 */
	public boolean isComplete(boolean bIncludeDND);

  		/**
  		 * When a download is completed it is rechecked (if the option is enabled). This method
  		 * returns true during this phase (at which time the status will be seeding)
  		 * @return
  		 * @since 2.3.0.6
  		 */
  	
	public boolean
 	isChecking();
	
	/**
	 * This returns the full save path for the download. If the download is a simple torrent,
	 * this will be the full path of the file being downloaded. If the download is a multiple
	 * file torrent, this will be the path to the directory containing all the files in the
	 * torrent.
	 * 
	 * @return Full save path for this download.
	 */
  	public String
	getSavePath();
  	
  		/**
  		 * Move a download's data files to a new location. Download must be stopped and persistent
  		 * 
  		 * <p>
  		 * 
  		 * If a download is running, it will be automatically paused and resumed afterwards - be
  		 * aware that this behaviour may generate <tt>stateChanged</tt> events being fired.
		 *
		 * @since 2.3.0.5
  		 * @param new_parent_dir
  		 * @throws DownloadException
  		 */
  	
  	public void
  	moveDataFiles(
  		File	new_parent_dir )
  	
  		throws DownloadException;
  	
  	/**
  	 * Move a download's data files to a new location, and rename the download at the same time.
  	 * Download must be stopped and persistent. This is equivalent to calling <tt>moveDataFiles[File]</tt>
  	 * and then <tt>renameDownload[String]</tt>.
  	 * 
  	 * For convenience, either argument can be <tt>null</tt>, but not both.
  	 * 
  	 * <p>
  	 * 
  	 * If a download is running, it will be automatically paused and resumed afterwards - be
  	 * aware that this behaviour may generate <tt>stateChanged</tt> events being fired.
  	 * 
  	 * @since 3.0.2
  	 * @throws DownloadException
  	 * @see {@link #moveDataFiles(File)}
  	 * @see {@link #renameDownload(String)}
  	 */
  	public void moveDataFiles(File new_parent_dir, String new_name) throws DownloadException;
  	
  	
  		/**
		 * Move a download's torrent file to a new location. Download must be stopped and persistent
		 * @since 2.3.0.5
		 * @param new_parent_dir
		 * @throws DownloadException
		 */
  	public void
  	moveTorrentFile(
  		File	new_parent_dir ) 
  	
  		throws DownloadException;

  	/**
  	 * Renames the file (for a single file torrent) or directory (for a multi file torrent) where the
  	 * download is being saved to. The download must be in a state to move the data files to a new location
  	 * (see {@link #moveDataFiles(File)}).
  	 * 
  	 * <p>
  	 * 
  	 * This will not rename the displayed name for the torrent - if you wish to do that, you must do it via
  	 * the {@link org.gudy.azureus2.plugins.torrent.TorrentAttribute TorrentAttribute} class.
  	 * 
  	 * <p>
  	 * 
  	 * If a download is running, it will be automatically paused and resumed afterwards - be
  	 * aware that this behaviour may generate <tt>stateChanged</tt> events being fired.
  	 * 
  	 * @param name New name for the download.
  	 * @see #moveDataFiles(File)
  	 */
  	public void renameDownload(String name) throws DownloadException;
  	
  		/**
  		 * return the current peer manager for the download. 
  		 * @return	null returned if torrent currently doesn't have one (e.g. it is stopped)
  		 */
  	
  	public PeerManager
	getPeerManager();
  	
		/**
		 * Return the disk manager, null if its not running
		 * @return
		 * @since 2.3.0.1
		 */
	
	public DiskManager
	getDiskManager();
	
		/**
		 * Returns info about the torrent's files. Note that this will return "stub" values if the 
		 * download isn't running (not including info such as completion status)
		 * @return
		 * @since 2.3.0.1
		 */
	
	public DiskManagerFileInfo[]
	getDiskManagerFileInfo();
	
	/**
	 * Returns file info for the given index. Note that this will return "stub" values if the 
	 * download isn't running (not including info such as completion status)
	 * @return null if index is invalid
	 * @since 4.3.1.5
	 */

  public DiskManagerFileInfo
  getDiskManagerFileInfo(int index);

  /**
   * Return the number of DiskManagerFile objects
   * @return
   * @since 4.6.0.5
   */
	public int getDiskManagerFileCount();

  		/**
  		 * request a tracker announce 
  		 * @since 2.1.0.5
  		 */
  	
  	public void
	requestTrackerAnnounce();
  	
  		/**
		 * request a tracker announce 
		 * @since 2.3.0.7
		 */
  	
 	public void
	requestTrackerAnnounce(
		boolean		immediate );
 	
		/**
		 * request a tracker announce 
		 * @since 2.3.0.7
		 */
 	
	public void
	requestTrackerScrape(
		boolean		immediate );
	

	

	
	/**
	 * The torrents with the highest rankings will be seeded first.
	 * 
	 * @return Seeding Rank
	 */
	public int getSeedingRank();

	/**
	 * The torrents with the highest rankings will be seeded first.
	 * 
	 * @param rank New Ranking
	 */
	public void setSeedingRank(int rank);
  
  /**
   * Get the local peerID advertised to the download swarm.
   * @return self peer id
   * 
   * @since 2.1.0.5
   */
  public byte[] getDownloadPeerId();
  
  /**
   * Is advanced AZ messaging enabled for this download.
   * @return true if enabled, false if disabled
   */
  public boolean isMessagingEnabled();
  
  /**
   * Enable or disable advanced AZ messaging for this download.
   * @param enabled true to enabled, false to disabled
   */
  public void setMessagingEnabled( boolean enabled );
  
  
  /**
   * Returns an array of size 2 indicating the appropriate locations for this
   * download's data files and torrent file, based on Azureus's rules regarding
   * default save paths, and move on completion rules.
   * 
   * <p>
   * 
   * This method takes one argument - <i>for_moving</i>. This essentially
   * indicates whether you are getting this information for purposes of just
   * finding where Azureus would store these files by default, or whether you
   * are considering moving the files from its current location.
   * 
   * <p>
   * 
   * If <i>for_moving</i> is <tt>false</tt>, this method will determine locations
   * for the download and the torrent file where Azureus would store them by
   * default (it may return the current paths used by this download). 
   * 
   * <p>
   * 
   * If <i>for_moving</i> is <tt>true</tt>, then this method will consider the
   * download's current location, and whether it is allowed to move it - you
   * may not be allowed to move this download (based on Azureus's current rules)
   * if the download doesn't exist within a default save directory already. If
   * a download is complete, we consider whether we are allowed to move downloads
   * on completion, and whether that includes downloads outside the default save
   * directory.
   * 
   * <p>
   * 
   * In this case, the array may contain <tt>null</tt> indicating that the Azureus
   * doesn't believe that the download should be moved (based on the current rules
   * the user has set out). However, you are not prevented from changing the
   * location of the torrent file or download.
   * 
   * @since 2.5.0.2
   * @param for_moving Indicates whether you want this information for the purposes
   *     of moving the download or not.
   * @author amc1
   * @deprecated Use {@link #calculateDefaultDownloadLocation()} instead.
   * @return An array of type <tt>File</tt> of size 2, first element containing the
   *     calculated location for the download's data files, and the second element
   *     containing the location for the download's torrent file.
   */
  public File[] calculateDefaultPaths(boolean for_moving);

  /**
   * Returns <tt>true</tt> if the download is being saved to one of the default
   * save directories.
   * 
   * @since 2.5.0.2
   * @deprecated Use {@link DefaultSaveLocationManager#isInDefaultSaveDir(Download)} instead.
   * @author amc1
   */
  public boolean isInDefaultSaveDir();
  
  /**
   * @since 3.0.4.3
   * @return
   */
  
  public boolean isRemoved();
  
  /**
   * Returns <tt>true</tt> if Azureus will allow the data files for the torrent
   * to be moved.
   * 
   * @since 3.0.5.1
   */
  public boolean canMoveDataFiles();
  
  /**
   * Returns a {@link SaveLocationChange} object describing the appropriate location
   * for the download (and torrent file) to exist in, based on the download's completion
   * state, the <tt>for-completion</tt> rules in place, and the {@link SaveLocationManager}
   * object in use.
   * 
   * @since 3.0.5.3 
   */
  public SaveLocationChange calculateDefaultDownloadLocation();
  
  /**
   * Apply the changes in the given {@link SaveLocationChange} object - this includes
   * moving torrent and data file data.
   * 
   * @param slc The change to apply.
   * @since 3.1.0.1
   * @throws DownloadException If there is a problem moving the data.
   */
  public void changeLocation(SaveLocationChange slc) throws DownloadException;

  	/**
  	 * get user-defined key/value
  	 * @param key
  	 * @return
  	 * @since 3.0.5.3
  	 */
  
  public Object getUserData( Object key );
  
  	/**
  	 * set user defined value. this is TRANSIENT and not persisted over Azureus stop/start
  	 * @param key
  	 * @param data
  	 */
  public void setUserData( Object key, Object data );
  
  /**
   * Simple method to start the download. Will not raise an error if it
   * didn't work, or if the download is already running.
   * 
   * @since 3.0.5.3
   * @param force <tt>true</tt> to force the download to be started.
   */
  public void startDownload(boolean force);
  
  /**
   * Simple method to stop the download. Will not raise an error if it
   * didn't work, or if the download is already stopped.
   * 
   * @since 3.0.5.3
   */
  public void stopDownload();

  
}
