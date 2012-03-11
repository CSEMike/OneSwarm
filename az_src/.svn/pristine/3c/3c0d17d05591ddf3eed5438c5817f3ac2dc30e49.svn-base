/*
 * Created on May 28, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 *
 */

package com.aelitis.azureus.core.peermanager.unchoker;

import java.net.InetAddress;
import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.network.Connection;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

/**
* @author MjrTom
*			2005/Oct/08: s/getLastPiece
*			2006/Jan/02: use RandomUtils
*/

public class UnchokerUtilTest {
  
  private static final int NUM_PEERS_TO_TEST = 100;
  private static final int BYTE_RANGE = 100*1024*1024;
  private static final int TEST_ROUNDS = 1000000;
  
  

  public static void main(String[] args) {
    TreeMap counts = new TreeMap( new Comparator(){
      public int compare( Object  o1, Object  o2 ) {
        PEPeer peer1 = (PEPeer)o1;
        PEPeer peer2 = (PEPeer)o2;
        
        long score1 = peer1.getStats().getTotalDataBytesSent() - peer1.getStats().getTotalDataBytesReceived();
        long score2 = peer2.getStats().getTotalDataBytesSent() - peer2.getStats().getTotalDataBytesReceived();
        
        return (int)(score1 - score2);
      }
    });
    
    ArrayList test_peers = generateTestPeers();
     
    for( int i=0; i < TEST_ROUNDS; i++ ) {
      if( i % 100000 == 0 )  System.out.println( "round=" +i );
      
      PEPeer opt_peer = UnchokerUtil.getNextOptimisticPeer( test_peers, true, false );
      Integer count = (Integer)counts.get( opt_peer );
      if( count == null )  count = new Integer( 0 );
      counts.put( opt_peer, new Integer( count.intValue() + 1 ) );
    }
    
    
    int max_picked = 0;
    
    for( Iterator it = counts.values().iterator(); it.hasNext(); ) {
      int count = ((Integer)it.next()).intValue();
      if( count > max_picked )  max_picked = count;
    }
    
    int pos = 0;
    
    for( Iterator it = counts.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry entry = (Map.Entry)it.next();
      PEPeer peer = (PEPeer)entry.getKey();
      int count = ((Integer)entry.getValue()).intValue();
      
      long score = peer.getStats().getTotalDataBytesSent() - peer.getStats().getTotalDataBytesReceived();
      
      float ratio = (float)peer.getStats().getTotalDataBytesSent() / (peer.getStats().getTotalDataBytesReceived() + 1);

      int percentile = (count *100) / max_picked;      
      
      System.out.println( "[" +pos+ "] score=" +score+ ", ratio=" +ratio+ ", picked=" +count+ "x, percentile=" +percentile+ "%" );
      pos++;
    }
  }

  
  
  
  private static ArrayList generateTestPeers() {
    ArrayList peers = new ArrayList();

    for( int i=0; i < NUM_PEERS_TO_TEST; i++ ) {
      final int bytes_received = RandomUtils.nextInt( BYTE_RANGE );
      final int bytes_sent = RandomUtils.nextInt( BYTE_RANGE );
      
      final PEPeerStats[] f_stats = { null };
      final PEPeer peer = new PEPeer() {
    	public InetAddress getAlternativeIPv6() { return null; }
        public void addListener( PEPeerListener listener ){}
        public void removeListener( PEPeerListener listener ){}
        public int getPeerState(){  return PEPeer.TRANSFERING;  }
        public PEPeerManager getManager(){ return null; }
        public String getPeerSource(){ return null; }
        public byte[] getId(){ return null; }
        public String getIp(){ return null; }
        public int getPort(){ return 0; }
        public String getIPHostName(){ return null; }
        public int getTCPListenPort(){ return 0; }
        public int getUDPListenPort(){ return 0; }
        public int getUDPNonDataListenPort() { return 0;}
        public BitFlags getAvailable(){ return null; }
        public boolean isPieceAvailable(int pieceNumber){ return false; }
        public boolean transferAvailable(){ return true; }
        public boolean isDownloadPossible() { return true; }
        public void setSnubbed(boolean b){}
        public boolean isChokingMe(){ return true;  }
        public boolean isChokedByMe() {  return true;  }
        public void sendChoke(){}
        public void sendUnChoke(){}
        public void sendStatsRequest(Map request) {}
        public boolean isInteresting(){  return true;  }
        public boolean isInterested(){  return true;  }
        public boolean isRelativeSeed() { return false; }
        public boolean isSeed(){ return false;  }
        public boolean isSnubbed(){ return false;  }
        public long getSnubbedTime() { return 0; }
        public boolean hasReceivedBitField() {return false; }
        public PEPeerStats getStats(){  return f_stats[0];  }
        public boolean isIncoming(){ return false;  }
        public int getPercentDoneInThousandNotation(){ return 0; }
        public String getClient(){ return null; }
        public boolean isOptimisticUnchoke(){ return false;  }
        public void setOptimisticUnchoke( boolean is_optimistic ){}
        public void setUploadHint(int timeToSpread){}        
        public int getUploadHint(){ return 0; }        
        public void setUniqueAnnounce(int uniquePieceNumber){}        
        public int getUniqueAnnounce(){ return 0; }
        public Object getData (String key){ return null; }
        public void setData (String key, Object value){}
        public Connection getPluginConnection(){ return null; }
        public boolean supportsMessaging(){ return false;  }
        public int getMessagingMode(){ return PEPeer.MESSAGING_BT_ONLY; }
        public Message[] getSupportedMessages(){ return null; }
        public String getEncryption(){ return( "" ); }
        public String getProtocol(){ return( "" ); }
        public int getReservedPieceNumber() { return -1; }
        public void addReservedPieceNumber(int pieceNumber) {}
        public void removeReservedPieceNumber(int pieceNumber) {}
        public int[] getReservedPieceNumbers() { return null; }
		public int[] getIncomingRequestedPieceNumbers() { return null; }
		public int[] getOutgoingRequestedPieceNumbers() { return null; }
		public int getPercentDoneOfCurrentIncomingRequest(){ return 0; }  
		public int getPercentDoneOfCurrentOutgoingRequest(){ return 0; }  
		public long getTimeSinceConnectionEstablished(){ return 0; }
		public int getLastPiece() { return -1; }
		public void setLastPiece(int pieceNumber) {}
		public int getConsecutiveNoRequestCount() {return 0; }
		public void setConsecutiveNoRequestCount(int num) {}
		public void setSuspendedLazyBitFieldEnabled(boolean enable) {			
		}
		public int getIncomingRequestCount() {
			// TODO Auto-generated method stub
			return 0;
		}
		public int getOutgoingRequestCount() {
			// TODO Auto-generated method stub
			return 0;
		}
		public boolean isLANLocal() {
			// TODO Auto-generated method stub
			return false;
		}
		public boolean sendRequestHint(int piece_number, int offset, int length, int life) {
			// TODO Auto-generated method stub
			return false;
		}
		public int[] getRequestHint() {
			// TODO Auto-generated method stub
			return null;
		}
		public void
		clearRequestHint()
		{
		}
		public void sendRejectRequest(DiskManagerReadRequest request) {
			// TODO Auto-generated method stub
			
		}
		public void setUploadRateLimitBytesPerSecond( int bytes ){}
		public void setDownloadRateLimitBytesPerSecond( int bytes ){}
		public int getUploadRateLimitBytesPerSecond(){ return 0 ;}
		public int getDownloadRateLimitBytesPerSecond(){ return 0; }
		public void addRateLimiter(LimitedRateGroup limiter, boolean upload) {
			// TODO Auto-generated method stub
			
		}
		public void removeRateLimiter(LimitedRateGroup limiter, boolean upload) {
			// TODO Auto-generated method stub
			
		}
		public void setHaveAggregationEnabled(boolean enabled) {
			// TODO Auto-generated method stub
		}
		public int getOutboundDataQueueSize() {
			// TODO Auto-generated method stub
			return 0;
		}
		public byte[] getHandshakeReservedBytes() {
			return null;
		}
		public String getClientNameFromExtensionHandshake() {return null;}
		public String getClientNameFromPeerID() {return null;}
		public long getBytesRemaining() {
			// TODO Auto-generated method stub
			return 0;
		}
		public Object getUserData(Object key) {
			// TODO Auto-generated method stub
			return null;
		}
		public void setUserData(Object key, Object value) {
			// TODO Auto-generated method stub
			
		}
		public boolean isPriorityConnection() {
			// TODO Auto-generated method stub
			return false;
		}
		public void setPriorityConnection(boolean is_priority) {
			// TODO Auto-generated method stub
			
		}
		public boolean isUnchokeOverride() {
			// TODO Auto-generated method stub
			return false;
		}
      };
      
     f_stats[0] = new PEPeerStats() {
    	public PEPeer getPeer() {return( peer );}
    	public void setPeer(PEPeer p) {}
        public void dataBytesSent( int num_bytes ){}
        public void protocolBytesSent( int num_bytes ){}
        public void dataBytesReceived( int num_bytes ){}
        public void protocolBytesReceived( int num_bytes ){}
        public void bytesDiscarded( int num_bytes ){}
        public void hasNewPiece( int piece_size ){}
        public void statisticalSentPiece( int piece_size ){}
        
        public long getDataReceiveRate(){  return 0;  }
        public long getProtocolReceiveRate(){  return 0;  }
        public long getTotalDataBytesReceived(){  return bytes_received;  }
        public long getTotalProtocolBytesReceived(){  return 0;  }
        public long getDataSendRate(){  return 0;  }
        public long getProtocolSendRate(){  return 0;  }
        public long getTotalDataBytesSent(){  return bytes_sent;  }
        public long getTotalProtocolBytesSent(){  return 0;  }
        public long getSmoothDataReceiveRate(){  return 0;  }
        public long getTotalBytesDiscarded(){  return 0;  }
        public long getEstimatedDownloadRateOfPeer(){  return 0;  }
        public long getEstimatedUploadRateOfPeer(){  return 0;  }
        public long getTotalBytesDownloadedByPeer(){  return 0;  }
        public void diskReadComplete( long bytes ){};
        public int getTotalDiskReadCount(){  return 0;  }
        public int getAggregatedDiskReadCount(){  return 0;  }
        public long getTotalDiskReadBytes(){  return 0;  }
        public void setUploadRateLimitBytesPerSecond( int bytes ){}
        public void setDownloadRateLimitBytesPerSecond( int bytes ){}
        public int getUploadRateLimitBytesPerSecond(){return 0;}
        public int getDownloadRateLimitBytesPerSecond(){return 0;}
        public long getEstimatedSecondsToCompletion(){return(0);};
        public int getPermittedBytesToSend(){ return 0; }
        public void permittedSendBytesUsed( int num ){}
        public int getPermittedBytesToReceive(){ return 0; }
        public void permittedReceiveBytesUsed( int num ){}
      };
      peers.add( peer );
    }
    
    return peers;
  }
  
  public static class
  UF
  	extends UnchokerFactory
  {
	public Unchoker 
	getUnchoker(
		boolean seeding) 
	{
		return super.getUnchoker(seeding);
	}  
  } 
}
