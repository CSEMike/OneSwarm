/*
 * Created on 7 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.update;

import java.io.File;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.progress.IProgressReport;
import org.gudy.azureus2.ui.swt.progress.IProgressReportConstants;
import org.gudy.azureus2.ui.swt.progress.IProgressReporter;
import org.gudy.azureus2.ui.swt.progress.IProgressReporterListener;
import org.gudy.azureus2.ui.swt.progress.ProgressReportingManager;
import org.gudy.azureus2.update.CoreUpdateChecker;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIFunctionsUserPrompter;

import edu.uw.cse.netlab.reputation.GloballyAwareOneHopUnchoker;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.update.*;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateMonitor
	implements UpdateCheckInstanceListener
{
	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(UpdateMonitor.class.getName());
	
	private static final LogIDs LOGID = LogIDs.GUI;

	// ********************************************************************
	/*
	 * EDIT: by isdal
	 * decreased while in BETA, might want to increase to every hour later
	 */
	//public static final long AUTO_UPDATE_CHECK_PERIOD = 23 * 60 * 60 * 1000; // 23 hours
	public static final long AUTO_UPDATE_CHECK_PERIOD = 15 * 60 * 1000; // 15 minutes
	// *********************************************************************
	private static final String MSG_PREFIX = "UpdateMonitor.messagebox.";

	private static UpdateMonitor singleton;

	private static AEMonitor class_mon = new AEMonitor("UpdateMonitor:class");

	public static UpdateMonitor getSingleton(AzureusCore core) {
		try {
			class_mon.enter();

			if (singleton == null) {

				singleton = new UpdateMonitor(core);
			}

			return (singleton);

		} finally {

			class_mon.exit();
		}
	}

	private AzureusCore azCore;

	private UpdateWindow current_update_window;

	private UpdateCheckInstance current_update_instance;
	
	private long last_recheck_time;
	
	protected UpdateMonitor(AzureusCore _azureus_core) {
		azCore = _azureus_core;

		PluginInterface defPI = azCore.getPluginManager().getDefaultPluginInterface();
		UpdateManager um = defPI.getUpdateManager();

		um.addListener(new UpdateManagerListener() {
			public void checkInstanceCreated(UpdateCheckInstance instance) {
				instance.addListener(UpdateMonitor.this);
				
				new updateStatusChanger(instance);
			}
		});

		um.addVerificationListener(new UpdateManagerVerificationListener() {
			public boolean acceptUnVerifiedUpdate(final Update update) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					String title = MessageText.getString(MSG_PREFIX
							+ "accept.unverified.title");
					String text = MessageText.getString(MSG_PREFIX
							+ "accept.unverified.text", new String[] {
						update.getName()
					});
					return uiFunctions.promptUser(title, text, new String[] {
						MessageText.getString("Button.yes"),
						MessageText.getString("Button.no")
					}, 1, MSG_PREFIX + "accept.unverified",
							MessageText.getString("MessageBoxWindow.nomoreprompting"), false,
							0) == 0;
				}

				return false;
			}

			public void verificationFailed(final Update update, final Throwable cause) {
				final String cause_str = Debug.getNestedExceptionMessage(cause);
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					String title = MessageText.getString(MSG_PREFIX
							+ "verification.failed.title");
					String text = MessageText.getString(MSG_PREFIX
							+ "verification.failed.text", new String[] {
						update.getName(),
						cause_str
					});
					uiFunctions.promptUser(title, text, new String[] {
						MessageText.getString("Button.ok")
					}, 0, null, null, false, 0);
				}
			}
		});


		SimpleTimer.addPeriodicEvent("UpdateMon:autocheck",
				AUTO_UPDATE_CHECK_PERIOD, new TimerEventPerformer() {
					public void perform(TimerEvent ev) {
						performAutoCheck(false);
					}
				});

		// wait a bit before starting check to give rest of AZ time to initialise 
		new DelayedEvent("UpdateMon:wait", 2500, new AERunnable() {
			public void runSupport() {
				
					// check for non-writeable app dir on non-vista platforms (vista we've got a chance of
					// elevating perms when updating) and warn user. Particularly useful on OSX when
					// users haven't installed properly
			
				if ( !( Constants.isWindowsVistaOrHigher || SystemProperties.isJavaWebStartInstance())){
				
					String	app_str = SystemProperties.getApplicationPath();
					
					if ( !new File(app_str).canWrite()){
						
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						
						if ( uiFunctions != null ){
							
							if ( app_str.endsWith( File.separator )){
								
								app_str = app_str.substring(0, app_str.length()-1);
							}
							
							UIFunctionsUserPrompter prompt = 
								uiFunctions.getUserPrompter(
									MessageText.getString("updater.cant.write.to.app.title"), 
									MessageText.getString("updater.cant.write.to.app.details", new String[]{app_str}), 
									new String[]{ MessageText.getString( "Button.ok" )}, 
									0 );
							
							//prompt.setHtml( "http://a.b.c/" );
							
							prompt.setIconResource( "warning" );
							
							prompt.setRememberID( "UpdateMonitor.can.not..write.to.app.dir", false );
							
							prompt.setRememberText( MessageText.getString( "MessageBoxWindow.nomoreprompting" ));
							
							prompt.open();
						}
					}
				}
				
				performAutoCheck(true);
			}
		});
	}

	protected class updateStatusChanger
		implements IProgressReportConstants
	{
		UpdateCheckInstance instance;
		
		int check_num = 0;

		/*
		 * Creates a ProgressReporter for the update process 
		 */
		IProgressReporter updateReporter = ProgressReportingManager.getInstance().addReporter(
				MessageText.getString("UpdateWindow.title"));

		protected updateStatusChanger(UpdateCheckInstance _instance) {

			instance = _instance;
			
			/*
			 * Init reporter and allow cancel
			 */
			updateReporter.setCancelAllowed(true);
			updateReporter.setTitle(MessageText.getString("updater.progress.window.title"));
			updateReporter.appendDetailMessage(format(instance, "added"));

			String name = instance.getName();
			if (MessageText.keyExists(name)) {
				updateReporter.setMessage(MessageText.getString(name));
			} else {
				updateReporter.setMessage(name);
			}

			updateReporter.setMinimum(0);
			updateReporter.setMaximum(instance.getCheckers().length);
			updateReporter.setSelection(check_num, null);

			/*
			 * Add a listener to the reporter for a cancel event and cancel the update
			 * check instance if the event is detected
			 */
			updateReporter.addListener(new IProgressReporterListener() {

				public int report(IProgressReport progressReport) {
					if (progressReport.getReportType() == REPORT_TYPE_DONE
							|| progressReport.getReportType() == REPORT_TYPE_ERROR) {
						return RETVAL_OK_TO_DISPOSE;
					}

					if (progressReport.getReportType() == REPORT_TYPE_CANCEL) {
						if (null != instance) {
							instance.cancel();
						}
						return RETVAL_OK_TO_DISPOSE;
					}

					return RETVAL_OK;
				}

			});

			/*
			 * Add listener to the running state of the update check instance and forward
			 * to the reporter when they arrive
			 */
			instance.addListener(new UpdateCheckInstanceListener() {
				public void cancelled(UpdateCheckInstance instance) {
					updateReporter.appendDetailMessage(format(instance,
							MessageText.getString("Progress.reporting.status.canceled")));
					
						updateReporter.cancel();
				}

				public void complete(UpdateCheckInstance instance) {
					updateReporter.appendDetailMessage(format(instance,
							MessageText.getString("Progress.reporting.status.finished")));
					updateReporter.setDone();
				}
			});

			UpdateChecker[] checkers = instance.getCheckers();

			for (int i = 0; i < checkers.length; i++) {
				final UpdateChecker checker = checkers[i];

				/*
				 * Add update check listener to get running state
				 */
				checker.addListener(new UpdateCheckerListener() {

					public void cancelled(UpdateChecker checker) {
						// we don't count a cancellation as progress step
						updateReporter.appendDetailMessage(format(checker,
								MessageText.getString("Progress.reporting.status.canceled")));
					}

					public void completed(UpdateChecker checker) {

						updateReporter.appendDetailMessage(format(checker,
								MessageText.getString("Progress.reporting.status.finished")));

						updateReporter.setSelection(++check_num, null);
					}

					public void failed(UpdateChecker checker) {
						
						updateReporter.appendDetailMessage(format(checker,
								MessageText.getString("Progress.reporting.default.error")));

						updateReporter.setSelection(++check_num, null);

						// PIAMOD -- fail silently as far as the UI is concerned but record in the log
						// notify user of a failed update, use default error message
//						updateReporter.setErrorMessage(null);
						
						logger.warning("Update failed: " + checker.getComponent().getName());
					}
				});

				/*
				 * Add a listener to get the detail messages
				 */
				checker.addProgressListener(new UpdateProgressListener() {
					public void reportProgress(String str) {
						updateReporter.appendDetailMessage(format(checker, "    " + str));
					}
				});
			}
		}
	}

	// ============================================================
	// Convenience methods for formatting the detail messages for 
	// the update process
	// ============================================================	

	private String format(UpdateCheckInstance instance, String str) {
		String name = instance.getName();
		if (MessageText.keyExists(name)) {
			name = MessageText.getString(name);
		}
		return name + " - " + str;
	}

	private String format(UpdateChecker checker, String str) {
		return "    " + checker.getComponent().getName() + " - " + str;
	}

	
	protected void requestRecheck()
	{
		if (Logger.isEnabled()){
			Logger.log(new LogEvent(LOGID, "UpdateMonitor: recheck requested" ));
		}
		
		performCheck( false, true, true, null );
	}
	
	protected void performAutoCheck(final boolean start_of_day) {
		boolean check_at_start = false;
		boolean check_periodic = false;
		boolean bOldSWT = SWT.getVersion() < 3139;

		// no update checks for java web start

		if (!SystemProperties.isJavaWebStartInstance()) {

			// force check when SWT is really old
			check_at_start = COConfigurationManager.getBooleanParameter("update.start")
					|| bOldSWT;
			check_periodic = COConfigurationManager.getBooleanParameter("update.periodic");
		}

		// periodic -> check at start as well

		check_at_start = check_at_start || check_periodic;

		if ((check_at_start && start_of_day) || (check_periodic && !start_of_day)) {

			performCheck(bOldSWT, true, false, null ); // this will implicitly do usage stats

		} else {

			new DelayedEvent("UpdateMon:wait2", 5000, new AERunnable() {
				public void runSupport() {
					if (start_of_day) {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.setStatusText("");
						}
					}

					CoreUpdateChecker.doUsageStats();
				}
			});
		}
	}

	public void 
	performCheck(
		final boolean 						bForce, 
		final boolean 						automatic,
		final boolean						isRecheck,
		final UpdateCheckInstanceListener 	l ) 
	{
		long now = SystemTime.getCurrentTime();

		if ( isRecheck ){
			
			if ( last_recheck_time > now || now - last_recheck_time < 23*60*60*1000 ){
				
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID,
							"skipping recheck as consecutive recheck too soon"));

				return;
			}
			
			last_recheck_time = now;
			
		}else{
			
			last_recheck_time	= 0;
		}
		
		if (SystemProperties.isJavaWebStartInstance()) {

			// just in case we get here somehome!
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID,
						"skipping update check as java web start"));

			return;
		}

		// kill any existing update window

		if (current_update_window != null && !current_update_window.isDisposed()) {
			current_update_window.dispose();
		}

		if (current_update_instance != null) {

			current_update_instance.cancel();
		}

		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			// XXX What kind of format is this!?
			uiFunctions.setStatusText("MainWindow.status.checking ...");
		}

		// take this off this GUI thread in case it blocks for a while

		AEThread2 t = new AEThread2("UpdateMonitor:kickoff", true) {
			public void run() {
				UpdateManager um = azCore.getPluginManager().getDefaultPluginInterface().getUpdateManager();

				current_update_instance = um.createUpdateCheckInstance(bForce
						? UpdateCheckInstance.UCI_INSTALL : UpdateCheckInstance.UCI_UPDATE,
						"update.instance.update");

				if (!automatic) {

					current_update_instance.setAutomatic(false);
				}

				if (l != null) {
					current_update_instance.addListener(l);
				}
				current_update_instance.start();
			}
		};

		t.start();
	}

	public void complete(UpdateCheckInstance instance) {
		// we can get here for either update actions (triggered above) or for plugin
		// install actions (triggered by the plugin installer)

		boolean update_action = instance.getType() == UpdateCheckInstance.UCI_UPDATE;

		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.setStatusText("");
		}

		Update[] us = instance.getUpdates();

		boolean hasDownloads = false;

		// updates with zero-length downloaders exist for admin purposes
		// and shoudn't cause the window to be shown if only they exist

		for (int i = 0; i < us.length; i++) {

			if (us[i].getDownloaders().length > 0) {

				hasDownloads = true;

				break;
			}
		}

		// this controls whether or not the update window is displayed
		// note that we just don't show the window if this is set, we still do the
		// update check (as amongst other things we want ot know the latest
		// version of the core anyway	    

		if (hasDownloads) {

			// don't show another update if one's already there!

			UpdateWindow this_window = null;
			boolean autoDownload = COConfigurationManager.getBooleanParameter("update.autodownload");

			if (update_action) {
				if (!autoDownload
						&& (current_update_window == null || current_update_window.isDisposed())) {

					this_window = current_update_window = new UpdateWindow( this, azCore,instance);
				}
			} else {

				// always show an installer window

				this_window = new UpdateWindow( this, azCore, instance);
			}

			if (this_window != null) {

				for (int i = 0; i < us.length; i++) {

					if (us[i].getDownloaders().length > 0) {

						this_window.addUpdate(us[i]);
					}
				}

				this_window.updateAdditionComplete();

			} else {
				if (autoDownload) {
					new UpdateAutoDownloader(us, new UpdateAutoDownloader.cbCompletion() {
						public void allUpdatesComplete(boolean requiresRestart, boolean bHadMandatoryUpdates) {
							if (requiresRestart) {
								UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
								if (uiFunctions != null) {
									String title = MessageText.getString(MSG_PREFIX
											+ "restart.title");
									String text = MessageText.getString(MSG_PREFIX
											+ "restart.text");
									//uiFunctions.bringToFront();
									System.out.println("skipping bring to front");
									int timeout = 180000;
									//*************************************************************
									// Edit: by isdal
									// Freakin' vista UAC causes the update to hang in a UAC window
									// waiting for user confirm, change to no auto restart in... 
									if( Constants.isWindowsVistaOrHigher ){
										timeout = -1;
									}
									//*************************************************************
									if (azCore != null && !azCore.getPluginManager().isSilentRestartEnabled()) {
										timeout = -1;
									}
									if (uiFunctions.promptUser(title, text, new String[] {
										MessageText.getString("UpdateWindow.restart"),
										MessageText.getString("UpdateWindow.restartLater")
									}, 0, null, null, false, timeout) == 0) {
										uiFunctions.dispose(true, false);
									}
								}
							}else if ( bHadMandatoryUpdates ){
								
									// no restart and mandatory -> rescan for optional updates now
								
								requestRecheck();
							}
						}
					});
				} else {
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
								"UpdateMonitor: user dialog already "
										+ "in progress, updates skipped"));
				}

			}
		} else {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "UpdateMonitor: check instance "
						+ "resulted in no user-actionable updates"));

		}
	}

	public void cancelled(UpdateCheckInstance instance) {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.setStatusText("");
		}
	}
}
