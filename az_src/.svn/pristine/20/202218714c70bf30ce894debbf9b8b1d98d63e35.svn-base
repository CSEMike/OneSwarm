/*
 * Created on 02-Jan-2005
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

package org.gudy.azureus2.pluginsimpl.local.utils.xml.rss;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.gudy.azureus2.core3.util.Debug;

/**
 * @author parg
 *
 */

public class 
RSSUtils 
{
	public static Date
	parseRSSDate(
		String	date_str )
	{
		try{
			// see rfc822 [EEE,] dd MMM yyyy HH:mm::ss z
			// assume 4 digit year
				
			SimpleDateFormat	format;
			
			if ( date_str.indexOf( "," ) == -1 ){
				
				format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.US );
				
			}else{
				
				format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US );
			}
			
			
			return( format.parse( date_str ));
			
		}catch( ParseException e ){
			
			String[]	fallbacks =
			{	
				"dd MMM yyyy HH:mm:ss z",				// As above but laxer
				"EEE dd MMM yyyy HH:mm:ss z",			// As above but laxer
				"EEE MMM dd HH:mm:ss z yyyy",			// Fri Sep 26 00:00:00 EDT 2008
				"EEE MMM dd HH:mm z yyyy",				// Fri Sep 26 00:00 EDT 2008	
				"EEE MMM dd HH z yyyy",					// Fri Sep 26 00 EDT 2008	
				"yyyy-MM-dd HH:mm:ss",					// 2009-02-08 22:56:45	
				"yyyy-MM-dd",							// 2009-02-08	
			};
			
				// remove commas as these keep popping up in silly places
			
			date_str = date_str.replace( ',', ' ' );
			
				// remove duplicate white space
			
			date_str = date_str.replaceAll( "(\\s)+", " " );
			
			for (int i=0;i<fallbacks.length;i++){
				
				try{
					return(  new SimpleDateFormat(fallbacks[i], Locale.US ).parse( date_str ));
					
				}catch( ParseException f ){
				}
			}
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public static Date
	parseAtomDate(
		String	date_str )
	{
			// full-time from http://tools.ietf.org/html/rfc3339 with T and Z
		
		final String[]	formats = {
				"yyyy-MM-dd'T'kk:mm:ss'Z'",
				"yyyy-MM-dd'T'kk:mm:ssz", 
				"yyyy-MM-dd'T'kk:mm:ssZ", 
				"yyyy-MM-dd'T'kk:mm:ss" };
		
		for (int i=0;i<formats.length;i++){

			try{
				
				SimpleDateFormat format = new SimpleDateFormat( formats[i], Locale.US );
							
				return( format.parse( date_str ));

			}catch( ParseException e ){
			
				// Debug.printStackTrace(e);
			}
		}
		
		return( null );
	}
}
