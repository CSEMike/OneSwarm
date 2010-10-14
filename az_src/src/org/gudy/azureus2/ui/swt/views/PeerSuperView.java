/*
 * Created on 2 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.peer.PeerInfoView;
import org.gudy.azureus2.ui.swt.views.peer.RemotePieceDistributionView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.*;

import com.aelitis.azureus.ui.common.table.*;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PieceItem
 */

public class PeerSuperView
	extends TableViewTab
	implements GlobalManagerListener, DownloadManagerPeerListener,
	TableLifeCycleListener, TableViewSWTMenuFillListener
{
  private static final TableColumnCore[] basicItems;
  static {
	  TableColumnCore[] items = PeersView.getBasicColumnItems(TableManager.TABLE_ALL_PEERS);
	  basicItems = new TableColumnCore[items.length + 1];
	  System.arraycopy(items, 0, basicItems, 0, items.length);
	  basicItems[items.length] = new DownloadNameItem(TableManager.TABLE_ALL_PEERS);
  }  
  
  private GlobalManager g_manager;
	private TableViewSWT tv;
	private Shell shell;
	private boolean active_listener = true;


  /**
   * Initialize
   *
   */
  public PeerSuperView(GlobalManager gm) {
		tv = new TableViewSWTImpl(TableManager.TABLE_ALL_PEERS, "AllPeersView",
				basicItems, "connected_time", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		setTableView(tv);
		tv.setRowDefaultHeight(16);
		tv.setEnableTabViews(true);
		tv.setCoreTabViews(new IView[] {
			new PeerInfoView(),
			new RemotePieceDistributionView(),
			new LoggerView()
		});
		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		
		this.g_manager = gm; 
	}	
  
	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
	public void tableViewInitialized() {
		if (tv instanceof TableViewSWT) {
			shell = ((TableViewSWT)tv).getComposite().getShell();
		} else {
			shell = Utils.findAnyShell();
		}
		registerGlobalManagerListener();
	}
		
	public void tableViewDestroyed() {
		unregisterListeners();
	}

	public void fillMenu(final Menu menu) {
		PeersView.fillMenu(menu, tv, shell, false);
	}


  /* DownloadManagerPeerListener implementation */
  public void peerAdded(PEPeer created) {
    tv.addDataSource(created);
  }

  public void peerRemoved(PEPeer removed) {
    tv.removeDataSource(removed);
  }

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		if (g_manager == null || tv.isDisposed()) {
			return;
		}

		ArrayList sources = new ArrayList();
		Iterator itr = g_manager.getDownloadManagers().iterator();
		while (itr.hasNext()) {
			Object[] peers = ((DownloadManager)itr.next()).getCurrentPeers();
			if (peers != null) {
				sources.addAll(Arrays.asList(peers));
			}
		}
		if (sources.isEmpty()) {
			return;
		}
		
		tv.addDataSources(sources.toArray());
		tv.processDataSourceQueue();
	}

	private void registerGlobalManagerListener() {
		this.active_listener = false;
  		try {g_manager.addListener(this);}
  		finally {this.active_listener = true;}
		addExistingDatasources();
	}
	
	private void unregisterListeners() {
		if (this.g_manager == null) {return;}
		this.g_manager.removeListener(this);
		Iterator itr = g_manager.getDownloadManagers().iterator();
		while(itr.hasNext()) {
			DownloadManager dm = (DownloadManager)itr.next();
			downloadManagerRemoved(dm);
		}
	}
	
	public void	downloadManagerAdded(DownloadManager dm) {
		dm.addPeerListener(this, !this.active_listener);
	}
	public void	downloadManagerRemoved(DownloadManager dm) {
		dm.removePeerListener(this);
	}
	
	// Methods I have to implement but have no need for...
	public void	destroyInitiated() {}		
	public void destroyed() {}
    public void seedingStatusChanged(boolean seeding_only_mode) {}
	public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {}
	public void peerManagerAdded(PEPeerManager manager){}
	public void peerManagerRemoved(PEPeerManager manager) {}
	public void peerManagerWillBeAdded(PEPeerManager manager) {}
}
