/*
 * Created on 19-Dec-2005
 * Created by Paul Gardner
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

package org.gudy.azureus2.core3.disk.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.RealTimeInfo;

public class 
DiskManagerRecheckScheduler 
{
	private static boolean 	friendly_hashing;
	private static boolean 	smallest_first;

    static{
    	
    	 ParameterListener param_listener = new ParameterListener() {
    	    public void 
			parameterChanged( 
				String  str ) 
    	    {
    	   	      friendly_hashing 	= COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );
    	   	      smallest_first	= COConfigurationManager.getBooleanParameter( "diskmanager.hashchecking.smallestfirst" ); 
    	    }
    	 };

 		COConfigurationManager.addAndFireParameterListeners(
 				new String[]{
 					"diskmanager.friendly.hashchecking",
 					"diskmanager.hashchecking.smallestfirst" },
 				param_listener );
    }
    
	private List		instances		= new ArrayList();
	private AEMonitor	instance_mon	= new AEMonitor( "DiskManagerRecheckScheduler" );
	
	
	public DiskManagerRecheckInstance
	register(
		DiskManagerHelper	helper,
		boolean				low_priority )
	{
		try{
			instance_mon.enter();
			
			DiskManagerRecheckInstance	res = 
				new DiskManagerRecheckInstance( 
						this, 
						helper.getTorrent().getSize(),
						(int)helper.getTorrent().getPieceLength(),
						low_priority );
			
			instances.add( res );
			
			if ( smallest_first ){
				
				Collections.sort(
						instances,
						new Comparator()
						{
							public int
							compare(
								Object	o1,
								Object	o2 )
							{
								long	comp = ((DiskManagerRecheckInstance)o1).getMetric() - ((DiskManagerRecheckInstance)o2).getMetric();
								
								if ( comp < 0 ){
									
									return( -1 );
									
								}else if ( comp == 0 ){
									
									return( 0 );
									
								}else{
									return( 1 );
								}
							}
						});
			}
			
			return( res );
			
		}finally{
			
			instance_mon.exit();
		}
	}
	
	protected boolean
	getPermission(
		DiskManagerRecheckInstance	instance )
	{
		boolean	result 	= false;
		int		delay	= 250;
		
		try{
			instance_mon.enter();

			if ( instances.get(0) == instance ){
					    
				boolean	low_priority = instance.isLowPriority();
				
					// defer low priority activities if we are running a real-time task
				
				if ( low_priority && RealTimeInfo.isRealTimeTaskActive()){
					
					result = false;
					
				}else{
					
		            if ( friendly_hashing ){
		            	
		            	delay	= 0;	// delay introduced elsewhere
		            	
		            }else if ( !low_priority ){
		            	
		            	delay	= 1;	// high priority recheck, just a smidge of a delay
		            	
		            }else{
		            	
			            	//delay a bit normally anyway, as we don't want to kill the user's system
			            	//during the post-completion check (10k of piece = 1ms of sleep)
		            	
		            	delay = instance.getPieceLength() /1024 /10;
		              
		            	delay = Math.min( delay, 409 );
		              
		            	delay = Math.max( delay, 12 );
	  				}
					
		            result	= true;
				}
			}
		}finally{
			
			instance_mon.exit();
		}
		
		if ( delay > 0 ){
			
			try{
				Thread.sleep( delay );
				
			}catch( Throwable e ){
				
			}
		}
		
		return( result );
	}
	
	protected void
	unregister(
		DiskManagerRecheckInstance	instance )
	{
		try{
			instance_mon.enter();
			
			instances.remove( instance );
		}finally{
			
			instance_mon.exit();
		}	
	}
}
