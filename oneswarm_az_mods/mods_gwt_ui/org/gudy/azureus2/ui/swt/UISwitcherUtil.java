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

/**
 * @author TuxPaper
 * @created Mar 21, 2007
 *
 */
public class UISwitcherUtil
{
	private static final long UPTIME_NEWUSER = 60 * 60 * 1; // 1 hour

	private static boolean NOT_GOOD_ENOUGH_FOR_AZ2_USERS_YET = true;
	
	public static ArrayList listeners = new ArrayList();
	
	public static String lastUI = null;
	
	public static void addListener(UISwitcherListener l) {
		listeners.add(l);
		if (lastUI != null) {
			triggerListeners(lastUI);
		}
	}
	
	public static void removeListener(UISwitcherListener l) {
		listeners.remove(l);
	}

	public static String openSwitcherWindow(boolean bForceAsk) {
		lastUI = _openSwitcherWindow(bForceAsk);
		triggerListeners(lastUI);
		return lastUI;
	}
	
	private static void triggerListeners(String ui) {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			UISwitcherListener l = (UISwitcherListener) array[i];
			l.uiSwitched(ui);
		}
	}
	
	public static String _openSwitcherWindow(boolean bForceAsk) {
		Class uiswClass = null;
		try {
			uiswClass = Class.forName("com.aelitis.azureus.ui.swt.shells.uiswitcher.UISwitcherWindow");
		} catch (ClassNotFoundException e1) {
		}
		if (uiswClass == null) {
			return "az2";
		}

		if (!bForceAsk) {
			String forceUI = System.getProperty("force.ui");
			if (forceUI != null) {
				COConfigurationManager.setParameter("ui", forceUI);
				return forceUI;
			}

			// Flip people who install this client over top of an existing az
			// to az3ui.  The installer will write a file to the program dir,
			// while an upgrade won't
			if (!COConfigurationManager.getBooleanParameter("installer.ui.alreadySwitched", false)
					&& FileUtil.getApplicationFile("installer.log").exists()) {
				COConfigurationManager.setParameter("installer.ui.alreadySwitched", true);
				COConfigurationManager.setParameter("ui", "az3");
				COConfigurationManager.setParameter("az3.virgin.switch", true);
				return "az3";
			}
			
			boolean asked = COConfigurationManager.getBooleanParameter("ui.asked",
					false);

			if (asked || COConfigurationManager.hasParameter("ui", true)) {
				//****************************************************************
				/*
				 * EDIT, by isdal
				 * some edits to make az2 the default ui
				 */
				//return COConfigurationManager.getStringParameter("ui", "az3");
				return COConfigurationManager.getStringParameter("ui", "az2");
				//****************************************************************
			}

			// Never auto-ask people who never have had 2.x, because they'd be scared
			// and cry at the advanced coolness of the az2 ui
			String sFirstVersion = COConfigurationManager.getStringParameter("First Recorded Version");
			if (Constants.compareVersions(sFirstVersion, "3.0.0.0") >= 0) {
				//************************************************************
				/*
				 * we acutally want people to use the az2 ui
				 */
				//COConfigurationManager.setParameter("ui", "az3");
				COConfigurationManager.setParameter("ui", "az2");
				//return "az3";
				return "az2";
			}

			// Short Circuit: We don't want to ask az2 users yet
			if (NOT_GOOD_ENOUGH_FOR_AZ2_USERS_YET) {
				COConfigurationManager.setParameter("ui", "az2");
				return "az2";
			}
		}

		// either !asked or forceAsked at this point

		try {

			final int[] result = {
				-1
			};

			final Class fuiswClass = uiswClass;

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						final Constructor constructor = fuiswClass.getConstructor(new Class[] {});

						Object object = constructor.newInstance(new Object[] {});

						Method method = fuiswClass.getMethod("open", new Class[] {});

						Object resultObj = method.invoke(object, new Object[] {});

						if (resultObj instanceof Number) {
							result[0] = ((Number) resultObj).intValue();
						}
					} catch (Exception e) {
						Debug.printStackTrace(e);
					}
				}
			}, false);

			if (result[0] == 0) {
				// Full AZ3UI
				COConfigurationManager.setParameter("ui", "az3");
				COConfigurationManager.setParameter("v3.Start Advanced", false);
			} else if (result[0] == 1) {
				// AZ3UI w/Advanced view default
				COConfigurationManager.setParameter("ui", "az3");
				COConfigurationManager.setParameter("v3.Start Advanced", true);
			} else if (result[0] == 2) {
				COConfigurationManager.setParameter("ui", "az2");
			}

			if (result[0] != -1) {
				COConfigurationManager.setParameter("ui.asked", true);
			}
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}

		return COConfigurationManager.getStringParameter("ui");
	}
	
	public static boolean isAZ3Avail() {
		Class uiswClass = null;
		try {
			uiswClass = Class.forName("com.aelitis.azureus.ui.swt.shells.uiswitcher.UISwitcherWindow");
			return true;
		} catch (ClassNotFoundException e1) {
		}
		return false;
	}
}
