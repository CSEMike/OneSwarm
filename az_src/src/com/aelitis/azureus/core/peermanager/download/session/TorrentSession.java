/*
 * Created on Jul 5, 2005
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

import org.gudy.azureus2.core3.util.*;


public interface TorrentSession {

  /**
   * Send the given session bitfield to the peer.
   * @param bitfield to send
   */
  public void sendSessionBitfield( DirectByteBuffer bitfield );
  
  /**
   * Send the given session piece request info to the peer.
   * @param unchoke_id given when the peer unchoked us
   * @param piece_number of request
   * @param piece_offset of request
   * @param length of requested chunk
   */
  public void sendSessionRequest( byte unchoke_id, int piece_number, int piece_offset, int length );
  
  /**
   * Send the given session piece request cancel info to the peer.
   * @param piece_number of request
   * @param piece_offset of request
   * @param length of request
   */
  public void sendSessionCancel( int piece_number, int piece_offset, int length );
  
  /**
   * Send the given session piece number haves to the peer.
   * @param piece_numbers to notify have
   */
  public void sendSessionHave( int[] piece_numbers );

  /**
   * Send the given requested session piece data chunk to the peer
   * @param piece_number of chunk
   * @param piece_offset of chunk
   * @param data of piece chunk
   * @return piece key used to notify on completion of piece message send
   */
  public Object sendSessionPiece( int piece_number, int piece_offset, DirectByteBuffer data );
  
  /**
   * Terminate torrent session.
   * @param reason for ending the session
   */
  public void endSession( String reason );  
}
