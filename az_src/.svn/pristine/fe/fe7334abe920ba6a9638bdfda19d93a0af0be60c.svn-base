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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class HealthItem
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener, TableCellSWTPaintListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	static final int COLUMN_WIDTH = 16;

	public static final String COLUMN_ID = "health";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_ESSENTIAL });
	}

	static TRHost tracker_host = null;

	/** Default Constructor */
	public HealthItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, COLUMN_WIDTH, sTableID);
		initializeAsGraphic(POSITION_LAST, COLUMN_WIDTH);
		setMinWidth(COLUMN_WIDTH);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void refresh(TableCell cell) {
		if (tracker_host == null) {
			try {
			 	tracker_host = AzureusCoreFactory.getSingleton().getTrackerHost();
			} catch (Throwable t) {
			}
			if (tracker_host == null) {
				return;
			}
		}

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


		String sHelpID = null;

		if (health == DownloadManager.WEALTH_KO) {
			sHelpID = "health.explain.red";
		} else if (health == DownloadManager.WEALTH_OK) {
			sHelpID = "health.explain.green";
		} else if (health == DownloadManager.WEALTH_NO_TRACKER) {
			sHelpID = "health.explain.blue";
		} else if (health == DownloadManager.WEALTH_NO_REMOTE) {
			sHelpID = "health.explain.yellow";
		} else if (health == DownloadManager.WEALTH_ERROR) {
		} else {
			sHelpID = "health.explain.grey";
		}

		String sToolTip = (health == DownloadManager.WEALTH_ERROR && dm != null)
				? dm.getErrorDetails() : MessageText.getString(sHelpID);
		if (ht != null)
			sToolTip += "\n" + MessageText.getString("health.explain.share");
		cell.setToolTip(sToolTip);
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		
		Comparable sortValue = cell.getSortValue();
		if (!(sortValue instanceof Long)) {
			return;
		}
		boolean isShare = false;
		long health = ((Long) sortValue).longValue();
		if (health >= 256) {
			health -= 256;
			isShare = true;
		}

		String image_name;

		if (health == DownloadManager.WEALTH_KO) {
			image_name = "st_ko";
		} else if (health == DownloadManager.WEALTH_OK) {
			image_name = "st_ok";
		} else if (health == DownloadManager.WEALTH_NO_TRACKER) {
			image_name = "st_no_tracker";
		} else if (health == DownloadManager.WEALTH_NO_REMOTE) {
			image_name = "st_no_remote";
		} else if (health == DownloadManager.WEALTH_ERROR) {
			image_name = "st_error";
		} else {
			image_name = "st_stopped";
		}

		if (isShare) {
			image_name += "_shared";
		}

		ImageLoader imageLoader = ImageLoader.getInstance();
		Image img = imageLoader.getImage(image_name);

		try {
  		Rectangle cellBounds = cell.getBounds();
  
  		if (img != null && !img.isDisposed()) {
  			Rectangle imgBounds = img.getBounds();
  			gc.drawImage(img, cellBounds.x
  					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
  					+ ((cellBounds.height - imgBounds.height) / 2));
  		}
		} finally {
			imageLoader.releaseImage(image_name);
		}
	}
}
