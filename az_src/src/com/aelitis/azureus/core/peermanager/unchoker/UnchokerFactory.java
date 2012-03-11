/*
 * Created on Aug 24, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.peermanager.unchoker;

import org.gudy.azureus2.core3.util.Debug;


public class 
UnchokerFactory 
{
	public static final String	DEFAULT_MANAGER = "com.aelitis.azureus.core.peermanager.unchoker.UnchokerFactory";
	
	private static UnchokerFactory	factory = getSingleton( null );
	
	
	public static UnchokerFactory
	getSingleton()
	{
		return( factory );
	}
	
	private static UnchokerFactory
	getSingleton(
		String	explicit_implementation )
	{
		String	impl = explicit_implementation;
		
		if ( impl == null ){
			
			impl = System.getProperty( DEFAULT_MANAGER );
		}
		
		if ( impl == null ){
			
			impl	= DEFAULT_MANAGER;
		}
		
		try{
			Class impl_class = UnchokerFactory.class.getClassLoader().loadClass( impl );
			
			factory = (UnchokerFactory)impl_class.newInstance();
							
		}catch( Throwable e ){
			
			Debug.out( "Failed to instantiate unchoker factory '" + impl + "'", e );
			
			factory = new UnchokerFactory();
		}

		return( factory );
	}
	
	public Unchoker
	getUnchoker(
		boolean	seeding )
	{
		if ( seeding ){
			
			return( new SeedingUnchoker());
			
		}else{
			
			return( new DownloadingUnchoker());
		}
	}
}
