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
package org.gudy.azureus2.ui.swt.pluginsinstaller;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.core.AzureusCore;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.installer.InstallablePlugin;
import org.gudy.azureus2.plugins.installer.PluginInstallerListener;
import org.gudy.azureus2.plugins.installer.StandardPlugin;

/**
 * @author Olivier Chalouhi
 *
 */
public class InstallPluginWizard extends Wizard {
      
  int mode;
  
  StandardPlugin[]	standard_plugins;
  List plugins = new ArrayList();
  boolean shared = false;
  String list_title_text;
  
  public static void 
  register(
	final AzureusCore		core,
	final Display			display )
  {
	core.getPluginManager().getPluginInstaller().addListener(
		new PluginInstallerListener()
		{
			public boolean
			installRequest(
				final String			reason,
				final InstallablePlugin	plugin )
			
				throws PluginException
			{
				if ( plugin instanceof StandardPlugin ){
					
					display.asyncExec(
						new Runnable()
						{
							public void
							run()
							{
								new InstallPluginWizard( reason, (StandardPlugin)plugin );
							}
						});
					
					return( true );
				}else{
					
					return( false );
				}
			}
		});
  }
  
  
  public InstallPluginWizard()
	{
		super("installPluginsWizard.title");			
				
		IPWModePanel mode_panel = new IPWModePanel(this,null);
	
		setFirstPanel(mode_panel);
	}
  
  	public 
  	InstallPluginWizard(
  		String				reason,
  		StandardPlugin		plugin )
  	{
		super("installPluginsWizard.title");			
			
		standard_plugins 	= new StandardPlugin[]{ plugin };
		list_title_text		= reason;
		
		plugins = new ArrayList();
		plugins.add( plugin );
		
		IPWListPanel list_panel = new IPWListPanel(this,null);
		
		setFirstPanel(list_panel);
	}
  	
  	protected StandardPlugin[]
  	getStandardPlugins(AzureusCore core)
  	
  		throws PluginException
  	{
  		if ( standard_plugins == null ){
  			
  			standard_plugins = core.getPluginManager().getPluginInstaller().getStandardPlugins();
  		}
  		
  		return( standard_plugins );
  	}
  	
  	protected String
  	getListTitleText()
  	{
  		if ( list_title_text == null ){
  			
  			list_title_text = MessageText.getString( "installPluginsWizard.list.loaded" );
  		}
  		
  		return( list_title_text );
  	}
  	
  	public void 
	onClose() 
	{
		// Call the parent class to clean up resources
		super.onClose();	
	}
  	
  	public void 
	setPluginList(List _plugins) {
  	  plugins = _plugins;
  	}
  	
  	public List
  	getPluginList()
  	{
  		return( plugins );
  	}
  	
  	public void performInstall() 
  	{
   	  InstallablePlugin[]	ps = new InstallablePlugin[ plugins.size()];
  	  
  	  plugins.toArray( ps );
  	  
  	  if ( ps.length > 0 ){
  	  	
  	    try{
  	    	
  	      ps[0].getInstaller().install(ps,shared);
  	      
  	    }catch(Exception e){
  	    	
  	      Debug.printStackTrace(e);
  	      
  	      Logger.log(new LogAlert(LogAlert.REPEATABLE,
						"Failed to initialise installer", e));
  	    }
  	  }
  	}
}
