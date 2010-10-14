/*
 * Created on 29.11.2003
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
package org.gudy.azureus2.core3.internat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;

/**
 * @author Rene Leonhardt
 */
public class 
IntegratedResourceBundle 
	extends ResourceBundle 
{
	private static final Object	NULL_OBJECT = new Object();
	
	private static final Map	bundle_map = new WeakHashMap();

	private static TimerEventPeriodic	compact_timer;		

	protected static void
	resetCompactTimer()
	{
		synchronized( bundle_map ){

			if ( compact_timer == null && System.getProperty("transitory.startup", "0").equals("0")){
								
				compact_timer = SimpleTimer.addPeriodicEvent( 
					"IRB:compactor",
					60*1000,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event )
						{	
							synchronized( bundle_map ){
					
								Iterator it = bundle_map.keySet().iterator();
					
								boolean	did_something = false;
					
								while( it.hasNext()){
					
									IntegratedResourceBundle	rb = (IntegratedResourceBundle)it.next();
					
									if ( rb.compact()){
					
										did_something	= true;
									}
								}
					
								if ( !did_something ){
					
									compact_timer.cancel();
				
									compact_timer	= null;
								}
							}
						}
					});
			}
		}
	}
	
	private Locale	locale;

	private Map	messages 		= new HashMap();
	private Map	used_messages;
	
	private int		clean_count	= 0;
	private File	scratch_file;
	
	
	public 
	IntegratedResourceBundle(
		ResourceBundle 		main, 
		Map 				localizationPaths) 
	{
		this( main, localizationPaths, null );
	}

	public 
	IntegratedResourceBundle(
		ResourceBundle 		main, 
		Map 				localizationPaths,
		Collection 			resource_bundles) 
	{
		locale = main.getLocale();

			// use a somewhat decent initial capacity, proper calculation would require java 1.6
		
		addResourceMessages( main );

		for (Iterator iter = localizationPaths.keySet().iterator(); iter.hasNext();){
			String localizationPath = (String) iter.next();
			ClassLoader classLoader = (ClassLoader) localizationPaths.get(localizationPath);
			ResourceBundle newResourceBundle = null;
			try {
				if(classLoader != null)
					newResourceBundle = ResourceBundle.getBundle(localizationPath, locale ,classLoader);
				else
					newResourceBundle = ResourceBundle.getBundle(localizationPath, locale,IntegratedResourceBundle.class.getClassLoader());
			} catch (Exception e) {
				//        System.out.println(localizationPath+": no resource bundle for " +
				// main.getLocale());
				try {
					if(classLoader != null)
						newResourceBundle = ResourceBundle.getBundle(localizationPath, MessageText.LOCALE_DEFAULT,classLoader);
					else 
						newResourceBundle = ResourceBundle.getBundle(localizationPath, MessageText.LOCALE_DEFAULT,IntegratedResourceBundle.class.getClassLoader());
				} catch (Exception e2) {
					System.out.println(localizationPath + ": no default resource bundle");
					continue;
				}
			}
			addResourceMessages(newResourceBundle);
		}

		if (resource_bundles != null) {
			for (Iterator itr = resource_bundles.iterator(); itr.hasNext();) {
				addResourceMessages((ResourceBundle)itr.next());
			}
		}
		
		used_messages = new LightHashMap( messages.size());
		
		synchronized( bundle_map ){
			
			bundle_map.put( this, NULL_OBJECT );
			
			resetCompactTimer();
		}
	}

	public Locale getLocale() 
	{
		return locale;
	}

	private Map
	getMessages()
	{
		return( loadMessages());
	}
	
	public Enumeration 
	getKeys() 
	{
		new Exception("Don't call me, call getKeysLight").printStackTrace();
		
		Map m = loadMessages();
		
		return( new Vector( m.keySet()).elements());
	}
	
	protected Iterator
	getKeysLight()
	{
		Map m = loadMessages();
		
		return( m.keySet().iterator());
	}
	
	protected Object 
	handleGetObject(
		String key )
	{
		Object	res;
		
		synchronized( bundle_map ){
		
			res = used_messages.get( key );
		}
		
		if ( res == NULL_OBJECT ){
			
			return( null );
		}
		
		if ( res == null ){
			
			synchronized( bundle_map ){

				loadMessages();
			
				if ( messages != null ){
					
					res = messages.get( key );
				}
			
				used_messages.put( key, res==null?NULL_OBJECT:res );
					
				clean_count	= 0;
				
				resetCompactTimer();
			}
		}
		
		return( res );
	}
	
	private void 
	addResourceMessages(
		ResourceBundle bundle )
	{
		if ( bundle != null ){
			
			if ( bundle instanceof IntegratedResourceBundle ){
				
				messages.putAll(((IntegratedResourceBundle)bundle).getMessages());
				
			}else{
				
				for (Enumeration enumeration = bundle.getKeys(); enumeration.hasMoreElements();) {
					
					String key = (String) enumeration.nextElement();
					
					messages.put(key, bundle.getObject(key));
				}
			}
		}
	}
	
	protected boolean
	compact()
	{
		// System.out.println("compact " + getString() + ": cc=" + clean_count );
		
		clean_count++;
		
		if ( clean_count == 1 ){
					
			return( true );
		}
		
		if ( scratch_file == null ){
			
			File temp_file = null;
			
			FileOutputStream	fos = null;
			
			try{
				Properties props = new Properties();
				
				props.putAll( messages );
				
				temp_file = AETemporaryFileHandler.createTempFile();

				fos = new FileOutputStream( temp_file );
				
				props.store( fos, "message cache" );
				
				fos.close();
				
				fos = null;
				
				scratch_file = temp_file;
				
			}catch( Throwable e ){
				
				if ( fos != null ){
					
					try{
						fos.close();
						
					}catch( Throwable f ){
						
					}
				}
				
				if  ( temp_file != null ){
				
					temp_file.delete();
				}
			}
		}
		
		if ( scratch_file != null && clean_count >= 2 ){
			
			messages = null;
		}
		
		if ( clean_count >= 5 ){
		
			Map	compact_um = new LightHashMap( used_messages.size() + 16 );
			
			compact_um.putAll( used_messages );
			
			used_messages = compact_um;
			
			return( false );
			
		}else{
		
			return( true );
		}
	}
	
	protected Map
	loadMessages()
	{
		synchronized( bundle_map ){
			
			if ( messages != null ){
				
				return( messages );
			}
			
			if ( scratch_file == null ){
				
				return( new HashMap());
			}
			
			Properties p = new Properties();
			
			FileInputStream	fis = null;
			
			try{
				
				fis = new FileInputStream( scratch_file );
				
				p.load( fis );
				
				fis.close();
				
				messages = new HashMap();
				
				messages.putAll( p );
				
				return( messages );
				
			}catch( Throwable e ){
				
				if ( fis != null ){
					
					try{
						fis.close();
						
					}catch( Throwable f ){
					}
				}
				
				Debug.out( "Failed to load message bundle scratch file", e );
				
				scratch_file.delete();
				
				scratch_file = null;
				
				return( new HashMap());
			}
		}
	}
	
	protected String
	getString()
	{
		return( locale + ": use=" + used_messages.size() + ",map=" + (messages==null?"":String.valueOf(messages.size())));
	}
}
