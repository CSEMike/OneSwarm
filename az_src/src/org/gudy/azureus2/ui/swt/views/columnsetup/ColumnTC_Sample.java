/**
 * Created on Jan 3, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.views.columnsetup;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.columnsetup.TableColumnSetupWindow.TableViewColumnSetup;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jan 3, 2009
 *
 */
public class ColumnTC_Sample
	extends CoreTableColumn
	implements TableCellAddedListener
{
	public static final String COLUMN_ID = "TableColumnSample";

	public ColumnTC_Sample(String tableID) {
		super(COLUMN_ID, tableID);
		setPosition(POSITION_INVISIBLE);
		setRefreshInterval(INTERVAL_LIVE);
		setWidth(120);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(final TableCell cell) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				TableColumnCore column = (TableColumnCore) cell.getDataSource();
				TableViewColumnSetup tv = (TableViewColumnSetup) ((TableCellCore) cell).getTableRowCore().getView();
				TableRowCore sampleRow = (TableRowCore) tv.getSampleRow();

				cell.addListeners(new Cell(cell, column, tv, sampleRow));
			}
		});
	}

	public class Cell
		implements TableCellRefreshListener, TableCellSWTPaintListener,
		TableCellVisibilityListener
	{
		private final TableColumnCore column;
		private FakeTableCell sampleCell;

		public Cell(TableCell parentCell, TableColumnCore column, TableViewColumnSetup tv, TableRowCore sampleRow) {
			this.column = column;
			if (sampleRow == null) {
				return;
			}
			sampleCell = new FakeTableCell(column);

			sampleCell.setControl(tv.getTableComposite());
			Rectangle bounds = ((TableCellImpl)parentCell).getBounds();
			sampleCell.setCellArea(bounds);
			if (sampleRow != null) {
				sampleCell.setDataSource(((TableRowCore)sampleRow).getDataSource(true));
			}
		}

		// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
		public void cellPaint(GC gc, TableCellSWT cell) {
			if (sampleCell == null) {
				return;
			}
			Rectangle bounds = ((TableCellImpl)cell).getBounds();
			sampleCell.setCellArea(bounds);
			try {
				sampleCell.doPaint(gc);
			} catch (Throwable e) {
				Debug.out(e);
			}
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener#cellVisibilityChanged(org.gudy.azureus2.plugins.ui.tables.TableCell, int)
		public void cellVisibilityChanged(TableCell cell, int visibility) {
			if (sampleCell == null) {
				return;
			}
			try {
				column.invokeCellVisibilityListeners((TableCellCore) sampleCell, visibility);
			} catch (Throwable e) {
				Debug.out(e);
			}
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
		public void refresh(TableCell cell) {
			if (sampleCell == null) {
				return;
			}
			if (!cell.isShown()) {
				return;
			}
			sampleCell.refresh(true, true, true);
			cell.setSortValue(sampleCell.getSortValue());
			//cell.setText(sampleCell.getText());
		}

	}
}
