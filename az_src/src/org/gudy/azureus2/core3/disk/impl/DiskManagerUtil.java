/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.core3.disk.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;

public class 
DiskManagerUtil 
{
    private static final LogIDs LOGID = LogIDs.DISK;

	protected static int        max_read_block_size;

	static{

		ParameterListener param_listener = new ParameterListener() {
			public void
			parameterChanged(
					String  str )
			{
				max_read_block_size = COConfigurationManager.getIntParameter( "BT Request Max Block Size" );
			}
		};

		COConfigurationManager.addAndFireParameterListener( "BT Request Max Block Size", param_listener);
	}

	public static boolean
	checkBlockConsistencyForHint(
		DiskManager	dm,
		String		originator,
		int 		pieceNumber,
		int 		offset,
		int 		length)
	{
		if (length <= 0 ) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Hint invalid: " + originator + " length=" + length + " <= 0"));
			return false;
		}
		if (pieceNumber < 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Hint invalid: " + originator + " pieceNumber=" + pieceNumber + " < 0"));
			return false;
		}
		if (pieceNumber >= dm.getNbPieces()) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Hint invalid: " + originator + " pieceNumber=" + pieceNumber + " >= this.nbPieces="
						+ dm.getNbPieces()));
			return false;
		}
		int pLength = dm.getPieceLength(pieceNumber);

		if (offset < 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Hint invalid: " + originator + " offset=" + offset + " < 0"));
			return false;
		}
		if (offset > pLength) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Hint invalid: " + originator + " offset=" + offset + " > pLength=" + pLength));
			return false;
		}
		if (offset + length > pLength) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Hint invalid: " + originator + " offset=" + offset + " + length=" + length
						+ " > pLength=" + pLength));
			return false;
		}

		return true;
	}
	
	public static boolean
	checkBlockConsistencyForRead(
		DiskManager	dm,
		String		originator,
		int 		pieceNumber,
		int 		offset,
		int 		length)
	{
		if ( !checkBlockConsistencyForHint( dm, originator, pieceNumber, offset, length )){
			
			return( false );
		}
		
		if (length > max_read_block_size) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
						"Read invalid: " + originator + " length=" + length + " > " + max_read_block_size));
			return false;
		}

		if(!dm.getPiece(pieceNumber).isDone()) {
			Logger.log(new LogEvent(dm, LOGID, LogEvent.LT_ERROR,
					"Read invalid: " + originator + " piece #" + pieceNumber + " not done"));
			return false;
		}
		
		return true;
	}


}
