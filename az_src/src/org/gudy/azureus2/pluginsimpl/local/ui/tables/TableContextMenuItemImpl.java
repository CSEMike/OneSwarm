/*
 * Azureus - a Java Bittorrent client
 * 2004/May/16 TuxPaper
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
 
package org.gudy.azureus2.pluginsimpl.local.ui.tables;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuItemImpl;

public class TableContextMenuItemImpl extends MenuItemImpl implements TableContextMenuItem {

  private String sTableID;
  
  public TableContextMenuItemImpl(PluginInterface pi, String tableID, String key) {
	  super(pi, MenuManager.MENU_TABLE, key);
	  sTableID = tableID;
  }
  
  public TableContextMenuItemImpl(TableContextMenuItemImpl ti, String key) {
	  super(ti, key);
	  this.sTableID = ti.getTableID();
  }

  public String getTableID() {
    return sTableID;
  }

  public void remove() {
	  removeWithEvents(UIManagerEvent.ET_REMOVE_TABLE_CONTEXT_MENU_ITEM,
				UIManagerEvent.ET_REMOVE_TABLE_CONTEXT_SUBMENU_ITEM);
	}
  
}