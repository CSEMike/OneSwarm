/*
 * Created on Oct 16, 2004
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;



/**
 * Processes reads of read-entities and handles the read selector.
 */
public class ReadController implements AzureusCoreStatsProvider{
	
	private static int 		IDLE_SLEEP_TIME  	= 50;
	private static boolean	AGGRESIVE_READ		= false;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"network.control.read.idle.time",
				"network.control.read.aggressive",
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					IDLE_SLEEP_TIME 	= COConfigurationManager.getIntParameter( "network.control.read.idle.time" );
					AGGRESIVE_READ		= COConfigurationManager.getBooleanParameter( "network.control.read.aggressive" );
				}
			});
	}
	
  private volatile ArrayList normal_priority_entities = new ArrayList();  //copied-on-write
  private volatile ArrayList high_priority_entities = new ArrayList();  //copied-on-write
  private final AEMonitor entities_mon = new AEMonitor( "ReadController:EM" );
  private int next_normal_position = 0;
  private int next_high_position = 0;
    
  private long	loop_count;
  private long	wait_count;
  private long	non_progress_count;
  private long	progress_count;

  private long	entity_check_count;
  private long	last_entity_check_count;
  
  private EventWaiter 	read_waiter = new EventWaiter();

  private int			entity_count;
  
  public ReadController() {
     
    //start read handler processing
    Thread read_processor_thread = new AEThread( "ReadController:ReadProcessor" ) {
      public void runSupport() {
        readProcessorLoop();
      }
    };
    read_processor_thread.setDaemon( true );
    read_processor_thread.setPriority( Thread.MAX_PRIORITY - 1 );
    read_processor_thread.start();
    
    Set	types = new HashSet();
    
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_LOOP_COUNT );
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_NP_COUNT );
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_P_COUNT );
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_WAIT_COUNT );
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_ENTITY_COUNT );
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_CON_COUNT );
    types.add( AzureusCoreStats.ST_NET_READ_CONTROL_READY_CON_COUNT );
    
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
				writer.println( "Read Controller" );
					
				try{
					writer.indent();
					
					ArrayList ref = normal_priority_entities;

					writer.println( "normal - " + ref.size());
					    
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
	  if ( types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_LOOP_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_LOOP_COUNT, new Long( loop_count  ));
	  }
	  
	  if ( types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_NP_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_NP_COUNT, new Long( non_progress_count  ));
	  }
	  
	  if ( types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_P_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_P_COUNT, new Long( progress_count  ));
	  }
	  
	  if ( types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_WAIT_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_WAIT_COUNT, new Long( wait_count  ));
	  }
	  
	  if ( types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_ENTITY_COUNT )){

		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_ENTITY_COUNT, new Long( high_priority_entities.size() + normal_priority_entities.size()));
	  }
	 
	  if ( 	types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_CON_COUNT ) ||
			types.contains( AzureusCoreStats.ST_NET_READ_CONTROL_READY_CON_COUNT )){
		   		    
		  int	ready_connections	= 0;
		  int	connections			= 0;
		  
		  ArrayList[] refs = { normal_priority_entities, high_priority_entities };
		    
		  for (int i=0;i<refs.length;i++){
			  
			  ArrayList	ref = refs[i];
			 
			  for (int j=0;j<ref.size();j++){
		  
			      RateControlledEntity entity = (RateControlledEntity)ref.get( j );
			      
			      connections 		+= entity.getConnectionCount();
			      
			      ready_connections += entity.getReadyConnectionCount( read_waiter );
			  }
		  }
		  
		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_CON_COUNT, new Long( connections ));
		  values.put( AzureusCoreStats.ST_NET_READ_CONTROL_READY_CON_COUNT, new Long( ready_connections ));
	  }
  }

  
  
  
  private void readProcessorLoop() {
    boolean check_high_first = true;
    
    while( true ) {
      loop_count++;
      try {
        if( check_high_first ) {
          check_high_first = false;
          if( !doHighPriorityRead() ) {
            if( !doNormalPriorityRead() ) {
            	if ( read_waiter.waitForEvent( IDLE_SLEEP_TIME )){
            		wait_count++;
            	}
            }
          }
        }
        else {
          check_high_first = true;
          if( !doNormalPriorityRead() ) {
            if( !doHighPriorityRead() ) {
            	if ( read_waiter.waitForEvent( IDLE_SLEEP_TIME )){
            		wait_count++;
            	}
            }
          }
        }
      }
      catch( Throwable t ) {
        Debug.out( "readProcessorLoop() EXCEPTION: ", t );
      }
    }
  }
  
  
  private boolean 
  doNormalPriorityRead() 
  {
	  return( doRead( getNextReadyNormalPriorityEntity()));
  }
  
  private boolean 
  doHighPriorityRead() 
  {
	  return( doRead( getNextReadyHighPriorityEntity()));
  }
  
  private boolean
  doRead(
	  RateControlledEntity	  ready_entity )
  {  
	  if ( ready_entity != null ){
		 
		  if ( AGGRESIVE_READ ){
			  
			  	// skip over failed readers to find a good one
			  
			  if ( ready_entity.doProcessing( read_waiter, 0 ) > 0 ) {
	
				  progress_count++;
	
				  return( true );
	
			  }else{
				  
				  non_progress_count++;
	
				  if ( entity_check_count - last_entity_check_count >= normal_priority_entities.size() + high_priority_entities.size() ){
	
					  last_entity_check_count	= entity_check_count;
					  
					  	// force a wait
					  
					  if ( read_waiter.waitForEvent( IDLE_SLEEP_TIME )){
		            		wait_count++;
					  }
					  
					  return( false);
				  }
	
				  return( true );
			  }
		  }else{
			  
			  return( ready_entity.doProcessing( read_waiter, 0 ) > 0 );
		  }
	  }
	  
	  return false;
  }
  
  
  private RateControlledEntity getNextReadyNormalPriorityEntity() {
    ArrayList ref = normal_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      entity_check_count++;
      next_normal_position = next_normal_position >= size ? 0 : next_normal_position;  //make circular
      RateControlledEntity entity = (RateControlledEntity)ref.get( next_normal_position );
      next_normal_position++;
      num_checked++;
      if( entity.canProcess( read_waiter ) ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  private RateControlledEntity getNextReadyHighPriorityEntity() {
    ArrayList ref = high_priority_entities;
    
    int size = ref.size();
    int num_checked = 0;

    while( num_checked < size ) {
      entity_check_count++;
      next_high_position = next_high_position >= size ? 0 : next_high_position;  //make circular
      RateControlledEntity entity = (RateControlledEntity)ref.get( next_high_position );
      next_high_position++;
      num_checked++;
      if( entity.canProcess( read_waiter ) ) {  //is ready
        return entity;
      }
    }

    return null;  //none found ready
  }
  
  
  
  /**
   * Add the given entity to the controller for read processing.
   * @param entity to process reads for
   */
  public void addReadEntity( RateControlledEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledEntity.PRIORITY_HIGH ) {
        //copy-on-write
        ArrayList high_new = new ArrayList( high_priority_entities.size() + 1 );
        high_new.addAll( high_priority_entities );
        high_new.add( entity );
        high_priority_entities = high_new;
      }
      else {
        //copy-on-write
        ArrayList norm_new = new ArrayList( normal_priority_entities.size() + 1 );
        norm_new.addAll( normal_priority_entities );
        norm_new.add( entity );
        normal_priority_entities = norm_new;
      }
      
      entity_count = normal_priority_entities.size() + high_priority_entities.size();
    }
    finally {  entities_mon.exit();  }
  }
  
  
  /**
   * Remove the given entity from the controller.
   * @param entity to remove from read processing
   */
  public void removeReadEntity( RateControlledEntity entity ) {
    try {  entities_mon.enter();
      if( entity.getPriority() == RateControlledEntity.PRIORITY_HIGH ) {
        //copy-on-write
        ArrayList high_new = new ArrayList( high_priority_entities );
        high_new.remove( entity );
        high_priority_entities = high_new;
      }
      else {
        //copy-on-write
        ArrayList norm_new = new ArrayList( normal_priority_entities );
        norm_new.remove( entity );
        normal_priority_entities = norm_new;
      }
      
      entity_count = normal_priority_entities.size() + high_priority_entities.size();
    }
    finally {  entities_mon.exit();  }
  }
  
  public int
  getEntityCount()
  {
	  return( entity_count );
  }
}
