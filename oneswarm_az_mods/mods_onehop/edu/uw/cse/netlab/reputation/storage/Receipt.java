package edu.uw.cse.netlab.reputation.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Date;
import java.util.Set;

import org.gudy.azureus2.core3.util.ByteFormatter;

import edu.uw.cse.netlab.reputation.LocalIdentity;
import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.KeyManipulation;

public class Receipt implements Serializable
{
	private static final long serialVersionUID = 1L;

	PublicKey mSigning = null;

	PublicKey mEncodingStateFor = null;

	/**
	 * This ensures that the receiver updates to mediating intermediaries will
	 * be faithful to the reported attribution sent after the top K set. This is
	 * null if the exchange is not mediated by any intermediaries.
	 */
	BloomFilter mOnBehalfOf = null;

	/**
	 * We use this locally to tag a connection's attribution with incoming
	 * receipts so we know which intermediaries to update. Although this
	 * information isn't sent over the wire, we need to keep track of it in our
	 * local DB in case the intermediary is not available during the lifetime of
	 * the connection.
	 */
	private transient Set<Long> mPreferredIntermediaries = null;

	public Set<Long> getPreferredIntermediaries() {
		return mPreferredIntermediaries;
	}

	public void setPreferredIntermediaries( Set<Long> inPrefs ) {
		mPreferredIntermediaries = inPrefs;
	}

	transient long mSenderID = -1, mReceiverID = -1;

	transient byte[] mHash = null;

	/*
	 * A subset of the information available in the state table -- this reflects
	 * the fact that we're only interested in mIntermediary's opinion as an
	 * intermediary (remaining info can be recovered by requesting a receipt
	 * from the peer)
	 */
	long sent_direct = 0, received_direct = 0;

	long peer_received_due_to_reco = 0, peer_sent_to_recos = 0;

	/*
	 * Since receipts also serve as a diff to mediating intermediaries, include
	 * the sent_direct_diff
	 */
	int sent_direct_diff = 0;

	/*
	 * This is scratch to hold the offset from the receipt bundle. We use this
	 * later during verification of receipts to check for cheating, but no need
	 * to send it over the wire twice (further, it can't be signed by the
	 * intermediary that signed this receipt)
	 */
	transient int peer_received_due_to_reco_offset = 0;

	byte[] mSignature = null;

	public byte[] getSignature() {
		return mSignature;
	}

	Date timestamp = null;

	public Date getTimestamp() {
		return timestamp;
	}

	public BloomFilter getOnBehalfOf() {
		return mOnBehalfOf;
	}

	public int get_received_due_to_reco_offset() {
		return peer_received_due_to_reco_offset;
	}

	public void set_received_due_to_reco_offset( int inOffset ) {
		peer_received_due_to_reco_offset = inOffset;
	}

	public boolean equals( Object rhs ) {
		if (rhs instanceof Receipt)
		{
			Receipt r = (Receipt) rhs;

			boolean mOnBehalf = false;

			if (mOnBehalfOf != null && r.mOnBehalfOf != null)
				mOnBehalf = mOnBehalfOf.equals(r.mOnBehalfOf);
			else
				mOnBehalf = mOnBehalfOf == null && r.mOnBehalfOf == null;

			return r.mSigning.equals(mSigning)
					&& r.mEncodingStateFor.equals(mEncodingStateFor)
					&& mOnBehalf
					&& r.sent_direct == this.sent_direct
					&& r.sent_direct_diff == this.sent_direct_diff
					&& r.received_direct == this.received_direct
					&& r.peer_received_due_to_reco == this.peer_received_due_to_reco
					&& r.peer_sent_to_recos == this.peer_sent_to_recos
					&& r.timestamp.equals(this.timestamp);
		}
		return false;
	}

	public long get_sent_direct() {
		return sent_direct;
	}

	public long get_received_direct() {
		return received_direct;
	}

	public long get_peer_received_due_to_reco() {
		return peer_received_due_to_reco;
	}

	public long get_peer_sent_to_recos() {
		return peer_sent_to_recos;
	}

	public PublicKey getSigningKey() {
		return mSigning;
	}

	public PublicKey getEncodingStateForKey() {
		return mEncodingStateFor;
	}

	public int get_sent_direct_diff() {
		return sent_direct_diff;
	}

	public long getSigningID() throws IOException {
		return ReputationDAO.get().get_internal_id(mSigning);
	}

	public long getEncodingStateForID() throws IOException {
		return ReputationDAO.get()
					.get_internal_id(mEncodingStateFor);
	}

