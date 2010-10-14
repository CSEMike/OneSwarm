/*
 * Created on 18-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.util;

import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketHelper;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketRequest;

/**
 * @author parg
 *
 */

public abstract class 
DHTTransportStatsImpl
	implements DHTTransportStats
{
	private byte	protocol_version;
	
	private long[]	pings		= new long[4];
	private long[]	find_nodes	= new long[4];
	private long[]	find_values	= new long[4];
	private long[]	stores		= new long[4];
	private long[]	stats		= new long[4];
	private long[]	data		= new long[4];
	private long[]	key_blocks	= new long[4];
	
	private long[]	aliens		= new long[6];

	private long	incoming_requests;
	private long	outgoing_requests;
	
	private long	incoming_version_requests;
	private long[]	incoming_request_versions;
	private long	outgoing_version_requests;
	private long[]	outgoing_request_versions;
	
	protected
	DHTTransportStatsImpl(
		byte	_protocol_version )
	{
		protocol_version	= _protocol_version;
		
		incoming_request_versions = new long[protocol_version+1];
		outgoing_request_versions = new long[protocol_version+1];
	}
	
	protected byte
	getProtocolVersion()
	{
		return( protocol_version );
	}
	
	public void
	add(
		DHTTransportStatsImpl	other )
	{
		add( pings, other.pings );
		add( find_nodes, other.find_nodes );
		add( find_values, other.find_values );
		add( stores, other.stores );
		add( stats, other.stats );
		add( data, other.data );
		add( key_blocks, other.key_blocks );
		add( aliens, other.aliens );
		
		incoming_requests += other.incoming_requests;
		outgoing_requests += other.outgoing_requests;
	}
	
	protected void
	add(
		long[]	a,
		long[] 	b )
	{
		for (int i=0;i<a.length;i++){
			a[i]	+= b[i];
		}
	}
	
	protected void
	snapshotSupport(
		DHTTransportStatsImpl	clone )
	{
		clone.pings			= (long[])pings.clone();
		clone.find_nodes	= (long[])find_nodes.clone();
		clone.find_values	= (long[])find_values.clone();
		clone.stores		= (long[])stores.clone();
		clone.data			= (long[])data.clone();
		clone.key_blocks	= (long[])key_blocks.clone();
		clone.aliens		= (long[])aliens.clone();
		
		clone.incoming_requests	= incoming_requests;
		clone.outgoing_requests	= outgoing_requests;
	}
		// ping
	
	public void
	pingSent(
		DHTUDPPacketRequest	request )
	{
		pings[STAT_SENT]++;
					
		outgoingRequestSent( request );
	}
	
	public void
	pingOK()
	{
		pings[STAT_OK]++;
	}
	public void
	pingFailed()
	{
		pings[STAT_FAILED]++;
	}
	public void
	pingReceived()
	{
		pings[STAT_RECEIVED]++;
	}
	
	public long[]
	getPings()
	{
		return( pings );
	}
	
		// key blocks
	
	public void
	keyBlockSent(
		DHTUDPPacketRequest	request )
	{
		key_blocks[STAT_SENT]++;
					
		outgoingRequestSent( request );
	}
	public void
	keyBlockOK()
	{
		key_blocks[STAT_OK]++;
	}
	public void
	keyBlockFailed()
	{
		key_blocks[STAT_FAILED]++;
	}
	public void
	keyBlockReceived()
	{
		key_blocks[STAT_RECEIVED]++;
	}
	
	public long[]
	getKeyBlocks()
	{
		return( key_blocks );
	}
	
		// find node
	
	public void
	findNodeSent(
		DHTUDPPacketRequest	request )
	{
		find_nodes[STAT_SENT]++;
					
		outgoingRequestSent( request );
	}
	public void
	findNodeOK()
	{
		find_nodes[STAT_OK]++;
	}
	public void
	findNodeFailed()
	{
		find_nodes[STAT_FAILED]++;
	}
	public void
	findNodeReceived()
	{
		find_nodes[STAT_RECEIVED]++;
	}
	public long[]
	getFindNodes()
	{
		return( find_nodes );
	}
	
		// find value
	
	public void
	findValueSent(
		DHTUDPPacketRequest	request )
	{
		find_values[STAT_SENT]++;
					
		outgoingRequestSent( request );
	}
	public void
	findValueOK()
	{
		find_values[STAT_OK]++;
	}
	public void
	findValueFailed()
	{
		find_values[STAT_FAILED]++;
	}
	public void
	findValueReceived()
	{
		find_values[STAT_RECEIVED]++;
	}
	public long[]
	getFindValues()
	{
		return( find_values );
	}
	
		// store
	
	public void
	storeSent(
		DHTUDPPacketRequest	request )
	{
		stores[STAT_SENT]++;
		
		outgoingRequestSent( request );
	}
	
	public void
	storeOK()
	{
		stores[STAT_OK]++;
	}
	public void
	storeFailed()
	{
		stores[STAT_FAILED]++;
	}
	public void
	storeReceived()
	{
		stores[STAT_RECEIVED]++;
	}
	public long[]
	getStores()
	{
		return( stores );
	}
		//stats
	
	public void
	statsSent(
		DHTUDPPacketRequest	request )
	{
		stats[STAT_SENT]++;
		
		outgoingRequestSent( request );
	}
	
	public void
	statsOK()
	{
		stats[STAT_OK]++;
	}
	public void
	statsFailed()
	{
		stats[STAT_FAILED]++;
	}
	public void
	statsReceived()
	{
		stats[STAT_RECEIVED]++;
	}
	
		//data
	
	public void
	dataSent(
		DHTUDPPacketRequest	request )
	{
		data[STAT_SENT]++;
		
		outgoingRequestSent( request );
	}
	
	public void
	dataOK()
	{
		data[STAT_OK]++;
	}
	
	public void
	dayaFailed()
	{
		data[STAT_FAILED]++;
	}
	
	public void
	dataReceived()
	{
		data[STAT_RECEIVED]++;
	}
	
	public long[]
	getData()
	{
		return( data );
	}
	
	protected void
	outgoingRequestSent(
		DHTUDPPacketRequest	request )
	{
		outgoing_requests++;		

		if ( DHTLog.TRACE_VERSIONS ){
			
			byte protocol_version = request.getProtocolVersion();
			
			if ( protocol_version >= 0 && protocol_version < outgoing_request_versions.length ){
				
				outgoing_request_versions[ protocol_version ]++;
				
				outgoing_version_requests++;
				
				if ( outgoing_version_requests%100 == 0 ){
					
					String	str= "";
					
					for (int i=0;i<outgoing_request_versions.length;i++){
						
						long	count = outgoing_request_versions[i];
						
						if ( count > 0 ){
							
							str += (str.length()==0?"":", ") + i + "=" +  count + "[" +
										((outgoing_request_versions[i]*100)/outgoing_version_requests) + "]";
						}
					}
					
					System.out.println( "net " + request.getTransport().getNetwork() + ": Outgoing versions: tot = " + outgoing_requests +"/" + outgoing_version_requests + ": " + str );
				}
				
				if ( outgoing_version_requests%1000 == 0 ){
					
					for (int i=0;i<outgoing_request_versions.length;i++){
						
						outgoing_request_versions[i] = 0;
					}
	
					outgoing_version_requests	= 0;
				}
			}
		}
	}
	
	public void
	incomingRequestReceived(
		DHTUDPPacketRequest	request,
		boolean				alien )
	{
		incoming_requests++;
		
		if ( alien ){
			
			int	type = request.getAction();
			
			if ( type == DHTUDPPacketHelper.ACT_REQUEST_FIND_NODE ){
				
				aliens[AT_FIND_NODE]++;
			
			}else if ( type == DHTUDPPacketHelper.ACT_REQUEST_FIND_VALUE ){
				
				aliens[AT_FIND_VALUE]++;
				
			}else if ( type == DHTUDPPacketHelper.ACT_REQUEST_PING ){
				
				aliens[AT_PING]++;
				
			}else if ( type == DHTUDPPacketHelper.ACT_REQUEST_STATS ){
				
				aliens[AT_STATS]++;
				
			}else if ( type == DHTUDPPacketHelper.ACT_REQUEST_STORE ){
				
				aliens[AT_STORE]++;
				
			}else if ( type == DHTUDPPacketHelper.ACT_REQUEST_KEY_BLOCK ){
				
				aliens[AT_KEY_BLOCK]++;
			}
		}
		
		if ( DHTLog.TRACE_VERSIONS ){
			
			byte protocol_version = request.getProtocolVersion();
			
			if ( protocol_version >= 0 && protocol_version < incoming_request_versions.length ){
				
				incoming_request_versions[ protocol_version ]++;
				
				incoming_version_requests++;
				
				if ( incoming_version_requests%100 == 0 ){
					
					String	str= "";
					
					for (int i=0;i<incoming_request_versions.length;i++){
						
						long	count = incoming_request_versions[i];
						
						if ( count > 0 ){
							
							str += (str.length()==0?"":", ") + i + "=" +  count + "[" +
										((incoming_request_versions[i]*100)/incoming_version_requests) + "]";
						}
					}
					
					System.out.println( "net " + request.getTransport().getNetwork() + ": Incoming versions: tot = " + incoming_requests +"/" + incoming_version_requests + ": " + str );
				}
				
				if ( incoming_version_requests%1000 == 0 ){
					
					for (int i=0;i<incoming_request_versions.length;i++){
						
						incoming_request_versions[i] = 0;
					}
	
					incoming_version_requests	= 0;
				}
			}
		}
	}
	
	public long[]
	getAliens()
	{
		return( aliens );
	}
	
	public long
	getIncomingRequests()
	{
		return( incoming_requests );
	}
	
	public String
	getString()
	{
		return( "ping:" + getString( pings ) + "," +
				"store:" + getString( stores ) + "," +
				"node:" + getString( find_nodes ) + "," +
				"value:" + getString( find_values ) + "," +
				"stats:" + getString( stats ) + "," +
				"data:" + getString( data ) + "," +
				"kb:" + getString( key_blocks ) + "," +
				"incoming:" + incoming_requests +"," +
				"alien:" + getString( aliens ));
	}
	
	protected String
	getString(
		long[]	x )
	{
		String	str = "";
		
		for (int i=0;i<x.length;i++){
			
			str += (i==0?"":",") + x[i];
		}
				
		return( str );
	}
}
