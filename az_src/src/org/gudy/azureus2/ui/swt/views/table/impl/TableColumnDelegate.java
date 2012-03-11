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

import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.views.table.TableColumnOrTreeColumn;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * @author TuxPaper
 * @created May 5, 2010
 *
 */
public class TableColumnDelegate
	implements TableColumnOrTreeColumn
{
	TableColumn column;

	public TableColumnDelegate(TableColumn column) {
		this.column = column;
	}

	public TableColumnDelegate(Table table, int style) {
		column = new TableColumn(table, style);
	}

	public Image getImage() {
		return column.getImage();
	}

	public String getText() {
		return column.getText();
	}

	public void addControlListener(ControlListener listener) {
		column.addControlListener(listener);
	}

	public void addListener(int eventType, Listener listener) {
		column.addListener(eventType, listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		column.addSelectionListener(listener);
	}

	public void addDisposeListener(DisposeListener listener) {
		column.addDisposeListener(listener);
	}

	public TableOrTreeSWT getParent() {
		return TableOrTreeUtils.getTableOrTreeSWT(column.getParent());
	}

	public boolean getMoveable() {
		return column.getMoveable();
	}

	public boolean getResizable() {
		return column.getResizable();
	}

	public String getToolTipText() {
		return column.getToolTipText();
	}

	public int getWidth() {
		return column.getWidth();
	}

	public void dispose() {
		column.dispose();
	}

	public boolean equals(Object obj) {
		return column.equals(obj);
	}

	public int getAlignment() {
		return column.getAlignment();
	}

	public Object getData() {
		return column.getData();
	}

	public Object getData(String key) {
		return column.getData(key);
	}

	public Display getDisplay() {
		return column.getDisplay();
	}

	public Listener[] getListeners(int eventType) {
		return column.getListeners(eventType);
	}

	public int getStyle() {
		return column.getStyle();
	}

	public int hashCode() {
		return column.hashCode();
	}

	public void pack() {
		column.pack();
	}

	public void removeControlListener(ControlListener listener) {
		column.removeControlListener(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		column.removeSelectionListener(listener);
	}

	public void setAlignment(int alignment) {
		column.setAlignment(alignment);
	}

	public void setImage(Image image) {
		column.setImage(image);
	}

	public void setMoveable(boolean moveable) {
		column.setMoveable(moveable);
	}

	public boolean isDisposed() {
		return column.isDisposed();
	}

	public void setResizable(boolean resizable) {
		column.setResizable(resizable);
	}

	public boolean isListening(int eventType) {
		return column.isListening(eventType);
	}

	public void notifyListeners(int eventType, Event event) {
		column.notifyListeners(eventType, event);
	}

	public void setText(String string) {
		column.setText(string);
	}

	public void setToolTipText(String string) {
		column.setToolTipText(string);
	}

	public void removeListener(int eventType, Listener listener) {
		column.removeListener(eventType, listener);
	}

	public void setWidth(int width) {
		column.setWidth(width);
	}

	public void removeDisposeListener(DisposeListener listener) {
		column.removeDisposeListener(listener);
	}

	public void setData(Object data) {
		column.setData(data);
	}

	public void setData(String key, Object value) {
		column.setData(key, value);
	}

	public String toString() {
		return column.toString();
	}

	/////////
	
	public Item getColumn() {
		return column;
	}
}
