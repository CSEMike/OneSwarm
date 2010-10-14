package edu.uw.cse.netlab.reputation.messages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

import edu.uw.cse.netlab.reputation.storage.Receipt;
import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.ByteManip;

public class ReceiptBundle implements Message
{
	public static final String ID_OS_RECEIPT_BUNDLE = "OS_RECEIPT_BUNDLE";
		
	byte mVersion;
	
	Receipt [] mReceipts;
	int [] received_reco_offsets;

	DirectByteBuffer buffer = null;
	
	public Receipt [] getReceipts() { return mReceipts; }
	public int [] getReceivedDueToRecoOffsets() { return received_reco_offsets; }
	
	public ReceiptBundle( Receipt [] inReceipts, List<Integer> inReceivedRecoOffsets, byte inVersion )
	{
		this(inReceipts, (int[])null, inVersion);
		received_reco_offsets = new int[inReceivedRecoOffsets.size()];
		for( int i=0; i<inReceivedRecoOffsets.size(); i++ )
			received_reco_offsets[i] = inReceivedRecoOffsets.get(i);
	}
	
	public ReceiptBundle( Receipt [] inReceipts, int [] inReceivedRecoOffsets, byte inVersion )
	{
		mReceipts = inReceipts;
		mVersion = inVersion;
		this.received_reco_offsets = inReceivedRecoOffsets;
	} 
	
	public String toString()
	{
		return "[ReceiptBundle] -- includes " + mReceipts.length + " receipts";
	}

	public Message deserialize( DirectByteBuffer data, byte version )
			throws MessageException 
	{
		Map root = MessagingUtil.convertBencodedByteStreamToPayload( data, 10, getID() );

		try
		{
			Receipt [] receipts = (Receipt[]) ByteManip.objectFromBytes((byte[])root.get("receipts"));
			int [] received_reco_offsets = (int[]) ByteManip.objectFromBytes((byte[])root.get("offsets"));
			
			return new ReceiptBundle(receipts, received_reco_offsets, version);
		} 
		catch( Exception e )
		{
			throw new MessageException(e.toString());
		}
	}

	public void destroy() 
	{
		if( buffer != null )
			buffer.returnToPool();
	}

	public DirectByteBuffer[] getData() {
		try 
		{
			if( buffer == null )
			{
				HashMap payload = new HashMap();
				payload.put("receipts", ByteManip.objectToBytes(mReceipts));
				payload.put("offsets", ByteManip.objectToBytes(received_reco_offsets));
				buffer = MessagingUtil.convertPayloadToBencodedByteStream( payload, DirectByteBuffer.SS_NONE );
			}
			return new DirectByteBuffer[]{buffer};
		}
		catch( IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public String getDescription() {
		return "ReceiptBundle";
	}

	public String getFeatureID() {
		return ID_OS_RECEIPT_BUNDLE;
	}

	public int getFeatureSubID() {
		return 0;
	}

	public String getID() {
		return ID_OS_RECEIPT_BUNDLE;
	}

	public byte[] getIDBytes() {
		return ID_OS_RECEIPT_BUNDLE.getBytes();
	}

	public int getType() {
		return 0;
	}

	public byte getVersion() {
		return mVersion;
	}

}
