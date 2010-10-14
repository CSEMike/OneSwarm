/*
 * Created on Apr 26, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.peerdb;

import java.util.*;

import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.util.bloom.*;


/**
 *
 */
public class PeerDatabase {
  private static final int MIN_REBUILD_WAIT_TIME = 60*1000;
  private static final int MAX_DISCOVERED_PEERS = 500;
  
  private static final int BLOOM_ROTATION_PERIOD = 7*60*1000;
  private static final int BLOOM_FILTER_SIZE = 10000;
  
  
  private final HashMap peer_connections = new HashMap();
  private final LinkedList discovered_peers = new LinkedList();
  private final AEMonitor map_mon = new AEMonitor( "PeerDatabase" );
  
  private PeerItem[] cached_peer_popularities = null;
  private int popularity_pos = 0;
  private long last_rebuild_time = 0;
  private long last_rotation_time = 0;
  
  private PeerItem self_peer;
  
  private BloomFilter filter_one = null;
  private BloomFilter filter_two = BloomFilterFactory.createAddOnly( BLOOM_FILTER_SIZE );
  
  
  
  
  
  protected PeerDatabase() {
    /* nothing */
  }
  
  
  /**
   * Register a new peer connection with the database.
   * @param base_peer_item key
   * @return registered connection
   */
  public PeerExchangerItem registerPeerConnection( PeerItem base_peer_item, PeerExchangerItem.Helper helper ) {
    try{  map_mon.enter();
      PeerExchangerItem new_connection = new PeerExchangerItem( this, base_peer_item, helper );
      
      //update connection adds
      for( Iterator it = peer_connections.entrySet().iterator(); it.hasNext(); ) {  //go through all existing connections
        Map.Entry entry = (Map.Entry)it.next();
        PeerItem old_key = (PeerItem)entry.getKey();
        PeerExchangerItem old_connection = (PeerExchangerItem)entry.getValue();
        
        if( old_connection.getHelper().isSeed() && new_connection.getHelper().isSeed() ) {
          continue;  //dont exchange seed peers to other seeds
        }
        
        old_connection.notifyAdded( base_peer_item );  //notify existing connection of new one
        new_connection.notifyAdded( old_key );  //notify new connection of existing one for initial exchange
      }

      peer_connections.put( base_peer_item, new_connection );
      return new_connection;
    }
    finally{  map_mon.exit();  }
  }
  
  
  protected void deregisterPeerConnection( PeerItem base_peer_key ) {
    try{  map_mon.enter();
      peer_connections.remove( base_peer_key );

      //update connection drops
      for( Iterator it = peer_connections.values().iterator(); it.hasNext(); ) {  //go through all remaining connections
        PeerExchangerItem old_connection = (PeerExchangerItem)it.next();
        
        //dont skip seed2seed drop notification, as the dropped peer may not have been seeding initially
        old_connection.notifyDropped( base_peer_key );  //notify existing connection of drop
      } 
    }
    finally{  map_mon.exit();  }
  }
  
