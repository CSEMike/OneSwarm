/*
 * Created on Mar 20, 2006 6:40:14 PM
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

import java.text.NumberFormat;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.Alerts.AlertHistoryListener;
import org.gudy.azureus2.ui.swt.progress.*;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.update.UpdateWindow;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIStatusTextClickListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * Moved from MainWindow and GUIUpdater
 */
public class MainStatusBar
	implements UIUpdatable
{
	/**
	 * Warning status icon identifier
	 */
	private static final String STATUS_ICON_WARN = "sb_warning";
	
	private static final String ID = "MainStatusBar";

	private AEMonitor this_mon = new AEMonitor(ID);

	private UpdateWindow updateWindow;

	private Composite statusBar;

	private CLabel statusText;

	private String statusTextKey = "";

	private String statusImageKey = null;

	private Image statusImage = null;

	private AZProgressBar progressBar;

	private CLabelPadding ipBlocked;

	private CLabelPadding srStatus;

	private CLabelPadding natStatus;

	private CLabelPadding dhtStatus;

	private CLabelPadding statusDown;

	private CLabelPadding statusUp;

	private Composite plugin_label_composite;
	
	private ArrayList<Runnable> listRunAfterInit = new ArrayList<Runnable>();

	private Display display;

	// For Refresh..
	private long last_sr_ratio = -1;

	private int last_sr_status = -1;

	private int lastNATstatus = -1;

	private String lastNATimageID = null;

	private int lastDHTstatus = -1;

	private long lastDHTcount = -1;

	private NumberFormat numberFormat;

	private OverallStats overall_stats;

	private ConnectionManager connection_manager;

	private DHTPlugin dhtPlugin;

	private UIFunctions uiFunctions;

	private UIStatusTextClickListener clickListener;

	//	 final int borderFlag = (Constants.isOSX) ? SWT.SHADOW_NONE : SWT.SHADOW_IN;
	private static final int borderFlag = SWT.SHADOW_NONE;

	/**
	 * Just a flag to differentiate az3 from other versions; default status bar text is handled differently between versions.
	 * Specifically speaking the Vuze UI status text is just empty whereas the Classic UI status text has an icon
	 * and the application version number.
	 */
	private boolean isAZ3 = false;

	/**
	 * Just a reference to the static <code>ProgressReportingManager</code> to make the code look cleaner instead of
	 * using <code>ProgressReportingManager.getInstance().xxx()</code> everywhere.
	 */
	private ProgressReportingManager PRManager = ProgressReportingManager.getInstance();

	/**
	 * A <code>GridData</code> for the progress bar; used to dynamically provide .widthHint to the layout manager
	 */
	private GridData progressGridData = new GridData(SWT.RIGHT, SWT.CENTER,
			false, false);

	/**
	 * A clickable image label that brings up the Progress viewer 
	 */
	private CLabelPadding progressViewerImageLabel;

	private String lastProgressImageID = null;

	private boolean updateProgressBarDisplayQueued = false;

	protected IProgressReport latestReport = null;

	protected AEMonitor latestReport_mon = new AEMonitor("latestReport");

	private String lastSRimageID = null;

	private int last_dl_limit;

	private long last_rec_data = - 1;

	private long last_rec_prot;

	private long[] max_rec = { 0 };
	private long[] max_sent = { 0 };

	private Image imgRec;
	private Image imgSent;

	private Image	warningIcon;
	private Image	infoIcon;
	
	private CLabelPadding statusWarnings;

	/**
	 * 
	 */
	public MainStatusBar() {
		numberFormat = NumberFormat.getInstance();
		// Proably need to wait for core to be running to make sure dht plugin is fully avail
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				PluginManager pm = core.getPluginManager();
				connection_manager = PluginInitializer.getDefaultInterface().getConnectionManager();
				PluginInterface dht_pi = pm.getPluginInterfaceByClass(DHTPlugin.class);
				if (dht_pi != null) {
					dhtPlugin = (DHTPlugin) dht_pi.getPlugin();
				}
			}
		});
	}

	/**
	 * 
	 * @return composite holding the statusbar
	 */
	public Composite initStatusBar(final Composite parent) {
		this.display = parent.getDisplay();
		this.uiFunctions = UIFunctionsManager.getUIFunctions();
		ImageLoader imageLoader = ImageLoader.getInstance();

		FormData formData;

		Color fgColor = parent.getForeground();

		statusBar = new Composite(parent, SWT.NONE);
		statusBar.setForeground(fgColor);
		isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));
		
		statusBar.getShell().addListener(SWT.Deiconify, new Listener() {
			public void handleEvent(Event event) {
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						if (!statusBar.isDisposed()) {
							statusBar.layout();
						}
					}
				});
			}
		});
		
		GridLayout layout_status = new GridLayout();
		layout_status.numColumns = 20;
		layout_status.horizontalSpacing = 0;
		layout_status.verticalSpacing = 0;
		layout_status.marginHeight = 0;
		if (Constants.isOSX) {
			// OSX has a resize widget on the bottom right.  It's about 15px wide.
			try {
				layout_status.marginRight = 15;
			} catch (NoSuchFieldError e) {
				// Pre SWT 3.1 
				layout_status.marginWidth = 15;
			}
		} else {
			layout_status.marginWidth = 0;
		}
		statusBar.setLayout(layout_status);

		//Either the Status Text
		statusText = new CLabel(statusBar, borderFlag);
		statusText.setForeground(fgColor);
		statusText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_FILL));

		addStatusBarMenu(statusText);

		GC gc = new GC(statusText);
		// add 6, because CLabel forces a 3 pixel indent
		int height = Math.max(16, gc.getFontMetrics().getHeight()) + 6;
		gc.dispose();

		formData = new FormData();
		formData.height = height;
		formData.bottom = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
		formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
		formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
		statusBar.setLayoutData(formData);

		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				if (clickListener == null) {
					if (updateWindow != null) {
						updateWindow.show();
					}
				} else {
					clickListener.UIStatusTextClicked();
				}
			}
		};

		statusText.addListener(SWT.MouseUp, listener);
		statusText.addListener(SWT.MouseDoubleClick, listener);

		// final int progressFlag = (Constants.isOSX) ? SWT.INDETERMINATE	: SWT.HORIZONTAL;
		// KN: Don't know why OSX is treated differently but this check was already here from the previous code
		if (true == Constants.isOSX) {
			progressBar = new AZProgressBar(statusBar, true);
		} else {
			progressBar = new AZProgressBar(statusBar, false);
		}

		progressBar.setVisible(false);
		progressGridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		progressGridData.widthHint = 5;
		progressBar.setLayoutData(progressGridData);

		if ( isAZ3 ){
		
			try{
				addFeedBack();
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		/*
		 * Progress reporting window image label
		 */

		progressViewerImageLabel = new CLabelPadding(statusBar, SWT.NONE);
		// image set below after adding listener
		progressViewerImageLabel.setToolTipText(MessageText.getString("Progress.reporting.statusbar.button.tooltip"));
		progressViewerImageLabel.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				/*
				 * Opens the progress viewer if any of the reporters in the array is NOT already opened
				 * KN: TODO -- This is only a partial solution to minimize the occurrence of the main progress window
				 * opening more than once.  The one remaining case where multiple windows will still open is
				 * when you have one opened already... then run another process such as a torrent file download...
				 * at this point this new process is not in the already opened window so the check would
				 * allow the second window to open.
				 */
				IProgressReporter[] reporters = PRManager.getReportersArray(false);
				if (reporters.length == 0) {
					/*
					 * If there's nothing to see then open the window; the default widow will say there's nothing to see
					 * KN: calling isShowingEmpty return true is there is already a window opened showing the empty panel
					 */
					if (false == ProgressReporterWindow.isShowingEmpty()) {
						ProgressReporterWindow.open(reporters,
								ProgressReporterWindow.SHOW_TOOLBAR);
					}
				} else {

					for (int i = 0; i < reporters.length; i++) {
						if (false == ProgressReporterWindow.isOpened(reporters[i])) {
							ProgressReporterWindow.open(reporters,
									ProgressReporterWindow.SHOW_TOOLBAR);
							break;
						}
					}
				}
			}
		});
		progressViewerImageLabel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				imageLoader.releaseImage("progress_error");
				imageLoader.releaseImage("progress_info");
				imageLoader.releaseImage("progress_viewer");
			}
		});

		this.plugin_label_composite = new Composite(statusBar, SWT.NONE);
		this.plugin_label_composite.setForeground(fgColor);
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginTop = 0;
		gridLayout.marginLeft = 0;
		gridLayout.marginRight = 0;
		gridLayout.numColumns = 20; // Something nice and big. :)

		GridData gridData = new GridData(GridData.FILL_VERTICAL);
		gridData.heightHint = height;
		gridData.minimumHeight = height;
		plugin_label_composite.setLayout(gridLayout);
		plugin_label_composite.setLayoutData(gridData);

		srStatus = new CLabelPadding(statusBar, borderFlag);
		srStatus.setText(MessageText.getString("SpeedView.stats.ratio"));

		COConfigurationManager.addAndFireParameterListener("Status Area Show SR",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						srStatus.setVisible(COConfigurationManager.getBooleanParameter(parameterName));
						statusBar.layout();
					}
				});

		natStatus = new CLabelPadding(statusBar, borderFlag);
		natStatus.setText("");

		COConfigurationManager.addAndFireParameterListener("Status Area Show NAT",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						natStatus.setVisible(COConfigurationManager.getBooleanParameter(parameterName));
						statusBar.layout();
					}
				});

		dhtStatus = new CLabelPadding(statusBar, borderFlag);
		dhtStatus.setText("");
		dhtStatus.setToolTipText(MessageText.getString("MainWindow.dht.status.tooltip"));

		COConfigurationManager.addAndFireParameterListener("Status Area Show DDB",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						dhtStatus.setVisible(COConfigurationManager.getBooleanParameter(parameterName));
						statusBar.layout();
					}
				});
		
			// ip filters
		
		ipBlocked = new CLabelPadding(statusBar, borderFlag);
		ipBlocked.setText("{} IPs:"); //$NON-NLS-1$
		Messages.setLanguageText(ipBlocked, "MainWindow.IPs.tooltip");
		ipBlocked.addListener(SWT.MouseDoubleClick, new ListenerNeedingCoreRunning() {
			public void handleEvent(AzureusCore core, Event event) {
				BlockedIpsWindow.showBlockedIps(core, parent.getShell());
			}
		});

		final Menu menuIPFilter = new Menu(statusBar.getShell(), SWT.POP_UP);
		ipBlocked.setMenu( menuIPFilter );
		
		menuIPFilter.addListener(
			SWT.Show, 
			new Listener()
			{
				public void
				handleEvent(Event e) 
				{
					MenuItem[] oldItems = menuIPFilter.getItems();
					
					for(int i = 0; i < oldItems.length; i++){
						
						oldItems[i].dispose();
					}

					if ( !AzureusCoreFactory.isCoreRunning()){
						
						return;
					}
					
					AzureusCore azureusCore = AzureusCoreFactory.getSingleton();

					final IpFilter ip_filter = azureusCore.getIpFilterManager().getIPFilter();
					
					final MenuItem ipfEnable = new MenuItem(menuIPFilter, SWT.CHECK);
					
					ipfEnable.setSelection( ip_filter.isEnabled());
					
					Messages.setLanguageText(ipfEnable, "MyTorrentsView.menu.ipf_enable");
					
					ipfEnable.addSelectionListener(
						new SelectionAdapter() 
						{
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								ip_filter.setEnabled( ipfEnable.getSelection());
							}
						});

					final MenuItem ipfOptions = new MenuItem(menuIPFilter, SWT.PUSH);
										
					Messages.setLanguageText(ipfOptions, "ipfilter.options");
					
					ipfOptions.addSelectionListener(
						new SelectionAdapter() 
						{
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								UIFunctions uif = UIFunctionsManager.getUIFunctions();

								if (uif != null) {

									uif.openView(UIFunctions.VIEW_CONFIG, "ipfilter");
								}
							}
						});
				}
			});
				
		COConfigurationManager.addAndFireParameterListener("Status Area Show IPF",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						ipBlocked.setVisible(COConfigurationManager.getBooleanParameter(parameterName));
						statusBar.layout();
					}
				});

			// down speed
		
		
		statusDown = new CLabelPadding(statusBar, borderFlag);
		statusDown.setImage(imageLoader.getImage("down"));
		//statusDown.setText(/*MessageText.getString("ConfigView.download.abbreviated") +*/"n/a");
		Messages.setLanguageText(statusDown,
				"MainWindow.status.updowndetails.tooltip");

		Listener lStats = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.getMDI().loadEntryByID(StatsView.VIEW_ID, true, false, "transfers");
			}
		};

		statusUp = new CLabelPadding(statusBar, borderFlag);
		statusUp.setImage(imageLoader.getImage("up"));
		//statusUp.setText(/*MessageText.getString("ConfigView.upload.abbreviated") +*/"n/a");
		Messages.setLanguageText(statusUp,
				"MainWindow.status.updowndetails.tooltip");

		statusDown.addListener(SWT.MouseDoubleClick, lStats);
		statusUp.addListener(SWT.MouseDoubleClick, lStats);

		Listener lDHT = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.getMDI().loadEntryByID(StatsView.VIEW_ID, true, false, "dht");
			}
		};

		dhtStatus.addListener(SWT.MouseDoubleClick, lDHT);

		Listener lSR = new Listener() {
			public void handleEvent(Event e) {

				uiFunctions.getMDI().loadEntryByID(StatsView.VIEW_ID, true, false, "activity");

				OverallStats stats = StatsFactory.getStats();
				
				if (stats == null) {
					return;
				}

				long ratio = (1000 * stats.getUploadedBytes() / (stats.getDownloadedBytes() + 1));

				if (ratio < 900) {

					//Utils.launch(Constants.AZUREUS_WIKI + "Share_Ratio");
				}
			}
		};

		srStatus.addListener(SWT.MouseDoubleClick, lSR);

		Listener lNAT = new ListenerNeedingCoreRunning() {
			public void handleEvent(AzureusCore core, Event e) {
				uiFunctions.openView(UIFunctions.VIEW_CONFIG,
						ConfigSection.SECTION_CONNECTION);

				if (PluginInitializer.getDefaultInterface().getConnectionManager().getNATStatus() != ConnectionManager.NAT_OK) {
					Utils.launch(Constants.AZUREUS_WIKI + "NAT_problem");
				}
			}
		};

		natStatus.addListener(SWT.MouseDoubleClick, lNAT);

		boolean bSpeedMenu = COConfigurationManager.getBooleanParameter("GUI_SWT_bOldSpeedMenu");

		if (bSpeedMenu) {
			// Status Bar Menu construction
			final Menu menuUpSpeed = new Menu(statusBar.getShell(), SWT.POP_UP);
			menuUpSpeed.addListener(SWT.Show, new Listener() {
				public void handleEvent(Event e) {
					if (!AzureusCoreFactory.isCoreRunning()) {
						return;
					}
					AzureusCore core = AzureusCoreFactory.getSingleton();
					GlobalManager globalManager = core.getGlobalManager();
					
					SelectableSpeedMenu.generateMenuItems(menuUpSpeed, core,
							globalManager, true);
				}
			});
			statusUp.setMenu(menuUpSpeed);
		} else {

			statusUp.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (!(e.button == 3 || (e.button == 1 && e.stateMask == SWT.CONTROL))) {
						return;
					}
					Event event = new Event();
					event.type = SWT.MouseUp;
					event.widget = e.widget;
					event.stateMask = e.stateMask;
					event.button = e.button;
					e.widget.getDisplay().post(event);

					CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
						public void azureusCoreRunning(AzureusCore core) {
							SelectableSpeedMenu.invokeSlider(core, true);
						}
					});
				}
			});
		}

		if (bSpeedMenu) {
			final Menu menuDownSpeed = new Menu(statusBar.getShell(), SWT.POP_UP);
			menuDownSpeed.addListener(SWT.Show, new Listener() {
				public void handleEvent(Event e) {
					if (!AzureusCoreFactory.isCoreRunning()) {
						return;
					}
					AzureusCore core = AzureusCoreFactory.getSingleton();
					GlobalManager globalManager = core.getGlobalManager();

					SelectableSpeedMenu.generateMenuItems(menuDownSpeed, core,
							globalManager, false);
				}
			});
			statusDown.setMenu(menuDownSpeed);
		} else {
			statusDown.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (!(e.button == 3 || (e.button == 1 && e.stateMask == SWT.CONTROL))) {
						return;
					}
					Event event = new Event();
					event.type = SWT.MouseUp;
					event.widget = e.widget;
					event.stateMask = e.stateMask;
					event.button = e.button;
					e.widget.getDisplay().post(event);

					CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
						public void azureusCoreRunning(AzureusCore core) {
							SelectableSpeedMenu.invokeSlider(core, false);
						}
					});
				}
			});
		}

		statusWarnings = new CLabelPadding(statusBar, borderFlag);
		warningIcon = imageLoader.getImage("image.sidebar.vitality.alert");
		infoIcon 	= imageLoader.getImage("image.sidebar.vitality.info");
		updateStatusWarnings();
		Messages.setLanguageText(statusWarnings,
				"MainWindow.status.warning.tooltip");
		Alerts.addMessageHistoryListener(new AlertHistoryListener() {
			public void alertHistoryAdded(LogAlert params) {
				updateStatusWarnings();
			}
			public void alertHistoryRemoved(LogAlert alert) {
				updateStatusWarnings();
			}
		});
		statusWarnings.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				if (SystemWarningWindow.numWarningWindowsOpen > 0) {
					return;
				}
				ArrayList<LogAlert> alerts = Alerts.getUnviewedLogAlerts();
				if (alerts.size() == 0) {
					return;
				}

				Shell shell = statusWarnings.getShell();
				Rectangle bounds = statusWarnings.getClientArea();
				Point ptBottomRight = statusWarnings.toDisplay(bounds.x + bounds.width, bounds.y);
				new SystemWarningWindow(alerts.get(0), ptBottomRight, shell, 0);
			}
			
			public void mouseDown(MouseEvent e) {
			}
			
			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		
		COConfigurationManager.addAndFireParameterListener("status.rategraphs",
				new ParameterListener() {
			public void parameterChanged(String parameterName) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						boolean doRateGraphs = COConfigurationManager.getBooleanParameter("status.rategraphs");
						if (doRateGraphs) {
							if (imgRec == null || imgRec.isDisposed()) {
  							imgRec = new Image(display, 100, 20);
  							GC gc = new GC(imgRec);
  							gc.setBackground(statusDown.getBackground());
  							gc.fillRectangle(0, 0, 100, 20);
  							gc.dispose();
  							statusDown.setBackgroundImage(imgRec);
							}
							
							if (imgSent == null || imgSent.isDisposed()) {
  							imgSent = new Image(display, 100, 20);
  							GC gc = new GC(imgSent);
  							gc.setBackground(statusUp.getBackground());
  							gc.fillRectangle(0, 0, 100, 20);
  							gc.dispose();
  							statusUp.setBackgroundImage(imgSent);
							}
						} else {
							statusUp.setBackgroundImage(null);
							statusDown.setBackgroundImage(null);
							Utils.disposeSWTObjects(new Object[] { imgRec, imgSent });
							imgRec = imgSent = null;
						}
					}
				});
			}
		});
		
		/////////
		
		PRManager.addListener(new ProgressListener());
		setProgressImage();
		
		uiFunctions.getUIUpdater().addUpdater(this);
		
		ArrayList<Runnable> list;
		this_mon.enter();
		try {
			list = listRunAfterInit;
			listRunAfterInit = null;
		} finally {
			this_mon.exit();
		}
		for (Runnable runnable : list) {
			try {
				runnable.run();
			} catch (Exception e) {
				Debug.out(e);
			}
		}
		
		statusBar.layout(true);

		return statusBar;
	}

	protected void updateStatusWarnings() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (statusWarnings == null || statusWarnings.isDisposed()) {
					return;
				}
				
				ArrayList<LogAlert> alerts = Alerts.getUnviewedLogAlerts();
				int count = alerts.size();
				
				Image icon = infoIcon;
				
				for ( LogAlert alert: alerts ){
					int type = alert.getType();
					
					if ( type == LogAlert.LT_ERROR || type == LogAlert.LT_WARNING ){
						
						icon = warningIcon;
						
						break;
					}
				}
				
				if ( statusWarnings.getImage() != icon ){
					statusWarnings.setImage( icon );
				}
				
				statusWarnings.setVisible(count > 0);
				statusWarnings.setText("" + count);
				statusWarnings.layoutNow();
			}
		});
	}

	private void addFeedBack() {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						_addFeedBack();
					}
				});
			}
		});
	}

	private void _addFeedBack() {
		/*
		 * Feedback
		 * 
		 */

		// only show after restart after 15 mins uptime
		OverallStats stats = StatsFactory.getStats();

		long secs_uptime = stats.getTotalUpTime();

		long last_uptime = COConfigurationManager.getLongParameter(
				"statusbar.feedback.uptime", 0);

		if (last_uptime == 0) {

			COConfigurationManager.setParameter("statusbar.feedback.uptime",
					secs_uptime);

		} else if (secs_uptime - last_uptime > 15 * 60) {

			createStatusEntry(new CLabelUpdater() {
				public boolean update(CLabelPadding label) {
					return( false );
				}

				public void created(CLabelPadding feedback) {
					feedback.setText(MessageText.getString("statusbar.feedback"));
					
					Listener feedback_listener = new Listener() {
						public void handleEvent(Event e) {
							
							String url = "feedback.start?" + Utils.getWidgetBGColorURLParam()
							+ "&fromWeb=false&os.name=" + UrlUtils.encode(Constants.OSName)
							+ "&os.version="
							+ UrlUtils.encode(System.getProperty("os.version"))
							+ "&java.version=" + UrlUtils.encode(Constants.JAVA_VERSION);
							
							// Utils.launch( url );
							
							UIFunctionsManagerSWT.getUIFunctionsSWT().viewURL(url, null, 600,
									520, true, false);
						}
					};
					
					feedback.setToolTipText(MessageText.getString("statusbar.feedback.tooltip"));
					feedback.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
					feedback.setForeground(Colors.blue);
					feedback.addListener(SWT.MouseUp, feedback_listener);
					feedback.addListener(SWT.MouseDoubleClick, feedback_listener);
					
					feedback.setVisible(true);
				}
			});
			
		}
	}

	/**
	 * @param statusBar2
	 *
	 * @since 4.0.0.1
	 */
	private void addStatusBarMenu(Composite cSB) {
		if (!Constants.isCVSVersion()) {
			return;
		}
		Menu menu = new Menu(cSB);
		cSB.setMenu(menu);

		MenuItem itemShow = new MenuItem(menu, SWT.CASCADE);
		itemShow.setText("Show");
		Menu menuShow = new Menu(itemShow);
		itemShow.setMenu(menuShow);

		final String[] statusAreaLangs = {
			"ConfigView.section.style.status.show_sr",
			"ConfigView.section.style.status.show_nat",
			"ConfigView.section.style.status.show_ddb",
			"ConfigView.section.style.status.show_ipf",
		};
		final String[] statusAreaConfig = {
			"Status Area Show SR",
			"Status Area Show NAT",
			"Status Area Show DDB",
			"Status Area Show IPF",
		};

		for (int i = 0; i < statusAreaConfig.length; i++) {
			final String configID = statusAreaConfig[i];
			String langID = statusAreaLangs[i];

			final MenuItem item = new MenuItem(menuShow, SWT.CHECK);
			Messages.setLanguageText(item, langID);
			item.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					COConfigurationManager.setParameter(configID,
							!COConfigurationManager.getBooleanParameter(configID));
				}
			});
			menuShow.addListener(SWT.Show, new Listener() {
				public void handleEvent(Event event) {
					item.setSelection(COConfigurationManager.getBooleanParameter(configID));
				}
			});
		}
		
	}

	/**
	 * 
	 * @param keyedSentence
	 */
	public void setStatusText(String keyedSentence) {
		this.statusTextKey = keyedSentence == null ? "" : keyedSentence;
		setStatusImageKey(null);
		this.clickListener = null;
		if (statusTextKey.length() == 0) { // reset
			resetStatus();
		}

		updateStatusText();
	}
	
	private void setStatusImageKey(String newStatusImageKey) {
		if (("" + statusImageKey).equals("" + newStatusImageKey)) {
			return;
		}
		ImageLoader imageLoader = ImageLoader.getInstance();
		if (statusImageKey != null) {
			imageLoader.releaseImage(statusImageKey);
		}
		statusImageKey = newStatusImageKey;
		if (statusImageKey != null) {
			statusImage = imageLoader.getImage(statusImageKey);
		} else {
			statusImage = null;
		}
	}

	private void resetStatus() {
		if (Constants.isCVSVersion()) {
			statusTextKey = "MainWindow.status.unofficialversion ("
					+ Constants.AZUREUS_VERSION + ")";
			setStatusImageKey(STATUS_ICON_WARN);
		} else if (!Constants.isOSX && COConfigurationManager.getStringParameter("ui").equals("az2")) { //don't show official version numbers for OSX L&F
			statusTextKey = Constants.APP_NAME + " " + Constants.AZUREUS_VERSION;
			setStatusImageKey(null);
		}

	}

	/**
	 * @param statustype
	 * @param string
	 * @param l
	 */
	public void setStatusText(int statustype, String string,
			UIStatusTextClickListener l) {
		this.statusTextKey = string == null ? "" : string;

		if (statusTextKey.length() == 0) { // reset
			resetStatus();
		}

		this.clickListener = l;
		if (statustype == UIFunctions.STATUSICON_WARNING) {
			setStatusImageKey(STATUS_ICON_WARN);
		}
		if (statustype == UIFunctions.STATUSICON_WARNING) {
			setStatusImageKey(STATUS_ICON_WARN);
		} else {
			setStatusImageKey(null);
		}

		updateStatusText();
	}

	/**
	 * 
	 *
	 */
	public void updateStatusText() {
		if (display == null || display.isDisposed())
			return;
		final String text;
		if (updateWindow != null) {
			text = "MainWindow.updateavail";
		} else {
			text = this.statusTextKey;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (statusText != null && !statusText.isDisposed()) {
					statusText.setText(MessageText.getStringForSentence(text));
					statusText.setImage(statusImage);
				}
			}
		});
	}

	/**
	 * 
	 *
	 */
	public void refreshStatusText() {
		if (statusText != null && !statusText.isDisposed())
			statusText.update();
	}

	/**
	 * 
	 * @param updateWindow
	 */
	public void setUpdateNeeded(UpdateWindow updateWindow) {
		this.updateWindow = updateWindow;
		if (updateWindow != null) {
			statusText.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
			statusText.setForeground(Colors.colorWarning);
			updateStatusText();
		} else {
			statusText.setCursor(null);
			statusText.setForeground(null);
			updateStatusText();
		}
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (statusBar.isDisposed()) {
			uiFunctions.getUIUpdater().removeUpdater(this);
			return;
		}

		// Plugins.
		Control[] plugin_elements = this.plugin_label_composite.getChildren();
		for (int i = 0; i < plugin_elements.length; i++) {
			if (plugin_elements[i] instanceof UpdateableCLabel) {
				((UpdateableCLabel) plugin_elements[i]).checkForRefresh();
			}
		}

		if (ipBlocked.isVisible()) {
			updateIPBlocked();
		}
		
		if (srStatus.isVisible()) {
			updateShareRatioStatus();
		}


		if (natStatus.isVisible()) {
			updateNatStatus();
		}
		
		if (dhtStatus.isVisible()) {
			updateDHTStatus();
		}


		// UL/DL Status Sections
		if (AzureusCoreFactory.isCoreRunning()) {
			AzureusCore core = AzureusCoreFactory.getSingleton();
			GlobalManager gm = core.getGlobalManager();
			GlobalManagerStats stats = gm.getStats();

			int dl_limit = NetworkManager.getMaxDownloadRateBPS() / 1024;
			long rec_data = stats.getDataReceiveRate();
			long rec_prot = stats.getProtocolReceiveRate();
			
			if (last_dl_limit != dl_limit || last_rec_data != rec_data || last_rec_prot != rec_prot) {
				last_dl_limit = dl_limit;
				last_rec_data = rec_data;
				last_rec_prot = rec_prot;

				statusDown.setText((dl_limit == 0 ? "" : "[" + dl_limit + "K] ")
						+ DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(rec_data, rec_prot));
			}
			
			boolean auto_up = TransferSpeedValidator.isAutoSpeedActive(gm)
					&& TransferSpeedValidator.isAutoUploadAvailable(core);

			int ul_limit_norm = NetworkManager.getMaxUploadRateBPSNormal() / 1024;

			String seeding_only;
			if (NetworkManager.isSeedingOnlyUploadRate()) {
				int ul_limit_seed = NetworkManager.getMaxUploadRateBPSSeedingOnly() / 1024;
				if (ul_limit_seed == 0) {
					seeding_only = "+" + Constants.INFINITY_STRING + "K";
				} else {
					int diff = ul_limit_seed - ul_limit_norm;
					seeding_only = (diff >= 0 ? "+" : "") + diff + "K";
				}
			} else {
				seeding_only = "";
			}

			int sent_data = stats.getDataSendRate();
			if (imgRec != null && !imgRec.isDisposed()) {
				updateGraph(statusDown, imgRec, rec_data, max_rec);
				updateGraph(statusUp, imgSent, sent_data, max_sent);
			}


			statusUp.setText((ul_limit_norm == 0 ? "" : "[" + ul_limit_norm + "K"
					+ seeding_only + "]")
					+ (auto_up ? "* " : " ")
					+ DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
							sent_data, stats.getProtocolSendRate()));
		}
	}

	private void updateGraph(CLabelPadding label, Image img,
			long newVal, long[] max) {
		GC gc = new GC(img);
		try {
  		long val = newVal;
  		Rectangle bounds = img.getBounds();
  		final int padding = 2;
  		int x = bounds.width - padding - padding;
  		if (val > max[0]) {
  			int y = 20 - (int) (max[0] * 20 / val);
  			gc.setBackground(label.getBackground());
  			gc.fillRectangle(padding, 0, x, y);
  			// gc.drawImage(imgRec, 1, 0, x, 20, 0, y, x, 20 - y);
  			gc.copyArea(padding + 1, 0, x, 20, padding, y);
  			max[0] = val;
  		} else {
  			gc.copyArea(padding + 1, 0, x, 20, padding, 0);
  			// gc.drawImage(imgRec, 1, 0, x, 20, 0, 0, x, 20);
  		}
  		gc.setForeground(label.getBackground());
  		int breakPoint = 20 - (max[0] == 0 ? 0
  				: (int) (val * 20 / max[0]));
  		gc.drawLine(x, 0, x, breakPoint);
  		gc.setForeground(Colors.blues[5]);
  		gc.drawLine(x, breakPoint, x, 20);
		} finally {
  		gc.dispose();
		}
		label.redraw();
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void updateDHTStatus() {
		if (dhtPlugin == null) {
			return;
		}
		// DHT Status Section
		int dht_status = dhtPlugin.getStatus();
		long dht_count = -1;
		//boolean	reachable = false;
		if (dht_status == DHTPlugin.STATUS_RUNNING) {
			DHT[] dhts = dhtPlugin.getDHTs();

			//reachable = dhts.length > 0 && dhts[0].getTransport().isReachable();

			//if ( reachable ){
			dht_count = dhts[0].getControl().getStats().getEstimatedDHTSize();
			//}
		}

		if (lastDHTstatus != dht_status || lastDHTcount != dht_count) {
			boolean hasImage = dhtStatus.getImage() != null;
			boolean needImage = true;
			switch (dht_status) {
				case DHTPlugin.STATUS_RUNNING:

					dhtStatus.setToolTipText(MessageText.getString("MainWindow.dht.status.tooltip"));
					dhtStatus.setText(MessageText.getString("MainWindow.dht.status.users").replaceAll(
							"%1", numberFormat.format(dht_count)));

					/*
					if ( reachable ){
						dhtStatus.setImage(ImageRepository.getImage("greenled"));
						dhtStatus.setToolTipText(MessageText
								.getString("MainWindow.dht.status.tooltip"));
						dhtStatus.setText(MessageText.getString("MainWindow.dht.status.users").replaceAll("%1", numberFormat.format(dht_count)));
					} else {
						dhtStatus.setImage(ImageRepository.getImage("yellowled"));
						dhtStatus.setToolTipText(MessageText
								.getString("MainWindow.dht.status.unreachabletooltip"));
						dhtStatus.setText(MessageText
								.getString("MainWindow.dht.status.unreachable"));
					}
					*/
					break;

				case DHTPlugin.STATUS_DISABLED:
					//dhtStatus.setImage(ImageRepository.getImage("grayled"));
					dhtStatus.setText(MessageText.getString("MainWindow.dht.status.disabled"));
					break;

				case DHTPlugin.STATUS_INITALISING:
					//dhtStatus.setImage(ImageRepository.getImage("yellowled"));
					dhtStatus.setText(MessageText.getString("MainWindow.dht.status.initializing"));
					break;

				case DHTPlugin.STATUS_FAILED:
					//dhtStatus.setImage(ImageRepository.getImage("redled"));
					dhtStatus.setText(MessageText.getString("MainWindow.dht.status.failed"));
					break;

				default:
					needImage = false;
					break;
			}

			if (hasImage != needImage) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				if (needImage) {
					Image img = imageLoader.getImage("sb_count");
					dhtStatus.setImage(img);
				} else {
					imageLoader.releaseImage("sb_count");
					dhtStatus.setImage(null);
				}
			}
			lastDHTstatus = dht_status;
			lastDHTcount = dht_count;
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void updateNatStatus() {
		// NAT status Section
		if (connection_manager == null) {
			return;
		}

		int nat_status = connection_manager.getNATStatus();

		if (lastNATstatus != nat_status) {
			String imgID;
			String tooltipID;
			String statusID;

			switch (nat_status) {
				case ConnectionManager.NAT_UNKNOWN:
					imgID = "grayled";
					tooltipID = "MainWindow.nat.status.tooltip.unknown";
					statusID = "MainWindow.nat.status.unknown";
					break;

				case ConnectionManager.NAT_OK:
					imgID = "greenled";
					tooltipID = "MainWindow.nat.status.tooltip.ok";
					statusID = "MainWindow.nat.status.ok";
					break;

				case ConnectionManager.NAT_PROBABLY_OK:
					imgID = "yellowled";
					tooltipID = "MainWindow.nat.status.tooltip.probok";
					statusID = "MainWindow.nat.status.probok";
					break;

				default:
					imgID = "redled";
					tooltipID = "MainWindow.nat.status.tooltip.bad";
					statusID = "MainWindow.nat.status.bad";
					break;
			}
			
			if (!imgID.equals(lastNATimageID)) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				natStatus.setImage(imageLoader.getImage(imgID));

				if (lastNATimageID != null) {
					imageLoader.releaseImage(lastNATimageID);
				}
				lastNATimageID = imgID;
			}

			natStatus.setToolTipText(MessageText.getString(tooltipID));
			natStatus.setText(MessageText.getString(statusID));
			lastNATstatus = nat_status;
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void updateShareRatioStatus() {
		// SR status section
		
		if (overall_stats == null) {
			overall_stats = StatsFactory.getStats();
			
			if (overall_stats == null) {
				return;
			}
		}

		long ratio = (1000 * overall_stats.getUploadedBytes() / (overall_stats.getDownloadedBytes() + 1));

		int sr_status;

		if (ratio < 500) {

			sr_status = 0;

		} else if (ratio < 900) {

			sr_status = 1;

		} else {

			sr_status = 2;
		}

		if (sr_status != last_sr_status) {

			String imgID;

			switch (sr_status) {
				case 2:
					imgID = "greenled";
					break;

				case 1:
					imgID = "yellowled";
					break;

				default:
					imgID = "redled";
					break;
			}

			if (!imgID.equals(lastSRimageID)) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				srStatus.setImage(imageLoader.getImage(imgID));
				if (lastSRimageID != null) {
					imageLoader.releaseImage(lastSRimageID);
				}
				lastSRimageID  = imgID;
			}
			
			last_sr_status = sr_status;
		}

		if (ratio != last_sr_ratio) {

			String tooltipID;

			switch (sr_status) {
				case 2:
					tooltipID = "MainWindow.sr.status.tooltip.ok";
					break;

				case 1:
					tooltipID = "MainWindow.sr.status.tooltip.poor";
					break;

				default:
					tooltipID = "MainWindow.sr.status.tooltip.bad";
					break;
			}

			String ratio_str = "";

			String partial = "" + ratio % 1000;

			while (partial.length() < 3) {

				partial = "0" + partial;
			}

			ratio_str = (ratio / 1000) + "." + partial;

			srStatus.setToolTipText(MessageText.getString(tooltipID, new String[] {
				ratio_str
			}));

			last_sr_ratio = ratio;
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void updateIPBlocked() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return;
		}
		
		AzureusCore azureusCore = AzureusCoreFactory.getSingleton();

		// IP Filter Status Section
		IpFilter ip_filter = azureusCore.getIpFilterManager().getIPFilter();

		ipBlocked.setForeground( ip_filter.isEnabled()?Colors.black:Colors.grey);
		
		ipBlocked.setText("IPs: "
				+ numberFormat.format(ip_filter.getNbRanges())
				+ " - "
				+ numberFormat.format(ip_filter.getNbIpsBlockedAndLoggable())
				+ "/"
				+ numberFormat.format(ip_filter.getNbBannedIps())
				+ "/"
				+ numberFormat.format(azureusCore.getIpFilterManager().getBadIps().getNbBadIps()));
		
		ipBlocked.setToolTipText(MessageText.getString("MainWindow.IPs.tooltip",
				new String[] {
					ip_filter.isEnabled()?
					DisplayFormatters.formatDateShort(ip_filter.getLastUpdateTime()):MessageText.getString( "ipfilter.disabled" )
				}));
	}

	/**
	 * @param string
	 */
	public void setDebugInfo(String string) {
		if (statusText != null && !statusText.isDisposed())
			statusText.setToolTipText(string);
	}
	
	public boolean isMouseOver() {
		if (statusText == null || statusText.isDisposed()) {
			return false;
		}
		return statusText.getDisplay().getCursorControl() == statusText;
	}

	public static interface CLabelUpdater
	{
		public void created(CLabelPadding label);
		public boolean update(CLabelPadding label);
	}

	/**
	 * CLabel that shrinks to fit text after a specific period of time.
	 * Makes textual changes less jumpy
	 * 
	 * @author TuxPaper
	 * @created Mar 21, 2006
	 *
	 */
	public class CLabelPadding
		extends Canvas implements PaintListener
	{
		private int lastWidth = 0;

		private long widthSetOn = 0;

		private static final int KEEPWIDTHFOR_MS = 30 * 1000;
		
		String text = "";

		private Image image;

		private Image bgImage;

		/**
		 * Default Constructor
		 * 
		 * @param parent
		 * @param style
		 */
		public CLabelPadding(Composite parent, int style) {
			super(parent, style | SWT.DOUBLE_BUFFERED);

			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER
					| GridData.VERTICAL_ALIGN_FILL);
			setLayoutData(gridData);
			setForeground(parent.getForeground());
			
			addPaintListener(this);
		}

		public void paintControl(PaintEvent e) {
			Point size = getSize();
			e.gc.setAdvanced(true);
			if (bgImage != null && !bgImage.isDisposed()) {
				Rectangle bounds = bgImage.getBounds();
				if (display.getCursorControl() != this) {
					e.gc.setAlpha(100);
				}
				e.gc.drawImage(bgImage, 0, 0, bounds.width, bounds.height, 0, 2,
						size.x, size.y - 4);
				e.gc.setAlpha(255);
			}
			Rectangle clientArea = getClientArea();
			

			Image image = getImage();
			Rectangle imageBounds = null;
			if (image != null && !image.isDisposed()) {
				imageBounds = image.getBounds();
			}
			GCStringPrinter sp = new GCStringPrinter(e.gc, getText(), clientArea,
					true, true, SWT.CENTER);
			sp.calculateMetrics();
			Point textSize = sp.getCalculatedSize();

			if (imageBounds != null) {
				int pad = 2;
				int ofs = imageBounds.width + imageBounds.x;
				int xStartImage = (clientArea.width - textSize.x - ofs - pad) / 2;
				e.gc.drawImage(image, xStartImage,
						(clientArea.height / 2) - (imageBounds.height / 2));
				clientArea.x += xStartImage + ofs + pad;
				clientArea.width -= xStartImage + ofs + pad;
			} else {
				int ofs = (clientArea.width / 2) - (textSize.x / 2);
				clientArea.x += ofs;
				clientArea.width -= ofs;
			}
			sp.printString(e.gc, clientArea, SWT.LEFT);

			int x = clientArea.x + clientArea.width - 1;
			e.gc.setAlpha(20);
			e.gc.drawLine(x, 3, x, clientArea.height - 3);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.custom.CLabel#computeSize(int, int, boolean)
		 */
		public Point computeSize(int wHint, int hHint) {
			return computeSize(wHint, hHint, true);
		}

		public Point computeSize(int wHint, int hHint, boolean changed) {
			try {
				Point pt = computeSize(wHint, hHint, changed, false);

				return pt;
			} catch (Throwable t) {
				Debug.out("Error while computing size for CLabel with text:"
						+ getText() + "; " + t.toString());
				return new Point(0, 0);
			}
		}

		// @see org.eclipse.swt.widgets.Control#computeSize(int, int)
		public Point computeSize(int wHint, int hHint, boolean changed, boolean realWidth) {
			if (!isVisible()) {
				return (new Point(0, 0));
			}

			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
				return new Point(wHint, hHint);
			}
			Point pt = new Point(wHint, hHint);

			Point lastSize = new Point(0, 0);

			Image image = getImage();
			if (image != null && !image.isDisposed()) {
				Rectangle bounds = image.getBounds();
				int ofs = bounds.width + bounds.x + 5;
				lastSize.x += ofs;
				lastSize.y = bounds.height;
			}

			GC gc = new GC(this);
			GCStringPrinter sp = new GCStringPrinter(gc, getText(), new Rectangle(0,
					0, 10000, 20), true, true, SWT.LEFT);
			sp.calculateMetrics();
			Point lastTextSize = sp.getCalculatedSize();
			gc.dispose();

			lastSize.x += lastTextSize.x + 10;
			lastSize.y = Math.max(lastSize.y, lastTextSize.y);

			if (wHint == SWT.DEFAULT) {
				pt.x = lastSize.x;
			}
			if (hHint == SWT.DEFAULT) {
				pt.y = lastSize.y;
			}

			if (!realWidth) {
	  			long now = System.currentTimeMillis();
	  			if (lastWidth > pt.x && now - widthSetOn < KEEPWIDTHFOR_MS) {
	  				pt.x = lastWidth;
	  			} else {
	  				if (lastWidth != pt.x) {
	  					lastWidth = pt.x;
	  				}
	  				widthSetOn = now;
	  			}
			}

			return pt;
		}
		

		public void setImage(Image image) {
			this.image = image;
		}
		
		public Image getImage() {
			return image;
		}
		
		public void setBackgroundImage(Image image) {
			bgImage = image;
		}
		
		public Image getBackgroundImage() {
			return bgImage;
		}
		
		public String getText() {
			return text;
		}
		
		public void setText(String text) {
			if (text == null) {
				text = "";
			}
			if (text.equals(getText())) {
				return;
			}
			this.text = text;
			int oldWidth = lastWidth;
			Point pt = computeSize(SWT.DEFAULT, SWT.DEFAULT, true, true);
			if (pt.x > oldWidth && text.length() > 0) {
				statusBar.layout();
			} else if (pt.x < oldWidth) {
				Utils.execSWTThreadLater(KEEPWIDTHFOR_MS, new AERunnable() {
					public void runSupport() {
						if (statusBar == null || statusBar.isDisposed()) {
							return;
						}
						statusBar.layout();
					}
				});
			}
			redraw();
		}
		
		public void
		reset()
		{
			widthSetOn 	= 0;
			lastWidth	= 0;
		}
		
		public void layoutNow() {
			widthSetOn = 0;
			statusBar.layout();
		}

	}

	private class UpdateableCLabel
		extends CLabelPadding
	{

		private CLabelUpdater updater;

		public UpdateableCLabel(Composite parent, int style, CLabelUpdater updater) {
			super(parent, style);
			this.updater = updater;
		}

		private void checkForRefresh() {
			if ( updater.update(this)){
				layoutPluginComposite();
			}
		}
	}

	public void createStatusEntry(final CLabelUpdater updater) {
		AERunnable r = new AERunnable() {
			public void runSupport() {
				UpdateableCLabel result = new UpdateableCLabel(plugin_label_composite, borderFlag,
						updater);
				result.setLayoutData(new GridData(GridData.FILL_BOTH));
				layoutPluginComposite();
				updater.created(result);
			}
		};
		this_mon.enter();
		try {
			if (listRunAfterInit != null) {
				listRunAfterInit.add(r);
				return;
			}
		} finally {
			this_mon.exit();
		}

		Utils.execSWTThread(r);
	}

	private void
	layoutPluginComposite()
	{
		Control[] plugin_elements = this.plugin_label_composite.getChildren();
		for (int i = 0; i < plugin_elements.length; i++) {
			if (plugin_elements[i] instanceof UpdateableCLabel) {
				((UpdateableCLabel) plugin_elements[i]).reset();
			}
		}
		statusBar.layout();
	}
	// =============================================================
	// Below code are ProgressBar/Status text specific
	// =============================================================	
	/**
	 * Show or hide the Progress Bar
	 * @param state
	 */
	private void showProgressBar(boolean state) {
		/*
		 * We show/hide the progress bar simply by setting the .widthHint and letting the statusBar handle the layout
		 */
		if (true == state && false == progressBar.isVisible()) {
			progressGridData.widthHint = 100;
			progressBar.setVisible(true);
			statusBar.layout();
		} else if (false == state && true == progressBar.isVisible()) {
			progressBar.setVisible(false);
			progressGridData.widthHint = 0;
			statusBar.layout();
		}
	}

	/**
	 * Updates the display of the ProgressBar and/or the status text
	 * @param pReport the <code>ProgressReport</code> containing the information
	 * to display; can be <code>null</code> in which case the status text and progress bar will be reset to default states
	 */
	private void updateProgressBarDisplay(IProgressReport pReport) {
		latestReport_mon.enter();
		try {
			latestReport = pReport;
		} finally {
			latestReport_mon.exit();
		}
		if (null == progressBar || progressBar.isDisposed()
				|| updateProgressBarDisplayQueued) {
			return;
		}
		updateProgressBarDisplayQueued = true;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				latestReport_mon.enter();
				try {
					updateProgressBarDisplayQueued = false;

					if ((null == progressBar || true == progressBar.isDisposed())) {
						return;
					}

					if (null != latestReport) {
						/*
						 * Pass the values through to the progressbar
						 */
						progressBar.setMinimum(latestReport.getMinimum());
						progressBar.setMaximum(latestReport.getMaximum());
						progressBar.setIndeterminate(latestReport.isIndeterminate());
						progressBar.setPercentage(latestReport.getPercentage());
						showProgressBar(true);

						/*
						 * Update status text
						 */
						if (true == isAZ3) {
							statusText.setText(latestReport.getName());
						} else {
							setStatusText(latestReport.getName());
						}
					}

					else {
						/*
						 * Since the pReport is null then reset progress display appropriately
						 */
						showProgressBar(false);

						if (true == isAZ3) {
							statusText.setText("");
						} else {
							setStatusText(null);
						}
					}
				} finally {
					latestReport_mon.exit();
				}

			}

		}, true);

	}

	private void setProgressImage() {
		String imageID;

		if (PRManager.getReporterCount(ProgressReportingManager.COUNT_ERROR) > 0) {
			imageID = "progress_error";
		} else if (PRManager.getReporterCount(ProgressReportingManager.COUNT_ALL) > 0) {
			imageID = "progress_info";
		} else {
			imageID = "progress_viewer";
		}

		if (!imageID.equals(lastProgressImageID)) {
			final String fImageID = imageID;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (progressViewerImageLabel.isDisposed()) {
						return;
					}
					ImageLoader imageLoader = ImageLoader.getInstance();
					progressViewerImageLabel.setImage(imageLoader.getImage(fImageID));
					if (lastProgressImageID != null) {
						imageLoader.releaseImage(lastProgressImageID);
					}
					lastProgressImageID  = fImageID;
				}
			});
		}
	}

	/**
	 * A listener that listens to any changes notified from the <code>ProgressReportingManager</code> and
	 * accordingly update the progress bar and/or the status text area.
	 * @author knguyen
	 *
	 */
	private class ProgressListener
		implements IProgressReportingListener
	{

		public int reporting(int eventType, IProgressReporter reporter) {

			/*
			 * Show the appropriate image based on the content of the reporting manager
			 */
			setProgressImage();

			if (null == reporter) {
				return RETVAL_OK;
			}

			if (MANAGER_EVENT_REMOVED == eventType) {
				updateFromPrevious();
			} else if (MANAGER_EVENT_ADDED == eventType
					|| MANAGER_EVENT_UPDATED == eventType) {
				/*
				 * Get a ProgressReport to ensure all data is consistent
				 */
				IProgressReport pReport = reporter.getProgressReport();

				/*
				 * Pops up the ProgressReportingWindow to show this report if it is an error report;
				 * this is to help catch the users attention
				 */
				if (true == pReport.isInErrorState()) {
					
					if(true == "reporterType_updater".equals(pReport.getReporterType())){
						/*
						 * Suppressing the pop-up for update-related errors
						 */
						return RETVAL_OK;
					}
					
					final IProgressReporter final_reporter = reporter;
					

					/*
					 * The new window is opened only if there is not one already showing the same reporter
					 */
					if (false == ProgressReporterWindow.isOpened(final_reporter)) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if ( !ProgressReporterWindow.isOpened(final_reporter)){
									ProgressReporterWindow.open(final_reporter,
											ProgressReporterWindow.NONE);
								}
							}
						}, true);
					}
				}

				/*
				 * If this reporter is not active then get the previous reporter that is still active and display info from that
				 */
				if (false == pReport.isActive()) {
					updateFromPrevious();
				} else {
					update(pReport);
				}
			}

			return RETVAL_OK;
		}

		private void update(final IProgressReport pReport) {

			if (null == pReport) {
				updateProgressBarDisplay(null);
				return;
			}

			/*
			 * If there is at least 2 reporters still active then show the progress bar as indeterminate
			 * and display the text from the current reporter
			 */
			if (true == PRManager.hasMultipleActive()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						setStatusText(pReport.getName());
						progressBar.setIndeterminate(true);
						showProgressBar(true);
					}
				}, true);
			} else {
				updateProgressBarDisplay(pReport);
			}
		}

		private void updateFromPrevious() {
			/*
			 * Get the previous reporter that is still active
			 */
			IProgressReporter previousReporter = PRManager.getNextActiveReporter();

			/*
			 * If null then we reset the status text and the progress bar
			 */
			if (null != previousReporter) {
				update(previousReporter.getProgressReport());
			} else {
				update(null);
			}
		}
	}

	public Rectangle getBounds() {
		if (null != statusBar) {
			return statusBar.getBounds();
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return ID;
	}
}