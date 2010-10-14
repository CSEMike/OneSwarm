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

package org.gudy.azureus2.core3.html.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.html.*;


public class 
HTMLChunkImpl 
{
	String		content;
	
	protected
	HTMLChunkImpl()
	{
	}

	protected
	HTMLChunkImpl(
		String		_content )
	{
		content	= _content;
	}
	
	protected void
	setContent(
		String		str )
	{
		content	= str;
	}
	
	public String[]
	getLinks()
	{
		int	pos	= 0;

		List	res = new ArrayList();
		
		while(true){
			
			int	p1 = content.indexOf( "<", pos );
			
			if ( p1 == -1 ){
				
				break;
			}
			
			p1++;
			
			int	p2 = content.indexOf( ">", p1 );
			
			if ( p2 == -1 ){
				
				break;
			}
			
			pos	= p2;
			
			String	tag 	= content.substring( p1, p2 ).trim();
			
			String	lc_tag 	= tag.toLowerCase();
						
			if ( lc_tag.startsWith("a " )){
				
				int	hr_start = lc_tag.indexOf( "href");
				
				if ( hr_start == -1 ){
					
					continue;
				}
				
				hr_start = lc_tag.indexOf("=", hr_start);
				
				if ( hr_start == -1 ){
					
					continue;
				}
				
				hr_start += 1;
				
				while( 	hr_start < lc_tag.length() &&
						Character.isWhitespace(lc_tag.charAt(hr_start))){
					
					hr_start++;
				}
				
				int hr_end = hr_start;
				
				while(	hr_end < lc_tag.length() &&
						!Character.isWhitespace(lc_tag.charAt(hr_end))){
										
					hr_end++;
				}
				
				String	href = tag.substring(hr_start, hr_end ).trim();
				
				if ( href.startsWith("\"")){
					
					href = href.substring(1,href.length()-1);
				}
				
				res.add( href );
			}
		}
		
		String[]	res_array = new String[res.size()];
		
		res.toArray( res_array );
		
		return( res_array );
	}
	
	public HTMLTable[]
	getTables()
	{
		String[]	tables = getTagPairContent( "table" );
		
		HTMLTable[]	res = new HTMLTable[tables.length];
		
		for (int i=0;i<tables.length;i++){
			
			res[i] = new HTMLTableImpl( tables[i] );
		}
		
		return( res );
	}
	
		/**
		 * this just returns the tags themselves.
		 * @param tag
		 * @return
		 */
	
	public String[]
	getTags(
		String	tag_name )
	{
		tag_name = tag_name.toLowerCase();
							
		String	lc_content = content.toLowerCase();
							
		int	pos	= 0;

		List	res = new ArrayList();
														
		while(true){
			
			int	p1 = lc_content.indexOf( "<" + tag_name,  pos );
			
			if ( p1 == -1 ){
				
				break;
			}
			
			int	p2 = lc_content.indexOf( ">", p1 );
			
			if ( p2 == -1 ){
				
				break;
			}

			res.add( content.substring( p1+1, p2 ));
			
			pos	= p2+1;
		}
		
		String[]	x = new String[res.size()];
		
		res.toArray( x );
		
		return( x );
	}
	
	public String[]
	getTagPairContent(
		String	tag_name )
	{
		tag_name = tag_name.toLowerCase();
		
		String	lc_content = content.toLowerCase();
		
		int	pos	= 0;

		List	res = new ArrayList();
		
		int	level 		= 0;
		int	start_pos	= -1;
		
		while(true){
			
			int	start_tag_start = lc_content.indexOf( "<" + tag_name,  pos );
			int end_tag_start	= lc_content.indexOf( "</" + tag_name, pos );
			
			if ( level == 0 ){
				
				if ( start_tag_start == -1 ){
					
					break;
				}
				
				start_pos = start_tag_start;
				
				level	= 1;
				
				pos		= start_pos+1;
				
			}else{
				
				if ( end_tag_start == -1 ){
					
					break;
				}
				
				if ( start_tag_start == -1 || end_tag_start < start_tag_start ){
					
					if ( level == 1 ){
						
						String	tag_contents = content.substring( start_pos + tag_name.length() + 1, end_tag_start );
						
						res.add( tag_contents );
						
						// System.out.println( "got tag:" + tag_contents );						
					}
					
					level--;
					
					pos	= end_tag_start + 1;
					
				}else{
					
					if ( start_tag_start == -1 ){
						
						break;
					}
					
					level++;
					
					pos = start_tag_start+1;
				}
			}
		}
		
		String[]	res_array = new String[res.size()];
		
		res.toArray( res_array );
		
		return( res_array );
	}
	
	public String
	getContent()
	{
		return( content );
	}
}
