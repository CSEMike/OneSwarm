/*
 * Created on 25 January 2007
 * Created by Allan Crooks
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;

/**
 * @author Allan Crooks
 *
 */
public class MenuManagerImpl implements MenuManager {
	
	private UIManagerImpl ui_manager;
	
	public MenuManagerImpl(UIManagerImpl _ui_manager) { ui_manager = _ui_manager; }
	
    public MenuItem addMenuItem(String menuID, String resource_key) {
    	PluginInterface pi = ui_manager.getPluginInterface();
    	MenuItemImpl item = new MenuItemImpl(pi, menuID, resource_key);
    	UIManagerImpl.fireEvent(pi, UIManagerEvent.ET_ADD_MENU_ITEM, item);
    	return item;
    }
    
    public MenuItem addMenuItem(MenuContext context, String resource_key) {
    	MenuContextImpl context_impl = (MenuContextImpl)context;
    	MenuItemImpl result = (MenuItemImpl)addMenuItem(context_impl.context, resource_key);
    	result.setContext(context_impl);
    	context_impl.dirty();
    	return result;
    }

    public MenuItem addMenuItem(MenuItem parent, String resource_key) {

		if (!(parent instanceof MenuItemImpl)) {
			throw new UIRuntimeException("parent must have been created by addMenuItem");
		}
		
		if (parent.getStyle() != MenuItemImpl.STYLE_MENU) {
			throw new UIRuntimeException("parent menu item must have the menu style associated");
		}
		
		MenuItemImpl item = new MenuItemImpl((MenuItemImpl)parent, resource_key);
		UIManagerImpl.fireEvent( ui_manager.getPluginInterface(), UIManagerEvent.ET_ADD_SUBMENU_ITEM, new Object[] {item, parent});
		return item;

    }
}
