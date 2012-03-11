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

package com.aelitis.azureus.core.dht.transport.udp;

import java.net.InetSocketAddress;

import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;

/**
 * @author parg
 *
 */

public interface 
DHTTransportUDP
	extends DHTTransport
{
	public static final byte PROTOCOL_VERSION_2304					= 8;	
	public static final byte PROTOCOL_VERSION_2306					= 12;	
	public static final byte PROTOCOL_VERSION_2400					= 13;	
	public static final byte PROTOCOL_VERSION_2402					= 14;	
	public static final byte PROTOCOL_VERSION_2500					= 15;	
	public static final byte PROTOCOL_VERSION_2502					= 16;	
	public static final byte PROTOCOL_VERSION_3111					= 17;	
	public static final byte PROTOCOL_VERSION_4204					= 22;	// min -> 17
	public static final byte PROTOCOL_VERSION_4208					= 23;	
	public static final byte PROTOCOL_VERSION_4310					= 26;	// somewhere min has gone to 22
	public static final byte PROTOCOL_VERSION_4407					= 50;	// cvs
	public static final byte PROTOCOL_VERSION_4511					= 50;	// main
	public static final byte PROTOCOL_VERSION_4600					= 50;	// min -> 50

	public static final byte PROTOCOL_VERSION_DIV_AND_CONT			= 6;
	public static final byte PROTOCOL_VERSION_ANTI_SPOOF			= 7;
	public static final byte PROTOCOL_VERSION_ENCRYPT_TT			= 8;	// refed from DDBase
	public static final byte PROTOCOL_VERSION_ANTI_SPOOF2			= 8;

		// we can't fix the originator position until a previous fix regarding the incorrect
		// use of a contact's version > sender's version is fixed. This will be done at 2.3.0.4
		// We can therefore only apply this fix after then
	
	public static final byte PROTOCOL_VERSION_FIX_ORIGINATOR		= 9;
	public static final byte PROTOCOL_VERSION_VIVALDI				= 10;
	public static final byte PROTOCOL_VERSION_REMOVE_DIST_ADD_VER	= 11;
	public static final byte PROTOCOL_VERSION_XFER_STATUS			= 12;
	public static final byte PROTOCOL_VERSION_SIZE_ESTIMATE			= 13;
	public static final byte PROTOCOL_VERSION_VENDOR_ID				= 14;
	public static final byte PROTOCOL_VERSION_BLOCK_KEYS			= 14;

	public static final byte PROTOCOL_VERSION_GENERIC_NETPOS		= 15;
	public static final byte PROTOCOL_VERSION_VIVALDI_FINDVALUE		= 16;
	public static final byte PROTOCOL_VERSION_ANON_VALUES			= 17;
	public static final byte PROTOCOL_VERSION_CVS_FIX_OVERLOAD_V1	= 18;
	public static final byte PROTOCOL_VERSION_CVS_FIX_OVERLOAD_V2	= 19;
	public static final byte PROTOCOL_VERSION_MORE_STATS			= 20;
	public static final byte PROTOCOL_VERSION_CVS_FIX_OVERLOAD_V3	= 21;
	public static final byte PROTOCOL_VERSION_MORE_NODE_STATUS		= 22;
	public static final byte PROTOCOL_VERSION_LONGER_LIFE			= 23;
	public static final byte PROTOCOL_VERSION_REPLICATION_CONTROL	= 24;
	public static final byte PROTOCOL_VERSION_REPLICATION_CONTROL2	= 25;
	public static final byte PROTOCOL_VERSION_REPLICATION_CONTROL3	= 26;

	
	public static final byte PROTOCOL_VERSION_RESTRICT_ID_PORTS		= 32;	// introduced now (2403/V15) to support possible future change to id allocation
																			// If/when introduced the min DHT version must be set to 15 at the same time

	public static final byte PROTOCOL_VERSION_RESTRICT_ID_PORTS2	= 33;
	public static final byte PROTOCOL_VERSION_RESTRICT_ID_PORTS2X	= 34;	// nothing new here - added to we can track CVS user's access to replication control
	public static final byte PROTOCOL_VERSION_RESTRICT_ID_PORTS2Y	= 35;	// another one to track fix to broken rep factor handling
	public static final byte PROTOCOL_VERSION_RESTRICT_ID_PORTS2Z	= 36;	// hopefully last one - needed to excluded nodes that don't support replication frequency
	
	public static final byte PROTOCOL_VERSION_RESTRICT_ID3			= 50;	// ip and port based restrictions 

		// multiple networks reformats the requests and therefore needs the above fix to work
	
	public static final byte PROTOCOL_VERSION_NETWORKS				= PROTOCOL_VERSION_FIX_ORIGINATOR;
	
		// current versions
	
	public static final byte PROTOCOL_VERSION_MAIN					= PROTOCOL_VERSION_RESTRICT_ID3;
	public static final byte PROTOCOL_VERSION_CVS					= PROTOCOL_VERSION_RESTRICT_ID3;

	public static final byte PROTOCOL_VERSION_MIN					= PROTOCOL_VERSION_RESTRICT_ID3;
	public static final byte PROTOCOL_VERSION_MIN_CVS				= PROTOCOL_VERSION_RESTRICT_ID3;
	
	
	
	public static final byte VENDOR_ID_AELITIS		= 0x00;
	public static final byte VENDOR_ID_ShareNET		= 0x01;			// http://www.sharep2p.net/
	public static final byte VENDOR_ID_NONE			= (byte)0xff;

	public static final byte VENDOR_ID_ME			= VENDOR_ID_AELITIS;

	public DHTTransportUDPContact
	importContact(
		InetSocketAddress	address,
		byte				protocol_version )
	
		throws DHTTransportException;
}
