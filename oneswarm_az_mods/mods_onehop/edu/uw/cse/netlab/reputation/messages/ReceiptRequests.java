package edu.uw.cse.netlab.reputation.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.ByteManip;

public class ReceiptRequests implements Message
{
	public static final String ID_OS_RECEIPT_REQUESTS = "OS_RECEIPT_REQUESTS";
	
	DirectByteBuffer buffer = null;
	
	byte mVersion;
	Map<PublicKey, Float> mKeys =  null;
	float [] mAttribution = null;
	
	public ReceiptRequests( Map<PublicKey, Float> inKeys, byte inVersion )
	{
		mVersion = inVersion;
		mKeys = inKeys;
	}
	
	public Map<PublicKey, Float> getKeys()
	{
		return mKeys;
	}

	public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException 
	{
		Map root = MessagingUtil.convertBencodedByteStreamToPayload( data, 10, getID() );

		try
		{
			Map<PublicKey, Float> keys = (Map<PublicKey, Float>)ByteManip.objectFromBytes((byte[])root.get("keys"));
			return new ReceiptRequests(keys, version);
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

	public DirectByteBuffer[] getData() 
	{
		if( buffer == null )
		{
			try
			{
				HashMap payload = new HashMap();
				
				payload.put("keys", ByteManip.objectToBytes(mKeys));
				
				buffer = MessagingUtil.convertPayloadToBencodedByteStream( payload, DirectByteBuffer.SS_NONE );
			} 
			catch( Exception e )
			{
				System.err.println("error generating receipt request: " + e);
				e.printStackTrace();
				return null;
			}
		}
		return new DirectByteBuffer[]{buffer};
	}

	public String getDescription() 
	{
		return "Receipt request";
	}

	public String getFeatureID() {
		return null;
	}

	public int getFeatureSubID() {
		return 0;
	}

	public String getID() {
		return ID_OS_RECEIPT_REQUESTS;
	}

	public byte[] getIDBytes() {
		return ID_OS_RECEIPT_REQUESTS.getBytes();
	}

	public int getType() {
		return 0;
	}

	public byte getVersion() {
		return mVersion;
	}

}
