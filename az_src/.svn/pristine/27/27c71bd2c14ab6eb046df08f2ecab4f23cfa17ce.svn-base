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

import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;


/**
 * Provides caller with access to a tree or table without needing to know
 * which it's operating on
 * 
 * @author TuxPaper
 * @created May 3, 2010
 */
public interface TableOrTreeSWT
{

	public Rectangle computeTrim(int x, int y, int width, int height);

	public void addControlListener(ControlListener listener);

	public void changed(Control[] changed);

	public void addDragDetectListener(DragDetectListener listener);

	public Rectangle getClientArea();

	public void addListener(int eventType, Listener listener);

	public void addFocusListener(FocusListener listener);

	public ScrollBar getHorizontalBar();

	public void addDisposeListener(DisposeListener listener);

	public ScrollBar getVerticalBar();

	public void addHelpListener(HelpListener listener);

	public void addKeyListener(KeyListener listener);

	public void addMenuDetectListener(MenuDetectListener listener);

	public void addMouseListener(MouseListener listener);

	public void addSelectionListener(SelectionListener listener);

	public void addMouseTrackListener(MouseTrackListener listener);

	public void addMouseMoveListener(MouseMoveListener listener);

	public void addMouseWheelListener(MouseWheelListener listener);

	public int getBackgroundMode();

	public void addPaintListener(PaintListener listener);

	public Control[] getChildren();

	public void addTraverseListener(TraverseListener listener);

	public void dispose();

	public Layout getLayout();

	public Control[] getTabList();

	public boolean getLayoutDeferred();

	public Point computeSize(int wHint, int hHint);

	public boolean isLayoutDeferred();

	public Object getData();

	public void layout();

	public Object getData(String key);

	public void layout(boolean changed);

	public Display getDisplay();

	public Listener[] getListeners(int eventType);

	public void layout(boolean changed, boolean all);

	public int getStyle();

	public boolean dragDetect(Event event);

	public boolean isListening(int eventType);

	public boolean dragDetect(MouseEvent event);

	public void notifyListeners(int eventType, Event event);

	public void removeListener(int eventType, Listener listener);

	public void removeDisposeListener(DisposeListener listener);

	public void setBackgroundMode(int mode);

	public boolean setFocus();

	public void setLayout(Layout layout);

	public int getBorderWidth();

	public void setLayoutDeferred(boolean defer);

	public Rectangle getBounds();

	public void setTabList(Control[] tabList);

	public void setData(Object data);

	public Cursor getCursor();

	public void setData(String key, Object value);

	public boolean getDragDetect();

	public boolean getEnabled();

	public Font getFont();

	public Color getForeground();

	public Object getLayoutData();

	public Point getLocation();

	public Menu getMenu();

	public Monitor getMonitor();

	public Composite getParent();

	public Region getRegion();

	public Shell getShell();

	public Point getSize();

	public String getToolTipText();

	public boolean getVisible();

	public String toString();

	public boolean isEnabled();

	public boolean isFocusControl();

	public boolean isReparentable();

	public boolean isVisible();

	public void moveAbove(Control control);

	public void moveBelow(Control control);

	public void pack();

	public void pack(boolean changed);

	public void clear(int index, boolean all);

	public boolean print(GC gc);

	public void clearAll(boolean all);

	public Point computeSize(int wHint, int hHint, boolean changed);

	public void redraw();

	public void redraw(int x, int y, int width, int height, boolean all);

	public void removeControlListener(ControlListener listener);

	public void removeDragDetectListener(DragDetectListener listener);

	public void removeFocusListener(FocusListener listener);

	public void removeHelpListener(HelpListener listener);

	public void removeKeyListener(KeyListener listener);

	public void removeMenuDetectListener(MenuDetectListener listener);

	public void removeMouseTrackListener(MouseTrackListener listener);

	public void removeMouseListener(MouseListener listener);

	public void removeMouseMoveListener(MouseMoveListener listener);

	public void removeMouseWheelListener(MouseWheelListener listener);

	public void removePaintListener(PaintListener listener);

	public void removeTraverseListener(TraverseListener listener);

	public void deselect(TableItemOrTreeItem item);

	public void setBackground(Color color);

	public void deselectAll();

	public boolean equals(Object obj);

	public boolean forceFocus();

	public Accessible getAccessible();

	public Color getBackground();

	public Image getBackgroundImage();

	public void setBackgroundImage(Image image);

	public void setBounds(int x, int y, int width, int height);

	public void setBounds(Rectangle rect);

	public void setCapture(boolean capture);

	public void setCursor(Cursor cursor);

	public void setDragDetect(boolean dragDetect);

	public void setEnabled(boolean enabled);

	public void setForeground(Color color);

	public void setLayoutData(Object layoutData);

	public void setLocation(int x, int y);

	public void setLocation(Point location);

	public void setMenu(Menu menu);

	public int getGridLineWidth();

	public int getHeaderHeight();

	public boolean getHeaderVisible();

	public void setRegion(Region region);

	public void setSize(int width, int height);

	public TableColumnOrTreeColumn getColumn(int index);

	public void setSize(Point size);

	public int getColumnCount();

	public void setToolTipText(String string);

	public int[] getColumnOrder();

	public void setVisible(boolean visible);

	public TableColumnOrTreeColumn[] getColumns();

	public TableItemOrTreeItem getItem(int index);

	public Point toControl(int x, int y);

	public Point toControl(Point point);

	public TableItemOrTreeItem getItem(Point point);

	public Point toDisplay(int x, int y);

	public Point toDisplay(Point point);

	public int getItemCount();

	public int getItemHeight();

	public TableItemOrTreeItem[] getItems();

	public boolean getLinesVisible();

	public TableItemOrTreeItem getParentItem();

	public TableItemOrTreeItem[] getSelection();

	public int getSelectionCount();

	public TableColumnOrTreeColumn getSortColumn();

	public int getSortDirection();

	public TableItemOrTreeItem getTopItem();

	public int hashCode();

	public boolean isDisposed();

	public void update();

	public int indexOf(TableColumnOrTreeColumn column);

	public int indexOf(TableItemOrTreeItem item);

	public boolean setParent(Composite parent);

	public void removeAll();

	public void removeSelectionListener(SelectionListener listener);

	public void removeTreeListener(TreeListener listener);

	public void setInsertMark(TableItemOrTreeItem item, boolean before);

	public void setItemCount(int count);

	public void setLinesVisible(boolean show);

	public void select(TableItemOrTreeItem item);

	public void selectAll();

	public void setColumnOrder(int[] order);

	public void setFont(Font font);

	public void setHeaderVisible(boolean show);

	public void setRedraw(boolean redraw);

	public void setSelection(TableItemOrTreeItem item);

	public void setSelection(TableItemOrTreeItem[] items);

	public void setSortColumn(TableColumnOrTreeColumn column);

	public void setSortDirection(int direction);

	public void setTopItem(TableItemOrTreeItem item);

	public void showColumn(TableColumnOrTreeColumn column);

	public void showItem(TableItemOrTreeItem item);

	public void showSelection();

	////////////
	
	public Composite getComposite();

	public int getTopIndex();

	public int getSelectionIndex();

	public int[] getSelectionIndices();

	public void setSelection(int[] newSelectedRowIndices);

	public void select(int[] newSelectedRowIndices);

	public boolean isSelected(TableItemOrTreeItem item);

	boolean equalsTableOrTree(TableOrTreeSWT tt);
	
	public TableItemOrTreeItem createNewItem(int style);

	public TableColumnOrTreeColumn createNewColumn(int style);

	public int indexOf(Widget item);
}