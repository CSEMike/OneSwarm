/*
 * Created on 29 nov. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.pluginsuninstaller;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author Olivier Chalouhi
 *
 */
public class UnInstallPluginWizard extends Wizard {
      
  List plugins = new ArrayList();
  
  public UnInstallPluginWizard(
 		Display 	display )
	{
		super("uninstallPluginsWizard.title");			
		
		UIPWListPanel list_panel = new UIPWListPanel(this,null);
	
		setFirstPanel(list_panel);
	}
  
  	public void 
	onClose() 
	{
		// Call the parent class to clean up resources
		super.onClose();	
	}
  	
  	public void setPluginList(List _plugins) {
  	  plugins = _plugins;
  	}
  	
 	public List
  	getPluginList()
  	{
  		return( plugins );
  	}
 	
  	public void performUnInstall() 
  	{
  	  PluginInterface[]	ps = new PluginInterface[ plugins.size()];
  	  
  	  plugins.toArray( ps );
  	  
  	  if ( ps.length > 0 ){
  	  	
  	    try{
  	    	
  	      ps[0].getPluginManager().getPluginInstaller().uninstall( ps );
  	      
  	    }catch(Exception e){
  	    	
  	      Debug.printStackTrace(e);
  	      
  	      Logger.log(new LogAlert(LogAlert.REPEATABLE,
						"Failed to initialise installer", e));
  	    }
  	  }
  	}
}
