/*
 * Created on Jul 12, 2006 2:56:52 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.SimpleBrowserWindow;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.UIStatusTextClickListener;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author TuxPaper
 * @created Jul 12, 2006
 *
 */
public class UIFunctionsImpl
	implements UIFunctionsSWT
{

	private final MainWindow mainwindow;

	/**
	 * @param window
	 */
	public UIFunctionsImpl(MainWindow mainwindow) {
		this.mainwindow = mainwindow;
	}

	// UIFunctions
	public void bringToFront() {
		bringToFront(true);
	}

	public void bringToFront(final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				//mainwindow.setVisible(true, tryTricks);
				
				// keep this in sync with Initializer.java
				Utils.launch("http://127.0.0.1:" + "29615/");
			}
		});
	}

	// UIFunctions
	public void addPluginView(final PluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				PluginsMenuHelper.getInstance().addPluginView(view,
						view.getPluginViewName());
			}
		});
	}

	public void openPluginView(final PluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(view, view.getPluginViewName());
			}
		});
	}

	public void removePluginView(final PluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				PluginsMenuHelper.getInstance().removePluginView(view,
						view.getPluginViewName());
			}
		});
	}

	public void showStats() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showStats();
			}
		});
	}

	public void showStatsDHT() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showStatsDHT();
			}
		});
	}

	public void showStatsTransfers() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showStatsTransfers();
			}
		});
	}

	public Shell getMainShell() {
		return mainwindow.getShell();
	}

	public void addPluginView(final UISWTPluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				PluginsMenuHelper.getInstance().addPluginView(view,
						view.getPluginViewName());
			}
		});
	}

	public void openPluginView(final UISWTPluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(view, view.getPluginViewName());
			}
		});
	}

	public void removePluginView(final UISWTPluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				PluginsMenuHelper.getInstance().removePluginView(view,
						view.getPluginViewName());
			}
		});
	}

	public boolean showConfig(String string) {
		return mainwindow.showConfig(string);
	}

	public void addPluginView(final String viewID, final UISWTViewEventListener l) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				PluginsMenuHelper.getInstance().addPluginView(viewID, l);
			}
		});
	}

	public boolean requestShutdown() {
		return mainwindow.destroyRequest();
	}

	public void refreshLanguage() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.setSelectedLanguageItem();
			}
		});
	}

	public void closeDownloadBars() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MiniBarManager.getManager().closeAll();
			}
		});
	}

	public boolean isGlobalTransferBarShown() {
		return AllTransfersBar.getManager().isOpen(mainwindow.getGlobalManager());
	}

	public void showGlobalTransferBar() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				AllTransfersBar.open(mainwindow.getGlobalManager(),
						mainwindow.getShell());
			}
		});
	}

	public void closeGlobalTransferBar() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				AllTransfersBar.close(mainwindow.getGlobalManager());
			}
		});
	}

	public UISWTInstanceImpl getSWTPluginInstanceImpl() {
		return mainwindow.getUISWTInstanceImpl();
	}

	public void openManagerView(final DownloadManager dm) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openManagerView(dm);
			}
		});
	}

	public void refreshIconBar() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.refreshIconBar();
			}
		});
	}

	public void removeManagerView(final DownloadManager dm) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.removeManagerView(dm);
			}
		});
	}

	public void showMyTracker() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showMyTracker();
			}
		});
	}

	public void closePluginView(final IView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.closePluginView(view);
			}
		});
	}

	public UISWTView[] getPluginViews() {
		return mainwindow.getPluginViews();
	}

	public void openPluginView(final String sParentID, final String sViewID,
			final UISWTViewEventListener l, final Object dataSource,
			final boolean bSetFocus) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(sParentID, sViewID, l, dataSource, bSetFocus);
			}
		});
	}

	public void removePluginView(final String viewID) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				PluginsMenuHelper.getInstance().removePluginViews(viewID);
			}
		});
	}

	public void setStatusText(final String string) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.setStatusText(string);
			}
		});
	}

	public void setStatusText(final int statustype, final String string,
			final UIStatusTextClickListener l) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.setStatusText(statustype, string, l);
			}
		});
	}

	public boolean dispose(boolean for_restart, boolean close_already_in_progress) {
		return mainwindow.dispose(for_restart, close_already_in_progress);
	}

	public Menu getMenu(int id) {
		if (mainwindow.getMenu() != null) {
			return mainwindow.getMenu().getMenu(id);
		}
		return null;
	}

	public void closePluginViews(final String sViewID) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.closePluginViews(sViewID);
			}
		});
	}

	public void openPluginView(final AbstractIView view, final String name) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(view, name);
			}
		});
	}

	public void showMyShares() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showMyShares();
			}
		});
	}

	public void showMyTorrents() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showMyTorrents();
			}
		});
	}

	public void showAllPeersView() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showAllPeersView();
			}
		});
	}

	public void showMultiOptionsView(final DownloadManager[] dms) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showMultiOptionsView(dms);
			}
		});
	}

	public void showConsole() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showConsole();
			}
		});
	}

	public UISWTInstance getUISWTInstance() {
		return mainwindow.getUISWTInstanceImpl();
	}

	// @see com.aelitis.azureus.ui.UIFunctions#viewURL(java.lang.String, java.lang.String, int, int, boolean, boolean)
	public boolean viewURL(final String url, final String target, final int w,
			final int h, final boolean allowResize, final boolean isModal) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				SimpleBrowserWindow window = new SimpleBrowserWindow(
						mainwindow.getShell(), url, w, h, allowResize, isModal);
				window.waitUntilClosed();
			}
		});
		return true;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#viewURL(java.lang.String, java.lang.String, double, double, boolean, boolean)
	public boolean viewURL(final String url, final String target, final double w,
			final double h, final boolean allowResize, final boolean isModal) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				SimpleBrowserWindow window = new SimpleBrowserWindow(
						mainwindow.getShell(), url, w, h, allowResize, isModal);
				window.waitUntilClosed();
			}
		});
		return true;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#promptUser(java.lang.String, java.lang.String, java.lang.String[], int, java.lang.String, java.lang.String, boolean, int)
	public int promptUser(String title, String text, String[] buttons,
			int defaultOption, String rememberID, String rememberText,
			boolean rememberByDefault, int autoCloseInMS) {
		return MessageBoxShell.open(getMainShell(), title, text, buttons,
				defaultOption, rememberID, rememberText, rememberByDefault,
				autoCloseInMS);
	}

	// @see com.aelitis.azureus.ui.UIFunctions#getUserPrompter(java.lang.String, java.lang.String, java.lang.String[], int)
	public UIFunctionsUserPrompter getUserPrompter(String title, String text,
			String[] buttons, int defaultOption) {

		MessageBoxShell mb = new MessageBoxShell(getMainShell(), title, text,
				buttons, defaultOption);
		return mb;
	}

	public void refreshTorrentMenu() {
		mainwindow.refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getMainStatusBar()
	public MainStatusBar getMainStatusBar() {
		return mainwindow.getMainStatusBar();
	}

	public IMainMenu createMainMenu(Shell shell) {
		return new MainMenu(shell);
	}

	public IMainWindow getMainWindow() {
		return mainwindow;
	}

}
