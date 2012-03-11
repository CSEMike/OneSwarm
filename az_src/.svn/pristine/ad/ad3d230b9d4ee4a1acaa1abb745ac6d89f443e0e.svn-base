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
 * @created May 3, 2010
 *
 */
public class TreeColumnDelegate implements TableColumnOrTreeColumn
{
	TreeColumn treeColumn;

	public TreeColumnDelegate(TreeColumn item) {
		treeColumn = item;
	}

	public TreeColumnDelegate(TableOrTreeSWT parent, int style) {
		treeColumn = new TreeColumn((Tree) parent.getComposite(), style);
	}

	public Image getImage() {
		return treeColumn.getImage();
	}

	public String getText() {
		return treeColumn.getText();
	}

	public void addControlListener(ControlListener listener) {
		treeColumn.addControlListener(listener);
	}

	public void addListener(int eventType, Listener listener) {
		treeColumn.addListener(eventType, listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		treeColumn.addSelectionListener(listener);
	}

	public void addDisposeListener(DisposeListener listener) {
		treeColumn.addDisposeListener(listener);
	}

	public boolean getMoveable() {
		return treeColumn.getMoveable();
	}

	public TableOrTreeSWT getParent() {
		return TableOrTreeUtils.getTableOrTreeSWT(treeColumn.getParent());
	}

	public boolean getResizable() {
		return treeColumn.getResizable();
	}

	public String getToolTipText() {
		return treeColumn.getToolTipText();
	}

	public int getWidth() {
		return treeColumn.getWidth();
	}

	public void dispose() {
		treeColumn.dispose();
	}

	public boolean equals(Object obj) {
		return treeColumn.equals(obj);
	}

	public int getAlignment() {
		return treeColumn.getAlignment();
	}

	public Object getData() {
		return treeColumn.getData();
	}

	public Object getData(String key) {
		return treeColumn.getData(key);
	}

	public Display getDisplay() {
		return treeColumn.getDisplay();
	}

	public Listener[] getListeners(int eventType) {
		return treeColumn.getListeners(eventType);
	}

	public int getStyle() {
		return treeColumn.getStyle();
	}

	public int hashCode() {
		return treeColumn.hashCode();
	}

	public void pack() {
		treeColumn.pack();
	}

	public void removeControlListener(ControlListener listener) {
		treeColumn.removeControlListener(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		treeColumn.removeSelectionListener(listener);
	}

	public void setAlignment(int alignment) {
		treeColumn.setAlignment(alignment);
	}

	public void setImage(Image image) {
		treeColumn.setImage(image);
	}

	public void setMoveable(boolean moveable) {
		treeColumn.setMoveable(moveable);
	}

	public void setResizable(boolean resizable) {
		treeColumn.setResizable(resizable);
	}

	public void setText(String string) {
		treeColumn.setText(string);
	}

	public void setToolTipText(String string) {
		treeColumn.setToolTipText(string);
	}

	public boolean isDisposed() {
		return treeColumn.isDisposed();
	}

	public boolean isListening(int eventType) {
		return treeColumn.isListening(eventType);
	}

	public void setWidth(int width) {
		treeColumn.setWidth(width);
	}

	public void notifyListeners(int eventType, Event event) {
		treeColumn.notifyListeners(eventType, event);
	}

	public void removeListener(int eventType, Listener listener) {
		treeColumn.removeListener(eventType, listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		treeColumn.removeDisposeListener(listener);
	}

	public void setData(Object data) {
		treeColumn.setData(data);
	}

	public void setData(String key, Object value) {
		treeColumn.setData(key, value);
	}

	public String toString() {
		return treeColumn.toString();
	}

	/////////////////
	
	public Item getColumn() {
		return treeColumn;
	}
}
