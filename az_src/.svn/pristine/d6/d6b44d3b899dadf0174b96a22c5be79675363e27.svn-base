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

package org.gudy.azureus2.pluginsimpl.local.ddb;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;

/**
 * @author parg
 *
 */

public class 
DDBaseKeyImpl
	implements DistributedDatabaseKey
{
	private Object		key;
	private byte[]		key_bytes;
	private String		description;
	
	protected 
	DDBaseKeyImpl(
		Object	_key )
	
		throws DistributedDatabaseException
	{
		this( _key, null );
	}
	
	protected 
	DDBaseKeyImpl(
		Object		_key,
		String		_description )
	
		throws DistributedDatabaseException
	{
		key			= _key;
		description	= _description;
				
		key_bytes	= DDBaseHelpers.encode( key );

		if ( description == null ){
			
			if ( key instanceof String ){
				
				description = (String)key;
				
			}else{
				
				description = "[" + ByteFormatter.nicePrint(key_bytes) + "]";
			}
		}
	}
	
	public Object
	getKey()
	{
		return( key );
	}
	
	protected byte[]
	getBytes()
	{
		return( key_bytes );
	}
	
	public String
	getDescription()
	{
		return( description );
	}
}
