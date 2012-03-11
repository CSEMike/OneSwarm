/*
 * Created on 27-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.update.sf;

/**
 * @author parg
 *
 */

public interface 
SFPluginDetailsLoader 
{
	public String[]
	getPluginIDs()
	
		throws SFPluginDetailsException;
	
	public SFPluginDetails
	getPluginDetails(
		String		name )
	
		throws SFPluginDetailsException;
	
	public SFPluginDetails[]
	getPluginDetails()
	
		throws SFPluginDetailsException;
	
	public void
	reset();
	
	public void
	addListener(
		SFPluginDetailsLoaderListener		l );
	
	public void
	removeListener(
		SFPluginDetailsLoaderListener		l );
}
