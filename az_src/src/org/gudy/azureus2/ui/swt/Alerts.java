/*
 * Created on May 1, 2004
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
package org.gudy.azureus2.ui.swt;

import java.util.*;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIStatusTextClickListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.MessageBox;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.core3.util.*;

/**
 * Utility methods to display popup window
 * 
 * TODO: Finish up moving from LGLogger to Logger/LogAlert.  ie alert_queue
 *        could store LogAlert instead of an object array.
 */
public class Alerts
{

	private static List alert_queue = new ArrayList();

	private static AEMonitor alert_queue_mon = new AEMonitor("Alerts:Q");

	private static List alert_history = new ArrayList();

	private static AEMonitor alert_history_mon = new AEMonitor("Alerts:H");

	private static boolean initialisation_complete = false;

	private static boolean has_unshown_messages = false;

	private static transient boolean stopping;

	private static List listeners = new ArrayList();

	private Alerts() {
	}

	private static void showWarningMessageBox(Object[] relatedTo, String message) {
		showMessageBoxUsingResourceString(relatedTo, SWT.ICON_WARNING,
				"AlertMessageBox.warning", message);
	}

	private static void showMessageBoxUsingResourceString(Object[] relatedTo,
			int type, String key, String message) {
		showMessageBox(relatedTo, type, MessageText.getString(key), message, null);
	}

	// All ShowMessageBox* functions should end up here..
	private static void showMessageBox(Object[] relatedTo, final int type,
			String title, String message, String details) {
		final Display display = SWTThread.getInstance().getDisplay();

		if (stopping || display.isDisposed()) {

			try {
				alert_queue_mon.enter();

				List close_alerts = COConfigurationManager.getListParameter(
						"Alerts.raised.at.close", new ArrayList());

				Map alert_map = new HashMap();

				alert_map.put("type", new Long(type));
				alert_map.put("title", title);

				alert_map.put("message", message);

				if (details != null) {
					alert_map.put("details", details);
				}

				close_alerts.add(alert_map);

				COConfigurationManager.setParameter("Alerts.raised.at.close",
						close_alerts);

				return;
			} finally {
				alert_queue_mon.exit();
			}
		}

		if (display.isDisposed()) {
			return;
		}

		final String message2;
		if (message != null
				&& COConfigurationManager.getBooleanParameter("Show Timestamp For Alerts")) {
			message2 = "["
					+ DisplayFormatters.formatDateShort(SystemTime.getCurrentTime())
					+ "] " + message;
		} else {
			message2 = (message == null) ? "" : message;
		}

		boolean suppress_popups = COConfigurationManager.getBooleanParameter("Suppress Alerts");
		boolean use_message_box = COConfigurationManager.getBooleanParameter("Use Message Box For Popups");

		// We'll add the message to the list and then force it to be displayed if necessary.
		MessageSlideShell.recordMessage(type, title, message2 == null ? ""
				: message2, details, relatedTo);

		
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			AlertListener l = (AlertListener) iter.next();
			if (!l.allowPopup(relatedTo, type)) {
				suppress_popups = true;
				return;
			}
		}
		
