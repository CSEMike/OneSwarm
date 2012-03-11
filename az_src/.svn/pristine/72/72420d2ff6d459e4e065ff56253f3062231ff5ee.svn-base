/*
 * Created on Feb 3, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.speedmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;

import com.aelitis.azureus.core.AzureusCore;

public class 
SpeedLimitHandler 
{
	private static SpeedLimitHandler		singleton;
	
	public static SpeedLimitHandler
	getSingleton(
		AzureusCore		core )
	{
		synchronized( SpeedLimitHandler.class ){
			
			if ( singleton == null ){
				
				singleton = new SpeedLimitHandler( core );
			}
			
			return( singleton );
		}
	}
	
	private AzureusCore		core;
		
	private TimerEventPeriodic		schedule_event;
	private List<ScheduleRule>		current_rules	= new ArrayList<ScheduleRule>();
	private ScheduleRule			active_rule;
	
	private
	SpeedLimitHandler(
		AzureusCore		_core )
	{
		core 	= _core;
		
		loadSchedule();
	}
	
	private synchronized Map
	loadConfig()
	{
		return( BEncoder.cloneMap( COConfigurationManager.getMapParameter( "speed.limit.handler.state", new HashMap())));
	}
	
	private synchronized void
	saveConfig(
		Map		map )
	{
		COConfigurationManager.setParameter( "speed.limit.handler.state", map );
		
		COConfigurationManager.save();
	}
	
	public List<String>
	reset()
	{
		LimitDetails details = new LimitDetails();
		
		details.loadForReset();
		
		details.apply();
		
		return( details.getString());
	}
	
	public List<String>
	getCurrent()
	{
		LimitDetails details = new LimitDetails();
		
		details.loadCurrent();
		
		List<String> lines = details.getString();
		
		ScheduleRule rule = active_rule;
		
		lines.add( "" );
		lines.add( "Scheduler" );
		lines.add( "    Rules defined: " + current_rules.size());
		lines.add( "    Active rule: " + (rule==null?"None":rule.getString()));
		
		return( lines );
	}
	
	public List<String>
	getProfileNames()
	{
		Map	map = loadConfig();
				
		List<String> profiles = new ArrayList<String>();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	name = importString( m, "n" );
				
				if ( name != null ){
					
					profiles.add( name );
				}
			}
		}

		return( profiles );
	}
	
	public List<String>
	loadProfile(
		String		name )
	{		
		Map	map = loadConfig();
				
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
					
					ld.apply();
					
					return( ld.getString());
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.add( "Profile not found" );

		return( result );
	}
	
	private boolean
	profileExists(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	public List<String>
	getProfile(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
										
					return( ld.getString());
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.add( "Profile not found" );

		return( result );
	}
	
	public List<String>
	getProfilesForDownload(
		byte[]		hash )
	{
		List<String> result = new ArrayList<String>();

		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			String	hash_str = Base32.encode( hash );
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null ){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
										
					if ( ld.getLimitsForDownload( hash_str ) != null ){
						
						result.add( p_name );
					}
				}
			}
		}
	
		return( result );
	}
	
	private void
	addRemoveDownloadsToProfile(
		String			name,
		List<byte[]>	hashes,
		boolean			add )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		List<String>	hash_strs = new ArrayList<String>();
		
		for ( byte[] hash: hashes ){
			
			hash_strs.add( Base32.encode( hash ));
		}
		
		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
					
					ld.addRemoveDownloads( hash_strs, add );
					
					m.put( "p", ld.export());
					
					saveConfig( map );

					return;
				}
			}
		}
	}
	
	public void
	addDownloadsToProfile(
		String			name,
		List<byte[]>	hashes )
	{
		addRemoveDownloadsToProfile( name, hashes, true );
	}
	
	public void
	removeDownloadsFromProfile(
		String			name,
		List<byte[]>	hashes )
	{
		addRemoveDownloadsToProfile( name, hashes, false );
	}
	
	public void
	deleteProfile(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					list.remove( m );
					
					saveConfig( map );
					
					return;
				}
			}
		}
	}
	
	public List<String>
	saveProfile(
		String		name )
	{
		LimitDetails details = new LimitDetails();
		
		details.loadCurrent();
		
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list == null ){
			
			list = new ArrayList<Map>();
			
			map.put( "profiles", list );
		}
		
		for ( Map m: list ){
			
			String	p_name = importString( m, "n" );
			
			if ( p_name != null && name.equals( p_name )){
				
				list.remove( m );
				
				break;
			}
		}
		
		Map m = new HashMap();
		
		list.add( m );
		
		m.put( "n", name );
		m.put( "p", details.export());
		
		saveConfig( map );
		
		ScheduleRule	rule;
		
		synchronized( this ){
			
			rule = active_rule;
		}
		
		if ( rule != null && rule.profile_name.equals( name )){
			
			details.apply();
		}
		
		return( details.getString());
	}
	
	private synchronized List<String>
	loadSchedule()
	{
		List<String>	result = new ArrayList<String>();
		
		List<String> schedule_lines = BDecoder.decodeStrings( COConfigurationManager.getListParameter( "speed.limit.handler.schedule.lines", new ArrayList()));

		boolean	enabled = true;
		
		List<ScheduleRule>	rules = new ArrayList<ScheduleRule>();
		
		for ( String line: schedule_lines ){
			
			line = line.trim();
			
			if ( line.length() == 0 || line.startsWith( "#" )){
				
				continue;
			}
			
			String lc_line = line.toLowerCase( Locale.US );
			
			if ( lc_line.startsWith( "enable" )){
				
				String[]	bits = lc_line.split( "=" );
				
				boolean	ok = false;
				
				if ( bits.length == 2 ){
					
					String arg = bits[1];
					
					if ( arg.equals( "yes" )){
						
						enabled = true;
						ok		= true;
						
					}else if ( arg.equals( "no" )){
						
						enabled = false;
						ok		= true;
					}
				}
				
				if ( !ok ){
					
					result.add( "'" +line + "' is invalid: use enable=(yes|no)" );
				}
			}else{
				
				String[]	_bits = line.split( " " );
							
				List<String>	bits = new ArrayList<String>();
				
				for ( String b: _bits ){
					
					b = b.trim();
					
					if ( b.length() > 0 ){
						
						bits.add( b );
					}
				}
				
				List<String>	errors = new ArrayList<String>();
				
				if ( bits.size() == 6 ){
					
					String	freq_str = bits.get(0).toLowerCase( Locale.US );
					
					byte	freq = 0;
					
					if ( freq_str.equals( "daily" )){
						
						freq = ScheduleRule.FR_DAILY;
						
					}else if ( freq_str.equals( "weekdays" )){
						
						freq = ScheduleRule.FR_WEEKDAY;
						
					}else if ( freq_str.equals( "weekends" )){
						
						freq = ScheduleRule.FR_WEEKEND;
						
					}else if ( freq_str.equals( "mon" )){
						
						freq = ScheduleRule.FR_MON;
						
					}else if ( freq_str.equals( "tue" )){
						
						freq = ScheduleRule.FR_TUE;
						
					}else if ( freq_str.equals( "wed" )){
						
						freq = ScheduleRule.FR_WED;
						
					}else if ( freq_str.equals( "thu" )){
						
						freq = ScheduleRule.FR_THU;
						
					}else if ( freq_str.equals( "fri" )){
						
						freq = ScheduleRule.FR_FRI;
						
					}else if ( freq_str.equals( "sat" )){
						
						freq = ScheduleRule.FR_SAT;
						
					}else if ( freq_str.equals( "sun" )){
						
						freq = ScheduleRule.FR_SUN;
						
					}else{
						
						errors.add( "frequency '" + freq_str + "' is invalid" );
					}
					
					String	profile = bits.get(1);
					
					if ( !profileExists( profile )){
						
						errors.add( "profile '" + profile + "' not found" );
						
						profile = null;
					}
					
					int from_mins = -1;
					
					if ( bits.get(2).equalsIgnoreCase( "from" )){
						
						from_mins = getMins( bits.get(3));
					}
					
					if ( from_mins == -1 ){
						
						errors.add( "'from' is invalid" );
					}
					
					int to_mins = -1;
					
					if ( bits.get(4).equalsIgnoreCase( "to" )){
						
						to_mins = getMins( bits.get(5));
					}
					
					if ( to_mins == -1 ){
						
						errors.add( "'to' is invalid" );
					}
					
					if ( errors.size() == 0 ){
						
						rules.add( new ScheduleRule( freq, profile, from_mins, to_mins ));
						
					}else{
						
						String	err_str = "";
						
						for ( String e: errors ){
							
							err_str += (err_str.length()==0?"":", ") + e;
						}
						
						result.add( "'" + line + "' is invalid (" + err_str + ") - use <frequency> <profile> from <hh:mm> to <hh:mm>" );
					}
				}else{
					
					result.add( "'" + line + "' is invalid: use <frequency> <profile> from <hh:mm> to <hh:mm>" );
				}
			}
		}
		
		if ( enabled ){
			
			current_rules = rules;
			
			if ( schedule_event == null && rules.size() > 0 ){
				
				schedule_event = 
					SimpleTimer.addPeriodicEvent(
						"speed handler scheduler",
						30*1000,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event) 
							{
								checkSchedule();
							}
						});
			}
			
			if ( active_rule != null || rules.size() > 0 ){
			
				checkSchedule();
			}
		}else{
	
			current_rules.clear();
			
			if ( schedule_event != null ){
				
				schedule_event.cancel();
				
				schedule_event = null;
			}
			
			if ( active_rule != null ){
				
				active_rule	= null;
				
				reset();
			}
		}
		
		return( result );
	}
	
	private int
	getMins(
		String	str )
	{
		try{
			String[]	bits = str.split( ":" );
			
			if ( bits.length == 2 ){
				
				return( Integer.parseInt(bits[0].trim())*60 + Integer.parseInt(bits[1].trim()));
			}
		}catch( Throwable e ){
		}
		
		return( -1 );
	}
	
	private synchronized void
	checkSchedule()
	{
		Calendar cal = new GregorianCalendar();
		
		int	day_of_week = cal.get( Calendar.DAY_OF_WEEK );
		int	hour_of_day	= cal.get( Calendar.HOUR_OF_DAY );
		int min_of_hour	= cal.get( Calendar.MINUTE );
		
		int	day = -1;
		
		switch( day_of_week ){
		case Calendar.MONDAY:
			day = ScheduleRule.FR_MON;
			break;
		case Calendar.TUESDAY:
			day = ScheduleRule.FR_TUE;
			break;
		case Calendar.WEDNESDAY:
			day = ScheduleRule.FR_WED;
			break;
		case Calendar.THURSDAY:
			day = ScheduleRule.FR_THU;
			break;
		case Calendar.FRIDAY:
			day = ScheduleRule.FR_FRI;
			break;
		case Calendar.SATURDAY:
			day = ScheduleRule.FR_SAT;
			break;
		case Calendar.SUNDAY:
			day = ScheduleRule.FR_SUN;
			break;
		}
		
		int	min_of_day = hour_of_day * 60 + min_of_hour;
		
		ScheduleRule latest_match = null;
		
		for ( ScheduleRule main_rule: current_rules ){
			
			List<ScheduleRule>	sub_rules = main_rule.splitByDay();
			
			for ( ScheduleRule rule: sub_rules ){
			
				if (( rule.frequency | day ) == 0 ){
					
					continue;
				}
				
				if (	rule.from_mins <= min_of_day &&
						rule.to_mins >= min_of_day ){
					
					latest_match = main_rule;
				}
			}
		}
		
		if ( latest_match == null ){
			
			active_rule = null;
			
			reset();
			
		}else{
			
			String	profile_name = latest_match.profile_name;
							
			if ( active_rule == null || !active_rule.sameAs( latest_match )){
				
				if ( profileExists( profile_name )){

					active_rule = latest_match;
				
					loadProfile( profile_name );
					
				}else{
					
					active_rule = null;
					
					reset();
				}
			}
		}
	}
	
	public List<String>
	getSchedule()
	{
		List<String>	result = new ArrayList<String>();
		
		result.add( "# Enter rules on separate lines below this section." );
		result.add( "# Rules are of the following types:" );
		result.add( "#    enable=(yes|no)   - controls whether the entire schedule is enabled or not (default=enabled)" );
		result.add( "#    <frequency> <profile_name> from <time> to <time>" );
		result.add( "#        frequency: daily|weekdays|weekends|<day_of_week>" );
		result.add( "#            days_of_week: mon|tue|wed|thu|fri|sat|sun" );
		result.add( "#    <time>: hh:mm - 24 hour clock; 00:00=midnight; local time" );
		result.add( "#" );
		result.add( "# For example - assuming there are profiles called 'no_limits' and 'limited_uplaod' defined:" );
		result.add( "#" );
		result.add( "#     daily no_limits from 00:00 to 23:59" );
		result.add( "#     daily limited_upload from 06:00 to 22:00" );
		result.add( "#" );
		result.add( "# When multiple rules apply the one further down the list of rules take precedence" );
		result.add( "# Comment lines are prefixed with '#'" );

		
		List<String> profiles = getProfileNames();
		
		if ( profiles.size() == 0 ){
			
			result.add( "# No profiles currently defined, you'll need to add some." );
			
		}else{
			String	str = "";
			
			for( String s: profiles ){
				str += (str.length()==0?"":", ") + s;
			}
			
			result.add( "# Current profiles details:" );
			result.add( "#     defined: " + str );
			
			ScheduleRule	current_rule;
			
			synchronized( this ){
				
				current_rule = active_rule;
			}
		
			result.add( "#     active: " + (current_rule==null?"none":current_rule.profile_name ));
		}
		
		result.add( "# ---- Do not edit this line or any text above! ----" );
		
		List<String> schedule_lines = BDecoder.decodeStrings( COConfigurationManager.getListParameter( "speed.limit.handler.schedule.lines", new ArrayList()));
		
		if ( schedule_lines.size() == 0 ){
			
			schedule_lines.add( "" );
			schedule_lines.add( "" );
			
		}else{
		
			for ( String l: schedule_lines ){
			
				result.add( l.trim());
			}
		}
		
		return( result );
	}
	
	public List<String>
	setSchedule(
		List<String>		lines )
	{
		int	trim_from = 0;
		
		for ( int i=0; i<lines.size(); i++ ){
			
			String	line = lines.get( i );
			
			if ( line.startsWith( "# ---- Do not edit" )){
				
				trim_from = i+1;
			}
		}
		
		if ( trim_from > 0 ){
			
			lines = lines.subList( trim_from, lines.size());
		}
		
		COConfigurationManager.setParameter( "speed.limit.handler.schedule.lines", lines );
		
		return( loadSchedule());
	}
	
	private String
	formatUp(
		int	rate )
	{
		return( "Up=" + format( rate ));
	}
	
	private String
	formatDown(
		int	rate )
	{
		return( "Down=" + format( rate ));
	}
	
	private String
	format(
		int		rate )
	{
		if ( rate < 0 ){
			
			return( "Disabled" );
			
		}else if ( rate == 0 ){
			
			return( "Unlimited" );
			
		}else{
			
			return( DisplayFormatters.formatByteCountToKiBEtcPerSec( rate ));
		}
	}
	
    private void
    exportBoolean(
    	Map<String,Object>	map,
    	String				key,
    	boolean				b )
    {
    	map.put( key, new Long(b?1:0));
    }
    
    private boolean
    importBoolean(
    	Map<String,Object>	map,
    	String				key )
    {
    	Long	l = (Long)map.get( key );
    	
    	if ( l != null ){
    		
    		return( l == 1 );
    	}
    	
    	return( false );
    }
    
    private void
    exportInt(
    	Map<String,Object>	map,
    	String				key,
    	int					i )
    {
    	map.put( key, new Long( i ));
    }
    
    private int
    importInt(
    	Map<String,Object>	map,
    	String				key )
    {
    	Long	l = (Long)map.get( key );
    	
    	if ( l != null ){
    		
    		return( l.intValue());
    	}
    	
    	return( 0 );
    }
    
    private void
    exportString(
    	Map<String,Object>	map,
    	String				key,
    	String				s )
    {
    	try{
    		map.put( key, s.getBytes( "UTF-8" ));
    		
    	}catch( Throwable e ){
    	}
    }
    
    private String
    importString(
    	Map<String,Object>	map,
    	String				key )
    {
       	Object obj= map.get( key );
       	
       	if ( obj instanceof String ){
       		
       		return((String)obj);
       		
       	}else if ( obj instanceof byte[] ){
       	
    		try{
    			return( new String((byte[])obj, "UTF-8" ));
    			
    		}catch( Throwable e ){
	    	}
       	}
       	
    	return( null );
    }
    
	private class
	LimitDetails
	{
	    private boolean		auto_up_enabled;
	    private boolean		auto_up_seeding_enabled;
	    private boolean		seeding_limits_enabled;
	    private int			up_limit;
	    private int			up_seeding_limit;
	    private int			down_limit;
	    
	    private Map<String,int[]>	download_limits = new HashMap<String, int[]>();
	    private Map<String,int[]>	category_limits = new HashMap<String, int[]>();
	    
	    private 
	    LimitDetails()
	    {	
	    }
	    
	    private 
	    LimitDetails(
	    	Map<String,Object>		map )
	    {
	    	auto_up_enabled 		= importBoolean( map, "aue" );
	    	auto_up_seeding_enabled	= importBoolean( map, "ause" );
	    	seeding_limits_enabled	= importBoolean( map, "sle" );
	    	
	    	up_limit			= importInt( map, "ul" );
	    	up_seeding_limit	= importInt( map, "usl" );
	    	down_limit			= importInt( map, "dl" );
	    	
	    	List<Map<String,Object>>	d_list = (List<Map<String,Object>>)map.get( "dms" );
	    	
	    	if ( d_list != null ){
	    		
	    		for ( Map<String,Object> m: d_list ){
	    			
	    			String	k = importString( m, "k" );
	    			
	    			if ( k != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				download_limits.put( k, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    	
	    	List<Map<String,Object>>	c_list = (List<Map<String,Object>>)map.get( "cts" );
	    	
	    	if ( c_list != null ){
	    		
	    		for ( Map<String,Object> m: c_list ){
	    			
	    			String	k = importString( m, "k" );
	    			
	    			if ( k != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				category_limits.put( k, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    }
	    
	    private Map<String,Object>
	    export()
	    {
	    	Map<String,Object>	map = new HashMap<String, Object>();
	    	
	    	exportBoolean( map, "aue", auto_up_enabled );
	    	exportBoolean( map, "ause", auto_up_seeding_enabled );
	    	exportBoolean( map, "sle", seeding_limits_enabled );
	    	
	    	exportInt( map, "ul", up_limit );
	    	exportInt( map, "usl", up_seeding_limit );
	    	exportInt( map, "dl", down_limit );
	    	
	    	List<Map<String,Object>>	d_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "dms", d_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		d_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	List<Map<String,Object>>	c_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "cts", c_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		c_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	return( map );
	    }
	    
	    private void
	    loadForReset()
	    {
	    		// just maintain the auto upload setting over a reset
	    	
		    auto_up_enabled 		= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
	    }
	    
	    private void
	    loadCurrent()
	    {
		    auto_up_enabled 		= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
		    auto_up_seeding_enabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
		    seeding_limits_enabled 	= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.UPLOAD_SEEDING_ENABLED_CONFIGKEY );
		    up_limit 				= COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
		    up_seeding_limit 		= COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
		    down_limit				= COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
		
		    download_limits.clear();
		    
			GlobalManager gm = core.getGlobalManager();

			List<DownloadManager>	downloads = gm.getDownloadManagers();
			
			for ( DownloadManager download: downloads ){
				
				TOTorrent torrent = download.getTorrent();
				
				byte[]	hash = null;
				
				if ( torrent!= null ){
					
					try{
						hash = torrent.getHash();
						
					}catch( Throwable e ){
						
					}
				}
				
				if ( hash != null ){
					int	download_up_limit 	= download.getStats().getUploadRateLimitBytesPerSecond();
					int	download_down_limit = download.getStats().getDownloadRateLimitBytesPerSecond();
					
			    	if ( download_up_limit > 0 || download_down_limit > 0 ){
			    		
			    		download_limits.put( Base32.encode( hash ), new int[]{ download_up_limit, download_down_limit });
			    	}
				}
			}
		    
			Category[] categories = CategoryManager.getCategories();
		 
			category_limits.clear();
			
		    for ( Category category: categories ){
		    	
		    	int	cat_up_limit	 	= category.getUploadSpeed();
		    	int	cat_down_limit 		= category.getDownloadSpeed();
		    	
		    	if ( cat_up_limit > 0 || cat_down_limit > 0 ){
		    	
		    		category_limits.put( category.getName(), new int[]{ cat_up_limit, cat_down_limit });
		    	}
		    }
	    }
	    
	    private int[]
	    getLimitsForDownload(
	    	String	hash )
	    {
	    	return( download_limits.get( hash ));
	    }
	    
	    private void
	    addRemoveDownloads(
	    	List<String>		hashes,
	    	boolean				add )
	    {
			GlobalManager gm = core.getGlobalManager();

	    	for ( String hash: hashes ){
	    		
	    		if ( add ){

	   				DownloadManager download = gm.getDownloadManager( new HashWrapper( Base32.decode( hash )));
	    			
	    			if ( download != null ){
	    						
						int	download_up_limit 	= download.getStats().getUploadRateLimitBytesPerSecond();
						int	download_down_limit = download.getStats().getDownloadRateLimitBytesPerSecond();
						
				    	if ( download_up_limit > 0 || download_down_limit > 0 ){
				    		
				    		download_limits.put(hash, new int[]{ download_up_limit, download_down_limit });
				    	}
	    			}
	    		}else{
	    			
	    			download_limits.remove( hash );
	    		}
	    	}
	    }
	    
	    private void
	    apply()
	    {			    		
	    		// don't manage this properly because the speedmanager has a 'memory' of 
    			// the last upload limit in force before it became active and we're
    			// not persisting this... rare use case methinks anyway

    		COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, auto_up_enabled );
	    	COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY, auto_up_seeding_enabled );

    		if ( !( auto_up_enabled || auto_up_seeding_enabled )){
      				
 		     	COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, up_limit );
    		}
    		
		    COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_ENABLED_CONFIGKEY, seeding_limits_enabled );
		    COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY, up_seeding_limit );

		    COConfigurationManager.setParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY, down_limit );

			GlobalManager gm = core.getGlobalManager();

			Set<DownloadManager>	all_managers = new HashSet<DownloadManager>( gm.getDownloadManagers());
			
			for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
				
				byte[] hash = Base32.decode( entry.getKey());
				
				DownloadManager dm = gm.getDownloadManager( new HashWrapper( hash ));
			
				if ( dm != null ){
						
					int[]	limits = entry.getValue();
					
					dm.getStats().setUploadRateLimitBytesPerSecond( limits[0] );
					dm.getStats().setDownloadRateLimitBytesPerSecond( limits[1] );
					
					all_managers.remove( dm );
				}
			}

			for ( DownloadManager dm: all_managers ){
				
				dm.getStats().setUploadRateLimitBytesPerSecond( 0 );
				dm.getStats().setDownloadRateLimitBytesPerSecond( 0 );
			}
			
			Set<Category> all_categories = new HashSet<Category>( Arrays.asList(CategoryManager.getCategories()));
			 
			Map<String, Category> cat_map = new HashMap<String, Category>();
			
			for ( Category c: all_categories ){
				
				cat_map.put( c.getName(), c );
			}
								
			for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
		    	
		    	String cat_name = entry.getKey();
		    	
		    	Category category = cat_map.get( cat_name );
		    	
		    	if ( category != null ){
		    		
		    		int[]	limits = entry.getValue();
		    		
		    		category.setUploadSpeed( limits[0] );
		    		category.setDownloadSpeed( limits[1] );
		    		
		    		all_categories.remove( category );
		    	}
			}
			
			for ( Category category: all_categories ){
				
	    		category.setUploadSpeed( 0 );
	    		category.setDownloadSpeed( 0 );
			}
	    }
	    
	    private List<String>
	    getString()
	    {
			List<String> result = new ArrayList<String>();
			
			result.add( "Global Limits" );
				    	    
		    if ( auto_up_enabled ){
		    	
			    result.add( "    Auto upload limit enabled" );
			    
		    }else if ( auto_up_seeding_enabled ){
		    				    	
		    	result.add( "    Auto upload seeding limit enabled" );

		    }else{
		    	
		    	result.add( "    " + formatUp( up_limit*1024 ));

	    		if ( seeding_limits_enabled ){

	    			result.add( "    Seeding only limit enabled" );
		    		
		    		result.add( "    Seeding only: " + format( up_seeding_limit*1024 ));
		    	}
		    }

		    
		    result.add( "    " + formatDown( down_limit*1024 ));

		    result.add( "" );
		    
		    result.add( "Download Limits" );
		    
		    int	total_download_limits = 0;
		    int	total_download_limits_up 	= 0;
		    int	total_download_limits_down 	= 0;
		    
			GlobalManager gm = core.getGlobalManager();

			for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
				
				byte[] hash = Base32.decode( entry.getKey());
				
				DownloadManager dm = gm.getDownloadManager( new HashWrapper( hash ));
			
				if ( dm != null ){
						
					int[]	limits = entry.getValue();
					
		    		total_download_limits++;
		    		
		    		int	up 		= limits[0];
		    		int	down 	= limits[1];
		    		
		    		total_download_limits_up 	+= up;
		    		total_download_limits_down 	+= down;
		    		
		    		result.add( "    " + dm.getDisplayName() + ": " + formatUp( up ) + ", " + formatDown( down ));
		    	}
			}
			
		    if ( total_download_limits == 0 ){
		    	
		    	result.add( "    None" );
		    	
		    }else{
		    	
		    	result.add( "    ----" );
		    	
		    	result.add( "    Total=" + total_download_limits + " - Compounded limits: " + formatUp( total_download_limits_up ) + ", " + formatDown( total_download_limits_down ));
		    }
		    
			Category[] categories = CategoryManager.getCategories();
		 
			Map<String, Category> cat_map = new HashMap<String, Category>();
			
			for ( Category c: categories ){
				
				cat_map.put( c.getName(), c );
			}
			
		    result.add( "" );

			result.add( "Category Limits" );
			
			int	total_cat_limits = 0;
		    int	total_cat_limits_up 	= 0;
		    int	total_cat_limits_down 	= 0;

			for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
		    	
		    	String cat_name = entry.getKey();
		    	
		    	Category category = cat_map.get( cat_name );
		    	
		    	if ( category != null ){
		    		
		    		if ( category.getType() == Category.TYPE_UNCATEGORIZED ){
		    			
		    			cat_name = "Uncategorised";
		    		}
		    		
					int[]	limits = entry.getValue();
					
		    		total_cat_limits++;

		    		int	up 		= limits[0];
		    		int	down 	= limits[1];
		    		
		    		total_cat_limits_up 	+= up;
		    		total_cat_limits_down 	+= down;
		    		
		    		result.add( "    " + cat_name + ": " + formatUp( up ) + ", " + formatDown( down ));
		    	}
		    }
		    
		    if ( total_cat_limits == 0 ){
		    	
		    	result.add( "    None" );
		    	
		    }else{
		    	
		    	result.add( "    ----" );
		    	
		    	result.add( "    Total=" + total_cat_limits + " - Compounded limits: " + formatUp( total_cat_limits_up ) + ", " + formatDown( total_cat_limits_down ));

		    }
		    
			return( result );
	    }
	}
	
	private class
	ScheduleRule
	{
		private static final byte	FR_MON		= 0x01;
		private static final byte	FR_TUE		= 0x02;
		private static final byte	FR_WED		= 0x04;
		private static final byte	FR_THU		= 0x08;
		private static final byte	FR_FRI		= 0x10;
		private static final byte	FR_SAT		= 0x20;
		private static final byte	FR_SUN		= 0x40;
		private static final byte	FR_OVERFLOW	= (byte)0x80;
		private static final byte	FR_WEEKDAY	= ( FR_MON | FR_TUE | FR_WED | FR_THU | FR_FRI );
		private static final byte	FR_WEEKEND	= ( FR_SAT | FR_SUN );
		private static final byte	FR_DAILY	= ( FR_WEEKDAY | FR_WEEKEND );
		
		private String	profile_name;
		private byte	frequency;
		private int		from_mins;
		private int		to_mins;
		
		private 
		ScheduleRule(
			byte			_freq,
			String			_profile,
			int				_from,
			int				_to )
		{
			frequency 		= _freq;
			profile_name	= _profile;
			from_mins		= _from;
			to_mins			= _to;
		}
		
		private List<ScheduleRule>
		splitByDay()
		{
			List<ScheduleRule>	result = new ArrayList<ScheduleRule>();
			
			if ( to_mins > from_mins ){
			
				result.add( this );
				
			}else{
				
					// handle rules that wrap across days. e.g. 23:00 to 00:00
				
				byte next_frequency = (byte)(frequency << 1 );
				
				if ((next_frequency & FR_OVERFLOW ) != 0 ){
					
					next_frequency &= ~FR_OVERFLOW;
					
					next_frequency |= FR_MON;
				}
				
				ScheduleRule	rule1 = new ScheduleRule( frequency, profile_name, from_mins, 23*60+59 );
				ScheduleRule	rule2 = new ScheduleRule( next_frequency, profile_name, 0, to_mins );

				result.add( rule1 );
				result.add( rule2 );
			}
			
			return( result );
		}
		
		private boolean
		sameAs(
			ScheduleRule	other )
		{
			if ( other == null ){
				
				return( false );
			}
			
			return( frequency == other.frequency &&
					profile_name.equals( other.profile_name ) &&
					from_mins == other.from_mins &&
					to_mins == other.to_mins );
		}
		
		public String
		getString()
		{
			String	freq_str = "";
			
			if ( frequency == FR_DAILY ){
				
				freq_str = "daily";
				
			}else if ( frequency == FR_WEEKDAY ){
				
				freq_str = "weekdays";
				
			}else if ( frequency == FR_WEEKEND ){
				
				freq_str = "weekends";
				
			}else if ( frequency == FR_MON ){
				
				freq_str = "mon";
				
			}else if ( frequency == FR_TUE ){
				
				freq_str = "tue";
				
			}else if ( frequency == FR_WED ){
				
				freq_str = "wed";
				
			}else if ( frequency == FR_THU ){
				
				freq_str = "thu";
				
			}else if ( frequency == FR_FRI ){
				
				freq_str = "fri";
				
			}else if ( frequency == FR_SAT ){
				
				freq_str = "sat";
				
			}else if ( frequency == FR_SUN ){
				
				freq_str = "sun";
			}
			
			return( "profile=" + profile_name + ", frequency=" + freq_str + ", from=" + getTime( from_mins ) + ", to=" + getTime( to_mins ));
		}
		
		private String
		getTime(
			int	mins )
		{
			String str = getTimeBit( mins/60 ) + ":" + getTimeBit( mins % 60 );
		
			return( str );
		}
		
		private String
		getTimeBit(
			int	num )
		{
			String str = String.valueOf( num );
			
			if ( str.length() < 2 ){
				
				str = "0" + str;
			}
			
			return( str );
		}
	}
}
