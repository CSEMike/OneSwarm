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

package org.gudy.azureus2.ui.swt.views.table;

import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.plugins.ui.tables.TableRowMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableView;

/**
 * @author TuxPaper
 * @created Feb 2, 2007
 *
 */
public interface TableViewSWT<DATASOURCETYPE>
	extends TableView<DATASOURCETYPE>
{
	/** Helpful output when trying to debug add/removal of rows */
	public final static boolean DEBUGADDREMOVE = System.getProperty("debug.swt.table.addremove", "0").equals("1");

	void addKeyListener(KeyListener listener);

	public void addMenuFillListener(TableViewSWTMenuFillListener l);

	DragSource createDragSource(int style);

	DropTarget createDropTarget(int style);

	public Composite getComposite();

	TableRowCore getRow(DropTargetEvent event);

	/**
	 * @param dataSource
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	TableRowSWT getRowSWT(DATASOURCETYPE dataSource);

	Composite getTableComposite();

	void initialize(Composite composite);

	/**
	 * @param image
	 * @return
	 */
	Image obfusticatedImage(Image image);

	/**
	 * @param listener
	 */
	void removeKeyListener(KeyListener listener);

	/**
	 * @param mainPanelCreator
	 */
	void setMainPanelCreator(TableViewSWTPanelCreator mainPanelCreator);


	/**
	 * @param size
	 */
	void setRowDefaultIconSize(Point size);
	

	/**
	 * @param x
	 * @param y
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	TableCellCore getTableCell(int x, int y);

	/**
	 * @return Offset potision of the cursor relative to the cell the cursor is in
	 *
	 * @since 3.0.4.3
	 */
	Point getTableCellMouseOffset(TableCellSWT tableCell);

	/**
	 * @param listener
	 *
	 * @since 3.1.1.1
	 */
	void removeRefreshListener(TableRowRefreshListener listener);

	/**
	 * @param listener
	 *
	 * @since 3.1.1.1
	 */
	void addRefreshListener(TableRowRefreshListener listener);

	/**
	 * @return
	 *
	 * @since 4.1.0.9
	 */
	String getFilterText();

	/**
	 * @param filterCheck
	 *
	 * @since 4.1.0.9
	 */
	void enableFilterCheck(Text txtFilter, TableViewFilterCheck<DATASOURCETYPE> filterCheck);

	/**
	 * @since 4.7.0.1
	 */
	void disableFilterCheck();
	
	boolean isFiltered( DATASOURCETYPE ds );
	
	/**
	 * @param s
	 *
	 * @since 4.1.0.8
	 */
	void setFilterText(String s);

	/**
	 * @param composite
	 * @param min
	 * @param max
	 * 
	 * @since 4.1.0.9
	 */
	boolean enableSizeSlider(Composite composite, int min, int max);

	void disableSizeSlider();

	/**
	 * @param listener
	 *
	 * @since 4.2.0.3
	 */
	void addRowPaintListener(TableRowSWTPaintListener listener);

	/**
	 * @param listener
	 *
	 * @since 4.2.0.3
	 */
	void removeRowPaintListener(TableRowSWTPaintListener listener);

	/**
	 * @param listener
	 *
	 * @since 4.4.0.7
	 */
	void removeRowMouseListener(TableRowMouseListener listener);

	/**
	 * @param listener
	 *
	 * @since 4.4.0.7
	 */
	void addRowMouseListener(TableRowMouseListener listener);

	TableOrTreeSWT getTableOrTreeSWT();

	/**
	 * @since 4.5.0.5
	 */
	void refilter();

	/**
	 * @param menuEnabled
	 *
	 * @since 4.6.0.5
	 */
	void setMenuEnabled(boolean menuEnabled);

	/**
	 * @return
	 *
	 * @since 4.6.0.5
	 */
	boolean isMenuEnabled();

	void packColumns();
}
