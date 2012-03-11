/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.ui.swt.views.tableitems.files.FirstPieceItem;

/**
 * @author TuxPaper
 * @created Mar 21, 2007
 *
 */
public class UISwitcherUtil
{
	private static final long UPTIME_NEWUSER = 60 * 60 * 1; // 1 hour

	private static ArrayList listeners = new ArrayList();

	private static String switchedToUI = null;

	public static void addListener(UISwitcherListener l) {
		listeners.add(l);
		if (switchedToUI != null) {
			triggerListeners(switchedToUI);
		}
	}

	public static void removeListener(UISwitcherListener l) {
		listeners.remove(l);
	}

	public static void openSwitcherWindow() {
		_openSwitcherWindow();
	}

	public static void triggerListeners(String ui) {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			UISwitcherListener l = (UISwitcherListener) array[i];
			l.uiSwitched(ui);
		}
	}

	public static String calcUIMode() {
		// Can't use Constants.isSafeMode - it's not set by the time we
		// get here.
		if ("1".equals(System.getProperty("azureus.safemode"))) {
			// If we are in safe-mode, prefer the classic UI - less likely to cause problems.
			return "az2";
		}

		String lastUI = COConfigurationManager.getStringParameter("ui", "az2");
		COConfigurationManager.setParameter("lastUI", lastUI);

		String forceUI = System.getProperty("force.ui");
		if (forceUI != null) {
			COConfigurationManager.setParameter("ui", forceUI);
			return forceUI;
		}

		// Flip people who install this client over top of an existing az
		// to az3ui.  The installer will write a file to the program dir,
		// while an upgrade won't
		boolean installLogExists = FileUtil.getApplicationFile("installer.log").exists();
		boolean alreadySwitched = COConfigurationManager.getBooleanParameter(
				"installer.ui.alreadySwitched", false);
		if (!alreadySwitched && installLogExists) {
			COConfigurationManager.setParameter("installer.ui.alreadySwitched", true);
			COConfigurationManager.setParameter("ui", "az3");
			COConfigurationManager.setParameter("az3.virgin.switch", true);

			return "az3";
		}

		boolean asked = COConfigurationManager.getBooleanParameter("ui.asked",
				false);

		if (asked || COConfigurationManager.hasParameter("ui", true)) {
			return COConfigurationManager.getStringParameter("ui", "az3");
		}

		// Never auto-ask people who never have had 2.x, because they'd be scared
		// and cry at the advanced coolness of the az2 ui
		String sFirstVersion = COConfigurationManager.getStringParameter("First Recorded Version");
		if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {
			COConfigurationManager.setParameter("ui", "az3");
			return "az3";
		}

		// For new users who install pre v3 Azureus, and then immediately upgrade 
		// to v3:
		// Give them v3 by default since they've (in theory) never used az2ui
		// Note: Users with any existing 3.x.x.x version will not get because
		//       they have the "ui" parameter set and there's logic above to
		//       exit early.
		try {
			Map map = FileUtil.readResilientConfigFile("azureus.statistics");
			if (map != null) {
				Map overallMap = (Map) map.get("all");
				if (overallMap != null) {
					long uptime = 0;
					Object uptimeObject = overallMap.get("uptime");
					if (uptimeObject instanceof Number) {
						uptime = ((Number) uptimeObject).longValue();
					}
					// during a previous azureus, we may have screwed up uptime
					// and it might be zero.. so check for that..
					if (uptime < UPTIME_NEWUSER && uptime >= 0) {
						COConfigurationManager.setParameter("ui", "az3");
						COConfigurationManager.setParameter("az3.virgin.switch", true);
						COConfigurationManager.setParameter("az3.switch.immediate", true);
						return "az3";
					}
				}
			}
		} catch (Exception e) {
			Debug.out(e);
			// ignore
		}

		// Short Circuit: We don't want to ask az2 users yet
		COConfigurationManager.setParameter("ui", "az2");
		return "az2";
	}

	public static void _openSwitcherWindow() {
		Class uiswClass = null;
		try {
			uiswClass = Class.forName("com.aelitis.azureus.ui.swt.shells.uiswitcher.UISwitcherWindow");
		} catch (ClassNotFoundException e1) {
		}
		if (uiswClass == null) {
			return;
		}

		// either !asked or forceAsked at this point

		try {

			final Constructor constructor = uiswClass.getConstructor(new Class[] {});

			Object object = constructor.newInstance(new Object[] {});

			Method method = uiswClass.getMethod("open", new Class[] {});

			method.invoke(object, new Object[] {});

		} catch (Exception e) {
			Debug.printStackTrace(e);
		}

		return;
	}
}
