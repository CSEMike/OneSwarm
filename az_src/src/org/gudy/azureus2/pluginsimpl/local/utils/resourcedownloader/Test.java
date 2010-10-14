/*
 * Created on 25-Apr-2004
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

import java.io.*;
import java.net.*;
import java.util.Properties;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
Test
	implements ResourceDownloaderListener, Plugin
{
	public void
	initialize(
		PluginInterface pi )
	{
		try{
			ResourceDownloaderFactory	rdf = pi.getUtilities().getResourceDownloaderFactory();
			
			// ResourceDownloader rd_t = rdf.create(new URL("http://play.aelitis.com/torrents/Azureus2201-B22.jar.torrent"));
			
			//rd_t = rdf.getMetaRefreshDownloader(rd_t);
			
			//rd_t = rdf.getRetryDownloader(rd_t, 3);
			
			//rd_t = rdf.getTimeoutDownloader(rd_t,1000);
			
			//rd_t = rdf.getTorrentDownloader(rd_t, true, new File("C:\\temp"));

			//ResourceDownloader rd_u = rdf.create(new URL("http://azureus.sourceforge.net/cvs/Azureus2201-B22.jar"));
			
			//rd_u = rdf.getMetaRefreshDownloader(rd_u);
			
			//rd_u = rdf.getRetryDownloader(rd_u, 3);
			
			//rd_u = rdf.getSuffixBasedDownloader(rd_u);

			ResourceDownloader rd_u = rdf.create( new URL( "http://torrents.aelitis.com:88/files/Azureus3009-B5.jar" ));
			
			// rd_u = rdf.getTorrentDownloader( rd_u, true );

			rd_u.addListener(
					new ResourceDownloaderAdapter()
				    {
						public boolean
				        completed(
				        	ResourceDownloader 	downloader,
				            InputStream 		data )
						{
				        	System.out.println( "complete" );

							return( true );
						}
						
				        public void 
						reportPercentComplete(
							ResourceDownloader 	downloader, 
							final int 			percentage )
				        {
				        	System.out.println( "percentage = " + percentage );
				        }
				            
				    	public void
				    	reportAmountComplete(
				    		ResourceDownloader	downloader,
				    		long				amount )
				    	{	
				    		System.out.println( "amount = " + amount );
				    	}
				    	
				        public void 
						reportActivity(
							ResourceDownloader	downloader, 
							String 				activity) 
				        {
				        	System.out.println( "activity = " + activity );
			            }

				        public void 
						failed(
							ResourceDownloader 			downloader,
							ResourceDownloaderException e) 
				        {
				        	System.out.println( "failed" );
				        	
				        	e.printStackTrace();
				        }
				    });

			InputStream is = rd_u.download();

			FileUtil.copyFile( is, new File( "C:\\temp\\file.jar" ));
			
			is.close();
			
			/*
			ResourceDownloader top_downloader =
				rdf.getAlternateDownloader(
						new ResourceDownloader[]{rd_t,rd_u,});

			final long totalk = top_downloader.getSize();
			
			top_downloader.addListener(
				new ResourceDownloaderListener()
			    {
					public boolean
			        completed(
			        		final ResourceDownloader downloader,
			                InputStream data )
					{
			        	System.out.println( "top - complete" );

						return( true );
					}
					
			        public void 
					reportPercentComplete(
						ResourceDownloader 	downloader, 
						final int 			percentage )
			        {
			        	System.out.println( "top - percentage = " + percentage );
			        }
			            

			        public void 
					reportActivity(
						ResourceDownloader	downloader, 
						String 				activity) 
			        {
			        	System.out.println( "top - activity = " + activity );
		            }

			        public void 
					failed(
						ResourceDownloader 			downloader,
						ResourceDownloaderException e) 
			        {
			        	System.out.println( "top - failed" );
			        }
			    });
					
			top_downloader.asyncDownload();
				*/
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
		System.out.println( "percent = " + percentage );
	}
	
	public void
	reportAmountComplete(
		ResourceDownloader	downloader,
		long				amount )
	{	
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		System.out.println( "activity = " + activity );
	}
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		System.out.println( "Completed" );
		
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		System.out.println( "Failed");
		
		Debug.printStackTrace( e );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			PluginManager.registerPlugin( Test.class );
										
			PluginManager.startAzureus( PluginManager.UI_NONE, new Properties() );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
}
