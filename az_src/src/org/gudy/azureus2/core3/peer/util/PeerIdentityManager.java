/*
 * Created on Mar 18, 2004
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
package org.gudy.azureus2.core3.peer.util;


import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;


/**
 * Maintains peer identity information.
 */
public class PeerIdentityManager {

  private static final boolean MUTLI_CONTROLLERS	= COConfigurationManager.getBooleanParameter( "peer.multiple.controllers.per.torrent.enable", false );

  private static final AEMonitor 		class_mon	= new AEMonitor( "PeerIdentityManager:class");

  private static final Map 				dataMap = new LightHashMap();

  private static int totalIDs = 0;
 
  /*
  static{
	  new AEThread("mon",true)
	  {
		  public void
		  runSupport()
		  {
			  monitor();
		  }
	  }.start();
  }
    
  static void
  monitor()
  {
	  while( true ){
		  
		  try{
		  
			  class_mon.enter();
			  
			  System.out.println( "tot = " + getTotalIdentityCount());
			  
			  Iterator it = dataMap.entrySet().iterator();
			  
			  while( it.hasNext()){
				  
				  Map.Entry	entry = (Map.Entry)it.next();
				  
				  PeerIdentityDataID id = (PeerIdentityDataID)entry.getKey();
				  
				  Map	vals = (Map)entry.getValue();
				  
				  System.out.println( "  id " + ByteFormatter.encodeString( id.getDataID())+ " -> " + vals.size());
			  }
		  }finally{
			  
			  class_mon.exit();
		  }
		  try{
			  Thread.sleep(10000);
		  }catch( Throwable e ){
			  
		  }
	  }
  }
  */
  
  public static PeerIdentityDataID
  createDataID(
  	byte[]		data )
  {
  	PeerIdentityDataID	data_id = new PeerIdentityDataID( data );
  	
  	Map peerMap;
  	
    try{
        class_mon.enter();
      
        peerMap = (Map)dataMap.get( data_id );
        
        if( peerMap == null ){
        	
          peerMap = new LightHashMap();
          
          dataMap.put( data_id, peerMap );
        }
    }finally{
    	
    	class_mon.exit();
    }
	
	data_id.setPeerMap( peerMap );
	
	return( data_id );
  }
  
  //Main peer identity container.
  //Add new identity items (like pgp key, authentication user/pass, etc)
  //to this class if/when needed.
  private static class PeerIdentity {
    private final byte[] id;
    private final short	port;
    private final int hashcode;
    
    private PeerIdentity( byte[] _id, int local_port ) {
      this.id = _id;
      port = (short)local_port;
      this.hashcode = new String( id ).hashCode();
    }
    
    public boolean equals( Object obj ) {
      if (this == obj)  return true;
      if (obj != null && obj instanceof PeerIdentity) {
        PeerIdentity other = (PeerIdentity)obj;
        if ( MUTLI_CONTROLLERS ){
        	if ( port != other.port ){
        		return( false );
        	}
        }
        return Arrays.equals(this.id, other.id);
      }
      return false;
    }
    
    public int hashCode() {
      return hashcode;
    }
    
    protected String
    getString()
    {
    	return( ByteFormatter.encodeString( id ));
    }
  }
  
  
  /**
   * Add a new peer identity to the manager.
   * @param data_id unique id for the data item associated with this connection
   * @param peer_id unique id for this peer connection
   * @param ip remote peer's ip address
   */
  public static boolean 
  addIdentity( PeerIdentityDataID data_id, byte[] peer_id, int local_port, String ip ) {
     PeerIdentity peerID = new PeerIdentity( peer_id, local_port );
    
    try{
      class_mon.enter();
        
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap == null ) {
        peerMap = new HashMap();
        dataMap.put( data_id, peerMap );
      }
           
      String old = (String)peerMap.put( peerID, ip );
      if( old == null ) {
        totalIDs++;
        
        return( true );
      }else{    	
    	return( false );
      }
    }finally{
      class_mon.exit();
    }
  }
  
  
  /**
   * Remove a peer identity from the manager.
   * @param data_id id for the data item associated with this connection
   * @param peer_id id for this peer connection
   */
  public static void removeIdentity( PeerIdentityDataID data_id, byte[] peer_id, int local_port  ) {
     
    try{
    	class_mon.enter();
      
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap != null ) {
        PeerIdentity peerID = new PeerIdentity( peer_id, local_port );
               
        String old = (String)peerMap.remove( peerID );
        if( old != null ) {
          totalIDs--;
        }else{
      	  Debug.out( "id not present: id=" + peerID.getString());
   	
        }
      }
    }finally{
    	class_mon.exit();
    }
  }
  

  /**
   * Check if the manager already has the given peer identity.
   * @param data_id id for the data item associated with this connection
   * @param peer_id id for this peer connection
   * @return true if the peer identity is found, false if not found
   */
  public static boolean containsIdentity( PeerIdentityDataID data_id, byte[] peer_id, int local_port ) {
    PeerIdentity peerID = new PeerIdentity( peer_id, local_port );
    
    try{
    	class_mon.enter();
  
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap != null ) {
        if( peerMap.containsKey( peerID ) ) {
          return true;
        }
      }
    }finally{
    	class_mon.exit();
    }
    
    return false;
  }
  
  
  /**
   * Get the total number of peer identities managed.
   * @return total number of peers over all data items
   */
  public static int getTotalIdentityCount() {
    return totalIDs;
  }
  
  
  /**
   * Get the total number of peer identities managed for the given data item.
   * @param data_id data item to count over
   * @return total number of peers for this data item
   */
  public static int 
  getIdentityCount( 
  	PeerIdentityDataID data_id )
  {
  	return( data_id.getPeerMap().size());
  }
  
  
  /**
   * Check if the given IP address is already present in the manager's
   * peer identity list for the given data item (i.e. check if there is
   * already a peer with that IP address).
   * @param data_id id for the data item associated with this connection
   * @param ip IP address to check for
   * @return true if the IP is found, false if not found
   */
  public static boolean containsIPAddress( PeerIdentityDataID data_id, String ip ) {
    
    try{
    	class_mon.enter();
   	  
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap != null ) {
        if( peerMap.containsValue( ip ) ) {
          return true;
        }
      }
    }finally{
    	class_mon.exit();
    }
    
    return false;
  }

}
