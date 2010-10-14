/*
 * Created on 17 Sep 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 */
package com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep;

import com.aelitis.azureus.core.peermanager.messaging.Message;

/**
 * @author Allan Crooks
 *
 */
public interface LTMessage extends Message {
	
	public static final String LT_FEATURE_ID = "LT1";

	public static final String ID_LT_HANDSHAKE       = "lt_handshake";
	public static final byte[] ID_LT_HANDSHAKE_BYTES = ID_LT_HANDSHAKE.getBytes();
	public static final int SUBID_LT_HANDSHAKE       = 0;
	
	public static final String ID_UT_PEX             = "ut_pex";
	public static final byte[] ID_UT_PEX_BYTES       = ID_UT_PEX.getBytes();
	public static final int SUBID_UT_PEX             = 1;
	
	// Placeholder message indicating that a message was sent for an extension which has
	// been disabled.
	public static final String ID_DISABLED_EXT       = "disabled_extension";
	public static final byte[] ID_DISABLED_EXT_BYTES = ID_DISABLED_EXT.getBytes();
	public static final int SUBID_DISABLED_EXT       = 2;
}
