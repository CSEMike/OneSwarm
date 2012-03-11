/*
 * File    : PeersItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.Locale;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

/** # of Peers
 * 
 * A new object is created for each cell, so that we can listen to the
 * scrapes and update individually (and only when needed).
 * 
 * Total connected peers are left to update on INTERVAL_LIVE, as they aren't
 * very expensive.  It would probably be more expensive to hook the
 * peer listener and only refresh on peer added/removed, because that happens
 * frequently.
 *
 * @author Olivier
 * @author TuxPaper
 * 		2004/Apr/17: modified to TableCellAdapter
 * 		2005/Oct/13: Use listener to update total from scrape
 */
public class PeersItem extends CoreTableColumn implements
		TableCellAddedListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "peers";


	private static String textStarted;
	private static String textStartedOver;
	private static String textNotStarted;
	private static String textStartedNoScrape;
	private static String textNotStartedNoScrape;

	
	static {
		MessageText.addAndFireListener(new MessageTextListener() {
			public void localeChanged(Locale old_locale, Locale new_locale) {
				textStarted = MessageText.getString("Column.seedspeers.started");
				textStartedOver = MessageText.getString("Column.seedspeers.started.over");
				textNotStarted = MessageText.getString("Column.seedspeers.notstarted");
				textStartedNoScrape = MessageText.getString("Column.seedspeers.started.noscrape");
				textNotStartedNoScrape = MessageText.getString("Column.seedspeers.notstarted.noscrape");
			}
		});
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_SWARM });
	}

	/** Default Constructor */
	public PeersItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 60, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell extends AbstractTrackerCell {
		long lTotalPeers = -1;
		
		/**
		 * Initialize
		 * 
		 * @param cell
		 */
		public Cell(TableCell cell) {
			super(cell);
		}

		public void scrapeResult(TRTrackerScraperResponse response) {
			if (checkScrapeResult(response)) {
				lTotalPeers = response.getPeers();
			}
		}

		public void refresh(TableCell cell) {
			super.refresh(cell);

			long lConnectedPeers = 0;
			if (dm != null) {
				lConnectedPeers = dm.getNbPeers();

				if (lTotalPeers == -1) {
					TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
					if (response != null && response.isValid()) {
						lTotalPeers = response.getPeers();
					}
				}
			}
			
			long totalPeers = lTotalPeers;
			if (totalPeers <= 0) {
				DownloadManager dm = (DownloadManager) cell.getDataSource();
				if (dm != null) {
					totalPeers = dm.getActivationCount();
				}
			}

			long value = lConnectedPeers * 10000000;
			if (totalPeers > 0)
				value = value + totalPeers;
			if (!cell.setSortValue(value) && cell.isValid())
				return;

			int state = dm.getState();
			boolean started = state == DownloadManager.STATE_SEEDING
					|| state == DownloadManager.STATE_DOWNLOADING;
			boolean hasScrape = lTotalPeers >= 0;

			String tmp;
			if (started) {
				tmp = hasScrape ? (lConnectedPeers > lTotalPeers ? textStartedOver
						: textStarted) : textStartedNoScrape;
			} else {
				tmp = hasScrape ? textNotStarted : textNotStartedNoScrape;
			}
			
			tmp = tmp.replaceAll("%1", String.valueOf(lConnectedPeers));
			tmp = tmp.replaceAll("%2", String.valueOf(totalPeers));
			cell.setText(tmp);
		}

		public void cellHover(TableCell cell) {
			super.cellHover(cell);

			long lConnectedPeers = 0;
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				lConnectedPeers = dm.getNbPeers();
			}

			String sToolTip = lConnectedPeers + " "
					+ MessageText.getString("GeneralView.label.connected") + "\n";
			if (lTotalPeers != -1) {
				sToolTip += lTotalPeers + " "
						+ MessageText.getString("GeneralView.label.in_swarm");
			} else {
				TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
				sToolTip += "?? " + MessageText.getString("GeneralView.label.in_swarm");
				if (response != null)
					sToolTip += "(" + response.getStatusString() + ")";
			}
			
			int activationCount = dm==null?0:dm.getActivationCount();
			if (activationCount > 0) {
				sToolTip += "\n"
						+ MessageText.getString("PeerColumn.activationCount",
								new String[] { "" + activationCount });
			}
			cell.setToolTip(sToolTip);
		}
	}
}
