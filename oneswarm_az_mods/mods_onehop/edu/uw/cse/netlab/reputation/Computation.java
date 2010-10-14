package edu.uw.cse.netlab.reputation;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
import org.gudy.azureus2.core3.util.ByteFormatter;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.uw.cse.netlab.reputation.storage.Receipt;
import edu.uw.cse.netlab.reputation.storage.ReputationDAO;
import edu.uw.cse.netlab.utils.BloomFilter;

public class Computation
{
	private static Logger logger = Logger.getLogger(Computation.class.getName());
	
	public static double intermediary_weight( long inID ) throws IOException
	{
		ReputationDAO rep = ReputationDAO.get();
		
		/* Equation 1:
		 * 
		 * The data I've received from intermediary both directly and indirectly 
		 * --------------------------------------------------------------------------
		 * The data I've sent to intermediary and due to intermediary recommendations
		 */ 
		
		 double top = 		(rep.get_received_direct(inID) + rep.get_local_recv_due_to_remote_reco(inID));
		 					//------------------------------------------------------------------------------
		 double bottom = 	(rep.get_sent_direct(inID) + rep.get_local_sent_due_to_remote_reco(inID));
		 
		 /*
		  * Sanity checking
		  */
		 if( bottom == 0 )
			 return 5.0; // TODO: This is arbitrary, make sure this makes sense in practice.
		 
		 if( top == 0 )
			 System.err.println( "Negotiating with a shared intermediary that has no standing. (Top K shouldn't have included this intermediary): " + inID );
		 
		 return Math.min(top/bottom, 100.0); // TODO: Also arbitrary, make sure this is sensible. 
	}
	
	public static double peer_value_at_intermediary( Receipt inReceipt ) throws IOException
	{
		/* Equation 2:
		 * 
		 * Total data that peer has sent to any of intermediary's recommendations + data sent directly to intermediary 
		 * --------------------------------------------------------------------------------------------------------------
		 * Total data sent to inPeer because of standing with intermediary + data sent directly from intermediary to peer 
		 */
		double top = 	inReceipt.get_peer_sent_to_recos() + inReceipt.get_received_direct();
		//				-----------------------------------------------------------------------------
		double bottom =	inReceipt.get_peer_received_due_to_reco() + inReceipt.get_sent_direct();
		
		if( bottom == 0 )
			return 5.0; // TODO: arbitrary constant. 
		
		return Math.min(top/bottom, 100.0); // TODO: arbitrary constant. 
	}
	
	public static double indirect_reputation( Receipt [] inSharedIntermediaries ) throws IOException
	{
		if( inSharedIntermediaries.length == 0 )
			return 0.0;
		
		double rep_sum = 0.0;
		
		for( Receipt r : inSharedIntermediaries )
		{
			if( r == null )
				throw new IOException("********** r is null");
			rep_sum += intermediary_weight(r.getSigningID()) * peer_value_at_intermediary(r);
		}
		
		return rep_sum/(double)inSharedIntermediaries.length;
	}
	
	public static double direct_reputation( PublicKey inRemotePeerKey ) throws IOException
	{
		ReputationDAO rep = ReputationDAO.get();
		
		long remote_id = rep.get_internal_id(inRemotePeerKey);
		
		double top = rep.get_received_direct(remote_id);
		double bottom = rep.get_sent_direct(remote_id);
		
		if( top == -1 ||
			bottom == -1 )
		{
			return -1;
		}
		
		if( top == 0 )
			return 0;
		
		if( bottom == 0 )
			return 5.0; // TODO: arbitrary constant
		
		return Math.min(top/bottom, 100.0); // TODO: arbitrary constant
	}
	
	public static Map<PublicKey, Float> desired_nodes_from_topK( BloomFilter inFilter ) throws IOException
	{
		ReputationDAO rep = ReputationDAO.get();
		List<PublicKey> desired = new ArrayList<PublicKey>();
		
		for( PublicKey k : rep.get_desired_peers() )
		{
			if( inFilter.test(k.getEncoded()) )
			{
				logger.finest("\tadding: " + ByteFormatter.encodeString(k.getEncoded()));
				
				desired.add(k);
			}
		}
		
		logger.finer("shared total (desired_nodes_from_topK): " + desired.size());
		
		// Default policy is choose random 10. 
		Collections.shuffle(desired);
		PublicKey [] keys = desired.subList(0, Math.min(desired.size(), 10)).toArray(new PublicKey[0]);
		double equal_split = 1.0 / (double)keys.length;
		Map<PublicKey, Float> map = new HashMap<PublicKey, Float>();
		for( PublicKey k : keys )
			map.put(k, (float)equal_split); // TODO: make this not equally load balanced, maybe...
		return map;
	}

	public static final double DIRECT_WEIGHT = 0.5;
	/**
	 * This function computes the fractional weight with which to record the indirect observations of a peer
	 * 
	 * @param peer The peer whose weight we are computing
	 * @return weight
	 */
	public static double indirect_advertisements_weight( PEPeerTransportProtocol peer ) 
	{
		/**
		 * Compare the recent volume of data received from this peer to the recent volume received from others 
		 */
		
		long max_rate = 0, max_volume = 0; 
		
		for( DownloadManager dm : (List<DownloadManager>)AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers() )
		{
			if( dm.getPeerManager() == null )
			{
				logger.warning("null peer manager for swarm: " + dm.getDisplayName());
				continue;
			}
			if( dm.getPeerManager().getPeers() == null )
			{
				logger.warning("null peer list for swarm: " + dm.getDisplayName());
				continue;
			}
			for( PEPeerTransportProtocol p : (List<PEPeerTransportProtocol>)dm.getPeerManager().getPeers() )
			{
				PEPeerStats s = p.getStats();
				if( s.getTotalDataBytesReceived() > max_volume )
					max_volume = s.getTotalDataBytesReceived();
				if( s.getDataReceiveRate() > max_rate )
					max_rate = s.getDataReceiveRate();
			}
		}
		
		logger.finer("indirect_advertisements_weight(), max_volume: " + max_volume + " max_rate: " + max_rate);
		
		double direct = max_volume > 0 ? ((double)peer.getStats().getTotalDataBytesReceived()/(double)max_volume) : 0;
		double indirect = max_rate > 0 ? ((double)peer.getStats().getDataReceiveRate()/(double)max_rate) : 0;
		
		logger.finer("indirect_obs_weight: direct: " + direct + " indirect: " + indirect);
		
		return DIRECT_WEIGHT * direct +
			   (1-DIRECT_WEIGHT) * indirect;
	}

}
