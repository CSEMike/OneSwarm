/*
 * Created on 15-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.download;

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azureus.core.util.CaseSensitiveFileMap;

/**
 * @author parg
 */

public interface 
DownloadManagerState 
{
	public static final String AT_VERSION					= "version";
	public static final String AT_CATEGORY					= "category";
	public static final String AT_NETWORKS					= "networks";
	public static final String AT_USER						= "user";
	public static final String AT_PEER_SOURCES				= "peersources";
	public static final String AT_PEER_SOURCES_DENIED		= "peersourcesdenied";
	public static final String AT_TRACKER_CLIENT_EXTENSIONS	= "trackerclientextensions";
	public static final String AT_FILE_LINKS				= "filelinks";
	public static final String AT_FILE_STORE_TYPES			= "storetypes";
	public static final String AT_FILE_DOWNLOADED			= "filedownloaded";
	public static final String AT_FLAGS						= "flags";
	public static final String AT_PARAMETERS				= "parameters";
	public static final String AT_DISPLAY_NAME              = "displayname";
	public static final String AT_USER_COMMENT              = "comment";
	public static final String AT_RELATIVE_SAVE_PATH        = "relativepath";
	public static final String AT_SECRETS				 	= "secrets";
	public static final String AT_RESUME_STATE		 		= "resumecomplete";
	public static final String AT_PRIMARY_FILE		 		= "primaryfile";
	public static final String AT_TIME_SINCE_DOWNLOAD		= "timesincedl";
	public static final String AT_TIME_SINCE_UPLOAD			= "timesinceul";
	public static final String AT_AVAIL_BAD_TIME			= "badavail";
	public static final String AT_TIME_STOPPED				= "timestopped";
	
	public static Object[][] ATTRIBUTE_DEFAULTS = {
		{ AT_VERSION,								new Integer( -1 )},
		{ AT_TIME_SINCE_DOWNLOAD,					new Integer( -1 )},
		{ AT_TIME_SINCE_UPLOAD,						new Integer( -1 )},
		{ AT_AVAIL_BAD_TIME,						new Long( -1 )},
	};
	
	public static final long FLAG_ONLY_EVER_SEEDED						= Download.FLAG_ONLY_EVER_SEEDED;
	public static final long FLAG_SCAN_INCOMPLETE_PIECES				= Download.FLAG_SCAN_INCOMPLETE_PIECES;
	public static final long FLAG_DISABLE_AUTO_FILE_MOVE    			= Download.FLAG_DISABLE_AUTO_FILE_MOVE;
	public static final long FLAG_MOVE_ON_COMPLETION_DONE   			= Download.FLAG_MOVE_ON_COMPLETION_DONE;
	public static final long FLAG_LOW_NOISE								= Download.FLAG_LOW_NOISE;
	public static final long FLAG_ALLOW_PERMITTED_PEER_SOURCE_CHANGES	= Download.FLAG_ALLOW_PERMITTED_PEER_SOURCE_CHANGES;
	
	public static final String	PARAM_MAX_PEERS							= "max.peers";
	public static final String	PARAM_MAX_PEERS_WHEN_SEEDING			= "max.peers.when.seeding";
	public static final String	PARAM_MAX_PEERS_WHEN_SEEDING_ENABLED	= "max.peers.when.seeding.enabled";
	public static final String	PARAM_MAX_SEEDS							= "max.seeds";
	public static final String	PARAM_MAX_UPLOADS						= "max.uploads";
	public static final String	PARAM_MAX_UPLOADS_WHEN_SEEDING			= "max.uploads.when.seeding";
	public static final String	PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED	= "max.uploads.when.seeding.enabled";
	public static final String	PARAM_STATS_COUNTED						= "stats.counted";
	public static final String	PARAM_DOWNLOAD_ADDED_TIME				= "stats.download.added.time";
	public static final String	PARAM_DOWNLOAD_COMPLETED_TIME			= "stats.download.completed.time";
	public static final String	PARAM_MAX_UPLOAD_WHEN_BUSY				= "max.upload.when.busy";
	public static final String  PARAM_DND_FLAGS							= "dndflags";
	public static final String  PARAM_RANDOM_SEED						= "rand";
	
	public static final int DEFAULT_MAX_UPLOADS	= 4;
	public static final int MIN_MAX_UPLOADS		= 2;
	
	public static Object[][] PARAMETERS = {
		{ PARAM_MAX_PEERS,							new Integer( 0 ) },
		{ PARAM_MAX_PEERS_WHEN_SEEDING,				new Integer( 0 ) },
		{ PARAM_MAX_PEERS_WHEN_SEEDING_ENABLED,		new Boolean( false ) },
		{ PARAM_MAX_SEEDS,							new Integer( 0 ) },
		{ PARAM_MAX_UPLOADS,						new Long( DEFAULT_MAX_UPLOADS ) },
		{ PARAM_MAX_UPLOADS_WHEN_SEEDING, 			new Integer( DEFAULT_MAX_UPLOADS ) },
		{ PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED, 	new Boolean( false ) },
		{ PARAM_STATS_COUNTED, 						new Boolean( false ) },
		{ PARAM_DOWNLOAD_ADDED_TIME,				new Long( 0 ) },
		{ PARAM_DOWNLOAD_COMPLETED_TIME, 			new Long( 0 ) },
		{ PARAM_MAX_UPLOAD_WHEN_BUSY,				new Long( 0 ) },
		{ PARAM_DND_FLAGS, 							new Long( 0 ) },
		{ PARAM_RANDOM_SEED, 						new Long( 0 ) },
	};
	
	public TOTorrent
	getTorrent();
	
	public DownloadManager
	getDownloadManager();
	
	public File 
	getStateFile(
		String	name );
	
	public void
	setFlag(
		long		flag,
		boolean		set );
	
	public boolean
	getFlag(
		long		flag );
	
		/**
		 * Reset to default value
		 * @param name
		 */
	
	public void
	setParameterDefault(
		String	name );
	
	public int
	getIntParameter(
		String	name );
	
	public void
	setIntParameter(
		String	name,
		int		value );
	
	public long
	getLongParameter(
		String	name );
	
	public void
	setLongParameter(
		String	name,
		long	value );
	
	public boolean
	getBooleanParameter(
		String	name );
	
	public void
	setBooleanParameter(
		String		name,
		boolean		value );
	
	public void
	clearResumeData();
	
	public Map
	getResumeData();
	
	public void
	setResumeData(
		Map	data );
	
	public boolean
	isResumeDataComplete();
	
	public void
	clearTrackerResponseCache();
	
	public Map
	getTrackerResponseCache();
	
	public void
	setTrackerResponseCache(
		Map		value );
	
	public Category 
	getCategory();
	
	public void 
	setCategory(
		Category cat );
	
	public String getDisplayName();
	public void setDisplayName(String name);
	
	public String getUserComment();
	public void setUserComment(String name);
	
	public String getRelativeSavePath();
	public void setRelativeSavePath(String path);

	public void setPrimaryFile(String fileFullPath);
	public String getPrimaryFile();

	public String
	getTrackerClientExtensions();
	
	public void
	setTrackerClientExtensions(
		String		value );
	
	public String[]		// from AENetworkClassifier constants
	getNetworks();
	
	public boolean 
	isNetworkEnabled(
	    String		network); //from AENetworkClassifier constants
	
	public void
	setNetworks(
		String[]	networks );	// from AENetworkClassifier constants
	
	public void
	setNetworkEnabled(
	    String		network,				// from AENetworkClassifier constants
	    boolean		enabled);
	
	public String[]		// from PEPeerSource constants
	getPeerSources();
	
	public boolean
	isPeerSourcePermitted(
		String		peerSource );
	
	public void
	setPeerSourcePermitted(
		String		peerSource,
		boolean		permitted );
	
	public boolean
	isPeerSourceEnabled(
	    String		peerSource); // from PEPeerSource constants
	
	public void
	setPeerSources(
		String[]	sources );	// from PEPeerSource constants

	public void
	setPeerSourceEnabled(
	    String		source,		// from PEPeerSource constants
	    boolean		enabled);
	
		// file links
	
	public void
	setFileLink(
		File	link_source,
		File	link_destination );

	public void
	clearFileLinks();
	
	public File
	getFileLink(
		File	link_source );
	
		/**
		 * returns a File -> File map of the defined links (empty if no links)
		 * @return
		 */
	
	public CaseSensitiveFileMap
	getFileLinks();
	
	/**
	 * @return
	 */
	boolean isOurContent();
	
	// General access - make sure you use an AT_ value defined above when calling
	// these methods.
	public void setAttribute(String	name, String value);
	public String getAttribute(String name);
	public void	setMapAttribute(String name, Map value);
	public Map getMapAttribute(String name);
	public void	setListAttribute(String	name, String[] values);
	public String[]	getListAttribute(String	name);
	public void setIntAttribute(String name, int value);
	public int getIntAttribute(String name);
	public void setLongAttribute(String name, long value);
	public long getLongAttribute(String name);
	public void setBooleanAttribute(String name, boolean value);
	public boolean getBooleanAttribute(String name);	
	public boolean hasAttribute(String name);
	
	public void
	setActive(
		boolean	active );
	
	public void discardFluff();
	
	public void
	save();
	
		/**
		 * deletes the saved state
		 */
	
	public void
	delete();
	
	public void
	addListener(
		DownloadManagerStateListener	l );
	
	public void
	removeListener(
		DownloadManagerStateListener	l );

	/**
	 * @param name
	 * @return
	 */
	boolean parameterExists(String name);
	
	public void generateEvidence(IndentWriter writer);
	
	public void supressStateSave(boolean supress);
}
