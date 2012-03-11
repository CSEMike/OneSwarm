/**
 * Created on May 3, 2010
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

package org.gudy.azureus2.ui.swt.views.table;

import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;


/**
 * @author TuxPaper
 * @created May 3, 2010
 *
 */
public interface TableItemOrTreeItem
{

	public void addListener(int eventType, Listener listener);

	public void addDisposeListener(DisposeListener listener);

	public void clear(int index, boolean all);

	public void clearAll(boolean all);

	public void dispose();

	public boolean equals(Object obj);

	public Color getBackground();

	public Color getBackground(int index);

	public Rectangle getBounds();

	public Rectangle getBounds(int index);

	public Object getData();

	public Object getData(String key);

	public Display getDisplay();

	public boolean getChecked();

	public Listener[] getListeners(int eventType);

	public boolean getExpanded();

	public int getStyle();

	public Font getFont();

	public Font getFont(int index);

	public Color getForeground();

	public Color getForeground(int index);

	public boolean getGrayed();

	public void notifyListeners(int eventType, Event event);

	public TableItemOrTreeItem getItem(int index);

	public int getItemCount();

	public TableItemOrTreeItem[] getItems();

	public Image getImage();

	public Image getImage(int index);

	public Rectangle getImageBounds(int index);

	public void removeListener(int eventType, Listener listener);

	public TableOrTreeSWT getParent();

	public TableItemOrTreeItem getParentItem();

	public String getText();

	public String getText(int index);

	public void removeDisposeListener(DisposeListener listener);

	public Rectangle getTextBounds(int index);

	public int hashCode();

	public boolean isDisposed();

	public boolean isListening(int eventType);

	public int indexOf(TableItemOrTreeItem item);

	public void removeAll();

	public void setBackground(Color color);

	public void setBackground(int index, Color color);

	public void setData(Object data);

	public void setData(String key, Object value);

	public void setChecked(boolean checked);

	public void setExpanded(boolean expanded);

	public void setFont(Font font);

	public String toString();

	public void setFont(int index, Font font);

	public void setForeground(Color color);

	public void setForeground(int index, Color color);

	public void setGrayed(boolean grayed);

	public void setImage(Image[] images);

	public void setImage(int index, Image image);

	public void setImage(Image image);

	public void setItemCount(int count);

	public void setText(String[] strings);

	public void setText(int index, String string);

	public void setText(String string);

	///////////////
	
	public Item getItem();
}