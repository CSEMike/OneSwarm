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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.views.table.TableColumnOrTreeColumn;
import org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * @author TuxPaper
 * @created May 3, 2010
 *
 */
public class TableOrTreeUtils
{
	private static Map<Object, Object> mapDelegates = new HashMap<Object, Object>();

	public static TableItemOrTreeItem getEventItem(Widget item) {
		synchronized (mapDelegates) {
			Object object = mapDelegates.get(item);
			if (object instanceof TableItemOrTreeItem) {
				return (TableItemOrTreeItem) object;
			}
		}

		TableItemOrTreeItem delegate = null;
		synchronized (mapDelegates) {
			if (item instanceof TreeItem) {
				delegate = new TreeItemDelegate((TreeItem) item);
				mapDelegates.put(item, delegate);
			} else if (item instanceof TableItem) {
				delegate = new TableItemDelegate((TableItem) item);
				mapDelegates.put(item, delegate);
			}
		}

		if (delegate != null) {
			delegate.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					synchronized (mapDelegates) {
						mapDelegates.remove(e.widget);
					}
				}
			});
		}
		return delegate;
	}

	public static TableOrTreeSWT getTableOrTreeSWT(Widget widget) {
		synchronized (mapDelegates) {
			Object object = mapDelegates.get(widget);
			if (object instanceof TableOrTreeSWT) {
				return (TableOrTreeSWT) object;
			}
		}

		TableOrTreeSWT delegate = null;
		synchronized (mapDelegates) {
			if (widget instanceof Tree) {
				delegate = new TreeDelegate((Tree) widget);
				mapDelegates.put(widget, delegate);
			} else if (widget instanceof Table) {
				delegate = new TableDelegate((Table) widget);
				mapDelegates.put(widget, delegate);
			}
		}

		if (delegate != null) {
			delegate.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					synchronized (mapDelegates) {
						mapDelegates.remove(e.widget);
					}
				}
			});
		}
		return delegate;
	}

	public static TableItemOrTreeItem createNewItem(TableOrTreeSWT parent,
			int style) {
		TableItemOrTreeItem delegate = null;
		synchronized (mapDelegates) {
			if (parent instanceof TreeDelegate) {
				delegate = new TreeItemDelegate(parent, style);
				mapDelegates.put(((TreeItemDelegate) delegate).item, delegate);
			} else if (parent instanceof TableDelegate) {
				delegate = new TableItemDelegate((TableDelegate) parent, style);
				mapDelegates.put(((TableItemDelegate)delegate).item, delegate);
			}
		}

		if (delegate != null) {
			delegate.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					synchronized (mapDelegates) {
						mapDelegates.remove(e.widget);
					}
				}
			});
		}
		return delegate;
	}

	public static TableColumnOrTreeColumn getTableColumnEventItem(Widget item) {
		if (item instanceof TreeColumn) {
			return new TreeColumnDelegate((TreeColumn) item);
		}
		if (item instanceof TableColumn) {
			return new TableColumnDelegate((TableColumn) item);
		}
		return null;
	}

	public static TableOrTreeSWT createGrid(Composite parent, int style,
			boolean tree) {
		TableOrTreeSWT delegate = null;
		synchronized (mapDelegates) {
			if (tree) {
				try {
					delegate = new TreeDelegate(parent, style);
					mapDelegates.put(((TreeDelegate) delegate).getComposite(), delegate);
				} catch (Exception e) {
					Debug.out(e);
				}
			} else {
				delegate = new TableDelegate(parent, style);
				mapDelegates.put(((TableDelegate)delegate).getComposite(), delegate);
			}
		}

		if (delegate != null) {
			delegate.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					synchronized (mapDelegates) {
						mapDelegates.remove(e.widget);
					}
				}
			});
		}
		return delegate;
	}

	public static ControlEditor createTableOrTreeEditor(TableOrTreeSWT tableOrTree) {
		return (tableOrTree instanceof TreeDelegate) ? new TreeEditor(
				(Tree) tableOrTree.getComposite()) : new TableEditor(
				(Table) tableOrTree.getComposite());
	}

	/**
	 * @since 4.4.0.5
	 */
	public static void setEditorItem(ControlEditor editor, Control input,
			int column, TableItemOrTreeItem item) {
		if (item instanceof TreeItemDelegate) {
			((TreeEditor) editor).setEditor(input, (TreeItem) item.getItem(), column);
		} else {
			((TableEditor) editor).setEditor(input, (TableItem) item.getItem(),
					column);
		}
	}

}
