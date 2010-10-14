package edu.uw.cse.netlab.testharness;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.security.impl.SESecurityManagerImpl;

public class ReceiveOrders implements Runnable
{
	int mPort;

	public ReceiveOrders( int inPort ) 
	{
		mPort = inPort;
	}
	
	public void run() 
	{
		try 
		{
			ServerSocket sock = null;
			try 
			{
				sock = new ServerSocket(mPort);
			} 
			catch( IOException e ) // probably already running on this machine...
			{
				// try terminating the other instance -- we use this for auto update... 
				Socket s = new Socket("127.0.0.1", mPort);
				s.setSoTimeout(60*1000);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(new String("shutdown"));
				oos.reset();
				oos.flush();
				System.out.println("sent shutdown, waiting ack...");
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				String ack = (String)ois.readObject(); // we should get an ack for this
				s.close();
				System.out.println("got ack");
				try {
					Thread.sleep(1000);
				} catch ( Exception e2 ) {}
				
				sock = new ServerSocket(mPort);
			}
			while( true )
			{
				final Socket s = sock.accept();
				(new Thread() {
					public void run()
					{
						try 
						{
							s.setSoTimeout(10*1000);
							ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
							final ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
							
							Object o = ois.readObject();
							if( o instanceof String )
							{
								if( ((String)o).equals("restart") )
								{
									// in the process of starting another version of ourselves we should shutdown our currently running instance.
									System.out.println("got restart command");
									String java = System.getenv("JAVA");
									if( java == null )
										java="java";
									System.out.println("java is: " + java);
									
									Runtime.getRuntime().exec(new String[]{
											java,
											"-Dazureus.security.manager.install=0", 
											"-cp", "/proj/rip/exp/os/bin/az_base:/proj/rip/exp/os/bin/os_mods",
											"edu.uw.cse.netlab.testharness.CLI_Main"
											});
									System.out.println("exec'd...");
								}
								else if( ((String)o).equals("shutdown") )
								{
									oos.writeObject("ack");
									oos.flush();
									System.out.println("acked shutdown request");
									
									// it doesn't get more pushy than this. 
									Runtime.getRuntime().halt(0);
									//System.exit(0);
								}
								else if( ((String)o).equals("stdout") ||
										((String)o).equals("stderr") )
								{
									File out = ((String)o).equals("stdout") ? CLI_Main.get().getOut() : CLI_Main.get().getErr();
									synchronized(out)
									{
										try {
											RandomAccessFile f = new RandomAccessFile(out,"r");
											byte [] buff = new byte[(int)Math.min(f.length(), 10*1024)];
											f.seek(f.length()-buff.length);
											f.read(buff);
										
											oos.writeObject(new String(buff));
											System.out.println("wrote log back");
										}
										catch( Exception e )
										{
											System.err.println("reading logs: " + e );
											e.printStackTrace();
										}
									}
								}
							} // instanceof String
						}
						catch( Exception e )
						{
							System.out.println("error after accept " + e);
							e.printStackTrace();
						}
					}
				}).start();
			}
		}
		catch( Exception e )
		{
			System.err.println(e);
			e.printStackTrace();
			CLI_Main.exit();
		}
	}
}
