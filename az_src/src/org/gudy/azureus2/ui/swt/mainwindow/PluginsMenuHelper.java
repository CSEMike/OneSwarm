package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.BasicPluginViewImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class PluginsMenuHelper
{
	private static PluginsMenuHelper INSTANCE = null;

	private AEMonitor plugin_helper_mon = new AEMonitor("plugin_helper_mon");

	private Map plugin_view_info_map = new TreeMap();

	private Map plugin_logs_view_info_map = new TreeMap();

	private PluginsMenuHelper() {
		//Making this private
	}

	public static PluginsMenuHelper getInstance() {
		if (null == INSTANCE) {
			INSTANCE = new PluginsMenuHelper();
		}
		return INSTANCE;
	}

	public void buildPluginMenu(Menu pluginMenu, Shell parent,
			boolean includeGetPluginsMenu) {

		try {

			plugin_helper_mon.enter();
			createIViewInfoMenuItems(pluginMenu, plugin_view_info_map);

			MenuItem menu_plugin_logViews = MenuFactory.addLogsViewMenuItem(pluginMenu);
			createIViewInfoMenuItems(menu_plugin_logViews.getMenu(),
					plugin_logs_view_info_map);

		} finally {
			plugin_helper_mon.exit();
		}

		MenuFactory.addSeparatorMenuItem(pluginMenu);

		org.gudy.azureus2.plugins.ui.menus.MenuItem[] plugin_items;
		plugin_items = MenuItemManager.getInstance().getAllAsArray("mainmenu");
		if (plugin_items.length > 0) {
			MenuBuildUtils.addPluginMenuItems(parent, plugin_items, pluginMenu, true,
					true, MenuBuildUtils.BASIC_MENU_ITEM_CONTROLLER);
			MenuFactory.addSeparatorMenuItem(pluginMenu);
		}

		MenuFactory.addPluginInstallMenuItem(pluginMenu);
		MenuFactory.addPluginUnInstallMenuItem(pluginMenu);

		if (true == includeGetPluginsMenu) {
			MenuFactory.addGetPluginsMenuItem(pluginMenu);
		}
	}

	public void addPluginView(String sViewID, UISWTViewEventListener l) {
		IViewInfo view_info = new IViewInfo();
		view_info.viewID = sViewID;
		view_info.event_listener = l;

		String sResourceID = UISWTViewImpl.CFG_PREFIX + sViewID + ".title";
		boolean bResourceExists = MessageText.keyExists(sResourceID);

		String name;

		if (bResourceExists) {
			name = MessageText.getString(sResourceID);
		} else {
			// try plain resource
			sResourceID = sViewID;
			bResourceExists = MessageText.keyExists(sResourceID);

			if (bResourceExists) {
				name = MessageText.getString(sResourceID);
			} else {
				name = sViewID.replace('.', ' '); // support old plugins
			}
		}

		view_info.name = name;

		Map map_to_use = (l instanceof BasicPluginViewImpl)
				? this.plugin_logs_view_info_map : this.plugin_view_info_map;

		try {
			plugin_helper_mon.enter();
			map_to_use.put(name, view_info);
		} finally {
			plugin_helper_mon.exit();
		}
	}

	private void removePluginViewsWithID(String sViewID, Map map) {
		if (sViewID == null) {
			return;
		}
		Iterator itr = map.values().iterator();
		IViewInfo view_info = null;
		while (itr.hasNext()) {
			view_info = (IViewInfo) itr.next();
			if (sViewID.equals(view_info.viewID)) {
				itr.remove();
			}
		}
	}

	public void removePluginViews(final String sViewID) {
		try {
			plugin_helper_mon.enter();
			removePluginViewsWithID(sViewID, plugin_view_info_map);
			removePluginViewsWithID(sViewID, plugin_logs_view_info_map);
		} finally {
			plugin_helper_mon.exit();
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (uiFunctions != null) {
					uiFunctions.closePluginViews(sViewID);
				}
			}
		});
	}

	public void addPluginView(final AbstractIView view, final String name) {
		IViewInfo view_info = new IViewInfo();
		view_info.name = name;
		view_info.view = view;
		try {
			plugin_helper_mon.enter();
			plugin_view_info_map.put(name, view_info);
		} finally {
			plugin_helper_mon.exit();
		}
	}

	public void removePluginView(final AbstractIView view, final String name) {
		IViewInfo view_info = null;
		try {
			plugin_helper_mon.enter();
			view_info = (IViewInfo) this.plugin_view_info_map.remove(name);
		} finally {
			plugin_helper_mon.exit();
		}

		if (view_info != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
					if (uiFunctions != null) {
						uiFunctions.closePluginView(view);
					}
				}
			});
		}
	}

	/**
	 * Populates Azureus' menu bar
	 * @param locales
	 * @param parent
	 */

	private void createIViewInfoMenuItem(Menu parent, final IViewInfo info) {
		MenuItem item = new MenuItem(parent, SWT.NULL);
		item.setText(info.name);
		if (info.viewID != null) {
			item.setData("ViewID", info.viewID);
		}
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (uiFunctions != null) {
					info.openView(uiFunctions);
				}
			}
		});
	}

	private void createIViewInfoMenuItems(Menu parent, Map menu_data) {
		Iterator itr = menu_data.values().iterator();
		while (itr.hasNext()) {
			createIViewInfoMenuItem(parent, (IViewInfo) itr.next());
		}
	}

	private static class IViewInfo
	{
		public AbstractIView view;

		public String name;

		public String viewID;

		public UISWTViewEventListener event_listener;

		public void openView(UIFunctionsSWT uiFunctions) {
			if (event_listener != null) {
				uiFunctions.openPluginView(UISWTInstance.VIEW_MAIN, viewID,
						event_listener, null, true);
			} else {
				uiFunctions.openPluginView(view, name);
			}
		}

	}
}
