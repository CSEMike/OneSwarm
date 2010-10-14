/*
 * File    : PRUDPPacketError.java
 * Created : 20-Jan-2004
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

package org.gudy.azureus2.core3.tracker.protocol.udp;

/**
 * @author parg
 *
 */

import java.io.*;

import com.aelitis.net.udp.uc.PRUDPPacketReply;

public class 
PRUDPPacketReplyError
extends PRUDPPacketReply
{
	protected String	message;
	
	public
	PRUDPPacketReplyError(
		int			trans_id,
		String		_message )
	{
		super( PRUDPPacketTracker.ACT_REPLY_ERROR, trans_id );
		
		message	= _message;
	}
	
	protected
	PRUDPPacketReplyError(
		DataInputStream		is,
		int					trans_id )
	
		throws IOException
	{
		super( PRUDPPacketTracker.ACT_REPLY_ERROR, trans_id );
		
		int	avail = is.available();
		
		byte[]	data = new byte[avail];
		
		is.read( data );
		
		message	= new String( data );
	}
	
	public String
	getMessage()
	{
		return( message );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		byte[]	data = message.getBytes();
		
		os.write( data );
	}
	
	public String
	getString()
	{
		return( super.getString().concat(",[msg=").concat(message).concat("]"));
	}
}
