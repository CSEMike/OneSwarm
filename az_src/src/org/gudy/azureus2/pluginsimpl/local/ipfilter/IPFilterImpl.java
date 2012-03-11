/*
 * File    : IPFilterImpl.java
 * Created : 02-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.local.ipfilter;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.plugins.ipfilter.*;

import org.gudy.azureus2.core3.ipfilter.*;

public class 
IPFilterImpl
	implements IPFilter
{
	protected IpFilter		filter;
	
	public
	IPFilterImpl()
	{
		filter = IpFilterManagerFactory.getSingleton().getIPFilter();
	}
	
	public File
	getFile()
	{
		return( filter.getFile());
	}
	
	public void
	reload()
	
		throws IPFilterException
	{
		try{
			filter.reload();
			
		}catch( Throwable e ){
			
			throw( new IPFilterException( "IPFilter::reload fails", e ));
		}
	}
	
	public void
	save()
	
		throws IPFilterException
	{
		try{
			filter.save();
			
		}catch( Throwable e ){
			
			throw( new IPFilterException( "IPFilter::reload fails", e ));
		}
	}
	
	public IPRange[]
	getRanges()
	{
		IpRange[] l = filter.getRanges();
		
		IPRange[]	res = new IPRange[l.length];
		
		for (int i=0;i<l.length;i++){
			
			res[i] = new IPRangeImpl(this, l[i]);
		}
		
		return( res );
	}

	public int
	getNumberOfRanges()
	{
		return( filter.getNbRanges());
	}
	
	public int
	getNumberOfBlockedIPs()
	{
		return( filter.getNbIpsBlocked());
	}
	

	public int 
	getNumberOfBannedIPs() 
	{
		return( filter.getNbBannedIps());
	}
	
	public boolean 
	isInRange(
		String IPAddress )
	{
		return( filter.isInRange(IPAddress));
	}
	
	public IPRange
	createRange(
		boolean this_session_only )
	{
		return( new IPRangeImpl( this, filter.createRange( this_session_only )));
	}
		
	public void
	addRange(
		IPRange	range )
	{
		if ( !(range instanceof IPRangeImpl )){
			
			throw( new RuntimeException( "range must be created by createRange"));
		}
		
		filter.addRange(((IPRangeImpl)range).getRange());
	}
	
	public IPRange
	createAndAddRange(
		String		description,
		String		start_ip,
		String		end_ip,
		boolean		this_session_only )
	{
		IPRange	range = createRange( this_session_only );
		
		range.setDescription( description );
		
		range.setStartIP( start_ip );
		
		range.setEndIP( end_ip );
		
		range.checkValid();
		
		if ( range.isValid()){
			
			addRange( range );
			
			return( range );
		}
		
		return( null );
	}
	
	public void
	removeRange(
		IPRange	range )
	{
		if ( !(range instanceof IPRangeImpl )){
			
			throw( new RuntimeException( "range must be created by createRange"));
		}
		
		filter.removeRange(((IPRangeImpl)range).getRange());
	}

	public IPBlocked[]
	getBlockedIPs()
	{
		BlockedIp[]	l = filter.getBlockedIps();
		
		IPBlocked[]	res = new IPBlocked[l.length];
		
		for (int i=0;i<l.length;i++){
			
			res[i] = new IPBlockedImpl(this,l[i]);
		}
		
		return( res );
	}
	
	public void 
	block(
		String IPAddress)
	{
		filter.ban( IPAddress, "<plugin>", false );
	}
	
	public IPBanned[]
 	getBannedIPs()
 	{
 		BannedIp[]	l = filter.getBannedIps();
 		
 		IPBanned[]	res = new IPBanned[l.length];
 		
 		for (int i=0;i<l.length;i++){
 			
 			res[i] = new IPBannedImpl(l[i]);
 		}
 		
 		return( res );
 	}	
	
	public void 
	ban(
		String IPAddress,
		String text)
	{
		filter.ban( IPAddress, text, false );
	}	
	
	public void 
	unban(
		String IPAddress)
	{
		filter.unban( IPAddress );
	}
	
	public boolean
	isEnabled()
	{
		return( filter.isEnabled());
	}
		
	public void
	setEnabled(
		boolean	enabled )
	{
		filter.setEnabled( enabled );
	}
	
	public boolean
	getInRangeAddressesAreAllowed()
	{
		return( filter.getInRangeAddressesAreAllowed());
	}
	
	public void
	setInRangeAddressesAreAllowed(
		boolean	b )
	{
		filter.setInRangeAddressesAreAllowed( b );
	}
	
	public void
	markAsUpToDate()
	{
		filter.markAsUpToDate();
	}
	
	public long
	getLastUpdateTime()
	{
		return( filter.getLastUpdateTime());
	}
}
