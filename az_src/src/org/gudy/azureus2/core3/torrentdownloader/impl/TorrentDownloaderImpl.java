/*
 * Written and copyright 2001-2003 Tobias Minich.
 * 
 * HTTPDownloader.java
 * 
 * Created on 17. August 2003, 22:22
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

package org.gudy.azureus2.core3.torrentdownloader.impl;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.protocol.magnet.MagnetConnection;
import org.gudy.azureus2.core3.torrent.*;


/**
 * @author Tobias Minich
 */
public class TorrentDownloaderImpl extends AEThread implements TorrentDownloader {

  private String 	url_str;
  private String	referrer;
  private String 	file_str;
  
  private URL url;
  private HttpURLConnection con;
  private String error = "Ok";
  private String status = "";
  private TorrentDownloaderCallBackInterface iface;
  private int state = STATE_NON_INIT;
  private int percentDone = 0;
  private int readTotal = 0;
  private boolean cancel = false;
  private String filename, directoryname;
  private File file = null;
  private byte[] buf = new byte[1020];
  private int bufBytes = 0;
  private boolean deleteFileOnCancel = true;
  

  private AEMonitor this_mon 	= new AEMonitor( "TorrentDownloader" );
	private int errCode;

  public TorrentDownloaderImpl() {
    super("Torrent Downloader");
     setDaemon(true);
  }

  public void 
  init(
  		TorrentDownloaderCallBackInterface	_iface, 
		String 								_url,
		String								_referrer,
		String								_file )
  {
    this.iface = _iface;
    
    //clean up accidental left-facing slashes
    _url = _url.replace( (char)92, (char)47 );
    
    // it's possible that the URL hasn't been encoded (see Bug 878990)
    _url = _url.replaceAll( " ", "%20" );

    setName("TorrentDownloader: " + _url);
    
    url_str 	= _url;
    referrer	= _referrer;
    file_str	= _file;
  }

  public void notifyListener() {
    if (this.iface != null)
      this.iface.TorrentDownloaderEvent(this.state, this);
    else if (this.state == STATE_ERROR)
      System.err.println(this.error);
  }

  private void cleanUpFile() {
    if ((this.file != null) && this.file.exists())
      this.file.delete();
  }

