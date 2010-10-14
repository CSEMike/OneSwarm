package org.gudy.azureus2.core3.disk.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;

/**
 * This class allows plugins to read files sequentially, but still allows them
 * to use the DiskManager's Disk Cache
 * 
 * @author isdal
 * 
 */

public class SequentialDiskReaderImpl
	extends InputStream
{

	// specify the number of seconds of video that has to be done after the
	// current piece to kick in the emergency piece getter
	// private final int MIN_REAL_TIME_LIMIT = 1;

	// specify the min percentage of completed data in the next 10 sec to make a
	// piece an emergency piece
	private static final double			 MIN_EMERGENCY_PERCENTAGE = 0.9;

	private final long								numPiecesFor10sData;

	private EmergencyPieceProvider		emergencyProvider;

	private final DiskManagerPiece[]	pieces;

	private final DiskManager				 diskManager;

	private final DownloadManager		 dm;

	private volatile boolean					quit										 = false;

	private Object										syncObject							 = new Object();

	private int											 currentPiece						 = 0;

	private int											 currentByteInPiece			 = 0;

	private final int								 firstPieceInFile;

	private final int								 lastPieceInFile;

	private final DiskManagerFileInfo fileInfo;

	private long											totalWritten						 = 0;

	public SequentialDiskReaderImpl(DownloadManager dm,
			DiskManagerFileInfo fileInfo, long streamByteRate) {
		System.out.println("Creating Sequential Disk reader");
		this.fileInfo = fileInfo;
		this.dm = dm;
		this.diskManager = dm.getDiskManager();

		this.numPiecesFor10sData = Math.round(streamByteRate * 10.0
				/ diskManager.getPieceLength());
		this.pieces = diskManager.getPieces();
		// this.file = file;
		this.firstPieceInFile = fileInfo.getFirstPieceNumber();
		this.lastPieceInFile = firstPieceInFile + fileInfo.getNumPieces();

		this.currentPiece = firstPieceInFile;
		System.out.println("Sequential Disk reader started");

		// files might start a couple bytes into a piece, try to figure out where it starts
		int fileIndex = fileInfo.getIndex();
		long sumFileLen = 0;
		for (int i = 0; i < fileIndex; i++) {
			sumFileLen += dm.getTorrent().getFiles()[i].getLength();
		}
		int posInPiece = 0;
		if (sumFileLen > 0) {
			posInPiece = (int) (sumFileLen % dm.getTorrent().getPieceLength());
		}
		System.out.println("file starts at byte: " + sumFileLen
				+ " in the torrent, and byte " + posInPiece + " in the piece");
		currentByteInPiece = posInPiece;
		// check if the download is complete
		// boolean bComplete = fileInfo.getLength() == fileInfo.getDownloaded();
		// if (!bComplete) {
		// DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer
		// .getSingleton();
		// if (dmEnhancer != null) {
		// edm = dmEnhancer.getEnhancedDownload(dm);
		// edm.setContentBps(streamBitRate);
		// System.out.println("edm progressive: "
		// + edm.getProgressiveMode());
		// if (edm != null
		// && (!edm.supportsProgressiveMode() || edm
		// .getProgressivePlayETA() > 0)) {
		// return;
		// }
		// }
		// }
	}

	public void asyncReadCompleted() {
		synchronized (syncObject) {
			syncObject.notify();
		}
	}

	/**
	 * reads at most destination.length bytes from the file, waiting if
	 * necessary for data to become available
	 * 
	 * @param destination
	 *            the destination
	 * @return the number of bytes read
	 * @throws IOException
	 */
	@Override
	public int read(byte[] destination, int offset, int bytesToRead)
			throws IOException {
		if (destination == null) {
			throw new NullPointerException();
		}
		if (offset < 0 || bytesToRead < 0
				|| offset + bytesToRead > destination.length) {
			throw new ArrayIndexOutOfBoundsException();
		}

		// int bytesLeftToRead = length;
		int bytesRead = 0;
		// System.out.println("Got request for " + bytesToRead + " bytes");

		Queue<ReadRequestListener> outstandingRequests = new LinkedList<ReadRequestListener>();

		// first, create and submit all requests
		while (bytesToRead > bytesRead && !quit) {
			System.out.println("got request for " + bytesToRead + " currentPiece="
					+ currentPiece + " current byte=" + currentByteInPiece);
			// check if we are outside the file
			if (currentPiece > lastPieceInFile) {

				// check if we actually read anything
				if (bytesRead == 0) {
					// if not, return end of file reached
					return -1;
				}
				break;
			}
			try {
				boolean pieceBoosted = false;
				while (!pieces[currentPiece].isDone() && !quit) {
					// oups, we reached a piece that is not yet downloaded
					// if we already read something, return it
					if (!pieceBoosted) {
						System.out.println("Reached piece that is still downloading "
								+ currentPiece);

						double percentageDone = percentageDoneOfNext10s(pieces,
								currentPiece);
						if (percentageDone > MIN_EMERGENCY_PERCENTAGE) {
							System.out.println("boosting piece");

							if (emergencyProvider == null) {
								PiecePicker picker = dm.getPeerManager().getPiecePicker();
								emergencyProvider = new EmergencyPieceProvider();
								emergencyProvider.activate(picker);
							}
							emergencyProvider.boostPiece(currentPiece);
							pieceBoosted = true;
						} else {
							System.out.println("not boosting it yet, percent done="
									+ percentageDone + " limit=" + MIN_EMERGENCY_PERCENTAGE);

						}
						// // long numDoneBytesAfterPiece =
						// numDoneBytesAfterPiece(
						// pieces, currentPiece);
						// if (numDoneBytesAfterPiece > realTimeLimit) {
						// System.out.println("boosting piece");
						//
						// if (emergencyProvider == null) {
						// PiecePicker picker = dm.getPeerManager()
						// .getPiecePicker();
						// emergencyProvider = new EmergencyPieceProvider();
						// emergencyProvider.activate(picker);
						// }
						// emergencyProvider.boostPiece(currentPiece);
						// pieceBoosted = true;
						// } else {
						// System.out.println("not boosting it yet, numDome="
						// + numDoneBytesAfterPiece + " limit="
						// + realTimeLimit);
						//
						// }
					}
					if (bytesRead > 0) {
						break;
					} else {
						// ok, lets just wait a while and try again
						// TODO I'm sure there is a listener I can attach in
						// some
						// way to get notified about this event, might want to
						// figure that out later

						Thread.sleep(500);

						System.out.println("Waiting for piece " + currentPiece
								+ " to download");
						System.out.println("status of next piece: "
								+ pieces[currentPiece + 1].isDone());
						if (fileInfo.getDownload().getState() == Download.ST_STOPPED) {
							System.out.println("download stopped");
							System.err.println("timed out waiting for download to complete");
							System.err.println("total written from stream: " + totalWritten);
							System.err.println("download manager state: " + dm.getState());
							throw new IOException("read position=" + totalWritten);
						}
					}
				}
			} catch (InterruptedException e) {
			} catch (DownloadException e) {

				e.printStackTrace();
				return -1;
			}
			int bytesLeftInPiece = pieces[currentPiece].getLength()
					- currentByteInPiece;

			int bytesLeftToRead = bytesToRead - bytesRead;
			// check if this read can be handled by this piece
			boolean partialRead = bytesLeftInPiece > bytesLeftToRead;
			DiskManagerReadRequest readRequest;
			if (partialRead) {
				readRequest = diskManager.createReadRequest(currentPiece,
						currentByteInPiece, bytesLeftToRead);
				// move the pointer of where we are in the piece
				currentByteInPiece += bytesLeftToRead;

				bytesRead += bytesLeftToRead;
			} else {
				readRequest = diskManager.createReadRequest(currentPiece,
						currentByteInPiece, bytesLeftInPiece);

				// ok, so after this we need to move on to the next piece
				currentPiece++;
				currentByteInPiece = 0;

				// and update the left to read counter
				bytesRead += bytesLeftInPiece;
			}
			System.out.println("enqueueing read request: "
					+ readRequest.getPieceNumber() + " " + readRequest.getOffset());
			ReadRequestListener listener = new ReadRequestListener(this);
			outstandingRequests.add(listener);
			diskManager.enqueueReadRequest(readRequest, listener);
		}
		ReadRequestListener readRequest = outstandingRequests.poll();

		int writePosition = 0;
		while (readRequest != null) {
			byte[] data = readRequest.getData();

			// wait for the data to become available
			synchronized (syncObject) {

				while (data == null) {
					try {

						System.out.println("Waiting to asynchrounus read to complete "
								+ writePosition + "/" + bytesRead);
						syncObject.wait(2000);
						data = readRequest.getData();
						if (data != null) {
							System.out.println("asynchronus read completed");
						} else {
							System.err.println("timed out waiting for read to complete");
							System.err.println("total written from stream: " + totalWritten);
							System.err.println("download manager state: " + dm.getState());
							if (fileInfo.getDownloaded() == fileInfo.getLength()) {
								// we are done anyway... let the file reader take care of this
								throw new IOException("read position=" + totalWritten);
							} else if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
								// ok, maybe the read was slow, try again
								syncObject.wait(5000);
								data = readRequest.getData();
								if (data != null) {
									System.out.println("asynchronus read completed");
								} else {
									// ok, this must be an error
									return -1;
								}
							}

						}
					} catch (InterruptedException e) {
						System.out.println("SequentialDiskReader interupted");
						return -1;
					}
					if (readRequest.readFailed()) {
						System.out.println("Read failed");
						return -1;
					}

				}
				// great! we got the data, copy and update writePosition
				System.arraycopy(data, 0, destination, writePosition, data.length);
				writePosition += data.length;
				totalWritten += data.length;
			}
			readRequest = outstandingRequests.poll();
		}

		if (writePosition != bytesRead) {
			System.err.println("SequentialDiskReader: Strange, requested "
					+ bytesRead + ", got " + writePosition);
		}

		return bytesRead;
	}

	private double percentageDoneOfNext10s(DiskManagerPiece[] pieces,
			int currentPiece) {
		double total = 0;
		double completed = 0;
		for (int i = currentPiece; i < currentPiece + numPiecesFor10sData && i < pieces.length; i++) {
			DiskManagerPiece piece = pieces[i];
			boolean[] written = piece.getWritten();
			total += piece.getLength();
			if (written == null) {
				completed += piece.getLength();
			} else {
				for (int j = 0; j < written.length; j++) {
					if (written[j]) {
						completed += piece.getBlockSize(j);
					}
				}
			}
		}
		return completed / total;
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		this.read(b);
		return b[0];
	}

	@Override
	public int read(byte[] b) throws IOException {

		return read(b, 0, b.length);

	}

	@Override
	public void close() {

		quit = true;
		if (emergencyProvider != null) {
			emergencyProvider.deactivate(dm.getPeerManager().getPiecePicker());
		}
	}

	@Override
	/**
	 * calculates the number of byte
	 */
	public int available() {
		return 0;
	}

	@Override
	public void mark(int readLimit) {

	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public long skip(long n) {
		return 0;
	}

	public void reset() {
		currentByteInPiece = 0;
		currentPiece = firstPieceInFile;
	}

	private class ReadRequestListener
		implements DiskManagerReadRequestListener
	{

		private final SequentialDiskReaderImpl parent;

		private volatile byte[]								data	= null;

		private volatile boolean							 error = false;

		public ReadRequestListener(SequentialDiskReaderImpl parent) {
			this.parent = parent;
		}

		public int getPriority() {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean readFailed() {
			return error;
		}

		public void readCompleted(DiskManagerReadRequest request,
				DirectByteBuffer dataBuffer) {

			ByteBuffer buffer = dataBuffer.getBuffer(DirectByteBuffer.SS_CACHE);
			buffer.position(0);
			int len = buffer.limit();
			this.data = new byte[len];
			buffer.get(data);
			buffer.position(0);

			parent.asyncReadCompleted();
		}

		public void readFailed(DiskManagerReadRequest request, Throwable cause) {
			cause.printStackTrace();
			error = true;
			parent.asyncReadCompleted();
		}

		public byte[] getData() {
			return data;
		}

		public void requestExecuted(long bytes) {
			// TODO Auto-generated method stub

		}

	}
}
