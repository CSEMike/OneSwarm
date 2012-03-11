/*
 * Created on Oct 7, 2004
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkConnectionBase;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.RateHandler;




/**
 *
 */
public class TransferProcessor {
  private static final boolean RATE_LIMIT_LAN_TOO	= false;
  
  static{
	  if ( RATE_LIMIT_LAN_TOO ){
		  
		  System.err.println( "**** TransferProcessor: RATE_LIMIT_LAN_TOO enabled ****" );
	  }
  }
  
  public static final int TYPE_UPLOAD   = 0;
  public static final int TYPE_DOWNLOAD = 1;
  
  private final LimitedRateGroup max_rate;
  
  private final RateHandler	main_rate_handler;
  private final ByteBucket main_bucket;
  private final EntityHandler main_controller;
  
  private final HashMap group_buckets = new HashMap();
  private final HashMap connections = new HashMap();
  private final AEMonitor connections_mon;

  private final boolean	multi_threaded;
  
  /**
   * Create new transfer processor for the given read/write type, limited to the given max rate.
   * @param processor_type read or write processor
   * @param max_rate_limit to use
   */
  public TransferProcessor( int processor_type, LimitedRateGroup max_rate_limit, boolean multi_threaded ) {
    this.max_rate 		= max_rate_limit;
    this.multi_threaded	= multi_threaded;
    
    connections_mon = new AEMonitor( "TransferProcessor:" +processor_type );

    main_bucket = createBucket( max_rate.getRateLimitBytesPerSecond() ); 

    main_rate_handler = 
    	new RateHandler() {
        public int getCurrentNumBytesAllowed() {
          if( main_bucket.getRate() != max_rate.getRateLimitBytesPerSecond() ) { //sync rate
            main_bucket.setRate( max_rate.getRateLimitBytesPerSecond() );
          }
          return main_bucket.getAvailableByteCount();
        }
        
        public void bytesProcessed( int num_bytes_written ) {
          main_bucket.setBytesUsed( num_bytes_written );
        }
      };
      
    main_controller = new EntityHandler( processor_type, main_rate_handler );
  }
  

    
  
  /**
   * Register peer connection for upload handling.
   * NOTE: The given max rate limit is ignored until the connection is upgraded.
   * @param connection to register
   * @param group rate limit group
   */
  public void registerPeerConnection( NetworkConnectionBase connection, boolean upload ) {
    final ConnectionData conn_data = new ConnectionData();

    try {  connections_mon.enter();
    
      LimitedRateGroup[]	groups = connection.getRateLimiters( upload );
      //do group registration
      GroupData[]	group_datas = new GroupData[groups.length];
    
      for (int i=0;i<groups.length;i++){
    	  LimitedRateGroup group = groups[i];
    	  
		  // boolean log = group.getName().contains("parg");

    	  GroupData group_data = (GroupData)group_buckets.get( group );
	      if( group_data == null ) {
	        int limit = NetworkManagerUtilities.getGroupRateLimit( group );
	        group_data = new GroupData( createBucket( limit ) );
	        group_buckets.put( group, group_data );
	        
	        /*
	        if ( log ){
	    	  System.out.println( "Creating RL1: " + group.getName() + " -> " + group_data );
	        }
	        */
	      }
	      group_data.group_size++;
	      
	      group_datas[i] = group_data;
	      
	      /*
	      if ( log ){
	    	  System.out.println( "Applying RL1: " + group.getName() + " -> " + connection );
	      }
	      */
      }
      conn_data.groups = groups;
      conn_data.group_datas = group_datas;
      conn_data.state = ConnectionData.STATE_NORMAL;
     
      
      connections.put( connection, conn_data );
    }
    finally {  connections_mon.exit();  }
    
    main_controller.registerPeerConnection( connection );
  }
  
  public boolean isRegistered( NetworkConnectionBase connection ){
    try{ connections_mon.enter();
      return( connections.containsKey( connection ));
    }
    finally{ connections_mon.exit(); }
  }
  
  /**
   * Cancel upload handling for the given peer connection.
   * @param connection to cancel
   */
  public void deregisterPeerConnection( NetworkConnectionBase connection ) {
    try{ connections_mon.enter();
      ConnectionData conn_data = (ConnectionData)connections.remove( connection );
      
      if( conn_data != null ) {
    	  
    	GroupData[]	group_datas = conn_data.group_datas;
    	
  			//do groups de-registration
    	 
    	for (int i=0;i<group_datas.length;i++){
    		
    		GroupData	group_data = group_datas[i];
    		
    		if( group_data.group_size == 1 ) {  //last of the group
          
    			group_buckets.remove( conn_data.groups[i] ); //so remove
    			
    		}else {
    		
    			group_data.group_size--;
    		}
        }
      }
    }
    finally{ connections_mon.exit(); }
    

    main_controller.cancelPeerConnection( connection );
  }
  
