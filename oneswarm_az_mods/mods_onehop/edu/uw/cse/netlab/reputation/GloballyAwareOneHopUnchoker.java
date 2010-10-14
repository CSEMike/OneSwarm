package edu.uw.cse.netlab.reputation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.peermanager.unchoker.UnchokerUtil;

public class GloballyAwareOneHopUnchoker
{
	public static final int																 RECOMPUTE_INTERVAL_SECS = 9;

	private static Logger																	 logger									= Logger.getLogger(GloballyAwareOneHopUnchoker.class.getName());

	int																										 nonlan_upload_budget;

	int																										 lan_upload_budget			 = 0;

	boolean																								 use_lan_speed					 = false;

	private Map<HashWrapper, List<PEPeerTransportProtocol>> swarm_to_active				 = Collections.synchronizedMap(new HashMap<HashWrapper, List<PEPeerTransportProtocol>>());

	private Map<HashWrapper, List<PEPeerTransportProtocol>> global_unchokes				 = Collections.synchronizedMap(new HashMap<HashWrapper, List<PEPeerTransportProtocol>>());

	private Map<HashWrapper, List<PEPeerTransportProtocol>> global_chokes					 = Collections.synchronizedMap(new HashMap<HashWrapper, List<PEPeerTransportProtocol>>());

	public static volatile GloballyAwareOneHopUnchoker							 inst										= null;

	public static GloballyAwareOneHopUnchoker get() {
		if (inst == null)
			inst = new GloballyAwareOneHopUnchoker();
		return inst;
	}

