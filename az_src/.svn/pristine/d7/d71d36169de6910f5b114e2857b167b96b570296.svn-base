/*
 * Created on 17-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;



public class 
TransportHelperFilterStreamCipher 
	extends TransportHelperFilterStream
{
	private TransportCipher					read_cipher;
	private TransportCipher					write_cipher;
		
	public
	TransportHelperFilterStreamCipher(
		TransportHelper			_transport,
		TransportCipher			_read_cipher,
		TransportCipher			_write_cipher )
	{
		super( _transport );
		
		read_cipher		= _read_cipher;
		write_cipher	= _write_cipher;
	}
	
	protected void
	cryptoOut(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException
	{
		write_cipher.update( source_buffer, target_buffer );
	}
	
	protected void
	cryptoIn(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException
	{
		read_cipher.update( source_buffer, target_buffer );
	}	
	
	public boolean 
	isEncrypted()
	{
		return( true );
	}
	
	public String
	getName(boolean verbose)
	{
		String proto_str = getHelper().getName(verbose);
		
		if ( proto_str.length() > 0 ){
			
			proto_str = " (" + proto_str + ")";
		}
		
		return( read_cipher.getName() + proto_str );
	}
}
