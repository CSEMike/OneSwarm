/*
 * Created on Mar 1, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.util;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AEThread2;

public class 
LaunchManager 
{
	private static LaunchManager	singleton = new LaunchManager();
	
	public static LaunchManager
	getManager()
	{
		return( singleton );
	}
	
	private CopyOnWriteList<LaunchController>	controllers	= new CopyOnWriteList<LaunchController>();
	
	public void
	launchRequest(
		final LaunchTarget	target,
		final LaunchAction	action )
	{
		new AEThread2( "LaunchManager:request" )
		{
			public void
			run()
			{
				for ( LaunchController c: controllers ){
					
					try{
						c.handleRequest( target );
						
					}catch( Throwable e ){
						
						action.actionDenied( e );
						
						return;
					}
				}
				
				action.actionAllowed();
			}
		}.start();
	}
	
	public LaunchTarget
	createTarget(
		DownloadManager		dm )
	{
		return( new LaunchTarget( dm ));
	}
	
	public LaunchTarget
	createTarget(
		DiskManagerFileInfo		fi )
	{
		return( new LaunchTarget( fi ));
	}
	
	public void
	addController(
		LaunchController	controller )
	{
		controllers.add( controller );
	}
	
	public void
	removeController(
		LaunchController	controller )
	{
		controllers.remove( controller );
	}
	
	public class
	LaunchTarget
	{
		private DownloadManager			dm;
		private DiskManagerFileInfo		file_info;
		
		private 
		LaunchTarget(
			DownloadManager		_dm )
		{
			dm		= _dm;
		}
		
		private 
		LaunchTarget(
			DiskManagerFileInfo		_file_info )
		{
			file_info	= _file_info;	
			dm			= file_info.getDownloadManager();
		}
		
		public DownloadManager
		getDownload()
		{
			return( dm );
		}
		
		public DiskManagerFileInfo
		getFile()
		{
			return( file_info );
		}
	}
	
	public interface
	LaunchController
	{
		public void
		handleRequest(
			LaunchTarget		target )
		
			throws Throwable;
	}
	
	public interface
	LaunchAction
	{
		public void
		actionAllowed();
		
		public void
		actionDenied(
			Throwable			reason );
			
	}
}
