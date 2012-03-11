/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytracker;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class StatusItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public StatusItem() {
    super("status", POSITION_LAST, 60, TableManager.TABLE_MYTRACKER);
    setRefreshInterval(INTERVAL_LIVE);
  }


  public void refresh(TableCell cell) {
    TRHostTorrent item = (TRHostTorrent)cell.getDataSource();

    String status_text = "";
    
    if( item != null ) {
      int status = item.getStatus();
      
      if( !cell.setSortValue( status ) && cell.isValid() ) {
        return;
      }
      
      if (status == TRHostTorrent.TS_STARTED) {
        status_text = MessageText.getString( "MyTrackerView.status.started" );
      }
      else if (status == TRHostTorrent.TS_STOPPED) {
        status_text = MessageText.getString( "MyTrackerView.status.stopped" );
      }
      else if (status == TRHostTorrent.TS_FAILED) {
        status_text = MessageText.getString( "MyTrackerView.status.failed" );
      }
      else if (status == TRHostTorrent.TS_PUBLISHED) {
        status_text = MessageText.getString( "MyTrackerView.status.published" );
      }
    }
    
    cell.setText( status_text );
  }
}
