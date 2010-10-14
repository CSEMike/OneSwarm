/*
 * Created on Jan 13, 2006
 * Created by Alon Rohter
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
package com.aelitis.azureus.core.peermanager.messaging.advanced;

import com.aelitis.azureus.core.peermanager.messaging.Message;


/**
 * 
 */
public interface ADVMessage extends Message {
	
	public static final String PLUGIN_MESSAGE_FEATURE_ID = "AZPLUGMSG";
	

	public static final String ADV_FEATURE_ID = "ADV1";

  public static final String ID_ADV_HANDSHAKE    	    = "ADV_HANDSHAKE";
  public static final byte[] ID_ADV_HANDSHAKE_BYTES     = ID_ADV_HANDSHAKE.getBytes();
  
  public static final int SUBID_ADV_HANDSHAKE					= 0;
	
}
