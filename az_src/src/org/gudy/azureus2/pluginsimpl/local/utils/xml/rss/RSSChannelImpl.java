/*
 * Created on 02-Jan-2005
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

package org.gudy.azureus2.pluginsimpl.local.utils.xml.rss;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.xml.rss.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.*;

/**
 * @author parg
 *
 */

public class 
RSSChannelImpl
	implements RSSChannel
{
	private SimpleXMLParserDocumentNode	node;
	
	private RSSItem[]	items;
	
	protected
	RSSChannelImpl(
		SimpleXMLParserDocumentNode	_node )
	{
		node	= _node;
		
		SimpleXMLParserDocumentNode[]	xml_items = node.getChildren();
		
		List	its = new ArrayList();
		
		for (int i=0;i<xml_items.length;i++){
			
			SimpleXMLParserDocumentNode	xml_item = xml_items[i];
			
			if ( xml_item.getName().equalsIgnoreCase("item")){
				
				its.add( new RSSItemImpl( xml_item ));
			}
		}
		
		items	= new RSSItem[ its.size()];
		
		its.toArray( items );
	}
	
	public String
	getTitle()
	{
		return( node.getChild( "title" ).getValue());
	}
	
	public String
	getDescription()
	{
		return( node.getChild( "description" ).getValue());
	}
	
	public URL
	getLink()
	{
		try{
			return( new URL( node.getChild("link").getValue()));
			
		}catch( MalformedURLException e ){
		
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public Date
	getPublicationDate()
	{
			// optional attribute
		
		SimpleXMLParserDocumentNode	pd = node.getChild( "pubdate" );
		
		if ( pd == null ){
			
			return( null );
		}
		
		return( RSSUtils.parseDate( pd.getValue()));
	}
	
	public RSSItem[]
	getItems()
	{
		return( items );
	}
	
	public SimpleXMLParserDocumentNode
	getNode()
	{
		return( node );
	}
}
