/*
 * Created on 11-Jan-2005
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

package com.aelitis.azureus.core.dht.router;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.router.impl.*;

public class 
DHTRouterFactory 
{
	private static final List	observers = new ArrayList();
	
	public static DHTRouter
	create(
		int							K,
		int							B,
		int							max_rep_per_node,
		byte[]						id,
		DHTRouterContactAttachment	attachment,
		DHTLogger					logger )
	{
		DHTRouterImpl	res = new DHTRouterImpl( K, B, max_rep_per_node, id, attachment, logger );
		
		for( int i=0;i<observers.size();i++){
			
			try{
				((DHTRouterFactoryObserver)observers.get(i)).routerCreated( res );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( res );
	}
	
	public static void
	addObserver(
		DHTRouterFactoryObserver	observer )
	{
		observers.add( observer );
	}
	
	public static void
	removeObserver(
		DHTRouterFactoryObserver	observer )
	{
		observers.remove( observer );
	}
}
