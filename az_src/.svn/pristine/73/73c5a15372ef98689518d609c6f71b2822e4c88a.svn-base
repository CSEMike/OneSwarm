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
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */

public class 
RSSItemImpl
	implements RSSItem
{
	private boolean							is_atom;
	private SimpleXMLParserDocumentNode		node;
	
	protected
	RSSItemImpl(
		SimpleXMLParserDocumentNode		_node,
		boolean							_is_atom )
	{
		is_atom	= _is_atom;
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
		SimpleXMLParserDocumentNode link_node = node.getChild( "link" );
		
		if ( link_node != null ){

			try{
				if ( is_atom ){
					
					SimpleXMLParserDocumentAttribute attr = link_node.getAttribute( "href" );
					
					if ( attr == null ){
						
						return( null );
					}
					
					String	value = attr.getValue().trim();
					
					if ( value.length() == 0 ){
						
						return( null );
					}
					
					return( new URL( value ));

				}else{
				
					String	value = link_node.getValue().trim();
					
					if ( value.length() == 0 ){
						
						return( null );
					}
					
					return( new URL( value ));
				}
			}catch( Throwable e ){
			
				Debug.printStackTrace(e);
				
				return( null );
			}
		}
		
		return( null );
	}
	
	public Date
	getPublicationDate()
	{
		SimpleXMLParserDocumentNode pd = node.getChild( is_atom?"published":"pubdate" );
		
		if ( pd != null ){

			if ( is_atom ){
				
				return( RSSUtils.parseAtomDate( pd.getValue()));
				
			}else{
				
				return( RSSUtils.parseRSSDate( pd.getValue()));
			}
		}
		
		return( null );
	}
	
	public String
	getUID()
	{
		SimpleXMLParserDocumentNode uid = node.getChild( is_atom?"id":"guid" );

		if ( uid != null ){
				
			String value = uid.getValue().trim();
			
			if ( value.length() > 0 ){
				
				return( value );
			}
		}
		
		return( null );
	}
	
	public SimpleXMLParserDocumentNode
	getNode()
	{
		return( node );
	}
}
