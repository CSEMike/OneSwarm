/*
 * Created on 8 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.impl.AzureusCoreSingleInstanceClient;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 *
 */
public class
StartServer
{
	private static final LogIDs LOGID = LogIDs.GUI;

  private ServerSocket socket;
  private int state;

  private boolean bContinue;
  public static final int STATE_FAULTY = 0;
  public static final int STATE_LISTENING = 1;

  protected List		queued_torrents = new ArrayList();
  protected boolean		core_started	= false;
  protected AEMonitor	this_mon		= new AEMonitor( "StartServer" );

  private static final int START_SERVER_PORT =
	  System.getProperty("oneswarm.integration.start.server.port") == null ? 6885 :
		  Integer.parseInt(System.getProperty("oneswarm.integration.start.server.port"));

	public StartServer() {
		try {
			// NOLAR: only bind to localhost
			socket = new ServerSocket(START_SERVER_PORT, 50, InetAddress.getByName("127.0.0.1"));

			state = STATE_LISTENING;

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "StartServer: listening on " + "127.0.0.1:"
						+ START_SERVER_PORT + " for passed torrent info"));
		} catch (Throwable t) {

			// DON'T USE LOGGER here as we DON't want to initialise all the
			// logger stuff
			// and in particular AEDiagnostics config dirty stuff!!!!
			state = STATE_FAULTY;
			String reason = t.getMessage() == null ? "<>" : t.getMessage();

			System.out.println("StartServer ERROR: unable" + " to bind to 127.0.0.1:"
					+ START_SERVER_PORT + " listening" + " for passed torrent info: " + reason);
		}
	}

	public void pollForConnections(final AzureusCore azureus_core) {
		azureus_core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			@Override
			public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
				if (component instanceof UIFunctionsSWT) {

					openQueuedTorrents(azureus_core);
				}
			}
		});

		if (socket != null) {

			Thread t = new AEThread("Start Server") {
				@Override
				public void runSupport() {
					pollForConnectionsSupport(azureus_core);
				}
			};

			t.setDaemon(true);

			t.start();
		}
	}

  private void
  pollForConnectionsSupport(
	AzureusCore		azureus_core )
  {
    bContinue = true;
    while (bContinue) {
      BufferedReader br = null;
      try {
        Socket sck = socket.accept();
        String address = sck.getInetAddress().getHostAddress();
        if (address.equals("localhost") || address.equals("127.0.0.1")) {

        	System.out.println("startserver got connection");

          br = new BufferedReader(new InputStreamReader(sck.getInputStream(),Constants.DEFAULT_ENCODING));
          String line = br.readLine();
          System.out.println("received : " + line);

          if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, "Main::startServer: received '"
								+ line + "'"));

          if (line != null) {
        	  String [] args = parseArgs(line);
        	  if (args != null && args.length > 0) {
        		  String debug_str = args[0];
        		  for (int i=1; i<args.length; i++) {
        			  debug_str += " ; " + args[i];
        		  }
        		  Logger.log(new LogEvent(LOGID, "Main::startServer: decoded to '" + debug_str + "'"));
        		  processArgs(azureus_core,args);
        	  }
          }
        }
        sck.close();

      }
      catch (Exception e) {
        if(!(e instanceof SocketException))
        	Debug.printStackTrace( e );
        //bContinue = false;
      } finally {
        try {
          if (br != null)
            br.close();
        } catch (Exception e) { /*ignore */
        }
      }
    }
  }

  private static String[] parseArgs(String line) {
	  if (!line.startsWith(AzureusCoreSingleInstanceClient.ACCESS_STRING + ";")) {return null;}

	  // I'm sure there's a lovely regex which could do this, but I can't be bothered to figure
	  // it out.
	  ArrayList parts = new ArrayList();
	  StringBuffer buf = new StringBuffer();
	  boolean escape_mode = false;
	  char c;
	  for (int i=AzureusCoreSingleInstanceClient.ACCESS_STRING.length() + 1; i<line.length(); i++) {
		  c = line.charAt(i);
		  if (escape_mode) {buf.append(c); escape_mode = false;}
		  else if (c == '&') {escape_mode = true;}
		  else if (c == ';') {parts.add(buf.toString()); buf.setLength(0);}
		  else {buf.append(c);}
	  }
	  if (buf.length() > 0) {parts.add(buf.toString());}
	  return (String[])parts.toArray(new String[parts.size()]);

  }

  protected void
  processArgs(
	AzureusCore		azureus_core,
   	String 			args[])
  {

	  for( String s : args ) {
		  System.out.println("processArgs: " + s);
	  }

    if (args.length < 1 || !args[0].equals( "args" )){

    	return;
    }

    if (args.length == 1
				|| !COConfigurationManager.getBooleanParameter("add_torrents_silently")) {
			showMainWindow();
		}

    boolean	open	= true;

    for (int i = 1; i < args.length; i++) {

    	String	arg = args[i];

    	if ( i == 1 ){

	  	    if ( arg.equalsIgnoreCase( "--closedown" )){

	  	    	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	    	if (uiFunctions != null) {
	  	    		uiFunctions.requestShutdown();
	  	    	}

	  	    	return;

	  	    }else if ( arg.equalsIgnoreCase( "--open" )){

	  	    	continue;

	  	    }else if ( arg.equalsIgnoreCase( "--share" )){

	  	    	open	= false;

	  	    	continue;
	  	    }
    	}

        String file_name = arg;

        File file = new File(file_name);

        if ( !file.exists() && !isURI( file_name )){

        		// handle hex info hashes

	        if ( file_name.length() == 40 ){

	        	byte[]	hash = null;

	        	try{
	        		hash = ByteFormatter.decodeString( file_name );

	        	}catch( Throwable e ){
	        	}

	        	if ( hash != null && hash.length == 20 ){

	        		file_name = "magnet:?xt=urn:btih:" + Base32.encode( hash );
	        	}
	        }

	        	// handle base32 info hash

	        if ( file_name.length() == 32 ){

	        	byte[]	hash = null;

	        	try{
	        		hash = Base32.decode( file_name );

	        	}catch( Throwable e ){
	        	}

	        	if ( hash != null && hash.length == 20 ){

	        		file_name = "magnet:?xt=urn:btih:" +file_name;
	        	}
	        }
        }

        if ( isURI( file_name )) {

        	if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, "StartServer: args[" + i
								+ "] handling as a URI: " + file_name));

        }else{

            try {


              if (!file.exists()) {

                throw (new Exception("File not found"));
              }

              file_name = file.getCanonicalPath();

              Logger.log(new LogEvent(LOGID, "StartServer: file = " + file_name));

            } catch (Throwable e) {

            	Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
            			"Failed to access torrent file '" + file_name
            			+ "'. Ensure sufficient temporary file space "
            			+ "available (check browser cache usage)."));
            }
        }

        boolean	queued = false;

        try {
          this_mon.enter();

          if (!core_started) {

            queued_torrents.add( new Object[]{ file_name, new Boolean( open )});

            queued = true;
          }
        } finally {

          this_mon.exit();
        }

        if ( !queued ){

        	handleFile( azureus_core, file_name, open );
        }
      }
  }

  protected boolean
  isURI(
	String	file_name )
  {
      String file_name_lower = file_name.toLowerCase();

	  return( file_name_lower.startsWith( "http:" ) || file_name_lower.startsWith( "https:" ) || file_name_lower.startsWith( "magnet:" ) || file_name_lower.startsWith("oneswarm:"));
  }

  protected void
  handleFile(
	AzureusCore	azureus_core,
	String		file_name,
	boolean		open )
  {
      try {

      	if ( open ){

      		TorrentOpener.openTorrent(file_name);

      	}else{

      		File	f = new File( file_name );

      		if ( f.isDirectory()){

      			ShareUtils.shareDir( azureus_core, file_name );

      		}else{

         		ShareUtils.shareFile( azureus_core, file_name );
      		}
      	}
      } catch (Throwable e) {

        Debug.printStackTrace(e);
      }
  }

  protected void
  openQueuedTorrents(
	AzureusCore		azureus_core )
  {
    try{
      	this_mon.enter();

      	core_started	= true;

    }finally{

      	this_mon.exit();
    }

    for (int i=0;i<queued_torrents.size();i++){

    	Object[]	entry = (Object[])queued_torrents.get(i);

    	String	file_name 	= (String)entry[0];
    	boolean	open		= ((Boolean)entry[1]).booleanValue();

    	handleFile( azureus_core, file_name, open );
    }
  }

  protected void
  showMainWindow()
  {
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.bringToFront();
  	}
  }

  public void stopIt() {
    bContinue = false;
    try {
      socket.close();
    }
    catch (Throwable e) {/*ignore */}
  }
  /**
   * @return
   */
  public int getState() {
    return state;
  }

  // Test argument parsing code.
  public static void main(String [] args) {
	  String[] input_tests = new String[] {
		  "a;b;c",
		  "test",
		  AzureusCoreSingleInstanceClient.ACCESS_STRING + ";b;c;d", // Simple test
		  AzureusCoreSingleInstanceClient.ACCESS_STRING + ";b;c&;d;e", // Less simple test
		  AzureusCoreSingleInstanceClient.ACCESS_STRING + ";b;c&&;d;e", // Even less simple test
		  AzureusCoreSingleInstanceClient.ACCESS_STRING + ";b;c&&&;d;e", // Awkward test
	  };

	  String[][] output_results = new String[][] {
        null,
        null,
        new String[] {"b", "c", "d"},
        new String[] {"b", "c;d", "e"},
        new String[] {"b", "c&", "d", "e"},
        new String[] {"b", "c&;d", "e"},
	  };

	  for (int i=0; i<input_tests.length; i++) {
		  System.out.println("Testing: " + input_tests[i]);
		  String[] result = parseArgs(input_tests[i]);
		  if (result == output_results[i]) {continue;}
		  if (Arrays.equals(result, output_results[i])) {continue;}
		  System.out.println("TEST FAILED");
		  System.out.println("  Expected: " + Arrays.asList(output_results[i]));
		  System.out.println("  Decoded : " + Arrays.asList(result));
		  System.exit(1);
	  }

	  System.out.println("Done.");
  }

}
