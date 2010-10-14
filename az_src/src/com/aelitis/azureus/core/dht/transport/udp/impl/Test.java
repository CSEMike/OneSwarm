/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;

/**
 * @author parg
 *
 */

public class 
Test 
	implements DHTTransportRequestHandler
{
	public static void
	main(
		String[]	args )
	{
		new Test();
	}
	
	protected
	Test()
	{
		try{
			DHTTransport	udp1 = 
				DHTTransportFactory.createUDP(
					DHTTransportUDP.PROTOCOL_VERSION_MAIN, DHT.NW_MAIN, false,
					null, null, 6881, 5, 3, 5000, 50, 25, false, false, com.aelitis.azureus.core.dht.impl.Test.getLogger());
		
			udp1.setRequestHandler( this );
			
			DHTTransport	udp2 = DHTTransportFactory.createUDP( 
					DHTTransportUDP.PROTOCOL_VERSION_MAIN, DHT.NW_MAIN, false,
					null, null, 6882, 5, 3, 5000, 50, 25, false, false, com.aelitis.azureus.core.dht.impl.Test.getLogger());
		
			udp2.setRequestHandler( this );

			final DHTTransportUDPContact	c1 = (DHTTransportUDPContact)udp1.getLocalContact();
			
			for (int i=0;i<10;i++){
				
				final int f_i = i;
				
				c1.sendPing(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						pingReply(
							DHTTransportContact contact )
						{
							System.out.println( "ping reply: " + f_i );
						}
						
						public void
						failed(
							DHTTransportContact 	contact,
							Throwable				error )
						{
							System.out.println( "ping failed" );
						}
					});
			}
	
			c1.sendStore(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						storeReply(
							DHTTransportContact contact,
							byte[]				types )
						{
							System.out.println( "store reply" );
						}
						
						public void
						failed(
							DHTTransportContact 	contact,
							Throwable				error )
						{
							System.out.println( "store failed" );
						}
					},
					new byte[][]{ new byte[23] },
					new DHTTransportValue[][]{{
						new DHTTransportValue()
						{
							public boolean
							isLocal()
							{
								return( false );
							}
							
							public long
							getCreationTime()
							{
								return( 2 );
							}
							
							public byte[]
							getValue()
							{
								return( "sdsd".getBytes());
							}
							
							public int
							getVersion()
							{
								return( 0 );
							}
							
							public DHTTransportContact
							getOriginator()
							{
								return( c1 );
							}
							
							public int
							getFlags()
							{
								return(0);
							}
							
							public String
							getString()
							{
								return( new String(getValue()));
							}
						}
					}});
			
			c1.sendFindNode(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						findNodeReply(
							DHTTransportContact 	contact,
							DHTTransportContact[] 	contacts )
						{
							System.out.println( "findNode reply" );
						}
						
						public void
						failed(
							DHTTransportContact 	contact,
							Throwable 				e )
						{
							System.out.println( "findNode failed" );
						}
					},
					new byte[12]);
			
			System.out.println( "sending find value" );
			
			c1.sendFindValue(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						findValueReply(
							DHTTransportContact 	contact,
							DHTTransportContact[] 	contacts )
						{
							System.out.println( "findValue contacts reply" );
						}
						
						public void
						findValueReply(
							DHTTransportContact 	contact,
							DHTTransportValue[]		values,
							byte					diversification_type,
							boolean					more_to_come )
						{
							System.out.println( "findValue value reply" );
						}
						
						public void
						failed(
							DHTTransportContact 	contact,
							Throwable				error )
						{
							System.out.println( "findValue failed" );
						}
					},
					new byte[3], 1, (byte)0 );
			
			System.out.println( "sending complete" );

			Thread.sleep(1000000);
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public void
	pingRequest(
		DHTTransportContact contact )
	{
		System.out.println( "TransportHandler: ping" );
	}
		
	public void
	keyBlockRequest(
		DHTTransportContact contact,
		byte[]				key_block_request,
		byte[]				key_block_signature )
	{
		System.out.println( "TransportHandler: keyBlockRequest" );
	}
	
	public DHTTransportStoreReply
	storeRequest(
		DHTTransportContact 	contact, 
		final byte[][]			keys,
		DHTTransportValue[][]	value_sets )
	{
		System.out.println( "TransportHandler: store" );
		
		return( 
				new DHTTransportStoreReply()
				{
					public byte[]
		        	getDiversificationTypes()
					{
						return( new byte[keys.length] );
					}
		        	
		        	public boolean
		        	blocked()
		        	{
		        		return( false );
		        	}
		        	
		        	public byte[]
		        	getBlockRequest()
		        	{
		        		return( null );
		        	}
		        	
		        	public byte[]
		        	getBlockSignature()
		        	{
		        		return( null );
		        	}
				});
	}
	
	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact contact, 
		byte[]				id )
	{
		System.out.println( "TransportHandler: findNode" );
		
		return(new DHTTransportContact[]{ contact });
	}
	
	public DHTTransportFindValueReply
	findValueRequest(
		final DHTTransportContact contact, 
		byte[]					key,
		int						max_values,
		byte					flags )
	{
		System.out.println( "TransportHandler: findValue" );
		
		return( 
				new DHTTransportFindValueReply()
				{
					public boolean
					hit()
					{
						return( false );
					}
					
					public byte
					getDiversificationType()
					{
						return( DHT.DT_NONE );
					}
					
					public DHTTransportValue[]
					getValues()
					{
						return( null );
					}
					
					public DHTTransportContact[]
					getContacts()
					{
						return( new DHTTransportContact[]{ contact } );
					}
					
					public boolean
					blocked()
					{
						return( false );
					}
					
					public byte[]
					getBlockedKey()
					{
						return( null );
					}
					
					public byte[]
					getBlockedSignature()
					{
						return( null );
					}
				});
	}

	public void
	contactImported(
		DHTTransportContact	contact )
	{
		
	}
	
	public void
	contactRemoved(
		DHTTransportContact	contact )
	{
	}

	public int
	getTransportEstimatedDHTSize()
	{
		return(0);
	}
	
	public void
	setTransportEstimatedDHTSize(
		int	size )
	{
	}
	
	public DHTTransportFullStats
	statsRequest(
		DHTTransportContact	contact )
	{
		return( null );
	}
}
