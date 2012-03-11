/*
 * File    : CommentItem.java
 * Created : 26 Oct 2006
 * By      : Allan Crooks
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
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * User-editable comment for a download.
 *
 * @author amc1
 */
public class CommentItem
       extends CoreTableColumn 
       implements TableCellRefreshListener, TableCellMouseListener, ObfusticateCellText
{
	public static final Class DATASOURCE_TYPE = Download.class;

  public static final String COLUMN_ID = "comment";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT });
	}

	/** Default Constructor */
  public CommentItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, 300, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
    setType(TableColumn.TYPE_TEXT);
    setObfustication(true);
    setMinWidth(50);
  }

  public void refresh(TableCell cell) {
    String comment = null;
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    comment = dm.getDownloadState().getUserComment();
    if (comment != null) {
    	comment = comment.replace('\r', ' ').replace('\n', ' ');
    }
    cell.setText((comment == null) ? "" : comment);
  }
  
	public void cellMouseTrigger(TableCellMouseEvent event) {
		DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		if (dm == null) {return;}
		
		event.skipCoreFunctionality = true;
		if (event.eventType != TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK) {return;}
		TorrentUtil.promptUserForComment(new DownloadManager[] {dm});
	}
	
	public String getObfusticatedText(TableCell cell) {
		DownloadManager dm = (DownloadManager)cell.getDataSource();
		return Integer.toHexString(dm.hashCode());
	}

  
}