	private byte[] get_protected_bytes() throws IOException {
		ByteArrayOutputStream summary_bytes = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(summary_bytes);
		dos.writeLong(get_sent_direct());
		dos.writeLong(get_received_direct());
		dos.writeLong(get_peer_received_due_to_reco());
		dos.writeLong(get_peer_sent_to_recos());
		dos.writeInt(get_sent_direct_diff());
		summary_bytes.write(mSigning.getEncoded());
		summary_bytes.write(mEncodingStateFor.getEncoded());
		ObjectOutputStream oos = new ObjectOutputStream(summary_bytes);
		oos.writeObject(timestamp);
		oos.writeObject(mOnBehalfOf);
		oos.close();
		return summary_bytes.toByteArray();
	}

	public void generate_signature() throws IOException {
		byte[] bytes = get_protected_bytes();
		try
		{
			Signature s = Signature.getInstance("SHA1withRSA");
			s.initSign(LocalIdentity.get().getKeys().getPrivate());
			s.update(bytes);
			byte[] sigBytes = s.sign();
			mSignature = sigBytes;
		} catch( Exception e )
		{
			System.err.println(e);
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}

	private void writeObject( ObjectOutputStream out ) throws IOException {
		if (mSignature == null)
			generate_signature();
		out.defaultWriteObject();
	}

	private void readObject( ObjectInputStream in ) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		verify_signature();
	}

	public void verify_signature() throws IOException {
		byte[] bytes = get_protected_bytes();
		try
		{
			Signature s = Signature.getInstance("SHA1withRSA");
			s.initVerify(mSigning);
			s.update(bytes);
			if (s.verify(mSignature) == false)
				throw new IOException("receipt check failed!");
		} catch( Exception e )
		{
			System.err.println(e);
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}

	public Receipt(PublicKey inEncodingStateFor, BloomFilter inOnBehalfOf,
			int inDiff, boolean inInflate) throws IOException
	{
		this(inEncodingStateFor, inOnBehalfOf);
		sent_direct_diff = inDiff;
		if (inInflate)
		{
			inflate();
		}
	}

	private void inflate() {
		/**
		 * TODO: magic number This is only used for indirect computation. We
		 * will rely on our local state for direct interaction
		 */
		received_direct *= 100;
	}

	/**
	 * Constructs a receipt from our perspective for inRemotePeer
	 * 
	 * @param inRemotePeer
	 *            The peer for which to mint the receipt
	 */
	public Receipt(PublicKey inEncodingStateFor, BloomFilter inOnBehalfOf)
			throws IOException
	{
		mSigning = LocalIdentity.get().getKeys().getPublic();
		mEncodingStateFor = inEncodingStateFor;

		mOnBehalfOf = inOnBehalfOf;

		ReputationDAO rep = ReputationDAO.get();

		sent_direct = rep.get_sent_direct(getEncodingStateForID());
		received_direct = rep.get_received_direct(getEncodingStateForID());

		peer_received_due_to_reco = rep
				.get_others_sent_due_to_my_reco(getEncodingStateForID());
		peer_sent_to_recos = rep
				.get_others_recv_due_to_my_reco(getEncodingStateForID());

		timestamp = new Date();
	}

	public Receipt(PublicKey inEncodingStateFor, BloomFilter inOnBehalfOf,
			boolean inInflate) throws IOException
	{
		this(inEncodingStateFor, inOnBehalfOf);
		if (inInflate)
			inflate();
	}

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception {

		
	}

	public byte[] getSHA1() {
		if (mHash == null)
		{
			try
			{
				MessageDigest digest = MessageDigest.getInstance("SHA-1");
				mHash = digest.digest(this.get_protected_bytes());
			} catch( Exception e )
			{
				System.err.println("Error generating receipt hash: " + e);
				e.printStackTrace();
				return null;
			}
		}
		return mHash;
	}

	public String toString() {
		long sid = -1;
		long bid = -1;

		try
		{
			sid = ReputationDAO.get().get_internal_id(mSigning);
			bid = ReputationDAO.get().get_internal_id(mEncodingStateFor);
		} catch( Exception e )
		{
			e.printStackTrace();
		}

		return "\nSigner: " + sid + "\nEncoding for: " + bid
				+ "\non behalf of: " + mOnBehalfOf + "\nSent:" + "\n\tDirect: "
				+ sent_direct + " To recos: " + peer_sent_to_recos
				+ "\nReceived: " + "\n\tDirect: " + received_direct
				+ " To recos: " + peer_received_due_to_reco + "\nTime: "
				+ timestamp.toGMTString() + "\nDiff: " + get_sent_direct_diff();
	}
}
