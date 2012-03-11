package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.HSLColor;
import org.gudy.azureus2.ui.swt.views.table.TableColumnOrTreeColumn;
import org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class TableViewSWT_EraseItem
	implements Listener
{
	final static public Color[] alternatingColors = new Color[] {
		null,
		Colors.colorAltRow
	};

	private final TableOrTreeSWT table;
	
	private TableViewSWTImpl<?> tv;
	
	private boolean drawExtended;
	
	private boolean first = true;

	private static Color colorLine;
	
	public TableViewSWT_EraseItem(TableViewSWTImpl<?> _tv, TableOrTreeSWT table) {
		this.table = table;
		this.tv = _tv;
		COConfigurationManager.addAndFireParameterListener("Table.extendedErase",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								drawExtended = COConfigurationManager.getBooleanParameter("Table.extendedErase");
								if (!first) {
									Rectangle bounds = tv.getTableOrTreeSWT().getBounds();
									tv.getTableOrTreeSWT().redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
								}
								first = false;
							}
						});
					}
				});

		if (colorLine == null) {
  		colorLine = tv.getComposite().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
  		HSLColor hslColor = new HSLColor();
  		hslColor.initHSLbyRGB(colorLine.getRed(), colorLine.getGreen(),
  				colorLine.getBlue());
  
  		int lum = hslColor.getLuminence();
  		if (lum > 127)
  			lum -= 25;
  		else
  			lum += 40;
  		hslColor.setLuminence(lum);
  		
  		colorLine = new Color(tv.getComposite().getDisplay(), hslColor.getRed(), hslColor.getGreen(), hslColor.getBlue());
		}

		/* Background image badly slows down tree drawing
		int itemHeight = table.getItemHeight() + 1;
		int height = 1920;
		Image image = new Image(table.getComposite().getDisplay(), 10, height);
		GC gc = new GC(image);
		int y = table.getHeaderHeight();
		while (y < height) {
			gc.setBackground(table.getBackground());
			gc.fillRectangle(0, y, 10, itemHeight);
			gc.setBackground(alternatingColors[1]);
			gc.fillRectangle(0, y + itemHeight, 10, itemHeight);
			y += itemHeight * 2;
		}
		gc.dispose();
		table.setBackgroundImage(image);
		*/
	}
	
	public void handleEvent(Event event) {
		if (event.type == SWT.EraseItem) {
			TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
			eraseItem(event, event.gc, item, event.index, drawExtended, item.getBounds(event.index), tv, false);
		} else if (drawExtended) {
			paint(event);
		}
	}

	private void paint(Event event) {
		//System.out.println("paint " + event.getBounds() + ";" + table.getColumnCount());
		int numItems = table.getItemCount();
		int rowHeight = table.getItemHeight();
		int height = rowHeight * numItems;
		int rowAreaHeight = tv.clientArea.height + tv.clientArea.y;

		int blankHeight = rowAreaHeight - height;
		if (blankHeight > 0) {
			int startY;
			if (numItems > 0) {
				TableItemOrTreeItem lastItem = table.getItem(numItems - 1);
					// dunno how, but I have a stacktrace with an NPE caused by lastItem being null (even though numItems > 0 ...)
				if ( lastItem != null ){
					Rectangle lastItemBounds;
					if (lastItem.getExpanded()) {
	  				TableItemOrTreeItem[] subItems = lastItem.getItems();
	  				lastItemBounds = subItems == null || subItems.length == 0
	  						? lastItem.getBounds() : subItems[subItems.length - 1].getBounds();
					} else {
						lastItemBounds = lastItem.getBounds();
					}
					startY = lastItemBounds.y + lastItemBounds.height;
				}else{
					startY = 0;
				}
			} else {
				startY = 0;
			}
			//if (event.width == tv.clientArea.width && event.height < blankHeight) {
			//	System.out.println("MOO");
			//	table.redraw(tv.clientArea.x, startY, tv.clientArea.width, blankHeight, false);
			//	return;
			//}
			//System.out.println("numItems=" + numItems + ";" + startY);
			GC gc = event.gc;
			gc.setClipping((Rectangle) null);
			for (int i = 0; i < blankHeight / rowHeight; i++) {
				int curY = (startY + (i * rowHeight));
				int pos = (i + numItems) % 2;
				//System.out.println(i + "==" + pos + " for " + (startY + (i * rowHeight)));
				Color color = i <= -1 ? ColorCache.getRandomColor() : alternatingColors[pos];
				if (color == null) {
					//color = event.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
					continue;
				}
				gc.setBackground(color);
				gc.fillRectangle(tv.clientArea.x, startY + (i * rowHeight),
						tv.clientArea.width, rowHeight);
			}

			// Vertical lines between columns
			if (TableViewSWTImpl.DRAW_VERTICAL_LINES) {
				TableColumnOrTreeColumn[] columns = table.getColumns();
				int pos = 0;
				//gc.setForeground(ColorCache.getColor(event.display, 245, 245, 245));

				gc.setForeground(Colors.black);
				gc.setAdvanced(true);
				gc.setAlpha(10);
				for (TableColumnOrTreeColumn tc : columns) {
					gc.drawLine(pos - 1, startY, pos - 1, startY + blankHeight);
					pos += tc.getWidth();
				}
				gc.drawLine(pos - 1, startY, pos - 1, startY + blankHeight);
			}
		}
	}

	public static void eraseItem(Event event, GC gc, TableItemOrTreeItem item,
			int columnNo, boolean drawExtended, Rectangle bounds, TableViewSWTImpl<?> tv, boolean alwaysDrawBG) {

		TableOrTreeSWT table = tv.getTableOrTreeSWT();

		if (event == null || (event.detail & (SWT.HOT | SWT.SELECTED | SWT.FOCUSED)) == 0) {

			int pos;
			TableItemOrTreeItem parentItem = item.getParentItem();
			if (parentItem != null) {
				pos = parentItem.indexOf(item) + ((table.indexOf(parentItem) + 1) % 2);
			} else {
				pos = table.indexOf(item);
			}
			Color color = alternatingColors[pos % 2];
			if (color != null) {
				gc.setBackground(color);
				if (parentItem != null) {
					gc.setAlpha(128);
				}
			}
			Rectangle drawBounds = bounds;
			if (TableViewSWTImpl.DRAW_FULL_ROW && drawExtended
					&& columnNo == table.getColumnCount() - 1) {
				tv.swt_calculateClientArea();
				drawBounds = new Rectangle(bounds.x, bounds.y, tv.clientArea.x
						+ tv.clientArea.width - bounds.x, bounds.height);
				gc.setClipping(drawBounds);
				//System.out.println(bounds.width);
			}
			if (alwaysDrawBG || color != null) {
				if (color == null) {
					gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				}
				gc.fillRectangle(drawBounds);
				if (event != null) {
					event.detail &= ~SWT.BACKGROUND;
				}
			}
		}
		
		if (event != null && (event.detail & SWT.SELECTED) > 0 && !table.isFocusControl()) {
			gc.setBackground(Colors.blues[3]);
			gc.fillRectangle(bounds);
			if (event != null) {
				event.detail &= ~SWT.BACKGROUND;
			}
		}

		// Vertical lines between columns
		if (TableViewSWTImpl.DRAW_VERTICAL_LINES && drawExtended && colorLine != null) {
			if (item != null
					&& (bounds.width == item.getParent().getColumn(columnNo).getWidth())
					&& bounds.width > 0) {
				Color fg = gc.getForeground();
				gc.setForeground(colorLine);
				// needed because windows shifts the area over, dragging our old line
				// around.  Clear clipping so we can erase it
				gc.setClipping((Rectangle) null);
				gc.drawLine(bounds.x + bounds.width - 1, bounds.y - 1, bounds.x
						+ bounds.width - 1, bounds.y + bounds.height);
				gc.setForeground(fg);
			}
		}
	}
}
