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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.columnsetup.TableColumnSetupWindow.TableViewColumnSetup;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jan 3, 2009
 *
 */
public class ColumnTC_NameInfo
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellSWTPaintListener,
	TableCellMouseMoveListener, TableCellToolTipListener
{
	public static final String COLUMN_ID = "TableColumnNameInfo";

	public static Font fontHeader = null;
	
	private static String[] profText = { "beginner", "intermediate", "advanced" };

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnTC_NameInfo(String tableID) {
		super(COLUMN_ID, tableID);
		initialize(ALIGN_LEAD | ALIGN_TOP, POSITION_INVISIBLE, 415,
				INTERVAL_INVALID_ONLY);
		setType(TYPE_GRAPHIC);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		TableColumnCore column = (TableColumnCore) cell.getDataSource();
		String key = column.getTitleLanguageKey();
		cell.setSortValue(MessageText.getString(key, column.getName()));
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		TableColumnCore column = (TableColumnCore) cell.getDataSource();
		String key = column.getTitleLanguageKey();
		Rectangle bounds = cell.getBounds();
		if (bounds == null || bounds.isEmpty()) {
			return;
		}
		
		Font fontDefault = gc.getFont();
		if (fontHeader == null) {
			FontData[] fontData = gc.getFont().getFontData();
			fontData[0].setStyle(SWT.BOLD);
			fontData[0].setHeight(fontData[0].getHeight() + 1);
			fontHeader = new Font(gc.getDevice(), fontData);
		}

		gc.setFont(fontHeader);

		bounds.y += 3;
		bounds.x += 7;
		bounds.width -= 14;
		String name = MessageText.getString(key, column.getName());
		GCStringPrinter sp = new GCStringPrinter(gc, name, bounds, GCStringPrinter.FLAG_SKIPCLIP, SWT.TOP);
		sp.printString();

		Point titleSize = sp.getCalculatedSize();

		gc.setFont(fontDefault);
		String info = MessageText.getString(key + ".info", "");
		Rectangle infoBounds = new Rectangle(bounds.x + 10, bounds.y + titleSize.y
				+ 5, bounds.width - 15, bounds.height - 20);
		GCStringPrinter.printString(gc, info, infoBounds, true, false);

		TableColumnInfo columnInfo = (TableColumnInfo) cell.getTableRow().getData(
				"columninfo");
		if (columnInfo == null) {
			final TableColumnManager tcm = TableColumnManager.getInstance();
			columnInfo = tcm.getColumnInfo(column.getForDataSourceType(),
					column.getTableID(), column.getName());
			cell.getTableRowCore().setData("columninfo", columnInfo);
		}
		Rectangle profBounds = new Rectangle(bounds.width - 100, bounds.y - 2, 100, 20);
		byte proficiency = columnInfo.getProficiency();
		if (proficiency > 0 && proficiency < profText.length) {
			Color oldColor = gc.getForeground();
			gc.setForeground(Colors.grey);
			GCStringPrinter.printString(gc,
					MessageText.getString("ConfigView.section.mode."
							+ profText[proficiency]), profBounds, true,
					false, SWT.RIGHT | SWT.TOP);
			gc.setForeground(oldColor);
		}

		Rectangle hitArea;
		TableViewColumnSetup tv = (TableViewColumnSetup) ((TableCellCore) cell).getTableRowCore().getView();
		if (tv.isColumnAdded(column)) {
			hitArea = Utils.EMPTY_RECT;
		} else {
			int x = bounds.x + titleSize.x + 15;
			int y = bounds.y - 1;
			int h = 15;

			String textAdd = MessageText.getString("Button.add");
			GCStringPrinter sp2 = new GCStringPrinter(gc, textAdd,
					new Rectangle(x, y, 500, h), true, false, SWT.CENTER);
			sp2.calculateMetrics();
			int w = sp2.getCalculatedSize().x + 12;
			
			gc.setAdvanced(true);
			gc.setAntialias(SWT.ON);
			gc.setBackground(ColorCache.getColor(gc.getDevice(), 255, 255, 255));
			gc.fillRoundRectangle(x, y, w, h, 15, h);
			gc.setBackground(ColorCache.getColor(gc.getDevice(), 215, 215, 215));
			gc.fillRoundRectangle(x + 2, y + 2, w, h, 15, h);
			gc.setForeground(ColorCache.getColor(gc.getDevice(), 145, 145, 145));
			gc.drawRoundRectangle(x, y, w, h, 15, h);

			gc.setForeground(ColorCache.getColor(gc.getDevice(), 50, 50, 50));
			hitArea = new Rectangle(x, y, w + 2, h);
			sp2.printString(gc, hitArea, SWT.CENTER);
			bounds = cell.getBounds();
			hitArea.x -= bounds.x;
			hitArea.y -= bounds.y;
		}
		cell.getTableRowCore().setData("AddHitArea", hitArea);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (event.button == 1
				&& event.eventType == TableRowMouseEvent.EVENT_MOUSEUP
				&& (event.cell instanceof TableCellCore)) {
			Object data = event.cell.getTableRow().getData("AddHitArea");
			if (data instanceof Rectangle) {
				Rectangle hitArea = (Rectangle) data;
				if (hitArea.contains(event.x, event.y)) {
					TableViewColumnSetup tv = (TableViewColumnSetup) ((TableCellCore) event.cell).getTableRowCore().getView();
					Object dataSource = event.cell.getDataSource();
					if (dataSource instanceof TableColumnCore) {
						TableColumnCore column = (TableColumnCore) dataSource;
						tv.chooseColumn(column);
					}
				}
			}
		} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEMOVE) {
			Object data = event.cell.getTableRow().getData("AddHitArea");
			if (data instanceof Rectangle) {
				Rectangle hitArea = (Rectangle) data;
				if (hitArea.contains(event.x, event.y)) {
					((TableCellSWT)event.cell).setCursorID(SWT.CURSOR_HAND);
					return;
				}
			}
			((TableCellSWT)event.cell).setCursorID(SWT.CURSOR_ARROW);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
	}
}
