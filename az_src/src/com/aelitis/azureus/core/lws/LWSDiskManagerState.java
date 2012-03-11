/*
 * Created on 11-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.lws;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateAttributeListener;
import org.gudy.azureus2.core3.download.DownloadManagerStateListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.IndentWriter;

import com.aelitis.azureus.core.util.CaseSensitiveFileMap;

public class 
LWSDiskManagerState
	implements DownloadManagerState
{
	private long flags = FLAG_LOW_NOISE | FLAG_DISABLE_AUTO_FILE_MOVE;
	
	protected
	LWSDiskManagerState()
	{
	}
	
	public TOTorrent
	getTorrent()
	{
		return( null );
	}
	
	public File
	getStateFile(
		String	name )
	{
		return( null );
	}
	
	public File 
	getStateFile() 
	{
		return null;
	}
	
	public DownloadManager
	getDownloadManager()
	{
		return( null );
	}
	
	public void
	clearResumeData()
	{
	}
	
	public Map
	getResumeData()
	{
		return( new HashMap());
	}
	
	public void
	setResumeData(
		Map	data )
	{
	}
	
	public boolean
	isResumeDataComplete()
	{
		return( true );
	}
	
	public void
	clearTrackerResponseCache()
	{
	}
	
	public Map
	getTrackerResponseCache()
	{
		return( new HashMap());
	}

	public void
	setTrackerResponseCache(
		Map		value )
	{
	}
	
	public void
	setFlag(
		long		flag,
		boolean		set )
	{
		flags |= flag;
	}
	
	public boolean
	getFlag(
		long		flag )
	{
		return(( flags & flag ) != 0 );
	}
	
	public long 
	getFlags() 
	{
		return( flags );
	}
	
	public boolean 
	isOurContent() 
	{
		return false;
	}
	
	public int
	getIntParameter(
		String	name )
	{
		return( 0 );
	}
	
	public void
	setIntParameter(
		String	name,
		int	value )
	{
	}
	
	public long
	getLongParameter(
		String	name )
	{
		return( 0 );
	}
	
	public void 
	setParameterDefault(
		String name) 
	{
	}
	
	public void
	setLongParameter(
		String	name,
		long	value )
	{
	}
	
	public boolean
	getBooleanParameter(
		String	name )
	{
		return( false );
	}
	
	public void
	setBooleanParameter(
		String		name,
		boolean		value )
	{	
	}
	
	public void
	setAttribute(
		String		name,
		String		value )
	{
	}			
	
	public String
	getAttribute(
		String		name )
	{
		return( null );
	}
	

	public void setIntAttribute(String name, int value){}
	public int getIntAttribute(String name){ return( 0 ); }
	public void setLongAttribute(String name, long value){}
	public long getLongAttribute(String name){ return( 0 ); }
	public void setBooleanAttribute(String name, boolean value){}
	public boolean getBooleanAttribute(String name){ return( false ); }
	public boolean hasAttribute(String name){ return( false );}
	
	public String
	getTrackerClientExtensions()
	{
		return( null );
	}
	
	public void
	setTrackerClientExtensions(
		String		value )
	{
	}
	
	public void
	setListAttribute(
		String		name,
		String[]	values )
	{
	}
	
	public String[]
	getListAttribute(
		String	name )
	{
		return( null );
	}
	
	public String 
	getListAttribute(
		String 		name, 
		int 		idx) 
	{
		return null;
	}
	
	public void
	setMapAttribute(
		String		name,
		Map			value )
	{
	}
	
	public Map
	getMapAttribute(
		String		name )
	{
		return( null );
	}
	
	public Category 
	getCategory()
	{
		return( null );
	}
	
	public void 
	setCategory(
		Category cat )
	{
	}
	
	public void 
	setPrimaryFile(
		DiskManagerFileInfo dmfi)
	{
	}

	public DiskManagerFileInfo 
	getPrimaryFile() 
	{
		return null;
	}
	
	public String[]		
	getNetworks()
	{
		return( new String[0] );
	}
	
	
    public boolean 
    isNetworkEnabled(
    	String network) 
    {	      
    	return false;
    }
					
	public void
	setNetworks(
		String[]		networks )
	{
	}
	

    public void 
    setNetworkEnabled(
        String network,
        boolean enabled) 
    {	      
    }
	
	public String[]		
	getPeerSources()
	{
		return( new String[0] );
	}
	
	public boolean
	isPeerSourcePermitted(
		String	peerSource )
	{
		return( false );
	}
	
	public void 
	setPeerSourcePermitted(
		String peerSource, 
		boolean permitted )
	{
	}
	
    public boolean
    isPeerSourceEnabled(
        String peerSource)
    {
    	return false;
    }
	
	public void
	setPeerSources(
		String[]		networks )
	{
	}
	

    public void
    setPeerSourceEnabled(
        String source,
        boolean enabled ) 
    {
    }
	
    public void
	setFileLink(
		File	link_source,
		File	link_destination )
    {
    }
    
    public void 
    discardFluff() 
    {
    }
    
	public void
	clearFileLinks()
	{
	}
	
	public File
	getFileLink(
		File	link_source )
	{
		return( null );
	}
	
	public CaseSensitiveFileMap
	getFileLinks()
	{
		return( new CaseSensitiveFileMap());
	}
	
	public String 
	getUserComment() 
	{
		return( "" );
	}
	
	public void 
	setUserComment(
		String name ) 
	{
	}
	
	public String 
	getRelativeSavePath()
	{
		return null;
	}
	
	public void 
	setRelativeSavePath(
		String path )
	{
	}
	
	public void
	setActive(
		boolean	a )
	{
	}
	
	public void
	save()
	{	
	}
	
	public void
	delete()
	{
	}
	
	public void 
	suppressStateSave(
		boolean suppress) 
	{
	}
	
	public void
	addListener(
		DownloadManagerStateListener	l )
	{
	}
	
	public void
	removeListener(
		DownloadManagerStateListener	l )
	{
	}
	
	public void 
	addListener(
		DownloadManagerStateAttributeListener 	l,
		String 									attribute, 
		int 									event_type) 
	{
	}
	
	public void 
	removeListener(
		DownloadManagerStateAttributeListener 	l,
		String 									attribute, 
		int 									event_type) 
	{
	}
	
	public void 
	generateEvidence(
		IndentWriter writer) 
	{
	}
	
	public String 
	getDisplayName()
	{
		return null;
	}
	
	public void 
	setDisplayName(
		String name) 
	{	
	}
	
	public boolean 
	parameterExists(
		String name) 
	{
		return false;
	}
	
	public void 
	supressStateSave(
		boolean supress ) 
	{
	}
}
