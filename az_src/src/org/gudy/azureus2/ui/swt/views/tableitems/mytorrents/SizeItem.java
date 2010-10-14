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

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/** Size of Torrent cell
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class SizeItem
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	public static String COLUMN_ID = "size";

	private static boolean DO_MULTILINE = true;

	/** Default Constructor */
	public SizeItem(String sTableID) {
		super(COLUMN_ID, ALIGN_TRAIL, POSITION_LAST, 70, sTableID);
		setRefreshInterval(INTERVAL_GRAPHIC);
		setMinWidthAuto(true);
	}

	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		sizeitemsort value = (dm == null) ? new sizeitemsort(0, 0)
				: new sizeitemsort(dm.getSize(), dm.getStats().getRemaining());

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
