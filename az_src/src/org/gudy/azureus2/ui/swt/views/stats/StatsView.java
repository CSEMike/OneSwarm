/*
 * Created on Sep 13, 2004
 * Created by Olivier Chalouhi
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
 *
 */
package org.gudy.azureus2.ui.swt.views.stats;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.*;
import org.gudy.azureus2.ui.swt.views.IViewAlwaysInitialize;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * aka "Statistics View" that contains {@link ActivityView}, 
 * {@link TransferStatsView}, {@link CacheView}, {@link DHTView},
 * {@link VivaldiView}
 */
public class StatsView
	implements IViewAlwaysInitialize, UISWTViewCoreEventListener
{
	public static String VIEW_ID = UISWTInstance.VIEW_STATISTICS;
	
	public static final int EVENT_PERIODIC_UPDATE = 0x100;

	private CTabFolder folder;

	private ArrayList<UISWTViewCore> tabViews = new ArrayList<UISWTViewCore>();

	private int idxActivityTab = -1;

	private int idxDHTView = -1;

	private int idxTransfersView = -1;

	private UpdateThread updateThread;

	private Object dataSource;

	private UISWTViewCore activeView;

	private UISWTView swtView;

	private Composite parent;

	private static boolean registeredCoreSubViews;

	private class UpdateThread
		extends Thread
	{
		boolean bContinue;

		public UpdateThread() {
			super("StatsView Update Thread");
		}

		public void run() {
			bContinue = true;

			while (bContinue) {

				for (UISWTViewCore iview : tabViews) {
					try {
						iview.triggerEvent(EVENT_PERIODIC_UPDATE, null);
					} catch (Exception e) {
						Debug.printStackTrace(e);
					}
				}

				try {
					Thread.sleep(1000);
				} catch (Throwable e) {

					Debug.out(e);
					break;
				}
			}
		}

		public void stopIt() {
			bContinue = false;
		}
	}

	private void initialize(Composite composite) {
		parent = composite;
		folder = new CTabFolder(composite, SWT.LEFT);
		folder.setBorderVisible(true);

		Label lblClose = new Label(folder, SWT.WRAP);
		lblClose.setText("x");
		lblClose.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				delete();
			}
		});
		folder.setTopRight(lblClose);
		folder.setTabHeight(20);

    // Call plugin listeners
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();

			if (pluginUI != null && !registeredCoreSubViews) {
				pluginUI.addView(UISWTInstance.VIEW_STATISTICS,
						ActivityView.MSGID_PREFIX, ActivityView.class, null);

				pluginUI.addView(UISWTInstance.VIEW_STATISTICS,
						TransferStatsView.MSGID_PREFIX, TransferStatsView.class, null);

				pluginUI.addView(UISWTInstance.VIEW_STATISTICS, CacheView.MSGID_PREFIX,
						CacheView.class, null);

				pluginUI.addView(UISWTInstance.VIEW_STATISTICS, DHTView.MSGID_PREFIX,
						DHTView.class, DHTView.DHT_TYPE_MAIN);

				pluginUI.addView(UISWTInstance.VIEW_STATISTICS,
						VivaldiView.MSGID_PREFIX, VivaldiView.class,
						VivaldiView.DHT_TYPE_MAIN);

				if (NetworkAdmin.getSingleton().hasDHTIPV6()) {
					pluginUI.addView(UISWTInstance.VIEW_STATISTICS, DHTView.MSGID_PREFIX
							+ ".6", DHTView.class, DHTView.DHT_TYPE_MAIN_V6);
					pluginUI.addView(UISWTInstance.VIEW_STATISTICS,
							VivaldiView.MSGID_PREFIX + ".6", VivaldiView.class,
							VivaldiView.DHT_TYPE_MAIN_V6);
				}

				if (Constants.isCVSVersion()) {
					pluginUI.addView(UISWTInstance.VIEW_STATISTICS, DHTView.MSGID_PREFIX
							+ ".cvs", DHTView.class, DHTView.DHT_TYPE_CVS);
					pluginUI.addView(UISWTInstance.VIEW_STATISTICS,
							VivaldiView.MSGID_PREFIX + ".cvs", VivaldiView.class,
							VivaldiView.DHT_TYPE_CVS);
				}

				registeredCoreSubViews = true;
			}

			UISWTViewEventListenerHolder[] pluginViews = pluginUI == null
					? null : pluginUI.getViewListeners(UISWTInstance.VIEW_STATISTICS);
			for (int i = 0; i < pluginViews.length; i++) {
				UISWTViewEventListenerHolder l = pluginViews[i];
				String name = l.getViewID();
				if (name.equals(ActivityView.MSGID_PREFIX)) {
					idxActivityTab = i;
				} else if (name.equals(TransferStatsView.MSGID_PREFIX)) {
					idxTransfersView = i;
				} else if (idxDHTView == -1 && name.equals(DHTView.MSGID_PREFIX)) {
					idxTransfersView = i;
				}
				if (l != null) {
					try {
						UISWTViewImpl view = new UISWTViewImpl(
								UISWTInstance.VIEW_STATISTICS, name, l, null);
						addSection(view, null);
					} catch (Exception e) {
						// skip
					}
				}
			}
		}

		// Initialize view when user selects it
		folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				CTabItem item = (CTabItem) e.item;
				selectView(item);
			}
		});

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (folder == null || folder.isDisposed() || folder.getItemCount() == 0) {
					return;
				}
				selectView(folder.getItem(0));
			}
		});

		updateThread = new UpdateThread();
		updateThread.setDaemon(true);
		updateThread.start();

		dataSourceChanged(dataSource);
	}

	private void selectView(CTabItem item) {
		if (item == null) {
			return;
		}
		if (folder.getSelection() != item) {
			folder.setSelection(item);
		}
		folder.getShell().setCursor(
				folder.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		try {
			// Send one last refresh to previous tab, just in case it
			// wants to do something when view goes invisible
			refresh();

  		Object ds = item.getData("ds");
  		if (ds == null) {
  			ds = dataSource;
  		}

			UISWTViewCore view = (UISWTViewCore) item.getData("IView");
    	if (view == null) {
    		Class<?> cla = (Class<?>)item.getData("claEventListener");
    		UISWTViewEventListener l = (UISWTViewEventListener) cla.newInstance();
				view = new UISWTViewImpl(UISWTInstance.VIEW_MAIN, cla.getSimpleName(),
						l, ds);
    		item.setData("IView", view);
    	}
			activeView = view;

			if (item.getControl() == null) {
    		view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, ds);
				view.initialize(folder);
				item.setControl(view.getComposite());
			}

			item.getControl().setFocus();

			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions != null) {
				uiFunctions.refreshIconBar(); // For edit columns view
			}

			refresh();
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			folder.getShell().setCursor(null);
		}
	}

	// Copied from ManagerView
	private UISWTViewCore getActiveView() {
		return activeView;
	}

	// Copied from ManagerView
	private void refresh() {
		if (folder == null || folder.isDisposed())
			return;

		try {
			UISWTViewCore view = getActiveView();
			if (view != null) {
				view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
			}

			CTabItem[] items = folder.getItems();

			for (int i = 0; i < items.length; i++) {
				CTabItem item = items[i];
				view = (UISWTViewCore) item.getData("IView");
				try {
					if (item.isDisposed() || view == null) {
						continue;
					}
					String lastTitle = item.getText();
					String newTitle = view.getFullTitle();
					if (lastTitle == null || !lastTitle.equals(newTitle)) {
						item.setText(escapeAccelerators(newTitle));
					}
					String lastToolTip = item.getToolTipText();
					String newToolTip = view.getFullTitle();
					if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
						item.setToolTipText(newToolTip);
					}
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}
			}

		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
	}

	// Copied from ManagerView
	private static String escapeAccelerators(String str) {
		if (str == null) {

			return (str);
		}

		return (str.replaceAll("&", "&&"));
	}

	private String getFullTitle() {
		return MessageText.getString("Stats.title.full");
	}

	private void delete() {
		if (updateThread != null) {
			updateThread.stopIt();
		}

		if (folder != null && !folder.isDisposed()) {

			folder.setSelection(0);
		}

		//Don't ask me why, but without this an exception is thrown further
		// (in folder.dispose() )
		//TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
		if (Utils.isCarbon) {
			if (folder != null && !folder.isDisposed()) {
				CTabItem[] items = folder.getItems();
				for (int i = 0; i < items.length; i++) {
					if (!items[i].isDisposed())
						items[i].dispose();
				}
			}
		}

		for (int i = 0; i < tabViews.size(); i++) {
			UISWTViewCore view = tabViews.get(i);
			if (view != null) {
    		view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
			}
		}
		tabViews.clear();

		Utils.disposeSWTObjects(new Object[] {
			folder,
			parent
		});
	}

	private void dataSourceChanged(Object newDataSource) {
		dataSource = newDataSource;
		if (folder == null) {
			return;
		}
		if (newDataSource instanceof String) {
			int i = -1;

			if ("dht".equals(newDataSource)) {
				i = idxDHTView;
			} else if ("transfers".equals(newDataSource)) {
				i = idxTransfersView;
			} else {
				i = idxActivityTab;
			}

			if (i >= 0) {
				CTabItem item = folder.getItem(i);
				selectView(item);
			}

		}
	}

	private int addSection(String titleIdPrefix, Class<?> claEventListener) {
		return addSection(titleIdPrefix, claEventListener, null);
	}

	private int addSection(String titleIdPrefix, Class<?> claEventListener, Object ds) {
		CTabItem item = new CTabItem(folder, SWT.NULL);
		Messages.setLanguageText(item, titleIdPrefix + ".title.full");
		item.setData("claEventListener", claEventListener);
		if (ds != null) {
			item.setData("ds", ds);
		}
		return folder.indexOf(item);
	}

	private void addSection(UISWTViewCore view, Object dataSource) {
		if (view == null)
			return;

		view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, dataSource);

		CTabItem item = new CTabItem(folder, SWT.NULL);
		Messages.setLanguageText(item, view.getTitleID());
		item.setData("IView", view);
		tabViews.add(view);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener#eventOccurred(org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent)
	 */
	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				swtView.setTitle(getFullTitle());
				Messages.updateLanguageForControl(folder);
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				dataSourceChanged(event.getData());
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

}
