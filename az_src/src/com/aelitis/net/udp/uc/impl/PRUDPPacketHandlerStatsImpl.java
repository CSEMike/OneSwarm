/*
 * Created on 25-Jan-2005
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

package com.aelitis.net.udp.uc.impl;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.udp.uc.PRUDPPacketHandlerStats;

/**
 * @author parg
 *
 */

public class 
PRUDPPacketHandlerStatsImpl
	implements PRUDPPacketHandlerStats, Cloneable
{
	private PRUDPPacketHandlerImpl	packet_handler;
	
	private long packets_sent;
	private long packets_received;
	private long requests_timeout;
	private long bytes_sent;
	private long bytes_received;
	
	protected
	PRUDPPacketHandlerStatsImpl(
		PRUDPPacketHandlerImpl	_packet_handler )
	{
		packet_handler	= _packet_handler;
	}
	
	public long
	getPacketsSent()
	{
		return( packets_sent );
	}
	
	protected void
	packetSent(
		int		len )
	{
		packets_sent++;
		bytes_sent += len;
	}
	
	public long
	getPacketsReceived()
	{
		return( packets_received );
	}
	
	protected void
	packetReceived(
		int	len )
	{
		packets_received++;
		bytes_received	+= len;
	}
	
	protected void
	primordialPacketSent(
		int	len )
	{	
	}
	
	protected void
	primordialPacketReceived(
		int	len )
	{	
	}
	
	public long
	getRequestsTimedOut()
	{
		return( requests_timeout );
	}
	
	protected void
	requestTimedOut()
	{
		requests_timeout++;
	}
	
	public long
	getBytesSent()
	{
		return( bytes_sent );
	}
	
	public long
	getBytesReceived()
	{
		return( bytes_received );
	}
	
	public long
	getSendQueueLength()
	{
		return( packet_handler.getSendQueueLength());
	}
	
	public long
	getReceiveQueueLength()
	{
		return( packet_handler.getReceiveQueueLength());
		
	}
	
	public PRUDPPacketHandlerStats
	snapshot()
	{
		try{
			return((PRUDPPacketHandlerStats)clone());
			
		}catch( CloneNotSupportedException e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
}
