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
 * 
 */
package com.aelitis.azureus.plugins.startstoprules.defaultplugin;

import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * @author TuxPaper
 * @created Dec 13, 2005
 *
 */
public class DefaultRankCalculator implements Comparable {
	/** All of the First Priority rules must match */
	public static final int FIRSTPRIORITY_ALL = 0;

	/** Any of the First Priority rules must match */
	public static final int FIRSTPRIORITY_ANY = 1;

	/** 
	 * Force torrent to be "Actively Seeding/Downloading" for this many ms upon
	 * start of torrent.
	 */
	private static final int FORCE_ACTIVE_FOR = 30000;

	/** 
	 * Wait XX ms before really changing activity (DL or CDing) state when
	 * state changes via speed change
	 */
	private static final int ACTIVE_CHANGE_WAIT = 10000;

	/** Maximium ranking that a torrent can get using the SPRATIO ranking type */
	private static int SPRATIO_BASE_LIMIT = 99999;

	/** 
	 * Amount to shift over the rank of the SEEDONLY ranking type, to make room
	 * in case the user has fallback to SPRATIO set.
	 */
	private static int SEEDONLY_SHIFT = SPRATIO_BASE_LIMIT + 1;

	/**
	 * For loading config settings
	 */
	private static COConfigurationListener configListener = null;

	//
	// Seeding Rank (SR) Limits and Values

	/** Rank that complete starts at (and incomplete ends at + 1) */
	public static final int SR_COMPLETE_STARTS_AT = 1000000000; // billion

	/** Maximimum ranking for time queue mode. 1 unit is a second */
	public static final int SR_TIMED_QUEUED_ENDS_AT = 999999; // 11.57 days

	/** Ranks below this value are for torrents to be ignored (moved to bottom & queued) */
	public static final int SR_IGNORED_LESS_THAN = -1;

	/** Seeding Rank value when download is marked as not queued */
	public static final int SR_NOTQUEUED = -2;

	/** Seeding Rank value when download is marked as S:P Ratio Met for FP */
	public static final int SR_FP_SPRATIOMET = -3;

	/** Seeding Rank value when download is marked as P:1S Ratio Met */
	public static final int SR_RATIOMET = -4;

	/** Seeding Rank value when download is marked as # Seeds Met */
	public static final int SR_NUMSEEDSMET = -5;

	/** Seeding Rank value when download is marked as 0 Peers and FP */
	public static final int SR_FP0PEERS = -6;

	/** Seeding Rank value when download is marked as 0 Peers */
	public static final int SR_0PEERS = -7;

	/** Seeding Rank value when download is marked as Share Ratio Met */
	public static final int SR_SHARERATIOMET = -8;

	//
	// Static config values

	/** Ranking System to use */
	protected static int iRankType = -1;

	/** Min # of Peers needed before boosting the rank of downloads with no seeds */
	private static int minPeersToBoostNoSeeds;

	/** Min Speed needed to count a incomplete download as being actively downloading */
	private static int minSpeedForActiveDL;

	/** Min speed needed to count a complete download as being actively seeding */
	private static int minSpeedForActiveSeeding;

	// Ignore torrent if seed count is at least..
	private static int iIgnoreSeedCount;

	// Ignore even when First Priority
	private static boolean bIgnore0Peers;

	private static int iIgnoreShareRatio;

	private static int iIgnoreShareRatio_SeedStart;

	private static int iIgnoreRatioPeers;

	private static int iIgnoreRatioPeers_SeedStart;

	private static int iRankTypeSeedFallback;

	private static boolean bPreferLargerSwarms;

	private static int minQueueingShareRatio;

	// Ignore First Priority
	private static int iFirstPriorityIgnoreSPRatio;

	private static boolean bFirstPriorityIgnore0Peer;

	private static int iFirstPriorityType;

	private static int iFirstPrioritySeedingMinutes;

	private static int iFirstPriorityActiveMinutes;

