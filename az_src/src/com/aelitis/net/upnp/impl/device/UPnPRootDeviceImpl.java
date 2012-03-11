/*
 * Created on 14-Jun-2004
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

package com.aelitis.net.upnp.impl.device;


/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.UPnPImpl;

public class 
UPnPRootDeviceImpl 
	implements  UPnPRootDevice
{
	public static final String	ROUTERS[]				= 
		{ 	"3Com ADSL 11g",
			//"WRT54G",
		};
	
	public static final String	BAD_ROUTER_VERSIONS[]	= 
		{ 	"2.05",
			//"any",
		};
	
	public static final boolean BAD_ROUTER_REPORT_FAIL[]	=
		{
			true,		// report on fail
			//true,		// report always	removed, apparently it works OK now according to manufacturer
		};
	
	final private UPnPImpl			upnp;
	final private NetworkInterface	network_interface;
	final private InetAddress		local_address;
	
	final private String		usn;
	final private URL			location;
	
	private URL			url_base_for_relative_urls;
	private URL			saved_url_base_for_relative_urls;
	
	private String		info;
	
	private UPnPDeviceImpl	root_device;
	
	private boolean			port_mapping_result_received;
	
	private boolean		destroyed;
	
	private List		listeners	= new ArrayList();
	
	public
	UPnPRootDeviceImpl(
		UPnPImpl			_upnp,
		NetworkInterface	_network_interface,
		InetAddress			_local_address,
		String				_usn,
		URL					_location )
	
		throws UPnPException
	{
		upnp				= _upnp;
		network_interface	= _network_interface;
		local_address		= _local_address;
		usn					= _usn;
		location			= _location;
		
		SimpleXMLParserDocument	doc = upnp.downloadXML( this, location );
			
		SimpleXMLParserDocumentNode url_base_node = doc.getChild("URLBase");
		
		try{
			if ( url_base_node != null ){
				
				String	url_str = url_base_node.getValue().trim();
			
					// url_str is sometimes blank
				
				if ( url_str.length() > 0 ){
					
					url_base_for_relative_urls = new URL(url_str);
				}
			}
			
			upnp.log( "Relative URL base is " + (url_base_for_relative_urls==null?"unspecified":url_base_for_relative_urls.toString()));
			
		}catch( MalformedURLException e ){
			
			upnp.log( "Invalid URLBase - " + (url_base_node==null?"mill":url_base_node.getValue()));
			
			upnp.log( e );
			
			Debug.printStackTrace( e );
		}
		
		SimpleXMLParserDocumentNode device = doc.getChild( "Device" );
		
		if ( device == null ){
			
			throw( new UPnPException( "Root device '" + usn + "(" + location + ") is missing the device description" ));
		}
		
		root_device = new UPnPDeviceImpl( this, "", device );
		
		info = root_device.getFriendlyName();
		
		String	version	= root_device.getModelNumber();
		
		if ( version != null ){
			
			info += "/" + version;
		}
	}
	
	public Map 
	getDiscoveryCache() 
	{		
		try{
			Map	cache = new HashMap();

			cache.put( "ni", network_interface.getName().getBytes( "UTF-8" ));
			cache.put( "la", local_address.getHostAddress().getBytes( "UTF-8" ));
			cache.put( "usn", usn.getBytes( "UTF-8" ));
			cache.put( "loc", location.toExternalForm().getBytes( "UTF-8" ));
			
			return( cache );
		
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public void
	portMappingResult(
		boolean	ok )
	{
		if ( port_mapping_result_received ){
			
			return;
		}
		
		port_mapping_result_received	= true;
		
		if ( ok ){
			
			info += "/OK";
			
		}else{

			info += "/Failed";
		}
		
		String	model 	= root_device.getModelName();
		String	version	= root_device.getModelNumber();
		
		if ( model == null || version == null ){
			
			return;
		}
			
		for (int i=0;i<ROUTERS.length;i++){
			
			if ( ROUTERS[i].equals( model )){
				
				if ( isBadVersion( version, BAD_ROUTER_VERSIONS[i])){
					
					boolean	report_on_fail = BAD_ROUTER_REPORT_FAIL[i];
					
					if ( report_on_fail && ok ){
						
						// don't warn here, only warn on failures
						
					}else{
						
						String	url = root_device.getModelURL();
						
						upnp.logAlert( 
								"Device '" + model + "', version '" + version + 
								"' has known problems with UPnP. Please update to the latest software version (see " + 
								(url==null?"the manufacturer's web site":url) + ") and refer to http://wiki.vuze.com/w/UPnP",
								false,
								UPnPLogListener.TYPE_ONCE_EVER );
					}
					
					break;
				}
			}
		}
	}

	public String
	getInfo()
	{
		return( info );
	}
	
	protected String
	getAbsoluteURL(
		String	url )
	{
		String	lc_url = url.toLowerCase().trim();
		
		if ( lc_url.startsWith( "http://") || lc_url.startsWith( "https://" )){
			
				// already absolute
			
			return( url );
		}
		
			// relative URL
		
		if ( url_base_for_relative_urls != null ){
			
			String	abs_url = url_base_for_relative_urls.toString();
			
			if ( !abs_url.endsWith("/")){
				
				abs_url += "/";
			}
			
			if ( url.startsWith("/")){
				
				abs_url += url.substring(1);
				
			}else{
				
				abs_url += url;
			}
			
			return( abs_url );
			
		}else{
		
				// base on the root document location
			
			String	abs_url = location.toString();
		
			int	p1 = abs_url.indexOf( "://" ) + 3;
			
			p1 = abs_url.indexOf( "/", p1 );
			
			abs_url = abs_url.substring( 0, p1 );
			
			return( abs_url + (url.startsWith("/")?"":"/") + url );
		}
	}
	
	protected synchronized void
	clearRelativeBaseURL()
	{
		if ( url_base_for_relative_urls != null ){
			
			saved_url_base_for_relative_urls 	= url_base_for_relative_urls;
			url_base_for_relative_urls			= null;
		}
	}
	
	protected synchronized void
	restoreRelativeBaseURL()
	{
		if ( saved_url_base_for_relative_urls != null ){
			
			url_base_for_relative_urls			= saved_url_base_for_relative_urls;
			saved_url_base_for_relative_urls	= null;
		}
	}
	
	public UPnP
	getUPnP()
	{
		return( upnp );
	}
	
	public NetworkInterface
	getNetworkInterface()
	{
		return( network_interface );
	}
	
	public InetAddress
	getLocalAddress()
	{
		return( local_address );
	}
	
	public String
	getUSN()
	{
		return( usn  );
	}
	
	public URL
	getLocation()
	{
		return( location );
	}
	
	public UPnPDevice
	getDevice()
	{
		return( root_device );
	}
	
	public void
	destroy(
		boolean		replaced )
	{
		destroyed	= true;
		
		for (int i=0;i<listeners.size();i++){
			
			((UPnPRootDeviceListener)listeners.get(i)).lost( this, replaced);
		}
	}
	
	public boolean
	isDestroyed()
	{
		return( destroyed );
	}
	
	public void
	addListener(
		UPnPRootDeviceListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		UPnPRootDeviceListener	l )
	{
		listeners.remove( l );
	}
	
	protected boolean
	isBadVersion(
		String	current,
		String	bad )
	{
		if ( bad.equals( "any" )){
			
			return( true );
		}
			// comparator does A10 -vs- A9 correctly (i.e. 111 is > 20 )
		
		Comparator comp = upnp.getAdapter().getAlphanumericComparator();
		
		//Comparator comp = getAlphanumericComparator( true );
		
			// look for a delimiter (non alpha/numeric)
		
		Set	delimiters = new HashSet();
		
		char	current_delim 	= '1';
		char	bad_delim		= '1';
		
		for (int i=0;i<current.length();i++){
			
			char	c = current.charAt(i);
			
			if ( !Character.isLetterOrDigit( c )){
				
				delimiters.add( new Character( c ));
				
				current_delim = c;
			}
		}
		
		for (int i=0;i<bad.length();i++){
			
			char	c = bad.charAt(i);
			
			if ( !Character.isLetterOrDigit( c )){
				
				delimiters.add( new Character( c ));
				
				bad_delim = c;
			}
		}
		
		if ( 	delimiters.size() != 1 || 
				current_delim != bad_delim ){
			
			return( comp.compare( current, bad ) <= 0 );
		}
		
		StringTokenizer	current_tk 	= new StringTokenizer( current, ""+current_delim );
		StringTokenizer	bad_tk 		= new StringTokenizer( bad, ""+bad_delim );
		
		int	num_current = current_tk.countTokens();
		int	num_bad		= bad_tk.countTokens();
		
		for (int i=0;i<Math.min( num_current, num_bad);i++){
			
			String	current_token 	= current_tk.nextToken();
			String	bad_token 		= bad_tk.nextToken();
			
			int	res = comp.compare( current_token, bad_token );
			
			if ( res != 0 ){
				
				return( res < 0 );
			}
		}
		
		return( num_current <= num_bad );
	}
	

	/*
	public static void
	main(
		String[]	args )
	{
		String[]	test_current	= { "1.11.2" };
		String[]	test_bad		= { "1.11" };
		
		for (int i=0;i<test_current.length;i++){
			
			System.out.println( test_current[i] + " / " + test_bad[i] + " -> " + isBadVersion( test_current[i], test_bad[i] ));
			System.out.println( test_bad[i] + " / " + test_current[i] + " -> " + isBadVersion( test_bad[i], test_current[i] ));
		}
	}
	*/
}
