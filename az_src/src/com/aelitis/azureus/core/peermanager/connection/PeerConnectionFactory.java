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

package com.aelitis.azureus.core.peermanager.connection;

import java.util.ArrayList;

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;

public class PeerConnectionFactory {

  private static final PeerConnectionFactory instance = new PeerConnectionFactory();
  
  private volatile ArrayList creation_listeners_cow = new ArrayList(); // Copy on write!
  private AEMonitor creation_listeners_mon = new AEMonitor( "PeerConnectionFactory");
  
  
  
  public static PeerConnectionFactory getSingleton() {  return instance;  }
  

  public AZPeerConnection createAZPeerConnection( PeerItem peer_identity, NetworkConnection connection ) {
    AZPeerConnection conn = new AZPeerConnection( peer_identity, connection );
    
    ArrayList listeners = creation_listeners_cow;
    for( int i=0; i < listeners.size(); i++ ) {
      CreationListener listener = (CreationListener)listeners.get( i );
      listener.connectionCreated( conn );
    }
    
    return conn;
  }

  
  
  public void registerCreationListener( CreationListener listener ) {
    try{  creation_listeners_mon.enter();
      ArrayList newlist = new ArrayList( creation_listeners_cow.size() + 1 );
      newlist.addAll( creation_listeners_cow );
      newlist.add( listener );
      creation_listeners_cow = newlist;
    }
    finally{  creation_listeners_mon.exit();  }
  }
  
  
  
  public void deregisterCreationListener( CreationListener listener ) {
    try{  creation_listeners_mon.enter();
      ArrayList newlist = new ArrayList( creation_listeners_cow );
      newlist.remove( listener );
      creation_listeners_cow = newlist;
    }
    finally{  creation_listeners_mon.exit();  }
  }
  
  
  
  
  public interface CreationListener {
    public void connectionCreated( AZPeerConnection connection );
  }
}
