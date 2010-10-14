/*
 * File    : ConfigSectionRepository.java
 * Created : 1 feb. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.pluginsimpl.local.ui.config;

import java.util.ArrayList;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.core3.util.*;

public class ConfigSectionRepository {

  private static ConfigSectionRepository 	instance;
  private static AEMonitor					class_mon	= new AEMonitor( "ConfigSectionRepository:class");
  
  private ArrayList items;

  private ConfigSectionRepository() {
   items = new ArrayList();
  }

  public static ConfigSectionRepository getInstance() {
  	try{
  		class_mon.enter();
  		
	    if(instance == null)
	      instance = new ConfigSectionRepository();
	    return instance;
  	}finally{
  		
  		class_mon.exit();
  	}
  }

  public void addConfigSection(ConfigSection item) {
  	try{
  		class_mon.enter();
  		
  		items.add(item);
  		
    }finally{
    	
    	class_mon.exit();
    }
  }
  
  public void removeConfigSection(ConfigSection item) {
	  	try{
	  		class_mon.enter();
	  		
	  		items.remove(item);
	  		
	    }finally{
	    	
	    	class_mon.exit();
	    }
	  }
  
  public ArrayList getList() {
 	try{
  		class_mon.enter();
   
  		return (ArrayList)items.clone();
  		
 	  }finally{
    	
    	class_mon.exit();
    } 		
  }

}