  public void
  setRateLimiterFreezeState(
		boolean	frozen )
  {
	  main_bucket.setFrozen( frozen );
  }
  
  public void
  addRateLimiter(
	NetworkConnectionBase 	connection,
	LimitedRateGroup		group )
  {
	  try{ 
		  connections_mon.enter();
	  
	      ConnectionData conn_data = (ConnectionData)connections.get( connection );
	      
	      if ( conn_data != null ){
	    	 
			  LimitedRateGroup[]	groups 		= conn_data.groups;

			  for (int i=0;i<groups.length;i++){
				  
				  if ( groups[i] == group ){
					  
					  return;
				  }
			  }
			  
			  // boolean log = group.getName().contains("parg");
			  
	    	  GroupData group_data = (GroupData)group_buckets.get( group );
	    	  
		      if ( group_data == null ){
		    	  
		    	  int limit = NetworkManagerUtilities.getGroupRateLimit( group );

		    	  group_data = new GroupData( createBucket( limit ) );

		    	  /*
		    	  if ( log ){
		    		  System.out.println( "Creating RL2: " + group.getName() + " -> " + group_data );
		    	  }
				  */
		    	  
		    	  group_buckets.put( group, group_data );
		      }
		      
		      /*
		      if ( log ){
		    	  System.out.println( "Applying RL2: " + group.getName() + " -> " + connection );
		      }
			  */
		      
		      group_data.group_size++;
		   
			  GroupData[]			group_datas = conn_data.group_datas; 

		      int	len = groups.length;

		      LimitedRateGroup[]	new_groups = new LimitedRateGroup[ len + 1 ];
		      
		      System.arraycopy( groups, 0, new_groups, 0, len );
		      new_groups[len] = group;
		      
		      conn_data.groups 		= new_groups;
		      
		      GroupData[]	new_group_datas = new GroupData[ len + 1 ];
		      
		      System.arraycopy( group_datas, 0, new_group_datas, 0, len );
		      new_group_datas[len] = group_data;

		      conn_data.group_datas = new_group_datas;
	      }
	  }finally{
		 
		  connections_mon.exit();
	  }
  }
  
  public void
  removeRateLimiter(
	NetworkConnectionBase 	connection,
	LimitedRateGroup		group )
  {
	   try{ 
		   connections_mon.enter();
		   
		   ConnectionData conn_data = (ConnectionData)connections.get( connection );
	      
		   if ( conn_data != null ){
	    	  
			   LimitedRateGroup[]	groups 		= conn_data.groups;
			   GroupData[]			group_datas = conn_data.group_datas; 
			   
			   int	len = groups.length;

			   if ( len == 0 ){
				   
				   return;
			   }
			   
			   LimitedRateGroup[]	new_groups 		= new LimitedRateGroup[ len - 1 ];
			   GroupData[]			new_group_datas = new GroupData[ len - 1 ];

			   int	pos = 0;
			   
			   for (int i=0;i<groups.length;i++){
	    		
				   if ( groups[i] == group ){
					   
					   GroupData	group_data = conn_data.group_datas[i];
	    		
					   if ( group_data.group_size == 1 ){  //last of the group
	          
						   group_buckets.remove( conn_data.groups[i] ); //so remove
	    			
					   }else {
	    		
						   group_data.group_size--;
					   }
				   }else{
					   
					   if ( pos == new_groups.length ){
						   
						   return;
					   }
					   
					   new_groups[pos]		= groups[i];
					   new_group_datas[pos]	= group_datas[i];
					   
					   pos++;
				   }
			   }
			   
			   conn_data.groups 		= new_groups;
			   conn_data.group_datas 	= new_group_datas;
		   }
	   }finally{ 
		   
		   connections_mon.exit(); 
	   } 
  }
  

  // private static long last_log = 0;
  
