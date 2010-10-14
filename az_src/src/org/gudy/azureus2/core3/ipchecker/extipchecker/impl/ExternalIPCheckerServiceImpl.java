/*
 * File    : ExternalIPCheckerServiceImpl.java
 * Created : 09-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.ipchecker.extipchecker.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.ipchecker.extipchecker.*;
import org.gudy.azureus2.core3.util.*;

public abstract class 
ExternalIPCheckerServiceImpl 
	implements ExternalIPCheckerService, Cloneable
{
	private static final int		MAX_PAGE_SIZE	= 4096;
	private static final String	MSG_KEY_ROOT	= "IPChecker.external";
	
	private String		name;
	private String		description;
	private String		url;
	
	private boolean		completed;
	
	private Vector		listeners	= new Vector();
	private AEMonitor		this_mon	= new AEMonitor( "ExtIPCheckServ");
	
	private AESemaphore	timeout_sem	= new AESemaphore( "ExtIPCheckServ" );
	
	protected
	ExternalIPCheckerServiceImpl(
		String	name_key )
	{
		name 		= MessageText.getString( name_key + ".name" );
		description = MessageText.getString( name_key + ".description" );
		url			= MessageText.getString( name_key + ".url" );
	}
	
	public void
	initiateCheck(
		long		timeout )
	{
		_clone().initiateCheckSupport(timeout);
	}
	
	protected ExternalIPCheckerServiceImpl
	_clone()
	{
		try{
			return((ExternalIPCheckerServiceImpl)clone());
			
		}catch( CloneNotSupportedException e){
			
			Debug.printStackTrace( e );
			
			return( null );
		}
	}
	
	protected void
	initiateCheckSupport(
		final long		timeout )
	{
	
		Thread	t = 
			new AEThread("IPChecker")
			{
				public void
				runSupport()
				{
					try{
						
						initiateCheckSupport();
						
					}finally{
						
						setComplete();
					}
				}
			};
			
		t.setDaemon( true );
		
		t.start();
		
		if ( timeout > 0 ){
			
			Thread	t2 = 
				new AEThread("IPChecker2")
				{
					public void
					runSupport()
					{
						try{
							
							if ( !timeout_sem.reserve( timeout )){
							
								if ( !completed ){
									
									informFailure( "timeout" );
									
									setComplete();
								}
							}
						}catch( Throwable e ){
								
							Debug.printStackTrace( e );
						}
					}
				};
			
			t2.setDaemon( true );
		
			t2.start();
			
		}
	}
	
	protected abstract void
	initiateCheckSupport();
	
	protected void
	setComplete()
	{
		completed = true;
	}
	
	protected String
	loadPage(
		String		url_string )
	{
		try{
		
			URL	url = new URL( url_string );

			HttpURLConnection	connection 	= null;
			InputStream			is			= null;
			
			try{
				connection = (HttpURLConnection)url.openConnection();
				
				int	response = connection.getResponseCode();
				
				if( response == HttpURLConnection.HTTP_ACCEPTED || response == HttpURLConnection.HTTP_OK ){
								
					is = connection.getInputStream();
					
					String	page = "";
					
					while( page.length() < MAX_PAGE_SIZE ){
						
						byte[]	buffer = new byte[2048];
						
						int	len = is.read( buffer );
						
						if ( len < 0 ){
							
							break;
						}
						
						page += new String(buffer, 0, len);
					}
					
					return( page );
					
				}else{
					
					informFailure( "httpinvalidresponse", "" + response );
					
					return( null );
				}
			}finally{
				
				try{
				
					if ( is != null ){
						
						is.close();
					}
					
					if ( connection != null ){
						
						connection.disconnect();
					}
				}catch( Throwable e){
					
					Debug.printStackTrace( e );
				}
			}
		}catch( Throwable e ){
			
			informFailure( "httploadfail", e.toString());
			
			return( null );
		}
	}

	protected String
	extractIPAddress(
		String		str )
	{
		int		pos = 0;
		
		while(pos < str.length()){
			
			int	p1 = str.indexOf( '.', pos );
			
			if ( p1 == -1 ){
				
				informFailure( "ipnotfound"  );
				
				return( null );
			}
			
			if ( p1 > 0 ){
				
				if ( Character.isDigit(str.charAt(p1-1))){
					
					int	p2 = p1-1;
					
					while(p2>=0&&Character.isDigit(str.charAt(p2))){
						
						p2--;
					}
					
					p2++;
					
					int	p3 = p2+1;
					
					int	dots = 0;
					
					while(p3<str.length()){
						
						char	c = str.charAt(p3);
						
						if ( c == '.' ){
					
							dots++;
									
						}else if ( Character.isDigit( c )){
					
						}else{
							
							break;
						}
					
						p3++;
					}
					
					if ( dots == 3 ){
						
						return( str.substring(p2,p3));
					}
				}
			}
			
			pos	= p1+1;
		}
		
		informFailure( "ipnotfound"  );

		return( null );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public String
	getDescription()
	{
		return( description );
	}
	
	public String
	getURL()
	{
		return( url );
	}
	
	protected  void
	informSuccess(
		String		ip )
	{
		try{
			this_mon.enter();			
			
			if ( !completed ){
				
				timeout_sem.releaseForever();

				for ( int i=0;i<listeners.size();i++){
					
					((ExternalIPCheckerServiceListener)listeners.elementAt(i)).checkComplete( this, ip );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	informFailure(
		String		msg_key )
	{
		try{
			this_mon.enter();
	
			informFailure( msg_key, null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	informFailure(
		String		msg_key,
		String		extra )
	{
		try{
			this_mon.enter();
	
			if ( !completed ){
			
				timeout_sem.releaseForever();
				
				String	message = MessageText.getString( MSG_KEY_ROOT + "." + msg_key );
				
				if ( extra != null ){
					
					message += ": " + extra;
				}
				
				for ( int i=0;i<listeners.size();i++){
					
					((ExternalIPCheckerServiceListener)listeners.elementAt(i)).checkFailed( this, message );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	reportProgress(
		String		msg_key )
	{
		try{
			this_mon.enter();
	
			reportProgress( msg_key,  null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	reportProgress(
			String		msg_key,
			String		extra )
	{
		try{
			this_mon.enter();
	
			if ( !completed ){
			
				String	message = MessageText.getString( MSG_KEY_ROOT.concat(".").concat(msg_key) );
				
				if ( extra != null ){
					
					message = message.concat(": ").concat(extra);
				}
				for ( int i=0;i<listeners.size();i++){
					
					((ExternalIPCheckerServiceListener)listeners.elementAt(i)).reportProgress( this, message );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addListener(
		ExternalIPCheckerServiceListener	l )
	{
		try{
			this_mon.enter();
	
			listeners.addElement( l );
			
		}finally{
			
			this_mon.exit();
		}
	}
		
	public void
	removeListener(
		ExternalIPCheckerServiceListener	l )
	{
		try{
			this_mon.enter();
	
			listeners.removeElement( l );
			
		}finally{
			
			this_mon.exit();
		}
	}
}
