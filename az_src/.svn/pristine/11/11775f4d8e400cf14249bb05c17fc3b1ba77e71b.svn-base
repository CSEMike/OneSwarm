/*
 * File    : SizeItem.java
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

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.HSLColor;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import com.aelitis.azureus.ui.common.table.TableCellCore;

/** Size of Torrent cell
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class SizeItem
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "size";

	private static boolean DO_MULTILINE = true;

	/** Default Constructor */
	public SizeItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 70, sTableID);
		addDataSourceType(DiskManagerFileInfo.class);
		setRefreshInterval(INTERVAL_GRAPHIC);
		setMinWidthAuto(true);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
			CAT_CONTENT,
			CAT_BYTES
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void refresh(TableCell cell) {
		Object ds = cell.getDataSource();
		sizeitemsort value;
		if (ds instanceof DownloadManager) {
			DownloadManager dm = (DownloadManager) ds;
			
			value = new sizeitemsort(dm.getSize(), dm.getStats().getRemaining());
		} else if (ds instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
			value = new sizeitemsort(fileInfo.getLength(), fileInfo.getLength()
					- fileInfo.getDownloaded());
		} else {
			return;
		}

		// cell.setSortValue(value) always returns true and if I change it,
		// I'm afraid something will break.. so use compareTo
		if (value.compareTo(cell.getSortValue()) == 0 && cell.isValid())
			return;

		cell.setSortValue(value);

		String s = DisplayFormatters.formatByteCountToKiBEtc(value.size);

		if (DO_MULTILINE && cell.getMaxLines() > 1 && value.remaining > 0) {
			s += "\n"
					+ DisplayFormatters.formatByteCountToKiBEtc(value.remaining, false,
							false, 0) + " to go";
		}
		cell.setText(s);
		if (Utils.getUserMode() > 0 && (cell instanceof TableCellSWT)) {
			if (value.size >= 0x40000000l) {
				((TableCellSWT)cell).setTextAlpha(200 | 0x100);
			} else if (value.size < 0x100000) {
				((TableCellSWT)cell).setTextAlpha(180);
			} else {
				((TableCellSWT)cell).setTextAlpha(255);
			}
		}
	}

	private class sizeitemsort
		implements Comparable
	{
		private final long size;

		private final long remaining;

		public sizeitemsort(long size, long remaining) {
			this.size = size;
			this.remaining = remaining;
		}

		public int compareTo(Object arg0) {
			if (!(arg0 instanceof sizeitemsort)) {
				return 1;
			}

			sizeitemsort otherObj = (sizeitemsort) arg0;
			if (size == otherObj.size) {
				return remaining == otherObj.remaining ? 0
						: remaining > otherObj.remaining ? 1 : -1;
			}
			return size > otherObj.size ? 1 : -1;
		}
	}
}
