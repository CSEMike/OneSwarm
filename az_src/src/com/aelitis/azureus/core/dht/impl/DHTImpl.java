/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.impl;

import java.io.*;
import java.util.Properties;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.control.*;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherFactory;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionManager;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTester;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterFactory;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTImpl 
	implements DHT
{
	private DHTStorageAdapter		storage_adapter;
	private DHTNATPuncherAdapter	nat_adapter;
	private DHTControl			control;
	private DHTNATPuncher		nat_puncher;
	private DHTSpeedTester		speed_tester;
	private	Properties			properties;
	private DHTLogger			logger;
	
	public 
	DHTImpl(
		DHTTransport			_transport,
		Properties				_properties,
		DHTStorageAdapter		_storage_adapter,
		DHTNATPuncherAdapter	_nat_adapter,
		DHTLogger				_logger )
	{		
		properties		= _properties;
		storage_adapter	= _storage_adapter;
		nat_adapter		= _nat_adapter;
		logger			= _logger;
		
		DHTNetworkPositionManager.initialise( storage_adapter );
		
		DHTLog.setLogger( logger );
		
		int		K 		= getProp( PR_CONTACTS_PER_NODE, 			DHTControl.K_DEFAULT );
		int		B 		= getProp( PR_NODE_SPLIT_FACTOR, 			DHTControl.B_DEFAULT );
		int		max_r	= getProp( PR_MAX_REPLACEMENTS_PER_NODE, 	DHTControl.MAX_REP_PER_NODE_DEFAULT );
		int		s_conc 	= getProp( PR_SEARCH_CONCURRENCY, 			DHTControl.SEARCH_CONCURRENCY_DEFAULT );
		int		l_conc 	= getProp( PR_LOOKUP_CONCURRENCY, 			DHTControl.LOOKUP_CONCURRENCY_DEFAULT );
		int		o_rep 	= getProp( PR_ORIGINAL_REPUBLISH_INTERVAL, 	DHTControl.ORIGINAL_REPUBLISH_INTERVAL_DEFAULT );
		int		c_rep 	= getProp( PR_CACHE_REPUBLISH_INTERVAL, 	DHTControl.CACHE_REPUBLISH_INTERVAL_DEFAULT );
		int		c_n 	= getProp( PR_CACHE_AT_CLOSEST_N, 			DHTControl.CACHE_AT_CLOSEST_N_DEFAULT );
		
		control = DHTControlFactory.create( 
				new DHTControlAdapter()
				{
					public DHTStorageAdapter
					getStorageAdapter()
					{
						return( storage_adapter );
					}
					
					public boolean
					isDiversified(
						byte[]		key )
					{
						if ( storage_adapter == null ){
							
							return( false );
						}
						
						return( storage_adapter.isDiversified( key ));
					}
					
					public byte[][]
					diversify(
						String				description,
						DHTTransportContact	cause,
						boolean				put_operation,
						boolean				existing,
						byte[]				key,
						byte				type,
						boolean				exhaustive,
						int					max_depth )
					{
						boolean	valid;
						
						if ( existing ){
							
							valid =	 	type == DHT.DT_FREQUENCY ||
										type == DHT.DT_SIZE ||
										type == DHT.DT_NONE;
						}else{
							
							valid = 	type == DHT.DT_FREQUENCY ||
										type == DHT.DT_SIZE;
						}
						
						if ( storage_adapter != null && valid ){
							
							if ( existing ){
								
								return( storage_adapter.getExistingDiversification( key, put_operation, exhaustive, max_depth ));
								
							}else{
								
								return( storage_adapter.createNewDiversification( description, cause, key, put_operation, type, exhaustive, max_depth ));
							}
						}else{
							
							if ( !valid ){
								
								Debug.out( "Invalid diversification received: type = " + type );
							}
							
							if ( existing ){
								
								return( new byte[][]{ key });
								
							}else{
								
								return( new byte[0][] );
							}
						}
					}
				},
				_transport, 
				K, B, max_r,
				s_conc, l_conc, 
				o_rep, c_rep, c_n,
				logger );
		
		if ( nat_adapter != null ){
			
			nat_puncher	= DHTNATPuncherFactory.create( nat_adapter, this );
		}
		
		speed_tester = DHTSpeedTesterFactory.create( this );
	}
	
	protected int
	getProp(
		String		name,
		int			def )
	{
		Integer	x = (Integer)properties.get(name);
		
		if ( x == null ){
			
			properties.put( name, new Integer( def ));
			
			return( def );
		}
		
		return( x.intValue());
	}
	
	public int
	getIntProperty(
		String		name )
	{
		return(((Integer)properties.get(name)).intValue());
	}
	
	public boolean
	isDiversified(
		byte[]		key )
	{
		return( control.isDiversified( key ));
	}
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		DHTOperationListener	listener )
	{
		control.put( key, description, value, flags, (byte)0, DHT.REP_FACT_DEFAULT, true, listener );
	}
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		boolean					high_priority,
		DHTOperationListener	listener )
	{
		control.put( key, description, value, flags, (byte)0, DHT.REP_FACT_DEFAULT, high_priority, listener );
	}
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		byte					life_hours,
		boolean					high_priority,
		DHTOperationListener	listener )
	{
		control.put( key, description, value, flags, life_hours, DHT.REP_FACT_DEFAULT, high_priority, listener );
	}
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		byte					life_hours,
		byte					replication_control,
		boolean					high_priority,
		DHTOperationListener	listener )
	{
		control.put( key, description, value, flags, life_hours, replication_control, high_priority, listener );
	}
	
	public DHTTransportValue
	getLocalValue(
		byte[]		key )
	{
		return( control.getLocalValue( key ));
	}
		
	public void
	get(
		byte[]					key,
		String					description,
		byte					flags,
		int						max_values,
		long					timeout,
		boolean					exhaustive,
		boolean					high_priority,
		DHTOperationListener	listener )
	{
		control.get( key, description, flags, max_values, timeout, exhaustive, high_priority, listener );
	}
		
	public byte[]
	remove(
		byte[]					key,
		String					description,
		DHTOperationListener	listener )
	{
		return( control.remove( key, description, listener ));
	}
	
	public byte[]
	remove(
		DHTTransportContact[]	contacts,
		byte[]					key,
		String					description,
		DHTOperationListener	listener )
	{
		return( control.remove( contacts, key, description, listener ));
	}
	
	public DHTTransport
	getTransport()
	{
		return( control.getTransport());
	}
	
	public DHTRouter
	getRouter()
	{
		return( control.getRouter());
	}
	
	public DHTControl
	getControl()
	{
		return( control );
	}
	
	public DHTDB
	getDataBase()
	{
		return( control.getDataBase());
	}
	
	public DHTNATPuncher
	getNATPuncher()
	{
		return( nat_puncher );
	}
	
	public DHTSpeedTester
	getSpeedTester()
	{
		return( speed_tester );
	}
	
	public DHTStorageAdapter
	getStorageAdapter()
	{
		return( storage_adapter );
	}
	
	public void
	integrate(
		boolean		full_wait )
	{
		control.seed( full_wait );	
		
		if ( nat_puncher!= null ){
			
			nat_puncher.start();
		}
	}
	
	public void
	destroy()
	{
		if ( nat_puncher != null ){
			
			nat_puncher.destroy();
		}
		
		DHTNetworkPositionManager.destroy( storage_adapter );
	}
	
	public void
	exportState(
		DataOutputStream	os,
		int					max )
	
		throws IOException
	{	
		control.exportState( os, max );
	}
	
	public void
	importState(
		DataInputStream		is )
	
		throws IOException
	{	
		control.importState( is );
	}
	
	public void
	setLogging(
		boolean	on )
	{
		DHTLog.setLogging( on );
	}
	
	public DHTLogger
	getLogger()
	{
		return( logger );
	}
	
	public void
	print(
		boolean	full )
	{
		control.print( full );
	}
}
