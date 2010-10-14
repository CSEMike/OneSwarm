/*
 * Created on 28-Nov-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.pluginsimpl.local.installer;

import java.util.Properties;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.plugins.*;
//import org.gudy.azureus2.plugins.installer.PluginInstaller;

/**
 * @author parg
 *
 */
public class 
Test 
	implements Plugin, PluginListener
{
	protected PluginManager		manager;
	
	public
	Test()
	{
		// constructor for Plugin
	}
	
	public 
	Test(
		boolean	ignore )
	{
		Properties props = new Properties();
		
		props.put( PluginManager.PR_MULTI_INSTANCE, "true" );
		
		PluginManager.registerPlugin( Test.class );
	
		PluginManager.startAzureus( PluginManager.UI_SWT, props );
	}
	
	public void 
	initialize(
		PluginInterface pi )
	  
		throws PluginException
	{
		manager	= pi.getPluginManager();
		
		pi.addListener( this );
	}
	
	public void
	initializationComplete()
	{
		new AEThread("install tester" )
		{
			public void
			runSupport()
			{	
				try{
					sleep(10000);
					
					/*
					PluginInstaller	installer = manager.getPluginInstaller();
					
					StandardPlugin[]	sps = installer.getStandardPlugins();
					
					String	install_name = "azshareexporter";
					
					StandardPlugin	install_act = null;
					
					for (int i=0;i<sps.length;i++){
						
						StandardPlugin	sp = sps[i];
						
						System.out.println( "Standard Plugin: " + sp.getId() + " - " + sp.getVersion() + ", installed = " + sp.getAlreadyInstalledPlugin());
						
						if ( sp.getId().equals( install_name )){
							
							install_act = sp;
						}
					}
					
					install_act.install( true );
					*/
					
					/*
					FilePluginInstaller inst = installer.installFromFile(new File("C:\\temp\\azshareexporter_0.1.jar"));
					
					inst.install( false );
					*/
					
					PluginInterface pi = manager.getPluginInterfaceByID("azshareexporter");
					
					pi.uninstall();
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public void
	closedownInitiated()
	{
		
	}
	
	public void
	closedownComplete()
	{
		
	}
	
	public static void
	main(
		String[]	args )
	{
		new Test(true);
	}
}