	private GloballyAwareOneHopUnchoker() {
		logger.info("Creating globally aware unchoker instance...");

		COConfigurationManager.addAndFireParameterListener("Max Upload Speed KBs",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						nonlan_upload_budget = COConfigurationManager.getIntParameter("Max Upload Speed KBs");

						if (nonlan_upload_budget == 0)
							nonlan_upload_budget = 100000;

						logger.finer("nonlan_upload_budget: " + nonlan_upload_budget);
					}
				});

		COConfigurationManager.addAndFireParameterListener("LAN Speed Enabled",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						use_lan_speed = COConfigurationManager.getBooleanParameter("LAN Speed Enabled");
						logger.finer("lan speed: " + Boolean.toString(use_lan_speed));
					}
				});

		COConfigurationManager.addAndFireParameterListener(
				"Max LAN Download Speed KBs", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						lan_upload_budget = COConfigurationManager.getIntParameter("Max LAN Download Speed KBs");

						if (lan_upload_budget == 0)
							lan_upload_budget = 100000;

						logger.finer("lan upload limit: " + lan_upload_budget);
					}
				});

		AzureusCoreImpl.getSingleton().getGlobalManager().addListener(
				new GlobalManagerListener() {
					public void destroyInitiated() {
					}

					public void destroyed() {
					}

					public void downloadManagerAdded(DownloadManager dm) {
					}

					public void downloadManagerRemoved(DownloadManager dm) {
						try {
							logger.fine("Removing active peers for download manager: "
									+ dm.getDisplayName());
							swarm_to_active.remove(dm.getTorrent().getHashWrapper());
						} catch (TOTorrentException e) {
							e.printStackTrace();
						}
					}

					public void seedingStatusChanged(boolean seeding_only_mode) {
					}
				});

		if (nonlan_upload_budget == 0)
			nonlan_upload_budget = 100000;
		if (lan_upload_budget == 0)
			lan_upload_budget = 100000;

		logger.finer("initial budgets: lan: " + lan_upload_budget + " / nonlan: "
				+ nonlan_upload_budget);
	}

	public boolean hasExtraCapacity() {
		if (nonlan_upload_budget > 0) {
			int bound = (int) (0.9 * (double) (nonlan_upload_budget * 1024));
			boolean haveIt = AzureusCoreImpl.getSingleton().getGlobalManager().getStats().getDataAndProtocolSendRate() < bound;
			
			if( logger.isLoggable(Level.FINEST) ) {
				if (haveIt) {
					logger.finest("have extra capacity: "
							+ AzureusCoreImpl.getSingleton().getGlobalManager().getStats().getDataAndProtocolSendRate()
							+ " " + nonlan_upload_budget * 1024 + " bound: " + bound);
				} else {
					logger.finest("no extra capacity: "
							+ AzureusCoreImpl.getSingleton().getGlobalManager().getStats().getDataAndProtocolSendRate()
							+ " " + nonlan_upload_budget * 1024 + " bound: " + bound);
				}
			}
			
			return haveIt;
		}
		return AzureusCoreImpl.getSingleton().getGlobalManager().getStats().getDataAndProtocolSendRate() < 10 * 1024;
	}

	private long last_recompute;

	public synchronized void full_unchoke_recompute() {
		long start = System.currentTimeMillis();
		logger.finer("=============== Globally aware full unchoke recompute... "
				+ swarm_to_active.size() + " active peer lists =================");

		global_chokes.clear();
		global_unchokes.clear();

		List<PEPeerTransportProtocol> all_peers = new ArrayList<PEPeerTransportProtocol>();
		for (HashWrapper hw : swarm_to_active.keySet()) {
			List<PEPeerTransportProtocol> peers = swarm_to_active.get(hw);
			all_peers.addAll(peers);
			global_chokes.put(hw, new ArrayList<PEPeerTransportProtocol>());
			global_unchokes.put(hw, new ArrayList<PEPeerTransportProtocol>());
		}

		int max_to_unchoke = COConfigurationManager.getIntParameter("Max.Peer.Connections.Total");
		if (max_to_unchoke == 0) {
			max_to_unchoke = 500;
		}

		logger.finer("max to unchoke: " + max_to_unchoke);

		int total_unchokes = 0, total_lan_unchokes = 0;

		for (PEPeerTransportProtocol p : all_peers) {
			p.setReputation(computeReputation(p));

			if (p.isLANLocal() && total_lan_unchokes < 5) { // magic #. keep a few LAN local peers always unchoked. these will be over quickly anyway
				global_unchokes.get(new HashWrapper(p.getControl().getHash())).add(p);
				total_lan_unchokes++;
				
				if( logger.isLoggable(Level.FINER) ) {
					logger.finer(p + " from " + p.getControl().getDisplayName()
							+ " is LAN local and will be unchoked, total lan unchokes: "
							+ total_lan_unchokes);
				}
			}
		}

		final PEPeerTransportProtocol[] peers_by_reputation = all_peers.toArray(new PEPeerTransportProtocol[0]);
		Arrays.sort(peers_by_reputation, new Comparator<PEPeerTransportProtocol>() {
			public int compare(PEPeerTransportProtocol o1, PEPeerTransportProtocol o2) {
				double v = o2.getReputation() - o2.getReputation();
				if (Math.abs(v) < 1e-14)
					return 0;
				if (v > 0)
					return -1;
				return 1;
			}
		});

		if( logger.isLoggable(Level.FINEST) ) {
			for (PEPeerTransportProtocol p : peers_by_reputation) {
				logger.finest(p.getControl().getDisplayName() + " " + p + " reputation: "
						+ p.getReputation() + " (is OneSwarm?: "
						+ Boolean.toString(p.isOneSwarm()) + ")");
			}
		}

		/**
		 * Unchoke policy is this: 
		 * 1. Descending order peers with reputation > 1.0
		 * 2. Any OneSwarm peers (randomly)
		 * 3. Any BitTorrent peers (randomly) if our ratio is < 1.0
		 * 4. Any snubbed peers (randomly)
		 */

		// 1. Descending order peers with reputation > 1.0
		int positiveROI_cutoff = 0;
		for (positiveROI_cutoff = 0; total_unchokes < max_to_unchoke
				&& positiveROI_cutoff < all_peers.size()
				&& calc_budget() < nonlan_upload_budget; positiveROI_cutoff++) {
			PEPeerTransportProtocol peer = peers_by_reputation[positiveROI_cutoff];

			if (peer.getReputation() < 1.0)
				break;

			if (UnchokerUtil.isUnchokable(peer, false)) {
				global_unchokes.get(new HashWrapper(peer.getControl().getHash())).add(
						peer);
				total_unchokes++;
				peer.setOptimisticUnchoke(false);
				
				if( logger.isLoggable(Level.FINER) ) {
					logger.finer("1) added reputation > 1.0, " + peer + " / total: "
							+ total_unchokes + " rep: " + peer.getReputation());
				}
			} else {
				if( logger.isLoggable(Level.FINEST) ) {
					logger.finest("1) not unchokable: " + peer + " (rep: "
							+ peer.getReputation());
				}
			}
		}

		if (COConfigurationManager.getBooleanParameter("oneswarm.disallow.ratio.less.than.one") == false) {
			
			if( logger.isLoggable(Level.FINER) ) {
				logger.finer("allowing ratio < 1.0 peers, these include: "
						+ (all_peers.size() - positiveROI_cutoff) + " candidates");
			}

			// Randomly permute peers < 1.0
			PEPeerTransportProtocol[] shoddy_peers = new PEPeerTransportProtocol[peers_by_reputation.length
					- positiveROI_cutoff];
			System.arraycopy(peers_by_reputation, positiveROI_cutoff, shoddy_peers,
					0, shoddy_peers.length);
			Collections.shuffle(Arrays.asList(shoddy_peers));
			
			if( logger.isLoggable(Level.FINER) ) {
				logger.finer("before 2). shoddy_peer.length: " + shoddy_peers.length
						+ " / positiveROI_cutoff: " + positiveROI_cutoff
						+ " max_to_unchoke: " + max_to_unchoke + " unchokes_size: "
						+ total_unchokes + " budget: " + calc_budget());
			}

			// 2. Any OneSwarm peers (randomly)
			for (int i = 0; i < shoddy_peers.length
					&& total_unchokes < max_to_unchoke
					&& calc_budget() < nonlan_upload_budget; i++) {
				PEPeerTransportProtocol peer = shoddy_peers[i];

				if( logger.isLoggable(Level.FINEST) ) {
					logger.finest("2) shoddy rep: " + peer.getReputation() + " " + peer);
				}

				if (UnchokerUtil.isUnchokable(peer, false) && peer.isOneSwarm()) {
					global_unchokes.get(new HashWrapper(peer.getControl().getHash())).add(
							peer);
					total_unchokes++;
					peer.setOptimisticUnchoke(true);
					if( logger.isLoggable(Level.FINER) ) {
						logger.finer("adding in 2) " + peer + " / total: " + total_unchokes
								+ " rep: " + peer.getReputation());
					}
				} else {
					if( logger.isLoggable(Level.FINER) ) { 
						logger.finest("2. not unchokable (or not OneSwarm)" + peer);
					}
				}
			}

			if( logger.isLoggable(Level.FINER) ) {
				logger.finer("before 3). shoddy_peer.length: " + shoddy_peers.length
						+ " / positiveROI_cutoff: " + positiveROI_cutoff
						+ " max_to_unchoke: " + max_to_unchoke + " unchokes_size: "
						+ total_unchokes + " budget: " + calc_budget());
			}

			// 3. Any BitTorrent peers (randomly) if our ratio is < 1.0
			for (int i = 0; i < shoddy_peers.length
					&& total_unchokes < max_to_unchoke
					&& calc_budget() < nonlan_upload_budget; i++) {
				PEPeerTransportProtocol peer = shoddy_peers[i];
				// Some of these will be OneSwarm peers that we added previously, skip these.
				List<PEPeerTransportProtocol> unchoked_this_swarm = global_unchokes.get(new HashWrapper(
						peer.getControl().getHash()));
				if (unchoked_this_swarm.contains(peer)) {
					continue;
				}

				if (UnchokerUtil.isUnchokable(peer, false)) {
					unchoked_this_swarm.add(peer);
					total_unchokes++;
					peer.setOptimisticUnchoke(true);
					if( logger.isLoggable(Level.FINER) ) {
						logger.finer("adding in 3) " + peer + " / total: " + total_unchokes
								+ " rep: " + peer.getReputation());
					}
				} else {
					if( logger.isLoggable(Level.FINEST) ) {
						logger.finest("not unchokable 3): " + peer);
					}
				}
			} // if( System.getProperty("oneswarm.allow.ratio.less.than.one") != null )

			if( logger.isLoggable(Level.FINER) ) {
				logger.finer("before 4). shoddy_peer.length: " + shoddy_peers.length
						+ " / positiveROI_cutoff: " + positiveROI_cutoff
						+ " max_to_unchoke: " + max_to_unchoke + " unchokes_size: "
						+ total_unchokes + " budget: " + calc_budget());
			}

			// 4. Any snubbed peers
			for (int i = 0; total_unchokes < max_to_unchoke && i < all_peers.size()
					&& calc_budget() < nonlan_upload_budget; i++) {
				PEPeerTransportProtocol peer = peers_by_reputation[i];

				List<PEPeerTransportProtocol> unchoked_this_swarm = global_unchokes.get(new HashWrapper(
						peer.getControl().getHash()));
				if (unchoked_this_swarm.contains(peer)) {
					continue;
				}

				if (UnchokerUtil.isUnchokable(peer, true)) {
					unchoked_this_swarm.add(peer);
					total_unchokes++;
					peer.setOptimisticUnchoke(true);
					
					if( logger.isLoggable(Level.FINER) ) {
						logger.finer("adding in 4) " + peer + " / total: " + total_unchokes
								+ " rep: " + peer.getReputation());
					}
				} else {
					if( logger.isLoggable(Level.FINEST) ) {
						logger.finest("4. not unchokable " + peer);
					}
				}
			}
		} // not allowing ratio < 1.0
		else {
			if( logger.isLoggable(Level.FINE) ) {
				logger.fine("global unchoker not allowing ratio less than 1.0, thus skipping: "
						+ (all_peers.size() - positiveROI_cutoff) + " candidates");
			}
		}

		/**
		 * If not unchoked, choke
		 */
		int chokes = 0;
		for (int i = 0; i < all_peers.size(); i++) {
			PEPeerTransportProtocol peer = peers_by_reputation[i];
			HashWrapper this_hash = new HashWrapper(peer.getControl().getHash());
			if (global_unchokes.get(this_hash).contains(peer) == false) {
				global_chokes.get(this_hash).add(peer);
				peer.setWeight(1);
				chokes++;
			}
		}

		if( logger.isLoggable(Level.FINE) ) {
			logger.fine("=========== ended in " + (System.currentTimeMillis() - start)
					+ " ms, upload budget: " + calc_budget() + " with " + total_unchokes
					+ " unchokes of " + all_peers.size() + " chokes: " + chokes
					+ " =================");
		}
	}

	private double computeReputation(PEPeerTransportProtocol p) {
		try {
			// From the perspective of the one hop unchoker, these peers are useless
			if (p.isOneSwarm() == false) {
				return 0.0;
			}

			return Math.max(
					Computation.direct_reputation(p.getCertificate().getPublicKey()),
					Computation.indirect_reputation(p.getSharedIntermediaries()));
		} catch (IOException e) {
			e.printStackTrace();
			return -1; // -1 ~ "has no standing"
		}
	}

	private double calc_budget() {
		logger.finest("calc_budget");

		double min_roi = Double.MAX_VALUE;
		for (List<PEPeerTransportProtocol> list : global_unchokes.values()) {
			for (PEPeerTransportProtocol peer : list) {
				if (peer.getReputation() < min_roi && peer.getReputation() != 0)
					min_roi = peer.getReputation();

				/**
				 * -1 is a sentinel value so we know when we've set this later. F2F connections appear as the same peer (with different 
				 * channel IDs). We need to set their weight exactly once, but they might multiple times in the list. 
				 */
				peer.setWeight(-1.0);
			}
		}

		double total = 0;

		for (List<PEPeerTransportProtocol> list : global_unchokes.values()) {
			for (PEPeerTransportProtocol peer : list) {
				if (peer.getWeight() > 0)
					continue; // we've already set this one. 

				/**
				 * These are considered separately
				 */
				if (peer.isLANLocal()) {
					if( logger.isLoggable(Level.FINEST) ) {
						logger.finest("\tskipping LAN local: " + peer);
					}
					peer.setWeight(5.0); // this doesn't really matter since it's ignored by the LAN rate limiter
					continue;
				}

				// TODO: magic numbers: 20% and 5 KBps
				double used = Math.min(0.20 * nonlan_upload_budget,
						(peer.getReputation() / min_roi) * 5.0);
				// In case we didn't actually find any oneswarm peers, provision everyone at 5.0 
				if (min_roi == Double.MAX_VALUE)
					used = 5.0;
				peer.setWeight(used);
				total += used;

				if( logger.isLoggable(Level.FINEST) ) {
					logger.finest("\traw budget: " + peer + " " + used + " / " + total
							+ " min_roi: " + min_roi);
				}
			}
		}

		/**
		 * This can arise if we have no history with any peer. 
		 */
		if (total == 0)
			return 0;

		/**
		 * Normalize 
		 */
		int id = (int) (Math.random() * 100); // disambiguate log output
		for (List<PEPeerTransportProtocol> list : global_unchokes.values()) {
			for (PEPeerTransportProtocol peer : list) {
				peer.setWeight(peer.getWeight() / total);
				
				if( logger.isLoggable(Level.FINEST) ) {
					logger.finest("\t" + id + " " + peer + " weight: " + peer.getWeight());
				}
			}
		}

		if( logger.isLoggable(Level.FINEST) ) {
			logger.finest("\tout: "
					+ total
					+ " actual UL: "
					+ AzureusCoreImpl.getSingleton().getGlobalManager().getStats().getDataAndProtocolSendRate()
					/ 1024);
		}

		return total;
	}

	public void consider_peers(HashWrapper swarm, ArrayList all_peers) {

		// by: isdal
		// type safetypy not all peers are of type PEPeerTransportProtocol
		ArrayList<PEPeerTransportProtocol> pepeers = new ArrayList<PEPeerTransportProtocol>(
				all_peers.size());
		for (Object o : all_peers) {
			if (o instanceof PEPeerTransportProtocol) {
				pepeers.add((PEPeerTransportProtocol) o);
			}
		}

		swarm_to_active.put(swarm, pepeers);
		if( logger.isLoggable(Level.FINER) ) {
			logger.finer("adding peers for swarm: "
					+ ByteFormatter.encodeString(swarm.getBytes()) + " next recompute: "
					+ ((last_recompute + 9 * 1000) - System.currentTimeMillis()));
		}
		
		/**
		 * don't do this every time a swarm calls its unchoke recompute. instead, wait 9 secs in between to keep it to ~once per TFT round
		 */
		if (last_recompute + RECOMPUTE_INTERVAL_SECS * 1000 < System.currentTimeMillis()) {
			full_unchoke_recompute();
			last_recompute = System.currentTimeMillis();
		}
	}

	public synchronized ArrayList unchokes_for_swarm(HashWrapper swarm) {
		for (List<PEPeerTransportProtocol> l : global_unchokes.values()) {
			if (l.size() > 0) {
				logger.finer("unchoke size: " + l.size() + " for "
						+ ByteFormatter.encodeString(swarm.getBytes()));
			}
		}
		ArrayList out = (ArrayList) global_unchokes.get(swarm);
		if (out == null) {
			
			logger.warning("Null global_unchokes for swarm: "
					+ ByteFormatter.encodeString(swarm.getBytes()));
			
			return new ArrayList();
		}
		return out;
	}

	public synchronized ArrayList chokes_for_swarm(HashWrapper swarm) {
		for (List<PEPeerTransportProtocol> l : global_chokes.values()) {
			if (l.size() > 0) {
				logger.finer("choke size: " + l.size() + " for "
						+ ByteFormatter.encodeString(swarm.getBytes()));
			}
		}
		ArrayList out = (ArrayList) global_chokes.get(swarm);
		if (out == null) {
			//logger.warning("Null global_chokes for swarm: " + ByteFormatter.encodeString(swarm.getBytes()));
			return new ArrayList();
		}
		return out;
	}
}
