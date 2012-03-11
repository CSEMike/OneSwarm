/*
 * Created on May 27, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.util;


/**
 * This class DOES NOT COPY THE UNDERLYING BYTES AND ONLY SUPPORTS Short.MAX_VALUE bytes
 * @author Azureus
 *
 */

public class 
HashWrapper2 
{
	final private byte[] 	hash;
	final private short		offset;
	final private short		length;
	final private int		hash_code;

	public 
	HashWrapper2(
		byte[] hash ) 
	{
		this( hash, 0, hash.length );
	}

	public 
	HashWrapper2(
		byte[] 	_hash, 
		int 	_offset,
		int 	_length ) 
	{
		if ( _offset >= Short.MAX_VALUE ){
			throw( new RuntimeException( "Illegal value - offset too large" ));
		}
		
		if ( _length >= Short.MAX_VALUE ){
			throw( new RuntimeException( "Illegal value - length too large" ));
		}

		hash	= _hash;
		offset	= (short)_offset;
		length	= (short)_length;
		
		int	hc = 0;
		
		for (int i=offset; i<offset+length; i++) {

			hc = 31*hc + hash[i];
		}
		
		hash_code = hc;
	}

	public final boolean 
	equals(
		Object o) 
	{
		if( !( o instanceof HashWrapper2 )){
			
			return false;
		}
		
		HashWrapper2 other = (HashWrapper2)o;
		
		if ( other.length != length ){
			
			return( false );
		}
		
		byte[]	other_hash 		= other.hash;
		int		other_offset	= other.offset;
		
		for ( int i=0;i<length;i++){
			
			if ( hash[offset+i] != other_hash[other_offset+i] ){
				
				return( false );
			}
		}
		
		return( true );
	}

	public int	 
	hashCode() 
	{
		return( hash_code );
	}
}
