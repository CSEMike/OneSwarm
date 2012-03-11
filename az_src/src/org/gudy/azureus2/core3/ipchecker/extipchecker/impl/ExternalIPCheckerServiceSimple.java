/*
 * File    : ExternalIPCheckerServiceSimple.java
 * Created : 10-Nov-2003
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
public class 
ExternalIPCheckerServiceSimple
	extends ExternalIPCheckerServiceImpl 
{
	protected String	url;
	
	protected
	ExternalIPCheckerServiceSimple(
		String		_key,
		String		_url )
	{
		super( _key );
		
		url		= _url;
	}
	
	public boolean
	supportsCheck()
	{
		return( true  );
	}

	public void
	initiateCheck(
		long		timeout )
	{
		super.initiateCheck( timeout );
	}
	
	protected void
	initiateCheckSupport()
	{
		reportProgress( "loadingwebpage", url );
		
		String	page = loadPage( url );

		if ( page != null ){
		
			reportProgress( "analysingresponse" );		
				
			String	IP = extractIPAddress( page );
			
			if ( IP != null ){	
		
				reportProgress( "addressextracted", IP );
		
				informSuccess( IP );
			}
		}
	}
}
