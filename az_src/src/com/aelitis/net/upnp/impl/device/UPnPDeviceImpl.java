/*
 * Created on 15-Jun-2004
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

import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */

import java.net.InetAddress;
import java.net.URL;
import java.util.*;


import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.*;
import com.aelitis.net.upnp.impl.services.UPnPServiceImpl;

public class 
UPnPDeviceImpl
	implements UPnPDevice
{
	private UPnPRootDeviceImpl		root_device;
	
	private String	device_type;
	private String	friendly_name;
	private String	manufacturer;
	private String	manufacturer_url;
	private String	model_description;
	private String	model_name;
	private String	model_number;
	private String	model_url;
	private String	presentation_url;
	
	
	private List		devices		= new ArrayList();
	private List		services	= new ArrayList();
	private List<UPnPDeviceImage>		images	= new ArrayList<UPnPDeviceImage>();
	
	protected
	UPnPDeviceImpl(
		UPnPRootDeviceImpl				_root_device,
		String							indent,
		SimpleXMLParserDocumentNode		device_node )
	{
		root_device		= _root_device;
		
		device_type		= getMandatoryField( device_node, "DeviceType" );
		friendly_name	= getMandatoryField( device_node, "FriendlyName" );
		
		/*
		  <modelName>3Com ADSL 11g</modelName> 
		  <modelNumber>1.07</modelNumber> 
		  <modelURL>http://www.3com.com/</modelURL> 
		  */
		
		manufacturer		= getOptionalField( device_node, "manufacturer" );
		manufacturer_url	= getOptionalField( device_node, "manufacturerURL" );
		model_description	= getOptionalField( device_node, "modelDescription" );
		model_name			= getOptionalField( device_node, "modelName" );
		model_number		= getOptionalField( device_node, "modelNumber");
		model_url			= getOptionalField( device_node, "modelURL");
		presentation_url	= getOptionalField( device_node, "presentationURL");
		
		boolean	interested = device_type.equalsIgnoreCase( "urn:schemas-upnp-org:device:WANConnectionDevice:1" );
		
		root_device.getUPnP().log( indent + friendly_name + (interested?" *":""));
		
		SimpleXMLParserDocumentNode	service_list = device_node.getChild( "ServiceList" );
		
		if ( service_list != null ){
				
			SimpleXMLParserDocumentNode[] service_nodes = service_list.getChildren();
			
			for (int i=0;i<service_nodes.length;i++){
				
				services.add( new UPnPServiceImpl( this, indent + "  ", service_nodes[i]));
			}
		}
		
		SimpleXMLParserDocumentNode	dev_list = device_node.getChild( "DeviceList" );
		
		if ( dev_list != null ){
				
			SimpleXMLParserDocumentNode[] device_nodes = dev_list.getChildren();
			
			for (int i=0;i<device_nodes.length;i++){
				
				devices.add( new UPnPDeviceImpl( root_device, indent + "  ", device_nodes[i]));
			}
		}
		
		SimpleXMLParserDocumentNode	icon_list = device_node.getChild( "iconList" );
		if (icon_list != null) {
			SimpleXMLParserDocumentNode[] children = icon_list.getChildren();
			
			for (SimpleXMLParserDocumentNode child : children) {
				if (!"icon".equalsIgnoreCase(child.getName())) {
					continue;
				}
				
				String oUrl = getOptionalField(child, "url");
				if (oUrl == null) {
					continue;
				}
				
				int width = -1;
				int height = -1;
				String oWidth = getOptionalField(child, "width");
				String oHeight = getOptionalField(child, "height");
				try {
					width = Integer.parseInt(oWidth);
					height = Integer.parseInt(oHeight);
				} catch (Throwable t) {
				}
				
				images.add(new UPnPDeviceImageImpl(width, height, oUrl,
						getOptionalField(child, "mime")));
			}
		}
	}
	
	public String
	getAbsoluteURL(
		String	url )
	{
		return( root_device.getAbsoluteURL(url));
	}
	
	public InetAddress
	getLocalAddress()
	{
		return( root_device.getLocalAddress());
	}
	
	public synchronized void
	clearRelativeBaseURL()
	{
		root_device.clearRelativeBaseURL();
	}
	
	public synchronized void
	restoreRelativeBaseURL()
	{
		root_device.restoreRelativeBaseURL();
	}
	
	public UPnPImpl
	getUPnP()
	{
		return( (UPnPImpl)root_device.getUPnP());
	}
	
	public UPnPRootDevice
	getRootDevice()
	{
		return( root_device );
	}
	
	public String
	getDeviceType()
	{
		return( device_type );
	}
	
	public String
	getFriendlyName()
	{
		return( friendly_name );
	}
	
	public String
	getManufacturer()
	{
		return( manufacturer );
	}
	
	public String
	getManufacturerURL()
	{
		return( manufacturer_url );
	}
	
	public String
	getModelDescription()
	{
		return( model_description );
	}
	
	public String
	getModelName()
	{
		return( model_name );
	}
	public String
	getModelNumber()
	{
		return( model_number );
	}
	
	public String
	getModelURL()
	{
		return( model_url );
	}
	
	public String
	getPresentation()
	{
		return( presentation_url==null?null:getAbsoluteURL( presentation_url ));
	}
	
	public UPnPDevice[]
	getSubDevices()
	{
		UPnPDevice[]	res = new UPnPDevice[devices.size()];
		
		devices.toArray( res );
		
		return( res );
	}
	
	public UPnPService[]
	getServices()
	{
		UPnPService[]	res = new UPnPService[services.size()];
		
		services.toArray( res );
		
		return( res );
	}
	
	public UPnPDeviceImage[]
	getImages()
	{
		return images.toArray(new UPnPDeviceImage[0]);
	}
	
	protected String
	getOptionalField(
		SimpleXMLParserDocumentNode	node,
		String						name )
	{
		SimpleXMLParserDocumentNode	child = node.getChild(name);
		
		if ( child == null ){
			
			return( null);
		}
		
		return( child.getValue().trim());
	}
	
	protected String
	getMandatoryField(
		SimpleXMLParserDocumentNode	node,
		String						name )
	{
		SimpleXMLParserDocumentNode	child = node.getChild(name);
		
		if ( child == null ){
			
			root_device.getUPnP().log( "Mandatory field '" + name + "' is missing" );
			
			return( "<missing field '" + name + "'>" );
		}
		
		return( child.getValue().trim());
	}
}
