/*
 * Created on 03-May-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

import org.gudy.azureus2.core3.logging.*;

public class 
ResourceDownloaderFactoryImpl
	implements ResourceDownloaderFactory
{

	private static final LogIDs LOGID = LogIDs.CORE;
	protected static ResourceDownloaderFactoryImpl	singleton = new ResourceDownloaderFactoryImpl();

	// A list of SourceForge mirrors.
	private static final String[] SF_MIRRORS = new String[] {
		"jaist", "nchc", "keihanna", "optusnet", "peterhost", "ovh", "puzzle",
		"switch", "mesh", "kent", "surfnet", "heanet", "citkit", "internap",
		"cogent", "umn", "easynews", "ufpr"
	};
	
	public static ResourceDownloaderFactory
	getSingleton()
	{
		return( singleton );
	}
	
	public ResourceDownloader
	create(
		File		file )
	{
		return( new ResourceDownloaderFileImpl( null, file ));
	}

	
	public ResourceDownloader
	create(
		URL		url )
	{
		if ( url.getProtocol().equalsIgnoreCase("file")){
			
			try{
				return( new ResourceDownloaderFileImpl( null, new File( new URI( url.toString()))));
			
			}catch( Throwable e ){
				
				return( new ResourceDownloaderURLImpl( null, url ));
			}
		}else{
			
			return( new ResourceDownloaderURLImpl( null, url ));
		}
	}
	
	public ResourceDownloader create(URL url, boolean force_no_proxy) {
		ResourceDownloader rd = create(url);
		if (force_no_proxy && rd instanceof ResourceDownloaderURLImpl) {
			((ResourceDownloaderURLImpl)rd).setForceNoProxy(force_no_proxy);
		}
		return rd;
	}
	
	public ResourceDownloader
	create(
		URL		url,
		String postData)
	{
		return new ResourceDownloaderURLImpl(null, url, postData, false, null, null);
	}
	
	public ResourceDownloader
	create(
		URL		url,
		String	user_name,
		String	password )
	{
		return( new ResourceDownloaderURLImpl( null, url, user_name, password ));
	}
	
	public ResourceDownloader
	create(
		ResourceDownloaderDelayedFactory		factory )
	{
		return( new ResourceDownloaderDelayedImpl( null, factory ));
	}
	
	public ResourceDownloader
	getRetryDownloader(
		ResourceDownloader		downloader,
		int						retry_count )
	{
		ResourceDownloader res = new ResourceDownloaderRetryImpl( null, downloader, retry_count );

		return( res );
	}
	
	public ResourceDownloader
	getTimeoutDownloader(
		ResourceDownloader		downloader,
		int						timeout_millis )
	{
		ResourceDownloader res = new ResourceDownloaderTimeoutImpl( null, downloader, timeout_millis );
		
		return( res );
	}
	
	public ResourceDownloader
	getAlternateDownloader(
		ResourceDownloader[]		downloaders )
	{
		return( getAlternateDownloader( downloaders, -1, false ));
	}
	
	public ResourceDownloader
	getAlternateDownloader(
		ResourceDownloader[]		downloaders,
		int							max_to_try )
	{
		return( getAlternateDownloader( downloaders, max_to_try, false ));
	}
	
	public ResourceDownloader
	getRandomDownloader(
		ResourceDownloader[]		downloaders )
	{
		return( getAlternateDownloader( downloaders, -1, true ));
	}
	
	public ResourceDownloader
	getRandomDownloader(
		ResourceDownloader[]		downloaders,
		int							max_to_try )
	{
		return( getAlternateDownloader( downloaders, max_to_try, true ));
	}
	
	protected ResourceDownloader
	getAlternateDownloader(
		ResourceDownloader[]		downloaders,
		int							max_to_try,
		boolean						random )
	{
		ResourceDownloader res = new ResourceDownloaderAlternateImpl( null, downloaders, max_to_try, random );
				
		return( res );
	}
	
	public ResourceDownloader
	getMetaRefreshDownloader(
		ResourceDownloader			downloader )
	{
		ResourceDownloader res = new ResourceDownloaderMetaRefreshImpl( null, downloader );
				
		return( res );
	}
	
	public ResourceDownloader
	getTorrentDownloader(
		ResourceDownloader			downloader,
		boolean						persistent )
	{
		return( getTorrentDownloader( downloader, persistent, null ));
	}
	
	public ResourceDownloader
	getTorrentDownloader(
		ResourceDownloader			downloader,
		boolean						persistent,
		File						download_directory )
	{
		return( new ResourceDownloaderTorrentImpl( null, downloader, persistent, download_directory ));	
	}
	
	public ResourceDownloader
	getSuffixBasedDownloader(
		ResourceDownloader			_downloader )
	{
		ResourceDownloaderBaseImpl	dl = (ResourceDownloaderBaseImpl)_downloader;
		
		URL	target = null;
		
		while( true ){
			
			List	kids = dl.getChildren();
			
			if ( kids.size() == 0 ){
				
				target = ((ResourceDownloaderURLImpl)dl).getURL();
				
				break;
			}
			
			dl = (ResourceDownloaderBaseImpl)kids.get(0);
		}
		
		if ( target == null ){
			
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "ResourceDownloader: suffix "
						+ "based downloader failed to find leaf"));
			
			return( _downloader );
		}
		
		if ( target.getPath().toLowerCase().endsWith(".torrent")){
			
			return( getTorrentDownloader( _downloader, true ));
			
		}else{
			
			return( _downloader );
		}
	}
	
	public ResourceDownloader[] getSourceforgeDownloaders(String project_name, String filename) {
		String template = "http://%s.dl.sourceforge.net/sourceforge/" + project_name + "/" + filename;
		ResourceDownloader[] result = new ResourceDownloader[SF_MIRRORS.length];
		
		for (int i=0; i<result.length; i++) {
			String url = template.replaceFirst("%s", SF_MIRRORS[i]);
			try {result[i] = create(new URL(url));}
			catch (MalformedURLException me) {throw new RuntimeException(me);}
		}
		return result;
	}
	
	public ResourceDownloader getSourceforgeDownloader(String project_name, String filename) {
		return getRandomDownloader(getSourceforgeDownloaders(project_name, filename));
	}
}
