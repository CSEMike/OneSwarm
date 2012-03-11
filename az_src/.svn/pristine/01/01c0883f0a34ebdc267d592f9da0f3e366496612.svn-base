/*
 * Created on Nov 1, 2011
 * Created by Paul Gardner
 * 
 * Copyright 2011 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.util;

import java.util.*;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.Debug;

public class 
RegExUtil 
{
	private static ThreadLocal<Map<String,Object[]>>		tls	= 
		new ThreadLocal<Map<String,Object[]>>()
		{
			public Map<String,Object[]>
			initialValue()
			{
				return( new HashMap<String,Object[]>());
			}
		};
		
	public static Pattern
	getCachedPattern(
		String		namespace,
		String		pattern )
	{	
		return( getCachedPattern( namespace, pattern, 0 ));
	}
	
	public static Pattern
	getCachedPattern(
		String		namespace,
		String		pattern,
		int			flags )
	{		
		Map<String,Object[]> map = tls.get();
		
		Object[] entry = map.get( namespace );
		
		if ( entry == null || !pattern.equals((String)entry[0])){
		
			Pattern result = Pattern.compile( pattern, flags );
			
			map.put( namespace, new Object[]{ pattern, result });
			
			return( result );
			
		}else{
			
			return((Pattern)entry[1]);
		}
	}
	
	public static boolean
	mightBeEvil(
		String	str )
	{
			// http://en.wikipedia.org/wiki/ReDoS
			
		if ( !str.contains( ")" )){
			
			return( false );
		}
		
		char[]	chars = str.toCharArray();
		
		Stack<Integer>	stack = new Stack<Integer>();
		
		for (int i=0;i<chars.length;i++){
			
			char c = chars[i];
			
			if ( c == '(' ){
				
				stack.push( i+1 );
				
			}else if ( c == ')' ){
				
				if ( stack.isEmpty()){
					
					Debug.out( "bracket un-matched in " + str + " - treating as evil" );
					
					return( true );
					
				}else{
					
					int	start = stack.pop();

					if ( i < chars.length-1){
						
						char next = chars[i+1];
						
						if ( next == '*' || next == '+' || next == '{' ){
										
							for ( int j=start;j<i;j++){
								
								c = chars[j];
								
								if ( "+*{|".indexOf( c ) != -1 ){
									
									Debug.out( "regular expression " + str + " might be evil due to '" + str.substring( start-1, i+2) + "'" );

									return( true );
								}
							}
						}
					}
				}
			}
		}
		
		return( false );
	}
}
