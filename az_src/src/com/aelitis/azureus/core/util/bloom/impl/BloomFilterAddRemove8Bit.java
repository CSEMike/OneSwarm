/*
 * Created on 13-May-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.util.bloom.impl;

import java.util.Map;

import com.aelitis.azureus.core.util.bloom.BloomFilter;

public class 
BloomFilterAddRemove8Bit
	extends BloomFilterImpl
{
	private byte[]		map;

	public
	BloomFilterAddRemove8Bit(
		int		_max_entries )
	{
		super( _max_entries );
			
		map	= new byte[getMaxEntries()];
	}
	
	public
	BloomFilterAddRemove8Bit(
		Map<String,Object>		x )
	{
		super( x );
		
		map = (byte[])x.get( "map" );
	}
	
	protected void
	serialiseToMap(
		Map<String,Object>		x )
	{
		super.serialiseToMap( x );
		
		x.put( "map", map.clone());
	}
	
	public BloomFilter 
	getReplica() 
	{
		return( new BloomFilterAddRemove8Bit( getMaxEntries()));
	}
	
	protected int
	trimValue(
		int	value )
	{
		if ( value < 0 ){
			return( 0 );
		}else if ( value > 255 ){
			return( 255 );
		}else{
			return( value );
		}
	}
	
	protected int
	getValue(
		int		index )
	{
		return( map[index] & 0xff );
	}
	
	protected int
	incValue(
		int		index )
	{
		int	original_value = getValue( index );
		
		if ( original_value >= 255 ){
			
			return( 255 );
		}
				
		setValue( index, (byte)(original_value+1));
		
		return( original_value );
	}
	
	protected int
	decValue(
		int		index )
	{
		int	original_value = getValue( index );

		if ( original_value <= 0 ){
			
			return( 0 );
		}
				
		setValue( index, (byte)(original_value-1));
		
		return( original_value );
	}
	
	private void
	setValue(
		int		index,
		byte	value )
	{
		map[index] = value;
	}
}
