/*
 * File    : IPRange.java
 * Created : 05-Mar-2004
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

import org.gudy.azureus2.plugins.ipfilter.*;

import org.gudy.azureus2.core3.ipfilter.*;

public class 
IPRangeImpl 
	implements IPRange
{
	private IPFilter		filter;
	private IpRange		range;
	
	protected
	IPRangeImpl(
		IPFilter	_filter,
		IpRange		_range )
	{
		filter	= _filter;
		range	= _range;
	}
	
	protected IpRange
	getRange()
	{
		return( range );
	}
	
	public String
	getDescription()
	{
		return( range.getDescription());
	}
	
	public void
	setDescription(
		String	str )
	{
		range.setDescription(str);
	}
		
	public boolean
	isValid()
	{
		return( range.isValid());
	}
  
	public void
	checkValid()
	{
		range.checkValid();
	}
	
	public boolean
	isSessionOnly()
	{
		return( range.isSessionOnly());
	}
	
	public String
	getStartIP()
	{
		return( range.getStartIp());
	}
	
	public void
	setStartIP(
		String	str )
	{
		range.setStartIp(str);
	}
		
	public String
	getEndIP()
	{
		return( range.getEndIp());
	}
	
	public void
	setEndIP(
		String	str )
	{
		range.setEndIp(str);
	}
  
	public void
	setSessionOnly(
		boolean sessionOnly )
	{
		range.setSessionOnly(sessionOnly);
	}
		
	public boolean 
	isInRange(
		String ipAddress )
	{
		return( range.isInRange(ipAddress));
	}
	
	public void
	delete()
	{
		filter.removeRange( this );
	}
	
	public boolean
	equals(
		Object		other )
	{
		if ( !(other instanceof IPRangeImpl )){
			
			return( false );
		}
		
		return( compareTo( other ) == 0 );
	}
	
	public int
	compareTo(
		Object		other )
	{
		if ( !(other instanceof IPRangeImpl )){
			
			throw( new RuntimeException( "other object must be IPRange" ));
			
		}
		
		IPRangeImpl	o = (IPRangeImpl)other;
		
		String	ip1 = getStartIP();
		String	ip2 = o.getStartIP();
			
		int	res = ip1.compareTo(ip2);
		
		if ( res != 0 ){
			return( res );
		}
		
		ip1	= getEndIP();
		ip2 = o.getEndIP();
		
		if ( ip1 == null && ip2 == null ){
			return(0);
		}
		
		if ( ip1 == null ){
			return( -1 );
		}
		
		if ( ip2 == null ){
			return( 1 );
		}
		
		return( ip1.compareTo(ip2));
	}
}
