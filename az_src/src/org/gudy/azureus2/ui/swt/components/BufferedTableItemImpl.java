/*
 * File    : BufferedTableItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Table;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

import com.aelitis.azureus.ui.swt.utils.ColorCache2;
import com.aelitis.azureus.ui.swt.utils.ColorCache2.*;

/**
 * @author Olivier
 *
 */
public abstract class BufferedTableItemImpl implements BufferedTableItem
{
	protected BufferedTableRow row;

	private int position;

	private CachedColor ourFGColor_cache = null;
	
	private String text = "";
	
	private Image icon = null;

	private AERunnable runnableDirtyCell;

	private boolean isDirty;

	public BufferedTableItemImpl(BufferedTableRow row, int position) {
		this.row = row;
		this.position = position;
	}

	public String getText() {
		return text;
	}

	public boolean setText(String text) {
		if (this.text.equals(text)) {
			return false;
		}
		this.text = (text == null) ? "" : text;
		
		redraw();
		
		return true;
	}

  // @see org.gudy.azureus2.ui.swt.components.BufferedTableItem#redraw()
  public void redraw() {
		//System.out.println("redraw via " + Debug.getCompressedStackTrace(5));

  	synchronized (this) {
			if (isDirty) {
				return;
			}
		}
  	
  	if (!row.isVisibleNoSWT()) {
  		return;
  	}

		// Might be a good optimization.. haven't tried it
  	//if (isInPaintItem()) {
		//		&& row.getTable().getData("fullPaint") == Boolean.TRUE) {
  	//	return;
  	//}
  	
		if (runnableDirtyCell == null) {
			synchronized (this) {
				if (runnableDirtyCell == null) {
					runnableDirtyCell = new AERunnable() {
						public void runSupport() {
							synchronized (this) {
								isDirty = false;
							}
							if (isInPaintItem()
									&& row.getTable().getData("fullPaint") == Boolean.TRUE) {
								return;
							}
							// row.isVisible is time consuming.  getBounds intersecting 
							// clientArea will probably be empty when not visible
							//if (!row.isVisible()) {
							//	return;
							//}
							Rectangle bounds = getBoundsRaw();
							if (bounds != null) {
								TableOrTreeSWT table = row.getTable();
								Rectangle dirty = table.getClientArea().intersection(bounds);
								//System.out.println("old = " + this.text + ";new=" + text + ";dirty=" + bounds);

								if (!dirty.isEmpty()) {
									quickRedrawCell(table, dirty, bounds);
								}
							}
						}
					};
				}
			}
		}
		
		synchronized (this) {
			isDirty = true;
		}
		Utils.execSWTThread(runnableDirtyCell);
	}

	protected void quickRedrawCell(TableOrTreeSWT table, Rectangle dirty,
			Rectangle cellBounds) {
		table.redraw(dirty.x, dirty.y, dirty.width, dirty.height, false);
	}

	public void setIcon(Image img) {
		if (position != -1) {
			row.setImage(position, img);
			icon = img;
		}
	}

	public Image getIcon() {
		if (position != -1) {
			Image image = row.getImage(position);
			return (image != null) ? image : icon;
		}

		return null;
	}

	public void setRowForeground(Color color) {
		row.setForeground(color);
	}

	public boolean setForeground(Color color) {
		if (position == -1)
			return false;

		boolean ok = row.setForeground(position, color);
		if (ok && ourFGColor_cache != null) {
			if (!ourFGColor_cache.isDisposed()) {ourFGColor_cache.dispose();}
			ourFGColor_cache = null;
		}
		return ok;
	}
	
	public Color getForeground() {
		if (position == -1)
			return null;

		return row.getForeground(position);
	}

