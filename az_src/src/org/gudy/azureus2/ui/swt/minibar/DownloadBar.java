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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.DoubleBufferedLabel;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Allan Crooks
 *
 */
public class DownloadBar extends MiniBar {
	
	private static MiniBarManager manager;
	static {
		manager = new MiniBarManager("AllTransfersBar");
	}
	
	public static MiniBarManager getManager() {
		return manager;
	}
	
	public static DownloadBar open(DownloadManager download, Shell main) {
		DownloadBar result = (DownloadBar)manager.getMiniBarForObject(download);
		if (result == null) {
			result = new DownloadBar(download, main);
		}
		return result;
	}

	public static void close(DownloadManager download) {
		DownloadBar result = (DownloadBar)manager.getMiniBarForObject(download);
		if (result != null) {result.close();}
	}
	
	private DownloadManager download;
	private DoubleBufferedLabel download_name;
	private ProgressBar progress_bar;
	private DoubleBufferedLabel down_speed;
	private DoubleBufferedLabel up_speed;
	private DoubleBufferedLabel eta;
	
	private DownloadBar(DownloadManager download, Shell main) {
		super(manager);
		this.download = download;
		this.construct(main);
	}
	
	public Object getContextObject() {return this.download;}
	
	public void beginConstruction() {
		
		// Download name.
		this.createFixedTextLabel("MinimizedWindow.name", false, false);
		this.download_name = this.createDataLabel(200);
		
		// Download progress.
		this.progress_bar = this.createPercentProgressBar(100);
		
		// Download speed.
		this.createFixedTextLabel("ConfigView.download.abbreviated", false, false);
		this.down_speed = this.createSpeedLabel();
		
		// Upload speed.
		this.createFixedTextLabel("ConfigView.upload.abbreviated", false, false);
		this.up_speed = this.createSpeedLabel();
		
		// ETA.
		this.createFixedTextLabel("TableColumn.header.eta", true, false);
		this.eta = this.createDataLabel(65);
	}
	
	public void buildMenu(Menu menu, MenuEvent menuEvent) {
		
		// Queue
		final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue");
		Utils.setMenuItemImage(itemQueue, "start");
		itemQueue.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ManagerUtils.queue(download, splash);
			}
		});
		itemQueue.setEnabled(ManagerUtils.isStartable(download));


		// Stop
		final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop");
		Utils.setMenuItemImage(itemStop, "stop");
		itemStop.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ManagerUtils.stop(download, splash);
			}
		});
		itemStop.setEnabled(ManagerUtils.isStopable(download));
		
		new MenuItem(menu, SWT.SEPARATOR);
		super.buildMenu(menu);
	}
	
	protected void refresh0() {
		DownloadManagerStats stats = download.getStats();

        download_name.setText(download.getDisplayName());
        int percent = stats.getCompleted();
        
        this.updateSpeedLabel(down_speed, stats.getDataReceiveRate(), stats.getProtocolReceiveRate());
        this.updateSpeedLabel(up_speed, stats.getDataSendRate(), stats.getProtocolSendRate());
        
        eta.setText(DisplayFormatters.formatETA(stats.getETA()));
        if (progress_bar.getSelection() != percent) {
        	progress_bar.setSelection(percent);
        	progress_bar.redraw();
        }
	}
	
	public String[] getPluginMenuIdentifiers(Object context) {
		if (context == null) {return null;}
		return new String[] {"downloadbar", "download_context"};
	}
	
	public Object getPluginMenuContextObject() {
		try {return DownloadManagerImpl.getDownloadStatic(this.download);}
		catch (DownloadException de) {return null;}
	}

}
