/*
 * Created on 12 May 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.minibar;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.DoubleBufferedLabel;
import org.gudy.azureus2.ui.swt.mainwindow.SelectableSpeedMenu;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * @author Allan Crooks
 *
 */
public class AllTransfersBar extends MiniBar {
	
	private static MiniBarManager manager;
	static {
		manager = new MiniBarManager("AllTransfersBar");
	}
	
	public static MiniBarManager getManager() {
		return manager;
	}
	
	public static AllTransfersBar getBarIfOpen(GlobalManager g_manager) {
		return (AllTransfersBar)manager.getMiniBarForObject(g_manager);
	}
	
	public static AllTransfersBar open(GlobalManager g_manager, Shell main) {
		AllTransfersBar result = getBarIfOpen(g_manager);
		if (result == null) {
			result = new AllTransfersBar(g_manager, main);
		}
		return result;
	}

	public static void close(GlobalManager g_manager) {
		AllTransfersBar result = getBarIfOpen(g_manager);
		if (result != null) {result.close();}
	}
	
	private GlobalManager g_manager;
	private DoubleBufferedLabel down_speed;
	private DoubleBufferedLabel up_speed;
	private DoubleBufferedLabel next_eta;
	
	private AllTransfersBar(GlobalManager gmanager, Shell main) {
		super(manager);
		this.g_manager = gmanager;
		this.construct(main);
	}
	
	public Object getContextObject() {return this.g_manager;}
	
	public void beginConstruction() {
		this.createFixedTextLabel("MinimizedWindow.all_transfers", false, true);
		this.createGap(40);

			// Download speed.
		
		Label dlab = this.createFixedTextLabel("ConfigView.download.abbreviated", false, false);
		this.down_speed = this.createSpeedLabel();
		
		final Menu downloadSpeedMenu = new Menu(getShell(),	SWT.POP_UP);

		downloadSpeedMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				if ( AzureusCoreFactory.isCoreRunning()){
					SelectableSpeedMenu.generateMenuItems(
						downloadSpeedMenu, AzureusCoreFactory.getSingleton(),
						g_manager, false);
				}
			}
		});

		dlab.setMenu(downloadSpeedMenu);
		down_speed.setMenu(downloadSpeedMenu);
		
			// Upload speed.
		
		Label ulab = this.createFixedTextLabel("ConfigView.upload.abbreviated", false, false);
		this.up_speed = this.createSpeedLabel();
		
		final Menu uploadSpeedMenu = new Menu(getShell(),	SWT.POP_UP);

		uploadSpeedMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				if ( AzureusCoreFactory.isCoreRunning()){
					SelectableSpeedMenu.generateMenuItems(
						uploadSpeedMenu, AzureusCoreFactory.getSingleton(),
						g_manager, true);
				}
			}
		});

		ulab.setMenu(uploadSpeedMenu);
		up_speed.setMenu(uploadSpeedMenu);
		
			// next eta
		
		this.createFixedTextLabel("TableColumn.header.eta_next", true, false);
		this.next_eta = this.createDataLabel(65);
	}
	
	public void buildMenu(Menu menu, MenuEvent menuEvent) {
		
		// Start All
		MenuItem start_all = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(start_all, "MainWindow.menu.transfers.startalltransfers");
		Utils.setMenuItemImage(start_all, "start");
		start_all.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ManagerUtils.asyncStartAll();
			}
		});
		start_all.setEnabled(true);

		// Stop All
		MenuItem stop_all = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(stop_all, "MainWindow.menu.transfers.stopalltransfers");
		Utils.setMenuItemImage(stop_all, "stop");
		stop_all.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ManagerUtils.asyncStopAll();
			}
		});
		stop_all.setEnabled(true);
		
		// Pause All
		MenuItem pause_all = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(pause_all, "MainWindow.menu.transfers.pausetransfers");
		Utils.setMenuItemImage(pause_all, "pause");
		pause_all.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ManagerUtils.asyncPause();
			}
		});
		pause_all.setEnabled(g_manager.canPauseDownloads());
		
		// Resume All
		MenuItem resume_all = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(resume_all, "MainWindow.menu.transfers.resumetransfers");
		Utils.setMenuItemImage(resume_all, "resume");
		resume_all.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ManagerUtils.asyncResume();
			}
		});
		resume_all.setEnabled(g_manager.canResumeDownloads());
		
		new MenuItem(menu, SWT.SEPARATOR);
		super.buildMenu(menu);
	}
	
	protected void refresh0() {
		GlobalManagerStats stats = g_manager.getStats();
		this.updateSpeedLabel(down_speed, stats.getDataReceiveRate(),stats.getProtocolReceiveRate());
		this.updateSpeedLabel(up_speed, stats.getDataSendRate(),stats.getProtocolSendRate());
		
		long	min_eta = Long.MAX_VALUE;
		int		num_downloading = 0;
		
		List<DownloadManager> dms = g_manager.getDownloadManagers();
		for ( DownloadManager dm: dms ){
			if ( dm.getState() == DownloadManager.STATE_DOWNLOADING ){
				
				num_downloading++;
				
				long eta = dm.getStats().getETA();
				
				if ( eta < min_eta ){
					
					min_eta = eta;
				}
			}
		}
		
		if ( min_eta == Long.MAX_VALUE ){
			
			min_eta = Constants.CRAPPY_INFINITE_AS_LONG;
		}
		next_eta.setText(num_downloading==0?"":DisplayFormatters.formatETA(min_eta));
	}
	
	public String getPluginMenuIdentifier(Object context) {
		return "transfersbar";
	}
	
	protected void storeLastLocation(Point location) {
		COConfigurationManager.setParameter("transferbar.x", location.x);
		COConfigurationManager.setParameter("transferbar.y", location.y);
	}
	
	protected Point getInitialLocation() {
		if (!COConfigurationManager.getBooleanParameter("Remember transfer bar location")) {
			return null;
		}
		if (!COConfigurationManager.hasParameter("transferbar.x", false)) {
			return null;
		}
		int x = COConfigurationManager.getIntParameter("transferbar.x");
		int y = COConfigurationManager.getIntParameter("transferbar.y");
		return new Point(x, y);
	}

}
