/*
 * Created on 02-Jan-2005
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

package org.gudy.azureus2.pluginsimpl.local.test;


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ddb.*;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeEvent;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeListener;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;


/**
 * @author parg
 *
 */


public class 
Test 
	implements Plugin
{
	private static AESemaphore		init_sem 	= new AESemaphore("PluginTester");
	private static AEMonitor		class_mon	= new AEMonitor( "PluginTester" );

	private static Test		singleton;
		
	
	public static Test
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				new AEThread( "plugin initialiser" )
				{
					public void
					runSupport()
					{
						PluginManager.registerPlugin( Test.class );
		
						Properties props = new Properties();
						
						props.put( PluginManager.PR_MULTI_INSTANCE, "true" );
						
						PluginManager.startAzureus( PluginManager.UI_SWT, props );
					}
				}.start();
			
				init_sem.reserve();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}	
	
	protected PluginInterface		plugin_interface;
	
	public void 
	initialize(
		PluginInterface _pi )
	{	
		plugin_interface	= _pi;
		
		singleton = this;
		
		init_sem.release();
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						Thread	t  = 
							new AEThread("test")
							{
								public void
								runSupport()
								{
									
									//testLinks();
									testMessaging();
									try{
										// PlatformManagerFactory.getPlatformManager().performRecoverableFileDelete( "C:\\temp\\recycle.txt" );
										// PlatformManagerFactory.getPlatformManager().setTCPTOSEnabled( false );
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
							};
							
						t.setDaemon(true);
						
						t.start();
					}
					
					public void
					closedownInitiated()
					{	
					}
					
					public void
					closedownComplete()
					{
					}
				});
	}
	
	protected void
	testMessaging()
	{
		try{
			AzureusCoreFactory.getSingleton().getCryptoManager().addPasswordHandler(
				new CryptoManagerPasswordHandler()
				{
					public char[]
		        	getPassword(
		        		int			handler_type,
		        		int			action_type,
		        		String		reason )
					{
						System.out.println( "CryptoPassword (" + reason + ")");
						
						return( "changeit".toCharArray());
					}
				});
			
			final SESecurityManager	sec_man = plugin_interface.getUtilities().getSecurityManager();
			
			final SEPublicKey	my_key = sec_man.getPublicKey( SEPublicKey.KEY_TYPE_ECC_192, "test" );

			final int	stream_crypto 	= MessageManager.STREAM_ENCRYPTION_RC4_REQUIRED;
			final boolean	use_sts		= true;
			final int	block_crypto 	= SESecurityManager.BLOCK_ENCRYPTION_AES;
			
			GenericMessageRegistration	reg = 
				plugin_interface.getMessageManager().registerGenericMessageType(
					"GENTEST", "Gen test desc", 
					stream_crypto,
					new GenericMessageHandler()
					{
						public boolean
						accept(
							GenericMessageConnection	connection )
						
							throws MessageException
						{
							System.out.println( "accept" );
							
							try{
								if ( use_sts ){
									
									connection = sec_man.getSTSConnection(
											connection, 
											my_key,
											new SEPublicKeyLocator()
											{
												public boolean
												accept(
													SEPublicKey	other_key )
												{
													System.out.println( "acceptKey" );
													
													return( true );
												}
											},
											"test",
											block_crypto );
								}
										
								connection.addListener(
									new GenericMessageConnectionListener()
									{
										public void
										connected(
											GenericMessageConnection	connection )
										{
										}
										
										public void
										receive(
											GenericMessageConnection	connection,
											PooledByteBuffer			message )
										
											throws MessageException
										{
											System.out.println( "receive: " + message.toByteArray().length );
											
											PooledByteBuffer	reply = 
												plugin_interface.getUtilities().allocatePooledByteBuffer( 
														new byte[connection.getMaximumMessageSize()]);
											
											connection.send( reply );
										}
										
										public void
										failed(
											GenericMessageConnection	connection,
											Throwable 					error )
										
											throws MessageException
										{
											System.out.println( "Responder connection error:" );

											error.printStackTrace();
										}	
									});
								
							}catch( Throwable e ){
								
								connection.close();
								
								e.printStackTrace();
							}
							
							return( true );
						}
					});
			
			InetSocketAddress	tcp_target = new InetSocketAddress( "127.0.0.1", 		6889 );
			InetSocketAddress	udp_target = new InetSocketAddress( "212.159.18.92", 	6881 );
			
			GenericMessageEndpoint	endpoint = reg.createEndpoint( tcp_target );
			
			endpoint.addTCP( tcp_target );
			endpoint.addUDP( udp_target );
			
			while( true ){
				
				try{
					for (int i=0;i<1000;i++){
						
						System.out.println( "Test: initiating connection" );
						
						final AESemaphore	sem = new AESemaphore( "wait!" );
						
						GenericMessageConnection	con = reg.createConnection( endpoint );
						
						if ( use_sts ){
							
							con = sec_man.getSTSConnection( 
								con, my_key,
								new SEPublicKeyLocator()
								{
									public boolean
									accept(
										SEPublicKey	other_key )
									{
										System.out.println( "acceptKey" );
										
										return( true );
									}
								},
								"test", block_crypto );
						}
						
						con.addListener(
							new GenericMessageConnectionListener()
							{
								public void
								connected(
									GenericMessageConnection	connection )
								{
									System.out.println( "connected" );
									
									PooledByteBuffer	data = plugin_interface.getUtilities().allocatePooledByteBuffer( "1234".getBytes());
									
									try{
										connection.send( data );
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
								
								public void
								receive(
									GenericMessageConnection	connection,
									PooledByteBuffer			message )
								
									throws MessageException
								{
									System.out.println( "receive: " + message.toByteArray().length );
									
									
									try{
										Thread.sleep(30000);
									}catch( Throwable e ){
										
									}
								
									/*
									PooledByteBuffer	reply = 
										plugin_interface.getUtilities().allocatePooledByteBuffer( new byte[16*1024]);
									
									
									connection.send( reply );
									*/
									
									System.out.println( "closing connection" );
									
									connection.close();
									
									sem.release();
								}
								
								public void
								failed(
									GenericMessageConnection	connection,
									Throwable 					error )
								
									throws MessageException
								{
									System.out.println( "Initiator connection error:" );
									
									error.printStackTrace();
									
									sem.release();
								}
							});
						
			
						con.connect();
						
						sem.reserve();
						
						Thread.sleep( 1000 );
					}
				
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					try{
						System.out.println( "Sleeping before retrying" );
						
						Thread.sleep( 30000 );
						
					}catch( Throwable f ){
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	testLinks()
	{
		plugin_interface.getDownloadManager().addListener(
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{
					DiskManagerFileInfo[]	info = download.getDiskManagerFileInfo();
					
					for (int i=0;i<info.length;i++){
						
						info[i].setLink( new File( "C:\\temp" ));
					}
				}
				
				public void
				downloadRemoved(
					Download	download )
				{
					
				}
			});
	}
	
	protected void
	testDDB()
	{
		try{
			DistributedDatabase	db = plugin_interface.getDistributedDatabase();
			
			DistributedDatabaseKey	key = db.createKey( new byte[]{ 4,7,1,2,5,8 });

			boolean	do_write	= false;
			
			if ( do_write ){
				
				DistributedDatabaseValue[] values = new DistributedDatabaseValue[500];
				
				for (int i=0;i<values.length;i++){
					
					byte[]	val = new byte[20];
					
					Arrays.fill( val, (byte)i );
					
					values[i] = db.createValue( val );
				}
				
				
				db.write(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							System.out.println( "Event:" + event.getType());
							
							if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_WRITTEN ){
								
								try{
									System.out.println( 
											"    write - key = " + 
											ByteFormatter.encodeString((byte[])event.getKey().getKey()) + 
											", val = " + ByteFormatter.encodeString((byte[]) event.getValue().getValue(byte[].class)));
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
						}
					},
					key,
					values );
			}else{
				
				db.read(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
								System.out.println( "Event:" + event.getType());
								
								if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_READ ){
									
									try{
										System.out.println( 
												"    read - key = " + 
												ByteFormatter.encodeString((byte[])event.getKey().getKey()) + 
												", val = " + ByteFormatter.encodeString((byte[]) event.getValue().getValue(byte[].class)));
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
							}
						},
						key,
						60000 );			
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	taTest()
	{
		try{
			
			final TorrentAttribute ta = plugin_interface.getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
			
			ta.addTorrentAttributeListener(
				new TorrentAttributeListener()
				{
					public void
					event(
						TorrentAttributeEvent	ev )
					{
						System.out.println( "ev: " + ev.getType() + ", " + ev.getData());
						
						if ( ev.getType() == TorrentAttributeEvent.ET_ATTRIBUTE_VALUE_ADDED ){
							
							if ( "plop".equals( ev.getData())){
								
								ta.removeDefinedValue( "plop" );
							}
						}
					}
				});
			
			ta.addDefinedValue( "wibble" );
					
			
			plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
						try{
							download.setAttribute( ta, "wibble" );
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						
					}
				});
				
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		getSingleton();
	}
}
