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

import java.util.Map;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.session.impl.*;


public class TorrentSessionController {
  protected static final int SESSION_TYPE_BT = 0;
  protected static final int SESSION_TYPE_AZ = 1;
  
  private final TorrentDownload download;
  private final TorrentSession session;
  
  
  private final TorrentSessionListener listener = new TorrentSessionListener() {
    public void sessionIsEstablished() {
//    TODO
    }
    
    public void receivedSessionBitfield( DirectByteBuffer bitfield ){
//    TODO
    }

    public void receivedSessionRequest( byte unchoke_id, int piece_number, int piece_offset, int length ) {
//    TODO
    }
    
    public void receivedSessionCancel( int piece_number, int piece_offset, int length ) {
//    TODO
    }

    public void receivedSessionHave( int piece_number ) {
//    TODO
    }

    public void receivedSessionPiece( int piece_number, int piece_offset, DirectByteBuffer data ) {
//    TODO
    }

    public void sentSessionPiece( Object piece_key ) {
//    TODO
    }
    
    public void sessionIsEnded( String reason ) {
//    TODO
    }
  };
  
  
  
  
  protected TorrentSessionController( int session_type, TorrentDownload download, AZPeerConnection peer, int remote_id, Map incoming_syn ) {
    this.download = download;
    
    if( session_type == SESSION_TYPE_AZ ) {
      session = new AZTorrentSession( download, peer, listener, remote_id, incoming_syn );
    }
    else {  //SESSION_TYPE_BT
      session = new BTTorrentSession( peer, listener );
    }
  }
  
  
  
  

}
