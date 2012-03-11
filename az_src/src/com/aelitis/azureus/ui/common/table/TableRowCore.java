/**
 * Copyright (C) 2004-2007 Aelitis SAS, All rights Reserved
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
package com.aelitis.azureus.ui.common.table;

import java.util.List;

import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableRowMouseEvent;

/** 
 * Core Table Row functions are those available to plugins plus
 * some core-only functions.  The core-only functions are listed here.
 *
 * @author TuxPaper
 * @since 2.0.8.5 2004/May/14
 */
public interface TableRowCore extends TableRow
{
	/** Invalidates Row */
	public void invalidate();

	/** 
	 * Delete the row 
	 */
	public void delete();

	/** Refresh all the cells in the row 
	 *
	 * @param bDoGraphics Refresh graphic cells to
	 */
	public List refresh(boolean bDoGraphics);

	/** Location of a column has changed
	 *
	 * @param iStartColumn Cells starting at this value may need repainting
	 *
	 * XXX Rename to cellLocationChanged?
	 */
	public void locationChanged(int iStartColumn);

	/** Retrieve the Data Source related to this row
	 *
	 * @param bCoreObject true - return a core object<br>
	 *                    false - return a plugin object
	 * @return the Data Source Object related to the row
	 */
	public Object getDataSource(boolean bCoreObject);

	public int getIndex();

	/** Adjust cell height.  Don't use if any other column/cell uses setImage()
	 *
	 * @param iHeight new Row Height.  Will not reduce row's height (SWT)
	 * @return success level
	 */
	public boolean setHeight(int iHeight);

	/** Retrieve a cell based on the supplied value
	 *
	 * @param field Column name of the cell to be returned
	 * @return TableCellCore object related to this row and the specified column 
	 */
	public TableCellCore getTableCellCore(String field);

	/** Retreive whether the row is visible to the user.  In SWT, when the table
	 * is not VIRTUAL, all rows are "visible"
	 * 
	 * @return visibility state
	 */
	public boolean isVisible();

	/**
	 * Link the row to a SWT TableItem
	 * 
	 * @param newIndex new position row should be
	 * @return false - already linked to that item at that index
	 */
	public boolean setTableItem(int newIndex);
	
	public boolean setTableItem(int newIndex, boolean isVisible);
	
	

	public void setSelected(boolean bSelected);

	public boolean isRowDisposed();

	public void setUpToDate(boolean upToDate);

	/**
	 * @param bDoGraphics
	 * @param bVisible
	 */
	public List refresh(boolean bDoGraphics, boolean bVisible);

	/**
	 * like refresh, except a different name to confuse us.
	 */
	public void redraw();

	/**
	 * @return
	 */
	public TableView getView();

	/**
	 * @param event
	 *
	 * @since 3.0.1.7
	 */
	void invokeMouseListeners(TableRowMouseEvent event);

	/**
	 * 
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public boolean isMouseOver();

	/**
	 * @param height
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public boolean setDrawableHeight(int height);

	/**
	 * @param length
	 *
	 * @since 4.4.0.5
	 */
	public void setSubItemCount(int length);
	
	public int getSubItemCount();

	public boolean isExpanded();

	public void setExpanded(boolean b);
	
	public TableRowCore getParentRowCore();

	/**
	 * @return
	 *
	 * @since 4.4.0.5
	 */
	public boolean isInPaintItem();

	/**
	 * @param indexOf
	 * @return 
	 *
	 * @since 4.4.0.5
	 */
	TableRowCore linkSubItem(int indexOf);

	/**
	 * @param datasources
	 *
	 * @since 4.4.0.5
	 */
	void setSubItems(Object[] datasources);

	TableRowCore[] getSubRowsWithNull();

	void removeSubRow(Object datasource);
}
