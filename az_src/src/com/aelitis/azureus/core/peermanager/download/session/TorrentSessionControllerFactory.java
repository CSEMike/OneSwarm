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

import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;

public class TorrentSessionControllerFactory {

  private static final TorrentSessionControllerFactory instance = new TorrentSessionControllerFactory();
  
  
  protected static TorrentSessionControllerFactory getSingleton(){  return instance;  }
  
  
  public TorrentSessionController createInboundAZController( TorrentDownload download, AZPeerConnection peer, int remote_id, Map incoming_syn ) {
    return new TorrentSessionController( TorrentSessionController.SESSION_TYPE_AZ, download, peer, remote_id, incoming_syn );
  }
  
  
  public TorrentSessionController createOutboundAZController( TorrentDownload download, AZPeerConnection peer ) {
    return new TorrentSessionController( TorrentSessionController.SESSION_TYPE_AZ, download, peer, -1, null );
  }
  
  
  public TorrentSessionController createBTController( TorrentDownload download, AZPeerConnection peer ) {
    return new TorrentSessionController( TorrentSessionController.SESSION_TYPE_BT, download, peer, -1, null );
  }
  
}
