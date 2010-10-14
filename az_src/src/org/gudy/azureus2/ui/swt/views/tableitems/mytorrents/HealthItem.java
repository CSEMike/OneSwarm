/*
 * File    : HealthItem.java
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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.AzureusCoreFactory;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class HealthItem
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener
{
	static final int COLUMN_WIDTH = 16;

	static TRHost tracker_host = AzureusCoreFactory.getSingleton().getTrackerHost();

	/** Default Constructor */
	public HealthItem(String sTableID) {
		super("health", sTableID);
		initializeAsGraphic(POSITION_LAST, COLUMN_WIDTH);
		setMinWidth(COLUMN_WIDTH);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void refresh(TableCell cell) {

		DownloadManager dm = (DownloadManager) cell.getDataSource();
		int health;
		TRHostTorrent ht;

		if (dm == null) {
			health = 0;
			ht = null;
		} else {
			health = dm.getHealthStatus();
			ht = tracker_host.getHostTorrent(dm.getTorrent());
		}

		if (!cell.setSortValue(health + (ht == null ? 0 : 256)) && cell.isValid())
			return;

		String image_name;
		String sHelpID = null;

		if (health == DownloadManager.WEALTH_KO) {
			image_name = "st_ko";
			sHelpID = "health.explain.red";
		} else if (health == DownloadManager.WEALTH_OK) {
			image_name = "st_ok";
			sHelpID = "health.explain.green";
		} else if (health == DownloadManager.WEALTH_NO_TRACKER) {
			image_name = "st_no_tracker";
			sHelpID = "health.explain.blue";
		} else if (health == DownloadManager.WEALTH_NO_REMOTE) {
			image_name = "st_no_remote";
			sHelpID = "health.explain.yellow";
		} else if (health == DownloadManager.WEALTH_ERROR) {
			image_name = "st_error";
		} else {
			image_name = "st_stopped";
			sHelpID = "health.explain.grey";
		}

		if (ht != null) {
			image_name += "_shared";
		}

		boolean graphicWasSet = false;
		if (cell instanceof TableCellSWT) {
			graphicWasSet = ((TableCellSWT) cell).setGraphic(ImageRepository.getImage(image_name));
		} else {
			graphicWasSet = cell.setGraphic(new UISWTGraphicImpl(ImageRepository.getImage(image_name)));
		}
		if (graphicWasSet) {
			String sToolTip = (health == DownloadManager.WEALTH_ERROR)
					? dm.getErrorDetails() : MessageText.getString(sHelpID);
			if (ht != null)
				sToolTip += "\n" + MessageText.getString("health.explain.share");
			cell.setToolTip(sToolTip);
		}

	}
}
