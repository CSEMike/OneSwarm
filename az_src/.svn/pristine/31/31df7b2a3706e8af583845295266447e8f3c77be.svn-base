/**
 * Created on Feb 9, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.donations;
 
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureDetails;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.security.CryptoManagerFactory;

/**
 * @author TuxPaper
 * @created Feb 9, 2009
 *
 */
public class DonationWindow
{
	public static boolean DEBUG = System.getProperty("donations.debug", "0").equals(
			"1");

	private static int reAskEveryHours = 96;

	private static int initialAskHours = 48;

	private static boolean pageLoadedOk = false;

	private static Shell shell = null;

	private static Browser browser;

	private static BrowserFunction browserFunction;

	public static void checkForDonationPopup() {
		if (shell != null) {
			if (DEBUG) {
				new MessageBoxShell(SWT.OK, "Donations Test", "Already Open").open(null);
			}
			return;
		}
		
		FeatureManager fm = PluginInitializer.getDefaultInterface().getUtilities().getFeatureManager();
		
		FeatureDetails[] fds = fm.getFeatureDetails( "core" );
		
		for ( FeatureDetails fd: fds ){
			
			if ( !fd.hasExpired()){
				
				return;
			}
		}

		long maxDate = COConfigurationManager.getLongParameter("donations.maxDate", 0);
		boolean force = maxDate > 0 && SystemTime.getCurrentTime() > maxDate ? true : false;	
		
		//Check if user has already donated first
		boolean alreadyDonated = COConfigurationManager.getBooleanParameter(
				"donations.donated", false);
		if (alreadyDonated && !force) {
			if (DEBUG) {
				new MessageBoxShell(SWT.OK, "Donations Test",
						"Already Donated! I like you.").open(null);
			}
			return;
		}
		
		OverallStats stats = StatsFactory.getStats();
		if (stats == null) {
			return;
		}

		long upTime = stats.getTotalUpTime();
		int hours = (int) (upTime / (60 * 60)); //secs * mins

		//Ask every DONATIONS_ASK_AFTER hours.
		int nextAsk = COConfigurationManager.getIntParameter(
				"donations.nextAskHours", 0);

		if (nextAsk == 0) {
			// First Time
			COConfigurationManager.setParameter("donations.nextAskHours", hours
					+ initialAskHours);
			COConfigurationManager.save();
			if (DEBUG) {
				new MessageBoxShell(SWT.OK, "Donations Test",
						"Newbie. You're active for " + hours + ".").open(null);
			}
			return;
		}

		if (hours < nextAsk && !force) {
			if (DEBUG) {
				new MessageBoxShell(SWT.OK, "Donations Test", "Wait "
						+ (nextAsk - hours) + ".").open(null);
			}
			return;
		}

		long minDate = COConfigurationManager.getLongParameter("donations.minDate",
				0);
		if (minDate > 0 && minDate > SystemTime.getCurrentTime()) {
			if (DEBUG) {
				new MessageBoxShell(SWT.OK, "Donation Test", "Wait "
						+ ((SystemTime.getCurrentTime() - minDate) / 1000 / 3600 / 24)
						+ " days").open(null);
			}
			return;
		}

		COConfigurationManager.setParameter("donations.nextAskHours", hours
				+ reAskEveryHours);
		COConfigurationManager.save();

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				open(false, "check");
			}
		});
	}

	public static void open(final boolean showNoLoad, final String sourceRef) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_open(showNoLoad, sourceRef);
			}
		});
	}

	public static void _open(final boolean showNoLoad, final String sourceRef) {
		if (shell != null && !shell.isDisposed()) {
			return;
		}
		final Shell parentShell = Utils.findAnyShell();
		shell = ShellFactory.createShell(parentShell, SWT.BORDER
				| SWT.APPLICATION_MODAL | SWT.TITLE);
		shell.setLayout(new FillLayout());
		if (parentShell != null) {
			parentShell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		}

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
				}
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				e.doit = false;
			}
		});
		
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (parentShell != null) {
					parentShell.setCursor(e.display.getSystemCursor(SWT.CURSOR_ARROW));
				}
				if (browserFunction != null && !browserFunction.isDisposed()) {
					browserFunction.dispose();
				}
				shell = null;
			}
		});

		browser = Utils.createSafeBrowser(shell, SWT.NONE);
		if (browser == null) {
			shell.dispose();
			return;
		}

		browser.addTitleListener(new TitleListener() {
			public void changed(TitleEvent event) {
				if (shell == null || shell.isDisposed()) {
					return;
				}
				shell.setText(event.title);
			}
		});
		
		browserFunction = new BrowserFunction(browser, "sendDonationEvent") {
			public Object function(Object[] arguments) {

				if (shell == null || shell.isDisposed()) {
					return null;
				}
				
				if (arguments == null) {
					Debug.out("Invalid sendDonationEvent null ");
					return null;
				}
				if (arguments.length < 1) {
					Debug.out("Invalid sendDonationEvent length " + arguments.length + " not 1");
					return null;
				}
				if (!(arguments[0] instanceof String)) {
					Debug.out("Invalid sendDonationEvent "
							+ (arguments[0] == null ? "NULL"
									: arguments.getClass().getSimpleName()) + " not String");
					return null;
				}

				String text = (String) arguments[0];
				if (text.contains("page-loaded")) {
					pageLoadedOk = true;
					COConfigurationManager.setParameter("donations.count",
							COConfigurationManager.getLongParameter("donations.count", 1) + 1);
					Utils.centreWindow(shell);
					if (parentShell != null) {
						parentShell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					}
					shell.open();
				} else if (text.contains("reset-ask-time")) {
					int time = reAskEveryHours;
					String[] strings = text.split(" ");
					if (strings.length > 1) {
						try {
							time = Integer.parseInt(strings[1]);
						} catch (Throwable t) {
						}
					}
					resetAskTime(time);
				} else if (text.contains("never-ask-again")) {
					neverAskAgain();
				} else if (text.contains("close")) {
					Utils.execSWTThreadLater(0, new AERunnable() {	
						public void runSupport() {
							if (shell != null && !shell.isDisposed()) {
								shell.dispose();
							}
						}
					});
				} else if (text.startsWith("open-url")) {
					String url = text.substring(9);
					Utils.launch(url);
				} else if (text.startsWith("set-size")) {
					String[] strings = text.split(" ");
					if (strings.length > 2) {
						try {
							int w = Integer.parseInt(strings[1]);
							int h = Integer.parseInt(strings[2]);

							Rectangle computeTrim = shell.computeTrim(0, 0, w, h);
							shell.setSize(computeTrim.width, computeTrim.height);
						} catch (Exception e) {
						}
					}
				}
				return null;
			}
		};

		browser.addStatusTextListener(new StatusTextListener() {
			String last = null;

			public void changed(StatusTextEvent event) {
				String text = event.text.toLowerCase();
				if (last != null && last.equals(text)) {
					return;
				}
				last = text;
				browserFunction.function(new Object[] {
					text
				});
			}
		});

		browser.addLocationListener(new LocationListener() {
			public void changing(LocationEvent event) {
			}

			public void changed(LocationEvent event) {
			}
		});

		long upTime = StatsFactory.getStats().getTotalUpTime();
		int upHours = (int) (upTime / (60 * 60)); //secs * mins
		String azid = Base32.encode(CryptoManagerFactory.getSingleton().getSecureID());
		final String url = "http://"
				+ System.getProperty("platform_address", "www.vuze.com") + ":"
				+ System.getProperty("platform_port", "80") + "/"
				+ "donate.start?locale=" + MessageText.getCurrentLocale().toString() + "&azv="
				+ Constants.AZUREUS_VERSION + "&count="
				+ COConfigurationManager.getLongParameter("donations.count", 1)
				+ "&uphours=" + upHours + "&azid=" + azid + "&sourceref="
				+ UrlUtils.encode(sourceRef);

		SimpleTimer.addEvent("donation.pageload", SystemTime.getOffsetTime(6000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (!pageLoadedOk) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									Debug.out("Page Didn't Load:" + url);
									shell.dispose();
									if (showNoLoad) {
										new MessageBoxShell(SWT.OK,
  											MessageText.getString("DonationWindow.noload.title"),
  											MessageText.getString("DonationWindow.noload.text",
														new String[] {
															url
														})).open(null);
									}
								}
							});
						}
					}
				});

		browser.setUrl(url);
	}

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	protected static void  neverAskAgain() {
		COConfigurationManager.setParameter("donations.donated", true);
		updateMinDate();
		COConfigurationManager.save();
	}

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	public static void resetAskTime() {
		resetAskTime(reAskEveryHours);
	}

	public static void resetAskTime(int askEveryHours) {
		long upTime = StatsFactory.getStats().getTotalUpTime();
		int hours = (int) (upTime / (60 * 60)); //secs * mins
		int nextAsk = hours + askEveryHours;
		COConfigurationManager.setParameter("donations.nextAskHours", nextAsk);
		COConfigurationManager.setParameter("donations.lastVersion", Constants.AZUREUS_VERSION);
		updateMinDate();
		COConfigurationManager.save();
	}

	public static void updateMinDate() {
		COConfigurationManager.setParameter("donations.minDate", SystemTime.getOffsetTime(1000l * 3600 * 24 * 30));  //30d ahead
		COConfigurationManager.setParameter("donations.maxDate", SystemTime.getOffsetTime(1000l * 3600 * 24 * 120));  //4mo ahead
		//COConfigurationManager.save();
	}
	
   //unused
	//public static void setMinDate(long timestamp) {
	//	COConfigurationManager.setParameter("donations.minDate", timestamp);
	//	COConfigurationManager.save();
	//}

	public static int getInitialAskHours() {
		return initialAskHours;
	}

	public static void setInitialAskHours(int i) {
		initialAskHours = i;
	}

	public static void main(String[] args) {
		try {
			AzureusCoreFactory.create().start();
			//checkForDonationPopup();
			open(true, "test");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Display d = Display.getDefault();
		while (true) {
			if (!d.readAndDispatch()) {
				d.sleep();
			}
		}
	}

}