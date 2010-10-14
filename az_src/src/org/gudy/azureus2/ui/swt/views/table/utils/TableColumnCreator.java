/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.views.table.utils;

import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

/**
 * @author TuxPaper
 * @created Dec 19, 2007
 *
 */
public class TableColumnCreator
{
	public static TableColumnCore[] createIncompleteDM(String tableID) {
		return new TableColumnCore[] {
			new HealthItem(tableID),
			new RankItem(tableID),
			new NameItem(tableID),
			new CommentIconItem(tableID),
			new SizeItem(tableID),
			new DownItem(),
			new DoneItem(tableID),
			new StatusItem(tableID),
			new SeedsItem(tableID),
			new PeersItem(tableID),
			new DownSpeedItem(),
			new UpSpeedItem(tableID),
			new ETAItem(),
			new UpSpeedLimitItem(tableID),
			new TrackerStatusItem(tableID),

			// Initially Invisible
			new CompletedItem(tableID),
			new ShareRatioItem(tableID, false),
			new UpItem(tableID, false),
			new RemainingItem(),
			new PiecesItem(),
			new CompletionItem(),
			new CommentItem(tableID),
			new MaxUploadsItem(tableID),
			new TotalSpeedItem(tableID),
			new FilesDoneItem(tableID),
			new SavePathItem(tableID),
			new TorrentPathItem(tableID),
			new CategoryItem(tableID),
			new NetworksItem(tableID),
			new PeerSourcesItem(tableID),
			new AvailabilityItem(tableID),
			new AvgAvailItem(tableID),
			new SecondsSeedingItem(tableID),
			new SecondsDownloadingItem(tableID),
			new TimeSinceDownloadItem(tableID),
			new TimeSinceUploadItem(tableID),
			new OnlyCDing4Item(tableID),
			new TrackerNextAccessItem(tableID),
			new TrackerNameItem(tableID),
			new SeedToPeerRatioItem(tableID),
			new DownSpeedLimitItem(tableID),
			new SwarmAverageSpeed(tableID),
			new SwarmAverageCompletion(tableID),
			new DateAddedItem(tableID),
			new BadAvailTimeItem(tableID),
		};
	}

	public static TableColumnCore[] createCompleteDM(String tableID) {
		return new TableColumnCore[] {
			new HealthItem(tableID),
			new RankItem(tableID),
			new NameItem(tableID),
			new CommentIconItem(tableID),
			new SizeItem(tableID),
			new DoneItem(tableID),
			new StatusItem(tableID),
			new SeedsItem(tableID),
			new PeersItem(tableID),
			new UpSpeedItem(tableID),
			new ShareRatioItem(tableID, true),
			new UpItem(tableID, true),
			new UpSpeedLimitItem(tableID),

			// Initially Invisible
			new CompletedItem(tableID),
			new CommentItem(tableID),
			new MaxUploadsItem(tableID),
			new TotalSpeedItem(tableID),
			new FilesDoneItem(tableID),
			new SavePathItem(tableID),
			new TorrentPathItem(tableID),
			new CategoryItem(tableID),
			new NetworksItem(tableID),
			new PeerSourcesItem(tableID),
			new AvailabilityItem(tableID),
			new AvgAvailItem(tableID),
			new SecondsSeedingItem(tableID),
			new SecondsDownloadingItem(tableID),
			new TimeSinceUploadItem(tableID),
			new OnlyCDing4Item(tableID),
			new TrackerStatusItem(tableID),
			new TrackerNextAccessItem(tableID),
			new TrackerNameItem(tableID),
			new SeedToPeerRatioItem(tableID),
			new SwarmAverageSpeed(tableID),
			new SwarmAverageCompletion(tableID),
			new DateAddedItem(tableID),
			new DateCompletedItem(tableID),
		};
	}
}
