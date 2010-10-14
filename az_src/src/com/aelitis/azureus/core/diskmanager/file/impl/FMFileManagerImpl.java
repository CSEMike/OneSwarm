/*
 * File    : FMFileManagerImpl.java
 * Created : 12-Feb-2004
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

package com.aelitis.azureus.core.diskmanager.file.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.File;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.diskmanager.file.*;
import com.aelitis.azureus.core.util.CaseSensitiveFileMap;

public class 
FMFileManagerImpl
	implements FMFileManager
{
	public static final boolean DEBUG	= false;
	
	protected static FMFileManagerImpl	singleton;
	protected static AEMonitor			class_mon	= new AEMonitor( "FMFileManager:class" );
	
	
	public static FMFileManager
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new FMFileManagerImpl();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected LinkedHashMap		map;
	protected AEMonitor			map_mon	= new AEMonitor( "FMFileManager:Map");

	protected HashMap			links		= new HashMap();
	protected AEMonitor			links_mon	= new AEMonitor( "FMFileManager:Links");

	protected boolean			limited;
	protected int				limit_size;
	
	protected AESemaphore		close_queue_sem;
	protected List				close_queue;
	protected AEMonitor			close_queue_mon	= new AEMonitor( "FMFileManager:CQ");
	
	protected List				files;
	protected AEMonitor			files_mon		= new AEMonitor( "FMFileManager:File");
	
	protected 
	FMFileManagerImpl()
	{
		limit_size = COConfigurationManager.getIntParameter( "File Max Open" );
		
		limited		= limit_size > 0;
	
		if ( DEBUG ){
			
			System.out.println( "FMFileManager::init: limit = " + limit_size );
			
			files = new ArrayList();
		}
		
		map	= new LinkedHashMap( limit_size, (float)0.75, true );	// ACCESS order selected - this means oldest

		if ( limited ){
			
			close_queue_sem	= new AESemaphore("FMFileManager::closeqsem");
			
			close_queue		= new LinkedList();
			
			Thread	t = new AEThread("FMFileManager::closeQueueDispatcher")
				{
					public void
					runSupport()
					{
						closeQueueDispatch();
					}
				};
			
			t.setDaemon(true);
			
			t.start();
		}
	}
	
	protected CaseSensitiveFileMap
	getLinksEntry(
		TOTorrent	torrent )
	{
		Object	links_key;
		
		try{
			
			links_key = torrent.getHashWrapper();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			links_key	= "";
		}
		
		CaseSensitiveFileMap	links_entry = (CaseSensitiveFileMap)links.get( links_key );
		
		if ( links_entry == null ){
			
			links_entry	= new CaseSensitiveFileMap();
			
			links.put( links_key, links_entry );
		}
		
		return( links_entry );
	}
	
	public void
	setFileLinks(
		TOTorrent 				torrent,
		CaseSensitiveFileMap	new_links )
	{
		try{
			links_mon.enter();
			
			CaseSensitiveFileMap	links_entry = getLinksEntry( torrent );
			
			Iterator	it = new_links.keySetIterator();
			
			while( it.hasNext()){
				
				File	source 	= (File)it.next();
				File	target	= (File)new_links.get(source);
				
				// System.out.println( "setLink:" + source + " -> " + target );
				
				if ( target != null && !source.equals(target)){
					
					links_entry.put( source, target );
				}else{
					
					links_entry.remove( source );
				}
			}
		}finally{
			
			links_mon.exit();
		}
	}
	
	public File
	getFileLink(
		TOTorrent	torrent,
		File		file )
	{
		try{
			links_mon.enter();
			
			CaseSensitiveFileMap	links_entry = getLinksEntry( torrent );

			File	res = (File)links_entry.get( file );
			
			if ( res == null ){
				
				res = file;
			}
			
			// System.out.println( "getLink:" + file + " -> " + res );
			
			return( res );
			
		}finally{
			
			links_mon.exit();
		}
	}
	
	public FMFile
	createFile(
		FMFileOwner	owner,
		File		file,
		int			type )
	
		throws FMFileManagerException
	{
		FMFile	res;
		
		if ( AEDiagnostics.USE_DUMMY_FILE_DATA ){
			
			res = new FMFileTestImpl( owner, this, file, type );
			
		}else{
		
			if ( limited ){
	
				res = new FMFileLimited( owner, this, file, type );
				
			}else{
				
				res = new FMFileUnlimited( owner, this, file, type );
			}
		}
			
		if (DEBUG){
			
			try{
				files_mon.enter();
			
				files.add( res );
				
			}finally{
				
				files_mon.exit();
			}
		}
		
		return( res );
	}
	
	
	protected void
	getSlot(
		FMFileLimited	file )
	{
			// must close the oldest file outside sync block else we'll get possible deadlock
		
		FMFileLimited	oldest_file = null;
		
		try{
			map_mon.enter();
		
			if (DEBUG ){
				System.out.println( "FMFileManager::getSlot: " + file.getName() +", map_size = " + map.size());
			}
			
			if ( map.size() >= limit_size ){
				
				Iterator it = map.keySet().iterator();
				
				oldest_file = (FMFileLimited)it.next();
				
				it.remove();
			}
			
			map.put( file, file );
			
		}finally{
			
			map_mon.exit();
		}
		
		if ( oldest_file != null ){
			
			closeFile( oldest_file );			

		}
	}
	
	protected void
	releaseSlot(
		FMFileLimited	file )
	{
		if ( DEBUG ){
			System.out.println( "FMFileManager::releaseSlot: " + file.getName());
		}
		
		try{
			map_mon.enter();
			
			map.remove( file );
			
		}finally{
			
			map_mon.exit();
		}
	}
	
	protected void
	usedSlot(
		FMFileLimited	file )
	{	
		if ( DEBUG ){
			System.out.println( "FMFileManager::usedSlot: " + file.getName());
		}
		
		try{
			map_mon.enter();
		
				// only update if already present - might not be due to delay in
				// closing files
			
			if ( map.containsKey( file )){
				
				map.put( file, file );		// update MRU
			}
		}finally{
			
			map_mon.exit();
		}
	}
	
	protected void
	closeFile(
		FMFileLimited	file )
	{
		if ( DEBUG ){
			System.out.println( "FMFileManager::closeFile: " + file.getName());
		}

		try{
			close_queue_mon.enter();
			
			close_queue.add( file );
			
		}finally{
			
			close_queue_mon.exit();
		}
		
		close_queue_sem.release();
	}
	
	protected void
	closeQueueDispatch()
	{
		while(true){
			
			if ( DEBUG ){
				
				close_queue_sem.reserve(1000);
				
			}else{
				
				close_queue_sem.reserve();
			}
			
			FMFileLimited	file = null;
			
			try{
				close_queue_mon.enter();
			
				if ( close_queue.size() > 0 ){
					
					file = (FMFileLimited)close_queue.remove(0);

					if ( DEBUG ){
						
						System.out.println( "FMFileManager::closeQ: " + file.getName() + ", rem = " + close_queue.size());
					}
				}
			}finally{
				
				close_queue_mon.exit();
			}
			
			if ( file != null ){
				
				try{
					file.close(false);
				
				}catch( Throwable e ){
				
					Debug.printStackTrace( e );
				}
			}
			
			if ( DEBUG ){
				
				try{
					files_mon.enter();
					
					int	open = 0;
					
					for (int i=0;i<files.size();i++){

						FMFileLimited	f = (FMFileLimited)files.get(i);
						
						if ( f.isOpen()){
							
							open++;
						}
					}
					
					System.out.println( "INFO: files = " + files.size() + ", open = " + open );
					
				}finally{
					
					files_mon.exit();
				}
			}
		}
	}
	
	protected void
	generate(
		IndentWriter	writer )
	{
		writer.println( "FMFileManager slots" );
		
		try{
			writer.indent();
			
			try{
				map_mon.enter();
							
				Iterator it = map.keySet().iterator();
					
				while( it.hasNext()){
					
					FMFileLimited	file = (FMFileLimited)it.next();
					
					writer.println( file.getString());
				}
							
			}finally{
				
				map_mon.exit();
			}
		}finally{
			
			writer.exdent();
		}
	}
	protected static void
	generateEvidence(
		IndentWriter	writer )
	{
		getSingleton();
		
		singleton.generate( writer );
	}
}
