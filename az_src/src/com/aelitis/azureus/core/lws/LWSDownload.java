/*
 * Created on Jul 16, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.lws;

import java.io.File;
import java.net.URL;
import java.util.*;


import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadActivationEvent;
import org.gudy.azureus2.plugins.download.DownloadActivationListener;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.download.DownloadCompletionListener;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyListener;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadAnnounceResultImpl;


public class 
LWSDownload
	implements Download
{
	private LightWeightSeed				lws;
	private TRTrackerAnnouncer			announcer;
	
	private DownloadAnnounceResultImpl	announce_result;
	
	private Map	user_data			= new HashMap();
	private Map	torrent_attributes 	= new HashMap();
	
	private DownloadScrapeResult	scrape_result = 
		new DownloadScrapeResult()
		{
			public Download
			getDownload()
			{
				return( LWSDownload.this );
			}

			public int
			getResponseType()
			{
				return( announce_result.getResponseType() == DownloadAnnounceResult.RT_SUCCESS?RT_SUCCESS:RT_ERROR );
			}

			public int
			getSeedCount()
			{
				return( announce_result.getSeedCount());
			}

			public int
			getNonSeedCount()
			{
				int	seeds 		= getSeedCount();
				int	reported	= announce_result.getReportedPeerCount();
				
				int	min_peers = reported - seeds;
				
				int	peers = announce_result.getNonSeedCount();
				
				if ( peers < min_peers ){
					
					peers = min_peers;
				}
				
				return( peers );
			}

			public long
			getScrapeStartTime()
			{
				return( 0 );
			}

			public void 
			setNextScrapeStartTime(
				long 	nextScrapeStartTime )
			{
			}

			public long
			getNextScrapeStartTime()
			{
				return( 0 );
			}

			public String
			getStatus()
			{
				if ( getResponseType() == RT_SUCCESS ){
					
					return( "OK" );
					
				}else{
					
					return( announce_result.getError());
				}
			}

			public URL
			getURL()
			{
				return( announce_result.getURL());
			}
		};
		

	protected 
	LWSDownload(
		LightWeightSeed			_lws,
		TRTrackerAnnouncer		_announcer )
	{			
		lws				= _lws;
		announcer		= _announcer;
		
		announce_result = new DownloadAnnounceResultImpl( this, announcer.getLastResponse());
	}
	
	public int
	getState()
	{
		return( Download.ST_SEEDING );
	}

	public int
	getSubState()
	{
		return( Download.ST_SEEDING );
	}
	
	public String
	getErrorStateDetails()
	{
		return( "" );
	}
	
	public void
	setFlag(
		long		flag,
		boolean		value )
	{
		notSupported();
	}
	
	public boolean
	getFlag(
		long		flag )
	{
		return( false );
	}
	
	public long 
	getFlags() 
	{
		return 0;
	}
	
	public int
	getIndex()
	{
		return( 0 );
	}
	
	public File[] 
	calculateDefaultPaths(
		boolean for_moving) 
	{
		return new File[2];
	}
	
	public SaveLocationChange 
	calculateDefaultDownloadLocation() 
	{
		return null;
	}
	
	public boolean 
	isInDefaultSaveDir() 
	{
		return false;
	}
	
	public Torrent
	getTorrent()
	{
		return( lws.getTorrent());
	}

	public void
	initialize()
	
		throws DownloadException
	{	
	}

	public void
	start()
	
		throws DownloadException
	{	
	}

	public void 
	startDownload(
		boolean force) 
	{
	}
	
	public void 
	stopDownload() 
	{
	}
	
	public void
	stop()
	
		throws DownloadException
	{	
	}

	public void
	stopAndQueue()
	
		throws DownloadException
	{	
	}

	public void
	restart()
	
		throws DownloadException
	{	
	}
	
	public void
	pause()
	{
	}
	
	public void
	resume()
	{
	}
	
	public void
	recheckData()
	
		throws DownloadException
	{	
	}

	public boolean
	isStartStopLocked()
	{
		return( false );
	}
	

	public boolean
	isForceStart()
	{
		return( true );
	}
	
	public void
	setForceStart(
		boolean forceStart )
	{
	}	

	public int
	getPriority()
	{
		return( 0 );
	}

	public void
	setPriority(
		int		priority )
	{
	}

	public boolean
	isPriorityLocked()
	{
		return( false );
	}

	public boolean
	isPaused()
	{
		return( false );
	}

	public String 
	getName()
	{
		return( lws.getName());
	}

	public String 
	getTorrentFileName()
	{
		return( getName());
	}
 
	public String
	getAttribute(
		TorrentAttribute		attribute )
	{
		synchronized( torrent_attributes ){
			
			return((String)torrent_attributes.get( attribute ));
		}
	}
	
	public void
	setAttribute(
		TorrentAttribute		attribute,
		String					value )
	{
		synchronized( torrent_attributes ){
			
			torrent_attributes.put( attribute, value );
		}
	}
	
	public String[]
	getListAttribute(
		TorrentAttribute		attribute )
	{
		TorrentManager tm = PluginInitializer.getDefaultInterface().getTorrentManager();

		if ( attribute == tm.getAttribute( TorrentAttribute.TA_NETWORKS )){
			
			return( new String[]{ "Public" });
			
		}else if ( attribute == tm.getAttribute( TorrentAttribute.TA_PEER_SOURCES )){
			
			return( new String[]{ "DHT" });
		}
		
		return( null );
	}
	
	public void 
	setListAttribute(
		TorrentAttribute 	attribute, 
		String[] 			value) 
	{
		notSupported();
	}
	
	public void
	setMapAttribute(
		TorrentAttribute		attribute,
		Map						value )
	{
		notSupported();
	}
	
	public Map
	getMapAttribute(
		TorrentAttribute		attribute )
	{
		return( null );
	}
	  
	public void setIntAttribute(TorrentAttribute name, int value){notSupported();}
	public int getIntAttribute(TorrentAttribute name){ return( 0 ); }
	public void setLongAttribute(TorrentAttribute name, long value){notSupported();}
	public long getLongAttribute(TorrentAttribute name){ return( 0 ); }
	public void setBooleanAttribute(TorrentAttribute name, boolean value){notSupported();}
	public boolean getBooleanAttribute(TorrentAttribute name){ return( false ); }
	public boolean hasAttribute(TorrentAttribute name){ return( false );}

	public void 
	addAttributeListener(
		DownloadAttributeListener l,
		TorrentAttribute attr, 
		int event_type) 
	{
	}
	
	public void 
	removeAttributeListener(
		DownloadAttributeListener l,
		TorrentAttribute attr, 
		int event_type) 
	{
	}
	
	public String 
	getCategoryName()
	{
		return( null );
	}
	  
	public void 
	setCategory(
		String sName)
	{
		notSupported();
	}

	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		throw( new DownloadRemovalVetoException( "no way" ));
	}
	
	public void
	remove(
		boolean	delete_torrent,
		boolean	delete_data )
	
		throws DownloadException, DownloadRemovalVetoException
	{
		throw( new DownloadRemovalVetoException( "no way" ));
	}

	public boolean 
	isRemoved()
	{
		return false;
	}
	
	public int
	getPosition()
	{
		return( 0 );
	}
	
	public long
	getCreationTime()
	{
		return( 0 );
	}

	public void
	setPosition(
		int newPosition)
	{
		notSupported();
	}

	public void
	moveUp()
	{
		notSupported();
	}

	public void
	moveDown()
	{
		notSupported();
	}
	
	public void
	moveTo(
		int		position )
	{
		notSupported();
	}

	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException
	{
		throw( new DownloadRemovalVetoException( "no way" ));
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		announcer.setAnnounceResult( result );
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
	}
	
	public DownloadAnnounceResult
	getLastAnnounceResult()
	{
		announce_result.setContent(  announcer.getLastResponse());
		
		return( announce_result );
	}
	
	public DownloadScrapeResult
	getLastScrapeResult()
	{
		announce_result.setContent(  announcer.getLastResponse());
		
		return( scrape_result );
	}

	public DownloadActivationEvent
	getActivationState()
	{
		return( null );
	}

	public DownloadStats
	getStats()
	{
		return( null );
	}

    public boolean
    isPersistent()
    {
    	return( false );
    }

  	public void
	setMaximumDownloadKBPerSecond(
		int		kb )
  	{
  		notSupported();
  	}
  	
  	public int
	getMaximumDownloadKBPerSecond()
  	{
  		return( 0 );
  	}

    public int 
    getUploadRateLimitBytesPerSecond()
    {
    	return( 0 );
    }
    
    public void 
    setUploadRateLimitBytesPerSecond( 
    	int max_rate_bps )
    {
    	notSupported();
    }
    
    public int 
    getDownloadRateLimitBytesPerSecond() 
    {
    	return 0;
    }

    public void 
    setDownloadRateLimitBytesPerSecond( 
    	int max_rate_bps ) 
    {
    	notSupported();
    }
    
	public boolean 
	isComplete()
	{
		return( true );
	}

	public boolean 
	isComplete(
		boolean bIncludeDND)
	{
		return( true );
	}

	public boolean
 	isChecking()
	{
		return( false );
	}
	
  	public String
	getSavePath()
  	{
  		return( "" );
  	}

  	public void
  	moveDataFiles(
  		File	new_parent_dir )
  	
  		throws DownloadException
  	{
  		notSupported();
  	}

  	public boolean 
  	canMoveDataFiles() 
  	{
  		return false;
  	}
  	
  	public void
  	moveTorrentFile(
  		File	new_parent_dir ) 
  	
  		throws DownloadException
	{
  		notSupported();
  	}

  	public void 
  	renameDownload(
  		String name )
  	
  		throws DownloadException
  	{	
  		notSupported();
  	}

  	public org.gudy.azureus2.plugins.peers.PeerManager
	getPeerManager()
  	{
  		return( null );
  	}
	
	public org.gudy.azureus2.plugins.disk.DiskManager
	getDiskManager()
	{
		return( null );
	}
	
	public DiskManagerFileInfo[]
	getDiskManagerFileInfo()
	{
		return( null );
	}
	
	public DiskManagerFileInfo
	getDiskManagerFileInfo(int i)
	{
		return( null );
	}

	public int getDiskManagerFileCount()
	{
		return 0;
	}


  	public void
	requestTrackerAnnounce()
  	{
  	}

 	public void
	requestTrackerAnnounce(
		boolean		immediate )
 	{
 	}

	public void
	requestTrackerScrape(
		boolean		immediate )
	{
	}

	public void
	addListener(
		DownloadListener	l )
	{
	}

	public void
	removeListener(
		DownloadListener	l )
	{
	}

	public void 
	addCompletionListener(
		DownloadCompletionListener l ) 
	{
		notSupported();
	}
	
	public void 
	removeCompletionListener(
		DownloadCompletionListener l ) 
	{
		notSupported();
	}
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l )
	{
	}

	public void 
	addTrackerListener(
		DownloadTrackerListener l, 
		boolean immediateTrigger)
	{
	}
  
	public void
	removeTrackerListener(
		DownloadTrackerListener	l )
	{
	}

	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		notSupported();
	}

	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		notSupported();
	}

	public void
	addActivationListener(
		DownloadActivationListener		l )
	{
		notSupported();
	}

	public void
	removeActivationListener(
		DownloadActivationListener		l )
	{
		notSupported();
	}

	public void
	addPeerListener(
		DownloadPeerListener	l )
	{
		notSupported();
	}

	public void
	removePeerListener(
		DownloadPeerListener	l )
	{
		notSupported();
	}


	public int 
	getSeedingRank()
	{
		return( 0 );
	}
	
	public void 
	setSeedingRank(
		int rank)
	{
		notSupported();
	}
  
	public void
	addPropertyListener(
		DownloadPropertyListener	l )
	{
		notSupported();
	}
	
	public void
	removePropertyListener(
		DownloadPropertyListener	l )
	{
		notSupported();
	}

	public byte[] 
	getDownloadPeerId()
	{
		return( null );
	}

	public boolean 
	isMessagingEnabled()
	{
		return( true );
	}

	public void 
	setMessagingEnabled( 
		boolean enabled )
	{
	}	  
			
	public void 
	moveDataFiles(
		File 	new_parent_dir, 
		String 	new_name )
	
		throws DownloadException 
	{
		notSupported();
	}
	
	public Object 
	getUserData(
		Object key ) 
	{
		synchronized( user_data ){
		
			return( user_data.get( key ));
		}
	}
	
	public void 
	setUserData(
		Object key, 
		Object data ) 
	{
		synchronized( user_data ){
		
			user_data.put( key, data );
		}
	}
	
	public void 
	changeLocation(
		SaveLocationChange slc )
			
		throws DownloadException 
	{
		notSupported();
	}
	
	protected void
	notSupported()
	{
		Debug.out( "Not Supported" );
	}
}