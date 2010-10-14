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

package com.aelitis.azureus.core.peermanager.download.session.impl;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.session.*;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;



public class BTTorrentSession implements TorrentSession {
  private final TorrentSessionListener listener;
  private final AZPeerConnection peer;
  
  private final IncomingMessageQueue.MessageQueueListener incoming_q_listener = new IncomingMessageQueue.MessageQueueListener() {
    public boolean messageReceived( Message message ) {
      
      //ID_BT_BITFIELD
      if( message.getID().equals( BTMessage.ID_BT_BITFIELD ) ) {
        BTBitfield bitf = (BTBitfield)message;
        listener.receivedSessionBitfield( bitf.getBitfield() );
        return true;
      }

      //ID_BT_HAVE
      if( message.getID().equals( BTMessage.ID_BT_HAVE ) ) {
        BTHave have = (BTHave)message;
        listener.receivedSessionHave( have.getPieceNumber() );
        return true;
      }
      
      //ID_BT_REQUEST
      if( message.getID().equals( BTMessage.ID_BT_REQUEST ) ) {
        BTRequest req = (BTRequest)message;
        listener.receivedSessionRequest( (byte)-1, req.getPieceNumber(), req.getPieceOffset(), req.getLength() );
        return true;
      }
      
      //ID_BT_CANCEL
      if( message.getID().equals( BTMessage.ID_BT_CANCEL ) ) {
        BTCancel can = (BTCancel)message;
        listener.receivedSessionCancel( can.getPieceNumber(), can.getPieceOffset(), can.getLength() );
        return true;
      }
      
      //ID_BT_PIECE
      if( message.getID().equals( BTMessage.ID_BT_PIECE ) ) {
        BTPiece piece = (BTPiece)message;
        listener.receivedSessionPiece( piece.getPieceNumber(), piece.getPieceOffset(), piece.getPieceData() );
        return true;
      }

      return false;
    }
    
    public void protocolBytesReceived( int byte_count ) {}      
    public void dataBytesReceived( int byte_count ) {}
  };
  
  
  
  private final OutgoingMessageQueue.MessageQueueListener sent_message_listener = new OutgoingMessageQueue.MessageQueueListener() {
    public boolean messageAdded( Message message ) {   return true;   }
    
    public void messageSent( Message message ) {
      if( message.getID().equals( BTMessage.ID_BT_PIECE ) ) {
        listener.sentSessionPiece( message );
      }
    }
    public void messageQueued( Message message ) {/*nothing*/}
    public void messageRemoved( Message message ) {/*nothing*/}
    public void protocolBytesSent( int byte_count ) {/*ignore*/}
    public void dataBytesSent( int byte_count ) {/*ignore*/}
    public void flush(){}
  };
  
  
  
  public BTTorrentSession( AZPeerConnection peer, TorrentSessionListener listener ) {
    this.peer = peer;
    this.listener = listener;
    
    peer.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( incoming_q_listener );
    peer.getNetworkConnection().getOutgoingMessageQueue().registerQueueListener( sent_message_listener );
    
    listener.sessionIsEstablished();  //notify of readiness
  }
  
  
  
  public void sendSessionBitfield( DirectByteBuffer bitfield ) {
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( new BTBitfield( bitfield, (byte)1 ), false );
  }
  

  public void sendSessionRequest( byte unchoke_id, int piece_number, int piece_offset, int length ) {
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( new BTRequest( piece_number, piece_offset, length, (byte)1 ), false );
  }
  

  public void sendSessionCancel( int piece_number, int piece_offset, int length ) {
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( new BTCancel( piece_number, piece_offset, length, (byte)1 ), false );
  }
  

  public void sendSessionHave( int[] piece_numbers ) {
    for( int i=0; i < piece_numbers.length; i++ ) { 
      peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( new BTHave( piece_numbers[i], (byte)1 ), false );
    }
  }


  public Object sendSessionPiece( int piece_number, int piece_offset, DirectByteBuffer data ) {
    BTPiece piece = new BTPiece( piece_number, piece_offset, data, (byte)1 );
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( piece, false );
    return piece;
  }

  
  public void endSession( String reason ) {
    peer.getNetworkConnection().getIncomingMessageQueue().cancelQueueListener( incoming_q_listener );
    peer.getNetworkConnection().getOutgoingMessageQueue().cancelQueueListener( sent_message_listener );
  }
  
}
