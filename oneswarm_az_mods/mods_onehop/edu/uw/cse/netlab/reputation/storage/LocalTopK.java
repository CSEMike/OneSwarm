package edu.uw.cse.netlab.reputation.storage;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.gudy.azureus2.core3.util.ByteFormatter;

import edu.uw.cse.netlab.utils.BloomFilter;
import edu.uw.cse.netlab.utils.KeyManipulation;


/**
 * 
 * This bundles together the bloom filter as well as a current topK set. 
 * 
 */
public class LocalTopK
{
	BloomFilter mBF = null;
	PublicKey [] mKeys = null; // by occurrences
	
	public LocalTopK( PublicKey [] inKeys )
	{
		mKeys = inKeys;
		compute_bf();
	}
	
	public static BloomFilter createTopK_BF(int inKeysToStore)
	{
		try { 
			return new BloomFilter(8192*3, Math.max(Math.min(inKeysToStore, 2000), 2000));
		} 
		catch( NoSuchAlgorithmException e ) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private void compute_bf()
	{
		mBF = LocalTopK.createTopK_BF(mKeys.length);
		for( PublicKey k : mKeys )
		{
			mBF.insert(k.getEncoded());
			System.out.println("inserting key: " + ByteFormatter.encodeString(k.getEncoded()) );
			if( mBF.test(k.getEncoded()) == false )
				System.err.println("******** lookup failed for key we just inserted"); 
		}	
	}
	
	public PublicKey [] getKeys() { return mKeys; }
	public BloomFilter getBloomFilter() { return mBF; }
	
	public String toString() 
	{
		StringBuilder sb = new StringBuilder("[TopK] " + mBF.toString());
		
		for( PublicKey k : mKeys )
		{
			try
			{
				sb.append("\n" + KeyManipulation.concise(k.getEncoded()) + " (" + ReputationDAO.get().get_internal_id(k) + ")");
			} catch( Exception e ) {}	
		}
		
		return sb.toString();
	}
}
