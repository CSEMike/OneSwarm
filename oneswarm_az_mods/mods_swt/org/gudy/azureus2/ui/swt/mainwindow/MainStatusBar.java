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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.progress.*;
import org.gudy.azureus2.ui.swt.update.UpdateWindow;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIStatusTextClickListener;

/**
 * Moved from MainWindow and GUIUpdater
 */
public class MainStatusBar
{
	/**
	 * Warning status icon identifier
	 */
	private static final String STATUS_ICON_WARN = "sb_warning";

	private AEMonitor this_mon = new AEMonitor("MainStatusBar");

	private UpdateWindow updateWindow;

	private Composite statusBar;

	private CLabel statusText;

	private String statusTextKey = "";

	private String statusImageKey = null;

	private AZProgressBar progressBar;

	private CLabel ipBlocked;

	private CLabel srStatus;

	private CLabel natStatus;

	private CLabel dhtStatus;

	private CLabel statusDown;

	private CLabel statusUp;

	private Composite plugin_label_composite;

	private Display display;

	// For Refresh..
	private long last_sr_ratio = -1;

	private int last_sr_status = -1;

	private int lastNATstatus = -1;

	private int lastDHTstatus = -1;

	private long lastDHTcount = -1;

	private NumberFormat numberFormat;

	private OverallStats overall_stats;

	private ConnectionManager connection_manager;

	private DHTPlugin dhtPlugin;

	private GlobalManager globalManager;

	private AzureusCore azureusCore;

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

	private Image progress_error_img = null;

	private Image progress_info_img = null;

	private Image progress_viewer_img = null;

	private Image currentProgressImage = null;

	private boolean updateProgressBarDisplayQueued = false;

	protected IProgressReport latestReport = null;

	protected AEMonitor latestReport_mon = new AEMonitor("latestReport");

	/**
	 * 
	 */
	public MainStatusBar() {
		numberFormat = NumberFormat.getInstance();
		overall_stats = StatsFactory.getStats();
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		connection_manager = pm.getDefaultPluginInterface().getConnectionManager();
		PluginInterface dht_pi = pm.getPluginInterfaceByClass(DHTPlugin.class);
		if (dht_pi != null) {
			dhtPlugin = (DHTPlugin) dht_pi.getPlugin();
		}
	}

