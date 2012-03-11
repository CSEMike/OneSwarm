/*
 * Created on Jun 14, 2006 9:02:55 PM
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
package com.aelitis.azureus.ui;


import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

/**
 * @author TuxPaper
 * @created Jun 14, 2006
 *
 */
public interface UIFunctions
	extends AzureusCoreComponent
{
	public static int STATUSICON_NONE = 0;

	public static int STATUSICON_WARNING = 1;

	public static int STATUSICON_ERROR = 2;

	
	public static final int VIEW_CONSOLE = 0;
	public static final int VIEW_CONFIG = 4;
	public static final int VIEW_DM_DETAILS = 5;
	public static final int VIEW_DM_MULTI_OPTIONS = 6;
	public static final int VIEW_MYSHARES = 7;
	public static final int VIEW_MYTORRENTS = 8;
	public static final int VIEW_MYTRACKER = 9;
	public static final int VIEW_ALLPEERS = 10;
	public static final int VIEW_PEERS_STATS = 12;
	
	public static final int ACTION_FULL_UPDATE				= 1;	// arg: String - url; response Boolean - ok
	public static final int ACTION_UPDATE_RESTART_REQUEST	= 2;	// arg: Boolean - true->no auto-select response Boolean - ok
	

	/**
	 * Bring main window to the front
	 */
	void bringToFront();

	/**
	 * Bring main window to the front
	 * 
	 * @param noTricks Don't try any tricks to force it to the top
	 *
	 * @since 3.0.1.7
	 */
	void bringToFront(boolean noTricks);
	
	/**
	 * Change/Refresh the language of the UI
	 */
	void refreshLanguage();

	/**
	 * 
	 */
	void refreshIconBar();

	
	/**
	 * @param string
	 */
	void setStatusText(String string);

	void setStatusText(int statustype, String string, UIStatusTextClickListener l);

	/**
	 * Request the UI be shut down.
	 * 
	 * @return true - request granted, UI is being shut down
	 *         false - request denied (example: password entry failed)
	 */
	boolean dispose(boolean for_restart, boolean close_already_in_progress);

	boolean viewURL(String url, String target, int w, int h, boolean allowResize,
			boolean isModal);

	boolean viewURL(String url, String target, double wPct, double hPct,
			boolean allowResize, boolean isModal);

	void viewURL(String url, String target, String sourceRef);


	public UIFunctionsUserPrompter getUserPrompter(String title, String text,
			String[] buttons, int defaultOption);

	public void promptUser(String title, String text, String[] buttons,
			int defaultOption, String rememberID, String rememberText,
			boolean bRememberByDefault, int autoCloseInMS, UserPrompterResultListener l);
	
	/**
	 * Retrieves the class that handles periodically updating the UI
	 * 
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public UIUpdater getUIUpdater();

	/**
	 * @param viewID
	 * @param data
	 *
	 * @since 3.1.1.1
	 */
	void openView(int viewID, Object datasource);
	
	void doSearch(String searchText);
	
	public void
	installPlugin(
		String			plugin_id,
		String			resource_prefix,
		actionListener	listener );
	
	/**
	 * 
	 * @param action_id
	 * @param args
	 * @param listener
	 */
	public void
	performAction(
		int				action_id,
		Object			args,
		actionListener	listener );
	
	interface 
	actionListener
	{
		public void
		actionComplete(
			Object		result );
	}

	/**
	 * Retrieve the MDI (Sidebar, TabbedMDI)
	 * @return
	 */
	public MultipleDocumentInterface getMDI();		

	/**
	 * Might launch the old-school Mr Slidey
	 */
	void forceNotify(int iconID, String title, String text, String details,
			Object[] relatedObjects, int timeoutSecs);		
}
