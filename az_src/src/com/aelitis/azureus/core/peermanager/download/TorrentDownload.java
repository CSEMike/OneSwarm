/*
 * Created on Jul 10, 2005
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

package com.aelitis.azureus.core.peermanager.download;

import org.gudy.azureus2.core3.peer.impl.PEPeerControl;

import com.aelitis.azureus.core.peermanager.download.session.*;
import com.aelitis.azureus.core.peermanager.download.session.auth.StandardAuthenticator;


public class TorrentDownload {
  private final PEPeerControl legacy_peer_manager;
  private TorrentSessionAuthenticator session_auth = new StandardAuthenticator();  //default to standard auth
  
  
  protected TorrentDownload( PEPeerControl legacy_peer_manager ) {
    this.legacy_peer_manager = legacy_peer_manager;
    TorrentSessionManager.getSingleton().registerForSessionManagement( this );  //register for incoming session requests
  }
  
  
  public PEPeerControl getLegacyPeerManager() {  return legacy_peer_manager;  }
  
  
  public byte[] getInfoHash() {  return legacy_peer_manager.getHash();  }
  
  
  
  public void setSessionAuthenticator( TorrentSessionAuthenticator auth ) {  session_auth = auth;  }
  
  public TorrentSessionAuthenticator getSessionAuthenticator() {  return session_auth;  }
  
  
  
  
  public void registerTorrentSession( TorrentSession session ) {

  }
  
  
  public void deregisterTorrentSession( TorrentSession session ) {

  }
  
  
  
  
  
  
  public void destroy() {
    TorrentSessionManager.getSingleton().deregisterForSessionManagement( this );
  }
  
}
