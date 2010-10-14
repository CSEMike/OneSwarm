package edu.uw.cse.netlab.reputation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;

import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.impl.PRUDPPacketHandlerImpl;

import edu.uw.cse.netlab.reputation.messages.Attestation;
import edu.uw.cse.netlab.reputation.storage.Receipt;
import edu.uw.cse.netlab.reputation.storage.ReputationDAO;
import edu.uw.cse.netlab.reputation.storage.SoftStateListener;
import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.ByteManip;
import edu.uw.cse.netlab.utils.CoreWaiter;
import edu.uw.cse.netlab.utils.KeyManipulation;

class UpdateReceiptWrapper implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public long seq;
	public Receipt receipt;
	
	public UpdateReceiptWrapper( long inSeq, Receipt inReceipt )
	{
		receipt = inReceipt;
		seq = inSeq;
	}
}

class SentUpdate
{
	// Who did we ask
	public PublicKey intermediary;
	// Who did we ask about
	public PublicKey receiver;
	
	// For timing out short-term attempts
	public long		time_sent;
	
	// these are SHORT TERM attempts to combat packet loss rather than actual downtime
	public short 	attempts;
	
	/**
	 *  This is the attested receipt: i.e., the (potentially outdated) receipt from the intermediary that was forwarded to us 
	 *  by the would-be receiver
	 */
	public Receipt	receipt;
	
	static private long seq = 0; // for multiple outstanding intermediary <-> receiver reqs
	public long our_seq = 0;
	
	public long attest_id = -1;
	
	public SentUpdate(PublicKey inIntermediary, PublicKey inReceiver, Receipt inReceipt, Long attest_id) {
		our_seq = seq++;
		intermediary = inIntermediary;
		receiver = inReceiver;
		time_sent = System.currentTimeMillis();
		attempts = 0;
		receipt = inReceipt;
		this.attest_id = attest_id.longValue();
	}
	
	public boolean equals( Object rhs )
	{
		if( rhs instanceof SentUpdate )
		{
			SentUpdate r = (SentUpdate)rhs;
			return r.intermediary.equals(intermediary) &&
				r.receiver.equals(receiver) &&
				r.time_sent == time_sent &&
				r.attempts == attempts;
		}
		return false;
	}
}

public class UpdatePacketHandler extends CoreWaiter
{
	public static final byte [] MAGIC = new byte[]{(byte)0, (byte)0, (byte)0, (byte)211, (byte)23};
	
	private static Logger logger = Logger.getLogger(UpdatePacketHandler.class.getName());
	
	private DatagramSocket mSocket;
	
	private static UpdatePacketHandler mInst = null;
	public synchronized static UpdatePacketHandler get() 
	{ 
		if( mInst == null )
			mInst = new UpdatePacketHandler();
		
		return mInst; 
	}
	
	List<SentUpdate> outstanding_updates;
	
	/**
	 * This thread periodically checks for updates from the DB that need 
	 * verified at intermediaries and sends them off. 
	 */
	AEThread2 mUpdateScanner;
	
	private UpdatePacketHandler()
	{
		super();
	}
	
