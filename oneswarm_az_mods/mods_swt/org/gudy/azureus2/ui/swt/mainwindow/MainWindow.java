/*
 * Created on Jun 25, 2003
 * Modified Apr 13, 2004 by Alon Rohter
 * Modified Apr 17, 2004 by Olivier Chalouhi (OSX system menu)
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.components.ColorUtils;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateShell;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.sharing.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIStatusTextClickListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * Runnable : so that GUI initialization is done via asyncExec(this)
 * STProgressListener : To make it visible once initialization is done
 */
public class MainWindow
	extends AERunnable
	implements ParameterListener, IconBarEnabler, AEDiagnosticsEvidenceGenerator,
	ObfusticateShell, IMainWindow
{
	private static final LogIDs LOGID = LogIDs.GUI;

	private static MainWindow window;

	private Initializer initializer;

	private GUIUpdater updater;

	private AzureusCore azureus_core;

	private GlobalManager globalManager;

	//NICO handle swt on macosx
	public static boolean isAlreadyDead = false;

	public static boolean isDisposeFromListener = false;

	private Display display;

	private Composite parent;

	private Shell shell;

	private IMainMenu mainMenu;

	private IconBar iconBar;

	private boolean useCustomTab;

	private Composite folder;

	/** 
	 * Handles initializing and refreshing the status bar (w/help of GUIUpdater)
	 */
	private MainStatusBar mainStatusBar;

	private TrayWindow downloadBasket;

	private SystemTraySWT systemTraySWT;

	private HashMap downloadViews;

	private AEMonitor downloadViews_mon = new AEMonitor("MainWindow:dlviews");

	private Tab mytorrents;

	private Tab all_peers;

	private Tab my_tracker_tab;

	private Tab my_shares_tab;

	private Tab stats_tab;

	private Tab console;

	private Tab multi_options_tab;

	private Tab config;

	private ConfigView config_view;

	protected AEMonitor this_mon = new AEMonitor("MainWindow");

	private UISWTInstanceImpl uiSWTInstanceImpl = null;

	private ArrayList events;

	private UIFunctionsSWT uiFunctions;

	private boolean bIconBarEnabled = false;

	private boolean bShowMainWindow;

	private boolean bSettingVisibility = false;

	public MainWindow(AzureusCore _azureus_core, Initializer _initializer,
			ArrayList events) {
		bShowMainWindow = false; // PIAMOD
		try {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));

			AEDiagnostics.addEvidenceGenerator(this);

			azureus_core = _azureus_core;

			globalManager = azureus_core.getGlobalManager();

			initializer = _initializer;

			display = SWTThread.getInstance().getDisplay();

			window = this;

			this.events = events;

			display.asyncExec(this);

		} catch (AzureusCoreException e) {

			Debug.printStackTrace(e);
		}
	}

	/**
	 * runSupport() MUST BE CALLED TO FINISH INITIALIZATION
	 * @param _azureus_core
	 * @param _initializer
	 * @param shell
	 * @param parent
	 */
	public MainWindow(AzureusCore _azureus_core, Initializer _initializer,
			Shell shell, Composite parent, UISWTInstanceImpl swtinstance) {
		this.shell = shell;
		this.parent = parent;
		bShowMainWindow = true;

		try {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));

			AEDiagnostics.addEvidenceGenerator(this);

			azureus_core = _azureus_core;

			globalManager = azureus_core.getGlobalManager();

			initializer = _initializer;

			display = SWTThread.getInstance().getDisplay();

			window = this;

			uiSWTInstanceImpl = swtinstance;

		} catch (AzureusCoreException e) {

			Debug.printStackTrace(e);
		}
	}

	public void setShowMainWindow(boolean b) {
		bShowMainWindow = b;
	}

	// @see org.gudy.azureus2.core3.util.AERunnable#runSupport()
	public void runSupport() {
		FormData formData;

		try {
			uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions == null) {
				uiFunctions = new UIFunctionsImpl(this);
				UIFunctionsManager.setUIFunctions(uiFunctions);
			} else {
				uiFunctions = new UIFunctionsImpl(this);
			}

			globalManager.loadExistingTorrentsNow(true);

			useCustomTab = COConfigurationManager.getBooleanParameter("useCustomTab");
			Tab.setUseCustomTab(useCustomTab);

			COConfigurationManager.addParameterListener("config.style.useSIUnits",
					this);

			mytorrents = null;
			my_tracker_tab = null;
			console = null;
			config = null;
			config_view = null;
			downloadViews = new HashMap();

			Control attachToTopOf = null;
			Control controlAboveFolder = null;
			Control controlBelowFolder = null;

			//The Main Window
			if (shell == null) {
				shell = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE
						| SWT.MAX | SWT.MIN);
				shell.setData("class", this);
				shell.setText("OneSwarm"); //$NON-NLS-1$
				Utils.setShellIcon(shell);

				if (parent == null) {
					parent = shell;
				}

				// register window
				ShellManager.sharedManager().addWindow(shell);

				mainMenu = new MainMenu(shell);

				FormLayout mainLayout = new FormLayout();
				mainLayout.marginHeight = 0;
				mainLayout.marginWidth = 0;
				try {
					mainLayout.spacing = 0;
				} catch (NoSuchFieldError e) { /* Pre SWT 3.0 */
				}
				shell.setLayout(mainLayout);

				Utils.linkShellMetricsToConfig(shell, "window");

				//NICO catch the dispose event from file/quit on osx
				shell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent event) {
						if (!isAlreadyDead) {
							isDisposeFromListener = true;
							if (shell != null) {
								shell.removeDisposeListener(this);
								dispose(false, false);
							}
							isAlreadyDead = true;
						}
					}
				});

				shell.addShellListener(new ShellAdapter() {
					public void shellClosed(ShellEvent event) {
						if (bSettingVisibility) {
							return;
						}
						if (systemTraySWT != null
								&& COConfigurationManager.getBooleanParameter("Enable System Tray")
								&& COConfigurationManager.getBooleanParameter("Close To Tray")) {

							minimizeToTray(event);
						} else {
							event.doit = dispose(false, false);
						}
					}

					public void shellIconified(ShellEvent event) {
						if (bSettingVisibility) {
							return;
						}
						if (systemTraySWT != null
								&& COConfigurationManager.getBooleanParameter("Enable System Tray")
								&& COConfigurationManager.getBooleanParameter("Minimize To Tray")) {

							minimizeToTray(event);
						}
					}

					public void shellDeiconified(ShellEvent e) {
						if (Constants.isOSX
								&& COConfigurationManager.getBooleanParameter("Password enabled")) {
							shell.setVisible(false);
							if (PasswordWindow.showPasswordWindow(display)) {
								shell.setVisible(true);
							}
						}
					}
				});

				// Separator between menu and icon bar
				Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
				formData = new FormData();
				formData.top = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
				separator.setLayoutData(formData);

				attachToTopOf = separator;

				mainStatusBar = new MainStatusBar();
				Composite statusBar = mainStatusBar.initStatusBar(azureus_core,
						globalManager, display, shell);

				controlAboveFolder = attachToTopOf;
				controlBelowFolder = statusBar;

			}

			try {
				Utils.createTorrentDropTarget(parent, true);
			} catch (SWTError e) {
				// "Cannot initialize Drop".. don't spew stack trace
				Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
						"Drag and Drop not available: " + e.getMessage()));
			} catch (Throwable e) {
				Logger.log(new LogEvent(LOGID, "Drag and Drop not available", e));
			}

			if (!useCustomTab) {
				folder = new TabFolder(parent, SWT.V_SCROLL);
			} else {
				folder = new CTabFolder(parent, SWT.CLOSE | SWT.BORDER);
				final Color bg = ColorUtils.getShade(folder.getBackground(),
						(Constants.isOSX) ? -25 : -6);
				final Color fg = ColorUtils.getShade(folder.getForeground(),
						(Constants.isOSX) ? 25 : 6);
				folder.setBackground(bg);
				folder.setForeground(fg);
				//((CTabFolder)folder).setBorderVisible(false);
				folder.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent event) {
						bg.dispose();
						fg.dispose();
					}
				});

				((CTabFolder) folder).addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void close(CTabFolderEvent event) {
						if (!Tab.closed((Item) event.item)) {
							event.doit = false;
						}
					}
				});
			}

			formData = new FormData();
			if (controlAboveFolder == null) {
				formData.top = new FormAttachment(0, 0);
			} else {
				formData.top = new FormAttachment(controlAboveFolder);
			}

			if (controlBelowFolder == null) {
				formData.bottom = new FormAttachment(100, 0);
			} else {
				formData.bottom = new FormAttachment(controlBelowFolder);
			}
			formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
			formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
			folder.setLayoutData(formData);

			Tab.initialize(this, folder);

			folder.getDisplay().addFilter(SWT.KeyDown, new Listener() {
				public void handleEvent(Event event) {
					// Another window has control, skip filter
					Control focus_control = display.getFocusControl();
					if (focus_control != null && focus_control.getShell() != shell)
						return;

					int key = event.character;
					if ((event.stateMask & SWT.MOD1) != 0 && event.character <= 26
							&& event.character > 0)
						key += 'a' - 1;

					// ESC or CTRL+F4 closes current Tab
					if (key == SWT.ESC
							|| (event.keyCode == SWT.F4 && event.stateMask == SWT.CTRL)) {
						Tab.closeCurrent();
						event.doit = false;
					} else if (event.keyCode == SWT.F6
							|| (event.character == SWT.TAB && (event.stateMask & SWT.CTRL) != 0)) {
						// F6 or Ctrl-Tab selects next Tab
						// On Windows the tab key will not reach this filter, as it is
						// processed by the traversal TRAVERSE_TAB_NEXT.  It's unknown
						// what other OSes do, so the code is here in case we get TAB
						if ((event.stateMask & SWT.SHIFT) == 0) {
							event.doit = false;
							Tab.selectNextTab(true);
							// Shift+F6 or Ctrl+Shift+Tab selects previous Tab
						} else if (event.stateMask == SWT.SHIFT) {
							Tab.selectNextTab(false);
							event.doit = false;
						}
					} else if (key == 'l' && (event.stateMask & SWT.MOD1) != 0) {
						// Ctrl-L: Open URL
						OpenTorrentWindow.invokeURLPopup(shell, globalManager);
						event.doit = false;
					}
				}
			});

			SelectionAdapter selectionAdapter = new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					if (display != null && !display.isDisposed())
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if (useCustomTab) {
									CTabItem item = (CTabItem) event.item;
									if (item != null && !item.isDisposed()
											&& !folder.isDisposed()) {
										try {
											((CTabFolder) folder).setSelection(item);
											Control control = item.getControl();
											if (control != null) {
												control.setVisible(true);
												control.setFocus();
											}
										} catch (Throwable e) {
											Debug.printStackTrace(e);
											//Do nothing
										}
									}
								}
								if (iconBar != null) {
									iconBar.setCurrentEnabler(MainWindow.this);
								}
								refreshTorrentMenu();
							}

						});
				}
			};

			if (!useCustomTab) {
				((TabFolder) folder).addSelectionListener(selectionAdapter);
			} else {
				try {
					((CTabFolder) folder).setMinimumCharacters(75);
				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "Can't set MIN_TAB_WIDTH", e));
				}
				//try {
				///  TabFolder2ListenerAdder.add((CTabFolder)folder);
				//} catch (NoClassDefFoundError e) {
				((CTabFolder) folder).addCTabFolderListener(new CTabFolderAdapter() {
					public void itemClosed(CTabFolderEvent event) {
						if (!event.doit) {
							return;
						}
						Tab.closed((CTabItem) event.item);
						event.doit = true;
						((CTabItem) event.item).dispose();
					}
				});
				//}

				((CTabFolder) folder).addSelectionListener(selectionAdapter);

				try {
					((CTabFolder) folder).setSelectionBackground(new Color[] {
						display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
						display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
						display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
					}, new int[] {
						10,
						90
					}, true);
				} catch (NoSuchMethodError e) {
					/** < SWT 3.0M8 **/
					((CTabFolder) folder).setSelectionBackground(new Color[] {
						display.getSystemColor(SWT.COLOR_LIST_BACKGROUND)
					}, new int[0]);
				}
				((CTabFolder) folder).setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

				try {
					/* Pre 3.0M8 doesn't have Simple-mode (it's always simple mode)
					   in 3.0M9, it was called setSimpleTab(boolean)
					   in 3.0RC1, it's called setSimple(boolean)
					   Prepare for the future, and use setSimple()
					 */
					((CTabFolder) folder).setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
				} catch (NoSuchMethodError e) {
					/** < SWT 3.0RC1 **/
				}
			}

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Initializing GUI complete"));

			globalManager.addListener(new GlobalManagerAdapter() {
				public void downloadManagerAdded(DownloadManager dm) {
					MainWindow.this.downloadManagerAdded(dm);
				}

				public void downloadManagerRemoved(DownloadManager dm) {
					MainWindow.this.downloadManagerRemoved(dm);
				}
			});

			PluginManager plugin_manager = azureus_core.getPluginManager();

			plugin_manager.firePluginEvent(PluginEvent.PEV_CONFIGURATION_WIZARD_STARTS);

			if (!COConfigurationManager.getBooleanParameter("Wizard Completed")) {
				// returns after the wizard is done
				new ConfigureWizard(getAzureusCore(), true);
			}

			plugin_manager.firePluginEvent(PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES);

			// attach the UI to plugins
			// Must be done before initializing views, since plugins may register
			// table columns and other objects
			if (uiSWTInstanceImpl == null) {
				uiSWTInstanceImpl = new UISWTInstanceImpl(azureus_core);
				uiSWTInstanceImpl.init();

				// check if any plugins shut us down
				if (isAlreadyDead) {
					return;
				}

				postPluginSetup();
			}

		} catch (Throwable e) {
			Debug.printStackTrace(e);
		}

		showMainWindow();
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	public void postPluginSetup() {
		if (azureus_core.getTrackerHost().getTorrents().length > 0) {
			showMyTracker();
		}

		PluginManager plugin_manager = azureus_core.getPluginManager();

		// share manager init is async so we need to deal with this

		PluginInterface default_pi = plugin_manager.getDefaultPluginInterface();

		try {
			final ShareManager share_manager = default_pi.getShareManager();

			default_pi.addListener(new PluginListener() {
				public void initializationComplete() {
				}

				public void closedownInitiated() {
					int share_count = share_manager.getShares().length;

					if (share_count != COConfigurationManager.getIntParameter("GUI_SWT_share_count_at_close")) {

						COConfigurationManager.setParameter("GUI_SWT_share_count_at_close",
								share_count);
					}
				}

				public void closedownComplete() {
				}
			});

			if (share_manager.getShares().length > 0
					|| COConfigurationManager.getIntParameter("GUI_SWT_share_count_at_close") > 0) {

				showMyShares();
			}
		} catch (ShareException e) {
			Debug.out(e);
		}

		if (COConfigurationManager.getBooleanParameter("Open MyTorrents")) {
			showMyTorrents();
		}

		//  share progress window

		new ProgressWindow();

		if (COConfigurationManager.getBooleanParameter("Open Console")) {
			showConsole();
		}
		events = null;

		if (COConfigurationManager.getBooleanParameter("Open Config")) {
			showConfig();
		}

		if (COConfigurationManager.getBooleanParameter("Open Stats On Start")) {
			showStats();
		}

		if (COConfigurationManager.getBooleanParameter("Open Transfer Bar On Start")) {
			uiFunctions.showGlobalTransferBar();
		}

		COConfigurationManager.addParameterListener("GUI_SWT_bFancyTab", this);

		updater = new GUIUpdater(this);
		updater.start();

		COConfigurationManager.addAndFireParameterListener("IconBar.enabled",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						setIconBarEnabled(COConfigurationManager.getBooleanParameter(parameterName));
					}
				});
	}

	protected boolean getIconBarEnabled() {
		return bIconBarEnabled;
	}

	protected void setIconBarEnabled(boolean enabled) {
		if (enabled == bIconBarEnabled || shell.isDisposed()) {
			return;
		}
		bIconBarEnabled = enabled;
		COConfigurationManager.setParameter("IconBar.enabled", bIconBarEnabled);
		if (bIconBarEnabled) {
			try {
				iconBar = new IconBar(parent);
				iconBar.setCurrentEnabler(this);
				Composite cIconBar = iconBar.getComposite();

				FormData folderLayoutData = (FormData) folder.getLayoutData();

				FormData formData = new FormData();
				if (folderLayoutData.top != null
						&& folderLayoutData.top.control != null) {
					formData.top = new FormAttachment(folderLayoutData.top.control);
				} else {
					formData.top = new FormAttachment(0, 0);
				}
				folderLayoutData.top = new FormAttachment(cIconBar);

				formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
				this.iconBar.setLayoutData(formData);

			} catch (Exception e) {
				Logger.log(new LogEvent(LOGID, "Creating Icon Bar", e));
			}
		} else if (iconBar != null) {
			try {
				FormData folderLayoutData = (FormData) folder.getLayoutData();
				FormData iconBarLayoutData = (FormData) iconBar.getComposite().getLayoutData();

				if (iconBarLayoutData.top != null
						&& iconBarLayoutData.top.control != null) {
					folderLayoutData.top = new FormAttachment(
							iconBarLayoutData.top.control);
				} else {
					folderLayoutData.top = new FormAttachment(0, 0);
				}

				iconBar.delete();
				iconBar = null;
			} catch (Exception e) {
				Logger.log(new LogEvent(LOGID, "Removing Icon Bar", e));
			}
		}
		shell.layout(true, true);
	}
	
	public static void forceShowForOldUI()
	{
		getWindow().bShowMainWindow = true;
		getWindow().showMainWindow();
	}

	protected void showMainWindow() {
		
		/**
		 * PIAMOD -- always use tray if not on OS X, otherwise no way to quit!
		 */
		if( systemTraySWT == null )
		{
			if( Constants.isOSX == false )
			{
				systemTraySWT = new SystemTraySWT();
			}
			else if( COConfigurationManager.getBooleanParameter("Enable System Tray") )
			{
				/**
				 * because of dock, we could skip this if we wanted (also, the systray icon is a bit buggy on OS X)
				 */
				systemTraySWT = new SystemTraySWT();
			}
		}
		
		if (!bShowMainWindow) {
			
			/**
			 * We still want to trigger lifecycle listener so various windows will be willing to popup, 
			 * e.g., open torrents
			 */
			
			COConfigurationManager.addAndFireParameterListener("Show Download Basket",
					this);

			azureus_core.triggerLifeCycleComponentCreated(uiFunctions);
			azureus_core.getPluginManager().firePluginEvent(
					PluginEvent.PEV_INITIALISATION_UI_COMPLETES);
			
			return;
		}

		// No tray access on OSX yet
		boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray")
				&& (!Constants.isOSX || SWT.getVersion() > 3300);
		boolean bPassworded = COConfigurationManager.getBooleanParameter("Password enabled");
		boolean bStartMinimize = bEnableTray
				&& (bPassworded || COConfigurationManager.getBooleanParameter("Start Minimized"));

		if (!bStartMinimize) {
			shell.layout();
			shell.open();
			if (!Constants.isOSX) {
				shell.forceActive();
			}
		} else if (Constants.isOSX) {
			shell.setMinimized(true);
			shell.setVisible(true);
		}

		/**
		 * PIAMOD -- commented out, always use tray, see above. 
		 */
//		if (bEnableTray) {
//
//			try {
//				systemTraySWT = new SystemTraySWT();
//
//			} catch (Throwable e) {
//
//				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
//						"Upgrade to SWT3.0M8 or later for system tray support."));
//			}
//
//			if (bStartMinimize) {
//				minimizeToTray(null);
//			}
//			//Only show the password if not started minimized
//			//Correct bug #878227
//			else {
//				if (bPassworded) {
//					minimizeToTray(null);
//					setVisible(true, true); // invokes password
//				}
//			}
//		}

		COConfigurationManager.addAndFireParameterListener("Show Download Basket",
				this);

		// PIAMOD -- never want to see this.
		//checkForWhatsNewWindow();

		// check file associations   
		//AssociationChecker.checkAssociations();

		azureus_core.triggerLifeCycleComponentCreated(uiFunctions);
		azureus_core.getPluginManager().firePluginEvent(
				PluginEvent.PEV_INITIALISATION_UI_COMPLETES);
	}

	protected void showMyTracker() {
		if (my_tracker_tab == null) {
			my_tracker_tab = new Tab(new MyTrackerView(azureus_core));
			my_tracker_tab.getView().getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							my_tracker_tab = null;
						}
					});
		} else {
			my_tracker_tab.setFocus();
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showMyShares() {
		if (my_shares_tab == null) {
			my_shares_tab = new Tab(new MySharesView(azureus_core));
			my_shares_tab.getView().getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							my_shares_tab = null;
						}
					});
		} else {
			my_shares_tab.setFocus();
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showMyTorrents() {
		if (mytorrents == null) {
			MyTorrentsSuperView view = new MyTorrentsSuperView(azureus_core);
			mytorrents = new Tab(view);
			mytorrents.getView().getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							mytorrents = null;
						}
					});
		} else {
			mytorrents.setFocus();
		}
		refreshIconBar();
		refreshTorrentMenu();
	}

	protected void showAllPeersView() {
		if (all_peers == null) {
			PeerSuperView view = new PeerSuperView(azureus_core.getGlobalManager());
			all_peers = new Tab(view);
			all_peers.getView().getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							all_peers = null;
						}
					});
		} else {
			all_peers.setFocus();
		}
		refreshIconBar();
		refreshTorrentMenu();
	}

	protected void showMultiOptionsView(DownloadManager[] managers) {
		if (multi_options_tab != null) {
			multi_options_tab.dispose();
		}

		TorrentOptionsView view = new TorrentOptionsView(managers);

		multi_options_tab = new Tab(view);

		view.getComposite().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				multi_options_tab = null;
			}
		});

		refreshIconBar();
		refreshTorrentMenu();
	}

	private void minimizeToTray(ShellEvent event) {
		//Added this test so that we can call this method with null parameter.
		if (event != null)
			event.doit = false;

		// XXX hack for release.. should not access param outside Utils.linkShellMetrics
		COConfigurationManager.setParameter("window.maximized",
				shell.getMaximized());
		shell.setVisible(false);

		if (downloadBasket != null)
			downloadBasket.setVisible(true);

		MiniBarManager.getManager().setAllVisible(true);
	}

	private void updateComponents() {
		if (mainStatusBar != null)
			mainStatusBar.refreshStatusText();

		if (folder != null) {
			if (useCustomTab) {
				((CTabFolder) folder).update();
			} else {
				((TabFolder) folder).update();
			}
		}
	}

	protected boolean destroyRequest() {
		Logger.log(new LogEvent(LOGID, "MainWindow::destroyRequest"));

		if (COConfigurationManager.getBooleanParameter("Password enabled")) {

			if (!PasswordWindow.showPasswordWindow(display)) {
				Logger.log(new LogEvent(LOGID, "    denied - password is enabled"));

				return false;
			}
		}

		Utils.execSWTThread(new Runnable() {
			public void run() {
				dispose(false, false);
			}
		});
		return true;
	}

	private void downloadManagerAdded(DownloadManager created) {
		created.addListener(new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager manager, int state) {
				downloadManagerStateChanged(manager, state);
			}
		});
	}

	protected void openManagerView(DownloadManager downloadManager) {
		try {
			downloadViews_mon.enter();

			if (downloadViews.containsKey(downloadManager)) {
				Tab tab = (Tab) downloadViews.get(downloadManager);
				tab.setFocus();
				refreshIconBar();
				refreshTorrentMenu();
			} else {
				Tab tab = new Tab(new ManagerView(azureus_core, downloadManager));
				downloadViews.put(downloadManager, tab);
			}
		} finally {

			downloadViews_mon.exit();
		}
	}

	protected void removeManagerView(DownloadManager downloadManager) {
		try {
			downloadViews_mon.enter();

			downloadViews.remove(downloadManager);
		} finally {

			downloadViews_mon.exit();
		}
	}

	private void downloadManagerRemoved(DownloadManager removed) {
		try {
			downloadViews_mon.enter();

			if (downloadViews.containsKey(removed)) {
				final Tab tab = (Tab) downloadViews.get(removed);
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (display == null || display.isDisposed())
							return;

						tab.dispose();
					}
				});

			}
		} finally {

			downloadViews_mon.exit();
		}
	}

	protected Display getDisplay() {
		return this.display;
	}

	protected Shell getShell() {
		return shell;
	}

	public void setVisible(final boolean visible, final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				bSettingVisibility = true;
				try {
					boolean currentlyVisible = shell.getVisible()
							&& !shell.getMinimized();
					if (visible && !currentlyVisible) {
						if (COConfigurationManager.getBooleanParameter("Password enabled",
								false)) {
							if (!PasswordWindow.showPasswordWindow(display)) {
								shell.setVisible(false);
								return;
							}
						}
					}

					ArrayList wasVisibleList = null;
					boolean bHideAndShow = false;
					// temp disabled
					//tryTricks && visible && Constants.isWindows && display.getActiveShell() != shell;
					if (bHideAndShow) {
						wasVisibleList = new ArrayList();
						// We don't want the window to just flash and not open, so:
						// -Minimize main shell
						// -Set all shells invisible
						try {
							shell.setMinimized(true);
							Shell[] shells = shell.getDisplay().getShells();
							for (int i = 0; i < shells.length; i++) {
								if (shells[i].isVisible()) {
									wasVisibleList.add(shells[i]);
									shells[i].setVisible(false);
								}
							}
						} catch (Exception e) {
						}
					}

					if (visible) {
						shell.setMinimized(false);
						if (!currentlyVisible
								&& COConfigurationManager.getBooleanParameter("window.maximized")) {
							shell.setMaximized(true);
						}
					} else {
						// XXX hack for release.. should not access param outside Utils.linkShellMetrics
						COConfigurationManager.setParameter("window.maximized",
								shell.getMaximized());
					}

					shell.setVisible(visible);
					if (visible) {
						if (downloadBasket != null) {
							downloadBasket.setVisible(false);
							downloadBasket.setMoving(false);
						}

						/*
						 if (trayIcon != null)
						 trayIcon.showIcon();
						 */
						shell.forceActive();

						if (bHideAndShow) {
							try {
								Shell[] shells = shell.getDisplay().getShells();
								for (int i = 0; i < shells.length; i++) {
									if (shells[i] != shell) {
										if (wasVisibleList.contains(shells[i])) {
											shells[i].setVisible(visible);
										}
										shells[i].setFocus();
									}
								}
							} catch (Exception e) {
							}
						}
					}
				} finally {
					bSettingVisibility = false;
				}

			}
		});
	}

	protected boolean isVisible() {
		return shell.isVisible();
	}

	public boolean dispose(final boolean for_restart,
			final boolean close_already_in_progress) {
		return Utils.execSWTThreadWithBool("MainWindow.dispose",
				new AERunnableBoolean() {
					public boolean runSupport() {
						return _dispose(for_restart, close_already_in_progress);
					}
				});
	}

	private boolean _dispose(boolean for_restart,
			boolean close_already_in_progress) {
		if (isAlreadyDead) {
			return true;
		}

		if (!UIExitUtilsSWT.canClose(globalManager, for_restart)) {
			return false;
		}

		if (systemTraySWT != null) {
			systemTraySWT.dispose();
		}

		/**
		 * Explicitly force the transfer bar location to be saved (if appropriate and open).
		 * 
		 * We can't rely that the normal mechanism for doing this won't fail (which it usually does)
		 * when the GUI is being disposed of.
		 */
		AllTransfersBar transfer_bar = AllTransfersBar.getBarIfOpen(AzureusCoreFactory.getSingleton().getGlobalManager());
		if (transfer_bar != null) {
			transfer_bar.forceSaveLocation();
		}

		// close all tabs
		Tab.closeAllTabs();

		isAlreadyDead = true; //NICO try to never die twice...
		/*
		if (this.trayIcon != null)
		  SysTrayMenu.dispose();
		*/

		if (updater != null) {

			updater.stopIt();
		}

		if (initializer != null) {
			initializer.stopIt(for_restart, close_already_in_progress);
		}

		//NICO swt disposes the mainWindow all by itself (thanks... ;-( ) on macosx
		if (!shell.isDisposed() && !isDisposeFromListener) {
			shell.dispose();
		}

		COConfigurationManager.removeParameterListener("config.style.useSIUnits",
				this);
		COConfigurationManager.removeParameterListener("Show Download Basket", this);
		COConfigurationManager.removeParameterListener("GUI_SWT_bFancyTab", this);

		UIExitUtilsSWT.uiShutdown();

		return true;
	}

	protected GlobalManager getGlobalManager() {
		return globalManager;
	}

	/**
	 * @return
	 */
	protected static MainWindow getWindow() {
		return window;
	}

	/**
	 * @return
	 */
	protected TrayWindow getTray() {
		return downloadBasket;
	}

	Map pluginTabs = new HashMap();

	protected void openPluginView(String sParentID, String sViewID,
			UISWTViewEventListener l, Object dataSource, boolean bSetFocus) {

		UISWTViewImpl view = null;
		try {
			view = new UISWTViewImpl(sParentID, sViewID, l);
		} catch (Exception e) {
			Tab tab = (Tab) pluginTabs.get(sViewID);
			if (tab != null) {
				tab.setFocus();
			}
			return;
		}
		view.dataSourceChanged(dataSource);

		Tab tab = new Tab(view, bSetFocus);

		pluginTabs.put(sViewID, tab);
	}

	/**
	 * Close all plugin views with the specified ID
	 * 
	 * @param sViewID
	 */
	protected void closePluginViews(String sViewID) {
		Item[] items;

		if (folder instanceof CTabFolder)
			items = ((CTabFolder) folder).getItems();
		else if (folder instanceof TabFolder)
			items = ((TabFolder) folder).getItems();
		else
			return;

		for (int i = 0; i < items.length; i++) {
			IView view = Tab.getView(items[i]);
			if (view instanceof UISWTViewImpl) {
				String sID = ((UISWTViewImpl) view).getViewID();
				if (sID != null && sID.equals(sViewID)) {
					try {
						closePluginView(view);
					} catch (Exception e) {
						Debug.printStackTrace(e);
					}
				}
			}
		} // for
	}

	/**
	 * Get all open Plugin Views
	 * 
	 * @return open plugin views
	 */
	protected UISWTView[] getPluginViews() {
		Item[] items;

		if (folder instanceof CTabFolder)
			items = ((CTabFolder) folder).getItems();
		else if (folder instanceof TabFolder)
			items = ((TabFolder) folder).getItems();
		else
			return new UISWTView[0];

		ArrayList views = new ArrayList();

		for (int i = 0; i < items.length; i++) {
			IView view = Tab.getView(items[i]);
			if (view instanceof UISWTViewImpl) {
				views.add(view);
			}
		} // for

		return (UISWTView[]) views.toArray(new UISWTView[0]);
	}

	protected void openPluginView(final AbstractIView view, final String name) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Tab tab = (Tab) pluginTabs.get(name);
				if (tab != null) {
					tab.setFocus();
				} else {
					tab = new Tab(view);
					pluginTabs.put(name, tab);
				}
			}
		});
	}

	protected void closePluginView(IView view) {
		Item tab = Tab.getTab(view);

		if (tab != null) {

			Tab.closed(tab);
		}
	}

	public void removeActivePluginView(String view_name) {
		pluginTabs.remove(view_name);
	}

	// @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	public void parameterChanged(String parameterName) {
		if (parameterName.equals("Show Download Basket")) {
			if (COConfigurationManager.getBooleanParameter("Show Download Basket")) {
				if (downloadBasket == null) {
					downloadBasket = new TrayWindow(this);
					downloadBasket.setVisible(true);
				}
			} else if (downloadBasket != null) {
				downloadBasket.setVisible(false);
				downloadBasket = null;
			}
		}

		if (parameterName.equals("GUI_SWT_bFancyTab")
				&& folder instanceof CTabFolder && folder != null
				&& !folder.isDisposed()) {
			try {
				((CTabFolder) folder).setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
			} catch (NoSuchMethodError e) {
				/** < SWT 3.0RC1 **/
			}
		}

		if (parameterName.equals("config.style.useSIUnits")) {
			updateComponents();
		}
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		if (itemKey.equals("open"))
			return true;
		if (itemKey.equals("new"))
			return true;
		IView currentView = getCurrentView();
		if (currentView != null)
			return currentView.isEnabled(itemKey);
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#itemActivated(java.lang.String)
	public void itemActivated(String itemKey) {
		if (itemKey.equals("open")) {
			TorrentOpener.openTorrentWindow();
			return;
		}
		if (itemKey.equals("new")) {
			new NewTorrentWizard(getAzureusCore(), display);
			return;
		}
		IView currentView = getCurrentView();
		if (currentView != null)
			currentView.itemActivated(itemKey);
	}

	IView getCurrentView() {
		try {
			if (!useCustomTab) {
				TabItem[] selection = ((TabFolder) folder).getSelection();
				if (selection.length > 0) {
					return Tab.getView(selection[0]);
				}
				return null;
			}
			return Tab.getView(((CTabFolder) folder).getSelection());
		} catch (Exception e) {
			return null;
		}
	}

	protected void refreshIconBar() {
		if (iconBar != null) {
			iconBar.setCurrentEnabler(this);
		}
	}

	protected void refreshTorrentMenu() {
		if (this.mainMenu == null) {
			return;
		}
		DownloadManager[] dm;
		boolean detailed_view;
		TableViewSWT tv = null;
		IView currentView = getCurrentView();

		if (currentView instanceof ManagerView) {
			dm = new DownloadManager[] {
				((ManagerView) currentView).getDownload(),
			};
			detailed_view = true;
		} else if (currentView instanceof MyTorrentsSuperView) {
			dm = ((MyTorrentsSuperView) this.getCurrentView()).getSelectedDownloads();
			detailed_view = false;
		} else {
			dm = null;
			detailed_view = false;
		}

		if (currentView instanceof TableViewTab) {
			tv = ((TableViewTab) currentView).getTableView();
		}

		/*
		 * KN: Reflectively find the Torrents menu item and update its data
		 */
		final MenuItem torrentItem = MenuFactory.findMenuItem(
				mainMenu.getMenu(IMenuConstants.MENU_ID_MENU_BAR),
				MenuFactory.MENU_ID_TORRENT);

		if (null != torrentItem) {
			final DownloadManager[] dm_final = dm;
			final TableViewSWT tv_final = tv;
			final boolean detailed_view_final = detailed_view;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (null == dm_final) {
						torrentItem.setEnabled(false);
					} else {
						torrentItem.setData("downloads", dm_final);
						torrentItem.setData("TableView", tv_final);
						torrentItem.setData("is_detailed_view",
								Boolean.valueOf(detailed_view_final));
						torrentItem.setEnabled(true);
					}
				}
			}, true); // async
		}

	}

	protected void close() {
		getShell().close();
	}

	protected void closeViewOrWindow() {
		if (getCurrentView() != null)
			Tab.closeCurrent();
		else
			close();
	}

	protected ConfigView showConfig() {
		if (config == null) {
			config_view = new ConfigView(azureus_core);
			config = new Tab(config_view);
			config_view.getComposite().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					config = null;
					config_view = null;
				}
			});
		} else {
			config.setFocus();
			refreshIconBar();
			refreshTorrentMenu();
		}
		return config_view;
	}

	protected boolean showConfig(String id) {
		showConfig();
		if (config_view == null) {
			return false;
		}
		if (id == null) {
			return true;
		}
		return config_view.selectSection(id);
	}

	protected void showConsole() {
		if (console == null) {
			console = new Tab(new LoggerView(events));
			console.getView().getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							console = null;
						}
					});
		} else {
			console.setFocus();
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showStats() {
		if (stats_tab == null) {
			stats_tab = new Tab(new StatsView(globalManager, azureus_core));
			stats_tab.getView().getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							stats_tab = null;
						}
					});
		} else {
			stats_tab.setFocus();
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showStatsDHT() {
		showStats();
		if (stats_tab == null) {
			return;
		}
		((StatsView) stats_tab.getView()).showDHT();
	}

	protected void showStatsTransfers() {
		showStats();
		if (stats_tab == null) {
			return;
		}
		((StatsView) stats_tab.getView()).showTransfers();
	}

	protected void setSelectedLanguageItem() {
		try {
			this_mon.enter();

			Messages.updateLanguageForControl(shell);

			if (systemTraySWT != null) {
				systemTraySWT.updateLanguage();
			}

			if (mainStatusBar != null) {
				mainStatusBar.refreshStatusText();
			}

			if (folder != null) {
				if (useCustomTab) {
					((CTabFolder) folder).update();
				} else {
					((TabFolder) folder).update();
				}
			}

			if (downloadBasket != null) {
				downloadBasket.updateLanguage();
			}

			Tab.updateLanguage();

			if (mainStatusBar != null) {
				mainStatusBar.updateStatusText();
			}

			if (mainMenu != null) {
				MenuFactory.updateMenuText(mainMenu.getMenu(IMenuConstants.MENU_ID_MENU_BAR));
			}
		} finally {

			this_mon.exit();
		}
	}

	/**
	 * @deprecated Use {@link #getMainMenu()} instead
	 * @return
	 */
	public MainMenu getMenu() {
		return (MainMenu) mainMenu;
	}

	/**
	 * @deprecated Use {@link #setMainMenu(IMainMenu)} instead
	 * @param menu
	 */
	public void setMenu(MainMenu menu) {
		mainMenu = menu;
	}

	public IMainMenu getMainMenu() {
		return mainMenu;
	}

	public void setMainMenu(IMainMenu menu) {
		mainMenu = menu;
	}

	private void downloadManagerStateChanged(final DownloadManager manager,
			int state) {
		// if state == STARTED, then open the details window (according to config)
		if (state == DownloadManager.STATE_DOWNLOADING
				|| state == DownloadManager.STATE_SEEDING) {
			if (display != null && !display.isDisposed()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (display == null || display.isDisposed())
							return;

						if (COConfigurationManager.getBooleanParameter("Open Details")) {
							openManagerView(manager);
						}

						boolean complete = manager.isDownloadComplete(false);

						if (((!complete) && COConfigurationManager.getBooleanParameter("Open Bar Incomplete"))
								|| (complete && COConfigurationManager.getBooleanParameter("Open Bar Complete"))) {

							DownloadBar.open(manager, shell);
						}
					}
				});
			}
		}
	}

	protected AzureusCore getAzureusCore() {
		return (azureus_core);
	}

	// @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	public void generate(IndentWriter writer) {
		writer.println("SWT UI");

		try {
			writer.indent();

			writer.println("MyTorrents");

			Tab t = mytorrents;
			if (t != null) {
				try {
					writer.indent();

					t.generateDiagnostics(writer);
				} finally {

					writer.exdent();
				}
			}

			t = my_tracker_tab;
			if (t != null) {
				writer.println("MyTracker");

				try {
					writer.indent();

					t.generateDiagnostics(writer);
				} finally {

					writer.exdent();
				}
			}

			t = my_shares_tab;
			if (t != null) {
				writer.println("MyShares");

				try {
					writer.indent();

					t.generateDiagnostics(writer);
				} finally {

					writer.exdent();
				}
			}

			TableColumnManager.getInstance().generateDiagnostics(writer);
		} finally {

			writer.exdent();
		}
	}

	private void checkForWhatsNewWindow() {
		final String CONFIG_LASTSHOWN = "welcome.version.lastshown";

		// Config used to store int, such as 2500.  Now, it stores a string
		// getIntParameter will return default value if parameter is string (user
		// downgraded)
		// getStringParameter will bork if parameter isn't really a string

		try {
			String lastShown = "";
			boolean bIsStringParam = true;
			try {
				lastShown = COConfigurationManager.getStringParameter(CONFIG_LASTSHOWN,
						"");
			} catch (Exception e) {
				bIsStringParam = false;
			}

			if (lastShown.length() == 0) {
				// check if we have an old style version
				int latestDisplayed = COConfigurationManager.getIntParameter(
						CONFIG_LASTSHOWN, 0);
				if (latestDisplayed > 0) {
					bIsStringParam = false;
					String s = "" + latestDisplayed;
					for (int i = 0; i < s.length(); i++) {
						if (i != 0) {
							lastShown += ".";
						}
						lastShown += s.charAt(i);
					}
				}
			}

			if (Constants.compareVersions(lastShown, Constants.getBaseVersion()) < 0) {
				new WelcomeWindow(shell);
				if (!bIsStringParam) {
					// setting parameter to a different value type makes az unhappy
					COConfigurationManager.removeParameter(CONFIG_LASTSHOWN);
				}
				COConfigurationManager.setParameter(CONFIG_LASTSHOWN,
						Constants.getBaseVersion());
				COConfigurationManager.save();
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	protected UISWTInstanceImpl getUISWTInstanceImpl() {
		return uiSWTInstanceImpl;
	}

	/**
	 * @param string
	 */
	protected void setStatusText(String string) {
		// TODO Auto-generated method stub
		if (mainStatusBar != null)
			mainStatusBar.setStatusText(string);
	}

	/**
	 * @param statustype
	 * @param string
	 * @param l
	 */
	protected void setStatusText(int statustype, String string,
			UIStatusTextClickListener l) {
		if (mainStatusBar != null) {
			mainStatusBar.setStatusText(statustype, string, l);
		}
	}

	protected SystemTraySWT getSystemTraySWT() {
		return systemTraySWT;
	}

	protected MainStatusBar getMainStatusBar() {
		return mainStatusBar;
	}

	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateShell#generateObfusticatedImage()
	public Image generateObfusticatedImage() {
		Image image;

		IView[] allViews = Tab.getAllViews();
		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];

			if (view instanceof ObfusticateTab) {
				Item tab = Tab.getTab(view);
				tab.setText(((ObfusticateTab) view).getObfusticatedHeader());
				folder.update();
			}
		}

		Rectangle clientArea = shell.getClientArea();
		image = new Image(display, clientArea.width, clientArea.height);

		GC gc = new GC(shell);
		try {
			gc.copyArea(image, clientArea.x, clientArea.y);
		} finally {
			gc.dispose();
		}

		IView currentView = getCurrentView();

		if (currentView instanceof ObfusticateImage) {
			Point ofs = shell.toDisplay(clientArea.x, clientArea.y);
			try {
				((ObfusticateImage) currentView).obfusticatedImage(image, ofs);
			} catch (Exception e) {
				Debug.out("Obfusticating " + currentView, e);
			}
		}

		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];

			if (view instanceof ObfusticateTab) {
				view.refresh();
			}
		}

		return image;
	}

	private static Point getStoredWindowSize() {
		Point size = null;

		boolean isMaximized = COConfigurationManager.getBooleanParameter(
				"window.maximized", false);
		if (isMaximized) {
			Display current = Display.getCurrent();
			if (current != null) {
				Rectangle clientArea = current.getClientArea();
				size = new Point(clientArea.width, clientArea.height);
				return size;
			}
		}

		String windowRectangle = COConfigurationManager.getStringParameter(
				"window.rectangle", null);
		if (windowRectangle != null) {
			String[] values = windowRectangle.split(",");
			if (values.length == 4) {
				try {
					size = new Point(Integer.parseInt(values[2]),
							Integer.parseInt(values[3]));
				} catch (Exception e) {
				}
			}
		}
		return size;
	}

	public static void addToVersionCheckMessage(final Map map) {
		try {
			if (window == null || window.shell == null || window.shell.isDisposed()) {
				Point size = getStoredWindowSize();
				if (size == null) {
					return;
				}

				map.put("mainwindow.w", new Long(size.x));
				map.put("mainwindow.h", new Long(size.y));
				return;
			}

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					Point size = null;

					if (window != null) {
						final Shell shell = window.getShell();
						if (shell != null && !shell.isDisposed() && !shell.getMinimized()) {
							size = shell.getSize();
						}
					}

					if (size == null) {
						size = getStoredWindowSize();
						if (size == null) {
							return;
						}
					}
					map.put("mainwindow.w", new Long(size.x));
					map.put("mainwindow.h", new Long(size.y));
				}

			}, false);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public UIFunctionsSWT getUIFunctions() {
		return uiFunctions;
	}

	public boolean isVisible(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			return bIconBarEnabled;
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		}

		return true;
	}

	public void setVisible(int windowElement, boolean value) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			setIconBarEnabled(value);
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		}
	}

	public Rectangle getMetrics(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			if (null != iconBar && null != iconBar.getComposite()) {
				return iconBar.getComposite().getBounds();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {

			return mainStatusBar.getBounds();

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_SEARCHBAR) {

			//KN: No search bar in classic UI

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {

			//KN: No tab bar in classic UI

		}
		return new Rectangle(0, 0, 0, 0);
	}

}