	public boolean setForeground(final int red, final int green, final int blue) {
		if (position == -1) {
			return false;
		}

		Color oldColor = row.getForeground(position);
		if (oldColor != null) {

  		RGB newRGB = new RGB(red, green, blue);
  
  		if (oldColor.getRGB().equals(newRGB)) {
  			return false;
  		}
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_setForeground(red, green, blue);
			}
		});
		return true;
	}

	private boolean swt_setForeground(final int red, final int green, final int blue) {
		if (red == -1 && green == -1 && blue == -1) {
			return setForeground(null);
		}

		Color oldColor = row.getForeground(position);

		RGB newRGB = new RGB(red, green, blue);

		if (oldColor != null && oldColor.getRGB().equals(newRGB)) {
			return false;
		}

		CachedColor newColor = ColorCache2.getColor( row.getTable().getDisplay(), newRGB );
		boolean ok = row.setForeground(position, newColor.getColor());
		if (ok) {
			if (ourFGColor_cache != null && !ourFGColor_cache.isDisposed())
				ourFGColor_cache.dispose();
			ourFGColor_cache = newColor;
		} else {
			if (!newColor.isDisposed())
				newColor.dispose();
		}

		return ok;
	}

	public Color getBackground() {
		return row.getBackground();
	}

	public Rectangle getBounds() {
		if (position == -1) {
			return null;
		}
		if (isInPaintItem()) {
			InPaintInfo data = (InPaintInfo) row.getTable().getData("inPaintInfo");
			return new Rectangle(data.curCellBounds.x, data.curCellBounds.y,
					data.curCellBounds.width, data.curCellBounds.height);
		}
		return row.getBounds(position);
	}
	
  public Rectangle getBoundsRaw() {
    return getBounds();
  }


	public TableOrTreeSWT getTable() {
		return row.getTable();
	}

	public void dispose() {
		if (ourFGColor_cache != null && !ourFGColor_cache.isDisposed())
			ourFGColor_cache.dispose();
	}

	public boolean isShown() {
		return true;
// XXX Bounds check is almost always slower than any changes we
//     are going to do to the column
//		if (position < 0) {
//			return false;
//		}
//		
//		Rectangle bounds = row.getBounds(position);
//		if (bounds == null) {
//			return false;
//		}
//
//		return row.getTable().getClientArea().intersects(bounds);
	}

	public boolean needsPainting() {
		return false;
	}

	public void locationChanged() {
	}

	public int getPosition() {
		return position;
	}

	public Image getBackgroundImage() {
		TableOrTreeSWT table = row.getTable();
		
		Rectangle bounds = getBounds();
		
		if (bounds.isEmpty()) {
			return null;
		}
		
		Image image = new Image(table.getDisplay(), bounds.width, bounds.height);
		
		GC gc = new GC(image);
		gc.setForeground(getBackground());
		gc.setBackground(getBackground());
		gc.fillRectangle(0, 0, bounds.width, bounds.height);
		//gc.copyArea(image, bounds.x, bounds.y);
		gc.dispose();
		
		return image;
	}

  // @see org.gudy.azureus2.ui.swt.components.BufferedTableItem#getMaxLines()
  public int getMaxLines() {
  	return 1;
  }
  
  // @see org.gudy.azureus2.ui.swt.components.BufferedTableItem#setCursor(int)
  public void setCursor(final int cursorID) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (row == null) {
					return;
				}
				TableOrTreeSWT table = row.getTable();
				if (table == null || table.isDisposed()) {
					return;
				}
				table.setCursor(table.getDisplay().getSystemCursor(cursorID));
			}
		});
  }
  
  // @see org.gudy.azureus2.ui.swt.components.BufferedTableItem#isMouseOver()
  public boolean isMouseOver() {
		TableOrTreeSWT table = row.getTable();
		if (table == null || table.isDisposed()) {
			return false;
		}
		Point pt = table.getDisplay().getCursorLocation();
		pt = table.toControl(pt);

		Rectangle bounds = getBounds();
		return bounds == null ? false : bounds.contains(pt);
  }
  
	public boolean isInPaintItem() {
		if (row.inPaintItem()) {
			InPaintInfo data = (InPaintInfo) row.getTable().getData("inPaintInfo");
			return data.curCellIndex == position;
		}
		return false;
	}
	
	// @see org.gudy.azureus2.ui.swt.components.BufferedTableItem#getHeight()
	public int getHeight() {
		return row.getHeight();
	}
}
