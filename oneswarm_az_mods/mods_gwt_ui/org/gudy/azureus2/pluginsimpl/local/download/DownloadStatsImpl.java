/*
 * File    : DownloadStatsImpl.java
 * Created : 08-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.local.download;

/**
 * @author parg
 *
 */

import java.io.InputStream;

import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.impl.EmergencyPieceProvider;
import org.gudy.azureus2.core3.disk.impl.SequentialDiskReaderImpl;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.DownloadStats;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.impl.PiecePickerImpl;

public class DownloadStatsImpl
	implements DownloadStats
{
	//TODO: move this to Download.java
	//	 ****************************************************
	//	 EDIT made by isdal@cs.washington.edu
	/* Made this change to be able to know how much of a file 
	 * that has been downloaded and written to disk
	 * I need to know how many continuous finished bytes there 
	 * are after a certain piece (that piece being the first 
	 * in a file)
	 * This allows us to stream data as the download 
	 * completes
	 */
	//	public long getContinousFinishedBytes(int firstPieceNumber,int numPiecesInFile){
	//		DiskManagerPiece[] pieces = dm.getDiskManager().getPieces();
	//		
	//		System.out.println("calculating download position");
	//		long continousBytes = 0;
	//		int lastPieceNumber = firstPieceNumber + numPiecesInFile;
	//		
	//		System.out.println("first=" + firstPieceNumber + " last=" + lastPieceNumber);
	//		if(pieces.length >= lastPieceNumber){
	//			for (int i = firstPieceNumber; i < lastPieceNumber; i++) {
	//				DiskManagerPiece piece = pieces[i];
	//				
	//				if(piece == null){
	//					// hmm, piece is null... return
	//					System.out.println("piece " + i + " is null"
	//							);
	//					return continousBytes;
	//				} 
	//				// this must be the first unwritten piece... return
	//				if(piece.isDone()){
	//					System.out.println("piece " + i + " is done");
	//					if(!piece.isWritten())
	//						System.out.println("piece " + i + " is not written to disk");
	//					//return continousBytes;
	//				} 
	//				
	//				// piece is finished and written, add piece size
	//				continousBytes += piece.getLength();				
	//			}
	//		} else {
	//			System.out.println("strange: " + "piece num problem, DiskManagerFileInfo says: piece num at least='"
	//					+lastPieceNumber+"'" + " peermanager says: '" + pieces.length+"'");
	//			
	//			Debug.out("piece num problem, DiskManagerFileInfo says: piece num at least='"
	//					+lastPieceNumber+"'" + " peermanager says: '" + pieces.length+"'");
	//		}
	//		
	//		return continousBytes;
	//	}
	public InputStream getFileStream(DiskManagerFileInfo info, long streamRate) {
		return new SequentialDiskReaderImpl(dm, info, streamRate);
	}

	public boolean isFirstLastMbDone(DiskManagerFileInfo info, boolean startIfNot) {
		//		dm.getPeerManager().getP
		int firstPiece = info.getFirstPieceNumber();
		int lastPiece = firstPiece + info.getNumPieces() - 1;

		boolean firstLastDone = true;
		if (dm.getDiskManager() == null) {
			return false;
		}
		if(dm.getPeerManager() == null){
			return false;
		}
		
		if( lastPiece < 0 ) {
			return false;
		}
		
		DiskManagerPiece[] pieces = dm.getDiskManager().getPieces();
		// check the first half meg
		//		int bytesToCheck = 1024 * 1024;
		//		int piecesToCheck = Math.min(bytesToCheck / pieces[firstPiece].getLength(),
		//				lastPiece);
		int piecesToCheck = PiecePickerImpl.REQUESTS_LAST_BYTES / dm.getDiskManager().getPieceLength();
		if (pieces[firstPiece].isDone() && pieces[lastPiece].isDone()) {
			System.out.println("checking " + (2 * piecesToCheck) + " pieces");

			for (int i = firstPiece; i < firstPiece + piecesToCheck; i++) {
				if (!pieces[i].isDone()) {
					firstLastDone = false;
					break;
				}
			}

			for (int i = lastPiece; i > lastPiece - piecesToCheck; i--) {
				if (!pieces[i].isDone()) {
					firstLastDone = false;
					break;
				}
			}

		} else {
			firstLastDone = false;
		}

		if (startIfNot && !firstLastDone) {
			// ok, still need to download some
			// add a emergency piece provider
			EmergencyPieceProvider emergencyProvider = new EmergencyPieceProvider();
			PiecePicker picker = dm.getPeerManager().getPiecePicker();
			emergencyProvider.activate(picker);
			for (int j = firstPiece; j < firstPiece + piecesToCheck; j++) {
				emergencyProvider.boostPiece(j);
				System.out.println("boosting piece " + j);
			}

			if (!pieces[lastPiece].isDone()) {
				emergencyProvider.boostPiece(lastPiece);
				System.out.println("boosting piece " + lastPiece);
			}
		}
		return firstLastDone;
	}

	public void deleteDataFiles() {
		if (dm == null) {
			System.out.println("download manager is null???, canceling file removal");
			return;
		}
		// copied from DownloadManagerImpl (since for some reason the interface does not export this function (!?)
		DiskManagerFactory.deleteDataFiles(dm.getTorrent(),
				dm.getAbsoluteSaveLocation().getParent(),
				dm.getAbsoluteSaveLocation().getName());

		// Attempted fix for bug 1572356 - apparently sometimes when we perform removal of a download's data files,
		// it still somehow gets processed by the move-on-removal rules. I'm making the assumption that this method
		// is only called when a download is about to be removed.
		dm.getDownloadState().setFlag(
				DownloadManagerState.FLAG_DISABLE_AUTO_FILE_MOVE, true);
	}

	//******************************************************

	protected DownloadManager			dm;

	protected DownloadManagerStats dm_stats;

	protected DownloadStatsImpl(DownloadManager _dm) {
		dm = _dm;
		dm_stats = dm.getStats();
	}

	public String getStatus() {
		return (DisplayFormatters.formatDownloadStatusDefaultLocale(dm));
	}

	public String getStatus(boolean localised) {
		return (localised) ? DisplayFormatters.formatDownloadStatus(dm)
				: getStatus();

	}

	public String getDownloadDirectory() {
		return (dm.getSaveLocation().getParent());
	}

	public String getTargetFileOrDir() {
		return (dm.getSaveLocation().toString());
	}

	public String getTrackerStatus() {
		return (dm.getTrackerStatus());
	}

	public int getCompleted() {
		return (dm_stats.getCompleted());
	}

	public int getDownloadCompleted(boolean bLive) {
		return (dm_stats.getDownloadCompleted(bLive));
	}

	public int getCheckingDoneInThousandNotation() {
		org.gudy.azureus2.core3.disk.DiskManager disk = dm.getDiskManager();

		if (disk != null) {

			return (disk.getCompleteRecheckStatus());
		}

		return (-1);
	}

	public long getDownloaded() {
		return (dm_stats.getTotalDataBytesReceived());
	}

	public long getRemaining() {
		return (dm_stats.getRemaining());
	}

	public long getUploaded() {
		return (dm_stats.getTotalDataBytesSent());
	}

	public long getDiscarded() {
		return (dm_stats.getDiscarded());
	}

	public long getDownloadAverage() {
		return (dm_stats.getDataReceiveRate());
	}

	public long getUploadAverage() {
		return (dm_stats.getDataSendRate());
	}

	public long getTotalAverage() {
		return (dm_stats.getTotalAverage());
	}

	public String getElapsedTime() {
		return (dm_stats.getElapsedTime());
	}

	public String getETA() {
		return (DisplayFormatters.formatETA(dm_stats.getETA()));
	}

	public long getHashFails() {
		return (dm_stats.getHashFailCount());
	}

	public int getShareRatio() {
		return (dm_stats.getShareRatio());
	}

	// in ms
	public long getTimeStarted() {
		return (dm_stats.getTimeStarted());
	}

	public float getAvailability() {
		return (dm_stats.getAvailability());
	}

	public long getSecondsOnlySeeding() {
		return dm_stats.getSecondsOnlySeeding();
	}

	public long getSecondsDownloading() {
		return dm_stats.getSecondsDownloading();
	}

	public long getTimeStartedSeeding() {
		return dm_stats.getTimeStartedSeeding();
	}

	public long getSecondsSinceLastDownload() {
		return (dm_stats.getTimeSinceLastDataReceivedInSeconds());
	}

	public long getSecondsSinceLastUpload() {
		return (dm_stats.getTimeSinceLastDataSentInSeconds());
	}

	public int getHealth() {
		switch (dm.getHealthStatus()) {

			case DownloadManager.WEALTH_STOPPED: {
				return (DownloadStats.HEALTH_STOPPED);

			}
			case DownloadManager.WEALTH_NO_TRACKER: {
				return (DownloadStats.HEALTH_NO_TRACKER);

			}
			case DownloadManager.WEALTH_NO_REMOTE: {
				return (DownloadStats.HEALTH_NO_REMOTE);

			}
			case DownloadManager.WEALTH_OK: {
				return (DownloadStats.HEALTH_OK);

			}
			case DownloadManager.WEALTH_KO: {
				return (DownloadStats.HEALTH_KO);

			}
			case DownloadManager.WEALTH_ERROR: {
				return (DownloadStats.HEALTH_ERROR);
			}
			default: {
				Debug.out("Invalid health status");

				return (dm.getHealthStatus());

			}
		}
	}
}
