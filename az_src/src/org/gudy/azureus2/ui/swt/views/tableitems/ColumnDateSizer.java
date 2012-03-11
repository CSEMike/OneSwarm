/**
 * Created on Oct 5, 2008
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

package org.gudy.azureus2.ui.swt.views.tableitems;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;

/**
 * @author TuxPaper
 * @created Oct 5, 2008
 *
 */
public abstract class ColumnDateSizer
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	private static int PADDING = 10;
	int curFormat = 0;

	int[] maxWidthUsed = new int[TimeFormatter.DATEFORMATS_DESC.length];

	Date[] maxWidthDate = new Date[TimeFormatter.DATEFORMATS_DESC.length];

	private boolean showTime = true;

	private boolean multiline = true;

	private static Font fontBold;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnDateSizer(Class forDataSrouceType, String columnID, int width, String tableID) {
		super(forDataSrouceType, columnID, ALIGN_TRAIL, width, tableID);

		TableContextMenuItem menuShowTime = addContextMenuItem(
				"TableColumn.menu.date_added.time", MENU_STYLE_HEADER);
		menuShowTime.setStyle(TableContextMenuItem.STYLE_CHECK);
		menuShowTime.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setData(showTime);
			}
		});
		menuShowTime.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				showTime = !showTime;
				setUserData("showTime", new Long(showTime ? 1 : 0));
				maxWidthUsed = new int[TimeFormatter.DATEFORMATS_DESC.length];
				maxWidthDate = new Date[TimeFormatter.DATEFORMATS_DESC.length];
				curFormat = -1;
				recalcWidth(new Date());
				if (curFormat < 0) {
					curFormat = TimeFormatter.DATEFORMATS_DESC.length - 1;
				}
			}
		});
	}
	
	// @see com.aelitis.azureus.ui.common.table.impl.TableColumnImpl#postConfigLoad()
	public void postConfigLoad() {
		Object oShowTime = getUserData("showTime");
		if (oShowTime instanceof Number) {
			Number nShowTime = (Number) oShowTime;
			showTime = nShowTime.byteValue() == 1;
		} else {
	    int userMode = COConfigurationManager.getIntParameter("User Mode");
			showTime = userMode > 1;
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public final void refresh(TableCell cell) {
		refresh(cell, 0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(final TableCell cell, final long timestamp) {
		if (!cell.setSortValue(timestamp) && cell.isValid()) {
			return;
		}

		if (timestamp <= 0) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			
			public void runSupport() {
				Date date = new Date(timestamp);

				if (curFormat >= 0) {
					if (multiline && cell.getHeight() < 20) {
						multiline = false;
					}
					String suffix = showTime && !multiline ? " hh:mm a" : "";

					int newWidth = calcWidth(date, TimeFormatter.DATEFORMATS_DESC[curFormat]
							+ suffix);

					//SimpleDateFormat temp2 = new SimpleDateFormat(TimeFormatter.DATEFORMATS_DESC[curFormat] + suffix + (showTime && multiline ? "\nh:mm a" : ""));
					//System.out.println(curFormat + ":newWidth=" +  newWidth + ":max=" + maxWidthUsed[curFormat] + ":cell=" + cell.getWidth() + "::" + temp2.format(date));
					if (newWidth > cell.getWidth() - PADDING) {
						if (newWidth > maxWidthUsed[curFormat]) {
							maxWidthUsed[curFormat] = newWidth;
							maxWidthDate[curFormat] = date;
						}
						recalcWidth(date);
					}

					String s = TimeFormatter.DATEFORMATS_DESC[curFormat] + suffix;
					SimpleDateFormat temp = new SimpleDateFormat(s
							+ (showTime && multiline ? "\nh:mm a" : ""));
					cell.setText(temp.format(date));
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.common.table.impl.TableColumnImpl#setWidth(int)
	public void setWidth(int width) {
		int oldWidth = this.getWidth();
		super.setWidth(width);

		if (oldWidth == width) {
			return;
		}
		if (maxWidthDate != null) {
			if (maxWidthDate[curFormat] == null) {
				maxWidthDate[curFormat] = new Date();
			}
			recalcWidth(maxWidthDate[curFormat]);
		}
	}

	private void recalcWidth(Date date) {
		String suffix = showTime && !multiline ? " hh:mm a" : "";

		int width = getWidth();

		if (maxWidthDate == null) {
			maxWidthUsed = new int[TimeFormatter.DATEFORMATS_DESC.length];
			maxWidthDate = new Date[TimeFormatter.DATEFORMATS_DESC.length];
		}

		int idxFormat = TimeFormatter.DATEFORMATS_DESC.length - 1;

		GC gc = new GC(Display.getDefault());
		if (fontBold == null) {
			FontData[] fontData = gc.getFont().getFontData();
			for (int i = 0; i < fontData.length; i++) {
				FontData fd = fontData[i];
				fd.setStyle(SWT.BOLD);
			}
			fontBold = new Font(gc.getDevice(), fontData);
		}
		gc.setFont(fontBold);

		try {
			Point minSize = new Point(99999, 0);
			for (int i = 0; i < TimeFormatter.DATEFORMATS_DESC.length; i++) {
				if (maxWidthUsed[i] > width - PADDING) {
					continue;
				}
				SimpleDateFormat temp = new SimpleDateFormat(
						TimeFormatter.DATEFORMATS_DESC[i] + suffix);
				Point newSize = gc.stringExtent(temp.format(date));
				if (newSize.x < width - PADDING) {
					idxFormat = i;
					if (maxWidthUsed[i] < newSize.x) {
						maxWidthUsed[i] = newSize.x;
						maxWidthDate[i] = date;
					}
					break;
				}
				if (newSize.x < minSize.x) {
					minSize = newSize;
					idxFormat = i;
				}
			}
		} catch (Throwable t) {
			return;
		} finally {
			gc.dispose();
		}

		if (curFormat != idxFormat) {
			//System.out.println("switch fmt to " + idxFormat + ", max=" + maxWidthUsed[idxFormat]);
			curFormat = idxFormat;
			invalidateCells();
		}
	}

	private int calcWidth(Date date, String format) {
		GC gc = new GC(Display.getDefault());
		if (fontBold == null) {
			FontData[] fontData = gc.getFont().getFontData();
			for (int i = 0; i < fontData.length; i++) {
				FontData fd = fontData[i];
				fd.setStyle(SWT.BOLD);
			}
			fontBold = new Font(gc.getDevice(), fontData);
		}
		gc.setFont(fontBold);
		SimpleDateFormat temp = new SimpleDateFormat(format);
		Point newSize = gc.stringExtent(temp.format(date));
		gc.dispose();
		return newSize.x;
	}

	public boolean getShowTime() {
		return showTime;
	}

	public void setShowTime(boolean showTime) {
		this.showTime = showTime;
	}

	/**
	 * @return the multiline
	 */
	public boolean isMultiline() {
		return multiline;
	}

	/**
	 * @param multiline the multiline to set
	 */
	public void setMultiline(boolean multiline) {
		this.multiline = multiline;
	}
}
