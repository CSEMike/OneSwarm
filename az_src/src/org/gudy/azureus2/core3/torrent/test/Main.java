/*
 * File    : Main.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.torrent.test;


import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.Debug;

public class 
Main
{
	static int TT_ENCODE		= 1;
	static int TT_DECODE		= 2;
	static int TT_CREATE		= 3;
	
	static void
	usage()
	{
		System.err.println( "Usage: encode|decode|create" );
		
		SESecurityManager.exitVM(1);
	}
	
	public static void
	main(
		String[]	args )
	{
		int	test_type= 0;
		
		if ( args.length != 1 ){
			
			usage();
		}
		
		if ( args[0].equalsIgnoreCase( "encode" )){
			
			test_type = TT_ENCODE;
			
		}else if ( args[0].equalsIgnoreCase( "decode" )){
			
			test_type = TT_DECODE;
			
		}else if ( args[0].equalsIgnoreCase( "create" )){
			
			test_type = TT_CREATE;
						
		}else{
			
			usage();
		}
		
		try{
			if ( test_type == TT_ENCODE ){
				
				/*
				File f = new File("D:\\az2060-broken.torrent");
						
				TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedFile( f );
			
				TOTorrentAnnounceURLSet set = torrent.getAnnounceURLGroup().createAnnounceURLSet(new URL[]{ new URL("http://localhost:6970/announce"), new URL("http://localhost:6969/announce")});
				
				torrent.getAnnounceURLGroup().setAnnounceURLSets( new TOTorrentAnnounceURLSet[]{ set });
				
				torrent.setAdditionalStringProperty( "Wibble", "wobble" );
				
				torrent.print();
			
				torrent.serialiseToBEncodedFile( new File("c:\\temp\\test2.torrent"));
				
				*/
			
			}else if ( test_type == TT_DECODE ){
							 
				File f = new File("c:\\temp\\az3008-broken.torrent" );
			
				TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedFile( f );
			
				// System.out.println( "\turl group sets = " + torrent.getAnnounceURLGroup().getAnnounceURLSets().length);
				
				torrent.print();		
				
			}else if ( test_type == TT_CREATE ){
			
				TOTorrentProgressListener list = new TOTorrentProgressListener()
					{
						public void
						reportProgress(
							int		p )
						{
							System.out.println( "" + p );
						}
						public void
						reportCurrentTask(
							String	task_description )
						{
							System.out.println( "task = " + task_description );
						}				
					};
				
				boolean	do_file 	= false;
				boolean	do_fixed	= false;
				
				TOTorrent t;
				
				if ( do_fixed ){
					
					if ( do_file ){
						
						TOTorrentCreator c = TOTorrentFactory.createFromFileOrDirWithFixedPieceLength( 
								new File("c:\\temp\\test.wmf"), 
								new URL( "http://127.0.0.1:6969/announce" ),
								1024*10 );
						
						c.addListener( list );
						
						t = c.create();
					}else{
		
						TOTorrentCreator c = TOTorrentFactory.createFromFileOrDirWithFixedPieceLength( 
								new File("c:\\temp\\scans"), 
								new URL("http://127.0.0.1:6969/announce" ), 
								1024*256 ); 
								
						
						c.addListener( list );
						
						t = c.create();
					}
				}else{
					if ( do_file ){
						
						TOTorrentCreator c = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( 
								new File("c:\\temp\\test.wmf"), 
								new URL( "http://127.0.0.1:6969/announce" ));
								
						
						
						c.addListener( list );
						
						t = c.create();
						
					}else{
		
						TOTorrentCreator c = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( 
								new File("c:\\temp\\qqq"), 
								new URL("http://127.0.0.1:6969/announce" )); 
								
						
						
						c.addListener( list );
						
						t = c.create();
								
						t.setCreationDate( 12345L );
						
						t.setComment( "poo pee plop mcjock");
					}
				}
				
				t.print();
				
				t.serialiseToBEncodedFile( new File("c:\\temp\\test.torrent" ));						 
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
		}
	}
}