/*
 * File    : NatCheckerServer.java
 * Created : 12 oct. 2003 19:05:09
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

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.http.HTTPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;



/**
 * 
 *
 */
public class NatCheckerServer extends AEThread {
	private static final LogIDs LOGID = LogIDs.NET;
    private static final String incoming_handshake = "NATCHECK_HANDSHAKE";
  
    private final InetAddress	bind_ip;
    private boolean				bind_ip_set;
    private final String 		check;
    private final boolean		http_test;
    
    private ServerSocket server;
 
    private volatile boolean bContinue = true;
    private final boolean use_incoming_router;
    private NetworkManager.ByteMatcher matcher;
    
    
    public 
    NatCheckerServer(
    	InetAddress 	_bind_ip, 
    	int 			_port,  
    	String 			_check, 
    	boolean 		_http_test )
    
    	throws Exception
    {     
      super("Nat Checker Server");
      
      bind_ip		= _bind_ip;
      check		 	= _check;
      http_test		= _http_test;
      
      if ( http_test ){
    	  
    	  HTTPNetworkManager	net_man = HTTPNetworkManager.getSingleton();
    	  
    	  if ( net_man.isHTTPListenerEnabled()){
       	  
    		  use_incoming_router = _port == net_man.getHTTPListeningPortNumber();
    		  
    	  }else{
    		  
    		  use_incoming_router = false;
    	  }
 
    	  if ( use_incoming_router ){
    		  
    		  if ( !net_man.isEffectiveBindAddress( bind_ip )){
    			  
    			  net_man.setExplicitBindAddress( bind_ip );
    			  
    			  bind_ip_set	= true;
    		  }
    	  }
      }else{
    	
    	  TCPNetworkManager	net_man = TCPNetworkManager.getSingleton();

      	  if ( net_man.isTCPListenerEnabled()){
           	  
    		  use_incoming_router = _port == net_man.getTCPListeningPortNumber();
    		  
    	  }else{
    		  
    		  use_incoming_router = false;
    	  }  
      	  
	      if ( use_incoming_router ) {
	 
 		 	if ( !net_man.isEffectiveBindAddress( bind_ip )){
    			  
    			  net_man.setExplicitBindAddress( bind_ip );
    			  
    			  bind_ip_set	= true;
    		  }
 		  
	    	  	//test port and currently-configured listening port are the same,
	    	  	//so register for incoming connection routing
	        
	        matcher = new NetworkManager.ByteMatcher() {
			  public int matchThisSizeOrBigger(){ return( maxSize()); }
	          public int maxSize() {  return incoming_handshake.getBytes().length;  }
	          public int minSize(){ return maxSize(); }
	        
	          public Object matches( TransportHelper transport, ByteBuffer to_compare, int port ) {             
	            int old_limit = to_compare.limit();
	            to_compare.limit( to_compare.position() + maxSize() );
	            boolean matches = to_compare.equals( ByteBuffer.wrap( incoming_handshake.getBytes() ) );
	            to_compare.limit( old_limit );  //restore buffer structure
	            return matches?"":null;
	          }
	          public Object minMatches( TransportHelper transport, ByteBuffer to_compare, int port ) { return( matches( transport, to_compare, port )); } 
	          public byte[][] getSharedSecrets(){ return( null ); }
	  	   	  public int getSpecificPort(){return( -1 );
			}
	        };
	        
	        NetworkManager.getSingleton().requestIncomingConnectionRouting(
	            matcher,
	            new NetworkManager.RoutingListener() {
	              public void 
	              connectionRouted( 
	            	NetworkConnection 	connection, 
	            	Object 				routing_data ) 
	              {
	            	  if (Logger.isEnabled())
	            		  Logger.log(new LogEvent(LOGID, "Incoming connection from ["
	            				  + connection + "] successfully routed to NAT CHECKER"));
	
	            	  try{
	            		  ByteBuffer	msg = getMessage();
	
	            		  Transport transport = connection.getTransport();
	
	            		  long	start = SystemTime.getCurrentTime();
	
	            		  while( msg.hasRemaining()){
	
	            			  transport.write( new ByteBuffer[]{ msg }, 0, 1 );
	
	            			  if ( msg.hasRemaining()){
	
	            				  long now = SystemTime.getCurrentTime();
	
	            				  if ( now < start ){
	
	            					  start = now;
	
	            				  }else{
	
	            					  if ( now - start > 30000 ){
	
	            						  throw( new Exception( "Timeout" ));
	            					  }
	            				  }
	
	            				  Thread.sleep( 50 );
	            			  }
	            		  }
	            	  }catch( Throwable t ) {
	            		
	            		  Debug.out( "Nat check write failed", t );
	            	  }
	
	            	  connection.close( null );
	              }
	              
	              public boolean
	          	  autoCryptoFallback()
	              {
	            	  return( true );
	              }
	            },
	            new MessageStreamFactory() {
	              public MessageStreamEncoder createEncoder() {  return new AZMessageEncoder(AZMessageEncoder.PADDING_MODE_NONE);  /* unused */}
	              public MessageStreamDecoder createDecoder() {  return new AZMessageDecoder();  /* unused */}
	            });
	      }
          		
	      if (Logger.isEnabled())
  				Logger.log(new LogEvent(LOGID, "NAT tester using central routing for "
  						+ "server socket"));
      }
      
      if ( !use_incoming_router ){
     
    	  //different port than already listening on, start new listen server
    	  
        try{
 
          server = new ServerSocket();  //unbound          
          server.setReuseAddress( true );  //set SO_REUSEADDR 
          
          InetSocketAddress address;

          if( bind_ip != null ) {
        	  
        	  address = new InetSocketAddress( bind_ip, _port );
        	  
          }else {
        	  
        	  address = new InetSocketAddress( _port );
          }
       
  	      server.bind( address );
  	      
  	      if (Logger.isEnabled())	Logger.log(new LogEvent(LOGID, "NAT tester server socket bound to " +address ));
          
 
        }catch(Exception e) { 
        	
        	Logger.log(new LogEvent(LOGID, "NAT tester failed to setup listener socket", e ));
        	
        	throw( e );   
        }
      }
    }
    
