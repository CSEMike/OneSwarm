/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.impl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.*;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.nat.impl.DHTNATPuncherImpl;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.loopback.DHTTransportLoopbackImpl;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

/**
 * @author parg
 *
 */

public class 
Test 
	implements DHTNATPuncherAdapter
{
	static boolean	AELITIS_TEST	= false;
	static InetSocketAddress	AELITIS_ADDRESS = new InetSocketAddress("213.186.46.164", 6881);
	
	static int DEFAULT_NETWORK = DHT.NW_CVS;
	
	static{
		
		DHTTransportUDPImpl.TEST_EXTERNAL_IP	= true;
	}
	
	int num_dhts			= 3;
	int num_stores			= 2;
	static int MAX_VALUES	= 10000;
	
	boolean	udp_protocol	= true;
	int		udp_timeout		= 1000;
	

	static int		K			= 20;
	static int		B			= 5;
	static int		ID_BYTES	= 20;
	
	int		fail_percentage	= 00;
	
	static Properties	dht_props = new Properties();

	static{
		dht_props.put( DHT.PR_CONTACTS_PER_NODE, new Integer(K));
		dht_props.put( DHT.PR_NODE_SPLIT_FACTOR, new Integer(B));
		dht_props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer(30000));
		dht_props.put( DHT.PR_ORIGINAL_REPUBLISH_INTERVAL, new Integer(60000));
	}

	static byte[]	th_key	= new byte[]{ 1,1,1,1 };
	
	static Map	check = new HashMap();


	static DHTLogger	logger;
	
	static{
		
		final LoggerChannel c_logger = AzureusCoreFactory.create().getPluginManager().getDefaultPluginInterface().getLogger().getNullChannel("test");
		
		c_logger.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	content )
				{
					System.out.println( content );
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					System.out.println( str );
					
					error.printStackTrace();
				}
			});
		
		logger = 
			new DHTLogger()
			{
				public void
				log(
					String	str )
				{
					c_logger.log( str );
				}
			
				public void
				log(
					Throwable e )
				{
					c_logger.log( e );
				}
				
				public void
				log(
					int		log_type,
					String	str )
				{
					if ( isEnabled( log_type )){
						
						c_logger.log( str );
					}
				}
			
				public boolean
				isEnabled(
					int	log_type )
				{
					return( true );
				}
					
				public PluginInterface
				getPluginInterface()
				{
					return( c_logger.getLogger().getPluginInterface());
				}
			};
	}

	public static DHTLogger
	getLogger()
	{
		return( logger );
	}
	
	public static void
	main(
		String[]		args )
	{
		new Test();
	}
	
	
	Map	port_map = new HashMap();
	
	protected
	Test()
	{
		try{
			DHTLog.setLogging( true );
			
			DHT[]			dhts 		= new DHT[num_dhts*2+30];
			DHTTransport[]	transports 	= new DHTTransport[num_dhts*2+30];
			
			
			for (int i=0;i<num_dhts;i++){
				
				createDHT( dhts, transports, DEFAULT_NETWORK, i );
			}

			for (int i=0;i<num_dhts-1;i++){
			
				if ( AELITIS_TEST ){
					
					((DHTTransportUDP)transports[i]).importContact( AELITIS_ADDRESS, DHTTransportUDP.PROTOCOL_VERSION_MAIN );
					
				}else{
					ByteArrayOutputStream	baos = new ByteArrayOutputStream();
					
					DataOutputStream	daos = new DataOutputStream( baos );
					
					transports[i].getLocalContact().exportContact( daos );
					
					daos.close();
					
					transports[i+1].importContact( new DataInputStream( new ByteArrayInputStream( baos.toByteArray())));
				}
					
				dhts[i].integrate(true);
					
				if ( i > 0 && i%10 == 0 ){
						
					System.out.println( "Integrated " + i + " DHTs" );
				}
			}
			
			if ( AELITIS_TEST ){
				
				((DHTTransportUDP)transports[num_dhts-1]).importContact( AELITIS_ADDRESS, DHTTransportUDP.PROTOCOL_VERSION_MAIN );

			}else{
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				DataOutputStream	daos = new DataOutputStream( baos );
				
				transports[0].getLocalContact().exportContact( daos );
				
				daos.close();
				
				transports[num_dhts-1].importContact( new DataInputStream( new ByteArrayInputStream( baos.toByteArray())));
			}
			
			
			dhts[num_dhts-1].integrate( true );
			
			DHTTransportLoopbackImpl.setFailPercentage(fail_percentage);
			
			//dht1.print();
			
			//DHTTransportLoopbackImpl.setLatency( 500);
			
			/*
			System.out.println( "before put:" + transports[99].getStats().getString());
			dhts[99].put( "fred".getBytes(), new byte[2]);
			System.out.println( "after put:" + transports[99].getStats().getString());

			System.out.println( "get:"  + dhts[0].get( "fred".getBytes()));
			System.out.println( "get:"  + dhts[77].get( "fred".getBytes()));
			*/
			
			Map	store_index = new HashMap();
			
			for (int i=0;i<num_stores;i++){
				
				int	dht_index = (int)(Math.random()*num_dhts);
				
				DHT	dht = dhts[dht_index];

				dht.put( (""+i).getBytes(), "", new byte[4], (byte)0, new DHTOperationAdapter());
			
				store_index.put( ""+i, dht );
				
				if ( i != 0 && i %100 == 0 ){
					
					System.out.println( "Stored " + i + " values" );
				}
			}
			
			Timer	timer = new Timer("");
			
			timer.addPeriodicEvent(
				10000,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event) 
					{
						if ( !udp_protocol ){
							
							DHTTransportStats stats = DHTTransportLoopbackImpl.getOverallStats();
						
							System.out.println( "Overall stats: " + stats.getString());
						}
					}
				});
			
			LineNumberReader	reader = new LineNumberReader( new InputStreamReader( System.in ));
			
			while( true ){
				
				System.out.print( "> " );
				
				try{
					String	str = reader.readLine().trim();
					
					if ( str == null ){
						
						break;
					}
					
					int	pos = str.indexOf(' ');
					
					if ( pos == -1 || pos == 0 ){
						
						usage();
						
						continue;
					}
					
					int	dht_index = (int)(Math.random()*num_dhts);
					
					DHT	dht = dhts[dht_index];
					
					String	lhs = str.substring(0,pos);
					String	rhs = str.substring(pos+1);
					
					DHTTransportStats	stats_before 	= null;
					
					char command = lhs.toLowerCase().charAt(0);
					
					if ( command == 'p' ){
						
						pos = rhs.indexOf('=');
						
						if ( pos == -1 ){
							
							usage();
							
						}else{
						
							System.out.println( "Using dht " + dht_index );
							
							stats_before = dht.getTransport().getStats().snapshot();
	
							String	key = rhs.substring(0,pos);
							String	val = rhs.substring(pos+1);
							
							dht.put( key.getBytes(), "", val.getBytes(), (byte)(Math.random()*255), new DHTOperationAdapter() );
						}
					}else if ( command == 'x' ){
						
						dht = (DHT)store_index.get( rhs );
						
						if ( dht == null ){
							
							System.out.println( "DHT not found" );
							
						}else{
							
							stats_before = dht.getTransport().getStats().snapshot();
							
							byte[]	res = dht.remove( rhs.getBytes(), "", new DHTOperationAdapter());
							
							if ( res != null ){
								
								store_index.remove( rhs );
							}
							
							System.out.println( "-> " + (res==null?"null":new String(res)));
						}
					}else if ( command == 'e' ){
						
						dht = (DHT)store_index.get( rhs );
						
						if ( dht == null ){
							
							System.out.println( "DHT not found" );
							
						}else{
							
							DataOutputStream	daos = new DataOutputStream( new FileOutputStream( "C:\\temp\\dht.state"));
							
							dht.exportState( daos, 0 );
							
							daos.close();
						}
					}else if ( command == 'g' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
					
						dht.get( 
								rhs.getBytes(), "", (byte)0, 32, 0, false, false,
								new DHTOperationAdapter()
								{
									public void
									read(
										DHTTransportContact	contact,
										DHTTransportValue	value )
									{
										System.out.println( "-> " + new String(value.getValue()));
									}
																	
									public void
									complete(
										boolean				timeout )
									{
										System.out.println( "-> complete" );
									}		
								});
						
					}else if ( command == '?' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
					
						final DHT	f_dht = dht;
						
						dht.get( 
								rhs.getBytes(), "", DHT.FLAG_STATS, 32, 0, false, false,
								new DHTOperationAdapter()
								{
									public void
									read(
										DHTTransportContact	contact,
										DHTTransportValue	value )
									{
										System.out.println( "-> " + new String(value.getValue()) + ", flags=" + value.getFlags());
										
										try{
											DHTStorageKeyStats	stats = f_dht.getStorageAdapter().deserialiseStats( new DataInputStream( new ByteArrayInputStream( value.getValue())));
											
											System.out.println( "    stats: size = " + stats.getSize() + ", entries=" + stats.getEntryCount() + ", rpm=" + stats.getReadsPerMinute());
										}catch( Throwable e ){
											
											e.printStackTrace();
										}
									}
																	
									public void
									complete(
										boolean				timeout )
									{
										System.out.println( "-> complete" );
									}		
								});
						
						
						
					}else if ( command == 'd' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
						
						byte[]	res = dht.remove( rhs.getBytes(), "", new DHTOperationAdapter());
						
						System.out.println( "-> " + (res==null?"null":new String(res)));
						
					}else if ( command == 'z' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
						
						dht.get( rhs.getBytes(), "", (byte)0, 10, 0, false, false,
								new DHTOperationListener()
								{
									public void
									searching(
										DHTTransportContact	contact,
										int					level,
										int					active_searches )
									{
										
									}
									
									public void
									diversified()
									{
									}
									
									public void
									found(
										DHTTransportContact	contact )
									{
									}
									
									public void
									read(
										final DHTTransportContact	contact,
										final DHTTransportValue		value )
									{
										System.out.println( "-> " + value.getString());
	
										new AEThread("blah")
										{
											public void
											runSupport()
											{
												DHTTransportFullStats stats = contact.getStats();
										
												System.out.println( "    stats = " + stats.getString() );
											}
										}.start();
									}
									public void
									wrote(
										final DHTTransportContact	contact,
										DHTTransportValue	value )
									{
									}
									
						
									public void
									complete(
										boolean				timeout )
									{
										System.out.println( "complete");
									}
								});
						
						
					}else if ( command == 'v' ){
				
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							stats_before = dht.getTransport().getStats().snapshot();
							
							dht.print();
	
							List	l = dht.getControl().getContacts();
							
							for (int i=0;i<l.size();i++){
								
								DHTControlContact	c = (DHTControlContact)l.get(i);
								
								System.out.println( "  contact:" + c.getRouterContact().getString() + "/" + c.getTransportContact().getString());
							}
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 'q' ){
						
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							dht.destroy();
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 't' ){
						
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							stats_before = dht.getTransport().getStats().snapshot();
							
							((DHTTransportUDPImpl)transports[index]).testInstanceIDChange();
							
							dht.integrate( true );
	
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 's' ){
						
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							stats_before = dht.getTransport().getStats().snapshot();
							
							((DHTTransportUDPImpl)transports[index]).testTransportIDChange();
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 'a' ){
						
						int	net = DEFAULT_NETWORK;
						
						try{
							net = Integer.parseInt( rhs );
							
						}catch( Throwable e ){
							
						}
						
						createDHT( dhts, transports, net, num_dhts++ );
						
						dht	= dhts[num_dhts-1];
						
						stats_before = transports[num_dhts-1].getStats().snapshot();
						
						ByteArrayOutputStream	baos = new ByteArrayOutputStream();
						
						DataOutputStream	daos = new DataOutputStream( baos );
						
						List	ok_t = new ArrayList();
						
						for (int i=0;i<num_dhts-1;i++){
							
							DHTTransport	t = transports[i];
							
							if ( t.getNetwork() == net ){
								
								ok_t.add( t );
							}
						}
						
						if ( ok_t.size() > 0 ){
							
							DHTTransport	r_t = (DHTTransport)ok_t.get((int)(Math.random()*(ok_t.size()-1)));
						
							r_t.getLocalContact().exportContact( daos );
							
							daos.close();
						
							transports[num_dhts-1].importContact( new DataInputStream( new ByteArrayInputStream( baos.toByteArray())));
						}else{
							
							System.out.println( "No comaptible networks found" );
							
						}
						
						dht.integrate( true );
	
						dht.print();
						
					}else if ( command == 'r' ){
						
						System.out.println( "read - dht0 -> dht1" );
											
						byte[]	res = 
							dhts[0].getTransport().readTransfer(
									new DHTTransportProgressListener()
									{
										public void
										reportSize(
											long	size )
										{
											System.out.println( "   read size: " + size );
										}
										
										public void
										reportActivity(
											String	str )
										{
											System.out.println( "   read act: " + str );
										}
										
										public void
										reportCompleteness(
											int		percent )
										{
											System.out.println( "   read %: " + percent );
										}
									},
									dhts[1].getTransport().getLocalContact(),
									th_key,
									new byte[]{1,2,3,4},
									30000 );
		
						System.out.println( "res = " + res );
						
					}else if ( command == 'w' ){
						
						System.out.println( "write - dht0 -> dht1" );
											
						dhts[0].getTransport().writeTransfer(
									new DHTTransportProgressListener()
									{
										public void
										reportSize(
											long	size )
										{
											System.out.println( "   write size: " + size );
										}
										
										public void
										reportActivity(
											String	str )
										{
											System.out.println( "   write act: " + str );
										}
										
										public void
										reportCompleteness(
											int		percent )
										{
											System.out.println( "   write %: " + percent );
										}
									},
									dhts[1].getTransport().getLocalContact(),
									th_key,
									new byte[]{1,2,3,4},
									new byte[1000],
									60000 );
		
					}else if ( command == 'c' ){
						
						System.out.println( "call - dht0 <-> dht1" );
											
						byte[] res = 
							dhts[0].getTransport().writeReadTransfer(
									new DHTTransportProgressListener()
									{
										public void
										reportSize(
											long	size )
										{
											System.out.println( "   readWrite size: " + size );
										}
										
										public void
										reportActivity(
											String	str )
										{
											System.out.println( "   readWrite act: " + str );
										}
										
										public void
										reportCompleteness(
											int		percent )
										{
											System.out.println( "   readWrite %: " + percent );
										}
									},
									dhts[1].getTransport().getLocalContact(),
									th_key,
									new byte[1000],
									60000 );
						
						System.out.println( "    reply: len = " + res.length );
						
					}else if ( command == 'b' ){
						
						if ( rhs.equals("1")){
							
							System.out.println( "rendezvous bind: dht2 -> rdv dht1" );
						
							DHTNATPuncherImpl	puncher = (DHTNATPuncherImpl)dhts[2].getNATPuncher();
						
							puncher.setRendezvous( 
								dhts[2].getTransport().getLocalContact(),
								dhts[1].getTransport().getLocalContact());

						}else if ( rhs.equals("2" )){
							
							System.out.println( "rendezvous punch: dht0 -> rdv dht2" );
							
							DHTNATPuncherImpl	puncher = (DHTNATPuncherImpl)dhts[0].getNATPuncher();

							Map	originator_data = new HashMap();
							
							originator_data.put( "hello", "mum" );
							
							Map client_data = puncher.punch( "Test", dhts[2].getTransport().getLocalContact(), null, originator_data);
							
							System.out.println( "   punch client data: " + client_data );
						}
					}else if ( command == 'k' ){
						
						int	sp = rhs.indexOf(' ');
						
						String	key_block;
						boolean	add;
						
						if ( sp == -1 ){
							key_block = rhs;
							add	= true;
						}else{
							key_block = rhs.substring(0,sp);
							add = false;
						}
						
						String	mod = "123";
						String	exp = "567";
						
						
						KeyFactory key_factory = KeyFactory.getInstance("RSA");
						
						RSAPrivateKeySpec 	private_key_spec = 
							new RSAPrivateKeySpec( new BigInteger(mod,16), new BigInteger(exp,16));
				
						RSAPrivateKey	key = (RSAPrivateKey)key_factory.generatePrivate( private_key_spec );
						
						byte[]	req = new byte[ 8 + 20 ];
						
						req[0]	= (byte)(add?0x01:0x00);
						
						int	time = (int)(System.currentTimeMillis()/1000);
						
						req[4] = (byte)(time>>24);
						req[5] = (byte)(time>>16);
						req[6] = (byte)(time>>8);
						req[7] = (byte)(time);
						
						System.arraycopy( new SHA1Simple().calculateHash(key_block.getBytes()), 0, req, 8, 20 );
						
						Signature	sig = Signature.getInstance("MD5withRSA" );
						
						sig.initSign( key );
						
						sig.update( req );
						
						dhts[1].getTransport().getLocalContact().sendKeyBlock(
							new DHTTransportReplyHandlerAdapter()
							{
								public void
								keyBlockReply(
									DHTTransportContact 	_contact )
								{
									System.out.println( "key block sent ok" );
								}
								
								public void
								failed(
									DHTTransportContact 	contact,
									Throwable				error )
								{
									System.out.println( "key block failed" );
									
									error.printStackTrace();
								}
							},
							req,
							sig.sign());
						
					}else{
						
						usage();
					}
									
					if ( stats_before != null ){
						
						DHTTransportStats	stats_after = dht.getTransport().getStats().snapshot();
	
						System.out.println( "before:" + stats_before.getString());
						System.out.println( "after:" + stats_after.getString());
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	createDHT(
		DHT[]			dhts,
		DHTTransport[]	transports,
		int				network,
		int				i )
	
		throws DHTTransportException
	{
		DHTTransport	transport;
		
		if ( udp_protocol ){
			
			Integer	next_port = (Integer)port_map.get( new Integer( network ));
			
			if ( next_port == null ){
				
				next_port = new Integer(0);
				
			}else{
				
				next_port = new Integer( next_port.intValue() + 1 );
			}
			
			port_map.put( new Integer( network ), next_port );
			
			byte protocol = network==0?DHTTransportUDP.PROTOCOL_VERSION_MAIN:DHTTransportUDP.PROTOCOL_VERSION_CVS; 
			
			// byte protocol = i%2==0?DHTTransportUDP.PROTOCOL_VERSION_MAIN:DHTTransportUDP.PROTOCOL_VERSION_CVS; 

			
			transport = DHTTransportFactory.createUDP( 
							protocol,
							network, 
							false,
							null, 
							null, 
							6890 + next_port.intValue(), 
							5, 
							3, 
							udp_timeout, 
							50, 
							25, 
							false, 
							false,
							logger );
			
		}else{
			
			transport = DHTTransportFactory.createLoopback(ID_BYTES);
		}
		
		transport.registerTransferHandler(
			th_key,
			new DHTTransportTransferHandler()
			{
				public String
				getName()
				{
					return( "test" );
				}
				
				public byte[]
				handleRead(
					DHTTransportContact	originator,
					byte[]				key )
				{
					byte[]	data = new byte[1000];
					
					System.out.println("handle read -> length = " + data.length );
					
					return( data );
				}
				
				public byte[]
				handleWrite(
					DHTTransportContact	originator,
					byte[]				key,
					byte[]				value )
				{
					byte[]	reply = null;
					
					if ( value.length == 1000 ){
						
						reply = new byte[4];
					}
					
					System.out.println("handle write -> length = " + value.length +", reply = " + reply );
					
					return( reply );
				}
			});
		
		/*
		HashWrapper	id = new HashWrapper( transport.getLocalContact().getID());
		
		if ( check.get(id) != null ){
			
			System.out.println( "Duplicate ID - aborting" );
			
			return;
		}
		
		check.put(id,"");
		*/
		
		DHTStorageAdapter	storage_adapter = new DHTPluginStorageManager( network, logger, new File( "C:\\temp\\dht\\" + i));

		DHT	dht = DHTFactory.create( transport, dht_props, storage_adapter, this, logger );
		
		dhts[i]	= dht;					

		transports[i] = transport;
	}
	
	/*
	public DHTStorageKey
	keyCreated(
		HashWrapper		key,
		boolean			local )
	{
		System.out.println( "key created" );
		
		return( 
				new DHTStorageKey()
				{
					public byte
					getDiversificationType()
					{
						return( DHT.DT_NONE );
					}
					public void
					serialiseStats(
						DataOutputStream		os )
					
						throws IOException
					{	
						os.writeInt( 45 );
					}
				});
	}
	
	public void
	keyDeleted(
		DHTStorageKey		key )
	{
		System.out.println( "key deleted" );
	}
	
	public void
	keyRead(
		DHTStorageKey			adapter_key,
		DHTTransportContact		contact )
	{
		System.out.println( "value read" );
	}
	
	public void
	valueAdded(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		System.out.println( "value added" );
	}
	
	public void
	valueUpdated(
		DHTStorageKey		key,
		DHTTransportValue	old_value,
		DHTTransportValue	new_value)
	{
		System.out.println( "value updated" );
	}
	
	public void
	valueDeleted(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		System.out.println( "value deleted" );
	}
	
	public byte[][]
	getExistingDiversification(
		byte[]			key,
		boolean			put_operation )
	{
		System.out.println( "getExistingDiversification: put = " + put_operation );

		return( new byte[][]{ key });
	}
	
	public byte[][]
	createNewDiversification(
		byte[]			key,
		boolean			put_operation,
		int				diversification_type )
	{
		System.out.println( "createNewDiversification: put = " + put_operation + ", type = " + diversification_type );

		return( new byte[0][] );
	}
	*/
	
	public Map
	getClientData(
		InetSocketAddress	originator,
		Map					originator_data )
	{
		System.out.println( "getClientData - " + originator_data + "/" + originator );

		Map	res = new HashMap();
		
		res.put( "udp_data_port", new Long( 1234 ));
		res.put( "tcp_data_port", new Long( 5678 ));
		
		return( res );
	}
	
	protected static void
	usage()
	{
		System.out.println( "syntax: [p g] <key>[=<value>]" );
	}
}
