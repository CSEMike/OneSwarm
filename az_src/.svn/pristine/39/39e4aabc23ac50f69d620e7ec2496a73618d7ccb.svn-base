/*
 * File    : SuperSeedPeer.java
 * Created : 14 déc. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.peer.impl.control;

import org.gudy.azureus2.core3.peer.PEPeer;

/**
 * @author Olivier
 *
 */
public class SuperSeedPeer implements Comparable{

  public PEPeer peer;
  
  public SuperSeedPeer(PEPeer peer) {
    this.peer = peer;
  }
  
  public int compareTo(Object obj) {
    SuperSeedPeer otherPeer = (SuperSeedPeer) obj;
    return this.peer.getUploadHint() - otherPeer.peer.getUploadHint();
  }
}
