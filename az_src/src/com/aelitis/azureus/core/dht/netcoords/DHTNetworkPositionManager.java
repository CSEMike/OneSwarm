/*
 * Created on 24-Apr-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.dht.netcoords;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.impl.DHTLog;

public class 
DHTNetworkPositionManager 
{
	private static DHTNetworkPositionProvider[]	providers = new DHTNetworkPositionProvider[0];
	
	private static DHTStorageAdapter	storage_adapter = null;
	
	public static void
	initialise(
		DHTStorageAdapter		adapter )
	{
		synchronized( providers ){
			
			if ( storage_adapter == null ){
				
				storage_adapter	= adapter;
				
				for (int i=0;i<providers.length;i++){
				
					DHTNetworkPositionProvider	provider = providers[i];

					try{
						startUp( provider );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			}
		}
	}
	
	private static void
	startUp(
		DHTNetworkPositionProvider	provider )
	{
		byte[] data = storage_adapter.getStorageForKey( "NPP:" + provider.getPositionType());
		
		if ( data == null ){
			
			data = new byte[0];
		}
		
		try{
			provider.startUp( new DataInputStream( new ByteArrayInputStream( data )));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public static void
	destroy(
		DHTStorageAdapter		adapter )
	{
		synchronized( providers ){
			
			if ( storage_adapter == adapter ){

				for (int i=0;i<providers.length;i++){

					try{
						DHTNetworkPositionProvider	provider = providers[i];
						
						ByteArrayOutputStream	baos = new ByteArrayOutputStream();
						
						DataOutputStream	dos = new DataOutputStream( baos );
						
						provider.shutDown( dos );
						
						dos.flush();
						
						byte[]	data = baos.toByteArray();
						
						storage_adapter.setStorageForKey( "NPP:" + provider.getPositionType(), data );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
				
				storage_adapter	= null;
			}
		}
	}
	
	public static DHTNetworkPositionProviderInstance
	registerProvider(
		final DHTNetworkPositionProvider	provider )
	{
		synchronized( providers ){
	
			DHTNetworkPositionProvider[]	p = new DHTNetworkPositionProvider[providers.length + 1 ];
			
			System.arraycopy( providers, 0, p, 0, providers.length );
			
			p[providers.length] = provider;
			
			providers	= p;
			
			if ( storage_adapter != null ){
			
				startUp( provider );
			}
		}
		
		return( new DHTNetworkPositionProviderInstance()
				{	
					public void
					log(
						String		log )
					{
						DHTLog.log("NetPos " + provider.getPositionType() + ": " + log );
					}
				});
	}
	
	public static DHTNetworkPositionProvider
	getProvider(
		byte		type )
	{
		synchronized( providers ){

			for (int i=0;i<providers.length;i++){
				
				if ( providers[i].getPositionType() == type ){
					
					return( providers[i] );
				}
			}
		}
		
		return( null );
	}
	
	public static DHTNetworkPosition[]
	getLocalPositions()
	{
		DHTNetworkPositionProvider[]	prov = providers;
		
		List res = new ArrayList();
		
		for (int i=0;i<prov.length;i++){
			
			try{
				DHTNetworkPosition	pos = prov[i].getLocalPosition();
				
				if ( pos != null ){
					
					res.add( pos );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return((DHTNetworkPosition[])res.toArray(new DHTNetworkPosition[res.size()])); 

	}
	
	public static DHTNetworkPosition
	getBestLocalPosition()
	{
		DHTNetworkPosition	best_position = null;
		
		DHTNetworkPosition[]	positions = getLocalPositions();
		
		byte	best_provider = DHTNetworkPosition.POSITION_TYPE_NONE;

		for (int i=0;i<positions.length;i++){
			
			DHTNetworkPosition	position = positions[i];
			
			int	type = position.getPositionType();
			
			if ( type > best_provider ){
				
				best_position = position;
			}
		}
		
		return( best_position );
	}
	
	public static DHTNetworkPosition[]
	createPositions(
		byte[]		ID,
		boolean		is_local )
	{
		DHTNetworkPositionProvider[]	prov = providers;
		
		DHTNetworkPosition[]	res = new DHTNetworkPosition[prov.length];
		
		int	skipped	= 0;
		
		for (int i=0;i<res.length;i++){
			
			try{
				res[i] = prov[i].create( ID, is_local );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				skipped++;
			}
		}
		
		if  ( skipped > 0 ){
			
			DHTNetworkPosition[] x	= new DHTNetworkPosition[ res.length - skipped ];
			
			int	pos = 0;
			
			for (int i=0;i<res.length;i++){
				
				if ( res[i] != null ){
					
					x[pos++] = res[i];
				}
			}
			
			res	= x;
			
			if ( res.length == 0 ){
				
				Debug.out( "hmm" );
			}
		}
		
		return( res );
	}
	
	public static float
	estimateRTT(
		DHTNetworkPosition[]		p1s,
		DHTNetworkPosition[]		p2s )
	{
		byte	best_provider = DHTNetworkPosition.POSITION_TYPE_NONE;
		
		float	best_result	= Float.NaN;
		
		for (int i=0;i<p1s.length;i++){
			
			DHTNetworkPosition	p1 = p1s[i];
			
			byte	p1_type = p1.getPositionType();
			
			for (int j=0;j<p2s.length;j++){
				
				DHTNetworkPosition	p2 = p2s[j];
				
				if ( p1_type == p2.getPositionType()){
					
					try{
						float	f = p1.estimateRTT( p2 );
						
						if ( !Float.isNaN( f )){
							
							if ( p1_type > best_provider ){
								
								best_result		= f;
								best_provider	= p1_type;
							}
						}
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
					
					break;
				}
			}
		}
		
		return( best_result );
	}
	
	public static void
	update(
		DHTNetworkPosition[]	local_positions,
		byte[]					remote_id,
		DHTNetworkPosition[]	remote_positions,
		float					rtt )
	{	
		for (int i=0;i<local_positions.length;i++){
			
			DHTNetworkPosition	p1 = local_positions[i];
						
			for (int j=0;j<remote_positions.length;j++){
				
				DHTNetworkPosition	p2 = remote_positions[j];
				
				if ( p1.getPositionType() == p2.getPositionType()){
					
					try{
						p1.update( remote_id, p2, rtt );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
					
					break;
				}
			}
		}
	}
	
	public static byte[]
	serialisePosition(
		DHTNetworkPosition	pos )
	
		throws IOException
	{
		ByteArrayOutputStream	baos = new ByteArrayOutputStream();
		
		DataOutputStream	dos = new DataOutputStream( baos );
	
		dos.writeByte( 1 );	// version
		dos.writeByte( pos.getPositionType());
		
		pos.serialise( dos );
		
		dos.close();
		
		return( baos.toByteArray());
	}
	
	public static DHTNetworkPosition
   	deserialisePosition(
   		byte[]		bytes )
   	
   		throws IOException
   	{
   		ByteArrayInputStream	bais = new ByteArrayInputStream( bytes );
   		
   		DataInputStream	dis = new DataInputStream( bais );
   	
   		dis.readByte();	// version
   		
   		byte	position_type = dis.readByte();
   		
   		return( deserialise( position_type, dis ));
   	}
	
	public static DHTNetworkPosition
	deserialise(
		byte			position_type,
		DataInputStream	is )
	
		throws IOException
	{
		DHTNetworkPositionProvider[]	prov = providers;

		is.mark( 512 );

		for (int i=0;i<prov.length;i++){
			
			if ( prov[i].getPositionType() == position_type ){
				
				DHTNetworkPositionProvider	provider = prov[i];
				
				try{
					DHTNetworkPosition np = provider.deserialisePosition( is );
					
					// System.out.println( "Deserialised: " + np.getPositionType());
					
					return( np );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
					
					is.reset();
				}
				
				break;
			}
		}
		
		return( null );
	}
}