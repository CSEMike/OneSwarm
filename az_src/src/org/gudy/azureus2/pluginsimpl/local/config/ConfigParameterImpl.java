/*
 * Created on 30-Aug-2004
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

package org.gudy.azureus2.pluginsimpl.local.config;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.plugins.config.*;

public class 
ConfigParameterImpl
	implements ConfigParameter, ParameterListener
{
	protected String			key;
	
	protected List				listeners	= new ArrayList();
	
	public
	ConfigParameterImpl(
		String						_key )
	{
		key		= _key;
	}
	
	public void 
	parameterChanged(
		String parameterName)
	{
		for ( int i=0;i<listeners.size();i++){
			
			((ConfigParameterListener)listeners.get(i)).configParameterChanged( this );
		}
	}
	
	public void
	addConfigParameterListener(
		ConfigParameterListener	l )
	{
		listeners.add(l);
		
		if ( listeners.size() == 1 ){
			
			COConfigurationManager.addParameterListener( key, this );
		}		
	}
		
	public void
	removeConfigParameterListener(
		ConfigParameterListener	l )
	{
		listeners.remove(l);
		
		if ( listeners.size() == 0 ){
			
			COConfigurationManager.removeParameterListener( key, this );
		}		
	}
}