	protected void init()
	{
		logger.info("update packet handler init()");
		
		outstanding_updates = Collections.synchronizedList(new LinkedList<SentUpdate>());
		
		mUpdateScanner = new AEThread2("update scanner", true)
		{
			public void run()
			{
				ReputationDAO rep = ReputationDAO.get();
				long last_packet = System.currentTimeMillis();
				while( true )
				{
					try {
						rep.get_attestation_for_verification();
						if( rep != null )
						{
							Thread.sleep(30*1000);
							continue;
						}
					} catch( Exception e ) {
						e.printStackTrace();
					}
					logger.fine("receipt verification scanner is running...");
					
					try
					{
						SentUpdate update = null;
						
						/**
						 * First, try to clear / timeout any existing updates that are outstanding
						 */
						List<SentUpdate> to_remove = new LinkedList<SentUpdate>();
						synchronized(outstanding_updates) {
							for( SentUpdate candidate : outstanding_updates )
							{
								logger.fine("inspecting outstanding update to: " + KeyManipulation.concise(candidate.intermediary.getEncoded()));
								// Timeout check
								// TODO: magic constant: 2 second timeout on UDP messages. 
								if( candidate.time_sent != 0 && (candidate.time_sent + 2000) < System.currentTimeMillis() )
								{
									candidate.attempts++;
									candidate.time_sent = 0;
									logger.fine("timeout");
								}
								
								// Intermediary presumed unavailable
								// TODO: magic constant: 3 retries for UDP send. 
								if( candidate.attempts == 3 )
								{
									to_remove.add(candidate);
									continue;
								}
								
								// This message is still outstanding so don't resent request
								if( candidate.time_sent != 0 )
									continue;
								
								// At this point, we have something to retry (either we completed an IP lookup or its time to retry)
								update = candidate;
								logger.fine("found update to verify in outstanding updates");
							}
						}
						
						// prune dead entries
						for( SentUpdate dead : to_remove )
						{
							// Two possible reasons this failed: 1) host actually unavailable or 2) out of date IP mapping.
							// Here register a check to the mapping. Eventually we will long-term retry this update. 
							rep.getSoftStateSync().refreshRemoteID(dead.intermediary, null);
							outstanding_updates.remove(dead);
						}
						
						// If nothing to do locally, queue up a new update from the DB if any exist. 
						if( update == null )
						{
							Object [] tuple = rep.get_attestation_for_verification();
							Receipt to_verify = (Receipt)tuple[0];
							Long which_intermediary = (Long)tuple[1];
							Long attest_id = (Long)tuple[2];
							
							// Absolutely nothing to verify, wait a while before trying again. (DEBUG -- don't)   
							if( to_verify == null )
							{
								logger.finest("nothing to verify, sleeping...");
								try { 
									// TODO: magic constant
									Thread.sleep(30*1000);
								} catch( Exception e ) {}
								continue;
							}
							
							logger.fine("got receipt to verify: " + to_verify);
							
							outstanding_updates.add(new SentUpdate(
									rep.get_public_key(which_intermediary), 
									to_verify.getEncodingStateForKey(), 
									to_verify, 
									attest_id));
						}
						else	
						{
							final SentUpdate update_shadow = update;
							
							logger.fine("executing update: " + update + " checking soft state" + "(" + update_shadow.hashCode() + ")" );
							
							/**
							 * First, do we have this intermediary's IP _at all_? 
							 */
							String sstate = rep.get_soft_state(update_shadow.intermediary);
							logger.finer("intermediary key: " + ByteFormatter.encodeString(update_shadow.intermediary.getEncoded()));
							if( sstate == null )
							{
								logger.finer("didn't have soft state, attempting refresh and removing from outstanding updates: " + KeyManipulation.concise(update.intermediary.getEncoded()));
								// no need to try this 3 times and lodge 3 dht lookups
								outstanding_updates.remove(update);
								rep.getSoftStateSync().refreshRemoteID(update_shadow.intermediary, new SoftStateListener(){
										public void refresh_complete( PublicKey inID ) 
										{
											// promote update to the front of the list
											synchronized(outstanding_updates)
											{
												logger.finer("soft state refresh finished, reinserting update into outstanding updates: " + KeyManipulation.concise(update_shadow.intermediary.getEncoded()) + " (" + update_shadow.hashCode() + ")");
												outstanding_updates.remove(update_shadow);
												update_shadow.time_sent = 0;
												outstanding_updates.add(0, update_shadow);
											}
										}
									});
								continue;
							}
							
							logger.finer("got soft state");
							
							InetAddress addr = InetAddress.getByName(sstate.split("\\s+")[0]);
							int port = Integer.parseInt(sstate.split("\\s+")[1]);
							
							UpdateReceiptWrapper out = new UpdateReceiptWrapper( update_shadow.our_seq, update_shadow.receipt );
							
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							baos.write(MAGIC);
							baos.write(ByteManip.objectToBytes(out));
							byte [] receipt_bytes = baos.toByteArray();
							
							update.attempts++;
							DatagramPacket p = new DatagramPacket( receipt_bytes, receipt_bytes.length, addr, port );
							/** 
							 * Rate limit -- 1 packet / sec
							 * TODO: magic constant: rate limit 1/sec
							 */
							try {
								Thread.sleep(1000 - (System.currentTimeMillis()-last_packet));
							} catch( Exception e ) {} 
							logger.fine("******* sending verify receipt packet ********** length: " + receipt_bytes.length + " to " + addr + " port " + port);
							
							mSocket.send(p);
							update.time_sent = last_packet = System.currentTimeMillis();
						}
					}
					catch( Exception e ) 
					{
						e.printStackTrace();
						logger.warning("Update scanner: " + e.toString());
						try {
							Thread.sleep(1000);
						} catch( Exception e2 ) {}
					}
				}
			} // run
		}; // mUpdateScanner def
		
		logger.finest("getting udp port handler");
		
		// Get the socket for the udp.listen.port
		PRUDPPacketHandlerImpl handler = (PRUDPPacketHandlerImpl)(PRUDPPacketHandlerFactory.getHandler(COConfigurationManager.getIntParameter("UDP.Listen.Port")));
		mSocket = handler.getSocket();
		
		logger.finest("got handler on port: " + mSocket.getLocalPort() + " sock is: " + mSocket);
		logger.fine("starting update scanner thread");
		mUpdateScanner.start();
	}
	
