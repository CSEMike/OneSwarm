 /*
 * File    : BufferedGraphicTableItem1.java
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

/** Draws an image at a column in a row of a table using direct paints to the 
 *  table.
 * In comparison to BufferedGraphicTable2,
 * Pros:
 *  - Cleaner
 *  - More proper
 *
 * Cons:
 *  - Bug - overpainting of table causing our cell to redraw everytime any other cell redraws
 *          (New for Windows since SWT3.0M8, always been there for linux)
 *  - Bug - incorrect drawing location on linux (new to SWT3.0M8)
 *  - other bugs
 *
 * @see BufferedGraphicTable2
 * @author TuxPaper
 *
 */
public abstract class BufferedGraphicTableItem1 extends BufferedTableItemImpl
		implements BufferedGraphicTableItem
{
  private int marginHeight = 1;
  private int marginWidth = 1;
  private int orientation = SWT.CENTER;

	//The Buffered image
  private Image image;
  /** Track if we have ever drawn the cell.  Don't draw the cell using our
   * own GC if we've never drawn before.  ie.  If we setGraphic before the
   * cell is visible, don't paint.
   */
  private boolean neverDrawn = true;
  
  
  public BufferedGraphicTableItem1(BufferedTableRow row,int position) {
    super(row, position);
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
    boolean bDoRedraw = (img == null);

    if (bImageSet) {
      // redraw if size changed to wipe area
      if (!bDoRedraw && 
          image != null && !image.isDisposed() && !img.isDisposed() &&
          !image.getBounds().equals(img.getBounds()))
        bDoRedraw = true;
      image = img;
    }

    doPaint(bDoRedraw);

    return bImageSet;
  }

  public boolean needsPainting() {
  	return true;
  }
  

  /**
   * Clear old image from screen (if needed) and paint image
   * 
   * @param bForceClear Force clear of area before drawing.  Normally, a
   *                     non-transparent image will draw overtop of the
   *                     area, instead of first clearing it. 
   */
  private void doPaint(boolean bForceClear) {
		if (image == null || image.isDisposed())
			return;

		if (bForceClear
				|| image.getImageData().getTransparencyType() != SWT.TRANSPARENCY_NONE) {
			// images with transparency need their area cleared first, otherwise we 
			// end up multiplying values (alpha type) or not clearing pixels 
			// (all types)
			Table table = getTable();

			Rectangle bounds = getBoundsForCanvas();
			//In case item isn't displayed bounds is null
			if (bounds == null)
				return;

			// This should trigger a doPaint(gc)
			table.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
		} else {
			doPaint((GC) null);
		}
	}

  /** Paint the bar without updating it's data.  Unless the size changed.
   */
  public void doPaint(GC gc) {
  	if (neverDrawn) {
  		if (gc == null)
  			return;
  		neverDrawn = false;
  	}

    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if (bounds == null || image == null || image.isDisposed()) {
      //System.out.println(row.getIndex() + " nb");
      return;
    }
    
    Table table = getTable();

//    System.out.println("doPnt#" + row.getIndex()+": " + 
//    		((gc == null) ? "GC NULL" : String.valueOf(gc.getClipping())) + 
//        "ta="+table.getClientArea()+";bounds="+bounds);

    Rectangle imageBounds = image.getBounds();
    
    if (imageBounds.width <= 0 || imageBounds.height <= 0 || bounds.width <= 0
				|| bounds.height <= 0) {
      //System.out.println(row.getIndex() + " < 0");
    	return;
    }

    Rectangle tableBounds = table.getClientArea();
    if (bounds.y + bounds.height - tableBounds.y < table.getHeaderHeight()
				|| bounds.y > tableBounds.height) {
//    	System.out.println("doPnt#" + row.getIndex() + ": "
//					+ (bounds.y + bounds.height - tableBounds.y) + "<" + tableBounds.y
//					+ " || " + bounds.y + " > " + tableBounds.height);
      return;
    }
    
    if (orientation == SWT.FILL) {
      if (imageBounds.width != bounds.width
					|| imageBounds.height != bounds.height) {
        //System.out.println("doPaint() sizewrong #"+row.getIndex()+ ".  Image="+imageBounds +";us="+bounds);
/**/
        // Enable this for semi-fast visual update with some flicker
        boolean ourGC = (gc == null);
        if (ourGC)
          gc = new GC(table);
        if (gc != null) {
          int iAdj = VerticalAligner.getTableAdjustVerticalBy(table);
          bounds.y += iAdj;
          iAdj = VerticalAligner.getTableAdjustHorizontallyBy(table);
          bounds.x += iAdj;

          gc.drawImage(image, 0, 0, imageBounds.width, imageBounds.height, 
                       bounds.x, bounds.y, bounds.width, bounds.height);
          if (ourGC)
            gc.dispose();
        }
        // _OR_ enable refresh() for slower visual update with lots of flicker
        //refresh();
        
        // OR, disable both and image will be updated on next graphic bar update
        
        // TODO: make config option to choose
/**/
        invalidate();
        return;
      }
    } else {
  		if (imageBounds.width < bounds.width) {
	    	if (orientation == SWT.CENTER)
	    		bounds.x += (bounds.width - imageBounds.width) / 2;
	    	else if (orientation == SWT.RIGHT)
	    		bounds.x = (bounds.x + bounds.width) - imageBounds.width;
  		}

  		if (imageBounds.height < bounds.height) {
  			bounds.y += (bounds.height - imageBounds.height) / 2;
  		}
    }
    
    Rectangle clipping = new Rectangle(bounds.x, bounds.y, 
                                       bounds.width, 
                                       bounds.height);
    int iMinY = table.getHeaderHeight() + tableBounds.y;
    if (clipping.y < iMinY) {
      clipping.height -= iMinY - clipping.y;
      clipping.y = iMinY;
    }
    int iMaxY = tableBounds.height + tableBounds.y;
    if (clipping.y + clipping.height > iMaxY)
      clipping.height = iMaxY - clipping.y + 1;

    if (clipping.width <= 0 || clipping.height <= 0) {
      //System.out.println(row.getIndex() + " clipping="+clipping + ";" + iMinY + ";" + iMaxY + ";tca=" + tableBounds);
      return;
    }

    // See Eclipse Bug 42416
    // "[Platform Inconsistency] GC(Table) has wrong origin"
    // Notes/Questions:
    // - GTK's "new GC(table)" starts under header, instead of above
    //   -- so, adjust bounds up
    // - Appears to apply to new GC(table) AND GC passed by PaintEvent from a Table PaintListener
    // - Q) .height may be effected (smaller than it should be).  How does this effect clipping?
    // - Q) At what version does this bug start appearing?
    //   A) Reports suggest at least 2.1.1
    int iAdj = VerticalAligner.getTableAdjustVerticalBy(table);
    bounds.y += iAdj;
    clipping.y += iAdj;
    // New: GTK M8+ has a bounds.x bug.. works fine in M7, but assume people have M8 or higher (3.0final)
    iAdj = VerticalAligner.getTableAdjustHorizontallyBy(table);
    bounds.x += iAdj;
    clipping.x += iAdj;

    boolean ourGC = (gc == null);
    if (ourGC) {
      gc = new GC(table);
      if (gc == null) {
        return;
      }
    }

    Point srcStart = new Point(clipping.x - bounds.x, clipping.y - bounds.y); 
    Rectangle dstRect = new Rectangle(clipping.x, clipping.y, 
    		imageBounds.width - srcStart.x, imageBounds.height - srcStart.y);

    Utils.drawImage(gc, image, srcStart, dstRect, clipping, 0, 0, false);
    
    if (ourGC) {
      gc.dispose();
    }
  }

  
  public void dispose() {
    super.dispose();
    image = null;
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
    return bounds;
  }

  public Point getSize() {
    Rectangle bounds = getBounds();
    if(bounds == null)
      return new Point(0, 0);
    return new Point(bounds.width - (marginWidth * 2), 
                     bounds.height - (marginHeight * 2));
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
