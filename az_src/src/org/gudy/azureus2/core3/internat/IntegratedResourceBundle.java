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
import java.io.InputStream;
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
	private static final boolean DEBUG = false;

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
					
									if (DEBUG) {
										System.out.println("Compact RB " + rb.getString());
									}

									if ( rb.compact()){
					
										did_something	= true;
									}
									if (DEBUG) {
										System.out.println("        to " + rb.getString());
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

	private Map	messages;
	private Map	used_messages;
	private List null_values;
	
	private boolean		messages_dirty;
	private int			clean_count	= 0;
	private boolean		one_off_discard_done;
	
	private File		scratch_file_name;
	private InputStream	scratch_file_is;

	private final int initCapacity;
	
	private Map<String,String>	added_strings;
	
	public 
	IntegratedResourceBundle(
		ResourceBundle 		main, 
		Map 				localizationPaths) 
	{
		this( main, localizationPaths, null, 10 );
	}

	public 
	IntegratedResourceBundle(
		ResourceBundle 		main, 
		Map 				localizationPaths,
		int					initCapacity) 
	{
		this( main, localizationPaths, null, initCapacity );
	}

	public 
	IntegratedResourceBundle(
		ResourceBundle 		main, 
		Map 				localizationPaths,
		Collection 			resource_bundles,
		int					initCapacity)  
	{
		this.initCapacity = initCapacity;
		messages = new LightHashMap(initCapacity);

		locale = main.getLocale();

			// use a somewhat decent initial capacity, proper calculation would require java 1.6
		
		addResourceMessages( main );
		
		synchronized (localizationPaths)
		{
			for (Iterator iter = localizationPaths.keySet().iterator(); iter.hasNext();){
				String localizationPath = (String) iter.next();
				ClassLoader classLoader = (ClassLoader) localizationPaths.get(localizationPath);

				addPluginBundle(localizationPath, classLoader);
			}
		}

		if (resource_bundles != null) {
			synchronized (resource_bundles)
			{
				for (Iterator itr = resource_bundles.iterator(); itr.hasNext();) {
					addResourceMessages((ResourceBundle)itr.next());
				}
			}

		}
		
		used_messages = new LightHashMap( messages.size());
		
		synchronized( bundle_map ){
			
			bundle_map.put( this, NULL_OBJECT );
			
			resetCompactTimer();
		}
		//System.out.println("IRB Size = " + messages.size() + "/cap=" + initCapacity + ";" + Debug.getCompressedStackTrace());
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
		Map m = new LightHashMap(loadMessages());
		
		return( m.keySet().iterator());
	}

	/**
	 * Gets a string, using default if key doesn't exist.  Skips
	 * throwing MissingResourceException when key doesn't exist, which saves
	 * some CPU cycles
	 * 
	 * @param key
	 * @param def
	 * @return
	 *
	 * @since 3.1.1.1
	 */
  public String getString(String key, String def) {
		String s = (String) handleGetObject(key);
		if (s == null) {
			if (parent != null) {
				s = parent.getString(key);
			}
			if (s == null) {
				return def;
			}
		}
		return s;
	}

	
	protected Object 
	handleGetObject(
		String key )
	{
		Object	res;
		
		synchronized( bundle_map ){
		
			res = used_messages.get( key );
		}
		
		Integer keyHash = null;
		if (null_values != null) {
			keyHash = new Integer(key.hashCode());
  		int index = Collections.binarySearch(null_values, keyHash);
  		if (index >= 0) {
  			return null;
  		}
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

				if (res == null && null_values != null) {

		  		int index = Collections.binarySearch(null_values, keyHash);
					if (index < 0) {
						index = -1 * index - 1; // best guess
					}

					if (index > null_values.size()) {
						index = null_values.size();
					}
					
					null_values.add(index, keyHash);
					
				} else {

					used_messages.put( key, res==null?NULL_OBJECT:res );
				}
					
				clean_count	= 0;
				
				resetCompactTimer();
			}
		}
		
		return( res );
	}
	
	public void addPluginBundle(String localizationPath, ClassLoader classLoader)
	{
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
				return;
			}
		}
				
		addResourceMessages(newResourceBundle);

		
	}
	
	public void 
	addResourceMessages(
		ResourceBundle bundle )
	{
		synchronized (bundle_map)
		{
			loadMessages();
			
			if ( bundle != null ){
				
				messages_dirty = true;
				
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
//		System.out.println("after addrb; IRB Size = " + messages.size() + "/cap=" + initCapacity + ";" + Debug.getCompressedStackTrace());
	}
	
	protected boolean
	compact()
	{
		// System.out.println("compact " + getString() + ": cc=" + clean_count );
		
		clean_count++;
		
		if ( clean_count == 1 ){
					
			return( true );
		}
		
		if ( scratch_file_is == null || messages_dirty ){
			
			File temp_file = null;
			
			FileOutputStream	fos = null;
			
				// we have a previous cache, discard 
			
			if ( scratch_file_is != null ){
				
					// System.out.println( "discard cache file " + scratch_file_name + " for " + this );
				
				try{
					scratch_file_is.close();
										
				}catch( Throwable e ){	
					
					scratch_file_name = null;
					
				}finally{
					
					scratch_file_is = null;
				}
			}
			
			try{
				Properties props = new Properties();
				
				props.putAll( messages );
				
				if ( scratch_file_name == null ){
				
					temp_file = AETemporaryFileHandler.createTempFile();
					
				}else{
					
					temp_file = scratch_file_name;
				}
				
				fos = new FileOutputStream( temp_file );
				
				props.store( fos, "message cache" );
				
				fos.close();
				
				fos = null;
				
					// System.out.println( "wrote cache file " + temp_file + " for " + this );
				
				scratch_file_name	= temp_file;
				scratch_file_is 	= new FileInputStream( temp_file );
				
				messages_dirty		= false;
				
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
		
		if ( scratch_file_is != null ){
			
			if ( clean_count >= 2 ){
		
				/*
				if ( messages != null ){
				
					System.out.println( "messages discarded  " + scratch_file_name + " for " + this );
				}
				*/
				
					// throw away full message map after 2 ticks
			
				messages = null;
			}
		
			if ( clean_count == 5 && !one_off_discard_done){

					// System.out.println( "used discard " + scratch_file_name + " for " + this );
				
				one_off_discard_done = true;
				
					// one off discard of used_messages to clear out any that were
					// accessed once and never again
			
				used_messages.clear();
			}
		}
		
		if ( clean_count > 5 ){
		
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
			
			Map	result;
			
			if ( scratch_file_is == null ){
				
				result = new LightHashMap();
				
			}else{
			
					// System.out.println( "read cache file " + scratch_file_name + " for " + this );

				
				Properties p = new Properties();
				
				InputStream	fis = scratch_file_is;
				
				try{
									
					p.load( fis );
					
					fis.close();
					
					scratch_file_is = new FileInputStream( scratch_file_name );
					
					messages = new LightHashMap();
					
					messages.putAll( p );
					
					result = messages;
					
				}catch( Throwable e ){
					
					if ( fis != null ){
						
						try{
							fis.close();
							
						}catch( Throwable f ){
						}
					}
					
					Debug.out( "Failed to load message bundle scratch file", e );
					
					scratch_file_name.delete();
					
					scratch_file_is = null;
					
					result = new LightHashMap();
				}
			}
			
			if ( added_strings != null ){
				
				result.putAll( added_strings );
			}
			
			return( result );
		}
	}
	
	protected String
	getString()
	{
		return( locale + ": use=" + used_messages.size() + ",map=" + (messages==null?"":String.valueOf(messages.size())) 
				+ (null_values == null ? "" : ",null=" + null_values.size()) + ",added=" + (added_strings==null?"":added_strings.size()));
	}
	
	public void
	addString(String key, String value) {
		synchronized( bundle_map ){
			if ( added_strings == null ){
				
				added_strings = new HashMap<String, String>();
			}
			
			added_strings.put( key, value );
			
			if ( messages != null ){
				
				messages.put( key, value );
			}
		}
	}

	public boolean getUseNullList() {
		return null_values != null;
	}

	public void setUseNullList(boolean useNullList) {
		if (useNullList && null_values == null) {
			null_values = new ArrayList(0);
		} else if (!useNullList && null_values != null) {
			null_values = null;
		}
	}
	
	public void clearUsedMessagesMap(int initialCapacity) {
		used_messages = new LightHashMap(initialCapacity);
		if (null_values != null) {
			null_values = new ArrayList(0);
		}
	}
}
