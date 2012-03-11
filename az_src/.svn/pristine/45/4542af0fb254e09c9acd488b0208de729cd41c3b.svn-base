/*
 * File    : SeedingRankColumnListener.java
 * Created : Sep 27, 2005
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

package com.aelitis.azureus.plugins.startstoprules.defaultplugin;

import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/** A "My Torrents" column for displaying Seeding Rank.
 */
public class SeedingRankColumnListener implements
		TableCellRefreshListener, COConfigurationListener {
	private Map downloadDataMap;

	private PluginConfig pluginConfig;

	private int minTimeAlive;

	private int iRankType;

	private boolean bDebugLog;

	public SeedingRankColumnListener(Map _downloadDataMap, PluginConfig pc) {
		downloadDataMap = _downloadDataMap;
		pluginConfig = pc;
		COConfigurationManager.addListener(this);
		configurationSaved();
	}

	public void refresh(TableCell cell) {
		Download dl = (Download) cell.getDataSource();
		if (dl == null)
			return;

		DefaultRankCalculator dlData = null;
		Object o = cell.getSortValue();
		if (o instanceof DefaultRankCalculator)
			dlData = (DefaultRankCalculator) o;
		else {
			dlData = (DefaultRankCalculator) downloadDataMap.get(dl);
			cell.setSortValue(dlData);
		}
		if (dlData == null)
			return;
			

		long sr = dl.getSeedingRank();

		String sText = "";
		if (sr >= 0) {
			if (dlData.getCachedIsFP())
				sText += MessageText.getString("StartStopRules.firstPriority") + " ";

			if (iRankType == StartStopRulesDefaultPlugin.RANK_TIMED) {
				//sText += "" + sr + " ";
				if (sr > DefaultRankCalculator.SR_TIMED_QUEUED_ENDS_AT) {
					long timeStarted = dl.getStats().getTimeStartedSeeding();
					long timeLeft;
					if (dl.isForceStart())
						timeLeft = Constants.CRAPPY_INFINITY_AS_INT;
					else if (timeStarted <= 0)
						timeLeft = minTimeAlive;
					else
						timeLeft = (minTimeAlive - (SystemTime.getCurrentTime() - timeStarted));
					sText += TimeFormatter.format(timeLeft / 1000);
				} else if (sr > 0) {
					sText += MessageText.getString("StartStopRules.waiting");
				}
			} else if (sr > 0) {
				sText += String.valueOf(sr);
			}
		} else if (sr == DefaultRankCalculator.SR_FP0PEERS)
			sText = MessageText.getString("StartStopRules.FP0Peers");
		else if (sr == DefaultRankCalculator.SR_FP_SPRATIOMET)
			sText = MessageText.getString("StartStopRules.SPratioMet");
		else if (sr == DefaultRankCalculator.SR_RATIOMET)
			sText = MessageText.getString("StartStopRules.ratioMet");
		else if (sr == DefaultRankCalculator.SR_NUMSEEDSMET)
			sText = MessageText.getString("StartStopRules.numSeedsMet");
		else if (sr == DefaultRankCalculator.SR_NOTQUEUED)
			sText = "";
		else if (sr == DefaultRankCalculator.SR_0PEERS)
			sText = MessageText.getString("StartStopRules.0Peers");
		else if (sr == DefaultRankCalculator.SR_SHARERATIOMET)
			sText = MessageText.getString("StartStopRules.shareRatioMet");
		else {
			sText = "ERR" + sr;
		}
		// Add a Star if it's before minTimeAlive
		if (SystemTime.getCurrentTime() - dl.getStats().getTimeStartedSeeding() < minTimeAlive)
			sText = "* " + sText;
		cell.setText(sText);
		if (bDebugLog) {
			cell.setToolTip("FP:\n" + dlData.sExplainFP + "\n" + "SR:" + dlData.sExplainSR
					+ "\n" + "TRACE:\n" + dlData.sTrace);
		} else {
			cell.setToolTip(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.config.COConfigurationListener#configurationSaved()
	 */
	public void configurationSaved() {
		minTimeAlive = pluginConfig
				.getUnsafeIntParameter("StartStopManager_iMinSeedingTime") * 1000;
		iRankType = pluginConfig.getUnsafeIntParameter("StartStopManager_iRankType");
		bDebugLog = pluginConfig.getUnsafeBooleanParameter("StartStopManager_bDebugLog");
	}
}
