 /*
 * File    : BufferedGraphicTableItem.java
 * Created : 24 nov. 2003
 *
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Canvas;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/** Draws an image at a column in a row of a table using a Canvas.
 * In comparison to BufferedGraphicTable, which uses direct paints to table,
 * Pros:
 *  - Skip the table redrawing and overdrawing bugs in 3.0M8 (or greater?)
 *
 * Cons:
 *  - Lag
 *  - A lot more control is needed to do the same thing
 *
 * @see BufferedGraphicTable
 * @author TuxPaper
 *
 */
public abstract class BufferedGraphicTableItem2 extends BufferedTableItemImpl
		implements BufferedGraphicTableItem
{
  private int marginHeight = 1;
  private int marginWidth = 1;
  private int orientation = SWT.CENTER;

  /** Canvas that image is drawn on */
  Canvas cBlockView = null;
  //The Buffered image
  private Image image;
  
  /** Used for !fillCell */
  private Color lastBackColor = null;
  
  
  public BufferedGraphicTableItem2(BufferedTableRow row,int position) {
    super(row, position);
  }
  
  private void createBlockView() {
    // For !fillCell, we draw the background manually, because some OSes
    // send a paint listener after setBackground() (causing another doPaint),
    // and some do not.
    int iStyle = SWT.NO_FOCUS | SWT.NO_BACKGROUND;
    if (orientation == SWT.FILL) {
      iStyle |= SWT.NO_REDRAW_RESIZE;
    }
    cBlockView = new Canvas(getTable(), iStyle);
    cBlockView.setBackground(null);
    cBlockView.addPaintListener(new PaintListener() {
    	public void paintControl(PaintEvent event) {
        if (event.width == 0 || event.height == 0)
          return;
    		doPaint(event.gc.getClipping());
    	}
    });
    //cBlockView.moveAbove(null);

    cBlockView.addMouseListener(new MouseAdapter() {
        public void mouseDown(MouseEvent e) {
          Table table = getTable();
          Rectangle r = cBlockView.getBounds();
          TableItem[] item = { table.getItem(new Point(r.x, r.y)) };
          if (item[0] != null) {
            table.setSelection(item);
          }
          table.setFocus();
        }
        public void mouseUp(MouseEvent e) {
          getTable().setFocus();
        }
      }
    );
  }
    

  /** Retrieve the graphic related to this table item.
   * @return the Image that is draw in the cell, or null if there is none.
   */
  public Image getGraphic() {
    return image;
  }
  
  /* Sets image to be drawn.
   * @param img Image to be stored & drawn
   * @return true - image was changed.  false = image was the same
   */
  public boolean setGraphic(Image img) {
    boolean bImageSet = (image != img);

    if (bImageSet) {
      if (cBlockView == null) {
        createBlockView();
      }
      image = img;
    }
    if (img != null) {
      doPaint((Rectangle)null);
    }

    return bImageSet;
  }

  public boolean needsPainting() {
  	return true;
  }
  
  public void locationChanged() {
    if (cBlockView == null || cBlockView.isDisposed())
      return;

    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if (bounds == null || image == null || image.isDisposed()) {
      return;
    }
    // moveAbove reduces the # redraws
    //cBlockView.moveAbove(null);
    cBlockView.setLocation(bounds.x, bounds.y);
  }

  /** Inherited doPaint(GC) call.  This is called when the Table needs 
   * repainting.  Since we capture the Canvas' paint, most of the Table
   * repainting can be ignored.  Cases where the cell bounds or background
   * color changed, however, require action.
   */
  public void doPaint(GC gc) {
    if (cBlockView == null || cBlockView.isDisposed())
      return;

    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if (bounds == null || image == null || image.isDisposed()) {
      return;
    }
    Rectangle canvasBounds = cBlockView.getBounds();
    if (canvasBounds.x != bounds.x || canvasBounds.y != bounds.y) {
      //cBlockView.moveAbove(null);
      cBlockView.setLocation(bounds.x, bounds.y);
      canvasBounds = cBlockView.getBounds();
      //debugOut("doPaint(GC): move cBlockView to " + bounds.x + "x" + bounds.y, false);
    }

    Table table = getTable();
    Rectangle tableBounds = table.getClientArea();
    if (tableBounds.y < table.getHeaderHeight()) {
      tableBounds.y = table.getHeaderHeight();
    }
    Rectangle rNewCanvas = bounds.intersection(tableBounds);
    //debugOut("doPaint(gc) rNewCanvas="+rNewCanvas+";canvasBounds="+canvasBounds+";tableBounds="+tableBounds, false);
    if (rNewCanvas.width <= 0 || rNewCanvas.height <= 0) {
      return;
    }
    if (!rNewCanvas.equals(canvasBounds) ||
        (orientation != SWT.FILL && !getRowBackground(table).equals(lastBackColor))) {
      rNewCanvas.x -= canvasBounds.x;
      rNewCanvas.y -= canvasBounds.y;
      doPaint(rNewCanvas);
    }
  }

  /** Paint the bar without updating it's data.  Unless the size changed.
   */
  public void doPaint(Rectangle clipping) {
    //debugOut("doPaint() clipping="+clipping, false);
    if (cBlockView == null || cBlockView.isDisposed())
      return;

    Table table = getTable();
    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if (bounds == null || image == null || image.isDisposed()) {
      return;
    }
    
    Rectangle canvasBounds = cBlockView.getBounds();
    //debugOut("Block:"+canvasBounds+";cell:"+bounds,false);
    if (canvasBounds.x != bounds.x || canvasBounds.y != bounds.y) {
      //cBlockView.moveAbove(null);
      cBlockView.setLocation(bounds.x, bounds.y);
      canvasBounds = cBlockView.getBounds();
      //debugOut("doPaint(clipping): move cBlockView to " + bounds.x + "x" + bounds.y, false);
    }
    if (bounds.width != canvasBounds.width ||
        bounds.height != canvasBounds.height) {
      cBlockView.setSize(bounds.width, bounds.height);
      canvasBounds = cBlockView.getBounds();
    }
    //debugOut("doPaint()" + ((gc == null) ? "GC NULL" : String.valueOf(gc.getClipping())) + 
    //         "ta="+table.getClientArea()+";bounds="+bounds, false);
    
    if (orientation == SWT.FILL) {
      Rectangle imageBounds = image.getBounds();
      if (imageBounds.width != bounds.width ||
          imageBounds.height != bounds.height) {
        // Enable this for semi-fast visual update with some flicker
        cBlockView.setSize(bounds.width, bounds.height);
        GC gc = new GC(cBlockView);
        if (gc == null) {
          return;
        }
        gc.drawImage(image, 0, 0, imageBounds.width, imageBounds.height, 
                     0, 0, bounds.width, bounds.height);
        gc.dispose();
/*
        // _OR_ enable refresh() for slower visual update with lots of flicker
        //refresh();
        
        // OR, disable both and image will be updated on next graphic bar update
        
        // TODO: make config option to choose
*/
        invalidate();
        return;
      }
    }
    
    if (clipping == null) {
      clipping = new Rectangle(0, 0, bounds.width, bounds.height);
    }
    Rectangle tableBounds = table.getClientArea();
    if (tableBounds.y < table.getHeaderHeight()) {
      tableBounds.y = table.getHeaderHeight();
    }
    //debugOut("doPaint() tableBounds="+tableBounds+";canvasBounds="+canvasBounds+";clipping="+clipping, false);
    tableBounds.x -= canvasBounds.x;
    tableBounds.y -= canvasBounds.y;
    clipping = clipping.intersection(tableBounds);
    //debugOut("doPaint() clipping="+clipping, false);

    if (clipping.x + clipping.width <= 0 && clipping.y + clipping.height <= 0) {
      return;
    }
    

    GC gc = new GC(cBlockView);
    if (gc == null) {
      return;
    }
    if (orientation == SWT.FILL) {
      gc.setClipping(clipping);
      gc.drawImage(image, 0, 0);
    } else {
/*
      // Grab a pixel beside the cell and draw that as our background
      // Advantage: paints correct color when hilighted and not in focus
      // Disadvatage: doesn't always work!
      GC gc2 = new GC(table);
      Image i = new Image(table.getDisplay(), 1, 1);
      gc2.copyArea(i, bounds.x + bounds.width + 1, bounds.y + (bounds.width / 2));
      gc2.dispose();
      
      gc.drawImage(i, 0, 0, 1, 1, 
                   0,0, rBlockViewBounds.width, rBlockViewBounds.height);
*/      

      lastBackColor = getRowBackground(table);
      gc.setBackground(lastBackColor);
      gc.fillRectangle(clipping);
/*      
      Rectangle imageBounds = image.getBounds();
      Rectangle r = canvasBounds.intersection(tableBounds);
      int x = (r.width - imageBounds.width) / 2;
      if (x <= 0)
        x = 0;
      clipping.x += x;
*/
      int x = 0;
      gc.setClipping(clipping);
      gc.drawImage(image, x, 0);
    }
    gc.dispose();
  }

  /** Don't forget to call super.dispose()! */
  public void dispose() {
    super.dispose();
    image = null;
    if (cBlockView != null) {
      if (!cBlockView.isDisposed())
        cBlockView.dispose();
      cBlockView = null;
    }
  }
  
  /** Calculate the bounds of the receiver should be drawing in
    * @return what size/position the canvas should be
    */
  public Rectangle getBoundsForCanvas() {
    Rectangle bounds = getBounds();
    if(bounds == null)
      return null;
    bounds.y += marginHeight;
    bounds.height -= (marginHeight * 2);
    bounds.x += marginWidth;
    bounds.width -= (marginWidth * 2);
    
    if (bounds.width <= 0 || bounds.height <= 0)
      return null;

    return bounds;
  }
  
  public Point getSize() {
    Rectangle bounds = getBounds();
    if(bounds == null)
      return new Point(0, 0);
    return new Point(bounds.width - (marginWidth * 2), 
                     bounds.height - (marginHeight * 2));
  }

  private Color getRowBackground(Table table) {
    if (row.isSelected() && false) {
      if (table.isFocusControl())
        return table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
      else
        return table.getDisplay().getSystemColor(SWT.COLOR_GRAY);
    } else {
      return getBackground();
    }
  }

  public void invalidate() {
  }

	public int getMarginHeight() {
		return marginHeight;
	}

	public int getMarginWidth() {
		return marginWidth;
	}

  public void setMargin(int width, int height) {
  	if (width >= 0) {
  		marginWidth = width;
  	}
  	
  	if (height >= 0) {
  		marginHeight = height;
  	}
  }

	public int getOrientation() {
		return orientation;
	}
  
  public void setOrientation(int orientation) {
  	this.orientation = orientation;
  }
}