		if (suppress_popups) {
			try {
				alert_queue_mon.enter();
				if (!has_unshown_messages) {
					final UIFunctions ui_functions = UIFunctionsManager.getUIFunctions();
					if (ui_functions != null) {
						ui_functions.setStatusText(UIFunctions.STATUSICON_WARNING,
								MessageText.getString("AlertMessageBox.unread"),
								new UIStatusTextClickListener() {
									public void UIStatusTextClicked() {
										MessageSlideShell.displayLastMessage(display, true);
										ui_functions.setStatusText("");
										has_unshown_messages = false;
									}
								});
						has_unshown_messages = true;
					}
				}
				return;
			} finally {
				alert_queue_mon.exit();
			}
		} else if (!use_message_box) {
			MessageSlideShell.displayLastMessage(display, true);
		} else {
			/**
			 * I don't like displaying dialog boxes with titles like "Information" and "Error".
			 * So if we are going to be displaying those message titles, then just revert back
			 * to something like "Azureus" (sounds good to me!).
			 */
			String amb_key_suffix;
			switch (type) {
				case SWT.ICON_ERROR:
					amb_key_suffix = "error";
					break;
				case SWT.ICON_INFORMATION:
					amb_key_suffix = "information";
					break;
				case SWT.ICON_WARNING:
					amb_key_suffix = "warning";
					break;
				default:
					amb_key_suffix = null;
					break;
			}

			final String title2;
			if (amb_key_suffix != null
					&& title.equals(MessageText.getString("AlertMessageBox."
							+ amb_key_suffix))) {
				title2 = Constants.AZUREUS_NAME;
			} else {
				title2 = title;
			}

			display.asyncExec(new AERunnable() {
				public void runSupport() {
					// XXX: Not sure whether findAnyShell is the best thing to use here...
					Shell s = Utils.findAnyShell();
					if (s == null) {
						return;
					}

					MessageBox mb = new MessageBox(s, type | SWT.OK);
					mb.setText(title2);
					mb.setMessage(MessageSlideShell.stripOutHyperlinks(message2));
					mb.open();
				}
			});
		} // end else
	} // end method

	public static void showErrorMessageBoxUsingResourceString(Object[] relatedTo,
			String title_key, Throwable error) {
		showErrorMessageBox(relatedTo, MessageText.getString(title_key), error);
	}

	private static void showErrorMessageBox(Object[] relatedTo, String message,
			Throwable error) {
		String error_message = Debug.getStackTrace(error);
		showMessageBox(relatedTo, SWT.ICON_ERROR,
				MessageText.getString("AlertMessageBox.error"), message + "\n"
						+ Debug.getExceptionMessage(error), error_message);
	}

	private static void showErrorMessageBox(Object[] relatedTo, String message) {
		showMessageBoxUsingResourceString(relatedTo, SWT.ICON_ERROR,
				"AlertMessageBox.error", message);
	}

	private static void showCommentMessageBox(Object[] relatedTo, String message) {
		showMessageBoxUsingResourceString(relatedTo, SWT.ICON_INFORMATION,
				"AlertMessageBox.information", message);
	}

	/**
	 * @param alert
	 *
	 * @since 3.0.0.9
	 */
	protected static void showAlert(LogAlert alert) {
		String key = (alert.err == null) ? alert.text : alert.text + ":"
				+ alert.err.toString();
		try {
			alert_history_mon.enter();

			if (!alert.repeatable) {
				if (alert_history.contains(key)) {
					return;
				}

				alert_history.add(key);

				if (alert_history.size() > 512) {
					alert_history.remove(0);
				}
			}
		} finally {
			alert_history_mon.exit();
		}

		if (alert.err == null) {
			if (alert.entryType == LogAlert.AT_INFORMATION) {
				showCommentMessageBox(alert.relatedTo, alert.text);
			} else if (alert.entryType == LogAlert.AT_WARNING) {
				showWarningMessageBox(alert.relatedTo, alert.text);
			} else {
				showErrorMessageBox(alert.relatedTo, alert.text);
			}

		} else {
			showErrorMessageBox(alert.relatedTo, alert.text, alert.err);
		}

	}

	public static void initComplete() {
		new AEThread("Init Complete") {
			public void runSupport() {
				try {
					alert_queue_mon.enter();

					initialisation_complete = true;

					for (int i = 0; i < alert_queue.size(); i++) {
						LogAlert alert = (LogAlert) alert_queue.get(i);

						showAlert(alert);
					}

					List close_alerts = COConfigurationManager.getListParameter(
							"Alerts.raised.at.close", new ArrayList());

					if (close_alerts.size() > 0) {

						COConfigurationManager.setParameter("Alerts.raised.at.close",
								new ArrayList());

						String intro = MessageText.getString("alert.raised.at.close")
								+ "\n";

						for (int i = 0; i < close_alerts.size(); i++) {

							try {
								Map alert_map = (Map) close_alerts.get(i);

								byte[] details = (byte[]) alert_map.get("details");

								showMessageBox(null, ((Long) alert_map.get("type")).intValue(),
										new String((byte[]) alert_map.get("title")), intro
												+ new String((byte[]) alert_map.get("message")),
										details == null ? null : new String(details));

							} catch (Throwable e) {

								Debug.printStackTrace(e);
							}
						}
					}

					alert_queue.clear();

				} finally {

					alert_queue_mon.exit();
				}
			}
		}.start();
	}

	public static void stopInitiated() {
		stopping = true;
	}

	public static void init() {
		Logger.addListener(new ILogAlertListener() {
			/* (non-Javadoc)
			 * @see org.gudy.azureus2.core3.logging.ILogAlertListener#alertRaised(org.gudy.azureus2.core3.logging.LogAlert)
			 */
			public void alertRaised(LogAlert alert) {
				if (!initialisation_complete) {
					try {
						alert_queue_mon.enter();

						alert_queue.add(alert);

					} finally {

						alert_queue_mon.exit();
					}

					return;
				}

				showAlert(alert);
			}
		});
	}

	
	public static void addListener(AlertListener l) {
		listeners .add(l);
	}
	
	public static interface AlertListener {
		public boolean allowPopup(Object[] relatedObjects, int configID);
	}
}
