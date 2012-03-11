/*
 * Created on Sep 27, 2004
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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;


/**
 * Processes writes of write-entities and handles the write selector.
 */
public class WriteController implements AzureusCoreStatsProvider{
  
	private static int 		IDLE_SLEEP_TIME  	= 50;
	private static boolean	AGGRESIVE_WRITE		= false;
	private static int		BOOSTER_GIFT 		= 5*1024;

	static{
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"network.control.write.idle.time",
				"network.control.write.aggressive",
				"Bias Upload Slack KBs",
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					IDLE_SLEEP_TIME 	= COConfigurationManager.getIntParameter( "network.control.write.idle.time" );
					AGGRESIVE_WRITE		= COConfigurationManager.getBooleanParameter( "network.control.write.aggressive" );
					BOOSTER_GIFT 		= COConfigurationManager.getIntParameter( "Bias Upload Slack KBs" )*1024;
				}
			});
	}
	
  private volatile ArrayList<RateControlledEntity> normal_priority_entities = new ArrayList<RateControlledEntity>();  //copied-on-write
  private volatile ArrayList<RateControlledEntity> boosted_priority_entities = new ArrayList<RateControlledEntity>();  //copied-on-write
  private volatile ArrayList<RateControlledEntity> high_priority_entities = new ArrayList<RateControlledEntity>();  //copied-on-write
  private final AEMonitor entities_mon = new AEMonitor( "WriteController:EM" );
  private int next_normal_position = 0;
  private int next_boost_position = 0;
  private int next_high_position = 0;
    

  private long	booster_process_time;;
  private int	booster_normal_written;
  private int	booster_stat_index;
  private int[]	booster_normal_writes 	= new int[5];
  private int[]	booster_gifts 			= new int[5];
  
  private int aggressive_np_normal_priority_count;
  private int aggressive_np_high_priority_count;
  
  private long	process_loop_time;
  private long	wait_count;
  private long	progress_count;
  private long	non_progress_count;
  
  private EventWaiter 	write_waiter = new EventWaiter();
  
  private NetworkManager	net_man;
  
  private int	entity_count = 0;
  
  /**
   * Create a new write controller.
   */
  public WriteController() {
    
    //start write handler processing
    Thread write_processor_thread = new AEThread( "WriteController:WriteProcessor" ) {
      public void runSupport() {
        writeProcessorLoop();
      }
    };
    write_processor_thread.setDaemon( true );
    write_processor_thread.setPriority( Thread.MAX_PRIORITY - 1 );
    write_processor_thread.start();
    
    Set	types = new HashSet();
    
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_WAIT_COUNT );
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_NP_COUNT );
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_P_COUNT );
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_ENTITY_COUNT );
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_CON_COUNT );
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_READY_CON_COUNT );
    types.add( AzureusCoreStats.ST_NET_WRITE_CONTROL_READY_BYTE_COUNT );
       
    AzureusCoreStats.registerProvider(
    	types,
    	this );
    
    AEDiagnostics.addEvidenceGenerator(
    	new AEDiagnosticsEvidenceGenerator()
    	{
    		public void 
    		generate(
    			IndentWriter writer ) 
    		{
				writer.println( "Write Controller" );
					
				try{
					writer.indent();
					
					ArrayList ref = normal_priority_entities;

					writer.println( "normal - " + ref.size());
					    
					for (int i=0;i<ref.size();i++){
						
						RateControlledEntity entity = (RateControlledEntity)ref.get( i );

						writer.println( entity.getString());
					}
					
					ref = boosted_priority_entities;

					writer.println( "boosted - " + ref.size());
					    
					for (int i=0;i<ref.size();i++){
						
						RateControlledEntity entity = (RateControlledEntity)ref.get( i );

						writer.println( entity.getString());
					}
					
					ref = high_priority_entities;

					writer.println( "priority - " + ref.size());
					    
					for (int i=0;i<ref.size();i++){
						
						RateControlledEntity entity = (RateControlledEntity)ref.get( i );

						writer.println( entity.getString());
					}
				}finally{
					
					writer.exdent();
				}
    		}
    	});
  }
  
  public void
  updateStats(
		  Set		types,
		  Map		values )
  {
	  if ( types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_WAIT_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_WAIT_COUNT, new Long( wait_count ));
	  }
	  
	  if ( types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_NP_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_NP_COUNT, new Long( non_progress_count ));
	  }
	  
	  if ( types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_P_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_P_COUNT, new Long( progress_count ));
	  }

	  if ( types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_ENTITY_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_ENTITY_COUNT, new Long( high_priority_entities.size() + boosted_priority_entities.size() + normal_priority_entities.size()));
	  }
	  
	  if ( 	types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_CON_COUNT ) ||
			types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_READY_CON_COUNT ) ||
			types.contains( AzureusCoreStats.ST_NET_WRITE_CONTROL_READY_BYTE_COUNT )){
			   
		  long	ready_bytes			= 0;
		  int	ready_connections	= 0;
		  int	connections			= 0;
		  
		  ArrayList[] refs = { normal_priority_entities, boosted_priority_entities, high_priority_entities };
		    
		  for (int i=0;i<refs.length;i++){
			  
			  ArrayList	ref = refs[i];
			 
			  for (int j=0;j<ref.size();j++){
				  
			      RateControlledEntity entity = (RateControlledEntity)ref.get( j );
			      
			      connections 		+= entity.getConnectionCount();
			      
			      ready_connections += entity.getReadyConnectionCount( write_waiter );
			      
			      ready_bytes		+= entity.getBytesReadyToWrite();
			  }
		  }
		  
		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_CON_COUNT, new Long( connections ));
		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_READY_CON_COUNT, new Long( ready_connections ));
		  values.put( AzureusCoreStats.ST_NET_WRITE_CONTROL_READY_BYTE_COUNT, new Long( ready_bytes ));
	  }
  }
  
  private void writeProcessorLoop() {
    boolean check_high_first = true;
    
    long	last_check = SystemTime.getMonotonousTime();
    
    net_man = NetworkManager.getSingleton();

    while( true ) {
      
      process_loop_time = SystemTime.getMonotonousTime();

      try {
        if( check_high_first ) {
          check_high_first = false;
          if( !doHighPriorityWrite() ) {
            if( !doNormalPriorityWrite() ) {
              if ( write_waiter.waitForEvent( IDLE_SLEEP_TIME )){
            	  wait_count++;
              }
            }
          }
        }
        else {
          check_high_first = true;
          if( !doNormalPriorityWrite() ) {
            if( !doHighPriorityWrite() ) {
            	if ( write_waiter.waitForEvent( IDLE_SLEEP_TIME )){
            		wait_count++;
            	}
            }
          }
        }
      }
      catch( Throwable t ) {
        Debug.out( "writeProcessorLoop() EXCEPTION: ", t );
      }
            
      if ( process_loop_time - last_check > 5000 ){
    	  
    	  last_check = process_loop_time;
    	  
    	  boolean	changed = false;
    	  
    	  ArrayList<RateControlledEntity> ref = normal_priority_entities;
    	  
    	  for ( RateControlledEntity e: ref ){
    		  
    		 if ( e.getPriorityBoost()){
    			  
    			 changed = true;
    			  
    			 break;
    		 }
    	  }
    	  
    	  if ( !changed ){
    		  
    	  	 ref = boosted_priority_entities;
        	  
        	 for ( RateControlledEntity e: ref ){
        		  
        		 if ( !e.getPriorityBoost()){
        			  
        			 changed = true;
        			  
        			 break;
        		 }
        	  }
    	  }
    	  
    	  if ( changed ){
    		 
 		    try{  
 		    	entities_mon.enter();
 		    	
 		    	ArrayList<RateControlledEntity> new_normal 	= new ArrayList<RateControlledEntity>();
 		    	ArrayList<RateControlledEntity> new_boosted = new ArrayList<RateControlledEntity>();
 		    	
 		    	for ( RateControlledEntity e: normal_priority_entities ){
 		    		
 		    		if ( e.getPriorityBoost()){
 		    			
 		    			new_boosted.add( e );
 		    			
 		    		}else{
 		    			
 		    			new_normal.add( e );
 		    		}
 		    	}
 		    	
		    	for ( RateControlledEntity e: boosted_priority_entities ){
 		    		
 		    		if ( e.getPriorityBoost()){
 		    			
 		    			new_boosted.add( e );
 		    			
 		    		}else{
 		    			
 		    			new_normal.add( e );
 		    		}
 		    	}
		    	
		    	normal_priority_entities 	= new_normal;
		    	boosted_priority_entities	= new_boosted;
		    	
 		    }finally{
 		    	
 		    	entities_mon.exit();
 		    }
    	  }
      }
    }
  }
  
  
  private boolean 
  doNormalPriorityWrite() 
  {
    int result = processNextReadyNormalPriorityEntity();
    
    if ( result > 0 ){
    	   
    	progress_count++;
    		
    	return true;
    	
    }else if ( result == 0 ){
    		
    	non_progress_count++;
			
    	if ( AGGRESIVE_WRITE ){
    			
    		aggressive_np_normal_priority_count++;
    			     			
    		if ( aggressive_np_normal_priority_count < ( normal_priority_entities.size() + boosted_priority_entities.size())){
    				
    			return( true );
    				
    		}else{
    				
    			aggressive_np_normal_priority_count = 0;
    		}
    	}
    }
    
    return false;
  }
  
  private boolean doHighPriorityWrite() {
    RateControlledEntity ready_entity = getNextReadyHighPriorityEntity();
    if( ready_entity != null ){
    	if ( ready_entity.doProcessing( write_waiter, 0 ) > 0 ) {
    
    		progress_count++;
    		
    		return true;
    		
    	}else{
    		
    		non_progress_count++;
    		
    		if ( AGGRESIVE_WRITE ){
    			
    			aggressive_np_high_priority_count++;
    			
    			if ( aggressive_np_high_priority_count < high_priority_entities.size()){
    				
    				return( true );
    				
    			}else{
    				
    				aggressive_np_high_priority_count = 0;
    			}
    		}
    	}
    }
    return false;
  }
  
  
  private int 
  processNextReadyNormalPriorityEntity() 
  {
	  ArrayList<RateControlledEntity> boosted_ref = boosted_priority_entities;

	  final int boosted_size = boosted_ref.size();
	
	  try{
		  if ( boosted_size > 0 ){
			  			  
			  if ( process_loop_time - booster_process_time >= 1000 ){
			
				  booster_process_time = process_loop_time;
				  
				  booster_gifts[ booster_stat_index ] 			= BOOSTER_GIFT;
				  booster_normal_writes[ booster_stat_index]	= booster_normal_written;
				  
				  booster_stat_index++;
				  
				  if ( booster_stat_index >= booster_gifts.length ){
					  
					  booster_stat_index = 0;
				  }
				  
				  booster_normal_written = 0;
			  }
			  
			  int	total_gifts 		= 0;
			  int	total_normal_writes	= 0;
				  
			  for (int i=0;i<booster_gifts.length;i++){
					  
				  total_gifts			+= booster_gifts[i];
				  total_normal_writes 	+= booster_normal_writes[i];
			  }
				  
			  int	effective_gift = total_gifts - total_normal_writes;
			  
			  if ( effective_gift > 0 ){
				  
				  ArrayList<RateControlledEntity> normal_ref = normal_priority_entities;
	
				  int normal_size = normal_ref.size();
				  
				  int num_checked = 0;
				  
				  int position = next_normal_position;
				  
				  List<RateControlledEntity> ready = new ArrayList<RateControlledEntity>();
				  
				  while( num_checked < normal_size ) {
					  position = position >= normal_size ? 0 : position; 
					  RateControlledEntity entity = normal_ref.get( position );
					  position++;
					  num_checked++;
					  if( entity.canProcess( write_waiter )) {
						 next_normal_position = position;
						 ready.add( entity );
					  }
				  }
				  	
				  int	num_ready = ready.size();
				  
				  if ( num_ready > 0 ){
					  		
					  int	gift_used = 0;
					  
					  for ( RateControlledEntity r: ready ){
						  
						  int	permitted = effective_gift / num_ready;
						  
						  if ( permitted <= 0 ){
							  
							  permitted = 1;
						  }
						  			  
						  if ( r.canProcess( write_waiter )){
							  
							  int	done = r.doProcessing( write_waiter, permitted );
							  
							  if ( done > 0 ){
							  
								  booster_normal_written += done;
								  
								  gift_used += done;
							  }
						  }
						  
						  num_ready--;
					  }
						  
					  for ( int i=booster_stat_index; gift_used > 0 && i<booster_stat_index+booster_gifts.length; i++){
						  
						  int	avail = booster_gifts[i%booster_gifts.length];
						  
						  if ( avail > 0 ){
							  
							  int	temp = Math.min( avail, gift_used );
							  
							  avail 	-= temp;
							  gift_used -= temp;
							  
							  booster_gifts[i%booster_gifts.length] = avail;
						  }
					  }
				  }
			  }
			  
			  int num_checked = 0;
		
			  while( num_checked < boosted_size ) {
				  next_boost_position = next_boost_position >= boosted_size ? 0 : next_boost_position;  //make circular
				  RateControlledEntity entity = boosted_ref.get( next_boost_position );
				  next_boost_position++;
				  num_checked++;
				  if( entity.canProcess( write_waiter ) ) {  //is ready
					  return( entity.doProcessing( write_waiter, 0 ));
				  }
			  }
			  
			  	// give remaining normal peers a chance to use the bandwidth boosted peers couldn't, but prevent
			  	// more from being allocated while doing so to prevent them from grabbing more than they should
			  
			  net_man.getUploadProcessor().setRateLimiterFreezeState( true );

		  }else{
			  
			  booster_normal_written = 0;
		  }
		  	  
		  ArrayList<RateControlledEntity> normal_ref = normal_priority_entities;
	
		  int normal_size = normal_ref.size();
		  
		  int num_checked = 0;
	
		  while( num_checked < normal_size ) {
			  next_normal_position = next_normal_position >= normal_size ? 0 : next_normal_position;  //make circular
			  RateControlledEntity entity = (RateControlledEntity)normal_ref.get( next_normal_position );
			  next_normal_position++;
			  num_checked++;
			  if( entity.canProcess( write_waiter ) ) {  //is ready
				  int bytes = entity.doProcessing( write_waiter, 0 );
				  
				  if ( bytes > 0 ){
										  
					  booster_normal_written += bytes;
				  }
				  
				  return( bytes );
			  }
		  }
	
		  return( -1 );
		  
	  }finally{
		  
		  if ( boosted_size > 0 ){
			  
			  net_man.getUploadProcessor().setRateLimiterFreezeState( false );
		  }
	  }
  }
  
  
  private RateControlledEntity getNextReadyHighPriorityEntity() {
    ArrayList ref = high_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      next_high_position = next_high_position >= size ? 0 : next_high_position;  //make circular
      RateControlledEntity entity = (RateControlledEntity)ref.get( next_high_position );
      next_high_position++;
      num_checked++;
      if( entity.canProcess( write_waiter ) ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  
  /**
   * Add the given entity to the controller for write processing.
   * @param entity to process writes for
   */
  public void addWriteEntity( RateControlledEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledEntity.PRIORITY_HIGH ) {
        //copy-on-write
        ArrayList high_new = new ArrayList( high_priority_entities.size() + 1 );
        high_new.addAll( high_priority_entities );
        high_new.add( entity );
        high_priority_entities = high_new;
      }
      else {
        if ( entity.getPriorityBoost()){
	        ArrayList boost_new = new ArrayList( boosted_priority_entities.size() + 1 );
	        boost_new.addAll( boosted_priority_entities );
	        boost_new.add( entity );
	        boosted_priority_entities = boost_new;
        }else{
	        ArrayList norm_new = new ArrayList( normal_priority_entities.size() + 1 );
	        norm_new.addAll( normal_priority_entities );
	        norm_new.add( entity );
	        normal_priority_entities = norm_new;
        }
      }
      
      entity_count = normal_priority_entities.size() + boosted_priority_entities.size() + high_priority_entities.size();
    }
    finally {  entities_mon.exit();  }
  }
  
  
  /**
   * Remove the given entity from the controller.
   * @param entity to remove from write processing
   */
  public void removeWriteEntity( RateControlledEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledEntity.PRIORITY_HIGH ) {
        //copy-on-write
        ArrayList high_new = new ArrayList( high_priority_entities );
        high_new.remove( entity );
        high_priority_entities = high_new;
      }
      else {
        //copy-on-write
    	if ( boosted_priority_entities.contains( entity )){
	        ArrayList boosted_new = new ArrayList( boosted_priority_entities );
	        boosted_new.remove( entity );
	        boosted_priority_entities = boosted_new;
    	}else{
	        ArrayList norm_new = new ArrayList( normal_priority_entities );
	        norm_new.remove( entity );
	        normal_priority_entities = norm_new;
    	}
      }
      
      entity_count = normal_priority_entities.size() + boosted_priority_entities.size() + high_priority_entities.size();
    }
    finally {  entities_mon.exit();  }
  }
  
  public int
  getEntityCount()
  {
	  return( entity_count );
  }
}
