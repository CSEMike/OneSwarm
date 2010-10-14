/*
 * File    : PluginMyTorrentsItemFactory.java
 * Created : 29 nov. 2003
 * By      : Olivier
 * Modified from PluginPeersItemFactory by TuxPaper
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

/**
 * 
 * This interface represents the factory responsible of creating PluginMyTorrentsItem.<br>
 * It must also define some methods giving general information about the item. 
 * 
 * @author TuxPaper
 *
 * @deprecated Use {@link org.gudy.azureus2.plugins.ui.tables}
 */
public interface PluginMyTorrentsItemFactory {
  /**
   * The String type, used for ordering.
   */
  public static final String TYPE_STRING = "S";
  
  /**
   * The int type, used for ordering.
   */
  public static final String TYPE_INT = "I";
  
  /** 
   * Right orient the colmn items text 
   */
  public static final String ORIENT_RIGHT = "R";

  /** 
   * Left orient the colmn items text 
   */
  public static final String ORIENT_LEFT = "L";

  /**
   * For getDefaultPosition(). Make column invisible initially.
   */
  public static final int POSITION_INVISIBLE = -1;
  /**
   * For getDefaultPosition(). Make column the last column initially.
   */
  public static final int POSITION_LAST = -2;
  
  /**
   * Visible for Completed Torrents table
   */
  public static final int TABLE_COMPLETE = 1;
  /**
   * Visible for Incompleted Torrents table
   */
  public static final int TABLE_INCOMPLETE = 2;
  /**
   * Visible for all My Torrent tables
   */
  public static final int TABLE_ALL = 4;
  
  /**
   * The logical name of the column.<br>
   * Note that spaces in the name should be avoid.<br>
   * In order to the plugin to display correctly the column name, a key in the
   * Plugin language file will need to contain MyTorrentsView.<i>getName() result</i>=The column name.<br>
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
   * Default location to put the column
   * @return Column Number (0 based), or -1 for initially invisible
   */
  public int getDefaultPosition();
  
  /**
   * Orientation of the columns text
   * @return ORIENT_LEFT or ORIENT_RIGHT
   */
  public String getOrientation();
  
  /**
   * Which tables the column will be visible in
   * @return TABLE_COMPLETE, TABLE_INCOMPLETE or both
   */
  public int getTablesVisibleIn();

  /**
   * This method is called whenever a new line is created.
   * @param item the MyTorrentsTableItem that is being created
   * @return the PluginMyTorrentsItem you created
   */
  public PluginMyTorrentsItem getInstance(MyTorrentsTableItem item);
}
