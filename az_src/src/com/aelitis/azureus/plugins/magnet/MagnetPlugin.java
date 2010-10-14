/*
 * Created on 03-Mar-2005
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

package com.aelitis.azureus.plugins.magnet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.InetSocketAddress;
import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.net.magneturi.*;

/**
 * @author parg
 *
 */

public class 
MagnetPlugin
	implements Plugin
{
	private PluginInterface		plugin_interface;
		
	private CopyOnWriteList		listeners = new CopyOnWriteList();
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Magnet URI Handler" );
		
		MenuItemListener	listener = 
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem		_menu,
					Object			_target )
				{
					Download download = (Download)((TableRow)_target).getDataSource();
				  
					if ( download == null || download.getTorrent() == null ){
						
						return;
					}
					
					Torrent torrent = download.getTorrent();
					
					String	cb_data = "magnet:?xt=urn:btih:" + Base32.encode( torrent.getHash());

					// removed this as well - nothing wrong with allowing magnet copy
					// for private torrents - they still can't be tracked if you don't
					// have permission
					
					
					/*if ( torrent.isPrivate()){
						
						cb_data = getMessageText( "private_torrent" );
						
					}else if ( torrent.isDecentralised()){
					*/	
						// ok
						
						/* relaxed this as we allow such torrents to be downloaded via magnet links
						 * (as opposed to tracked in the DHT)
						 
					}else if ( torrent.isDecentralisedBackupEnabled()){
							
						TorrentAttribute ta_peer_sources 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_PEER_SOURCES );

						String[]	sources = download.getListAttribute( ta_peer_sources );
		
						boolean	ok = false;
								
						for (int i=0;i<sources.length;i++){
									
							if ( sources[i].equalsIgnoreCase( "DHT")){
										
								ok	= true;
										
								break;
							}
						}
		
						if ( !ok ){
							
							cb_data = getMessageText( "decentral_disabled" );
						}
					}else{
						
						cb_data = getMessageText( "decentral_backup_disabled" );
						*/
					// }
					
					// System.out.println( "MagnetPlugin: export = " + url );
					
					try{
						plugin_interface.getUIManager().copyToClipBoard( cb_data );
						
					}catch( Throwable  e ){
						
						e.printStackTrace();
					}
				}
			};
		
		final TableContextMenuItem menu1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "MagnetPlugin.contextmenu.exporturi" );
		final TableContextMenuItem menu2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, 	"MagnetPlugin.contextmenu.exporturi" );
			
		menu1.addListener( listener );
		menu2.addListener( listener );

		MagnetURIHandler.getSingleton().addListener(
			new MagnetURIHandlerListener()
			{
				public byte[]
				badge()
				{
					InputStream is = getClass().getClassLoader().getResourceAsStream( "com/aelitis/azureus/plugins/magnet/Magnet.gif" );
					
					if ( is == null ){
						
						return( null );
					}
					
					try{
						ByteArrayOutputStream	baos = new ByteArrayOutputStream();
						
						try{
							byte[]	buffer = new byte[8192];
							
							while( true ){
	
								int	len = is.read( buffer );
				
								if ( len <= 0 ){
									
									break;
								}
		
								baos.write( buffer, 0, len );
							}
						}finally{
							
							is.close();
						}
						
						return( baos.toByteArray());
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
						
						return( null );
					}
				}
							
				public byte[]
				download(
					final MagnetURIHandlerProgressListener		muh_listener,
					final byte[]								hash,
					final InetSocketAddress[]					sources,
					final long									timeout )
				
					throws MagnetURIHandlerException
				{
						// see if we've already got it!
					
					try{
						Download	dl = plugin_interface.getDownloadManager().getDownload( hash );
					
						if ( dl != null ){
							
							Torrent	torrent = dl.getTorrent();
							
							if ( torrent != null ){
								
								return( torrent.writeToBEncodedData());
							}
						}
					}catch( Throwable e ){
					
						Debug.printStackTrace(e);
					}
					
					return( MagnetPlugin.this.download(
							new MagnetPluginProgressListener()
							{
								public void
								reportSize(
									long	size )
								{
									muh_listener.reportSize( size );
								}
								
								public void
								reportActivity(
									String	str )
								{
									muh_listener.reportActivity( str );
								}
								
								public void
								reportCompleteness(
									int		percent )
								{
									muh_listener.reportCompleteness( percent );
								}
							},
							hash,
							sources,
							timeout ));
				}
				
				public boolean
				download(
					URL		url )
				
					throws MagnetURIHandlerException
				{
					try{
						
						plugin_interface.getDownloadManager().addDownload( url, false );
						
						return( true );
						
					}catch( DownloadException e ){
						
						throw( new MagnetURIHandlerException( "Operation failed", e ));
					}
				}
				
				public boolean
				set(
					String		name,
					Map		values )
				{
					List	l = listeners.getList();
					
					for (int i=0;i<l.size();i++){
						
						if (((MagnetPluginListener)l.get(i)).set( name, values )){
							
							return( true );
						}
					}
					
					return( false );
				}
				
				public int
				get(
					String		name,
					Map			values )
				{
					List	l = listeners.getList();
					
					for (int i=0;i<l.size();i++){
						
						int res = ((MagnetPluginListener)l.get(i)).get( name, values );
						
						if ( res != Integer.MIN_VALUE ){
							
							return( res );
						}
					}
					
					return( Integer.MIN_VALUE );
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
						// make sure DDB is initialised as we need it to register its
						// transfer types
					
					Thread t = 
						new AEThread( "MagnetPlugin:init" )
						{
							public void
							runSupport()
							{
								plugin_interface.getDistributedDatabase();
							}
						};
					
					t.setDaemon( true );
					
					t.start();
				}
				
				public void
				closedownInitiated(){}
				
				public void
				closedownComplete(){}			
			});
		
		plugin_interface.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UISWTInstance	swt = (UISWTInstance)instance;
							
							Image	image = swt.loadImage( "com/aelitis/azureus/plugins/magnet/icons/magnet.gif" );

							menu1.setGraphic( swt.createGraphic( image ));
							menu2.setGraphic( swt.createGraphic( image ));							
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
	}
	
	public URL
	getMagnetURL(
		Download		d )
	{
		Torrent	torrent = d.getTorrent();
		
		if ( torrent == null ){
			
			return( null );
		}
		
		return( getMagnetURL( torrent.getHash()));
	}
	
	public URL
	getMagnetURL(
		byte[]		hash )
	{
		try{
			return( new URL( "magnet:?xt=urn:btih:" + Base32.encode(hash)));
		
		}catch( Throwable e ){
		
			Debug.printStackTrace(e);
		
			return( null );
		}
	}
	
	public byte[]
	badge()
	{
		return( null );
	}
	
	public byte[]
	download(
		final MagnetPluginProgressListener		listener,
		final byte[]							hash,
		final InetSocketAddress[]				sources,
		final long								timeout )
	
		throws MagnetURIHandlerException
	{
		try{
			listener.reportActivity( getMessageText( "report.waiting_ddb" ));

			final DistributedDatabase db = plugin_interface.getDistributedDatabase();
			
			final List			potential_contacts 		= new ArrayList();
			final AESemaphore	potential_contacts_sem 	= new AESemaphore( "MagnetPlugin:liveones" );
			final AEMonitor		potential_contacts_mon	= new AEMonitor( "MagnetPlugin:liveones" );
			
			final int[]			outstanding		= {0};

			listener.reportActivity(  getMessageText( "report.searching" ));
			
			DistributedDatabaseListener	ddb_listener = 
				new DistributedDatabaseListener()
				{
					public void
					event(
						DistributedDatabaseEvent 		event )
					{
						int	type = event.getType();
	
						if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
													
							contactFound( event.getValue().getContact());
			
						}else if (	type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ||
									type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
								
								// now inject any explicit sources
							
							for (int i=0;i<sources.length;i++){
								
								try{
									contactFound( db.importContact(sources[i]));
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
							
							potential_contacts_sem.release();
						}
					}
					
					public void
					contactFound(
						final DistributedDatabaseContact	contact )
					{
						listener.reportActivity( getMessageText( "report.found", contact.getName()));
				
						outstanding[0]++;
						
						Thread t = 
							new AEThread( "MagnetPlugin:HitHandler")
							{
								public void
								runSupport()
								{
									try{
										boolean	alive = contact.isAlive(20*1000);
																						
										listener.reportActivity( 
												getMessageText( alive?"report.alive":"report.dead",	contact.getName()));
										
										try{
											potential_contacts_mon.enter();
											
											Object[]	entry = new Object[]{ new Boolean( alive ), contact};
											
											boolean	added = false;
											
											if ( alive ){
												
													// try and place before first dead entry 
										
												for (int i=0;i<potential_contacts.size();i++){
													
													if (!((Boolean)((Object[])potential_contacts.get(i))[0]).booleanValue()){
														
														potential_contacts.add(i, entry );
														
														added = true;
														
														break;
													}
												}
											}
											
											if ( !added ){
												
												potential_contacts.add( entry );	// dead at end
											}
												
											potential_contacts_sem.release();
												
										}finally{
												
											potential_contacts_mon.exit();
										}
									}finally{
										
										try{
											potential_contacts_mon.enter();													

											outstanding[0]--;
											
										}finally{
											
											potential_contacts_mon.exit();
										}
									}
								}
							};
							
						t.setDaemon(true);
						
						t.start();
					}
				};
				
			db.read(
				ddb_listener,
				db.createKey( hash, "Torrent download lookup for '" + ByteFormatter.encodeString( hash ) + "'" ),
				timeout,
				DistributedDatabase.OP_EXHAUSTIVE_READ | DistributedDatabase.OP_PRIORITY_HIGH );
			
			long	remaining	= timeout;
			
			while( remaining > 0 ){
					
				long start = SystemTime.getCurrentTime();
				
				potential_contacts_sem.reserve( remaining );
				
				remaining -= ( SystemTime.getCurrentTime() - start );
				
				DistributedDatabaseContact	contact;
				boolean						live_contact;
				
				try{
					potential_contacts_mon.enter();
					
					if ( potential_contacts.size() == 0 ){
						
						if ( outstanding[0] == 0 ){
						
							break;
							
						}else{
							
							continue;
						}
					}else{
					
						Object[]	entry = (Object[])potential_contacts.remove(0);
						
						live_contact 	= ((Boolean)entry[0]).booleanValue(); 
						contact 		= (DistributedDatabaseContact)entry[1];
					}
					
				}finally{
					
					potential_contacts_mon.exit();
				}
					
				// System.out.println( "magnetDownload: " + contact.getName() + ", live = " + live_contact );
				
				if ( !live_contact ){
					
					listener.reportActivity( getMessageText( "report.tunnel", contact.getName()));

					contact.openTunnel();
				}
				
				try{
					listener.reportActivity( getMessageText( "report.downloading", contact.getName()));
					
					DistributedDatabaseValue	value = 
						contact.read( 
								new DistributedDatabaseProgressListener()
								{
									public void
									reportSize(
										long	size )
									{
										listener.reportSize( size );
									}
									public void
									reportActivity(
										String	str )
									{
										listener.reportActivity( str );
									}
									
									public void
									reportCompleteness(
										int		percent )
									{
										listener.reportCompleteness( percent );
									}
								},
								db.getStandardTransferType( DistributedDatabaseTransferType.ST_TORRENT ),
								db.createKey ( hash , "Torrent download content for '" + ByteFormatter.encodeString( hash ) + "'"),
								timeout );
										
					if ( value != null ){
						
						return( (byte[])value.getValue(byte[].class));
					}
				}catch( Throwable e ){
					
					listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));
					
					Debug.printStackTrace(e);
				}
			}
		
			return( null );		// nothing found
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));

			throw( new MagnetURIHandlerException( "MagnetURIHandler failed", e ));
		}
	}
	
	protected String
	getMessageText(
		String	resource )
	{
		return( plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "MagnetPlugin." + resource ));
	}
	
	protected String
	getMessageText(
		String	resource,
		String	param )
	{
		return( plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( 
				"MagnetPlugin." + resource, new String[]{ param }));
	}
	
	public void
	addListener(
		MagnetPluginListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		MagnetPluginListener		listener )
	{
		listeners.remove( listener );
	}
}