  private void error(int errCode, String err) {
  	try{
  		this_mon.enter();	// what's the point of this?
  	
  		this.state = STATE_ERROR;
  		this.setError(errCode, err);
  		this.cleanUpFile();
  		this.notifyListener();
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void 
  runSupport() {

  	try{
  		new URL( url_str );  //determine if this is already a proper URL
  	}
  	catch( Throwable t ) {  //it's not
  		//check if the string is just a hex-encoded torrent infohash
  		if( url_str.length() == 40 ) {
  			try{
  				//if so, convert to magnet:?xt=urn:btih:ZFQ7PUPQ2QMPFSD6AP4JASDDACA5MZU7 format		
  				byte[] infohash = ByteFormatter.decodeString( url_str.toUpperCase() );  //convert from HEX to raw bytes
  				url_str = "magnet:?xt=urn:btih:" +Base32.encode( infohash );  //convert to BASE32
  			}
  			catch( Throwable e ) {  /*e.printStackTrace();*/		}
  		}
  	}
 
    try {      
    	url = AddressUtils.adjustURL( new URL(url_str));
      
    	String	protocol = url.getProtocol().toLowerCase();
	  
    	// hack here - the magnet download process requires an additional paramter to cause it to
    	// stall on error so the error can be reported
	  
    	if ( protocol.equals( "magnet" )){
		  
    		url = AddressUtils.adjustURL( new URL(url_str+"&pause_on_error=true"));
    	}
	  
    	for (int i=0;i<2;i++){
      	try{
      
	      if ( protocol.equals("https")){
	      	
	      	// see ConfigurationChecker for SSL client defaults
	      	
	      	HttpsURLConnection ssl_con = (HttpsURLConnection)url.openConnection();
	      	
	      	// allow for certs that contain IP addresses rather than dns names
	      	
	      	ssl_con.setHostnameVerifier(
	      			new HostnameVerifier()
	      			{
	      				public boolean
	      				verify(
	      					String		host,
							SSLSession	session )
	      				{
	      					return( true );
	      				}
	      			});
	      	
	      	con = ssl_con;
	      	
	      }else{
	      	
	      	con = (HttpURLConnection) url.openConnection();
	      	
	      }
	      
	      con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);     
	      
	      if ( referrer != null && referrer.length() > 0 ){
	      
	      	con.setRequestProperty( "Referer", referrer );
	      }
	      
	      this.con.connect();
	      
	      break;
	      
      	}catch( SSLException e ){
      		
      		if ( i == 0 ){
				
      			if ( SESecurityManager.installServerCertificates( url ) != null ){
      				
      				// certificate has been installed
					
      				continue;	// retry with new certificate
      			}
      		}

      		throw( e );
      	}
      }
      
      int response = this.con.getResponseCode();
      if ((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
        this.error(response, Integer.toString(response) + ": " + this.con.getResponseMessage());
        return;
      }

      /*
      Map headerFields = this.con.getHeaderFields();
      
      System.out.println("Header of download of " + url_str);
      for (Iterator iter = headerFields.keySet().iterator(); iter.hasNext();) {
				String s = (String) iter.next();
				System.out.println(s + ":" + headerFields.get(s));
				
			}
	*/
      filename = this.con.getHeaderField("Content-Disposition");
      if ((filename!=null) && filename.toLowerCase().matches(".*attachment.*")) // Some code to handle b0rked servers.
        while (filename.toLowerCase().charAt(0)!='a')
          filename = filename.substring(1);
      if ((filename == null) || !filename.toLowerCase().startsWith("attachment") || (filename.indexOf('=') == -1)) {
        String tmp = this.url.getFile();
        if (tmp.length() == 0 || tmp.equals("/")) {
        	filename = url.getHost();
        }
        else if ( tmp.startsWith("?")){
        
        	// probably a magnet URI - use the hash
        	// magnet:?xt=urn:sha1:VGC53ZWCUXUWVGX7LQPVZIYF4L6RXSU6
        	
       	
        	String	query = tmp.toUpperCase();
        		
    		int	pos = query.indexOf( "XT=URN:SHA1:");
    		
    		if ( pos == -1 ){
    			
    	   		pos = query.indexOf( "XT=URN:BTIH:");		
    		}
    		
    		if ( pos != -1 ){
    			
    			pos += 12;
    			
    			int	p2 = query.indexOf( "&", pos );
    			
    			if ( p2 == -1 ){
    				
    				filename = query.substring(pos);
    				
    			}else{
    				
    				filename = query.substring(pos,p2);
    			}
        	}else{
        		
        		filename = "Torrent" + (long)(Math.random()*Long.MAX_VALUE);
        	}
    		
    		
    		filename += ".tmp";
    		
        }else{
	        if (tmp.lastIndexOf('/') != -1)
	          tmp = tmp.substring(tmp.lastIndexOf('/') + 1);
	        
	        // remove any params in the url
	        
	        int	param_pos = tmp.indexOf('?');
	        
	        if ( param_pos != -1 ){
	          tmp = tmp.substring(0,param_pos);
	        }
	        
	        filename = URLDecoder.decode(tmp, Constants.DEFAULT_ENCODING );
        }
      } else {
        filename = filename.substring(filename.indexOf('=') + 1);
        if (filename.startsWith("\"") && filename.endsWith("\""))
          filename = filename.substring(1, filename.lastIndexOf('\"'));
        
        filename = URLDecoder.decode(filename, Constants.DEFAULT_ENCODING );
        
        	// not sure of this piece of logic here but I'm not changing it at the moment
        
        File temp = new File(filename);
        filename = temp.getName();
      }

      filename = FileUtil.convertOSSpecificChars( filename );
      
      directoryname = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
      boolean useTorrentSave = COConfigurationManager.getBooleanParameter("Save Torrent Files");

      if (file_str != null) {
      	// not completely sure about the whole logic in this block
        File temp = new File(file_str);

        //if we're not using a default torrent save dir
        if (!useTorrentSave || directoryname.length() == 0) {
          //if it's already a dir
          if (temp.isDirectory()) {
            //use it
            directoryname = temp.getCanonicalPath();
          }
          //it's a file
          else {
            //so use its parent dir
            directoryname = temp.getCanonicalFile().getParent();
          }
        }

        //if it's a file
        if (!temp.isDirectory()) {
          //set the file name
          filename = temp.getName();
        }
      }
      // what would happen here if directoryname == null and file_str == null??
      
      this.state = STATE_INIT;
      this.notifyListener();
    } catch (java.net.MalformedURLException e) {
      this.error(0, "Exception while parsing URL '" + url + "':" + e.getMessage());
    } catch (java.net.UnknownHostException e) {
      this.error(0, "Exception while initializing download of '" + url + "': Unknown Host '" + e.getMessage() + "'");
    } catch (java.io.IOException ioe) {
      this.error(0, "I/O Exception while initializing download of '" + url + "':" + ioe.toString());
    } catch( Throwable e ){
        this.error(0, "Exception while initializing download of '" + url + "':" + e.toString());   	
    }
    
    if ( this.state == STATE_ERROR ){
    	
    	return;
    }
    
    try{
		final boolean	status_reader_run[] = { true };
    
    	this.state = STATE_START;
      
    	notifyListener();
      
    	this.state = STATE_DOWNLOADING;
      
    	notifyListener();
  
        Thread	status_reader = 
        	new AEThread( "TorrentDownloader:statusreader" )
			{
        		public void
				runSupport()
        		{
        			boolean changed_status	= false;
        			
        			while( true ){
        				
        				try{
        					Thread.sleep(250);
        					
        					try{
        						this_mon.enter();
        						
        						if ( !status_reader_run[0] ){
        						
        							break;
        						}
        					}finally{
        						
        						this_mon.exit();
        					}
        					
        					String	s = con.getResponseMessage();
        					        					
        					if ( !s.equals( getStatus())){
        						
        						if ( !s.toLowerCase().startsWith("error:")){
        							
        							setStatus(s);
        							
        						}else{
        							
        							error(con.getResponseCode(), s.substring(6));
        						}
        						
        						changed_status	= true;
        					}
        				}catch( Throwable e ){
        					
        					break;
        				}
        			}
        			
        			if ( changed_status ){
        				
        				setStatus( "" );
        			}
        		}
			};
			
		status_reader.setDaemon( true );
		
		status_reader.start();
  
		InputStream in;
			
		try{
			in = this.con.getInputStream();
				
		}finally{
			
			try{ 
				this_mon.enter();
					
				status_reader_run[0]	= false;
				
			}finally{
					
				this_mon.exit();
			}
		}
			
	    if ( this.state != STATE_ERROR ){
		    	
	    	this.file = new File(this.directoryname, filename);

	    	boolean useTempFile = false;
	    	try {
	    		this.file.createNewFile();
	    		useTempFile = !this.file.exists();
	    	} catch (Throwable t) {
	    		useTempFile = true;
	    	}
	    	
	    	if (useTempFile) {
	    		this.file = File.createTempFile("AZU", ".torrent", new File(
							this.directoryname));
	    		this.file.createNewFile();
	    	}
	        
	        FileOutputStream fileout = new FileOutputStream(this.file, false);
	        
	        bufBytes = 0;
	        
	        int size = this.con.getContentLength();
	        
			this.percentDone = -1;
			
	        do {
	          if (this.cancel){
	            break;
	          }
	          
	          try {
	          	bufBytes = in.read(buf);
	            
	            this.readTotal += bufBytes;
	            
	            if (size != 0){
	              this.percentDone = (100 * this.readTotal) / size;
	            }
	            
	            notifyListener();
	            
	          } catch (IOException e) {
	          }
	          
	          if (bufBytes > 0){
	            fileout.write(buf, 0, bufBytes);
	          }
	        } while (bufBytes > 0);
	        
	        in.close();
	        
	        fileout.flush();
	        
	        fileout.close();
	        
	        if (this.cancel) {
	          this.state = STATE_CANCELLED;
	          if (deleteFileOnCancel) {
	          	this.cleanUpFile();
	          }
	        } else {
	          if (this.readTotal <= 0) {
	            this.error(0, "No data contained in '" + this.url.toString() + "'");
	            return;
	          }
	          
	          	// if the file has come down with a not-so-useful name then we try to rename
	          	// it to something more useful
	          
	          try{
	          	if ( !filename.toLowerCase().endsWith(".torrent" )){
	
	          		TOTorrent	torrent = TorrentUtils.readFromFile( file, false );
	          		
	          		String	name = TorrentUtils.getLocalisedName( torrent ) + ".torrent";
	          		
	          		File	new_file	= new File( directoryname, name );
	          		
	          		if ( file.renameTo( new_file )){
	          			
	          			filename	= name;
					
	          			file	= new_file;
	          		}
	          	}
	          }catch( Throwable e ){
	          		
	          	Debug.printStackTrace( e );
	          }
	          
	          this.state = STATE_FINISHED;
	        }
	        this.notifyListener();
	      }
      } catch (Exception e) {
    	  
    	if ( !cancel ){
    		
    		Debug.out("'" + this.directoryname + "' '" +  filename + "'", e);
    	}
      	
        this.error(0, "Exception while downloading '" + this.url.toString() + "':" + e.getMessage());
      }
  }

  public boolean 
  equals(Object obj) 
  {
    if (this == obj){
    	
      return true;
    }
    
    if ( obj instanceof TorrentDownloaderImpl ){
    	
      TorrentDownloaderImpl other = (TorrentDownloaderImpl) obj;
      
      if (other.getURL().equals(this.url.toString())){
    	  
    	  File	other_file 	= other.getFile();
    	  File	this_file	= file;
    	  
    	  if ( other_file == this_file ){
    		  
    		  return( true );
    	  }
    	  
    	  if ( other_file == null || this_file == null ){
    		  
    		  return( false );
    	  }
    	  
    	  return( other_file.getAbsolutePath().equals(this_file.getAbsolutePath()));
    	  
      	}else{
      
      		return false;
      	}
    }else{
    	return false;
    }
  }

  
  public int hashCode() {  return this.url.hashCode();  }
  
  
  
  public String getError() {
    return this.error;
  }

  public void setError(int errCode, String err) {
    this.error = err;
    this.errCode = errCode;
  }
  
  public int getErrorCode() {
  	return errCode;
  }

  protected void
  setStatus(
  	String	str )
  {
  	status	= str;
  	notifyListener();
  }
  
  public String
  getStatus()
  {
  	return( status );
  }
  
  public java.io.File getFile() {
    if ((!this.isAlive()) || (this.file == null))
      this.file = new File(this.directoryname, filename);
    return this.file;
  }

  public int getPercentDone() {
    return this.percentDone;
  }

  public int getDownloadState() {
    return this.state;
  }

  public void setDownloadState(int state) {
    this.state = state;
  }

  public String getURL() {
    return this.url.toString();
  }

  public void cancel() {
    this.cancel = true;
    if ( con instanceof MagnetConnection ){
    	con.disconnect();
    }
  }

  public void setDownloadPath(String path, String file) {
    if (!this.isAlive()) {
      if (path != null)
        this.directoryname = path;
      if (file != null)
        filename = file;
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader#getTotalRead()
   */
  public int getTotalRead() {
    return this.readTotal;
  }

  public byte[] getLastReadBytes() {
  	if (bufBytes <= 0) {
  		return new byte[0];
  	}
  	byte[] bytes = new byte[bufBytes];
  	System.arraycopy(buf, 0, bytes, 0, bufBytes);
  	return bytes;
  }

  public int getLastReadCount() {
  	return bufBytes;
  }
  
  public void setDeleteFileOnCancel(boolean deleteFileOnCancel) {
  	this.deleteFileOnCancel = deleteFileOnCancel;
  }
  
  public boolean getDeleteFileOnCancel() {
  	return deleteFileOnCancel;
  }
}