	private static long minTimeAlive;

	private static boolean bAutoStart0Peers;

	//
	// Class variables

	protected Download dl;

	private boolean bActivelyDownloading;

	private long lDLActivelyChangedOn;

	private boolean bActivelySeeding;

	private long lCDActivelyChangedOn;

	private boolean bIsFirstPriority;

	/** Public for tooltip to access it */
	public String sExplainFP = "";

	/** Public for tooltip to access it */
	public String sExplainSR = "";

	/** Public for tooltip to access it */
	public String sTrace = "";

	private AEMonitor downloadData_this_mon = new AEMonitor(
			"StartStopRules:downloadData");

	private final StartStopRulesDefaultPlugin rules;
	
	
	
	// state-caches for sorting
	
	int lastModifiedScrapeResultPeers = 0;
	int lastModifiedScrapeResultSeeds = 0;
	int lastModifiedShareRatio = 0;
	// modified by a listener in StartStopRulesDefaultPlugin
	boolean lastScrapeResultOk = false;

	/**
	 * Default Initializer
	 * 
	 * @param _rules
	 * @param _dl
	 */
	public DefaultRankCalculator(StartStopRulesDefaultPlugin _rules, Download _dl) {
		rules = _rules;
		dl = _dl;

		try {
			downloadData_this_mon.enter();

			if (configListener == null) {

				configListener = new COConfigurationListener() {
					public void configurationSaved() {
						reloadConfigParams(rules.plugin_config);
					}
				};

				COConfigurationManager.addListener(configListener);
				configListener.configurationSaved();
			}
		} finally {
			downloadData_this_mon.exit();
		}
	}

	/**
	 * Load config values into the static variables
	 * 
	 * @param cfg
	 */
	public static void reloadConfigParams(PluginConfig cfg) {
		final String PREFIX = "StartStopManager_";

		iRankType = cfg.getIntParameter(PREFIX + "iRankType");

		minPeersToBoostNoSeeds = cfg.getIntParameter(PREFIX
				+ "iMinPeersToBoostNoSeeds");
		minSpeedForActiveDL = cfg.getIntParameter(PREFIX + "iMinSpeedForActiveDL");
		minSpeedForActiveSeeding = cfg.getIntParameter(PREFIX
				+ "iMinSpeedForActiveSeeding");

		iRankTypeSeedFallback = cfg.getIntParameter(PREFIX
				+ "iRankTypeSeedFallback");
		bPreferLargerSwarms = cfg.getBooleanParameter(PREFIX
				+ "bPreferLargerSwarms");
		minTimeAlive = cfg.getIntParameter(PREFIX + "iMinSeedingTime") * 1000;
		bAutoStart0Peers = cfg.getBooleanParameter(PREFIX + "bAutoStart0Peers");

		// Ignore torrent if seed count is at least..
		iIgnoreSeedCount = cfg.getIntParameter(PREFIX + "iIgnoreSeedCount");
		bIgnore0Peers = cfg.getBooleanParameter(PREFIX + "bIgnore0Peers");
		iIgnoreShareRatio = (int) (1000 * cfg.getFloatParameter("Stop Ratio"));
		iIgnoreShareRatio_SeedStart = cfg.getIntParameter(PREFIX
				+ "iIgnoreShareRatioSeedStart");
		iIgnoreRatioPeers = cfg.getIntParameter("Stop Peers Ratio", 0);
		iIgnoreRatioPeers_SeedStart = cfg.getIntParameter(PREFIX
				+ "iIgnoreRatioPeersSeedStart", 0);

		minQueueingShareRatio = cfg.getIntParameter(PREFIX
				+ "iFirstPriority_ShareRatio");
		iFirstPriorityType = cfg.getIntParameter(PREFIX + "iFirstPriority_Type");
		iFirstPrioritySeedingMinutes = cfg.getIntParameter(PREFIX
				+ "iFirstPriority_SeedingMinutes");
		iFirstPriorityActiveMinutes = cfg.getIntParameter(PREFIX
				+ "iFirstPriority_DLMinutes");
		// Ignore FP
		iFirstPriorityIgnoreSPRatio = cfg.getIntParameter(PREFIX
				+ "iFirstPriority_ignoreSPRatio");
		bFirstPriorityIgnore0Peer = cfg.getBooleanParameter(PREFIX
				+ "bFirstPriority_ignore0Peer");
	}

