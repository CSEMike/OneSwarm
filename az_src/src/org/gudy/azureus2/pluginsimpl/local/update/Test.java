/*
 * Created on 17-Dec-2004
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

package org.gudy.azureus2.pluginsimpl.local.update;

import java.util.Properties;
import java.io.*;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;

import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.update.UpdateManager;

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
		new AEThread("update tester" )
		{
			public void
			runSupport()
			{	
				try{
	
					UpdateManager	update_man = manager.getDefaultPluginInterface().getUpdateManager();
					
					UpdateInstaller installer = update_man.createInstaller();
					
					File	from_file 	= new File( "C:\\temp\\update_from" );
					File	to_file		= new File( "C:\\temp\\update_to" );
					
					PrintWriter pw = new PrintWriter( new FileWriter( from_file ));
					
					pw.println( "hello mum");
					
					pw.close();
					
					to_file.delete();
					
					installer.addMoveAction( from_file.toString(), to_file.toString());
					
					update_man.applyUpdates( false );
					
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
