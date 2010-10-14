/*
 * Created on 05-May-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.io.File;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableRowCore;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;

public class DateAddedItem
	extends CoreTableColumn
	implements TableCellRefreshListener
{

	public DateAddedItem(String sTableID) {
		super("date_added", ALIGN_TRAIL, POSITION_INVISIBLE, 120, sTableID);

		TableContextMenuItem menuReset = addContextMenuItem("MyTorrentsView.date_added.menu.reset");
		menuReset.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof TableRowCore) {
					TableRowCore row = (TableRowCore) target;
					Object dataSource = row.getDataSource(true);
					if (dataSource instanceof DownloadManager) {
						DownloadManager dm = (DownloadManager) dataSource;

						DownloadManagerState state = dm.getDownloadState();

						try {
							long add_time = new File(dm.getTorrentFileName()).lastModified();

							if (add_time >= 0) {
								state.setLongParameter(
										DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME, add_time);
							}

						} catch (Throwable e) {
						}
					}
					row.getTableCell("date_added").invalidate();
				}
			}
		});
	}

	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		long value = (dm == null) ? 0 : dm.getDownloadState().getLongParameter(
				DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);

		if (!cell.setSortValue(value) && cell.isValid())
			return;

		cell.setText(DisplayFormatters.formatDate(value));
	}
}
