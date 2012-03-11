/*
 * File    : BufferedTableItem.java
 * Created : 24-Nov-2003
 * By      : parg
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
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

import com.aelitis.azureus.ui.swt.utils.ColorCache2;
import com.aelitis.azureus.ui.swt.utils.ColorCache2.*;

/**
 * A buffered Table Row.
 *<p> 
 * We buffer certain properties of TableRow to save CPU cycles.  For example,
 * foreground_colors is cached because TableItem.getForegroundColor is expensive
 * when there is no color set, and setForegroundColor is always expensive.
 *<p> 
 * Text is not buffered as SWT does a good job of buffering it already.
 *<p>
 * 
 * @note For Virtual tables, we can not set any visual properties until
 *        SWT.SetData has been called.  getData("SD") is set after SetData
 *        call, and the row is invalidated.  Thus, there is no need to set
 *        any visual properties until the row #isVisible()
 * 
 * @author parg<br>
 * @author TuxPaper (SWT.Virtual Stuff)
 */
public class 
BufferedTableRow
{
	private static final int VALUE_SIZE_INC	= 8;

	// for checkWidget(int)
	public final static int REQUIRE_TABLEITEM = 0;
	public final static int REQUIRE_TABLEITEM_INITIALIZED = 1;
	public final static int REQUIRE_VISIBILITY = 2;
	
	protected TableOrTreeSWT table;
	protected TableItemOrTreeItem	item;
	
	protected Image[]	image_values	= new Image[0];
	protected Color[]	foreground_colors	= new Color[0];
	
	protected CachedColor		foreground_cache;
	protected CachedColor     	ourForeground_cache;
	
	private Point ptIconSize = null;

	private Image imageBG;

	private int numSubItems;

	private boolean expanded;

	private boolean isVirtual;
	
	
	/**
	 * Default constructor
	 * 
	 * @param _table
	 */
	public BufferedTableRow(TableOrTreeSWT _table)
	{
		table = _table;
		item = null;
	}
	
	/**
	 * Disposes of underlying SWT TableItem. If no TableItem has been
	 * assigned to the row yet, an unused TableItem will be disposed of, if
	 * available.
	 * <p>
	 * Disposing of fonts, colors and other resources are the responsibilty of 
	 * the caller.
	 */
	public void
	dispose()
	{
		if (table != null && !table.isDisposed() && Utils.isThisThreadSWT()) {
			if (!checkWidget(REQUIRE_TABLEITEM)) {
				// No assigned spot yet, or not our spot:
				// find a row with no TableRow data

				TableItemOrTreeItem[] items = table.getItems();
				for (int i = items.length - 1; i >= 0; i--) {
					TableItemOrTreeItem item = items[i];
					if (!item.isDisposed()) {
						Object itemRow = item.getData("TableRow");
						if (itemRow == null || itemRow == this) {
							this.item = item;
							break;
						}
					}
				}
			}

			boolean itemNeedsDisposal = item != null && !item.isDisposed(); 
			
			if (ourForeground_cache != null && !ourForeground_cache.isDisposed()) {
				// Even though we are going to dispose soon, set foreground to null
				// in case for some reason the OS gets a paint event in between
				// the color disposal and the item disposal
				if (itemNeedsDisposal) {
					item.setForeground(null);
				}
				this.ourForeground_cache.dispose();
			}

			if (itemNeedsDisposal) {
				item.dispose();
			} else if (table.getItemCount() > 0) {
				System.err.println("No table row was found to dispose");
			}
		} else {
			if (!Utils.isThisThreadSWT()) {
				System.err.println("Calling BufferedTableRow.dispose on non-SWT thread!");
				System.err.println(Debug.getStackTrace(false, false));
			}
		}
		item = null;
	}
	
	/**
	 * Sets the receiver's image at a column.
	 *
	 * @param index the column index
	 * @param new_image the new image
	 */
	public void
	setImage(
   		int 	index,
		Image	new_image )
	{
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return;

		if ( index >= image_values.length ){
			
			int	new_size = Math.max( index+1, image_values.length+VALUE_SIZE_INC );
			
			Image[]	new_images = new Image[new_size];
			
			System.arraycopy( image_values, 0, new_images, 0, image_values.length );
			
			image_values = new_images;
		}
		
		Image	image = image_values[index];
		
		if ( new_image == image ){
			
			return;
		}
		
		image_values[index] = new_image;
		
		item.setImage( index, new_image );	
	}
	
	public Image getImage(int index) {
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return null;
		
		return item.getImage(index);
	}

	/**
	 * Checks if the widget is valid
	 * 
	 * @param checkFlags REQUIRE_* flags (OR'd)
	 * 
	 * @return True: Ok; False: Not ok
	 */
	public boolean checkWidget(int checkFlags) {
		boolean bWidgetOk = item != null && !item.isDisposed()
				&& item.getData("TableRow") == this;

		final boolean bCheckVisibility = (checkFlags & REQUIRE_VISIBILITY) > 0;
		final boolean bCheckInitialized = (checkFlags & REQUIRE_TABLEITEM_INITIALIZED) > 0;

		if (bWidgetOk && bCheckInitialized) {
			bWidgetOk = !isVirtual
					|| item.getData("SD") != null;
		}

		if (bWidgetOk && bCheckVisibility) {
			if (_isVisible()) {
				// Caller assumes that a visible item can be modified, so 
				// make sure we initialize it.
				if (!bCheckInitialized && isVirtual
						&& item.getData("SD") == null) {
					// This is catch is temporary for SWT 3212, because there are cases where
					// it says it isn't disposed, when it really almost is
					try {
						item.setData("SD", "1");
					} catch (NullPointerException badSWT) {
					}

		    	setIconSize(ptIconSize);
					invalidate();
				}
			} else {
				bWidgetOk = false;
			}
		}

		return bWidgetOk;
	}
	
	private boolean _isVisible() {
		// Least time consuming checks first

		if (inPaintItem()) {
			return true;
		}

		if (!table.isVisible()) {
			return false;
		}
		
		//if (table.getData("inPaintItem") != null) {
		//	System.out.println(table.indexOf((Widget) table.getData("inPaintItem")) + ";" + table.indexOf(item));
		//	System.out.println(Debug.getCompressedStackTrace());
		//}
		
		Rectangle bounds = getBounds(0);
		if (bounds == null) {
			return false;
		}
		return table.getClientArea().contains(bounds.x, bounds.y);

		/*
		int index = table.indexOf(item);
		if (index == -1)
			return false;
		
		int iTopIndex = table.getTopIndex();
		if (index < iTopIndex)
			return false;

		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
		if (index > iBottomIndex)
			return false;

  	//System.out.println("i-" + index + ";top=" + iTopIndex + ";b=" + iBottomIndex);
		
		return true;
		*/
	}
	
	
	public Color
	getForeground()
	{
		if (foreground_cache != null) {
			return foreground_cache.getColor();
		}
		
		if (!Utils.isSWTThread()) {
			return null;
		}

		if (foreground_cache == null && isSelected()) {
			return table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
		}

		if (!checkWidget(REQUIRE_TABLEITEM)) {
			return null;
		}

		return( item.getForeground());
	}
	
	public void
	setForeground(
		Color	c )
	{
		if (foreground_cache == null && c == null) {return;}
		
		if (foreground_cache != null ){
			
			Color existing = foreground_cache.getColor();
			
			if ( existing != null && existing.equals(c)){
		
				return;
			}
		}
		
		foreground_cache = ColorCache2.getColor( c );
		if (this.ourForeground_cache != null) {
			if (!this.ourForeground_cache.isDisposed()) {
				this.ourForeground_cache.dispose();
			}
			this.ourForeground_cache = null;
		}
		
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return;

		item.setForeground(c);
	}

	public void setForeground(final int red, final int green, final int blue) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_setForeground(red, green, blue);
			}
		});
	}

	
	public void swt_setForeground(int red, int green, int blue) {
		if (red == -1 && green == -1 && blue == -1) {
			this.setForeground(null);
			return;
		}
		
		RGB newRGB = new RGB(red, green, blue);
		if (this.foreground_cache != null ){
			
			Color c = this.foreground_cache.getColor();
			
			if ( c != null && c.getRGB().equals(newRGB)){
		
				return;
			}
		}
		
		// Hopefully it is OK to just assume it is safe to dispose of the colour,
		// since we're expecting it to match this.foreground.
		CachedColor newColor = ColorCache2.getColor(getTable().getDisplay(), newRGB);
		if (checkWidget(REQUIRE_TABLEITEM_INITIALIZED)) {
			item.setForeground(newColor.getColor());
		}
		if (ourForeground_cache != null && !ourForeground_cache.isDisposed()) {
			ourForeground_cache.dispose();
		}
		this.foreground_cache = newColor;
		this.ourForeground_cache = newColor;
	}
	
	public boolean
	setForeground(
	  final int index,
		final Color	new_color )
	{
				
		synchronized (this) {
			
  		if ( index >= foreground_colors.length ){
  			
  			int	new_size = Math.max( index+1, foreground_colors.length+VALUE_SIZE_INC );
  			
  			Color[]	new_colors = new Color[new_size];
  			
  			System.arraycopy( foreground_colors, 0, new_colors, 0, foreground_colors.length );
  			
  			foreground_colors = new_colors;
  		}
  
  		Color value = foreground_colors[index];
  		
  		if ( new_color == value ){
  			
  			return false;
  		}
  		
  		if (	new_color != null && 
  				value != null &&
  				new_color.equals( value )){
  					
  			return false;
  		}
  		
  		foreground_colors[index] = new_color;
		}

		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED)) {
			return true;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if ( !item.isDisposed()){
					item.setForeground(index, new_color);
				}
			}
		});
    
    return true;
	}

	public Color getForeground(int index)
	{
		synchronized (this) {
  		if (index >= foreground_colors.length) {
  		  return getForeground();
  		}
  
  		if (foreground_colors[index] == null) {
  			if (isSelected()) {
  				if (!Utils.isSWTThread()) {
  					return null;
  				}
  
    			Color systemColor = table.getDisplay().getSystemColor(
    					table.isFocusControl() ? SWT.COLOR_LIST_SELECTION_TEXT
    							: SWT.COLOR_WIDGET_FOREGROUND);
    			return systemColor;
  			}
  			return getForeground();
  		}
  
  		return foreground_colors[index];
		}
	}
	
	protected String
	getText(
		int		index )
	{
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return "";

		// SWT >= 3041(Win),3014(GTK),3002(Carbon) and returns "" if range check
		// fails
		return item.getText(index);
	}

  /**
   * @param index
   * @param new_value
   * @return true if the item has been updated
   */
	public boolean
	setText(
		int			index,
		String		new_value )
	{
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return false;
		
		if (index < 0 || index >= table.getColumnCount())
			return false;
		
		if (new_value == null)
			new_value = "";

		if (item.getText(index).equals(new_value))
			return false;

		item.setText( index, new_value );
    
    return true;
	}
	
  public Rectangle getBounds(int index) {
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return null;

		Rectangle r = item.getBounds(index);
		if (r == null || r.width == 0 || r.height == 0)
			return null;

		return r; 
	}

  protected TableOrTreeSWT getTable() {
  	return table;
  }
  
  public Color getBackground() {
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
      return null;
		
		if (isSelected()) {
			// XXX This isn't the right color on Cocoa when in focus
			return table.getDisplay().getSystemColor(
					table.isFocusControl() ? SWT.COLOR_LIST_SELECTION
							: SWT.COLOR_WIDGET_BACKGROUND);
		}
    return item.getBackground();
  }

  /**
   * The Index is this item's the position in list.
   *
   * @return Item's Position
   */
  public int getIndex() {
		if (!checkWidget(REQUIRE_TABLEITEM))
  		return -1;

    return table.indexOf(item);
  }
  
  public boolean isSelected() {
		if (!checkWidget(REQUIRE_TABLEITEM))
  		return false;

    return table.isSelected(item);
  }

  public void setSelected(final boolean bSelected) {
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_setSelected(bSelected);
			}
		});
  }
  
  private void swt_setSelected(boolean bSelected) {
		if (!checkWidget(REQUIRE_TABLEITEM))
  		return;

    if (bSelected)
      table.select(item);
    else
      table.deselect(item);
  }

  public boolean setTableItem(int newIndex, boolean isVisible) {
  	TableItemOrTreeItem newRow;

  	try {
//  		if (item != null && !item.isDisposed() && table.indexOf(item) == newIndex) {
//  			return false;
//  		}
  		
  		//System.out.println((item == null ? null : "" + table.indexOf(item)) + ":" + newIndex + ":" + isVisible + ":" + table.getData("maxItemShown"));
  		newRow = table.getItem(newIndex);
  	} catch (IllegalArgumentException er) {
  		if (item == null || item.isDisposed()) {
  			return false;
  		}
  		item = null;
  		return true;
  	} catch (Throwable e) {
  		System.out.println("setTableItem(" + newIndex + ", " + isVisible + ")");
  		e.printStackTrace();
  		return false;
  	}
  	
  	return setTableItem(newRow, isVisible);
  }

  public boolean setTableItem(TableItemOrTreeItem newRow, boolean isVisible) {
  	if (item == null) {
  		isVirtual = (table.getStyle() & SWT.VIRTUAL) > 0;
  		if (ptIconSize == null) {
  			ptIconSize = new Point(1, table.getItemHeight());
  		}
  	}
  	if (newRow.isDisposed()) {
  		Debug.out("newRow disposed from " + Debug.getCompressedStackTrace());
  		return false;
  	}

  	if (newRow.equals(item)) {
  		if (newRow.getData("TableRow") == this) {
  			return false;
  		}
  	}

  	//if (!newRow.getParent().equalsTableOrTree(table))
  	//	return false;

// The following is commented out after moving visible logic to TableRowImpl
//  	if (!isVisible) {
//  		// Q&D, clear out.. we'll fill it correctly when it's visible
//  		if (newRow.getData("TableRow") != null) {
//    		newRow.setData("TableRow", null);
//    		table.deselect(newRow);
//    		return true;
//  		}
//  		//System.out.println("quickclear " + table.indexOf(newRow));
//  		return false;
//  	}
		//System.out.println("slowset " + table.indexOf(newRow));

  	boolean lastItemExisted = item != null && !item.isDisposed();
  	// can't base newRowHadItem on "TableRow" as we clear it in "unlinking" stage
  	//boolean newRowHadItem = newRow.getData("TableRow") != null;

  	if (newRow.getData("SD") != null) {
  	} else {
  		newRow.setData("SD", "1");
  		setIconSize(ptIconSize);
  	}

		newRow.setForeground(foreground_cache==null?null:foreground_cache.getColor());

		int numColumns = table.getColumnCount();
		for (int i = 0; i < numColumns; i++) {
			try {
				//newRow.setImage(i, null);
				newRow.setForeground(i, i < foreground_colors.length
						? foreground_colors[i] : null);
			} catch (NoSuchMethodError e) {
				/* Ignore for Pre 3.0 SWT.. */
			}
		}

  	try {
  		newRow.setData("TableRow", this);
  	} catch (Exception e) {
  		e.printStackTrace();
  		System.out.println("Disposed? " + newRow.isDisposed());
  		if (!newRow.isDisposed()) {
  			System.out.println("TR? " + newRow.getData("TableRow"));
  			System.out.println("SD? " + newRow.getData("SD"));
  		}
  	}

	  image_values	= new Image[0];
	  //foreground_colors	= new Color[0];
    //foreground = null;

    // unlink old item from tablerow
    if (lastItemExisted && item.getData("TableRow") == this && !newRow.equals(item)) {
    	item.setData("TableRow", null);
    	table.deselect(item);
    	/*
  		int numColumns = table.getColumnCount();
  		for (int i = 0; i < numColumns; i++) {
        try {
        	//item.setImage(i, null);
        	item.setForeground(i, null);
        } catch (NoSuchMethodError e) {
        }
  		}
  		*/
    }

    item = newRow;

  	setSubItemCount(numSubItems);
		item.setExpanded(expanded);
		// Need to execute (de)select later, because if we are in a paint event
		// that paint event may be reporting a row selected before the selection
		// event fired (Cocoa 3650 fires paint before select yet the row is marked
		// selected)
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (table.isDisposed() || item == null) {
					return;
				}
				if (item.isDisposed()
						|| item.getData("TableRow") != BufferedTableRow.this) {
					return;
				}
				// select/deselect takes less time than a table.isSelected (tree)
				if (isSelected()) {
					table.select(item);
				} else {
					table.deselect(item);
				}
			}
		});
		if (isVisible && !inPaintItem()) {
		//if (newRowHadItem && isVisible && !inPaintItem()) {
			//invalidate();
			// skip the visibility check and SWT thread wrapping of invalidate
			Rectangle r = item.getBounds(0);
			table.redraw(0, r.y, table.getClientArea().width, r.height, true);
		}

    return true;
  }

  public boolean setHeight(int iHeight) {
  	return setIconSize(new Point(1, iHeight));
  }
  
  public int getHeight() {
  	return ptIconSize == null ? 0 : ptIconSize.y;
  }
  
  public boolean setIconSize(Point pt) {
    ptIconSize = pt;

    if (pt == null)
      return false;
    
		if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED))
			return false;
		
    Image oldImage = item.getImage(0);
    if (oldImage != null) {
    	Rectangle r = oldImage.getBounds();
    	if (r.width == pt.x && r.height == pt.y)
    		return false;
    }
		
    // set row height by setting image
    Image image = new Image(item.getDisplay(), pt.x, pt.y);
    item.setImage(0, image);
    item.setImage(0, null);
    image.dispose();
    
    return true;
  }

  /**
	 * Whether the row is currently visible to the user
	 * 
	 * @return visibility
	 */
  public boolean isVisible() {
  	return checkWidget(REQUIRE_VISIBILITY);
  }
	
  /**
   * Overridable function that is called when row needs invalidation.
   *
   */
  public void invalidate() {
  	Utils.execSWTThread(new AERunnable() {
		
			public void runSupport() {
		  	if (!checkWidget(REQUIRE_TABLEITEM_INITIALIZED | REQUIRE_VISIBILITY))
		  		return;

				Rectangle r = item.getBounds(0);

				table.redraw(0, r.y, table.getClientArea().width, r.height, true);
			}
		});
  }
  
  public void setBackgroundImage(Image image) {
  	if (imageBG != null && !imageBG.isDisposed()) {
  		imageBG.dispose();
  	}
  	imageBG = image;
  }
  
  public Image getBackgroundImage() {
  	return imageBG;
  }

	public void setSubItemCount(int i) {
		numSubItems = i;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (item != null && !item.isDisposed()) {
					item.setItemCount(numSubItems == 0 ? 0 : 1);
				}
			}
		});
	}
	
	public int getSubItemCount() {
		return numSubItems;
	}
	
	public TableItemOrTreeItem[] getSubItems() {
		return table.getItems();
	}

	public void setExpanded(boolean b) {
		expanded = b;
		if (item != null && !item.isDisposed()) {
	    if (item.getItemCount() != numSubItems) {
	    	item.setItemCount(numSubItems);
				Rectangle r = item.getBounds(0);
				if (r != null) {
					table.redraw(0, r.y, table.getClientArea().width, r.height, true);
				}
	    }
	    TableItemOrTreeItem[] items = item.getItems();
	    for (TableItemOrTreeItem subItem : items) {
				subItem.setData("TableRow", null);
			}
	    item.setExpanded(b);
		}
	}
	
	public boolean isExpanded() {
		return expanded;
	}

	/**
	 * @return
	 *
	 * @since 4.4.0.5
	 */
	public boolean inPaintItem() {
		if (item != null && !item.isDisposed()) {
			InPaintInfo info = (InPaintInfo) table.getData("inPaintInfo");
			if (info != null && item.equals(info.item)) {
				return true;
			}
		}
		return false;
	}

	public boolean isVisibleNoSWT() {
		return true;  // assume the worst
	}
	
	public TableItemOrTreeItem getItem() {
		return item;
	}
}
