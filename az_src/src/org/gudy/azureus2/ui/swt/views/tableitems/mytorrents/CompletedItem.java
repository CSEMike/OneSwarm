/**
 * Copyright (C) 2008 Aelitis, All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros 8 Allee Lenotre, La Grille Royale,
 * 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

/**
 * @author Aaron Grunthal
 * @create 11.02.2008
 */
public class CompletedItem extends CoreTableColumn implements TableCellRefreshListener {
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "completed";

	/** Default Constructor */
	public CompletedItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 50, sTableID);
		setMinWidthAuto(true);
	}
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_PROGRESS,
		});
	}


	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null)
			return;
		
		TRTrackerScraperResponse resp = dm.getTrackerScrapeResponse();
		if (resp == null)
			return;
		
		int completed = resp.getCompleted();
		if(cell.setSortValue(completed) || !cell.isValid())
			cell.setText(completed == -1 ? "?" : Integer.toString(completed));
	}
}
