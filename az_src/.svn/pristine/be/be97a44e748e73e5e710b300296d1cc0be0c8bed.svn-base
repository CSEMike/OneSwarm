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
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
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
  public static int MAX_SIMULTANIOUS_CONNECT_ATTEMPTS;
  
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
					max_outbound_connections = COConfigurationManager.getIntParameter( "network.tcp.max.connections.outstanding" );
				}
			});
  }
  
  
  private static final int CONNECT_ATTEMPT_TIMEOUT = 15*1000;  // parg: reduced from 30 sec as almost never see worthwhile connections take longer that this
  private static final int CONNECT_ATTEMPT_STALL_TIME = 3*1000;  //3sec
  private static final boolean SHOW_CONNECT_STATS = false;
  
  private final VirtualChannelSelector connect_selector = new VirtualChannelSelector( "Connect/Disconnect Manager", VirtualChannelSelector.OP_CONNECT, true );
  
  private long connection_request_id_next;
  
  private final Set<ConnectionRequest> new_requests = 
	  new TreeSet<ConnectionRequest>(
			new Comparator<ConnectionRequest>()
			{
				public int 
				compare(
					ConnectionRequest r1, 
					ConnectionRequest r2 )
				{
					if ( r1 == r2 ){
						
						return( 0 );
					}
					
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
  
  private final List<ConnectListener> 	canceled_requests 	= new ArrayList<ConnectListener>();
  
  private final AEMonitor	new_canceled_mon	= new AEMonitor( "ConnectDisconnectManager:NCM");
  
  private final Map<ConnectionRequest,Object> pending_attempts = new HashMap<ConnectionRequest, Object>();
  
  private final LinkedList<SocketChannel> 	pending_closes 	= new LinkedList<SocketChannel>();
  
  private final Map<SocketChannel,Long>		delayed_closes	= new HashMap<SocketChannel, Long>();
  
  private final AEMonitor	pending_closes_mon = new AEMonitor( "ConnectDisconnectManager:PC");
       
  private boolean max_conn_exceeded_logged;
  
  
  public 
  TCPConnectionManager() 
  {
	  Set<String>	types = new HashSet<String>();
	  
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
						Set<String>				types,
						Map<String,Object>		values )
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
	  
	  new AEThread2( "ConnectDisconnectManager", true ) 
	  {
		  public void 
		  run() 
		  {
			  while( true ){
				  
				  addNewOutboundRequests();
				  
				  runSelect();
				  
				  doClosings();
			  }
		  }
	  }.start();
  }
  
  public int
  getMaxOutboundPermitted()
  {
	  return( Math.max( max_outbound_connections - new_requests.size(), 0 ));
  }
  
  private void 
  addNewOutboundRequests() 
  {    
	  while( pending_attempts.size() < MIN_SIMULTANIOUS_CONNECT_ATTEMPTS ){

		  ConnectionRequest cr = null;

		  try{
			  new_canceled_mon.enter();

			  if( new_requests.isEmpty() )  break;

			  Iterator<ConnectionRequest> it = new_requests.iterator();

			  cr = it.next();

			  it.remove();

		  }finally{

			  new_canceled_mon.exit();
		  }

		  if( cr != null ){

			  addNewRequest( cr ); 
		  }
	  }
  }
  
  

  private void 
  addNewRequest( 
	final ConnectionRequest request ) 
  {
	  request.setConnectTimeout( request.listener.connectAttemptStarted( request.getConnectTimeout()));


	  boolean ipv6problem = false;

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

			  String ip_tos = COConfigurationManager.getStringParameter( "network.tcp.socket.IPDiffServ" );
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

			  try {
				  InetAddress bindIP = NetworkAdmin.getSingleton().getMultiHomedOutgoingRoundRobinBindAddress(request.address.getAddress());
				  if ( bindIP != null ) {
					  if (Logger.isEnabled()) 	Logger.log(new LogEvent(LOGID, "Binding outgoing connection [" + request.address + "] to local IP address: " + bindIP+":"+local_bind_port));
					  request.channel.socket().bind( new InetSocketAddress( bindIP, local_bind_port ) );
				  }
				  else if( local_bind_port > 0 ) {       
					  if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Binding outgoing connection [" + request.address + "] to local port #: " +local_bind_port));
					  request.channel.socket().bind( new InetSocketAddress( local_bind_port ) );     
				  }
			  } catch(SocketException e) {
				  if(e.getMessage().equals("Address family not supported by protocol family: bind") && !NetworkAdmin.getSingleton().hasIPV6Potential(true));
				  ipv6problem = true;
				  throw e;
			  }

		  }
		  catch( Throwable t ) {
			  if(!ipv6problem)
			  {
				  //dont pass the exception outwards, so we will continue processing connection without advanced options set
				  String msg = "Error while processing advanced socket options.";
				  Debug.out( msg, t );
				  Logger.log(new LogAlert(LogAlert.UNREPEATABLE, msg, t));
			  } else
			  {
				  // can't support NIO + ipv6 on this system, pass on and don't raise an alert
				  throw t;
			  }
		  }

		  request.channel.configureBlocking( false );
		  request.connect_start_time = SystemTime.getMonotonousTime();

		  if ( request.channel.connect( request.address ) ) {  //already connected

			  finishConnect( request );

		  }else{

			  //not yet connected, so register for connect selection

			  try{
				  new_canceled_mon.enter();

				  pending_attempts.put( request, null );

			  }finally{

				  new_canceled_mon.exit();
			  }
			  
			  connect_selector.register( 
					  request.channel, 
					  new VirtualChannelSelector.VirtualSelectorListener() 
					  {
						  public boolean 
						  selectSuccess( 
								  VirtualChannelSelector 	selector, 
								  SocketChannel 			sc, 
								  Object 					attachment ) 
						  {    
							  try{
								  new_canceled_mon.enter();

								  pending_attempts.remove( request );

							  }finally{

								  new_canceled_mon.exit();
							  }

							  finishConnect( request );

							  return true;
						  }

						  public void 
						  selectFailure( 
								  VirtualChannelSelector 	selector, 
								  SocketChannel 			sc,
								  Object 					attachment, 
								  Throwable 				msg ) 
						  {
							  try{
								  new_canceled_mon.enter();

								  pending_attempts.remove( request );

							  }finally{

								  new_canceled_mon.exit();
							  }

							  closeConnection( request.channel );

							  request.listener.connectFailure( msg );
						  }
					  }, null );
		  }
	  }catch( Throwable t ){

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

		  if (ipv6problem || t instanceof UnresolvedAddressException || t instanceof NoRouteToHostException ){
			  
			  Logger.log(new LogEvent(LOGID,LogEvent.LT_WARNING,msg));
			  
		  }else{
			  
			  Logger.log(new LogEvent(LOGID,LogEvent.LT_ERROR,msg,t));
		  }
		  
		  if( request.channel != null ){
			  
			  closeConnection( request.channel );
		  }
		  
		  request.listener.connectFailure( t );
	  }
  }
  
  
  
  
  private void 
  finishConnect( 
	ConnectionRequest request ) 
  {
	  try {
		  if( request.channel.finishConnect() ) {

			  if( SHOW_CONNECT_STATS ) {
				  long queue_wait_time = request.connect_start_time - request.request_start_time;
				  long connect_time = SystemTime.getMonotonousTime() - request.connect_start_time;
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
			  	
			  }finally{
				  
				  new_canceled_mon.exit(); 
			  }

			  if( canceled ){
				  
				  closeConnection( request.channel );
				  
			  }else{
				  
				  connect_selector.cancel( request.channel );
				  
				  request.listener.connectSuccess( request.channel );
			  }
		  }else{ 
			  
			  		//should never happen
			  
			  Debug.out( "finishConnect() failed" );
			  
			  request.listener.connectFailure( new Throwable( "finishConnect() failed" ) );

			  closeConnection( request.channel );
		  }
	  }catch( Throwable t ) {

		  if( SHOW_CONNECT_STATS ) {
			  long queue_wait_time = request.connect_start_time - request.request_start_time;
			  long connect_time = SystemTime.getMonotonousTime() - request.connect_start_time;
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

      for (Iterator<ConnectListener> can_it = canceled_requests.iterator(); can_it.hasNext();){
    	  
        ConnectListener key =can_it.next();

        for (Iterator<ConnectionRequest> pen_it = pending_attempts.keySet().iterator(); pen_it.hasNext();) {
        	
          ConnectionRequest request =pen_it.next();
          
          if ( request.listener == key ){
         	  
            connect_selector.cancel(request.channel);

            closeConnection(request.channel);

            pen_it.remove();
            
            break;
          }
        }
      }

      canceled_requests.clear();
      
    }finally{
    	
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
    
    final long now =SystemTime.getMonotonousTime();
    
    List<ConnectionRequest> timeouts = null;
    try{
        new_canceled_mon.enter();

	    for (Iterator<ConnectionRequest> i =pending_attempts.keySet().iterator(); i.hasNext();) {
	    	
	      final ConnectionRequest request =i.next();
	      
	      final long waiting_time =now -request.connect_start_time;
	      
	      if( waiting_time > request.connect_timeout ) {
	    	  
	        i.remove();
	
	        SocketChannel channel = request.channel;
	        
	        connect_selector.cancel( channel );
	
	        closeConnection( channel );

	        if ( timeouts == null ){
	        	
	        	timeouts = new ArrayList<ConnectionRequest>();
	        }
	        
	        timeouts.add( request );
	              
	      }else if( waiting_time >= CONNECT_ATTEMPT_STALL_TIME ) {
	    	  
	        num_stalled_requests++;
	        
	      }else if( waiting_time < 0 ) {  //time went backwards
	    	  
	        request.connect_start_time =now;
	      }
	    }
    }finally{
    	
        new_canceled_mon.exit();
    }
    
    if ( timeouts != null ){
    	
    	for ( ConnectionRequest request: timeouts ){
    		
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
    }
    
    //check if our connect queue is stalled, and expand if so
    if ( num_stalled_requests == pending_attempts.size() && pending_attempts.size() <MAX_SIMULTANIOUS_CONNECT_ATTEMPTS) {

    	ConnectionRequest cr =null;

    	try{
    		new_canceled_mon.enter();

    		if( !new_requests.isEmpty()){

    			Iterator<ConnectionRequest> it = new_requests.iterator();

    			cr = it.next();

    			it.remove();
    		}
    	}finally{
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
    
    	long	now = SystemTime.getMonotonousTime();
    	
    	if ( delayed_closes.size() > 0 ){
    		   		
    		Iterator<Map.Entry<SocketChannel,Long>>	it = delayed_closes.entrySet().iterator();
    		
    		while( it.hasNext()){
    			
    			Map.Entry<SocketChannel,Long>	entry = (Map.Entry<SocketChannel,Long>)it.next();
    			
    			long	wait = ((Long)entry.getValue()).longValue() - now;
    			
    			if ( wait < 0 || wait > 60*1000 ){
    				
    				pending_closes.addLast( entry.getKey());
    				
    				it.remove();    				
    			}
    		}
    	}
    	
    	while( !pending_closes.isEmpty() ) {
    		
    		SocketChannel channel = pending_closes.removeFirst();
    		
    		if( channel != null ) {
        	
    			connect_selector.cancel( channel );
        	
    			try{ 
    				channel.close();
    				
    			}catch( Throwable t ){
    				
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
  public void requestNewConnection( InetSocketAddress address, ConnectListener listener, int priority ) {   
	  requestNewConnection( address, listener, CONNECT_ATTEMPT_TIMEOUT, priority  );
  }
  
  public void 
  requestNewConnection( 
	  InetSocketAddress 	address, 
	  ConnectListener 		listener, 
	  int					connect_timeout, 
	  int 					priority )
  {    
	  List<ConnectionRequest>	kicked = null;
	  
	  try{
		  new_canceled_mon.enter();

		  //insert at a random position because new connections are usually added in 50-peer
		  //chunks, i.e. from a tracker announce reply, and we want to evenly distribute the
		  //connect attempts if there are multiple torrents running

		  ConnectionRequest cr = new ConnectionRequest( connection_request_id_next++, address, listener, connect_timeout, priority );

		  new_requests.add( cr );

		  if ( new_requests.size() >= max_outbound_connections ){

			  if ( !max_conn_exceeded_logged ){

				  max_conn_exceeded_logged = true;

				  Debug.out( "TCPConnectionManager: max outbound connection limit reached (" + max_outbound_connections + ")" );
			  }
		  }

		  if ( priority == ProtocolEndpoint.CONNECT_PRIORITY_HIGHEST ){

			  for (Iterator<ConnectionRequest> pen_it = pending_attempts.keySet().iterator(); pen_it.hasNext();){

				  ConnectionRequest request =(ConnectionRequest) pen_it.next();

				  if ( request.priority == ProtocolEndpoint.CONNECT_PRIORITY_LOW ){

					  if ( !canceled_requests.contains( request.listener )){
					  
						  canceled_requests.add( request.listener );
					  
						  if ( kicked == null ){
						  
							  kicked = new ArrayList<ConnectionRequest>();
						  }
					  
						  kicked.add( request );
					  }
				  }
			  }
		  }
	  }finally{

		  new_canceled_mon.exit();
	  }
	  
	  if ( kicked != null ){
		  
		  for (int i=0;i<kicked.size();i++){
			  
			  try{
				  ((ConnectionRequest)kicked.get(i)).listener.connectFailure(
						 new Exception( "Low priority connection request abandoned in favour of high priority" ));
				  
			  }catch( Throwable e ){
				  
				  Debug.printStackTrace( e );
			  }
		  }
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

  public void 
  closeConnection( 
		SocketChannel channel, 
		int delay ) 
  {
	  try{
		  pending_closes_mon.enter();

		  if ( delay == 0 ){

			  if ( !delayed_closes.containsKey( channel )){

				  if ( !pending_closes.contains( channel )){

					  pending_closes.addLast( channel );
				  }
			  }
		  }else{

			  delayed_closes.put( channel, new Long( SystemTime.getMonotonousTime() + delay ));
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
      for( Iterator<ConnectionRequest> i = new_requests.iterator(); i.hasNext(); ) {
        ConnectionRequest request = i.next();
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
  
  

  private static class 
  ConnectionRequest 
  {
    private final InetSocketAddress address;
    private final ConnectListener listener;
    private final long request_start_time;
    private long connect_start_time;
    private int connect_timeout;
    private SocketChannel channel;
    private final short		rand;
    private final int		priority;
    private final long		id;
        
    private ConnectionRequest( long _id, InetSocketAddress _address, ConnectListener _listener, int _connect_timeout, int _priority  ) {

      id	= _id;
      address = _address;
      listener = _listener;
      connect_timeout	= _connect_timeout;
      request_start_time = SystemTime.getMonotonousTime();
      rand = (short)( Short.MAX_VALUE*Math.random());
      priority = _priority;
    }
    
    private int
    getConnectTimeout()
    {
    	return( connect_timeout );
    }
    
    private void
    setConnectTimeout(
    	int		_ct )
    {
    	connect_timeout = _ct;
    }
    
    private long
    getID()
    {
    	return( id );
    }
    
    private int
    getPriority()
    {
    	return( priority );
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
      * @return adjusted connect timeout
      */
     public int connectAttemptStarted( int default_timeout );    
     
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
