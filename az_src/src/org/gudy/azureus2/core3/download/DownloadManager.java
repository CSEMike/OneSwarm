/*
 * File    : DownloadManager.java
 * Created : 19-Oct-2003
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

package org.gudy.azureus2.core3.download;

import java.io.File;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;

public interface
DownloadManager
{
    public static final int STATE_START_OF_DAY  = -1;   // should never actually see this one

    public static final int STATE_WAITING       = 0;
    public static final int STATE_INITIALIZING  = 5;
    public static final int STATE_INITIALIZED   = 10;

    public static final int STATE_ALLOCATING = 20;
    public static final int STATE_CHECKING = 30;

    // Ready: Resources allocated

    public static final int STATE_READY = 40;
    public static final int STATE_DOWNLOADING = 50;
    public static final int STATE_FINISHING = 55;
    public static final int STATE_SEEDING = 60;
    public static final int STATE_STOPPING = 65;

    // Stopped: can't be automatically started

    public static final int STATE_STOPPED 	= 70;
    public static final int STATE_CLOSED	= 71;	// download never *has* this state, just used to inform 
    												// when stopping for az closedown

    // Queued: Same as stopped, except can be automatically started

    public static final int STATE_QUEUED = 75;

    public static final int STATE_ERROR = 100;



    public static final int WEALTH_STOPPED          = 1;
    public static final int WEALTH_NO_TRACKER       = 2;
    public static final int WEALTH_NO_REMOTE        = 3;
    public static final int WEALTH_OK               = 4;
    public static final int WEALTH_KO               = 5;
    public static final int WEALTH_ERROR          = 6;

    public void
    initialize();

    public int
    getState();

        /**
         * For stopping this returns the target state after stopping (stopped/queued)
         * @return
         */

    public int
    getSubState();

    public void
    setStateWaiting();

    public void
    setStateQueued();

    public void
    startDownload();

    public boolean canForceRecheck();

    public void forceRecheck();

		/**
		 * @param l
		 *
		 * @since 3.0.0.7
		 */
		void forceRecheck(ForceRecheckListener l);

        /**
         * Reset the file download state to totally undownloaded. Download must be stopped
         * @param file
         */

    public void
    resetFile(
        DiskManagerFileInfo     file );

        /**
         * Recheck a particular file. Download must be stopped
         * @param file
         */

    public void
    recheckFile(
        DiskManagerFileInfo     file );
    	
    	/**
    	 * Use with care - introduced to support speed tests whereby we want to avoid checking the
    	 * virtual torrent used for the test
    	 * @param enabled
    	 */
    
    public void
    setPieceCheckingEnabled(
    	boolean	enabled );

  /**
   * Stop the download manager, and do any file/torrent removals.
   * @param _stateAfterStopping
   * @param remove_torrent remove the .torrent file after stopping
   * @param remove_data remove the data file after stopping
   */

    public void
    stopIt(
        int     stateAfterStopping,
        boolean remove_torrent,
        boolean remove_data );

    public boolean
    pause();

    public boolean
    isPaused();

    public void
    resume();

    public GlobalManager
    getGlobalManager();

    public DiskManager
    getDiskManager();

    public DiskManagerFileInfo[]
    getDiskManagerFileInfo();

    public PEPeerManager
    getPeerManager();

    public DownloadManagerState
    getDownloadState();

    public TOTorrent
    getTorrent();

    public TRTrackerAnnouncer
    getTrackerClient();

    public void
    requestTrackerAnnounce(
        boolean     immediate );

    public void
    requestTrackerScrape(
        boolean     immediate );

    public TRTrackerScraperResponse
    getTrackerScrapeResponse();

    public void
    setTrackerScrapeResponse(
        TRTrackerScraperResponse    response );

    public String
    getDisplayName();

        /**
         * returns a name based on the torrent hash or an empty string if torrent invalid
         * @return
         */

    public String
    getInternalName();

    public long
    getSize();

    /**
     * This includes the full path to the torrent file.
     * @return
     */
    public String
    getTorrentFileName();

    public void
    setTorrentFileName(String string);

    public File
    getAbsoluteSaveLocation();

    public File
    getSaveLocation();

        /**
         * changes the save directory. Only call this if you know what you are doing!!!!
         * @param sPath
         */

    public void
    setTorrentSaveDir(
        String sPath );

    /**
     * changes the save directory. Only call this if you know what you are doing!!!!
     * @param sPath
     */
    public void setTorrentSaveDir(String parent_dir, String dl_name);

    public boolean isForceStart();

    public void setForceStart(boolean forceStart);

    public boolean
    isPersistent();

    /**
     * Retrieves whether the download is complete
     * 
     * @param bIncludingDND true- include files marked as Do Not Download.<BR>
     *                       false- don't include files marked DND.<p>
     *                       If there are DND files and you choose to include
     *                       DND in the calculation, false will always be 
     *                       returned.
     * @return whether download is complete
     */
    public boolean
    isDownloadComplete(boolean bIncludingDND);

    public String
    getTrackerStatus();

    public int
    getTrackerTime();

    public String
    getTorrentComment();

    public String
    getTorrentCreatedBy();

    public long
    getTorrentCreationDate();

    public int
    getNbPieces();

    public String
    getPieceLength();

    public int
    getNbSeeds();

    public int
    getNbPeers();

    /**
     * Checks if all the files the user wants to download from this torrent
     * actually exist on their filesystem.
     * <p>
     * If a file does not exist, the download will be set to error state.
     * 
     * @return Whether all the non-skipped (non-DND) files exist
     */
    public boolean
    filesExist();

    public String
    getErrorDetails();

    public DownloadManagerStats
    getStats();

    public int
    getPosition();

    public void
    setPosition(
        int     newPosition );

  	/**
  	 * Retrieve whether this download is assumed complete.
  	 * <p>
  	 * Assumed complete status is kept while the torrent is in a non-running 
  	 * state, even if it has no data.  
  	 * <p>
  	 * When the torrent starts up, the real complete
  	 * level will be checked, and if the torrent
  	 * actually does have missing data, the download will be thrown
  	 * into error state.
  	 * <p>
  	 * Only a forced-recheck should clear this flag.
  	 * 
  	 * @see {@link #requestAssumedCompleteMode()}
  	 */
    public boolean
    getAssumedComplete();

    /**
     * Will set this download to be "assumed complete" for if the download
     * is already complete (excluding DND)
     * 
     * @return true- success; false- failure, download not complete
     */
    public boolean
    requestAssumedCompleteMode();

    /**
     * @return the wealthy status of this download
     */
    public int getHealthStatus();

    /**
     * See plugin ConnectionManager.NAT_ constants for return values
     * @return
     */

     public int
     getNATStatus();

        /**
         * persist resume data
         *
         */

    public void
    saveResumeData();

        /**
         * persist any general download related information, excluding resume data which is
         * managed separately by saveResumeData
         */

    public void
    saveDownload();

      /** To retreive arbitrary objects against this object. */
    public Object getData (String key);
      /** To store arbitrary objects against this object. */
    public void setData (String key, Object value);


      /**
       * Determine whether disk allocation has already been done.
       * Used for checking if data is missing on a previously-loaded torrent.
       * @return true if data files have already been allocated
       */
    public boolean isDataAlreadyAllocated();

      /**
       * Set whether data allocation has already been done, so we know
       * when to allocate and when to throw a missing-data error message.
       * @param already_allocated
       */

    public void setDataAlreadyAllocated( boolean already_allocated );


    public void setSeedingRank(int rank);

    public int getSeedingRank();

    public void setMaxUploads( int max_slots );
    
    public int getMaxUploads();
    
	/**
	 * Returns the max uploads depending on whether the download is seeding and it has a separate
	 * rate for this
	 * @return
	 */

	public int
	getEffectiveMaxUploads();

        /**
         * returns the currently in force upload speed limit which may vary from the stats. value
         * as this gives the fixed per-torrent limit
         * @return
         */

    public int
    getEffectiveUploadRateLimitBytesPerSecond();

    public void
    setCryptoLevel(
    	int	level );
    
    public int
    getCryptoLevel();
    
        /**
         * Move data files to new location. Torrent must be in stopped/error state
         * @param new_parent_dir
         * @return
         */

    public void
    moveDataFiles(
        File    new_parent_dir )

        throws DownloadManagerException;
    
    /**
     * Rename the download - this means the name of the file being downloaded (for single
     * file torrents), or the name of the directory holding the files (in a multi-file torrent).
     * 
     * This does not alter the displayed name of the download.
     * 
     * @param new_name
     * @throws DownloadManagerException
     */
    public void renameDownload(String new_name) throws DownloadManagerException;
    
    /**
     * Move the files and rename a download in one go.
     * 
     * For convenience - either argument can be null, but not both.
     * 
     * @see #moveDataFiles(File)
     * @see #renameDownload(String)
     */
    public void moveDataFiles(File new_parent_dir, String new_name) throws DownloadManagerException;

        /**
         * Move torrent file to new location. Download must be stopped/error
         * @param new_parent_dir
         * @return
         */

    public void
    moveTorrentFile(
        File    new_parent_dir )

        throws DownloadManagerException;

    /**
     * Returns an array of size 2 indicating the appropriate locations for this
     * download's data files and torrent file, based on Azureus's rules regarding
     * default save paths, and move on completion rules.
     * 
     * This method takes one argument - <i>for_moving</i>. This essentially
     * indicates whether you are getting this information for purposes of just
     * finding where Azureus would store these files by default, or whether you
     * are considering moving the files from its current location.
     * 
     * If <i>for_moving</i> is <tt>false</tt>, this method will determine locations
     * for the download and the torrent file where Azureus would store them by
     * default (it may return the current paths used by this download). 
     * 
     * If <i>for_moving</i> is <tt>true</tt>, then this method will consider the
     * download's current location, and whether it is allowed to move it - you
     * may not be allowed to move this download (based on Azureus's current rules)
     * if the download doesn't exist within a default save directory already. If
     * a download is complete, we consider whether we are allowed to move downloads
     * on completion, and whether that includes downloads outside the default save
     * directory.
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
     */
    public boolean isInDefaultSaveDir();
    
        /**
         * gives the time this download was created (not the torrent but the download itself)
         * @return
         */

    public long
    getCreationTime();

    public void
    setCreationTime(
            long        t );

    public void
    setAnnounceResult(
        DownloadAnnounceResult  result );

    public void
    setScrapeResult(
        DownloadScrapeResult    result );

  /**
   * Is extended messaging enabled for this download (meaning AzMP and LTEP support).
   * @return true if enabled, false if disabled
   */
    public boolean isExtendedMessagingEnabled();

  /**
   * Enable or disable extended messaging messaging for this download.
   * @param enable true to enabled, false to disabled
   * 
   * @deprecated Want to get rid of this if possible.
   */
    public void setAZMessagingEnabled( boolean enable );

        /**
         * Indicates that the download manager is no longer needed
         * @param is_duplicate indicates whether this dm is being destroyed because it is a duplicate 
         */

    public void
    destroy(
    	boolean	is_duplicate );

    public boolean
    isDestroyed();
    
    public PEPiece[]
    getCurrentPieces();

    public PEPeer[]
    getCurrentPeers();

    	/**
    	 * Gives the download an opportunity to schedule seeding mode piece rechecks if desired
    	 * @return true if a piece has been rechecked
    	 */
    
	public boolean
	seedPieceRecheck();
	
	public void
	addRateLimiter(
		LimitedRateGroup	group,
		boolean				upload );
	
	public void
	removeRateLimiter(
		LimitedRateGroup	group,
		boolean				upload );
	
    public void
    addListener(
            DownloadManagerListener listener );

    public void
    removeListener(
            DownloadManagerListener listener );

    	// tracker listeners
    
    public void
    addTrackerListener(
        DownloadManagerTrackerListener  listener );

    public void
    removeTrackerListener(
        DownloadManagerTrackerListener  listener );

    	// peer listeners
    
    public void
    addPeerListener(
        DownloadManagerPeerListener listener );

    public void
    addPeerListener(
        DownloadManagerPeerListener listener,
        boolean bDispatchForExisting );

    public void
    removePeerListener(
        DownloadManagerPeerListener listener );

		// piece listeners

    public void
    addPieceListener(
    		DownloadManagerPieceListener listener );

    public void
    addPieceListener(
        DownloadManagerPieceListener listener,
        boolean bDispatchForExisting );

    public void
    removePieceListener(
    		DownloadManagerPieceListener listener );

    	// disk listeners
    
    public void
    addDiskListener(
        DownloadManagerDiskListener listener );

    public void
    removeDiskListener(
        DownloadManagerDiskListener listener );

    public int
    getActivationCount();
    
    public void
    addActivationListener(
    	DownloadManagerActivationListener listener );

    public void
    removeActivationListener(
    	DownloadManagerActivationListener listener );

    
    public void
    generateEvidence(
        IndentWriter        writer );
    
    public int[] getStorageType(DiskManagerFileInfo[] infos);

}