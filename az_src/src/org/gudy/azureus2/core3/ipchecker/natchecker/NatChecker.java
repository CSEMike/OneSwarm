/*
 * File    : NatChecker.java
 * Created : 12 oct. 2003 18:46:00
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.ipchecker.natchecker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Map;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASN;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.*;

import com.aelitis.azureus.plugins.upnp.*;

/**
 * @author Olivier
 * 
 */
public class NatChecker {
	private static final LogIDs LOGID = LogIDs.NET;

  public static final int NAT_OK = 1;
  public static final int NAT_KO = 2;
  public static final int NAT_UNABLE = 3;
  
 
  private int			result;
  private String		additional_info	= "";
  private InetAddress	ip_address;
  
  public 
  NatChecker(
  	AzureusCore		azureus_core,
	InetAddress		bind_ip,
	int 			port,
	boolean			http_test )
  {	
    String check = "azureus_rand_" + String.valueOf( (int)(Math.random() * 100000) );
    
    if ( port < 0 || port > 65535 || port == 6880 ){
    	
    	result = NAT_UNABLE;
    	
    	additional_info	= "Invalid port";
    	
    	return;
    }
    
    NatCheckerServer server;
    
    try{
    	server = new NatCheckerServer( bind_ip, port, check, http_test );
    
    }catch( Throwable e ){
    	
    	result = NAT_UNABLE;
    	
    	additional_info	= "Can't initialise server: " + Debug.getNestedExceptionMessage(e);
    	
    	return;
    }
    
  
    

    //do UPnP if necessary
    PluginInterface pi_upnp = azureus_core.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );

    UPnPMapping new_mapping = null;

    String	upnp_str = null;
    
    if( pi_upnp != null ) {

      UPnPPlugin upnp = (UPnPPlugin)pi_upnp.getPlugin();

      UPnPMapping mapping = upnp.getMapping( true, port );

      if ( mapping == null ) {

        new_mapping = mapping = upnp.addMapping( "NAT Tester", true, port, true );

        // give UPnP a chance to work

        try {
          Thread.sleep( 500 );
          
        }
        catch (Throwable e) {

          Debug.printStackTrace( e );
        }
      }
      
      UPnPPluginService[]	services = upnp.getServices();
      
      if ( services.length > 0 ){
    	  
    	  upnp_str = "";
      
	      for (int i=0;i<services.length;i++){
	    	  
	    	  UPnPPluginService service = services[i];
	    	  
	    	  upnp_str += (i==0?"":",") + service.getInfo();
	      }
      }
    }

    //run check
    try {
      server.start();
      
      String urlStr = Constants.NAT_TEST_SERVER_HTTP + (http_test?"httptest":"nattest") + "?port=" + String.valueOf( port ) + "&check=" + check;
           
      if ( upnp_str != null ){
    	
    	  urlStr += "&upnp=" + URLEncoder.encode( upnp_str, "UTF8" );
      }
      
      NetworkAdminASN net_asn = NetworkAdmin.getSingleton().getCurrentASN();
      
      String	as 	= net_asn.getAS();
      String	asn = net_asn.getASName();
      
      if ( as.length() > 0 ){
    	
      	  urlStr += "&as=" + URLEncoder.encode( as, "UTF8" );
      	  urlStr += "&asn=" + URLEncoder.encode( asn, "UTF8" );
      }
      
      urlStr += "&locale=" + MessageText.getCurrentLocale().toString();
      
      String	ip_override = TRTrackerUtils.getPublicIPOverride();
      
      if ( ip_override != null ){
    	  
    	  urlStr += "&ip=" + ip_override;
      }
      
      URL url = new URL( urlStr );
      HttpURLConnection con = (HttpURLConnection)url.openConnection();
      con.connect();
      
      ByteArrayOutputStream message = new ByteArrayOutputStream();
      InputStream is = con.getInputStream();
      
      byte[] data = new byte[ 1024 ];
      
      int	expected_length = -1;
            
      while( true ){
    	  
        int	len = is.read( data );
        
        if ( len <= 0 ){
        	
        	break;
        }
        
        message.write( data, 0, len );
        
        if ( expected_length == -1 && message.size() >= 4 ){
        	
        	byte[]	bytes = message.toByteArray();
        	
        	ByteBuffer	bb = ByteBuffer.wrap( bytes );
        	
        	expected_length = bb.getInt();
        	
        	message = new ByteArrayOutputStream();
        	
        	if ( bytes.length > 4 ){
        		
        		message.write( bytes, 4, bytes.length - 4 );
        	}
        }
        
        if ( expected_length != -1 && message.size() == expected_length ){
        	
        	break;
        }
      }
      
      Map map = BDecoder.decode( message.toByteArray() );
      int reply_result = ((Long)map.get( "result" )).intValue();
      
      switch( reply_result ) {
        case 0 :{
          byte[] reason = (byte[])map.get( "reason" );
          if( reason != null ) {
          	Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
								"NAT CHECK FAILED: " + new String(reason)));
          }
          result = NAT_KO;
          additional_info = reason==null?"Unknown":new String(reason, "UTF8");
          break;
        }
        case 1 :{
          result = NAT_OK;
          byte[] reply = (byte[])map.get( "reply" );
          if( reply != null ) {
        	  additional_info = new String(reply, "UTF8");
          }
          break;
        }
        default :{
          result = NAT_UNABLE;
          additional_info = "Invalid response";
          break;
        }
      }
      
      byte[]	ip_bytes = (byte[])map.get( "ip_address" );
      
      if ( ip_bytes != null ){
    	  
    	  try{
    		  ip_address = InetAddress.getByAddress( ip_bytes );
    		  
    	  }catch( Throwable e ){
    		  
    	  }
      }
    }
    catch (Exception e) {
    	result = NAT_UNABLE;
    	additional_info = "Error: " + Debug.getNestedExceptionMessage( e );
    }
    finally {
    	
      server.stopIt();
      
      if( new_mapping != null ) {

        new_mapping.destroy();
      }

    }
  }
  
  public int
  getResult()
  {
	  return( result );
  }
  
  public InetAddress
  getExternalAddress()
  {
	  return( ip_address );
  }
  
  public String
  getAdditionalInfo()
  {
	  return( additional_info );
  }
}