	/**
	 * 
	 * @return composite holding the statusbar
	 */
	public Composite initStatusBar(final AzureusCore core,
			final GlobalManager globalManager, Display display, final Composite parent) {
		this.display = display;
		this.globalManager = globalManager;
		this.azureusCore = core;
		this.uiFunctions = UIFunctionsManager.getUIFunctions();

		FormData formData;

		Color fgColor = parent.getForeground();

		statusBar = new Composite(parent, SWT.NONE);
		statusBar.setForeground(fgColor);
		isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));

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

		// This is the highest image displayed on the statusbar
		Image image = ImageRepository.getImage(STATUS_ICON_WARN);
		int imageHeight = (image == null) ? 20 : image.getBounds().height;

		GC gc = new GC(statusText);
		// add 6, because CLabel forces a 3 pixel indent
		int height = Math.max(imageHeight, gc.getFontMetrics().getHeight()) + 6;
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

		/*
		 * Progress reporting window image label
		 */
		progress_error_img = ImageRepository.getImage("progress_error");
		progress_info_img = ImageRepository.getImage("progress_info");
		progress_viewer_img = ImageRepository.getImage("progress_viewer");

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

		statusBar.layout();

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
		ipBlocked = new CLabelPadding(statusBar, borderFlag);
		ipBlocked.setText("{} IPs:"); //$NON-NLS-1$
		Messages.setLanguageText(ipBlocked, "MainWindow.IPs.tooltip");
		ipBlocked.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent arg0) {
				BlockedIpsWindow.showBlockedIps(azureusCore, parent.getShell());
			}
		});

		COConfigurationManager.addAndFireParameterListener("Status Area Show IPF",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						ipBlocked.setVisible(COConfigurationManager.getBooleanParameter(parameterName));
						statusBar.layout();
					}
				});

		statusDown = new CLabelPadding(statusBar, borderFlag);
		statusDown.setImage(ImageRepository.getImage("down"));
		statusDown.setText(/*MessageText.getString("ConfigView.download.abbreviated") +*/"n/a");
		Messages.setLanguageText(statusDown,
				"MainWindow.status.updowndetails.tooltip");

		Listener lStats = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.showStats();
			}
		};

		statusUp = new CLabelPadding(statusBar, borderFlag);
		statusUp.setImage(ImageRepository.getImage("up"));
		statusUp.setText(/*MessageText.getString("ConfigView.upload.abbreviated") +*/"n/a");
		Messages.setLanguageText(statusUp,
				"MainWindow.status.updowndetails.tooltip");

		statusDown.addListener(SWT.MouseDoubleClick, lStats);
		statusUp.addListener(SWT.MouseDoubleClick, lStats);

		Listener lDHT = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.showStatsDHT();
			}
		};

		dhtStatus.addListener(SWT.MouseDoubleClick, lDHT);

		Listener lSR = new Listener() {
			public void handleEvent(Event e) {

				uiFunctions.showStatsTransfers();

				OverallStats stats = StatsFactory.getStats();

				long ratio = (1000 * stats.getUploadedBytes() / (stats.getDownloadedBytes() + 1));

				if (ratio < 900) {

					Utils.launch(Constants.AZUREUS_WIKI + "Share_Ratio");
				}
			}
		};

		srStatus.addListener(SWT.MouseDoubleClick, lSR);

		Listener lNAT = new Listener() {
			public void handleEvent(Event e) {
				uiFunctions.showConfig(ConfigSection.SECTION_CONNECTION);

				if (azureusCore.getPluginManager().getDefaultPluginInterface().getConnectionManager().getNATStatus() != ConnectionManager.NAT_OK) {
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

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							SelectableSpeedMenu.invokeSlider(true);
						}
					});
				}
			});
		}

		if (bSpeedMenu) {
			final Menu menuDownSpeed = new Menu(statusBar.getShell(), SWT.POP_UP);
			menuDownSpeed.addListener(SWT.Show, new Listener() {
				public void handleEvent(Event e) {
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

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							SelectableSpeedMenu.invokeSlider(false);
						}
					});
				}
			});
		}

		PRManager.addListener(new ProgressListener());
		setProgressImage();
		return statusBar;
	}

	/**
	 * 
	 * @param keyedSentence
	 */
	public void setStatusText(String keyedSentence) {
		this.statusTextKey = keyedSentence == null ? "" : keyedSentence;
		statusImageKey = null;
		this.clickListener = null;
		if (statusTextKey.length() == 0) { // reset
			resetStatus();
		}

		updateStatusText();
	}

	//*****************************************
	/*
	 * Edits by isdal, force OneSwarm here 
	 */
	private void resetStatus() {
//		if (Constants.isCVSVersion()) {
//			statusTextKey = "MainWindow.status.unofficialversion ("
//					+ Constants.AZUREUS_VERSION + ")";
//			statusImageKey = STATUS_ICON_WARN;
//		} else if (!Constants.isOSX) { //don't show official version numbers for OSX L&F
//			statusTextKey = Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION;
//			statusImageKey = null;
//		}
		if(COConfigurationManager.getBooleanParameter("oneswarm.beta.updates")){
			statusTextKey = "OneSwarm " + Constants.AZUREUS_VERSION + "-dev: core=" + Constants.getOneSwarmAzureusModsVersion();// + " f2f=" + Constants.getF2FVersion() + " gwt=" + Constants.getWebUiVersion();
			statusImageKey = STATUS_ICON_WARN;
		} else {//if(!Constants.isOSX){
			statusTextKey = "OneSwarm " + Constants.AZUREUS_VERSION;
			statusImageKey = null;
		}
	}
	//******************************************

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
			statusImageKey = STATUS_ICON_WARN;
		}
		if (statustype == UIFunctions.STATUSICON_WARNING) {
			statusImageKey = STATUS_ICON_WARN;
		} else {
			statusImageKey = null;
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
					statusText.setImage((statusImageKey == null) ? null
							: ImageRepository.getImage(statusImageKey));
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
			statusText.setCursor(Cursors.handCursor);
			statusText.setForeground(Colors.colorWarning);
			updateStatusText();
		} else {
			statusText.setCursor(null);
			statusText.setForeground(null);
			updateStatusText();
		}
	}

	/**
	 */
	public void refreshStatusBar() {
		if (ipBlocked.isDisposed()) {
			return;
		}

		// Plugins.
		Control[] plugin_elements = this.plugin_label_composite.getChildren();
		for (int i = 0; i < plugin_elements.length; i++) {
			if (plugin_elements[i] instanceof UpdateableCLabel) {
				((UpdateableCLabel) plugin_elements[i]).checkForRefresh();
			}
		}

		// IP Filter Status Section
		IpFilter ip_filter = azureusCore.getIpFilterManager().getIPFilter();

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
					DisplayFormatters.formatDateShort(ip_filter.getLastUpdateTime())
				}));

		// SR status section

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

			srStatus.setImage(ImageRepository.getImage(imgID));

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

		// NAT status Section

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

			natStatus.setImage(ImageRepository.getImage(imgID));
			natStatus.setToolTipText(MessageText.getString(tooltipID));
			natStatus.setText(MessageText.getString(statusID));
			lastNATstatus = nat_status;
		}

		// DHT Status Section
		int dht_status = (dhtPlugin == null) ? DHTPlugin.STATUS_DISABLED
				: dhtPlugin.getStatus();
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
			Image img = ImageRepository.getImage("sb_count");
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
					img = null;
					break;
			}

			dhtStatus.setImage(img);
			lastDHTstatus = dht_status;
			lastDHTcount = dht_count;
		}

		// UL/DL Status Sections

		int dl_limit = NetworkManager.getMaxDownloadRateBPS() / 1024;

		GlobalManagerStats stats = globalManager.getStats();

		statusDown.setText((dl_limit == 0 ? "" : "[" + dl_limit + "K] ")
				+ DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
						stats.getDataReceiveRate(), stats.getProtocolReceiveRate()));

		boolean auto_up = COConfigurationManager.getBooleanParameter(TransferSpeedValidator.getActiveAutoUploadParameter(globalManager))
				&& TransferSpeedValidator.isAutoUploadAvailable(azureusCore);

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

		statusUp.setText((ul_limit_norm == 0 ? "" : "[" + ul_limit_norm + "K"
				+ seeding_only + "]")
				+ (auto_up ? "* " : " ")
				+ DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
						stats.getDataSendRate(), stats.getProtocolSendRate()));

		// End of Status Sections
		statusBar.layout();
	}

	/**
	 * @param string
	 */
	public void setDebugInfo(String string) {
		if (!statusText.isDisposed())
			statusText.setToolTipText(string);
	}

	public static interface CLabelUpdater
	{
		public void update(CLabel label);
	}

	/**
	 * CLabel that shrinks to fit text after a specific period of time.
	 * Makes textual changes less jumpy
	 * 
	 * @author TuxPaper
	 * @created Mar 21, 2006
	 *
	 */
	private class CLabelPadding
		extends CLabel
	{
		private int lastWidth = 0;

		private long widthSetOn = 0;

		private static final int KEEPWIDTHFOR_MS = 30 * 1000;

		/**
		 * Default Constructor
		 * 
		 * @param parent
		 * @param style
		 */
		public CLabelPadding(Composite parent, int style) {
			super(parent, style | SWT.CENTER);

			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER
					| GridData.VERTICAL_ALIGN_FILL);
			setLayoutData(gridData);
			setForeground(parent.getForeground());
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.custom.CLabel#computeSize(int, int, boolean)
		 */
		public Point computeSize(int wHint, int hHint, boolean changed) {
			if (!isVisible()) {
				return (new Point(0, 0));
			}
			Point pt = super.computeSize(wHint, hHint, changed);
			pt.x += 4;

			long now = System.currentTimeMillis();
			if (lastWidth > pt.x && now - widthSetOn < KEEPWIDTHFOR_MS) {
				pt.x = lastWidth;
			} else {
				if (lastWidth != pt.x)
					lastWidth = pt.x;
				widthSetOn = now;
			}

			return pt;
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
			updater.update(this);
		}
	}

	public CLabel createStatusEntry(final CLabelUpdater updater) {
		final CLabel[] result = new CLabel[1];
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					this_mon.enter();
					result[0] = new UpdateableCLabel(plugin_label_composite, borderFlag,
							updater);
					result[0].setLayoutData(new GridData(GridData.FILL_BOTH));
				} finally {
					this_mon.exit();
				}
			}
		}, false);
		return result[0];
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
		Image newProgressImage;
		if (PRManager.getReporterCount(ProgressReportingManager.COUNT_ERROR) > 0) {
			newProgressImage = progress_error_img;
		} else if (PRManager.getReporterCount(ProgressReportingManager.COUNT_ALL) > 0) {
			newProgressImage = progress_info_img;
		} else {
			newProgressImage = progress_viewer_img;
		}
		if (currentProgressImage != newProgressImage) {
			currentProgressImage = newProgressImage;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					progressViewerImageLabel.setImage(currentProgressImage);
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
		implements IProgressReportingListener, IProgressReportConstants
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

}