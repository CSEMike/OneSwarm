/*
 * File    : TrackerCellUtils.java
 * Created : Nov 24, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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

import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.client.impl.bt.TRTrackerBTScraperResponseImpl;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;

/**
 * @author TuxPaper
 *
 */
public class TrackerCellUtils
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static void updateColor(TableCell cell, DownloadManager dm, boolean show_errors ) {
		if (dm == null || cell == null)
			return;

		if ( show_errors ){
			if ( dm.isTrackerError()){
				cell.setForegroundToErrorColor();
				return;
			}
		}
		TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
		if (response instanceof TRTrackerBTScraperResponseImpl) {
			boolean bMultiHashScrapes = ((TRTrackerBTScraperResponseImpl) response).getTrackerStatus().getSupportsMultipeHashScrapes();
			Color color = (bMultiHashScrapes) ? null : Colors.grey;
			cell.setForeground(Utils.colorToIntArray(color));
		}else{
			cell.setForeground(Utils.colorToIntArray(null));
		}
	}

	public static String getTooltipText(TableCell cell, DownloadManager dm, boolean show_errors ) {
		if (dm == null || cell == null)
			return null;

		if ( show_errors ){
			if ( dm.isTrackerError()){
				return( null );
			}
		}
		String sToolTip = null;
		TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
		if (response instanceof TRTrackerBTScraperResponseImpl) {
			String sPrefix = ((TRTrackerBTScraperResponseImpl) response).getTrackerStatus().getSupportsMultipeHashScrapes()
					? "" : "No";
			sToolTip = MessageText.getString("Tracker.tooltip." + sPrefix
					+ "MultiSupport");
		}
		return sToolTip;
	}
}
