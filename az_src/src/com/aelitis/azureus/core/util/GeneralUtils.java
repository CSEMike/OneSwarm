/*
 * Created on Jun 9, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class 
GeneralUtils 
{
		/**
		 * string.replaceAll does \\ and $ expansion in replacement, this doesn't, in fact it
		 * doesn't do any pattern matching at all, it is a literal replacement
		 * @param str
		 * @param from_str		= NOTE, no regex support
		 * @param replacement
		 * @return
		 */

        // XXX: This doesn't appear to be used...
    public static String
	replaceAll(
		String	str,
		String	from_str,
		String	replacement )
	{
		StringBuffer	res = null;
				
		int	pos = 0;
		
		while( true ){
		
			int	p1 = str.indexOf( from_str, pos );
			
			if ( p1 == -1 ){
				
				if ( res == null ){
				
					return( str );
				}
				
				res.append( str.substring( pos ));
				
				return( res.toString());
				
			}else{
				
				if ( res == null ){
					
					res = new StringBuffer( str.length() * 2 );
				}
				
				if ( p1 > pos ){
					
					res.append( str.substring( pos, p1 ));
				}
				
				res.append( replacement );
				
				pos = p1 + from_str.length();
			}
		}
	}
	
	/**
	 * as above but does safe replacement of multiple strings (i.e. a match in the replacement
	 * of one string won't be substituted by another)
	 * @param str
	 * @param from_strs
	 * @param to_strs
	 * @return
	 */
	public static String
	replaceAll(
		String		str,
		String[]	from_strs,
		String[]	to_strs )
	{
		StringBuffer	res = null;
				
		int	pos = 0;
		
		while( true ){
		
			int	min_match_pos 	= Integer.MAX_VALUE;
			int	match_index		= -1;
			
			for ( int i=0;i<from_strs.length;i++ ){
			
				int	pt = str.indexOf( from_strs[i], pos );
				
				if ( pt != -1 ){
					
					if ( pt < min_match_pos ){
						
						min_match_pos		= pt;
						match_index			= i;
					}
				}
			}
			
			if ( match_index == -1 ){
				
				if ( res == null ){
				
					return( str );
				}
				
				res.append( str.substring( pos ));
				
				return( res.toString());
				
			}else{
				
				if ( res == null ){
					
					res = new StringBuffer( str.length() * 2 );
				}
				
				if ( min_match_pos > pos ){
					
					res.append( str.substring( pos, min_match_pos ));
				}
				
				res.append( to_strs[match_index] );
				
				pos = min_match_pos + from_strs[match_index].length();
			}
		}
	}
	
	private final static String REGEX_URLHTML = "<A HREF=\"(.+?)\">(.+?)</A>";
	public static String stripOutHyperlinks(String message) {
		return Pattern.compile(REGEX_URLHTML, Pattern.CASE_INSENSITIVE).matcher(
				message).replaceAll("$2");
	}
	
		/**
		 * splits space separated tokens respecting quotes (either " or ' )
		 * @param str
		 * @return
		 */
		
	public static String[]
	splitQuotedTokens(
		String		str )
	{
		List<String>	bits = new ArrayList<String>();
		
		char	quote 				= ' ';
		boolean	escape 				= false;
		boolean	bit_contains_quotes = false;
			
		String	bit = "";
		
		char[] chars = str.toCharArray();
		
		for (int i=0;i<chars.length;i++){
			
			char c = chars[i];
			
			if ( Character.isWhitespace(c)){
				
				c = ' ';
			}
			
			if ( escape ){
				
				bit += c;
				
				escape = false;
				
				continue;
				
			}else if ( c == '\\' ){
				
				escape = true;
				
				continue;
			}
			
			if ( c == '"' || c == '\'' && ( i == 0 || chars[ i-1 ] != '\\' )){
				
				if ( quote == ' ' ){
						
					bit_contains_quotes = true;
					
					quote = c;
					
				}else if ( quote == c ){
										
					quote = ' ';
					
				}else{
					
					bit += c;
				}
			}else{
				
				if ( quote == ' ' ){
					
					if ( c == ' ' ){
						
						if ( bit.length() > 0 || bit_contains_quotes ){
							
							bit_contains_quotes = false;
							
							bits.add( bit );
							
							bit = "";
						}
					}else{
					
						bit += c;
					}
				}else{
					
					bit += c;
				}
			}
		}	
		
		if ( quote != ' ' ){
			
			bit += quote;
		}
		
		if ( bit.length() > 0 || bit_contains_quotes ){
			
			bits.add( bit );
		}
		
		return( bits.toArray( new String[bits.size()]));
	}
}
