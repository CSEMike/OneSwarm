/*
 * Created on Sep 9, 2008
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


package com.aelitis.azureus.core.custom.impl;

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.custom.*;

import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;

public class 
CustomizationManagerImpl 
	implements CustomizationManager
{
	private static CustomizationManagerImpl		singleton = new CustomizationManagerImpl();
	
	public static CustomizationManager
	getSingleton()
	{
		return( singleton );
	}

	private Map	customization_file_map = new HashMap();
	
	private String				current_customization_name;
	private CustomizationImpl	current_customization;
	
	protected
	CustomizationManagerImpl()
	{
		VuzeFileHandler.getSingleton().addProcessor(
				new VuzeFileProcessor()
				{
					public void
					process(
						VuzeFile[]		files,
						int				expected_types )
					{
						for (int i=0;i<files.length;i++){
							
							VuzeFile	vf = files[i];
							
							VuzeFileComponent[] comps = vf.getComponents();
							
							for (int j=0;j<comps.length;j++){
								
								VuzeFileComponent comp = comps[j];
								
								if ( comp.getType() == VuzeFileComponent.COMP_TYPE_CUSTOMIZATION ){
									
									try{
										Map map = comp.getContent();
										
										((CustomizationManagerImpl)getSingleton()).importCustomization( map );
										
										comp.setProcessed();
										
									}catch( Throwable e ){
										
										Debug.printStackTrace(e);
									}
								}
							}
						}
					}
				});	
		
	    File	user_dir = FileUtil.getUserFile("custom");
	    
	    File	app_dir	 = FileUtil.getApplicationFile("custom");
	    
	    loadCustomizations( app_dir );
	    
	    if ( !user_dir.equals( app_dir )){
	    
	    	loadCustomizations( user_dir );
	    }
	    
	    String active = COConfigurationManager.getStringParameter( "customization.active.name", "" );
	    
	    if ( customization_file_map.get( active ) == null ){
	    	
	    		// hmm, its been deleted or not set yet. look for new ones
	    	
	    	Iterator it = customization_file_map.keySet().iterator();
	    	
	    	while( it.hasNext()){
	    		
	    		String	name = (String)it.next();
	    		
	    		final String version_key = "customization.name." + name + ".version";
	    		
	    		String existing_version = COConfigurationManager.getStringParameter( version_key, "0" );
	    			
	    		if ( existing_version.equals( "0" )){
	    			
	    			active = name;
	    			
	    			String version = ((String[])customization_file_map.get( name ))[0];
	    			
	    			COConfigurationManager.setParameter( "customization.active.name", active );
	    			
	    			COConfigurationManager.setParameter( version_key, version );
	    			
	    			break;
	    		}
	    	}
	    }
	    	
	    current_customization_name = active;
	}
	
	protected void
	loadCustomizations(
		File		dir )
	{
		if ( dir.isDirectory()){
	
			File[]	files = dir.listFiles();
			
			if ( files != null ){
				
				for (int i=0;i<files.length;i++){
					
					File	file = files[i];
					
					String	name = file.getName();
					
					if ( !name.endsWith( ".zip" )){
						
						logInvalid( file );
						
						continue;
					}
					
					String	base = name.substring( 0, name.length() - 4 );
					
					int	u_pos = base.lastIndexOf( '_' );
				
					if ( u_pos == -1 ){
						
						logInvalid( file );
						
						continue;
					}
					
					String	lhs = base.substring(0,u_pos).trim();
					String	rhs	= base.substring(u_pos+1).trim();
					
					if ( lhs.length() == 0 || !Constants.isValidVersionFormat( rhs )){
						
						logInvalid( file );
						
						continue;
					}
					
					String[]	details = (String[])customization_file_map.get( lhs );
					
					if ( details == null ){
						
						customization_file_map.put( lhs, new String[]{ rhs, file.getAbsolutePath()});
						
					}else{
						
						String	old_version = details[0];
						
						if ( Constants.compareVersions( old_version, rhs ) < 0 ){
							
							customization_file_map.put( lhs, new String[]{ rhs, file.getAbsolutePath()});
						}
					}
				}
			}
		}
	}
	
	protected void
	logInvalid(
		File file )
	{
		Debug.out( "Invalid customization file name '" + file.getAbsolutePath() + "' - format must be <name>_<version>.zip where version is numeric and dot separated" );
	}
	
	protected void
	importCustomization(
		Map		map )
	
		throws CustomizationException
	{
		try{
			String	name 	= new String((byte[])map.get( "name" ), "UTF-8" );
			
			String	version = new String((byte[])map.get( "version" ), "UTF-8" );
			
			if ( !Constants.isValidVersionFormat( version )){
				
				throw( new CustomizationException( "Invalid version specification: " + version ));
			}
			
			byte[]	data = (byte[])map.get( "data" );
			
		    File	user_dir = FileUtil.getUserFile("custom");
		    
		    if ( !user_dir.exists()){
		    	
		    	user_dir.mkdirs();
		    }
		    
		    File	target = new File( user_dir, name + "_" + version + ".zip" );
		    
		    if ( !target.exists()){
		    	
		    	if ( !FileUtil.writeBytesAsFile2( target.getAbsolutePath(), data )){
		    		
		    		throw( new CustomizationException( "Failed to save customization to " + target ));
		    	}
		    }
		}catch( CustomizationException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new CustomizationException( "Failed to import customization", e ));
		}
	}
	
	protected void
	exportCustomization(
		CustomizationImpl	cust,
		File				to_file )
	
		throws CustomizationException
	{
		if ( to_file.isDirectory()){
			
			to_file = new File( to_file, cust.getName() + "_" + cust.getVersion() + ".vuze");
		}
		
		if ( !to_file.getName().endsWith( ".vuze" )){
			
			to_file = new File( to_file.getParentFile(), to_file.getName() + ".vuze" );
		}
		
		try{
			Map	contents = new HashMap();
			
			byte[]	data = FileUtil.readFileAsByteArray( cust.getContents());
			
			contents.put( "name", cust.getName());
			contents.put( "version", cust.getVersion());
			contents.put( "data", data );
			
			VuzeFile	vf = VuzeFileHandler.getSingleton().create();
			
			vf.addComponent(
				VuzeFileComponent.COMP_TYPE_CUSTOMIZATION,
				contents);
			
			vf.write( to_file );
			
		}catch( Throwable e ){
			
			throw( new CustomizationException( "Failed to export customization", e ));
		}
	}
	
	public Customization
	getActiveCustomization()
	{
		synchronized( this ){
			
			if ( current_customization == null ){
				
				if ( current_customization_name != null ){
					
					String[] entry = (String[])customization_file_map.get( current_customization_name );
					
					if ( entry != null ){
						
						try{
							current_customization = 
								new CustomizationImpl(
									this,
									current_customization_name,
									entry[0],
									new File( entry[1] ));
							
							SimpleTimer.addEvent( 
								"Custom:clear", 
								SystemTime.getCurrentTime() + 120*1000,
								new TimerEventPerformer()
								{
									public void 
									perform(
										TimerEvent event ) 
									{
										synchronized( CustomizationManagerImpl.this ){
											
											current_customization = null;
										}
									}
								});
							
						}catch( CustomizationException e ){
							
							e.printStackTrace();
						}
					}
				}
			}
			
			return( current_customization );
		}
	}
	
	public Customization[]
	getCustomizations()
	{
		List	result = new ArrayList();
		
		synchronized( this ){
			
			Iterator	it = customization_file_map.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				String		name = (String)entry.getKey();
				String[]	bits = (String[])entry.getValue();
				
				String	version = (String)bits[0];
				File	file	= new File(bits[1]);
				
				try{
					
					CustomizationImpl cust = new CustomizationImpl( this, name, version, file );
					
					result.add( cust );
					
				}catch( Throwable e ){
				}
			}
		}
		
		return((Customization[])result.toArray(new Customization[result.size()]));
	}
	
	public static void
	main(
		String[]		args )
	{
		try{
			CustomizationManagerImpl	manager = (CustomizationManagerImpl)getSingleton();
			
			CustomizationImpl cust = new CustomizationImpl( manager, "blah", "1.2", new File( "C:\\temp\\cust\\details.zip" ));
		
			cust.exportToVuzeFile( new File( "C:\\temp\\cust" ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
