/*
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.net.URL;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerTrackerListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * Base cell class for cells listening to the tracker listener
 */
abstract class AbstractTrackerCell implements TableCellRefreshListener,
		TableCellToolTipListener, TableCellDisposeListener,
		DownloadManagerTrackerListener {

	TableCell cell;

	DownloadManager dm;

	/**
	 * Initialize
	 * 
	 * @param cell
	 */
	public AbstractTrackerCell(TableCell cell) {
		this.cell = cell;
		cell.addListeners(this);

		dm = (DownloadManager) cell.getDataSource();
		if (dm == null)
			return;
		dm.addTrackerListener(this);
	}

	public void announceResult(TRTrackerAnnouncerResponse response) {
		// Don't care about announce
	}

	public boolean checkScrapeResult(final TRTrackerScraperResponse response) {
		if (response != null) {
			TableCell cell_ref = cell;
			
			if ( cell_ref == null ){
				return( false );
			}
			// Exit if this scrape result is not from the tracker currently being used.
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null || dm != this.dm)
				return false;

			TOTorrent	torrent = dm.getTorrent();
			
			if ( torrent == null ){
				return( false );
			}
			URL announceURL = torrent.getAnnounceURL();
			URL responseURL = response.getURL();
			if (announceURL != responseURL && announceURL != null
					&& responseURL != null
					&& !announceURL.toString().equals(responseURL.toString()))
				return false;
			

			cell_ref.invalidate();
			
			return response.isValid();
		}

		return false;
	}

	public void refresh(TableCell cell) {
		DownloadManager oldDM = dm;
		dm = (DownloadManager) cell.getDataSource();

		// datasource changed, change listener
		if (dm != oldDM) {
			if (oldDM != null)
				oldDM.removeTrackerListener(this);

			if (dm != null)
				dm.addTrackerListener(this);
		}
	}

	public void cellHover(TableCell cell) {
	}

	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}

	public void dispose(TableCell cell) {
		if (dm != null)
			dm.removeTrackerListener(this);
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null && dm != this.dm)
			dm.removeTrackerListener(this);
		dm = null;
		cell = null;
	}
}
