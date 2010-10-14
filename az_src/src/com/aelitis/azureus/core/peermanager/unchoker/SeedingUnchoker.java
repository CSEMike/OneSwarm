/*
 * Created on Apr 5, 2005
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

package com.aelitis.azureus.core.peermanager.unchoker;

import java.util.*;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;



/**
 * Unchoker implementation to be used while in seeding mode.
 */
public class SeedingUnchoker implements Unchoker {
  private ArrayList chokes = new ArrayList();
  private ArrayList unchokes = new ArrayList();
  

  public SeedingUnchoker() {
    /* nothing */
  }
  
  
  public ArrayList getImmediateUnchokes( int max_to_unchoke, ArrayList all_peers ) {
    
    int	peer_count = all_peers.size();
    
    if ( max_to_unchoke > peer_count ){
    	
    	max_to_unchoke = peer_count;
    }
  
    //count all the currently unchoked peers
    int num_unchoked = 0;
    for( int i=0; i < all_peers.size(); i++ ) {
    	PEPeerTransport peer = (PEPeerTransport)all_peers.get( i );
      if( !peer.isChokedByMe() )  num_unchoked++;
    }
    
    //if not enough unchokes
    int needed = max_to_unchoke - num_unchoked;
    
    if ( needed > 0 ) {
  
    	ArrayList to_unchoke = UnchokerUtil.getNextOptimisticPeers( all_peers, false, false, needed );
    	
    	if ( to_unchoke == null ){
    		
    		return( new ArrayList(0));
    	}
    	
    	for ( int i=0;i<to_unchoke.size();i++){
    		
    		((PEPeerTransport)to_unchoke.get(i)).setOptimisticUnchoke( true );
    	}
    	
    	return( to_unchoke );
    	
    }else{
    	   
    	return( new ArrayList(0));
    }
  }
  
  

  public void calculateUnchokes( int max_to_unchoke, ArrayList all_peers, boolean force_refresh ) {
    
    //get all the currently unchoked peers
    for( int i=0; i < all_peers.size(); i++ ) {
    	PEPeerTransport peer = (PEPeerTransport)all_peers.get( i );
      
      if( !peer.isChokedByMe() ) { 
        if( UnchokerUtil.isUnchokable( peer, false ) ) {
          unchokes.add( peer );
        }
        else {  //should be immediately choked
          chokes.add( peer );
        }
      }
    }
    
    
    //if too many unchokes
    while( unchokes.size() > max_to_unchoke ) {
      chokes.add( unchokes.remove( unchokes.size() - 1 ) );
    }
    
    
    //we only recalculate the uploads when we're force to refresh the optimistic unchokes
    if( force_refresh ) {
      int max_optimistic = ((max_to_unchoke - 1) / 5) + 1;  //one optimistic unchoke for every 5 upload slots

      //we need to make room for new opt unchokes by finding the "worst" peers
      ArrayList peers_ordered_by_rate = new ArrayList();
      ArrayList peers_ordered_by_uploaded = new ArrayList();
      
      long[] rates = new long[ unchokes.size() ];  //0-initialized
      long[] uploaded = new long[ rates.length ];  //0-initialized
        
      //calculate factor rankings
      for( int i=0; i < unchokes.size(); i++ ) {
      	PEPeerTransport peer = (PEPeerTransport)unchokes.get( i );

        long rate = peer.getStats().getDataSendRate();
        if( rate > 256 ) {  //filter out really slow peers
          //calculate reverse order by our upload rate to them
          UnchokerUtil.updateLargestValueFirstSort( rate, rates, peer, peers_ordered_by_rate, 0 );
          
          //calculate order by the total number of bytes we've uploaded to them
          UnchokerUtil.updateLargestValueFirstSort( peer.getStats().getTotalDataBytesSent(), uploaded, peer, peers_ordered_by_uploaded, 0 );
        }
      }

      Collections.reverse( peers_ordered_by_rate );  //we want higher rates at the end

      ArrayList peers_ordered_by_rank = new ArrayList();
      long[] ranks = new long[ peers_ordered_by_rate.size() ];
      Arrays.fill( ranks, Long.MIN_VALUE );
      
      //combine factor rankings to get best
      for( int i=0; i < unchokes.size(); i++ ) {
      	PEPeerTransport peer = (PEPeerTransport)unchokes.get( i );
        
        //"better" peers have high indexes (toward the end of each list)
        long rate_factor = peers_ordered_by_rate.indexOf( peer );
        long uploaded_factor = peers_ordered_by_uploaded.indexOf( peer );
        
        if( rate_factor == -1 )  continue;  //wasn't downloading fast enough, skip add so it will be choked automatically
        
        long rank_factor = rate_factor + uploaded_factor;
        
        UnchokerUtil.updateLargestValueFirstSort( rank_factor, ranks, peer, peers_ordered_by_rank, 0 );
      }

      //make space for new optimistic unchokes
      while( peers_ordered_by_rank.size() > max_to_unchoke - max_optimistic ) {
        peers_ordered_by_rank.remove( peers_ordered_by_rank.size() - 1 );
      }

      //update choke list with drops and unchoke list with optimistic unchokes
      ArrayList to_unchoke = new ArrayList();
      for( Iterator it = unchokes.iterator(); it.hasNext(); ) {
      	PEPeerTransport peer = (PEPeerTransport)it.next();
        
        peer.setOptimisticUnchoke( false ); 
        
        if( !peers_ordered_by_rank.contains( peer ) ) {  //should be choked
          //we assume that any/all chokes are to be replace by optimistics
        	PEPeerTransport optimistic_peer = UnchokerUtil.getNextOptimisticPeer( all_peers, false, false );
          
          if( optimistic_peer != null ) {  //only choke if we've got a peer to replace it with
            chokes.add( peer );
            it.remove();
            
            to_unchoke.add( optimistic_peer );
            optimistic_peer.setOptimisticUnchoke( true );
          }
        } 
      }
      
      for( int i=0; i < to_unchoke.size(); i++ ) {
        unchokes.add( to_unchoke.get( i ) );
      }
      
    }

  }
  
  
  public ArrayList getChokes() {
    ArrayList to_choke = chokes;
    chokes = new ArrayList();
    return to_choke;
  }
  
  
  public ArrayList getUnchokes() {
    ArrayList to_unchoke = unchokes;
    unchokes  = new ArrayList();
    return to_unchoke;
  }
  
}
