/*
 * File    : TableCellImpl.java
 * Created : 24 nov. 2003
 * By      : Olivier
 * Originally PluginItem.java, and changed to be more generic.
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

import java.text.Collator;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Item;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.*;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnSWTUtils;

import com.aelitis.azureus.ui.common.table.*;


/** TableCellImpl represents one cell in the table.  
 * Table access is provided by BufferedTableItem.  
 * TableCellImpl is stored in and accessed by TableRowCore.
 * Drawing control gets passed to listeners.
 *
 * For plugins, this object is the implementation to TableCell.
 *
 * This object is needed to split core code from plugin code.
 */
public class TableCellImpl 
       implements TableCellSWT
{
	private static final LogIDs LOGID = LogIDs.GUI;
	
	private static final boolean canUseQuickDraw = Constants.isWindows;
	
	private static final byte FLAG_VALID = 1;
	private static final byte FLAG_SORTVALUEISTEXT = 2;
	private static final byte FLAG_TOOLTIPISAUTO = 4;
  /**
   * For refreshing, this flag manages whether the row is actually up to date.
   * 
   * We don't update any visuals while the row isn't visible.  But, validility
   * does get set to true so that the cell isn't forced to refresh every
   * cycle when not visible.  (We can't just never call refresh when the row
   * is not visible, as refresh also sets the sort value)
   *  
   * When the row does become visible, we have to invalidate the row so
   * that the row will set its visuals again (this time, actually
   * updating a viewable object).
   */
	private static final byte FLAG_UPTODATE = 8;
	private static final byte FLAG_DISPOSED = 16;
	private static final byte FLAG_MUSTREFRESH = 32;
	private static final byte FLAG_VISUALLY_CHANGED_SINCE_REFRESH = 64;
	
	private byte flags;

  private TableRowCore tableRow;
  private Comparable sortValue;
  private BufferedTableItem bufferedTableItem;
  private ArrayList refreshListeners;
  private ArrayList disposeListeners;
  private ArrayList tooltipListeners;
	private ArrayList cellMouseListeners;
	private ArrayList cellMouseMoveListeners;
	private ArrayList cellVisibilityListeners;
	private ArrayList cellSWTPaintListeners;
	private ArrayList<TableCellClipboardListener> cellClipboardListeners;
  private TableColumnCore tableColumn;
  private byte refreshErrLoopCount;
  private byte tooltipErrLoopCount;
  private byte loopFactor;
  private Object oToolTip;
  private Object defaultToolTip;
	private int iCursorID = SWT.CURSOR_ARROW;
	private Graphic graphic = null;
	
	private ArrayList childCells;
  
	public boolean bDebug = false;
  
  private static AEMonitor 	this_mon 	= new AEMonitor( "TableCell" );

  // Getting the cell's bounds can be slow.  QUICK_WIDTH uses TableColumn's width
	private static final boolean QUICK_WIDTH = true;

	private static int MAX_REFRESHES = 10;
	private static int MAX_REFRESHES_WITHIN_MS = 100;

	private boolean bInRefresh = false;
	private long lastRefresh;
	private int numFastRefreshes;

	private byte restartRefresh = 0;
	private boolean bInRefreshAsync = false;
	
	private int textAlpha = 255;

	private Image icon;
	
  public TableCellImpl(TableRowCore _tableRow, TableColumnCore _tableColumn,
      int position, BufferedTableItem item) {
    this.tableColumn = _tableColumn;
    this.tableRow = _tableRow;
    flags = FLAG_SORTVALUEISTEXT;
    refreshErrLoopCount = 0;
    tooltipErrLoopCount = 0;
    loopFactor = 0;

    if (item != null) {
    	bufferedTableItem = item;
    } else {
    	createBufferedTableItem(position);
    }

    tableColumn.invokeCellAddedListeners(TableCellImpl.this);
    //bDebug = (position == 1) && tableColumn.getTableID().equalsIgnoreCase("Peers");
  }

  /**
   * Initialize
   *  
   * @param _tableRow
   * @param _tableColumn
   * @param position
   */
  public TableCellImpl(TableRowSWT _tableRow, TableColumnCore _tableColumn,
                       int position) {
  	this(_tableRow, _tableColumn, position, null);
  }
  
  private void createBufferedTableItem(int position) {
    BufferedTableRow bufRow = (BufferedTableRow)tableRow;
    if (tableColumn.getType() == TableColumnCore.TYPE_GRAPHIC) {
      bufferedTableItem = new BufferedGraphicTableItem1(bufRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
        	clearFlag(FLAG_VALID);
        	redraw();
        }
        protected void quickRedrawCell(TableOrTreeSWT table, Rectangle dirty, Rectangle cellBounds) {
        	TableItemOrTreeItem item = row.getItem();
					boolean ourQuickRedraw = canUseQuickDraw && tableRow != null
							&& !tableRow.isMouseOver() && !tableRow.isSelected();
					if (ourQuickRedraw) {
      			TableCellImpl.this.quickRedrawCell2(table, item, dirty, cellBounds);
      		} else {
      			super.quickRedrawCell(table, dirty, cellBounds);
      		}
        }
      };
    	setOrientationViaColumn();
    } else {
			bufferedTableItem = new BufferedTableItemImpl(bufRow, position) {
				public void refresh() {
					TableCellImpl.this.refresh();
				}

				public void invalidate() {
					clearFlag(FLAG_VALID);
				}

				protected void quickRedrawCell(TableOrTreeSWT table, Rectangle dirty,
						Rectangle cellBounds) {
					TableItemOrTreeItem item = row.getItem();
					boolean ourQuickRedraw = canUseQuickDraw && tableRow != null
							&& !tableRow.isMouseOver() && !tableRow.isSelected();
					if (ourQuickRedraw) {
						TableCellImpl.this.quickRedrawCell2(table, item, dirty, cellBounds);
					} else {
						super.quickRedrawCell(table, dirty, cellBounds);
					}
				}
			};
    }
  }

	protected void quickRedrawCell2(TableOrTreeSWT table,
			TableItemOrTreeItem tableItemOrTreeItem, Rectangle dirty,
			Rectangle cellBounds) {
		if (bufferedTableItem.isInPaintItem()) {
			return;
		}
		Rectangle bounds = new Rectangle(0, 0, cellBounds.width, cellBounds.height);
		Point pt = new Point(cellBounds.x, cellBounds.y);
		Image img = new Image(table.getDisplay(), bounds);

		int colPos = bufferedTableItem.getPosition();

		Item item = tableItemOrTreeItem.getItem();
		table.setData("inPaintInfo", new InPaintInfo(item, colPos, bounds));
		table.setData("fullPaint", Boolean.TRUE);

		GC gc = new GC(img);
		try {
			TableViewSWTImpl tv = (TableViewSWTImpl) tableRow.getView();
			TableViewSWT_EraseItem.eraseItem(null, gc, tableItemOrTreeItem, colPos,
					false, bounds, tv, true);
			//gc.setBackground(ColorCache.getRandomColor());
			//gc.fillRectangle(bounds);

			Color fg = getForegroundSWT();
			if (fg != null) {
				gc.setForeground(fg);
			}
			gc.setBackground(getBackgroundSWT());

			TableViewSWT_PaintItem.paintItem(gc, tableItemOrTreeItem, colPos,
					tableRow.getIndex(), bounds, tv, true);
		} finally {
			gc.dispose();
		}

		gc = new GC(table.getComposite());
		try {
			//System.out.println("draw " + bounds);
			gc.drawImage(img, pt.x, pt.y);
		} finally {
			img.dispose();
			gc.dispose();
		}

		table.setData("inPaintInfo", null);
		table.setData("fullPaint", Boolean.FALSE);
	}

	protected void quickRedrawCell(TableOrTreeSWT table,
			TableItemOrTreeItem tableItemOrTreeItem, Rectangle dirty,
			Rectangle cellBounds) {
		if (bufferedTableItem.isInPaintItem()) {
			return;
		}
		
		int colPos = bufferedTableItem.getPosition();

		Item item = tableItemOrTreeItem.getItem();
		table.setData("inPaintInfo", new InPaintInfo(item, colPos, cellBounds));
		table.setData("fullPaint", Boolean.TRUE);

		GC gc = new GC(table.getComposite());
		try {
			TableViewSWTImpl tv = (TableViewSWTImpl) tableRow.getView();
			TableViewSWT_EraseItem.eraseItem(null, gc, tableItemOrTreeItem,
					bufferedTableItem.getPosition(), true, cellBounds, tv, true);

			Color fg = getForegroundSWT();
			if (fg != null) {
				gc.setForeground(fg);
			}
			gc.setBackground(getBackgroundSWT());
			
			TableViewSWT_PaintItem.paintItem(gc, tableItemOrTreeItem,
					bufferedTableItem.getPosition(), tableRow.getIndex(), cellBounds, tv, true);
		} finally {
			gc.dispose();
		}

		table.setData("inPaintInfo", null);
		table.setData("fullPaint", Boolean.FALSE);
	}

	private void pluginError(Throwable e) {
    String sTitleLanguageKey = (tableColumn==null?"?":tableColumn.getTitleLanguageKey());

    String sPosition = (bufferedTableItem == null) 
      ? "null" 
      : "" + bufferedTableItem.getPosition() + 
        " (" + MessageText.getString(sTitleLanguageKey) + ")";
    Logger.log(new LogEvent(LOGID, "Table Cell Plugin for Column #" + sPosition
				+ " generated an exception ", e));
  }

  private void pluginError(String s) {
    String sTitleLanguageKey = tableColumn.getTitleLanguageKey();

		String sPosition = "r"
				+ tableRow.getIndex()
				+ (bufferedTableItem == null ? "null" : "c"
						+ bufferedTableItem.getPosition() + " ("
						+ MessageText.getString(sTitleLanguageKey) + ")");
		Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
				"Table Cell Plugin for Column #" + sPosition + ":" + s + "\n  "
						+ Debug.getStackTrace(true, true)));
  }
  
  /* Public API */
  ////////////////
  
  public Object getDataSource() {
		// if we've been disposed then row/col are null
	  
	TableRowCore	row = tableRow;
	TableColumnCore	col	= tableColumn;
	
	if ( row == null || col == null){
		return( null );
	}
	
    return row.getDataSource(col.getUseCoreDataSource());
  }
  
  public TableColumn getTableColumn() {
    return tableColumn;
  }

  public TableRow getTableRow() {
    return tableRow;
  }

  public String getTableID() {
    return tableRow.getTableID();
  }
  
  public boolean isValid() {
  	// Called often.. inline faster
  	return (flags & FLAG_VALID) != 0;
    //return hasFlag(FLAG_VALID);
  }
  
  public Color getForegroundSWT() {
		if (isDisposed()) {
			return null;
		}

    return bufferedTableItem.getForeground();
  }
  
  public Color getBackgroundSWT() {
		if (isDisposed()) {
			return null;
		}

		return bufferedTableItem.getBackground();
	}

  public int[] getBackground() {
		if (bufferedTableItem == null) {
			return null;
		}
		Color color = bufferedTableItem.getBackground();

		if (color == null) {
			return null;
		}

		return new int[] {
			color.getRed(),
			color.getGreen(),
			color.getBlue()
		};
	}

  // @see org.gudy.azureus2.plugins.ui.tables.TableCell#getForeground()
  public int[] getForeground() {
  	if (bufferedTableItem == null) {
  		return new int[] { 0, 0, 0 };
  	}
		Color color = bufferedTableItem.getForeground();

		if (color == null) {
			return new int[3];
		}

		return new int[] { color.getRed(), color.getGreen(), color.getBlue()
		};
	}
  
  public boolean setForeground(Color color) {
		if (isDisposed()) {
			return false;
		}

  	// Don't need to set when not visible
  	if (isInvisibleAndCanRefresh())
  		return false;

    boolean set = bufferedTableItem.setForeground(color);
    if (set) {
    	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
    }
    return set;
  }

	public boolean setForeground(int red, int green, int blue) {
		if (isDisposed()) {
			return false;
		}

		// Don't need to set when not visible
		if (isInvisibleAndCanRefresh())
			return false;

		boolean set;
		if (red < 0 || green < 0 || blue < 0) {
			set = bufferedTableItem.setForeground(null);
		} else {
			set = bufferedTableItem.setForeground(red, green, blue);
		}
		if (set) {
			setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
		}
		return set;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForeground(int[])
	public boolean setForeground(int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			return setForeground((Color) null);
		}
		return setForeground(rgb[0], rgb[1], rgb[2]);
	}

  public boolean setForegroundToErrorColor() {
	  return setForeground(Colors.colorError);
  }

  public boolean setText(String text) {
		if (isDisposed()) {
			return false;
		}

		if (text == null)
  		text = "";
  	boolean bChanged = false;

  	if (hasFlag(FLAG_SORTVALUEISTEXT) && !text.equals(sortValue)) {
  		bChanged = true;
  		sortValue = text;
    	tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
    	if (bDebug)
    		debug("Setting SortValue to text;");
  	}
  	

// Slower than setText(..)!
//  	if (isInvisibleAndCanRefresh()) {
//  		if (bDebug) {
//  			debug("setText ignored: invisible");
//  		}
//  		return false;
//  	}

    if (bufferedTableItem.setText(text) && !hasFlag(FLAG_SORTVALUEISTEXT))
    	bChanged = true;

		if (bDebug) {
			debug("setText (" + bChanged + ") : " + text);
		}

		if (bChanged) {
			setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
		}

		boolean do_auto = this.tableColumn.doesAutoTooltip();
		
		// If we were using auto tooltips (and we aren't any more), then
		// clear up previously set tooltips.
		if (!do_auto) {
			if (hasFlag(FLAG_TOOLTIPISAUTO)) {
				this.oToolTip = null;
				clearFlag(FLAG_TOOLTIPISAUTO);
			}
		}
		
		else {
			this.oToolTip = text;
			setFlag(FLAG_TOOLTIPISAUTO);
		}
		
  	return bChanged;
  }
  
  private boolean isInvisibleAndCanRefresh() {
  	return !isShown()
				&& (refreshListeners != null || tableColumn.hasCellRefreshListener());
  }
  
  public String getText() {
  	if (hasFlag(FLAG_SORTVALUEISTEXT) && sortValue instanceof String)
  		return (String)sortValue;
  	if (bufferedTableItem == null) {
  		return null;
  	}
    return bufferedTableItem.getText();
  }

  public boolean isShown() {
  	if (bufferedTableItem == null) {
  		return false;
  	}

    return bufferedTableItem.isShown()
				&& tableRow.getView().isColumnVisible(tableColumn);
  }
  
  public boolean setSortValue(Comparable valueToSort) {
		if (!tableColumn.isSortValueLive()) {
			// objects that can't change aren't live
			if (!(valueToSort instanceof Number) && !(valueToSort instanceof String)
					&& !(valueToSort instanceof TableColumnSortObject)) {
				tableColumn.setSortValueLive(true);
			}
		}
		return _setSortValue(valueToSort);
	}

  private boolean _setSortValue(Comparable valueToSort) {
		if (isDisposed()) {
			return false;
		}

    if (sortValue == valueToSort)
      return false;

    if (hasFlag(FLAG_SORTVALUEISTEXT)) {
    	clearFlag(FLAG_SORTVALUEISTEXT);
    	if (sortValue instanceof String)
	    	// Make sure text is actually in the cell (it may not have been if
	      // cell wasn't created at the time of setting)
	      setText((String)sortValue);
    }
    
    if ((valueToSort instanceof String) && (sortValue instanceof String)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

    if ((valueToSort instanceof Number) && (sortValue instanceof Number)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

  	if (bDebug)
  		debug("Setting SortValue to "
					+ ((valueToSort == null) ? "null" : valueToSort.getClass().getName()));
  	
  	tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
    sortValue = valueToSort;

    // Columns with SWT Paint Listeners usually rely on a repaint whenever the
    // sort value changes
  	if (cellSWTPaintListeners != null
				|| tableColumn.hasCellOtherListeners("SWTPaint")) {
  		redraw();
  	}

    return true;
  }
  
  public boolean setSortValue(long valueToSort) {
		if (isDisposed()) {
			return false;
		}

		if ((sortValue instanceof Long)
				&& ((Long) sortValue).longValue() == valueToSort)
			return false;

		return _setSortValue(Long.valueOf(valueToSort));
  }
  
  public boolean setSortValue( float valueToSort ) {
		if (isDisposed()) {
			return false;
		}

		if (sortValue instanceof Float
				&& ((Float) sortValue).floatValue() == valueToSort)
			return false;

		return _setSortValue(new Float(valueToSort));
  }

  public Comparable getSortValue() {
  	if (bDebug)
			debug("GetSortValue;"
					+ (sortValue == null ? "null" : sortValue.getClass().getName() + ";"
							+ sortValue.toString()));

    if (sortValue == null) {
      if (bufferedTableItem != null)
        return bufferedTableItem.getText();
      return "";
    }
    return sortValue;
  }
  
  public void setToolTip(Object tooltip) {
    oToolTip = tooltip;

    if (tooltip == null) {
    	setFlag(FLAG_TOOLTIPISAUTO);
    } else {
    	clearFlag(FLAG_TOOLTIPISAUTO);
    }
  }

  public Object getToolTip() {
    return oToolTip;
  }

  public Object getDefaultToolTip() {
	return defaultToolTip;
  }
  public void setDefaultToolTip(Object tt) {
	defaultToolTip = tt;
  }
  
	public boolean isDisposed() {
		return (flags & FLAG_DISPOSED) != 0;
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getMaxLines()
	public int getMaxLines() {
		if (bufferedTableItem == null) {
			// use 1 in case some plugin borks on div by zero
			return 1;
		}
		return bufferedTableItem.getMaxLines();
	}
  
  /* Start TYPE_GRAPHIC Functions */

	public Point getSize() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    return ((BufferedGraphicTableItem)bufferedTableItem).getSize();
  }

  public int getWidth() {
  	if (isDisposed()) {
  		return -1;
  	}
  	if (QUICK_WIDTH) {
  		return tableColumn.getWidth() - 2 - (getMarginWidth() * 2);
  	}
  	Point pt = null;
  	
    if (bufferedTableItem instanceof BufferedGraphicTableItem) {
    	pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    } else {
    	Rectangle bounds = bufferedTableItem.getBounds();
    	if (bounds != null) {
    		pt = new Point(bounds.width, bounds.height);
    	}
    }
    if (pt == null)
      return -1;
    return pt.x;
  }

  public int getHeight() {
  	return bufferedTableItem.getHeight();
  }

  // @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#setGraphic(org.eclipse.swt.graphics.Image)
  public boolean setGraphic(Image img) {
		if (isDisposed()) {
			return false;
		}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;

    graphic = null;
    boolean b = ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(img);
    if (b) {
    	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
			bufferedTableItem.redraw();
    }
    return b;
  }

  // @see org.gudy.azureus2.plugins.ui.tables.TableCell#setGraphic(org.gudy.azureus2.plugins.ui.Graphic)
  public boolean setGraphic(Graphic img) {
  	if (img != null){
  		if (isDisposed()) {
  			return false;
  		}
  	}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;

    if (img == graphic && numFastRefreshes >= MAX_REFRESHES) {
    	pluginError("TableCellImpl::setGraphic to same Graphic object. "
					+ "Forcing refresh.");
    }
    
    graphic = img;

    if (img == null) {
      boolean b = ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(null);
      if (b) {
      	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
  			bufferedTableItem.redraw();
      }
    }

    if (img instanceof UISWTGraphic){
    	Image imgSWT = ((UISWTGraphic)img).getImage();
    	boolean b = ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(imgSWT);
      if (b) {
      	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
  			bufferedTableItem.redraw();
      }
    }
    
    return( false );
  }

  public Graphic getGraphic() {
  	if (graphic != null) {
  		return graphic;
  	}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    Image img = ((BufferedGraphicTableItem)bufferedTableItem).getGraphic();
    return new UISWTGraphicImpl(img);
  }

  public Image getGraphicSWT() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    return ((BufferedGraphicTableItem)bufferedTableItem).getGraphic();
  }

  public void setFillCell(boolean bFillCell) {
		if (isDisposed()) {
			return;
		}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    
    if (bFillCell)
    	((BufferedGraphicTableItem)bufferedTableItem).setOrientation(SWT.FILL);
    else
    	setOrientationViaColumn();
    setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
  }

	public void setMarginHeight(int height) {
		if (isDisposed()) {
			return;
		}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).setMargin(-1, height);
    setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
  }

  public void setMarginWidth(int width) {
		if (isDisposed()) {
			return;
		}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).setMargin(width, -1);
    setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
  }

	public int getMarginHeight() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return 0;
    return ((BufferedGraphicTableItem)bufferedTableItem).getMarginHeight();
  }

  public int getMarginWidth() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return 0;
    return ((BufferedGraphicTableItem)bufferedTableItem).getMarginWidth();
  }

  /* End TYPE_GRAPHIC Functions */

  public void addRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (refreshListeners == null)
  			refreshListeners = new ArrayList(1);

  		if (bDebug) {
  			debug("addRefreshListener; count=" + refreshListeners.size());
  		}
  		refreshListeners.add(listener);
  		
  	}finally{
  		this_mon.exit();
  	}
  }

  public void removeRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  
	    if (refreshListeners == null)
	      return;
	
	    refreshListeners.remove(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void addDisposeListener(TableCellDisposeListener listener) {
  	try{
  		this_mon.enter();
  
	    if (disposeListeners == null) {
	      disposeListeners = new ArrayList(1);
	    }
	    disposeListeners.add(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void removeDisposeListener(TableCellDisposeListener listener) {
  	try{
  		this_mon.enter();
  
  		if (disposeListeners == null)
  			return;

  		disposeListeners.remove(listener);
  		
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  public void addToolTipListener(TableCellToolTipListener listener) {
  	try{
  		this_mon.enter();
  
  		if (tooltipListeners == null) {
  			tooltipListeners = new ArrayList(1);
  		}
  		tooltipListeners.add(listener);
  		
  	}finally{
  		this_mon.exit();
  	}
  }

  public void removeToolTipListener(TableCellToolTipListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (tooltipListeners == null)
  			return;

  		tooltipListeners.remove(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  
	public void addMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				cellMouseListeners = new ArrayList(1);

			cellMouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				return;

			cellMouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null)
				cellMouseMoveListeners = new ArrayList(1);

			cellMouseMoveListeners.add(listener);
			
		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null)
				return;

			cellMouseMoveListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				cellVisibilityListeners = new ArrayList(1);

			cellVisibilityListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				return;

			cellVisibilityListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	/**
	 * @param listenerObject
	 *
	 * @since 3.1.1.1
	 */
	private void addSWTPaintListener(TableCellSWTPaintListener listener) {
		try {
			this_mon.enter();

			if (cellSWTPaintListeners == null)
				cellSWTPaintListeners = new ArrayList(1);

			cellSWTPaintListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void invokeSWTPaintListeners(GC gc) {
  	if (tableColumn != null) {
			Object[] swtPaintListeners = tableColumn.getCellOtherListeners("SWTPaint");
			if (swtPaintListeners != null) { 
  			for (int i = 0; i < swtPaintListeners.length; i++) {
  				try {
  					TableCellSWTPaintListener l = (TableCellSWTPaintListener) swtPaintListeners[i];
  
  					l.cellPaint(gc, this);
  
  				} catch (Throwable e) {
  					Debug.printStackTrace(e);
  				}
  			}
			}
		}

		if (cellSWTPaintListeners == null) {
			return;
		}
		

		for (int i = 0; i < cellSWTPaintListeners.size(); i++) {
			try {
				TableCellSWTPaintListener l = (TableCellSWTPaintListener) (cellSWTPaintListeners.get(i));

				l.cellPaint(gc, this);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	private void addCellClipboardListener(TableCellClipboardListener listener) {
		try {
			this_mon.enter();

			if (cellClipboardListeners == null)
				cellClipboardListeners = new ArrayList<TableCellClipboardListener>(1);

			cellClipboardListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public String getClipboardText() {
		if (isDisposed()) {
			return "";
		}
		String text = tableColumn.getClipboardText(this);
		if (text != null) {
			return text;
		}

		try {
			this_mon.enter();

			if (cellClipboardListeners != null) {
				for (TableCellClipboardListener l : cellClipboardListeners) {
					try {
						text = l.getClipboardText(this);
					} catch (Exception e) {
						Debug.out(e);
					}
					if (text != null) {
						break;
					}
				}
			}
		} finally {
			this_mon.exit();
		}
		if (text == null) {
			text = this.getText();
		}
		return text;
	}

	public void addListeners(Object listenerObject) {
		if (listenerObject instanceof TableCellDisposeListener) {
			addDisposeListener((TableCellDisposeListener)listenerObject);
		}

		if (listenerObject instanceof TableCellRefreshListener)
			addRefreshListener((TableCellRefreshListener)listenerObject);

		if (listenerObject instanceof TableCellToolTipListener)
			addToolTipListener((TableCellToolTipListener)listenerObject);

		if (listenerObject instanceof TableCellMouseMoveListener) {
			addMouseMoveListener((TableCellMouseMoveListener) listenerObject);
		}

		if (listenerObject instanceof TableCellMouseListener) {
			addMouseListener((TableCellMouseListener) listenerObject);
		}

		if (listenerObject instanceof TableCellVisibilityListener)
			addVisibilityListener((TableCellVisibilityListener)listenerObject);

		if (listenerObject instanceof TableCellSWTPaintListener)
			addSWTPaintListener((TableCellSWTPaintListener)listenerObject);

		if (listenerObject instanceof TableCellClipboardListener)
			addCellClipboardListener((TableCellClipboardListener)listenerObject);
	}

	/**
	 * If a plugin in trying to invalidate a cell, then clear the sort value
	 * too.
	 */
	public void invalidate() {
		if (isDisposed()) {
			return;
		}

  	invalidate(true);
	}

	/* Start of Core-Only function */
  //////////////////////////////////
	
	public void redraw() {
		if (!tableRow.isVisible()) {
			return;
		}
		if (bufferedTableItem != null) {
			bufferedTableItem.redraw();
		}
	}
	
  public void invalidate(final boolean bMustRefresh) {
  	//if (bInRefresh && Utils.isThisThreadSWT()) {
  	//	System.out.println("Invalidating when in refresh via " + Debug.getCompressedStackTrace());
  	//}
  	if ((flags & FLAG_VALID) == 0) { //!hasFlag(FLAG_VALID)
  		if (bMustRefresh) {
  			if ((flags & FLAG_MUSTREFRESH) != 0) {
  				return;
  			}
  		} else {
  			return;
  		}
  	}
  	clearFlag(FLAG_VALID);
  	
  	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);

  	if (bDebug)
  		debug("Invalidate Cell;" + bMustRefresh);

  	if (bMustRefresh) {
  		setFlag(FLAG_MUSTREFRESH);
  		if (bufferedTableItem != null) {
  			bufferedTableItem.invalidate();
  		}
  	}
  }
  
  // @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh()
  public boolean refresh() {
    return refresh(true);
  }
  
  // @see com.aelitis.azureus.ui.common.table.TableCellCore#refreshAsync()
  public void refreshAsync() {
  	if (bInRefreshAsync) {
  		//System.out.println(System.currentTimeMillis() + "] SKIP " + restartRefresh);
  		if (restartRefresh < Byte.MAX_VALUE) {
  			restartRefresh++;
  		}
  		return;
  	}
  	bInRefreshAsync = true;

		AERunnable runnable = new AERunnable() {
			public void runSupport() {
				//System.out.println(System.currentTimeMillis() + "] REFRESH!");
				restartRefresh = 0;
				refresh(true);
				bInRefreshAsync = false;
				//System.out.println(System.currentTimeMillis() + "] REFRESH OUT!");
				if (restartRefresh > 0) {
					refreshAsync();
				}
			}
		};
		Utils.execSWTThreadLater(25, runnable);
  }
  
  // @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean)
  public boolean refresh(boolean bDoGraphics) {
  	boolean isRowShown;
  	if (tableRow != null) {
  		TableView view = tableRow.getView();
  		isRowShown = view.isRowVisible(tableRow);
  	} else {
  		isRowShown = true;
  	}
		boolean isCellShown = isRowShown && isShown();
		return refresh(bDoGraphics, isRowShown, isCellShown);
  }

  // @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean, boolean)
  public boolean refresh(boolean bDoGraphics, boolean bRowVisible) {
		boolean isCellShown = bRowVisible && isShown();
		return refresh(bDoGraphics, bRowVisible, isCellShown);
  }

  // @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean, boolean, boolean)
  public boolean refresh(boolean bDoGraphics, boolean bRowVisible,  boolean bCellVisible)
  {
//  	if (Utils.isThisThreadSWT()) {
//  		System.out.println("ONSWT: " + Debug.getCompressedStackTrace());
//  	}
	TableColumnCore	tc = tableColumn;
	
  	if (tc == null) {
  		return false;
  	}
	  boolean ret = getVisuallyChangedSinceRefresh();
  	clearFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);

	  int iErrCount = 0;
	  if (refreshErrLoopCount > 2) {
		  return ret;
	  }

	  iErrCount = tc.getConsecutiveErrCount();
	  if (iErrCount > 10) {
		  refreshErrLoopCount = 3;
		  return ret;
	  }

	  if (bInRefresh) {
		  // Skip a Refresh call when being called from within refresh.
		  // This could happen on virtual tables where SetData calls us again, or
		  // if we ever introduce plugins to refresh.
		  if (bDebug)
			  debug("Calling Refresh from Refresh :) Skipping.");
		  return ret;
	  }
	  try {
		  bInRefresh = true;
		  if (ret) {
  		  long now = SystemTime.getCurrentTime();
  		  if (now - lastRefresh < MAX_REFRESHES_WITHIN_MS) {
  		  	numFastRefreshes++;
  		  	if (numFastRefreshes >= MAX_REFRESHES) {
  		  		if ((numFastRefreshes % MAX_REFRESHES) == 0) {
    			  	pluginError("this plugin is crazy. tried to refresh "
									+ numFastRefreshes + " times in " + (now - lastRefresh)
									+ "ms");
  		  		}
  		  		return ret;
  		  	}
  		  } else {
  		  	numFastRefreshes = 0;
  			  lastRefresh = now;
  		  }
		  }

		  // See bIsUpToDate variable comments
		  if (bCellVisible && !isUpToDate()) {
			  if (bDebug)
				  debug("Setting Invalid because visible & not up to date");
			  clearFlag(FLAG_VALID);
			  setFlag(FLAG_UPTODATE);
		  } else if (!bCellVisible && isUpToDate()) {
			  if (bDebug)
				  debug("Setting not up to date because cell not visible " + Debug.getCompressedStackTrace());
		  	clearFlag(FLAG_UPTODATE);
		  }
		  
		  if (bDebug) {
			  debug("Cell Valid?" + hasFlag(FLAG_VALID) + "; Visible?" + tableRow.isVisible() + "/" + bufferedTableItem.isShown());
		  }
		  int iInterval = tc.getRefreshInterval();
		  if (iInterval == TableColumnCore.INTERVAL_INVALID_ONLY 
		  	&& !hasFlag(FLAG_MUSTREFRESH | FLAG_VALID) && hasFlag(FLAG_SORTVALUEISTEXT) && sortValue != null
			  && tc.getType() == TableColumnCore.TYPE_TEXT_ONLY) {
			  if (bCellVisible) {
				  if (bDebug)
					  debug("fast refresh: setText");
				  ret = setText((String)sortValue);
				  setFlag(FLAG_VALID);
			  }
		  } else if ((iInterval == TableColumnCore.INTERVAL_LIVE ||
			  (iInterval == TableColumnCore.INTERVAL_GRAPHIC && bDoGraphics) ||
			  (iInterval > 0 && (loopFactor % iInterval) == 0) ||
			  !hasFlag(FLAG_VALID) || hasFlag(FLAG_MUSTREFRESH))) 
		  {
			  boolean bWasValid = isValid();

			  if (hasFlag(FLAG_MUSTREFRESH)) {
			  	clearFlag(FLAG_MUSTREFRESH);
			  }

			  if (bDebug)
				  debug("invoke refresh; wasValid? " + bWasValid);

			  long lTimeStart = Constants.isCVSVersion()?SystemTime.getMonotonousTime():0;
			  tc.invokeCellRefreshListeners(this, !bCellVisible);
			  if (refreshListeners != null) {
				  for (int i = 0; i < refreshListeners.size(); i++) {
					  TableCellRefreshListener l = (TableCellRefreshListener)refreshListeners.get(i);
					  if(l instanceof TableCellLightRefreshListener)
						  ((TableCellLightRefreshListener)l).refresh(this,!bCellVisible);
					  else
						  l.refresh(this);
				  }
			  }
			  if ( Constants.isCVSVersion()){
				  long lTimeEnd = SystemTime.getMonotonousTime();
				  tc.addRefreshTime(lTimeEnd - lTimeStart);
			  }
			  
			  // Change to valid only if we weren't valid before the listener calls
			  // This is in case the listeners set valid to false when it was true
			  if (!bWasValid && !hasFlag(FLAG_MUSTREFRESH)) {
			  	setFlag(FLAG_VALID);
			  }
		  }
		  loopFactor++;
		  refreshErrLoopCount = 0;
		  if (iErrCount > 0)
			  tc.setConsecutiveErrCount(0);

		  ret = getVisuallyChangedSinceRefresh();
		  if (bDebug)
			  debug("refresh done; visual change? " + ret + ";" + Debug.getCompressedStackTrace());
	  } catch (Throwable e) {
		  refreshErrLoopCount++;
		  if (tc != null) {
			  tc.setConsecutiveErrCount(++iErrCount);
		  }
		  pluginError(e);
		  if (refreshErrLoopCount > 2)
			  Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
				  "TableCell will not be refreshed anymore this session."));
	  } finally {
		  bInRefresh = false;
	  }

    if (childCells != null) {
    	Object[] childCellsArray = childCells.toArray();
    	for (int i = 0; i < childCellsArray.length; i++) {
				TableCellImpl childCell = (TableCellImpl) childCellsArray[i];
				childCell.refresh(bDoGraphics, bRowVisible, bCellVisible);
			}
    }
	  return ret;
  }
  
  public boolean getVisuallyChangedSinceRefresh() {
  	return hasFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
  }


  public void dispose() {
  	setFlag(FLAG_DISPOSED);

    tableColumn.invokeCellDisposeListeners(this);

    if (disposeListeners != null) {
      try {
        for (Iterator iter = disposeListeners.iterator(); iter.hasNext();) {
          TableCellDisposeListener listener = (TableCellDisposeListener)iter.next();
          listener.dispose(this);
        }
        disposeListeners = null;
      } catch (Throwable e) {
        pluginError(e);
      }
    }
    
    if (bufferedTableItem != null) {
			//bufferedTableItem.setForeground(null);
			bufferedTableItem.dispose();
		}
    
    refreshListeners = null;
    bufferedTableItem = null;
    tableColumn = null;
    tableRow = null;
    sortValue = null;
  }
  
  public boolean setIcon(Image img) {
  	if (isInvisibleAndCanRefresh())
  		return false;

  	icon = img;
  	
    graphic = null;
    setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
    return true;
  }
  
  public Image getIcon() {
  	return icon;
  }

  public boolean needsPainting() {
		if (isDisposed()) {
			return false;
		}

  	if (cellSWTPaintListeners != null || tableColumn.hasCellOtherListeners("SWTPaint")) {
  		return true;
  	}
  	if (bufferedTableItem == null) {
  		return false;
  	}
    return bufferedTableItem.needsPainting();
  }
  
  public void doPaint(GC gc) {
  	//This sometimes causes a infinite loop if the listener invalidates
  	//the drawing area
  	//if ((!hasFlag(FLAG_UPTODATE) || !hasFlag(FLAG_VALID)) && !bInRefresh && !bInRefreshAsync
		//		&& (refreshListeners != null || tableColumn.hasCellRefreshListener())) {
  	//	if (bDebug) {
  	//		debug("doPaint: invoke refresh");
  	//	}
  	//	refresh(true);
  	//}

		if (bDebug) {
			debug("doPaint up2date:" + hasFlag(FLAG_UPTODATE) + ";v:" + hasFlag(FLAG_VALID) + ";rl=" + refreshListeners);
		}
		
		invokeSWTPaintListeners(gc);
		
    if (childCells != null) {
    	Object[] childCellsArray = childCells.toArray();
    	for (int i = 0; i < childCellsArray.length; i++) {
				TableCellImpl childCell = (TableCellImpl) childCellsArray[i];
				childCell.doPaint(gc);
			}
    }
  }

  public void locationChanged() {
  	if (bufferedTableItem != null) {
  		bufferedTableItem.locationChanged();
  	}
  }

  public TableRowCore getTableRowCore() {
    return tableRow;
  }

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getTableRowSWT()
	public TableRowSWT getTableRowSWT() {
		if (tableRow instanceof TableRowSWT) {
			return (TableRowSWT)tableRow;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "TableCell {"
				+ (tableColumn == null ? "disposed" : tableColumn.getName())
				+ ","
				+ (tableRow == null ? "" : "r" + tableRow.getIndex())
				+ (bufferedTableItem == null ? "c?" : "c"
						+ bufferedTableItem.getPosition()) + "," + getText() + ","
				+ getSortValue() + "}";
	}

	/* Comparable Implementation */
  
  /** Compare our sortValue to the specified object.  Assumes the object 
   * is TableCellImp (safe assumption)
   */
  public int compareTo(Object o) {
    try {
      Comparable ourSortValue = getSortValue();
      Comparable otherSortValue = ((TableCellImpl)o).getSortValue();
      if (ourSortValue instanceof String && otherSortValue instanceof String) {
        // Collator.getInstance cache's Collator object, so this is relatively
        // fast.  However, storing it as static somewhere might give a small
        // performance boost.  If such an approach is take, ensure that the static
        // variable is updated the user chooses an different language.
        Collator collator = Collator.getInstance(Locale.getDefault());
        return collator.compare(ourSortValue, otherSortValue);
      }
      try {
        return ourSortValue.compareTo(otherSortValue);
      } catch (ClassCastException e) {
        // It's possible that a row was created, but not refreshed yet.
        // In that case, one sortValue will be String, and the other will be
        // a comparable object that the plugin defined.  Those two sortValues 
        // may not be compatable (for good reason!), so just skip it.
      }
    } catch (Exception e) {
      System.out.println("Could not compare cells");
      Debug.printStackTrace( e );
    }
    return 0;
  }

  public void invokeToolTipListeners(int type) {
  	if (tableColumn == null)
  		return;

    tableColumn.invokeCellToolTipListeners(this, type);

    if (tooltipListeners == null || tooltipErrLoopCount > 2)
      return;

    int iErrCount = tableColumn.getConsecutiveErrCount();
    if (iErrCount > 10)
      return;

    try {
	    if (type == TOOLTIPLISTENER_HOVER) {
	      for (int i = 0; i < tooltipListeners.size(); i++)
	        ((TableCellToolTipListener)(tooltipListeners.get(i))).cellHover(this);
	    } else {
	      for (int i = 0; i < tooltipListeners.size(); i++)
	        ((TableCellToolTipListener)(tooltipListeners.get(i))).cellHoverComplete(this);
	    }
	    tooltipErrLoopCount = 0;
    } catch (Throwable e) {
      tooltipErrLoopCount++;
      tableColumn.setConsecutiveErrCount(++iErrCount);
      pluginError(e);
      if (tooltipErrLoopCount > 2)
      	Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"TableCell's tooltip will not be refreshed anymore this session."));
    }
  }

  public void invokeMouseListeners(TableCellMouseEvent event) {
		ArrayList listeners = event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE
				? cellMouseMoveListeners : cellMouseListeners;
		if (listeners == null)
			return;
		
		if (event.cell != null && event.row == null) {
			event.row = event.cell.getTableRow();
		}
		
		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableCellMouseListener l = (TableCellMouseListener) (listeners.get(i));

				l.cellMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public void invokeVisibilityListeners(int visibility,
			boolean invokeColumnListeners) {
		if (invokeColumnListeners && tableColumn != null) {
			tableColumn.invokeCellVisibilityListeners(this, visibility);
		}

		if (cellVisibilityListeners == null)
			return;

		for (int i = 0; i < cellVisibilityListeners.size(); i++) {
			try {
				TableCellVisibilityListener l = (TableCellVisibilityListener) (cellVisibilityListeners.get(i));

				l.cellVisibilityChanged(this, visibility);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}


  public static final Comparator TEXT_COMPARATOR = new TextComparator();
  private static class TextComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			return arg0.toString().compareToIgnoreCase(arg1.toString());
		}
  }
  
	public void setUpToDate(boolean upToDate) {
	  if (bDebug)
		  debug("set up to date to " + upToDate);
		if (upToDate) {
			setFlag(FLAG_UPTODATE);
		} else {
			clearFlag(FLAG_UPTODATE);
		}
	}
	
	public boolean isUpToDate() {
		return hasFlag(FLAG_UPTODATE);
	}
	
	public void debug(final String s) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				System.out.println(SystemTime.getCurrentTime() + ": r"
						+ tableRow.getIndex() + "c" + tableColumn.getPosition() + "r.v?"
						+ ((tableRow.isVisible() ? "Y" : "N")) + ";" + s);
			}
		}, true);
	}

	public Rectangle getBounds() {
		if (isDisposed()) {
			return new Rectangle(0, 0, 0, 0);
		}
		Rectangle bounds = bufferedTableItem.getBounds();
		if (bounds == null) {
			return new Rectangle(0, 0, 0, 0);
		}
    return bounds;
	}

	private void setOrientationViaColumn() {
		if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
			return;
		
		int align = tableColumn.getAlignment();
		BufferedGraphicTableItem ti = (BufferedGraphicTableItem) bufferedTableItem;
		ti.setOrientation(TableColumnSWTUtils.convertColumnAlignmentToSWT(align));
	}

	public String getObfusticatedText() {
		if (isDisposed()) {
			return null;
		}
		if (tableColumn.isObfusticated()) {
			if (tableColumn instanceof ObfusticateCellText) {
				return ((ObfusticateCellText)tableColumn).getObfusticatedText(this);
			}
			
			return "";
		}
		return null;
	}

	public Graphic getBackgroundGraphic() {
		if (bufferedTableItem == null) {
			return null;
		}
  	return new UISWTGraphicImpl(bufferedTableItem.getBackgroundImage());
	}

	public Image getBackgroundImage() {
		if (bufferedTableItem == null) {
			return null;
		}
  	return bufferedTableItem.getBackgroundImage();
	}
	
	public BufferedTableItem getBufferedTableItem() {
		return bufferedTableItem;
	}

	public int getCursorID() {
		return iCursorID;
	}
	
	public void setCursorID(int cursorID) {
		if (iCursorID == cursorID) {
			return;
		}
		iCursorID = cursorID;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (isMouseOver()) {
					bufferedTableItem.setCursor(iCursorID);
				}
			}
		});
	}
	
	public boolean isMouseOver() {
		if (bufferedTableItem == null) {
			return false;
		}
		if (!tableRow.isVisible()) {
			return false;
		}
		return bufferedTableItem.isMouseOver();
	}
	
	public int[] getMouseOffset() {
		Point ofs = ((TableViewSWT) tableRow.getView()).getTableCellMouseOffset(this);
		return ofs == null ? null : new int[] { ofs.x, ofs.y };
	}
	
	private boolean hasFlag(int flag) {
		return (flags & flag) != 0;
	}
	
	private void setFlag(int flag) {
		flags |= flag;
	}
	
	private void clearFlag(int flag) {
		flags &= ~flag;
	}

	/**
	 * @param childCell
	 *
	 * @since 3.0.5.3
	 */
	public void addChildCell(TableCellImpl childCell) {
		if (childCells == null) {
			childCells = new ArrayList(1);
		}
		//TODO: childCell.setParentCell(this);
		childCells.add(childCell);
	}

  public int getTextAlpha() {
		return textAlpha;
	}

	public void setTextAlpha(int textOpacity) {
		this.textAlpha = textOpacity;
	}

	public Rectangle getBoundsOnDisplay() {
		Rectangle bounds = getBounds();
		Point pt = ((TableViewSWT) tableRow.getView()).getTableOrTreeSWT().toDisplay(bounds.x, bounds.y);
		bounds.x = pt.x;
		bounds.y = pt.y;
		return bounds;
	}
}
