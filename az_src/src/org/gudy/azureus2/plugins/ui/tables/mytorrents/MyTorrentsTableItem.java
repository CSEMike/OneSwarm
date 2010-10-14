/*
 * File    : MyTorrentsTableItem.java
 * Created : 29 nov. 2003
 * By      : Olivier
 * Adapted to MyTorrents by TuxPaper 2004/02/16
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
 
package org.gudy.azureus2.plugins.ui.tables.mytorrents;

import org.gudy.azureus2.plugins.download.Download;

/**
 * This interface provides access to a Table Item in the My Torrents View.<br>
 * 
 * @deprecated Use {@link org.gudy.azureus2.plugins.ui.tables.TableCell}
 */
public interface MyTorrentsTableItem {
  
  /**
   * This method MUST be used on each refresh, and NO CACHING of the object
   * should be made by the Plugin. There is no link between a Table Item and
   * a Download as the peer may change, for example when the table is re-ordered.
   * @return the current PEPeer associated with this Item (row)
   */
  public Download getDownload();
  
  /**
   * This method can be called to set the Text in the Table Item.
   * Caching is done, so that if same text is used several times,
   * there won't be any 'flickering' effect. Ie the text is only updated if
   * it's different from current value.
   * @param text the text to be set
   * @return true if the text was updated, false if not.
   */
  public boolean setText(String text);
}
