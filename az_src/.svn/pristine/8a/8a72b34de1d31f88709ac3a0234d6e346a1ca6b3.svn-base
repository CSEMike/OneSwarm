/*
 * Created by Joseph Bridgewater
 * Created on Feb 05, 2006
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

package org.gudy.azureus2.ui.swt.views.tableitems.pieces;

import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * @author MjrTom
 * Shows if more requests can be made on the piece or not
 */
public class RequestedItem
    extends CoreTableColumn
    implements TableCellRefreshListener
{
    /** Default Constructor */
    public RequestedItem()
    {
        super("Requested", ALIGN_CENTER, POSITION_INVISIBLE, 20, TableManager.TABLE_TORRENT_PIECES);
        setRefreshInterval(INTERVAL_LIVE);
    }

  	public void fillTableColumnInfo(TableColumnInfo info) {
  		info.addCategories(new String[] {
  			CAT_SWARM,
  		});
  	}

    public void refresh(TableCell cell)
    {
        boolean value =false;
        final PEPiece pePiece =(PEPiece) cell.getDataSource();
        if (pePiece !=null)
        {
             value = pePiece.isRequested();
        }
        if (!cell.setSortValue(value ?1 :0) &&cell.isValid())
            return;
        cell.setText(value?"*" :"");
    }
}
