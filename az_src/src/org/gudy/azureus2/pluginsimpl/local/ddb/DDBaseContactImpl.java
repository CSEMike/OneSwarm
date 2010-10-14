/*
 * Created on 22-Feb-2005
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

package org.gudy.azureus2.pluginsimpl.local.ddb;

import java.net.InetSocketAddress;

import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;

import com.aelitis.azureus.plugins.dht.DHTPluginContact;


/**
 * @author parg
 *
 */

public class 
DDBaseContactImpl
	implements DistributedDatabaseContact
{
	private DDBaseImpl				ddb;
	private DHTPluginContact		contact;
	
	protected
	DDBaseContactImpl(
		DDBaseImpl				_ddb,
		DHTPluginContact		_contact )
	{
		ddb			= _ddb;
		contact		= _contact;
	}
	
	public String
	getName()
	{
		return( contact.getName());
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( contact.getAddress());
	}
	
	public boolean
	isAlive(
		long		timeout )
	{
		return( contact.isAlive( timeout ));
	}
	
	public boolean
	isOrHasBeenLocal()
	{
		return( contact.isOrHasBeenLocal());
	}

	public boolean
	openTunnel()
	{
		return( contact.openTunnel() != null );
	}
	
	public void
	write(
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			data )
	
		throws DistributedDatabaseException
	{
		throw( new DistributedDatabaseException( "not implemented" ));
	}
	
	public DistributedDatabaseValue
	read(
		final DistributedDatabaseProgressListener	listener,
		DistributedDatabaseTransferType				type,
		DistributedDatabaseKey						key,
		long										timeout )
	
		throws DistributedDatabaseException
	{
		return( ddb.read( this, listener, type, key, timeout ));
	}
	
	protected DHTPluginContact
	getContact()
	{
		return( contact );
	}
}
