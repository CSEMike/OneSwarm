/*
 * Created on 16-May-2004
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

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.update.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;

public class 
UpdateInstallerImpl
	implements UpdateInstaller
{
		// change these and you'll need to change the Updater!!!!
	
	protected static final String	UPDATE_DIR 	= "updates";
	protected static final String	ACTIONS		= "install.act";
	
	protected static AEMonitor	class_mon 	= new AEMonitor( "UpdateInstaller:class" );

	private UpdateManager	manager;
	private File			install_dir;
	
	protected static void
	checkForFailedInstalls(
		UpdateManager	manager )
	{
		try{
			File	update_dir = new File( manager.getUserDir() + File.separator + UPDATE_DIR );
			
			File[]	dirs = update_dir.listFiles();
			
			if ( dirs != null ){
				
				boolean	found_failure = false;
				
				String	files = "";
				
				for (int i=0;i<dirs.length;i++){
					
					File	dir = dirs[i];
					
					if ( dir.isDirectory()){
						
							// if somethings here then the install failed
						
						found_failure	= true;
						
						File[] x = dir.listFiles();
						
						if ( x != null ){
							
							for (int j=0;j<x.length;j++){
								
								files += (files.length()==0?"":",") + x[j].getName();
							}
						}
						
						FileUtil.recursiveDelete( dir );
					}
				}
				
				if ( found_failure ){
					Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
							MessageText.getString("Alert.failed.update", new String[]{ files })));
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	protected
	UpdateInstallerImpl(
		UpdateManager	_manager )
	
		throws UpdateException
	{
		manager	= _manager;
		
		try{
			class_mon.enter();
			
				// updates are in general user-specific (e.g. plugin updates) so store here
				// obviously core ones will affect all users
			
			String	update_dir = getUserDir() + File.separator + UPDATE_DIR;
			
			for (int i=1;i<1024;i++){
				
				File	try_dir = new File( update_dir + File.separator + "inst_" + i );
								
				if ( !try_dir.exists()){
					
					if ( !FileUtil.mkdirs(try_dir)){
		
						throw( new UpdateException( "Failed to create a temporary installation dir"));
					}
					
					install_dir	= try_dir;
					
					break;
				}
			}
			
			if ( install_dir == null ){
				
				throw( new UpdateException( "Failed to find a temporary installation dir"));
			}
		}finally{
			
			class_mon.exit();
		}
	}
	
	public void
	addResource(
		String      resource_name,
		InputStream   is )
  
    	throws UpdateException
	{
		addResource(resource_name,is,true);
	}
  
	public void
	addResource(
		String			resource_name,
		InputStream		is,
		boolean 		closeInputStream)
	
		throws UpdateException
	{
		try{
			File	target_file = new File(install_dir, resource_name );
		
			FileUtil.copyFile( is, new FileOutputStream( target_file ),closeInputStream);
			
		}catch( Throwable e ){
			
			throw( new UpdateException( "UpdateInstaller: resource addition fails", e ));
		}
	}
		
	public String
	getInstallDir()
	{
		return( manager.getInstallDir());
	}
		
	public String
	getUserDir()
	{
		return( manager.getUserDir());
	}
	
	public void
	addMoveAction(
		String		from_file_or_resource,
		String		to_file )
	
		throws UpdateException
	{
		// System.out.println( "move action:" + from_file_or_resource + " -> " + to_file );
		
		if ( from_file_or_resource.indexOf(File.separator) == -1 ){
			
			from_file_or_resource = install_dir.toString() + File.separator + from_file_or_resource;
		}
		
		try{
				// see if this action has a chance of succeeding
			
			File	to_f = new File( to_file );
			
			File	parent = to_f.getParentFile();
			
			if ( parent != null && !parent.exists()){
				
				parent.mkdirs();
			}
			
			if ( parent != null ){
				
				// we're going to need write access to the parent, let's try
				
				if ( !parent.canWrite()){
					
						// Vista install process goes through permissions elevation process
						// so don't warn about lack of write permissions
					
					if ( !Constants.isWindowsVistaOrHigher ){
						
						Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING,
							"The location '" + parent.toString()
									+ "' isn't writable, this update will probably fail."
									+ " Check permissions and retry the update"));
						
					}
				}
			}
							
			try{
				PlatformManager	pm = PlatformManagerFactory.getPlatformManager();
					
				if ( pm.hasCapability( PlatformManagerCapabilities.CopyFilePermissions )){
					
					String	parent_str = parent.getAbsolutePath();
										
					PlatformManagerFactory.getPlatformManager().copyFilePermissions(
							parent_str, from_file_or_resource );
				}
			}catch( Throwable e ){
					
				Debug.out( e );
			}
		}catch( Throwable e ){
			
		}
		
		appendAction( "move," + from_file_or_resource  + "," + to_file );
	}
  
  
	public void
	addChangeRightsAction(
		String    rights,
		String    to_file )
  
    	throws UpdateException
	{ 
		appendAction( "chmod," + rights  + "," + to_file );
	}
  
	public void
	addRemoveAction(
		String    file )
  
    	throws UpdateException
	{ 
		appendAction( "remove," + file );
	}
	
	protected void
	appendAction(
		String		data )
	
		throws UpdateException
	{
		PrintWriter	pw = null;
	
		try{		
			
			pw = new PrintWriter(new FileWriter( install_dir.toString() + File.separator + ACTIONS, true ));

			pw.println( data );
			
		}catch( Throwable e ){
			
			throw( new UpdateException( "Failed to write actions file", e ));
			
		}finally{
			
			if ( pw != null ){
		
				try{
		
					pw.close();
					
				}catch( Throwable e ){
	
					throw( new UpdateException( "Failed to write actions file", e ));
				}
			}
		}
	}
}
