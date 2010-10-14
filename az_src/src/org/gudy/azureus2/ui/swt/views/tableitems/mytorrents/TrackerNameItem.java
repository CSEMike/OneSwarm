/*
 * File    : TrackerStatusItem.java
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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.StringInterner;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * @author Olivier
 *
 */
public class TrackerNameItem
       extends CoreTableColumn 
       implements TableCellRefreshListener, TableCellToolTipListener
{
  public TrackerNameItem(String sTableID) {
    super("trackername", POSITION_INVISIBLE, 120, sTableID);
    setRefreshInterval(5);
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    String name = "";
    
    if( dm != null && dm.getTorrent() != null ) {
      String host = dm.getTorrent().getAnnounceURL().getHost();
      String[] parts = host.split( "\\." );
        
      int used = 0;
      for( int i = parts.length-1; i >= 0; i-- ) {
        if( used > 4 ) break; //don't use more than 4 segments
        String chunk = parts[ i ];
        if( used < 2 || chunk.length() < 11 ) {  //use first end two always, but trim out >10 chars (passkeys)
          if( used == 0 ) name = chunk;
          else name = chunk + "." + name;
          used++;
        }
        else break;
      }
      
      if(name.equals(host))
    	  name = host;
      else
    	  name = StringInterner.intern(name);
    }
        
    if (cell.setText(name) || !cell.isValid()) {
    	TrackerCellUtils.updateColor(cell, dm);
    }
  }

	public void cellHover(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		cell.setToolTip(TrackerCellUtils.getTooltipText(cell, dm));
	}

	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}
}
