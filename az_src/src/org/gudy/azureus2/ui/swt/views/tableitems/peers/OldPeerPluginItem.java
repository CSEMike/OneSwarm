/*
 * File    : OldPeerPluginItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.ui.tables.peers.*;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;

/** Cell/Factory to support old style Plugin columns
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 * @deprecated
 */
// Link the old PluginMyTorrentsItemFactory to the new generic stuff
public class OldPeerPluginItem
       extends TableColumnImpl
       implements TableCellAddedListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
  private PluginPeerItemFactory oldFactory;
  private String oldFactoryType;

  public OldPeerPluginItem(String sTableID, String sCellName, 
                           PluginPeerItemFactory item) {
    super(sTableID, sCellName);
    oldFactory = item;
    oldFactoryType = oldFactory.getType();
    addCellAddedListener(this);
    setRefreshInterval(TableColumn.INTERVAL_LIVE);
  }
  
  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  /**
   * @deprecated
   */
  private class Cell
          implements TableCellRefreshListener, PeerTableItem
  {
    PluginPeerItem pluginItem;
    TableCell cell;
    boolean bTextSet;
  
    public Cell(TableCell item) {
      cell = item;
      pluginItem = OldPeerPluginItem.this.oldFactory.getInstance(this);

      // listener is disposed of in core when cell is removed
			cell.addListeners(this);
    }
    
    public Peer getPeer() {
      return (Peer)cell.getDataSource();
    }
    
    public void refresh(TableCell cell) {
      try {
        if (cell.isShown()) {
          bTextSet = false;
          pluginItem.refresh();

          if (oldFactoryType.equals(PluginPeerItemFactory.TYPE_STRING)) {
            String s = pluginItem.getStringValue();
            cell.setSortValue(s);
            // Some plugins didn't think a refresh actually meant we needed new
            // text..
            if (!bTextSet && !cell.isValid())
              cell.setText(s);
          } else {
            int i = pluginItem.getIntValue();
            if (!bTextSet && !cell.isValid())
              cell.setText(String.valueOf(i));
            cell.setSortValue(i);
          }
        }
      } catch(Throwable e) {
      	Logger.log(new LogEvent(LOGID,
						"Plugin in PeersView generated an exception", e));
        Debug.printStackTrace( e );
      }
    }
    
    public boolean setText(String s) {
      bTextSet = true;
      return cell.setText(s);
    }
  }
}
