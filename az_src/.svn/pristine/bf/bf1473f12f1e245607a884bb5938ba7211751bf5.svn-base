/*
 * Created on 18-Feb-2005
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

package org.gudy.azureus2.plugins.ddb;

import java.net.InetSocketAddress;

/**
 * @author parg
 *
 */

public interface 
DistributedDatabaseContact 
{
	public byte[]
	getID();
	
	public String
	getName();
	
	public InetSocketAddress
	getAddress();
	
	public int
	getDHT();
	
	public boolean
	isAlive(
		long		timeout );
	
		// async version - event types: complete -> alive, timeout -> dead
	
	public void
	isAlive(
		long							timeout,
		DistributedDatabaseListener		listener );
	
	public boolean
	isOrHasBeenLocal();
	
		/**
		 * Tries to open a NAT tunnel to the contact. Should only be used if direct contact fails
		 * @return
		 */
	
	public boolean
	openTunnel();
	
	public void
	write(
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			data )
	
		throws DistributedDatabaseException;
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseProgressListener	listener,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		long								timeout )
	
		throws DistributedDatabaseException;
}
