/*
 * File    : SystemTraySWT.java
 * Created : 2 avr. 2004
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.systray;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.SelectableSpeedMenu;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class SystemTraySWT
{

	Display				display;

	UIFunctionsSWT uiFunctions;

	Tray					 tray;

	TrayItem			 trayItem;

	Menu					 menu;

	Image					trayIconNormal = null, trayIconNotify = null;

	public SystemTraySWT() {

		System.out.println("****** our systray");

		uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		display = SWTThread.getInstance().getDisplay();

		tray = display.getSystemTray();
		trayItem = new TrayItem(tray, SWT.NULL);

		if (!Constants.isOSX) {
			//			trayItem.setImage(ImageRepository.getImage("azureus"));
			trayIconNormal = ImageRepository.getImage("azureus");
		} else {
			//			trayItem.setImage(ImageRepository.getImage("azureus_grey"));
			trayIconNormal = ImageRepository.getImage("azureus_grey");
		}
		try {
			trayIconNotify = ImageRepository.getImage("trayicon_notify");
		} catch (Exception e) {
			e.printStackTrace();
		}

		trayItem.setImage(trayIconNormal);
		trayItem.setVisible(true);

		menu = new Menu(uiFunctions.getMainShell(), SWT.POP_UP);
		menu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent _menu) {
			}

			public void menuHidden(MenuEvent _menu) {
				if (Constants.isOSX) {
					trayItem.setImage(ImageRepository.getImage("azureus_grey"));
				}
			}
		});

		MenuBuildUtils.addMaintenanceListenerForMenu(menu,
				new MenuBuildUtils.MenuBuilder() {
					public void buildMenu(Menu menu) {
						fillMenu(menu);
					}
				});

		trayItem.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event arg0) {
				showMainWindow();
			}
		});

		trayItem.addListener(SWT.Selection, new Listener() {
			long lastTime = 0;

			public void handleEvent(Event arg0) {
				// Bug in Windows (seems to have started around SWT 3.3 Release
				// Candidates) where double click isn't interpreted as DefaultSelection
				// Since we "know" SWT.Selection is actually a mouse down, check
				// if two mouse downs happen in a short timespan and fake a
				// DefaultSelection
				if (Constants.isWindows) {
					long now = SystemTime.getCurrentTime();
					if (now - lastTime < 200) {
						showMainWindow();
					} else {
						lastTime = now;
					}
				} else if (Constants.isOSX) {
					trayItem.setImage(ImageRepository.getImage("azureus_white"));
					menu.setVisible(true);
				}
			}
		});

		trayItem.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event arg0) {
				menu.setVisible(true);
			}
		});

	}

	public void fillMenu(final Menu menu) {

		final MenuItem itemShow = new MenuItem(menu, SWT.NULL);
		//Messages.setLanguageText(itemShow, "SystemTray.menu.show");
		itemShow.setText("Show OneSwarm...");

		new MenuItem(menu, SWT.SEPARATOR);

		//		final MenuItem itemCloseAll = new MenuItem(menu, SWT.NULL);
		//		Messages.setLanguageText(itemCloseAll,
		//				"SystemTray.menu.closealldownloadbars");
		//
		//		final MenuItem itemShowGlobalTransferBar = new MenuItem(menu, SWT.CHECK);
		//		Messages.setLanguageText(itemShowGlobalTransferBar,
		//			"SystemTray.menu.open_global_transfer_bar");
		//
		//		new MenuItem(menu, SWT.SEPARATOR);

		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;
		menu_items = MenuItemManager.getInstance().getAllAsArray("systray");
		if (menu_items.length > 0) {
			MenuBuildUtils.addPluginMenuItems(uiFunctions.getMainShell(), menu_items,
					menu, true, true, MenuBuildUtils.BASIC_MENU_ITEM_CONTROLLER);
			new MenuItem(menu, SWT.SEPARATOR);
		}

		createUploadLimitMenu(menu);
		createDownloadLimitMenu(menu);

		/*
		 * if we are running on windows:
		 */
		if (Constants.isWindows) {
			new MenuItem(menu, SWT.SEPARATOR);
			final MenuItem startWithWindowsCheckbox = new MenuItem(menu, SWT.CHECK);
			startWithWindowsCheckbox.setText("Start with Windows");
			startWithWindowsCheckbox.setSelection(COConfigurationManager.getBooleanParameter("autostart"));
			startWithWindowsCheckbox.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent arg0) {
					if (COConfigurationManager.getBooleanParameter("autostart")) {
						COConfigurationManager.setParameter("autostart", false);
					} else {
						COConfigurationManager.setParameter("autostart", true);
					}
				}

				public void widgetDefaultSelected(SelectionEvent arg0) {
				}
			});
		}

		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem itemStartAll = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemStartAll, "SystemTray.menu.startalltransfers");

		final MenuItem itemStopAll = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemStopAll, "SystemTray.menu.stopalltransfers");

		final MenuItem itemPause = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemPause, "SystemTray.menu.pausetransfers");

		final MenuItem itemResume = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemResume, "SystemTray.menu.resumetransfers");

		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem itemOldUI = new MenuItem(menu, SWT.NULL);
		itemOldUI.setText("Show Classic UI");
		itemOldUI.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				MainWindow.forceShowForOldUI();
			}
		});

		itemShow.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				showMainWindow();
			}
		});

		itemStartAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				AzureusCoreFactory.getSingleton().getGlobalManager().startAllDownloads();
			}
		});

		itemStopAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				ManagerUtils.asyncStopAll();
			}
		});

		itemPause.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				ManagerUtils.asyncPause();
			}
		});

		itemResume.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				AzureusCoreFactory.getSingleton().getGlobalManager().resumeDownloads();
			}
		});

		if (Constants.isOSX == false) {
			new MenuItem(menu, SWT.SEPARATOR);

			final MenuItem itemExit = new MenuItem(menu, SWT.NULL);
			Messages.setLanguageText(itemExit, "SystemTray.menu.exit");

			itemExit.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event arg0) {
					uiFunctions.requestShutdown();
				}
			});
		}

		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		itemPause.setEnabled(gm.canPauseDownloads());
		itemResume.setEnabled(gm.canResumeDownloads());

		//		itemCloseAll.addListener(SWT.Selection, new Listener() {
		//			public void handleEvent(Event arg0) {
		//				uiFunctions.closeDownloadBars();
		//			}
		//		});
		//
		//		itemShowGlobalTransferBar.setSelection(uiFunctions.isGlobalTransferBarShown());
		//		itemShowGlobalTransferBar.addListener(SWT.Selection, new Listener() {
		//			public void handleEvent(Event arg0) {
		//				if (uiFunctions.isGlobalTransferBarShown()) {
		//					uiFunctions.closeGlobalTransferBar();
		//				}
		//				else {
		//					uiFunctions.showGlobalTransferBar();
		//				}
		//			}
		//		});
	}

	/**
	 * Creates the global upload limit context menu item
	 * @param parent The system tray contextual menu
	 */
	private final void createUploadLimitMenu(final Menu parent) {
		final MenuItem uploadSpeedItem = new MenuItem(parent, SWT.CASCADE);
		uploadSpeedItem.setText(MessageText.getString("GeneralView.label.maxuploadspeed"));

		final Menu uploadSpeedMenu = new Menu(uiFunctions.getMainShell(),
				SWT.DROP_DOWN);

		uploadSpeedMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				AzureusCore core = AzureusCoreFactory.getSingleton();
				SelectableSpeedMenu.generateMenuItems(uploadSpeedMenu, core,
						core.getGlobalManager(), true);
			}
		});

		uploadSpeedItem.setMenu(uploadSpeedMenu);
	}

	/**
	 * Creates the global download limit context menu item
	 * @param parent The system tray contextual menu
	 */
	private final void createDownloadLimitMenu(final Menu parent) {
		final MenuItem downloadSpeedItem = new MenuItem(parent, SWT.CASCADE);
		downloadSpeedItem.setText(MessageText.getString("GeneralView.label.maxdownloadspeed"));

		final Menu downloadSpeedMenu = new Menu(uiFunctions.getMainShell(),
				SWT.DROP_DOWN);

		downloadSpeedMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				AzureusCore core = AzureusCoreFactory.getSingleton();
				SelectableSpeedMenu.generateMenuItems(downloadSpeedMenu, core,
						core.getGlobalManager(), false);
			}
		});

		downloadSpeedItem.setMenu(downloadSpeedMenu);
	}

	public void dispose() {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (trayItem != null && !trayItem.isDisposed()) {
					trayItem.dispose();
				}
			}
		});
	}

	ClassLoader mF2FClassLoader = null;

	public void update() {
		if (trayItem.isDisposed())
			return;
		List managers = AzureusCoreFactory.getSingleton().getGlobalManager().getDownloadManagers();
		//StringBuffer toolTip = new StringBuffer("Azureus - ");//$NON-NLS-1$
		StringBuffer toolTip = new StringBuffer();
		int seeding = 0;
		int downloading = 0;

		for (int i = 0; i < managers.size(); i++) {
			DownloadManager manager = (DownloadManager) managers.get(i);
			int state = manager.getState();
			if (state == DownloadManager.STATE_DOWNLOADING)
				downloading++;
			if (state == DownloadManager.STATE_SEEDING)
				seeding++;
		}

		/**
		 * If we've loaded the F2F plugin, get the number of unread messages and add this to the tooltip
		 */
		if (mF2FClassLoader == null) {
			PluginInterface f2fInterface = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"osf2f");
			if (f2fInterface != null) {
				if (f2fInterface.isOperational()) {
					mF2FClassLoader = f2fInterface.getPluginClassLoader();
				} else {
					;
				}
			}
		}

		if (mF2FClassLoader != null) {
			try {
				Class chatClass = Class.forName("edu.washington.cs.oneswarm.f2f.chat.ChatDAO");
				Object chatInstance = chatClass.getMethod("get").invoke(null);
				Map<String, Integer> unread = (Map<String, Integer>) chatClass.getMethod(
						"getUnreadMessageCounts").invoke(chatInstance);
				int count = 0;
				for (Integer v : unread.values()) {
					count += v;
				}

				if (count > 0) {
					toolTip.append(MessageText.getString(
							"SystemTray.tooltip.chatmessages").replaceAll("%1", "" + count)
							+ "\n");
					if (trayItem.getImage() != trayIconNotify && trayIconNotify != null) {
						trayItem.setImage(trayIconNotify);
					}
				} else if (trayItem.getImage() != trayIconNormal) {
					trayItem.setImage(trayIconNormal);
				}

			} catch (Exception e) {
				e.printStackTrace();
				mF2FClassLoader = null;
			}
		}

		// something went funny here across Java versions, leading " " got lost

		String seeding_text = MessageText.getString("SystemTray.tooltip.seeding").replaceAll(
				"%1", "" + seeding);
		String downloading_text = MessageText.getString(
				"SystemTray.tooltip.downloading").replaceAll("%1", "" + downloading);

		/*	if ( !seeding_text.startsWith(" " )){
		 seeding_text = " " + seeding_text;
		 }*/
		if (!downloading_text.startsWith(" ")) {
			downloading_text = " " + downloading_text;
		}

		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		GlobalManagerStats stats = gm.getStats();

		toolTip.append(seeding_text).append(downloading_text).append("\n");
		toolTip.append(MessageText.getString("ConfigView.download.abbreviated")).append(
				" ");

		toolTip.append(DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
				stats.getDataReceiveRate(), stats.getProtocolReceiveRate()));

		toolTip.append(", ").append(
				MessageText.getString("ConfigView.upload.abbreviated")).append(" ");
		toolTip.append(DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
				stats.getDataSendRate(), stats.getProtocolSendRate()));

		trayItem.setToolTipText(toolTip.toString());

		//Why should we refresh the image? it never changes ...
		//and is a memory bottleneck for some non-obvious reasons.
		//trayItem.setImage(ImageRepository.getImage("azureus"));
		trayItem.setVisible(true);
	}

	private void showMainWindow() {
		Utils.launch(Constants.ONESWARM_ENTRY_URL);
	}

	public void updateLanguage() {
		if (menu != null) {
			Messages.updateLanguageForControl(menu);
		}

		update();
	}

}
