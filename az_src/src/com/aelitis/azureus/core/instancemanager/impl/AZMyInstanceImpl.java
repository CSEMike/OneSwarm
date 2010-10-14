/*
 * Created on 20-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.instancemanager.impl;

import java.net.InetAddress;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginListener;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

public class 
AZMyInstanceImpl
	extends AZInstanceImpl
{
	public static final long	FORCE_READ_EXT_MIN	= 8*60*60*1000;
	public static final long	UPNP_READ_MIN		= 5*60*1000;
	
	private AzureusCore				core;
	private AZInstanceManagerImpl	manager;
	
	private String				id;
	private InetAddress			internal_address;
	private int					tcp_port;
	private int					udp_port;
	private int					udp_non_data_port;
	
	private long				last_upnp_read;
	
	private InetAddress			dht_address;
	private long				dht_address_time;
	
	private long				last_force_read_ext;
	private InetAddress			last_external_address;
	
	protected
	AZMyInstanceImpl(
		AzureusCore				_core,
		AZInstanceManagerImpl	_manager )

	{
		core	= _core;
		manager	= _manager;
		
		id	= COConfigurationManager.getStringParameter( "ID", "" );
		
		if ( id.length() == 0 ){
			
			id	= "" + SystemTime.getCurrentTime();
		}
		
		id = ByteFormatter.encodeString( new SHA1Simple().calculateHash( id.getBytes()));
		
		COConfigurationManager.addListener( 
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					readConfig( false );
				}
			});
		
		readConfig( true );
		
		core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				started(
					AzureusCore		core )
				{
					core.removeLifecycleListener( this );
					
				    PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
		        	
				    DHTPlugin dht = null;
				    
				    if ( dht_pi != null ){
			    	
				    	dht = (DHTPlugin)dht_pi.getPlugin();
				    	
				    	if ( dht != null ){
				    		
				        	dht.addListener(
				        		new DHTPluginListener()
				        		{
				        			public void
				        			localAddressChanged(
				        				DHTPluginContact	local_contact )
				        			{
				        				dht_address 		= local_contact.getAddress().getAddress();
				        				dht_address_time	= SystemTime.getCurrentTime();
				        				
				        				manager.informChanged( AZMyInstanceImpl.this );
				        			}
				        		});
				    	}
				    }
				}
			});
	}
	
	protected void
	readConfig(
		boolean	first_time )
	{
		InetAddress	new_internal_address	= NetworkAdmin.getSingleton().getSingleHomedServiceBindAddress();
		
		if ( new_internal_address == null ){
			
			try{
				new_internal_address = InetAddress.getByName( "0.0.0.0" );
				
			}catch( Throwable e ){			
			}
		}
				
		int	new_tcp_port 			= TCPNetworkManager.getSingleton().getTCPListeningPortNumber();
		int	new_udp_port 			= UDPNetworkManager.getSingleton().getUDPListeningPortNumber();
		int new_udp_non_data_port	= UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber();
		
		boolean	same = true;
		
		if ( !first_time ){
			
			same = 	internal_address.equals( new_internal_address) &&
					tcp_port == new_tcp_port &&
					udp_port == new_udp_port &&
					udp_non_data_port == new_udp_non_data_port;
		}
		
		internal_address 	= new_internal_address;
		tcp_port			= new_tcp_port;
		udp_port			= new_udp_port;
		udp_non_data_port	= new_udp_non_data_port;
		
		if ( !same ){
			
			manager.informChanged( this );
		}
	}
	
	protected InetAddress
	readExternalAddress()
	{
		InetAddress	 external_address = null;

			// no point in kicking off any queries if we're closing
		
		if ( manager.isClosing()){
			
			external_address	= last_external_address;
			
			if ( external_address == null ){
				
				try{
					external_address = InetAddress.getByName("127.0.0.1");
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			return( external_address );
		}
		
	    PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
        	
	    DHTPlugin dht = null;
	    
	    if ( dht_pi != null ){
    	
	    	dht = (DHTPlugin)dht_pi.getPlugin();
	    }
	    
	    	// if DHT has informed us of an address then we use this - most reliable up to date one
	    	// unless the version server cache time is more recent
	    
	    if ( dht_address != null && dht_address_time <= SystemTime.getCurrentTime()){
	    	
	    	long cache_time = VersionCheckClient.getSingleton().getCacheTime( false );
	    	 
	    	if ( cache_time <= dht_address_time ){
	    		
	    		external_address = dht_address;
	    	}
	    }

	    if ( 	external_address == null &&
	    		( dht == null || dht.getStatus() != DHTPlugin.STATUS_RUNNING )){
		
	    		// use cached version if available and the DHT isn't

			String	str_address = VersionCheckClient.getSingleton().getExternalIpAddress( true, false );
		
			if ( str_address != null ){
				
				try{
					
					external_address	= InetAddress.getByName( str_address );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		if ( external_address == null && dht != null  ){
			
				// no cache, use DHT (this will hang during initialisation, hence the use of cached
				// version above
			
			try{
				external_address = dht.getLocalAddress().getAddress().getAddress();
	        	
			}catch( Throwable e ){
			}
		}
		
		long	now = SystemTime.getCurrentTime();
		
		if ( last_force_read_ext > now ){
			
			last_force_read_ext	= now;
		}
		
		boolean	ok_to_try_ext = now - last_force_read_ext > FORCE_READ_EXT_MIN;
		
	    	// try upnp - limit frequency unless external read is possible in which
			// case we try upnp first
			// currently we only use UPnP to validate our current external address, not
			// to deduce new ones (as for example there may be multiple upnp devices and
			// we don't know which one to believe
	   
		if ( external_address == null && last_external_address != null ){
			
			if ( last_upnp_read > now ){
				
				last_upnp_read = now;
			}
			
			if ( now - last_upnp_read > UPNP_READ_MIN || ok_to_try_ext ){
				
				last_upnp_read	= now;
				
				try{
				    PluginInterface upnp_pi = core.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
			        			    
				    if ( upnp_pi != null ){
			    	
				    	UPnPPlugin upnp = (UPnPPlugin)upnp_pi.getPlugin();
				    	
				    	String[]	addresses = upnp.getExternalIPAddresses();
				    	
				    	for (int i=0;i<addresses.length;i++){
				    		
				    		if ( addresses[i].equals( last_external_address.getHostAddress())){
				    			
				    			external_address = last_external_address; 
				    			
				    			break;
				    		}
				    	}
				    }
				}catch( Throwable e ){
				}
			}
		}
		
		if ( external_address == null ){
			
				// force read it
			
			if ( ok_to_try_ext ){
				
				last_force_read_ext	= now;
				
				external_address = core.getPluginManager().getDefaultPluginInterface().getUtilities().getPublicAddress();
			}
		}
		
			// no good address available		
		
		if ( external_address == null ){
				
			if ( last_external_address != null ){
				
				external_address = last_external_address;
				
			}else{
				try{
					external_address = InetAddress.getByName("127.0.0.1");
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}else{
			
			last_external_address	= external_address;
		}
		
		return( external_address );
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public InetAddress
	getInternalAddress()
	{
		return( internal_address );
	}
	
	public InetAddress
	getExternalAddress()
	{
		return( readExternalAddress());
	}
	
	public int
	getTCPListenPort()
	{
		return( tcp_port );
	}
	
	public int
	getUDPListenPort()
	{
		return( udp_port );
	}
	
	public int 
	getUDPNonDataListenPort() 
	{
		return( udp_non_data_port );
	}
}
