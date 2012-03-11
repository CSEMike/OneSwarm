/*
 * Created on Sep 1, 2003 
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 */
 
package org.gudy.azureus2.core3.disk.impl.piecemapper.impl;

import java.util.List;

import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapEntry;

/**
 * @author Moti
 *
 * PieceList contains a list of pieces; it also provides accessor and
 * utility methods.
 */
public class 
PieceListImpl
	implements DMPieceList
{
	
	final private PieceMapEntryImpl[] pieces;	
	final private int[] cumulativeLengths;
	
	static public PieceListImpl 
	convert(
		List pieceList) 
	{
		return new PieceListImpl((PieceMapEntryImpl[])pieceList.toArray(new PieceMapEntryImpl[pieceList.size()]));	
	}
	
	protected
	PieceListImpl(
		PieceMapEntryImpl[] _pieces) 
	{
		pieces = _pieces;
		cumulativeLengths = new int[pieces.length];
		
		initializeCumulativeLengths();
	}

	private void 
	initializeCumulativeLengths() 
	{
		int runningLength = 0;
		for (int i = 0; i < pieces.length; i++) {
			runningLength += pieces[i].getLength();
			cumulativeLengths[i] = runningLength;
		}
	}
	
	public int size() {
		return pieces.length;	
	}
		
	public DMPieceMapEntry get(int index) {
		return pieces[index];	
	}
	
	public int getCumulativeLengthToPiece(int index) {
		return cumulativeLengths[index];	
	}
}
