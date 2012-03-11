/*
 * File    : TRTrackerClientFactoryImpl.java
 * Created : 04-Nov-2003
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

package org.gudy.azureus2.core3.tracker.client.impl;

/**
 * @author parg
 *
 */

import java.util.*;


import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerAnnouncerFactoryImpl 
{
	protected static List<TRTrackerAnnouncerFactoryListener>	listeners 	= new ArrayList<TRTrackerAnnouncerFactoryListener>();
	protected static List<TRTrackerAnnouncerImpl>				clients		= new ArrayList<TRTrackerAnnouncerImpl>();
	
	protected static AEMonitor 		class_mon 	= new AEMonitor( "TRTrackerClientFactory" );

	public static TRTrackerAnnouncer
	create(
		TOTorrent		torrent,
		String[]		networks,
		boolean			manual )
		
		throws TRTrackerAnnouncerException
	{
		TRTrackerAnnouncerImpl	client = new TRTrackerAnnouncerMuxer( torrent, networks, manual );
		
		if ( !manual ){
			
			List	listeners_copy	= new ArrayList();
			
			try{
				class_mon.enter();
				
				clients.add( client );
			
				listeners_copy = new ArrayList( listeners );
		
			}finally{
				
				class_mon.exit();
			}
			
			for (int i=0;i<listeners_copy.size();i++){
				
				try{
					((TRTrackerAnnouncerFactoryListener)listeners_copy.get(i)).clientCreated( client );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		return( client );
	}
	
		/*
		 * At least once semantics for this one
		 */
	
	public static void
	addListener(
		 TRTrackerAnnouncerFactoryListener	l )
	{
		List	clients_copy;
		
		try{
			class_mon.enter();
		
			listeners.add(l);
			
			clients_copy = new ArrayList( clients );
	
		}finally{
			
			class_mon.exit();
		}
		
		for (int i=0;i<clients_copy.size();i++){
			
			try{
				l.clientCreated((TRTrackerAnnouncer)clients_copy.get(i));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public static void
	removeListener(
		 TRTrackerAnnouncerFactoryListener	l )
	{
		try{
			class_mon.enter();
		
			listeners.remove(l);
			
		}finally{
			
			class_mon.exit();
		}
	}

	public static void
	destroy(
		TRTrackerAnnouncer	client )
	{
		if ( !client.isManual()){
			
			List	listeners_copy	= new ArrayList();
			
			try{
				class_mon.enter();
			
				clients.remove( client );
				
				listeners_copy	= new ArrayList( listeners );
	
			}finally{
				
				class_mon.exit();
			}
			
			for (int i=0;i<listeners_copy.size();i++){
				
				try{
					((TRTrackerAnnouncerFactoryListener)listeners_copy.get(i)).clientDestroyed( client );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
}
