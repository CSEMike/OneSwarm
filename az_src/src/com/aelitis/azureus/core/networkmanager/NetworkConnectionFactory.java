/*
 * Created on Feb 21, 2005
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

package com.aelitis.azureus.core.networkmanager;

import com.aelitis.azureus.core.networkmanager.impl.NetworkConnectionImpl;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

/**
 *
 */
public class NetworkConnectionFactory {
  
  /**
   * Create an OUTGOING connection.
   * @param remote_address
   * @param encoder
   * @param decoder
   * @return outbound connection
   */
  protected static NetworkConnection create( ConnectionEndpoint	target, MessageStreamEncoder encoder, MessageStreamDecoder decoder, boolean connect_with_crypto, boolean allow_fallback, byte[][] shared_secrets ) {
    return new NetworkConnectionImpl( target, encoder, decoder, connect_with_crypto, allow_fallback, shared_secrets );
  }

  
  /**
   * Create an INCOMING connection.
   * @param remote_channel
   * @param data_already_read
   * @param encoder
   * @param decoder
   * @return inbound connection
   */
  protected static NetworkConnection create( Transport transport, MessageStreamEncoder encoder, MessageStreamDecoder decoder ) {
    return new NetworkConnectionImpl( transport, encoder, decoder );
  }
}
