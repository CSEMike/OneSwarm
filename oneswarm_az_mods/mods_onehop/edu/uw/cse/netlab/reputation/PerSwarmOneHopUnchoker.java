package edu.uw.cse.netlab.reputation;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.peermanager.unchoker.Unchoker;
import com.aelitis.azureus.core.peermanager.unchoker.UnchokerUtil;

public class PerSwarmOneHopUnchoker implements Unchoker
{
	private HashWrapper mSwarm;
	private GloballyAwareOneHopUnchoker global = GloballyAwareOneHopUnchoker.get();
	
	private static Logger logger = Logger.getLogger(PerSwarmOneHopUnchoker.class.getName());
	
	public PerSwarmOneHopUnchoker( HashWrapper inSwarm ) {
		if( inSwarm == null )
		{
			logger.severe("Null hash when creating PerSwarmOneHopUnchoker!");
		}
		mSwarm = inSwarm;
	}

	public void calculateUnchokes(int max_to_unchoke, ArrayList all_peers, boolean force_refresh) {
		global.consider_peers(mSwarm, all_peers);
	}

	public ArrayList getChokes() {
		ArrayList out = global.chokes_for_swarm(mSwarm);
		return out;
	}

	public ArrayList getImmediateUnchokes(int max_to_unchoke, ArrayList all_peers) {
		/**
		 * Only do this if we have some spare capacity
		 */
		ArrayList out = null;
		if( global.hasExtraCapacity() )
		{
			out = UnchokerUtil.getNextOptimisticPeers( all_peers, true, false, 1 ); // 1 per sec
			if( out != null )
			{
				//System.out.println("returning " + out.size() + " immediate unchokes");
			}
			else
			{
				out = new ArrayList(0);
			}
		}
		else
			out = new ArrayList(0);
		return out;
	}

	public ArrayList getUnchokes() {
		ArrayList out = global.unchokes_for_swarm(mSwarm);
		return out;
	}
}
