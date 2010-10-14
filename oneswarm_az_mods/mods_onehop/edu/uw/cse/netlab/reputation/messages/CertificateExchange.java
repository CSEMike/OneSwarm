package edu.uw.cse.netlab.reputation.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

import edu.uw.cse.netlab.reputation.LocalIdentity;
import edu.uw.cse.netlab.reputation.storage.LocalTopK;
import edu.uw.cse.netlab.reputation.storage.ReputationDAO;
import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.ByteManip;
import edu.uw.cse.netlab.utils.KeyManipulation;

public class CertificateExchange implements Message
{
	public static final String ID_OS_CERT_EXCHANGE = "OS_CERT_EXCHANGE";
	
	public static final String CERT_FEATURE_ID = "OS_CERTS";
	
	X509Certificate mCert = null;
	BloomFilter mBF = null;
	PublicKey [] mAdvertisements = new PublicKey[0];
	
	DirectByteBuffer buffer = null;
	
	byte mVersion;
	
	public CertificateExchange( X509Certificate inCert, BloomFilter inTopKFilter, PublicKey[] inAdvertised, byte inVersion )
	{
		mCert = inCert;
		mVersion = inVersion;
		mBF = inTopKFilter;
		mAdvertisements = inAdvertised;
	}
	
	public X509Certificate getCertificate() { return mCert; }
	public BloomFilter getTopK() { return mBF; } 
	
	public PublicKey [] getAdvertisements() { return mAdvertisements; }
	
	public byte getVersion() { return mVersion; }
	
	public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException
	{	
		Map root = MessagingUtil.convertBencodedByteStreamToPayload( data, 10, getID() );

		try
		{
			BloomFilter bf = (BloomFilter)ByteManip.objectFromBytes((byte[])root.get("bloom"));
			byte [][] direct = (byte[][])ByteManip.objectFromBytes((byte[])root.get("adverts"));
			PublicKey [] ad_keys=null;
			if( direct.length > 50 )
				throw new MessageException( "direct advertisements is too large: " + direct.length );
			ad_keys =  new PublicKey[direct.length];
			for( int i=0; i<direct.length; i++ )
				ad_keys[i] = KeyManipulation.keyForEncodedBytes(direct[i]);
		
			return new CertificateExchange((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream((byte[])root.get("cert"))), 
					bf, 
					ad_keys, 
					version);
		} 
		catch (Exception e)
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
			if( mCert == null )
				return null;
			try 
			{
				byte [] mCertBytes = mCert.getEncoded();
				HashMap test = (new HashMap());
				test.put("cert", mCertBytes);
				
				LocalTopK topK = ReputationDAO.get().get_topK_by_obs();
				test.put("bloom", ByteManip.objectToBytes(topK.getBloomFilter()));
			
				byte b [][] = new byte[mAdvertisements.length][];
				for( int i=0; i<mAdvertisements.length; i++ )
					b[i] = mAdvertisements[i].getEncoded();

				test.put("adverts", ByteManip.objectToBytes(b));
				buffer = MessagingUtil.convertPayloadToBencodedByteStream( test, DirectByteBuffer.SS_MSG );
			}
			catch( Exception e ) 
			{ 
				System.err.println("Couldn't convert non-null cert for exchange: " + e);
				e.printStackTrace();
				return null; 
			}
		}
		return new DirectByteBuffer[]{buffer};
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		if( mCert == null )
			return "CertificateExchange: null";
		else
			return "CertificateExchange: " + mCert.toString();
	}

	public String getFeatureID()
	{
		return CertificateExchange.CERT_FEATURE_ID;
	}

	public int getFeatureSubID()
	{
		return 0;
	}

	public String getID()
	{
		return ID_OS_CERT_EXCHANGE;
	}

	public byte[] getIDBytes()
	{
		return getID().getBytes();
	}

	public int getType()
	{
		return 0;
	}
	
	public String toString()
	{
		return mCert + " " + mBF + " indirect ads: " + mAdvertisements.length;
	}
	
	public static final void main( String [] args ) throws Exception
	{
		BloomFilter bf = new BloomFilter(8192*3, 2000);
		
		byte [] rand = new byte[20];
		Random r = new Random(5);
		r.nextBytes(rand);
		
		bf.insert(rand);
		
		CertificateExchange mine = new CertificateExchange(LocalIdentity.get().getCertificate(), bf, new PublicKey[0], (byte)0);
		DirectByteBuffer bytes = mine.getData()[0];
		CertificateExchange dup = (CertificateExchange)(new CertificateExchange(null, null, null, (byte)0)).deserialize(bytes, (byte)0);
	}
	
}
