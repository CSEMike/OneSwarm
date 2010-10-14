/*
 * File    : PluginPeerItemFactory.java
 * Created : 29 nov. 2003
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
 
package org.gudy.azureus2.plugins.ui.tables.peers;

/**
 * 
 * This interface represents the factory responsible of creating PluginPeerItem.<br>
 * It must also define some methods giving general information about the item. 
 * 
 * @author Olivier
 *
 * @deprecated Use {@link org.gudy.azureus2.plugins.ui.tables}
 */
public interface PluginPeerItemFactory {
  /**
   * The String type, used for ordering.
   */
  public static final String TYPE_STRING = "S";
  
  /**
   * The int type, used for ordering.
   */
  public static final String TYPE_INT = "I";
  
  /**
   * The logical name of the column.<br>
   * Note that spaces in the name should be avoid.<br>
   * In order to the plugin to display correctly the column name, a key in the
   * Plugin language file will need to contain PeersView.&lt;getName() result&gt;=The column name.<br>
   * @return the column name (identification)
   */
  public String getName();
  
  /**
   * The type of the contained data.<br>
   * Current supported types are int / long (TYPE_INTEGER) and
   * String TYPE_STRING.
   * @return TYPE_STRING or TYPE_INT
   */
  public String getType();
  
  /**
   * The 'column' default size 
   * @return the size in pixels
   */
  public int getDefaultSize();

  /**
   * This method is called whenever a new line is created.
   * @param item the PeerTableItem that is being created
   * @return the PluginPeerItem that will have to deal with it
   */
  public PluginPeerItem getInstance(PeerTableItem item);
}