	/**
	 * 
	 * This should be called only by our modified UDP packet handler. We might receive here from 
	 * any UDP port so we can't rely only on the socket we have here... 
	 * 
	 * @param packet the packet to check
	 * @return true if this was an update packet that we processed, false otherwise
	 */
	public boolean packetReceived( DatagramPacket packet ) 
	{
		byte [] data = packet.getData();
		
		for( int i=0; i<MAGIC.length; i++ )
			if( MAGIC[i] != data[i] )
				return false;
		
		logger.finer("******** packet received, passed magic header check ********** " + " port " + packet.getPort() + " " + packet.getLength() + " " + packet.getData().length);
				
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		bais.skip(MAGIC.length);
		
		try
		{
			ObjectInputStream ois = new ObjectInputStream(bais);
			UpdateReceiptWrapper r = (UpdateReceiptWrapper)ois.readObject();
			
			if( isUpdate(r) )
				receiveUpdate(r, packet);
			else 
			{
				SentUpdate s = isAcknowledgement(r);
				if( s != null )
					receiveAck(s, r);
				else
					throw new IOException("neither an update nor an ack receipt: " + r);
			}
			
			return true;
		}
		catch( Exception e )
		{
			logger.warning("Error parsing update message (after magic number): " + e.toString());
		}
		return false;
	}
	
	private void receiveAck( SentUpdate inUpdate, UpdateReceiptWrapper r ) throws IOException
	{
		logger.finer("processing acknowledgement from " + KeyManipulation.concise(inUpdate.intermediary.getEncoded()));
		
		/**
		 * We need to make sure the intermediary's state is >= the claimed standing by the remote peer
		 * (otherwise, either the remote peer is replaying receipts from the intermediary too many times or the 
		 * intermediary is lying)
		 */
		Receipt reported_by_intermediary = r.receipt;
		Receipt given_by_peer = inUpdate.receipt;
		long received_due_to_reco_offset = given_by_peer.get_received_due_to_reco_offset();
		
		// TODO: add more sanity checks here -- data directly sent/received, etc. 
		
		/**
		 * For now, we'll assume that the intermediary will at least be consistent with itself (i.e. direct received strictly increasing -- but we should check this in the future)  
		 * and instead check for the repeated use of old receipts. 
		 */
		if( given_by_peer.get_peer_received_due_to_reco() - received_due_to_reco_offset > 
				reported_by_intermediary.get_peer_received_due_to_reco() )
		{
			// TODO: We've possibly detected cheating here. If we want to add some penalty for this, here is the place 
			logger.warning("possible replay / accounting irregularity: " + given_by_peer.get_peer_received_due_to_reco() + " " + 
					received_due_to_reco_offset + " " + reported_by_intermediary.get_peer_received_due_to_reco() );
		}
		else
			logger.finer("passed check, given by peer: " + given_by_peer.get_peer_received_due_to_reco() + " / offset: " +received_due_to_reco_offset + " reported by int: " + reported_by_intermediary.get_peer_received_due_to_reco());
		
		ReputationDAO.get().attestation_verification_complete(inUpdate.attest_id);
	}

