/*
 * Created on Feb 8, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.messaging.azureus;


import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.*;



/**
 * 
 *
 */

public class 
AZMessageEncoder 
implements 
	MessageStreamEncoder 
{
	public static final int PADDING_MODE_NONE			= 0;
	public static final int PADDING_MODE_NORMAL			= 1;
	public static final int PADDING_MODE_MINIMAL		= 2;
	
	private int padding_mode;

	public 
	AZMessageEncoder( 
		int _padding_mode ) 
	{
		padding_mode = _padding_mode;
	}



	public RawMessage[] encodeMessage( Message message ) {
		return new RawMessage[]{ AZMessageFactory.createAZRawMessage( message, padding_mode  )};
	}


}