    protected ByteBuffer
    getMessage()
    
    	throws IOException
    {
		  Map	map = new HashMap();

		  map.put( "check", check );

		  byte[]	map_bytes = BEncoder.encode( map );

		  ByteBuffer msg = ByteBuffer.allocate( 4 + map_bytes.length );

		  msg.putInt( map_bytes.length );
		  msg.put( map_bytes );

		  msg.flip();
		  
		  return( msg );
    }
    
    public void runSupport() {
      while(bContinue) {
        try {
          if (use_incoming_router) {
            //just NOOP loop sleep while waiting for routing
            Thread.sleep(20);
          }
          else {
            //listen for accept
          	Socket sck = server.accept();
          	
          	sck.getOutputStream().write( getMessage().array());
          	
          	sck.close();
          }
        } catch(Exception e) {
        	//Debug.printStackTrace(e);
        	bContinue = false;
        }
      }
    }
      
    public void stopIt() {
      bContinue = false;
      
      if( use_incoming_router ) {
    	  
    	  if ( http_test ){
    		  
    		  if ( bind_ip_set ){
    			  
    			  HTTPNetworkManager.getSingleton().clearExplicitBindAddress();
    		  }
    	  }else{
    		  
    		  NetworkManager.getSingleton().cancelIncomingConnectionRouting( matcher );
    		  
    		  if ( bind_ip_set ){
    			  
    			  TCPNetworkManager.getSingleton().clearExplicitBindAddress();
    		  }
    	  }
      }
      else if( server != null ) {
        try {
          server.close();
        }
        catch(Throwable t) { t.printStackTrace(); }
      }
    }
    
    
  }
