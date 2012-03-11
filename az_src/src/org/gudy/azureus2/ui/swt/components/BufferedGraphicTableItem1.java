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
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

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
   * @return true - image was changed.<br>  
   *         false = image was the same (doesn't mean image bits were the same)
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
    //doPaint(bDoRedraw);

    return bImageSet;
  }

  public boolean needsPainting() {
  	return true;
  }

  public void dispose() {
    super.dispose();
    image = null;
  }
  
  /** Calculate the bounds of the receiver should be drawing in
    * @return what size/position the canvas should be
    */
  public Rectangle getBoundsForCanvas() {
    Rectangle bounds = super.getBounds();
    if(bounds == null)
      return null;
    bounds.y += marginHeight;
    bounds.height -= (marginHeight * 2);
    bounds.x += marginWidth;
    bounds.width -= (marginWidth * 2);
    return bounds;
  }
  
  // @see {getBounds}
  public Rectangle getBounds() {
  	return getBoundsForCanvas();
  }
  
  public Rectangle getBoundsRaw() {
    return super.getBounds();
  }

  public Point getSize() {
    Rectangle bounds = super.getBounds();
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
  
  public Image getBackgroundImage() {
  	Image imageRowBG = row.getBackgroundImage();
  	if (imageRowBG != null) {
  		Rectangle bounds = super.getBounds();
  		
  		int wInside = bounds.width - (marginWidth * 2);
  		int hInside = bounds.height - (marginHeight * 2);
  		Image imageCellBG = new Image(Display.getDefault(), wInside, hInside);
  		GC gc = new GC(imageCellBG);
  		gc.drawImage(imageRowBG, bounds.x + marginWidth, 0 + marginHeight,
					wInside, hInside, 0, 0, wInside, hInside);
  		gc.dispose();
  		
  		return imageCellBG;
  	}
  	
		Rectangle bounds = super.getBounds();
		
		if (bounds.isEmpty()) {
			return null;
		}
		
		Image image = new Image(Display.getDefault(), bounds.width
				- (marginWidth * 2), bounds.height - (marginHeight * 2));
		
		GC gc = new GC(image);
		gc.setForeground(getBackground());
		gc.setBackground(getBackground());
		gc.fillRectangle(0, 0, bounds.width, bounds.height);
		gc.dispose();
		
		return image;
	}
}
