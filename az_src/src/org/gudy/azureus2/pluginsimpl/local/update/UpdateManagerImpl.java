/*
 * Created on 07-May-2004
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.AEVerifier;
import org.gudy.azureus2.core3.util.AEVerifierException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.update.*;

import com.aelitis.azureus.core.AzureusCore;

public class 
UpdateManagerImpl
	implements UpdateManager
{
	protected static UpdateManagerImpl		singleton;
		
	public static UpdateManager
	getSingleton(
		AzureusCore		core )
	{
		if ( singleton == null ){
			
			singleton = new UpdateManagerImpl( core );
		}
		
		return( singleton );
	}

	protected AzureusCore	azureus_core;
		
	protected List	components 				= new ArrayList();
	protected List	listeners				= new ArrayList();
	protected List	verification_listeners	= new ArrayList();
	
	protected List	installers	= new ArrayList();
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UpdateManager" );

	protected
	UpdateManagerImpl(
		AzureusCore		_azureus_core )
	{
		azureus_core	= _azureus_core;
		
		UpdateInstallerImpl.checkForFailedInstalls( this );
		
			// cause the platform manager to register any updateable components
		
		try{
			PlatformManagerFactory.getPlatformManager();
			
		}catch( Throwable e ){
		
		}
	}
	
	public void
	registerUpdatableComponent(
		UpdatableComponent		component,
		boolean					mandatory )
	{
		try{
			this_mon.enter();
			
			components.add( new UpdatableComponentImpl( component, mandatory ));
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public UpdateCheckInstance
	createUpdateCheckInstance()
	{
		return( createUpdateCheckInstance( UpdateCheckInstance.UCI_UPDATE, "" ));
	}
	
	public UpdateCheckInstance
	createUpdateCheckInstance(
		int			type,
		String		name )
	{
		try{
			this_mon.enter();
	
			UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[components.size()];
			
			components.toArray( comps );
			
			UpdateCheckInstance	res = new UpdateCheckInstanceImpl( this, type, name, comps );
			
			for (int i=0;i<listeners.size();i++){
				
				((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public UpdateCheckInstance
	createEmptyUpdateCheckInstance(
		int			type,
		String		name )
	{
		try{
			this_mon.enter();
	
			UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[0];
			
			UpdateCheckInstance	res = new UpdateCheckInstanceImpl( this, type, name, comps );
			
			for (int i=0;i<listeners.size();i++){
				
				((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}		
	}

	public UpdateInstaller
	createInstaller()
		
		throws UpdateException
	{
		UpdateInstaller	installer = new UpdateInstallerImpl( this );
		
		installers.add( installer );
		
		return( installer );
	}
	
	public UpdateInstaller[]
	getInstallers()
	{
		UpdateInstaller[]	res = new UpdateInstaller[installers.size()];
		
		installers.toArray( res );
		
		return( res );
	}
	
	public String
	getInstallDir()
	{
		String	str = SystemProperties.getApplicationPath();
		
		if ( str.endsWith(File.separator)){
			
			str = str.substring(0,str.length()-1);
		}
		
		return( str );
	}
		
	public String
	getUserDir()
	{
		String	str = SystemProperties.getUserPath();
		
		if ( str.endsWith(File.separator)){
			
			str = str.substring(0,str.length()-1);
		}
		
		return( str );	
	}
	
	public void
	restart()
	
		throws UpdateException
	{
		applyUpdates( true );
	}
	
	public void
	applyUpdates(
		boolean	restart_after )
	
		throws UpdateException
	{
		try{
			if ( restart_after ){
				
				azureus_core.requestRestart();
				
			}else{
				
				azureus_core.requestStop();
			}
		}catch( Throwable e ){
			
			throw( new UpdateException( "UpdateManager:applyUpdates fails", e ));
		}
	}
	
	public InputStream
	verifyData(
		Update			update,
		InputStream		is,
		boolean			force )
	
		throws UpdateException
	{
		boolean	queried 	= false;
		boolean	ok			= false;
		Throwable	failure	= null;
		
		try{
			File	temp = AETemporaryFileHandler.createTempFile();
			
			FileUtil.copyFile( is, temp );
			
			try{
				AEVerifier.verifyData( temp );
			
				ok	= true;
				
				return( new FileInputStream( temp ));

			}catch( AEVerifierException e ){
								
				if ( (!force) && e.getFailureType() == AEVerifierException.FT_SIGNATURE_MISSING ){
					
					for (int i=0;i<verification_listeners.size();i++){
						
						try{
							queried	= true;
							
							if ( ((UpdateManagerVerificationListener)verification_listeners.get(i)).acceptUnVerifiedUpdate(
									update )){
								
								ok	= true;
								
								return( new FileInputStream( temp ));
							}
						}catch( Throwable f ){
							
							Debug.printStackTrace(f);
						}
					}
				}
				
				failure	= e;
				
				throw( e );
			}
		}catch( UpdateException e ){

			failure	= e;
			
			throw( e );
				
		}catch( Throwable e ){
			
			failure	= e;
			
			throw( new UpdateException( "Verification failed", e ));
			
		}finally{
			
			if ( !( queried || ok )){
				
				if ( failure == null ){
					
					failure = new UpdateException( "Verification failed" );
				}
				
				for (int i=0;i<verification_listeners.size();i++){
					
					try{
						((UpdateManagerVerificationListener)verification_listeners.get(i)).verificationFailed( update, failure );
	
					}catch( Throwable f ){
						
						Debug.printStackTrace(f);
					}
				}
			}
		}
	}
	

	public void
	addVerificationListener(
		UpdateManagerVerificationListener	l )
	{
		verification_listeners.add( l );
	}
	
	public void
	removeVerificationListener(
		UpdateManagerVerificationListener	l )
	{
		verification_listeners.add( l );
	}
	
	public void
	addListener(
		UpdateManagerListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		UpdateManagerListener	l )
	{
		listeners.remove(l);
	}
}
