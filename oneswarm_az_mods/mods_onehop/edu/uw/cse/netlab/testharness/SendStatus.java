package edu.uw.cse.netlab.testharness;

import java.util.*;
import java.io.*;
import java.net.Socket;

public class SendStatus implements Runnable
{
	String mHost = null;
	int mPort = 0;
	String mActiveExp = null;
	byte [] torrent_bytes = null;
	
	public byte [] getTorrent() { return torrent_bytes; }
	
	public SendStatus( String inHost, int inPort )
	{
		mHost = inHost;
		mPort = inPort;
	}
	
	public void run()
	{
		while( true )
		{
			try
			{
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run()
					{
						try 
						{
							Socket s = new Socket( mHost, mPort );
							
							PrintStream out = new PrintStream(s.getOutputStream());
							out.print("GET /peers&down&" + mPort + "\r\n\r\n" );
							out.flush();
							
							s.close();
							
							System.out.println("send down notification");
						} catch( Exception e ) {}
					}
				});
				
				while( true )
				{
					Socket s = new Socket( mHost, mPort );
					System.out.println("Heartbeat connected: " + mHost + ":" + mPort);
					
					PrintStream out = new PrintStream(s.getOutputStream());
					out.print("GET /peers&up&" + mPort + "\r\n\r\n" );
					out.flush();
					
					s.close();
					
					/**
					 * We haven't received anything to do yet and can talk to the coordinator, so ask
					 */
					if( mActiveExp == null )
					{
						s = new Socket( mHost, mPort );
						out = new PrintStream(s.getOutputStream());
						out.print("GET /curr\r\n\r\n" );
						out.flush();
						ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
						String name = (String)ois.readObject();
						if( name.length() > 0 )
						{
							mActiveExp = name;
							System.out.println("Got current exp: " + name );
							get_experiment();
						}	
						else
							System.out.println("No active experiment at coordinator.");
					}
					
					Thread.sleep(2*60*1000);
				}
			}
			catch( Exception e )
			{
				System.err.println("Heartbeat failed: " + e);
				e.printStackTrace();
			} 
			try {
				Thread.sleep(1*60*1000);
			} catch( Exception e ) {
				break;
			}
		}
		System.err.println("Total heartbeat failure...");
	}
	
	public byte [] req_file( String path ) throws IOException
	{
		Socket s = new Socket(mHost, mPort);
		s.getOutputStream().write((path + "\r\n\r\n").getBytes());
		s.getOutputStream().flush();
		
		byte [] bytes = null;
		
		try
		{
			Integer incoming_fsize = (Integer) (new ObjectInputStream(s.getInputStream())).readObject();
			bytes = new byte[incoming_fsize.intValue()];
			s.getInputStream().read(bytes);
		} 
		catch (ClassNotFoundException e)
		{
			System.err.println(e);
			e.printStackTrace();
		}
		return bytes;
	}

	private void get_experiment() throws IOException 
	{
		byte [] desc = req_file("GET /exps/" + mActiveExp + "/desc");
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(desc)));
		while( in.ready() )
		{
			String l = in.readLine();
			String [] toks = l.split("\\s+");
			if( l.startsWith("torrent") )
				torrent_bytes = req_file("GET /exps/" + mActiveExp + "/" + toks[1]);
		}
		if( torrent_bytes == null )
		{
			System.err.println("couldn't load torrent bytes for exp");
			mActiveExp = null;
		}
	}
}
