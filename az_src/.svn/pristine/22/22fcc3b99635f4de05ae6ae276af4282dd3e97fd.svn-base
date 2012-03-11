/*
 * Created on 25-Jan-2007
 * Created by Allan Crooks
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuItemImpl;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;

/**
 * A class which helps generate build SWT menus.
 * 
 * It provides two main functions. The first provides the ability to create
 * regenerateable menus (it will dispose old items when not displayed and
 * invoke a callback method to regenerate it).
 * 
 * The second provides the ability to create SWT menus based on the plugin
 * API for menu creation - this allows internal code that generates menus
 * to include plugins to append to their own internal menus. 
 * 
 * @author Allan Crooks
 */
public class MenuBuildUtils {

	/**
	 * An interface to be used for addMaintenanceListenerForMenu. 
	 */
	public static interface MenuBuilder {
		public void buildMenu(Menu root_menu, MenuEvent menuEvent);
	}

	/**
	 * Creates and adds a listener object to implement regeneratable menus.
	 * 
	 * The first piece of functionality it offers is the ability to call a
	 * callback method to generate the menu when it is about to be displayed
	 * (the callback method is done by passing an object implementing the
	 * MenuBuilder interface).
	 * 
	 * This means that the caller of this method only needs to provide the
	 * logic to construct the menu's contents. This is helpful if you want
	 * to update an existing menu.
	 * 
	 * The second piece of functionality is that it automatically does what
	 * is required to dispose of existing menu items when the menu is hidden.
	 */
	public static void addMaintenanceListenerForMenu(final Menu menu,
			final MenuBuilder builder) {

		// Was taken from TableView.java
		menu.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX)
					return;

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed())
							return;
						org.eclipse.swt.widgets.MenuItem[] items = menu
								.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
					}
				});
			};

			public void menuShown(MenuEvent e) {
				org.eclipse.swt.widgets.MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++)
					items[i].dispose();

				bShown = true;
				builder.buildMenu(menu, e);
			}
		});
	}

	/**
	 * This is an interface used by addPluginMenuItems.
	 */
	public static interface PluginMenuController {
		
		/**
		 * This method should create a listener object which should be invoked
		 * when the given menu item is selected.
		 */
		public Listener makeSelectionListener(MenuItem plugin_menu_item);
		
		/**
		 * This method will be invoked just before the given menu item is
		 * displayed in a menu.
		 */
		public void notifyFillListeners(MenuItem menu_item);
	}

	/**
	 * This is an implementation of the PluginMenuController interface for use with
	 * MenuItemImpl classes - note that this is not intended for use by subclasses of
	 * MenuItemImpl (like TableContextMenuItemImpl).
	 * 
	 * The object passed at construction time is the object to be passed when selection
	 * listeners and fill listeners are notified.
	 */
	public static class MenuItemPluginMenuControllerImpl implements
			PluginMenuController {

		private Object[] objects;

		public MenuItemPluginMenuControllerImpl(Object[] o) {
			this.objects = o;
		}

		public Listener makeSelectionListener(MenuItem menu_item) {
			final MenuItemImpl mii = (MenuItemImpl) menu_item;
			return new Listener() {
				public void handleEvent(Event e) {
					mii.invokeListenersMulti(objects);
				}
			};
		}

		public void notifyFillListeners(MenuItem menu_item) {
			((MenuItemImpl) menu_item).invokeMenuWillBeShownListeners(objects);
		}

	}

	/**
	 * An instance of MenuItemPluginMenuControllerImpl with a default value of
	 * null - this will be the value passed when notifying selection and fill
	 * listeners.
	 */
	public static final PluginMenuController BASIC_MENU_ITEM_CONTROLLER = new MenuItemPluginMenuControllerImpl(null);

	/**
	 * Creates menu items inside the given menu based on the plugin API MenuItem
	 * instances provided. This method is provided mainly as a utility method to
	 * make it easier for menus to incorporate menu components specified by
	 * plugins.
	 * 
	 * Usually, the list of array items will be extracted from a class like
	 * MenuItemManager or TableContextMenuManager, where plugins will usually
	 * register menu items they have created.
	 * 
	 * @param composite Some composite to get a shell from.
	 * @param items The list of plugin MenuItem to add
	 * @param parent The SWT Menu to add to.
	 * @param prev_was_separator Indicates if the previous item in the menu is
	 *            a separator or not
	 * @param enable_items Indicates whether you want generated items to be
	 *            enabled or not. If false, all items will be disabled. If true,
	 *            then the items *may* be enabled, depending on how each MenuItem
	 *            is configured.  
	 * @param controller The callback object used by this method when creating the
	 *            SWT menus (used for invoking fill and selection listeners).
	 */
	public static void addPluginMenuItems(Composite composite,
			MenuItem[] items, Menu parent,	boolean prev_was_separator,
			boolean enable_items, PluginMenuController controller) {
		
		for (int i = 0; i < items.length; i++) {
			final MenuItemImpl az_menuitem = (MenuItemImpl) items[i];
			
			controller.notifyFillListeners(az_menuitem);
			if (!az_menuitem.isVisible()) {continue;}
			
			final int style = az_menuitem.getStyle();
			final int swt_style;

			boolean this_is_separator = false;

			// Do we have any children? If so, we override any manually defined
			// style.
			boolean is_container = false;
			

			if (style == TableContextMenuItem.STYLE_MENU) {
				swt_style = SWT.CASCADE;
				is_container = true;
			} else if (style == TableContextMenuItem.STYLE_PUSH) {
				swt_style = SWT.PUSH;
			} else if (style == TableContextMenuItem.STYLE_CHECK) {
				swt_style = SWT.CHECK;
			} else if (style == TableContextMenuItem.STYLE_RADIO) {
				swt_style = SWT.RADIO;
			} else if (style == TableContextMenuItem.STYLE_SEPARATOR) {
				this_is_separator = true;
				swt_style = SWT.SEPARATOR;
			} else {
				swt_style = SWT.PUSH;
			}

			final org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(
					parent, swt_style);

			if (swt_style == SWT.SEPARATOR) {continue;}
			
			if (prev_was_separator && this_is_separator) {continue;} // Skip contiguous separators
			if (this_is_separator && i == items.length - 1) {continue;} // Skip trailing separator

			prev_was_separator = this_is_separator;

			if (enable_items) {

				if (style == TableContextMenuItem.STYLE_CHECK
						|| style == TableContextMenuItem.STYLE_RADIO) {

					Boolean selection_value = (Boolean) az_menuitem.getData();
					if (selection_value == null) {
						throw new RuntimeException(
								"MenuItem with resource name \""
										+ az_menuitem.getResourceKey()
										+ "\" needs to have a boolean value entered via setData before being used!");
					}
					menuItem.setSelection(selection_value.booleanValue());
				}
			}
			
			final Listener main_listener = controller.makeSelectionListener(az_menuitem);
			menuItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					if (az_menuitem.getStyle() == MenuItem.STYLE_CHECK
							|| az_menuitem.getStyle() == MenuItem.STYLE_RADIO) {
						if (!menuItem.isDisposed()) {
							az_menuitem.setData(new Boolean(menuItem.getSelection()));
						}
					}
					main_listener.handleEvent(e);
				}
			});
			
			if (is_container) {
				Menu this_menu = new Menu(composite.getShell(), SWT.DROP_DOWN);
				menuItem.setMenu(this_menu);
				addPluginMenuItems(composite, az_menuitem.getItems(), this_menu, false,
						enable_items, controller);
			}
			
			String custom_title = az_menuitem.getText();
			menuItem.setText(custom_title);

			Graphic g = az_menuitem.getGraphic();
			if (g instanceof UISWTGraphic) {
				Utils.setMenuItemImage(menuItem, ((UISWTGraphic) g).getImage());
			}

			menuItem.setEnabled(enable_items && az_menuitem.isEnabled());

		}
	}

}
