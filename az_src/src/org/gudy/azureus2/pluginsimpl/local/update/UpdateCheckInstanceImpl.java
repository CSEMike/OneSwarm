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
import org.gudy.azureus2.core3.logging.*;

import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
UpdateCheckInstanceImpl
	implements UpdateCheckInstance
{
	private static final LogIDs LOGID = LogIDs.CORE;
	private List	listeners			= new ArrayList();
	private List	updates 			= new ArrayList();
	private List	decision_listeners	= new ArrayList();
	

	private AESemaphore	sem 	= new AESemaphore("UpdateCheckInstance");

	private UpdateManager	manager;
	private int				check_type;
	private String			name;

	private UpdatableComponentImpl[]		components;
	private UpdateCheckerImpl[]				checkers;
	
	private boolean		completed;
	private boolean		cancelled;
	
	private boolean		automatic	= true;
	
	protected AEMonitor this_mon 	= new AEMonitor( "UpdateCheckInstance" );
	
	protected
	UpdateCheckInstanceImpl(
		UpdateManager				_manager,
		int							_check_type,
		String						_name,
		UpdatableComponentImpl[]	_components )
	{
		manager		= _manager;
		check_type	= _check_type;
		name		= _name;
		components	= _components;
		
		checkers	= new UpdateCheckerImpl[components.length];
		
 		for (int i=0;i<components.length;i++){
			
			UpdatableComponentImpl	comp = components[i];
			
			checkers[i] = new UpdateCheckerImpl( this, comp, sem );
		}
	}

	public int
	getType()
	{
		return( check_type );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public void
	addUpdatableComponent(
		UpdatableComponent		component,
		boolean					mandatory )
	{
			// add new component
		
		UpdatableComponentImpl	comp = new UpdatableComponentImpl( component, mandatory );
		
		UpdatableComponentImpl[]	new_comps = new UpdatableComponentImpl[components.length+1];
		
		System.arraycopy( components, 0, new_comps, 0, components.length );
		
		new_comps[components.length]	= comp;
		
		components	= new_comps;
		
			// add a new checker
		
		UpdateCheckerImpl	checker = new UpdateCheckerImpl( this, comp, sem );
		
		UpdateCheckerImpl[]	new_checkers = new UpdateCheckerImpl[checkers.length+1];
		
		System.arraycopy( checkers, 0, new_checkers, 0, checkers.length );
		
		new_checkers[checkers.length]	= checker;
		
		checkers	= new_checkers;
	}
	
	public void
	setAutomatic(
		boolean	a )
	{
		automatic = a;
	}
	
	public boolean
	isAutomatic()
	{
		return( automatic );
	}
	
	public void
	start()
	{
		for (int i=0;i<components.length;i++){
			
			final UpdateCheckerImpl			checker = checkers[i];
			
			Thread	t = 
				new AEThread( "UpdatableComponent Checker:" + i )
				{
					public void
					runSupport()
					{					
						try{		
							checker.getComponent().checkForUpdate( checker );
							
						}catch( Throwable e ){
							
							checker.failed();
						}
					}
				};
				
			t.setDaemon( true );
			
			t.start();
		}
		
		Thread	t = 
			new AEThread( "UpdatableComponent Completion Waiter" )
			{
				public void
				runSupport()
				{
					for (int i=0;i<components.length;i++){
			
						sem.reserve();
					}
					
					try{
						this_mon.enter();
						
						if ( cancelled ){
							
							return;
						}
					
						completed	= true;
						
					}finally{
						
						this_mon.exit();
					}
					
					boolean	mandatory_failed = false;
					
					for (int i=0;i<checkers.length;i++){
						
						if ( components[i].isMandatory() && checkers[i].getFailed()){
							
							mandatory_failed	= true;
							
							break;
						}
					}
					
					List	target_updates = new ArrayList();
					
						// if any mandatory checks failed then we can't do any more
					
					if ( mandatory_failed ){
						
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
										"Dropping all updates as a mandatory update check failed"));

					}else{
							// If there are any manadatory updates then we just go ahead with them and drop the rest
						
						boolean	mandatory_only	= false;
						
						for (int i=0;i<updates.size();i++){
							
							UpdateImpl	update = (UpdateImpl)updates.get(i);
							
							if ( update.isMandatory()){
								
								mandatory_only	= true;
								
								break;
							}
						}
						
						for (int i=0;i<updates.size();i++){
							
							UpdateImpl	update = (UpdateImpl)updates.get(i);
														
							if ( update.isMandatory() || !mandatory_only ){
								
								target_updates.add( update );
								
							}else{
								if (Logger.isEnabled())
								  Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
								                          "Dropping update '" + update.getName()
                                            + "' as non-mandatory and "
                                            + "mandatory updates found"));
							}
						}
					}

					updates	= target_updates;
					
					for (int i=0;i<listeners.size();i++){
					
						try{
							((UpdateCheckInstanceListener)listeners.get(i)).complete( UpdateCheckInstanceImpl.this );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
		
	protected UpdateImpl
	addUpdate(
		UpdatableComponentImpl	comp,
		String					update_name,
		String[]				desc,
		String					new_version,
		ResourceDownloader[]	downloaders,
		int						restart_required )
	{
		try{
			this_mon.enter();
		
			UpdateImpl	update = 
				new UpdateImpl( this, update_name, desc, new_version, 
								downloaders, comp.isMandatory(), restart_required );
			
			updates.add( update );
						
			if ( cancelled ){
				
				update.cancel();
			}
			
			return( update );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public Update[]
	getUpdates()
	{
		try{
			this_mon.enter();
		
			Update[]	res = new Update[updates.size()];
		
			updates.toArray( res );
		
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public UpdateChecker[]
	getCheckers()
	{
		return( checkers );
	}
	
	public UpdateInstaller
	createInstaller()
	
		throws UpdateException
	{
		return( manager.createInstaller());
	}
	
	public void
	cancel()
	{
		boolean	just_do_updates = false;
		
		try{
			this_mon.enter();
			
			if ( completed ){
				
				just_do_updates = true;
			}
		
			cancelled	= true;
			
		}finally{
			
			this_mon.exit();
		}
			
		
		for (int i=0;i<updates.size();i++){
			
			((UpdateImpl)updates.get(i)).cancel();
		}

		if ( !just_do_updates ){
			
			for (int i=0;i<checkers.length;i++){
				
				if ( checkers[i] != null ){
					
					checkers[i].cancel();
				}
			}
			
			for (int i=0;i<listeners.size();i++){
					
				try{
					((UpdateCheckInstanceListener)listeners.get(i)).cancelled( this );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
	
	public boolean
	isCancelled()
	{
		return( cancelled );
	}
	
	public UpdateManager
	getManager()
	{
		return( manager );
	}
	
	protected Object
	getDecision(
		Update		update,
		int			decision_type,
		String		decision_name,
		String		decision_description,
		Object		decision_data )
	{
		for (int i=0;i<decision_listeners.size();i++){
			
			Object res = 
				((UpdateManagerDecisionListener)decision_listeners.get(i)).decide(
						update, decision_type, decision_name, decision_description, decision_data );
			
			if ( res != null ){
				
				return( res );
			}
		}
		
		return( null );
	}
	
	public void
	addDecisionListener(
		UpdateManagerDecisionListener	l )
	{
		decision_listeners.add(l);
	}
	
	public void
	removeDecisionListener(
		UpdateManagerDecisionListener	l )
	{
		decision_listeners.remove(l);
	}
	
	public void
	addListener(
		UpdateCheckInstanceListener	l )
	{
		listeners.add( l );
		
		if ( completed ){
			
			l.complete( this );
			
		}else if ( cancelled ){
			
			l.cancelled( this );
		}
	}
	
	public void
	removeListener(
		UpdateCheckInstanceListener	l )
	{
		listeners.remove(l);
	}
}
