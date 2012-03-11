/*
 * Created on 01-Dec-2004
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

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.InstallablePlugin;
import org.gudy.azureus2.plugins.installer.PluginInstallationListener;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin;

/**
 * @author parg
 *
 */

public abstract class 
InstallablePluginImpl 
	implements InstallablePlugin
{
	private PluginInstallerImpl		installer;
	
	
	protected
	InstallablePluginImpl(
		PluginInstallerImpl		_installer )
	{
		installer = _installer;
	}
	
	/**
	 * Returns the plugin's interface if already installed, null if it isn't
	 * @return
	 */
	
	public boolean 
	isAlreadyInstalled() 
	{
		PluginInterface pi = getAlreadyInstalledPlugin();
		
		if ( pi == null ){
			
			return( false );
		}
		
		String version = getVersion();
		
		if ( version == null || version.length() == 0 ){
			
			return( false );
		}
		
		String existing_version = pi.getPluginVersion();
		
			// this is the case when running with plugin in eclipse
		
		if ( existing_version == null ){
			
			return( true );
		}
		
		return( Constants.compareVersions( existing_version, version ) >= 0);
	}
	
	public PluginInterface
	getAlreadyInstalledPlugin()
	{
		return( installer.getAlreadyInstalledPlugin( getId()));
	}
	
	public void
	install(
		boolean		shared )
	
		throws PluginException
	{
		installer.install( this, shared );
	}	
	
	public void
	install(
		boolean				shared,
		boolean				low_noise,
		final boolean		wait_until_done )
	
		throws PluginException
	{
		final AESemaphore sem = new AESemaphore( "FPI" );
		
		final PluginException[]	error = { null };
		
		installer.install( 
			new InstallablePlugin[]{ this }, 
			shared,
			low_noise,
			null,
			new PluginInstallationListener()
			{
				public void 
				completed() 
				{
					sem.release();
				}
				
				public void 
				cancelled() 
				{
					failed( new PluginException( "Install cancelled" ));
				}
				
				public void 
				failed(
					PluginException e ) 
				{
					error[0] = e;
					
					sem.release();
					
					if ( !wait_until_done ){
						
						Debug.out( "Install failed", e );
					}
				}
			});
		
		if ( wait_until_done ){
			
			sem.reserve();
			
			if ( error[0] != null ){
				
				throw( error[0] );
			}
		}
	}	
	
	public void
	uninstall()
	
		throws PluginException
	{
		installer.uninstall( this );
	}	
	
	public PluginInstaller
	getInstaller()
	{
		return( installer );
	}
	
	public abstract void
	addUpdate(
			UpdateCheckInstance	inst,
			PluginUpdatePlugin	plugin_update_plugin,
			Plugin				plugin,
			PluginInterface		plugin_interface );
}
