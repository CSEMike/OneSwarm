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
 
package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/** File's First Piece # 
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class FirstPieceItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public FirstPieceItem() {
    super("firstpiece", ALIGN_TRAIL, POSITION_LAST, 75, TableManager.TABLE_TORRENT_FILES);
    setMinWidthAuto(true);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_PROTOCOL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_ADVANCED);
	}

  public void refresh(TableCell cell) {
    DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
    long sort_value;
    
    if ( fileInfo == null ){
    	sort_value = 0;
    }else{
    	sort_value = fileInfo.getFirstPieceNumber();
    	
    	if ( sort_value >= 0 ){
    		
    		sort_value = (sort_value << 32) + fileInfo.getIndex();
    	}
    }

    
    if( !cell.setSortValue( sort_value ) && cell.isValid() ) {
      return;
    }
    
		// < 0 -> unknown skeleton value 
	
    cell.setText( sort_value<0?"":(""+fileInfo.getFirstPieceNumber()));
  }
}
