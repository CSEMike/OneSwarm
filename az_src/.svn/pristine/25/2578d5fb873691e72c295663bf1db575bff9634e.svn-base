/*
 * File    : BadIpsImpl.java
 * Created : 10 nov. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.ipfilter.impl;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.ipfilter.BadIp;
import org.gudy.azureus2.core3.ipfilter.BadIps;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 *
 */
public class BadIpsImpl implements BadIps {
  
  private static BadIps 	instance; 
  private static AEMonitor	class_mon	= new AEMonitor( "BadIps:class" );
  
  private Map 		bad_ip_map;
  private AEMonitor	bad_ip_map_mon		= new AEMonitor( "BadIps:Map");
  
  public static BadIps 
  getInstance() 
  {
  	try{
  		class_mon.enter();
  	
  		if( instance == null ){
    
  			instance = new BadIpsImpl();
  		}
    
  		return( instance );
  		
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public BadIpsImpl() 
  {
    bad_ip_map = new HashMap();
  }
  
  public int 
  addWarningForIp(
  	String ip ) 
  {
    try{
    	bad_ip_map_mon.enter(); 
    
    	BadIpImpl	bad_ip = (BadIpImpl)bad_ip_map.get( ip );
    	
    	if ( bad_ip == null ){
    		
    		bad_ip = new BadIpImpl(ip);
    		
    		bad_ip_map.put( ip, bad_ip );
    	}
    	
    	return( bad_ip.incrementWarnings());
    	
    }finally{
    	
    	bad_ip_map_mon.exit();
    }
  }

 
  public int 
  getNbWarningForIp(
  	String ip) 
  {
    try{
    	bad_ip_map_mon.enter();
    	
        BadIpImpl bad_ip = (BadIpImpl) bad_ip_map.get(ip);
      
      if(bad_ip == null) {
      	
        return 0;
        
      }else{
      	
        return bad_ip.getNumberOfWarnings();
      }
    }finally{
    
    	bad_ip_map_mon.exit();
    }
  }
  
  public BadIp[]
  getBadIps()
  {
    try{
    	bad_ip_map_mon.enter();
    	
  		BadIp[]	res = new BadIp[bad_ip_map.size()];
  		
  		bad_ip_map.values().toArray( res );
  		
  		return( res );
    }finally{
        
       	bad_ip_map_mon.exit();
    }
  }
  
  public void
  clearBadIps()
  {
    try{
    	bad_ip_map_mon.enter();
  
    	bad_ip_map.clear();
    	
    }finally{
        
        bad_ip_map_mon.exit();
    }
  }
  
  public int
  getNbBadIps()
  {
  	return( bad_ip_map.size());
  }
}
