/*
 * Created on Oct 19, 2010
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


package org.gudy.azureus2.pluginsimpl.local.ui.config;

import java.lang.ref.WeakReference;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

public class 
ConfigSectionHolder
	implements ConfigSection
{
	private ConfigSection					section;
	private WeakReference<PluginInterface>	pi;
	protected
	ConfigSectionHolder(
		ConfigSection		_section,
		PluginInterface		_pi )
	{
		section		= _section;
		
		if ( _pi != null ){
			
			pi = new WeakReference<PluginInterface>( _pi );
		}
	}
	
	public String 
	configSectionGetParentSection()
	{
		return( section.configSectionGetParentSection());
	}

	public String 
	configSectionGetName()
	{
		return( section.configSectionGetName());	
	}

	public void 
	configSectionSave()
	{
		section.configSectionSave();
	}

	public void 
	configSectionDelete()
	{
		section.configSectionDelete();
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( pi==null?null:pi.get());
	}
}
