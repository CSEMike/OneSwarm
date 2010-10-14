/*
 * Created on Apr 16, 2004
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

package org.gudy.azureus2.platform.win32.access.impl;

/**
 * @author parg
 *
 */

import java.net.InetAddress;

import org.gudy.azureus2.platform.PlatformManagerPingCallback;

public class 
Test 
{
	public static void
	main(
		String[]	args )
	{
		try{
			/*
			AEWin32Access access = AEWin32Manager.getAccessor();
			
			String	app_data = access.getUserAppData();
			
			System.out.println( "AppData = " + app_data );

			String az_home = access.getApplicationInstallDir("azureus");
			
			File	az_exe = new File( az_home + File.separator + "Azureus.exe" ).getAbsoluteFile();
			
			if ( az_exe.exists()){
				
				//<Nolar> 	WriteRegStr HKCR ".torrent" "" "BitTorrent"
				//<Nolar> 	WriteRegStr HKCR "BitTorrent" "" "Bittorrent File"
				//<Nolar> 	WriteRegStr HKCR "BitTorrent\shell" "" "open"
				//<Nolar> 	WriteRegStr HKCR "BitTorrent\DefaultIcon" "" $INSTDIR\Azureus.exe,1
				//<Nolar>   	WriteRegStr HKCR "BitTorrent\shell\open\command" "" '"$INSTDIR\Azureus.exe" "%1"'
				//<Nolar>   	WriteRegStr HKCR "BitTorrent\Content Type" "" "application/x-bittorrent"
				
				System.out.println( "current = " + 
						access.readStringValue( 	
						AEWin32Access.HKEY_CLASSES_ROOT,
						"BitTorrent\\shell\\open\\command",
						"" ));
				
				access.deleteKey( 	AEWin32Access.HKEY_CLASSES_ROOT,
									".torrent" );
			
				access.deleteKey( 	AEWin32Access.HKEY_CLASSES_ROOT,
									"BitTorrent",
									true );
			
				access.writeStringValue( 	AEWin32Access.HKEY_CLASSES_ROOT,
											".torrent",
											"",
											"BitTorrent" );
				
				access.writeStringValue( 	
						AEWin32Access.HKEY_CLASSES_ROOT,
						"BitTorrent",
						"",
						"Bittorrent File" );

				access.writeStringValue( 	
						AEWin32Access.HKEY_CLASSES_ROOT,
						"BitTorrent\\shell",
						"",
						"open" );
				
				access.writeStringValue( 	
						AEWin32Access.HKEY_CLASSES_ROOT,
						"BitTorrent\\DefaultIcon",
						"",
						az_exe.toString() + ",1" );
				
				access.writeStringValue( 	
						AEWin32Access.HKEY_CLASSES_ROOT,
						"BitTorrent\\shell\\open\\command",
						"",
						"\"" + az_exe.toString() + "\" \"%1\"" );
				
				access.writeStringValue( 	
						AEWin32Access.HKEY_CLASSES_ROOT,
						"BitTorrent\\Content Type" ,
						"",
						"application/x-bittorrent" );
				
			}else{
				
				System.out.println( "can't find Azureus.exe");
			}
		*/
			
			//AEWin32AccessInterface.copyPermission( "C:\\temp\\fred", "C:\\temp\\bill" );
			
			
			AEWin32AccessImpl.getSingleton( false ).traceRoute(
					InetAddress.getByName( "192.168.1.143" ),
					InetAddress.getByName( "www.google.com"),
					new PlatformManagerPingCallback()
					{
						public boolean
						reportNode(
							int				distance,
							InetAddress		address,
							int				millis )
						{
							System.out.println( distance + ": " + address + " - " + millis );
							
							return( true );
						}
					});
			
			// AEWin32AccessInterface.ping( "www.google.com" );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
