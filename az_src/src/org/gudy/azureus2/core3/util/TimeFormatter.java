/*
 * Created on 27 juin 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.core3.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Olivier
 * 
 */
public class TimeFormatter {
  // XXX should be i18n'd
	static final String[] TIME_SUFFIXES = { "s", "m", "h", "d", "y" };

	
	private static final SimpleDateFormat http_date_format = 
		new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US );

	static{
			// see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
		
		http_date_format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	/**
	 * Format time into two time sections, the first chunk trimmed, the second
	 * with always with 2 digits.  Sections are *d, **h, **m, **s.  Section
	 * will be skipped if 0.   
	 * 
	 * @param time time in seconds
	 * @return Formatted time string
	 */
	public static String format(long time) {
		if (time == Constants.INFINITY_AS_INT || time >= Constants.INFINITE_AS_LONG)
			return Constants.INFINITY_STRING;

		if (time < 0)
			return "";

		// secs, mins, hours, days
		int[] vals = {
			(int) time % 60,
			(int) (time / 60) % 60,
			(int) (time / 3600) % 24,
			(int) (time / 86400) % 365,
			(int) (time / 31536000)
			};

		int end = vals.length - 1;
		while (vals[end] == 0 && end > 0) {
			end--;
		}
		
		String result = vals[end] + TIME_SUFFIXES[end];

		// skip until we have a non-zero time section
		do {
			end--;
		} while (end >= 0 && vals[end] == 0);
		
		if (end >= 0)
			result += " " + twoDigits(vals[end]) + TIME_SUFFIXES[end];

		return result;
	}

	/**
	 * Format time into "[[# y] # d] 00:00:00" format
	 * 
	 * @param time time in seconds
	 * @return
	 */
    public static String formatColon(long time)
    {
      if (time == Constants.INFINITY_AS_INT || time >= Constants.INFINITE_AS_LONG) return Constants.INFINITY_STRING;
      if (time < 0) return "";

      int secs = (int) time % 60;
      int mins = (int) (time / 60) % 60;
      int hours = (int) (time /3600) % 24;
      int days = (int) (time / 86400) % 365;
      int years = (int) (time / 31536000);
      
      String result = "";
      if (years > 0) result += years + "y ";
      if (years > 0 || days > 0) result += days + "d ";
      result += twoDigits(hours) + ":" + twoDigits(mins) + ":" + twoDigits(secs);

      return result;
    }
    
    private static String twoDigits(int i) {
      return (i < 10) ? "0" + i : String.valueOf(i);
    }
    
    	/**
    	 * parse time in h:m:s format to SECONDS
    	 * @param str
    	 * @return
    	 */
    
    public static int
    parseColon(
    	String	str )
    {
    	final int[]	multipliers = { 1, 60, 3600, 86400, 31536000 };
    	
    	String[]	bits = str.split( ":" );
    	
    	int	result = 0;
    	
    	for (int i=0;i<bits.length;i++){
    		
    		String bit = bits[bits.length-(i+1)].trim();
    		
    		if ( bit.length() > 0 ){
    			
    			result += multipliers[i] * Integer.parseInt( bit );
    		}
    	}
    	
    	return( result );
    }
    
    public static String
    formatNanoAsMilli(
    	long	nanos )
    {
    	final long truncator = 60*1000000000L;
    	
    	nanos = nanos - ((nanos/truncator) * truncator);
    	
    	return( String.valueOf(((double)nanos)/1000000) + " ms" );
    }
    
    public static String
    getHTTPDate(
    	long		millis )
    {
		synchronized( http_date_format ){
			
			return( http_date_format.format(new Date( millis )));
		}
    }
    
    public static String
    milliStamp()
    {
    	long nanos = SystemTime.getHighPrecisionCounter();
    	
    	final long truncator = 60*1000000000L;
    	
    	nanos = nanos - ((nanos/truncator) * truncator);

    	String	str = String.valueOf( nanos/1000000 );
    	
    	while( str.length() < 5 ){
    		
    		str = "0" + str;
    	}
    	
    	return( str + ": " );
    }
    
    public static void
    milliTrace(
    	String	str )
    {
    	System.out.println( milliStamp() + str );
    }
}
