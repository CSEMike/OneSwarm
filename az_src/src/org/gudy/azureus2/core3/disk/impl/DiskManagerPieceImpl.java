/*
 * Created on 08-Oct-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.disk.impl;

/**
 * @author parg
 * @author MjrTom
 *			2005/Oct/08: startPriority/resumePriority handling and minor clock fixes
 *			2006/Jan/02: refactoring, change booleans to statusFlags
 */

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;

public class DiskManagerPieceImpl
	implements DiskManagerPiece
{
    //private static final LogIDs LOGID = LogIDs.PIECES;

	private static final byte	PIECE_STATUS_NEEDED		= 0x01;	//want to have the piece
	private static final byte	PIECE_STATUS_WRITTEN	= 0x20;	//piece fully written to storage
	private static final byte	PIECE_STATUS_CHECKING	= 0x40;	//piece is being hash checked
       
    private static final byte PIECE_STATUS_MASK_DOWNLOADABLE	=
    	PIECE_STATUS_CHECKING | PIECE_STATUS_WRITTEN | PIECE_STATUS_NEEDED;

    										// 0x65;    // Needed IS once again included in this
		
	private static final byte PIECE_STATUS_MASK_NEEDS_CHECK 	= PIECE_STATUS_CHECKING | PIECE_STATUS_WRITTEN;

    //private static boolean statusTested =false;

    private final DiskManagerHelper	diskManager;
	private final int				pieceNumber;
	
		/** the number of blocks in this piece: can be short as this gives up to .5GB piece sizes with 16K blocks */
	
	private final short				nbBlocks;
	
	// to save memory the "written" field is only maintained for pieces that are
	// downloading. A value of "null" means that either the piece hasn't started 
	// download or that it is complete.
	// access to "written" is single-threaded (by the peer manager) apart from when
	// the disk manager is saving resume data.
	// actually this is not longer strictly true, as setDone is called asynchronously
	// however, this issue can be worked around by working on a reference to the written data
	// as problems only occur when switching from all-written to done=true, both of which signify
	// the same state of affairs.
	
	protected volatile boolean[]	written;

    private byte         statusFlags;
    
	/** it's *very* important to accurately maintain the "done" state of a piece. Currently the statusFlags
	 * are updated in a non-thread-safe manner so a 'done' field is maintained seperatly.  Synchronizing
	 * access to statusFlags or done would cause a tremendous performance hit.
	 */
    private short		read_count;
    
    private boolean		done;
    
	public DiskManagerPieceImpl(final DiskManagerHelper _disk_manager, final int pieceIndex, int length )
	{
		diskManager =_disk_manager;
		pieceNumber = pieceIndex;
		
		nbBlocks =(short)((length +DiskManager.BLOCK_SIZE -1) /DiskManager.BLOCK_SIZE);

		statusFlags = PIECE_STATUS_NEEDED;
	}
	
	public DiskManager getManager()
	{
		return diskManager;
	}
	
	public int getPieceNumber()
	{
		return pieceNumber;
	}
	
	/**
	 * @return int number of bytes in the piece
	 */
	public int getLength()
	{
		return( diskManager.getPieceLength( pieceNumber ));
	}
	
	public int getNbBlocks()
	{
		return nbBlocks;
	}
	
	public short
	getReadCount()
	{
		return( read_count );
	}
	
	public void
	setReadCount(
		short	c )
	{
		read_count	= c;
	}
	
    public int getBlockSize(final int blockNumber)
    {
        if ( blockNumber == nbBlocks -1 ){
        
        	int	len = getLength() % DiskManager.BLOCK_SIZE;
        	
        	if ( len != 0 ){
        		
        		return( len );
        	}
        }
        
        return DiskManager.BLOCK_SIZE;
    }
    
    public boolean
    isSkipped()
    {
		final DMPieceList pieceList =diskManager.getPieceList(pieceNumber);
		for (int i =0; i <pieceList.size(); i++){
			if ( !pieceList.get(i).getFile().isSkipped()){
				return( false );
			}
		}
		return( true );
    }
    
	public boolean isNeeded()
	{
		return (statusFlags &PIECE_STATUS_NEEDED) !=0;
	}

	public boolean calcNeeded()
	{
		boolean filesNeeded =false;
		final DMPieceList pieceList =diskManager.getPieceList(pieceNumber);
		for (int i =0; i <pieceList.size(); i++)
		{
			final DiskManagerFileInfoImpl file =pieceList.get(i).getFile();
			final long fileLength =file.getLength();
			filesNeeded |=fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped();
		}
		if (filesNeeded)
		{
			statusFlags |=PIECE_STATUS_NEEDED;
			return true;
		}
		statusFlags &=~PIECE_STATUS_NEEDED;
		return false;
	}

	public void clearNeeded()
	{
		statusFlags &=~PIECE_STATUS_NEEDED;
	}

	public void setNeeded()
	{
		statusFlags |=PIECE_STATUS_NEEDED;
	}

	public void setNeeded(boolean b)
	{
		if (b)
			statusFlags |=PIECE_STATUS_NEEDED;
		else
			statusFlags &=~PIECE_STATUS_NEEDED;
	}

	public boolean isWritten()
	{
		return (statusFlags &PIECE_STATUS_WRITTEN) !=0;
	}


	/** written[] can be null, in which case if the piece is Done,
	*  all blocks are complete otherwise no blocks are complete
	*/
	public boolean[] getWritten()
	{
		return written;
	}

	public boolean isWritten(final int blockNumber)
	{
		if (done)
			return true;
		final boolean[] writtenRef =written;
		if (writtenRef ==null)
			return false;
		return writtenRef[blockNumber];
	}

	public int getNbWritten()
	{
		if (done)
			return nbBlocks;
		final boolean[] writtenRef =written;
		if (writtenRef ==null)
			return 0;
		int res =0;
		for (int i =0; i <nbBlocks; i++ )
		{
			if (writtenRef[i])
				res++;
		}
		return res;
	}

	public void setWritten(final int blockNumber)
	{
		if (written ==null)
			written =new boolean[nbBlocks];
		final boolean[] written_ref =written;
		
		written_ref[blockNumber] =true;
		for (int i =0; i <nbBlocks; i++)
		{
			if (!written_ref[i])
				return;
		}
		statusFlags |=PIECE_STATUS_WRITTEN;
	}

	public boolean isChecking()
	{
		return (statusFlags &PIECE_STATUS_CHECKING) !=0;
	}

	public void setChecking()
	{
		statusFlags |=PIECE_STATUS_CHECKING;
	}

    public boolean isNeedsCheck()
    {
    	return !done &&(statusFlags &PIECE_STATUS_MASK_NEEDS_CHECK) ==PIECE_STATUS_WRITTEN;
    }


    // this cannot be implemented the same as others could be
	// because the true state of Done is only determined by
	// having gone through setDoneSupport()
	public boolean calcDone()
	{
		return done;
	}

	public boolean isDone()
	{
		return done;
	}

	public void setDone(boolean b)
	{
		// we delegate this operation to the disk manager so it can synchronise the activity
        if (b !=done)
        {
            diskManager.setPieceDone(this, b);
        }
	}

	/** this is ONLY used by the disk manager to update the done state while synchronized
	 *i.e. don't use it else where!
	 * @param b
	 */
	
	public void setDoneSupport(final boolean b)
	{
        done =b;
        if (done)
            written =null;
	}

	public void setDownloadable()
	{		
		setDone(false);
		statusFlags &=~(PIECE_STATUS_MASK_DOWNLOADABLE);
		calcNeeded();	// Needed wouldn't have been calced before if couldn't download more
	}

	public boolean isDownloadable()
	{
		return !done &&(statusFlags &PIECE_STATUS_MASK_DOWNLOADABLE) == PIECE_STATUS_NEEDED;
	}
	
	/**
	 * @return true if the piece is Needed and not Done
	 */
	public boolean isInteresting()
	{
		return !done &&(statusFlags &PIECE_STATUS_NEEDED) != 0;
	}
    
	public void reset()
	{
		setDownloadable();
		written =null;
	}

	public void reDownloadBlock(int blockNumber)
	{
		final boolean[] written_ref = written;
		if (written_ref !=null)
		{
			written_ref[blockNumber] =false;
			setDownloadable();
		}
	}
    
 
    /*
    public static final void testStatus()
    {
        if (statusTested)
            return;
        
        statusTested =true;
        int originalStatus =statusFlags;
        
        for (int i =0; i <0x100; i++)
        {
            statusFlags =i;
            Logger.log(new LogEvent(this, LOGID, LogEvent.LT_INFORMATION,
                "Done:" +isDone()
                +"  Checking:" +isChecking()
                +"  Written:" +isWritten()
                +"  Downloaded:" +isDownloaded()
                +"  Requested:" +isRequested()
                +"  Needed:" +isNeeded()
                +"  Interesting:" +isInteresting()
                +"  Requestable:" +isRequestable()
                +"  EGMActive:" +isEGMActive()
                +"  EGMIgnored:" +isEGMIgnored()
            ));
        }
        statusFlags =originalStatus;
    }
    */
	
	public String
	getString()
	{
		String	text = "";
		
		text += ( isNeeded()?"needed,":"" );
		text += ( isDone()?"done,":"" );
		
		if ( !isDone()){
			text += ( isDownloadable()?"downable,":"" );
			text += ( isWritten()?"written":("written " + getNbWritten())) + ",";
			text += ( isChecking()?"checking":"" );
		}
		
		if ( text.endsWith(",")){
			text = text.substring(0,text.length()-1);
		}
		return( text );
	}
}
