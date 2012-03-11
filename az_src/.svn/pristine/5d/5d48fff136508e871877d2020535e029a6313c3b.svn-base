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
package org.gudy.azureus2.ui.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;

/**
 * Similar to TableContextMenuManager - this keeps references to created menu
 * items by plugins to be used by external code.
 * 
 * @author amc1
 */
public class MenuItemManager {

	private static MenuItemManager instance;

	private static AEMonitor class_mon = new AEMonitor("MenuManager");

	/*
	 * Holds all the MenuItem objects.
	 *    key = MENU_* type (see MenuManager in the plugin API)
	 *    value = Map: key = menu key value = MenuItem object
	 */
	private Map<String, Map<String, MenuItem>> items;

	private AEMonitor items_mon = new AEMonitor("MenuManager:items");
	
	private ArrayList<MenuItemManagerListener> listeners = new ArrayList<MenuItemManagerListener>(0);

	private MenuItemManager() {
		this.items = new HashMap<String, Map<String, MenuItem>>();
	}

	/**
	 * Return the static MenuManager instance
	 */
	public static MenuItemManager getInstance() {
		try {
			class_mon.enter();
			if (instance == null)
				instance = new MenuItemManager();
			return instance;
		} finally {
			class_mon.exit();
		}
	}

	public void addMenuItem(MenuItem item) {
		try {
			String name = item.getResourceKey();
			String sMenuID = item.getMenuID();
			try {
				this.items_mon.enter();
				Map<String, MenuItem> mTypes = this.items.get(sMenuID);
				if (mTypes == null) {
					// LinkedHashMap to preserve order
					mTypes = new LinkedHashMap<String, MenuItem>();
					this.items.put(sMenuID, mTypes);
				}
				mTypes.put(name, item);

			} finally {
				this.items_mon.exit();
			}
		} catch (Exception e) {
			System.out.println("Error while adding Menu Item");
			Debug.printStackTrace(e);
		}
	}
	
	public void removeAllMenuItems(String sMenuID) {
		this.items.remove(sMenuID);
	}
	
	public void removeMenuItem(MenuItem item) {
		Map<String, MenuItem> menu_item_map = this.items.get(item.getMenuID());
		if (menu_item_map != null) {menu_item_map.remove(item.getResourceKey());}
	}

	public MenuItem[] getAllAsArray(String sMenuID) {
		if (sMenuID != null) {
			triggerMenuItemQuery(sMenuID);
		}
		Map<String, MenuItem> local_menu_item_map = this.items.get(sMenuID);
		Map<String, MenuItem> global_menu_item_map = this.items.get(null);
		if (local_menu_item_map == null && global_menu_item_map == null) {
			return new MenuItem[0];
		}
		
		if (sMenuID == null) {local_menu_item_map = null;}
		
		ArrayList<MenuItem> l = new ArrayList<MenuItem>();
		if (local_menu_item_map != null) {l.addAll(local_menu_item_map.values());}
		if (global_menu_item_map != null) {l.addAll(global_menu_item_map.values());}
		return l.toArray(new MenuItem[l.size()]);
	}
	
	public MenuItem[] getAllAsArray(String[] menu_ids) {
		ArrayList<MenuItem> l  = new ArrayList<MenuItem>();
		for (int i=0; i<menu_ids.length; i++) {
			if (menu_ids[i] != null) {
				triggerMenuItemQuery(menu_ids[i]);
			}
			addMenuItems(menu_ids[i], l);
		}
		addMenuItems(null, l);
		return l.toArray(new MenuItem[l.size()]);
	}
	
	private void addMenuItems(String menu_id, ArrayList<MenuItem> l) {
		Map<String, MenuItem> menu_map = this.items.get(menu_id);
		if (menu_map != null) {l.addAll(menu_map.values());}
	}

	public void addListener(MenuItemManagerListener l) {
		synchronized (listeners) {
			if (!listeners.contains(l)) {
				listeners.add(l);
			}
		}
	}
	
	public void removeListener(MenuItemManagerListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}
	
	private void triggerMenuItemQuery(String id) {
		MenuItemManagerListener[] listenersArray;
		synchronized (listeners) {
			listenersArray = listeners.toArray(new MenuItemManagerListener[0]);
		}
		for (MenuItemManagerListener l : listenersArray) {
			try {
				l.queryForMenuItem(id);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void triggerMenuItemCleanup(String id) {
		MenuItemManagerListener[] listenersArray;
		synchronized (listeners) {
			listenersArray = listeners.toArray(new MenuItemManagerListener[0]);
		}
		for (MenuItemManagerListener l : listenersArray) {
			try {
				l.cleanupMenuItem(id);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}
}
