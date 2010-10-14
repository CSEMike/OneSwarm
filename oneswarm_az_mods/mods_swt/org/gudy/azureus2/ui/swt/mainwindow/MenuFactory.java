package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.importtorrent.wizard.ImportTorrentWizard;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.pluginsuninstaller.UnInstallPluginWizard;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class MenuFactory
	implements IMenuConstants
{

	public static boolean isAZ3_ADV = COConfigurationManager.getBooleanParameter("v3.Start Advanced");

	private static boolean isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));

	public static MenuItem createFileMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_FILE);
	}

	public static MenuItem createTransfersMenuItem(Menu menuParent) {
		MenuItem transferMenuItem = createTopLevelMenuItem(menuParent,
				MENU_ID_TRANSFERS);

		Menu transferMenu = transferMenuItem.getMenu();

		MenuFactory.addStartAllMenuItem(transferMenu);
		MenuFactory.addStopAllMenuItem(transferMenu);

		final MenuItem itemPause = MenuFactory.addPauseMenuItem(transferMenu);
		final MenuItem itemResume = MenuFactory.addResumeMenuItem(transferMenu);
		//		if (notMainWindow) {
		//			MenuFactory.performOneTimeDisable(itemPause, true);
		//			MenuFactory.performOneTimeDisable(itemResume, true);
		//		}

		transferMenu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent menu) {
				itemPause.setEnabled(getCore().getGlobalManager().canPauseDownloads());
				itemResume.setEnabled(getCore().getGlobalManager().canResumeDownloads());
			}

			public void menuHidden(MenuEvent menu) {
			}
		});

		return transferMenuItem;
	}

	public static MenuItem createViewMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_VIEW);
	}

	public static MenuItem createAdvancedMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_ADVANCED);
	}

	public static MenuItem createTorrentMenuItem(final Menu menuParent) {
		final MenuItem torrentItem = createTopLevelMenuItem(menuParent,
				MENU_ID_TORRENT);

		/*
		 * The Torrents menu is context-sensitive to which torrent is selected in the UI.
		 * For this reason we need to dynamically build the menu when ever it is about to be displayed
		 * so that the states of the menu items accurately reflect what was selected in the UI. 
		 */
		MenuBuildUtils.addMaintenanceListenerForMenu(torrentItem.getMenu(),
				new MenuBuildUtils.MenuBuilder() {
					public void buildMenu(Menu menu) {
						DownloadManager[] current_dls = (DownloadManager[]) torrentItem.getData("downloads");
						if (current_dls == null) {
							return;
						}
						boolean is_detailed_view = ((Boolean) torrentItem.getData("is_detailed_view")).booleanValue();
						TableViewSWT tv = (TableViewSWT) torrentItem.getData("TableView");

						TorrentUtil.fillTorrentMenu(menu, current_dls, getCore(),
								menuParent.getShell(), !is_detailed_view, 0, tv);

						/*
						 * KN: This is a reference to a plugins class from a core class;
						 * maybe MenuItem should be moved to core?
						 */
						org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;

						menu_items = MenuItemManager.getInstance().getAllAsArray(
								new String[] {
									"torrentmenu",
									"download_context"
								});
						if (menu_items.length > 0) {

							addSeparatorMenuItem(menu);

							Object[] plugin_dls = DownloadManagerImpl.getDownloadStatic(current_dls);

							MenuBuildUtils.addPluginMenuItems(menuParent.getShell(),
									menu_items, menu, true, true,
									new MenuBuildUtils.MenuItemPluginMenuControllerImpl(
											plugin_dls));
						}
					}
				});
		return torrentItem;
	}

	public static MenuItem createToolsMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_TOOLS);
	}

	/**
	 * Creates the Plugins menu item and all it's children
	 * @param menuParent
	 * @param includeGetPluginsMenu if <code>true</code> then also include a menu item for getting new plugins
	 * @return
	 */
	public static MenuItem createPluginsMenuItem(final Menu menuParent,
			final boolean includeGetPluginsMenu) {

		MenuItem pluginsMenuItem = createTopLevelMenuItem(menuParent,
				MENU_ID_PLUGINS);
		MenuBuildUtils.addMaintenanceListenerForMenu(pluginsMenuItem.getMenu(),
				new MenuBuildUtils.MenuBuilder() {
					public void buildMenu(Menu menu) {
						PluginsMenuHelper.getInstance().buildPluginMenu(menu,
								menuParent.getShell(), includeGetPluginsMenu);
					}
				});

		return pluginsMenuItem;
	}

	public static MenuItem createWindowMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_WINDOW);
	}

	public static MenuItem createHelpMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_HELP);
	}

	public static MenuItem addCreateMenuItem(Menu menuParent) {
		MenuItem file_create = addMenuItem(menuParent, MENU_ID_CREATE,
				new Listener() {
					public void handleEvent(Event e) {
						new NewTorrentWizard(getCore(), getDisplay());
					}
				});
		return file_create;
	}

	public static MenuItem createOpenMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_OPEN);
	}

	public static MenuItem addLogsViewMenuItem(Menu menuParent) {
		return createTopLevelMenuItem(menuParent, MENU_ID_LOG_VIEWS);
	}

	public static MenuItem addOpenTorrentMenuItem(Menu menuParent) {
		return addMenuItem(menuParent, MENU_ID_OPEN_TORRENT, new Listener() {
			public void handleEvent(Event e) {
				TorrentOpener.openTorrentWindow();
			}
		});
	}

	public static MenuItem addOpenTorrentForTrackingMenuItem(Menu menuParent) {
		MenuItem file_new_torrent_for_tracking = addMenuItem(menuParent,
				MENU_ID_OPEN_TORRENT_FOR_TRACKING, new Listener() {
					public void handleEvent(Event e) {
						TorrentOpener.openTorrentTrackingOnly();
					}
				});
		return file_new_torrent_for_tracking;
	}

	public static MenuItem createShareMenuItem(Menu menuParent) {
		MenuItem file_share = createTopLevelMenuItem(menuParent, MENU_ID_SHARE);
		return file_share;
	}

	public static MenuItem addShareFileMenuItem(final Menu menuParent) {

		MenuItem file_share_file = addMenuItem(menuParent, MENU_ID_SHARE_FILE,
				new Listener() {
					public void handleEvent(Event e) {
						ShareUtils.shareFile(getCore(), menuParent.getShell());
					}
				});
		return file_share_file;
	}

	public static MenuItem addShareFolderMenuItem(final Menu menuParent) {
		MenuItem file_share_dir = addMenuItem(menuParent, MENU_ID_SHARE_DIR,
				new Listener() {
					public void handleEvent(Event e) {
						ShareUtils.shareDir(getCore(), menuParent.getShell());
					}
				});
		return file_share_dir;
	}

	public static MenuItem addShareFolderContentMenuItem(final Menu menuParent) {
		MenuItem file_share_dircontents = addMenuItem(menuParent,
				MENU_ID_SHARE_DIR_CONTENT, new Listener() {
					public void handleEvent(Event e) {
						ShareUtils.shareDirContents(getCore(), menuParent.getShell(), false);
					}
				});
		return file_share_dircontents;
	}

	public static MenuItem addShareFolderContentRecursiveMenuItem(
			final Menu menuParent) {
		MenuItem file_share_dircontents_rec = addMenuItem(menuParent,
				MENU_ID_SHARE_DIR_CONTENT_RECURSE, new Listener() {
					public void handleEvent(Event e) {
						ShareUtils.shareDirContents(getCore(), menuParent.getShell(), true);
					}
				});
		return file_share_dircontents_rec;
	}

	public static MenuItem addImportMenuItem(Menu menuParent) {
		MenuItem file_import = addMenuItem(menuParent, MENU_ID_IMPORT,
				new Listener() {
					public void handleEvent(Event e) {
						new ImportTorrentWizard(getCore(), getDisplay());
					}
				});
		return file_import;
	}

	public static MenuItem addExportMenuItem(Menu menuParent) {
		MenuItem file_export = addMenuItem(menuParent, MENU_ID_EXPORT,
				new Listener() {
					public void handleEvent(Event e) {
						new ExportTorrentWizard(getCore(), getDisplay());
					}
				});
		return file_export;
	}

	public static MenuItem addCloseWindowMenuItem(final Menu menuParent) {

		MenuItem closeWindow = addMenuItem(menuParent, MENU_ID_WINDOW_CLOSE,
				new Listener() {
					public void handleEvent(Event event) {
						Shell shell = menuParent.getShell();
						if (shell != null && !shell.isDisposed()) {
							menuParent.getShell().close();
						}
					}
				});
		return closeWindow;
	}

	public static MenuItem addCloseTabMenuItem(Menu menu,
			final MainWindow mainWindow) {
		return addMenuItem(menu, MENU_ID_CLOSE_TAB, new Listener() {
			public void handleEvent(Event event) {
				if (MainWindow.isAlreadyDead) {
					return;
				}
				mainWindow.closeViewOrWindow();
			}
		});
	}

	public static MenuItem addCloseDetailsMenuItem(Menu menu) {
		final MenuItem item = addMenuItem(menu, MENU_ID_CLOSE_ALL_DETAIL,
				new Listener() {
					public void handleEvent(Event e) {
						Tab.closeAllDetails();
					}
				});

		Listener enableHandler = new Listener() {
			public void handleEvent(Event event) {
				if (true == MenuFactory.isEnabledForCurrentMode(item)) {
					if (false == item.isDisposed() && false == event.widget.isDisposed()) {
						item.setEnabled(Tab.hasDetails());
					}
				}
			}
		};

		menu.addListener(SWT.Show, enableHandler);
		//		shell.addListener(SWT.FocusIn, enableHandler);
		Tab.addTabAddedListener(enableHandler);
		Tab.addTabRemovedListener(enableHandler);

		return item;
	}

	public static MenuItem addCloseDownloadBarsToMenu(Menu menu) {
		final MenuItem item = addMenuItem(menu, MENU_ID_CLOSE_ALL_DL_BARS,
				new Listener() {
					public void handleEvent(Event e) {
						MiniBarManager.getManager().closeAll();
					}
				});

		Listener enableHandler = new Listener() {
			public void handleEvent(Event event) {
				item.setEnabled(false == MiniBarManager.getManager().getShellManager().isEmpty());
			}
		};
		menu.addListener(SWT.Show, enableHandler);
		//		shell.addListener(SWT.FocusIn, enableHandler);
		return item;
	}

	public static MenuItem addRestartMenuItem(Menu menuParent) {
		MenuItem file_restart = new MenuItem(menuParent, SWT.NULL);
		Messages.setLanguageText(file_restart, MENU_ID_RESTART); //$NON-NLS-1$

		file_restart.addListener(SWT.Selection, new Listener() {

			public void handleEvent(Event event) {
				UIFunctionsManagerSWT.getUIFunctionsSWT().dispose(true, false);
			}
		});
		return file_restart;
	}

	public static MenuItem addExitMenuItem(Menu menuParent) {
		final MenuItem file_exit = new MenuItem(menuParent, SWT.NULL);
		if (!COConfigurationManager.getBooleanParameter("Enable System Tray")
				|| !COConfigurationManager.getBooleanParameter("Close To Tray")) {
			KeyBindings.setAccelerator(file_exit, MENU_ID_EXIT);
		}
		Messages.setLanguageText(file_exit, MENU_ID_EXIT); //$NON-NLS-1$

		file_exit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				UIFunctionsManagerSWT.getUIFunctionsSWT().dispose(false, false);
			}
		});

		// let platform decide
		ParameterListener paramListener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				if (COConfigurationManager.getBooleanParameter("Enable System Tray")
						&& COConfigurationManager.getBooleanParameter("Close To Tray")) {
					KeyBindings.removeAccelerator(file_exit, MENU_ID_EXIT);
				} else {
					KeyBindings.setAccelerator(file_exit, MENU_ID_EXIT);
				}
			}
		};
		COConfigurationManager.addParameterListener("Enable System Tray",
				paramListener);
		COConfigurationManager.addParameterListener("Close To Tray", paramListener);
		return file_exit;
	}

	public static MenuItem addStartAllMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_START_ALL_TRANSFERS, new Listener() {
			public void handleEvent(Event event) {
				getCore().getGlobalManager().startAllDownloads();
				/*
				 * KN: Not sure why we can not use the call below as opposed to the line above
				 *  which was the exiting code
				 */
				// ManagerUtils.asyncStartAll();
			}
		});
	}

	public static MenuItem addStopAllMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_STOP_ALL_TRANSFERS, new Listener() {
			public void handleEvent(Event event) {
				ManagerUtils.asyncStopAll();
			}
		});
	}

	public static MenuItem addPauseMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_PAUSE_TRANSFERS, new Listener() {
			public void handleEvent(Event event) {
				ManagerUtils.asyncPause();
			}
		});
	}

	public static MenuItem addResumeMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_RESUME_TRANSFERS, new Listener() {
			public void handleEvent(Event event) {
				ManagerUtils.asyncResume();
			}
		});
	}

	public static MenuItem addMyTorrentsMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_MY_TORRENTS, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showMyTorrents();
				}
			}
		});
	}

	public static MenuItem addAllPeersMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_ALL_PEERS, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showAllPeersView();
				}
			}
		});
	}

	public static MenuItem addMyTrackerMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_MY_TRACKERS, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showMyTracker();
				}
			}
		});
	}

	public static MenuItem addMySharesMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_MY_SHARES, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showMyShares();
				}
			}
		});
	}

	public static MenuItem addViewToolbarMenuItem(Menu menu) {
		final MenuItem item = addMenuItem(menu, SWT.CHECK, MENU_ID_TOOLBAR,
				new Listener() {
					public void handleEvent(Event e) {
						UIFunctionsSWT uiFunctions = getUIFunctionSWT();
						if (null != uiFunctions) {
							IMainWindow mainWindow = uiFunctions.getMainWindow();
							boolean isToolbarVisible = mainWindow.isVisible(IMainWindow.WINDOW_ELEMENT_TOOLBAR);
							mainWindow.setVisible(IMainWindow.WINDOW_ELEMENT_TOOLBAR,
									!isToolbarVisible);
						}
					}
				});

		final ParameterListener listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				item.setSelection(COConfigurationManager.getBooleanParameter(parameterName));
			}
		};

		COConfigurationManager.addAndFireParameterListener("IconBar.enabled",
				listener);
		item.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				COConfigurationManager.removeParameterListener("IconBar.enabled",
						listener);
			}
		});
		return item;
	}

	public static MenuItem addTransferBarToMenu(final Menu menu) {
		final MenuItem item = addMenuItem(menu, SWT.CHECK, MENU_ID_TRANSFER_BAR,
				new Listener() {
					public void handleEvent(Event e) {
						if (AllTransfersBar.getManager().isOpen(
								getCore().getGlobalManager())) {
							AllTransfersBar.close(getCore().getGlobalManager());
						} else {
							AllTransfersBar.open(getCore().getGlobalManager(),
									menu.getShell());
						}
					}
				});

		menu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				item.setSelection(!MiniBarManager.getManager().getShellManager().isEmpty());
			}
		});
		return item;
	}

	public static MenuItem addBlockedIPsMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_IP_FILTER, new Listener() {
			public void handleEvent(Event event) {
				//				if (MainWindow.isAlreadyDead) {
				//					return;
				//				}
				BlockedIpsWindow.showBlockedIps(getCore(),
						getUIFunctionSWT().getMainShell());
			}
		});
	}

	public static MenuItem addConsoleMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_CONSOLE, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showConsole();
				}
			}
		});
	}

	public static MenuItem addStatisticsMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_STATS, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showStats();
				}
			}
		});
	}

	public static MenuItem addNatTestMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_NAT_TEST, new Listener() {
			public void handleEvent(Event e) {
				new NatTestWindow();
			}
		});
	}

	public static MenuItem addSpeedTestMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_SPEED_TEST, new Listener() {
			public void handleEvent(Event e) {
				new SpeedTestWizard(getCore(), getDisplay());
			}
		});
	}

	public static MenuItem addConfigWizardMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_CONFIGURE, new Listener() {
			public void handleEvent(Event e) {
				new ConfigureWizard(getCore(), false);
			}
		});
	}

	public static MenuItem addOptionsMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_OPTIONS, new Listener() {
			public void handleEvent(Event e) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.showConfig(null);
				}

				/* ==== Code for opening the Preferences in a pop-up dialog ===
				Shell shell = ShellFactory.createShell(SWT.RESIZE | SWT.DIALOG_TRIM);
				shell.setLayout(new GridLayout());
				shell.setText(MessageText.getString(MessageText.resolveLocalizationKey("ConfigView.title.full")));
				Utils.setShellIcon(shell);
				ConfigView cView = new ConfigView(getCore());
				cView.initialize(shell);
				shell.pack();
				UIFunctionsSWT uiFunctions = getUIFunctionSWT();
				if (uiFunctions != null) {
					Utils.centerWindowRelativeTo(shell, uiFunctions.getMainShell());
				}
				shell.open();
				*/
			}
		});
	}

	public static MenuItem addMinimizeWindowMenuItem(Menu menu) {
		final Shell shell = menu.getShell();

		final MenuItem item = addMenuItem(menu, MENU_ID_WINDOW_MINIMIZE,
				new Listener() {
					public void handleEvent(Event event) {
						if (shell.isDisposed()) {
							event.doit = false;
							return;
						}
						shell.setMinimized(true);
					}
				});

		Listener enableHandler = new Listener() {
			public void handleEvent(Event event) {
				if (null != shell && false == shell.isDisposed()) {
					if (((shell.getStyle() & SWT.MIN) != 0)) {
						item.setEnabled(false == shell.getMinimized());
					} else {
						item.setEnabled(false);
					}
				}
			}
		};

		menu.addListener(SWT.Show, enableHandler);
		shell.addListener(SWT.FocusIn, enableHandler);
		shell.addListener(SWT.Iconify, enableHandler);
		shell.addListener(SWT.Deiconify, enableHandler);

		return item;
	}

	public static MenuItem addBringAllToFrontMenuItem(Menu menu) {
		final MenuItem item = addMenuItem(menu, MENU_ID_WINDOW_ALL_TO_FRONT,
				new Listener() {
					public void handleEvent(Event event) {
						Iterator iter = ShellManager.sharedManager().getWindows();
						while (iter.hasNext()) {
							Shell shell = (Shell) iter.next();
							if (!shell.isDisposed() && !shell.getMinimized())
								shell.open();
						}
					}
				});

		final Listener enableHandler = new Listener() {
			public void handleEvent(Event event) {
				Iterator iter = ShellManager.sharedManager().getWindows();
				boolean hasNonMaximizedShell = false;
				while (iter.hasNext()) {
					Shell shell = (Shell) iter.next();
					if (false == shell.isDisposed() && false == shell.getMinimized()) {
						hasNonMaximizedShell = true;
						break;
					}
				}
				item.setEnabled(hasNonMaximizedShell);
			}
		};

		menu.addListener(SWT.Show, enableHandler);
		menu.getShell().addListener(SWT.FocusIn, enableHandler);

		ShellManager.sharedManager().addWindowAddedListener(enableHandler);
		ShellManager.sharedManager().addWindowRemovedListener(enableHandler);
		item.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				ShellManager.sharedManager().removeWindowAddedListener(enableHandler);
				ShellManager.sharedManager().removeWindowRemovedListener(enableHandler);
			}
		});

		return item;
	}

	/**
	 * Appends the list of opened interactive windows to the bottom of the specified shell menu
	 * @param menuParent The shell menu
	 * @param shell
	 */
	public static void appendWindowMenuItems(final Menu menuParent) {
		final Shell shell = menuParent.getShell();
		final int numTopItems = menuParent.getItemCount();
		Listener rebuild = new Listener() {
			public void handleEvent(Event event) {
				try {
					if (menuParent.isDisposed() || shell.isDisposed())
						return;

					final int size = ShellManager.sharedManager().getSize();
					if (size == menuParent.getItemCount() - numTopItems) {
						for (int i = numTopItems; i < menuParent.getItemCount(); i++) {
							final MenuItem item = menuParent.getItem(i);
							item.setSelection(item.getData() == shell);
						}
						return;
					}

					for (int i = numTopItems; i < menuParent.getItemCount();)
						menuParent.getItem(i).dispose();

					Iterator iter = ShellManager.sharedManager().getWindows();
					for (int i = 0; i < size; i++) {
						final Shell sh = (Shell) iter.next();

						if (sh.isDisposed() || sh.getText().length() == 0)
							continue;

						final MenuItem item = new MenuItem(menuParent, SWT.CHECK);

						item.setText(sh.getText());
						item.setSelection(shell == sh);
						item.setData(sh);

						item.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent event) {
								if (event.widget.isDisposed() || sh.isDisposed())
									return;

								if (sh.getMinimized())
									sh.setMinimized(false);

								sh.open();
							}
						});
					}
				} catch (Exception e) {
					Logger.log(new LogEvent(LogIDs.GUI, "rebuild menu error", e));
				}
			}
		};

		ShellManager.sharedManager().addWindowAddedListener(rebuild);
		ShellManager.sharedManager().addWindowRemovedListener(rebuild);
		shell.addListener(SWT.FocusIn, rebuild);
		menuParent.addListener(SWT.Show, rebuild);
	}

	public static MenuItem addZoomWindowMenuItem(Menu menuParent) {
		final Shell shell = menuParent.getShell();
		final MenuItem item = addMenuItem(menuParent, MENU_ID_WINDOW_ZOOM,
				new Listener() {
					public void handleEvent(Event event) {
						if (shell.isDisposed()) {
							event.doit = false;
							return;
						}
						shell.setMaximized(!shell.getMaximized());
					}
				});

		Listener enableHandler = new Listener() {
			public void handleEvent(Event event) {
				if (null != shell && false == shell.isDisposed()) {
					if (false == Constants.isOSX) {
						if (true == shell.getMaximized()) {
							Messages.setLanguageText(item,
									MessageText.resolveLocalizationKey(MENU_ID_WINDOW_ZOOM_RESTORE));
						} else {
							Messages.setLanguageText(item,
									MessageText.resolveLocalizationKey(MENU_ID_WINDOW_ZOOM_MAXIMIZE));
						}
					}

					if (((shell.getStyle() & SWT.MAX) != 0)) {
						item.setEnabled(false == shell.getMinimized());
					} else {
						item.setEnabled(false);
					}
				}
			}
		};

		menuParent.addListener(SWT.Show, enableHandler);
		shell.addListener(SWT.FocusIn, enableHandler);
		shell.addListener(SWT.Iconify, enableHandler);
		shell.addListener(SWT.Deiconify, enableHandler);

		return item;
	}

	public static MenuItem addAboutMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_ABOUT, new Listener() {
			public void handleEvent(Event e) {
				AboutWindow.show(getDisplay());
			}
		});
	}

	public static MenuItem addHealthMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_HEALTH, new Listener() {
			public void handleEvent(Event e) {
				HealthHelpWindow.show(getDisplay());
			}
		});
	}

	public static MenuItem addWhatsNewMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_WHATS_NEW, new Listener() {
			public void handleEvent(Event e) {
				Utils.launch("http://azureus.sourceforge.net/changelog.php?version="
						+ Constants.AZUREUS_VERSION);
			}
		});
	}

	/**
	 * Add the FAQ menu item to the given menu
	 * @param menu
	 * @param faq_url the fully qualified url to the appropriate FAQ (Vuze uses a different url than Classic)
	 * @return
	 */
	public static MenuItem addFAQMenuItem(Menu menu, final String faq_url) {
		return addMenuItem(menu, MENU_ID_FAQ, new Listener() {
			public void handleEvent(Event e) {
				Utils.launch(faq_url);
			}
		});
	}

	public static MenuItem addReleaseNotesMenuItem(final Menu menu) {
		return addMenuItem(menu, MENU_ID_RELEASE_NOTES, new Listener() {
			public void handleEvent(Event e) {
				new WelcomeWindow(menu.getShell());
			}
		});
	}

	public static MenuItem addGetPluginsMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_PLUGINS_HELP, new Listener() {
			public void handleEvent(Event e) {
				Utils.launch("http://azureus.sourceforge.net/plugin_list.php");
			}
		});
	}

	public static MenuItem addDebugHelpMenuItem(Menu menu) {
		return addMenuItem(menu, MENU_ID_DEBUG_HELP, new Listener() {
			public void handleEvent(Event e) {
				UIDebugGenerator.generate();
			}
		});
	}

	public static MenuItem addCheckUpdateMenuItem(final Menu menu) {
		return addMenuItem(menu, MENU_ID_UPDATE_CHECK, new Listener() {
			public void handleEvent(Event e) {
				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (uiFunctions != null) {
					// PIAMOD -- we don't want this
					//uiFunctions.bringToFront();
				}
				UpdateMonitor.getSingleton(getCore()).performCheck(true, false, false,
						new UpdateCheckInstanceListener() {
							public void cancelled(UpdateCheckInstance instance) {
							}

							public void complete(UpdateCheckInstance instance) {
								if (instance.getUpdates().length == 0) {
									Utils.execSWTThread(new AERunnable() {
										public void runSupport() {
											Utils.openMessageBox(menu.getShell(),
													SWT.ICON_INFORMATION | SWT.OK,
													"window.update.noupdates", (String[]) null);
										}
									});
								}
							}
						});
			}
		});
	}

	public static MenuItem addPluginInstallMenuItem(Menu menuParent) {
		return addMenuItem(menuParent, MENU_ID_PLUGINS_INSTALL, new Listener() {
			public void handleEvent(Event e) {
				new InstallPluginWizard(getCore(), getDisplay());
			}
		});
	}

	public static MenuItem addPluginUnInstallMenuItem(Menu menuParent) {
		return addMenuItem(menuParent, MENU_ID_PLUGINS_UNINSTALL, new Listener() {
			public void handleEvent(Event e) {
				new UnInstallPluginWizard(getCore(), getDisplay());
			}
		});
	}

	/**
	 * Creates a menu item that is simply a label; it does nothing is selected
	 * @param menu
	 * @param localizationKey
	 * @return
	 */
	public static final MenuItem addLabelMenuItem(Menu menu,
			String localizationKey) {
		MenuItem item = new MenuItem(menu, SWT.NULL);
		Messages.setLanguageText(item, localizationKey);
		item.setEnabled(false);
		return item;
	}

	//==========================

	public static MenuItem addSeparatorMenuItem(Menu menuParent) {
		return new MenuItem(menuParent, SWT.SEPARATOR);
	}

	private static MenuItem createTopLevelMenuItem(Menu menuParent,
			String localizationKey) {
		Menu menu = new Menu(menuParent.getShell(), SWT.DROP_DOWN);
		MenuItem menuItem = new MenuItem(menuParent, SWT.CASCADE);
		Messages.setLanguageText(menuItem, localizationKey);
		menuItem.setMenu(menu);

		/*
		 * A top level menu and its menu item has the same ID; this is used to locate them at runtime 
		 */
		menu.setData(KEY_MENU_ID, localizationKey);
		menuItem.setData(KEY_MENU_ID, localizationKey);

		return menuItem;
	}

	public static final MenuItem addMenuItem(Menu menu, String localizationKey,
			Listener selListener) {
		return addMenuItem(menu, localizationKey, selListener, SWT.NONE);
	}

	public static final MenuItem addMenuItem(Menu menu, String localizationKey,
			Listener selListener, int style) {
		MenuItem menuItem = new MenuItem(menu, style);
		Messages.setLanguageText(menuItem,
				MessageText.resolveLocalizationKey(localizationKey));
		KeyBindings.setAccelerator(menuItem,
				MessageText.resolveAcceleratorKey(localizationKey));
		if (null != selListener) {
			menuItem.addListener(SWT.Selection, selListener);
		}
		/*
		 * Using the localizationKey as the id for the menu item; this can be used to locate it at runtime
		 * using .KN: missing method pointers
		 */
		menuItem.setData(KEY_MENU_ID, localizationKey);
		return menuItem;
	}

	public static final MenuItem addMenuItem(Menu menu, int style,
			String localizationKey, Listener selListener) {
		MenuItem menuItem = new MenuItem(menu, style);
		Messages.setLanguageText(menuItem, localizationKey);
		KeyBindings.setAccelerator(menuItem, localizationKey);
		menuItem.addListener(SWT.Selection, selListener);
		/*
		 * Using the localizationKey as the id for the menu item; this can be used to locate it at runtime
		 * using .KN: missing method pointers
		 */
		menuItem.setData(KEY_MENU_ID, localizationKey);
		return menuItem;
	}

	private static UIFunctionsSWT getUIFunctionSWT() {
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null != uiFunctions) {
			return uiFunctions;
		}
		throw new IllegalStateException(
				"No instance of UIFunctionsSWT found; the UIFunctionsManager might not have been initialized properly");
	}

	private static AzureusCore getCore() {
		return AzureusCoreFactory.getSingleton();
	}

	private static Display getDisplay() {
		return SWTThread.getInstance().getDisplay();
	}

	public static void updateMenuText(Object menu) {
		if (menu == null)
			return;
		if (menu instanceof Menu) {
			MenuItem[] menus = ((Menu) menu).getItems();
			for (int i = 0; i < menus.length; i++) {
				updateMenuText(menus[i]);
			}
		} else if (menu instanceof MenuItem) {
			MenuItem item = (MenuItem) menu;
			if (item.getData(KEY_MENU_ID) instanceof String) {
				String localizationKey = (String) item.getData(KEY_MENU_ID);
				item.setText(MessageText.getString(localizationKey));
				KeyBindings.setAccelerator(item,
						MessageText.resolveAcceleratorKey(localizationKey));
				updateMenuText(item.getMenu());
			}
		}
	}

	public static void performOneTimeDisable(MenuItem item,
			boolean affectsChildMenuItems) {
		item.setEnabled(false);
		if (affectsChildMenuItems) {
			Menu childMenu = item.getMenu();
			if (childMenu == null)
				return;

			for (int i = 0; i < childMenu.getItemCount(); i++) {
				childMenu.getItem(i).setEnabled(false);
			}
		}
	}

	/**
	 * Find and return the menu with the given id starting from the given menu
	 * 
	 * @param menuToStartWith
	 * @param idToMatch any of the menu keys listed in {@link org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants}
	 * @return may return <code>null</code> if not found
	 */
	public static Menu findMenu(Menu menuToStartWith, String idToMatch) {

		/*
		 * This is a recursive method; it will start at the given menuToStartWith
		 * and recursively traverse to all its sub menus until a matching
		 * menu is found or until it has touched all sub menus
		 */
		if (null == menuToStartWith || true == menuToStartWith.isDisposed()
				|| null == idToMatch || idToMatch.length() < 1) {
			return null;
		}

		/*
		 * The given menuToStartWith may be the one we're looking for
		 */
		if (true == idToMatch.equals(getID(menuToStartWith))) {
			return menuToStartWith;
		}

		MenuItem[] items = menuToStartWith.getItems();

		/*
		 * Go deeper into each child to try and find it
		 */
		for (int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			Menu menuToFind = findMenu(item.getMenu(), idToMatch);
			if (null != menuToFind) {
				return menuToFind;
			}
		}

		return null;
	}

	/**
	 * Find and return the menu item with the given id starting from the given menu
	 * 
	 * @param menuToStartWith
	 * @param idToMatch any of the menu keys listed in {@link org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants}
	 * @return may return <code>null</code> if not found
	 */
	public static MenuItem findMenuItem(Menu menuToStartWith, String idToMatch) {
		/*
		 * This is a recursive method; it will start at the given menuToStartWith
		 * and recursively traverse to all its sub menus until a matching
		 * menu item is found or until it has touched all existing menu items
		 */
		if (null == menuToStartWith || true == menuToStartWith.isDisposed()
				|| null == idToMatch || idToMatch.length() < 1) {
			return null;
		}

		MenuItem[] items = menuToStartWith.getItems();

		for (int i = 0; i < items.length; i++) {
			MenuItem item = items[i];
			if (true == idToMatch.equals(getID(item))) {
				return item;
			}

			/*
			 * Go deeper into each child to try and find it
			 */
			MenuItem menuItemToFind = findMenuItem(item.getMenu(), idToMatch);
			if (null != menuItemToFind) {
				return menuItemToFind;
			}
		}

		return null;
	}

	private static String getID(Widget widget) {
		if (null != widget && false == widget.isDisposed()) {
			Object id = widget.getData(KEY_MENU_ID);
			if (null != id) {
				return id.toString();
			}
		}
		return "";
	}

	public static void setEnablementKeys(Widget widget, int keys) {
		if (null != widget && false == widget.isDisposed()) {
			widget.setData(KEY_ENABLEMENT, new Integer(keys));
		}
	}

	public static int getEnablementKeys(Widget widget) {
		if (null != widget && false == widget.isDisposed()) {
			Object keys = widget.getData(KEY_ENABLEMENT);
			if (keys instanceof Integer) {
				return ((Integer) keys).intValue();
			}
		}
		return -1;
	}

	/**
	 * Updates the enabled state of the given menu and all its applicable children
	 * <p><b>NOTE:</b> This method currently iterates through the menu hierarchy to
	 * set the enablement which may be inefficient since most menus do not have this flag set;
	 * it may be desirable to employ a map of only the effected menus for efficient direct
	 * access to them</p>
	 * @param menuToStartWith
	 */
	public static void updateEnabledStates(Menu menuToStartWith) {
		/*
		 * This is a recursive method; it will start at the given menuToStartWith
		 * and recursively traverse to all its sub menus until a matching
		 * menu item is found or until it has touched all existing menu items
		 */
		if (null == menuToStartWith || true == menuToStartWith.isDisposed()) {
			return;
		}

		/*
		 * If the given menu itself is disabled then just return since
		 * its menu items can not be seen anyway
		 */
		if (false == setEnablement(menuToStartWith)) {
			return;
		}

		MenuItem[] items = menuToStartWith.getItems();

		for (int i = 0; i < items.length; i++) {
			MenuItem item = items[i];

			/*
			 * If the current menu item is disabled then just return since
			 * its children items can not be seen anyway
			 */
			if (false == setEnablement(item)) {
				continue;
			}

			/*
			 * Go deeper into the children items and set their enablement
			 */
			updateEnabledStates(item.getMenu());

		}
	}

	/**
	 * Sets whether the given widget is enabled or not based on the value of the
	 * KEY_ENABLEMENT object data set into the given widget.
	 * @param widget
	 * @return
	 */
	public static boolean setEnablement(Widget widget) {
		if (null != widget && false == widget.isDisposed()) {
			boolean isEnabled = isEnabledForCurrentMode(widget);

			if (widget instanceof MenuItem) {
				((MenuItem) widget).setEnabled(isEnabled);
			} else if (widget instanceof Menu) {
				((Menu) widget).setEnabled(isEnabled);
			}
			return isEnabled;
		}
		return false;
	}

	/**
	 * Returns whether the given widget should be enabled for the current mode;
	 * current mode can be az2, az3, or az3 advanced.
	 * @param widget
	 * @return
	 */
	public static boolean isEnabledForCurrentMode(Widget widget) {
		int keys = getEnablementKeys(widget);
		if (keys <= 0) {
			return true;
		} else if (true == isAZ3_ADV) {
			return ((keys & FOR_AZ3_ADV) != 0);
		} else if (true == isAZ3) {
			return ((keys & FOR_AZ3) != 0);
		} else {
			return ((keys & FOR_AZ2) != 0);
		}
	}
}
