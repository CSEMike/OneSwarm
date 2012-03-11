/*
 * Created on 19 Jun 2006
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

package org.gudy.azureus2.pluginsimpl.local.utils.security;

import java.util.Arrays;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;

public class 
SEPublicKeyImpl
	implements SEPublicKey
{
	public static SEPublicKey
	decode(
		byte[]	encoded )
	{
		int	type = encoded[0]&0xff;
		
		byte[]	x = new byte[encoded.length-1];
		
		System.arraycopy( encoded, 1, x, 0, x.length );
		
		return( new SEPublicKeyImpl( type, x ));
	}
	
	private int		type;
	private byte[]	encoded;
	private int		hashcode;
	
	protected
	SEPublicKeyImpl(
		int			_type,
		byte[]		_encoded )
	{
		type		= _type;
		encoded		= _encoded;
		hashcode	= new HashWrapper( encoded ).hashCode();
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public byte[]
	encodePublicKey()
	{
		byte[]	res = new byte[encoded.length+1];
		
		res[0] = (byte)type;
		
		System.arraycopy( encoded, 0, res, 1, encoded.length );
		
		return( res );
	}
	
	public byte[]
	encodeRawPublicKey()
	{
		byte[]	res = new byte[encoded.length];
				
		System.arraycopy( encoded, 0, res, 0, encoded.length );
		
		return( res );
	}
	
	public boolean
	equals(
		Object	other )
	{
		if ( other instanceof SEPublicKeyImpl ){
			
			return( Arrays.equals( encoded, ((SEPublicKeyImpl)other).encoded ));
			
		}else{
			
			return( false );
		}
	}
	
	public int
	hashCode()
	{
		return( hashcode );
	}
}
