/*
 * File    : PluginItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.MyTorrentsTableItem;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItem;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;

import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;

/** Cell/Factory to support old style Plugin columns
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 * @deprecated
 */
// Link the old PluginMyTorrentsItemFactory to the new generic stuff
public class OldMyTorrentsPluginItem
       extends TableColumnImpl
       implements TableCellAddedListener
{
  private static final LogIDs LOGID = LogIDs.GUI;
  private PluginMyTorrentsItemFactory oldFactory;
  private String oldFactoryType;

  public OldMyTorrentsPluginItem(String sTableID, String sCellName, 
                                 PluginMyTorrentsItemFactory item) {
    super(sTableID, sCellName);
    oldFactory = item;
    oldFactoryType = oldFactory.getType();
    addCellAddedListener(this);
    setRefreshInterval(TableColumn.INTERVAL_LIVE);
  }
  
  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener, MyTorrentsTableItem
  {
    PluginMyTorrentsItem pluginItem;
    TableCell cell;
  
    public Cell(TableCell item) {
      cell = item;
      pluginItem = OldMyTorrentsPluginItem.this.oldFactory.getInstance(this);

      // listener is disposed of in core when cell is removed
			cell.addListeners(this);
    }
    
    public Download getDownload() {
      return (Download)cell.getDataSource();
    }
    
    public void refresh(TableCell cell) {
      try {
        if (cell.isShown()) {
          pluginItem.refresh();

          if (oldFactoryType.equals(PluginMyTorrentsItemFactory.TYPE_STRING))
            cell.setSortValue(pluginItem.getStringValue());
          else
            cell.setSortValue(pluginItem.getIntValue());
        }
      } catch(Throwable e) {
      	Logger.log(new LogEvent(LOGID, "Plugin in MyTorrentsView "
						+ "generated an exception", e));
      }
    }
    
    public boolean setText(String s) {
      return cell.setText(s);
    }
  }
}