  /**
   * Upgrade the given connection to a high-speed transfer handler.
   * @param connection to upgrade
   */
  public void upgradePeerConnection( final NetworkConnectionBase connection, int partition_id ) {
    ConnectionData connection_data = null;
    
    try{ connections_mon.enter();
      connection_data = (ConnectionData)connections.get( connection );
    }
    finally{ connections_mon.exit(); }
    
    if( connection_data != null && connection_data.state == ConnectionData.STATE_NORMAL ) {
      final ConnectionData conn_data = connection_data;
      
      main_controller.upgradePeerConnection( connection, new RateHandler() {
        public int getCurrentNumBytesAllowed() {          
          // sync global rate
          if( main_bucket.getRate() != max_rate.getRateLimitBytesPerSecond() ) {
            main_bucket.setRate( max_rate.getRateLimitBytesPerSecond() );
          }
          
          int allowed = main_bucket.getAvailableByteCount();

          // reserve bandwidth for the general pool
          allowed -= connection.getMssSize();
          
          if ( allowed < 0 )allowed = 0;
          
          	// only apply group rates to non-lan local connections 
          
          if ( RATE_LIMIT_LAN_TOO || !( connection.isLANLocal() && NetworkManager.isLANRateEnabled())){
	          // sync group rates
	          
	          try{
		           for (int i=0;i<conn_data.group_datas.length;i++){
		        	   
		        	  LimitedRateGroup group = conn_data.groups[i];
		        	  
		     		  //boolean log = group.getName().contains("parg");

			          int group_rate = NetworkManagerUtilities.getGroupRateLimit( conn_data.groups[i] );
			          
			          ByteBucket group_bucket = conn_data.group_datas[i].bucket;
			          
			          /*
			          if ( log ){
			        	  long now = SystemTime.getCurrentTime();
			        	  if ( now - last_log > 500 ){
			        		  last_log = now;
			        		  System.out.println( "    " + group.getName() + " -> " + group_rate + "/" + group_bucket.getAvailableByteCount());
			        	  }
			          }
			          */
			          
			          if ( group_bucket.getRate() != group_rate ){
			        	  
			        	  group_bucket.setRate( group_rate );
			          }
			          
			          int 	group_allowed = group_bucket.getAvailableByteCount();
			          
			          if ( group_allowed < allowed ){
			        	  
			        	  allowed = group_allowed;
			          }
		           }
	          }catch( Throwable e ){
	        	  // conn_data.group stuff is not synchronized for speed but can cause borkage if new
	        	  // limiters added so trap here
	        	  
	        	  if (!( e instanceof IndexOutOfBoundsException )){
	        		  
	        		  Debug.printStackTrace(e);
	        	  }
	          }
          }
                  	            
           return allowed;
        }

        public void bytesProcessed( int num_bytes_written ) {
          if ( RATE_LIMIT_LAN_TOO || !( connection.isLANLocal() && NetworkManager.isLANRateEnabled())){
	          for (int i=0;i<conn_data.group_datas.length;i++){
	        	  conn_data.group_datas[i].bucket.setBytesUsed( num_bytes_written );
	          }
          }
          main_bucket.setBytesUsed( num_bytes_written );
        }
      }, partition_id );
      
      conn_data.state = ConnectionData.STATE_UPGRADED;
    }
  }
  
  
  /**
   * Downgrade the given connection back to a normal-speed transfer handler.
   * @param connection to downgrade
   */
  public void downgradePeerConnection( NetworkConnectionBase connection ) {
    ConnectionData conn_data = null;
    
    try{ connections_mon.enter();
      conn_data = (ConnectionData)connections.get( connection );
    }
    finally{ connections_mon.exit(); }
    
    if( conn_data != null && conn_data.state == ConnectionData.STATE_UPGRADED ) {
      main_controller.downgradePeerConnection( connection );
      conn_data.state = ConnectionData.STATE_NORMAL;
    }
  }
  
  public RateHandler
  getRateHandler()
  {
	  return( main_rate_handler );
  }
  
  public RateHandler
  getRateHandler(
	NetworkConnectionBase	connection )
  {
	return( main_controller.getRateHandler( connection ));  
  }
  
  private ByteBucket
  createBucket(
	int	bytes_per_sec )
  {
	  if ( multi_threaded ){
		  
		  return( new ByteBucketMT( bytes_per_sec ));
		  
	  }else{
		  
		  return( new ByteBucketST( bytes_per_sec ));
	  }
  }
  
  private static class ConnectionData {
    private static final int STATE_NORMAL   = 0;
    private static final int STATE_UPGRADED = 1;
    
    private int state;
    private LimitedRateGroup[] groups;
    private GroupData[] group_datas;
  }

    
  private static class GroupData {
    private final ByteBucket bucket;
    private int group_size = 0;
    
    private GroupData( ByteBucket bucket ) {
      this.bucket = bucket;
    }
  }
  
  
}
