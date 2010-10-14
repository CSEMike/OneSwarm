/*
 * Created on Jul 3, 2005
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

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.*;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.session.*;



public class TorrentSessionManager {
  
  private static final TorrentSessionManager instance = new TorrentSessionManager();
  
  private final HashMap hashes = new HashMap();
  private final AEMonitor hashes_mon = new AEMonitor( "TorrentSessionManager" );
  

  public static TorrentSessionManager getSingleton(){  return instance;  }

  
  private TorrentSessionManager() {
    /*nothing*/
  }
  
  
  public void init() {
    //register for new peer connection creation notification, so that we can catch torrent session syn messages
    PeerConnectionFactory.getSingleton().registerCreationListener( new PeerConnectionFactory.CreationListener() {
      public void connectionCreated( final AZPeerConnection connection ) {
        connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( new IncomingMessageQueue.MessageQueueListener() {
          public boolean messageReceived( Message message ) {
            if( message.getID().equals( AZMessage.ID_AZ_SESSION_SYN ) ) {
              AZSessionSyn syn = (AZSessionSyn)message;

              byte[] hash = syn.getInfoHash();

              //check for valid session infohash
              TorrentDownload download = null;
                
              try{ hashes_mon.enter();
                download = (TorrentDownload)hashes.get( new HashWrapper( hash ) );
              }
              finally{ hashes_mon.exit();  }
                
              if( download == null ) {
                System.out.println( "unknown session infohash " +ByteFormatter.nicePrint( hash, true ) );
                AZSessionEnd end = new AZSessionEnd( hash, "unknown session infohash", (byte)1 );
                connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
              }
              else { //success
                //TODO
                //TorrentSession session = TorrentSessionFactory.getSingleton().createIncomingSession( download, connection, syn.getSessionID() );
                //session.authenticate( syn.getSessionInfo() );  //init processing //TODO
              }
               
              syn.destroy();
              return true;
            }
            
            return false;
          }

          public void protocolBytesReceived( int byte_count ){}
          public void dataBytesReceived( int byte_count ){}
        });
      }
    });
  }
  
    
  
  /**
   * Register the given download for torrent session management.
   * @param download to add
   */
  public void registerForSessionManagement( TorrentDownload download ) {
    try{ hashes_mon.enter();
      hashes.put( new HashWrapper( download.getInfoHash() ), download );
    }
    finally{ hashes_mon.exit();  }
  }
  
  
  /**
   * Deregister the given download from torrent session management.
   * @param download to remove
   */
  public void deregisterForSessionManagement( TorrentDownload download ) {
    try{ hashes_mon.enter();
      hashes.remove( new HashWrapper( download.getInfoHash() ) );
    }
    finally{ hashes_mon.exit();  }
  }
  
  
  /**
   * Initiate a torrent session for the given download with the given peer connection.
   * @param download for session
   * @param connection to send request to
   */
  public void requestTorrentSession( TorrentDownload download, AZPeerConnection connection ) {
    //TorrentSession session = TorrentSessionFactory.getSingleton().createOutgoingSession( download, connection );
    //session.authenticate( null );  //init processing  //TODO
  }

  
}
