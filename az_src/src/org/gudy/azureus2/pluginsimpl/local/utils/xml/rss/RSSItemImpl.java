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
import java.util.Date;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSItem;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */

public class 
RSSItemImpl
	implements RSSItem
{
	private SimpleXMLParserDocumentNode		node;
	
	protected
	RSSItemImpl(
		SimpleXMLParserDocumentNode	_node )
	{
		node	= _node;
	}
	
	public String
	getTitle()
	{
		if ( node.getChild( "title" ) != null ){
		
			return( node.getChild( "title" ).getValue());
		}
		
		return( null );
	}
	
	public String
	getDescription()
	{
		if ( node.getChild( "description" ) != null ){
			
			return( node.getChild( "description" ).getValue());
		}
			
		return( null );	
	}
	
	public URL
	getLink()
	{
		if ( node.getChild( "link" ) != null ){

			try{
				return( new URL( node.getChild("link").getValue()));
				
			}catch( MalformedURLException e ){
			
				Debug.printStackTrace(e);
				
				return( null );
			}
		}
		
		return( null );
	}
	
	public Date
	getPublicationDate()
	{
		if ( node.getChild( "pubdate" ) != null ){

			return( RSSUtils.parseDate( node.getChild( "pubdate" ).getValue()));
		}
		
		return( null );
	}
	
	public SimpleXMLParserDocumentNode
	getNode()
	{
		return( node );
	}
}
