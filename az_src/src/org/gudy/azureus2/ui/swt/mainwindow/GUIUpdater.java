/*
 * Created on 4 mai 2004
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
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

/**
 * @author Olivier Chalouhi
 *
 */
public class GUIUpdater extends AEThread implements ParameterListener {
	private static final LogIDs LOGID = LogIDs.GUI;

	/** Calculate timer statistics for GUI update */
	private static final boolean DEBUG_TIMER = Constants.isCVSVersion();

  private MainWindow 		mainWindow;
  private Display 			display;
  
  boolean finished = false;
  boolean refreshed = true;
  
  static List refreshables = new ArrayList();
  
  int waitTime;
  int inactiveFactor;
  int mainwindowTicks;

  
  Map averageTimes = DEBUG_TIMER ? new HashMap() : null;
  
  public 
  GUIUpdater(
	MainWindow 		mainWindow) 
  {       
    super("GUI updater", true);
    this.mainWindow = mainWindow;
    this.display = mainWindow.getDisplay();
    
    setPriority(Thread.MAX_PRIORITY -2);
    COConfigurationManager.addAndFireParameterListeners(new String[] {"GUI Refresh","Refresh When Inactive"}, this);
  }

  public void runSupport() {
    while (!finished) {
      if(refreshed)
        update();
      try {
        Thread.sleep(waitTime);
      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    waitTime = COConfigurationManager.getIntParameter("GUI Refresh");
    inactiveFactor = COConfigurationManager.getIntParameter("Refresh When Inactive");
  }

  private void update() {
    refreshed = false;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
			    long lTimeStart = System.currentTimeMillis();
			    Map timeMap = DEBUG_TIMER ? new LinkedHashMap() : null;

					if (display == null || display.isDisposed())
						return;

					IView view = null;
					if (!mainWindow.getShell().isDisposed() && mainWindow.isVisible()
							&& !mainWindow.getShell().getMinimized())
				mainupdate:	{
						
						mainwindowTicks++;
						
						if(mainWindow.getShell().getDisplay().getActiveShell() != mainWindow.getShell() && mainwindowTicks % inactiveFactor != 0)
							break mainupdate;


						view = mainWindow.getCurrentView();

						if (DEBUG_TIMER)
							timeMap.put("Init", new Long(System.currentTimeMillis()));
						if (view != null) {
							view.refresh();
							if (DEBUG_TIMER) {
								String s = view.getFullTitle().replaceAll("[0-9.]++\\% : ", "");
								timeMap.put("'" + s + "' Refresh", new Long(System.currentTimeMillis()));
							}
							Tab.refresh();
							if (DEBUG_TIMER)
								timeMap.put("Tab Refresh", new Long(System.currentTimeMillis()));
						}
						
						
						MainStatusBar mainStatusBar = mainWindow.getMainStatusBar();
						if (mainStatusBar != null)
							mainStatusBar.refreshStatusBar();
					}

					if (DEBUG_TIMER)
						timeMap.put("Status Bar", new Long(System.currentTimeMillis()));
					
					SystemTraySWT systemTraySWT = mainWindow.getSystemTraySWT();
					if (systemTraySWT != null)
						systemTraySWT.update();

					if (DEBUG_TIMER)
						timeMap.put("SysTray", new Long(System.currentTimeMillis()));

					try {
						MiniBarManager.getManager().refreshAll();
					} catch (Exception e) {
						Logger.log(new LogEvent(LOGID,
								"Error while trying to update DL Bars", e));
					}
					
					TrayWindow tray = mainWindow.getTray();
					if (tray != null) {
						tray.refresh();
					}

					if (DEBUG_TIMER) {
						timeMap.put("DLBars", new Long(System.currentTimeMillis()));

						makeDebugToolTip(lTimeStart, timeMap);
					}
					
					synchronized (refreshables)
					{
						for(int i=0;i < refreshables.size();i++)
						{
							Refreshable item = (Refreshable)((WeakReference)refreshables.get(i)).get();
							if(item == null)
							{
								refreshables.remove(i--);
								continue;
							}
							item.refresh();								
						}
					}

				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "Error while trying to update GUI", e));
				} finally {
					refreshed = true;
				}
			}
		});
  }

  public void stopIt() {
    finished = true;
    COConfigurationManager.removeParameterListener("Refresh When Inactive", this);
    COConfigurationManager.removeParameterListener("GUI Refresh", this);
    COConfigurationManager.removeParameterListener("config.style.refreshMT", this);
  }
  
  private void makeDebugToolTip(long lTimeStart, Map timeMap) {
		final int IDX_AVG = 0;
		final int IDX_SIZE = 1;
		final int IDX_MAX = 2;
		final int IDX_LAST = 3;
		final int IDX_TIME = 4;

		long lastTime = lTimeStart;
		for (Iterator iter = timeMap.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();

			if (!averageTimes.containsKey(key))
				averageTimes.put(key, new Object[] { new Long(0), new Long(0),
						new Long(0), new Long(0), new Long(System.currentTimeMillis()) });

			Object[] average = (Object[]) averageTimes.get(key);

			long l = ((Long) timeMap.get(key)).longValue();
			long diff = l - lastTime;
			if (diff > 0) {
				long count = ((Long) average[IDX_SIZE]).longValue();
				// Limit to 20.  Gives slightly scewed averages, but doesn't
				// require storing all 20 values and averaging them each time
				if (count >= 20)
					count = 19;
				long lNewAverage = ((((Long) average[IDX_AVG]).longValue() * count) + diff)
						/ (count + 1);
				average[IDX_AVG] = new Long(lNewAverage);
				average[IDX_SIZE] = new Long(count + 1);
				if (diff > ((Long) average[IDX_MAX]).longValue())
					average[IDX_MAX] = new Long(diff);
				average[IDX_LAST] = new Long(diff);
				average[IDX_TIME] = new Long(System.currentTimeMillis());
			} else {
				average[IDX_LAST] = new Long(diff);
			}
			averageTimes.put(key, average);
			lastTime = l;
		}

		StringBuffer sb = new StringBuffer();
		for (Iterator iter = averageTimes.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			Object[] average = (Object[]) averageTimes.get(key);

			long lLastUpdated = ((Long) average[IDX_TIME]).longValue();
			if (System.currentTimeMillis() - lLastUpdated > 10000) {
				iter.remove();
				continue;
			}

			long lTime = ((Long) average[IDX_AVG]).longValue();
			if (lTime > 0) {
				if (sb.length() > 0)
					sb.append("\n");
				sb.append(average[IDX_AVG] + "ms avg: ");
				sb.append("[" + key + "]");
				sb.append(average[IDX_SIZE] + " samples");
				sb.append("; max:" + average[IDX_MAX]);
				sb.append("; last:" + average[IDX_LAST]);
			}
		}

		MainStatusBar mainStatusBar = mainWindow.getMainStatusBar();
		if (mainStatusBar != null)
			mainStatusBar.setDebugInfo(sb.toString());
	}
  
  
  public static void addRefreshableItem(Refreshable item)
  {
	  synchronized (refreshables)
	  {
		  refreshables.add(new WeakReference(item));
	  }
  }
}



