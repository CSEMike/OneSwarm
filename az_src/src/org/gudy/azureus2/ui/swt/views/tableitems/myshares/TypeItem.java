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
 
package org.gudy.azureus2.ui.swt.views.tableitems.myshares;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class TypeItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public TypeItem() {
    super("type", POSITION_LAST, 100, TableManager.TABLE_MYSHARES);
  }

  public void refresh(TableCell cell) {
    ShareResource item = (ShareResource)cell.getDataSource();
    
    String text = "";
    
    if( item != null ) {
      int type = item.getType();
      
      if( !cell.setSortValue( type ) && cell.isValid() ) {
        return;
      }
      
      if (type == ShareResource.ST_DIR)
        text = MessageText.getString( "MySharesView.type.dir" );
      else if (type == ShareResource.ST_FILE)
        text = MessageText.getString( "MySharesView.type.file" );
      else if (type == ShareResource.ST_DIR_CONTENTS) {
        ShareResourceDirContents s = (ShareResourceDirContents)item;
        if (s.isRecursive())
          text = MessageText.getString( "MySharesView.type.dircontentsrecursive" );
        else
          text = MessageText.getString( "MySharesView.type.dircontents" );
      }
    }
    
    cell.setText( text );
  }
}
