/*
 * Created on May 30, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.impl.AzureusCoreSingleInstanceClient;


public class StartSocket {
	//private static final LogIDs LOGID = LogIDs.GUI;

		private final String[] args;
	
    public StartSocket(String _args[]) {
    	this.args = _args;
    }
    
    
    /**
     * Attempt to send args via socket connection.
     * @return true if successful, false if connection attempt failed
     */
    public boolean sendArgs() {
    	Socket sck = null;
    	PrintWriter pw = null;
    	try {
    		String msg = "StartSocket: passing startup args to already-running Azureus java process listening on [127.0.0.1: 6880]";
    		
    			// DON'T USE LOGGER here as we DON't want to initialise all the logger stuff
    			// and in particular AEDiagnostics config dirty stuff!!!!
    		
    		System.out.println( msg );
       	
    		sck = new Socket("127.0.0.1", 6880);
         
    			// NOTE - this formatting is also used by AzureusCoreSingleInstanceClient and other org.gudy.azureus2.ui.common.Main.StartSocket
    		
    		pw = new PrintWriter(new OutputStreamWriter(sck.getOutputStream(),Constants.DEFAULT_ENCODING));
         
    		StringBuffer buffer = new StringBuffer(AzureusCoreSingleInstanceClient.ACCESS_STRING + ";args;");
         
    		for(int i = 0 ; i < args.length ; i++) {
    			String arg = args[i].replaceAll("&","&&").replaceAll(";","&;");
    			buffer.append(arg);
    			buffer.append(';');
    		}
      	 
    		pw.println(buffer.toString());
    		pw.flush();
    		
    		return true;
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		Debug.printStackTrace( e );
    		return false;  //there was a problem connecting to the socket
    	}
    	finally {
    		try {
    			if (pw != null)  pw.close();
    		}
    		catch (Exception e) {}
    		
    		try {
    			if (sck != null) 	sck.close();
    		}
    		catch (Exception e) {}
    	}
    }
    
    
    public static void main(String args[]) {
      new StartSocket(args);
    }
  }