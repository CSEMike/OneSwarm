/*
 * Created on Jul 23, 2005
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.peermanager.download.session;

import org.gudy.azureus2.core3.util.DirectByteBuffer;


public interface TorrentSessionListener {
  /**
   * Notification that the session is fully established.
   */
  public void sessionIsEstablished();

  /**
   * Received the given session bitfield from the peer.
   * @param bitfield received
   */
  public void receivedSessionBitfield( DirectByteBuffer bitfield );
  
  /**
   * Received the given session piece request info from the peer.
   * @param unchoke_id given when we unchoked the peer
   * @param piece_number of request
   * @param piece_offset of request
   * @param length of requested chunk
   */
  public void receivedSessionRequest( byte unchoke_id, int piece_number, int piece_offset, int length );
  
  /**
   * Received the given session piece request cancel info from the peer.
   * @param piece_number of request
   * @param piece_offset of request
   * @param length of request
   */
  public void receivedSessionCancel( int piece_number, int piece_offset, int length );
  
  /**
   * Received the given session piece number have from the peer.
   * @param piece_number of have notify
   */
  public void receivedSessionHave( int piece_number );
  
  /**
   * Received the given requested session piece data chunk from the peer
   * @param piece_number of chunk
   * @param piece_offset of chunk
   * @param data of piece chunk
   */
  public void receivedSessionPiece( int piece_number, int piece_offset, DirectByteBuffer data );
  
  
  /**
   * The given session piece message has been sent to other peer.
   * @param piece_key representing session piece
   */
  public void sentSessionPiece( Object piece_key );
  
  /**
   * Notification that the session has been remotely/internally ended.
   * @param reason for session end
   */
  public void sessionIsEnded( String reason );
}
