/*
 * TorrentDownloaderFactory.java
 *
 * Created on 2. November 2003, 03:52
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 */

package org.gudy.azureus2.core3.torrentdownloader;

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderImpl;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderFactory {
  
  private static TorrentDownloaderImpl getClass(boolean logged) {
    try {
      return (TorrentDownloaderImpl) Class.forName("org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloader"+(logged?"Logged":"")+"Impl").newInstance();
    } catch (Exception e) {
    	Debug.printStackTrace( e );
      return null;
    }
  }
  
  /**
   * creates and initializes a TorrentDownloader object with the specified parameters.
   * NOTE: this does not actually start the TorrentDownloader object
   * @param callback object to notify about torrent download status
   * @param url url of torrent file to download
   * @param referrer url of referrer to set as HTTP_REFERER header when requesting torrent
   * @param fileordir path to a file or directory that the actual 
   *        torrent file should be saved to. if a default save directory is not specified, this will be used instead.
   * 		even if a default save directory is specified, if this parameter path refers to a file, the filename will
   * 		be used when saving the torrent  
   * @param whether or not logging is enabled for the torrent download. this is performed through the TorrentDownloaderLoggedImpl class which is only available in the uis project
   * @return
   */
  public static TorrentDownloader 
  create(
  	TorrentDownloaderCallBackInterface 	callback, 
	String 								url,
	String								referrer,
	String 								fileordir, 
	boolean 							logged ) 
  {
	  return( create( callback, url, referrer, null, fileordir, logged ));
  }
  
  public static TorrentDownloader 
  create(
  		TorrentDownloaderCallBackInterface 	callback, 
		String 								url,
		String								referrer,
		String 								fileordir ) 
  {
    return create(callback, url, referrer, fileordir, false );
  }
  
  public static TorrentDownloader 
  create(
  		TorrentDownloaderCallBackInterface 	callback, 
		String 								url,
		String								referrer,
		Map									request_properties,
		String 								fileordir) 
  {
	  return( create( callback, url, referrer, request_properties, fileordir, false ));
  }
  
  private static TorrentDownloader 
  create(
  		TorrentDownloaderCallBackInterface 	callback, 
		String 								url,
		String								referrer,
		Map									request_properties,
		String 								fileordir,
		boolean								logged )
  {
	  return( new TorrentDownloadRetrier( callback, url, referrer, request_properties, fileordir, logged ));
  }
  
  public static TorrentDownloader create(TorrentDownloaderCallBackInterface callback, String url, boolean logged) {
    return create(callback, url, null, null, logged);
  }
  
  public static TorrentDownloader create(TorrentDownloaderCallBackInterface callback, String url) {
      return create(callback, url, null, null, false);
  }
  
  public static TorrentDownloader create(String url, String fileordir, boolean logged) {
    return create(null, url, null, fileordir, logged);
  }
  
  public static TorrentDownloader create(String url, String fileordir) {
    return create(null, url, null, fileordir, false);
  }
  
  public static TorrentDownloader create(String url, boolean logged) {
    return create(null, url, null, null, logged);
  }
  
  public static TorrentDownloader create(String url) {
    return create(null, url, null, null, false);
  }
  
  public static void initManager(GlobalManager gm, boolean logged, boolean autostart, String downloaddir) {
    TorrentDownloaderManager.getInstance().init(gm, logged, autostart, downloaddir);
  }
    
  public static TorrentDownloader downloadManaged(String url, String fileordir, boolean logged) {
    return TorrentDownloaderManager.getInstance().download(url, fileordir, logged);
  }
  
  public static TorrentDownloader downloadManaged(String url, String fileordir) {
    return TorrentDownloaderManager.getInstance().download(url, fileordir);
  }
  
  public static TorrentDownloader downloadManaged(String url, boolean logged) {
    return TorrentDownloaderManager.getInstance().download(url, logged);
  }
  
  public static TorrentDownloader downloadManaged(String url) {
    return TorrentDownloaderManager.getInstance().download(url);
  }
  
  	private static class
  	TorrentDownloadRetrier
  		implements TorrentDownloader
  	{
		final private String 								url;
		final private String								referrer;
		final private Map									request_properties;
		final private String 								fileordir;
		final private boolean								logged;
  		
		private volatile TorrentDownloaderImpl	delegate;
		
		private volatile boolean	cancelled;
		
		private volatile boolean	sdp_set;
		private volatile String		sdp_path;
		private volatile String		sdp_file;
		
		private volatile boolean	dfoc_set;
		private volatile boolean	dfoc;
		private volatile boolean	irc_set;
		private volatile boolean	irc;
		
  		private
  		TorrentDownloadRetrier(
  			final TorrentDownloaderCallBackInterface 	_callback, 
  			String 										_url,
  			String										_referrer,
  			Map											_request_properties,
  			String 										_fileordir,
  			boolean										_logged )
  		{
  			url					= _url;
  			referrer			= _referrer;
  			request_properties	= _request_properties;
  			fileordir			= _fileordir;
  			logged				= _logged;

  			TorrentDownloaderCallBackInterface callback			= 
  				new TorrentDownloaderCallBackInterface()
  				{
  					private TorrentDownloaderCallBackInterface	original_callback = _callback;
  					
  					private boolean no_retry = original_callback == null;
  					
  					private boolean	init_reported 	= false;
  					private boolean	start_reported	= false;
  					
  					public void 
  					TorrentDownloaderEvent(
  						int 				state, 
  						TorrentDownloader 	_delegate )
  					{
  						if ( _delegate != delegate ){
  							
  							return;
  						}
  						
  						if ( state == STATE_INIT ){
  							
  							if ( init_reported ){
  								
  								return;
  							}
  							
  							init_reported = true;
  						}
  						
 						if ( state == STATE_START ){
  							
  							if ( start_reported ){
  								
  								return;
  							}
  							
  							start_reported = true;
  						}
 						
  						if ( cancelled ){
  							
  							no_retry = true;
  						}
  						
  						if ( no_retry ){
  							
  							if ( original_callback != null ){
  								
  								original_callback.TorrentDownloaderEvent( state, TorrentDownloadRetrier.this );
  							}
  							
  							return;
  						}
  					
  						if ( 	state == STATE_FINISHED ||
  								state == STATE_DUPLICATE ||
  								state == STATE_CANCELLED ){
  					
							if ( original_callback != null ){
  								
  								original_callback.TorrentDownloaderEvent( state, TorrentDownloadRetrier.this );
  							}
							
  							no_retry = true;
  							
  							return;
  						}
  						
  						if ( state == STATE_ERROR ){
  							
  							String lc_url = url.toLowerCase().trim();
  							
  							String	retry_url = null;
  							
  							if ( lc_url.startsWith( "http" )){
  							
  								retry_url = UrlUtils.parseTextForURL( url.substring( 5 ), true );
  							}
  							
  							if ( retry_url != null ){
  													
	  				 			delegate = TorrentDownloaderFactory.getClass( logged );  		  
	  				  			
	  				 			if ( sdp_set ){
	  				 				
	  				 				delegate.setDownloadPath( sdp_path, sdp_file );
	  				 			}
	  				 			
	  				 			if ( dfoc_set ){
	  				 				
	  				 				delegate.setDeleteFileOnCancel( dfoc );
	  				 			}
	  				 			
	  				 			if ( irc_set ){
	  				 				
	  				 				delegate.setIgnoreReponseCode( irc );
	  				 			}
	  				 			
	  				  			delegate.init( this, retry_url, referrer, request_properties, fileordir );
	  				  			
	  							no_retry	= true;
	  							
	  							delegate.start();
	  							
	  							return;
	  							
  							}else{
  								
	  							no_retry	= true;
  							}
  						}						
  						
						if ( original_callback != null ){
								
							original_callback.TorrentDownloaderEvent( state, TorrentDownloadRetrier.this );
						}
  					}
  					 
  				};

  			delegate = TorrentDownloaderFactory.getClass( logged );  		  
  			
  			delegate.init( callback, url, referrer, request_properties, fileordir );
  		}
  		
  		public void 
  		start()
  		{
  			delegate.start();
  		}

  		public void 
  		cancel()
  		{
  			cancelled = true;
  			
  			delegate.cancel();
  		}

  		public void 
  		setDownloadPath(
  			String path, 
  			String file)
  		{
  			sdp_set			= true;
 			sdp_path		= path;
  			sdp_file		= file;

  			delegate.setDownloadPath(path, file); 			
   		}

  		public int 
  		getDownloadState()
  		{
  			return( delegate.getDownloadState());
  		}

  		public File 
  		getFile()
  		{
  			return( delegate.getFile());
  		}

  		public int 
  		getPercentDone()
  		{
  			return( delegate.getPercentDone());
  		}
  		
  		public int 
  		getTotalRead()
  		{
  			return( delegate.getTotalRead());
  		}
  		
  		public String 
  		getError()
  		{
  			return( delegate.getError());
  		}
  		
  		public String 
  		getStatus()
  		{
  			return( delegate.getStatus());
  		}

  		public String 
  		getURL()
  		{
  			return( delegate.getURL());
  		}
  		
  		public int 
  		getLastReadCount()
  		{
  			return( delegate.getLastReadCount());
  		}
  		
  		public byte[] 
  		getLastReadBytes()
  		{
  			return( delegate.getLastReadBytes());
  		}
  		
  		public boolean 
  		getDeleteFileOnCancel()
  		{
  			return( delegate.getDeleteFileOnCancel());
  		}
  		
  		public void 
  		setDeleteFileOnCancel(
  			boolean deleteFileOnCancel )
  		{
  			dfoc_set	= true;
  			dfoc		= deleteFileOnCancel;
  			
  			delegate.setDeleteFileOnCancel( deleteFileOnCancel );
  		}
  		
  		public boolean 
  		isIgnoreReponseCode()
  		{
  			return( delegate.isIgnoreReponseCode());
  		}
  		
  		public void 
  		setIgnoreReponseCode(
  			boolean ignoreReponseCode)
  		{
  			irc_set	= true;
  			irc		= ignoreReponseCode;
  			
  			delegate.setIgnoreReponseCode( ignoreReponseCode );
  		}
  	}
}