  public void
  seedStatusChanged(
	  PeerExchangerItem	item )
  {
	  	// only bother with non-seed -> seed transitions, the opposite are too rate to bother with
	  
	  if ( item.getHelper().isSeed()){
		 
		  try{  
			  map_mon.enter();
		  
			  for ( Iterator it = peer_connections.values().iterator(); it.hasNext(); ){
				  		  
				  PeerExchangerItem connection = (PeerExchangerItem)it.next();
				  
				  if ( connection != item && connection.getHelper().isSeed()){
					  
					  // System.out.println( "seedStatusChanged: dropping: originator= " + item.getBasePeer().getAddressString() + ",target=" + connection.getBasePeer().getAddressString());
					  
					  connection.notifyDropped( item.getBasePeer() );
					  
					  item.notifyDropped( connection.getBasePeer() );
				  }
			  }
	      }finally{ 
	    	  
	    	  map_mon.exit();
	      }
	  }
  }
  
  
  /**
   * Add a potential peer obtained via tracker announce, DHT announce, plugin, etc.
   * @param peer to add
   */
  public void addDiscoveredPeer( PeerItem peer ) {
    try{  map_mon.enter();
      for( Iterator it = peer_connections.values().iterator(); it.hasNext(); ) {  //check to make sure we dont already know about this peer
        PeerExchangerItem connection = (PeerExchangerItem)it.next();
        if( connection.isConnectedToPeer( peer ) )  return;  //we already know about this peer via exchange, so ignore discovery
      }
      
      if( !discovered_peers.contains( peer ) ) {
        discovered_peers.addLast( peer );  //add unknown peer

        int max_cache_size = PeerUtils.MAX_CONNECTIONS_PER_TORRENT;
        if( max_cache_size < 1 || max_cache_size > MAX_DISCOVERED_PEERS )  max_cache_size = MAX_DISCOVERED_PEERS;
        
        if( discovered_peers.size() > max_cache_size ) {
          discovered_peers.removeFirst();
        }
      }
    }
    finally{  map_mon.exit();  }
  }

  
  /**
   * Mark the given peer as ourself.
   * @param self peer
   */
  public void setSelfPeer( PeerItem self ) {  self_peer = self;  }

  /**
   * Get the peer item that represents ourself.
   * @return self peer, or null if unknown
   */
  public PeerItem getSelfPeer() {
    /*
    //disabled for now, as getExternalIpAddress() will potential run a full version check every 60s
    if( self_peer == null ) {
      //determine our 'self' info from config
      String ip = VersionCheckClient.getSingleton().getExternalIpAddress();
      if( ip != null && ip.length() > 0 ) {
        self_peer = PeerItemFactory.createPeerItem( ip, NetworkManager.getSingleton().getTCPListeningPortNumber(), 0 );
      }
    }
    */
    
    return self_peer;
  }
  
  public PeerItem[]
  getDiscoveredPeers()
  {
	  try{  
		  map_mon.enter();
	  
		  return((PeerItem[])discovered_peers.toArray( new PeerItem[discovered_peers.size()] ));
		  
	  }finally{  
		
		  map_mon.exit();  
	  }
  }
  
  public int
  getDiscoveredPeerCount()
  {
	  try{  
		  map_mon.enter();
	  
		  return( discovered_peers.size());
		  
	  }finally{  
		
		  map_mon.exit();  
	  } 
  }
  
  /**
   * Get the next potential peer for optimistic connect.
   * @return peer to connect, or null of no optimistic peer available
   */
  public PeerItem getNextOptimisticConnectPeer( ) {
	  return(getNextOptimisticConnectPeer(0));
  }
  
