/**
* Created on Apr 17, 2007
* Created by Alan Snyder
* Copyright (C) 2007 Aelitis, All Rights Reserved.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*
* AELITIS, SAS au capital de 63.529,40 euros
* 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
*
*/


package com.aelitis.azureus.core.networkmanager.admin.impl;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.networkmanager.admin.*;


public class NetworkAdminSpeedTestSchedulerImpl
        implements NetworkAdminSpeedTestScheduler
{
    private static NetworkAdminSpeedTestSchedulerImpl instance = null;
    private NetworkAdminSpeedTestScheduledTestImpl currentTest = null;

     public static synchronized NetworkAdminSpeedTestScheduler getInstance(){
        if(instance==null){
            instance = new NetworkAdminSpeedTestSchedulerImpl();
        }
        return instance;
    }

    private NetworkAdminSpeedTestSchedulerImpl(){
     }

    public void
    initialise()
    {
    	NetworkAdminSpeedTesterBTImpl.startUp();
    }
    
    public synchronized NetworkAdminSpeedTestScheduledTest
    getCurrentTest()
    {
    	return( currentTest );
    }
    
    public synchronized NetworkAdminSpeedTestScheduledTest 
    scheduleTest(int type)
    	throws NetworkAdminException
    {
    	if ( currentTest != null ){

    		throw( new NetworkAdminException( "Test already scheduled" ));
    	}

    	if ( type == TEST_TYPE_BT ){

        PluginInterface plugin = PluginInitializer.getDefaultInterface(); 

    		currentTest = new NetworkAdminSpeedTestScheduledTestImpl(plugin, new NetworkAdminSpeedTesterBTImpl(plugin) );
            currentTest.getTester().setMode(type);

            currentTest.addListener(
    			new NetworkAdminSpeedTestScheduledTestListener()
    			{
    				public void stage(NetworkAdminSpeedTestScheduledTest test, String step){}

    				public void 
    				complete(NetworkAdminSpeedTestScheduledTest test )
    				{
    					synchronized( NetworkAdminSpeedTestSchedulerImpl.this ){
    						
    						currentTest = null;
    					}
    				}
  				
    			});
    	}else{

    		throw( new NetworkAdminException( "Unknown test type" ));
    	}

    	return( currentTest );
    }
    
    /**
     * Get the most recent result for the test.
     *
     * @return - Result
     */
    public NetworkAdminSpeedTesterResult getLastResult(int type) {
    	
        if ( type == TEST_TYPE_BT ){
        	
        	return( NetworkAdminSpeedTesterBTImpl.getLastResult());
        	
        }else{
        	
        	Debug.out( "Unknown test type" );
        	
        	return( null );
        }
    }


}
