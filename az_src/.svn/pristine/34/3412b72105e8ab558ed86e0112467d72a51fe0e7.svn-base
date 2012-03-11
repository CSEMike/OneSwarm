/*
 * File    : TorrentAnnounceURLListImpl.java
 * Created : 03-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.local.torrent;

/**
 * @author parg
 *
 */

import java.net.URL;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.torrent.*;

public class 
TorrentAnnounceURLListImpl
	implements TorrentAnnounceURLList
{
	protected TorrentImpl		torrent;
	
	protected
	TorrentAnnounceURLListImpl(
		TorrentImpl	_torrent )
	{
		torrent	= _torrent;
	}
	
	public TorrentAnnounceURLListSet[]
	getSets()
	{
		TOTorrentAnnounceURLGroup	group = torrent.getTorrent().getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
		
		TorrentAnnounceURLListSet[]	res = new TorrentAnnounceURLListSet[sets.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = new TorrentAnnounceURLListSetImpl( this, sets[i]);
		}
		
		return( res );
	}
	
	public void
	setSets(
		TorrentAnnounceURLListSet[]		sets )
	{
		TOTorrentAnnounceURLGroup	group = torrent.getTorrent().getAnnounceURLGroup();
				
		TOTorrentAnnounceURLSet[]	res = new TOTorrentAnnounceURLSet[sets.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = ((TorrentAnnounceURLListSetImpl)sets[i]).getSet();
		}
		
		group.setAnnounceURLSets( res );
		
		updated();
	}
	
	public TorrentAnnounceURLListSet
	create(
		URL[]		urls )
	{
		return( new TorrentAnnounceURLListSetImpl( this, torrent.getTorrent().getAnnounceURLGroup().createAnnounceURLSet(urls)));
	}
	
	public void
	addSet(
		URL[]		urls )
	{
		if ( setAlreadyExists( urls )){
			
			return;
		}
		
		TorrentUtils.announceGroupsInsertLast( torrent.getTorrent(), urls );
		
		updated();
	}
	
	public void
	insertSetAtFront(
		URL[]		urls )
	{
		if ( setAlreadyExists( urls )){
			
			return;
		}
		
		TorrentUtils.announceGroupsInsertFirst( torrent.getTorrent(), urls );
		
		updated();
	}
	
	protected boolean
	setAlreadyExists(
		URL[]		urls )
	{
		TOTorrentAnnounceURLGroup	group = torrent.getTorrent().getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();

		for (int i=0;i<sets.length;i++){
			
			URL[]	u = sets[i].getAnnounceURLs();
			
			if ( u.length != urls.length ){
				
				continue;
			}
			
			boolean	all_found = true;
			
			for (int j=0;j<urls.length;j++){
				
				URL	u1 = urls[j];
				
				boolean	this_found = false;
				
				for ( int k=0;k<u.length;k++){
					
					URL	u2 = u[k];
					
					if ( u1.toString().equals( u2.toString())){
						
						this_found = true;
						
						break;
					}
				}
				
				if ( !this_found ){
					
					all_found = false;
					
					break;
				}
			}
			
			if ( all_found ){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	protected void
	updated()
	{
		torrent.updated();
	}
}