	/** Sort first by SeedingRank Descending, then by Position Ascending.
	 */
	public int compareTo(Object obj) {
		if (!(obj instanceof DefaultRankCalculator)) {
			return -1;
		}

		DefaultRankCalculator dlData = (DefaultRankCalculator) obj;
		// Test Completeness
		boolean aIsComplete = dlData.dl.isComplete();
		boolean bIsComplete = dl.isComplete();
		if (aIsComplete && !bIsComplete)
			return -1;
		if (!aIsComplete && bIsComplete)
			return 1;

		// Test FP
		if (dlData.bIsFirstPriority && !bIsFirstPriority)
			return 1;
		if (!dlData.bIsFirstPriority && bIsFirstPriority)
			return -1;

		if (iRankType == StartStopRulesDefaultPlugin.RANK_NONE) {
			return dl.getPosition() - dlData.dl.getPosition();
		}

		// Check Rank
		int value = dlData.dl.getSeedingRank() - dl.getSeedingRank();
		if (value != 0)
			return value;

		if (iRankType != StartStopRulesDefaultPlugin.RANK_TIMED) {
			// Test Large/Small Swarm pref
			int numPeersThem = dlData.lastModifiedScrapeResultPeers;
			int numPeersUs = lastModifiedScrapeResultPeers;
			if (bPreferLargerSwarms)
				value = numPeersThem - numPeersUs;
			else
				value = numPeersUs - numPeersThem;
			if (value != 0)
				return value;

			// Test Share Ratio
			value = lastModifiedShareRatio - dlData.lastModifiedShareRatio;
			if (value != 0)
				return value;
		}

		// Test Position
		return dl.getPosition() - dlData.dl.getPosition();
	}

	public Download getDownloadObject() {
		return dl;
	}
	
	public boolean isForceActive() {
		DownloadStats stats = dl.getStats();
		return SystemTime.getCurrentTime() - stats.getTimeStarted() <= FORCE_ACTIVE_FOR;
	}

