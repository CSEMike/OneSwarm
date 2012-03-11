/*
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
 */
package com.aelitis.azureus.core.peermanager.control.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.peermanager.control.PeerControlScheduler;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;

public abstract class 
PeerControlSchedulerImpl
	implements PeerControlScheduler, AzureusCoreStatsProvider, ParameterListener
{
	private static final PeerControlSchedulerImpl[]	singletons;
	
	static{
		int	num = COConfigurationManager.getIntParameter( "peercontrol.scheduler.parallelism", 1 );
		
		if ( num < 1 ){
			
			num = 1;
			
		}else if ( num > 1 ){
			
			if ( COConfigurationManager.getBooleanParameter( "peercontrol.scheduler.use.priorities" )){
		
				Debug.out( "Multiple peer schedulers not supported for prioritised scheduling" );
				
				num = 1;
				
			}else{
				
				System.out.println( "Peer control scheduler parallelism=" + num );
			}
		}
		
		singletons = new PeerControlSchedulerImpl[ num ];
	}
	
	protected boolean useWeights = true;
	
	{
		COConfigurationManager.addAndFireParameterListener("Use Request Limiting Priorities", this);
	}
	
	public void parameterChanged(String parameterName) {
		useWeights = COConfigurationManager.getBooleanParameter("Use Request Limiting Priorities");		
	}
	
	static{ 
		
		for (int i=0;i<singletons.length;i++){
	
			PeerControlSchedulerImpl singleton;
			
			if ( COConfigurationManager.getBooleanParameter( "peercontrol.scheduler.use.priorities" )){
			
				singleton = new PeerControlSchedulerPrioritised();
				
			}else{
				
				singleton = new PeerControlSchedulerBasic();
	
			}
			
			singletons[i] = singleton;
			
			singleton.start();
		}
	}
	
	public static PeerControlScheduler
	getSingleton(
		int		id )
	{
		return( singletons[ id%singletons.length ]);
	}
		
	public static void
	overrideAllWeightedPriorities(
		boolean	b )
	{
		for ( PeerControlSchedulerImpl s: singletons ){
			
			s.overrideWeightedPriorities(b);
		}
	}
	
	public static void
	updateAllScheduleOrdering()
	{
		for ( PeerControlSchedulerImpl s: singletons ){
			
			s.updateScheduleOrdering();
		}
	}
	
	protected long	schedule_count;
	protected long	wait_count;
	protected long	yield_count;
	protected long	total_wait_time;
		
	protected
	PeerControlSchedulerImpl()
	{
		Set	types = new HashSet();
		
		types.add( AzureusCoreStats.ST_PEER_CONTROL_SCHEDULE_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_LOOP_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_YIELD_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_WAIT_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_WAIT_TIME );

		AzureusCoreStats.registerProvider( types, this );
	}
	
	protected void
	start()
	{
		new AEThread2( "PeerControlScheduler", true )
		{
			public void
			run()
			{
				schedule();
			}
			
		}.start();
	}

	public void
	updateStats(
		Set		types,
		Map		values )
	{
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_SCHEDULE_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_SCHEDULE_COUNT, new Long( schedule_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_LOOP_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_LOOP_COUNT, new Long( wait_count + yield_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_YIELD_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_YIELD_COUNT, new Long( yield_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_WAIT_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_WAIT_COUNT, new Long( wait_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_WAIT_TIME )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_WAIT_TIME, new Long( total_wait_time ));
		}
	}
		
	protected abstract void
	schedule();
	
	public void overrideWeightedPriorities(boolean override) {
		if(override)
			useWeights = false;
		else
			parameterChanged(null);
	}
}
