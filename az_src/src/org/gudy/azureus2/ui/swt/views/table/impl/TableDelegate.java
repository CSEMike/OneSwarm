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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.views.table.TableColumnOrTreeColumn;
import org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * Delegates a SWT {@link Table} into a {@link TableOrTreeSWT} allowing easy
 * switching from Table and Tree.
 * <p>
 * Uses own map for setData and getData for faster lookups and no SWT thread
 * checking
 * 
 * @author TuxPaper
 * @created May 5, 2010
 *
 */
public class TableDelegate
	implements TableOrTreeSWT
{
	Table table;
	
	Map<String, Object> data = new HashMap<String, Object>(5);

	private TableDelegate() {
	}

	protected TableDelegate(Table table) {
		this.table = table;
	}

	protected TableDelegate(Composite parent, int style) {
		table = new Table(parent, style);
	}

	public Rectangle computeTrim(int x, int y, int width, int height) {
		return table.computeTrim(x, y, width, height);
	}

	public void addControlListener(ControlListener listener) {
		table.addControlListener(listener);
	}

	public void changed(Control[] changed) {
		table.changed(changed);
	}

	public void addDragDetectListener(DragDetectListener listener) {
		table.addDragDetectListener(listener);
	}

	public Rectangle getClientArea() {
		return table.getClientArea();
	}

	public void addListener(int eventType, Listener listener) {
		table.addListener(eventType, listener);
	}

	public void addFocusListener(FocusListener listener) {
		table.addFocusListener(listener);
	}

	public ScrollBar getHorizontalBar() {
		return table.getHorizontalBar();
	}

	public void addDisposeListener(DisposeListener listener) {
		table.addDisposeListener(listener);
	}

	public ScrollBar getVerticalBar() {
		return table.getVerticalBar();
	}

	public void addHelpListener(HelpListener listener) {
		table.addHelpListener(listener);
	}

	public void addKeyListener(KeyListener listener) {
		table.addKeyListener(listener);
	}

	public void addMenuDetectListener(MenuDetectListener listener) {
		table.addMenuDetectListener(listener);
	}

	public void addMouseListener(MouseListener listener) {
		table.addMouseListener(listener);
	}

	public void addMouseTrackListener(MouseTrackListener listener) {
		table.addMouseTrackListener(listener);
	}

	public void addMouseMoveListener(MouseMoveListener listener) {
		table.addMouseMoveListener(listener);
	}

	public void addMouseWheelListener(MouseWheelListener listener) {
		table.addMouseWheelListener(listener);
	}

	public int getBackgroundMode() {
		return table.getBackgroundMode();
	}

	public void addPaintListener(PaintListener listener) {
		table.addPaintListener(listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		table.addSelectionListener(listener);
	}

	public Control[] getChildren() {
		return table.getChildren();
	}

	public void addTraverseListener(TraverseListener listener) {
		table.addTraverseListener(listener);
	}

	public void dispose() {
		table.dispose();
	}

	public Layout getLayout() {
		return table.getLayout();
	}

	public Control[] getTabList() {
		return table.getTabList();
	}

	public boolean getLayoutDeferred() {
		return table.getLayoutDeferred();
	}

	public Point computeSize(int wHint, int hHint) {
		return table.computeSize(wHint, hHint);
	}

	public boolean isLayoutDeferred() {
		return table.isLayoutDeferred();
	}

	public Object getData() {
		return getData(null);
	}

	public void layout() {
		table.layout();
	}

	public Object getData(String key) {
		synchronized (data) {
			return data.get(key);
		}
	}

	public void layout(boolean changed) {
		table.layout(changed);
	}

	public Display getDisplay() {
		return table.getDisplay();
	}

	public Listener[] getListeners(int eventType) {
		return table.getListeners(eventType);
	}

	public void layout(boolean changed, boolean all) {
		table.layout(changed, all);
	}

	public int getStyle() {
		return table.getStyle();
	}

	public boolean dragDetect(Event event) {
		return table.dragDetect(event);
	}

	public boolean isDisposed() {
		return table.isDisposed();
	}

	public boolean isListening(int eventType) {
		return table.isListening(eventType);
	}

	public boolean dragDetect(MouseEvent event) {
		return table.dragDetect(event);
	}

	public void notifyListeners(int eventType, Event event) {
		table.notifyListeners(eventType, event);
	}

	public void removeListener(int eventType, Listener listener) {
		table.removeListener(eventType, listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		table.removeDisposeListener(listener);
	}

	public void setBackgroundMode(int mode) {
		table.setBackgroundMode(mode);
	}

	public boolean setFocus() {
		return table.setFocus();
	}

	public Image getBackgroundImage() {
		return table.getBackgroundImage();
	}

	public void setLayout(Layout layout) {
		table.setLayout(layout);
	}

	public int getBorderWidth() {
		return table.getBorderWidth();
	}

	public void setLayoutDeferred(boolean defer) {
		table.setLayoutDeferred(defer);
	}

	public Rectangle getBounds() {
		return table.getBounds();
	}

	public void setTabList(Control[] tabList) {
		table.setTabList(tabList);
	}

	public void setData(Object data) {
		setData(null, data);
	}

	public Cursor getCursor() {
		return table.getCursor();
	}

	public void setData(String key, Object value) {
		synchronized (data) {
			data.put(key, value);
		}
	}

	public boolean getDragDetect() {
		return table.getDragDetect();
	}

	public boolean getEnabled() {
		return table.getEnabled();
	}

	public Font getFont() {
		return table.getFont();
	}

	public Color getForeground() {
		return table.getForeground();
	}

	public Object getLayoutData() {
		return table.getLayoutData();
	}

	public Point getLocation() {
		return table.getLocation();
	}

	public Menu getMenu() {
		return table.getMenu();
	}

	public Monitor getMonitor() {
		return table.getMonitor();
	}

	public void clear(int index) {
		table.clear(index);
	}

	public Composite getParent() {
		return table.getParent();
	}

	public Region getRegion() {
		return table.getRegion();
	}

	public void clear(int start, int end) {
		table.clear(start, end);
	}

	public Shell getShell() {
		return table.getShell();
	}

	public Point getSize() {
		return table.getSize();
	}

	public String getToolTipText() {
		return table.getToolTipText();
	}

	public boolean getVisible() {
		return table.getVisible();
	}

	public String toString() {
		return table.toString();
	}

	public void clear(int[] indices) {
		table.clear(indices);
	}

	public void clearAll() {
		table.clearAll();
	}

	public boolean isEnabled() {
		return table.isEnabled();
	}

	public Point computeSize(int wHint, int hHint, boolean changed) {
		return table.computeSize(wHint, hHint, changed);
	}

	public boolean isFocusControl() {
		return table.isFocusControl();
	}

	public boolean isReparentable() {
		return table.isReparentable();
	}

	public boolean isVisible() {
		return table.isVisible();
	}

	public void moveAbove(Control control) {
		table.moveAbove(control);
	}

	public void moveBelow(Control control) {
		table.moveBelow(control);
	}

	public void pack() {
		table.pack();
	}

	public void pack(boolean changed) {
		table.pack(changed);
	}

	public boolean print(GC gc) {
		return table.print(gc);
	}

	public void deselect(int[] indices) {
		table.deselect(indices);
	}

	public void deselect(int index) {
		table.deselect(index);
	}

	public void redraw() {
		table.redraw();
	}

	public void deselect(int start, int end) {
		table.deselect(start, end);
	}

	public void redraw(int x, int y, int width, int height, boolean all) {
		table.redraw(x, y, width, height, all);
	}

	public void deselectAll() {
		table.deselectAll();
	}

	public boolean equals(Object obj) {
		return table.equals(obj);
	}

	public boolean forceFocus() {
		return table.forceFocus();
	}

	public Accessible getAccessible() {
		return table.getAccessible();
	}

	public Color getBackground() {
		return table.getBackground();
	}

	public void removeControlListener(ControlListener listener) {
		table.removeControlListener(listener);
	}

	public void removeDragDetectListener(DragDetectListener listener) {
		table.removeDragDetectListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		table.removeFocusListener(listener);
	}

	public void removeHelpListener(HelpListener listener) {
		table.removeHelpListener(listener);
	}

	public void removeKeyListener(KeyListener listener) {
		table.removeKeyListener(listener);
	}

	public void removeMenuDetectListener(MenuDetectListener listener) {
		table.removeMenuDetectListener(listener);
	}

	public void removeMouseTrackListener(MouseTrackListener listener) {
		table.removeMouseTrackListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		table.removeMouseListener(listener);
	}

	public void removeMouseMoveListener(MouseMoveListener listener) {
		table.removeMouseMoveListener(listener);
	}

	public void removeMouseWheelListener(MouseWheelListener listener) {
		table.removeMouseWheelListener(listener);
	}

	public void removePaintListener(PaintListener listener) {
		table.removePaintListener(listener);
	}

	public void removeTraverseListener(TraverseListener listener) {
		table.removeTraverseListener(listener);
	}

	public TableColumnOrTreeColumn getColumn(int index) {
		return wrapOrNull(table.getColumn(index));
	}

	public int getColumnCount() {
		return table.getColumnCount();
	}

	public void setBackground(Color color) {
		table.setBackground(color);
	}

	public int[] getColumnOrder() {
		return table.getColumnOrder();
	}

	public void setBackgroundImage(Image image) {
		table.setBackgroundImage(image);
	}

	public TableColumnOrTreeColumn[] getColumns() {
		return wrapOrNull(table.getColumns());
	}

	public void setBounds(int x, int y, int width, int height) {
		table.setBounds(x, y, width, height);
	}

	public int getGridLineWidth() {
		return table.getGridLineWidth();
	}

	public int getHeaderHeight() {
		return table.getHeaderHeight();
	}

	public boolean getHeaderVisible() {
		return table.getHeaderVisible();
	}

	public TableItemOrTreeItem getItem(int index) {
		return wrapOrNull(table.getItem(index));
	}

	public void setBounds(Rectangle rect) {
		table.setBounds(rect);
	}

	public TableItemOrTreeItem getItem(Point point) {
		return wrapOrNull(table.getItem(point));
	}

	public void setCapture(boolean capture) {
		table.setCapture(capture);
	}

	public void setCursor(Cursor cursor) {
		table.setCursor(cursor);
	}

	public void setDragDetect(boolean dragDetect) {
		table.setDragDetect(dragDetect);
	}

	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
	}

	public int getItemCount() {
		return table.getItemCount();
	}

	public int getItemHeight() {
		return table.getItemHeight();
	}

	public TableItemOrTreeItem[] getItems() {
		return wrapOrNull(table.getItems());
	}

	public boolean getLinesVisible() {
		return table.getLinesVisible();
	}

	public void setForeground(Color color) {
		table.setForeground(color);
	}

	public TableItemOrTreeItem[] getSelection() {
		return wrapOrNull(table.getSelection());
	}

	public void setLayoutData(Object layoutData) {
		table.setLayoutData(layoutData);
	}

	public int getSelectionCount() {
		return table.getSelectionCount();
	}

	public void setLocation(int x, int y) {
		table.setLocation(x, y);
	}

	public int getSelectionIndex() {
		return table.getSelectionIndex();
	}

	public void setLocation(Point location) {
		table.setLocation(location);
	}

	public int[] getSelectionIndices() {
		return table.getSelectionIndices();
	}

	public void setMenu(Menu menu) {
		table.setMenu(menu);
	}

	public TableColumnOrTreeColumn getSortColumn() {
		return wrapOrNull(table.getSortColumn());
	}

	public int getSortDirection() {
		return table.getSortDirection();
	}

	public int getTopIndex() {
		int topIndex = table.getTopIndex();
		if (topIndex == 0 && table.getItemCount() == 0) {
			return -1;
		}
		return topIndex;
	}

	public int hashCode() {
		return table.hashCode();
	}

	public void setRegion(Region region) {
		table.setRegion(region);
	}

	public void setSize(int width, int height) {
		table.setSize(width, height);
	}

	public void setSize(Point size) {
		table.setSize(size);
	}

	public int indexOf(TableColumnOrTreeColumn column) {
		if (column == null) {
			return -1;
		}
		return table.indexOf((TableColumn) column.getColumn());
	}

	public void setToolTipText(String string) {
		table.setToolTipText(string);
	}

	public int indexOf(TableItemOrTreeItem item) {
		if (item == null) {
			return -1;
		}
		return table.indexOf((TableItem) item.getItem());
	}

	public void setVisible(boolean visible) {
		table.setVisible(visible);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#isSelected(org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem)
	public boolean isSelected(TableItemOrTreeItem item) {
		return table.isSelected(indexOf(item));
	}

	public Point toControl(int x, int y) {
		return table.toControl(x, y);
	}

	public Point toControl(Point point) {
		return table.toControl(point);
	}

	public Point toDisplay(int x, int y) {
		return table.toDisplay(x, y);
	}

	public Point toDisplay(Point point) {
		return table.toDisplay(point);
	}

	public void remove(int[] indices) {
		table.remove(indices);
	}

	public void remove(int index) {
		table.remove(index);
	}

	public void remove(int start, int end) {
		table.remove(start, end);
	}

	public void removeAll() {
		table.removeAll();
	}

	public void removeSelectionListener(SelectionListener listener) {
		table.removeSelectionListener(listener);
	}

	public void select(int[] indices) {
		table.select(indices);
	}

	public void select(int index) {
		table.select(index);
	}

	public void select(int start, int end) {
		table.select(start, end);
	}

	public void selectAll() {
		table.selectAll();
	}

	public void update() {
		table.update();
	}

	public boolean setParent(Composite parent) {
		return table.setParent(parent);
	}

	public void setColumnOrder(int[] order) {
		table.setColumnOrder(order);
	}

	public void setFont(Font font) {
		table.setFont(font);
	}

	public void setHeaderVisible(boolean show) {
		table.setHeaderVisible(show);
	}

	public void setItemCount(int count) {
		table.setItemCount(count);
	}

	public void setLinesVisible(boolean show) {
		table.setLinesVisible(show);
	}

	public void setRedraw(boolean redraw) {
		table.setRedraw(redraw);
	}

	public void setSelection(int[] indices) {
		table.setSelection(indices);
	}

	public void setSelection(TableItemOrTreeItem item) {
		table.setSelection(item == null ? null : (TableItem) item.getItem());
	}

	public void setSelection(TableItem[] items) {
		table.setSelection(items);
	}

	public void setSelection(int index) {
		table.setSelection(index);
	}

	public void setSelection(int start, int end) {
		table.setSelection(start, end);
	}

	public void setSortColumn(TableColumnOrTreeColumn column) {
		table.setSortColumn(column == null ? null
				: (TableColumn) column.getColumn());
	}

	public void setSortDirection(int direction) {
		table.setSortDirection(direction);
	}

	public void setTopIndex(int index) {
		table.setTopIndex(index);
	}

	public void showColumn(TableColumnOrTreeColumn column) {
		table.showColumn(column == null ? null : (TableColumn) column.getColumn());
	}

	public void showItem(TableItemOrTreeItem item) {
		table.showItem(item == null ? null : (TableItem) item.getItem());
	}

	public void showSelection() {
		table.showSelection();
	}

	///////

	private TableItemOrTreeItem wrapOrNull(TableItem item) {
		if (item == null) {
			return null;
		}
		return TableOrTreeUtils.getEventItem(item);
	}

	private TableItemOrTreeItem[] wrapOrNull(TableItem[] items) {
		if (items == null) {
			return null;
		}
		TableItemOrTreeItem[] returnItems = new TableItemOrTreeItem[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = TableOrTreeUtils.getEventItem(items[i]);
		}
		return returnItems;
	}

	private TableColumnOrTreeColumn wrapOrNull(TableColumn item) {
		if (item == null) {
			return null;
		}
		return new TableColumnDelegate(item);
	}

	private TableColumnOrTreeColumn[] wrapOrNull(TableColumn[] items) {
		if (items == null) {
			return null;
		}
		TableColumnOrTreeColumn[] returnItems = new TableColumnOrTreeColumn[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = new TableColumnDelegate(items[i]);
		}
		return returnItems;
	}

	private TableItem[] toTableItemArray(TableItemOrTreeItem[] items) {
		if (items == null) {
			return null;
		}
		TableItem[] returnItems = new TableItem[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = (TableItem) items[i].getItem();
		}
		return returnItems;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#clear(int, boolean)
	public void clear(int index, boolean allChildren) {
		table.clear(index);
	}

	public void clearAll(boolean allChildren) {
		table.clearAll();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#deselect(org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem)
	public void deselect(TableItemOrTreeItem item) {
		table.deselect(table.indexOf((TableItem) item.getItem()));
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#getParentItem()
	public TableItemOrTreeItem getParentItem() {
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#getTopItem()
	public TableItemOrTreeItem getTopItem() {
		int i = table.getTopIndex();
		return i < 0 || (i == 0 && table.getItemCount() == 0) ? null : getItem(i);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#removeTreeListener(org.eclipse.swt.events.TreeListener)
	public void removeTreeListener(TreeListener listener) {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#setInsertMark(org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem, boolean)
	public void setInsertMark(TableItemOrTreeItem item, boolean before) {
		// TODO Auto-generated method stub

	}

	public void select(TableItemOrTreeItem item) {
		table.select(table.indexOf((TableItem) item.getItem()));
	}

	public void setSelection(TableItemOrTreeItem[] items) {
		int[] indexes = new int[items.length];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = table.indexOf((TableItem) items[i].getItem());
		}
		table.select(indexes);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#setTopItem(org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem)
	public void setTopItem(TableItemOrTreeItem item) {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#getComposite()
	public Composite getComposite() {
		return table;
	}

	public boolean equalsTableOrTree(TableOrTreeSWT tt) {
		return table.equals(tt.getComposite());
	}

	public TableItemOrTreeItem createNewItem(int style) {
		return TableOrTreeUtils.createNewItem(this, style);
	}

	public TableColumnOrTreeColumn createNewColumn(int style) {
		return new TableColumnDelegate(table, style);
	}

  public int indexOf(Widget item) {
  	if (item instanceof TableItem) {
  		return table.indexOf((TableItem) item);
  	}
  	return -1;
  }
}
