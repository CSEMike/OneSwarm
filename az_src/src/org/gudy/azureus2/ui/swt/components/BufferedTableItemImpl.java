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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Olivier
 *
 */
public abstract class BufferedTableItemImpl implements BufferedTableItem
{
	protected BufferedTableRow row;

	private int position;

	private Color ourFGColor = null;
	
	private String text = "";
	
	private Image icon = null;

	public BufferedTableItemImpl(BufferedTableRow row, int position) {
		this.row = row;
		this.position = position;
	}

	public String getText() {
		if (Utils.SWT32_TABLEPAINT) {
			return text;
		}

		if (position != -1)
			return row.getText(position);
		return "";
	}

	public boolean setText(String text) {
		if (Utils.SWT32_TABLEPAINT) {
			if (this.text.equals(text)) {
				return false;
			}
	
			this.text = (text == null) ? "" : text;
			
			Rectangle bounds = getBounds();
			if (bounds != null) {
				Table table = row.getTable();
				Rectangle dirty = table.getClientArea().intersection(bounds);
				table.redraw(dirty.x, dirty.y, dirty.width, dirty.height, false);
			}
			
			return true;
		}

		if (position != -1)
			return row.setText(position, text);
		return false;
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
		if (ok && ourFGColor != null) {
			if (!ourFGColor.isDisposed()) {ourFGColor.dispose();}
			ourFGColor = null;
		}
		return ok;
	}
	
	public Color getForeground() {
		if (position == -1)
			return null;

		return row.getForeground(position);
	}

	public boolean setForeground(int red, int green, int blue) {
		if (position == -1)
			return false;
		
		if (red == -1 && green == -1 && blue == -1) {
			return setForeground(null);
		}

		Color oldColor = row.getForeground(position);

		RGB newRGB = new RGB(red, green, blue);

		if (oldColor != null && oldColor.getRGB().equals(newRGB)) {
			return false;
		}

		Color newColor = new Color(row.getTable().getDisplay(), newRGB);
		boolean ok = row.setForeground(position, newColor);
		if (ok) {
			if (ourFGColor != null && !ourFGColor.isDisposed())
				ourFGColor.dispose();
			ourFGColor = newColor;
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
		if (position != -1)
			return row.getBounds(position);
		return null;
	}

	public Table getTable() {
		return row.getTable();
	}

	public void dispose() {
		if (ourFGColor != null && !ourFGColor.isDisposed())
			ourFGColor.dispose();
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

	public void doPaint(GC gc) {
	}

	public void locationChanged() {
	}

	public int getPosition() {
		return position;
	}

	public Image getBackgroundImage() {
		Table table = row.getTable();
		
		Rectangle bounds = getBounds();
		
		if (bounds.isEmpty()) {
			return null;
		}
		
		Image image = new Image(table.getDisplay(), bounds.width, bounds.height);
		
		GC gc = new GC(table);
		gc.copyArea(image, bounds.x, bounds.y);
		gc.dispose();
		
		return image;
	}

  // @see org.gudy.azureus2.ui.swt.components.BufferedTableItem#redraw()
  public void redraw() {
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
				Table table = row.getTable();
				if (table == null || table.isDisposed()) {
					return;
				}
				table.setCursor(table.getDisplay().getSystemCursor(cursorID));
			}
		});
  }
}
