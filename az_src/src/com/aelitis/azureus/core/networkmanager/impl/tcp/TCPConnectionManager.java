/*
 * Created on Sep 13, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.net.*;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;




/**
 * Manages new connection establishment and ended connection termination.
 */
public class TCPConnectionManager {
  private static final LogIDs LOGID = LogIDs.NWMAN;

  private static int MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 3;  
  public static int MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = 5;  //NOTE: WinXP SP2 limits to 10 max at any given time
  
  private static int max_outbound_connections;
  
  static {
    MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = COConfigurationManager.getIntParameter( "network.max.simultaneous.connect.attempts" );
    
    if( MAX_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) { //should never happen, but hey
   	 MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = 1;
   	 COConfigurationManager.setParameter( "network.max.simultaneous.connect.attempts", 1 );
    }
    
    MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - 2;
    
    if( MIN_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) {
      MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 1;
    }

    COConfigurationManager.addParameterListener( "network.max.simultaneous.connect.attempts", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = COConfigurationManager.getIntParameter( "network.max.simultaneous.connect.attempts" );
        MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - 2;
        if( MIN_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) {
          MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 1;
        }
      }
    });
    
	COConfigurationManager.addAndFireParameterListeners(
			new String[]{
			    "network.tcp.max.connections.outstanding",
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					max_outbound_connections = COConfigurationManager.getIntParameter( "network.tcp.max.connections.outstanding", 2048 );
				}
			});
  }
  
  
  private static final int CONNECT_ATTEMPT_TIMEOUT = 30*1000;  //30sec
  private static final int CONNECT_ATTEMPT_STALL_TIME = 3*1000;  //3sec
  private static final boolean SHOW_CONNECT_STATS = false;
  
  private final VirtualChannelSelector connect_selector = new VirtualChannelSelector( "Connect/Disconnect Manager", VirtualChannelSelector.OP_CONNECT, true );
  
  private long connection_request_id_next;
  
  private final Set new_requests = 
	  new TreeSet(
			new Comparator()
			{
				public int 
				compare(
					Object o1, 
					Object o2 )
				{
					ConnectionRequest	r1 = (ConnectionRequest)o1;
					ConnectionRequest	r2 = (ConnectionRequest)o2;
					
					int	res = r1.getPriority() - r2.getPriority();
					
					if ( res == 0 ){
						
						res = r1.getRandom() - r2.getRandom();
						
						if ( res == 0 ){
							
							long l = r1.getID() - r2.getID();
							
							if ( l < 0 ){
								
								res = -1;
								
							}else if ( l > 0 ){
								
								res = 1;
								
							}else{
								
								Debug.out( "arghhh, borkage" );
							}
						}
					}
					
					return( res );
				}
			});
  
  private final ArrayList canceled_requests = new ArrayList();
  private final AEMonitor	new_canceled_mon= new AEMonitor( "ConnectDisconnectManager:NCM");
  
  private final HashMap pending_attempts = new HashMap();
  
  private final LinkedList 	pending_closes 	= new LinkedList();
  private final Map			delayed_closes	= new HashMap();
  
  private final AEMonitor	pending_closes_mon = new AEMonitor( "ConnectDisconnectManager:PC");
     
  private final Random random = new Random();
  
  private boolean max_conn_exceeded_logged;
  
  
  public 
  TCPConnectionManager() 
  {
	  Set	types = new HashSet();
	  
	  types.add( AzureusCoreStats.ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH );
	  types.add( AzureusCoreStats.ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH );
	  types.add( AzureusCoreStats.ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH );
	  types.add( AzureusCoreStats.ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH );
		
	  AzureusCoreStats.registerProvider( 
			  types,
			  new AzureusCoreStatsProvider()
			  {
					public void
					updateStats(
						Set		types,
						Map		values )
					{
						if ( types.contains( AzureusCoreStats.ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH )){
							
							values.put( AzureusCoreStats.ST_NET_TCP_OUT_CONNECT_QUEUE_LENGTH, new Long( new_requests.size()));
						}	
						
						if ( types.contains( AzureusCoreStats.ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH )){
							
							values.put( AzureusCoreStats.ST_NET_TCP_OUT_CANCEL_QUEUE_LENGTH, new Long( canceled_requests.size()));
						}					

						if ( types.contains( AzureusCoreStats.ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH )){
							
							values.put( AzureusCoreStats.ST_NET_TCP_OUT_CLOSE_QUEUE_LENGTH, new Long( pending_closes.size()));
						}					

						if ( types.contains( AzureusCoreStats.ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH )){
							
							values.put( AzureusCoreStats.ST_NET_TCP_OUT_PENDING_QUEUE_LENGTH, new Long( pending_attempts.size()));
						}					

					}
			  });
	  
    AEThread loop = new AEThread( "ConnectDisconnectManager" ) {
      public void runSupport() {
        mainLoop();
      }
    };
    loop.setDaemon( true );
    loop.start();    
  }
  

  private void mainLoop() {      
    while( true ) {
      addNewOutboundRequests();
      runSelect();
      doClosings();
    }
  }
  
	public int
	getMaxOutboundPermitted()
	{
		return( Math.max( max_outbound_connections - new_requests.size(), 0 ));
	}
  
  private void addNewOutboundRequests() {    
    while( pending_attempts.size() < MIN_SIMULTANIOUS_CONNECT_ATTEMPTS ) {
      ConnectionRequest cr = null;
      
      try{
        new_canceled_mon.enter();
      
        if( new_requests.isEmpty() )  break;
        
        Iterator it = new_requests.iterator();
        cr = (ConnectionRequest)it.next();
        it.remove();
      }
      finally{
        new_canceled_mon.exit();
      }
      
      if( cr != null ) {
        addNewRequest( cr ); 
      }
    }
  }
  
  

  private void addNewRequest( final ConnectionRequest request ) {
    request.listener.connectAttemptStarted();
    
    try {
      request.channel = SocketChannel.open();
      
      try {  //advanced socket options
        int rcv_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_RCVBUF" );
        if( rcv_size > 0 ) {
          if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, "Setting socket receive buffer size"
								+ " for outgoing connection [" + request.address + "] to: "
								+ rcv_size));
          request.channel.socket().setReceiveBufferSize( rcv_size );
        }
      
        int snd_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_SNDBUF" );
        if( snd_size > 0 ) {
        	if (Logger.isEnabled())
        		Logger.log(new LogEvent(LOGID, "Setting socket send buffer size "
        				+ "for outgoing connection [" + request.address + "] to: "
        				+ snd_size));
          request.channel.socket().setSendBufferSize( snd_size );
        }

        String ip_tos = COConfigurationManager.getStringParameter( "network.tcp.socket.IPTOS" );
        if( ip_tos.length() > 0 ) {
        	if (Logger.isEnabled())
        		Logger.log(new LogEvent(LOGID, "Setting socket TOS field "
        				+ "for outgoing connection [" + request.address + "] to: "
        				+ ip_tos));
          request.channel.socket().setTrafficClass( Integer.decode( ip_tos ).intValue() );
        }

      
        int local_bind_port = COConfigurationManager.getIntParameter( "network.bind.local.port" );
        
        if( local_bind_port > 0 ) {
        	request.channel.socket().setReuseAddress( true );
        }
        
        InetAddress bindIP = NetworkAdmin.getSingleton().getMultiHomedOutgoingRoundRobinBindAddress();
        if ( bindIP != null ) {
        	if (Logger.isEnabled()) 	Logger.log(new LogEvent(LOGID, "Binding outgoing connection [" + request.address + "] to local IP address: " + bindIP));
          request.channel.socket().bind( new InetSocketAddress( bindIP, local_bind_port ) );
        }
        else if( local_bind_port > 0 ) {       
        	if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Binding outgoing connection [" + request.address + "] to local port #: " +local_bind_port));
        	request.channel.socket().bind( new InetSocketAddress( local_bind_port ) );     
        }

      }
      catch( Throwable t ) {
        String msg = "Error while processing advanced socket options.";
        Debug.out( msg, t );
        Logger.log(new LogAlert(LogAlert.UNREPEATABLE, msg, t));
        //dont pass the exception outwards, so we will continue processing connection without advanced options set
      }
      
      request.channel.configureBlocking( false );
      request.connect_start_time = SystemTime.getCurrentTime();
      
      if( request.channel.connect( request.address ) ) {  //already connected
        finishConnect( request );
      }
      else {  //not yet connected, so register for connect selection
        pending_attempts.put( request, null );
        
        connect_selector.register( request.channel, new VirtualChannelSelector.VirtualSelectorListener() {
          public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {         
            pending_attempts.remove( request );
            finishConnect( request );
            return true;
          }
          
          public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
            pending_attempts.remove( request );
            
            closeConnection( request.channel );
           
            request.listener.connectFailure( msg );
          }
        }, null );
      }
    }
    catch( Throwable t ) {
      
      String full = request.address.toString();
      String hostname = request.address.getHostName();
      int port = request.address.getPort();
      boolean unresolved = request.address.isUnresolved();
      InetAddress	inet_address = request.address.getAddress();
      String full_sub = inet_address==null?request.address.toString():inet_address.toString();
      String host_address = inet_address==null?request.address.toString():inet_address.getHostAddress();
      
      String msg = "ConnectDisconnectManager::address exception: full="+full+ ", hostname="+hostname+ ", port="+port+ ", unresolved="+unresolved+ ", full_sub="+full_sub+ ", host_address="+host_address;
      if( request.channel != null ) {
        String channel = request.channel.toString();
        String socket = request.channel.socket().toString();
        String local_address = request.channel.socket().getLocalAddress().toString();
        int local_port = request.channel.socket().getLocalPort();
           SocketAddress ra = request.channel.socket().getRemoteSocketAddress();
        String remote_address;
           if( ra != null )  remote_address = ra.toString();
           else remote_address = "<null>";
        int remote_port = request.channel.socket().getPort();

        msg += "\n channel="+channel+ ", socket="+socket+ ", local_address="+local_address+ ", local_port="+local_port+ ", remote_address="+remote_address+ ", remote_port="+remote_port;
      }
      else {
        msg += "\n channel=<null>";
      }
      
      if ( t instanceof UnresolvedAddressException ){
    	  Debug.outNoStack( msg );
      }else{
    	  Debug.out( msg, t );
      }
      
      
      if( request.channel != null ) {
    	  closeConnection( request.channel );
      }
      request.listener.connectFailure( t );
    }
  }
  
  
  
  
  private void finishConnect( ConnectionRequest request ) {
    try {
      if( request.channel.finishConnect() ) {
            
        if( SHOW_CONNECT_STATS ) {
          long queue_wait_time = request.connect_start_time - request.request_start_time;
          long connect_time = SystemTime.getCurrentTime() - request.connect_start_time;
          int num_queued = new_requests.size();
          int num_connecting = pending_attempts.size();
          System.out.println("S: queue_wait_time="+queue_wait_time+
                              ", connect_time="+connect_time+
                              ", num_queued="+num_queued+
                              ", num_connecting="+num_connecting);
        }
        
        //ensure the request hasn't been canceled during the select op
        boolean canceled = false;
        try{  new_canceled_mon.enter();
          canceled = canceled_requests.contains( request.listener );
        }
        finally{ new_canceled_mon.exit(); }
        
        if( canceled ) {
        	closeConnection( request.channel );
        }
        else {
        	connect_selector.cancel( request.channel );
          request.listener.connectSuccess( request.channel );
        }
      }
      else { //should never happen
        Debug.out( "finishConnect() failed" );
        request.listener.connectFailure( new Throwable( "finishConnect() failed" ) );
        
        closeConnection( request.channel );
      }
    }
    catch( Throwable t ) {
          
      if( SHOW_CONNECT_STATS ) {
        long queue_wait_time = request.connect_start_time - request.request_start_time;
        long connect_time = SystemTime.getCurrentTime() - request.connect_start_time;
        int num_queued = new_requests.size();
        int num_connecting = pending_attempts.size();
        System.out.println("F: queue_wait_time="+queue_wait_time+
                            ", connect_time="+connect_time+
                            ", num_queued="+num_queued+
                            ", num_connecting="+num_connecting);
      }
          
      request.listener.connectFailure( t );
      
      closeConnection( request.channel );
    }
  }
  

  
  private void runSelect() {
    //do cancellations
    try{
      new_canceled_mon.enter();

      for (Iterator can_it =canceled_requests.iterator(); can_it.hasNext();) {
        ConnectListener key =(ConnectListener) can_it.next();

        ConnectionRequest to_remove =null;

        for (Iterator pen_it =pending_attempts.keySet().iterator(); pen_it.hasNext();) {
          ConnectionRequest request =(ConnectionRequest) pen_it.next();
          if (request.listener ==key) {
            connect_selector.cancel(request.channel);

            closeConnection(request.channel);

            to_remove =request;
            break;
          }
        }

        if( to_remove != null ) {
          pending_attempts.remove( to_remove );
        }
      }

      canceled_requests.clear();
    }
    finally{
      new_canceled_mon.exit();
    }

    //run select
    try{
      connect_selector.select(100);
    }
    catch( Throwable t ) {
      Debug.out("connnectSelectLoop() EXCEPTION: ", t);
    }

    //do connect attempt timeout checks
    int num_stalled_requests =0;
    final long now =SystemTime.getCurrentTime();
    for (Iterator i =pending_attempts.keySet().iterator(); i.hasNext();) {
      final ConnectionRequest request =(ConnectionRequest) i.next();
      final long waiting_time =now -request.connect_start_time;
      if( waiting_time > request.connect_timeout ) {
        i.remove();

        SocketChannel channel = request.channel;
        
        connect_selector.cancel( channel );

        closeConnection( channel );
              
        InetSocketAddress	sock_address = request.address;
        
       	InetAddress a = sock_address.getAddress();
        	
       	String	target;
       	
       	if ( a != null ){
        		
        	target = a.getHostAddress() + ":" + sock_address.getPort();
        		
        }else{
        		
        	target = sock_address.toString();
        }
               
        request.listener.connectFailure( new SocketTimeoutException( "Connection attempt to " + target + " aborted: timed out after " + request.connect_timeout/1000+ "sec" ) );
      }
      else if( waiting_time >= CONNECT_ATTEMPT_STALL_TIME ) {
        num_stalled_requests++;
      }
      else if( waiting_time < 0 ) {  //time went backwards
        request.connect_start_time =now;
      }
    }

    //check if our connect queue is stalled, and expand if so
    if (num_stalled_requests ==pending_attempts.size() &&pending_attempts.size() <MAX_SIMULTANIOUS_CONNECT_ATTEMPTS) {
      ConnectionRequest cr =null;

      try{
        new_canceled_mon.enter();

        if( !new_requests.isEmpty() ) {
            Iterator it = new_requests.iterator();
            cr = (ConnectionRequest)it.next();
            it.remove();
        }
      }
      finally{
        new_canceled_mon.exit();
      }

      if( cr != null ) {
        addNewRequest( cr );
      }
    }
  }
  
  
  private void doClosings() {
    try{
    	pending_closes_mon.enter();
    
    	long	now = SystemTime.getCurrentTime();
    	
    	if ( delayed_closes.size() > 0 ){
    		   		
    		Iterator	it = delayed_closes.entrySet().iterator();
    		
    		while( it.hasNext()){
    			
    			Map.Entry	entry = (Map.Entry)it.next();
    			
    			long	wait = ((Long)entry.getValue()).longValue() - now;
    			
    			if ( wait < 0 || wait > 60*1000 ){
    				
    				pending_closes.addLast( entry.getKey());
    				
    				it.remove();    				
    			}
    		}
    	}
    	
    	while( !pending_closes.isEmpty() ) {
    		
    		SocketChannel channel = (SocketChannel)pending_closes.removeFirst();
    		if( channel != null ) {
        	
    			connect_selector.cancel( channel );
        	
    			try{ 
    				channel.close();
    			}
    			catch( Throwable t ) {
    				/*Debug.printStackTrace(t);*/
    			}
    		}
    	}
    }finally{
    	
    	pending_closes_mon.exit();
    }
  }
  
  
  /**
   * Request that a new connection be made out to the given address.
   * @param address remote ip+port to connect to
   * @param listener to receive notification of connect attempt success/failure
   */
  public void requestNewConnection( InetSocketAddress address, ConnectListener listener, boolean high_priority ) {   
	  requestNewConnection( address, listener, CONNECT_ATTEMPT_TIMEOUT, high_priority  );
  }
  
  public void requestNewConnection( InetSocketAddress address, ConnectListener listener, long connect_timeout, boolean high_priority ) {    
	    try{
	      new_canceled_mon.enter();
	    
	      //insert at a random position because new connections are usually added in 50-peer
	      //chunks, i.e. from a tracker announce reply, and we want to evenly distribute the
	      //connect attempts if there are multiple torrents running

		  ConnectionRequest cr = new ConnectionRequest( connection_request_id_next++, address, listener, connect_timeout, high_priority );

	      new_requests.add( cr );
	      
	      if ( new_requests.size() >= max_outbound_connections ){
			
	    	if ( !max_conn_exceeded_logged ){
	    		
	    		max_conn_exceeded_logged = true;
	    	
	    		Debug.out( "TCPConnectionManager: max outbound connection limit reached (" + max_outbound_connections + ")" );
	    	}
	      }
	    }finally{
	    	
	      new_canceled_mon.exit();
	    }
  }
  
  /**
   * Close the given connection.
   * @param channel to close
   */
  public void 
  closeConnection( 
	SocketChannel channel ) 
  {
	  closeConnection( channel, 0 );
  }

  public void closeConnection( SocketChannel channel, int delay ) {
    try{
    	pending_closes_mon.enter();
    
    	if ( delay == 0 ){
    		
    		if ( !delayed_closes.containsKey( channel )){
    		
	    		if ( !pending_closes.contains( channel )){
	    			
	    			pending_closes.addLast( channel );
	    		}
    		}
    	}else{
    		
    		delayed_closes.put( channel, new Long( SystemTime.getCurrentTime() + delay ));
    	}
    }finally{
    	
    	pending_closes_mon.exit();
    }
  }
  
  
  /**
   * Cancel a pending new connection request.
   * @param listener_key used in the initial connect request
   */
  public void cancelRequest( ConnectListener listener_key ) {
    try{
      new_canceled_mon.enter();
    
      //check if we can cancel it right away
      for( Iterator i = new_requests.iterator(); i.hasNext(); ) {
        ConnectionRequest request = (ConnectionRequest)i.next();
        if( request.listener == listener_key ) {
          i.remove();
          return;
        }
      }
      
      canceled_requests.add( listener_key ); //else add for later removal during select
    }
    finally{
      new_canceled_mon.exit();
    }
  }
  
  

  private static class ConnectionRequest {
    private final InetSocketAddress address;
    private final ConnectListener listener;
    private final long request_start_time;
    private long connect_start_time;
    private final long connect_timeout;
    private SocketChannel channel;
    private final short		rand;
    private final boolean	high_priority;
    private final long		id;
        
    private ConnectionRequest( long _id, InetSocketAddress _address, ConnectListener _listener, long _connect_timeout, boolean _high_priority  ) {

      id	= _id;
      address = _address;
      listener = _listener;
      connect_timeout	= _connect_timeout;
      request_start_time = SystemTime.getCurrentTime();
      rand = (short)( Short.MAX_VALUE*Math.random());
      high_priority = _high_priority;
    }
    
    private long
    getID()
    {
    	return( id );
    }
    
    private int
    getPriority()
    {
    	return( high_priority?1:2 );
    }
    
    private short
    getRandom()
    {
    	return( rand );
    }
  }
  
  
///////////////////////////////////////////////////////////  
  
  /**
   * Listener for notification of connection establishment.
   */
  public interface ConnectListener {
     /**
      * The connection establishment process has started,
      * i.e. the connection is actively being attempted.
      */
     public void connectAttemptStarted();    
     
     /**
      * The connection attempt succeeded.
      * @param channel connected socket channel
      */
     public void connectSuccess( SocketChannel channel ) ;
     
    
    /**
     * The connection attempt failed.
     * @param failure_msg failure reason
     */
    public void connectFailure( Throwable failure_msg );
  }
   
/////////////////////////////////////////////////////////////
   
}
