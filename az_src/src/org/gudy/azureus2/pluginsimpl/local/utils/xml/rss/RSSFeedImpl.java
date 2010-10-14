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

import java.io.InputStream;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSChannel;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */

public class 
RSSFeedImpl
	implements RSSFeed
{
	private RSSChannel[]	channels;
	
	public
	RSSFeedImpl(
		Utilities			utilities,
		ResourceDownloader	downloader )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException
	{
		InputStream	is = downloader.download();
		
		try{
			SimpleXMLParserDocument	doc = utilities.getSimpleXMLParserDocumentFactory().create( is );
		
			SimpleXMLParserDocumentNode[]	xml_channels = doc.getChildren();
			
			List	chans = new ArrayList();
			
			for (int i=0;i<xml_channels.length;i++){
				
				SimpleXMLParserDocumentNode	xml_channel = xml_channels[i];
				
				if ( xml_channel.getName().equalsIgnoreCase("channel")){
					
					chans.add( new RSSChannelImpl( xml_channel ));
				}
			}
			
			channels	= new RSSChannel[ chans.size()];
			
			chans.toArray( channels );
			
		}finally{
			
			try{
				is.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public RSSChannel[]
	getChannels()
	{
		return( channels );
	}
}
