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
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.SelectableSpeedMenu;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatableAlways;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier Chalouhi
 *
 */
public class SystemTraySWT
	implements UIUpdatableAlways, MessageTextListener
{

	protected static AzureusCore core = null;

	Display display;

	UIFunctionsSWT uiFunctions;

	Tray tray;

	TrayItem trayItem;

	Menu menu;
	
	Image imgAzureus;
	Image imgAzureusGray;
	Image imgAzureusWhite;

	protected GlobalManager gm = null;

	private String seedingKeyVal;
	private String downloadingKeyVal;

	private String dlAbbrKeyVal;

	protected String ulAbbrKeyVal;
	
	long interval = 0;

	protected boolean enableTooltip;

	public SystemTraySWT() {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				SystemTraySWT.core = core;
				gm = core.getGlobalManager();
			}
		});
		
		COConfigurationManager.addAndFireParameterListener(
				"ui.systray.tooltip.enable", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						enableTooltip = COConfigurationManager.getBooleanParameter(parameterName);
						if (enableTooltip) {
							MessageText.addAndFireListener(SystemTraySWT.this);
						} else {
							MessageText.removeListener(SystemTraySWT.this);
							if (trayItem != null && !trayItem.isDisposed()) {
								trayItem.setToolTipText(null);
							}
						}
					}
				});

		uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		display = SWTThread.getInstance().getDisplay();

		tray = display.getSystemTray();
		trayItem = new TrayItem(tray, SWT.NULL);
		
		ImageLoader imageLoader = ImageLoader.getInstance();
		if (Constants.isOSX) {
			imgAzureusGray = imageLoader.getImage("azureus_grey");
			imgAzureusWhite = imageLoader.getImage("azureus_white");
			trayItem.setImage(imgAzureusGray);
		} else {
			imgAzureus = imageLoader.getImage("azureus");
			trayItem.setImage(imgAzureus);
		}

		trayItem.setVisible(true);

		menu = new Menu(uiFunctions.getMainShell(), SWT.POP_UP);
		menu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent _menu) {}

			public void menuHidden(MenuEvent _menu) {
				if(Constants.isOSX) {
					trayItem.setImage(imgAzureusGray);
				}
			}
		});
		
		MenuBuildUtils.addMaintenanceListenerForMenu(menu, new MenuBuildUtils.MenuBuilder() {
			public void buildMenu(Menu menu, MenuEvent menuEvent) {
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
					trayItem.setImage(imgAzureusWhite);
					menu.setVisible(true);
				}
			}
		});
		
		trayItem.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event arg0) {
				menu.setVisible(true);
			}
		});

		uiFunctions.getUIUpdater().addUpdater(this);
	}
	
	public void fillMenu(final Menu menu) {
		
		final MenuItem itemShow = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemShow, "SystemTray.menu.show");

		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem itemCloseAll = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemCloseAll,
				"SystemTray.menu.closealldownloadbars");
		
		final MenuItem itemShowGlobalTransferBar = new MenuItem(menu, SWT.CHECK);
		Messages.setLanguageText(itemShowGlobalTransferBar,
			"SystemTray.menu.open_global_transfer_bar");

		new MenuItem(menu, SWT.SEPARATOR);

		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;
		menu_items = MenuItemManager.getInstance().getAllAsArray("systray");
		if (menu_items.length > 0) {
			MenuBuildUtils.addPluginMenuItems(uiFunctions.getMainShell(), menu_items, menu, true, true, MenuBuildUtils.BASIC_MENU_ITEM_CONTROLLER);
			new MenuItem(menu, SWT.SEPARATOR);
		}
		
		createUploadLimitMenu(menu);
		createDownloadLimitMenu(menu);

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

		final MenuItem itemExit = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(itemExit, "SystemTray.menu.exit");

		itemShow.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				showMainWindow();
			}
		});

		itemStartAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if (gm == null) {
					return;
				}
				gm.startAllDownloads();
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
				if (gm == null) {
					return;
				}
				gm.resumeDownloads();
			}
		});

		itemPause.setEnabled(gm != null && gm.canPauseDownloads());
		itemResume.setEnabled(gm != null && gm.canResumeDownloads());

		itemCloseAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				uiFunctions.closeDownloadBars();
			}
		});
		
		itemShowGlobalTransferBar.setSelection(uiFunctions.isGlobalTransferBarShown());
		itemShowGlobalTransferBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if (uiFunctions.isGlobalTransferBarShown()) {
					uiFunctions.closeGlobalTransferBar();
				}
				else {
					uiFunctions.showGlobalTransferBar();
				}
			}
		});
		
		itemExit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				// User got a stack overflow (all SWT code) because of this dispose,
				// so execute it outside of the selection trigger and hope it doesn't
				// overflow there.
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						uiFunctions.dispose(false, false);
					}
				});
			}
		});
	}

	/**
	 * Creates the global upload limit context menu item
	 * @param parent The system tray contextual menu
	 */
	private final void createUploadLimitMenu(final Menu parent) {
		if (core == null) {
			return;
		}
		final MenuItem uploadSpeedItem = new MenuItem(parent, SWT.CASCADE);
		uploadSpeedItem.setText(MessageText.getString("GeneralView.label.maxuploadspeed"));

		final Menu uploadSpeedMenu = new Menu(uiFunctions.getMainShell(),
				SWT.DROP_DOWN);

		uploadSpeedMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				SelectableSpeedMenu.generateMenuItems(uploadSpeedMenu, core,
						gm, true);
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
				SelectableSpeedMenu.generateMenuItems(downloadSpeedMenu, core,
						gm, false);
			}
		});

		downloadSpeedItem.setMenu(downloadSpeedMenu);
	}

	public void dispose() {
		uiFunctions.getUIUpdater().removeUpdater(this);
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (trayItem != null && !trayItem.isDisposed()) {
					trayItem.dispose();
				}

				ImageLoader imageLoader = ImageLoader.getInstance();
				if (Constants.isOSX) {
					imageLoader.releaseImage("azureus_grey");
					imageLoader.releaseImage("azureus_white");
				} else {
					imageLoader.releaseImage("azureus");
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (interval++ % 10 > 0) {
			return;
		}
		if (trayItem.isDisposed()) {
			uiFunctions.getUIUpdater().removeUpdater(this);
			return;
		}
		if (core == null || !core.isStarted()) {
			return;
		}

		if (enableTooltip) {
  		GlobalManagerStats stats = gm.getStats();
  		List<?> managers = gm.getDownloadManagers();

  		StringBuffer toolTip = new StringBuffer();
  		int seeding = 0;
  		int downloading = 0;
  
  		// OMG this must be slow on 10k lists
  		for (int i = 0; i < managers.size(); i++) {
  			DownloadManager manager = (DownloadManager) managers.get(i);
  			int state = manager.getState();
  			if (state == DownloadManager.STATE_DOWNLOADING)
  				downloading++;
  			if (state == DownloadManager.STATE_SEEDING)
  				seeding++;
  		}
  
  		String seeding_text = seedingKeyVal.replaceAll("%1", "" + seeding);
  		String downloading_text = downloadingKeyVal.replaceAll("%1", "" + downloading);
  
  		toolTip.append(seeding_text).append(downloading_text).append("\n");
  		toolTip.append(dlAbbrKeyVal).append(
  				" ");
  
  		toolTip.append(DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
  				stats.getDataReceiveRate(), stats.getProtocolReceiveRate()));
  		
  		toolTip.append(", ").append(ulAbbrKeyVal).append(" ");
  		toolTip.append(DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
  				stats.getDataSendRate(), stats.getProtocolSendRate()));
  		
  		
  		trayItem.setToolTipText(toolTip.toString());
		}

		//Why should we refresh the image? it never changes ...
		//and is a memory bottleneck for some non-obvious reasons.
		//trayItem.setImage(ImageRepository.getImage("azureus"));   
		trayItem.setVisible(true);
	}

	private void showMainWindow() {
		uiFunctions.bringToFront(false);
	}

	public void updateLanguage() {
		if (menu != null) {
			Messages.updateLanguageForControl(menu);
		}

		updateUI();
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "SystemTraySWT";
	}

	public void localeChanged(Locale oldLocale, Locale newLocale) {
		seedingKeyVal = MessageText.getString("SystemTray.tooltip.seeding");
		downloadingKeyVal = MessageText.getString("SystemTray.tooltip.downloading");
		if (!downloadingKeyVal.startsWith(" ")) {
			downloadingKeyVal = " " + downloadingKeyVal;
		}

		dlAbbrKeyVal = MessageText.getString("ConfigView.download.abbreviated");
		ulAbbrKeyVal = MessageText.getString("ConfigView.upload.abbreviated");
	}
}
