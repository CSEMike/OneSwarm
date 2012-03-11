/*
 * Created on 17-Jun-2004
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

package org.gudy.azureus2.pluginsimpl.local.ui.config;

import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;

/**
 * @author parg
 *
 */

public class 
ActionParameterImpl 
	extends ParameterImpl
	implements ActionParameter
{
	private String	action_resource;
	private int		style	= STYLE_BUTTON;
	
	public 
	ActionParameterImpl(
		PluginConfigImpl 	config,
		String 			label_resource_name,
		String			action_resource_name )
	{ 
		super( config, label_resource_name, label_resource_name);
		
		action_resource	= action_resource_name;
	}
	
	public String
	getActionResource()
	{
		return( action_resource );
	}
	
	public void
	setStyle(
		int		_style )
	{
		style	= _style;
	}
	
	public int
	getStyle()
	{
		return( style );
	}
}
