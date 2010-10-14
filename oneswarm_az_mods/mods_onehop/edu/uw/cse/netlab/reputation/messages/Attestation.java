package edu.uw.cse.netlab.reputation.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

import edu.uw.cse.netlab.reputation.storage.Receipt;
import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.ByteManip;
import edu.uw.cse.netlab.utils.KeyManipulation;

public class Attestation implements Message
{
	public static final String ID_OS_ATTESTATION = "OS_ATTESTATION";

	Receipt mReceipt = null;
	
	DirectByteBuffer buffer = null;
	byte mVersion;
	
	public Attestation( PEPeerTransportProtocol inPeer, int inDiff, byte inVersion ) throws IOException
	{
		Set<PublicKey> attrib = inPeer.getAttribution();
		BloomFilter bf = null;
		try {
			if( attrib != null )
			{
				bf = new BloomFilter(128, 10);
				for( PublicKey k : attrib )
					bf.insert(k.getEncoded());
			}
		} 
		catch( NoSuchAlgorithmException e )
		{
			throw new IOException(e.toString());
		}
		mReceipt = new Receipt(inPeer.getCertificate().getPublicKey(), bf, inDiff, true);
		
		mVersion = inVersion;
	}
	
	public Attestation( PEPeerTransportProtocol inPeer, byte inVersion ) throws IOException
	{
		this(inPeer, 0, inVersion);
	}
	
	public Attestation( Receipt inReceipt, byte inVersion )
	{
		mVersion = inVersion;
		mReceipt = inReceipt;
	}
	
	public Receipt getReceipt() { return mReceipt; }
	
	public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException 
	{
		// TODO: figure out what these min data values should actually be...
		try
		{
			Map root = MessagingUtil.convertBencodedByteStreamToPayload( data, 512, getID() );
			Receipt r = (Receipt) ByteManip.objectFromBytes((byte[])root.get("receipt"));
			return new Attestation(r, version);
		} 
		catch( Exception e )
		{
			throw new MessageException(e.toString());
		}
	}

	public void destroy() 
	{
		if( buffer != null )
		{
			buffer.returnToPool();
			buffer = null;
		}
	}

	public DirectByteBuffer[] getData() 
	{
		if( buffer == null )
		{
			try
			{
				HashMap test = (new HashMap());
				test.put("receipt", ByteManip.objectToBytes(mReceipt));
				
				buffer = MessagingUtil.convertPayloadToBencodedByteStream( test, DirectByteBuffer.SS_MSG );
			}
			catch( Exception e )
			{
				System.err.println("Couldn't generate attestation: " + e);
				return null; 
			}
		}
		return new DirectByteBuffer[]{buffer};
	}

	public String getDescription() 
	{
		return "Attestation";
	}

	public String getFeatureID() {
		return ID_OS_ATTESTATION;
	}

	public int getFeatureSubID() {
		return 0;
	}

	public String getID() {
		return ID_OS_ATTESTATION;
	}

	public byte[] getIDBytes() {
		return ID_OS_ATTESTATION.getBytes();
	}

	public int getType() {
		return 0;
	}

	public byte getVersion() {
		return mVersion;
	}
	
	public String toString()
	{
		return "[Attestation] Receipt: " + mReceipt.toString();
	}
	
	public static final void main( String [] args ) throws Exception
	{
		BloomFilter bf = new BloomFilter(128,10);
		
		KeyPair p = KeyManipulation.randomKey();
		Receipt r = new Receipt(p.getPublic(), bf);
		
//		ByteManip.objectFromBytes(ByteManip.objectToBytes(r));
		
		Attestation first = new Attestation(r, (byte)0);		
		Attestation second = (Attestation)first.deserialize(first.getData()[0], (byte)0);
	}

}
