/*
 * Created on 20-Oct-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.peer.util;

import java.util.*;


public class 
PeerIdentityDataID 
{
    private final byte[] dataId;
    private final int hashcode;
    
    private Map		peer_map;
    
    protected 
	PeerIdentityDataID( 
		byte[] _data_id ) 
    {
      dataId 	= _data_id;
      
      this.hashcode = new String( dataId ).hashCode();
    }
    
    public byte[]
	getDataID()
    {
    	return( dataId );
    }
    
    protected Map
	getPeerMap()
    {
    	return( peer_map );
    }

    protected void
	setPeerMap(
		Map	m )
    {
    	peer_map	= m;
    }
    
    public boolean equals( Object obj ) {
      if (this == obj)  return true;
      if (obj != null && obj instanceof PeerIdentityDataID) {
        PeerIdentityDataID other = (PeerIdentityDataID)obj;
        return Arrays.equals(this.dataId, other.dataId);
      }
      return false;
    }
    
    public int hashCode() {
      return hashcode;
    }
  }