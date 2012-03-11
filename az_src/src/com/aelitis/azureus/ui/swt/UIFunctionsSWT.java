/*
 * Created on Jul 12, 2006 3:11:00 PM
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
package com.aelitis.azureus.ui.swt;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;
import org.gudy.azureus2.ui.swt.mainwindow.IMainMenu;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;

/**
 * @author TuxPaper
 * @created Jul 12, 2006
 *
 */
public interface UIFunctionsSWT
	extends UIFunctions
{
	public Shell getMainShell();

	/**
	 * @param viewID
	 * @param l
	 */
	void addPluginView(String viewID, UISWTViewEventListener l);

	/**
	 * 
	 */
	public void closeDownloadBars();

	public boolean isGlobalTransferBarShown();

	public void showGlobalTransferBar();

	public void closeGlobalTransferBar();

	/**
	 * @return
	 */
	public UISWTInstanceImpl getSWTPluginInstanceImpl();

	/**
	 * @return
	 */
	public UISWTView[] getPluginViews();

	/**
	 * 
	 * @param sParentID
	 * @param sViewID
	 * @param l
	 * @param dataSource
	 * @param bSetFocus
	 */
	public void openPluginView(String sParentID, String sViewID,
			UISWTViewEventListener l, Object dataSource, boolean bSetFocus);

	public void openPluginView(final UISWTViewCore view, final String name);

	/**
	 * @param viewID
	 */
	public void removePluginView(String viewID);

	/**
	 * @param impl
	 */
	public void closePluginView(UISWTViewCore view);

	public void closePluginViews(String sViewID);

	public UISWTInstance getUISWTInstance();

	public void refreshTorrentMenu();

	public MainStatusBar getMainStatusBar();

	/**
	 * Creates the main application menu and attach it to the given <code>Shell</code>;
	 * this is only used for OSX so that we can attach the global menu to popup dialogs which
	 * is the expected behavior on OSX.  Windows and Linux do not require this since they do not have
	 * a global menu and because their main menu is already attached to the main application window.
	 * @param shell
	 * @return
	 */
	public IMainMenu createMainMenu(Shell shell);

	public IMainWindow getMainWindow();

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public void closeAllDetails();

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public boolean hasDetailViews();
	
	public Shell showCoreWaitDlg();
	
	public MultipleDocumentInterfaceSWT getMDISWT();

	public void promptForSearch();

	public UIToolBarManager getToolBarManager();
}
