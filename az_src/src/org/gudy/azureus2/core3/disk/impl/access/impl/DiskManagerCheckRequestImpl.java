/*
 * Created on 12-Dec-2005
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

package org.gudy.azureus2.core3.disk.impl.access.impl;

import org.gudy.azureus2.core3.disk.DiskManagerCheckRequest;

public class 
DiskManagerCheckRequestImpl 
	extends DiskManagerRequestImpl
	implements DiskManagerCheckRequest
{
	private int		piece_number;
	private Object	user_data;
	private boolean	low_priority;
	private boolean	ad_hoc		= true;
	
	private byte[]	hash;
	
	public 
	DiskManagerCheckRequestImpl(
		int		_piece_number,
		Object	_user_data )
	{
		piece_number	= _piece_number;
		user_data		= _user_data;
	}
	
	protected String
	getName()
	{
		return( "Check: " + piece_number + ",lp=" + low_priority + ",ah=" + ad_hoc );
	}
	
	public int 
	getPieceNumber()
	{
		return( piece_number );
	}
	 
	public Object
	getUserData()
	{
		return( user_data );
	}
	
	public void
	setLowPriority(
		boolean	low )
	{
		low_priority	= low;
	}
	
	public boolean
	isLowPriority()
	{
		return( low_priority );
	}
	
	public void
	setAdHoc(
		boolean	_ad_hoc )
	{
		ad_hoc	= _ad_hoc;
	}
	
	public boolean
	isAdHoc()
	{
		return( ad_hoc );
	}
	
	public void
	setHash(
		byte[]		_hash )
	{
		hash	= _hash;
	}
	
	public byte[]
	getHash()
	{
		return( hash );
	}
}
