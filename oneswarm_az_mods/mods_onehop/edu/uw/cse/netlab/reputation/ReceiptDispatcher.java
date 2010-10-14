package edu.uw.cse.netlab.reputation;

import java.io.IOException;
import java.security.PublicKey;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerListener;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

import edu.uw.cse.netlab.reputation.messages.Attestation;
import edu.uw.cse.netlab.reputation.storage.Receipt;
import edu.uw.cse.netlab.reputation.storage.ReputationDAO;

public class ReceiptDispatcher
{
	private static Logger logger = Logger.getLogger(ReceiptDispatcher.class.getName());
	
	PEPeerTransportProtocol mPeer;

	Attestation mLastAttestation = null;

	long mLastSent = 0;

	long mLastBytesReceived = 0;

	PEPeerStats mStats;

	/**
	 * Have we yet recorded this peer's indirect observations from available intermediaries into the DAO?
	 */
	private boolean mRecordedIndirect = false;
	
	private boolean mDisconnectAttempt = false;

	public ReceiptDispatcher(PEPeerTransportProtocol inPeer)
	{
		mPeer = inPeer;
		mStats = mPeer.getStats();
		mLastBytesReceived = mStats.getTotalDataBytesReceived();
		mLastSent = System.currentTimeMillis(); // don't want to send an attestation right away
	}

	private void send() 
	{
		long old = mLastBytesReceived;
		mLastBytesReceived = mStats.getTotalDataBytesReceived();
		mLastSent = System.currentTimeMillis();
		mLastAttestation = mPeer.sendAttestation(mLastBytesReceived - old);
	}

	public void check( boolean inPendingDisconnect ) {
		
		// Avoid stack overflow if there are exceptions
		if( mDisconnectAttempt )
			return;
		
		/**
		 * Criteria for sending an attestion: 1. We're about to disconnect
		 * (inPendingDisconnect is true) 2. It's been 10+ minutes since our last
		 * receipt and something has changed 3. We've transferred 10+ MB.
		 */

		if ((inPendingDisconnect && mStats.getTotalDataBytesSent() > 0)
				|| (mLastSent + 10 * 60 * 1000) < System.currentTimeMillis()
				|| (mLastBytesReceived + 10 * 1024 * 1024) < mStats.getTotalDataBytesReceived())
		{
			if( inPendingDisconnect )
				mDisconnectAttempt = true;
			
			logger.fine("check indicates sending of receipt to " + mPeer.getIPHostName() + " " + mPeer.getDirectAdvertisements() == null ? "no direct ads" : "peer has direct advertisements");
//			System.out.println(new Boolean(inPendingDisconnect && mStats.getTotalDataBytesSent() > 0));
//			System.out.println(new Boolean( (mLastSent + 10 * 60 * 1000) < System.currentTimeMillis()));
//			System.out.println(new Boolean( (mLastBytesReceived + (10 * 1024 * 1024)) < mStats.getTotalDataBytesReceived()));
			send();
			
			if( mPeer.getDirectAdvertisements() != null && mRecordedIndirect  == false )
			{
				ReputationDAO rep = ReputationDAO.get();
				mRecordedIndirect = true;
				PublicKey [] ads = mPeer.getDirectAdvertisements();
				try {
					for( PublicKey k : ads )
						rep.indirect_observation(rep.get_internal_id(k), Computation.indirect_advertisements_weight(mPeer));
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}
	}
}
