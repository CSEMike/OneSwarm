/*
 * Created on Feb 28, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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

import java.util.AbstractMap;



public class
LightHashMapEx<S,T> 
	extends LightHashMap<S,T> implements Cloneable 
{
	public static final byte FL_MAP_ORDER_INCORRECT	= 0x01;
	
	private byte		flags;
	
	public
	LightHashMapEx(
		AbstractMap<S,T>	m )
	{
		super( m );
	}
	
	public void
	setFlag(
		byte		flag,
		boolean		set )
	{
		if ( set ){
			
			flags |= flag;
			
		}else{
			
			flags &= ~flag;
		}
	}
	
	public boolean
	getFlag(
		byte	flag )
	{
		return((flags&flag) != 0 );
	}
}
