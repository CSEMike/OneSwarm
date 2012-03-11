/*
 * Created on 12-May-2004
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

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
UpdateCheckerImpl
	implements UpdateChecker
{
	protected UpdateCheckInstanceImpl		check_instance;
	protected UpdatableComponentImpl		component;
	protected AESemaphore					semaphore;
	
	protected boolean						completed;
	protected boolean						failed;
	protected boolean						cancelled;

	protected boolean						sem_released;
	

	protected List	listeners			= new ArrayList();
	protected List	progress_listeners	= new ArrayList();
	
	protected AEMonitor this_mon 	= new AEMonitor( "UpdateChecker" );

	protected
	UpdateCheckerImpl(
		UpdateCheckInstanceImpl	_check_instance,
		UpdatableComponentImpl	_component,
		AESemaphore				_sem )
	{
		check_instance		= _check_instance;
		component			= _component;
		semaphore			= _sem;
	}
	
	public UpdateCheckInstance
	getCheckInstance()
	{
		return( check_instance );
	}
	
	public Update
	addUpdate(
		String				name,
		String[]			description,
		String				new_version,
		ResourceDownloader	downloader,
		int					restart_required )
	{
		return(	addUpdate(
					name, description, new_version,
					new ResourceDownloader[]{ downloader },
					restart_required ));
	}
	
	public Update
	addUpdate(
		String					name,
		String[]				description,
		String					new_version,
		ResourceDownloader[]	downloaders,
		int						restart_required )
	{
		reportProgress( "Adding update: " + name );
		
		return( check_instance.addUpdate( 
					component, name, description, new_version,
					downloaders, restart_required ));
	}
	
	public UpdateInstaller
	createInstaller()
	
		throws UpdateException
	{
		return( check_instance.createInstaller());
	}

	public UpdatableComponent
	getComponent()
	{
		return( component.getComponent());
	}
	
	public void
	completed()
	{
		try{
			this_mon.enter();
		
			if ( !sem_released ){
				
				completed	= true;
				
				for (int i=0;i<listeners.size();i++){
					
					try{
						((UpdateCheckerListener)listeners.get(i)).completed( this );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
				
				sem_released	= true;
				
				semaphore.release();
			}
		}finally{
			
			this_mon.exit();
		}
	}
		
	public void
	failed()
	{
		try{
			this_mon.enter();
		
			if ( !sem_released ){
				
				failed	= true;
	
				for (int i=0;i<listeners.size();i++){
					
					try{
						((UpdateCheckerListener)listeners.get(i)).failed( this );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
	
				sem_released	= true;
				
				semaphore.release();
			}	
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean
	getFailed()
	{
		return( failed );
	}
	
	protected void
	cancel()
	{
		cancelled	= true;
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((UpdateCheckerListener)listeners.get(i)).cancelled( this );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addListener(
		UpdateCheckerListener	l )
	{
		try{
			this_mon.enter();
		
			listeners.add( l );
			
			if ( failed ){
				
				l.failed( this );
				
			}else if ( completed ){
				
				l.completed( this );
			}
			
			if ( cancelled ){
				
				l.cancelled( this );
				
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeListener(
		UpdateCheckerListener	l )
	{
		try{
			this_mon.enter();
		
			listeners.remove(l);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	reportProgress(
		String		str )
	{
		List	ref = progress_listeners;
		
		for (int i=0;i<ref.size();i++){
			
			try{
				((UpdateProgressListener)ref.get(i)).reportProgress( str );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addProgressListener(
		UpdateProgressListener	l )
	{
		try{
			this_mon.enter();
		
			List	new_l = new ArrayList( progress_listeners );
			
			new_l.add( l );
			
			progress_listeners	= new_l;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeProgressListener(
		UpdateProgressListener	l )
	{
		try{
			this_mon.enter();
		
			List	new_l = new ArrayList( progress_listeners );
			
			new_l.remove( l );
			
			progress_listeners	= new_l;
				
		}finally{
			
			this_mon.exit();
		}
	}
}
