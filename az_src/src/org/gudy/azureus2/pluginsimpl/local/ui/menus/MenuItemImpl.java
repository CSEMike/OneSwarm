/*
 * Created on 6 Feb 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.pluginsimpl.local.ui.menus;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;

/**
 * amc1: This class was largely derived from TableContextMenuImpl.
 */
public class MenuItemImpl implements MenuItem {
	private String sMenuID;

	private String sName;

	private int style = STYLE_PUSH;

	private boolean enabled = true;

	private Object data;

	private Graphic graphic;

	private List listeners = new ArrayList();
	private List m_listeners = new ArrayList();

	private List fill_listeners = new ArrayList();

	private List children = new ArrayList();

	private MenuItemImpl parent = null;

	private String display_text = null;
	
	private boolean visible = true;

	public MenuItemImpl(String menuID, String key) {
		sMenuID = menuID;
		sName = key;
	}

	public MenuItemImpl(MenuItemImpl ti, String key) {
		this.parent = ti;
		this.parent.addChildMenuItem(this);
		this.sMenuID = this.parent.getMenuID();
		this.sName = key;
	}

	public String getResourceKey() {
		return sName;
	}

	public String getMenuID() {
		return sMenuID;
	}

	public int getStyle() {
		return (style);
	}

	public void setStyle(int _style) {
		if (this.style == MenuItem.STYLE_MENU && _style != MenuItem.STYLE_MENU) {
			throw new RuntimeException(
					"cannot revert menu style MenuItem object to another style");
		}
		style = _style;
	}

	public Object getData() {
		return (data);
	}

	public void setData(Object _data) {
		data = _data;
	}

	public boolean isEnabled() {
		return (enabled);
	}

	public void setEnabled(boolean _enabled) {
		enabled = _enabled;
	}

	public void setGraphic(Graphic _graphic) {
		graphic = _graphic;
	}

	public Graphic getGraphic() {
		return (graphic);
	}

	public void invokeMenuWillBeShownListeners(Object o) {
		for (int i = 0; i < fill_listeners.size(); i++) {
			try {
				((MenuItemFillListener) (fill_listeners.get(i)))
						.menuWillBeShown(this, o);
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public void addFillListener(MenuItemFillListener listener) {
		fill_listeners.add(listener);
	}

	public void removeFillListener(MenuItemFillListener listener) {
		fill_listeners.remove(listener);
	}
	
	  // Currently used by TableView (and other places).
	  public void invokeListenersMulti(Object[] rows) {
		  // We invoke the multi listeners first...
		  invokeListenersOnList(this.m_listeners, rows);
		  if (rows == null) {invokeListenersSingle(null); return;}
		  for (int i=0; i<rows.length; i++) {
			  invokeListenersSingle(rows[i]);
		  }
	  }
	 
	  public void addMultiListener(MenuItemListener l) {
		  m_listeners.add(l);
	  }
	  
	  public void removeMultiListener(MenuItemListener l) {
		  m_listeners.remove(l);
	  }

	private void invokeListenersSingle(Object o) {
		invokeListenersOnList(this.listeners, o);
	}

	public void addListener(MenuItemListener l) {
		listeners.add(l);
	}

	public void removeListener(MenuItemListener l) {
		listeners.remove(l);
	}

	public MenuItem getParent() {
		return this.parent;
	}

	public MenuItem[] getItems() {
		if (this.style != MenuItem.STYLE_MENU) {
			return null;
		}
		return (MenuItem[]) this.children.toArray(new MenuItem[this.children
				.size()]);
	}

	public MenuItem getItem(String key) {
		if (this.style != MenuItem.STYLE_MENU) {
			return null;
		}
		java.util.Iterator itr = this.children.iterator();
		MenuItem result = null;
		while (itr.hasNext()) {
			result = (MenuItem) itr.next();
			if (key.equals(result.getResourceKey())) {
				return result;
			}
		}
		return null;
	}

	private void addChildMenuItem(MenuItem child) {
		if (this.style != MenuItem.STYLE_MENU) {
			throw new RuntimeException("cannot add to non-container MenuItem");
		}
		this.children.add(child);
	}

	public String getText() {
		if (this.display_text == null) {
			return MessageText.getString(this.getResourceKey());
		}
		return this.display_text;
	}

	public void setText(String text) {
		this.display_text = text;
	}

	protected void invokeListenersOnList(List listeners_to_notify, Object o) {
		for (int i = 0; i < listeners_to_notify.size(); i++) {
			try {
				((MenuItemListener) (listeners_to_notify.get(i))).selected(
						this, o);
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void removeWithEvents(int root_menu_event, int sub_menu_event) {
		removeAllChildItems();
		if (this.parent != null) {
			UIManagerImpl.fireEvent(sub_menu_event, new Object[]{this.parent, this});
			parent.children.remove(this);
			this.parent = null;
		}
		else {
			UIManagerImpl.fireEvent(root_menu_event, this);
		}
		this.data = null;
		this.graphic = null;
		this.listeners.clear();
		this.fill_listeners.clear();
		this.m_listeners.clear();		
	}
	
	public void remove() {
		removeWithEvents(UIManagerEvent.ET_REMOVE_MENU_ITEM, UIManagerEvent.ET_REMOVE_SUBMENU_ITEM);
	}

	public void removeAllChildItems() {
		// This should make this.children be empty...
		MenuItem[] children = this.getItems();
		if (children != null) {
			for (int i=0; i<children.length; i++) {children[i].remove();}
		}
	}
	
	public boolean isVisible() {return visible;}
	public void setVisible(boolean visible) {this.visible = visible;}
	
	public boolean isSelected() {
		if (style != STYLE_CHECK && style != STYLE_RADIO) {
			throw new RuntimeException("Style is not STYLE_CHECK or STYLE_RADIO");
		}
		if (data == null) {
			throw new RuntimeException("Item is neither selected or deselected");
		}
		if (!(data instanceof Boolean)) {
			throw new RuntimeException("Invalid data assigned to menu item, should be boolean: " + data);
		}
		return ((Boolean)data).booleanValue();
	}

}