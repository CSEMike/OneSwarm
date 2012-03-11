/*
 * Created on 16-Jan-2005
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


/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
DHTLog 
{
	public static final boolean		GLOBAL_BLOOM_TRACE		= false;
	public static final boolean		LOCAL_BLOOM_TRACE		= false;
	public static final boolean		CONTACT_VERIFY_TRACE	= false;
	public static final boolean		TRACE_VERSIONS 			= false;
	
	static{
		if ( GLOBAL_BLOOM_TRACE ){
			System.out.println( "**** DHTLog: global bloom trace on ****" );
		}
		if ( LOCAL_BLOOM_TRACE ){
			System.out.println( "**** DHTLog: local bloom trace on ****" );
		}
		if ( CONTACT_VERIFY_TRACE ){
			System.out.println( "**** DHTLog: contact verify trace on ****" );
		}
		if ( TRACE_VERSIONS ){
			System.out.println( "**** DHTTransportStats: tracing protocol versions ****" );
		}
	}
	
	
	public static boolean	logging_on	= false;
			
	private static DHTLogger	logger;
	
	protected static void
	setLogging(
		boolean	on )
	{
		logging_on 	= on;
	}
	
	public static boolean
	isOn()
	{
		return( logging_on );
	}

	public static void
	log(
		String	str )
	{
		if ( logging_on ){
						
			if ( logger != null ){
				
				logger.log( str );
				
			}else{
				
				System.out.println( str );
			}
		}
	}
	
	public static void
	setLogger(
		DHTLogger l )
	{
		logger	= l;
	}
	
	
	public static String
	getString(
		byte[]	b )
	{
		if ( logging_on ){
			
			return( getString2(b));
	
			
		}else{
			
			return( "" );
		}
	}
	
	public static String
	getString2(
		byte[]	b )
	{
		String res = ByteFormatter.nicePrint(b);
		
		if ( res.length() > 8 ){
			
			res = res.substring(0,8)+"...";
		}
		
		return( res );
	}
	
	public static String
	getFullString(
		byte[]	b )
	{
		return( ByteFormatter.nicePrint(b));
	}
	
	public static String
	getString(
		HashWrapper	w )
	{
		if ( logging_on ){
			
			return( getString( w.getHash()));
			
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		DHTTransportContact[]	contacts )
	{
		if ( logging_on ){
			
			String	res = "{";
			
			for (int i=0;i<contacts.length;i++){
				
				res += (i==0?"":",") + getString(contacts[i].getID());
			}
			
			return( res + "}" );
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		DHTTransportContact	contact )
	{
		if ( logging_on ){
			return( contact.getString());
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		List		l )
	{
		if ( logging_on ){
			String	res = "{";
			
			for (int i=0;i<l.size();i++){
				
				res += (i==0?"":",") + getString((DHTTransportContact)l.get(i));
			}
			
			return( res + "}" );
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		Set			s )
	{
		if ( logging_on ){
			String	res = "{";
			
			Iterator it = s.iterator();
			
			while( it.hasNext()){
				
				res += (res.length()==1?"":",") + getString((DHTTransportContact)it.next());
			}
			
			return( res + "}" );
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		Map			s )
	{
		if ( logging_on ){
			String	res = "{";
			
			Iterator it = s.keySet().iterator();
			
			while( it.hasNext()){
				
				res += (res.length()==1?"":",") + getString((HashWrapper)it.next());
			}
			
			return( res + "}" );	
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		DHTTransportValue[]	values )
	{
		if ( logging_on ){
			
			if ( values == null ){
				
				return( "<null>");
			}
			
			String	res = "";
			
			for (int i=0;i<values.length;i++){
				
				res += (i==0?"":",") + getString( values[i] );
			}
			return( res );
		}else{
			return( "" );
		}
	}	
	
	public static String
	getString(
		DHTTransportValue	value )
	{
		if ( logging_on ){
			
			if ( value == null ){
				
				return( "<null>");
			}
			
			return( getString( value.getValue()) + " <" + (value.isLocal()?"loc":"rem" ) + ",flag=" + Integer.toHexString(value.getFlags()) + ",life=" + value.getLifeTimeHours() + ",rep=" + Integer.toHexString( value.getReplicationControl())+",orig=" + value.getOriginator().getExternalAddress() +">" );
		}else{
			return( "" );
		}
	}
}
