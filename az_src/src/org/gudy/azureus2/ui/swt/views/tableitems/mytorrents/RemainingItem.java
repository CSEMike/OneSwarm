/*
 * File    : NameItem.java
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

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class RemainingItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public RemainingItem() {
    super("remaining", ALIGN_TRAIL, POSITION_INVISIBLE, 70, TableManager.TABLE_MYTORRENTS_INCOMPLETE);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
  }

    private boolean bLastValueEstimate = false;
  
  public void refresh(TableCell cell) {
    long lRemaining = getRemaining(cell);

    if( !cell.setSortValue( lRemaining ) && cell.isValid() ) {
      return;
    }
    
    if (bLastValueEstimate) {
      cell.setText("~ " + DisplayFormatters.formatByteCountToKiBEtc(lRemaining));
    } else {
      cell.setText(DisplayFormatters.formatByteCountToKiBEtc(lRemaining));
    }
  }

  private long getRemaining(TableCell cell) {
    DownloadManager manager = (DownloadManager)cell.getDataSource();
    if (manager == null)
      return 0;

   return( manager.getStats().getRemaining());
  }
}
