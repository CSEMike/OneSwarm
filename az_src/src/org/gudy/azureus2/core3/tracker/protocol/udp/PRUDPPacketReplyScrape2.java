/*
 * File    : PRUDPPacketReplyScrape.java
 * Created : 21-Jan-2004
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
PRUDPPacketReplyScrape2
	extends PRUDPPacketReply
{
	// protected int		interval;
	
	protected static final int BYTES_PER_ENTRY = 12;
	protected int[]		complete;
	protected int[]		incomplete;
	protected int[]		downloaded;
	
	public
	PRUDPPacketReplyScrape2(
		int			trans_id )
	{
		super( PRUDPPacketTracker.ACT_REPLY_SCRAPE, trans_id );
	}
	
	protected
	PRUDPPacketReplyScrape2(
		DataInputStream		is,
		int					trans_id )
	
		throws IOException
	{
		super( PRUDPPacketTracker.ACT_REPLY_SCRAPE, trans_id );
		
		// interval = is.readInt();
		
		complete	= new int[is.available()/BYTES_PER_ENTRY];
		incomplete	= new int[complete.length];
		downloaded	= new int[complete.length];
		
		for (int i=0;i<complete.length;i++){
			
			complete[i] 	= is.readInt();
			downloaded[i] 	= is.readInt();
			incomplete[i] 	= is.readInt();
		}
	}
	
	/*
	public void
	setInterval(
			int		value )
	{
		interval	= value;
	}
	
	public int
	getInterval()
	{
		return( interval );
	}
	*/
	
	public void
	setDetails(
		int[]		_complete,
		int[]		_downloaded,
		int[]		_incomplete )
	{
		complete		= _complete;
		downloaded		= _downloaded;
		incomplete		= _incomplete;
	}
	
	
	public int[]
	getComplete()
	{
		return( complete );
	}
	
	public int[]
	getDownloaded()
	{
		return( downloaded );
	}
	
	public int[]
	getIncomplete()
	{
		return( incomplete );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
	throws IOException
	{
		super.serialise(os);
		
		// os.writeInt( interval );
		
		if ( complete != null ){
			
			for (int i=0;i<complete.length;i++){
				
				os.writeInt( complete[i] );
				os.writeInt( downloaded[i] );
				os.writeInt( incomplete[i] );
			}
		}
	}
	
	public String
	getString()
	{
		String	data = "";
		
		for (int i=0;i<complete.length;i++){
			data += (i==0?"":",") + complete[i] + "/" + incomplete[i] + "/" + downloaded[i];
		}
		return( super.getString()+"[entries="+complete.length+"=" + data +"]");
	}
}
