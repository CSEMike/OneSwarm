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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AESemaphore;


import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionManager;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.VivaldiPositionProvider;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;


/**
 * @author parg
 *
 */

public class 
DHTTransportUDPContactImpl
	implements DHTTransportUDPContact
{
	public static final int			NODE_STATUS_UNKNOWN		= 0xffffffff;
	public static final int			NODE_STATUS_ROUTABLE	= 0x00000001;

	static{
		DHTNetworkPositionManager.registerProvider( new VivaldiPositionProvider());
	}
	
	private	DHTTransportUDPImpl		transport;
	private InetSocketAddress		external_address;
	private InetSocketAddress		transport_address;
	
	private byte[]				id;
	private byte				protocol_version;
	private int					instance_id;
	private long				skew;
	private int					random_id;
	private int					node_status	= NODE_STATUS_UNKNOWN;
		
	private DHTNetworkPosition[]		network_positions;
	
	protected
	DHTTransportUDPContactImpl(
		boolean					_is_local,
		DHTTransportUDPImpl		_transport,
		InetSocketAddress		_transport_address,
		InetSocketAddress		_external_address,
		byte					_protocol_version,
		int						_instance_id,
		long					_skew )
	
		throws DHTTransportException
	{
		transport				= _transport;
		transport_address		= _transport_address;
		external_address		= _external_address;
		protocol_version		= _protocol_version;
		
		if ( transport_address.equals( external_address )){
			
			external_address	= transport_address;
		}
		
		instance_id		=		 _instance_id;
		skew			= 		_skew;
		
		if ( 	transport_address == external_address ||
				transport_address.getAddress().equals( external_address.getAddress())){

			id = DHTUDPUtils.getNodeID( external_address, protocol_version );
		}

		createNetworkPositions( _is_local );
	}
	
	public DHTTransport
	getTransport()
	{
		return( transport );
	}
	
	public byte
	getProtocolVersion()
	{
		return( protocol_version );
	}
	
	protected void
	setProtocolVersion(
		byte		v )
	{
		protocol_version 	= v;
	}
		
	public long
	getClockSkew()
	{
		return( skew );
	}
	
	public void
	setRandomID(
		int		_random_id )
	{
		random_id	= _random_id;
	}
	
	public int
	getRandomID()
	{
		return( random_id );
	}
	
	protected int
	getNodeStatus()
	{
		return( node_status );
	}
	
	protected void
	setNodeStatus(
		int		ns )
	{
		node_status	= ns;
	}
	
	public boolean
	isValid()
	{
		return( 	addressMatchesID() &&
					!transport.invalidExternalAddress( external_address.getAddress()));
	}
	
	protected boolean
	addressMatchesID()
	{
		return( id != null );
	}
	
	public InetSocketAddress
	getTransportAddress()
	{
		return( transport_address );
	}
	
	public void
	setTransportAddress(
		InetSocketAddress	address )
	{
		transport_address = address;
	}
	
	public InetSocketAddress
	getExternalAddress()
	{
		return( external_address );
	}
	
	public String
	getName()
	{
		return( DHTLog.getString2( id  ));
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( getExternalAddress());
	}
	
	public int
	getMaxFailForLiveCount()
	{
		return( transport.getMaxFailForLiveCount() );
	}
	
	public int
	getMaxFailForUnknownCount()
	{
		return( transport.getMaxFailForUnknownCount() );
	}
	
	public int
	getInstanceID()
	{
		return( instance_id );
	}
	
	protected void
	setInstanceIDAndVersion(
		int		_instance_id,
		byte	_protocol_version )
	{
		instance_id	= _instance_id;
		
			// target supports a higher version than we thought, update
		
		if ( _protocol_version > protocol_version ){
						
			protocol_version = _protocol_version;
		}
	}
	
	public boolean
	isAlive(
		long		timeout )
	{
		final AESemaphore	sem = new AESemaphore( "DHTTransportContact:alive");
		
		final boolean[]	alive = { false };
		
		try{
			sendPing(
				new DHTTransportReplyHandlerAdapter()
				{
					public void
					pingReply(
						DHTTransportContact contact )
					{
						alive[0]	= true;
						
						sem.release();
					}
					
					public void
					failed(
						DHTTransportContact 	contact,
						Throwable 				cause )
					{
						sem.release();
					}
				});
			
			sem.reserve( timeout );
		
			return( alive[0] );
			
		}catch( Throwable e ){
			
			return( false );
		}
	}

	public void 
	isAlive(
		DHTTransportReplyHandler 	handler, 
		long 						timeout )
	{
		transport.sendPing( this, handler, timeout, PRUDPPacketHandler.PRIORITY_IMMEDIATE );		
	}
	
	public void
	sendPing(
		DHTTransportReplyHandler	handler )
	{
		transport.sendPing( this, handler );
	}
	
	public void
	sendImmediatePing(
		DHTTransportReplyHandler	handler,
		long						timeout )
	{
		transport.sendImmediatePing( this, handler, timeout );
	}
	
	public void
	sendStats(
		DHTTransportReplyHandler	handler )
	{
		transport.sendStats( this, handler );
	}

	public void
	sendStore(
		DHTTransportReplyHandler	handler,
		byte[][]					keys,
		DHTTransportValue[][]		value_sets,
		boolean						immediate )
	{
		transport.sendStore( 
				this, handler, keys, value_sets, 
				immediate?PRUDPPacketHandler.PRIORITY_IMMEDIATE:PRUDPPacketHandler.PRIORITY_LOW );
	}
	
	public void 
	sendQueryStore(
		DHTTransportReplyHandler 	handler,
		int							header_length,
		List<Object[]>			 	key_details ) 
	{
		transport.sendQueryStore( this, handler, header_length, key_details);
	}
	
	public void
	sendFindNode(
		DHTTransportReplyHandler	handler,
		byte[]						nid )
	{
		transport.sendFindNode( this, handler, nid );
	}
		
	public void
	sendFindValue(
		DHTTransportReplyHandler	handler,
		byte[]						key,
		int							max_values,
		byte						flags )
	{
		transport.sendFindValue( this, handler, key, max_values, flags );
	}
	
	public void
	sendKeyBlock(
		final DHTTransportReplyHandler	handler,
		final byte[]					request,
		final byte[]					signature )
	{
			// gotta do anti-spoof
		
		sendFindNode(
			new DHTTransportReplyHandlerAdapter()
			{
				public void
				findNodeReply(
					DHTTransportContact 	contact,
					DHTTransportContact[]	contacts )
				{	
					transport.sendKeyBlockRequest( DHTTransportUDPContactImpl.this, handler, request, signature );
				}
				public void
				failed(
					DHTTransportContact 	_contact,
					Throwable				_error )
				{	
					handler.failed( _contact, _error );
				}
			},
			new byte[0] );
		
	}
	
	public DHTTransportFullStats
	getStats()
	{
		return( transport.getFullStats( this ));
	}
	
	public byte[]
	getID()
	{
		if ( id == null ){
			
			throw( new RuntimeException( "Invalid contact" ));
		}
		
		return( id );
	}
	
	public void
	exportContact(
		DataOutputStream	os )
	
		throws IOException, DHTTransportException
	{
		transport.exportContact( this, os );
	}
	
	public void
	remove()
	{
		transport.removeContact( this );
	}
	
	protected void
    setNetworkPositions(
    	DHTNetworkPosition[]	positions )
  	{
  		network_positions	= positions;
  	}
	
	public void
	createNetworkPositions(
		boolean  is_local )
	{
		network_positions	= DHTNetworkPositionManager.createPositions( id==null?DHTUDPUtils.getBogusNodeID():id, is_local );
	}
	
	public DHTNetworkPosition[]
  	getNetworkPositions()
	{
		return( network_positions );
	}
  	
  	public DHTNetworkPosition
  	getNetworkPosition(
  		byte	position_type )
  	{
  		for (int i=0;i<network_positions.length;i++){
  			
  			if ( network_positions[i].getPositionType() == position_type ){
  				
  				return( network_positions[i] );
  			}
  		}
  		
  		return( null );
  	}
	
	public String
	getString()
	{
		if ( transport_address.equals( external_address )){
			
			return( DHTLog.getString2(id) + "["+transport_address.toString()+",V" + getProtocolVersion() +"]");
		}
		
		return( DHTLog.getString2(id) + "[tran="+transport_address.toString()+",ext="+external_address+",V" + getProtocolVersion() +"]");
	}
}
