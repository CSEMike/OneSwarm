/*
 * Created on 13-Jul-2004
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

package org.gudy.azureus2.pluginsimpl.local;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsimpl.local.launch.PluginSingleInstanceHandler;

public class 
PluginManagerDefaultsImpl
	implements PluginManagerDefaults
{
	protected static  PluginManagerDefaultsImpl		singleton = new PluginManagerDefaultsImpl();
	
	private PluginManagerArgumentHandler		arg_handler;
	
	public static PluginManagerDefaults
	getSingleton()
	{
		return( singleton );
	}
	
	protected List	disabled	= new ArrayList();
	
	public String[]
	getDefaultPlugins()
	{
		return( PLUGIN_IDS );
	}
	
	public void
	setDefaultPluginEnabled(
		String	plugin_id,
		boolean	enabled )
	{
		if ( enabled ){
			
			disabled.remove( plugin_id );
			
		}else if ( !disabled.contains( plugin_id )){
			
			disabled.add( plugin_id );
		}
	}
	
	public boolean
	isDefaultPluginEnabled(
		String		plugin_id )
	{
		return( !disabled.contains( plugin_id));
	}
	
	public void
	setApplicationName(
		String		name )
	{
		SystemProperties.setApplicationName( name );
	}
	
	public String
	getApplicationName()
	{
		return( SystemProperties.getApplicationName());
	}
	
	public void
	setApplicationIdentifier(
		String		id )
	{
		SystemProperties.setApplicationIdentifier( id );
	}
	
	public String
	getApplicationIdentifier()
	{
		return( SystemProperties.getApplicationIdentifier());
	}
	
	public void
	setApplicationEntryPoint(
		String		ep )
	{
		SystemProperties.setApplicationEntryPoint( ep );
	}
	
	public String
	getApplicationEntryPoint()
	{
		return( SystemProperties.getApplicationEntryPoint());
	}
	
	public void
	setSingleInstanceHandler(
		int									single_instance_port,
		PluginManagerArgumentHandler		handler )
	{
		PluginSingleInstanceHandler.initialise( single_instance_port, handler );
	}
}
