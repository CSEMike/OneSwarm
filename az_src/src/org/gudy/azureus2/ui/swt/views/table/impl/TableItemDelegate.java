/**
 * Created on May 5, 2010
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
 
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Map;

import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * @author TuxPaper
 * @created May 5, 2010
 *
 */
public class TableItemDelegate
	implements TableItemOrTreeItem
{
	TableItem item;

	Map data = new LightHashMap(2);

	protected TableItemDelegate(TableItem item2) {
		item = item2;
		if (item == null) {
			System.out.println("NULL");
		}
	}

	protected TableItemDelegate(TableDelegate tableDelegate, int style) {
		item = new TableItem(tableDelegate.table, style);
		if (item == null) {
			System.out.println("NULL");
		}
	}

	public void addListener(int eventType, Listener listener) {
		item.addListener(eventType, listener);
	}

	public void addDisposeListener(DisposeListener listener) {
		item.addDisposeListener(listener);
	}

	public void dispose() {
		item.dispose();
	}

	public boolean equals(Object obj) {
		if (obj instanceof TableItemOrTreeItem) {
			return item.equals(((TableItemOrTreeItem) obj).getItem());
		}
		return item.equals(obj);
	}

	public Color getBackground() {
		return item.getBackground();
	}

	public Color getBackground(int index) {
		return item.getBackground(index);
	}

	public Rectangle getBounds() {
		return item.getBounds();
	}

	public Rectangle getBounds(int index) {
		return item.getBounds(index);
	}

	public boolean getChecked() {
		return item.getChecked();
	}

	public Font getFont() {
		return item.getFont();
	}

	public Font getFont(int index) {
		return item.getFont(index);
	}

	public Color getForeground() {
		return item.getForeground();
	}

	public Color getForeground(int index) {
		return item.getForeground(index);
	}

	public Object getData() {
		return getData(null);
	}
	
	public Object getData(String key) {
		synchronized (data) {
			return data.get(key);
		}
	}

	public boolean getGrayed() {
		return item.getGrayed();
	}

	public Image getImage() {
		return item.getImage();
	}

	public Image getImage(int index) {
		return item.getImage(index);
	}

	public Display getDisplay() {
		return item.getDisplay();
	}

	public Rectangle getImageBounds(int index) {
		return item.getImageBounds(index);
	}

	public Listener[] getListeners(int eventType) {
		return item.getListeners(eventType);
	}

	public int getImageIndent() {
		return item.getImageIndent();
	}

	public TableOrTreeSWT getParent() {
		return TableOrTreeUtils.getTableOrTreeSWT(item.getParent());
	}

	public int getStyle() {
		return item.getStyle();
	}

	public String getText() {
		return item.getText();
	}

	public String getText(int index) {
		return item.getText(index);
	}

	public Rectangle getTextBounds(int index) {
		return item.getTextBounds(index);
	}

	public int hashCode() {
		return item.hashCode();
	}

	public boolean isDisposed() {
		return item.isDisposed();
	}

	public boolean isListening(int eventType) {
		return item.isListening(eventType);
	}

	public void notifyListeners(int eventType, Event event) {
		item.notifyListeners(eventType, event);
	}

	public void setBackground(Color color) {
		item.setBackground(color);
	}

	public void setBackground(int index, Color color) {
		item.setBackground(index, color);
	}

	public void setChecked(boolean checked) {
		item.setChecked(checked);
	}

	public void setFont(Font font) {
		item.setFont(font);
	}

	public void removeListener(int eventType, Listener listener) {
		item.removeListener(eventType, listener);
	}

	public void setFont(int index, Font font) {
		item.setFont(index, font);
	}

	public void removeDisposeListener(DisposeListener listener) {
		item.removeDisposeListener(listener);
	}

	public void setForeground(Color color) {
		item.setForeground(color);
	}

	public void setForeground(int index, Color color) {
		item.setForeground(index, color);
	}

	public void setGrayed(boolean grayed) {
		item.setGrayed(grayed);
	}

	public void setImage(Image[] images) {
		item.setImage(images);
	}

	public void setImage(int index, Image image) {
		item.setImage(index, image);
	}

	public void setImage(Image image) {
		item.setImage(image);
	}

	public void setImageIndent(int indent) {
		item.setImageIndent(indent);
	}

	public void setText(String[] strings) {
		item.setText(strings);
	}
	
	public void setData(Object data) {
		setData(null, data);
	}
	
	public void setData(String key, Object value) {
		synchronized (data) {
			data.put(key, value);
		}
	}

	public void setText(int index, String string) {
		item.setText(index, string);
	}

	public void setText(String string) {
		item.setText(string);
	}

	public String toString() {
		return item.toString();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#clear(int, boolean)
	public void clear(int index, boolean all) {
		// TODO Auto-generated method stub
		
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#clearAll(boolean)
	public void clearAll(boolean all) {
		// TODO Auto-generated method stub
		
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#getExpanded()
	public boolean getExpanded() {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#getItem(int)
	public TableItemOrTreeItem getItem(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getItemCount() {
		return 0;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#getItems()
	public TableItemOrTreeItem[] getItems() {
		return new TableItemOrTreeItem[0];
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#getParentItem()
	public TableItemOrTreeItem getParentItem() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#indexOf(org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem)
	public int indexOf(TableItemOrTreeItem item) {
		// TODO Auto-generated method stub
		return 0;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#removeAll()
	public void removeAll() {
		// TODO Auto-generated method stub
		
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#setExpanded(boolean)
	public void setExpanded(boolean expanded) {
		// TODO Auto-generated method stub
		
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem#setItemCount(int)
	public void setItemCount(int count) {
		// TODO Auto-generated method stub
		
	}

	public Item getItem() {
		return item;
	}
	
	public boolean equals(TableItemOrTreeItem ti) {
		return item.equals(ti.getItem());
	}
}