	/**
	 * Retrieves whether the torrent is "actively" downloading
	 * 
	 * @return true: actively downloading
	 */
	public boolean getActivelyDownloading() {
		boolean bIsActive = false;
		DownloadStats stats = dl.getStats();
		int state = dl.getState();

		// In order to be active,
		// - Must be downloading (and thus incomplete)
		// - Must be above speed threshold, or started less than 30s ago
		if (state != Download.ST_DOWNLOADING) {
			bIsActive = false;
		} else if (SystemTime.getCurrentTime() - stats.getTimeStarted() <= FORCE_ACTIVE_FOR) {
			bIsActive = true;
		} else {
			// activity based on DL Average
			bIsActive = (stats.getDownloadAverage() >= minSpeedForActiveDL);

			if (bActivelyDownloading != bIsActive) {
				long now = SystemTime.getCurrentTime();
				// Change
				if (lDLActivelyChangedOn == -1) {
					// Start Timer
					lDLActivelyChangedOn = now;
					bIsActive = !bIsActive;
				} else if (now - lDLActivelyChangedOn < ACTIVE_CHANGE_WAIT) {
					// Continue as old state until timer finishes
					bIsActive = !bIsActive;
				}
			} else {
				// no change, reset timer
				lDLActivelyChangedOn = -1;
			}
		}

		if (bActivelyDownloading != bIsActive) {
			bActivelyDownloading = bIsActive;
			if (rules != null) {
				rules.requestProcessCycle(null);
				if (rules.bDebugLog)
					rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
							"somethingChanged: ActivelyDownloading changed");
			}
		}
		return bActivelyDownloading;
	}

	/**
	 * Retrieves whether the torrent is "actively" seeding
	 * 
	 * @return true: actively seeding
	 */
	public boolean getActivelySeeding() {
		boolean bIsActive = false;
		DownloadStats stats = dl.getStats();
		int state = dl.getState();
		// Timed torrents don't use a speed threshold, since they are based on time!
		// However, First Priorities need to be checked for activity so that 
		// timed ones can start when FPs are below threshold.  Ditto for 0 Peers
		// when bAutoStart0Peers
		if (iRankType == StartStopRulesDefaultPlugin.RANK_TIMED
				&& !isFirstPriority()
				&& !(bAutoStart0Peers && rules.calcPeersNoUs(dl) == 0 && lastScrapeResultOk)) {
			bIsActive = (state == Download.ST_SEEDING);

		} else if (state != Download.ST_SEEDING
				|| (bAutoStart0Peers && rules.calcPeersNoUs(dl) == 0)) {
			// Not active if we aren't seeding
			// Not active if we are AutoStarting 0 Peers, and peer count == 0
			bIsActive = false;
		} else if (SystemTime.getCurrentTime() - stats.getTimeStarted() <= FORCE_ACTIVE_FOR) {
			bIsActive = true;
		} else {
			bIsActive = (stats.getUploadAverage() >= minSpeedForActiveSeeding);

			if (bActivelySeeding != bIsActive) {
				long now = SystemTime.getCurrentTime();
				// Change
				if (lCDActivelyChangedOn == -1) {
					// Start Timer
					lCDActivelyChangedOn = now;
					bIsActive = !bIsActive;
				} else if (now - lCDActivelyChangedOn < ACTIVE_CHANGE_WAIT) {
					// Continue as old state until timer finishes
					bIsActive = !bIsActive;
				}
			} else {
				// no change, reset timer
				lCDActivelyChangedOn = -1;
			}
		}

		if (bActivelySeeding != bIsActive) {
			bActivelySeeding = bIsActive;
			if (rules != null) {
				rules.requestProcessCycle(null);
				if (rules.bDebugLog)
					rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
							"somethingChanged: ActivelySeeding changed");
			}
		}
		return bActivelySeeding;
	}

	/** Assign Seeding Rank based on RankType
	 * @return New Seeding Rank Value
	 */
	public int recalcSeedingRank() {
		try {
			downloadData_this_mon.enter();

			sExplainSR = "";

			int oldSR = dl.getSeedingRank();
			DownloadStats stats = dl.getStats();

			int newSR = 0;

			// make undownloaded sort to top so they can start first.
			if (!dl.isComplete()) {
				newSR = SR_COMPLETE_STARTS_AT + (10000 - dl.getPosition());
				dl.setSeedingRank(newSR);
				if ( rules.bDebugLog ){
					sExplainSR += "  not complete. SetSR " + newSR + "\n";
				}
				return newSR;
			}

			// here we are seeding

			lastModifiedShareRatio = stats.getShareRatio();
			lastModifiedScrapeResultPeers = rules.calcPeersNoUs(dl);
			lastModifiedScrapeResultSeeds = rules.calcSeedsNoUs(dl);

			boolean bScrapeResultsOk = (lastModifiedScrapeResultPeers > 0 || lastModifiedScrapeResultSeeds > 0
					|| lastScrapeResultOk) && (lastModifiedScrapeResultPeers >= 0 && lastModifiedScrapeResultSeeds >= 0);

			if (!isFirstPriority()) {
				// Check Ignore Rules
				// never apply ignore rules to First Priority Matches
				// (we don't want leechers circumventing the 0.5 rule)

				//0 means unlimited
				if (iIgnoreShareRatio != 0 && lastModifiedShareRatio >= iIgnoreShareRatio
						&& (lastModifiedScrapeResultSeeds >= iIgnoreShareRatio_SeedStart || !bScrapeResultsOk)
						&& lastModifiedShareRatio != -1) {
					
					if (rules.bDebugLog)
						sExplainSR += "  shareratio met: shareRatio(" + lastModifiedShareRatio
								+ ") >= " + iIgnoreShareRatio + "\n";

					dl.setSeedingRank(SR_SHARERATIOMET);
					return SR_SHARERATIOMET;
				} else if (rules.bDebugLog && iIgnoreShareRatio != 0
						&& lastModifiedShareRatio >= iIgnoreShareRatio) {
					sExplainSR += "  shareratio NOT met: ";
					if (lastModifiedScrapeResultSeeds >= iIgnoreShareRatio_SeedStart) 
						sExplainSR += lastModifiedScrapeResultSeeds + " below seed threshold of "
								+ iIgnoreShareRatio_SeedStart;
					sExplainSR += "\n";
				}

				if (lastModifiedScrapeResultPeers == 0 && bScrapeResultsOk) {
					// If both bIgnore0Peers and bFirstPriorityIgnore0Peer are on,
					// we won't know which one it is at this point.
					// We have to use the normal SR_0PEERS in case it isn't FP
					if (bIgnore0Peers) {
						if (rules.bDebugLog)
							sExplainSR += "  Ignore 0 Peers criteria met\n";

						dl.setSeedingRank(SR_0PEERS);
						return SR_0PEERS;
					}

//					if (bFirstPriorityIgnore0Peer) {
//						if (rules.bDebugLog)
//							sExplainSR += "  Ignore 0 Peers criteria for FP met\n";
//
//						dl.setSeedingRank(SR_FP0PEERS);
//						return SR_FP0PEERS;
//					}
				} else if (rules.bDebugLog && lastModifiedScrapeResultPeers == 0) {
					sExplainSR += "  0 Peer Ignore rule NOT applied: Scrape invalid\n";
				}

//				if (numPeers != 0 && iFirstPriorityIgnoreSPRatio != 0
//						&& numSeeds / numPeers >= iFirstPriorityIgnoreSPRatio) {
//					if (rules.bDebugLog)
//						sExplainSR += "  Ignore rule for S:P Ratio for FP met.  Current: ("
//								+ (numSeeds / numPeers)
//								+ ") >= Threshold("
//								+ iFirstPriorityIgnoreSPRatio + ")\n";
//
//					dl.setSeedingRank(SR_FP_SPRATIOMET);
//					return SR_FP_SPRATIOMET;
//				}

				//0 means disabled
				if ((iIgnoreSeedCount != 0) && (lastModifiedScrapeResultSeeds >= iIgnoreSeedCount)) {
					if (rules.bDebugLog)
						sExplainSR += "  SeedCount Ignore rule met.  numSeeds("
								+ lastModifiedScrapeResultSeeds + " >= iIgnoreSeedCount(" + iIgnoreSeedCount + ")\n"; 

					dl.setSeedingRank(SR_NUMSEEDSMET);
					return SR_NUMSEEDSMET;
				}

				// Ignore when P:S ratio met
				// (More Peers for each Seed than specified in Config)
				//0 means never stop
				if (iIgnoreRatioPeers != 0 && lastModifiedScrapeResultSeeds != 0) {
					float ratio = (float) lastModifiedScrapeResultPeers / lastModifiedScrapeResultSeeds;
					if (ratio <= iIgnoreRatioPeers
							&& lastModifiedScrapeResultSeeds >= iIgnoreRatioPeers_SeedStart) {

						if (rules.bDebugLog)
							sExplainSR += "  P:S Ignore rule met.  ratio(" + ratio
									+ " <= threshold(" + iIgnoreRatioPeers_SeedStart + ")\n";

						dl.setSeedingRank(SR_RATIOMET);
						return SR_RATIOMET;
					}
				}
			}

			// Never do anything with rank type of none
			if (iRankType == StartStopRulesDefaultPlugin.RANK_NONE) {
				if (rules.bDebugLog)
					sExplainSR += "  Ranking Type set to none.. blanking seeding rank\n";
				
				// everythink ok!
				dl.setSeedingRank(newSR);
				return newSR;
			}

			if (iRankType == StartStopRulesDefaultPlugin.RANK_TIMED) {
				if (bIsFirstPriority) {
					newSR += SR_TIMED_QUEUED_ENDS_AT + 1;
					dl.setSeedingRank(newSR);
					return newSR;
				}

				int state = dl.getState();
				if (state == Download.ST_STOPPING || state == Download.ST_STOPPED
						|| state == Download.ST_ERROR) {
					if (rules.bDebugLog)
						sExplainSR += "  Download stopping, stopped or in error\n";
					dl.setSeedingRank(SR_NOTQUEUED);
					return SR_NOTQUEUED;
				} else if (state == Download.ST_SEEDING || state == Download.ST_READY
						|| state == Download.ST_WAITING || state == Download.ST_PREPARING) {
					// force sort to top
					long lMsElapsed = 0;
					if (state == Download.ST_SEEDING && !dl.isForceStart())
						lMsElapsed = (SystemTime.getCurrentTime() - stats
								.getTimeStartedSeeding());

					if (lMsElapsed >= minTimeAlive) {
						dl.setSeedingRank(1);
						if (oldSR > SR_TIMED_QUEUED_ENDS_AT) {
							rules.requestProcessCycle(null);
							if (rules.bDebugLog)
								rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
										"somethingChanged: TimeUp");
						}
					} else {
						newSR = SR_TIMED_QUEUED_ENDS_AT + 1 + (int) (lMsElapsed / 1000);
						dl.setSeedingRank(newSR);
						if (oldSR <= SR_TIMED_QUEUED_ENDS_AT) {
							rules.requestProcessCycle(null);
							if (rules.bDebugLog)
								rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
										"somethingChanged: strange timer change");
						}
					}
					return newSR;
				} else {
					if (oldSR <= 0) {
						newSR = SR_TIMED_QUEUED_ENDS_AT - dl.getPosition();
						dl.setSeedingRank(newSR);
						rules.requestProcessCycle(null);
						if (rules.bDebugLog)
							rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
									"somethingChanged: NotIgnored");
					}
					return newSR;
				}
			}

			/** 
			 * Add to SeedingRank based on Rank Type
			 */

			// SeedCount and SPRatio require Scrape Results..
			if (bScrapeResultsOk) {
				if ((iRankType == StartStopRulesDefaultPlugin.RANK_SEEDCOUNT)
						&& (iRankTypeSeedFallback == 0 || iRankTypeSeedFallback > lastModifiedScrapeResultSeeds)) {
					if (lastModifiedScrapeResultSeeds < 10000)
						newSR = 10000 - lastModifiedScrapeResultSeeds;
					else
						newSR = 1;
					// shift over to make way for fallback
					newSR *= SEEDONLY_SHIFT;

				} else { // iRankType == RANK_SPRATIO or we are falling back
					if (lastModifiedScrapeResultPeers != 0) {
						if (lastModifiedScrapeResultSeeds == 0) {
							if (lastModifiedScrapeResultPeers >= minPeersToBoostNoSeeds)
								newSR += SPRATIO_BASE_LIMIT;
						} else { // numSeeds != 0 && numPeers != 0
							float x = (float) lastModifiedScrapeResultSeeds / lastModifiedScrapeResultPeers;
							newSR += SPRATIO_BASE_LIMIT / ((x + 1) * (x + 1));
						}
					}
				}
			} else {
				if (rules.bDebugLog)
					sExplainSR += "  Can't calculate SR, no scrape results\n";
			}

			if (newSR < 0)
				newSR = 1;

			if (newSR != oldSR)
				dl.setSeedingRank(newSR);
			return newSR;
		} finally {

			downloadData_this_mon.exit();
		}
	} // recalcSeedingRank

	/** Does the torrent match First Priority criteria? 
	 * @return FP State 
	 */
	public boolean isFirstPriority() {
		boolean bFP = pisFirstPriority();

		if (bIsFirstPriority != bFP) {
			bIsFirstPriority = bFP;
			rules.requestProcessCycle(null);
			if (rules.bDebugLog)
				rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
						"somethingChanged: FP changed");
		}
		return bIsFirstPriority;
	}

	private boolean pisFirstPriority() {
		if (rules.bDebugLog)
			sExplainFP = "FP if "
					+ (iFirstPriorityType == FIRSTPRIORITY_ALL ? "all" : "any")
					+ " criteria match:\n";

		if (!dl.isPersistent()) {
			if (rules.bDebugLog)
				sExplainFP += "Not FP: Download not persistent\n";
			return false;
		}

		if (dl.getState() == Download.ST_ERROR
				|| dl.getState() == Download.ST_STOPPED) {
			if (rules.bDebugLog)
				sExplainFP += "Not FP: Download is ERROR or STOPPED\n";
			return false;
		}

		// FP only applies to completed
		if (!dl.isComplete()) {
			if (rules.bDebugLog)
				sExplainFP += "Not FP: Download not complete\n";
			return false;
		}
		
		List listeners = rules.getFPListeners();
		StringBuffer fp_listener_debug = null;
		if (rules.bDebugLog && !listeners.isEmpty()) {fp_listener_debug = new StringBuffer();}
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			StartStopRulesFPListener l = (StartStopRulesFPListener) iter.next();
			boolean result = l.isFirstPriority(dl, lastModifiedScrapeResultSeeds, lastModifiedScrapeResultPeers, fp_listener_debug);
			if (fp_listener_debug != null && fp_listener_debug.length() > 0) {
				char last_ch = fp_listener_debug.charAt(fp_listener_debug.length()-1);
				if (last_ch != '\n') {fp_listener_debug.append('\n');}
				sExplainFP += fp_listener_debug;
				fp_listener_debug.setLength(0);
			}
			if (result) {
				return true;
			}
		}

		// FP doesn't apply when S:P >= set SPratio (SPratio = 0 means ignore)
		if (lastModifiedScrapeResultPeers > 0 && lastModifiedScrapeResultSeeds > 0
				&& (lastModifiedScrapeResultSeeds / lastModifiedScrapeResultPeers) >= iFirstPriorityIgnoreSPRatio
				&& iFirstPriorityIgnoreSPRatio != 0) {
			if (rules.bDebugLog)
				sExplainFP += "Not FP: S:P >= " + iFirstPriorityIgnoreSPRatio + ":1\n";
			return false;
		}

		//not FP if no peers  //Nolar, 2105 - Gouss, 2203
		if (lastModifiedScrapeResultPeers == 0 && lastScrapeResultOk && bFirstPriorityIgnore0Peer) {
			if (rules.bDebugLog)
				sExplainFP += "Not FP: 0 peers\n";
			return false;
		}

		int shareRatio = dl.getStats().getShareRatio();
		boolean bLastMatched = (shareRatio != -1)
				&& (shareRatio < minQueueingShareRatio);

		if (rules.bDebugLog)
			sExplainFP += "  shareRatio(" + shareRatio + ") < "
					+ minQueueingShareRatio + "=" + bLastMatched + "\n";

		if (!bLastMatched && iFirstPriorityType == FIRSTPRIORITY_ALL) {
			if (rules.bDebugLog)
				sExplainFP += "..Not FP.  Exit Early\n";
			return false;
		}
		if (bLastMatched && iFirstPriorityType == FIRSTPRIORITY_ANY) {
			if (rules.bDebugLog)
				sExplainFP += "..Is FP.  Exit Early\n";
			return true;
		}

		bLastMatched = (iFirstPrioritySeedingMinutes == 0);
		if (!bLastMatched) {
			long timeSeeding = dl.getStats().getSecondsOnlySeeding();
			if (timeSeeding >= 0) {
				bLastMatched = (timeSeeding < (iFirstPrioritySeedingMinutes * 60));
				if (rules.bDebugLog)
					sExplainFP += "  SeedingTime(" + timeSeeding + ") < "
							+ (iFirstPrioritySeedingMinutes * 60) + "=" + bLastMatched + "\n";
				if (!bLastMatched && iFirstPriorityType == FIRSTPRIORITY_ALL) {
					if (rules.bDebugLog)
						sExplainFP += "..Not FP.  Exit Early\n";
					return false;
				}
				if (bLastMatched && iFirstPriorityType == FIRSTPRIORITY_ANY) {
					if (rules.bDebugLog)
						sExplainFP += "..Is FP.  Exit Early\n";
					return true;
				}
			}
		} else if (rules.bDebugLog) {
			sExplainFP += "  Skipping Seeding Time check (user disabled)\n";
		}

		bLastMatched = (iFirstPriorityActiveMinutes == 0);
		if (!bLastMatched) {
			long timeActive = dl.getStats().getSecondsDownloading()
					+ dl.getStats().getSecondsOnlySeeding();
			if (timeActive >= 0) {
				bLastMatched = (timeActive < (iFirstPriorityActiveMinutes * 60));
				if (rules.bDebugLog)
					sExplainFP += "  ActiveTime(" + timeActive + ") < "
							+ (iFirstPriorityActiveMinutes * 60) + "=" + bLastMatched + "\n";
				if (!bLastMatched && iFirstPriorityType == FIRSTPRIORITY_ALL) {
					if (rules.bDebugLog)
						sExplainFP += "..Not FP.  Exit Early\n";
					return false;
				}
				if (bLastMatched && iFirstPriorityType == FIRSTPRIORITY_ANY) {
					if (rules.bDebugLog)
						sExplainFP += "..Is FP.  Exit Early\n";
					return true;
				}
			}
		} else if (rules.bDebugLog) {
			sExplainFP += "  Skipping DL Time check (user disabled)\n";
		}

		if (iFirstPriorityType == FIRSTPRIORITY_ALL) {
			if (rules.bDebugLog)
				sExplainFP += "..Is FP\n";
			return true;
		}

		if (rules.bDebugLog)
			sExplainFP += "..Not FP\n";
		return false;
	}

	/**
	 * 
	 * @return last calculated FP state
	 */
	public boolean getCachedIsFP() {
		return bIsFirstPriority;
	}

	public String toString() {
		return String.valueOf(dl.getSeedingRank());
	}

	/**
	 * Check Seeders for various changes not triggered by listeners
	 * 
	 * @return True: something changed
	 */
	public boolean changeChecker() {
		if (getActivelySeeding()) {
			int shareRatio = dl.getStats().getShareRatio();
			int numSeeds = rules.calcSeedsNoUs(dl);

			if (iIgnoreShareRatio != 0 && shareRatio >= iIgnoreShareRatio
					&& (numSeeds >= iIgnoreShareRatio_SeedStart || !lastScrapeResultOk)
					&& shareRatio != -1) {
				return true;
			}
		}

		/* READY downloads are usually waiting for a seeding torrent to
		 stop (the seeding torrent probably is within the "Minimum Seeding
		 Time" setting)
		 
		 The rules may go through several cycles before a READY torrent is
		 processed
		 */
		if (dl.getState() == Download.ST_READY) {
			if (rules.bDebugLog)
				rules.log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
						"somethingChanged: Download is ready");
			return true;
		}

		return false;
	}
}
