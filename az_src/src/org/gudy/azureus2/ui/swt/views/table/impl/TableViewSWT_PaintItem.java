package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.InPaintInfo;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnSWTUtils;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class TableViewSWT_PaintItem
	implements Listener
{
	Widget lastItem;

	int lastRowIndex = -1;

	private final TableOrTreeSWT table;

	private TableViewSWTImpl<?> tv;

	private static Font fontBold;

	public TableViewSWT_PaintItem(TableViewSWTImpl<?> tv, TableOrTreeSWT table) {
		this.table = table;
		this.tv = tv;
	}

	private int[] getColumnRange(TableItemOrTreeItem ti, int iMouseX, int width) {
		int[] columnRange = {
			-1,
			-1
		};
		int numColumns = table.getColumnCount();
		if (numColumns <= 0) {
			return columnRange;
		}
		for (int i = tv.bSkipFirstColumn ? 1 : 0; i < numColumns; i++) {
			Rectangle cellBounds = ti.getBounds(i);
			if (iMouseX >= cellBounds.x && iMouseX < cellBounds.x + cellBounds.width
					&& cellBounds.width > 0) {
				columnRange[0] = i;
				break;
			}
		}

		if (columnRange[0] == -1) {
			return columnRange;
		}

		if (columnRange[0] + 1 == numColumns) {
			columnRange[1] = columnRange[0];
		} else {

			int end = iMouseX + width;
			for (int i = columnRange[0] + 1; i < numColumns; i++) {
				Rectangle cellBounds = ti.getBounds(i);
				//System.out.println(cellBounds + ";" + end);
				if (cellBounds.x > end && cellBounds.width > 0) {
					columnRange[1] = i - 1;
					break;
				}
			}
			if (columnRange[1] == -1) {
				columnRange[1] = numColumns - 1;
			}
		}

		return columnRange;
	}

	public void handleEvent(Event event) {

		if (event.gc.getClipping().isEmpty()) {
			return;
		}

		if (!table.isEnabled()) {
			// added disable affect
			event.gc.setAlpha(100);
		}

		if (event.type == SWT.Paint) {
			int numPainted = 0;
			//Color fg = event.gc.getForeground();
			//Color bg = event.gc.getBackground();
			Region r = new Region();
			event.gc.getClipping(r);
			//System.out.println(r.contains(event.x, event.y));
			//event.gc.setForeground(ColorCache.getRandomColor());
			//event.gc.drawRectangle(event.x, event.y, event.width - 1, event.height - 1);
			//event.gc.setForeground(fg);
			// paint only gets bounds, gotta fill event.item and event.index
			TableItemOrTreeItem item = table.getItem(new Point(5, event.y));
			if (item == null) {
				return;
			}
			event.item = item.getItem();
			int[] columnRange = getColumnRange(item, event.x, event.width);
			if (columnRange[0] == -1) {
				return;
			}

			int height = tv.getRowDefaultHeight();

			//System.out.println("Dirty: " + event.getBounds() + "; +" + height + "; " + event.gc.getClipping());
			for (int i = columnRange[0]; i <= columnRange[1]; i++) {
				event.index = i;


				for (int y = event.y; y < (event.y + event.height); y += item.getBounds().height) {
					item = table.getItem(new Point(5, y));
					if (item == null) {
						break;
					}
					Rectangle bounds = item.getBounds(i);
					if (!r.intersects(bounds)) {
						//event.gc.setForeground(ColorCache.getRandomColor());
						//event.gc.
						continue;
					}
					event.item = item.getItem();
					//System.out.println(table.indexOf(event.item) + ": " + i);

					
					table.setData("inPaintInfo", new InPaintInfo(item.getItem(), event.index, bounds));

					if (event.item != lastItem) {
						table.setData("lastIndex", null);
						lastRowIndex = table.indexOf(event.item);
						table.setData("lastIndex", lastRowIndex);
					}

					boolean doErase = true;
					if (Constants.isWindows7OrHigher) {
						Point location = table.toControl(event.display.getCursorLocation());
						if (location.y >= bounds.y && location.y < bounds.y + bounds.height) {
							doErase = false;
						}
					}

					Font f = event.gc.getFont();
					if (doErase) {
						//TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
						TableViewSWT_EraseItem.eraseItem(event, event.gc, item, event.index, true, bounds, tv, true);
					}
					//TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
					paintItem(event.gc, item, event.index, lastRowIndex, bounds, tv, false);
					event.gc.setFont(f);
					event.gc.setClipping(r);
					
					numPainted++;

					lastItem = event.item;
				}

			}
			
			//System.out.println("# Painted:" + numPainted);

		} else {
			TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
			Rectangle bounds = item.getBounds(event.index);

			table.setData("inPaintInfo", new InPaintInfo((Item) event.item, event.index, bounds));

			if (event.item != lastItem) {
				table.setData("lastIndex", null);
				lastRowIndex = table.indexOf(event.item);
				table.setData("lastIndex", lastRowIndex);
			}

			//visibleRowsChanged();
			paintItem(event.gc, item, event.index, lastRowIndex, bounds, tv, false);

			lastItem = event.item;
		}

		table.setData("inPaintInfo", null);
		table.setData("lastIndex", null);
	}

	/**
	 * @param event
	 * @param rowIndex 
	 */
	public static void paintItem(GC gc, TableItemOrTreeItem item, int columnIndex,
			int rowIndex, Rectangle _cellBounds, TableViewSWTImpl<?> tv, boolean skipClipCalc) {
		//if (columnIndex == 1 && rowIndex == 0) {
		//	System.out.println("paintItem " + gc.getClipping() +":" + rowIndex + ":" + event.detail + ": " + Debug.getCompressedStackTrace());
		//}
		TableOrTreeSWT table = tv.getTableOrTreeSWT();
		try {
			//System.out.println(gc.getForeground().getRGB().toString());
			//System.out.println("paintItem " + gc.getClipping());
			if (TableViewSWTImpl.DEBUG_CELL_CHANGES) {
				Random random = new Random(SystemTime.getCurrentTime() / 500);
				gc.setBackground(ColorCache.getColor(gc.getDevice(),
						210 + random.nextInt(45), 210 + random.nextInt(45),
						210 + random.nextInt(45)));
				gc.fillRectangle(gc.getClipping());
			}

			if (item == null || item.isDisposed()) {
				return;
			}
			int iColumnNo = columnIndex;

			//System.out.println(SystemTime.getCurrentTime() + "] paintItem " + table.indexOf(item) + ":" + iColumnNo);
			if (tv.bSkipFirstColumn) {
				if (iColumnNo == 0) {
					return;
				}
				iColumnNo--;
			}

			TableColumnCore[] columnsOrdered = tv.getColumnsOrdered();

			if (iColumnNo >= columnsOrdered.length) {
				System.out.println("Col #" + iColumnNo + " >= " + columnsOrdered.length
						+ " count");
				return;
			}

			if (!tv.isColumnVisible(columnsOrdered[iColumnNo])) {
				//System.out.println("col not visible " + iColumnNo);
				return;
			}

			//if (rowIndex < tv.lastTopIndex || rowIndex > tv.lastBottomIndex) {
			// this refreshes whole row (perhaps multiple), saving the many
			// cell.refresh calls later because !cell.isUpToDate()
			//tv.visibleRowsChanged();
			//}

			Rectangle cellBounds = new Rectangle(_cellBounds.x, _cellBounds.y, _cellBounds.width, _cellBounds.height); // item.getBounds(columnIndex);

			//System.out.println("cb=" + cellBounds + ";b=" + event.getBounds() + ";clip=" + gc.getClipping());
			Rectangle origClipping = gc.getClipping();

			if (origClipping.isEmpty()
					|| (origClipping.width >= cellBounds.width && origClipping.height >= cellBounds.height)) {
				table.setData("fullPaint", Boolean.TRUE);
			} else {
				table.setData("fullPaint", Boolean.FALSE);
				//System.out.println("not full paint: " + origClipping + ";cellbounds=" + cellBounds);
			}

			TableRowSWT row = (TableRowSWT) tv.getRow(item);
			if (row == null) {
				//System.out.println("no row");
				return;
			}

			if (!tv.isRowVisible(row)) {
				tv.visibleRowsChanged();
			}

			tv.invokePaintListeners(gc, row, columnsOrdered[iColumnNo], cellBounds);

			int rowAlpha = row.getAlpha();

			int fontStyle = row.getFontStyle();
			if (fontStyle == SWT.BOLD) {
				gc.setFont(getFontBold(gc));
			}

			//if (item.getImage(columnIndex) != null) {
			//	cellBounds.x += 18;
			//	cellBounds.width -= 18;
			//}

			if (cellBounds.width <= 0 || cellBounds.height <= 0) {
				//System.out.println("no bounds");
				return;
			}

			TableCellSWT cell = row.getTableCellSWT(columnsOrdered[iColumnNo].getName());

			if (cell == null) {
				return;
			}

			if (!cell.isUpToDate()) {
				//System.out.println("R " + table.indexOf(item) + ":" + iColumnNo);
				cell.refresh(true, true);
				//return;
			}

			String text = cell.getText();

			if (!skipClipCalc) {
				Rectangle clipping = new Rectangle(cellBounds.x, cellBounds.y,
  					cellBounds.width, cellBounds.height);
  			// Cocoa calls paintitem while row is below tablearea, and painting there
  			// is valid!
  			if (!Utils.isCocoa) {
  				int iMinY = tv.headerHeight + tv.clientArea.y;
  
  				if (clipping.y < iMinY) {
  					clipping.height -= iMinY - clipping.y;
  					clipping.y = iMinY;
  				}
  				int iMaxY = tv.clientArea.height + tv.clientArea.y;
  				if (clipping.y + clipping.height > iMaxY) {
  					clipping.height = iMaxY - clipping.y + 1;
  				}
  			}
  
  			if (clipping.width <= 0 || clipping.height <= 0) {
  				//System.out.println(row.getIndex() + " clipping="+clipping + ";" );
  				return;
  			}
  
  			if (!origClipping.contains(clipping.x, clipping.y)
  					|| !origClipping.contains(clipping.x + clipping.width - 1, clipping.y
  							+ clipping.height - 1)) {
  				gc.setClipping(clipping);
  			}
			}

			if (rowAlpha < 255) {
				gc.setAlpha(rowAlpha);
			}

			if (cell.needsPainting()) {
				Image graphicSWT = cell.getGraphicSWT();
				if (graphicSWT != null && !graphicSWT.isDisposed()) {
					int marginWidth = cell.getMarginWidth();
					int marginHeight = cell.getMarginHeight();
					Rectangle graphicBounds = new Rectangle(cellBounds.x + marginWidth,
							cellBounds.y + marginHeight,
							cellBounds.width - (marginWidth * 2), cellBounds.height
									- (marginHeight * 2));
					Rectangle imageBounds = graphicSWT.getBounds();
					BufferedTableItem bufferedTableItem = cell.getBufferedTableItem();
					if (bufferedTableItem instanceof BufferedGraphicTableItem) {
						BufferedGraphicTableItem ti = (BufferedGraphicTableItem) bufferedTableItem;
						int orientation = ti.getOrientation();
						
						if (orientation == SWT.FILL) {
							if (!graphicBounds.isEmpty()) {
								gc.setAdvanced(true);
								//System.out.println(imageBounds + ";" + graphicBounds);
  							gc.drawImage(graphicSWT, 0, 0, imageBounds.width,
  									imageBounds.height, graphicBounds.x, graphicBounds.y,
  									graphicBounds.width, graphicBounds.height);
							}
						} else {
						
  			  		if (imageBounds.width < graphicBounds.width) {
    			    	if (orientation == SWT.CENTER) {
    			    		graphicBounds.x += (graphicBounds.width - imageBounds.width) / 2;
    			    	} else if (orientation == SWT.RIGHT) {
    			    		graphicBounds.x = (graphicBounds.x + graphicBounds.width) - imageBounds.width;
    			    	}
  			  		}
  			  		
  			  		if (imageBounds.height < graphicBounds.height) {
  			  			graphicBounds.y += (graphicBounds.height - imageBounds.height) / 2;
  			  		}

  			  		gc.drawImage(graphicSWT, graphicBounds.x, graphicBounds.y);
						}

					} else {
			  		gc.drawImage(graphicSWT, graphicBounds.x, graphicBounds.y);
					}
				}
				cell.doPaint(gc);
			}
			if (text.length() > 0) {
				int ofsx = 0;
				Image image = cell.getIcon();
				Rectangle imageBounds = null;
				if (image != null && !image.isDisposed()) {
					imageBounds = image.getBounds();
					int ofs = imageBounds.width;
					ofsx += ofs;
					cellBounds.x += ofs;
					cellBounds.width -= ofs;
				}
				//System.out.println("PS " + table.indexOf(item) + ";" + cellBounds + ";" + cell.getText());
				int style = TableColumnSWTUtils.convertColumnAlignmentToSWT(columnsOrdered[iColumnNo].getAlignment());
				if (cellBounds.height > 20) {
					style |= SWT.WRAP;
				}
				int textOpacity = cell.getTextAlpha();
				if (textOpacity < 255) {
					gc.setTextAntialias(SWT.ON);
					gc.setAlpha(textOpacity);
				} else if (textOpacity > 255) {
					gc.setFont(getFontBold(gc));
					gc.setTextAntialias(SWT.ON);
					gc.setAlpha(textOpacity & 255);
				}
				// put some padding on text
				ofsx += 6;
				cellBounds.x += 3;
				cellBounds.width -= 6;
				cellBounds.y += 2;
				cellBounds.height -= 4;
				if (!cellBounds.isEmpty()) {
					GCStringPrinter sp = new GCStringPrinter(gc, text, cellBounds, true,
							cellBounds.height > 20, style);
					
					boolean fit = sp.printString();
					if (fit) {

						cell.setDefaultToolTip(null);
					} else {

						cell.setDefaultToolTip(text);
					}

					Point size = sp.getCalculatedSize();
					size.x += ofsx;

					if (cell.getTableColumn().getPreferredWidth() < size.x) {
						cell.getTableColumn().setPreferredWidth(size.x);
					}

					if (imageBounds != null) {
						int drawToY = cellBounds.y + (cellBounds.height / 2)
								- (imageBounds.height / 2);
						if ((style & SWT.RIGHT) > 0) {
							int drawToX = cellBounds.x + cellBounds.width - size.x;
							gc.drawImage(image, drawToX, drawToY);
						} else {
							gc.drawImage(image, cellBounds.x - imageBounds.width - 3, drawToY);
						}
					}
				} else {
					cell.setDefaultToolTip(null);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Font getFontBold(GC gc) {
		if (fontBold == null) {
			FontData[] fontData = gc.getFont().getFontData();
			for (int i = 0; i < fontData.length; i++) {
				FontData fd = fontData[i];
				fd.setStyle(SWT.BOLD);
			}
			fontBold = new Font(gc.getDevice(), fontData);
		}
		return fontBold;
	}

}