	private SentUpdate isAcknowledgement( UpdateReceiptWrapper r ) 
	{
		PublicKey intermediary = r.receipt.getSigningKey();
		PublicKey regarding = r.receipt.getEncodingStateForKey();
		
		synchronized(outstanding_updates) {
			for( SentUpdate s : outstanding_updates )
			{
				if( s.intermediary.equals(intermediary) &&
					s.receiver.equals(regarding) &&
					r.seq == s.our_seq )
				{
					return s;
				}
			}
		}
		return null;
	}

	private boolean isUpdate( UpdateReceiptWrapper r ) 
	{
		BloomFilter bf = r.receipt.getOnBehalfOf();
		if( bf == null )
			return false;
		
		return bf.test(LocalIdentity.get().getKeys().getPublic().getEncoded());
	}

	public void receiveUpdate( UpdateReceiptWrapper r, DatagramPacket packet ) 
	{
		ReputationDAO db = ReputationDAO.get();
		
		try
		{
			/**
			 * If this update has already been applied, discard
			 */
			if( db.is_duplicate_update(r.receipt) )
			{
				// TODO: punish replay attack? -- more likely just a retransmit due to loss. fall through and send ack again here. 
				logger.warning("Seemingly a replay attack (or retransmit due to loss) from receipt: " + r);
			}
			else
			{
				/**
				 * Apply update to persistent storage. This might fail if:
				 * 	- the sender is not faithful to his attestation and
				 *  - the bloom filter allows a false positive with us and
				 *  - we don't have any local state for this peer
				 *  
				 * TODO: make this transactional? 
				 */
				if( r.receipt.getOnBehalfOf().getStoredCount() == -1 )
					throw new IOException("attesting receipt not correctly formatted, -1 stored count in onBehalfOf");
				
				logger.finer("received update, sent_direct_diff is: " + r.receipt.get_sent_direct_diff() + " stored count is: " + r.receipt.getOnBehalfOf().getStoredCount());
				
				db.others_recv_due_to_my_reco(r.receipt.getEncodingStateForID(), r.receipt.get_sent_direct_diff() / r.receipt.getOnBehalfOf().getStoredCount());
				db.others_sent_due_to_my_reco(r.receipt.getSigningID(), r.receipt.get_sent_direct_diff() / r.receipt.getOnBehalfOf().getStoredCount() );
				db.record_processed_update(r.receipt);
				
				// TODO: remove this, we record this information now only for debugging
				db.record_update(r.receipt);
				
				logger.fine("applied received update: " + 
						(r.receipt.get_sent_direct_diff() / r.receipt.getOnBehalfOf().getStoredCount()) );
			}
		}
		catch( IOException e )
		{
			System.err.println("Error applying update: " + e);
			e.printStackTrace();
			return;
		}
		
		/*
		 * next, need to ack this with a receipt indicating the state of the receiver 
		 * from our perspective. This receipt doesn't include any on behalf of info 
		 * and, since it includes state from us -> receiver, the sender (who will receive it 
		 * for verification) can't really use it for anything other than verification
		 */
		try
		{
			UpdateReceiptWrapper ack = new UpdateReceiptWrapper(r.seq, new Receipt(r.receipt.getEncodingStateForKey(), null, true));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(MAGIC);
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(ack);
			
			byte [] bytes = baos.toByteArray();
			mSocket.send(new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort()));
			logger.fine("sent ack to: " + packet.getAddress().toString() + " port " + packet.getPort());
		}
		catch( IOException e )
		{
			logger.warning("couldn't send update ack: " + e);
			e.printStackTrace();
			return;
		}
		
		logger.fine("applied received update " + r);
	}
	
	public static final void main( String [] args ) throws Exception
	{
		UpdatePacketHandler p = UpdatePacketHandler.get();
		FileInputStream fis = new FileInputStream("verifypacket21975073");
		byte [] b = new byte[fis.available()];
		fis.read(b);
		p.packetReceived(new DatagramPacket(b, b.length));
	}
}
