/*
 * Created on 4 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * @author Olivier Chalouhi
 */
public class MainMenu
	implements IMainMenu
{
	/**
	 * @deprecated
	 */
	public static int MENU_BAR = 0;

	/**
	 * @deprecated
	 */
	public static int MENU_TRANSFER = 1;

	/**
	 * @deprecated
	 */
	public static int MENU_VIEW = 2;

	private MainWindow mainWindow;

	private Menu menuBar;

	private AzureusCore core;

	/**
	 * <p>Creates the main menu bar and attaches it to the given shell</p>
	 * @param shell A shell
	 */
	public MainMenu(Shell shell) {
		mainWindow = MainWindow.getWindow();
		createMenus(shell);
	}

	public void setMainWindow(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void linkMenuBar(Shell parent) {
		parent.setMenuBar(menuBar);
	}

	private void createMenus(final Shell parent) {

		if (core == null) {
			core = AzureusCoreFactory.getSingleton();
		}

		//The Main Menu
		menuBar = new Menu(parent, SWT.BAR);
		parent.setMenuBar(menuBar);

		addFileMenu(parent);

		/* ======= View, Transfers, Torrent, Tools menus =====
		 * hig compliance for OSX dictates that more granular actions are positioned farther
		 * on the right of the menu and more general actions are closer to the left
		 * NOTE: we keep the original menu order (for non-OSX) in-tact so as to not disturb existing non-OSX users
		 */
		if (Constants.isOSX) {
			addViewMenu(parent);
			addTransferMenu(parent);
			addTorrentMenu(parent);
		} else {
			addTransferMenu(parent);
			addTorrentMenu(parent);
			addViewMenu(parent);

			/*
			 * The Tools menu is only present on non-OSX systems
			 */
			addToolsMenu(parent);
		}

		addPluginsMenu(parent);

		// ========== Windows menu (OSX only)================
		if (true == Constants.isOSX) {
			addWindowMenu(parent);
		}

		// =========== Debug menu (development only)=========
		if (Constants.isCVSVersion()) {
			DebugMenuHelper.createDebugMenuItem(menuBar);
		}

		addHelpMenu(parent);

		/*
		 * Enabled/disable menus based on what ui mode we're in
		 * NOTE: This is largely superfluous here since all menu items in the classic UI
		 * are available at all time; this method is left here for completeness should we
		 * add dynamic behavior to the menu in the future.  
		 */
		MenuFactory.updateEnabledStates(menuBar);

	}

	/**
	 * Creates the Window menu and all its children
	 * @param parent
	 */
	private void addWindowMenu(final Shell parent) {
		MenuItem menu_window = MenuFactory.createWindowMenuItem(menuBar);
		Menu windowMenu = menu_window.getMenu();

		MenuFactory.addMinimizeWindowMenuItem(windowMenu);
		MenuFactory.addZoomWindowMenuItem(windowMenu);

		MenuFactory.addSeparatorMenuItem(windowMenu);
		MenuFactory.addBlockedIPsMenuItem(windowMenu);

		MenuFactory.addSeparatorMenuItem(windowMenu);
		MenuFactory.addBringAllToFrontMenuItem(windowMenu);

		MenuFactory.addSeparatorMenuItem(windowMenu);
		MenuFactory.appendWindowMenuItems(windowMenu);
	}

	/**
	 * Creates the File menu and all its children
	 * @param parent
	 */
	private void addFileMenu(final Shell parent) {
		MenuItem fileItem = MenuFactory.createFileMenuItem(menuBar);
		Menu fileMenu = fileItem.getMenu();

		MenuFactory.addCreateMenuItem(fileMenu);

		MenuItem openMenuItem = MenuFactory.createOpenMenuItem(fileMenu);

		Menu openSubMenu = openMenuItem.getMenu();
		MenuFactory.addOpenTorrentMenuItem(openSubMenu);
		MenuFactory.addOpenTorrentForTrackingMenuItem(openSubMenu);

		MenuItem shareMenuItem = MenuFactory.createShareMenuItem(fileMenu);

		Menu shareSubMenu = shareMenuItem.getMenu();
		MenuFactory.addShareFileMenuItem(shareSubMenu);
		MenuFactory.addShareFolderMenuItem(shareSubMenu);
		MenuFactory.addShareFolderContentMenuItem(shareSubMenu);
		MenuFactory.addShareFolderContentRecursiveMenuItem(shareSubMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.addImportMenuItem(fileMenu);
		MenuFactory.addExportMenuItem(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.addCloseWindowMenuItem(fileMenu);
		MenuFactory.addCloseTabMenuItem(fileMenu, mainWindow);
		MenuFactory.addCloseDetailsMenuItem(fileMenu);
		MenuFactory.addCloseDownloadBarsToMenu(fileMenu);

		/*
		 * No need for restart and exit on OSX in the File menu since it is moved to the gobla application
		 * menu instead see org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer for detail about that menu
		 */
		if (false == Constants.isOSX) {
			MenuFactory.addSeparatorMenuItem(fileMenu);
			MenuFactory.addRestartMenuItem(fileMenu);
			MenuFactory.addExitMenuItem(fileMenu);
		}

	}

	/**
	 * Creates the Transfer menu and all its children
	 * @param parent
	 */
	private void addTransferMenu(final Shell parent) {
		MenuFactory.createTransfersMenuItem(menuBar);
	}

	/**
	 * Creates the View menu and all its children
	 * @param parent
	 */
	private void addViewMenu(final Shell parent) {
		try {
			MenuItem viewItem = MenuFactory.createViewMenuItem(menuBar);
			Menu viewMenu = viewItem.getMenu();

			MenuFactory.addLabelMenuItem(viewMenu, "MainWindow.menu.view.show");

			indent(MenuFactory.addMyTorrentsMenuItem(viewMenu));
			indent(MenuFactory.addMyTrackerMenuItem(viewMenu));
			indent(MenuFactory.addMySharesMenuItem(viewMenu));
			indent(MenuFactory.addViewToolbarMenuItem(viewMenu));
			indent(MenuFactory.addTransferBarToMenu(viewMenu));
			indent(MenuFactory.addAllPeersMenuItem(viewMenu));

			/*
			 * These 2 menus resides on the Tools menu on non-OSX platforms;
			 * since the Tools menu is not present in the OSX version these menus are added here to the View menu 
			 */
			if (Constants.isOSX) {
				indent(MenuFactory.addConsoleMenuItem(viewMenu));
				indent(MenuFactory.addStatisticsMenuItem(viewMenu));
			}

		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	/**
	 * Creates the Torrent menu and all its children
	 * @param parent
	 */
	private void addTorrentMenu(final Shell parent) {

		/*
		 * The Torrents menu is now a user-configured option
		 */
		if (true == COConfigurationManager.getBooleanParameter("show_torrents_menu")) {
			MenuFactory.createTorrentMenuItem(menuBar);
		}

	}

	/**
	 * Creates the Tools menu and all its children
	 * @param parent
	 */
	private void addToolsMenu(final Shell parent) {
		MenuItem item = MenuFactory.createToolsMenuItem(menuBar);
		Menu toolsMenu = item.getMenu();

		MenuFactory.addBlockedIPsMenuItem(toolsMenu);
		MenuFactory.addConsoleMenuItem(toolsMenu);
		MenuFactory.addStatisticsMenuItem(toolsMenu);
		MenuFactory.addNatTestMenuItem(toolsMenu);
		MenuFactory.addSpeedTestMenuItem(toolsMenu);

		MenuFactory.addSeparatorMenuItem(toolsMenu);
		MenuFactory.addConfigWizardMenuItem(toolsMenu);
		MenuFactory.addOptionsMenuItem(toolsMenu);
	}

	/**
	 * Creates the Plugins menu and all its children
	 * @param parent
	 */
	private void addPluginsMenu(final Shell parent) {
		/*
		 * NOTE: We are specifying that the Plugins menu be created WITHOUT the 'get plugins' menu item
		 * since that item is an addition only for the new Vuze menu
		 */
		MenuFactory.createPluginsMenuItem(menuBar, false);
	}

	/**
	 * Creates the Help menu and all its children
	 * @param parent
	 */
	private void addHelpMenu(final Shell parent) {
		MenuItem helpItem = MenuFactory.createHelpMenuItem(menuBar);

		Menu helpMenu = helpItem.getMenu();

		if (false == Constants.isOSX) {
			MenuFactory.addAboutMenuItem(helpMenu);
			MenuFactory.addSeparatorMenuItem(helpMenu);
		}

		MenuFactory.addHealthMenuItem(helpMenu);
		MenuFactory.addReleaseNotesMenuItem(helpMenu);
		MenuFactory.addWhatsNewMenuItem(helpMenu);
		
		MenuFactory.addFAQMenuItem(helpMenu, Constants.AZUREUS_WIKI);
		MenuFactory.addGetPluginsMenuItem(helpMenu);

		MenuFactory.addSeparatorMenuItem(helpMenu);

		if (false == SystemProperties.isJavaWebStartInstance()) {
			MenuFactory.addCheckUpdateMenuItem(helpMenu);
		}

		MenuFactory.addSeparatorMenuItem(helpMenu);
		MenuFactory.addDebugHelpMenuItem(helpMenu);

	}

	private static final void indent(MenuItem item) {
		item.setData("IndentItem", "YES");
		item.setText("  " + item.getText());
	}

	/**
	 * @deprecated This method has been replaced with {@link #getMenu(String)};
	 * use {@link #getMenu(IMenuConstants.MENU_ID_MENU_BAR)} ,
	 *     {@link #getMenu(IMenuConstants.MENU_ID_TRANSFERS)}, or
	 *     {@link #getMenu(IMenuConstants.MENU_ID_VIEW)} instead
	 * @param id
	 * @return
	 */
	public Menu getMenu(int id) {
		/*
		 * KN: Retrofitted to use the new getMenu(String) method
		 */
		if (id == MENU_BAR) {
			return getMenu(IMenuConstants.MENU_ID_MENU_BAR);
		}
		if (id == MENU_TRANSFER) {
			return getMenu(IMenuConstants.MENU_ID_TRANSFERS);
		}
		if (id == MENU_VIEW) {
			return getMenu(IMenuConstants.MENU_ID_VIEW);
		}

		return null;
	}

	public Menu getMenu(String id) {
		if (true == IMenuConstants.MENU_ID_MENU_BAR.equals(id)) {
			return menuBar;
		}
		return MenuFactory.findMenu(menuBar, id);
	}
}
