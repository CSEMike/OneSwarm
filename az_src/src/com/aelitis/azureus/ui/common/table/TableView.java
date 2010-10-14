/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.common.table;

import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * @author TuxPaper
 * @created Feb 2, 2007
 *
 */
public interface TableView
	extends AEDiagnosticsEvidenceGenerator
{
	/**
	 * @param listener
	 */
	void addCountChangeListener(TableCountChangeListener listener);

	/** Adds a dataSource to the table as a new row.  If the data source is
	 * already added, a new row will not be added.  This function runs 
	 * asynchronously, so the rows creation is not guaranteed directly after
	 * calling this function.
	 *
	 * You can't add datasources until the table is initialized
	 * 
	 * @param dataSource data source to add to the table
	 * @param bImmediate Add immediately, or queue and add at next refresh
	 */
	void addDataSource(Object dataSource);

	/**
	 * Add a list of dataSources to the table.  The array passed in may be 
	 * modified, so make sure you don't need it afterwards.
	 * 
	 * You can't add datasources until the table is initialized
	 * 
	 * @param dataSources
	 * @param bImmediate Add immediately, or queue and add at next refresh
	 */
	void addDataSources(Object[] dataSources);

	void addLifeCycleListener(TableLifeCycleListener l);

	void addRefreshListener(TableRefreshListener l, boolean trigger);

	/**
	 * @param listener
	 * @param bFireSelection
	 */
	void addSelectionListener(TableSelectionListener listener, boolean trigger);

	/**
	 * The data set that this table represents has been changed.  This is not
	 * for listening on changes to data sources changing within the table
	 * 
	 * @param l
	 * @param trigger
	 */
	void addTableDataSourceChangedListener(TableDataSourceChangedListener l,
			boolean trigger);

	/** 
	 * Send Selected rows to the clipboard in a SpreadSheet friendly format
	 * (tab/cr delimited)
	 */
	void clipboardSelected();

	/**
	 * Invalidate all the cells in a column
	 * 
	 * @param sColumnName Name of column to invalidate
	 */
	void columnInvalidate(String sColumnName);

	/**
	 * @param tableColumn
	 */
	void columnInvalidate(TableColumnCore tableColumn);

	void delete();

	/**
	 * Retrieve a list of <pre>TableCell</pre>s, in the last sorted order.
	 * The order will not be of the supplied cell's sort unless the table
	 * has been sorted by that column previously.
	 * <p>
	 * ie.  You can sort on the 5th column, and retrieve the cells for the
	 *      3rd column, but they will be in order of the 5th columns sort.  
	 * 
	 * @param sColumnName Which column cell's to return.  This does not sort
	 *         the array on the column. 
	 * @return array of cells
	 */
	TableCellCore[] getColumnCells(String columnName);

	/**
	 * @return
	 */
	Object[] getDataSources();

	/**
	 * @return
	 */
	Object getFirstSelectedDataSource();

	/**
	 * @return
	 */
	String getPropertiesPrefix();

	/**
	 * Get the row associated with a datasource
	 * @param dataSource a reference to a core Datasource object 
	 * 										(not a plugin datasource object)
	 * @return The row, or null
	 */
	TableRowCore getRow(Object dataSource);

	/** Get all the rows for this table, in the order they are displayed
	 *
	 * @return a list of TableRowSWT objects in the order the user sees them
	 */
	TableRowCore[] getRows();

	/** Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @return an array containing the selected data sources
	 */
	Object[] getSelectedDataSources();

	/** 
	 * Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @param bCoreDataSource
	 * @return an array containing the selected data sources
	 */
	Object[] getSelectedDataSources(boolean bCoreDataSource);

	/** 
	 * Returns an array of all selected TableRowSWT.  Null data sources are
	 * ommitted.
	 *
	 * @return an array containing the selected data sources
	 */
	TableRowCore[] getSelectedRows();

	/**
	 * @return
	 */
	TableColumnCore getSortColumn();

	/**
	 * @return
	 */
	boolean isDisposed();

	boolean isTableFocus();

	/**
	 * Process the queue of datasources to be added and removed
	 *
	 */
	void processDataSourceQueue();

	/**
	 * @param bForceSort
	 */
	void refreshTable(boolean bForceSort);

	/** 
	 * Remove all the data sources (table rows) from the table.
	 */
	void removeAllTableRows();

	/**
	 * @param dataSource
	 */
	void removeDataSource(Object dataSource);

	/**
	 * @param l
	 */
	void removeTableDataSourceChangedListener(TableDataSourceChangedListener l);

	/** For every row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each row/datasource
	 */
	void runForAllRows(TableGroupRowRunner runner);

	/** For every row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each row/datasource
	 */
	void runForAllRows(TableGroupRowVisibilityRunner runner);

	/**
	 * @param runner
	 */
	void runForSelectedRows(TableGroupRowRunner runner);

	/**
	 * Does not fire off selection events
	 */
	void selectAll();

	/**
	 * @param enableTabViews
	 */
	void setEnableTabViews(boolean enableTabViews);

	void setFocus();

	/**
	 * @param newDataSource
	 */
	void setParentDataSource(Object newDataSource);

	/**
	 * @param iHeight
	 */
	void setRowDefaultHeight(int iHeight);

	void setSelectedRows(TableRowCore[] rows);

	/**
	 * @param bIncludeQueue
	 * @return
	 */
	int size(boolean bIncludeQueue);

	void updateLanguage();

	/**
	 * @return
	 */
	TableRowCore getFocusedRow();

	/**
	 * @return
	 */
	String getTableID();

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	TableRowCore getRow(int x, int y);

	/**
	 * @param datasource
	 * @param immediate
	 */
	void addDataSource(Object datasource, boolean immediate);

	/**
	 * @param dataSource
	 * @return
	 */
	boolean dataSourceExists(Object dataSource);

	/**
	 * @param datasource
	 * @param immediate
	 */
	void removeDataSource(Object datasource, boolean immediate);

	/**
	 * @return
	 */
	TableColumnCore[] getVisibleColumns();

	/**
	 * @param columns
	 * @param defaultSortColumnID
	 * @param titleIsMinWidth TODO
	 */
	public void setColumnList(TableColumnCore[] columns,
			String defaultSortColumnID, boolean defaultSortAscending,
			boolean titleIsMinWidth);

	/**
	 * @param dataSources
	 */
	void removeDataSources(Object[] dataSources);

	/**
	 * @return
	 * 
	 * @since 3.0.0.7
	 */
	int getSelectedRowsSize();

	/**
	 * @param row
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	int indexOf(TableRowCore row);

	/**
	 * @param row
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	boolean isRowVisible(TableRowCore row);

	/**
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	TableCellCore getTableCellWithCursor();

	/**
	 * Retrieves the row that has the cursor over it
	 * 
	 * @return null if mouse isn't over a row
	 *
	 * @since 3.0.4.3
	 */
	TableRowCore getTableRowWithCursor();

	/**
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	int getRowDefaultHeight();
	
	boolean isColumnVisible(TableColumn column);

	/**
	 * @param position
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	TableRowCore getRow(int position);
}
