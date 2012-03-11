/*
 * File    : PEPieceImpl.java
 * Created : 15-Oct-2003
 * By      : Olivier
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

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 * @author MjrTom
 *			2005/Oct/08: numerous changes for new piece-picking
 *			2006/Jan/02: refactoring piece picking to elsewhere, and consolidations
 */

import java.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;


public class PEPieceImpl
    implements PEPiece
{
	private static final LogIDs LOGID = LogIDs.PIECES;
	
	private final DiskManagerPiece	dmPiece;
	private final PEPeerManager		manager;
	
	private final int       nbBlocks;       // number of blocks in this piece
    private long            creationTime;


	private final String[]	requested;
	private boolean			fully_requested;
	
	private final boolean[]	downloaded;
	private boolean			fully_downloaded;
	private long        	time_last_download;

	private final String[] 	writers;
	private List 			writes;
	
	private String			reservedBy;	// using address for when they send bad/disconnect/reconnect
	
	//In end game mode, this limitation isn't used
    private int             speed;      //slower peers dont slow down fast pieces too much
    
    private int             resumePriority;
      
    private Object			real_time_data;
    
	// experimental class level lock
	protected static final AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");
	
    /** piece for tracking partially downloaded pieces
     * @param _manager the PEPeerManager
     * @param _dm_piece the backing dmPiece
     * @param _pieceSpeed the speed threshold for potential new requesters
     */
	public PEPieceImpl(
		PEPeerManager 		_manager, 
		DiskManagerPiece	_dm_piece,
        int                 _pieceSpeed)
	{
        creationTime =SystemTime.getCurrentTime();
		manager =_manager;
		dmPiece =_dm_piece;
        speed =_pieceSpeed;

		nbBlocks =dmPiece.getNbBlocks();

		requested =new String[nbBlocks];
        
        final boolean[] written =dmPiece.getWritten();
		if (written ==null)
			downloaded =new boolean[nbBlocks];
		else
			downloaded =(boolean[])written.clone();

        writers =new String[nbBlocks];
		writes =new ArrayList(0);
	}

    public DiskManagerPiece getDMPiece()
    {
        return dmPiece;
    }

    public long getCreationTime()
    {
        final long now =SystemTime.getCurrentTime();
        if (now >=creationTime &&creationTime >0)
            return creationTime;
        creationTime =now;
        return now;
    }
    
    public long getTimeSinceLastActivity()
    {
        final long now =SystemTime.getCurrentTime();
        final long lastWriteTime =getLastDownloadTime(now);
        if (time_last_download >0 &&now >=time_last_download)
            return now -time_last_download;
        if (creationTime >0 &&now >=creationTime)
            return now -creationTime;
        creationTime =now;
        return 0;
    }
    
	public long getLastDownloadTime(final long now)
	{
		if (time_last_download <=now)
			return time_last_download;
		return time_last_download =now;
	}

	/** Tells if a block has been requested
	 * @param blockNumber the block in question
	 * @return true if the block is Requested already
	 */
	public boolean isRequested(int blockNumber)
	{
		return requested[blockNumber] !=null;
	}
	
	/** Tells if a block has been downloaded
	 * @param blockNumber the block in question
	 * @return true if the block is downloaded already
	 */
	public boolean isDownloaded(int blockNumber)
	{
		return downloaded[blockNumber];
	}
	
	/** This flags the block at the given offset as having been downloaded
     * If all blocks are now downloaed, sets the dmPiece as downloaded
	 * @param blockNumber
	 */
	public void setDownloaded(int offset)
	{
		time_last_download =SystemTime.getCurrentTime();
		downloaded[offset /DiskManager.BLOCK_SIZE] =true;
        for (int i =0; i <nbBlocks; i++)
        {
            if (!downloaded[i])
                return;
        }
        
        fully_downloaded	= true;
        fully_requested		= false;
	}
	
    /** This flags the block at the given offset as NOT having been downloaded
     * and the whole piece as not having been fully downloaded
     * @param blockNumber
     */
    public void clearDownloaded(int offset)
    {
        downloaded[offset /DiskManager.BLOCK_SIZE] =false;
       
        fully_downloaded	= false;
    }
    
    public boolean		
    isDownloaded()
    {
    	return( fully_downloaded );
    }
    
    public boolean[]	
    getDownloaded()
    {
    	return( downloaded );
    }
    
    public boolean
    hasUndownloadedBlock()
    {
    	for (int i =0; i <nbBlocks; i++ ){
    		
			if (!downloaded[i]){
				
				return( true );
			}
		}
    	
    	return( false );
    }
    
	/** This marks a given block as having been written by the given peer
	 * @param peer the PEPeer that sent the data
	 * @param blockNumber the block we're operating on
	 */
	public void setWritten(PEPeer peer, int blockNumber)
	{
		writers[blockNumber] =peer.getIp();
		dmPiece.setWritten(blockNumber);
	}
	
	/** This method clears the requested information for the given block
     * unless the block has already been downloaded, in which case the writer's
     * IP is recorded as a request for the block.
	 */
	public void clearRequested(int blockNumber)
	{
		requested[blockNumber] =downloaded[blockNumber] ?writers[blockNumber] :null;
		
		fully_requested = false;
	}
	
	public boolean      
	isRequested()
	{
		return( fully_requested );
	}
	    
	public void			
	setRequested()
	{
		fully_requested	= true;
	}

	/** This will scan each block looking for requested blocks. For each one, it'll verify
	 * if the PEPeer for it still exists and is still willing and able to upload data.
	 * If not, it'll unmark the block as requested.
	 * @return int of how many were cleared (0 to nbBlocks)
	 */
    /*
	public int checkRequests()
	{
        if (getTimeSinceLastActivity() <30 *1000)
            return 0;
		int cleared =0;
		boolean nullPeer =false;
		for (int i =0; i <nbBlocks; i++)
		{
			if (!downloaded[i] &&!dmPiece.isWritten(i))
			{
				final String			requester =requested[i];
				final PEPeerTransport	pt;
				if (requester !=null)
				{
					pt =manager.getTransportFromAddress(requester);
					if (pt !=null)
					{
						pt.setSnubbed(true);
						if (!pt.isDownloadPossible())
						{
                            clearRequested(i);
							cleared++;
						}
					} else
					{
						nullPeer =true;
                        clearRequested(i);
						cleared++;
					}
				}
			}
		}
		if (cleared >0)
		{
			dmPiece.clearRequested();
            if (Logger.isEnabled())
                Logger.log(new LogEvent(dmPiece.getManager().getTorrent(), LOGID, LogEvent.LT_WARNING,
                        "checkRequests(): piece #" +getPieceNumber()+" cleared " +cleared +" requests."
                        + (nullPeer ?" Null peer was detected." :"")));
		}
		return cleared;
	}
	*/
	
		/*
		 * Parg: replaced above commented out checking with one that verifies that the 
		 * requests still exist. As piece-picker activity and peer disconnect logic is multi-threaded
		 * and full of holes, this is a stop-gap measure to prevent a piece from being left with 
		 * requests that no longer exist
		 */
	
	public void 
	checkRequests()
	{
        if ( getTimeSinceLastActivity() < 30*1000 ){
        	
            return;
        }
        
		int cleared = 0;
				
		for (int i=0; i<nbBlocks; i++){
		
			if (!downloaded[i] &&!dmPiece.isWritten(i)){
			
				final String			requester = requested[i];
				
				if ( requester != null ){
				
					if ( !manager.requestExists( 
							requester, 
							getPieceNumber(),
							i *DiskManager.BLOCK_SIZE, 
							getBlockSize( i ))){

                        clearRequested(i);
                        
						cleared++;
					}
				}
			}
		}
		
		if ( cleared > 0 ){
					
            if (Logger.isEnabled())
                Logger.log(new LogEvent(dmPiece.getManager().getTorrent(), LOGID, LogEvent.LT_WARNING,
                        "checkRequests(): piece #" +getPieceNumber()+" cleared " +cleared +" requests" ));
		}else{
			
			if ( fully_requested && getNbUnrequested() > 0 ){

		          if (Logger.isEnabled())
		                Logger.log(new LogEvent(dmPiece.getManager().getTorrent(), LOGID, LogEvent.LT_WARNING,
		                        "checkRequests(): piece #" +getPieceNumber()+" reset fully requested" ));

				fully_requested = false;
			}
		}
	}
	
    
	/** @return true if the piece has any blocks that are not;
	 *  Downloaded, Requested, or Written
	 */
	public boolean hasUnrequestedBlock()
	{
		final boolean[] written =dmPiece.getWritten();
		for (int i =0; i <nbBlocks; i++ )
		{
			if (!downloaded[i] &&requested[i] ==null &&(written ==null ||!written[i]))
				return true;
		}
		return false;
	}

	/**
	 * This method scans a piece for the first unrequested block.  Upon finding it,
	 * it counts how many are unrequested up to nbWanted.
	 * The blocks are marked as requested by the PEPeer
	 * Assumption - single threaded access to this
	 * TODO: this should return the largest span equal or smaller than nbWanted
	 * OR, probably a different method should do that, so this one can support 'more sequential' picking
	 */
	public int[] getAndMarkBlocks(PEPeer peer, int nbWanted, boolean enable_request_hints )
	{
		final String ip =peer.getIp();
        final boolean[] written =dmPiece.getWritten();
		int blocksFound =0;
		
		if ( enable_request_hints ){
			
			int[]	request_hint = peer.getRequestHint();
			
			if ( request_hint != null && request_hint[0] == dmPiece.getPieceNumber()){
				
					// try to honour the hint first
				
				int	hint_block_start 	= request_hint[1] / DiskManager.BLOCK_SIZE;
				int hint_block_count	=  ( request_hint[2] + DiskManager.BLOCK_SIZE-1 ) / DiskManager.BLOCK_SIZE;
				
				for (int i =hint_block_start; i < nbBlocks && i <hint_block_start + hint_block_count; i++)
				{
					while (blocksFound <nbWanted &&(i +blocksFound) <nbBlocks &&!downloaded[i +blocksFound]
					    &&requested[i +blocksFound] ==null &&(written ==null ||!written[i]))
					{
						requested[i +blocksFound] =ip;
						blocksFound++;
					}
					if (blocksFound >0){
												
						return new int[] {i, blocksFound};
					}
				}
			}
		}
		
		// scan piece to find first free block
		for (int i =0; i <nbBlocks; i++)
		{
			while (blocksFound <nbWanted &&(i +blocksFound) <nbBlocks &&!downloaded[i +blocksFound]
			    &&requested[i +blocksFound] ==null &&(written ==null ||!written[i]))
			{
				requested[i +blocksFound] =ip;
				blocksFound++;
			}
			if (blocksFound >0)
				return new int[] {i, blocksFound};
		}
		return new int[] {-1, 0};
	}
	
	public void getAndMarkBlock(PEPeer peer, int index )
	{
		requested[index] = peer.getIp();
		
		if ( getNbUnrequested() <= 0 ){
			
			setRequested();
		}
	}
	
    public int getNbRequests()
    {
        int result =0;
        for (int i =0; i <nbBlocks; i++)
        {
            if (!downloaded[i] &&requested[i] !=null)
                result++;
        }
        return result;
    }

    public int getNbUnrequested()
    {
        int result =0;
        final boolean[] written =dmPiece.getWritten();
        for (int i =0; i <nbBlocks; i++ )
        {
            if (!downloaded[i] &&requested[i] ==null &&(written ==null ||!written[i]))
                result++;
        }
        return result;
    }

	/**
	 * Assumption - single threaded with getAndMarkBlock
	 */
	public boolean setRequested(PEPeer peer, int blockNumber)
	{
		if (!downloaded[blockNumber])
		{
			requested[blockNumber] =peer.getIp();
			return true;
		}
		return false;
	}
	
	public boolean		
	isRequestable()
	{
		return( dmPiece.isDownloadable() && !( fully_downloaded || fully_requested ));
	}
	
	public int 
	getBlockSize(
		int blockNumber) 
	{
		if ( blockNumber == (nbBlocks - 1)){
			
			int	length = dmPiece.getLength();
			
			if ((length % DiskManager.BLOCK_SIZE) != 0){
				
				return( length % DiskManager.BLOCK_SIZE );
			}
		}
		
		return DiskManager.BLOCK_SIZE;
	}
    
    public int getBlockNumber(int offset)
    {
        return offset /DiskManager.BLOCK_SIZE;
    }
	
	public int getNbBlocks()
	{
		return nbBlocks;
	}

	public List getPieceWrites()
	{
		List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
		}finally{
			
			class_mon.exit();
		}
		return result;
	}
	
	
	public List getPieceWrites(int blockNumber) {
		final List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
			
		}finally{
			
			class_mon.exit();
		}
		final Iterator iter = result.iterator();
		while(iter.hasNext()) {
			final PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			if(write.getBlockNumber() != blockNumber)
				iter.remove();
		}
		return result;
	}
	
	
	public List getPieceWrites(PEPeer peer) {
		final List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
		}finally{
			class_mon.exit();
		}
		final Iterator iter = result.iterator();
		while(iter.hasNext()) {
			PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			if(peer == null || ! peer.getIp().equals(write.getSender()))
				iter.remove();
		}
		return result;
	}
	
	public List 
	getPieceWrites( 
		String	ip ) 
	{
		final List result;
		
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
			
		}finally{
			
			class_mon.exit();
		}
		
		final Iterator iter = result.iterator();
		
		while(iter.hasNext()) {
			
			final PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			
			if ( !write.getSender().equals( ip )){
				
				iter.remove();
			}
		}
		
		return result;
	}

	public void reset()
	{
		dmPiece.reset();
		for (int i =0; i <nbBlocks; i++)
		{
            requested[i] =null;
			downloaded[i] =false;
			writers[i] =null;
		}
		fully_downloaded = false;
		time_last_download = 0;
		reservedBy =null;
		real_time_data=null;
	}

	public Object
	getRealTimeData()
	{
		return( real_time_data );
	}
	
	public void
	setRealTimeData(
		Object	o )
	{
		real_time_data = o;
	}
	
	protected void addWrite(PEPieceWriteImpl write) {
		try{
			class_mon.enter();
			
			writes.add(write);
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public void 
	addWrite(
		int blockNumber,
		String sender, 
		byte[] hash,
		boolean correct	)
	{
		addWrite( new PEPieceWriteImpl( blockNumber, sender, hash, correct ));
	}

	public String[] getWriters()
	{
		return writers;
	}

	public int getSpeed()
	{
		return speed;
	}

	public void setSpeed(int newSpeed)
	{
		speed =newSpeed;
	}

	public void
	setLastRequestedPeerSpeed(
		int		peerSpeed )
	{
		// Up the speed on this piece?
		if (peerSpeed > speed ){
			speed++;
		}
	}

	/**
	 * @return Returns the manager.
	 */
	public PEPeerManager getManager()
	{
		return manager;
	}

	public void setReservedBy(String peer)
	{
		reservedBy =peer;
	}

	public String getReservedBy()
	{
		return reservedBy;
	}

	/** for a block that's already downloadedt, mark up the piece
	 * so that the block will get downloaded again.  This is used
	 * when the piece fails hash-checking.
	 */
	public void reDownloadBlock(int blockNumber)
	{
		downloaded[blockNumber] =false;
		requested[blockNumber] =null;
		fully_downloaded = false;
		writers[blockNumber] = null;
		dmPiece.reDownloadBlock(blockNumber);
	}

	/** finds all blocks downloaded by the given address
	 * and marks them up for re-downloading 
	 * @param address String
	 */
	public void reDownloadBlocks(String address)
	{
		for (int i =0; i <writers.length; i++ )
		{
			final String writer =writers[i];

			if (writer !=null &&writer.equals(address))
				reDownloadBlock(i);
		}
	}

	public void setResumePriority(int p)
	{
		resumePriority =p;
	}

	public int getResumePriority()
	{
		return resumePriority;
	}

    /**
     * @return int of availability in the swarm for this piece
     * @see org.gudy.azureus2.core3.peer.PEPeerManager.getAvailability(int pieceNumber)
     */
    public int getAvailability()
    {
        return manager.getAvailability(dmPiece.getPieceNumber());
    }

    /** This support method returns how many blocks have already been
     * written from the dmPiece
     * @return int from dmPiece.getNbWritten()
     * @see org.gudy.azureus2.core3.disk.DiskManagerPiece.getNbWritten()
     */
    public int getNbWritten()
    {
        return dmPiece.getNbWritten();
    }
    
    /** This support method returns the dmPiece's written array
     * @return boolean[] from the dmPiece
     * @see org.gudy.azureus2.core3.disk.DiskManagerPiece.getWritten()
     */
    public boolean[] getWritten()
    {
        return dmPiece.getWritten();
    }
    public boolean isWritten()
    {
        return dmPiece.isWritten();
    }
    
    public boolean isWritten( int block)
    {
        return dmPiece.isWritten( block );
    }
	public int getPieceNumber()
	{
		return dmPiece.getPieceNumber();
	}

	public int getLength()
	{
		return dmPiece.getLength();
	}

	public void setRequestable()
	{
		fully_downloaded	= false;
		fully_requested		= false;
		
		dmPiece.setDownloadable();
	}

	public String
	getString()
	{
		String	text  = "";
		
		PiecePicker pp = manager.getPiecePicker();

		text	+= ( isRequestable()?"reqable,":"" );
		text	+= "req=" + getNbRequests() + ",";
		text	+= ( isRequested()?"reqstd,":"" );
		text	+= ( isDownloaded()?"downed,":"" );
		text	+= ( getReservedBy()!=null?"resrv,":"" );
		text	+= "speed=" + getSpeed() + ",";
		text	+= ( pp==null?("pri=" + getResumePriority()):pp.getPieceString(dmPiece.getPieceNumber()));
		
		if ( text.endsWith(",")){
			text = text.substring(0,text.length()-1);
		}
		
		return( text );
	}
}