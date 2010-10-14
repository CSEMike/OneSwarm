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

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.session.*;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;
import com.aelitis.azureus.core.peermanager.messaging.azureus.session.*;


public class AZTorrentSession implements TorrentSession {
  private static int next_session_id = 0;
  private static final AEMonitor session_mon = new AEMonitor( "TorrentSession" );

  private int local_session_id;
  private int remote_session_id;
  private final TorrentDownload download;
  private final AZPeerConnection peer;
  private final TorrentSessionListener listener;
  private TimerEvent syn_timeout_timer;

  
  private final IncomingMessageQueue.MessageQueueListener incoming_q_listener = new IncomingMessageQueue.MessageQueueListener() {
    public boolean messageReceived( Message message ) {
      //ID_AZ_SESSION_ACK
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_ACK ) ) {          
        AZSessionAck ack = (AZSessionAck)message;
        if( Arrays.equals( ack.getInfoHash(), download.getInfoHash() ) ) {
          remote_session_id = ack.getSessionID();  //capture send-to id
          syn_timeout_timer.cancel();  //abort timeout check
          try{
            download.getSessionAuthenticator().verifySessionAck( peer, ack.getSessionInfo() );
            listener.sessionIsEstablished();  //notify of readiness
          }
          catch( AuthenticatorException ae ) {
            end( "AuthenticatorException:: " +ae.getMessage(), true );  //send end message
          }
          ack.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_END
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_END ) ) {          
        AZSessionEnd end = (AZSessionEnd)message;
        if( Arrays.equals( end.getInfoHash(), download.getInfoHash() ) ) {
          System.out.println( "AZ_TORRENT_SESSION_END received: " +end.getEndReason() );
          listener.sessionIsEnded( end.getEndReason() );  //notify of remote termination
          destroy();  //close session       
          end.destroy();
          return true;
        } 
      }
      
