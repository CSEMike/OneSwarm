/*
 * Created on 05-Sep-2005
 * Created by Paul Gardner
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

package org.gudy.azureus2.ui.swt.plugins;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * Tools to manage a SWT Instance
 * 
 * @see org.gudy.azureus2.plugins.ui.UIManagerListener
 * @see org.gudy.azureus2.plugins.ui.UIManager#addUIListener(UIManagerListener)
 */
public interface UISWTInstance extends UIInstance {
	/** ID of main view */
	public static final String VIEW_MAIN = "Main";

	/** 
	 * ID of "My Torrents" view
	 * 
	 * @since 2.3.0.7
	 */
	public static final String VIEW_MYTORRENTS = "MyTorrents";

	/** 
	 * ID of "Peers" view
	 * 
	 * @since 2.3.0.7
	 */
	public static final String VIEW_TORRENT_PEERS = TableManager.TABLE_TORRENT_PEERS;

	/**
	 * ID of "Pieces" view
	 * 
	 * @since 2.3.0.7
	 */
	public static final String VIEW_TORRENT_PIECES = TableManager.TABLE_TORRENT_PIECES;

	/**
	 * ID of "Files" view
	 * 
	 * @since 2.3.0.7
	 */
	public static final String VIEW_TORRENT_FILES = TableManager.TABLE_TORRENT_FILES;
	
	/**
	 * ID of the top bar of az3ui
	 * 
	 * @since 3.0.1.3
	 */
	public static final String VIEW_TOPBAR = "TopBar";

	/** Retrieve the SWT Display object that Azureus uses (when in SWT mode).
	 * If you have a thread that does some periodic/asynchronous stuff, Azureus 
	 * will crashes with and 'InvalidThreadAccess' exception unless you
	 * embed your calls in a Runnable, and use getDisplay().aSyncExec(Runnable r);
	 *
	 * @return SWT Display object that Azureus uses
	 *
	 * @since 2.3.0.5
	 */
	public Display getDisplay();

	public Image
	loadImage(
		String	resource );
	
	/** Creates an UISWTGraphic object with the supplied SWT Image
	 *
	 * @param img Image to assign to the object
	 * @return a new UISWTGraphic object
	 *
	 * @since 2.3.0.5
	 */
	public UISWTGraphic createGraphic(Image img);

	/**
	 * Add a detail view to an Azureus parent view.  For views added to the Main
	 * window, this adds a menu option.  For the other parent views, this adds
	 * a new tab within Azureus' own detail view.
	 * 
	 * @param sParentID VIEW_* constant
	 * @param sViewID of your view.  Used as part of the resource id.<br>
	 *          "Views.plugins." + ID + ".title" = title of your view
	 * @param l Listener to be triggered when parent view wants to tell you 
	 *           an event has happened
	 *           
	 * @note If you want the window to auto-open, use openMainView when you gain
	 *        access to the UISWTInstance
	 *
	 * @since 2.3.0.6
	 */
	public void addView(String sParentID, String sViewID, UISWTViewEventListener l);

	/**
	 * Open a previously added view
	 * 
	 * @param sParentID ParentID of the view to be shown
	 * @param sViewID id of the view to be shown
	 * @param dataSource any data you need to pass the view
	 * @return success level
	 * 
	 * @since 2.5.0.1
	 */
	public boolean openView(String sParentID, String sViewID, Object dataSource);
	
	/** 
	 * Create and open a view in the main window immediately.  If you are calling
	 * this from {@link org.gudy.azureus2.plugins.ui.UIManagerListener#UIAttached(UIInstance)},
	 * the view will not gain focus.
	 * <p>
	 * Tip: You can add a menu item to a table view, and when triggered, have
	 *      it open a new window, passing the datasources that were selected
	 * 
	 * @param sViewID ID to give your view
	 * @param l Listener to be triggered when View Events occur
	 * @param dataSource objects to set {@link UISWTView#getDataSource()} with
	 *
	 * @since 2.3.0.6
	 */
	public void openMainView(String sViewID, UISWTViewEventListener l,
			Object dataSource);

	/**
	 * Remove all views that belong to a specific parent and of a specific View 
	 * ID.  If the parent is the main window, the menu item will be removed.<br>
	 * If you wish to remove (close) just one view, use 
	 * {@link UISWTView#closeView()}
	 * 
	 * @param sParentID One of VIEW_* constants
	 * @param sViewID View ID to remove
	 *
	 * @since 2.3.0.6
	 */
	public void removeViews(String sParentID, String sViewID);

	/**
	 * Get a list of views currently open on the specified VIEW_* view
	 * 
	 * @param sParentID VIEW_* constant
	 * @return list of views currently open
	 * 
	 * @since 2.3.0.6
	 */
	public UISWTView[] getOpenViews(String sParentID);

	/**
	 * A Plugin might call this method to add a View to Azureus's views
	 * The View will be accessible from View > Plugins > View name
	 * @param view The PluginView to be added
	 * @param autoOpen Whether the plugin should auto-open at startup
	 *
	 * @since 2.3.0.5
	 * @deprecated Use {@link #addView(String, String, UISWTViewEventListener)}
	 */

	public void addView(UISWTPluginView view, boolean autoOpen);

	/**
	 * Remove a view
	 * @param view
	 * 
	 * @since 2.3.0.5
	 * @deprecated Use {@link #removeViews(String, String)}
	 */
	public void removeView(UISWTPluginView view);

	/**
	 * Add an AWT panel as the plugin view
	 * @param view
	 * @param auto_open
	 * 
	 * @since 2.3.0.5
	 * @deprecated Use {@link #addView(String, String, UISWTViewEventListener)}
	 */
	public void addView(UISWTAWTPluginView view, boolean auto_open);

	/**
	 * Remove a view
	 * @param view
	 * 
	 * @since 2.3.0.5
	 * @deprecated Use {@link #removeViews(String, String)}
	 */
	public void removeView(UISWTAWTPluginView view);
	
	/**
	 * Shows or hides a download bar for a given download.
	 * 
	 * @since 3.0.0.5
	 * @param download Download to use.
	 * @param display <tt>true</tt> to show a download bar, <tt>false</tt> to hide it.
	 */
	public void showDownloadBar(Download download, boolean display);
	
	/**
	 * Shows or hides the transfers bar.
	 * 
	 * @since 3.0.1.3
	 * @param display <tt>true</tt> to show the bar, <tt>false</tt> to hide it.
	 */
	public void showTransfersBar(boolean display);
	
	/**
	 * Creates an entry in the status bar to display custom status information.
	 * 
	 * @since 3.0.0.7
	 * @see UISWTStatusEntry
	 */
	public UISWTStatusEntry createStatusEntry();
}
