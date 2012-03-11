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

import java.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.core3.util.*;

public class ConfigSectionRepository {

  private static ConfigSectionRepository 	instance;
  private static AEMonitor					class_mon	= new AEMonitor( "ConfigSectionRepository:class");
  
  private Map<ConfigSection,ConfigSectionHolder> items;

  private ConfigSectionRepository() {
   items = new LinkedHashMap<ConfigSection, ConfigSectionHolder>();
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

  public void addConfigSection(ConfigSection item, PluginInterface pi ) {
  	try{
  		class_mon.enter();
  		
  		items.put(item, new ConfigSectionHolder( item, pi ));
  		
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
  
  public ArrayList<ConfigSection> getList() {
	 	try{
	  		class_mon.enter();
	   
	  		return (new ArrayList<ConfigSection>( items.keySet() ));
	  		
	 	  }finally{
	    	
	    	class_mon.exit();
	    } 		
	  }
  
  public ArrayList<ConfigSectionHolder> getHolderList() {
 	try{
  		class_mon.enter();
   
  		return (new ArrayList<ConfigSectionHolder>( items.values() ));
  		
 	  }finally{
    	
    	class_mon.exit();
    } 		
  }

}
