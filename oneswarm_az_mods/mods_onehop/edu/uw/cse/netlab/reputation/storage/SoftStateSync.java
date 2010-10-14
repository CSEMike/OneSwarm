package edu.uw.cse.netlab.reputation.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Date;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.uw.cse.netlab.reputation.LocalIdentity;
import edu.uw.cse.netlab.utils.ByteManip;
import edu.uw.cse.netlab.utils.CoreWaiter;
import edu.uw.cse.netlab.utils.KeyManipulation;


/**
 * 
 * This class is responsible for keeping our local table of soft state key -> IP mappings 
 * fresh. 
 * 
 */

class SoftState
{
	private static final long serialVersionUID = 1L;
	
	public byte [] ip;
	public int tcp_port;
	public int udp_port;
	public byte [] sig;
	public long [] salt = new long[]{
		(long)(Math.random()*Long.MAX_VALUE),
		(long)(Math.random()*Long.MAX_VALUE),
		(long)(Math.random()*Long.MAX_VALUE),
		(long)(Math.random()*Long.MAX_VALUE),
		(long)(Math.random()*Long.MAX_VALUE)
		};
	public Date timestamp = new Date();
	
	public String toString() 
	{
		try {
			return "IP: " + ip == null ? "null" : InetAddress.getByAddress(ip) + " TCP: " + tcp_port + " UDP: " + udp_port + " Timestamp: " + timestamp;
		} 
		catch( Exception e ) { return null; }
	}
	
	public byte [] getBytes() throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(protectedBytes());
		baos.write(sig);
		
		return baos.toByteArray();
	}
	
	public byte [] protectedBytes() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		for( int i=0; i<salt.length; i++ )
			dos.writeLong(salt[i]);
		
		dos.writeShort((short)ip.length);
		baos.write(ip);
		
		dos.writeShort((short)tcp_port);
		dos.writeShort((short)udp_port);
		
		dos.writeLong(timestamp.getTime());
		return baos.toByteArray();
	}
	
	public static SoftState fromBytes( byte [] inBytes ) throws IOException
	{
		SoftState s = new SoftState();
		ByteArrayInputStream bais = new ByteArrayInputStream(inBytes);
		DataInputStream dis = new DataInputStream(bais);
		
		for( int i=0; i<s.salt.length; i++ )
			s.salt[i] = dis.readLong();
		
		short ip_size = (short)dis.readShort();
		if( ip_size > 64 )
		{
			throw new IOException("bogus IP size");
		}
		
		s.ip = new byte[ip_size];
		bais.read(s.ip);
		s.tcp_port = dis.readShort();
		s.udp_port = dis.readShort();
		
		if( s.tcp_port < 0 )
			s.tcp_port += 65536;
		if( s.udp_port < 0 )
			s.udp_port += 65536;
		
		s.timestamp = new Date(dis.readLong());
		
		s.sig = new byte[64];
		bais.read(s.sig);
		
		return s;
	}
	
	public void sign_local()
	{
		sig = null;
		try
		{
			byte [] dat = protectedBytes();
			Signature s = Signature.getInstance("SHA1withRSA");
			s.initSign(LocalIdentity.get().getKeys().getPrivate());
			s.update(dat);
			sig = s.sign();
		} catch( Exception e ) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
};

public class SoftStateSync extends CoreWaiter
{
	// For local state refresh and lookups
	public static final int TIMEOUT_MINUTES = 3;
	
	DistributedDatabase mDB;
	
	public SoftStateSync()
	{
		super();
	}
	
