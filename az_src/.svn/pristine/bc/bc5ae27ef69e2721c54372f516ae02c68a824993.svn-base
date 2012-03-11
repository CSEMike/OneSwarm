/*
 * File    : TableRowImpl.java
 * Originally TorrentRow.java, and changed to be more generic by TuxPaper
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
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.*;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.*;

import com.aelitis.azureus.ui.common.table.*;

/** Represents an entire row in a table.  Stores each cell belonging to the
 * row and handles refreshing them.
 *
 * @see TableCellImpl
 * 
 * @author TuxPaper
 *            2005/Oct/07: Moved TableItem.SetData("TableRow", ..) to 
 *                         BufferedTableRow
 *            2005/Oct/07: Removed all calls to BufferedTableRoe.getItem()
 */
public class TableRowImpl<COREDATASOURCE>
	extends BufferedTableRow
	implements TableRowSWT
{
	/** 
	 * List of cells in this row.  
	 * They are not stored in display order.
	 * Not written to after initializer
	 */
	private Map<String, TableCellCore> mTableCells;

	private Object coreDataSource;

	private Object pluginDataSource;

	private boolean bDisposed;

	private boolean bSetNotUpToDateLastRefresh = false;

	private TableView<COREDATASOURCE> tableView;

	private static AEMonitor this_mon = new AEMonitor("TableRowImpl");

	private ArrayList<TableRowMouseListener> mouseListeners;

	private boolean wasShown = false;

	private Map<String, Object> dataList;

	private int lastIndex = -1;

	private int fontStyle;

	private int alpha = 255;

	private TableRowCore parentRow;

	private TableRowImpl<Object>[] subRows;

	private AEMonitor mon_SubRows = new AEMonitor("subRows");

	private TableColumnCore[] columnsSorted;

	private boolean bSkipFirstColumn;

	// XXX add rowVisuallyupdated bool like in ListRow

	public TableRowImpl(TableRowCore parentRow, TableView<COREDATASOURCE> tv,
			TableOrTreeSWT table, TableColumnCore[] columnsSorted, String sTableID,
			Object dataSource, int index, boolean bSkipFirstColumn) {
		super(table);
		this.parentRow = parentRow;
		this.tableView = tv;
		coreDataSource = dataSource;
		bDisposed = false;
		lastIndex = index;

		mTableCells = new LightHashMap<String, TableCellCore>(columnsSorted.length,
				1);

		// create all the cells for the column
		for (int i = 0; i < columnsSorted.length; i++) {
			if (columnsSorted[i] == null) {
				continue;
			}

			if (!columnsSorted[i].handlesDataSourceType(getDataSource(false).getClass())) {
				mTableCells.put(columnsSorted[i].getName(), null);
				continue;
			}

			//System.out.println(dataSource + ": " + tableColumns[i].getName() + ": " + tableColumns[i].getPosition());
			TableCellImpl cell = new TableCellImpl(TableRowImpl.this,
					columnsSorted[i], bSkipFirstColumn ? i + 1 : i);
			mTableCells.put(columnsSorted[i].getName(), cell);
			//if (i == 10) cell.bDebug = true;
		}
	}

	/**
	 * Default constructor
	 * 
	 * @param table
	 * @param sTableID
	 * @param columnsSorted
	 * @param dataSource
	 * @param bSkipFirstColumn
	 */
	public TableRowImpl(TableView<COREDATASOURCE> tv, TableOrTreeSWT table,
			TableColumnCore[] columnsSorted, Object dataSource,
			boolean bSkipFirstColumn) {
		super(table);
		this.tableView = tv;
		this.columnsSorted = columnsSorted;
		coreDataSource = dataSource;
		this.bSkipFirstColumn = bSkipFirstColumn;
		bDisposed = false;

		mTableCells = new LightHashMap<String, TableCellCore>(columnsSorted.length,
				1);

		// create all the cells for the column
		for (int i = 0; i < columnsSorted.length; i++) {
			if (columnsSorted[i] == null) {
				continue;
			}
			//System.out.println(dataSource + ": " + tableColumns[i].getName() + ": " + tableColumns[i].getPosition());
			TableCellImpl cell = new TableCellImpl(TableRowImpl.this,
					columnsSorted[i], bSkipFirstColumn ? i + 1 : i);
			mTableCells.put(columnsSorted[i].getName(), cell);
			//if (i == 10) cell.bDebug = true;
		}
	}

	public boolean isValid() {
		if (bDisposed || mTableCells == null) {
			return true;
		}

		boolean valid = true;
		for (TableCell cell : mTableCells.values()) {
			if (cell != null && cell.isValid()) {
				return false;
			}
		}

		return valid;
	}

	/** TableRow Implementation which returns the 
	 * associated plugin object for the row.  Core Column Object who wish to get 
	 * core data source must re-class TableRow as TableRowCore and use
	 * getDataSource(boolean)
	 *
	 * @see TableRowCore.getDataSource()
	 */
	public Object getDataSource() {
		return getDataSource(false);
	}

	public String getTableID() {
		return tableView.getTableID();
	}

	public TableCell getTableCell(String field) {
		if (bDisposed || mTableCells == null) {
			return null;
		}

		return mTableCells.get(field);
	}

	public void addMouseListener(TableRowMouseListener listener) {
		try {
			this_mon.enter();

			if (mouseListeners == null) {
				mouseListeners = new ArrayList<TableRowMouseListener>(1);
			}

			mouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableRowMouseListener listener) {
		try {
			this_mon.enter();

			if (mouseListeners == null) {
				return;
			}

			mouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void invokeMouseListeners(TableRowMouseEvent event) {
		ArrayList<TableRowMouseListener> listeners = mouseListeners;
		if (listeners == null) {
			return;
		}

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableRowMouseListener l = listeners.get(i);

				l.rowMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	/* Start Core-Only functions */
	///////////////////////////////

	public void delete() {
		this_mon.enter();

		try {
			if (bDisposed) {
				return;
			}

			if (TableViewSWT.DEBUGADDREMOVE) {
				System.out.println((table.isDisposed() ? "" : table.getData("Name"))
						+ " row delete; index=" + getIndex());
			}

			for (TableCellCore cell : mTableCells.values()) {
				try {
					if (cell != null) {
						cell.dispose();
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}

			//setForeground((Color) null);

			bDisposed = true;
		} finally {
			this_mon.exit();
		}
	}

	public List<TableCellCore> refresh(boolean bDoGraphics) {
		if (bDisposed) {
			return Collections.EMPTY_LIST;
		}

		boolean bVisible = isVisible();

		return refresh(bDoGraphics, bVisible);
	}

	public List<TableCellCore> refresh(boolean bDoGraphics, boolean bVisible) {
		// If this were called from a plugin, we'd have to refresh the sorted column
		// even if we weren't visible
		List<TableCellCore> list = Collections.EMPTY_LIST;

		if (bDisposed) {
			return list;
		}

		if (!bVisible) {
			if (!bSetNotUpToDateLastRefresh) {
				setUpToDate(false);
				bSetNotUpToDateLastRefresh = true;
			}
			return list;
		}

		bSetNotUpToDateLastRefresh = false;

		//System.out.println(SystemTime.getCurrentTime() + "refresh " + getIndex() + ";vis=" + bVisible);

		((TableViewSWTImpl<COREDATASOURCE>) tableView).invokeRefreshListeners(this);

		for (TableCellCore cell : mTableCells.values()) {
			if (cell == null || cell.isDisposed()) {
				continue;
			}
			TableColumn column = cell.getTableColumn();
			//System.out.println(column);
			if (column != tableView.getSortColumn()
					&& !tableView.isColumnVisible(column)) {
				//System.out.println("skip " + column);
				continue;
			}
			boolean changed = cell.refresh(bDoGraphics, bVisible);
			if (changed) {
				if (list == Collections.EMPTY_LIST) {
					list = new ArrayList<TableCellCore>(mTableCells.size());
				}
				list.add(cell);
			}

		}

		//System.out.println();
		return list;
	}

	public void locationChanged(int iStartColumn) {
		if (bDisposed || !isVisible()) {
			return;
		}

		for (TableCellCore cell : mTableCells.values()) {
			if (cell != null && cell.getTableColumn().getPosition() > iStartColumn) {
				cell.locationChanged();
			}
		}
	}

	public TableCellCore getTableCellCore(String name) {
		if (bDisposed || mTableCells == null) {
			return null;
		}

		return mTableCells.get(name);
	}

	/**
	 * @param name
	 * @return
	 */
	public TableCellSWT getTableCellSWT(String name) {
		if (bDisposed || mTableCells == null) {
			return null;
		}

		TableCellCore cell = mTableCells.get(name);
		if (cell instanceof TableCellSWT) {
			return (TableCellSWT) cell;
		}
		return null;
	}

	public Object getDataSource(boolean bCoreObject) {
		if (bDisposed) {
			return null;
		}

		if (bCoreObject) {
			return coreDataSource;
		}

		if (pluginDataSource != null) {
			return pluginDataSource;
		}

		pluginDataSource = PluginCoreUtils.convert(coreDataSource, bCoreObject);

		return pluginDataSource;
	}

	public boolean isRowDisposed() {
		return bDisposed;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#getIndex()
	 */
	public int getIndex() {
		if (bDisposed) {
			return -1;
		}

		if (lastIndex >= 0) {
			if (parentRow != null) {
				return lastIndex;
			}
			TableRowCore row = ((TableViewSWTImpl<COREDATASOURCE>) tableView).getRowQuick(lastIndex);
			if (row == this) {
				return lastIndex;
			}
		}

		// don't set directly to lastIndex, so setTableItem will eventually do
		// its job
		return tableView.indexOf(this);

		//return super.getIndex();
	}

	public int getRealIndex() {
		return super.getIndex();
	}

	public boolean setTableItem(int newIndex, boolean isVisible) {
		if (bDisposed) {
			System.out.println("XXX setTI: bDisposed from "
					+ Debug.getCompressedStackTrace());
			return false;
		}

		int maxItemShown = tableView.getMaxItemShown();
		if (newIndex > maxItemShown) {
				//System.out.println((item == null ? null : "" + table.indexOf(item)) + ":" + newIndex + ":" + isVisible + ":" + tableView.getMaxItemShown());
				tableView.setMaxItemShown(newIndex);
			if (!isVisible) {
				return false;
			}
		}

		boolean changedIndex = lastIndex != newIndex;

		//if (getRealIndex() != newIndex) {
		//	((TableViewSWTImpl)tableView).debug("sTI " + newIndex + "; via " + Debug.getCompressedStackTrace(4));
		//}
		boolean changedSWTRow = !changedIndex ? false : super.setTableItem(newIndex, isVisible);
		//if (changedSWTRow) {
		//	System.out.println((item == null ? null : "" + table.indexOf(item)) + ":" + newIndex + ":" + isVisible + ":" + tableView.getMaxItemShown());
		//}
		if (changedIndex) {
			//System.out.println("row " + newIndex + " from " + lastIndex + ";" + tableView.isRowVisible(this) + ";" + changedSWTRow);
			lastIndex = newIndex;
		}
		//boolean rowVisible = tableView.isRowVisible(this);
		setShown(isVisible, changedSWTRow);
		if (changedSWTRow && isVisible) {
			redraw();
			//invalidate();
			//refresh(true, true);
			setUpToDate(false);
		}
		return changedSWTRow;
	}

	public boolean setTableItem(int newIndex) {
		return setTableItem(newIndex, true);
	}

	private static final boolean DEBUG_SET_FOREGROUND = System.getProperty("debug.setforeground") != null;

	private Object[] subDataSources;

	private static void setForegroundDebug(String method_sig, Color c) {
		if (DEBUG_SET_FOREGROUND && c != null) {
			Debug.out("BufferedTableRow " + method_sig + " -> " + c);
		}
	}

	private static void setForegroundDebug(String method_sig, int r, int g, int b) {
		if (DEBUG_SET_FOREGROUND && (!(r == 0 && g == 0 && b == 0))) {
			Debug.out("BufferedTableRow " + method_sig + " -> " + r + "," + g + ","
					+ b);
		}
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setForeground(int, int, int)
	public void setForeground(int r, int g, int b) {
		setForegroundDebug("setForeground(r, g, b)", r, g, b);
		// Don't need to set when not visible
		if (!isVisible()) {
			return;
		}

		super.setForeground(r, g, b);
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setForeground(org.eclipse.swt.graphics.Color)
	public void setForeground(final Color c) {
		setForegroundDebug("setForeground(Color)", c);
		// Don't need to set when not visible
		if (!isVisible()) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				TableRowImpl.this.setForegroundInSWTThread(c);
			}
		});
	}

	private void setForegroundInSWTThread(Color c) {
		setForegroundDebug("setForegroundInSWTThread(Color)", c);
		if (!isVisible()) {
			return;
		}

		super.setForeground(c);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRow#setForeground(int[])
	public void setForeground(int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			setForeground((Color) null);
			return;
		}
		setForeground(rgb[0], rgb[1], rgb[2]);
	}

	public void setForegroundToErrorColor() {
		this.setForeground(Colors.colorError);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#invalidate()
	 */
	public void invalidate() {
		super.invalidate();

		if (bDisposed) {
			return;
		}

		for (TableCellCore cell : mTableCells.values()) {
			if (cell != null) {
				cell.invalidate(false);
			}
		}
	}

	public void setUpToDate(boolean upToDate) {
		if (bDisposed) {
			return;
		}

		for (TableCellCore cell : mTableCells.values()) {
			if (cell != null) {
				cell.setUpToDate(upToDate);
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#redraw()
	public void redraw() {
		// this will call paintItem which may call refresh
		Rectangle bounds = getBounds();
		table.redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
	}

	public String toString() {
		String result = "TableRowImpl@" + Integer.toHexString(hashCode()) + "/#"
				+ lastIndex;
		return result;
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#getView()
	public TableView<COREDATASOURCE> getView() {
		return tableView;
	}

	/**
	 * @param b
	 *
	 * @since 3.0.4.3
	 */
	public void setShown(boolean b, boolean force) {
		if (bDisposed) {
			return;
		}

		if (b == wasShown && !force) {
			return;
		}
		wasShown = b;

		for (TableCellCore cell : mTableCells.values()) {
			if (cell != null) {
				cell.invokeVisibilityListeners(b
						? TableCellVisibilityListener.VISIBILITY_SHOWN
						: TableCellVisibilityListener.VISIBILITY_HIDDEN, true);
			}
		}

		/* Don't need to refresh; paintItem will trigger a refresh on
		 * !cell.isUpToDate()
		 *
		if (b) {
			refresh(b, true);
		}
		/**/
	}

	public boolean isMouseOver() {
		return tableView.getTableRowWithCursor() == this;
	}

	public void setData(String id, Object data) {
		synchronized (this) {
			if (dataList == null) {
				dataList = new HashMap<String, Object>(1);
			}
			if (data == null) {
				dataList.remove(id);
			} else {
				dataList.put(id, data);
			}
		}
	}

	public Object getData(String id) {
		synchronized (this) {
			return dataList == null ? null : dataList.get(id);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#setDrawableHeight(int)
	public boolean setDrawableHeight(int height) {
		return setHeight(height);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getBounds()
	public Rectangle getBounds() {
		Rectangle bounds = getBounds(1);
		if (bounds == null) {
			return new Rectangle(0, 0, 0, 0);
		}
		Rectangle tableBounds = table.getClientArea();
		bounds.x = tableBounds.x;
		bounds.width = tableBounds.width;
		return bounds;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#setFontStyle(int)
	public boolean setFontStyle(int style) {
		if (fontStyle == style) {
			return false;
		}

		fontStyle = style;
		invalidate();

		return true;
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#setAlpha(int)
	public boolean setAlpha(int alpha) {
		if (this.alpha == alpha) {
			return false;
		}

		this.alpha = alpha;
		invalidate();

		return true;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getAlpha()
	public int getAlpha() {
		return alpha;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getFontStyle()
	public int getFontStyle() {
		return fontStyle;
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#isVisible()
	public boolean isVisible() {
		return tableView.isRowVisible(this);
		//return Utils.execSWTThreadWithBool("isVisible", new AERunnableBoolean() {
		//	public boolean runSupport() {
		//		return TableRowImpl.super.isVisible();
		//	}
		//}, 1000);
	}

	public boolean isVisibleNoSWT() {
		return tableView.isRowVisible(this);
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setSelected(boolean)
	public void setSelected(boolean selected) {
		if (tableView instanceof TableViewSWTImpl) {
			((TableViewSWTImpl<COREDATASOURCE>) tableView).selectRow(this, true);
		}
	}

	public void setWidgetSelected(boolean selected) {
		super.setSelected(selected);
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#isSelected()
	public boolean isSelected() {
		return tableView.isSelected(this);
		/*
		return Utils.execSWTThreadWithBool("isSelected", new AERunnableBoolean() {
			public boolean runSupport() {
				return TableRowImpl.super.isSelected();
			}
		}, 1000);
		*/
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setSubItemCount(int)
	@SuppressWarnings("rawtypes")
	public void setSubItemCount(final int count) {
		super.setSubItemCount(count);
		if (count == getSubItemCount()) {
			if (count == 0 || (subRows != null && subRows[0] == null)) {
				return;
			}
		}
		mon_SubRows.enter();
		try {
			subRows = new TableRowImpl[count];
			for (int i = 0; i < count; i++) {
				//subRows[i] = new TableRowImpl(this, tableView, table, columnsSorted,
				//		getTableID(), null, i, bSkipFirstColumn);
				subRows[i] = null;
			}
		} finally {
			mon_SubRows.exit();
		}
	}

	@SuppressWarnings("rawtypes")
	public void setSubItems(Object[] datasources) {
		this.subDataSources = datasources;
		super.setSubItemCount(datasources.length);

		mon_SubRows.enter();
		try {
			subRows = new TableRowImpl[datasources.length];
			for (int i = 0; i < datasources.length; i++) {
				//subRows[i] = new TableRowImpl(this, tableView, table, columnsSorted,
				//		getTableID(), datasources[i], i, bSkipFirstColumn);
				subRows[i] = null;
			}
		} finally {
			mon_SubRows.exit();
		}
	}

	public TableRowCore linkSubItem(int indexOf) {
		mon_SubRows.enter();
		try {
			if (indexOf >= subRows.length) {
				return null;
			}
			TableRowImpl<Object> subRow = subRows[indexOf];
			if (subRow == null) {
				subRows[indexOf] = subRow = new TableRowImpl(this, tableView, table,
						columnsSorted, getTableID(), subDataSources[indexOf], indexOf,
						bSkipFirstColumn);
			}
			TableItemOrTreeItem subItem = item.getItem(indexOf);
			subRow.setTableItem(subItem, true);
			return subRow;
		} finally {
			mon_SubRows.exit();
		}
	}

	public TableRowCore[] getSubRowsWithNull() {
		mon_SubRows.enter();
		try {
			TableRowCore[] copyOf = new TableRowCore[subRows.length];
			System.arraycopy(subRows, 0, copyOf, 0, subRows.length);
			return copyOf;
		} finally {
			mon_SubRows.exit();
		}
	}

	public void removeSubRow(final Object datasource) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				swt_removeSubRow(datasource);
			}
		});
	}

	public void swt_removeSubRow(Object datasource) {
		if (datasource instanceof TableRowImpl) {
			removeSubRow(((TableRowImpl) datasource).getDataSource());
		}

		mon_SubRows.enter();
		try {
			if (subDataSources == null || subDataSources.length == 0
					|| subDataSources.length != subRows.length) {
				return;
			}

			for (int i = 0; i < subDataSources.length; i++) {
				Object ds = subDataSources[i];
				if (ds == datasource) { // use .equals instead?
					TableRowImpl rowToDel = subRows[i];
					TableRowImpl[] newSubRows = new TableRowImpl[subRows.length - 1];
					System.arraycopy(subRows, 0, newSubRows, 0, i);
					System.arraycopy(subRows, i + 1, newSubRows, i, subRows.length - i
							- 1);
					subRows = newSubRows;

					Object[] newDatasources = new Object[subRows.length];
					System.arraycopy(subDataSources, 0, newDatasources, 0, i);
					System.arraycopy(subDataSources, i + 1, newDatasources, i,
							subDataSources.length - i - 1);
					subDataSources = newDatasources;

					rowToDel.dispose();
					rowToDel.delete();

					super.setSubItemCount(subRows.length);

					break;
				}
			}
		} finally {
			mon_SubRows.exit();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#isInPaintItem()
	public boolean isInPaintItem() {
		return super.inPaintItem();
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#getParentRowCore()
	public TableRowCore getParentRowCore() {
		return parentRow;
	}

	public TableItemOrTreeItem getItem() {
		return super.item;
	}
}
