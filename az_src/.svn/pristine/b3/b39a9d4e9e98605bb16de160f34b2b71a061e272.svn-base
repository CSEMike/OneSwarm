/*
 * File    : PluginPEPeerStatsWrapper.java
 * Created : 01-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.peers;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.peer.*;

import org.gudy.azureus2.plugins.peers.*;

public class 
PeerStatsImpl 
	implements PeerStats
{
	private PeerManagerImpl		peer_manager;
	private PEPeerManager		manager;
	private PEPeerStats			delegate;
	private Peer				owner;
	
	public
	PeerStatsImpl(
		PeerManagerImpl	_peer_manager,
		Peer			_owner,
		PEPeerStats		_delegate )
	{
		peer_manager	= _peer_manager;
		manager			= peer_manager.getDelegate();
		delegate		= _delegate;
		owner			= _owner;
	}
	
	public PEPeerStats
	getDelegate()
	{
		return( delegate );
	}
	
	public int getDownloadAverage()
	{
		return( (int)delegate.getDataReceiveRate());
	}

	public int getReception()
	{
		return( (int)delegate.getSmoothDataReceiveRate());
	}

	public int getUploadAverage()
	{
		return( (int)delegate.getDataSendRate());
	}
  
	public int getTotalAverage()
	{
		return( (int)delegate.getEstimatedDownloadRateOfPeer());
	}
  
	public long getTotalDiscarded()
	{
		return( delegate.getTotalBytesDiscarded());
	}
 
	public long getTotalSent()
	{
		return( delegate.getTotalDataBytesSent());
	}
  
	public long getTotalReceived()
	{
		return( delegate.getTotalDataBytesReceived());
	}
 
	public int getStatisticSentAverage()
	{
		return( (int)delegate.getEstimatedUploadRateOfPeer());
	}
	
	public int 
	getPermittedBytesToReceive()
	{
		return( delegate.getPermittedBytesToReceive());
	}
	
	public void 
	permittedReceiveBytesUsed( 
		int bytes )
	{
		delegate.permittedReceiveBytesUsed( bytes );

		received( bytes );
	}
	
	public int 
	getPermittedBytesToSend()
	{
		return( delegate.getPermittedBytesToSend());
	}
	
	public void 
	permittedSendBytesUsed( 
		int bytes )
	{
		delegate.permittedSendBytesUsed( bytes );

		sent( bytes );
	}
	
	public void
	received(
		int		bytes )
	{
		delegate.dataBytesReceived( bytes );
		
		manager.dataBytesReceived( delegate.getPeer(), bytes );
	}
	
	public void
	sent(
		int		bytes )
	{		
		delegate.dataBytesSent( bytes );
		
		manager.dataBytesSent( delegate.getPeer(), bytes );
	}
	
	public void
	discarded(
		int		bytes )
	{
		delegate.bytesDiscarded( bytes );
		
		manager.discarded( delegate.getPeer(), bytes );
	}
	
	public long
	getTimeSinceConnectionEstablished()
	{
		return( peer_manager.getTimeSinceConnectionEstablished( owner ));
	}
	
	public int 
	getDownloadRateLimit() 
	{
		return( delegate.getDownloadRateLimitBytesPerSecond());
	}
	
	public void 
	setDownloadRateLimit( 
		int bytes ) 
	{
		delegate.setDownloadRateLimitBytesPerSecond( bytes );
	}
	
	public int 
	getUploadRateLimit() 
	{
		return( delegate.getUploadRateLimitBytesPerSecond());
	}
	
	public void 
	setUploadRateLimit( 
		int bytes ) 
	{
		delegate.setUploadRateLimitBytesPerSecond( bytes );
	}
	
	public long
	getOverallBytesRemaining()
	{
		return( delegate.getPeer().getBytesRemaining());
	}
}
