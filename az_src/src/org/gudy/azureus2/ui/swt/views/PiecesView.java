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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPieceListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.views.piece.MyPieceDistributionView;
import org.gudy.azureus2.ui.swt.views.piece.PieceInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.*;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PriorityItem, SpeedItem
 */

public class PiecesView 
	extends TableViewTab<PEPiece>
	implements DownloadManagerPeerListener, 
	DownloadManagerPieceListener,
	TableDataSourceChangedListener,
	TableLifeCycleListener
{
	private static boolean registeredCoreSubViews = false;

	private final static TableColumnCore[] basicItems = {
		new PieceNumberItem(),
		new SizeItem(),
		new BlockCountItem(),
		new BlocksItem(),
		new CompletedItem(),
		new AvailabilityItem(),
		new TypeItem(),
		new ReservedByItem(),
		new WritersItem(),
		new PriorityItem(),
		new SpeedItem(),
		new RequestedItem()
	};

	public static final String MSGID_PREFIX = "PiecesView";

	DownloadManager manager;
  
	private TableViewSWTImpl<PEPiece> tv;

	private Composite legendComposite;

  
	/**
	 * Initialize
	 *
	 */
	public PiecesView() {
		super(MSGID_PREFIX);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
	public TableViewSWT<PEPiece> initYourTableView() {
		tv = new TableViewSWTImpl<PEPiece>(PEPiece.class,
				TableManager.TABLE_TORRENT_PIECES, getPropertiesPrefix(), basicItems,
				basicItems[0].getName(), SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		tv.setEnableTabViews(true);

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();
			
			if (pluginUI != null && !registeredCoreSubViews) {
				
				pluginUI.addView(TableManager.TABLE_TORRENT_PIECES,
						"PieceInfoView", PieceInfoView.class, manager);

				pluginUI.addView(TableManager.TABLE_TORRENT_PIECES,
						"MyPieceDistributionView", MyPieceDistributionView.class, manager);

				registeredCoreSubViews = true;
			}
		}

		tv.addTableDataSourceChangedListener(this, true);
		tv.addLifeCycleListener(this);

		return tv;
	}

	// @see com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener#tableDataSourceChanged(java.lang.Object)
	public void tableDataSourceChanged(Object newDataSource) {
		if (manager != null){
			manager.removePeerListener(this);
			manager.removePieceListener(this);
		}
		
		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;

  	if (manager != null) {
			manager.addPeerListener(this, false);
			manager.addPieceListener(this, false);
			addExistingDatasources();
    }
	}

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
	public void tableViewInitialized() {
		if (legendComposite != null && tv != null) {
			Composite composite = ((TableViewSWT<PEPiece>) tv).getTableComposite();

			legendComposite = Legend.createLegendComposite(composite,
					BlocksItem.colors, new String[] {
					"PiecesView.legend.requested",
					"PiecesView.legend.written",        			
					"PiecesView.legend.downloaded",
						"PiecesView.legend.incache"
					});
	}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
			manager.addPeerListener(this, false);
			manager.addPieceListener(this, false);
			addExistingDatasources();
    }
    }

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewDestroyed()
	public void tableViewDestroyed() {
		if (legendComposite != null && legendComposite.isDisposed()) {
			legendComposite.dispose();
		}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
		}
	}

	/* DownloadManagerPeerListener implementation */
	public void pieceAdded(PEPiece created) {
    tv.addDataSource(created);
	}

	public void pieceRemoved(PEPiece removed) {    
    tv.removeDataSource(removed);
	}

	public void peerAdded(PEPeer peer) {  }
	public void peerRemoved(PEPeer peer) {  }
  public void peerManagerWillBeAdded( PEPeerManager	peer_manager ){}
	public void peerManagerAdded(PEPeerManager manager) {	}
	public void peerManagerRemoved(PEPeerManager	manager) {
		tv.removeAllTableRows();
	}

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		if (manager == null || tv.isDisposed()) {
			return;
		}

		PEPiece[] dataSources = manager.getCurrentPieces();
		if (dataSources == null || dataSources.length == 0)
			return;

		tv.addDataSources(dataSources);
  	tv.processDataSourceQueue();
	}

	/**
	 * @return the manager
	 */
	public DownloadManager getManager() {
		return manager;
	}
}
