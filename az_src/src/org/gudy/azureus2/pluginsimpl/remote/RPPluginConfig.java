/*
 * File    : RPPluginConfig.java
 * Created : 17-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.pluginsimpl.remote;

import org.gudy.azureus2.plugins.config.PluginConfigSource;

/**
 * @author parg
 *
 */

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.config.ConfigParameter;

public class 
RPPluginConfig
	extends		RPObject
	implements 	PluginConfig
{
	protected transient PluginConfig		delegate;
	protected transient	Properties			property_cache;
	
		// don't change these field names as they are visible on XML serialisation

	public String[]		cached_property_names;
	public Object[]		cached_property_values;
	
	public static PluginConfig
	create(
		PluginConfig		_delegate )
	{
		RPPluginConfig	res =(RPPluginConfig)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPPluginConfig( _delegate );
		}
			
		return( res );
	}
	
	protected
	RPPluginConfig(
		PluginConfig		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (PluginConfig)_delegate;
		
		cached_property_names 	= new String[]{
				CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC,
				CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC,
				CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC,
				CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT,
				CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL,
				CORE_PARAM_INT_MAX_DOWNLOADS,
				CORE_PARAM_INT_MAX_ACTIVE,
				CORE_PARAM_INT_MAX_ACTIVE_SEEDING,
				CORE_PARAM_INT_MAX_UPLOADS,
				CORE_PARAM_INT_MAX_UPLOADS_SEEDING
			};
		
		cached_property_values 	= new Object[]{
				new Integer( delegate.getIntParameter( cached_property_names[0] )),
				new Integer( delegate.getIntParameter( cached_property_names[1] )),
				new Integer( delegate.getIntParameter( cached_property_names[2] )),
				new Integer( delegate.getIntParameter( cached_property_names[3] )),
				new Integer( delegate.getIntParameter( cached_property_names[4] )),
				new Integer( delegate.getIntParameter( cached_property_names[5] )),
				new Integer( delegate.getIntParameter( cached_property_names[6] )),
				new Integer( delegate.getIntParameter( cached_property_names[7] )),
				new Integer( delegate.getIntParameter( cached_property_names[8] )),
				new Integer( delegate.getIntParameter( cached_property_names[9] ))
		};		
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	public void
	_setRemote(
		RPRequestDispatcher		_dispatcher )
	{
		super._setRemote( _dispatcher );
		
		property_cache	= new Properties();
		
		for (int i=0;i<cached_property_names.length;i++){
			
			// System.out.println( "cache:" + cached_property_names[i] + "=" + cached_property_values[i] );
			
			property_cache.put(cached_property_names[i],cached_property_values[i]);
		}
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		Object[] params = (Object[])request.getParams();
		
		if ( method.equals( "getPluginIntParameter[String,int]")){
			
			return( new RPReply( new Integer( delegate.getPluginIntParameter((String)params[0],((Integer)params[1]).intValue()))));
			
		}else if ( method.equals( "getPluginStringParameter[String,String]")){
				
			return( new RPReply( delegate.getPluginStringParameter((String)params[0],(String)params[1])));
		
		}else if ( method.equals( "setPluginParameter[String,int]")){
				
			delegate.setPluginParameter((String)params[0],((Integer)params[1]).intValue());
				
			return( null );
			
		}else if ( 	method.equals( "getIntParameter[String,int]") ||
				 	method.equals( "getParameter[String,int]")){
				
			return( new RPReply( new Integer( delegate.getIntParameter((String)params[0],((Integer)params[1]).intValue()))));
				
		}else if ( method.equals( "setParameter[String,int]")){
					
			delegate.setIntParameter((String)params[0],((Integer)params[1]).intValue());
			
			return( null );
			
		}else if ( method.equals( "save")){
			
			try{ 
				delegate.save();
				
				return( null );
				
			}catch( PluginException e ){
				
				return( new RPReply( e ));
			}
		}			
	
			
		throw( new RPException( "Unknown method: " + method ));
	}

	// ***************************************************

	public boolean
	isNewInstall()
	{
	  	notSupported();
	  	
	  	return( false );
	}
	
	public String
	getPluginConfigKeyPrefix()
	{
	  	notSupported();
	  	
	  	return(null);
	}
	
    public float getFloatParameter(String key) {
	  	notSupported();
	  	
	  	return(0);
    }

    public int getIntParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(0);
	  }

	  public int getIntParameter(String key, int default_value)
	  {
		Integer	res = (Integer)property_cache.get( key );
		
		if ( res == null ){
			
			res = (Integer)_dispatcher.dispatch( new RPRequest( this, "getIntParameter[String,int]", new Object[]{key,new Integer(default_value)} )).getResponse();
		}
		
		return( res.intValue());
	  }
		
	  public void
	  setIntParameter( 
		String	key, 
		int		value )
	  {
	  	property_cache.put( key, new Integer( value ));
	  	
		_dispatcher.dispatch( new RPRequest( this, "setParameter[String,int]", new Object[]{key,new Integer(value)} )).getResponse();
	  }
	  
	  public String getStringParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public String getStringParameter(String name, String _default )
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public boolean getBooleanParameter(String key)
	  {	
	  	notSupported();
	  	
	  	return(false);
	  }
	  
	  public boolean getBooleanParameter(String key, boolean _default )
	  {
	  	notSupported();
	  	
	  	return( false );
	  }
	  
	  public void setBooleanParameter(String key, boolean value )
	  {	
	  	notSupported();
	  }
	  
	  public byte[] getByteParameter(String name, byte[] _default )
	  {
	  	notSupported();
	  	
	  	return( null );
	  }
	   
	  public List
	  getPluginListParameter( String key, List	default_value )
	  {
		  	notSupported();
		  	
		  	return( null );		  
	  }
	 
	  public void
	  setPluginListParameter( String key, List	value )
	  {
		  notSupported();
	  }
	  
	  public Map
	  getPluginMapParameter( String key, Map	default_value )
	  {
		  	notSupported();
		  	
		  	return( null );		  
	  }
	 
	  public void
	  setPluginMapParameter( String key, Map	value )
	  {
		  notSupported();
	  }
	  public int getPluginIntParameter(String key)
	  {	
	  	notSupported();
	  	
	  	return(0);
	  }
	  
	  public int getPluginIntParameter(String key,int defaultValue)
	  {
		Integer	res = (Integer)_dispatcher.dispatch( new RPRequest( this, "getPluginIntParameter[String,int]", new Object[]{key,new Integer(defaultValue)} )).getResponse();
		
		return( res.intValue());
	  }
	  
	  public String getPluginStringParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public String getPluginStringParameter(String key,String defaultValue)
	  {
		String	res = (String)_dispatcher.dispatch( new RPRequest( this, "getPluginStringParameter[String,String]", new Object[]{key,defaultValue} )).getResponse();
		
		return( res );
	  }
	  
	  public boolean getPluginBooleanParameter(String key)
	  {
	  	notSupported();
	  	
	  	return(false);
	  }
	  
	  public byte[] getPluginByteParameter(String key, byte[] defaultValue )
	  {
	  	notSupported();
	  	
	  	return(null);
	  }
	  
	  public boolean getPluginBooleanParameter(String key,boolean defaultValue)
	  {
	  	notSupported();
	  	
	  	return(false);
	  }
	    
	  public void setPluginParameter(String key,int value)
	  {
		_dispatcher.dispatch( new RPRequest( this, "setPluginParameter[String,int]", new Object[]{key,new Integer(value)} ));
	  }
	  
	  public void setPluginParameter(String key,int value,boolean global)
	  {
		  notSupported();
	  }

	  public void setPluginParameter(String key,String value)
	  {
	  	
	  	notSupported();
	  }
	  
	  public void setPluginParameter(String key,boolean value)
	  {  	
	  	notSupported();
	  }
	  
	  public void setPluginParameter(String key,byte[] value)
	  {
	  	notSupported();
	  }
	  
	  public ConfigParameter
	  getParameter(
		String		key )
	  {
	  	notSupported();
	  	
	  	return( null );
	  }
		
	  public ConfigParameter
	  getPluginParameter(
	  		String		key )
	  {
	  	notSupported();
	  	
	  	return( null );
	  }
		
	  public boolean
	  getUnsafeBooleanParameter(
			  String		key,
			  boolean		default_value )
	  {
		  notSupported();

		  return( false );
	  }

	  public void
	  setUnsafeBooleanParameter(
			  String		key,
			  boolean		value )
	  {
		  notSupported();
	  }

	  public int
	  getUnsafeIntParameter(
			  String		key,
			  int		default_value )
	  {
		  notSupported();

		  return( 0 );
	  }

	  public void
	  setUnsafeIntParameter(
			  String		key,
			  int		value )
	  {
		  notSupported();
	  }

	  public long
	  getUnsafeLongParameter(
			  String		key,
			  long		default_value )
	  {
		  notSupported();

		  return( 0 );
	  }

	  public void
	  setUnsafeLongParameter(
			  String		key,
			  long		value )
	  {
		  notSupported();
	  }

	  public float
	  getUnsafeFloatParameter(
			  String		key,
			  float		default_value )
	  {
		  notSupported();

		  return( 0 );
	  }

	  public void
	  setUnsafeFloatParameter(
			  String		key,
			  float		value )
	  {
		  notSupported();
	  }

	  public String
	  getUnsafeStringParameter(
			  String		key,
			  String		default_value )
	  {
		  notSupported();

		  return( null );
	  }

	  public void
	  setUnsafeStringParameter(
			  String		key,
			  String		value )
	  {
		  notSupported();
	  }

	  public Map
	  getUnsafeParameterList()
	  {
		  notSupported();

		  return( null );
	  }
	  
	  public void
	  save()
	  	throws PluginException
	  {
	  	try{
	  		_dispatcher.dispatch( new RPRequest( this, "save", null)).getResponse();
	  		
		}catch( RPException e ){
			
			Throwable cause = e.getCause();
			
			if ( cause instanceof PluginException ){
				
				throw((PluginException)cause);
			}
			
			throw( e );
		}
	  }
    
		public File
		getPluginUserFile(
			String	name )
		{
			notSupported();
			
			return( null );
		}
		
		public void
		addListener(
			final PluginConfigListener	l )
		{
			notSupported();
		}

		// @see org.gudy.azureus2.plugins.PluginConfig#setPluginConfigKeyPrefix(java.lang.String)
		
		public void setPluginConfigKeyPrefix(String _key) {
			// TODO Auto-generated method stub
			
		}
		
		public boolean hasParameter(String x) {notSupported(); return false;}
		public boolean hasPluginParameter(String x) {notSupported(); return false;}
		public boolean removePluginParameter(String x) {notSupported(); return false;}
		public boolean removePluginColorParameter(String x) {notSupported(); return false;}
		
		  public byte[] getByteParameter(String key) {notSupported(); return null;}
		  public float getFloatParameter(String key, float default_value) {notSupported(); return 0;}
		  public long getLongParameter(String key) {notSupported(); return 0;}
		  public long getLongParameter(String key, long default_value) {notSupported(); return 0;}
		  public void setByteParameter(String key, byte[] value) {notSupported();}
		  public void setFloatParameter(String key, float value) {notSupported();}
		  public void setLongParameter(String key, long value) {notSupported();}
		  public void setStringParameter(String key, String value) {notSupported();}
		  public byte[] getPluginByteParameter(String key) {notSupported(); return null;}
		  public float getPluginFloatParameter(String key) {notSupported(); return 0;}
		  public float getPluginFloatParameter(String key, float default_value) {notSupported(); return 0;}
		  public long getPluginLongParameter(String key) {notSupported(); return 0;}
		  public long getPluginLongParameter(String key, long default_value) {notSupported(); return 0;}
		  public void setPluginParameter(String key, float value) {notSupported();}
		  public void setPluginParameter(String key, long value) {notSupported();}
		  public boolean getUnsafeBooleanParameter(String key) {notSupported(); return false;}
		  public byte[] getUnsafeByteParameter(String key) {notSupported(); return null;}
		  public byte[] getUnsafeByteParameter(String key, byte[] default_value) {notSupported(); return null;}
		  public float getUnsafeFloatParameter(String key) {notSupported(); return 0;}
		  public int getUnsafeIntParameter(String key) {notSupported(); return 0;}
		  public long getUnsafeLongParameter(String key) {notSupported(); return 0;}
		  public String getUnsafeStringParameter(String key) {notSupported(); return null;}
		  public void setUnsafeByteParameter(String key, byte[] value) {notSupported();}

		  public boolean getCoreBooleanParameter(String key) {notSupported(); return false;}
		  public boolean getCoreBooleanParameter(String key, boolean default_value) {notSupported(); return false;}
		  public byte[] getCoreByteParameter(String key, byte[] default_value) {notSupported(); return null;}
		  public byte[] getCoreByteParameter(String key) {notSupported(); return null;}
		  public float getCoreFloatParameter(String key) {notSupported(); return 0;}
		  public float getCoreFloatParameter(String key, float default_value) {notSupported(); return 0;}
		  public int getCoreIntParameter(String key) {notSupported(); return 0;}
		  public int getCoreIntParameter(String key, int default_value) {notSupported(); return 0;}
		  public String getCoreStringParameter(String key) {notSupported(); return null;}
		  public String getCoreStringParameter(String key, String default_value) {notSupported(); return null;}
		  public long getCoreLongParameter(String key) {notSupported(); return 0;}
		  public long getCoreLongParameter(String key, long default_value) {notSupported(); return 0;}
		  public void setCoreBooleanParameter(String key, boolean value) {notSupported();}
		  public void setCoreByteParameter(String key, byte[] value) {notSupported();}
		  public void setCoreFloatParameter(String key, float value) {notSupported();}
		  public void setCoreIntParameter(String key, int value) {notSupported();}
		  public void setCoreLongParameter(String key, long value) {notSupported();}
		  public void setCoreStringParameter(String key, String value) {notSupported();}
		  
		  public int[] getCoreColorParameter(String key) {notSupported(); return null;}
		  public int[] getCoreColorParameter(String key, int[] default_value) {notSupported(); return null;}
		  public void setCoreColorParameter(String key, int[] value) {notSupported();}
		  public void setCoreColorParameter(String key, int[] value, boolean override) {notSupported();}
		  public int[] getPluginColorParameter(String key) {notSupported(); return null;}
		  public int[] getPluginColorParameter(String key, int[] default_value) {notSupported(); return null;}
		  public int[] getPluginColorParameter(String key, int[] default_value, boolean override) {notSupported(); return null;}
		  public void setPluginColorParameter(String key, int[] value) {notSupported();}
		  public void setPluginColorParameter(String key, int[] value, boolean override) {notSupported();}
		  public int[] getUnsafeColorParameter(String key) {notSupported(); return null;}
		  public int[] getUnsafeColorParameter(String key, int[] default_value) {notSupported(); return null;}
		  public void setUnsafeColorParameter(String key, int[] default_value) {notSupported();}
		  public void setUnsafeColorParameter(String key, int[] default_value, boolean override) {notSupported();}
		  public PluginConfigSource getPluginConfigSource() {notSupported(); return null;}
		  public void setPluginConfigSource(PluginConfigSource source) {notSupported();}
		  public PluginConfigSource enableExternalConfigSource() {notSupported(); return null;}
		  
		  public void setPluginStringListParameter(String key, String[] value)  {notSupported();}
		  public String[] getPluginStringListParameter(String key)  {notSupported(); return null;}
}
