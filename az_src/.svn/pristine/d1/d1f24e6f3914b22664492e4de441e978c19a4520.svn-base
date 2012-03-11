/*
 * Created by Joseph Bridgewater
 * Created on Jan 2, 2006
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

package com.aelitis.azureus.core.peermanager.piecepicker;

import java.util.List;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.IndentWriter;


/**
 * @author MjrTom
 *
 */

public interface PiecePicker
{
	public static final int REQUEST_HINT_MAX_LIFE	= 120*1000;
	
    public boolean  hasDownloadablePiece();
    /** @return long value indicated serial number of current count of changes
     * to hasNeededUndonePiece.
     * A method interesting in tracking changes can compare this with a locally stored
     * value to determine if the hasNeededUndonePiece status has changed since the last check. 
     */
    public long     getNeededUndonePieceChange();
    

    public void     addHavePiece(PEPeer peer, int pieceNumber);
    
    /** This is called periodically by the peer control scheduler.
     * It should not normally be called by other methods.
     * It will update the global availability if neccesary
     * and then update the derived information 
     */
    public void     updateAvailability();
    public int[]    getAvailability();
    public int      getAvailability(final int pieceNumber);
    
    public float    getMinAvailability();
    public int		getMaxAvailability();
    public float    getAvgAvail();
	public long 	getAvailWentBadTime();
	
	public void		allocateRequests();

	public boolean	isInEndGameMode();
	public boolean	hasEndGameModeBeenAbandoned();
	public void		clearEndGameChunks();
	/** adds all blocks in the piece to endGameModeChunks
	 * @param pePiece
	 */
	public void		addEndGameChunks(final PEPiece pePiece);

	public void		removeFromEndGameModeChunks(final int pieceNumber, final int offset);
	
	public int	getNumberOfPieces();
	
	
	public int		getNbPiecesDone();
	
	
	public void
	addRTAProvider(
		PieceRTAProvider		shaper );
	
	public void
	removeRTAProvider(
		PieceRTAProvider		shaper );
	
	public List
	getRTAProviders();
	
	public void 
	addPriorityProvider(
		PiecePriorityProvider		shaper );
	
	public void
	removePriorityProvider(
		PiecePriorityProvider		shaper );
	
	public List
	getPriorityProviders();
	
	public void 
	addListener(
		PiecePickerListener		listener );
	
	public void 
	removeListener(
		PiecePickerListener		listener );

	public void
	destroy();
	
	public void
	generateEvidence(
		IndentWriter	writer );
	
	public String
	getPieceString(
		int	piece_number );
}