	protected void init()
	{
		mDB = DDBaseImpl.getSingleton(AzureusCoreImpl.getSingleton());
		
		(new AEThread2("Local IP refresher", true) {
			public void run() 
			{
				//System.out.println("local IP refresher thread started");
				long next = 0;
				while( true )
				{
					// once per hour. 
					try
					{
						next = 60 * 60 * 1000;
						if( mDB.isAvailable() )
						{
							republishLocalID();
						}
						else
						{
							// Give the DHT some time to start
							//System.out.println("dht not available, onehop waiting 10 seconds...");
							next = 10 * 1000; 
						}	
					} catch( IOException e )
					{
						System.err.println(e);
						e.printStackTrace();
					}
					
					try
					{
						Thread.sleep(next);
					} catch( InterruptedException e )
					{
						System.err.println(e);
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	public void refreshRemoteID( final PublicKey inID, final SoftStateListener inListener ) throws IOException
	{
		// Don't perform these repeatedly
		// TODO: magic constant -- min time between IP refreshes
		Date lastUpdate = ReputationDAO.get().get_last_soft_state_update(inID);
		if( lastUpdate != null && lastUpdate.after(new Date(System.currentTimeMillis()-(15*60*1000))) )
		{
			//System.out.println("skipping refresh of id due to timing: " + ByteFormatter.encodeString(inID.getEncoded()) );
			return;
		}
		
		//System.out.println("refreshing remote id: " + KeyManipulation.concise(inID.getEncoded()) );
		
		DistributedDatabaseKey k = keyFromPub(inID);
		try 
		{
			mDB.read(new DistributedDatabaseListener() {
					public void event( DistributedDatabaseEvent event ) 
					{
						//System.out.println("DDB event: " + event.getType() + " " + event.toString());
						try
						{
							if( event.getType() == DistributedDatabaseEvent.ET_VALUE_READ )
							{
								byte [] rbytes = (byte[])event.getValue().getValue(byte[].class);
								
								SoftState s = SoftState.fromBytes(rbytes);
								// verify the signature for this entry, this is kind of kludgy
								byte [] real_sig = s.sig;
								byte [] bytes = s.protectedBytes();
								try
								{
									Signature sigs = Signature.getInstance("SHA1withRSA");
									sigs.initVerify(inID);
									sigs.update(bytes);
									if( sigs.verify(real_sig) == false )
									{
										System.err.println("sig check failed for soft state update: " + s);
										return;
									}
									
									ReputationDAO rep = ReputationDAO.get();
									rep.update_soft_state(inID, s.ip, s.tcp_port, s.udp_port, s.timestamp);
									
									if( inListener != null )
										inListener.refresh_complete(inID);
								}
								catch( Exception e )
								{
									System.err.println(e);
									e.printStackTrace();
								}
							} // event occurred
						}
						catch( Exception e )
						{
							System.err.println(e);
							e.printStackTrace();
						}
					}
				}, k, TIMEOUT_MINUTES * 60 * 1000);
		}
		catch( DistributedDatabaseException e )
		{
			System.err.println(e);
			e.printStackTrace();
		}
	}
	
	private DistributedDatabaseKey keyFromPub( PublicKey inID ) 
	{
		try
		{
			return mDB.createKey(inID.getEncoded());
		} 
		catch( DistributedDatabaseException e )
		{
			System.err.println(e);
			e.printStackTrace();
			return null;
		}
	}
	
	boolean debug = false;
	
	public void republishLocalID(  ) throws IOException
	{
		if( debug )
			return;
		
		//System.out.println("republishing local: " + KeyManipulation.concise(LocalIdentity.get().getKeys().getPublic().getEncoded()));
		
		try
		{
			DistributedDatabaseKey k = keyFromPub(LocalIdentity.get().getKeys().getPublic());
			
			SoftState s = new SoftState();
			s.ip = InetAddress.getLocalHost().getAddress();
			s.tcp_port = (short)COConfigurationManager.getIntParameter("TCP.Listen.Port");
			s.udp_port = (short)COConfigurationManager.getIntParameter("UDP.Listen.Port");
			s.sign_local();
			
			byte [] bytes = s.getBytes();
			
			DistributedDatabaseValue v = mDB.createValue(bytes);
			
			//System.out.println("putting in: " + k + " " + v + " value size: " + bytes.length );
			
			mDB.write(new DistributedDatabaseListener() {
					public void event( DistributedDatabaseEvent event ) 
					{
						if( event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ||
							event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ) 
						{
			//				System.out.println("refresh local id event: " + event.getType() + " / " + event.toString());
						} 
					}
				}, k, v);
		}
		catch( DistributedDatabaseException e )
		{
			throw new IOException(e.toString());
		}
	}
	
	public static void main( String [] args ) throws Exception 
	{
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		DataOutputStream dos = new DataOutputStream(baos);
//		dos.writeInt(12);
//		byte [] random = baos.toByteArray();
//		
//		Signature gen = Signature.getInstance("SHA1withRSA");
//		gen.initSign(LocalIdentity.get().getKeys().getPrivate());
//		gen.update(random);
//		byte [] sig = gen.sign();
//		
//		gen = Signature.getInstance("SHA1withRSA");
//		gen.initVerify(LocalIdentity.get().getKeys().getPublic());
//		gen.update(random);
//		if( gen.verify(sig) == false )
//			System.out.println("hash check fails");
//		else
//			System.out.println("hash check pass");
		
//		FileInputStream f = new FileInputStream("/tmp/read1");
//		byte [] b = new byte[f.available()];
//		f.read(b);
//		SoftState s = SoftState.fromBytes(b);
//		System.out.println(s);
//		
//		byte [] bytes = s.protectedBytes();
//		try
//		{
//			Signature sigs = Signature.getInstance("SHA1withRSA");
//			sigs.initVerify(LocalIdentity.get().getKeys().getPublic());
//			sigs.update(bytes);
//			if( sigs.verify(s.sig) == false )
//			{
//				System.err.println("sig check failed for soft state update: " + s);
//				return;
//			}
//		}
//		catch( Exception e )
//		{
//			e.printStackTrace();
//		}
		
		SoftState s = new SoftState();
		s.ip = InetAddress.getLocalHost().getAddress();
		s.tcp_port = (short)COConfigurationManager.getIntParameter("TCP.Listen.Port");
		s.udp_port = (short)COConfigurationManager.getIntParameter("UDP.Listen.Port");
		s.sign_local();
		
		byte [] bytes = s.getBytes();
		
		//System.out.println("it's " + bytes.length + " bytes");
		
//		
//		SoftState s2 = SoftState.fromBytes(bytes);
//		System.out.println(s);
//		System.out.println(s2);
//		
//		bytes = s2.protectedBytes();
//		try
//		{
//			Signature sigs = Signature.getInstance("SHA1withRSA");
//			sigs.initVerify(LocalIdentity.get().getKeys().getPublic());
//			sigs.update(bytes);
//			if( sigs.verify(s.sig) == false )
//			{
//				System.err.println("sig check failed for soft state update: " + s);
//			}
//		}
//		catch( Exception e )
//		{
//			e.printStackTrace();
//		}
		
//		FileInputStream fis = new FileInputStream("dhtbytes4696307");
//		byte [] b = new byte[fis.available()];
//		fis.read(b);
//		
//		SoftState s = SoftState.fromBytes(b);
//		
//		System.out.println(s);
//		
//		System.out.println(s.sig.length);
//		
//		System.out.println(ByteFormatter.encodeString(s.sig));
//		s.sign_local();
//
//		System.out.println(s.sig.length);
//		System.out.println(ByteFormatter.encodeString(s.sig));
//		
		
		// DEBUG
//		FileOutputStream out = new FileOutputStream("/tmp/read1");
//		out.write(bytes);
//		out.close();
//	
	}
}
