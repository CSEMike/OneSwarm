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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.InetSocketAddress;
import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
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
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.sharing.ShareResourceFile;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.net.magneturi.*;

/**
 * @author parg
 *
 */

public class 
MagnetPlugin
	implements Plugin
{	
	private static final String	SECONDARY_LOOKUP 			= "http://magnet.vuze.com/";
	private static final int	SECONDARY_LOOKUP_DELAY		= 20*1000;
	private static final int	SECONDARY_LOOKUP_MAX_TIME	= 2*60*1000;
	
	private static final String	PLUGIN_NAME				= "Magnet URI Handler";
	private static final String PLUGIN_CONFIGSECTION_ID = "plugins.magnetplugin";

	private PluginInterface		plugin_interface;
		
	private CopyOnWriteList		listeners = new CopyOnWriteList();
	
	private boolean			first_download	= true;
	
	private BooleanParameter secondary_lookup;
	
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", PLUGIN_NAME );
	}
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		BasicPluginConfigModel	config = 
			plugin_interface.getUIManager().createBasicPluginConfigModel( ConfigSection.SECTION_PLUGINS, 
					PLUGIN_CONFIGSECTION_ID);
		
		secondary_lookup = config.addBooleanParameter2( "MagnetPlugin.use.lookup.service", "MagnetPlugin.use.lookup.service", true );
		
		MenuItemListener	listener = 
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem		_menu,
					Object			_target )
				{
					Torrent torrent;
					String name;
					Object ds = ((TableRow)_target).getDataSource();
					if (ds instanceof ShareResourceFile) {
						try {
							torrent = ((ShareResourceFile) ds).getItem().getTorrent();
						} catch (ShareException e) {
							return;
						}
						name = ((ShareResourceFile) ds).getName();
					} else if (ds instanceof Download) {
						Download download = (Download)((TableRow)_target).getDataSource();
						torrent = download.getTorrent();
						name = download.getName();
					} else {
						return;
					}
				  
					
					String	cb_data = "magnet:?xt=urn:btih:" + Base32.encode( torrent.getHash()) + "&dn=" + UrlUtils.encode(name);

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
		final TableContextMenuItem menu3 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYSHARES, 	"MagnetPlugin.contextmenu.exporturi" );
			
		menu1.addListener( listener );
		menu2.addListener( listener );
		menu3.addListener( listener );

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
					final String								args,
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
								
								public void
								reportContributor(
									InetSocketAddress	address )
								{
								}
								
								public boolean 
								verbose() 
								{
									return( muh_listener.verbose());
								}
							},
							hash,
							args,
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
					
					AEThread2 t = 
						new AEThread2( "MagnetPlugin:init", true )
						{
							public void
							run()
							{
								plugin_interface.getDistributedDatabase();
							}
						};
										
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
							menu3.setGraphic( swt.createGraphic( image ));							
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
		MagnetPluginProgressListener		listener,
		byte[]								hash,
		String								args,
		InetSocketAddress[]					sources,
		long								timeout )
	
		throws MagnetURIHandlerException
	{
		byte[]	torrent_data = downloadSupport( listener, hash, args, sources, timeout );
		
		if ( args != null ){
			
			String[] bits = args.split( "&" );
			
			List<String>	new_web_seeds = new ArrayList<String>();

			for ( String bit: bits ){
				
				String[] x = bit.split( "=" );
				
				if ( x.length == 2 ){
					
					if ( x[0].equalsIgnoreCase( "ws" )){
						
						try{
							new_web_seeds.add( new URL( UrlUtils.decode( x[1] )).toExternalForm());
							
						}catch( Throwable e ){							
						}
					}
				}
			}
			
			if ( new_web_seeds.size() > 0 ){
				
				try{
					TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( torrent_data );
	
					Object obj = torrent.getAdditionalProperty( "url-list" );
					
					List<String> existing = new ArrayList<String>();
					
					if ( obj instanceof byte[] ){
		                
						try{
							new_web_seeds.remove( new URL( new String((byte[])obj, "UTF-8" )).toExternalForm());
							
						}catch( Throwable e ){							
						}
					}else if ( obj instanceof List ){
						
						List<byte[]> l = (List<byte[]>)obj;
						
						for ( byte[] b: l ){
							
							try{
								existing.add( new URL( new String((byte[])b, "UTF-8" )).toExternalForm());
								
							}catch( Throwable e ){							
							}
						}
					}
					
					boolean update = false;
					
					for ( String e: new_web_seeds ){
						
						if ( !existing.contains( e )){
							
							existing.add( e );
							
							update = true;
						}
					}
					
					if ( update ){
					
						List<byte[]>	l = new ArrayList<byte[]>();
						
						for ( String s: existing ){
							
							l.add( s.getBytes( "UTF-8" ));
						}
						
						torrent.setAdditionalProperty( "url-list", l );
						
						torrent_data = BEncoder.encode( torrent.serialiseToMap());
					}
					
				}catch( Throwable e ){
				}
			}
		}
		
		return( torrent_data );
	}
	
	private byte[]
	downloadSupport(
		final MagnetPluginProgressListener		listener,
		final byte[]							hash,
		final String							args,
		final InetSocketAddress[]				sources,
		final long								timeout )
	
		throws MagnetURIHandlerException
	{
		try{
			if ( first_download ){
			
				listener.reportActivity( getMessageText( "report.waiting_ddb" ));
				
				first_download = false;
			}
			
			final DistributedDatabase db = plugin_interface.getDistributedDatabase();
			
			final List			potential_contacts 		= new ArrayList();
			final AESemaphore	potential_contacts_sem 	= new AESemaphore( "MagnetPlugin:liveones" );
			final AEMonitor		potential_contacts_mon	= new AEMonitor( "MagnetPlugin:liveones" );
			
			final int[]			outstanding		= {0};
			final boolean[]		lookup_complete	= {false};
			
			listener.reportActivity(  getMessageText( "report.searching" ));
			
			DistributedDatabaseListener	ddb_listener = 
				new DistributedDatabaseListener()
				{
					private Set	found_set = new HashSet();
					
					public void
					event(
						DistributedDatabaseEvent 		event )
					{
						int	type = event.getType();
	
						if ( type == DistributedDatabaseEvent.ET_OPERATION_STARTS ){

								// give live results a chance before kicking in explicit ones
							
							if ( sources.length > 0 ){
								
								new DelayedEvent(
									"MP:sourceAdd",
									10*1000,
									new AERunnable()
									{
										public void
										runSupport()
										{
											addExplicitSources();
										}
									});
							}
							
						}else if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
													
							contactFound( event.getValue().getContact());
			
						}else if (	type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ||
									type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
								
							listener.reportActivity( getMessageText( "report.found", String.valueOf( found_set.size())));
							
								// now inject any explicit sources

							addExplicitSources();
							
							try{
								potential_contacts_mon.enter();													

								lookup_complete[0] = true;
								
							}finally{
								
								potential_contacts_mon.exit();
							}
							
							potential_contacts_sem.release();
						}
					}
					
					protected void
					addExplicitSources()
					{	
						for (int i=0;i<sources.length;i++){
							
							try{
								contactFound( db.importContact(sources[i]));
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}
					
					public void
					contactFound(
						final DistributedDatabaseContact	contact )
					{
						String	key = contact.getAddress().toString();
						
						synchronized( found_set ){
							
							if ( found_set.contains( key )){
								
								return;
							}
							
							found_set.add( key );
						}
						
						if ( listener.verbose()){
						
							listener.reportActivity( getMessageText( "report.found", contact.getName()));
						}
						
						try{
							potential_contacts_mon.enter();													

							outstanding[0]++;
							
						}finally{
							
							potential_contacts_mon.exit();
						}
						
						contact.isAlive(
							20*1000,
							new DistributedDatabaseListener()
							{
								public void 
								event(
									DistributedDatabaseEvent event) 
								{
									try{
										boolean	alive = event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE;
											
										if ( listener.verbose()){
										
											listener.reportActivity( 
												getMessageText( alive?"report.alive":"report.dead",	contact.getName()));
										}
										
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
										
										potential_contacts_sem.release();
									}
								}
							});
					}
				};
				
			db.read(
				ddb_listener,
				db.createKey( hash, "Torrent download lookup for '" + ByteFormatter.encodeString( hash ) + "'" ),
				timeout,
				DistributedDatabase.OP_EXHAUSTIVE_READ | DistributedDatabase.OP_PRIORITY_HIGH );
			
			long	remaining	= timeout;
			
			long 	overall_start 			= SystemTime.getMonotonousTime();
			boolean	sl_enabled				= secondary_lookup.getValue() && FeatureAvailability.isMagnetSLEnabled();

			long	secondary_lookup_time 	= -1;
			
			long last_found = -1;
			
			final Object[] secondary_result = { null };
			
			while( remaining > 0 ){
					
				try{
					potential_contacts_mon.enter();

					if ( 	lookup_complete[0] && 
							potential_contacts.size() == 0 &&
							outstanding[0] == 0 ){
						
						break;
					}
				}finally{
					
					potential_contacts_mon.exit();
				}
								
				
				while( remaining > 0 ){
				
					long wait_start = SystemTime.getMonotonousTime();

					boolean got_sem = potential_contacts_sem.reserve( 1000 );
		
					long now = SystemTime.getMonotonousTime();
					
					remaining -= ( now - wait_start );
				
					if ( got_sem ){
					
						last_found = now;
						
						break;
						
					}else{
						
						if ( sl_enabled ){
							
							if ( secondary_lookup_time == -1 ){
							
								long	base_time;
								
								if ( last_found == -1 || now - overall_start > 60*1000 ){
									
									base_time = overall_start;
									
								}else{
									
									base_time = last_found;
								}
								
								long	time_so_far = now - base_time;
								
								if ( time_so_far > SECONDARY_LOOKUP_DELAY ){
									
									secondary_lookup_time = SystemTime.getMonotonousTime();
									
									doSecondaryLookup( listener, secondary_result, hash, args );
								}
							}else{
								
								try{
									byte[] torrent = getSecondaryLookupResult( secondary_result );
									
									if ( torrent != null ){
										
										return( torrent );
									}
								}catch( ResourceDownloaderException e ){
									
									// ignore, we just continue processing
								}
							}
						}

						continue;
					}
				}
				
				DistributedDatabaseContact	contact;
				boolean						live_contact;
				
				try{
					potential_contacts_mon.enter();
					
					// System.out.println( "rem=" + remaining + ",pot=" + potential_contacts.size() + ",out=" + outstanding[0] );
					
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
						
							// let's verify the torrent
						
						byte[]	data = (byte[])value.getValue(byte[].class);

						try{
							TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( data );
							
							if ( Arrays.equals( hash, torrent.getHash())){
							
								listener.reportContributor( contact.getAddress());
						
								return( data );
								
							}else{
								
								listener.reportActivity( getMessageText( "report.error", "torrent invalid (hash mismatch)" ));
							}
						}catch( Throwable e ){
							
							listener.reportActivity( getMessageText( "report.error", "torrent invalid (decode failed)" ));
						}
					}
				}catch( Throwable e ){
					
					listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));
					
					Debug.printStackTrace(e);
				}
			}
		
			if ( sl_enabled ){
				
				if ( secondary_lookup_time == -1 ){
					
					secondary_lookup_time = SystemTime.getMonotonousTime();
					
					doSecondaryLookup(listener, secondary_result, hash, args );
				}
				
				while( SystemTime.getMonotonousTime() - secondary_lookup_time < SECONDARY_LOOKUP_MAX_TIME ){
					
					try{
						byte[] torrent = getSecondaryLookupResult( secondary_result );
						
						if ( torrent != null ){
							
							return( torrent );
						}
						
						Thread.sleep( 500 );
						
					}catch( ResourceDownloaderException e ){
						
						break;
					}
				}
			}
			
			return( null );		// nothing found
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));

			throw( new MagnetURIHandlerException( "MagnetURIHandler failed", e ));
		}
	}
	
	protected void
	doSecondaryLookup(
		final MagnetPluginProgressListener		listener,
		final Object[]							result,
		byte[]									hash,
		String									args )
	{
		listener.reportActivity( getMessageText( "report.secondarylookup", null ));
		
		try{
			ResourceDownloaderFactory rdf = plugin_interface.getUtilities().getResourceDownloaderFactory();
		
			URL sl_url = new URL( SECONDARY_LOOKUP + "magnetLookup?hash=" + Base32.encode( hash ) + (args.length()==0?"":("&args=" + UrlUtils.encode( args ))));
			
			ResourceDownloader rd = rdf.create( sl_url );
			
			rd.addListener(
				new ResourceDownloaderAdapter()
				{
					public boolean
					completed(
						ResourceDownloader	downloader,
						InputStream			data )
					{
						listener.reportActivity( getMessageText( "report.secondarylookup.ok", null ));

						synchronized( result ){
						
							result[0] = data;
						}
						
						return( true );
					}
					
					public void
					failed(
						ResourceDownloader			downloader,
						ResourceDownloaderException e )
					{
						synchronized( result ){
							
							result[0] = e;
						}
						
						listener.reportActivity( getMessageText( "report.secondarylookup.fail" ));
					}
				});
			
			rd.asyncDownload();
			
		}catch( Throwable e ){
			
			listener.reportActivity( getMessageText( "report.secondarylookup.fail", Debug.getNestedExceptionMessage( e ) ));
		}
	}
	
	protected byte[]
	getSecondaryLookupResult(
		final Object[]	result )
	
		throws ResourceDownloaderException
	{
		Object x;
		
		synchronized( result ){
			
			x = result[0];
			
			result[0] = null;
		}
			
		if ( x instanceof InputStream ){
			
			InputStream is = (InputStream)x;
				
			try{
				TOTorrent t = TOTorrentFactory.deserialiseFromBEncodedInputStream( is );
				
				TorrentUtils.setPeerCacheValid( t );
		
				return( BEncoder.encode( t.serialiseToMap()));
				
			}catch( Throwable e ){							
			}
		}else if ( x instanceof ResourceDownloaderException ){
			
			throw((ResourceDownloaderException)x);
		}
		
		return( null );
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
