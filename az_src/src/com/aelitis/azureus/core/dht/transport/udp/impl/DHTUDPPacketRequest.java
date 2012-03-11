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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.packethandler.DHTUDPPacketNetworkHandler;
import com.aelitis.net.udp.uc.PRUDPPacketRequest;

/**
 * @author parg
 *
 */

public class 
DHTUDPPacketRequest 
	extends 	PRUDPPacketRequest
	implements 	DHTUDPPacket
{
	public static final int	DHT_HEADER_SIZE	= 
		PRUDPPacketRequest.PR_HEADER_SIZE + 
		1 + 		// protocol version
		1 + 		// originator version
		4 + 		// network
		4 +			// instance id
		8 +			// time
		DHTUDPUtils.INETSOCKETADDRESS_IPV4_SIZE;
		
	private DHTTransportUDPImpl	transport;
	
	private byte				protocol_version;
	private byte				vendor_id	= DHTTransportUDP.VENDOR_ID_NONE;
	private int					network;
	
	private byte				originator_version;
	private long				originator_time;
	private InetSocketAddress	originator_address;
	private int					originator_instance_id;
	
	private long				skew;
	
	public
	DHTUDPPacketRequest(
		DHTTransportUDPImpl				_transport,	
		int								_type,
		long							_connection_id,
		DHTTransportUDPContactImpl		_local_contact,
		DHTTransportUDPContactImpl		_remote_contact )
	{
		super( _type, _connection_id );
		
		transport	= _transport;
		
			// serialisation constructor
		
		protocol_version		= _remote_contact.getProtocolVersion();		
		
			// the target might be at a higher protocol version that us, so trim back if necessary
			// as we obviously can't talk a higher version than what we are!
		
		if ( protocol_version > _transport.getProtocolVersion() ){
			
			protocol_version = _transport.getProtocolVersion();
		}
		
		originator_address		= _local_contact.getExternalAddress();
		originator_instance_id	= _local_contact.getInstanceID();
		originator_time			= SystemTime.getCurrentTime();
	}
	
	protected
	DHTUDPPacketRequest(
		DHTUDPPacketNetworkHandler		network_handler,
		DataInputStream					is,
		int								type,
		long							con_id,
		int								trans_id )
	
		throws IOException
	{
		super( type, con_id, trans_id );
		
			// deserialisation constructor
		
		protocol_version	= is.readByte();
		
		if ( protocol_version >= DHTTransportUDP.PROTOCOL_VERSION_VENDOR_ID ){
			
			vendor_id	= is.readByte();			
		}
		
		if ( protocol_version >= DHTTransportUDP.PROTOCOL_VERSION_NETWORKS ){
			
			network	= is.readInt();
		}

		if ( protocol_version < ( network == DHT.NW_CVS?DHTTransportUDP.PROTOCOL_VERSION_MIN_CVS:DHTTransportUDP.PROTOCOL_VERSION_MIN )){
			
			throw( new IOException( "Invalid DHT protocol version, please update Azureus" ));
		}
		
			// we can only get the correct transport after decoding the network...
	
		transport = network_handler.getTransport( this );

		if ( protocol_version >= DHTTransportUDP.PROTOCOL_VERSION_FIX_ORIGINATOR ){
			
			originator_version = is.readByte();
			
		}else{
			
				// this should be set correctly in the post-deserialise code, however default
				// it for now
			
			originator_version = protocol_version;
		}
		
		originator_address		= DHTUDPUtils.deserialiseAddress( is );
		
		originator_instance_id	= is.readInt();
		
		originator_time			= is.readLong();
		
			// We maintain a rough view of the clock diff between them and us,
			// times are then normalised appropriately. 
			// If the skew is positive then this means our clock is ahead of their
			// clock. Thus any times they send us will need to have the skew added in
			// so that they're correct relative to us.
			// For example: X has clock = 01:00, they create a value that expires at
			// X+8 hours 09:00. They send X to us. Our clock is an hour ahead (skew=+1hr)
			// We receive it at 02:00 (our time) and therefore time it out an hour early.
			// We therefore need to adjust the creation time to be 02:00.
		
			// Likewise, when we return a time to a caller we need to adjust by - skew to
			// put the time into their frame of reference.
		
		skew = SystemTime.getCurrentTime() - originator_time;
		
		transport.recordSkew( originator_address, skew );
	}
	
	protected void
	postDeserialise(
		DataInputStream	is )
	
		throws IOException
	{
		if ( protocol_version < DHTTransportUDP.PROTOCOL_VERSION_FIX_ORIGINATOR ){

			if ( is.available() > 0 ){
				
				originator_version	= is.readByte();
							
			}else{
				
				originator_version = protocol_version;
			}
			
				// if the originator is a higher version than us then we can't do anything sensible
				// working at their version (e.g. we can't reply to them using that version). 
				// Therefore trim their perceived version back to something we can deal with
			
			if ( originator_version > getTransport().getProtocolVersion() ){
				
				originator_version = getTransport().getProtocolVersion();
			}
		}
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);

			// add to this and you need to amend HEADER_SIZE above
		
		os.writeByte( protocol_version );		
		
		if ( protocol_version >= DHTTransportUDP.PROTOCOL_VERSION_VENDOR_ID ){
						
			os.writeByte( DHTTransportUDP.VENDOR_ID_ME );
		}
		
		if ( protocol_version >= DHTTransportUDP.PROTOCOL_VERSION_NETWORKS ){
			
			os.writeInt( network );
		}

		if ( protocol_version >= DHTTransportUDP.PROTOCOL_VERSION_FIX_ORIGINATOR ){
		
				// originator version
			
			os.writeByte( getTransport().getProtocolVersion());
		}
		
		try{
			DHTUDPUtils.serialiseAddress( os, originator_address );
			
		}catch( DHTTransportException	e ){
			
			throw( new IOException( e.getMessage()));
		}
		
		os.writeInt( originator_instance_id );
		
		os.writeLong( originator_time );
	}
	
	protected void
	postSerialise(
		DataOutputStream	os )
	
		throws IOException
	{
		if ( protocol_version < DHTTransportUDP.PROTOCOL_VERSION_FIX_ORIGINATOR ){
			
				// originator version is at tail so it works with older versions
			
			os.writeByte( getTransport().getProtocolVersion());
		}
	}
	
	public DHTTransportUDPImpl
	getTransport()
	{
		return( transport );
	}
	
	protected long
	getClockSkew()
	{
		return( skew );
	}
	
	public byte
	getProtocolVersion()
	{
		return( protocol_version );
	}
	
	protected byte
	getVendorID()
	{
		return( vendor_id );
	}
	
	public int
	getNetwork()
	{
		return( network );
	}
	
	public void
	setNetwork(
		int		_network )
	{
		network	= _network;
	}
	
	protected byte
	getOriginatorVersion()
	{
		return( originator_version );
	}

	protected InetSocketAddress
	getOriginatorAddress()
	{
		return( originator_address );
	}
	
	protected void
	setOriginatorAddress(
		InetSocketAddress	address )
	{
		originator_address	= address;
	}
	
	protected int
	getOriginatorInstanceID()
	{
		return( originator_instance_id );
	}
	
	public String
	getString()
	{
		return( super.getString() + ",[prot=" + protocol_version + ",ven=" + vendor_id + ",net="+network+",ov=" + originator_version + "]");
	}
}
