/*
 * File    : RPDownloadAnnounceResult.java
 * Created : 30-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.remote.download;

/**
 * @author parg
 *
 */
import java.net.URL;

import org.gudy.azureus2.plugins.download.*;

import org.gudy.azureus2.pluginsimpl.remote.*;


public class 
RPDownloadScrapeResult
	extends		RPObject
	implements 	DownloadScrapeResult 
{
	protected transient DownloadScrapeResult		delegate;

		// don't change these field names as they are visible on XML serialisation

	public int				seed_count;
	public int				non_seed_count;
	
	public static RPDownloadScrapeResult
	create(
		DownloadScrapeResult		_delegate )
	{
		RPDownloadScrapeResult	res =(RPDownloadScrapeResult)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPDownloadScrapeResult( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPDownloadScrapeResult(
		DownloadScrapeResult		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (DownloadScrapeResult)_delegate;
		
		seed_count		= delegate.getSeedCount();
		non_seed_count	= delegate.getNonSeedCount();	
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();	
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	
		// ***************************************************
	
	public Download
	getDownload()
	{
		notSupported();

		return( null );
	}
	
	public int
	getResponseType()
	{
		notSupported();

		return( 0 );
	}
		
	public int
	getSeedCount()
	{
		return( seed_count );
	}
	
	public int
	getNonSeedCount()
	{
		return( non_seed_count );
	}
	
	public long getScrapeStartTime() {
		notSupported();

		return( 0 );
	}
	
	public void 
	setNextScrapeStartTime(
		long nextScrapeStartTime)
	{
		notSupported();		
	}
	
	public long
	getNextScrapeStartTime()
	{
		notSupported();
		
		return(0);
	}
	
	public String
	getStatus()
	{
		notSupported();
		
		return( null );
	}
	
	public URL
	getURL()
	{
		notSupported();
		
		return( null );
	}
}