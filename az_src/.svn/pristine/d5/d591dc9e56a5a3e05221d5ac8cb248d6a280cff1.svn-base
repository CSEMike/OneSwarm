/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

/**
 * @author TuxPaper
 * @created Nov 6, 2006
 *
 */
public class UIExitUtilsSWT
{
	private static boolean skipCloseCheck = false;
	
	private static CopyOnWriteList<canCloseListener>	listeners	= new CopyOnWriteList<canCloseListener>();
	
	public static void
	addListener(
		canCloseListener	l )
	{
		listeners.add( l );
	}
	
	public static void
	removeListener(
		canCloseListener	l )
	{
		listeners.remove( l );
	}
	
	public static void setSkipCloseCheck(boolean b) {
		skipCloseCheck = b;
	}
	
	/**
	 * @return
	 */
	public static boolean canClose(GlobalManager globalManager,
			boolean bForRestart) {
		if (skipCloseCheck) {
			return true;
		}
		
		Shell mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();
		if (mainShell != null
				&& (!mainShell.isVisible() || mainShell.getMinimized())
				&& COConfigurationManager.getBooleanParameter("Password enabled")) {

			if (!PasswordWindow.showPasswordWindow(Display.getCurrent())) {
				return false;
			}
		}
		
		
		if (COConfigurationManager.getBooleanParameter("confirmationOnExit")) {
			if (!getExitConfirmation(bForRestart)) {
				return false;
			}
		}

		for ( canCloseListener listener: listeners ){
			
			if ( !listener.canClose()){
				
				return( false );
			}
		}
		
		return true;
	}

	/**
	 * @return true, if the user choosed OK in the exit dialog
	 *
	 * @author Rene Leonhardt
	 */
	private static boolean getExitConfirmation(boolean for_restart) {
		MessageBoxShell mb = new MessageBoxShell(SWT.ICON_WARNING | SWT.YES
				| SWT.NO, for_restart ? "MainWindow.dialog.restartconfirmation"
				: "MainWindow.dialog.exitconfirmation", (String[]) null);
		mb.open(null);

		return mb.waitUntilClosed() == SWT.YES;
	}

	public static void uiShutdown() {
		// problem with closing down web start as AWT threads don't close properly
		if (SystemProperties.isJavaWebStartInstance()) {

			Thread close = new AEThread("JWS Force Terminate") {
				public void runSupport() {
					try {
						Thread.sleep(2500);

					} catch (Throwable e) {

						Debug.printStackTrace(e);
					}

					SESecurityManager.exitVM(1);
				}
			};

			close.setDaemon(true);

			close.start();
		}
	}
	
	public interface
	canCloseListener
	{
		public boolean
		canClose();
	}
}
