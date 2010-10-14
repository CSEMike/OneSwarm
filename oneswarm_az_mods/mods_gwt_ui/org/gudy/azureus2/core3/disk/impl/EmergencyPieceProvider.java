/**
 * 
 */
package org.gudy.azureus2.core3.disk.impl;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.PieceRTAProvider;

public class EmergencyPieceProvider implements PieceRTAProvider {

	private long[] pieceDeadlines;

	public void activate(PiecePicker picker) {

		int num_pieces = picker.getNumberOfPieces();
		pieceDeadlines = new long[num_pieces];
		picker.addRTAProvider(this);
		System.out.println("activating emergency piece picker");
	}

	public void deactivate(PiecePicker picker) {

		picker.removeRTAProvider(this);
		pieceDeadlines = null;

	}

	public void boostPiece(int pieceNum) {
		System.out.println("emergency piece picker: boosting piece: "
				+ pieceNum);
		pieceDeadlines[pieceNum] = System.currentTimeMillis();
	}

	// public long[] updatePriorities(PiecePicker picker) {
	// System.out.println("emergencyPiecePicker: updatePrioritiesCalled");
	// return (piece_priorities);
	// }

	public long getBlockingPosition() {
		System.out
				.println("EmergencyPieceProvider: get block position called");
		return 0;
	}

	public long getCurrentPosition() {
		System.out
				.println("EmergencyPieceProvider: get current position called");
		return 0;
	}

	public long[] updateRTAs(PiecePicker picker) {
		// System.out.println("EmergencyPieceProvider: updateRTAs called");
		return pieceDeadlines;
	}

	public long getStartPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getUserAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setBufferMillis(long millis) {
		// TODO Auto-generated method stub
		
	}
}