      //ID_AZ_SESSION_BITFIELD
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_BITFIELD ) ) {          
        AZSessionBitfield bitf = (AZSessionBitfield)message;
        if( bitf.getSessionID() == local_session_id ) {
          listener.receivedSessionBitfield( bitf.getBitfield() );       
          bitf.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_REQUEST
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_REQUEST ) ) {          
        AZSessionRequest req = (AZSessionRequest)message;
        if( req.getSessionID() == local_session_id ) {
          listener.receivedSessionRequest( req.getUnchokeID(), req.getPieceNumber(), req.getPieceOffset(), req.getLength() );
          req.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_CANCEL
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_CANCEL ) ) {          
        AZSessionCancel cancel = (AZSessionCancel)message;
        if( cancel.getSessionID() == local_session_id ) {
          listener.receivedSessionCancel( cancel.getPieceNumber(), cancel.getPieceOffset(), cancel.getLength() );
          cancel.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_HAVE
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_HAVE ) ) {          
        AZSessionHave have = (AZSessionHave)message;
        if( have.getSessionID() == local_session_id ) {
          int[] piecenums = have.getPieceNumbers();
          for( int i=0; i < piecenums.length; i++ ) {
            listener.receivedSessionHave( piecenums[i] );
          }     
          have.destroy();
          return true;
        }
      }
      
      //ID_AZ_SESSION_PIECE
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_PIECE ) ) {          
        AZSessionPiece piece = (AZSessionPiece)message;
        if( piece.getSessionID() == local_session_id ) {
          try{
            DirectByteBuffer data = download.getSessionAuthenticator().decodeSessionData( peer, piece.getPieceData() );
            listener.receivedSessionPiece( piece.getPieceNumber(), piece.getPieceOffset(), data );
          }
          catch( AuthenticatorException ae ) {
            piece.getPieceData().returnToPool();
            end( "AuthenticatorException:: " +ae.getMessage(), true );  //send end message
          }
          piece.destroy();
          return true;
        }
      }

      return false;
    }

    public void protocolBytesReceived( int byte_count ){}
    public void dataBytesReceived( int byte_count ){}
  };
  
  
  
  private final OutgoingMessageQueue.MessageQueueListener sent_message_listener = new OutgoingMessageQueue.MessageQueueListener() {
    public boolean messageAdded( Message message ) {   return true;   }
    
    public void messageSent( Message message ) {
      if( message.getID().equals( AZMessage.ID_AZ_SESSION_PIECE ) ) {          
        AZSessionPiece piece = (AZSessionPiece)message;
        if( piece.getSessionID() == remote_session_id ) {
          listener.sentSessionPiece( message );
        }
      }
    }
    public void messageQueued( Message message ) {/*nothing*/}
    public void messageRemoved( Message message ) {/*nothing*/}
    public void protocolBytesSent( int byte_count ) {/*ignore*/}
    public void dataBytesSent( int byte_count ) {/*ignore*/}
    public void flush(){}
  };
  
  
  

  public AZTorrentSession( TorrentDownload download, AZPeerConnection peer, TorrentSessionListener listener, int remote_id, Map incoming_syn ) {
    this.download = download;
    this.peer = peer;
    this.remote_session_id = remote_id;
    this.listener = listener;
    
    try{ session_mon.enter();
      local_session_id = next_session_id;
      next_session_id++;
    }
    finally{ session_mon.exit();  }
    
    peer.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( incoming_q_listener );
    peer.getNetworkConnection().getOutgoingMessageQueue().registerQueueListener( sent_message_listener );
    
    authenticate( incoming_syn );
  }
  


  private void authenticate( Map incoming_syn ) {
    if( incoming_syn == null ) {  //outgoing session
      //send out the session request
      AZSessionSyn syn = new AZSessionSyn( download.getInfoHash(), local_session_id, download.getSessionAuthenticator().createSessionSyn( peer ), (byte)1 );
      peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( syn, false );
      
      //set a timeout timer in case the other peer forgets to send an ACK or END reply
      syn_timeout_timer = SimpleTimer.addEvent( "AZTorrentSession:SynTImer", SystemTime.getCurrentTime() + 60*1000, new TimerEventPerformer() {
        public void  perform( TimerEvent  event ) {
          end( "No session ACK received after 60sec, request timed out.", true );
        }
      });
    }
    else {  //incoming session
      try{
        Map ack_reply = download.getSessionAuthenticator().verifySessionSyn( peer, incoming_syn );
        
        //send out the session acceptance
        AZSessionAck ack = new AZSessionAck( download.getInfoHash(), local_session_id, ack_reply, (byte)1 );
        peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( ack, false );
        
        listener.sessionIsEstablished();  //notify of readiness
      }
      catch( AuthenticatorException ae ) {  //on syn authentication error
        end( "AuthenticatorException:: " +ae.getMessage(), true );  //send end message
      }      
    }
  }
  
  

  private void end( String end_reason, boolean notify_listener ){
    System.out.println( "endSession:: " +end_reason );
    
    //send end notice to remote peer
    AZSessionEnd end = new AZSessionEnd( download.getInfoHash(), end_reason, (byte)1 );
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
    
    if( notify_listener ) {
      listener.sessionIsEnded( end_reason );  //notify listener of termination
    }
    
    destroy();
  }

  
  
  private void destroy(){
    if( syn_timeout_timer != null )  syn_timeout_timer.cancel();  //abort timeout check if running
    peer.getNetworkConnection().getIncomingMessageQueue().cancelQueueListener( incoming_q_listener );
    peer.getNetworkConnection().getOutgoingMessageQueue().cancelQueueListener( sent_message_listener );
  }
  
  
  public void endSession( String reason ) {
    end( reason, false );
  }
  


  public void sendSessionBitfield( DirectByteBuffer bitfield ) {
    AZSessionBitfield bitf = new AZSessionBitfield( remote_session_id, bitfield, (byte)1 );
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( bitf, false );
  }
  


  public void sendSessionRequest( byte unchoke_id, int piece_number, int piece_offset, int length ) {
    AZSessionRequest req = new AZSessionRequest( remote_session_id, unchoke_id, piece_number, piece_offset, length, (byte)1 );
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( req, false );
  }
  


  public void sendSessionCancel( int piece_number, int piece_offset, int length ) {
    AZSessionCancel can = new AZSessionCancel( remote_session_id, piece_number, piece_offset, length, (byte)1 );
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( can, false );
  }
  
 


  public void sendSessionHave( int[] piece_numbers ) {
    AZSessionHave have = new AZSessionHave( remote_session_id, piece_numbers, (byte)1 );
    peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( have, false );
  }
  

  public Object sendSessionPiece( int piece_number, int piece_offset, DirectByteBuffer data ) {
    try{
      DirectByteBuffer encoded = download.getSessionAuthenticator().encodeSessionData( peer, data );
      AZSessionPiece piece = new AZSessionPiece( remote_session_id, piece_number, piece_offset, encoded, (byte)1 );
      peer.getNetworkConnection().getOutgoingMessageQueue().addMessage( piece, false );
      return piece;
    }
    catch( AuthenticatorException ae ) {
      data.returnToPool();
      end( "AuthenticatorException:: " +ae.getMessage(), true );  //send end message
      return null;
    }
  }
}