  protected PeerItem getNextOptimisticConnectPeer( int recursion_count ) {
    PeerItem peer = null;
    boolean	 discovered_peer = false;
    
    //first see if there are any unknown peers to try
    try{  map_mon.enter();
      if( !discovered_peers.isEmpty() ) {
        peer = (PeerItem)discovered_peers.removeFirst();
        
        discovered_peer	= true;
      }
    }
    finally{  map_mon.exit();  }
    
    //pick one from those obtained via peer exchange if needed
    if( peer == null ) {
      if( cached_peer_popularities == null || popularity_pos == cached_peer_popularities.length ) {  //rebuild needed
        cached_peer_popularities = null;  //clear cache
        
        long time_since_rebuild = SystemTime.getCurrentTime() - last_rebuild_time;
        //only allow exchange list rebuild every few min, otherwise we'll spam attempts endlessly
        if( time_since_rebuild > MIN_REBUILD_WAIT_TIME || time_since_rebuild < 0 ) {
          cached_peer_popularities = getExchangedPeersSortedByLeastPopularFirst();
          popularity_pos = 0;
          last_rebuild_time = SystemTime.getCurrentTime();
        }
      }
      
      if( cached_peer_popularities != null && cached_peer_popularities.length > 0 ) {
        peer = cached_peer_popularities[ popularity_pos ];
        popularity_pos++;
        last_rebuild_time = SystemTime.getCurrentTime();  //ensure rebuild waits min rebuild time after the cache is depleted before trying attempts again
      }
    }

    //to reduce the number of wasted outgoing attempts, we limit how frequently we hand out the same optimistic peer in a given time period
    if( peer != null ) {
    	//check if it's time to rotate the bloom filters
    	
      long diff = SystemTime.getCurrentTime() - last_rotation_time;
      
      if( diff < 0 || diff > BLOOM_ROTATION_PERIOD ) {
        filter_one = filter_two;
        filter_two = BloomFilterFactory.createAddOnly( BLOOM_FILTER_SIZE );
        last_rotation_time = SystemTime.getCurrentTime();
      }
      
      	//check to see if we've already given this peer out optimistically in the last 5-10min
      
      boolean	already_recorded = false;
      
      byte[]	peer_serialisation = peer.getSerialization();
      
      if( filter_one.contains( peer_serialisation ) && recursion_count < 100 ) {
    	  
    	  	// we've recently given this peer, so recursively find another peer to try
      
    	  PeerItem next_peer = getNextOptimisticConnectPeer( recursion_count + 1);
    	  
    	  if ( next_peer != null ){
    		  
    		  	// we've found a better peer that this one. If the existing peer was discovered (as opposed to PEX)
    		  	// then we'll save it for later as it might come in useful
    		  
    		  if ( discovered_peer ){
    			  
    		    try{  map_mon.enter();
  
    		    	discovered_peers.addLast( peer );
   
   			    }finally{  
   			    	map_mon.exit();  
   			    }
    		  }
    		  
    		  peer	= next_peer;
    		  
    		  already_recorded	= true;	// this peer's already in the bloom filters as it has been returned
    		  							// by a recursive call
    	  }
      }
      
      if( !already_recorded ) {  //we've found a suitable peer
        
        filter_one.add( peer_serialisation );
        filter_two.add( peer_serialisation );
      }
    }
    
    return peer;
  }
  

  
  
  private PeerItem[] getExchangedPeersSortedByLeastPopularFirst() {
    HashMap popularity_counts = new HashMap();
    
    try{  map_mon.enter();
      //count popularity of all known peers
      for( Iterator it = peer_connections.values().iterator(); it.hasNext(); ) { 
        PeerExchangerItem connection = (PeerExchangerItem)it.next();
        PeerItem[] peers = connection.getConnectedPeers();
        
        for( int i=0; i < peers.length; i++ ) {
          PeerItem peer = peers[i];
          Integer count = (Integer)popularity_counts.get( peer );
          
          if( count == null ) {
            count = new Integer( 1 );
          }
          else {
            count = new Integer( count.intValue() + 1 );
          }
          
          popularity_counts.put( peer, count );
        }
      }
    }
    finally{  map_mon.exit();  }
    
    if( popularity_counts.isEmpty() )  return null;
        
    //now sort by popularity    
    Map.Entry[] sorted_entries = new Map.Entry[ popularity_counts.size() ];
    popularity_counts.entrySet().toArray( sorted_entries );

    Arrays.sort( sorted_entries, new Comparator() {
      public int compare( Object obj1, Object obj2 ) {
        Map.Entry en1 = (Map.Entry)obj1;
        Map.Entry en2 = (Map.Entry)obj2;
        return ((Integer)en1.getValue()).compareTo( (Integer)en2.getValue() );  //we want least popular in front
      }
    });
    
    PeerItem[] sorted_peers = new PeerItem[ sorted_entries.length ];
      
    for( int i=0; i < sorted_entries.length; i++ ) {
      Map.Entry entry = sorted_entries[i];
      sorted_peers[i] = (PeerItem)entry.getKey();
    } 
    
    return sorted_peers;
  }
  
  
  //TODO destroy() method?
  
  public String
  getString()
  {
	  return("pc=" + peer_connections.size() + ",dp=" + discovered_peers.size());
  }
}
